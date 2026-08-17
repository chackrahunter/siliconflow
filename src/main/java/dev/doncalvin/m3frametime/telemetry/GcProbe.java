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
		lastCollectionTimeMs = 0L;
		lastCollectionCount = 0L;
	}

	/** Schedules the MXBean query off the render thread. */
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

	public long frameGcDeltaMs() {
		return frameGcDeltaMs;
	}

	public long frameGcCountDelta() {
		return frameGcCountDelta;
	}

	public boolean suggestsGcSpike(long thresholdMs) {
		return frameGcDeltaMs >= Math.max(1, thresholdMs / 2) || frameGcCountDelta > 0 && frameGcDeltaMs > 0;
	}
}
