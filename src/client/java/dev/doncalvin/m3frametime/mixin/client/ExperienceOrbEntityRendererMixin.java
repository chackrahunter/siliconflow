package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ExperienceOrbEntityRenderer;
import net.minecraft.client.render.entity.state.ExperienceOrbEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ExperienceOrbEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntityRenderer.class)
public abstract class ExperienceOrbEntityRendererMixin {
	@Inject(
		method = "updateRenderState(Lnet/minecraft/entity/ExperienceOrbEntity;Lnet/minecraft/client/render/entity/state/ExperienceOrbEntityRenderState;F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/EntityRenderer;updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
			shift = At.Shift.AFTER
		),
		cancellable = true,
		require = 0
	)
	private void m3frametime$throttleOrbState(ExperienceOrbEntity entity, ExperienceOrbEntityRenderState state, float tickDelta, CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.ENTITY_TICK);
		try {
			M3Config cfg = M3FrametimeMod.config();
			if (cfg.farExperienceOrbThrottle && RenderCull.fartherThan(entity, cfg.farExperienceOrbDistance)) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.ENTITY_TICK);
		}
	}

	@Inject(
		method = "render(Lnet/minecraft/client/render/entity/state/ExperienceOrbEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$throttleOrbRender(ExperienceOrbEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.ENTITY_TICK);
		try {
			M3Config cfg = M3FrametimeMod.config();
			if (cfg.farExperienceOrbThrottle && RenderCull.fartherThan(state.x, state.y, state.z, cfg.farExperienceOrbDistance)) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.ENTITY_TICK);
		}
	}
}
