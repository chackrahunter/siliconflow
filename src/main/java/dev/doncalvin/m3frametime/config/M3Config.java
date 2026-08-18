package dev.doncalvin.m3frametime.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.engine.RamClassPolicy;
import dev.doncalvin.m3frametime.engine.SiliconChipInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
	/** Bounded in-memory diagnostics recorder; no disk writes unless spikeLogging is enabled. */
	public boolean performanceRecorderEnabled = false;
	/** Number of measured frame samples retained by the recorder. */
	public int performanceRecorderWindow = 240;
	/** Show last micro-stutter + diagnostics on vanilla F3 left panel. */
	public boolean f3StutterInfo = true;

	public boolean retinaGuard = false;
	public boolean preferIntegerScale = true;
	/**
	 * GLFW swap interval: -1 leave alone, 0 = uncapped (full M-chip FPS), 1 = VSync.
	 * Defaults to -1 so Minecraft/Iris retain ownership of VSync and pacing.
	 */
	public int swapInterval = -1;

	/** When true, entity distance cull also applies during Iris shadow passes (SHADER/BALANCED set this). */
	public boolean optimizeShadowPass = false;
	public double shadowEntityDistance = 0.0;

	public boolean entityCull = true;
	public double entityCullDistance = 80.0;
	/**
	 * When Sodium is loaded: also apply cheap AABB frustum reject in shouldRender.
	 */
	public boolean overrideSodiumEntityCull = false;
	public boolean entityShadowSkip = false;
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
	public double memoryPressureFreeMbThreshold = 384.0;
	/** Recovery threshold must be higher than the enter threshold to prevent oscillation. */
	public double memoryPressureRecoverMbThreshold = 768.0;
	/** Enable asynchronous heap/physical-memory sampling for mod-owned pressure policy. */
	public boolean memoryPolicyEnabled = true;
	/** Number of consecutive fresh samples required before entering pressure mode. */
	public int memoryPressureEnterSamples = 3;
	/** Number of consecutive recovered samples required before leaving pressure mode. */
	public int memoryPressureRecoverSamples = 20;
	/** Heap occupancy ratio that enters pressure mode; never changes -Xmx. */
	public double heapPressureEnterRatio = 0.90;
	/** Heap occupancy ratio that clears heap pressure. */
	public double heapPressureRecoverRatio = 0.80;

	/** Periodic soft cache hints: clear ScratchPool, request particle queue trim. Never calls the JVM collector. */
	public boolean softCacheHints = true;
	/** Client ticks between soft hints (~20 ticks/s). */
	public int softCacheHintIntervalTicks = 40;

	/** Session shader auto-throttle: overlay SiliconFlow budgets when Iris is active and frames/RAM stress. Not persisted. */
	public boolean shaderAutoThrottleEnabled = true;
	/** Consecutive stressed ticks before escalating a throttle level. */
	public int shaderThrottleEnterSamples = 3;
	/** Consecutive healthy ticks before dropping one throttle level. */
	public int shaderThrottleRecoverSamples = 20;
	/** p95 frame-time (ms) that counts as shader stress once enough frames exist. */
	public double shaderThrottleP95Ms = 28.0;

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

	public static M3Config defaults() {
		M3Config c = new M3Config();
		c.applyProfile(); // applyProfile() already applies chip scaling at its tail
		return c;
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("m3-frametime.json");
	}

	public void applyProfile() {
		String p = performanceProfile == null ? "PLAYABLE" : performanceProfile.trim().toUpperCase();
		performanceProfile = p;
		retinaGuard = false;
		optimizeShadowPass = false;
		spikeLogging = false;
		spikeThresholdMs = 35;
		memoryPressureFreeMbThreshold = 384.0;
		switch (p) {
			case "TELEMETRY" -> {
				swapInterval = -1;
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
				entityShadowSkip = false;
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
				memoryPressureFreeMbThreshold = 384.0;
				softCacheHints = true;
				softCacheHintIntervalTicks = 60;
				optimizeShadowPass = true;
				shadowEntityDistance = 48.0;
			}
			case "MAX" -> {
				performanceProfile = "MAX";
				swapInterval = -1;
				entityCull = true;
				entityCullDistance = 64.0;
				overrideSodiumEntityCull = false;
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
				entityShadowSkip = false;
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
				memoryPressureFreeMbThreshold = 384.0;
				softCacheHints = true;
				softCacheHintIntervalTicks = 40;
				optimizeShadowPass = false;
				shadowEntityDistance = 0.0;
			}
			case "SHADER" -> {
				// SHADER — optimized for Iris/Oculus shader gameplay: aggressive GPU budget management,
				// culls non-essential entities/particles to give maximum headroom to shader passes.
				// Visual quality stays high — shaders handle the beauty.
				performanceProfile = "SHADER";
				swapInterval = -1;
				entityCull = true;
				entityCullDistance = 64.0;
				overrideSodiumEntityCull = false;
				skipEntityNametags = true;
				particleCull = true;
				particleCullDistance = 32.0;
				maxParticles = 96;
				blockEntityCull = true;
				blockEntityCullDistance = 40.0;
				skipFarSignText = true;
				farSignDistance = 16.0;
				skipFarBannerPatterns = true;
				farBannerDistance = 20.0;
				entityShadowSkip = true;
				skipClouds = true; // Shader handles sky
				skipWeatherParticles = false; // Shaders make rain beautiful
				skipWeatherGeometry = false;
				skipToasts = true;
				skipStars = true; // Shader handles sky
				skipWorldBorder = true;
				skipBeaconBeams = false;
				skipVignette = true; // Shader handles post-processing
				skipNauseaOverlay = true;
				skipScoreboard = false;
				skipBossBar = false;
				skipUnderwaterOverlay = true; // Shader handles underwater
				skipFireOverlay = false;
				skipBobView = false;
				skipHurtTilt = false;
				skipPortalOverlay = true;
				skipStatusEffectOverlay = false;
				skipFloatingItem = false;
				skipSubtitles = true;
				skipDemoOverlay = true;
				skipLeashes = true;
				farItemEntityThrottle = true;
				farItemEntityDistance = 24.0;
				farExperienceOrbThrottle = true;
				farExperienceOrbDistance = 20.0;
				skipItemGlint = true; // Shader does its own specular
				lightmapThrottle = true;
				farPotionSwirlSkip = true;
				farPotionSwirlDistance = 32.0;
				forceFastGraphics = false;
				entityDistanceScaling = 0.75;
				farLimbThrottle = true;
				farLimbDistance = 48.0;
				farLimbTickMask = 3;
				farParticleSpawnSkip = true;
				ambientParticleThrottle = true;
				farSoundSkip = true;
				farSoundDistance = 40.0;
				memoryPressureFreeMbThreshold = 512.0; // Higher threshold for shader VRAM
				softCacheHints = true;
				softCacheHintIntervalTicks = 30;
				optimizeShadowPass = true;
				shadowEntityDistance = 32.0;
			}
			default -> {
				// PLAYABLE — default: looks pristine, RD 100% user-owned, ultra-optimized for 100+ FPS.
				// Gameplay cues stay intact here (world border visible, far sounds audible);
				// MAX/SHADER strip them for extra headroom.
				performanceProfile = "PLAYABLE";
				swapInterval = -1;
				entityCull = true;
				entityCullDistance = 80.0;
				overrideSodiumEntityCull = false;
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
				entityShadowSkip = false;
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
				farSoundSkip = false;
				farSoundDistance = 56.0;
				memoryPressureFreeMbThreshold = 384.0;
				softCacheHints = true;
				softCacheHintIntervalTicks = 40;
				optimizeShadowPass = false;
				shadowEntityDistance = 0.0;
			}
		}
		applyChipDefaults(this);
	}

	/**
	 * Active per-chip tuning. The profile's numeric budgets are treated as the BASE-tier target;
	 * stronger chips (PRO/MAX/ULTRA) scale render distances and particle budgets up, while the
	 * physical RAM class stays the hard ceiling. Called at the tail of {@link #applyProfile()}
	 * once — right after the profile has re-set its baseline values — so it never compounds.
	 * No-op until {@link SiliconChipInfo#isRegistered()} so placeholder budgets never persist.
	 */
	public static void applyChipDefaults(M3Config cfg) {
		if (!SiliconChipInfo.isRegistered()) {
			return;
		}
		double distScale;
		double particleScale;
		switch (SiliconChipInfo.getChipTier()) {
			case ULTRA -> { distScale = 1.45; particleScale = 2.0; }
			case MAX   -> { distScale = 1.25; particleScale = 1.6; }
			case PRO   -> { distScale = 1.10; particleScale = 1.3; }
			default    -> { distScale = 1.0;  particleScale = 1.0; } // BASE
		}
		cfg.entityCullDistance = RamClassPolicy.capEntityCull(cfg.entityCullDistance * distScale);
		cfg.particleCullDistance = RamClassPolicy.capParticleCull(cfg.particleCullDistance * distScale);
		cfg.blockEntityCullDistance = RamClassPolicy.capBlockEntityCull(cfg.blockEntityCullDistance * distScale);
		cfg.farSoundDistance = Math.min(256.0, cfg.farSoundDistance * distScale);
		cfg.maxParticles = RamClassPolicy.capParticles((int) Math.round(cfg.maxParticles * particleScale));
		if (RamClassPolicy.isRegistered()) {
			cfg.memoryPressureFreeMbThreshold = Math.max(cfg.memoryPressureFreeMbThreshold, RamClassPolicy.pressureEnterMb());
			cfg.memoryPressureRecoverMbThreshold = Math.max(
				cfg.memoryPressureRecoverMbThreshold,
				Math.max(cfg.memoryPressureFreeMbThreshold, RamClassPolicy.pressureRecoverMb())
			);
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
				throw new IllegalStateException("empty config");
			}
			M3Config merged = defaults();
			// Gson has no field-presence merge; overlay the parsed values onto a fully
			// initialized default object so partial files cannot zero-fill new fields.
			com.google.gson.JsonObject raw = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			for (var entry : raw.entrySet()) {
				try {
					var field = M3Config.class.getField(entry.getKey());
					Object value = GSON.fromJson(entry.getValue(), field.getGenericType());
					field.set(merged, value);
				} catch (NoSuchFieldException ignored) {
					M3FrametimeMod.LOGGER.debug("Ignoring unknown config field: {}", entry.getKey());
				}
			}
			if (merged.performanceProfile == null || merged.performanceProfile.isBlank()) merged.performanceProfile = "PLAYABLE";
			String profile = merged.performanceProfile.trim().toUpperCase();
			if (!profile.equals("TELEMETRY") && !profile.equals("BALANCED") && !profile.equals("MAX") && !profile.equals("PLAYABLE") && !profile.equals("SHADER")) merged.performanceProfile = "PLAYABLE";
			sanitize(merged);
			return merged;
		} catch (Exception e) {
			M3FrametimeMod.LOGGER.warn("Failed to load config, using defaults: {}", e.toString());
			M3Config fallback = defaults();
			fallback.save();
			return fallback;
		}
	}

	private static void sanitize(M3Config cfg) {
		if (!Double.isFinite(cfg.pacingEmaAlpha) || cfg.pacingEmaAlpha <= 0.0 || cfg.pacingEmaAlpha > 1.0) {
			cfg.pacingEmaAlpha = 0.12;
		}
		if (cfg.spikeThresholdMs < 1L) {
			cfg.spikeThresholdMs = 35L;
		}
		cfg.workerThreads = Math.max(0, cfg.workerThreads);
		cfg.performanceRecorderWindow = Math.max(32, Math.min(2048, cfg.performanceRecorderWindow));
		cfg.maxParticles = Math.max(0, cfg.maxParticles);
		cfg.softCacheHintIntervalTicks = Math.max(1, cfg.softCacheHintIntervalTicks);
		cfg.memoryPressureFreeMbThreshold = finiteRange(cfg.memoryPressureFreeMbThreshold, 384.0, 0.0, 16384.0);
		cfg.memoryPressureRecoverMbThreshold = finiteRange(cfg.memoryPressureRecoverMbThreshold, 768.0, cfg.memoryPressureFreeMbThreshold, 16384.0);
		cfg.memoryPressureEnterSamples = Math.max(1, Math.min(20, cfg.memoryPressureEnterSamples));
		cfg.memoryPressureRecoverSamples = Math.max(1, Math.min(120, cfg.memoryPressureRecoverSamples));
		cfg.heapPressureEnterRatio = finiteRange(cfg.heapPressureEnterRatio, 0.90, 0.50, 0.99);
		cfg.heapPressureRecoverRatio = finiteRange(cfg.heapPressureRecoverRatio, 0.80, 0.25, cfg.heapPressureEnterRatio);
		cfg.shaderThrottleEnterSamples = Math.max(1, Math.min(20, cfg.shaderThrottleEnterSamples));
		cfg.shaderThrottleRecoverSamples = Math.max(1, Math.min(120, cfg.shaderThrottleRecoverSamples));
		cfg.shaderThrottleP95Ms = finiteRange(cfg.shaderThrottleP95Ms, 28.0, 8.0, 100.0);
		cfg.farLimbTickMask = Math.max(0, cfg.farLimbTickMask);
		cfg.swapInterval = Math.max(-1, Math.min(1, cfg.swapInterval));

		cfg.shadowEntityDistance = nonNegative(cfg.shadowEntityDistance, 32.0);
		cfg.entityCullDistance = nonNegative(cfg.entityCullDistance, 80.0);
		cfg.particleCullDistance = nonNegative(cfg.particleCullDistance, 40.0);
		cfg.blockEntityCullDistance = nonNegative(cfg.blockEntityCullDistance, 48.0);
		cfg.farSignDistance = nonNegative(cfg.farSignDistance, 24.0);
		cfg.farBannerDistance = nonNegative(cfg.farBannerDistance, 32.0);
		cfg.farItemEntityDistance = nonNegative(cfg.farItemEntityDistance, 40.0);
		cfg.farExperienceOrbDistance = nonNegative(cfg.farExperienceOrbDistance, 36.0);
		cfg.farPotionSwirlDistance = nonNegative(cfg.farPotionSwirlDistance, 56.0);
		cfg.farSoundDistance = nonNegative(cfg.farSoundDistance, 56.0);
		cfg.farLimbDistance = nonNegative(cfg.farLimbDistance, 72.0);
		cfg.entityDistanceScaling = finiteRange(cfg.entityDistanceScaling, 1.0, 0.0, 1.0);
	}

	private static double nonNegative(double value, double fallback) {
		return Double.isFinite(value) && value >= 0.0 ? value : fallback;
	}

	private static double finiteRange(double value, double fallback, double min, double max) {
		return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
	}

	public synchronized void save() {
		Path target = path();
		Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
		try {
			Files.createDirectories(target.getParent());
			try (Writer writer = Files.newBufferedWriter(temporary)) {
				GSON.toJson(this, writer);
			}
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			M3FrametimeMod.LOGGER.warn("Failed to save config: {}", e.toString());
			try {
				Files.deleteIfExists(temporary);
			} catch (IOException cleanupError) {
				M3FrametimeMod.LOGGER.debug("Failed to remove temporary config: {}", cleanupError.toString());
			}
		}
	}
}
