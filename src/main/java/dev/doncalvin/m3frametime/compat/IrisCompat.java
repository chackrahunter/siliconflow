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
	private static final boolean IRIS_LOADED =
		FabricLoader.getInstance().isModLoaded("iris")
		|| FabricLoader.getInstance().isModLoaded("oculus");

	private static Object irisApiInstance;
	private static MethodHandle isShaderPackInUseHandle;
	private static MethodHandle isRenderingShadowPassHandle;
	private static boolean initialized;

	// Cached state to avoid per-frame reflection lookup
	private static boolean cachedShaderActive;
	private static long lastShaderCheckNanos;

	private IrisCompat() {}

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
				Method m1 = irisApiInstance.getClass().getMethod("isShaderPackInUse");
				isShaderPackInUseHandle = lookup.unreflect(m1);
				try {
					Method m2 = irisApiInstance.getClass().getMethod("isRenderingShadowPass");
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

	/** Returns true when Iris is currently rendering the shadow map pass (fast MethodHandle). */
	public static boolean isShadowPass() {
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
}
