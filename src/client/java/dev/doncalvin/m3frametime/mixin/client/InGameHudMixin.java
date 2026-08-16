package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soft-skip fullscreen HUD overlays that cost fill-rate on Retina GL→Metal. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipVignette(DrawContext context, Entity entity, CallbackInfo ci) {
		if (RamDiscipline.get().skipVignette()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipNausea(DrawContext context, float nauseaStrength, CallbackInfo ci) {
		if (RamDiscipline.get().skipNauseaOverlay()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$skipSidebar(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
		if (RamDiscipline.get().skipScoreboard()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void m3frametime$skipSidebarTick(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (RamDiscipline.get().skipScoreboard()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipPortal(DrawContext context, float nauseaStrength, CallbackInfo ci) {
		if (RamDiscipline.get().skipPortalOverlay()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (RamDiscipline.get().skipStatusEffectOverlay()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderDemoTimer", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipDemo(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (RamDiscipline.get().skipDemoOverlay()) {
			ci.cancel();
		}
	}
}
