package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.telemetry.SpikeScope;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "hasGlint", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipGlint(CallbackInfoReturnable<Boolean> cir) {
		SpikeScope.get().push(SpikeScope.Phase.GLINT);
		try {
			if (M3FrametimeMod.config().skipItemGlint) {
				cir.setReturnValue(false);
			}
		} finally {
			SpikeScope.get().pop(SpikeScope.Phase.GLINT);
		}
	}
}
