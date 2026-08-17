package dev.doncalvin.m3frametime.telemetry;

/** Immutable cross-thread view of the latest measured client telemetry. */
public final class TelemetrySnapshot {
	private static volatile TelemetrySnapshot current = unavailable();

	private final long timestampNanos;
	private final int fps;
	private final double frametimeMs;
	private final double emaFrametimeMs;
	private final double minFrametimeMs;
	private final double maxFrametimeMs;
	private final long heapUsedMb;
	private final long heapMaxMb;
	private final long freePhysicalMb;
	private final boolean shaderActive;
	private final boolean shadowPass;
	private final boolean memoryPressure;
	private final long pressureAgeMs;
	private final long pressureThresholdMb;
	private final boolean renderQosRequested;
	private final boolean recentSpike;
	private final long spikeCount;
	private final long lastSpikeAgeMs;
	private final StutterErrorCode lastErrorCode;

	public TelemetrySnapshot(
		long timestampNanos,
		int fps,
		double frametimeMs,
		double emaFrametimeMs,
		double minFrametimeMs,
		double maxFrametimeMs,
		long heapUsedMb,
		long heapMaxMb,
		long freePhysicalMb,
		boolean shaderActive,
		boolean shadowPass,
		boolean memoryPressure,
		long pressureAgeMs,
		long pressureThresholdMb,
		boolean renderQosRequested,
		boolean recentSpike,
		long spikeCount,
		long lastSpikeAgeMs,
		StutterErrorCode lastErrorCode
	) {
		this.timestampNanos = timestampNanos;
		this.fps = Math.max(0, fps);
		this.frametimeMs = finiteOrZero(frametimeMs);
		this.emaFrametimeMs = finiteOrZero(emaFrametimeMs);
		this.minFrametimeMs = finiteOrZero(minFrametimeMs);
		this.maxFrametimeMs = finiteOrZero(maxFrametimeMs);
		this.heapUsedMb = Math.max(0L, heapUsedMb);
		this.heapMaxMb = Math.max(0L, heapMaxMb);
		this.freePhysicalMb = freePhysicalMb;
		this.shaderActive = shaderActive;
		this.shadowPass = shadowPass;
		this.memoryPressure = memoryPressure;
		this.pressureAgeMs = pressureAgeMs;
		this.pressureThresholdMb = Math.max(0L, pressureThresholdMb);
		this.renderQosRequested = renderQosRequested;
		this.recentSpike = recentSpike;
		this.spikeCount = Math.max(0L, spikeCount);
		this.lastSpikeAgeMs = lastSpikeAgeMs;
		this.lastErrorCode = lastErrorCode == null ? StutterErrorCode.OK_000 : lastErrorCode;
	}

	public static TelemetrySnapshot current() {
		return current;
	}

	public static void publish(TelemetrySnapshot snapshot) {
		if (snapshot != null) {
			current = snapshot;
		}
	}

	public static TelemetrySnapshot unavailable() {
		return new TelemetrySnapshot(0L, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, -1L, false, false, false, -1L, 0L, false, false, 0L, -1L, StutterErrorCode.OK_000);
	}

	private static double finiteOrZero(double value) {
		return Double.isFinite(value) && value >= 0.0 ? value : 0.0;
	}

	public long timestampNanos() { return timestampNanos; }
	public int fps() { return fps; }
	public double frametimeMs() { return frametimeMs; }
	public double emaFrametimeMs() { return emaFrametimeMs; }
	public double minFrametimeMs() { return minFrametimeMs; }
	public double maxFrametimeMs() { return maxFrametimeMs; }
	public long heapUsedMb() { return heapUsedMb; }
	public long heapMaxMb() { return heapMaxMb; }
	public long freePhysicalMb() { return freePhysicalMb; }
	public boolean shaderActive() { return shaderActive; }
	public boolean shadowPass() { return shadowPass; }
	public boolean memoryPressure() { return memoryPressure; }
	public long pressureAgeMs() { return pressureAgeMs; }
	public long pressureThresholdMb() { return pressureThresholdMb; }
	public boolean renderQosRequested() { return renderQosRequested; }
	public boolean recentSpike() { return recentSpike; }
	public long spikeCount() { return spikeCount; }
	public long lastSpikeAgeMs() { return lastSpikeAgeMs; }
	public StutterErrorCode lastErrorCode() { return lastErrorCode; }
}
