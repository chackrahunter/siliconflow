package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.pool.ScratchPool;
import dev.doncalvin.m3frametime.threading.SpscRingBuffer;

import java.util.List;

/**
 * High-precision micro-stutter diagnostic engine with 236-code Error Matrix for Apple Silicon M3.
 * 100% zero-allocation architecture in all hot paths.
 */
public final class SpikeMonitor {
	public static final class SpikeEvent {
		public long frameDeltaNanos;
		public StutterErrorCode errorCode = StutterErrorCode.OK_000;
		public long gcDeltaMs;
		public long freeMb;
		public long wallNanos;
		public long timestampMillis;

		public void set(long frameDeltaNanos, StutterErrorCode errorCode, long gcDeltaMs, long freeMb, long wallNanos) {
			this.frameDeltaNanos = frameDeltaNanos;
			this.errorCode = errorCode != null ? errorCode : StutterErrorCode.OK_000;
			this.gcDeltaMs = gcDeltaMs;
			this.freeMb = freeMb;
			this.wallNanos = wallNanos;
			this.timestampMillis = System.currentTimeMillis();
		}
	}

	private static final int BUCKETS = 500; // 0..50 ms in 0.1 ms steps
	private static final int RING = 32;
	private static final SpikeMonitor INSTANCE = new SpikeMonitor();

	private final long[] histogram = new long[BUCKETS];
	private final SpikeEvent[] ringSlots;
	private final SpscRingBuffer<SpikeEvent> recent;
	private final SpikeEvent[] overlayCopy = new SpikeEvent[16];

	private long frameCount;
	private long spikeCount;
	private StutterErrorCode lastErrorCode = StutterErrorCode.OK_000;
	private long lastSpikeDeltaNanos;
	private long lastSpikeWallMillis;
	private long lastSpikeGcDeltaMs;
	private long lastSpikeFreePhysMb;
	private long lastSpikeHeapUsedMb;
	private long lastSpikeHeapMaxMb;

	// Min / Max FPS tracking
	private long sessionMinDeltaNanos = Long.MAX_VALUE;
	private long sessionMaxDeltaNanos = 0;
	private long rollingWindowStartNanos;
	private long rollingMinDeltaNanos = Long.MAX_VALUE;
	private long rollingMaxDeltaNanos = 0;
	private int cachedRollingMinFps;
	private int cachedRollingMaxFps;

	private SpikeMonitor() {
		ringSlots = new SpikeEvent[RING];
		for (int i = 0; i < RING; i++) {
			ringSlots[i] = new SpikeEvent();
		}
		recent = new SpscRingBuffer<>(RING, ringSlots);
		for (int i = 0; i < overlayCopy.length; i++) {
			overlayCopy[i] = new SpikeEvent();
		}
	}

	public static SpikeMonitor get() {
		return INSTANCE;
	}

	public void onFrameEnd(long deltaNanos) {
		frameCount++;
		long now = System.nanoTime();

		// Track session min/max frametimes (ignoring the very first warm-up frame)
		if (frameCount > 20 && deltaNanos > 500_000L) { // > 0.5ms (up to 2000 FPS cap)
			if (deltaNanos < sessionMinDeltaNanos) {
				sessionMinDeltaNanos = deltaNanos;
			}
			if (deltaNanos > sessionMaxDeltaNanos) {
				sessionMaxDeltaNanos = deltaNanos;
			}

			if (deltaNanos < rollingMinDeltaNanos) {
				rollingMinDeltaNanos = deltaNanos;
			}
			if (deltaNanos > rollingMaxDeltaNanos) {
				rollingMaxDeltaNanos = deltaNanos;
			}

			// Update rolling 5-second min/max FPS window
			if (now - rollingWindowStartNanos > 5_000_000_000L) {
				cachedRollingMaxFps = rollingMinDeltaNanos > 0 ? (int) Math.round(1_000_000_000.0 / rollingMinDeltaNanos) : 0;
				cachedRollingMinFps = rollingMaxDeltaNanos > 0 ? (int) Math.round(1_000_000_000.0 / rollingMaxDeltaNanos) : 0;
				rollingWindowStartNanos = now;
				rollingMinDeltaNanos = deltaNanos;
				rollingMaxDeltaNanos = deltaNanos;
			}
		}

		int bucket = (int) (deltaNanos / 100_000L); // 0.1 ms
		if (bucket < 0) {
			bucket = 0;
		} else if (bucket >= BUCKETS) {
			bucket = BUCKETS - 1;
		}
		histogram[bucket]++;

		double emaNanos = FramePacer.get().emaDeltaNanos();
		long thresholdNanos = M3FrametimeMod.config().spikeThresholdMs * 1_000_000L;
		// True micro-stutter: frame exceeds 33.3ms (drops below 30 FPS) or is 2.2x worse than a non-trivial EMA
		boolean isMicroStutter = deltaNanos >= thresholdNanos
			|| (emaNanos > 8_000_000L && deltaNanos > (long) (emaNanos * 2.2));

		if (!isMicroStutter) {
			return;
		}

		GcProbe gc = GcProbe.get();
		MemoryPressureProbe mem = MemoryPressureProbe.get();
		mem.sampleHeap();

		StutterErrorCode code = StutterErrorCode.fromSpike(
			deltaNanos,
			gc,
			mem,
			SpikeScope.get().dominant(),
			StackCompat.isShaderActive(),
			StackCompat.isShadowPass()
		);

		lastErrorCode = code;
		lastSpikeDeltaNanos = deltaNanos;
		lastSpikeWallMillis = System.currentTimeMillis();
		lastSpikeGcDeltaMs = gc.frameGcDeltaMs();
		lastSpikeFreePhysMb = mem.freePhysicalMb();
		lastSpikeHeapUsedMb = mem.heapUsedMb();
		lastSpikeHeapMaxMb = mem.heapMaxMb();
		spikeCount++;

		SpikeEvent slot = recent.forceClaimWriteSlot();
		slot.set(deltaNanos, code, lastSpikeGcDeltaMs, lastSpikeFreePhysMb, now);
		recent.publishWrite();
	}

	public long frameCount() {
		return frameCount;
	}

	public long spikeCount() {
		return spikeCount;
	}

	public StutterErrorCode lastErrorCode() {
		return lastErrorCode;
	}

	public double lastSpikeMs() {
		return lastSpikeDeltaNanos / 1_000_000.0;
	}

	public boolean hasSpike() {
		return spikeCount > 0 && lastSpikeWallMillis > 0L;
	}

	public long lastSpikeAgeMs() {
		if (!hasSpike()) {
			return -1L;
		}
		return Math.max(0L, System.currentTimeMillis() - lastSpikeWallMillis);
	}

	public int maxFps() {
		if (sessionMinDeltaNanos <= 0 || sessionMinDeltaNanos == Long.MAX_VALUE) {
			return 0;
		}
		return (int) Math.round(1_000_000_000.0 / sessionMinDeltaNanos);
	}

	public int minFps() {
		if (sessionMaxDeltaNanos <= 0) {
			return 0;
		}
		return (int) Math.round(1_000_000_000.0 / sessionMaxDeltaNanos);
	}

	public int rollingMaxFps() {
		return cachedRollingMaxFps > 0 ? cachedRollingMaxFps : maxFps();
	}

	public int rollingMinFps() {
		return cachedRollingMinFps > 0 ? cachedRollingMinFps : minFps();
	}

	/** Percentile from histogram; p in [0,1]. */
	public double percentileMs(double p) {
		if (frameCount == 0) {
			return 0;
		}
		long target = (long) Math.ceil(frameCount * p);
		long acc = 0;
		for (int i = 0; i < BUCKETS; i++) {
			acc += histogram[i];
			if (acc >= target) {
				return (i + 1) * 0.1;
			}
		}
		return BUCKETS * 0.1;
	}

	public int drainRecent(SpikeEvent[] out) {
		int n = 0;
		SpikeEvent e;
		while (n < out.length && (e = recent.poll()) != null) {
			out[n].set(e.frameDeltaNanos, e.errorCode, e.gcDeltaMs, e.freeMb, e.wallNanos);
			n++;
		}
		return n;
	}

	public SpikeEvent[] overlayScratch() {
		return overlayCopy;
	}

	/** Appends compact F3 left-panel lines with zero heap allocations. */
	public void appendF3Lines(List<String> out) {
		if (!M3FrametimeMod.config().f3StutterInfo) {
			return;
		}
		StringBuilder sb = ScratchPool.get().stringBuilder();
		out.add("");
		if (!hasSpike()) {
			out.add("[m3] stutter: none yet (status=OK-000)");
		} else {
			sb.setLength(0);
			sb.append("[m3] stutter: ");
			formatAge(sb, lastSpikeAgeMs()).append(" ago · ");
			ScratchPool.appendFixed(sb, lastSpikeMs(), 1).append("ms · [");
			sb.append(lastErrorCode.getCode()).append("] ").append(lastErrorCode.getTitle());
			out.add(sb.toString());

			sb.setLength(0);
			sb.append("[m3] cause: ").append(lastErrorCode.getDescription());
			out.add(sb.toString());

			sb.setLength(0);
			sb.append("[m3] mem@spike heap=").append(lastSpikeHeapUsedMb).append("/").append(lastSpikeHeapMaxMb)
				.append("MB freePhys=").append(lastSpikeFreePhysMb).append("MB");
			out.add(sb.toString());
		}
		MemoryPressureProbe mem = MemoryPressureProbe.get();
		mem.sampleHeap();

		sb.setLength(0);
		sb.append("[m3] now heap=").append(mem.heapUsedMb()).append("/").append(mem.heapMaxMb())
			.append("MB freePhys=").append(mem.freePhysicalMb()).append("MB")
			.append(mem.underPressure() ? " [MEM-001]" : " [OK]");
		out.add(sb.toString());

		sb.setLength(0);
		sb.append("[m3] fps=").append(maxFps()).append("max/").append(minFps()).append("min stutters=").append(spikeCount)
			.append(" p95=");
		ScratchPool.appendFixed(sb, percentileMs(0.95), 1).append(" p99=");
		ScratchPool.appendFixed(sb, percentileMs(0.99), 1).append(" ema=");
		ScratchPool.appendFixed(sb, FramePacer.get().emaDeltaMs(), 1).append("ms");
		out.add(sb.toString());
	}

	private static StringBuilder formatAge(StringBuilder sb, long ageMs) {
		if (ageMs < 1000L) {
			return sb.append(ageMs).append("ms");
		}
		if (ageMs < 60_000L) {
			return ScratchPool.appendFixed(sb, ageMs / 1000.0, 1).append("s");
		}
		long sec = ageMs / 1000L;
		long min = sec / 60L;
		sec %= 60L;
		return sb.append(min).append("m ").append(sec).append("s");
	}
}
