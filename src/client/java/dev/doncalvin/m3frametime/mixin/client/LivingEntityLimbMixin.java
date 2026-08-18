package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLimbMixin {
	@Inject(method = "updateLimbs(Z)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$throttleLimbs(boolean flutter, CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.ENTITY_TICK);
		try {
			LivingEntity entity = (LivingEntity) (Object) this;
			if (!RenderCull.isClient(entity) || RenderCull.isPlayer(entity)) {
				return;
			}
			M3Config cfg = M3FrametimeMod.config();
			if (!cfg.farLimbThrottle || !RenderCull.fartherThan(entity, cfg.farLimbDistance)) {
				return;
			}
			int mask = cfg.farLimbTickMask;
			if (mask > 0 && (entity.age & mask) != 0) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.ENTITY_TICK);
		}
	}
}
