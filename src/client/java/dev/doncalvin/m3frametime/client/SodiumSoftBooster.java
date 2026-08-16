package dev.doncalvin.m3frametime.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Soft-boosts Sodium chunk-builder thread count for Apple Silicon without starving the Render Thread.
 * <p>
 * On 8-core Apple Silicon M3 (4 P-Cores + 4 E-Cores), allocating 3 chunk builder threads
 * allows rapid terrain meshing while guaranteeing 1 P-Core is 100% dedicated to the Render Thread.
 * This permanently prevents macOS from demoting the Render Thread to slow E-Cores (SYS-001)
 * and prevents chunk queue saturation (CHK-001).
 */
public final class SodiumSoftBooster {
	private static final String OPTIONS_CLASS = "net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions";
	private static final String CLIENT_MOD = "net.caffeinemc.mods.sodium.client.SodiumClientMod";

	private static boolean applied;
	private static int lastTarget = -1;
	private static int lastConfigured = -1;

	private SodiumSoftBooster() {}

	/** Sodium's published auto formula (for logs / docs). */
	public static int sodiumAutoThreadCount(int cores) {
		int max = Math.max(1, cores);
		return Math.min(10, Math.max(1, Math.max(max / 3, max - 6)));
	}

	/**
	 * Balanced M-chip worker count: Leaves at least one full P-Core exclusively for the Render Thread.
	 */
	public static int mChipTargetThreads(int cores) {
		int c = Math.max(1, cores);
		// On 8-core Apple Silicon (4P + 4E), 3 builder threads leaves 1 P-Core dedicated to Render Thread.
		int target = Math.min(3, Math.max(2, c / 2));
		int configured = M3FrametimeMod.config().sodiumChunkBuilderThreads;
		if (configured > 0) {
			target = Math.min(c, configured);
		}
		return Math.max(1, target);
	}

	public static int lastTarget() {
		return lastTarget;
	}

	public static int lastConfigured() {
		return lastConfigured;
	}

	public static boolean applied() {
		return applied;
	}

	/**
	 * Set optimal {@code performance.chunkBuilderThreads} via reflection + persist.
	 */
	public static synchronized void applyIfNeeded() {
		if (applied || !StackCompat.sodium()) {
			return;
		}
		var cfg = M3FrametimeMod.config();
		if (!cfg.boostSodiumChunkBuilderThreads) {
			M3FrametimeMod.LOGGER.info("SodiumSoftBooster skipped (boostSodiumChunkBuilderThreads=false)");
			applied = true;
			return;
		}

		int cores = Runtime.getRuntime().availableProcessors();
		int auto = sodiumAutoThreadCount(cores);
		int target = mChipTargetThreads(cores);
		lastTarget = target;

		boolean runtimeOk = boostRuntimeOptions(target);
		boolean diskOk = patchOptionsJson(target);

		if (runtimeOk) {
			applied = true;
			M3FrametimeMod.LOGGER.info(
				"SodiumSoftBooster: chunk_builder_threads → {} (Sodium auto={}, cores={}, diskPersist={})",
				target,
				auto,
				cores,
				diskOk
			);
			M3FrametimeMod.LOGGER.info(
				"Apple Silicon optimization: 1 P-Core reserved exclusively for Render Thread, 3 threads for Sodium meshing"
			);
		} else if (diskOk) {
			M3FrametimeMod.LOGGER.info(
				"SodiumSoftBooster: patched sodium-options.json → {} (runtime options will apply on load)",
				target
			);
		} else {
			M3FrametimeMod.LOGGER.warn(
				"SodiumSoftBooster: could not set chunk_builder_threads={} yet (will retry).",
				target
			);
		}
	}

	private static boolean boostRuntimeOptions(int target) {
		try {
			Class<?> mod = Class.forName(CLIENT_MOD);
			Method optionsMethod = mod.getMethod("options");
			Object options = optionsMethod.invoke(null);
			if (options == null) {
				return false;
			}

			Field performanceField = options.getClass().getField("performance");
			Object performance = performanceField.get(options);
			Field threadsField = performance.getClass().getField("chunkBuilderThreads");
			int current = threadsField.getInt(performance);
			lastConfigured = current;

			threadsField.setInt(performance, target);
			lastConfigured = target;

			try {
				Class<?> optsClass = Class.forName(OPTIONS_CLASS);
				Method write = optsClass.getMethod("writeToDisk", optsClass);
				write.invoke(null, options);
			} catch (ReflectiveOperationException writeEx) {
				M3FrametimeMod.LOGGER.debug("Sodium writeToDisk skipped: {}", writeEx.toString());
			}
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		} catch (ReflectiveOperationException | IllegalStateException e) {
			M3FrametimeMod.LOGGER.debug("Sodium runtime options not ready: {}", e.toString());
			return false;
		}
	}

	private static boolean patchOptionsJson(int target) {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("sodium-options.json");
		try {
			JsonObject root;
			if (Files.isRegularFile(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					root = JsonParser.parseReader(reader).getAsJsonObject();
				}
			} else {
				root = new JsonObject();
			}

			JsonObject performance = root.has("performance") && root.get("performance").isJsonObject()
				? root.getAsJsonObject("performance")
				: new JsonObject();
			root.add("performance", performance);

			performance.addProperty("chunk_builder_threads", target);
			// Apple Silicon TBDR GPU direct zero-copy upload
			if (!performance.has("chunk_memory_allocator")) {
				performance.addProperty("chunk_memory_allocator", "SWAP");
			}
			performance.addProperty("use_compact_vertex_format", true);
			performance.addProperty("use_block_face_culling", true);

			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
			}
			lastConfigured = target;
			return true;
		} catch (Exception e) {
			M3FrametimeMod.LOGGER.debug("sodium-options.json patch failed: {}", e.toString());
			return false;
		}
	}
}
