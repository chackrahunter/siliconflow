package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.compat.IrisCompat;
import dev.doncalvin.m3frametime.config.FrameConfigCache;
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
 * Optional, explicit client-side entity distance/frustum reduction. Shader shadow passes remain untouched.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void m3frametime$frustumCull(
		Entity entity,
		Frustum frustum,
		double x,
		double y,
		double z,
		CallbackInfoReturnable<Boolean> cir
	) {
		FrameConfigCache cfg = FrameConfigCache.get();
		cfg.refresh();
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


		double max = cfg.entityCullDistance;
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
