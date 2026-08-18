package dev.doncalvin.m3frametime.engine;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.IrisCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import dev.doncalvin.m3frametime.telemetry.SpikeMonitor;
import dev.doncalvin.m3frametime.telemetry.StutterErrorCode;

import java.util.Objects;

/**
 * Session overlay that trims SiliconFlow-owned shader-adjacent budgets when Iris is active
 * and frames or RAM are under stress. Never mutates the Iris pack, Sodium allocator,
 * render distance, or {@code -Xmx}. Never {@code save()}s unless the user clicks a profile.
 */
public final class ShaderAutoThrottle {
	public enum Level {
		NONE,
		L0,
		L1,
		L2
	}

	private static final ShaderAutoThrottle INSTANCE = new ShaderAutoThrottle();
	private static final long RECENT_SPIKE_NS_AGE_MS = 4_000L;
	private static final int P95_MIN_FRAMES = 90;

	private M3Config trackedConfig;
	private String trackedProfile;
	private OverlaySnapshot snapshot;
	private boolean overlayActive;
	private Level level = Level.NONE;
	private int enterSamples;
	private int recoverSamples;
	private Level lastLoggedLevel = Level.NONE;

	private ShaderAutoThrottle() {}

	public static ShaderAutoThrottle get() {
		return INSTANCE;
	}

	public Level level() {
		return level;
	}

	public boolean overlayActive() {
		return overlayActive;
	}

	public String levelLabel() {
		return level.name();
	}

	/**
	 * Drop overlay tracking when the user/profile replaces live config.
	 * Does not restore — {@link M3Config#applyProfile()} already wrote the new values.
	 */
	public void onUserConfigMayHaveChanged(M3Config cfg) {
		if (cfg == null) {
			return;
		}
		String profile = cfg.performanceProfile;
		if (cfg != trackedConfig || !Objects.equals(profile, trackedProfile)) {
			overlayActive = false;
			level = Level.NONE;
			enterSamples = 0;
			recoverSamples = 0;
			trackedConfig = cfg;
			trackedProfile = profile;
			snapshot = OverlaySnapshot.capture(cfg);
		}
	}

	public void onClientTick() {
		M3Config cfg = M3FrametimeMod.config();
		onUserConfigMayHaveChanged(cfg);
		if (snapshot == null) {
			snapshot = OverlaySnapshot.capture(cfg);
		}

		if (!cfg.shaderAutoThrottleEnabled) {
			restoreIfNeeded(cfg);
			return;
		}

		boolean shaders = IrisCompat.isShaderActive();
		if (!shaders) {
			restoreIfNeeded(cfg);
			enterSamples = 0;
			recoverSamples = 0;
			return;
		}

		boolean stressed = isStressed(cfg);
		int enterNeed = Math.max(1, cfg.shaderThrottleEnterSamples);
		int recoverNeed = Math.max(1, cfg.shaderThrottleRecoverSamples);

		if (stressed) {
			recoverSamples = 0;
			if (++enterSamples >= enterNeed) {
				enterSamples = 0;
				escalate(cfg);
			}
		} else {
			enterSamples = 0;
			if (level == Level.NONE) {
				recoverSamples = 0;
				return;
			}
			if (++recoverSamples >= recoverNeed) {
				recoverSamples = 0;
				recover(cfg);
			}
		}
	}

	private boolean isStressed(M3Config cfg) {
		SpikeMonitor spikes = SpikeMonitor.get();
		boolean recentSpike = spikes.hasSpike()
			&& spikes.lastSpikeAgeMs() >= 0L
			&& spikes.lastSpikeAgeMs() < RECENT_SPIKE_NS_AGE_MS;
		boolean highP95 = spikes.frameCount() >= P95_MIN_FRAMES
			&& spikes.percentileMs(0.95) >= cfg.shaderThrottleP95Ms;
		RamDiscipline ram = RamDiscipline.get();
		boolean ramPressure = ram.pressureMode() || ram.pressureLevel() == RamDiscipline.PressureLevel.PRESSURE;
		return recentSpike || highP95 || ramPressure;
	}

	private boolean wantL2() {
		if (RamDiscipline.get().pressureMode()) {
			return true;
		}
		SpikeMonitor spikes = SpikeMonitor.get();
		if (!spikes.hasSpike() || spikes.lastSpikeAgeMs() >= RECENT_SPIKE_NS_AGE_MS) {
			return false;
		}
		StutterErrorCode code = spikes.lastErrorCode();
		return code == StutterErrorCode.GPU_004
			|| code == StutterErrorCode.GPU_001
			|| code == StutterErrorCode.GPU_031
			|| code == StutterErrorCode.LIT_023;
	}

	private void escalate(M3Config cfg) {
		Level next = switch (level) {
			case NONE -> Level.L0;
			case L0 -> wantL2() ? Level.L2 : Level.L1;
			case L1 -> Level.L2;
			case L2 -> Level.L2;
		};
		if (next != level) {
			applyLevel(cfg, next);
		}
	}

	private void recover(M3Config cfg) {
		Level next = switch (level) {
			case L2 -> Level.L1;
			case L1 -> Level.L0;
			case L0, NONE -> Level.NONE;
		};
		applyLevel(cfg, next);
	}

	private void restoreIfNeeded(M3Config cfg) {
		if (!overlayActive && level == Level.NONE) {
			return;
		}
		applyLevel(cfg, Level.NONE);
	}

	private void applyLevel(M3Config cfg, Level next) {
		if (snapshot == null) {
			snapshot = OverlaySnapshot.capture(cfg);
		}
		if (next == Level.NONE) {
			if (overlayActive) {
				snapshot.restore(cfg);
			}
			overlayActive = false;
			level = Level.NONE;
			logLevelChange();
			return;
		}
		if (!overlayActive) {
			snapshot = OverlaySnapshot.capture(cfg);
			overlayActive = true;
		}
		snapshot.restore(cfg);
		applyL0(cfg);
		if (next == Level.L1 || next == Level.L2) {
			applyL1(cfg);
		}
		if (next == Level.L2) {
			applyL2(cfg);
		}
		level = next;
		logLevelChange();
	}

	private void applyL0(M3Config cfg) {
		cfg.skipClouds = true;
		cfg.skipStars = true;
		cfg.skipVignette = true;
		cfg.skipItemGlint = true;
		cfg.skipEntityNametags = true;
		cfg.entityShadowSkip = true;
		cfg.skipToasts = true;
		cfg.skipUnderwaterOverlay = true;
		cfg.skipPortalOverlay = true;
		cfg.skipSubtitles = true;
		cfg.skipLeashes = true;
		cfg.skipNauseaOverlay = true;
		cfg.skipDemoOverlay = true;
	}

	private void applyL1(M3Config cfg) {
		cfg.entityCull = true;
		cfg.particleCull = true;
		cfg.blockEntityCull = true;
		cfg.entityCullDistance = RamClassPolicy.capEntityCull(Math.min(snapshot.entityCullDistance, 64.0));
		cfg.particleCullDistance = RamClassPolicy.capParticleCull(Math.min(snapshot.particleCullDistance, 32.0));
		cfg.blockEntityCullDistance = RamClassPolicy.capBlockEntityCull(Math.min(snapshot.blockEntityCullDistance, 40.0));
		cfg.farItemEntityDistance = Math.min(snapshot.farItemEntityDistance, 24.0);
		cfg.farExperienceOrbDistance = Math.min(snapshot.farExperienceOrbDistance, 20.0);
		cfg.farPotionSwirlDistance = Math.min(snapshot.farPotionSwirlDistance, 32.0);
		cfg.farLimbDistance = Math.min(snapshot.farLimbDistance, 48.0);
	}

	private void applyL2(M3Config cfg) {
		cfg.optimizeShadowPass = true;
		if (snapshot.shadowEntityDistance <= 0.0) {
			cfg.shadowEntityDistance = 32.0;
		} else {
			cfg.shadowEntityDistance = Math.min(snapshot.shadowEntityDistance, 32.0);
		}
		cfg.lightmapThrottle = true;
		cfg.maxParticles = RamClassPolicy.capParticles(Math.min(snapshot.maxParticles, RamClassPolicy.l2ParticleBudget()));
		cfg.skipScoreboard = true;
		cfg.skipStatusEffectOverlay = true;
		cfg.skipFloatingItem = true;
	}

	private void logLevelChange() {
		if (level == lastLoggedLevel) {
			return;
		}
		lastLoggedLevel = level;
		M3FrametimeMod.LOGGER.info("ShaderAutoThrottle: {} (session overlay, not persisted)", level.name());
	}

	private static final class OverlaySnapshot {
		final boolean skipClouds;
		final boolean skipStars;
		final boolean skipVignette;
		final boolean skipItemGlint;
		final boolean skipEntityNametags;
		final boolean entityShadowSkip;
		final boolean skipToasts;
		final boolean skipUnderwaterOverlay;
		final boolean skipPortalOverlay;
		final boolean skipSubtitles;
		final boolean skipLeashes;
		final boolean skipNauseaOverlay;
		final boolean skipDemoOverlay;
		final boolean entityCull;
		final boolean particleCull;
		final boolean blockEntityCull;
		final double entityCullDistance;
		final double particleCullDistance;
		final double blockEntityCullDistance;
		final double farItemEntityDistance;
		final double farExperienceOrbDistance;
		final double farPotionSwirlDistance;
		final double farLimbDistance;
		final boolean optimizeShadowPass;
		final double shadowEntityDistance;
		final boolean lightmapThrottle;
		final int maxParticles;
		final boolean skipScoreboard;
		final boolean skipStatusEffectOverlay;
		final boolean skipFloatingItem;

		private OverlaySnapshot(M3Config cfg) {
			this.skipClouds = cfg.skipClouds;
			this.skipStars = cfg.skipStars;
			this.skipVignette = cfg.skipVignette;
			this.skipItemGlint = cfg.skipItemGlint;
			this.skipEntityNametags = cfg.skipEntityNametags;
			this.entityShadowSkip = cfg.entityShadowSkip;
			this.skipToasts = cfg.skipToasts;
			this.skipUnderwaterOverlay = cfg.skipUnderwaterOverlay;
			this.skipPortalOverlay = cfg.skipPortalOverlay;
			this.skipSubtitles = cfg.skipSubtitles;
			this.skipLeashes = cfg.skipLeashes;
			this.skipNauseaOverlay = cfg.skipNauseaOverlay;
			this.skipDemoOverlay = cfg.skipDemoOverlay;
			this.entityCull = cfg.entityCull;
			this.particleCull = cfg.particleCull;
			this.blockEntityCull = cfg.blockEntityCull;
			this.entityCullDistance = cfg.entityCullDistance;
			this.particleCullDistance = cfg.particleCullDistance;
			this.blockEntityCullDistance = cfg.blockEntityCullDistance;
			this.farItemEntityDistance = cfg.farItemEntityDistance;
			this.farExperienceOrbDistance = cfg.farExperienceOrbDistance;
			this.farPotionSwirlDistance = cfg.farPotionSwirlDistance;
			this.farLimbDistance = cfg.farLimbDistance;
			this.optimizeShadowPass = cfg.optimizeShadowPass;
			this.shadowEntityDistance = cfg.shadowEntityDistance;
			this.lightmapThrottle = cfg.lightmapThrottle;
			this.maxParticles = cfg.maxParticles;
			this.skipScoreboard = cfg.skipScoreboard;
			this.skipStatusEffectOverlay = cfg.skipStatusEffectOverlay;
			this.skipFloatingItem = cfg.skipFloatingItem;
		}

		static OverlaySnapshot capture(M3Config cfg) {
			return new OverlaySnapshot(cfg);
		}

		void restore(M3Config cfg) {
			cfg.skipClouds = skipClouds;
			cfg.skipStars = skipStars;
			cfg.skipVignette = skipVignette;
			cfg.skipItemGlint = skipItemGlint;
			cfg.skipEntityNametags = skipEntityNametags;
			cfg.entityShadowSkip = entityShadowSkip;
			cfg.skipToasts = skipToasts;
			cfg.skipUnderwaterOverlay = skipUnderwaterOverlay;
			cfg.skipPortalOverlay = skipPortalOverlay;
			cfg.skipSubtitles = skipSubtitles;
			cfg.skipLeashes = skipLeashes;
			cfg.skipNauseaOverlay = skipNauseaOverlay;
			cfg.skipDemoOverlay = skipDemoOverlay;
			cfg.entityCull = entityCull;
			cfg.particleCull = particleCull;
			cfg.blockEntityCull = blockEntityCull;
			cfg.entityCullDistance = entityCullDistance;
			cfg.particleCullDistance = particleCullDistance;
			cfg.blockEntityCullDistance = blockEntityCullDistance;
			cfg.farItemEntityDistance = farItemEntityDistance;
			cfg.farExperienceOrbDistance = farExperienceOrbDistance;
			cfg.farPotionSwirlDistance = farPotionSwirlDistance;
			cfg.farLimbDistance = farLimbDistance;
			cfg.optimizeShadowPass = optimizeShadowPass;
			cfg.shadowEntityDistance = shadowEntityDistance;
			cfg.lightmapThrottle = lightmapThrottle;
			cfg.maxParticles = maxParticles;
			cfg.skipScoreboard = skipScoreboard;
			cfg.skipStatusEffectOverlay = skipStatusEffectOverlay;
			cfg.skipFloatingItem = skipFloatingItem;
		}
	}
}
