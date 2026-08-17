package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.display.GlfwSync;
import dev.doncalvin.m3frametime.display.RetinaGuard;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class WindowMixin {
	@Shadow
	@Final
	private long handle;

	@Unique
	private boolean m3frametime$glfwReady;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void m3frametime$onWindowCreated(CallbackInfo ci) {
		if (!m3frametime$glfwReady && this.handle != 0L) {
			GlfwSync.applyIfConfigured(this.handle);
			m3frametime$glfwReady = true;
		}
	}

	/** Integer scale only when retinaGuard opted in — off by default (MAX). */
	@Inject(method = "getScaleFactor", at = @At("RETURN"), cancellable = true)
	private void m3frametime$integerScale(CallbackInfoReturnable<Double> cir) {
		if (!RetinaGuard.enabled() || !RetinaGuard.isMacOs()) {
			return;
		}
		double raw = cir.getReturnValue();
		if (raw <= 0) {
			return;
		}
		int preferred = RetinaGuard.preferIntegerScale(raw);
		if (Math.abs(raw - preferred) > 0.01) {
			cir.setReturnValue((double) preferred);
		}
	}
}
