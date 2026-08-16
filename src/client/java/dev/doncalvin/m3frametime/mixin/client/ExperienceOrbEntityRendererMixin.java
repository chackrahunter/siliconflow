package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ClientDistance;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.entity.ExperienceOrbEntityRenderer;
import net.minecraft.client.render.entity.state.ExperienceOrbEntityRenderState;
import net.minecraft.entity.ExperienceOrbEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Far XP-orb updateRenderState throttle — skips work for far orbs and in shadow passes.
 */
@Mixin(ExperienceOrbEntityRenderer.class)
public abstract class ExperienceOrbEntityRendererMixin {
	@Inject(method = "updateRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$throttleFarOrb(
		ExperienceOrbEntity entity,
		ExperienceOrbEntityRenderState state,
		float tickDelta,
		CallbackInfo ci
	) {
		if (entity == null) {
			return;
		}
		if (StackCompat.isShadowPass()) {
			ci.cancel();
			return;
		}
		if (!M3FrametimeMod.config().farExperienceOrbThrottle) {
			return;
		}
		double lim = RamDiscipline.get().effectiveFarExperienceOrbDistance();
		if (lim <= 0.0 || !ClientDistance.tooFar(entity.getX(), entity.getY(), entity.getZ(), lim)) {
			return;
		}
		// Keep ~1/4 far orb updates.
		if ((entity.age & 3) == 0) {
			return;
		}
		ci.cancel();
	}
}
