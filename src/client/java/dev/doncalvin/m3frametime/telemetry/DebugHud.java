package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.math.FastMath;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.pool.ScratchPool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 1:1 Pixel-Perfect Glassmorphic Futuristic F8 Debug & Diagnostic HUD.
 * Renders glowing telemetry pills, live animated wave curves, real-time FPS/frametimes,
 * measured memory/network diagnostics, and the LED Dot-Matrix Status Badge.
 * 100% non-blocking, zero-lag, real-time responsive.
 */
public final class DebugHud {
	private static final DebugHud INSTANCE = new DebugHud();
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final long FPS_WINDOW_NANOS = 250_000_000L;
	private static final float TWO_PI = 6.2831855f;
	private static final int[] LED_PATTERN = {4, 6, 7, 31, 28, 12, 4};

	private boolean visible = false;
	private boolean enabled = true;
	private float wavePhase = 0.0f;
	private long lastRenderNanos;
	private long cachedClockSecond = Long.MIN_VALUE;
	private String cachedTime = "--:--:--";

	// Frame timing and live FPS measurement
	private long lastFrameNanos = 0;
	private long fpsWindowStartNanos = 0;
	private int frameCountInWindow = 0;
	private long frameNanosInWindow;

	// Live smooth display metrics
	private volatile int liveFps = 60;
	private volatile double liveFtMs = 16.6;
	private volatile double minFtMs = 16.6;
	private volatile double maxFtMs = 16.6;
	private volatile boolean shaderActive;

	// Ring buffer for min/max calculation (32 samples)
	private static final int RING_SIZE = 32;
	private final double[] ftHistory = new double[RING_SIZE];
	private int historyIdx = 0;
	private int historyCount = 0;

	private DebugHud() {}

	public static DebugHud get() {
		return INSTANCE;
	}

	public void toggle() {
		if (enabled) {
			visible = !visible;
		}
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean v) {
		this.visible = enabled && v;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (!enabled) {
			this.visible = false;
		}
	}

	public int getLiveFps() {
		return liveFps > 0 ? liveFps : 60;
	}

	public double getLiveFrametimeMs() {
		return liveFtMs > 0.0 ? liveFtMs : 16.6;
	}

	/** Called every frame from MinecraftClientMixin — must run unconditionally. */
	public void onFrameTick() {
		long now = System.nanoTime();
		if (fpsWindowStartNanos == 0L) {
			fpsWindowStartNanos = now;
			lastFrameNanos = now;
			return;
		}

		// Measure real frame-to-frame delta in milliseconds
		if (lastFrameNanos > 0) {
			long deltaNanos = now - lastFrameNanos;
			double deltaMs = deltaNanos / 1_000_000.0;
			if (deltaMs > 0.05 && deltaMs < 250.0) {
				ftHistory[historyIdx & (RING_SIZE - 1)] = deltaMs;
				historyIdx++;
				historyCount = Math.min(RING_SIZE, historyCount + 1);
				frameCountInWindow++;
				frameNanosInWindow += deltaNanos;
			}
		}
		lastFrameNanos = now;

		// Calculate live rolling FPS over 250ms windows for crisp real-time reactivity
		if (now - fpsWindowStartNanos >= FPS_WINDOW_NANOS && frameCountInWindow > 0) {
			double elapsedSec = (now - fpsWindowStartNanos) / 1_000_000_000.0;
			int calculatedFps = (int) Math.round(frameCountInWindow / Math.max(0.001, elapsedSec));
			liveFps = Math.max(1, calculatedFps);

			liveFtMs = frameNanosInWindow > 0L
				? frameNanosInWindow / (frameCountInWindow * 1_000_000.0)
				: 1000.0 / Math.max(1, liveFps);

			// Calculate rolling min/max
			double min = 999.0;
			double max = 0.0;
			for (int i = 0; i < historyCount; i++) {
				int index = (historyIdx - historyCount + i) & (RING_SIZE - 1);
				double d = ftHistory[index];
				if (d > 0.05) {
					if (d < min) min = d;
					if (d > max) max = d;
				}
			}
			minFtMs = min < 900.0 ? min : liveFtMs * 0.85;
			maxFtMs = max > 0.0 ? max : liveFtMs * 1.45;

			shaderActive = StackCompat.isShaderActive();
			boolean shadowPass = StackCompat.isShadowPass();
			MemoryPressureProbe memory = MemoryPressureProbe.get();
			SpikeMonitor spikes = SpikeMonitor.get();
			long spikeAge = spikes.lastSpikeAgeMs();
			TelemetrySnapshot.publish(new TelemetrySnapshot(
				now,
				liveFps,
				liveFtMs,
				FramePacer.get().emaDeltaMs(),
				minFtMs,
				maxFtMs,
				memory.heapUsedMb(),
				memory.heapMaxMb(),
				memory.freePhysicalMb(),
				shaderActive,
				shadowPass,
				memory.underPressure(),
				memory.pressureAgeMs(),
				memory.pressureThresholdMb(),
				M3FrametimeMod.config().boostDarwinQos,
				spikeAge >= 0L && spikeAge < 5000L,
				spikes.spikeCount(),
				spikeAge,
				spikes.lastErrorCode()
			));

			frameCountInWindow = 0;
			frameNanosInWindow = 0L;
			fpsWindowStartNanos = now;
		}
	}

	public void render(DrawContext context, RenderTickCounter tickCounter) {
		if (!enabled || !visible) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null) {
			return;
		}

		// Time-based animation keeps the HUD stable across different frame rates.
		long nowNanos = System.nanoTime();
		if (lastRenderNanos == 0L) {
			lastRenderNanos = nowNanos;
		}
		float deltaSeconds = Math.min(0.1f, Math.max(0.0f, (nowNanos - lastRenderNanos) / 1_000_000_000.0f));
		lastRenderNanos = nowNanos;
		wavePhase += deltaSeconds * 2.2f;
		if (wavePhase > TWO_PI) {
			wavePhase -= TWO_PI;
		}

		var tr = client.textRenderer;
		int x = 8;
		int y = 8;

		// ==========================================
		// 1. TOP HEADER PILL
		// [SILICONFLOW] | [MC ...] | [HH:MM:SS]
		// ==========================================
		String mcVer = dev.doncalvin.m3frametime.version.VersionDetector.get().getRawVersion();
		long clockSecond = System.currentTimeMillis() / 1000L;
		if (clockSecond != cachedClockSecond) {
			cachedClockSecond = clockSecond;
			cachedTime = LocalTime.now().format(TIME_FMT);
		}
		String headerText = "[SILICONFLOW] | [MC " + mcVer + "] | [" + cachedTime + "]";
		int headerW = tr.getWidth(headerText) + 10;
		int headerH = 13;

		drawGlassBox(context, x, y, headerW, headerH, 0xFF00E5FF, 0xD00A1118);
		context.drawText(tr, headerText, x + 5, y + 3, 0xFF00F2FE, false);

		y += headerH + 5;

		// ==========================================
		// 2. CARD 1: PERFORMANCE (LIVE FPS & FT WAVE)
		// ==========================================
		int cardW = 160;
		int card1H = 82;
		drawGlassBox(context, x, y, cardW, card1H, 0xFF00E5FF, 0xD00A1118);

		// Row 1: Live FPS
		StringBuilder sb = ScratchPool.get().stringBuilder();
		sb.append("FPS: ");
		ScratchPool.appendFixed(sb, (double) liveFps, 1);
		context.drawText(tr, sb.toString(), x + 6, y + 5, 0xFFFFE082, false);

		SpikeMonitor statusMonitor = SpikeMonitor.get();
		double statusP95 = statusMonitor.percentileMs(0.95);
		boolean statusBreach = statusMonitor.lastSpikeAgeMs() >= 0L && statusMonitor.lastSpikeAgeMs() < 5000L || statusP95 >= Math.max(33.3, M3FrametimeMod.config().spikeThresholdMs);
		String statusTag = statusBreach ? "[STUTTER]" : (liveFps >= 120 ? "[HIGH/STABLE]" : "[STABLE]");
		context.drawText(tr, statusTag, x + cardW - tr.getWidth(statusTag) - 6, y + 5, statusBreach ? 0xFFF85149 : 0xFF38EF7D, false);

		// Micro FPS bar line under FPS row
		context.fill(x + 6, y + 15, x + cardW - 6, y + 16, 0x5500E5FF);
		context.fill(x + 6, y + 15, x + 6 + (int) ((cardW - 12) * Math.min(1.0, liveFps / 1000.0)), y + 16, 0xFF00E5FF);

		// Row 2: Live FT ms [MIN / MAX]
		int ftLabelW = tr.getWidth("FT: ");
		context.drawText(tr, "FT: ", x + 6, y + 21, 0xFFFFFFFF, false);

		sb.setLength(0);
		ScratchPool.appendFixed(sb, liveFtMs, 1).append(" ms");
		context.drawText(tr, sb.toString(), x + 6 + ftLabelW, y + 21, 0xFFFFCA28, false);

		sb.setLength(0);
		sb.append("[MIN: ");
		ScratchPool.appendFixed(sb, minFtMs, 1).append(" / MAX: ");
		ScratchPool.appendFixed(sb, maxFtMs, 1).append("]");
		String minMaxStr = sb.toString();
		context.drawText(tr, minMaxStr, x + cardW - tr.getWidth(minMaxStr) - 6, y + 21, 0xFF90A4AE, false);

		SpikeMonitor frameStats = SpikeMonitor.get();
		sb.setLength(0);
		sb.append("P95/P99: ");
		ScratchPool.appendFixed(sb, frameStats.percentileMs(0.95), 1).append("/");
		ScratchPool.appendFixed(sb, frameStats.percentileMs(0.99), 1).append(" ms · 1%-low: ");
		double lowMs = frameStats.percentileMs(0.99);
		ScratchPool.appendFixed(sb, lowMs > 0.0 ? 1000.0 / lowMs : 0.0, 0).append(" FPS");
		context.drawText(tr, sb.toString(), x + 6, y + 34, 0xFFCBD5E1, false);

		// Row 3: Smooth Live Cyan Sine-Wave Curve
		int waveY = y + 54;
		int waveH = 14;
		int waveStartX = x + 6;
		int waveEndX = x + cardW - 6;
		boolean useFastMath = M3FrametimeMod.config().useFastMath;

		// Wave background glow track
		context.fill(waveStartX, waveY - waveH / 2, waveEndX, waveY + waveH / 2, 0x2000E5FF);

		int prevWavePtY = waveY;
		for (int px = waveStartX; px < waveEndX; px += 2) {
			float normX = (float) (px - waveStartX) / (float) (waveEndX - waveStartX);
			float s1 = useFastMath
				? FastMath.sin((normX * 9.0f) + wavePhase)
				: (float) Math.sin((normX * 9.0f) + wavePhase);
			float s2 = useFastMath
				? FastMath.cos((normX * 4.0f) - wavePhase * 0.7f)
				: (float) Math.cos((normX * 4.0f) - wavePhase * 0.7f);
			int curWavePtY = waveY + (int) ((s1 * 0.7f + s2 * 0.3f) * (waveH / 2.2f));

			if (px > waveStartX) {
				int minY = Math.min(prevWavePtY, curWavePtY);
				int maxY = Math.max(prevWavePtY, curWavePtY);
				context.fill(px, minY, Math.min(waveEndX, px + 2), maxY + 1, 0xFF00F2FE);
			}
			prevWavePtY = curWavePtY;
		}

		y += card1H + 5;

		// ==========================================
		// 3. CARD 2: NETWORK & SYNC (TPS & PING)
		// ==========================================
		int card2H = 28;
		drawGlassBox(context, x, y, cardW, card2H, 0xFFFFA726, 0xD00A1118);

		context.drawText(tr, "SERVER SYNC", x + 6, y + 5, 0xFFFFFFFF, false);

		// Live Ping Query
		int livePing = -1;
		if (client.getNetworkHandler() != null && client.player != null) {
			PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
			if (entry != null && entry.getLatency() >= 0) {
				livePing = entry.getLatency();
			}
		}

		sb.setLength(0);
		if (livePing >= 0) {
			sb.append("PING: ").append(livePing).append(" ms");
		} else {
			sb.append("PING: unavailable");
		}
		context.drawText(tr, sb.toString(), x + 6, y + 16, 0xFFFFCA28, false);

		y += card2H + 5;

		// ==========================================
		// 4. CARD 3: MEASURED HARDWARE STATUS
		// ==========================================
		int card3H = 58;
		drawGlassBox(context, x, y, cardW, card3H, 0xFFFFA726, 0xD00A1118);

		context.drawText(tr, "HARDWARE STATUS", x + 6, y + 4, 0xFFFFFFFF, false);
		context.drawText(tr, "GPU UTIL: unavailable", x + 6, y + 15, 0xFFFFCA28, false);
		context.drawText(tr, "CPU UTIL: unavailable", x + 6, y + 27, 0xFFFFFFFF, false);
		TelemetrySnapshot telemetry = TelemetrySnapshot.current();
		context.drawText(tr, "SHADER API: " + (telemetry.shaderActive() ? "active" : "inactive"), x + 6, y + 39, 0xFF00F2FE, false);
		context.drawText(tr, "SHADOW PASS: " + (telemetry.shadowPass() ? "active" : "idle"), x + 6, y + 51, 0xFF90A4AE, false);

		y += card3H + 5;

		// ==========================================
		// 5. BOTTOM ROW: MEMORY CARD + STATUS MATRIX BADGE
		// ==========================================
		int memCardW = 145;
		int bottomCardH = 48;
		drawGlassBox(context, x, y, memCardW, bottomCardH, 0xFFFFA726, 0xD00A1118);

		context.drawText(tr, "Memory", x + 6, y + 4, 0xFFFFFFFF, false);

		// Live RAM Usage
		Runtime rt = Runtime.getRuntime();
		long heapUsed = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
		long heapMax = rt.maxMemory() / (1024L * 1024L);
		double ramGb = heapUsed / 1024.0;
		double maxGb = heapMax / 1024.0;

		sb.setLength(0);
		sb.append("RAM USE: ");
		ScratchPool.appendFixed(sb, ramGb, 1).append(" GB / ");
		ScratchPool.appendFixed(sb, maxGb, 1).append(" GB");
		context.drawText(tr, sb.toString(), x + 6, y + 15, 0xFFFFCA28, false);
		drawProgressBar(context, x + 6, y + 24, memCardW - 12, 3, (float) (heapUsed / (double) Math.max(1L, heapMax)), 0xFFFFA726);

		MemoryPressureProbe pressure = MemoryPressureProbe.get();
		String pressureText = pressure.physicalPressureState();
		context.drawText(tr, pressureText + " free: " + pressure.freePhysicalMb() + "/" + pressure.pressureThresholdMb() + " MB", x + 6, y + 29, pressure.underPressure() ? 0xFFF85149 : 0xFF00F2FE, false);

		// ==========================================
		// 6. STATUS MATRIX BADGE (NEXT TO MEMORY)
		// ==========================================
		int badgeX = x + memCardW + 6;
		int badgeW = 180;
		drawGlassBox(context, badgeX, y, badgeW, bottomCardH, 0xFFFFA726, 0xD00A1118);

		// Left: Dot-Matrix LED Grid (5x7)
		int ledStartX = badgeX + 6;
		int ledStartY = y + 8;
		drawLedMatrixIcon(context, ledStartX, ledStartY);

		// Right: Status Text
		int textStartX = ledStartX + 22;
		SpikeMonitor monitor = SpikeMonitor.get();
		boolean recentSpike = monitor.hasSpike() && monitor.lastSpikeAgeMs() >= 0 && monitor.lastSpikeAgeMs() < 5000L;
		context.drawText(tr, "STATUS: " + (recentSpike ? "STUTTER" : "STABLE"), textStartX, y + 7, recentSpike ? 0xFFF85149 : 0xFF38EF7D, false);
		context.drawText(tr, "FPS: measured 250ms", textStartX, y + 17, 0xFF38EF7D, false);
		TelemetrySnapshot current = TelemetrySnapshot.current();
		StringBuilder pacingText = ScratchPool.get().stringBuilder();
		pacingText.append("PACING EMA: ");
		if (current.emaFrametimeMs() > 0.0) {
			ScratchPool.appendFixed(pacingText, current.emaFrametimeMs(), 1).append(" ms");
		} else {
			pacingText.append("n/a");
		}
		context.drawText(tr, pacingText.toString(), textStartX, y + 27, 0xFF00F2FE, false);
		context.drawText(tr, recentSpike ? "SPIKE DETECTED" : "NO RECENT SPIKE", textStartX, y + 37, 0xFF90A4AE, false);
	}


	private static void drawGlassBox(DrawContext context, int x, int y, int width, int height, int borderColor, int bgColor) {
		// Background box with glassmorphic transparency
		context.fill(x, y, x + width, y + height, bgColor);

		// Crisp 1px neon border
		context.fill(x, y, x + width, y + 1, borderColor);
		context.fill(x, y + height - 1, x + width, y + height, borderColor);
		context.fill(x, y, x + 1, y + height, borderColor);
		context.fill(x + width - 1, y, x + width, y + height, borderColor);
	}

	private static void drawProgressBar(DrawContext context, int x, int y, int width, int height, float progress, int barColor) {
		// Track background (darker semi-transparent)
		context.fill(x, y, x + width, y + height, 0x40FFFFFF);

		// Filled active portion
		int filledW = (int) (width * Math.max(0.0f, Math.min(1.0f, progress)));
		if (filledW > 0) {
			context.fill(x, y, x + filledW, y + height, barColor);
		}
	}

	private static void drawLedMatrixIcon(DrawContext context, int startX, int startY) {
		int dotSize = 2;
		int gap = 1;

		for (int r = 0; r < 7; r++) {
			for (int c = 0; c < 5; c++) {
				int px = startX + c * (dotSize + gap);
				int py = startY + r * (dotSize + gap);

				if ((LED_PATTERN[r] & (1 << c)) != 0) {
					context.fill(px, py, px + dotSize, py + dotSize, 0xFFFFA726); // Bright Glowing Amber
				} else {
					context.fill(px, py, px + dotSize, py + dotSize, 0x30FFA726); // Dim Background LED
				}
			}
		}
	}
}
