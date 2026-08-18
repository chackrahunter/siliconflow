package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntityRenderer.class)
public abstract class BannerPatternMixin {
	@Unique
	private static final ThreadLocal<Boolean> STRIP_PATTERNS = ThreadLocal.withInitial(() -> Boolean.FALSE);

	@Inject(
		method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
		at = @At("HEAD"),
		require = 0
	)
	private void m3frametime$markFarBanner(
		BannerBlockEntity banner,
		float tickDelta,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		int light,
		int overlay,
		CallbackInfo ci
	) {
		M3Config cfg = M3FrametimeMod.config();
		STRIP_PATTERNS.set(cfg.skipFarBannerPatterns && RenderCull.fartherThan(banner.getPos(), cfg.farBannerDistance));
	}

	@Inject(
		method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
		at = @At("RETURN"),
		require = 0
	)
	private void m3frametime$clearFarBanner(CallbackInfo ci) {
		STRIP_PATTERNS.set(Boolean.FALSE);
	}

	@ModifyVariable(
		method = "renderCanvas(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/SpriteIdentifier;ZLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;)V",
		at = @At("HEAD"),
		argsOnly = true,
		require = 0
	)
	private static BannerPatternsComponent m3frametime$stripCanvas(BannerPatternsComponent patterns) {
		return m3frametime$maybeStrip(patterns);
	}

	@ModifyVariable(
		method = "renderCanvas(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/SpriteIdentifier;ZLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;ZZ)V",
		at = @At("HEAD"),
		argsOnly = true,
		require = 0
	)
	private static BannerPatternsComponent m3frametime$stripCanvasGlint(BannerPatternsComponent patterns) {
		return m3frametime$maybeStrip(patterns);
	}

	@Unique
	private static BannerPatternsComponent m3frametime$maybeStrip(BannerPatternsComponent patterns) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (Boolean.TRUE.equals(STRIP_PATTERNS.get())) {
				return BannerPatternsComponent.DEFAULT;
			}
			return patterns;
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}
}
