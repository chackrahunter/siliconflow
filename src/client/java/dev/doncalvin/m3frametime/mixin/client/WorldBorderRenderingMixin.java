package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.WorldBorderRendering;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soft-skip world-border wall quads (rare but fill-rate heavy when near). */
@Mixin(WorldBorderRendering.class)
public abstract class WorldBorderRenderingMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipBorder(WorldBorder border, Vec3d cameraPos, double viewDistance, double farPlane, CallbackInfo ci) {
		if (RamDiscipline.get().skipWorldBorder()) {
			ci.cancel();
		}
	}
}
