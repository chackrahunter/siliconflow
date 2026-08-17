package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.pool.ScratchPool;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proactive memory discipline for Apple Silicon M3 (8 GB Unified RAM).
 * Damps allocation velocity before ZGC trigger threshold (preventing GC-001 / GC-002 pauses).
 * 100% Zero emergency downgrades: user owns all Video and Render Distance settings.
 */
public final class RamDiscipline {
	private static final RamDiscipline INSTANCE = new RamDiscipline();

	private final AtomicBoolean trimParticles = new AtomicBoolean(false);
	private final AtomicInteger tickCounter = new AtomicInteger();
	private final AtomicInteger enterSamples = new AtomicInteger();
	private final AtomicInteger recoverSamples = new AtomicInteger();
	private final AtomicBoolean pressureMode = new AtomicBoolean(false);
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
		if (cfg.memoryPolicyEnabled && (tickCounter.get() & 7) == 0) {
			probe.sampleHeap();
			probe.requestSample();
		}

		long heapUsed = probe.heapUsedMb();
		int tick = tickCounter.incrementAndGet();

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
			|| pressureMode.get();

		if (hintNow) {
			softCacheHint(pressureMode.get());
		}
	}

	/**
	 * Clear thread-local scratch pools and request particle queue trim.
	 * Explicitly avoids calling blocking the JVM collector.
	 */
	public void softCacheHint(boolean aggressive) {
		ScratchPool.releaseAllEphemeral();
		if (aggressive) {
			trimParticles.set(true);
		}
	}

	public boolean consumeParticleTrimRequest() {
		return trimParticles.getAndSet(false);
	}

	public int effectiveMaxParticles() {
		int configured = M3FrametimeMod.config().maxParticles;
		if (!pressureMode.get() || configured <= 0) return configured;
		return Math.max(16, configured / (MemoryPressureProbe.get().heapUnderPressure() ? 2 : 1));
	}

	public boolean pressureMode() { return pressureMode.get(); }

	private void updatePressureMode(M3Config cfg, MemoryPressureProbe probe, boolean physical, boolean heap) {
		if (!cfg.memoryPolicyEnabled) {
			pressureMode.set(false);
			enterSamples.set(0);
			recoverSamples.set(0);
			return;
		}
		boolean candidate = heap || (probe.isFresh() && physical);
		if (!pressureMode.get()) {
			recoverSamples.set(0);
			if (candidate && enterSamples.incrementAndGet() >= cfg.memoryPressureEnterSamples) {
				pressureMode.set(true);
				enterSamples.set(0);
			}
		} else {
			boolean recovered = !heap && (!probe.isFresh() || probe.freePhysicalMb() >= Math.round(cfg.memoryPressureRecoverMbThreshold));
			if (recovered && recoverSamples.incrementAndGet() >= cfg.memoryPressureRecoverSamples) {
				pressureMode.set(false);
				recoverSamples.set(0);
			} else if (!recovered) {
				recoverSamples.set(0);
			}
		}
	}

	public double effectiveParticleCullDistance() {
		return M3FrametimeMod.config().particleCullDistance;
	}

	public boolean skipClouds() {
		return M3FrametimeMod.config().skipClouds;
	}

	public boolean skipWeatherParticles() {
		return M3FrametimeMod.config().skipWeatherParticles;
	}

	public boolean skipWeatherGeometry() {
		return M3FrametimeMod.config().skipWeatherGeometry;
	}

	public boolean skipItemGlint() {
		return M3FrametimeMod.config().skipItemGlint;
	}

	public boolean skipStars() {
		return M3FrametimeMod.config().skipStars;
	}

	public boolean skipBeaconBeams() {
		return M3FrametimeMod.config().skipBeaconBeams;
	}

	public boolean skipWorldBorder() {
		return M3FrametimeMod.config().skipWorldBorder;
	}

	public boolean skipVignette() {
		return M3FrametimeMod.config().skipVignette;
	}

	public boolean skipNauseaOverlay() {
		return M3FrametimeMod.config().skipNauseaOverlay;
	}

	public boolean skipScoreboard() {
		return M3FrametimeMod.config().skipScoreboard;
	}

	public boolean skipBossBar() {
		return M3FrametimeMod.config().skipBossBar;
	}

	public boolean skipPortalOverlay() {
		return M3FrametimeMod.config().skipPortalOverlay;
	}

	public boolean skipStatusEffectOverlay() {
		return M3FrametimeMod.config().skipStatusEffectOverlay;
	}

	public boolean skipFloatingItem() {
		return M3FrametimeMod.config().skipFloatingItem;
	}

	public boolean skipSubtitles() {
		return M3FrametimeMod.config().skipSubtitles;
	}

	public boolean skipDemoOverlay() {
		return M3FrametimeMod.config().skipDemoOverlay;
	}

	public boolean skipLeashes() {
		return M3FrametimeMod.config().skipLeashes;
	}

	public boolean skipBobView() {
		return M3FrametimeMod.config().skipBobView;
	}

	public boolean skipHurtTilt() {
		return M3FrametimeMod.config().skipHurtTilt;
	}

	public boolean skipUnderwaterOverlay() {
		return M3FrametimeMod.config().skipUnderwaterOverlay;
	}

	public double effectiveEntityCullDistance() {
		return M3FrametimeMod.config().entityCullDistance;
	}

	public double effectiveBlockEntityCullDistance() {
		return M3FrametimeMod.config().blockEntityCullDistance;
	}

	public double effectiveFarItemEntityDistance() {
		return M3FrametimeMod.config().farItemEntityDistance;
	}

	public double effectiveFarExperienceOrbDistance() {
		return M3FrametimeMod.config().farExperienceOrbDistance;
	}
}
