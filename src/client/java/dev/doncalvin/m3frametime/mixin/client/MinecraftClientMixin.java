package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ChipPower;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.telemetry.GcProbe;
import dev.doncalvin.m3frametime.telemetry.SpikeMonitor;
import dev.doncalvin.m3frametime.telemetry.PerformanceRecorder;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "render", at = @At("HEAD"), require = 1)
	private void m3frametime$beginFrame(boolean tick, CallbackInfo ci) {
		ChipPower.applyOnce();
		SpikeScope.get().resetFrame();
		FramePacer.get().beginFrame();
		FramePacer.get().updateEma(M3FrametimeMod.config().pacingEmaAlpha);
		dev.doncalvin.m3frametime.telemetry.DebugHud.get().onFrameTick();
	}

	@Inject(method = "render", at = @At("RETURN"), require = 1)
	private void m3frametime$endFrame(boolean tick, CallbackInfo ci) {
		GcProbe.get().sampleFrame();
		SpikeMonitor.get().onFrameEnd(FramePacer.get().lastDeltaNanos());
		PerformanceRecorder.get().sampleDiagnostics();
		if (M3FrametimeMod.config().pacingEnabled) {
			FramePacer.get().paceIfNeeded(true);
		}
	}
}
