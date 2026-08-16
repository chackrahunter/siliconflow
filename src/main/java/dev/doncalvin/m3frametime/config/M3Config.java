package dev.doncalvin.m3frametime.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ultra-performance configuration for 8 GB Apple Silicon M3.
 * Full user control over Render Distance and Video Settings — zero emergency downgrades.
 * PLAYABLE = pristine visuals + max FPS; MAX = strip visuals; BALANCED = milder.
 */
public final class M3Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** PLAYABLE = default pretty+fun; MAX = strip visuals; BALANCED = milder; TELEMETRY = measure only. */
	public String performanceProfile = "PLAYABLE";

	/** Diagnostic only — sleeps/busy-waits after frames. NEVER on by default. */
	public boolean pacingEnabled = false;
	public double pacingEmaAlpha = 0.12;
	/** Spike threshold in ms. Default 35ms (>28 FPS frame duration). */
	public long spikeThresholdMs = 35;
	/** In-Game F8 Debug Overlay. */
	public boolean overlayEnabled = true;
	/** Console logging for spikes (disabled by default to prevent disk I/O hitching). */
	public boolean spikeLogging = false;
	/** Show last micro-stutter + diagnostics on vanilla F3 left panel. */
	public boolean f3StutterInfo = true;

	public boolean retinaGuard = false;
	public boolean preferIntegerScale = true;
	/**
	 * GLFW swap interval: -1 leave alone, 0 = uncapped (full M-chip FPS), 1 = VSync.
	 * Defaults to 0 so the GPU is not wait-idle on broken macOS VSync.
	 */
	public int swapInterval = 0;
	/** Raise Minecraft render-thread priority and native Darwin QoS so P-cores prioritize the game. */
	public boolean boostRenderThreadPriority = true;
	public boolean boostDarwinQos = true;

	/**
	 * Soft-boost Sodium chunk_builder_threads to an M-chip-friendly count (cores−1)
	 * via reflection / sodium-options.json — no hard Sodium compile dependency.
	 */
	public boolean boostSodiumChunkBuilderThreads = true;
	/**
	 * Explicit Sodium worker count. 0 = auto (availableProcessors − 1).
	 */
	public int sodiumChunkBuilderThreads = 0;
	/** Nudge Sodium "Chunk Render Task Executor" threads to NORM+1 and Darwin USER_INITIATED. */
	public boolean boostSodiumWorkerPriority = true;

	/** Iris & Shader Shadow-Pass Ultra-Culling (particles, far signs/banners, items in shadow maps). */
	public boolean optimizeShadowPass = true;
	public double shadowEntityDistance = 32.0;

	public boolean entityCull = true;
	public double entityCullDistance = 80.0;
	/**
	 * When Sodium is loaded: also apply cheap AABB frustum reject in shouldRender.
	 */
	public boolean overrideSodiumEntityCull = true;
	public boolean entityShadowSkip = true;
	/** Skip non-player nametag / label draws. */
	public boolean skipEntityNametags = false;

	public boolean particleCull = true;
	public double particleCullDistance = 40.0;
	/** Hard cap — tighter than Sodium/IF particle render opts. */
	public int maxParticles = 192;

	public boolean blockEntityCull = true;
	public double blockEntityCullDistance = 48.0;

	/** Skip text glyph rendering on far signs. */
	public boolean skipFarSignText = true;
	public double farSignDistance = 24.0;

	/** Skip multi-layer patterns on far banners. */
	public boolean skipFarBannerPatterns = true;
	public double farBannerDistance = 32.0;

	public boolean skipClouds = false;
	public boolean skipWeatherParticles = false;
	/** Rain/snow strip geometry (not just splash particles). */
	public boolean skipWeatherGeometry = false;
	public boolean skipToasts = false;
	public boolean skipStars = false;
	public boolean skipWorldBorder = true;
	public boolean skipBeaconBeams = false;
	public boolean skipVignette = false;
	public boolean skipNauseaOverlay = false;
	public boolean skipScoreboard = false;
	public boolean skipBossBar = false;
	public boolean skipUnderwaterOverlay = false;
	/** Fire first-person overlay — off by default (gameplay cue). */
	public boolean skipFireOverlay = false;
	public boolean skipBobView = false;
	public boolean skipHurtTilt = false;
	/** Nether/end portal fullscreen purple/warp fill. */
	public boolean skipPortalOverlay = false;
	/** Status-effect icon strip. */
	public boolean skipStatusEffectOverlay = false;
	/** Totem / floating-item fullscreen pop. */
	public boolean skipFloatingItem = false;
	/** Accessibility subtitles HUD list. */
	public boolean skipSubtitles = false;
	public boolean skipDemoOverlay = true;
	/** Soft-skip entity leash ribbon geometry (EntityRenderer.renderLeash). */
	public boolean skipLeashes = false;
	/** Far dropped-item updateRenderState throttle (spin/bob work). */
	public boolean farItemEntityThrottle = true;
	public double farItemEntityDistance = 40.0;
	/** Far XP-orb render/update throttle. */
	public boolean farExperienceOrbThrottle = true;
	public double farExperienceOrbDistance = 36.0;

	/** Skip enchantment foil multipass. */
	public boolean skipItemGlint = false;
	/** Rebuild lightmap every other tick when lighting is stable. */
	public boolean lightmapThrottle = true;
	/** Client-only: skip potion swirl particle updates for far mobs. */
	public boolean farPotionSwirlSkip = true;
	public double farPotionSwirlDistance = 56.0;

	public boolean forceFastGraphics = false;
	public double entityDistanceScaling = 1.0;

	public boolean soundPool = true;
	public int workerThreads = 0;
	public double memoryPressureFreeMbThreshold = 128.0;

	/** Periodic soft cache hints: clear ScratchPool, request particle queue trim. Never calls System.gc(). */
	public boolean softCacheHints = true;
	/** Client ticks between soft hints (~20 ticks/s). */
	public int softCacheHintIntervalTicks = 40;

	/** Client-only: throttle LivingEntity.updateLimbs for far mobs. */
	public boolean farLimbThrottle = true;
	public double farLimbDistance = 72.0;
	/** Bitmask: keep limbs when (age & mask) == 0; 3 ⇒ every 4th tick. */
	public int farLimbTickMask = 3;

	/** Client-only: skip far ambient particle spawns before Particle alloc. */
	public boolean farParticleSpawnSkip = true;
	/** Client-only: throttle ClientWorld.doRandomBlockDisplayTicks (1/4 keep). */
	public boolean ambientParticleThrottle = true;

	/** Client-only: skip far positional sounds (keeps music/UI/voice/players). */
	public boolean farSoundSkip = true;
	public double farSoundDistance = 56.0;

	/** Fast ARM64 math tables for sin/cos/distance. */
	public boolean useFastMath = true;

	public static M3Config defaults() {
		M3Config c = new M3Config();
		c.applyProfile();
		c.pacingEnabled = false;
		return c;
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("m3-frametime.json");
	}

	public void applyProfile() {
		String p = performanceProfile == null ? "PLAYABLE" : performanceProfile.trim().toUpperCase();
		performanceProfile = p;
		pacingEnabled = false;
		retinaGuard = false;
		boostRenderThreadPriority = true;
		boostDarwinQos = true;
		useFastMath = true;
		optimizeShadowPass = true;
		overlayEnabled = true;
		spikeLogging = false;
		spikeThresholdMs = 35;
		memoryPressureFreeMbThreshold = 128.0;
		switch (p) {
			case "TELEMETRY" -> {
				swapInterval = -1;
				boostRenderThreadPriority = false;
				boostDarwinQos = false;
				boostSodiumChunkBuilderThreads = false;
				boostSodiumWorkerPriority = false;
				entityCull = false;
				overrideSodiumEntityCull = false;
				particleCull = false;
				blockEntityCull = false;
				skipFarSignText = false;
				skipFarBannerPatterns = false;
				entityShadowSkip = false;
				skipEntityNametags = false;
				skipClouds = false;
				skipWeatherParticles = false;
				skipWeatherGeometry = false;
				skipToasts = false;
				skipStars = false;
				skipWorldBorder = false;
				skipBeaconBeams = false;
				skipVignette = false;
				skipNauseaOverlay = false;
				skipScoreboard = false;
				skipBossBar = false;
				skipUnderwaterOverlay = false;
				skipFireOverlay = false;
				skipBobView = false;
				skipHurtTilt = false;
				skipPortalOverlay = false;
				skipStatusEffectOverlay = false;
				skipFloatingItem = false;
				skipSubtitles = false;
				skipDemoOverlay = false;
				skipLeashes = false;
				farItemEntityThrottle = false;
				farExperienceOrbThrottle = false;
				skipItemGlint = false;
				lightmapThrottle = false;
				farPotionSwirlSkip = false;
				forceFastGraphics = false;
				maxParticles = 16384;
				farLimbThrottle = false;
				farParticleSpawnSkip = false;
				ambientParticleThrottle = false;
				farSoundSkip = false;
				softCacheHints = false;
				optimizeShadowPass = false;
			}
			case "BALANCED" -> {
				swapInterval = -1;
				boostRenderThreadPriority = true;
				boostDarwinQos = true;
				boostSodiumChunkBuilderThreads = true;
				boostSodiumWorkerPriority = true;
				entityCull = true;
				entityCullDistance = 96.0;
				overrideSodiumEntityCull = false;
				skipEntityNametags = false;
				particleCull = true;
				particleCullDistance = 48.0;
				maxParticles = 128;
				blockEntityCull = true;
				blockEntityCullDistance = 56.0;
				skipFarSignText = true;
				farSignDistance = 32.0;
				skipFarBannerPatterns = true;
				farBannerDistance = 40.0;
				entityShadowSkip = true;
				skipClouds = false;
				skipWeatherParticles = false;
				skipWeatherGeometry = false;
				skipToasts = false;
				skipStars = false;
				skipWorldBorder = true;
				skipBeaconBeams = false;
				skipVignette = false;
				skipNauseaOverlay = false;
				skipScoreboard = false;
				skipBossBar = false;
				skipUnderwaterOverlay = false;
				skipFireOverlay = false;
				skipBobView = false;
				skipHurtTilt = false;
				skipPortalOverlay = false;
				skipStatusEffectOverlay = false;
				skipFloatingItem = false;
				skipSubtitles = false;
				skipDemoOverlay = true;
				skipLeashes = false;
				farItemEntityThrottle = true;
				farItemEntityDistance = 40.0;
				farExperienceOrbThrottle = true;
				farExperienceOrbDistance = 36.0;
				skipItemGlint = false;
				lightmapThrottle = true;
				farPotionSwirlSkip = true;
				farPotionSwirlDistance = 56.0;
				forceFastGraphics = false;
				entityDistanceScaling = 1.0;
				farLimbThrottle = true;
				farLimbDistance = 80.0;
				farLimbTickMask = 1;
				farParticleSpawnSkip = true;
				ambientParticleThrottle = true;
				farSoundSkip = true;
				farSoundDistance = 64.0;
				memoryPressureFreeMbThreshold = 128.0;
				softCacheHints = true;
				softCacheHintIntervalTicks = 60;
				optimizeShadowPass = true;
				shadowEntityDistance = 40.0;
			}
			case "MAX" -> {
				performanceProfile = "MAX";
				swapInterval = 0;
				boostRenderThreadPriority = true;
				boostDarwinQos = true;
				boostSodiumChunkBuilderThreads = true;
				sodiumChunkBuilderThreads = 0;
				boostSodiumWorkerPriority = true;
				entityCull = true;
				entityCullDistance = 64.0;
				overrideSodiumEntityCull = true;
				skipEntityNametags = true;
				particleCull = true;
				particleCullDistance = 32.0;
				maxParticles = 128;
				blockEntityCull = true;
				blockEntityCullDistance = 40.0;
				skipFarSignText = true;
				farSignDistance = 20.0;
				skipFarBannerPatterns = true;
				farBannerDistance = 24.0;
				entityShadowSkip = true;
				skipClouds = true;
				skipWeatherParticles = true;
				skipWeatherGeometry = true;
				skipToasts = true;
				skipStars = true;
				skipWorldBorder = true;
				skipBeaconBeams = true;
				skipVignette = true;
				skipNauseaOverlay = true;
				skipScoreboard = true;
				skipBossBar = false;
				skipUnderwaterOverlay = true;
				skipFireOverlay = false;
				skipBobView = true;
				skipHurtTilt = true;
				skipPortalOverlay = true;
				skipStatusEffectOverlay = true;
				skipFloatingItem = true;
				skipSubtitles = true;
				skipDemoOverlay = true;
				skipLeashes = true;
				farItemEntityThrottle = true;
				farItemEntityDistance = 28.0;
				farExperienceOrbThrottle = true;
				farExperienceOrbDistance = 24.0;
				skipItemGlint = true;
				lightmapThrottle = true;
				farPotionSwirlSkip = true;
				farPotionSwirlDistance = 36.0;
				forceFastGraphics = true;
				entityDistanceScaling = 0.5;
				farLimbThrottle = true;
				farLimbDistance = 48.0;
				farLimbTickMask = 3;
				farParticleSpawnSkip = true;
				ambientParticleThrottle = true;
				farSoundSkip = true;
				farSoundDistance = 36.0;
				memoryPressureFreeMbThreshold = 128.0;
				softCacheHints = true;
				softCacheHintIntervalTicks = 40;
				optimizeShadowPass = true;
				shadowEntityDistance = 28.0;
			}
			default -> {
				// PLAYABLE — default: looks pristine, RD 100% user-owned, ultra-optimized for 100+ FPS.
				performanceProfile = "PLAYABLE";
				swapInterval = 0;
				boostRenderThreadPriority = true;
				boostDarwinQos = true;
				boostSodiumChunkBuilderThreads = true;
				sodiumChunkBuilderThreads = 0;
				boostSodiumWorkerPriority = true;
				entityCull = true;
				entityCullDistance = 80.0;
				overrideSodiumEntityCull = true;
				skipEntityNametags = false;
				particleCull = true;
				particleCullDistance = 40.0;
				maxParticles = 192;
				blockEntityCull = true;
				blockEntityCullDistance = 48.0;
				skipFarSignText = true;
				farSignDistance = 24.0;
				skipFarBannerPatterns = true;
				farBannerDistance = 32.0;
				entityShadowSkip = true;
				skipClouds = false;
				skipWeatherParticles = false;
				skipWeatherGeometry = false;
				skipToasts = false;
				skipStars = false;
				skipWorldBorder = true;
				skipBeaconBeams = false;
				skipVignette = false;
				skipNauseaOverlay = false;
				skipScoreboard = false;
				skipBossBar = false;
				skipUnderwaterOverlay = false;
				skipFireOverlay = false;
				skipBobView = false;
				skipHurtTilt = false;
				skipPortalOverlay = false;
				skipStatusEffectOverlay = false;
				skipFloatingItem = false;
				skipSubtitles = false;
				skipDemoOverlay = true;
				skipLeashes = false;
				farItemEntityThrottle = true;
				farItemEntityDistance = 40.0;
				farExperienceOrbThrottle = true;
				farExperienceOrbDistance = 36.0;
				skipItemGlint = false;
				lightmapThrottle = true;
				farPotionSwirlSkip = true;
				farPotionSwirlDistance = 56.0;
				forceFastGraphics = false;
				entityDistanceScaling = 1.0;
				farLimbThrottle = true;
				farLimbDistance = 72.0;
				farLimbTickMask = 3;
				farParticleSpawnSkip = true;
				ambientParticleThrottle = true;
				farSoundSkip = true;
				farSoundDistance = 56.0;
				memoryPressureFreeMbThreshold = 128.0;
				softCacheHints = true;
				softCacheHintIntervalTicks = 40;
				optimizeShadowPass = true;
				shadowEntityDistance = 32.0;
			}
		}
	}

	public static M3Config load() {
		Path file = path();
		if (!Files.isRegularFile(file)) {
			M3Config fresh = defaults();
			fresh.save();
			return fresh;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			M3Config loaded = GSON.fromJson(reader, M3Config.class);
			if (loaded == null) {
				return defaults();
			}
			if (loaded.performanceProfile == null || loaded.performanceProfile.isBlank()) {
				loaded.performanceProfile = "PLAYABLE";
			}
			String profile = loaded.performanceProfile.trim().toUpperCase();
			if (!"TELEMETRY".equals(profile)
				&& !"BALANCED".equals(profile)
				&& !"MAX".equals(profile)
				&& !"PLAYABLE".equals(profile)) {
				loaded.performanceProfile = "PLAYABLE";
				profile = "PLAYABLE";
			}
			if ("MAX".equals(profile) || "PLAYABLE".equals(profile)) {
				loaded.pacingEnabled = false;
				loaded.retinaGuard = false;
				loaded.boostRenderThreadPriority = true;
				loaded.boostDarwinQos = true;
				loaded.boostSodiumChunkBuilderThreads = true;
				loaded.boostSodiumWorkerPriority = true;
				loaded.useFastMath = true;
				loaded.optimizeShadowPass = true;
				loaded.overlayEnabled = true;
				if (loaded.swapInterval < 0) {
					loaded.swapInterval = 0;
				}
			}
			return loaded;
		} catch (Exception e) {
			M3FrametimeMod.LOGGER.warn("Failed to load config, using defaults: {}", e.toString());
			return defaults();
		}
	}

	public void save() {
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path())) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			M3FrametimeMod.LOGGER.warn("Failed to save config: {}", e.toString());
		}
	}
}
