package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
	@Inject(
		method = "renderClouds(ILnet/minecraft/client/option/CloudRenderMode;FLorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/util/math/Vec3d;F)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$skipClouds(
		int color,
		CloudRenderMode cloudRenderMode,
		float cloudHeight,
		Matrix4f positionMatrix,
		Matrix4f projectionMatrix,
		Vec3d cameraPos,
		float ticks,
		CallbackInfo ci
	) {
		m3frametime$cancel(ci);
	}

	@Inject(
		method = "renderClouds(Lnet/minecraft/client/render/RenderLayer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FFF)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$skipCloudLayer(RenderLayer layer, Matrix4f positionMatrix, Matrix4f projectionMatrix, float x, float y, float z, CallbackInfo ci) {
		m3frametime$cancel(ci);
	}

	private static void m3frametime$cancel(CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (M3FrametimeMod.config().skipClouds) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}
}
