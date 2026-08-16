package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skip most far-entity render-state updates + nametags + leash ribbons (visual only). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "updateRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$throttleFarState(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
		if (!M3FrametimeMod.config().entityCull || entity == null || entity instanceof PlayerEntity) {
			return;
		}
		double max = RamDiscipline.get().effectiveEntityCullDistance() * 0.5;
		if (!ClientDistance.tooFar(entity.getX(), entity.getY(), entity.getZ(), max)) {
			return;
		}
		// Update ~1/8 of far entities per tick — leftover win Sodium does not own.
		if ((entity.age & 7) == 0) {
			return;
		}
		ci.cancel();
	}

	@Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipNametag(Entity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
		if (!M3FrametimeMod.config().skipEntityNametags || entity == null || entity instanceof PlayerEntity) {
			return;
		}
		cir.setReturnValue(false);
	}

	@Inject(method = "renderLeash", at = @At("HEAD"), cancellable = true, require = 0)
	private static void m3frametime$skipLeash(
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		EntityRenderState.LeashData leashData,
		CallbackInfo ci
	) {
		if (RamDiscipline.get().skipLeashes()) {
			ci.cancel();
		}
	}
}
