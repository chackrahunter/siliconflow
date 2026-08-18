package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
	@Inject(method = "doRandomBlockDisplayTicks", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$throttleAmbient(int centerX, int centerY, int centerZ, CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.PARTICLE);
		try {
			if (!M3FrametimeMod.config().ambientParticleThrottle) {
				return;
			}
			ClientWorld world = (ClientWorld) (Object) this;
			if ((world.getTime() & 3L) != 0L) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.PARTICLE);
		}
	}
}
