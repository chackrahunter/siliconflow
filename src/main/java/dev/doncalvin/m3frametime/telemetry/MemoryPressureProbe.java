package dev.doncalvin.m3frametime.telemetry;

import com.sun.management.OperatingSystemMXBean;
import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Async physical-memory / pressure probe. Never blocks the render thread on process I/O.
 */
public final class MemoryPressureProbe {
	private static final MemoryPressureProbe INSTANCE = new MemoryPressureProbe();

	private final AtomicLong freePhysicalMb = new AtomicLong(-1);
	private final AtomicLong totalPhysicalMb = new AtomicLong(-1);
	private final AtomicLong heapUsedMb = new AtomicLong(-1);
	private final AtomicLong heapMaxMb = new AtomicLong(-1);
	private final AtomicBoolean pressure = new AtomicBoolean(false);
	private final AtomicBoolean heapPressure = new AtomicBoolean(false);
	private final AtomicBoolean sampling = new AtomicBoolean(false);
	private volatile long lastSampleNanos;

	private MemoryPressureProbe() {}

	public static MemoryPressureProbe get() {
		return INSTANCE;
	}

	public void requestSample() {
		long now = System.nanoTime();
		if (now - lastSampleNanos < 250_000_000L) {
			return;
		}
		if (!sampling.compareAndSet(false, true)) {
			return;
		}
		AdaptiveWorkerPool.get().execute(() -> {
			try {
				sampleNow();
			} finally {
				lastSampleNanos = System.nanoTime();
				sampling.set(false);
			}
		});
	}

	private void sampleNow() {
		try {
			sampleHeap();
			var bean = ManagementFactory.getOperatingSystemMXBean();
			if (bean instanceof OperatingSystemMXBean os) {
				long free = os.getFreeMemorySize();
				long total = os.getTotalMemorySize();
				if (free >= 0) {
					freePhysicalMb.set(free / (1024L * 1024L));
				}
				if (total >= 0) {
					totalPhysicalMb.set(total / (1024L * 1024L));
				}
				// On macOS Darwin, free physical pages are cached by the OS page cache. Only flag real pressure < 32MB.
				pressure.set(free >= 0 && (free / (1024L * 1024L)) < 32L);
			}
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("Memory probe failed: {}", t.toString());
		}
	}

	/** Cheap heap sample — safe on any thread. */
	public void sampleHeap() {
		Runtime rt = Runtime.getRuntime();
		long used = rt.totalMemory() - rt.freeMemory();
		long max = rt.maxMemory();
		heapUsedMb.set(used / (1024L * 1024L));
		heapMaxMb.set(max / (1024L * 1024L));
		// Treat ≥90% of -Xmx as heap pressure
		heapPressure.set(max > 0 && used >= (long) (max * 0.90));
	}

	public boolean underPressure() {
		return pressure.get() || heapPressure.get();
	}

	public boolean physicalUnderPressure() {
		return pressure.get();
	}

	public boolean heapUnderPressure() {
		return heapPressure.get();
	}

	public long freePhysicalMb() {
		return freePhysicalMb.get();
	}

	public long totalPhysicalMb() {
		return totalPhysicalMb.get();
	}

	public long heapUsedMb() {
		return heapUsedMb.get();
	}

	public long heapMaxMb() {
		return heapMaxMb.get();
	}
}
