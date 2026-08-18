package dev.doncalvin.m3frametime.telemetry;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;

/**
 * Lightweight GC pause probe via MXBeans.
 * Rate-limited to ~1-2 Hz to completely eliminate JMX/JVM lock contention in the render loop.
 */
public final class GcProbe {
	private static final GcProbe INSTANCE = new GcProbe();

	private final List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
	private long lastCollectionTimeMs;
	private long lastCollectionCount;
	private volatile long frameGcDeltaMs;
	private volatile long frameGcCountDelta;
	private final AtomicBoolean sampling = new AtomicBoolean();

	private GcProbe() {
		snapshotBaseline();
	}

	public static GcProbe get() {
		return INSTANCE;
	}

	private void snapshotBaseline() {
		// Prime the baselines with the JVM's cumulative GC totals so the first async sample
		// reports the delta since construction — not the entire since-launch GC time, which
		// would otherwise mislabel the first micro-stutter as GC-001.
		long time = 0L, count = 0L;
		for (GarbageCollectorMXBean bean : beans) {
			long t = bean.getCollectionTime(), c = bean.getCollectionCount();
			if (t > 0) time += t;
			if (c > 0) count += c;
		}
		lastCollectionTimeMs = time;
		lastCollectionCount = count;
	}

	/**
	 * Schedules the MXBean query off the render thread. Never blocks the caller.
	 * {@link #frameGcDeltaMs()} is the last completed sample — typically the previous
	 * frame — because MinecraftClientMixin invokes this at render RETURN and the worker
	 * finishes asynchronously. Do not treat a 0 ms delta as "no GC this frame."
	 */
	public void sampleFrame() {
		if (!sampling.compareAndSet(false, true)) return;
		AdaptiveWorkerPool.get().execute(() -> {
			try {
				long time = 0L, count = 0L;
				for (GarbageCollectorMXBean bean : beans) {
					long t = bean.getCollectionTime(), c = bean.getCollectionCount();
					if (t > 0) time += t;
					if (c > 0) count += c;
				}
				frameGcDeltaMs = Math.max(0L, time - lastCollectionTimeMs);
				frameGcCountDelta = Math.max(0L, count - lastCollectionCount);
				lastCollectionTimeMs = time; lastCollectionCount = count;
			} finally { sampling.set(false); }
		});
	}

	/** Last completed async sample; usually one frame behind the spike being attributed. */
	public long frameGcDeltaMs() {
		return frameGcDeltaMs;
	}

	public long frameGcCountDelta() {
		return frameGcCountDelta;
	}
}
