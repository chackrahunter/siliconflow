package dev.doncalvin.m3frametime;

import dev.doncalvin.m3frametime.client.ChipPower;
import dev.doncalvin.m3frametime.client.SodiumSoftBooster;
import dev.doncalvin.m3frametime.config.LiveConfigWatcher;
import dev.doncalvin.m3frametime.display.GlfwSync;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.telemetry.DebugHud;
import dev.doncalvin.m3frametime.telemetry.LiveAutoTuner;
import dev.doncalvin.m3frametime.telemetry.LiveTelemetryStream;
import dev.doncalvin.m3frametime.telemetry.MemoryPressureProbe;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticlesMode;
import org.lwjgl.glfw.GLFW;

public final class M3FrametimeClient implements ClientModInitializer {
	private static final String CATEGORY = "category.m3-frametime";

	private static KeyBinding overlayKey;
	private static boolean appliedGraphicsHints;
	private static int sodiumBoostRetryTicks;

	@Override
	public void onInitializeClient() {
		int cores = Runtime.getRuntime().availableProcessors();
		String fjKey = "java.util.concurrent.ForkJoinPool.common.parallelism";
		if (System.getProperty(fjKey) == null) {
			System.setProperty(fjKey, Integer.toString(Math.max(1, cores - 1)));
		}

		AdaptiveWorkerPool.get();
		MemoryPressureProbe.get().requestSample();

		// Soft-boost Sodium workers (3 builder threads on M3)
		SodiumSoftBooster.applyIfNeeded();

		overlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.m3-frametime.overlay",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_F8,
			CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (overlayKey.wasPressed()) {
				DebugHud.get().toggle();
			}
			RamDiscipline.get().onClientTick();

			long now = System.nanoTime();
			LiveTelemetryStream.get().sampleAndStream(now);
			LiveAutoTuner.get().tick(now);
			LiveConfigWatcher.get().checkHotReload(now);

			if (!appliedGraphicsHints) {
				applyStartupGraphicsHints(client);
				appliedGraphicsHints = true;
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
			"M3 Frametime {} ready | LiveTelemetry=ACTIVE | AutoTuner=ACTIVE | DarwinQos={} (F8 overlay)",
			cfg.performanceProfile,
			cfg.boostDarwinQos
		);
	}

	private static void applyStartupGraphicsHints(MinecraftClient client) {
		if (client == null || client.options == null) {
			return;
		}
		var cfg = M3FrametimeMod.config();
		if (cfg.entityShadowSkip) {
			SimpleOption<Boolean> shadows = client.options.getEntityShadows();
			if (shadows != null && Boolean.TRUE.equals(shadows.getValue())) {
				shadows.setValue(false);
			}
			if (client.getEntityRenderDispatcher() != null) {
				client.getEntityRenderDispatcher().setRenderShadows(false);
			}
		}
		if (cfg.forceFastGraphics) {
			applyFastGraphicsHints(client);
		}
	}

	private static void applyFastGraphicsHints(MinecraftClient client) {
		var cfg = M3FrametimeMod.config();
		SimpleOption<GraphicsMode> graphics = client.options.getGraphicsMode();
		if (graphics != null && graphics.getValue() != GraphicsMode.FAST) {
			graphics.setValue(GraphicsMode.FAST);
		}
		SimpleOption<ParticlesMode> particles = client.options.getParticles();
		if (particles != null && particles.getValue() != ParticlesMode.MINIMAL) {
			particles.setValue(ParticlesMode.MINIMAL);
		}
		SimpleOption<Integer> blend = client.options.getBiomeBlendRadius();
		if (blend != null && blend.getValue() > 0) {
			blend.setValue(0);
		}
		SimpleOption<Boolean> ao = client.options.getAo();
		if (ao != null && Boolean.TRUE.equals(ao.getValue())) {
			ao.setValue(false);
		}
		SimpleOption<Double> entityScale = client.options.getEntityDistanceScaling();
		if (entityScale != null && cfg.entityDistanceScaling > 0.0
			&& entityScale.getValue() > cfg.entityDistanceScaling) {
			entityScale.setValue(cfg.entityDistanceScaling);
		}
		SimpleOption<CloudRenderMode> clouds = client.options.getCloudRenderMode();
		if (clouds != null && clouds.getValue() != CloudRenderMode.OFF) {
			clouds.setValue(CloudRenderMode.OFF);
		}
		SimpleOption<Boolean> bob = client.options.getBobView();
		if (cfg.skipBobView && bob != null && Boolean.TRUE.equals(bob.getValue())) {
			bob.setValue(false);
		}
	}
}
