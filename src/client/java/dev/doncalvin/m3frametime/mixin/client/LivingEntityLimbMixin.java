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
 * Client-only limb animation throttle for far living entities.
 * Guards with world.isClient so integrated-server ticks (and Lithium) are untouched.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLimbMixin {
	@Inject(method = "updateLimbs(Z)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$throttleFarLimbs(boolean flutter, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.getWorld().isClient || self instanceof PlayerEntity) {
			return;
		}
		M3Config cfg = M3FrametimeMod.config();
		if (!cfg.farLimbThrottle || cfg.farLimbDistance <= 0.0) {
			return;
		}
		if (!ClientDistance.tooFar(self.getX(), self.getY(), self.getZ(), cfg.farLimbDistance)) {
			return;
		}
		int mask = cfg.farLimbTickMask;
		if (mask <= 0 || (self.age & mask) == 0) {
			return;
		}
		ci.cancel();
	}
}
