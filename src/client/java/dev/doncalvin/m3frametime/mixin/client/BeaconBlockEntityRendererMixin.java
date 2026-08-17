package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soft-skip beacon beam translucent geometry and skip in Iris shadow passes. */
@Mixin(BeaconBlockEntityRenderer.class)
public abstract class BeaconBlockEntityRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void m3frametime$skipBeacon(
		BeaconBlockEntity beacon,
		float tickDelta,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		int light,
		int overlay,
		CallbackInfo ci
	) {
		if (RamDiscipline.get().skipBeaconBeams()) {
			ci.cancel();
		}
	}
}
