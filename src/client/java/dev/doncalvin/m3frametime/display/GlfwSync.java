package dev.doncalvin.m3frametime.display;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import org.lwjgl.glfw.GLFW;

/**
 * High-performance GLFW swap-interval controller.
 * Uncaps frame rate (glfwSwapInterval=0) to unleash the full Apple Silicon M3 GPU power.
 * Caches refresh rate to prevent expensive CoreGraphics/WindowServer IPC on every frame.
 */
public final class GlfwSync {
	private static boolean applied;
	private static int cachedHz = 60;
	private static long lastQueryNanos;
	private static long lastHandle;

	private GlfwSync() {}

	public static void applyIfConfigured(long windowHandle) {
		if (applied || windowHandle == 0L) {
			return;
		}
		int interval = M3FrametimeMod.config().swapInterval;
		if (interval < 0) {
			return;
		}
		try {
			GLFW.glfwSwapInterval(interval);
			M3FrametimeMod.LOGGER.info("Applied glfwSwapInterval={} (Uncapped M3 Metal Pipeline)", interval);
			applied = true;
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.warn("glfwSwapInterval failed: {}", t.toString());
		}
	}

	public static int queryRefreshRate(long windowHandle) {
		if (windowHandle == 0L) {
			return cachedHz;
		}
		long now = System.nanoTime();
		if (windowHandle == lastHandle && now - lastQueryNanos < 5_000_000_000L && cachedHz > 0) {
			return cachedHz;
		}
		lastHandle = windowHandle;
		lastQueryNanos = now;
		try {
			var monitor = GLFW.glfwGetWindowMonitor(windowHandle);
			if (monitor == 0L) {
				monitor = GLFW.glfwGetPrimaryMonitor();
			}
			if (monitor == 0L) {
				return cachedHz;
			}
			var mode = GLFW.glfwGetVideoMode(monitor);
			if (mode != null && mode.refreshRate() > 0) {
				cachedHz = mode.refreshRate();
			}
			return cachedHz;
		} catch (Throwable t) {
			return cachedHz;
		}
	}
}
