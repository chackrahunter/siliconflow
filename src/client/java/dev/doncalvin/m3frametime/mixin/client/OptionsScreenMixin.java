package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.gui.SiliconDashboardScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a first-class entry point to SiliconFlow without covering vanilla Done. */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void m3frametime$addSiliconFlowButton(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;
        int width = 160;
        int x = Math.max(8, screen.width - width - 8);
        int y = 8;
        ((ScreenAccessor) screen).m3frametime$addDrawableChild(ButtonWidget.builder(
                Text.translatable("text.m3-frametime.options_button"),
                button -> MinecraftClient.getInstance().setScreen(new SiliconDashboardScreen(screen)))
            .dimensions(x, y, width, 20)
            .build());
    }
}
