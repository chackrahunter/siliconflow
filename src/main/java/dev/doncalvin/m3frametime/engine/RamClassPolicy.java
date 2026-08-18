package dev.doncalvin.m3frametime.engine;

import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.telemetry.MemoryPressureProbe;

/**
 * Scales SiliconFlow-owned budgets from physical unified memory class (8 / 16 / 24 / 36+ GB).
 * Never writes {@code -Xmx} and never calls the JVM collector.
 */
public final class RamClassPolicy {
	public enum RamClass {
		GB_8(8),
		GB_16(16),
		GB_24(24),
		GB_36_PLUS(36);

		private final int nominalGb;

		RamClass(int nominalGb) {
			this.nominalGb = nominalGb;
		}

		public int nominalGb() {
			return nominalGb;
		}
	}

	private static volatile boolean registered;
	private static volatile long physicalMemoryMb = -1L;
	private static volatile RamClass ramClass = RamClass.GB_16;

	private RamClassPolicy() {}

	public static void registerPhysicalMemoryMb(long mb) {
		physicalMemoryMb = mb;
		ramClass = classify(mb);
		registered = mb > 0L;
	}

	public static boolean isRegistered() {
		return registered;
	}

	public static long physicalMemoryMb() {
		long mb = physicalMemoryMb;
		if (mb > 0L) {
			return mb;
		}
		return MemoryPressureProbe.get().totalPhysicalMb();
	}

	public static RamClass classify(long physicalMb) {
		if (physicalMb <= 0L) {
			return RamClass.GB_16;
		}
		if (physicalMb < 12L * 1024L) {
			return RamClass.GB_8;
		}
		if (physicalMb < 20L * 1024L) {
			return RamClass.GB_16;
		}
		if (physicalMb < 30L * 1024L) {
			return RamClass.GB_24;
		}
		return RamClass.GB_36_PLUS;
	}

	public static int capParticles(int requested) {
		if (!registered) {
			return requested;
		}
		int cap = switch (ramClass) {
			case GB_8 -> 96;
			case GB_16 -> 192;
			case GB_24 -> 384;
			case GB_36_PLUS -> 1024;
		};
		return Math.max(0, Math.min(requested, cap));
	}

	public static double capEntityCull(double requested) {
		if (!registered) {
			return requested;
		}
		double cap = switch (ramClass) {
			case GB_8 -> 72.0;
			case GB_16 -> 96.0;
			case GB_24 -> 128.0;
			case GB_36_PLUS -> 192.0;
		};
		return Math.min(requested, cap);
	}

	public static double capParticleCull(double requested) {
		if (!registered) {
			return requested;
		}
		double cap = switch (ramClass) {
			case GB_8 -> 32.0;
			case GB_16 -> 48.0;
			case GB_24 -> 64.0;
			case GB_36_PLUS -> 96.0;
		};
		return Math.min(requested, cap);
	}

	public static double capBlockEntityCull(double requested) {
		if (!registered) {
			return requested;
		}
		double cap = switch (ramClass) {
			case GB_8 -> 40.0;
			case GB_16 -> 56.0;
			case GB_24 -> 72.0;
			case GB_36_PLUS -> 96.0;
		};
		return Math.min(requested, cap);
	}

	public static double pressureEnterMb() {
		return switch (ramClass) {
			case GB_8 -> 768.0;
			case GB_16 -> 512.0;
			case GB_24 -> 384.0;
			case GB_36_PLUS -> 256.0;
		};
	}

	public static double pressureRecoverMb() {
		return pressureEnterMb() * 2.0;
	}

	public static int l2ParticleBudget() {
		return switch (ramClass) {
			case GB_8 -> 48;
			case GB_16 -> 64;
			case GB_24 -> 96;
			case GB_36_PLUS -> 128;
		};
	}

	/**
	 * 8 GB machines with a 4G-style heap, or any class where {@code -Xmx} takes half of UMA.
	 * Detects {@link Runtime#maxMemory()} vs physical RAM; never mutates JVM args.
	 */
	public static boolean heapVsPhysicalUnhealthy() {
		long physical = physicalMemoryMb();
		long heapMaxMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
		if (physical <= 0L || heapMaxMb <= 0L) {
			return false;
		}
		RamClass rc = classify(physical);
		if (rc == RamClass.GB_8 && heapMaxMb >= 4096L) {
			return true;
		}
		return heapMaxMb >= physical;
	}

	/** Tighten SiliconFlow budgets on the live config. Does not {@code save()}. */
	public static void applySessionCaps(M3Config cfg) {
		if (cfg == null || !registered) {
			return;
		}
		cfg.maxParticles = capParticles(cfg.maxParticles);
		cfg.entityCullDistance = capEntityCull(cfg.entityCullDistance);
		cfg.particleCullDistance = capParticleCull(cfg.particleCullDistance);
		cfg.blockEntityCullDistance = capBlockEntityCull(cfg.blockEntityCullDistance);
		cfg.memoryPressureFreeMbThreshold = Math.max(cfg.memoryPressureFreeMbThreshold, pressureEnterMb());
		cfg.memoryPressureRecoverMbThreshold = Math.max(
			cfg.memoryPressureRecoverMbThreshold,
			Math.max(cfg.memoryPressureFreeMbThreshold, pressureRecoverMb())
		);
	}
}
