package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.config.M3Config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skip potion swirl particle updates for far living entities (client-only).
 * Lithium optimizes effect ticking logic; this is leftover visual particle cost.
 * Exact-target injection.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPotionSwirlMixin {
	@Inject(method = "updatePotionSwirls", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipFarPotionSwirls(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.getWorld().isClient || self instanceof PlayerEntity) {
			return;
		}
		M3Config cfg = M3FrametimeMod.config();
		if (!cfg.farPotionSwirlSkip || cfg.farPotionSwirlDistance <= 0.0) {
			return;
		}
		if (ClientDistance.tooFar(self.getX(), self.getY(), self.getZ(), cfg.farPotionSwirlDistance)) {
			ci.cancel();
		}
	}
}
