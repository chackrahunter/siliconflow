package dev.doncalvin.m3frametime.mixin;

import dev.doncalvin.m3frametime.math.FastMath;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Replaces vanilla MathHelper trigonometric operations with SiliconFlow FastMath.
 * Eliminates native FPU instruction latency and branch stalls on ARM64 Apple Silicon P-Cores.
 */
@Mixin(MathHelper.class)
public abstract class MathHelperMixin {

	/**
	 * @author SiliconFlow
	 * @reason High-performance zero-branch ARM64 lookup table sine
	 */
	@Overwrite
	public static float sin(float value) {
		return FastMath.sin(value);
	}

	/**
	 * @author SiliconFlow
	 * @reason High-performance zero-branch ARM64 lookup table cosine
	 */
	@Overwrite
	public static float cos(float value) {
		return FastMath.cos(value);
	}
}
