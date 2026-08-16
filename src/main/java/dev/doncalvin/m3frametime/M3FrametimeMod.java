package dev.doncalvin.m3frametime;

import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class M3FrametimeMod implements ModInitializer {
	public static final String MOD_ID = "m3-frametime";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static M3Config config = M3Config.defaults();

	@Override
	public void onInitialize() {
		// Best-effort before commonPool is touched by other mods.
		String fjKey = "java.util.concurrent.ForkJoinPool.common.parallelism";
		if (System.getProperty(fjKey) == null) {
			int cores = Runtime.getRuntime().availableProcessors();
			System.setProperty(fjKey, Integer.toString(Math.max(1, cores - 1)));
		}

		config = M3Config.load();
		String profile = config.performanceProfile == null ? "" : config.performanceProfile.trim();
		if ("MAX".equalsIgnoreCase(profile) || "PLAYABLE".equalsIgnoreCase(profile)) {
			config.pacingEnabled = false;
		} else if (config.pacingEnabled) {
			LOGGER.warn("pacingEnabled=true sleeps after frames (~10 FPS risk). Prefer false for MAX FPS.");
		}
		LOGGER.info(
			"M3 Frametime amplifies stack | profile={} pacing={} | sodium={} lithium={} ferritecore={} immediatelyfast={} | leftover: particles/BE/weather/clouds/distance + Sodium worker soft-boost",
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
		config = M3Config.load();
	}
}
