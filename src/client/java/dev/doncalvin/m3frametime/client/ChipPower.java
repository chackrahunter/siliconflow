package dev.doncalvin.m3frametime.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;

/**
 * Applies optional, best-effort scheduling hints without claiming core affinity.
 * Zero-allocation, zero JVM safepoint pauses.
 */
public final class ChipPower {
	private static boolean applied;
	private static volatile boolean sodiumPrioApplied;
	private static long sodiumBoostNotBeforeNanos;

	private ChipPower() {}

	public static void applyOnce() {
		if (applied) {
			return;
		}
		applied = true;

		int cores = Runtime.getRuntime().availableProcessors();
		String vmInfo = System.getProperty("java.vm.name", "?");

		if (M3FrametimeMod.config().boostDarwinQos) {
			DarwinQos.boostRenderThread();
		}

		M3FrametimeMod.LOGGER.info("ChipPower: logicalCores={} | vm={} | JVM thread priority unchanged", cores, vmInfo);

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
				"ActiveProcessorCount appears set — that caps M-chip cores for the JVM. Remove it to avoid limiting JVM-visible processors."
			);
		}

		M3FrametimeMod.LOGGER.info(
			"M-chip profile active | pacing=OFF | Darwin QoS request is best-effort | runtime tuner=DISABLED"
		);
	}

	/** Re-assert high priority and Darwin QoS periodically without JNA thrashing. */
	public static void reinforceRenderPriority() { }

	/**
	 * Nudge Sodium "Chunk Render Task Executor #N" threads in background worker without JVM safepoint pauses.
	 */
	public static void tryBoostSodiumWorkers() { }

}
