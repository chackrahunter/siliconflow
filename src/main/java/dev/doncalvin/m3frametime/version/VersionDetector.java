package dev.doncalvin.m3frametime.version;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;

import java.util.Optional;

/**
 * Universal runtime Minecraft version detector and capability evaluator for SiliconFlow.
 * Uses pure metadata-based version parsing without loading any Minecraft classes into the JVM,
 * ensuring zero MixinTargetAlreadyLoadedException issues during preLaunch.
 */
public final class VersionDetector {
	private static final VersionDetector INSTANCE = new VersionDetector();

	private final int major;
	private final int minor;
	private final int patch;
	private final String rawVersion;

	private VersionDetector() {
		String detectedVersion = "1.21.4"; // Default fallback
		int maj = 1;
		int min = 21;
		int pat = 4;

		try {
			Optional<ModContainer> mcMod = FabricLoader.getInstance().getModContainer("minecraft");
			if (mcMod.isPresent()) {
				Version v = mcMod.get().getMetadata().getVersion();
				detectedVersion = v.getFriendlyString();
				String[] parts = detectedVersion.split("[.\\-+_]");
				if (parts.length >= 1) {
					maj = parsePart(parts[0], 1);
				}
				if (parts.length >= 2) {
					min = parsePart(parts[1], 21);
				}
				if (parts.length >= 3) {
					pat = parsePart(parts[2], 0);
				}
			}
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.warn("VersionDetector: Failed to query Minecraft version from FabricLoader, falling back to 1.21.4: {}", t.toString());
		}

		this.major = maj;
		this.minor = min;
		this.patch = pat;
		this.rawVersion = detectedVersion;

		M3FrametimeMod.LOGGER.info(
			"SiliconFlow Version Engine: Active Minecraft {} (Major={}, Minor={}, Patch={})",
			this.rawVersion,
			this.major,
			this.minor,
			this.patch
		);
	}

	private static int parsePart(String s, int fallback) {
		try {
			return Integer.parseInt(s.replaceAll("[^0-9]", ""));
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	public static VersionDetector get() {
		return INSTANCE;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPatch() {
		return patch;
	}

	public String getRawVersion() {
		return rawVersion;
	}

	/** Returns true if the running Minecraft version is at least the specified major.minor.patch. */
	public boolean isAtLeast(int targetMajor, int targetMinor, int targetPatch) {
		if (this.major != targetMajor) {
			return this.major > targetMajor;
		}
		if (this.minor != targetMinor) {
			return this.minor > targetMinor;
		}
		return this.patch >= targetPatch;
	}

	/** Returns true if running on Minecraft 1.21.2+ with RenderState DTO architecture. */
	public boolean isRenderStateEra() {
		return isAtLeast(1, 21, 2);
	}

	/** Returns true if running on Minecraft 1.20+ with DrawContext GUI architecture. */
	public boolean isDrawContextEra() {
		return isAtLeast(1, 20, 0);
	}

	/** Returns true if running on Minecraft 1.17+ with modern OpenGL 3.2 core profile. */
	public boolean isModernGl() {
		return isAtLeast(1, 17, 0);
	}
}
