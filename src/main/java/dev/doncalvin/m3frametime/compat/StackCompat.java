package dev.doncalvin.m3frametime.compat;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Detects the optimized Fabric stack (Sodium, Iris, Lithium, FerriteCore, ImmediatelyFast)
 * so we amplify performance across the entire pipeline.
 */
public final class StackCompat {
	private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
	private static final boolean LITHIUM = FabricLoader.getInstance().isModLoaded("lithium");
	private static final boolean FERRITE = FabricLoader.getInstance().isModLoaded("ferritecore");
	private static final boolean IMMEDIATELY_FAST = FabricLoader.getInstance().isModLoaded("immediatelyfast");

	private StackCompat() {}

	public static boolean sodium() {
		return SODIUM;
	}

	public static boolean lithium() {
		return LITHIUM;
	}

	public static boolean ferritecore() {
		return FERRITE;
	}

	public static boolean immediatelyFast() {
		return IMMEDIATELY_FAST;
	}

	public static boolean iris() {
		return IrisCompat.isIrisLoaded();
	}

	public static boolean isShaderActive() {
		return IrisCompat.isShaderActive();
	}

	public static boolean isShadowPass() {
		return IrisCompat.isShadowPass();
	}

	/**
	 * Cheap AABB frustum early-out in EntityRenderDispatcher.shouldRender.
	 * Disabled by default with Sodium because renderer ownership and shader compatibility
	 * remain with Sodium/Iris. Users may opt in explicitly.
	 */
	public static boolean useAggressiveEntityFrustum() {
		return M3FrametimeMod.config().overrideSodiumEntityCull;
	}

	/**
	 * Leftover worker pool stays small to avoid competing with renderer-owned work.
	 */
	public static int preferredWorkerThreads() {
		return SODIUM ? 1 : Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 4));
	}
}
