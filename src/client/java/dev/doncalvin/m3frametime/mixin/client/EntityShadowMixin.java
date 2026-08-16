package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityShadowMixin {
	@Inject(method = "setRenderShadows", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$forceNoShadows(boolean renderShadows, CallbackInfo ci) {
		if (M3FrametimeMod.config().entityShadowSkip && renderShadows) {
			ci.cancel();
		}
	}
}
