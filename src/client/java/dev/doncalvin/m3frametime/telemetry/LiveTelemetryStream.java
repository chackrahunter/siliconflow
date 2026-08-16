package dev.doncalvin.m3frametime.telemetry;

import com.google.gson.JsonObject;
import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Live Flight Recorder & Real-time Telemetry Stream.
 * Periodically writes live JSON diagnostics to disk (run/m3-live-telemetry.json or config/m3-live-telemetry.json)
 * enabling real-time remote inspection of GPU frametimes, P-Core QoS, and stutter error codes.
 */
public final class LiveTelemetryStream {
	private static final LiveTelemetryStream INSTANCE = new LiveTelemetryStream();
	private static final Path LOG_PATH = FabricLoader.getInstance().getGameDir().resolve("m3-live-telemetry.json");

	private long lastWriteNanos;
	private volatile boolean writing;

	private LiveTelemetryStream() {}

	public static LiveTelemetryStream get() {
		return INSTANCE;
	}

	public void sampleAndStream(long nowNanos) {
		if (writing || !M3FrametimeMod.config().spikeLogging || nowNanos - lastWriteNanos < 2_000_000_000L) { // Stream only when enabled at 0.5 Hz
			return;
		}
		lastWriteNanos = nowNanos;
		writing = true;

		SpikeMonitor monitor = SpikeMonitor.get();
		FramePacer pacer = FramePacer.get();
		MemoryPressureProbe mem = MemoryPressureProbe.get();
		Runtime rt = Runtime.getRuntime();

		double ftMs = pacer.lastDeltaMs();
		double emaMs = pacer.emaDeltaMs();
		int fps = ftMs > 0 ? (int) Math.round(1000.0 / ftMs) : 0;
		int minFps = monitor.rollingMinFps();
		int maxFps = monitor.rollingMaxFps();
		int p50Fps = monitor.percentileMs(0.50) > 0 ? (int) Math.round(1000.0 / monitor.percentileMs(0.50)) : 0;
		int p99Fps = monitor.percentileMs(0.99) > 0 ? (int) Math.round(1000.0 / monitor.percentileMs(0.99)) : 0;

		StutterErrorCode lastCode = monitor.lastErrorCode();
		long lastSpikeAge = monitor.lastSpikeAgeMs();
		double lastSpikeMs = monitor.lastSpikeMs();

		long heapUsedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
		long heapMaxMb = rt.maxMemory() / (1024L * 1024L);
		long freePhysMb = mem.freePhysicalMb();

		boolean shaderActive = StackCompat.isShaderActive();
		boolean shadowPass = StackCompat.isShadowPass();

		AdaptiveWorkerPool.get().execute(() -> {
			try {
				JsonObject json = new JsonObject();
				json.addProperty("timestamp", System.currentTimeMillis());
				
				JsonObject fpsObj = new JsonObject();
				fpsObj.addProperty("current", fps);
				fpsObj.addProperty("min", minFps);
				fpsObj.addProperty("max", maxFps);
				fpsObj.addProperty("avg", p50Fps);
				fpsObj.addProperty("onePercentLow", p99Fps);
				json.add("fps", fpsObj);

				JsonObject ftObj = new JsonObject();
				ftObj.addProperty("currentMs", ftMs);
				ftObj.addProperty("emaMs", emaMs);
				ftObj.addProperty("p50Ms", monitor.percentileMs(0.50));
				ftObj.addProperty("p95Ms", monitor.percentileMs(0.95));
				ftObj.addProperty("p99Ms", monitor.percentileMs(0.99));
				json.add("frametime", ftObj);

				JsonObject diag = new JsonObject();
				diag.addProperty("stutterStatus", monitor.hasSpike() && lastSpikeAge < 5000 ? "STUTTER_ACTIVE" : "STABLE_OK");
				diag.addProperty("lastErrorCode", lastCode.getCode());
				diag.addProperty("lastErrorTitle", lastCode.getTitle());
				diag.addProperty("lastErrorDesc", lastCode.getDescription());
				diag.addProperty("lastSpikeDurationMs", lastSpikeMs);
				diag.addProperty("lastSpikeAgeMs", lastSpikeAge);
				diag.addProperty("totalSpikes", monitor.spikeCount());
				json.add("diagnostics", diag);

				JsonObject memObj = new JsonObject();
				memObj.addProperty("heapUsedMb", heapUsedMb);
				memObj.addProperty("heapMaxMb", heapMaxMb);
				memObj.addProperty("freePhysicalMb", freePhysMb);
				memObj.addProperty("underPressure", mem.underPressure());
				json.add("memory", memObj);

				JsonObject gfx = new JsonObject();
				gfx.addProperty("shaderActive", shaderActive);
				gfx.addProperty("shadowPassActive", shadowPass);
				gfx.addProperty("darwinQosPcoreLocked", M3FrametimeMod.config().boostDarwinQos);
				json.add("graphics", gfx);

				Files.createDirectories(LOG_PATH.getParent());
				try (Writer writer = Files.newBufferedWriter(LOG_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
					new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
				}
			} catch (Throwable ignored) {
			} finally {
				writing = false;
			}
		});
	}
}
