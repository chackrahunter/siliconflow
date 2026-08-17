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
 * Client-only limb animation throttle for far and stationary living entities.
 * Saves trigonometric computations and bone matrix calculations in animal pens and trading halls.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLimbMixin {
	@Inject(method = "updateLimbs(Z)V", at = @At("HEAD"), cancellable = true)
	private void m3frametime$throttleFarLimbs(boolean flutter, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.getWorld().isClient || self instanceof PlayerEntity) {
			return;
		}

		// 1. Stationary entity optimization: when idle, skip 7/8 limb ticks
		if (self.getVelocity().lengthSquared() < 0.0001) {
			if ((self.age & 7) != 0) {
				ci.cancel();
				return;
			}
		}

		// 2. Far entity distance throttle
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
