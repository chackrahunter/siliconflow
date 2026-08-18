package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.pool.ParticleTrim;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
	@Unique
	private static volatile boolean m3frametime$hooked;

	/**
	 * Cached live particle count for the max-particles cap. Resynced to the true total once
	 * per tick and incremented per approved spawn, so the per-spawn cap check is O(1) instead
	 * of walking every texture-sheet queue on every addParticle (the storm hot path).
	 */
	@Unique
	private int m3frametime$cachedCount;

	@Inject(method = "tick()V", at = @At("HEAD"), require = 0)
	private void m3frametime$registerTrimHook(CallbackInfo ci) {
		m3frametime$ensureHook();
		m3frametime$cachedCount = m3frametime$particleCount();
	}

	@Inject(
		method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$skipSpawn(
		ParticleEffect parameters,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ,
		CallbackInfoReturnable<Particle> cir
	) {
		m3frametime$ensureHook();
		SpikeScope.get().push(SpikeScope.Phase.PARTICLE);
		try {
			if (m3frametime$shouldSkipSpawn(x, y, z)) {
				cir.setReturnValue(null);
			} else {
				m3frametime$cachedCount++;
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.PARTICLE);
		}
	}

	@Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipQueued(Particle particle, CallbackInfo ci) {
		m3frametime$ensureHook();
		SpikeScope.get().push(SpikeScope.Phase.PARTICLE);
		try {
			if (particle == null) {
				return;
			}
			Box box = particle.getBoundingBox();
			if (box != null && m3frametime$shouldSkipSpawn(
				(box.minX + box.maxX) * 0.5,
				(box.minY + box.maxY) * 0.5,
				(box.minZ + box.maxZ) * 0.5
			)) {
				particle.markDead();
				ci.cancel();
			} else {
				m3frametime$cachedCount++;
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.PARTICLE);
		}
	}

	@Inject(
		method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$skipEmitter(Entity entity, ParticleEffect parameters, CallbackInfo ci) {
		m3frametime$skipFarEmitter(entity, ci);
	}

	@Inject(
		method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$skipEmitterMaxAge(Entity entity, ParticleEffect parameters, int maxAge, CallbackInfo ci) {
		m3frametime$skipFarEmitter(entity, ci);
	}

	@Unique
	private void m3frametime$skipFarEmitter(Entity entity, CallbackInfo ci) {
		m3frametime$ensureHook();
		SpikeScope.get().push(SpikeScope.Phase.PARTICLE);
		try {
			M3Config cfg = M3FrametimeMod.config();
			if (cfg.farParticleSpawnSkip && RenderCull.fartherThan(entity, cfg.particleCullDistance)) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.PARTICLE);
		}
	}

	@Unique
	private boolean m3frametime$shouldSkipSpawn(double x, double y, double z) {
		M3Config cfg = M3FrametimeMod.config();
		// Both flags gate the same far-distance test; evaluate the (single) squared-distance
		// check once instead of recomputing the camera vector twice.
		if ((cfg.farParticleSpawnSkip || cfg.particleCull) && RenderCull.fartherThan(x, y, z, cfg.particleCullDistance)) {
			return true;
		}
		return cfg.maxParticles >= 0 && m3frametime$cachedCount >= cfg.maxParticles;
	}

	@Unique
	private int m3frametime$particleCount() {
		try {
			ParticleManagerAccessor accessor = (ParticleManagerAccessor) this;
			int count = 0;
			Queue<Particle> incoming = accessor.m3frametime$getNewParticles();
			if (incoming != null) {
				count += incoming.size();
			}
			Map<?, Queue<Particle>> sheets = accessor.m3frametime$getParticles();
			if (sheets != null) {
				for (Queue<Particle> queue : sheets.values()) {
					if (queue != null) {
						count += queue.size();
					}
				}
			}
			return count;
		} catch (Throwable ignored) {
			return 0;
		}
	}

	@Unique
	private static void m3frametime$ensureHook() {
		if (m3frametime$hooked) {
			return;
		}
		ParticleTrim.register(ParticleManagerMixin::m3frametime$trimNow);
		m3frametime$hooked = true;
	}

	@Unique
	private static void m3frametime$trimNow() {
		SpikeScope.get().push(SpikeScope.Phase.PARTICLE_TRIM);
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null || client.particleManager == null) {
				return;
			}
			ParticleManagerAccessor accessor;
			try {
				accessor = (ParticleManagerAccessor) client.particleManager;
			} catch (Throwable ignored) {
				return;
			}
			M3Config cfg = M3FrametimeMod.config();
			int cap = Math.max(0, cfg.maxParticles);
			boolean aggressive = ParticleTrim.lastAggressive();
			int target = aggressive ? Math.max(8, cap / 4) : cap;
			int total = 0;
			Queue<Particle> incoming = accessor.m3frametime$getNewParticles();
			if (incoming != null) {
				total += incoming.size();
			}
			Map<?, Queue<Particle>> sheets = accessor.m3frametime$getParticles();
			if (sheets != null) {
				for (Queue<Particle> queue : sheets.values()) {
					if (queue != null) {
						total += queue.size();
					}
				}
			}
			Queue<?> emitters = accessor.m3frametime$getNewEmitterParticles();
			while (total > target) {
				boolean removed = false;
				if (incoming != null) {
					Particle particle = incoming.poll();
					if (particle != null) {
						particle.markDead();
						total--;
						removed = true;
					}
				}
				if (sheets != null) {
					for (Queue<Particle> queue : sheets.values()) {
						if (total <= target) {
							break;
						}
						if (queue == null) {
							continue;
						}
						Particle particle = queue.poll();
						if (particle != null) {
							particle.markDead();
							total--;
							removed = true;
						}
					}
				}
				if (aggressive && emitters != null) {
					Object emitter = emitters.poll();
					if (emitter instanceof Particle particle) {
						particle.markDead();
						removed = true;
					} else if (emitter != null) {
						removed = true;
					}
				}
				if (!removed) {
					break;
				}
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.PARTICLE_TRIM);
		}
	}
}
