package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Force glint consumers off when skipItemGlint — covers call sites that
 * pass an explicit glint boolean instead of consulting ItemStack.hasGlint.
 * Soft require=0; does not touch ImmediatelyFast batching.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererGlintMixin {
	@ModifyVariable(
		method = "getItemGlintConsumer",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 1,
		require = 0
	)
	private static boolean m3frametime$forceItemGlintOff(boolean glint) {
		return glint && !RamDiscipline.get().skipItemGlint();
	}

	@ModifyVariable(
		method = "getArmorGlintConsumer",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0,
		require = 0
	)
	private static boolean m3frametime$forceArmorGlintOff(boolean glint) {
		return glint && !RamDiscipline.get().skipItemGlint();
	}
}
