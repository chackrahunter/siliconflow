package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.pacing.FramePacer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Opt-in bounded recorder. Frame writes are primitive-only; snapshots are immutable. */
public final class PerformanceRecorder {
    private static final PerformanceRecorder INSTANCE = new PerformanceRecorder();
    private static final int MAX_WINDOW = 2048;
    private final AtomicReference<Snapshot> published = new AtomicReference<>(Snapshot.unavailable());
    private final long[] intervals = new long[MAX_WINDOW];
    private final boolean[] spikes = new boolean[MAX_WINDOW];
    private final String[] codes = new String[MAX_WINDOW];
    private int cursor;
    private int count;
    private long lastPublishNanos;
    private long recordedFrames;
    private long recordedSpikes;
    private long lastDiagnosticsNanos;

    private PerformanceRecorder() {}
    public static PerformanceRecorder get() { return INSTANCE; }

    // All three entry points run on the client/render thread (SpikeMonitor.onFrameEnd,
    // MinecraftClientMixin, LiveTelemetryStream before its worker dispatch), so no lock is
    // needed; the published Snapshot is the only cross-thread handoff and stays an AtomicReference.
    public void recordFrame(long intervalNanos, boolean spike, StutterErrorCode code) {
        if (!M3FrametimeMod.config().performanceRecorderEnabled || intervalNanos <= 0) return;
        int w = window();
        // Guard against the window being lowered at runtime (keeps cursor/count in the new range).
        if (cursor >= w) cursor = 0;
        if (count > w) count = w;
        int index = cursor;
        intervals[index] = intervalNanos;
        spikes[index] = spike;
        codes[index] = code == null ? StutterErrorCode.OK_000.getCode() : code.getCode();
        cursor = (cursor + 1) % w;
        if (count < w) count++;
        recordedFrames++;
        if (spike) recordedSpikes++;
    }

    /** Cheap render-thread gate; the actual system probes remain on the worker. */
    public void sampleDiagnostics() {
        if (!M3FrametimeMod.config().performanceRecorderEnabled) return;
        long now = System.nanoTime();
        if (now - lastDiagnosticsNanos < 250_000_000L) return;
        lastDiagnosticsNanos = now;
        MemoryPressureProbe mem = MemoryPressureProbe.get();
        published.set(new Snapshot(now, recordedFrames, recordedSpikes, count,
            mem.heapUsedMb(), mem.heapMaxMb(), mem.freePhysicalMb(), mem.pressureAgeMs(),
            GcProbe.get().frameGcDeltaMs(), GcProbe.get().frameGcCountDelta(),
            StackCompat.isShaderActive(), StackCompat.isShadowPass(),
            M3FrametimeMod.config().pacingEnabled, FramePacer.get().targetFrameNanos(),
            M3FrametimeMod.config().workerThreads, "measured"));
    }

    public Snapshot snapshot() {
        if (!M3FrametimeMod.config().performanceRecorderEnabled) return Snapshot.unavailable();
        Snapshot base = published.get();
        int w = window();
        int n = Math.min(count, w);
        double[] ms = new double[n];
        String[] errorCodes = new String[n];
        boolean[] spikeFlags = new boolean[n];
        int start = (cursor - n + w) % w;
        for (int i = 0; i < n; i++) {
            int index = (start + i) % w;
            ms[i] = intervals[index] / 1_000_000.0;
            errorCodes[i] = codes[index];
            spikeFlags[i] = spikes[index];
        }
        return base.withWindow(ms, spikeFlags, errorCodes);
    }

    private int window() { return Math.max(32, Math.min(MAX_WINDOW, M3FrametimeMod.config().performanceRecorderWindow)); }

    public record Snapshot(long timestampNanos, long recordedFrames, long recordedSpikes, int windowSize,
                          long heapUsedMb, long heapMaxMb, long freePhysicalMb, long pressureAgeMs,
                          long gcPauseDeltaMs, long gcCountDelta, boolean shaderActive, boolean shadowPass,
                          boolean pacingEnabled, long pacingTargetNanos, int workerThreads, String source,
                          double[] frametimeMs, boolean[] spikeFlags, String[] errorCodes) {
        static Snapshot unavailable() { return new Snapshot(0, 0, 0, 0, -1, -1, -1, -1, -1, -1, false, false, false, -1, -1, "unavailable", new double[0], new boolean[0], new String[0]); }
        Snapshot(long timestampNanos, long recordedFrames, long recordedSpikes, int windowSize, long heapUsedMb, long heapMaxMb, long freePhysicalMb, long pressureAgeMs, long gcPauseDeltaMs, long gcCountDelta, boolean shaderActive, boolean shadowPass, boolean pacingEnabled, long pacingTargetNanos, int workerThreads, String source) { this(timestampNanos, recordedFrames, recordedSpikes, windowSize, heapUsedMb, heapMaxMb, freePhysicalMb, pressureAgeMs, gcPauseDeltaMs, gcCountDelta, shaderActive, shadowPass, pacingEnabled, pacingTargetNanos, workerThreads, source, new double[0], new boolean[0], new String[0]); }
        Snapshot withWindow(double[] ms, boolean[] flags, String[] codes) { return new Snapshot(timestampNanos, recordedFrames, recordedSpikes, windowSize, heapUsedMb, heapMaxMb, freePhysicalMb, pressureAgeMs, gcPauseDeltaMs, gcCountDelta, shaderActive, shadowPass, pacingEnabled, pacingTargetNanos, workerThreads, source, Arrays.copyOf(ms, ms.length), Arrays.copyOf(flags, flags.length), Arrays.copyOf(codes, codes.length)); }
    }
}
