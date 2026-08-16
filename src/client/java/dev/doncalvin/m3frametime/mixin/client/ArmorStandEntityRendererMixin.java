package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.compat.StackCompat;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Culls Armor Stand state updates when distant in bases and during Iris shadow passes.
 */
@Mixin(ArmorStandEntityRenderer.class)
public abstract class ArmorStandEntityRendererMixin {
	@Inject(method = "updateRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$cullArmorStand(
		ArmorStandEntity entity,
		ArmorStandEntityRenderState state,
		float tickDelta,
		CallbackInfo ci
	) {
		if (entity == null) {
			return;
		}
		if (StackCompat.isShadowPass()) {
			double shadowDist = M3FrametimeMod.config().shadowEntityDistance;
			if (shadowDist > 0.0 && ClientDistance.tooFar(entity.getX(), entity.getY(), entity.getZ(), shadowDist)) {
				ci.cancel();
				return;
			}
		}
		double dist = M3FrametimeMod.config().entityCullDistance * 0.5;
		if (dist > 0.0 && ClientDistance.tooFar(entity.getX(), entity.getY(), entity.getZ(), dist)) {
			if ((entity.age & 3) != 0) {
				ci.cancel();
			}
		}
	}
}
