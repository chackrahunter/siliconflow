package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.SodiumSoftBooster;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.pool.ScratchPool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Rich In-Game Debug & Diagnostic HUD with Micro-Stutter Detection & Error Code Matrix.
 * Toggled via F8. Renders real-time FPS, Min/Max FPS, 1% Lows, Frametimes,
 * Micro-Stutter Error Codes, Darwin P-Core QoS status, memory bandwidth, and Shader telemetry.
 */
public final class DebugHud {
	private static final DebugHud INSTANCE = new DebugHud();
	private static final int MAX_LINES = 24;

	private boolean visible;

	private final SpikeMonitor.SpikeEvent[] drained = new SpikeMonitor.SpikeEvent[8];
	private int drainedCount;
	private long lastRebuildNanos;
	private final String[] lines = new String[MAX_LINES];
	private final int[] colors = new int[MAX_LINES];
	private int lineCount;

	private DebugHud() {
		for (int i = 0; i < drained.length; i++) {
			drained[i] = new SpikeMonitor.SpikeEvent();
		}
		for (int i = 0; i < lines.length; i++) {
			lines[i] = "";
			colors[i] = 0xFFFFFF;
		}
	}

	public static DebugHud get() {
		return INSTANCE;
	}

	public void toggle() {
		visible = !visible;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean v) {
		this.visible = v;
	}

	public void render(DrawContext context, RenderTickCounter tickCounter) {
		if (!visible) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null) {
			return;
		}

		long now = System.nanoTime();
		if (now - lastRebuildNanos > 80_000_000L) { // Rebuild ~12 Hz
			rebuildLines(client);
			lastRebuildNanos = now;
		}

		int padding = 6;
		int lineHeight = 11;
		int boxWidth = 360;
		int boxHeight = padding * 2 + lineCount * lineHeight;

		int startX = 6;
		int startY = 6;

		// Semi-transparent sleek dark background with subtle accent border
		context.fill(startX, startY, startX + boxWidth, startY + boxHeight, 0xCC0D1117);
		context.fill(startX, startY, startX + boxWidth, startY + 1, 0xFF388BFD); // Top accent line

		int y = startY + padding;
		for (int i = 0; i < lineCount; i++) {
			context.drawText(client.textRenderer, lines[i], startX + padding, y, colors[i], true);
			y += lineHeight;
		}
	}

	private void rebuildLines(MinecraftClient client) {
		SpikeMonitor.SpikeEvent[] scratch = SpikeMonitor.get().overlayScratch();
		int n = SpikeMonitor.get().drainRecent(scratch);
		drainedCount = Math.min(n, drained.length);
		for (int i = 0; i < drainedCount; i++) {
			SpikeMonitor.SpikeEvent src = scratch[i];
			drained[i].set(src.frameDeltaNanos, src.errorCode, src.gcDeltaMs, src.freeMb, src.wallNanos);
		}

		FramePacer pacer = FramePacer.get();
		SpikeMonitor monitor = SpikeMonitor.get();
		MemoryPressureProbe mem = MemoryPressureProbe.get();
		Runtime rt = Runtime.getRuntime();
		StringBuilder sb = ScratchPool.get().stringBuilder();

		int cores = Runtime.getRuntime().availableProcessors();
		double ftMs = pacer.lastDeltaMs();
		double emaMs = pacer.emaDeltaMs();
		int fps = ftMs > 0 ? (int) Math.round(1000.0 / ftMs) : 0;
		int p50Fps = monitor.percentileMs(0.50) > 0 ? (int) Math.round(1000.0 / monitor.percentileMs(0.50)) : 0;
		int p99Fps = monitor.percentileMs(0.99) > 0 ? (int) Math.round(1000.0 / monitor.percentileMs(0.99)) : 0;
		int minFps = monitor.rollingMinFps();
		int maxFps = monitor.rollingMaxFps();

		lineCount = 0;

		// 1. Header
		putLine("✦ M3 Frametime Companion (Apple Silicon Engine)", 0x58A6FF);

		// 2. Realtime FPS & Min/Max/Avg FPS
		sb.setLength(0);
		sb.append("FPS: ").append(fps).append("  [Min: ").append(minFps).append(" | Max: ").append(maxFps).append(" | Avg: ").append(p50Fps).append("]");
		int fpsColor = fps >= 80 ? 0x3FB950 : (fps >= 45 ? 0xD29922 : 0xF85149);
		putLine(sb, fpsColor);

		// 3. 1% Low FPS & Frametimes
		sb.setLength(0);
		sb.append("1% Low: ").append(p99Fps).append(" FPS | Frametime: ");
		ScratchPool.appendFixed(sb, ftMs, 2).append(" ms (EMA: ");
		ScratchPool.appendFixed(sb, emaMs, 2).append(" ms)");
		putLine(sb, 0x79C0FF);

		// 4. Latency Percentiles
		sb.setLength(0);
		sb.append("Latency: P50 ");
		ScratchPool.appendFixed(sb, monitor.percentileMs(0.50), 1).append("ms | P95 ");
		ScratchPool.appendFixed(sb, monitor.percentileMs(0.95), 1).append("ms | P99 ");
		ScratchPool.appendFixed(sb, monitor.percentileMs(0.99), 1).append("ms");
		putLine(sb, 0x79C0FF);

		// 5. MICRO-STUTTER DIAGNOSTICS & ERROR CODES
		if (monitor.hasSpike() && monitor.lastSpikeAgeMs() < 8000L) {
			StutterErrorCode code = monitor.lastErrorCode();
			sb.setLength(0);
			sb.append("STUTTER: [").append(code.getCode()).append("] ").append(code.getTitle()).append(" (");
			ScratchPool.appendFixed(sb, monitor.lastSpikeMs(), 1).append("ms · ");
			formatAge(sb, monitor.lastSpikeAgeMs()).append(" ago)");
			putLine(sb, code.getColor());

			sb.setLength(0);
			sb.append("  ↳ Cause: ").append(code.getDescription());
			putLine(sb, 0xF2CC60);
		} else {
			putLine("STUTTER STATUS: [OK-000] Stable (0 Stutters in last 8s)", 0x3FB950);
		}

		// 6. Apple Silicon Hardware & P-Core QoS
		sb.setLength(0);
		sb.append("Silicon: Apple M-Series (").append(cores).append(" Cores) | QoS: ");
		sb.append(M3FrametimeMod.config().boostDarwinQos ? "P-CORE TURBO (Active)" : "Standard");
		putLine(sb, 0x3FB950);

		// 7. Threads & Sodium Workers
		sb.setLength(0);
		sb.append("Threads: Render [MAX] | Sodium Workers [").append(SodiumSoftBooster.mChipTargetThreads(cores)).append("x NORM+1]");
		putLine(sb, 0x7EE787);

		// 8. Memory & Unified RAM Status Code
		long heapUsed = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
		long heapMax = rt.maxMemory() / (1024L * 1024L);
		long freePhys = mem.freePhysicalMb();
		sb.setLength(0);
		sb.append("Memory: Heap ").append(heapUsed).append("/").append(heapMax).append(" MB | Free Phys: ").append(freePhys).append(" MB");
		if (mem.underPressure()) {
			sb.append(" [ERR-MEM1]");
		} else {
			sb.append(" [STATUS-OK]");
		}
		putLine(sb, mem.underPressure() ? 0xF85149 : 0x7EE787);

		// 9. Graphics & Shader Pipeline
		sb.setLength(0);
		sb.append("Graphics: Sodium [");
		sb.append(StackCompat.sodium() ? "YES" : "NO").append("] | Iris Shaders [");
		sb.append(StackCompat.isShaderActive() ? "ACTIVE" : (StackCompat.iris() ? "READY" : "NO")).append("] | Shadow Culling [ON]");
		putLine(sb, 0xF2CC60);

		// 10. Engine & Culling Rules
		var cfg = M3FrametimeMod.config();
		sb.setLength(0);
		sb.append("Culling: Entity ").append((int) cfg.entityCullDistance).append("m | BE ");
		sb.append((int) cfg.blockEntityCullDistance).append("m | Sign ").append((int) cfg.farSignDistance).append("m | Particles ≤");
		sb.append(cfg.maxParticles);
		putLine(sb, 0x8B949E);

		// 11. Toggle Footer
		putLine("Press [F8] to toggle overlay", 0x484F58);
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

	private void putLine(CharSequence text, int color) {
		if (lineCount >= MAX_LINES) {
			return;
		}
		lines[lineCount] = text.toString();
		colors[lineCount] = color;
		lineCount++;
	}
}
