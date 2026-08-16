package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;
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
 * Particle hard budget + fast O(1) cached counter + distance skip + Iris shadow pass culling.
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
		M3Config cfg = M3FrametimeMod.config();
		int cap = RamDiscipline.get().effectiveMaxParticles();
		return cfg.particleCull && cap > 0 && m3frametime$countParticles() >= cap;
	}

	@Unique
	private boolean m3frametime$tooFar(double x, double y, double z) {
		M3Config cfg = M3FrametimeMod.config();
		double limDist = RamDiscipline.get().effectiveParticleCullDistance();
		if (!cfg.particleCull || limDist <= 0) {
			return false;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.gameRenderer == null || client.gameRenderer.getCamera() == null) {
			return false;
		}
		Vec3d cam = client.gameRenderer.getCamera().getPos();
		double dx = x - cam.x;
		double dy = y - cam.y;
		double dz = z - cam.z;
		double lim = limDist * limDist;
		return dx * dx + dy * dy + dz * dz > lim;
	}

	/** Mark excess particles dead down to effective cap (no System.gc). */
	@Unique
	private void m3frametime$trimToCap() {
		M3Config cfg = M3FrametimeMod.config();
		int cap = RamDiscipline.get().effectiveMaxParticles();
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

	@Inject(method = "tick", at = @At("HEAD"), require = 0)
	private void m3frametime$trimUnderPressure(CallbackInfo ci) {
		RamDiscipline ram = RamDiscipline.get();
		if (ram.consumeParticleTrimRequest() || m3frametime$overBudget()) {
			m3frametime$trimToCap();
		}
	}

	@Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$budgetAdd(Particle particle, CallbackInfo ci) {
		if (m3frametime$overBudget()) {
			particle.markDead();
			ci.cancel();
		} else {
			m3frametime$cachedCount++;
		}
	}

	@Inject(
		method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
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

	@Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$particlesBegin(
		Camera camera,
		float tickDelta,
		VertexConsumerProvider.Immediate immediate,
		CallbackInfo ci
	) {
		if (M3FrametimeMod.config().optimizeShadowPass && StackCompat.isShadowPass()) {
			ci.cancel();
		}
	}
}
