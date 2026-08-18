package dev.doncalvin.m3frametime;

import dev.doncalvin.m3frametime.client.ChipPower;
import dev.doncalvin.m3frametime.compat.IrisCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.config.LiveConfigWatcher;
import dev.doncalvin.m3frametime.engine.RamClassPolicy;
import dev.doncalvin.m3frametime.engine.ShaderAutoThrottle;
import dev.doncalvin.m3frametime.engine.SiliconCpuTopology;
import dev.doncalvin.m3frametime.gui.SiliconDashboardScreen;
import dev.doncalvin.m3frametime.telemetry.DebugHud;
import dev.doncalvin.m3frametime.telemetry.LiveTelemetryStream;
import dev.doncalvin.m3frametime.telemetry.MemoryPressureProbe;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class M3FrametimeClient implements ClientModInitializer {
	private static final String CATEGORY = "category.m3-frametime";

	private static KeyBinding overlayKey;
	private static KeyBinding dashboardKey;
	private static M3Config appliedGraphicsConfig;
	private static String appliedGraphicsProfile;
	private static boolean renderPriorityArmed;

	@Override
	public void onInitializeClient() {
		AdaptiveWorkerPool.get();
		MemoryPressureProbe.get().requestSample();
		SiliconCpuTopology.get();
		RamClassPolicy.applySessionCaps(M3FrametimeMod.config());
		ShaderAutoThrottle.get().onUserConfigMayHaveChanged(M3FrametimeMod.config());
		DebugHud.get().setEnabled(M3FrametimeMod.config().overlayEnabled);

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
					client.setScreen(new SiliconDashboardScreen(null));
				}
			}

			if (!renderPriorityArmed) {
				renderPriorityArmed = true;
				ChipPower.reinforceRenderPriority();
				ChipPower.tryBoostSodiumWorkers();
			}

			M3Config currentConfig = M3FrametimeMod.config();
			String currentProfile = currentConfig.performanceProfile;
			boolean profileChanged = currentProfile == null
				? appliedGraphicsProfile != null
				: !currentProfile.equals(appliedGraphicsProfile);
			if (appliedGraphicsConfig != currentConfig || profileChanged) {
				// Video options stay user/Iris/Sodium-owned; we only refresh the shader throttle.
				ShaderAutoThrottle.get().onUserConfigMayHaveChanged(currentConfig);
				appliedGraphicsConfig = currentConfig;
				appliedGraphicsProfile = currentProfile;
			}

			IrisCompat.onClientTick();
			RamDiscipline.get().onClientTick();
			ShaderAutoThrottle.get().onClientTick();

			long now = System.nanoTime();
			LiveTelemetryStream.get().sampleAndStream(now);
			LiveConfigWatcher.get().checkHotReload(now);

		});

		// Render F8 Debug HUD directly via HudRenderCallback
		HudRenderCallback.EVENT.register((context, tickCounter) -> DebugHud.get().render(context, tickCounter));


		var cfg = M3FrametimeMod.config();
		M3FrametimeMod.LOGGER.info(
			"M3 Frametime {} ready | LiveTelemetry=LOCAL_OPT_IN | ramClass={}GB | external owners unchanged (F8 overlay)",
			cfg.performanceProfile,
			SiliconCpuTopology.get().getRamClassGb()
		);
	}

}
