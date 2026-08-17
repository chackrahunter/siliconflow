package dev.doncalvin.m3frametime.engine;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.math.FastMath;

/**
 * SiliconFlow In-Game Hardware & Performance Benchmark Engine.
 * Tests raw Apple Silicon ARM64 FastMath throughput, off-heap memory copy speeds,
 * and calculates the official "SiliconFlow Performance Index (SPI)".
 */
public final class SiliconBenchmark {
	private static final SiliconBenchmark INSTANCE = new SiliconBenchmark();

	private volatile boolean running = false;
	private volatile double progress = 0.0;
	private volatile long fastMathOpsPerSec = 0;
	private volatile double memoryBandwidthGbSec = 0.0;
	private volatile double performanceIndex = 0.0;
	private volatile String resultRating = "Not Tested";

	private SiliconBenchmark() {}

	public static SiliconBenchmark get() {
		return INSTANCE;
	}

	public boolean isRunning() {
		return running;
	}

	public double getProgress() {
		return progress;
	}

	public long getFastMathOpsPerSec() {
		return fastMathOpsPerSec;
	}

	public double getMemoryBandwidthGbSec() {
		return memoryBandwidthGbSec;
	}

	public double getPerformanceIndex() {
		return performanceIndex;
	}

	public String getResultRating() {
		return resultRating;
	}

	public String getRecommendedProfile() {
		if (performanceIndex >= 95.0) {
			return "PLAYABLE";
		} else if (performanceIndex >= 80.0) {
			return "BALANCED";
		} else if (performanceIndex >= 60.0) {
			return "MAX";
		} else {
			return "MAX";
		}
	}

	public void runBenchmarkAsync() {
		if (running) return;
		running = true;
		progress = 0.1;

		Thread benchThread = new Thread(() -> {
			try {
				// Phase 1: ARM64 FastMath Throughput (25 Million operations)
				progress = 0.3;
				long t0 = System.nanoTime();
				float acc = 0.0f;
				int iterations = 25_000_000;
				for (int i = 0; i < iterations; i++) {
					float angle = i * 0.001f;
					acc += FastMath.sin(angle) * FastMath.cos(angle);
				}
				if (acc == Float.MIN_VALUE) {
					M3FrametimeMod.LOGGER.debug("SiliconBenchmark math accumulator reached sentinel");
				}
				long elapsedMathNanos = System.nanoTime() - t0;
				double mathSec = Math.max(0.001, elapsedMathNanos / 1_000_000_000.0);
				this.fastMathOpsPerSec = (long) ((iterations * 2.0) / mathSec);

				// Phase 2: Memory Transfer & Cache Throughput Test
				progress = 0.7;
				int bufferSize = 4 * 1024 * 1024; // 4MB buffer (L2 Cache Slice)
				byte[] src = new byte[bufferSize];
				byte[] dst = new byte[bufferSize];
				for (int i = 0; i < bufferSize; i += 64) {
					src[i] = (byte) (i & 0xFF);
				}

				long t1 = System.nanoTime();
				int copies = 400; // 1.6 GB memory moved
				for (int i = 0; i < copies; i++) {
					System.arraycopy(src, 0, dst, 0, bufferSize);
				}
				if (dst[bufferSize - 64] == 127) {
					M3FrametimeMod.LOGGER.debug("SiliconBenchmark copy checksum reached sentinel");
				}
				long elapsedMemNanos = System.nanoTime() - t1;
				double memSec = Math.max(0.001, elapsedMemNanos / 1_000_000_000.0);
				double totalBytesMoved = (double) bufferSize * copies;
				this.memoryBandwidthGbSec = (totalBytesMoved / (1024.0 * 1024.0 * 1024.0)) / memSec;

				// Phase 3: Compute SiliconFlow Performance Index (SPI)
				progress = 1.0;
				double mathScore = Math.min(100.0, (fastMathOpsPerSec / 500_000_000.0) * 40.0);
				double memScore = Math.min(100.0, (memoryBandwidthGbSec / 40.0) * 30.0);
				this.performanceIndex = Math.min(100.0, Math.max(0.0, (mathScore + memScore) / 70.0 * 100.0));

				if (performanceIndex >= 95.0) {
					this.resultRating = "🍎 S+ Tier: Apple Silicon Ultra / Max (Legendary)";
				} else if (performanceIndex >= 85.0) {
					this.resultRating = "🚀 S Tier: Apple Silicon Pro (eSports Champion)";
				} else if (performanceIndex >= 70.0) {
					this.resultRating = "⚡ A Tier: Apple Silicon Base (High Performance)";
				} else {
					this.resultRating = "🔋 B Tier: Standard Power (Balanced)";
				}

			} catch (Throwable t) {
				M3FrametimeMod.LOGGER.warn("SiliconBenchmark failed: {}", t.toString());
				resultRating = "Benchmark failed";
			} finally {
				running = false;
			}
		}, "SiliconFlow-Benchmark");

		benchThread.setPriority(Thread.NORM_PRIORITY);
		benchThread.setDaemon(true);
		benchThread.start();
	}
}
