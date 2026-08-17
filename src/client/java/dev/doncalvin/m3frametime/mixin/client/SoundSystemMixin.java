package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.config.M3Config;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sound system optimization: far positional sound culling.
 */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
	@Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipFarPlay(SoundInstance sound, CallbackInfo ci) {
		M3Config cfg = M3FrametimeMod.config();
		if (!cfg.farSoundSkip || sound == null) {
			return;
		}
		SoundCategory cat = sound.getCategory();
		if (cat == SoundCategory.MASTER
			|| cat == SoundCategory.MUSIC
			|| cat == SoundCategory.RECORDS
			|| cat == SoundCategory.VOICE
			|| cat == SoundCategory.PLAYERS) {
			return;
		}
		if (sound.isRelative() || sound.shouldAlwaysPlay()) {
			return;
		}
		if (ClientDistance.tooFar(sound.getX(), sound.getY(), sound.getZ(), cfg.farSoundDistance)) {
			ci.cancel();
		}
	}
}
