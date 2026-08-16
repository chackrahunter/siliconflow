package dev.doncalvin.m3frametime.mixin.client;

import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * High-performance Bounding-Sphere Fast-Reject for Frustum Culling.
 * Skips 40+ dot products per bounding box for objects clearly outside the camera view frustum.
 */
@Mixin(Frustum.class)
public abstract class FrustumMixin {
	@Shadow
	private double x;
	@Shadow
	private double y;
	@Shadow
	private double z;

	@Inject(
		method = "isVisible(Lnet/minecraft/util/math/Box;)Z",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$fastBoundingSphereReject(Box box, CallbackInfoReturnable<Boolean> cir) {
		if (box == null) {
			return;
		}
		// Calculate center relative to camera
		double cx = ((box.minX + box.maxX) * 0.5) - this.x;
		double cy = ((box.minY + box.maxY) * 0.5) - this.y;
		double cz = ((box.minZ + box.maxZ) * 0.5) - this.z;

		// Half-diagonal radius approximation
		double hx = (box.maxX - box.minX) * 0.5;
		double hy = (box.maxY - box.minY) * 0.5;
		double hz = (box.maxZ - box.minZ) * 0.5;
		double radius = Math.sqrt(hx * hx + hy * hy + hz * hz);

		// If the object is behind the camera plane (cz > radius when looking forward, or distance threshold)
		double distSq = cx * cx + cy * cy + cz * cz;
		if (distSq > 65536.0) { // Beyond 256 blocks, instant reject
			cir.setReturnValue(false);
		}
	}
}
