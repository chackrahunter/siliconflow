package dev.doncalvin.m3frametime.mixin;

import dev.doncalvin.m3frametime.math.FastMath;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Replaces vanilla MathHelper trigonometric & geometric operations with SiliconFlow FastMath.
 * Eliminates native FPU instruction latency, branch stalls, and division bottlenecks on ARM64 Apple Silicon P-Cores.
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

	/**
	 * @author SiliconFlow
	 * @reason Fast zero-overflow 2D Euclidean hypotenuse
	 */
	@Overwrite
	public static double hypot(double a, double b) {
		return FastMath.fastHypot(a, b);
	}

	/**
	 * @author SiliconFlow
	 * @reason Fast ARM64 polynomial atan2
	 */
	@Overwrite
	public static double atan2(double y, double x) {
		return FastMath.atan2((float) y, (float) x);
	}
}
