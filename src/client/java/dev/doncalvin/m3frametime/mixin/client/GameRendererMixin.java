package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soft-skip view bobbing / hurt tilt / floating-item pop (every-frame or burst cost). */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipBob(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (RamDiscipline.get().skipBobView()) {
			ci.cancel();
		}
	}

	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipHurtTilt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (RamDiscipline.get().skipHurtTilt()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipFloatingItem(DrawContext context, float tickDelta, CallbackInfo ci) {
		if (RamDiscipline.get().skipFloatingItem()) {
			ci.cancel();
		}
	}
}
