package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * World render optimizations + soft-skip weather particles/geometry.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
	@Inject(method = "addWeatherParticlesAndSound", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipWeather(Camera camera, CallbackInfo ci) {
		if (RamDiscipline.get().skipWeatherParticles()) {
			ci.cancel();
		}
	}

	/** Rain/snow strip pass — leftover when Sodium keeps vanilla weather. Soft require. */
	@Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipWeatherGeometry(
		net.minecraft.client.render.FrameGraphBuilder frameGraphBuilder,
		net.minecraft.util.math.Vec3d cameraPos,
		float tickDelta,
		net.minecraft.client.render.Fog fog,
		CallbackInfo ci
	) {
		if (RamDiscipline.get().skipWeatherGeometry()) {
			ci.cancel();
		}
	}
}
