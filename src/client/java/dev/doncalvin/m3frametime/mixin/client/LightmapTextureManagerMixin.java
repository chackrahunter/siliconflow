package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Inject(method = "update", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$throttleLightmap(float delta, CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.LIGHTMAP);
		try {
			if (!M3FrametimeMod.config().lightmapThrottle) {
				return;
			}
			MinecraftClient mc = this.client != null ? this.client : MinecraftClient.getInstance();
			if (mc == null || mc.world == null) {
				return;
			}
			ClientPlayerEntity player = mc.player;
			if (player != null) {
				if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)
					|| player.hasStatusEffect(StatusEffects.DARKNESS)
					|| player.isSubmergedInWater()) {
					return;
				}
			}
			if ((mc.world.getTime() & 1L) != 0L) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.LIGHTMAP);
		}
	}
}
