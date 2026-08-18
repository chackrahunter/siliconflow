package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipNametag(LivingEntity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (M3FrametimeMod.config().skipEntityNametags && !RenderCull.isPlayer(entity)) {
				cir.setReturnValue(false);
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}
}
