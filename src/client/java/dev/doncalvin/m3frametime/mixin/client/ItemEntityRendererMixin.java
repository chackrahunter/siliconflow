package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Far dropped-item updateRenderState throttle — skips spin/bob/stack work most ticks and in shadow passes.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
	@Inject(method = "updateRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$throttleFarItem(
		ItemEntity entity,
		ItemEntityRenderState state,
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
		if (!M3FrametimeMod.config().farItemEntityThrottle) {
			return;
		}
		double lim = RamDiscipline.get().effectiveFarItemEntityDistance();
		if (lim <= 0.0 || !ClientDistance.tooFar(entity.getX(), entity.getY(), entity.getZ(), lim)) {
			return;
		}
		// Keep ~1/4 far item anim updates; cancel the rest before stack/random work.
		if ((entity.age & 3) == 0) {
			return;
		}
		ci.cancel();
	}
}
