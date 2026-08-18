package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererGlintMixin {
	@Inject(
		method = "getItemGlintConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/RenderLayer;ZZ)Lnet/minecraft/client/render/VertexConsumer;",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private static void m3frametime$skipItemGlint(
		VertexConsumerProvider vertexConsumers,
		RenderLayer layer,
		boolean solid,
		boolean glint,
		CallbackInfoReturnable<VertexConsumer> cir
	) {
		SpikeScope.get().push(SpikeScope.Phase.GLINT);
		try {
			if (M3FrametimeMod.config().skipItemGlint && glint) {
				cir.setReturnValue(vertexConsumers.getBuffer(layer));
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.GLINT);
		}
	}

	@Inject(
		method = "getArmorGlintConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/RenderLayer;Z)Lnet/minecraft/client/render/VertexConsumer;",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private static void m3frametime$skipArmorGlint(
		VertexConsumerProvider provider,
		RenderLayer layer,
		boolean glint,
		CallbackInfoReturnable<VertexConsumer> cir
	) {
		SpikeScope.get().push(SpikeScope.Phase.GLINT);
		try {
			if (M3FrametimeMod.config().skipItemGlint && glint) {
				cir.setReturnValue(provider.getBuffer(layer));
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.GLINT);
		}
	}
}
