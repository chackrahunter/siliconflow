package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
	@Inject(method = "add", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipAdd(Toast toast, CallbackInfo ci) {
		m3frametime$skip(ci);
	}

	@Inject(method = "draw", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipDraw(DrawContext context, CallbackInfo ci) {
		m3frametime$skip(ci);
	}

	private static void m3frametime$skip(CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.HUD);
		try {
			if (M3FrametimeMod.config().skipToasts) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.HUD);
		}
	}
}
