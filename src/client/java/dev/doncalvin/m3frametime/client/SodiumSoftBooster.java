package dev.doncalvin.m3frametime.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.engine.SiliconCpuTopology;
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
 * Uses a bounded, explicit worker-count request only when the user enables it.
 * Sodium and macOS retain ownership of scheduling and worker implementation.
 * Core placement remains a macOS scheduler decision.
 * and prevents chunk queue saturation (CHK-001).
 */
public final class SodiumSoftBooster {
	private static final String OPTIONS_CLASS = "net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions";
	private static final String CLIENT_MOD = "net.caffeinemc.mods.sodium.client.SodiumClientMod";

	private static boolean applied;
	private static int lastTarget = -1;
	private static int lastConfigured = -1;
	private static int lastPatchedTarget = -1;
	private static final com.google.gson.Gson JSON = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

	private SodiumSoftBooster() {}

	/**
	 * Chip-adaptive auto-detection: delegates to topology when available,
	 * otherwise falls back to Sodium's published formula.
	 */
	public static int sodiumAutoThreadCount(int cores) {
		try {
			return SiliconCpuTopology.get().getSodiumWorkerThreads();
		} catch (Exception e) {
			int max = Math.max(1, cores);
			return Math.min(10, Math.max(1, Math.max(max / 3, max - 6)));
		}
	}

	/**
	 * Chip-adaptive worker count from topology, with user-config override.
	 */
	public static int mChipTargetThreads(int cores) {
		int target;
		try {
			target = SiliconCpuTopology.get().getSodiumWorkerThreads();
		} catch (Exception e) {
			int c = Math.max(1, cores);
			target = Math.min(3, Math.max(2, c / 2));
		}
		int configured = M3FrametimeMod.config().sodiumChunkBuilderThreads;
		if (configured > 0) {
			target = Math.min(cores, configured);
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
				"Sodium chunk-builder setting applied; OS core placement is not measurable or guaranteed"
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
		if (lastPatchedTarget == target) {
			return true;
		}
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
			// Do not claim ownership of Sodium's allocator, vertex format, or culling.
			// This opt-in bridge changes only the explicitly requested worker count.
			Files.createDirectories(path.getParent());
			Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temporary)) {
				JSON.toJson(root, writer);
			}
			try {
				Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
			lastConfigured = target;
			lastPatchedTarget = target;
			return true;
		} catch (Exception e) {
			M3FrametimeMod.LOGGER.debug("sodium-options.json patch failed: {}", e.toString());
			return false;
		}
	}
}
