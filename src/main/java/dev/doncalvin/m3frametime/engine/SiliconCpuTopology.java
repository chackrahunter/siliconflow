package dev.doncalvin.m3frametime.engine;

import dev.doncalvin.m3frametime.M3FrametimeMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Apple Silicon CPU Topology & Asymmetric Multiprocessing (AMP) Router.
 * Reports estimated Apple Silicon topology for diagnostics and conservative worker sizing.
 * macOS retains control over actual thread placement and scheduling.
 */
public final class SiliconCpuTopology {

	private static final SiliconCpuTopology INSTANCE = new SiliconCpuTopology();

	private final int totalCores;
	private final int estimatedPCores;
	private final int estimatedECores;
	private final String chipName;
	private final ChipTier chipTier;
	private final int chipGeneration;
	private final int gpuCoreCount;
	private final long totalPhysicalMemoryMb;

	private SiliconCpuTopology() {
		this.totalCores = Runtime.getRuntime().availableProcessors();

		String model = sysctlString("machdep.cpu.brand_string");
		if (parseGeneration(model) == 0) {
			model = sysctlString("hw.model");
		}
		int sysctlP = sysctlInt("hw.perflevel0.physicalcpu");
		int sysctlE = sysctlInt("hw.perflevel1.physicalcpu");
		long totalMemBytes = sysctlLong("hw.memsize");
		this.totalPhysicalMemoryMb = totalMemBytes > 0 ? totalMemBytes / (1024L * 1024L) : -1;

		int parsedGen = parseGeneration(model);
		ChipTier parsedTier = parseTier(model);

		if (model != null && !model.isBlank() && parsedGen > 0) {
			String trimmedModel = model.trim();
			this.chipName = trimmedModel.toLowerCase(Locale.ROOT).startsWith("apple ")
				? trimmedModel
				: "Apple " + trimmedModel;
			this.chipGeneration = parsedGen;
			this.chipTier = parsedTier;
			this.gpuCoreCount = estimateGpuCores(model);

			if (sysctlP > 0 && sysctlE > 0) {
				this.estimatedPCores = sysctlP;
				this.estimatedECores = sysctlE;
			} else {
				this.estimatedPCores = -1;
				this.estimatedECores = -1;
			}
		} else {
			this.chipName = "Unknown Silicon (" + totalCores + " cores)";
			this.chipTier = inferTierFromCores(totalCores);
			this.chipGeneration = 0;
			this.gpuCoreCount = 0;
			this.estimatedPCores = -1;
			this.estimatedECores = -1;
		}
		SiliconChipInfo.register(this.chipTier);
		RamClassPolicy.registerPhysicalMemoryMb(this.totalPhysicalMemoryMb);

		M3FrametimeMod.LOGGER.info(
			"SiliconCpuTopology: {} (Tier={}, Gen=M{}, P={}cores, E={}cores, GPU={}cores, RAM={}MB class={}GB)",
			chipName, chipTier, chipGeneration,
			estimatedPCores > 0 ? estimatedPCores : "?",
			estimatedECores > 0 ? estimatedECores : "?",
			gpuCoreCount,
			totalPhysicalMemoryMb > 0 ? totalPhysicalMemoryMb : "?",
			getRamClassGb()
		);
	}

	public static SiliconCpuTopology get() {
		return INSTANCE;
	}

	public int getTotalCores() {
		return totalCores;
	}

	public int getEstimatedPCores() {
		return estimatedPCores;
	}

	public int getEstimatedECores() {
		return estimatedECores;
	}

	public String getChipName() {
		return chipName;
	}

	public ChipTier getChipTier() {
		return chipTier;
	}

	public int getChipGeneration() {
		return chipGeneration;
	}

	public int getGpuCoreCount() {
		return gpuCoreCount;
	}

	public long getTotalPhysicalMemoryMb() {
		return totalPhysicalMemoryMb;
	}

	public RamClassPolicy.RamClass getRamClass() {
		return RamClassPolicy.classify(totalPhysicalMemoryMb);
	}

	public int getRamClassGb() {
		if (totalPhysicalMemoryMb <= 0L) {
			return 0;
		}
		return getRamClass().nominalGb();
	}

	/** Heap vs physical UMA mismatch — see {@link RamClassPolicy#heapVsPhysicalUnhealthy()}. */
	public boolean heapVsPhysicalUnhealthy() {
		return RamClassPolicy.heapVsPhysicalUnhealthy();
	}

	private static int parseGeneration(String model) {
		if (model == null) return 0;
		String m = model.trim().toLowerCase();
		if (m.contains("m1")) return 1;
		if (m.contains("m2")) return 2;
		if (m.contains("m3")) return 3;
		if (m.contains("m4")) return 4;
		if (m.contains("m5")) return 5;
		return 0;
	}

	private static ChipTier parseTier(String model) {
		if (model == null) return ChipTier.BASE;
		String m = model.trim().toLowerCase();
		if (m.contains("ultra")) return ChipTier.ULTRA;
		if (m.contains("max")) return ChipTier.MAX;
		if (m.contains("pro")) return ChipTier.PRO;
		return ChipTier.BASE;
	}

	private static int estimateGpuCores(String model) {
		if (model == null) return 10;
		String m = model.trim().toLowerCase();

		if (m.contains("m1 ultra")) return 64;
		if (m.contains("m1 max")) return 32;
		if (m.contains("m1 pro")) return 16;
		if (m.contains("m1")) return 8;

		if (m.contains("m2 ultra")) return 76;
		if (m.contains("m2 max")) return 38;
		if (m.contains("m2 pro")) return 19;
		if (m.contains("m2")) return 10;

		if (m.contains("m3 ultra")) return 80;
		if (m.contains("m3 max")) return 40;
		if (m.contains("m3 pro")) return 16;
		if (m.contains("m3")) return 10;

		if (m.contains("m4 ultra")) return 80;
		if (m.contains("m4 max")) return 40;
		if (m.contains("m4 pro")) return 20;
		if (m.contains("m4")) return 10;

		if (m.contains("m5 ultra")) return 80;
		if (m.contains("m5 max")) return 40;
		if (m.contains("m5 pro")) return 20;
		if (m.contains("m5")) return 10;

		return 10;
	}

	private static ChipTier inferTierFromCores(int cores) {
		if (cores <= 8) return ChipTier.BASE;
		if (cores <= 12) return ChipTier.PRO;
		if (cores <= 16) return ChipTier.MAX;
		return ChipTier.ULTRA;
	}

	/**
	 * Query a sysctl string value via ProcessBuilder.
	 * Called only once at init — never in a render hot path.
	 */
	private static String sysctlString(String key) {
		if (!isMacOS()) return null;
		Process proc = null;
		try {
			ProcessBuilder pb = new ProcessBuilder("sysctl", "-n", key);
			pb.redirectErrorStream(true);
			proc = pb.start();
			String result;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
				result = reader.readLine();
			}
			boolean finished = proc.waitFor(3, TimeUnit.SECONDS);
			if (!finished) {
				return null;
			}
			if (proc.exitValue() != 0 || result == null || result.isBlank()) {
				return null;
			}
			return result.trim();
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("sysctl query failed for {}: {}", key, t.toString());
			return null;
		} finally {
			if (proc != null && proc.isAlive()) {
				proc.destroyForcibly();
			}
		}
	}

	/** Query a sysctl integer value. Returns -1 on failure. */
	private static int sysctlInt(String key) {
		String val = sysctlString(key);
		if (val == null) return -1;
		try {
			return Integer.parseInt(val.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/** Query a sysctl long value. Returns -1 on failure. */
	private static long sysctlLong(String key) {
		String val = sysctlString(key);
		if (val == null) return -1;
		try {
			return Long.parseLong(val.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static boolean isMacOS() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return os.contains("mac") || os.contains("darwin");
	}
}
