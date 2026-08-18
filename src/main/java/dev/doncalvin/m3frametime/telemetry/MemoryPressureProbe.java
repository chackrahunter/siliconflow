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
	private volatile long pressureSinceNanos;
	private volatile long lastSuccessfulSampleNanos;
	private volatile boolean pressureObserved;
	private volatile long lastPhysicalSampleNanos;

	private MemoryPressureProbe() {}

	public static MemoryPressureProbe get() {
		return INSTANCE;
	}

	public void requestSample() {
		long now = System.nanoTime();
		if (!M3FrametimeMod.config().memoryPolicyEnabled) return;
		if (now - lastSampleNanos < 500_000_000L) {
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
				lastSuccessfulSampleNanos = System.nanoTime();
				lastPhysicalSampleNanos = lastSuccessfulSampleNanos;
				long free = os.getFreeMemorySize();
				long total = os.getTotalMemorySize();
				if (free >= 0) {
					freePhysicalMb.set(free / (1024L * 1024L));
				}
				if (total >= 0) {
					totalPhysicalMb.set(total / (1024L * 1024L));
				}
				long freeMb = free / (1024L * 1024L);
				// The configured threshold is a diagnostic guardrail, not a settings controller.
				double thresholdMb = M3FrametimeMod.config().memoryPressureFreeMbThreshold;
				boolean pressured = freeMb >= 0L && freeMb < Math.max(0.0, thresholdMb);
				boolean wasPressured = pressure.getAndSet(pressured);
				if (pressured) pressureObserved = true;
				if (pressured && !wasPressured) {
					pressureSinceNanos = System.nanoTime();
				} else if (!pressured) {
					pressureSinceNanos = 0L;
				}
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
		double enterRatio = M3FrametimeMod.config().heapPressureEnterRatio;
		heapPressure.set(max > 0 && used >= (long) (max * enterRatio));
	}

	public boolean underPressure() {
		return isFresh() && (pressure.get() || heapPressure.get());
	}

	public boolean physicalUnderPressure() {
		return isFresh() && pressure.get();
	}

	public boolean heapUnderPressure() {
		return heapPressure.get();
	}

	public long sampleAgeMs() {
		long sampled = lastSuccessfulSampleNanos;
		return sampled > 0L ? Math.max(0L, (System.nanoTime() - sampled) / 1_000_000L) : -1L;
	}

	public String physicalPressureState() {
		long free = freePhysicalMb();
		long age = sampleAgeMs();
		if (free < 0L || age < 0L) return "UNAVAILABLE";
		if (age > 2_000L) return "STALE";
		if (physicalUnderPressure()) return "PRESSURE";
		return pressureObserved ? "RECOVERED" : "NO_EVENT";
	}

	public long sampleTimestampNanos() { return lastPhysicalSampleNanos; }

	public boolean isFresh() {
		long sampled = lastPhysicalSampleNanos;
		return sampled > 0L && System.nanoTime() - sampled <= 2_000_000_000L;
	}

	public long pressureAgeMs() {
		long since = pressureSinceNanos;
		return since > 0L ? Math.max(0L, (System.nanoTime() - since) / 1_000_000L) : -1L;
	}

	public long pressureThresholdMb() {
		return Math.max(0L, Math.round(M3FrametimeMod.config().memoryPressureFreeMbThreshold));
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
