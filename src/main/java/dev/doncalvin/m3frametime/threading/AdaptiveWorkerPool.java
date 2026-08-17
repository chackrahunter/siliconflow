package dev.doncalvin.m3frametime.threading;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tiny daemon pool for non-render leftover work only (memory probe, soft cache hints).
 * With Sodium we keep this pool small so background work does not compete unnecessarily with meshing.
 */
public final class AdaptiveWorkerPool {
	private static AdaptiveWorkerPool INSTANCE;

	private final ExecutorService executor;
	private final int threadCount;
	private final AtomicBoolean taskGate = new AtomicBoolean();

	private AdaptiveWorkerPool(int threadCount) {
		this.threadCount = threadCount;
		AtomicInteger seq = new AtomicInteger();
		String profile = M3FrametimeMod.config().performanceProfile;
		boolean hotProfile = "MAX".equalsIgnoreCase(profile) || "PLAYABLE".equalsIgnoreCase(profile);
		// Below Sodium workers (NORM/NORM+1) and render (MAX); still above absolute MIN.
		int prio = hotProfile ? Thread.MIN_PRIORITY + 2 : Thread.MIN_PRIORITY + 1;
		ThreadFactory factory = r -> {
			Thread t = new Thread(r, "m3-frametime-worker-" + seq.getAndIncrement());
			t.setDaemon(true);
			t.setPriority(prio);
			return t;
		};
		this.executor = new ThreadPoolExecutor(threadCount, threadCount, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(32), factory, new ThreadPoolExecutor.DiscardPolicy());
	}

	public static synchronized AdaptiveWorkerPool get() {
		if (INSTANCE == null) {
			INSTANCE = new AdaptiveWorkerPool(resolveThreadCount());
			M3FrametimeMod.LOGGER.info(
				"AdaptiveWorkerPool started with {} thread(s) (sodium={} — leftover IO/probe only; Sodium owns mesh saturation)",
				INSTANCE.threadCount,
				StackCompat.sodium()
			);
		}
		return INSTANCE;
	}

	private static int resolveThreadCount() {
		int configured = M3FrametimeMod.config().workerThreads;
		if (configured > 0) {
			// Hard cap: never compete with a boosted Sodium mesh pool.
			int cap = StackCompat.sodium() ? 1 : 2;
			return Math.min(cap, configured);
		}
		return StackCompat.preferredWorkerThreads();
	}

	public void execute(Runnable task) {
		if (task == null || !taskGate.compareAndSet(false, true)) return;
		try {
			executor.execute(() -> {
				try { task.run(); } finally { taskGate.set(false); }
			});
		} catch (RejectedExecutionException ignored) {
			taskGate.set(false);
			M3FrametimeMod.LOGGER.debug("Dropping stale background task");
		}
	}

	public int threadCount() {
		return threadCount;
	}

	public void shutdown() {
		executor.shutdownNow();
	}
}
