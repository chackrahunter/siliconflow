package dev.doncalvin.m3frametime;

import dev.doncalvin.m3frametime.client.ChipPower;
import dev.doncalvin.m3frametime.client.SodiumSoftBooster;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.config.LiveConfigWatcher;
import dev.doncalvin.m3frametime.display.GlfwSync;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.telemetry.DebugHud;
import dev.doncalvin.m3frametime.telemetry.LiveTelemetryStream;
import dev.doncalvin.m3frametime.telemetry.MemoryPressureProbe;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class M3FrametimeClient implements ClientModInitializer {
	private static final String CATEGORY = "category.m3-frametime";

	private static KeyBinding overlayKey;
	private static KeyBinding dashboardKey;
	private static M3Config appliedGraphicsConfig;
	private static String appliedGraphicsProfile;
	private static int sodiumBoostRetryTicks;

	@Override
	public void onInitializeClient() {
		AdaptiveWorkerPool.get();
		MemoryPressureProbe.get().requestSample();
		dev.doncalvin.m3frametime.engine.SiliconCpuTopology.get();
		DebugHud.get().setEnabled(M3FrametimeMod.config().overlayEnabled);

		// Soft-boost Sodium workers (3 builder threads on M3)
		SodiumSoftBooster.applyIfNeeded();

		overlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.m3-frametime.overlay",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_F8,
			CATEGORY
		));

		dashboardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.m3-frametime.dashboard",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_F7,
			CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			DebugHud.get().setEnabled(M3FrametimeMod.config().overlayEnabled);
			while (overlayKey.wasPressed()) {
				DebugHud.get().toggle();
			}
			while (dashboardKey.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new dev.doncalvin.m3frametime.gui.SiliconDashboardScreen(null));
				}
			}
			RamDiscipline.get().onClientTick();

			long now = System.nanoTime();
			LiveTelemetryStream.get().sampleAndStream(now);
			LiveConfigWatcher.get().checkHotReload(now);

			M3Config currentConfig = M3FrametimeMod.config();
			String currentProfile = currentConfig.performanceProfile;
			boolean profileChanged = currentProfile == null
				? appliedGraphicsProfile != null
				: !currentProfile.equals(appliedGraphicsProfile);
			if (appliedGraphicsConfig != currentConfig || profileChanged) {
				applyStartupGraphicsHints(client);
				appliedGraphicsConfig = currentConfig;
				appliedGraphicsProfile = currentProfile;
			}

			// Retry SoftBooster until Sodium options are live; then nudge worker priorities after world load.
			if (!SodiumSoftBooster.applied() && sodiumBoostRetryTicks < 200) {
				sodiumBoostRetryTicks++;
				if ((sodiumBoostRetryTicks & 7) == 0) {
					SodiumSoftBooster.applyIfNeeded();
				}
			}
			if (client.world != null) {
				ChipPower.tryBoostSodiumWorkers();
			}
			if (client.getWindow() != null) {
				long handle = client.getWindow().getHandle();
				FramePacer.get().setRefreshRateHz(GlfwSync.queryRefreshRate(handle));
				GlfwSync.applyIfConfigured(handle);
			}
		});

		// Render F8 Debug HUD directly via HudRenderCallback
		HudRenderCallback.EVENT.register((context, tickCounter) -> DebugHud.get().render(context, tickCounter));

		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.getWindow() != null) {
			long handle = client.getWindow().getHandle();
			FramePacer.get().setRefreshRateHz(GlfwSync.queryRefreshRate(handle));
			GlfwSync.applyIfConfigured(handle);
		}

		var cfg = M3FrametimeMod.config();
		M3FrametimeMod.LOGGER.info(
			"M3 Frametime {} ready | LiveTelemetry=ACTIVE | RuntimeTuner=DISABLED | DarwinQos={} (F8 overlay)",
			cfg.performanceProfile,
			cfg.boostDarwinQos
		);
	}

	private static void applyStartupGraphicsHints(MinecraftClient client) {
		// Deliberately do not mutate Minecraft video options. Iris/Sodium and the user own these settings.
	}

}
