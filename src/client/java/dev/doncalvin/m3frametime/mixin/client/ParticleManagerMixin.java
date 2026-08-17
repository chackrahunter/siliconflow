package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.compat.IrisCompat;
import dev.doncalvin.m3frametime.config.FrameConfigCache;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

/**
 * Particle hard budget + cached queue count + optional distance skip.
 * Vanilla particle scheduling remains otherwise unchanged.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
	@Shadow
	@Final
	private Map<ParticleTextureSheet, Queue<Particle>> particles;

	@Unique
	private int m3frametime$cachedCount;
	@Unique
	private int m3frametime$lastCountTick = -1;

	@Unique
	private int m3frametime$countParticles() {
		MinecraftClient client = MinecraftClient.getInstance();
		int tick = (client != null && client.world != null) ? (int) (client.world.getTime() & 0x7FFFFFFF) : 0;
		if (tick != m3frametime$lastCountTick) {
			m3frametime$lastCountTick = tick;
			int n = 0;
			for (Queue<Particle> q : this.particles.values()) {
				n += q.size();
			}
			m3frametime$cachedCount = n;
		}
		return m3frametime$cachedCount;
	}

	@Unique
	private boolean m3frametime$overBudget() {
		FrameConfigCache cfg = FrameConfigCache.get();
		cfg.refresh();
		return cfg.particleCull && cfg.maxParticles > 0 && m3frametime$countParticles() >= cfg.maxParticles;
	}

	@Unique
	private boolean m3frametime$tooFar(double x, double y, double z) {
		FrameConfigCache cfg = FrameConfigCache.get();
		cfg.refresh();
		if (!cfg.particleCull || cfg.particleCullDistance <= 0.0) {
			return false;
		}
		return ClientDistance.tooFar(x, y, z, cfg.particleCullDistance);
	}

	@Unique
	private int m3frametime$countType(ParticleTextureSheet sheet) {
		Queue<Particle> queue = this.particles.get(sheet);
		return queue != null ? queue.size() : 0;
	}

	/**
	 * Per-type soft budget ratios of the global maxParticles cap.
	 * TERRAIN_SHEET is the most expensive (vertex-heavy), NO_RENDER the cheapest.
	 * ParticleTextureSheet is a Record; name() returns the static field name.
	 */
	@Unique
	private int m3frametime$typeBudget(String sheetType) {
		int max = RamDiscipline.get().effectiveMaxParticles();
		if (max <= 0) {
			return Integer.MAX_VALUE;
		}
		return switch (sheetType) {
			case "TERRAIN_SHEET" -> (int) (max * 0.40);
			case "PARTICLE_SHEET_TRANSLUCENT" -> (int) (max * 0.30);
			case "PARTICLE_SHEET_OPAQUE" -> (int) (max * 0.20);
			case "CUSTOM" -> (int) (max * 0.05);
			case "NO_RENDER" -> (int) (max * 0.05);
			default -> (int) (max * 0.10);
		};
	}

	@Unique
	private boolean m3frametime$overTypeBudget(ParticleTextureSheet sheet) {
		if (sheet == null) {
			return false;
		}
		int count = m3frametime$countType(sheet);
		int max = RamDiscipline.get().effectiveMaxParticles();
		if (max <= 0) {
			return false;
		}
		int limit;
		if (sheet == ParticleTextureSheet.TERRAIN_SHEET) {
			limit = (int) (max * 0.40);
		} else if (sheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
			limit = (int) (max * 0.30);
		} else if (sheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE) {
			limit = (int) (max * 0.20);
		} else if (sheet == ParticleTextureSheet.CUSTOM) {
			limit = (int) (max * 0.05);
		} else {
			limit = (int) (max * 0.05);
		}
		return count >= limit;
	}

	/** Mark excess particles dead down to effective cap (no the JVM collector). */
	@Unique
	private void m3frametime$trimToCap() {
		FrameConfigCache cfg = FrameConfigCache.get();
		cfg.refresh();
		int cap = cfg.maxParticles;
		if (!cfg.particleCull || cap <= 0) {
			return;
		}
		int count = m3frametime$countParticles();
		if (count <= cap) {
			return;
		}
		int excess = count - cap;
		for (Queue<Particle> q : this.particles.values()) {
			Iterator<Particle> it = q.iterator();
			while (it.hasNext() && excess > 0) {
				Particle p = it.next();
				p.markDead();
				it.remove();
				excess--;
			}
			if (excess <= 0) {
				break;
			}
		}
		m3frametime$cachedCount = Math.max(0, count - (count - cap));
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void m3frametime$trimUnderPressure(CallbackInfo ci) {
		RamDiscipline ram = RamDiscipline.get();
		if (ram.consumeParticleTrimRequest() || m3frametime$overBudget()) {
			m3frametime$trimToCap();
		}
	}

	@Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
	private void m3frametime$budgetAdd(Particle particle, CallbackInfo ci) {
		FrameConfigCache cfg = FrameConfigCache.get();
		cfg.refresh();
		if (!cfg.particleCull) {
			return;
		}

		if (m3frametime$overBudget()) {
			particle.markDead();
			ci.cancel();
			return;
		}


	}

	@Inject(
		method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void m3frametime$budgetAddEffect(
		ParticleEffect parameters,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ,
		CallbackInfoReturnable<Particle> cir
	) {
		if (m3frametime$overBudget() || m3frametime$tooFar(x, y, z)) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true)
	private void m3frametime$particlesBegin(
		Camera camera,
		float tickDelta,
		VertexConsumerProvider.Immediate immediate,
		CallbackInfo ci
	) {
		FrameConfigCache cfg = FrameConfigCache.get();
		cfg.refresh();
	}
}
