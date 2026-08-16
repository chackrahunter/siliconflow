package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity render reject: distance gate + frustum early-out + Iris shadow pass sub-pixel culling.
 * Drastically reduces shadow-pass draw calls to prevent Apple Silicon M3 GPU thermal throttling (GPU-040).
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$frustumCull(
		Entity entity,
		Frustum frustum,
		double x,
		double y,
		double z,
		CallbackInfoReturnable<Boolean> cir
	) {
		M3Config cfg = M3FrametimeMod.config();
		if (!cfg.entityCull || entity == null) {
			return;
		}
		if (entity instanceof PlayerEntity) {
			return;
		}
		double dx = entity.getX() - x;
		double dy = entity.getY() - y;
		double dz = entity.getZ() - z;
		double distSq = dx * dx + dy * dy + dz * dz;

		// Iris & Shader shadow map pass: skip non-shadow casters & sub-pixel small entities
		if (cfg.optimizeShadowPass && StackCompat.isShadowPass()) {
			// Dropped items, XP orbs, projectiles, and item frames cast negligible shadows > 12m
			if (distSq > 144.0 && (entity instanceof ItemEntity
				|| entity instanceof ExperienceOrbEntity
				|| entity instanceof PersistentProjectileEntity
				|| entity instanceof ItemFrameEntity)) {
				cir.setReturnValue(false);
				return;
			}

			// Small creatures cast sub-pixel shadows beyond 12 meters
			if (distSq > 144.0 && (entity instanceof BatEntity
				|| entity instanceof FishEntity
				|| entity instanceof BeeEntity
				|| entity instanceof ChickenEntity
				|| entity instanceof SilverfishEntity
				|| entity instanceof EndermiteEntity)) {
				cir.setReturnValue(false);
				return;
			}
			double shadowDist = cfg.shadowEntityDistance;
			if (distSq > shadowDist * shadowDist) {
				cir.setReturnValue(false);
				return;
			}
		}

		double max = RamDiscipline.get().effectiveEntityCullDistance();
		if (distSq > max * max) {
			cir.setReturnValue(false);
			return;
		}
		if (StackCompat.useAggressiveEntityFrustum() && frustum != null) {
			Box box = entity.getBoundingBox();
			if (!frustum.isVisible(box)) {
				cir.setReturnValue(false);
			}
		}
	}
}
