package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.SpikeMonitor;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Appends last stutter code and heap vs physical UMA to vanilla F3.
 * Intentionally not the F8 overlay strip.
 */
@Mixin(DebugHud.class)
public abstract class VanillaDebugHudMixin {
	@Inject(method = "getLeftText", at = @At("RETURN"), require = 1)
	private void m3frametime$appendStutterInfo(CallbackInfoReturnable<List<String>> cir) {
		List<String> lines = cir.getReturnValue();
		if (lines == null) {
			return;
		}
		SpikeMonitor.get().appendF3Lines(lines);
	}
}
