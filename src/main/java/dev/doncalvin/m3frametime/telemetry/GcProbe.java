package dev.doncalvin.m3frametime.telemetry;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Lightweight GC pause probe via MXBeans.
 * Rate-limited to ~1-2 Hz to completely eliminate JMX/JVM lock contention in the render loop.
 */
public final class GcProbe {
	private static final GcProbe INSTANCE = new GcProbe();

	private final List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
	private long lastCollectionTimeMs;
	private long lastCollectionCount;
	private long frameGcDeltaMs;
	private long frameGcCountDelta;
	private int sampleTicker;

	private GcProbe() {
		snapshotBaseline();
	}

	public static GcProbe get() {
		return INSTANCE;
	}

	private void snapshotBaseline() {
		long time = 0;
		long count = 0;
		for (GarbageCollectorMXBean bean : beans) {
			long t = bean.getCollectionTime();
			long c = bean.getCollectionCount();
			if (t > 0) {
				time += t;
			}
			if (c > 0) {
				count += c;
			}
		}
		lastCollectionTimeMs = time;
		lastCollectionCount = count;
	}

	/** Sample GC stats at ~1 Hz (every 60 frames) to eliminate JMX kernel lock stalls. */
	public void sampleFrame() {
		if ((++sampleTicker & 63) != 0) {
			return;
		}
		long time = 0;
		long count = 0;
		for (GarbageCollectorMXBean bean : beans) {
			long t = bean.getCollectionTime();
			long c = bean.getCollectionCount();
			if (t > 0) {
				time += t;
			}
			if (c > 0) {
				count += c;
			}
		}
		frameGcDeltaMs = Math.max(0, time - lastCollectionTimeMs);
		frameGcCountDelta = Math.max(0, count - lastCollectionCount);
		lastCollectionTimeMs = time;
		lastCollectionCount = count;
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
