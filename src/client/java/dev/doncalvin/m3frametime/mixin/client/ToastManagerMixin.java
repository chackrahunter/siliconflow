package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipAdd(Toast toast, CallbackInfo ci) {
		if (M3FrametimeMod.config().skipToasts) {
			ci.cancel();
		}
	}

	@Inject(method = "draw", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipDraw(DrawContext context, CallbackInfo ci) {
		if (M3FrametimeMod.config().skipToasts) {
			ci.cancel();
		}
	}
}
