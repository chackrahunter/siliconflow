package dev.doncalvin.m3frametime.telemetry;

import com.google.gson.JsonObject;
import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;
import dev.doncalvin.m3frametime.telemetry.PerformanceRecorder.Snapshot;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Live Flight Recorder & Real-time Telemetry Stream.
 * Periodically writes live JSON diagnostics to disk (run/m3-live-telemetry.json or config/m3-live-telemetry.json)
 * enabling remote inspection of GPU frametimes, Darwin QoS, and stutter error codes.
 */
public final class LiveTelemetryStream {
	private static final LiveTelemetryStream INSTANCE = new LiveTelemetryStream();
	private static final Path LOG_PATH = FabricLoader.getInstance().getGameDir().resolve("m3-live-telemetry.json");
	private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

	private long lastWriteNanos;
	private final AtomicBoolean writing = new AtomicBoolean();

	private LiveTelemetryStream() {}

	public static LiveTelemetryStream get() {
		return INSTANCE;
	}

	public void sampleAndStream(long nowNanos) {
		if (!M3FrametimeMod.config().spikeLogging || nowNanos - lastWriteNanos < 2_000_000_000L || !writing.compareAndSet(false, true)) {
			return;
		}
		lastWriteNanos = nowNanos;

		SpikeMonitor monitor = SpikeMonitor.get();
		FramePacer pacer = FramePacer.get();
		MemoryPressureProbe mem = MemoryPressureProbe.get();
		Runtime rt = Runtime.getRuntime();
		TelemetrySnapshot snapshot = TelemetrySnapshot.current();

		double ftMs = snapshot.frametimeMs() > 0.0 ? snapshot.frametimeMs() : pacer.lastDeltaMs();
		double emaMs = snapshot.emaFrametimeMs() > 0.0 ? snapshot.emaFrametimeMs() : pacer.emaDeltaMs();
		int fps = ftMs > 0 ? (int) Math.round(1000.0 / ftMs) : 0;
		int minFps = monitor.rollingMinFps();
		int maxFps = monitor.rollingMaxFps();
		double p50Ms = monitor.percentileMs(0.50);
		double p95Ms = monitor.percentileMs(0.95);
		double p99Ms = monitor.percentileMs(0.99);
		double onePercentLowMs = p99Ms;
		int p50Fps = p50Ms > 0 ? (int) Math.round(1000.0 / p50Ms) : 0;
		int p99Fps = p99Ms > 0 ? (int) Math.round(1000.0 / p99Ms) : 0;
		int onePercentLowFps = onePercentLowMs > 0 ? (int) Math.round(1000.0 / onePercentLowMs) : 0;

		StutterErrorCode lastCode = monitor.lastErrorCode();
		long lastSpikeAge = monitor.lastSpikeAgeMs();
		double lastSpikeMs = monitor.lastSpikeMs();
		boolean qualityBreach = (p95Ms >= Math.max(33.3, M3FrametimeMod.config().spikeThresholdMs)) || onePercentLowFps < 30;
		boolean stutterActive = (monitor.hasSpike() && lastSpikeAge >= 0 && lastSpikeAge < 5000) || qualityBreach;
		if (lastCode == StutterErrorCode.OK_000 && stutterActive) lastCode = StutterErrorCode.FRAME_001;
		final StutterErrorCode reportedCode = lastCode;
		long totalSpikes = monitor.spikeCount();

		long heapUsedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
		long heapMaxMb = rt.maxMemory() / (1024L * 1024L);
		long freePhysMb = mem.freePhysicalMb();
		Snapshot recorder = PerformanceRecorder.get().snapshot();

		boolean shaderActive = snapshot.timestampNanos() > 0L ? snapshot.shaderActive() : StackCompat.isShaderActive();
		boolean shadowPass = StackCompat.isShadowPass();
		boolean darwinQos = M3FrametimeMod.config().boostDarwinQos;

		try {
			AdaptiveWorkerPool.get().execute(() -> {
			try {
				JsonObject json = new JsonObject();
				long generatedAtMillis = System.currentTimeMillis();
				json.addProperty("timestamp", generatedAtMillis);
				json.addProperty("sampleTimestampNanos", nowNanos);
				json.addProperty("timestampSource", "wall-clock-at-write");
				
				JsonObject fpsObj = new JsonObject();
				fpsObj.addProperty("current", fps);
				fpsObj.addProperty("min", minFps);
				fpsObj.addProperty("max", maxFps);
				fpsObj.addProperty("avg", p50Fps);
				fpsObj.addProperty("onePercentLow", onePercentLowFps);
				fpsObj.addProperty("onePercentLowMs", onePercentLowMs);
				json.add("fps", fpsObj);

				JsonObject ftObj = new JsonObject();
				ftObj.addProperty("currentMs", ftMs);
				ftObj.addProperty("emaMs", emaMs);
				ftObj.addProperty("p50Ms", p50Ms);
				ftObj.addProperty("p95Ms", p95Ms);
				ftObj.addProperty("p99Ms", p99Ms);
				json.add("frametime", ftObj);

				JsonObject diag = new JsonObject();
				diag.addProperty("stutterStatus", stutterActive ? "STUTTER_ACTIVE" : "STABLE_OK");
				diag.addProperty("lastErrorCode", reportedCode.getCode());
				diag.addProperty("lastErrorTitle", reportedCode.getTitle());
				diag.addProperty("lastErrorDesc", reportedCode.getDescription());
				diag.addProperty("lastSpikeDurationMs", lastSpikeMs);
				diag.addProperty("lastSpikeAgeMs", lastSpikeAge);
				diag.addProperty("totalSpikes", totalSpikes);
				diag.addProperty("qualityBreach", qualityBreach);
				diag.addProperty("errorCodeSource", reportedCode == StutterErrorCode.FRAME_001 ? "measured-frame-timing" : "measured-subsystem-or-status");
				json.add("diagnostics", diag);

				JsonObject memObj = new JsonObject();
				memObj.addProperty("heapUsedMb", heapUsedMb);
				memObj.addProperty("heapMaxMb", heapMaxMb);
				memObj.addProperty("freePhysicalMb", freePhysMb);
				memObj.addProperty("pressureThresholdMb", mem.pressureThresholdMb());
				memObj.addProperty("pressureAgeMs", mem.pressureAgeMs());
				memObj.addProperty("underPressure", mem.underPressure());
				memObj.addProperty("physicalPressureState", mem.physicalPressureState());
				memObj.addProperty("physicalSampleAgeMs", mem.sampleAgeMs());
				memObj.addProperty("pressureEventActive", mem.physicalUnderPressure());
				json.add("memory", memObj);

				JsonObject gfx = new JsonObject();
				gfx.addProperty("shaderActive", shaderActive);
				gfx.addProperty("shadowPassActive", shadowPass);
				gfx.addProperty("darwinQosPcoreLocked", darwinQos);
				gfx.addProperty("shaderStateSource", snapshot.timestampNanos() > 0L ? "iris-api-sample" : "iris-api-fallback");
				json.add("graphics", gfx);

				JsonObject recorderObj = new JsonObject();
				recorderObj.addProperty("enabled", recorder.timestampNanos() > 0L);
				recorderObj.addProperty("source", recorder.source());
				recorderObj.addProperty("snapshotAvailable", recorder.timestampNanos() > 0L);
				recorderObj.addProperty("timestampNanos", recorder.timestampNanos());
				recorderObj.addProperty("windowSize", recorder.windowSize());
				recorderObj.addProperty("recordedFrames", recorder.recordedFrames());
				recorderObj.addProperty("recordedSpikes", recorder.recordedSpikes());
				recorderObj.addProperty("gcPauseDeltaMs", recorder.gcPauseDeltaMs());
				recorderObj.addProperty("gcCountDelta", recorder.gcCountDelta());
				recorderObj.addProperty("heapValidity", recorder.heapMaxMb() >= 0 ? "measured" : "unavailable");
				recorderObj.addProperty("physicalMemoryValidity", recorder.freePhysicalMb() >= 0 ? "measured" : "unavailable");
				recorderObj.addProperty("pressureAgeMs", recorder.pressureAgeMs());
				recorderObj.addProperty("pacingEnabled", recorder.pacingEnabled());
				recorderObj.addProperty("workerThreads", recorder.workerThreads());
				json.add("recorder", recorderObj);

				Files.createDirectories(LOG_PATH.getParent());
				try (Writer writer = Files.newBufferedWriter(LOG_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
					GSON.toJson(json, writer);
				}
			} catch (Throwable t) {
				M3FrametimeMod.LOGGER.debug("Live telemetry write failed: {}", t.toString());
			} finally {
				writing.set(false);
			}
			});
		} catch (RuntimeException e) {
			writing.set(false);
			M3FrametimeMod.LOGGER.debug("Live telemetry task rejected: {}", e.toString());
		}
	}
}
