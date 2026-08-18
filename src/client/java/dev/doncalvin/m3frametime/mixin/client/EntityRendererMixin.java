package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "hasLabel(Lnet/minecraft/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipNametag(Entity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (M3FrametimeMod.config().skipEntityNametags && !RenderCull.isPlayer(entity)) {
				cir.setReturnValue(false);
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}

	@Inject(
		method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$skipLabel(
		EntityRenderState state,
		Text text,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		int light,
		CallbackInfo ci
	) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (M3FrametimeMod.config().skipEntityNametags && !(state instanceof PlayerEntityRenderState)) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}

	@Inject(
		method = "renderLeash(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/entity/state/EntityRenderState$LeashData;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private static void m3frametime$skipLeash(
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		EntityRenderState.LeashData leashData,
		CallbackInfo ci
	) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (M3FrametimeMod.config().skipLeashes) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}
}
