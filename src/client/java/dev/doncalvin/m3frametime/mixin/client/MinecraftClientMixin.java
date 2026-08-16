package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ChipPower;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.client.DarwinQos;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.telemetry.GcProbe;
import dev.doncalvin.m3frametime.telemetry.SpikeMonitor;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Unique
	private int m3frametime$prioTick;

	@Inject(method = "render", at = @At("HEAD"), require = 0)
	private void m3frametime$beginFrame(boolean tick, CallbackInfo ci) {
		ChipPower.applyOnce();
		// Re-affirm Mach QoS every ~128 frames (~1 second) to permanently prevent P->E core migration
		if ((++m3frametime$prioTick & 127) == 0 && M3FrametimeMod.config().boostDarwinQos) {
			DarwinQos.boostRenderThread();
		}
		ClientDistance.invalidate();
		SpikeScope.get().resetFrame();
		FramePacer.get().beginFrame();
		FramePacer.get().updateEma(M3FrametimeMod.config().pacingEmaAlpha);
	}

	@Inject(method = "render", at = @At("RETURN"), require = 0)
	private void m3frametime$endFrame(boolean tick, CallbackInfo ci) {
		GcProbe.get().sampleFrame();
		SpikeMonitor.get().onFrameEnd(FramePacer.get().lastDeltaNanos());
		if (M3FrametimeMod.config().pacingEnabled) {
			FramePacer.get().paceIfNeeded(true);
		}
	}
}
