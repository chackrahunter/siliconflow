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
public abstract class LivingEntityPotionSwirlMixin {
	@Inject(method = "updatePotionSwirls", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipFarSwirl(CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.PARTICLE);
		try {
			LivingEntity entity = (LivingEntity) (Object) this;
			if (!RenderCull.isClient(entity) || RenderCull.isPlayer(entity)) {
				return;
			}
			M3Config cfg = M3FrametimeMod.config();
			if (cfg.farPotionSwirlSkip && RenderCull.fartherThan(entity, cfg.farPotionSwirlDistance)) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.PARTICLE);
		}
	}
}
