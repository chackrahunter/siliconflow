package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soft-skip starfield / celestial body draws (sky dome stays). */
@Mixin(SkyRendering.class)
public abstract class SkyRenderingMixin {
	@Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipStars(Fog fog, float starBrightness, MatrixStack matrices, CallbackInfo ci) {
		if (RamDiscipline.get().skipStars()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipCelestial(
		MatrixStack matrices,
		VertexConsumerProvider.Immediate vertexConsumers,
		float rot,
		int moonPhase,
		float alpha,
		float starBrightness,
		Fog fog,
		CallbackInfo ci
	) {
		if (RamDiscipline.get().skipStars()) {
			ci.cancel();
		}
	}
}
