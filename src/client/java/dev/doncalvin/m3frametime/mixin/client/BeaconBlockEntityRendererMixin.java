package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soft-skip beacon beam translucent geometry and skip in Iris shadow passes. */
@Mixin(BeaconBlockEntityRenderer.class)
public abstract class BeaconBlockEntityRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipBeacon(
		BeaconBlockEntity beacon,
		float tickDelta,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		int light,
		int overlay,
		CallbackInfo ci
	) {
		if (StackCompat.isShadowPass() || RamDiscipline.get().skipBeaconBeams()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "renderBeam(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;FJIII)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private static void m3frametime$skipBeamShort(
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		float tickDelta,
		long worldTime,
		int yOffset,
		int maxY,
		int color,
		CallbackInfo ci
	) {
		if (StackCompat.isShadowPass() || RamDiscipline.get().skipBeaconBeams()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "renderBeam(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/util/Identifier;FFJIIIFF)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private static void m3frametime$skipBeamFull(
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		Identifier texture,
		float tickDelta,
		float heightScale,
		long worldTime,
		int yOffset,
		int maxY,
		int color,
		float innerRadius,
		float outerRadius,
		CallbackInfo ci
	) {
		if (StackCompat.isShadowPass() || RamDiscipline.get().skipBeaconBeams()) {
			ci.cancel();
		}
	}
}
