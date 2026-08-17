package dev.doncalvin.m3frametime.config;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.telemetry.SpikeMonitor;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;

/**
 * Per-frame config snapshot cache. Eliminates hundreds of volatile reads per frame
 * by copying all hot-path config fields into plain fields, refreshed once per frame.
 * Access all fields directly (no getters) — intended for mixin hot paths.
 */
public final class FrameConfigCache {
	private static final FrameConfigCache INSTANCE = new FrameConfigCache();
	private long lastRefreshFrame = -1;

	public boolean skipClouds;
	public boolean skipWeatherParticles;
	public boolean skipWeatherGeometry;
	public boolean skipStars;
	public boolean skipBeaconBeams;
	public boolean skipWorldBorder;
	public boolean skipVignette;
	public boolean skipNauseaOverlay;
	public boolean skipScoreboard;
	public boolean skipBossBar;
	public boolean skipPortalOverlay;
	public boolean skipStatusEffectOverlay;
	public boolean skipFloatingItem;
	public boolean skipSubtitles;
	public boolean skipDemoOverlay;
	public boolean skipLeashes;
	public boolean skipBobView;
	public boolean skipHurtTilt;
	public boolean skipUnderwaterOverlay;
	public boolean skipItemGlint;
	public boolean skipToasts;
	public boolean skipFireOverlay;
	public boolean entityShadowSkip;
	public boolean skipEntityNametags;

	public boolean entityCull;
	public double entityCullDistance;
	public double shadowEntityDistance;
	public boolean optimizeShadowPass;
	public boolean overrideSodiumEntityCull;
	public boolean useAggressiveEntityFrustum;

	public double farItemEntityDistance;
	public double farExperienceOrbDistance;
	public boolean farSoundSkip;
	public double farSoundDistance;

	public boolean particleCull;
	public double particleCullDistance;
	public int maxParticles;
	public boolean farParticleSpawnSkip;
	public boolean lightmapThrottle;

	public String performanceProfile;

	private FrameConfigCache() {}

	public static FrameConfigCache get() {
		return INSTANCE;
	}

	public void refresh() {
		long currentFrame = SpikeMonitor.get().frameCount();
		if (currentFrame == lastRefreshFrame) return;
		lastRefreshFrame = currentFrame;

		var cfg = M3FrametimeMod.config();
		this.skipClouds = cfg.skipClouds;
		this.skipWeatherParticles = cfg.skipWeatherParticles;
		this.skipWeatherGeometry = cfg.skipWeatherGeometry;
		this.skipStars = cfg.skipStars;
		this.skipBeaconBeams = cfg.skipBeaconBeams;
		this.skipWorldBorder = cfg.skipWorldBorder;
		this.skipVignette = cfg.skipVignette;
		this.skipNauseaOverlay = cfg.skipNauseaOverlay;
		this.skipScoreboard = cfg.skipScoreboard;
		this.skipBossBar = cfg.skipBossBar;
		this.skipPortalOverlay = cfg.skipPortalOverlay;
		this.skipStatusEffectOverlay = cfg.skipStatusEffectOverlay;
		this.skipFloatingItem = cfg.skipFloatingItem;
		this.skipSubtitles = cfg.skipSubtitles;
		this.skipDemoOverlay = cfg.skipDemoOverlay;
		this.skipLeashes = cfg.skipLeashes;
		this.skipBobView = cfg.skipBobView;
		this.skipHurtTilt = cfg.skipHurtTilt;
		this.skipUnderwaterOverlay = cfg.skipUnderwaterOverlay;
		this.skipItemGlint = cfg.skipItemGlint;
		this.skipToasts = cfg.skipToasts;
		this.skipFireOverlay = cfg.skipFireOverlay;
		this.entityShadowSkip = cfg.entityShadowSkip;
		this.skipEntityNametags = cfg.skipEntityNametags;

		this.entityCull = cfg.entityCull;
		this.entityCullDistance = cfg.entityCullDistance;
		this.shadowEntityDistance = cfg.shadowEntityDistance;
		this.optimizeShadowPass = cfg.optimizeShadowPass;
		this.overrideSodiumEntityCull = cfg.overrideSodiumEntityCull;
		this.useAggressiveEntityFrustum = StackCompat.useAggressiveEntityFrustum();

		this.farItemEntityDistance = cfg.farItemEntityDistance;
		this.farExperienceOrbDistance = cfg.farExperienceOrbDistance;
		this.farSoundSkip = cfg.farSoundSkip;
		this.farSoundDistance = cfg.farSoundDistance;

		this.particleCull = cfg.particleCull;
		this.particleCullDistance = cfg.particleCullDistance;
		this.maxParticles = RamDiscipline.get().effectiveMaxParticles();
		this.farParticleSpawnSkip = cfg.farParticleSpawnSkip;
		this.lightmapThrottle = cfg.lightmapThrottle;

		this.performanceProfile = cfg.performanceProfile;
	}
}
