package dev.doncalvin.m3frametime.display;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

/**
 * Retina / HiDPI consistency helpers. Avoids fighting Sodium's GL path.
 */
public final class RetinaGuard {
	private RetinaGuard() {}

	public static boolean enabled() {
		return M3FrametimeMod.config().retinaGuard;
	}

	/**
	 * Prefer integer framebuffer/GUI scale relationships to reduce Retina blit shimmer.
	 * Returns adjusted scale factor suggestion (1,2,3...).
	 */
	public static int preferIntegerScale(double rawScale) {
		if (!M3FrametimeMod.config().preferIntegerScale) {
			return Math.max(1, (int) Math.round(rawScale));
		}
		int rounded = (int) Math.round(rawScale);
		return Math.max(1, rounded);
	}

	public static boolean isMacOs() {
		String os = System.getProperty("os.name", "").toLowerCase();
		return os.contains("mac");
	}
}
