package dev.doncalvin.m3frametime.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;

/**
 * Keeps Apple Silicon P-cores prioritized for Minecraft render thread and Sodium chunk builders.
 * Leverages native Darwin Mach thread QoS classes and high JVM thread priorities.
 * Zero-allocation, zero JVM safepoint pauses.
 */
public final class ChipPower {
	private static boolean applied;
	private static volatile boolean sodiumPrioApplied;

	private ChipPower() {}

	public static void applyOnce() {
		if (applied) {
			return;
		}
		applied = true;

		int cores = Runtime.getRuntime().availableProcessors();
		String vmInfo = System.getProperty("java.vm.name", "?");

		raiseForkJoinParallelism(cores);

		if (M3FrametimeMod.config().boostDarwinQos) {
			DarwinQos.boostRenderThread();
		}

		if (M3FrametimeMod.config().boostRenderThreadPriority) {
			Thread t = Thread.currentThread();
			try {
				t.setPriority(Thread.MAX_PRIORITY);
				M3FrametimeMod.LOGGER.info(
					"ChipPower: renderThread MAX_PRIORITY (name={}) | DarwinQos={} | logicalCores={} | vm={}",
					t.getName(),
					M3FrametimeMod.config().boostDarwinQos,
					cores,
					vmInfo
				);
			} catch (SecurityException e) {
				M3FrametimeMod.LOGGER.warn("Could not boost render thread priority: {}", e.toString());
			}
		} else {
			M3FrametimeMod.LOGGER.info("ChipPower: logicalCores={} (render boost disabled)", cores);
		}

		if (StackCompat.sodium()) {
			int auto = SodiumSoftBooster.sodiumAutoThreadCount(cores);
			int target = SodiumSoftBooster.mChipTargetThreads(cores);
			M3FrametimeMod.LOGGER.info(
				"ChipPower: Sodium worker target={} (Sodium auto≈{}) | availableProcessors={}",
				target,
				auto,
				cores
			);
			SodiumSoftBooster.applyIfNeeded();
		}

		String toolOpts = System.getenv("JAVA_TOOL_OPTIONS");
		if (System.getProperty("sun.java.command", "").contains("ActiveProcessorCount")
			|| (toolOpts != null && toolOpts.contains("ActiveProcessorCount"))) {
			M3FrametimeMod.LOGGER.warn(
				"ActiveProcessorCount appears set — that caps M-chip cores for the JVM. Remove it for full performance."
			);
		}

		M3FrametimeMod.LOGGER.info(
			"M-chip ultra-performance: pacing=OFF | Darwin QoS=ON | Sodium feeds P-cores | Shader shadow culling=ON"
		);
	}

	private static void raiseForkJoinParallelism(int cores) {
		String key = "java.util.concurrent.ForkJoinPool.common.parallelism";
		String existing = System.getProperty(key);
		int want = Math.max(1, cores - 1);
		if (existing == null || existing.isBlank()) {
			System.setProperty(key, Integer.toString(want));
			M3FrametimeMod.LOGGER.info("ChipPower: set {}={} (if commonPool not yet created)", key, want);
		}
		try {
			int live = java.util.concurrent.ForkJoinPool.commonPool().getParallelism();
			M3FrametimeMod.LOGGER.info("ChipPower: ForkJoinPool.commonPool parallelism={}", live);
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("ForkJoinPool probe: {}", t.toString());
		}
	}

	/** Re-assert high priority and Darwin QoS periodically without JNA thrashing. */
	public static void reinforceRenderPriority() {
		if (M3FrametimeMod.config().boostDarwinQos) {
			DarwinQos.boostRenderThread();
		}
		if (!M3FrametimeMod.config().boostRenderThreadPriority) {
			return;
		}
		Thread t = Thread.currentThread();
		if (t.getPriority() < Thread.MAX_PRIORITY - 1) {
			try {
				t.setPriority(Thread.MAX_PRIORITY);
			} catch (SecurityException ignored) {
			}
		}
	}

	/**
	 * Nudge Sodium "Chunk Render Task Executor #N" threads in background worker without JVM safepoint pauses.
	 */
	public static void tryBoostSodiumWorkers() {
		if (sodiumPrioApplied || !StackCompat.sodium()) {
			return;
		}
		sodiumPrioApplied = true;

		AdaptiveWorkerPool.get().execute(() -> {
			try {
				Thread.sleep(2500); // Allow Sodium chunk builders to spawn
				ThreadGroup root = Thread.currentThread().getThreadGroup();
				while (root.getParent() != null) {
					root = root.getParent();
				}
				Thread[] threads = new Thread[root.activeCount() + 32];
				int count = root.enumerate(threads, true);
				int want = Math.min(Thread.MAX_PRIORITY - 1, Thread.NORM_PRIORITY + 1);
				int boosted = 0;
				for (int i = 0; i < count; i++) {
					Thread t = threads[i];
					if (t != null && t.getName() != null && t.getName().startsWith("Chunk Render Task Executor")) {
						try {
							if (t.getPriority() < want) {
								t.setPriority(want);
								boosted++;
							}
						} catch (SecurityException ignored) {
						}
					}
				}
				if (boosted > 0) {
					M3FrametimeMod.LOGGER.info(
						"ChipPower: boosted {} Sodium chunk-worker thread(s) to priority {} (render stays MAX)",
						boosted,
						want
					);
				}
			} catch (Throwable ignored) {
			}
		});
	}
}
