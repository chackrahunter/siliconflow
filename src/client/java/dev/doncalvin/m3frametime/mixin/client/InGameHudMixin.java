package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipVignette(DrawContext context, Entity entity, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipVignette);
	}

	@Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipNausea(DrawContext context, float nauseaStrength, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipNauseaOverlay);
	}

	@Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipScoreboard);
	}

	@Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipScoreboardHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipScoreboard);
	}

	@Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipPortal(DrawContext context, float nauseaStrength, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipPortalOverlay);
	}

	@Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipStatusEffectOverlay);
	}

	@Inject(method = "renderDemoTimer", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipDemo(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		m3frametime$skip(ci, M3FrametimeMod.config().skipDemoOverlay);
	}

	@Unique
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
