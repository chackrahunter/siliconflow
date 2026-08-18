package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.IrisCompat;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.pool.ScratchPool;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-precision micro-stutter diagnostic engine. Attribution uses
 * {@link StutterErrorCode#fromSpike} against the live catalog ({@link StutterErrorCode#totalCount()} codes).
 * Histogram / ring updates stay allocation-free; Sodium allocator sampling is off-thread.
 */
public final class SpikeMonitor {
	private static final int BUCKETS = 500; // 0..50 ms in 0.1 ms steps
	private static final int ROLLING_CAP = 1024;
	private static final long ROLLING_WINDOW_NANOS = 5_000_000_000L;
	private static final long PERCENTILE_PERIOD_NANOS = 100_000_000L;
	private static final SpikeMonitor INSTANCE = new SpikeMonitor();

	private final long[] histogram = new long[BUCKETS];

	private long frameCount;
	private long spikeCount;
	private StutterErrorCode lastErrorCode = StutterErrorCode.OK_000;
	private long lastSpikeDeltaNanos;
	private long lastSpikeWallMillis;
	private long lastSpikeGcDeltaMs;
	private long lastSpikeFreePhysMb;
	private long lastSpikeHeapUsedMb;
	private long lastSpikeHeapMaxMb;

	private volatile boolean sodiumAllocatorWarning;
	private final AtomicBoolean sodiumSampling = new AtomicBoolean();
	private volatile long lastSodiumSampleNanos;
	private static volatile Method sodiumWarningMethod;
	private static volatile boolean sodiumWarningResolved;

	// Min / Max FPS tracking
	private long sessionMinDeltaNanos = Long.MAX_VALUE;
	private long sessionMaxDeltaNanos = 0;
	private long rollingWindowStartNanos;
	private long rollingMinDeltaNanos = Long.MAX_VALUE;
	private long rollingMaxDeltaNanos = 0;
	private int cachedRollingMinFps;
	private int cachedRollingMaxFps;

	private final long[] rollingDeltaNanos = new long[ROLLING_CAP];
	private final long[] rollingStampNanos = new long[ROLLING_CAP];
	private final long[] rollingScratch = new long[ROLLING_CAP];
	private int rollingWrite;
	private int rollingSize;
	private long lastPercentileComputeNanos;
	private double cachedRollingP50Ms;
	private double cachedRollingP95Ms;
	private double cachedRollingP99Ms;

	private SpikeMonitor() {}

	public static SpikeMonitor get() {
		return INSTANCE;
	}

	public void onFrameEnd(long deltaNanos) {
		frameCount++;
		long now = System.nanoTime();
		requestSodiumAllocatorSample();

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
		recordRollingSample(deltaNanos, now);

		double emaNanos = FramePacer.get().emaDeltaNanos();
		long thresholdNanos = M3FrametimeMod.config().spikeThresholdMs * 1_000_000L;
		// True micro-stutter: frame exceeds 33.3ms (drops below 30 FPS) or is 2.2x worse than a non-trivial EMA
		boolean isMicroStutter = deltaNanos >= thresholdNanos
			|| (emaNanos > 8_000_000L && deltaNanos > (long) (emaNanos * 2.2));

		if (!isMicroStutter) {
			PerformanceRecorder.get().recordFrame(deltaNanos, false, StutterErrorCode.OK_000);
			return;
		}

		GcProbe gc = GcProbe.get();
		MemoryPressureProbe mem = MemoryPressureProbe.get();
		boolean shadowPass = StackCompat.isShadowPass() || IrisCompat.queryLiveShadowPass();
		StutterErrorCode code = StutterErrorCode.fromSpike(
			deltaNanos,
			gc,
			mem,
			SpikeScope.get().dominant(),
			StackCompat.isShaderActive(),
			shadowPass,
			sodiumAllocatorWarning
		);

		lastErrorCode = code;
		lastSpikeDeltaNanos = deltaNanos;
		lastSpikeWallMillis = System.currentTimeMillis();
		lastSpikeGcDeltaMs = gc.frameGcDeltaMs();
		lastSpikeFreePhysMb = mem.freePhysicalMb();
		lastSpikeHeapUsedMb = mem.heapUsedMb();
		lastSpikeHeapMaxMb = mem.heapMaxMb();
		spikeCount++;
		PerformanceRecorder.get().recordFrame(deltaNanos, true, code);
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

	/**
	 * Cached ChipPower Sodium-allocator warning. Sampled off-thread; may lag one interval.
	 */
	public boolean sodiumAllocatorWarning() {
		return sodiumAllocatorWarning;
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

	/** Rolling 5-second median frametime in milliseconds. */
	public double rollingP50Ms() {
		return cachedRollingP50Ms;
	}

	/** Rolling 5-second p95 frametime in milliseconds. */
	public double rollingP95Ms() {
		return cachedRollingP95Ms;
	}

	/** Rolling 5-second p99 frametime in milliseconds. */
	public double rollingP99Ms() {
		return cachedRollingP99Ms;
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

	private void recordRollingSample(long deltaNanos, long now) {
		if (deltaNanos <= 500_000L) {
			return;
		}
		rollingDeltaNanos[rollingWrite] = deltaNanos;
		rollingStampNanos[rollingWrite] = now;
		rollingWrite++;
		if (rollingWrite >= ROLLING_CAP) {
			rollingWrite = 0;
		}
		if (rollingSize < ROLLING_CAP) {
			rollingSize++;
		}
		if (now - lastPercentileComputeNanos >= PERCENTILE_PERIOD_NANOS) {
			recomputeRollingPercentiles(now);
			lastPercentileComputeNanos = now;
		}
	}

	private void recomputeRollingPercentiles(long now) {
		int n = 0;
		long cutoff = now - ROLLING_WINDOW_NANOS;
		for (int i = 0; i < rollingSize; i++) {
			int idx = rollingWrite - rollingSize + i;
			if (idx < 0) {
				idx += ROLLING_CAP;
			}
			if (rollingStampNanos[idx] >= cutoff) {
				rollingScratch[n++] = rollingDeltaNanos[idx];
			}
		}
		if (n <= 0) {
			cachedRollingP50Ms = 0.0;
			cachedRollingP95Ms = 0.0;
			cachedRollingP99Ms = 0.0;
			return;
		}
		Arrays.sort(rollingScratch, 0, n);
		cachedRollingP50Ms = rollingScratch[percentileIndex(n, 0.50)] / 1_000_000.0;
		cachedRollingP95Ms = rollingScratch[percentileIndex(n, 0.95)] / 1_000_000.0;
		cachedRollingP99Ms = rollingScratch[percentileIndex(n, 0.99)] / 1_000_000.0;
	}

	private static int percentileIndex(int n, double p) {
		int idx = (int) Math.ceil(n * p) - 1;
		if (idx < 0) {
			return 0;
		}
		if (idx >= n) {
			return n - 1;
		}
		return idx;
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

	/**
	 * Reflects into client ChipPower off the render thread. ChipPower may read Sodium JSON
	 * on a cache miss; never do that on the frame path.
	 */
	private void requestSodiumAllocatorSample() {
		if (!StackCompat.sodium()) {
			sodiumAllocatorWarning = false;
			return;
		}
		long now = System.nanoTime();
		if (lastSodiumSampleNanos != 0L && now - lastSodiumSampleNanos < 5_000_000_000L) {
			return;
		}
		if (!sodiumSampling.compareAndSet(false, true)) {
			return;
		}
		lastSodiumSampleNanos = now;
		AdaptiveWorkerPool.get().execute(() -> {
			try {
				Method method = sodiumWarningMethod;
				if (!sodiumWarningResolved) {
					try {
						Class<?> clz = Class.forName("dev.doncalvin.m3frametime.client.ChipPower");
						method = clz.getMethod("sodiumAllocatorWarning");
						sodiumWarningMethod = method;
					} catch (Throwable ignored) {
						method = null;
					}
					sodiumWarningResolved = true;
				}
				if (method != null) {
					sodiumAllocatorWarning = method.invoke(null) != null;
				}
			} catch (Throwable ignored) {
				sodiumAllocatorWarning = false;
			} finally {
				sodiumSampling.set(false);
			}
		});
	}
}
