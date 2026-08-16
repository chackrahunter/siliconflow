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
		probe.sampleHeap();
		probe.requestSample();

		long heapUsed = probe.heapUsedMb();
		int tick = tickCounter.incrementAndGet();

		// Proactive damping: if heap usage approaches 1400MB, soft-trim ephemeral pools
		boolean highHeap = heapUsed > 1400L;
		boolean hintNow = (cfg.softCacheHints && cfg.softCacheHintIntervalTicks > 0 && (tick % cfg.softCacheHintIntervalTicks) == 0)
			|| highHeap;

		if (hintNow) {
			softCacheHint(highHeap);
		}
	}

	/**
	 * Clear thread-local scratch pools and request particle queue trim.
	 * Explicitly avoids calling blocking System.gc().
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
		return M3FrametimeMod.config().maxParticles;
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
