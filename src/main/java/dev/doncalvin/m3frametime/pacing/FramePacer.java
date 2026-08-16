package dev.doncalvin.m3frametime.pacing;

/**
 * Sub-millisecond frame delta tracker with EMA smoothing and optional pace-to-refresh.
 * Does not replace Minecraft's main loop — measures and optionally sleeps after swap.
 */
public final class FramePacer {
	private static final FramePacer INSTANCE = new FramePacer();

	private long lastFrameNanos;
	private long lastDeltaNanos;
	private double emaDeltaNanos;
	private long targetFrameNanos = 16_666_666L; // ~60 Hz default
	private boolean hasSample;

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
			long millis = sleepNanos / 1_000_000L;
			int nanos = (int) (sleepNanos % 1_000_000L);
			try {
				Thread.sleep(millis, nanos);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
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
}
