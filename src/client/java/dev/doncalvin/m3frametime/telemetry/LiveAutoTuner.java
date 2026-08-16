package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.DarwinQos;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.pacing.FramePacer;

/**
 * Intelligent Real-Time Auto-Tuner for Apple Silicon M3.
 * Continuously evaluates frametime variance and automatically self-adjusts
 * culling gates, shadow radii, and particle budgets in real time to prevent stutters.
 */
public final class LiveAutoTuner {
	private static final LiveAutoTuner INSTANCE = new LiveAutoTuner();

	private long lastTuneNanos;
	private int consecutiveSpikes;
	private int stableFrames;

	private LiveAutoTuner() {}

	public static LiveAutoTuner get() {
		return INSTANCE;
	}

	public void tick(long nowNanos) {
		if (nowNanos - lastTuneNanos < 1_000_000_000L) { // Evaluate every 1 second
			return;
		}
		lastTuneNanos = nowNanos;

		SpikeMonitor monitor = SpikeMonitor.get();
		FramePacer pacer = FramePacer.get();
		double p99Ms = monitor.percentileMs(0.99);
		double emaMs = pacer.emaDeltaMs();

		// If frametime is fluctuating severely (>16.6ms / <60 FPS or p99 is 2x of EMA)
		if (p99Ms > 16.6 || (emaMs > 0 && p99Ms > emaMs * 2.0)) {
			consecutiveSpikes++;
			stableFrames = 0;
			if (consecutiveSpikes >= 2) {
				applyEmergencyOptimization();
			}
		} else {
			stableFrames++;
			if (stableFrames >= 10) {
				consecutiveSpikes = 0;
			}
		}
	}

	/** Dynamically tightens culling & priorities in real-time when heavy load is detected. */
	private void applyEmergencyOptimization() {
		var cfg = M3FrametimeMod.config();
		
		// 1. Re-affirm Darwin Mach QoS P-Core lock
		DarwinQos.boostRenderThread();

		// 2. If Shaders are active and causing GPU spikes, tighten shadow entity distance
		if (StackCompat.isShaderActive()) {
			if (cfg.shadowEntityDistance > 24.0) {
				cfg.shadowEntityDistance = 24.0;
			}
		}

		// 3. Trim particle queues under stress
		if (cfg.maxParticles > 128) {
			cfg.maxParticles = 128;
		}

		// 4. Request RamDiscipline soft cleanup
		RamDiscipline.get().onClientTick();
	}
}
