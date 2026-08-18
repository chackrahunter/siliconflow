package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.engine.RamClassPolicy;
import dev.doncalvin.m3frametime.pool.ParticleTrim;
import dev.doncalvin.m3frametime.pool.ScratchPool;

/**
 * Proactive 3-tier memory discipline for Apple Silicon.
 * Damps allocation velocity before GC threshold (preventing GC-001 / GC-002 pauses).
 * 100% Zero emergency downgrades: user owns all Video and Render Distance settings.
 *
 * Pressure levels:
 *   NORMAL  — standard caching, periodic ScratchPool trims
 *   CAUTION — heap >75% or elevated allocation rate
 *   PRESSURE — heap >85% or very high allocation rate, maximum trims
 */
public final class RamDiscipline {
	private static final RamDiscipline INSTANCE = new RamDiscipline();

	/** Allocation rate that pulls the 3-tier gauge into CAUTION. */
	private static final long ALLOC_CAUTION_BPS = 64L * 1024L * 1024L;
	/** Allocation rate that pulls the 3-tier gauge into PRESSURE. */
	private static final long ALLOC_PRESSURE_BPS = 192L * 1024L * 1024L;

	/** Current pressure level — observable from telemetry and diagnostics. */
	public enum PressureLevel {
		NORMAL, CAUTION, PRESSURE
	}

	private int tickCounter;
	private int enterSamples;
	private int recoverSamples;
	private boolean pressureMode;
	private PressureLevel pressureLevel = PressureLevel.NORMAL;
	private long lastPolicySampleNanos;

	// Allocation rate tracking via heap deltas
	private long lastHeapUsedBytes;
	private long lastAllocCheckNanos;
	private volatile long allocationRateBytesPerSec;

	private RamDiscipline() {}

	public static RamDiscipline get() {
		return INSTANCE;
	}

	/**
	 * Client-tick entry: proactive GC damping, soft cache maintenance, allocation rate sampling.
	 */
	public void onClientTick() {
		M3Config cfg = M3FrametimeMod.config();
		MemoryPressureProbe probe = MemoryPressureProbe.get();

		// Sampling interval adapts to pressure level
		int sampleMask = pressureLevel == PressureLevel.PRESSURE ? 3
			: pressureLevel == PressureLevel.CAUTION ? 5 : 7;

		boolean didSample = cfg.memoryPolicyEnabled && (tickCounter & sampleMask) == 0;
		if (didSample) {
			probe.sampleHeap();
			probe.requestSample();
			sampleAllocationRate();
		}

		long heapUsed = probe.heapUsedMb();
		int tick = ++tickCounter;

		// Use the actual heap ceiling for pressure detection
		long heapMax = probe.heapMaxMb();
		boolean physicalPressure = probe.physicalUnderPressure();
		boolean heapPressure = pressureMode
			? probe.heapUsedMb() >= Math.round(heapMax * cfg.heapPressureRecoverRatio)
			: probe.heapUnderPressure();
		boolean allocPressure = allocationSuggestsPressure(heapUsed, heapMax);
		long policySample = probe.sampleTimestampNanos();
		if (policySample != lastPolicySampleNanos && policySample > 0L) {
			lastPolicySampleNanos = policySample;
			updatePressureMode(cfg, probe, physicalPressure, heapPressure, allocPressure);
		} else if (didSample && allocPressure) {
			updatePressureMode(cfg, probe, physicalPressure, heapPressure, true);
		}

		updatePressureLevel(heapUsed, heapMax);

		// Soft cache hint frequency depends on pressure level.
		// Even under pressureMode, never hint every tick — that hitches the render thread.
		int hintInterval = switch (pressureLevel) {
			case PRESSURE -> Math.max(1, cfg.softCacheHintIntervalTicks / 4);
			case CAUTION -> Math.max(1, cfg.softCacheHintIntervalTicks / 2);
			case NORMAL -> cfg.softCacheHintIntervalTicks;
		};

		boolean hintNow = cfg.softCacheHints && hintInterval > 0 && (tick % hintInterval) == 0;
		if (hintNow) {
			softCacheHint(pressureLevel == PressureLevel.PRESSURE || pressureMode);
		}
	}

	/**
	 * Sample allocation rate by diffing heap usage over time.
	 * Zero-allocation — only primitive deltas.
	 */
	private void sampleAllocationRate() {
		Runtime rt = Runtime.getRuntime();
		long usedNow = rt.totalMemory() - rt.freeMemory();
		long now = System.nanoTime();

		if (lastAllocCheckNanos > 0 && lastHeapUsedBytes > 0) {
			long elapsed = now - lastAllocCheckNanos;
			if (elapsed > 0) {
				long allocated = usedNow - lastHeapUsedBytes;
				// Only track positive allocation (GC can make it negative)
				if (allocated > 0) {
					allocationRateBytesPerSec = (long) ((double) allocated / elapsed * 1_000_000_000.0);
				} else {
					allocationRateBytesPerSec = 0;
				}
			}
		}
		lastHeapUsedBytes = usedNow;
		lastAllocCheckNanos = now;
	}

	private void updatePressureLevel(long heapUsed, long heapMax) {
		long rate = allocationRateBytesPerSec;
		double ratio = heapMax > 0 && heapUsed >= 0 ? (double) heapUsed / heapMax : 0.0;
		boolean veryHighAlloc = rate >= ALLOC_PRESSURE_BPS;
		boolean highAlloc = rate >= ALLOC_CAUTION_BPS;
		if (ratio >= 0.85 || veryHighAlloc || (ratio >= 0.70 && rate >= 96L * 1024L * 1024L)) {
			pressureLevel = PressureLevel.PRESSURE;
		} else if (ratio >= 0.75 || highAlloc || (ratio >= 0.60 && rate >= ALLOC_CAUTION_BPS / 2L)) {
			pressureLevel = PressureLevel.CAUTION;
		} else {
			pressureLevel = PressureLevel.NORMAL;
		}
	}

	private boolean allocationSuggestsPressure(long heapUsed, long heapMax) {
		long rate = allocationRateBytesPerSec;
		if (rate >= ALLOC_PRESSURE_BPS) {
			return true;
		}
		if (heapMax <= 0L) {
			return rate >= ALLOC_CAUTION_BPS;
		}
		double ratio = (double) heapUsed / heapMax;
		return rate >= ALLOC_CAUTION_BPS && ratio >= 0.70;
	}

	/**
	 * Clear thread-local scratch pools and request particle queue trim.
	 * Explicitly avoids calling the JVM collector.
	 */
	public void softCacheHint(boolean aggressive) {
		ScratchPool.releaseAllEphemeral();
		ParticleTrim.requestTrim(aggressive);
	}

	public boolean pressureMode() { return pressureMode; }
	public PressureLevel pressureLevel() { return pressureLevel; }

	/** Raw allocation rate in bytes/s. Useful for diagnostics. */
	public long allocationRateBytesPerSec() {
		return allocationRateBytesPerSec;
	}

	/** Allocation rate in MB/s. Useful for diagnostics. */
	public double allocationRateMbPerSec() {
		return allocationRateBytesPerSec / (1024.0 * 1024.0);
	}

	/**
	 * Dashboard/HUD warning: 8 GB UMA with a 4G-style heap, or heap taking half of physical RAM.
	 * Never changes JVM arguments.
	 */
	public boolean heapVsPhysicalUnhealthy() {
		return RamClassPolicy.heapVsPhysicalUnhealthy();
	}

	private void updatePressureMode(
		M3Config cfg,
		MemoryPressureProbe probe,
		boolean physical,
		boolean heap,
		boolean alloc
	) {
		if (!cfg.memoryPolicyEnabled) {
			pressureMode = false;
			enterSamples = 0;
			recoverSamples = 0;
			return;
		}
		boolean candidate = heap || alloc || (probe.isFresh() && physical);
		if (!pressureMode) {
			recoverSamples = 0;
			if (candidate && ++enterSamples >= cfg.memoryPressureEnterSamples) {
				pressureMode = true;
				enterSamples = 0;
			} else if (!candidate) {
				enterSamples = 0;
			}
		} else {
			boolean recovered = !heap && !alloc && (!probe.isFresh() || probe.freePhysicalMb() >= Math.round(cfg.memoryPressureRecoverMbThreshold));
			if (recovered && ++recoverSamples >= cfg.memoryPressureRecoverSamples) {
				pressureMode = false;
				recoverSamples = 0;
			} else if (!recovered) {
				recoverSamples = 0;
			}
		}
	}

}
