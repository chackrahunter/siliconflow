package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.pool.ScratchPool;


/**
 * Proactive memory discipline for Apple Silicon M3 (8 GB Unified RAM).
 * Damps allocation velocity before ZGC trigger threshold (preventing GC-001 / GC-002 pauses).
 * 100% Zero emergency downgrades: user owns all Video and Render Distance settings.
 */
public final class RamDiscipline {
	private static final RamDiscipline INSTANCE = new RamDiscipline();

	private int tickCounter;
	private int enterSamples;
	private int recoverSamples;
	private boolean pressureMode;
	private long lastPolicySampleNanos;

	private RamDiscipline() {}

	public static RamDiscipline get() {
		return INSTANCE;
	}

	/**
	 * Client-tick entry: proactive GC damping & soft cache maintenance.
	 */
	public void onClientTick() {
		M3Config cfg = M3FrametimeMod.config();
		MemoryPressureProbe probe = MemoryPressureProbe.get();
		if (cfg.memoryPolicyEnabled && (tickCounter & 7) == 0) {
			probe.sampleHeap();
			probe.requestSample();
		}

		long heapUsed = probe.heapUsedMb();
		int tick = ++tickCounter;

		// Use the actual heap ceiling instead of a fixed 1400 MB cutoff. This only
		// releases ephemeral caches and never changes user video/shader settings.
		long heapMax = probe.heapMaxMb();
		boolean highHeap = heapMax > 0L && heapUsed >= Math.round(heapMax * 0.90);
		boolean physicalPressure = probe.physicalUnderPressure();
		boolean heapPressure = probe.heapUnderPressure();
		long policySample = probe.sampleTimestampNanos();
		if (policySample != lastPolicySampleNanos && policySample > 0L) {
			lastPolicySampleNanos = policySample;
			updatePressureMode(cfg, probe, physicalPressure, heapPressure);
		}
		boolean hintNow = (cfg.softCacheHints && cfg.softCacheHintIntervalTicks > 0 && (tick % cfg.softCacheHintIntervalTicks) == 0)
			|| pressureMode;

		if (hintNow) {
			softCacheHint(pressureMode);
		}
	}

	/**
	 * Clear thread-local scratch pools and request particle queue trim.
	 * Explicitly avoids calling blocking the JVM collector.
	 */
	public void softCacheHint(boolean aggressive) {
		ScratchPool.releaseAllEphemeral();
	}

	public boolean pressureMode() { return pressureMode; }

	private void updatePressureMode(M3Config cfg, MemoryPressureProbe probe, boolean physical, boolean heap) {
		if (!cfg.memoryPolicyEnabled) {
			pressureMode = false;
			enterSamples = 0;
			recoverSamples = 0;
			return;
		}
		boolean candidate = heap || (probe.isFresh() && physical);
		if (!pressureMode) {
			recoverSamples = 0;
			if (candidate && ++enterSamples >= cfg.memoryPressureEnterSamples) {
				pressureMode = true;
				enterSamples = 0;
			}
		} else {
			boolean recovered = !heap && (!probe.isFresh() || probe.freePhysicalMb() >= Math.round(cfg.memoryPressureRecoverMbThreshold));
			if (recovered && ++recoverSamples >= cfg.memoryPressureRecoverSamples) {
				pressureMode = false;
				recoverSamples = 0;
			} else if (!recovered) {
				recoverSamples = 0;
			}
		}
	}

}
