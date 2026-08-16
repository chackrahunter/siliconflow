package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skip enchantment foil on item stacks. Soft require=0.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "hasGlint", at = @At("HEAD"), cancellable = true, require = 0)
	private void m3frametime$skipGlint(CallbackInfoReturnable<Boolean> cir) {
		if (RamDiscipline.get().skipItemGlint()) {
			cir.setReturnValue(false);
		}
	}
}
