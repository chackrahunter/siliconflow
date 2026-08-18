package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.IrisCompat;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(
		method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$shouldRender(
		Entity entity,
		Frustum frustum,
		double x,
		double y,
		double z,
		CallbackInfoReturnable<Boolean> cir
	) {
		boolean inShadowNow = m3frametime$latchShadowPass();
		SpikeScope scope = SpikeScope.get();
		scope.push(inShadowNow ? SpikeScope.Phase.SHADOW_ENTITY : SpikeScope.Phase.ENTITY_TICK);
		try {
			if (entity == null || RenderCull.isPlayer(entity)) {
				return;
			}
			M3Config cfg = M3FrametimeMod.config();
			if (cfg.optimizeShadowPass && inShadowNow && IrisCompat.isShadowPass()) {
				if (cfg.shadowEntityDistance <= 0.0 || RenderCull.fartherThan(entity, cfg.shadowEntityDistance)) {
					cir.setReturnValue(false);
				}
				return;
			}
			if (cfg.entityCull && RenderCull.fartherThan(entity, cfg.entityCullDistance)) {
				cir.setReturnValue(false);
				return;
			}
			boolean distanceOnly = StackCompat.sodium() && !StackCompat.useAggressiveEntityFrustum();
			if (!distanceOnly && frustum != null && !frustum.isVisible(entity.getBoundingBox())) {
				cir.setReturnValue(false);
			}
		} finally {
			scope.pop(inShadowNow ? SpikeScope.Phase.SHADOW_ENTITY : SpikeScope.Phase.ENTITY_TICK);
		}
	}

	@Inject(
		method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
		at = @At("HEAD"),
		require = 0
	)
	private void m3frametime$latchShadowRender(CallbackInfo ci) {
		m3frametime$latchShadowPass();
	}

	@Inject(
		method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
		at = @At("HEAD"),
		require = 0
	)
	private void m3frametime$latchShadowRenderWithRenderer(CallbackInfo ci) {
		m3frametime$latchShadowPass();
	}

	@Inject(
		method = "renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/entity/state/EntityRenderState;FFLnet/minecraft/world/WorldView;F)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private static void m3frametime$skipShadow(
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		EntityRenderState state,
		float opacity,
		float tickDelta,
		WorldView world,
		float radius,
		CallbackInfo ci
	) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (!M3FrametimeMod.config().entityShadowSkip) {
				return;
			}
			if (state instanceof PlayerEntityRenderState) {
				return;
			}
			ci.cancel();
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}

	@Unique
	private static boolean m3frametime$latchShadowPass() {
		boolean live = IrisCompat.queryLiveShadowPass();
		IrisCompat.setShadowPass(live);
		return live;
	}
}
