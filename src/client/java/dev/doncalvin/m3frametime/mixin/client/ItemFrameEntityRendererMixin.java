package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.compat.StackCompat;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Culls item frame render state in Iris shadow passes and for distant item frames in storage rooms.
 */
@Mixin(ItemFrameEntityRenderer.class)
public abstract class ItemFrameEntityRendererMixin {
	@Inject(method = "updateRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$cullItemFrame(
		ItemFrameEntity entity,
		ItemFrameEntityRenderState state,
		float tickDelta,
		CallbackInfo ci
	) {
		if (entity == null) {
			return;
		}
		if (StackCompat.isShadowPass()) {
			ci.cancel();
			return;
		}
		double dist = M3FrametimeMod.config().farItemEntityDistance;
		if (dist > 0.0 && ClientDistance.tooFar(entity.getX(), entity.getY(), entity.getZ(), dist)) {
			// Update far item frames only every 4th tick
			if ((entity.age & 3) != 0) {
				ci.cancel();
			}
		}
	}
}
