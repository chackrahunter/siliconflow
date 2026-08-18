package dev.doncalvin.m3frametime.threading;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Daemon pool for non-render background work (memory probe, GC probe, soft cache hints, config reload).
 * Permits multiple concurrent tasks via Semaphore instead of a single-task gate.
 * Thread count is scaled by chip tier — stronger chips get more workers.
 * Worker threads request Darwin UTILITY QoS at start; macOS remains scheduler owner.
 */
public final class AdaptiveWorkerPool {
	private static AdaptiveWorkerPool INSTANCE;

	private final ExecutorService executor;
	private final int threadCount;
	private final Semaphore taskPermits;

	private AdaptiveWorkerPool(int threadCount) {
		this.threadCount = threadCount;
		// Allow up to threadCount * 2 concurrent tasks (some may be IO-bound)
		this.taskPermits = new Semaphore(Math.max(2, threadCount * 2));
		AtomicInteger seq = new AtomicInteger();
		ThreadFactory factory = r -> {
			Thread t = new Thread(() -> {
				DarwinQos.requestSelf(DarwinQos.UTILITY);
				r.run();
			}, "m3-frametime-worker-" + seq.getAndIncrement());
			t.setDaemon(true);
			return t;
		};
		this.executor = new ThreadPoolExecutor(
			threadCount, threadCount,
			0L, java.util.concurrent.TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(64),
			factory,
			new ThreadPoolExecutor.DiscardOldestPolicy()
		);
	}

	public static synchronized AdaptiveWorkerPool get() {
		if (INSTANCE == null) {
			INSTANCE = new AdaptiveWorkerPool(resolveThreadCount());
			M3FrametimeMod.LOGGER.info(
				"AdaptiveWorkerPool started with {} thread(s), {} task permits (sodium={})",
				INSTANCE.threadCount,
				INSTANCE.taskPermits.availablePermits(),
				StackCompat.sodium()
			);
		}
		return INSTANCE;
	}

	private static int resolveThreadCount() {
		int configured = M3FrametimeMod.config().workerThreads;
		if (configured > 0) {
			return Math.min(configured, Runtime.getRuntime().availableProcessors() / 2);
		}
		return StackCompat.preferredWorkerThreads();
	}

	/**
	 * Submit a background task. Tasks may run concurrently (up to permit limit).
	 * If all permits are taken, the task is dropped without blocking the caller.
	 */
	public void execute(Runnable task) {
		if (task == null) return;
		if (!taskPermits.tryAcquire()) {
			M3FrametimeMod.LOGGER.debug("Dropping background task — all permits taken");
			return;
		}
		try {
			executor.execute(() -> {
				try {
					task.run();
				} finally {
					taskPermits.release();
				}
			});
		} catch (RejectedExecutionException ignored) {
			taskPermits.release();
			M3FrametimeMod.LOGGER.debug("Dropping stale background task — queue full");
		}
	}
}
