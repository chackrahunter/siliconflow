package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
	@Inject(
		method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/EntityRenderer;updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
			shift = At.Shift.AFTER
		),
		cancellable = true,
		require = 0
	)
	private void m3frametime$throttleItemState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.ENTITY_TICK);
		try {
			M3Config cfg = M3FrametimeMod.config();
			if (cfg.farItemEntityThrottle && RenderCull.fartherThan(entity, cfg.farItemEntityDistance)) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.ENTITY_TICK);
		}
	}

	@Inject(
		method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$throttleItemRender(ItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.ENTITY_TICK);
		try {
			M3Config cfg = M3FrametimeMod.config();
			if (cfg.farItemEntityThrottle && RenderCull.fartherThan(state.x, state.y, state.z, cfg.farItemEntityDistance)) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.ENTITY_TICK);
		}
	}
}
