package dev.doncalvin.m3frametime;

import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.engine.SiliconCpuTopology;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class M3FrametimeMod implements ModInitializer {
	public static final String MOD_ID = "m3-frametime";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static volatile M3Config config = M3Config.defaults();

	@Override
	public void onInitialize() {
		// Detect the Apple Silicon chip BEFORE the first config load so tier/RAM-class
		// scaling is registered when applyChipDefaults() runs — otherwise the persisted
		// config is derived from placeholder budgets and the per-chip tuning never applies.
		SiliconCpuTopology.get();
		M3Config loaded = M3Config.load();
		config = loaded;
		String profile = config.performanceProfile == null ? "" : config.performanceProfile.trim();
		if (config.pacingEnabled) {
			LOGGER.warn("pacingEnabled=true sleeps after frames (~10 FPS risk). Prefer false for MAX FPS.");
		}
		LOGGER.info(
			"M3 Frametime ready | profile={} pacing={} | sodium={} lithium={} ferritecore={} immediatelyfast={} | runtime tuner=disabled",
			config.performanceProfile,
			config.pacingEnabled,
			StackCompat.sodium(),
			StackCompat.lithium(),
			StackCompat.ferritecore(),
			StackCompat.immediatelyFast()
		);
	}

	public static M3Config config() {
		return config;
	}

	public static void reloadConfig() {
		publishConfig(M3Config.load());
	}

	public static void publishConfig(M3Config next) {
		if (next != null) {
			config = next;
		}
	}
}
