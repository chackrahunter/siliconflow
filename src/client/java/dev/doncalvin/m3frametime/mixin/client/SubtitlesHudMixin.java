package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.SubtitlesHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soft-skip accessibility subtitles HUD — audible-entry list churn + text layout.
 * Exact-target injection; does not touch ImmediatelyFast font batching.
 */
@Mixin(SubtitlesHud.class)
public abstract class SubtitlesHudMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipSubtitles(DrawContext context, CallbackInfo ci) {
		if (RamDiscipline.get().skipSubtitles()) {
			ci.cancel();
		}
	}
}
