package dev.doncalvin.m3frametime.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * High-Performance OpenGL & Metal State Deduplicator.
 * Eliminates redundant texture bindings and state toggles before issuing JNI calls to the macOS driver,
 * reducing CPU driver overhead and preventing Apple Silicon Metal TBDR pipeline stalls.
 */
@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
	@Unique
	private static int m3frametime$lastBoundTexture = -1;
	@Unique
	private static boolean m3frametime$depthMaskState = true;

	@Inject(method = "_bindTexture", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private static void m3frametime$dedupBindTexture(int texture, CallbackInfo ci) {
		if (texture == m3frametime$lastBoundTexture) {
			ci.cancel();
		} else {
			m3frametime$lastBoundTexture = texture;
		}
	}

	@Inject(method = "_depthMask", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
	private static void m3frametime$dedupDepthMask(boolean mask, CallbackInfo ci) {
		if (mask == m3frametime$depthMaskState) {
			ci.cancel();
		} else {
			m3frametime$depthMaskState = mask;
		}
	}
}
