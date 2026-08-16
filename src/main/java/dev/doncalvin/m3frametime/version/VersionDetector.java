package dev.doncalvin.m3frametime.version;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;

import java.util.Optional;

/**
 * Universal runtime Minecraft version detector and capability evaluator for SiliconFlow.
 * Automatically adapts mixins and render pipelines across Minecraft versions (1.16.5 up to 1.21.4+).
 */
public final class VersionDetector {
	private static final VersionDetector INSTANCE = new VersionDetector();

	private final int major;
	private final int minor;
	private final int patch;
	private final String rawVersion;
	private final boolean hasEntityRenderState;
	private final boolean hasDrawContext;
	private final boolean hasModernGl;

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

		// Probe capability classes safely via reflection
		this.hasEntityRenderState = isClassPresent("net.minecraft.client.render.entity.state.EntityRenderState");
		this.hasDrawContext = isClassPresent("net.minecraft.client.gui.DrawContext");
		this.hasModernGl = isClassPresent("net.minecraft.client.render.BufferRenderer");

		M3FrametimeMod.LOGGER.info(
			"SiliconFlow Version Engine: Detected Minecraft {} (Major={}, Minor={}, Patch={}) | RenderState Era={} | DrawContext Era={}",
			this.rawVersion,
			this.major,
			this.minor,
			this.patch,
			this.hasEntityRenderState,
			this.hasDrawContext
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
		return hasEntityRenderState || isAtLeast(1, 21, 2);
	}

	/** Returns true if running on Minecraft 1.20+ with DrawContext GUI architecture. */
	public boolean isDrawContextEra() {
		return hasDrawContext || isAtLeast(1, 20, 0);
	}

	/** Returns true if running on Minecraft 1.17+ with modern OpenGL 3.2 core profile. */
	public boolean isModernGl() {
		return hasModernGl || isAtLeast(1, 17, 0);
	}

	/** Probes whether a specific class exists in the active JVM classpath without triggering static initialization. */
	public static boolean isClassPresent(String className) {
		try {
			Class.forName(className, false, VersionDetector.class.getClassLoader());
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}
}
