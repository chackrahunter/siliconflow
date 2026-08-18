package dev.doncalvin.m3frametime.compat;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * High-speed, zero-dependency bridge to Iris / Oculus shader pipeline.
 * Uses MethodHandles for near-native invocation speed without reflection overhead in render hot paths.
 */
public final class IrisCompat {
	private static final boolean IRIS_LOADED = detectIris();

	// Published from the synchronized initReflection() and read from render/tick/worker threads.
	private static volatile Object irisApiInstance;
	private static volatile MethodHandle isShaderPackInUseHandle;
	private static volatile MethodHandle isRenderingShadowPassHandle;
	private static volatile boolean initialized;

	// Cached state to avoid per-frame reflection lookup (read across threads).
	private static volatile boolean cachedShaderActive;
	private static volatile long lastShaderCheckNanos;
	private static volatile boolean cachedInShadowPass;
	private static volatile boolean shadowPassSinceTick;

	private IrisCompat() {}

	private static boolean detectIris() {
		if (FabricLoader.getInstance().isModLoaded("iris") || FabricLoader.getInstance().isModLoaded("oculus")) return true;
		try { Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, IrisCompat.class.getClassLoader()); return true; }
		catch (Throwable ignored) { return false; }
	}

	private static synchronized void initReflection() {
		if (initialized || !IRIS_LOADED) {
			initialized = true;
			return;
		}
		initialized = true;
		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Method getInstanceMethod = apiClass.getMethod("getInstance");
			irisApiInstance = getInstanceMethod.invoke(null);
			if (irisApiInstance != null) {
				MethodHandles.Lookup lookup = MethodHandles.publicLookup();
				Method m1 = apiClass.getMethod("isShaderPackInUse");
				isShaderPackInUseHandle = lookup.unreflect(m1);
				try {
					Method m2 = apiClass.getMethod("isRenderingShadowPass");
					isRenderingShadowPassHandle = lookup.unreflect(m2);
				} catch (NoSuchMethodException ignored) {
				}
				M3FrametimeMod.LOGGER.info("IrisCompat: Connected to Iris Shader API via MethodHandles");
			}
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("IrisCompat: Shader API reflection skipped: {}", t.toString());
		}
	}

	public static boolean isIrisLoaded() {
		return IRIS_LOADED;
	}

	/** Returns true if a custom shaderpack is currently active (cached for 1 second). */
	public static boolean isShaderActive() {
		if (!IRIS_LOADED) {
			return false;
		}
		long now = System.nanoTime();
		if (now - lastShaderCheckNanos < 1_000_000_000L) {
			return cachedShaderActive;
		}
		lastShaderCheckNanos = now;
		if (!initialized) {
			initReflection();
		}
		if (irisApiInstance != null && isShaderPackInUseHandle != null) {
			try {
				cachedShaderActive = (boolean) isShaderPackInUseHandle.invoke(irisApiInstance);
				return cachedShaderActive;
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	/**
	 * Live Iris shadow-pass query. Unlike {@link #isShadowPass()}, this is not sticky
	 * across the rest of the frame — use it to latch {@link #setShadowPass(boolean)}.
	 */
	public static boolean queryLiveShadowPass() {
		if (!IRIS_LOADED) {
			return false;
		}
		if (!initialized) {
			initReflection();
		}
		if (irisApiInstance != null && isRenderingShadowPassHandle != null) {
			try {
				return (boolean) isRenderingShadowPassHandle.invoke(irisApiInstance);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	/** Returns true when Iris is currently rendering the shadow map pass (fast MethodHandle). */
	public static boolean isShadowPass() {
		if (cachedInShadowPass || shadowPassSinceTick) {
			return true;
		}
		if (!IRIS_LOADED) {
			return false;
		}
		if (!initialized) {
			initReflection();
		}
		if (irisApiInstance != null && isRenderingShadowPassHandle != null) {
			try {
				boolean live = (boolean) isRenderingShadowPassHandle.invoke(irisApiInstance);
				if (live) {
					cachedInShadowPass = true;
					shadowPassSinceTick = true;
				}
				return live;
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	/**
	 * Mixin-owned latch for the Iris shadow pass. Querying Iris at {@code MinecraftClient.render}
	 * RETURN is too late — cache the in-pass flag so GPU_004 can still attribute the frame.
	 */
	public static void setShadowPass(boolean inPass) {
		cachedInShadowPass = inPass;
		if (inPass) {
			shadowPassSinceTick = true;
		}
	}

	/** Drop the per-frame shadow latch after SpikeMonitor has consumed the previous frame. */
	public static void onClientTick() {
		shadowPassSinceTick = cachedInShadowPass;
	}
}
