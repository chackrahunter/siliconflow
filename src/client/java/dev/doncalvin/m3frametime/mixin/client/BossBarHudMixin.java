package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional soft-skip for boss-bar HUD. */
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipBossBar(DrawContext context, CallbackInfo ci) {
		if (RamDiscipline.get().skipBossBar()) {
			ci.cancel();
		}
	}
}
