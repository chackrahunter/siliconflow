package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.cull.RenderCull;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
	@Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipFarSound(SoundInstance sound, CallbackInfo ci) {
		m3frametime$maybeSkip(sound, ci);
	}

	@Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipFarDelayedSound(SoundInstance sound, int delay, CallbackInfo ci) {
		m3frametime$maybeSkip(sound, ci);
	}

	@Unique
	private static void m3frametime$maybeSkip(SoundInstance sound, CallbackInfo ci) {
		SpikeScope.get().push(SpikeScope.Phase.SOUND);
		try {
			M3Config cfg = M3FrametimeMod.config();
			if (!cfg.farSoundSkip || sound == null) {
				return;
			}
			if (sound.isRelative() || sound.getAttenuationType() == SoundInstance.AttenuationType.NONE) {
				return;
			}
			SoundCategory category = sound.getCategory();
			if (category == SoundCategory.MASTER
				|| category == SoundCategory.MUSIC
				|| category == SoundCategory.VOICE
				|| category == SoundCategory.PLAYERS) {
				return;
			}
			if (RenderCull.fartherThan(sound.getX(), sound.getY(), sound.getZ(), cfg.farSoundDistance)) {
				ci.cancel();
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.SOUND);
		}
	}
}
