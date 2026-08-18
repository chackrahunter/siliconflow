package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipBob(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipBobView);
	}

	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipHurtTilt);
	}

	@Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipFloatingItem(DrawContext context, float tickDelta, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipFloatingItem);
	}

	private static void m3frametime$skip(CallbackInfo ci, boolean skip) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (skip) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}
}
