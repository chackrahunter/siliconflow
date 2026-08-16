package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skip lightmap rebuilds when lighting is stable or when Iris shaders manage dynamic lighting.
 * Avoids redundant texture uploads and Metal GPU synchronization on Apple Silicon.
 */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Inject(method = "update", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$throttleLightmap(float delta, CallbackInfo ci) {
		if (!M3FrametimeMod.config().lightmapThrottle || this.client == null) {
			return;
		}
		ClientPlayerEntity player = this.client.player;
		ClientWorld world = this.client.world;
		if (player == null || world == null) {
			return;
		}
		if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)
			|| player.hasStatusEffect(StatusEffects.DARKNESS)
			|| player.hasStatusEffect(StatusEffects.CONDUIT_POWER)
			|| player.isSubmergedInWater()
			|| world.isThundering()) {
			return;
		}

		// When custom shaders are active, shaders handle lighting entirely in GPU GLSL — skip 100% of vanilla texture uploads and heap buffers
		if (StackCompat.isShaderActive()) {
			ci.cancel();
			return;
		}

		long time = world.getTime();
		if ((time & 1L) != 0L) {
			ci.cancel();
		}
	}
}
