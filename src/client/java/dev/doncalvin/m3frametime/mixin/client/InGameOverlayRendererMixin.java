package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soft-skip first-person screen overlays (fill-rate on Retina GL→Metal).
 * Fire overlay stays unless explicitly opted out — gameplay cue.
 */
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
	@Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private static void m3frametime$skipUnderwater(
		MinecraftClient client,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		CallbackInfo ci
	) {
		if (RamDiscipline.get().skipUnderwaterOverlay()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private static void m3frametime$skipInWall(
		Sprite sprite,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		CallbackInfo ci
	) {
		if (RamDiscipline.get().skipUnderwaterOverlay()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private static void m3frametime$skipFire(
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		CallbackInfo ci
	) {
		if (M3FrametimeMod.config().skipFireOverlay) {
			ci.cancel();
		}
	}
}
