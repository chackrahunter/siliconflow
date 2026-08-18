package dev.doncalvin.m3frametime.pacing;

import java.util.concurrent.locks.LockSupport;

/**
 * Sub-millisecond frame delta tracker with EMA smoothing, variance tracking,
 * and optional pace-to-refresh. Does not replace Minecraft's main loop.
 */
public final class FramePacer {
	private static final FramePacer INSTANCE = new FramePacer();
	private static final int VARIANCE_WINDOW = 16;

	private long lastFrameNanos;
	private long lastDeltaNanos;
	private double emaDeltaNanos;
	private long targetFrameNanos = 16_666_666L; // ~60 Hz default
	private boolean hasSample;

	// Frame-time variance tracking for jitter detection
	private final long[] recentDeltas = new long[VARIANCE_WINDOW];
	private int varianceIdx;
	private int varianceCount;
	private double cachedVariance;
	private long lastVarianceUpdateFrame;

	private FramePacer() {}

	public static FramePacer get() {
		return INSTANCE;
	}

	public void setRefreshRateHz(int hz) {
		if (hz <= 0) {
			hz = 60;
		}
		targetFrameNanos = 1_000_000_000L / hz;
	}

	public long targetFrameNanos() {
		return targetFrameNanos;
	}

	/** Call at the start of each client frame. Returns delta nanos since previous begin. */
	public long beginFrame() {
		long now = System.nanoTime();
		if (!hasSample) {
			lastFrameNanos = now;
			hasSample = true;
			lastDeltaNanos = targetFrameNanos;
			emaDeltaNanos = targetFrameNanos;
			return lastDeltaNanos;
		}
		long delta = now - lastFrameNanos;
		if (delta < 0) {
			delta = targetFrameNanos;
		}
		lastFrameNanos = now;
		lastDeltaNanos = delta;
		return delta;
	}

	public void updateEma(double alpha) {
		if (alpha <= 0.0 || alpha > 1.0) {
			alpha = 0.12;
		}
		emaDeltaNanos = emaDeltaNanos * (1.0 - alpha) + lastDeltaNanos * alpha;

		// Track recent frame deltas for variance calculation
		recentDeltas[varianceIdx & (VARIANCE_WINDOW - 1)] = lastDeltaNanos;
		varianceIdx++;
		if (varianceCount < VARIANCE_WINDOW) varianceCount++;
	}

	/**
	 * Optional post-frame pacing: busy-spin the last ~0.2 ms, sleep the rest.
	 * Diagnostic only — defaults off. Can devastate FPS when combined with VSync
	 * or a wrong refresh-rate read; never call unless pacingEnabled is true.
	 */
	public void paceIfNeeded(boolean enabled) {
		if (!enabled || !hasSample) {
			return;
		}
		long elapsed = System.nanoTime() - lastFrameNanos;
		long remaining = targetFrameNanos - elapsed;
		if (remaining <= 50_000L) {
			return;
		}
		long sleepNanos = remaining - 200_000L;
		if (sleepNanos > 0) {
			LockSupport.parkNanos(sleepNanos);
			if (Thread.interrupted()) {
				return;
			}
		}
		while (System.nanoTime() - lastFrameNanos < targetFrameNanos) {
			Thread.onSpinWait();
		}
	}

	public long lastDeltaNanos() {
		return lastDeltaNanos;
	}

	public double emaDeltaNanos() {
		return emaDeltaNanos;
	}

	public double lastDeltaMs() {
		return lastDeltaNanos / 1_000_000.0;
	}

	public double emaDeltaMs() {
		return emaDeltaNanos / 1_000_000.0;
	}

	/**
	 * Frame-time variance in nanoseconds squared.
	 * High variance indicates jitter / micro-stutter even if average FPS is acceptable.
	 */
	public double frametimeVariance() {
		if (varianceCount < 4) return 0.0;
		double mean = emaDeltaNanos;
		double sumSq = 0.0;
		for (int i = 0; i < varianceCount; i++) {
			double diff = recentDeltas[i] - mean;
			sumSq += diff * diff;
		}
		return sumSq / varianceCount;
	}

	/** Coefficient of variation (stddev / mean) as a percentage. >15% = notable jitter. */
	public double jitterPercent() {
		if (emaDeltaNanos <= 0.0) return 0.0;
		double variance = frametimeVariance();
		return (Math.sqrt(variance) / emaDeltaNanos) * 100.0;
	}
}
