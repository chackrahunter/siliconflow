package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import dev.doncalvin.m3frametime.client.ClientDistance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skip only explicitly far ambient particle spawns; vanilla ambient scheduling remains intact.
 */
@Mixin(ClientWorld.class)
public abstract class ClientWorldParticleMixin {
	@Inject(
		method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void m3frametime$skipFarParticleFlags(
		ParticleEffect parameters,
		boolean alwaysSpawn,
		boolean canSpawnOnMinimal,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ,
		CallbackInfo ci
	) {
		if (!alwaysSpawn && m3frametime$shouldSkipSpawn(x, y, z)) {
			ci.cancel();
		}
	}

	@Unique
	private boolean m3frametime$shouldSkipSpawn(double x, double y, double z) {
		M3Config cfg = M3FrametimeMod.config();
		double lim = RamDiscipline.get().effectiveParticleCullDistance();
		if (!cfg.farParticleSpawnSkip || !cfg.particleCull || lim <= 0.0) {
			return false;
		}
		return ClientDistance.tooFar(x, y, z, lim);
	}
}
