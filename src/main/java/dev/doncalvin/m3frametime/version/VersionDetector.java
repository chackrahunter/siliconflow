package dev.doncalvin.m3frametime.version;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;

import java.util.Optional;

/**
 * Runtime metadata detector for the version-specific SiliconFlow artifact.
 *
 * <p>This detector is diagnostic and gates optional behavior only. It cannot make
 * bytecode compiled against one Minecraft/Yarn version load on another version.</p>
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
		String detectedVersion = "unknown";
		boolean detected = false;
		int maj = 1;
		int min = 21;
		int pat = 4;

		try {
			Optional<ModContainer> mcMod = FabricLoader.getInstance().getModContainer("minecraft");
			if (mcMod.isPresent()) {
				Version v = mcMod.get().getMetadata().getVersion();
				detectedVersion = v.getFriendlyString();
				detected = true;
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
			M3FrametimeMod.LOGGER.warn("VersionDetector: Failed to query Minecraft version from FabricLoader; compatibility is unknown: {}", t.toString());
		}

		this.major = maj;
		this.minor = min;
		this.patch = pat;
		this.rawVersion = detectedVersion;

		if (!detected) {
			M3FrametimeMod.LOGGER.warn("SiliconFlow could not verify the Minecraft version; this artifact targets Minecraft 1.21.4.");
		}

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

	/** Exact Minecraft version compiled and tested by this artifact. */
	public boolean isSupportedArtifactVersion() {
		return major == 1 && minor == 21 && patch == 4 && "1.21.4".equals(rawVersion);
	}

	/** Whether runtime metadata matches the artifact target exactly. */
	public boolean isExactArtifactTarget() {
		return isSupportedArtifactVersion();
	}

	/** Exact target only; this artifact has no cross-release compatibility claim. */
	public boolean isExactTargetOnly() {
		return isExactArtifactTarget();
	}


}
