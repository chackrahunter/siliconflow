package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipClouds(
		int color,
		CloudRenderMode cloudRenderMode,
		float cloudHeight,
		Matrix4f modelViewMatrix,
		Matrix4f projectionMatrix,
		Vec3d cameraPos,
		float ticks,
		CallbackInfo ci
	) {
		if (RamDiscipline.get().skipClouds()) {
			ci.cancel();
		}
	}
}
