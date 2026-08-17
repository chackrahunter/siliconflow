package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soft-skip rain/snow geometry when Sodium leaves vanilla weather.
 */
@Mixin(WeatherRendering.class)
public abstract class WeatherRenderingMixin {
	@Inject(
		method = "renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void m3frametime$skipPrecipitation(
		World world,
		VertexConsumerProvider vertexConsumers,
		int ticks,
		float tickDelta,
		Vec3d cameraPos,
		CallbackInfo ci
	) {
		if (RamDiscipline.get().skipWeatherGeometry()) {
			ci.cancel();
		}
	}
}
