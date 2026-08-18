package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ChipPower;
import dev.doncalvin.m3frametime.compat.IrisCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.engine.ShaderAutoThrottle;
import dev.doncalvin.m3frametime.engine.SiliconCpuTopology;
import dev.doncalvin.m3frametime.pacing.FramePacer;
import dev.doncalvin.m3frametime.pool.ScratchPool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Compact F8 telemetry strip. Hidden overlay skips all draw work.
 */
public final class DebugHud {
	private static final DebugHud INSTANCE = new DebugHud();
	private static final long FPS_WINDOW_NANOS = 250_000_000L;
	private static final int RING_SIZE = 32;
	private static final int STRIP_W = 180;
	private static final int STRIP_H = 120;
	private static final int SPARK_BARS = 32;
	private static final int SPARK_H = 14;

	private boolean visible = false;
	private boolean enabled = true;

	private long lastFrameNanos = 0;
	private long fpsWindowStartNanos = 0;
	private int frameCountInWindow = 0;
	private long frameNanosInWindow;

	private volatile int liveFps = 60;
	private volatile double liveFtMs = 16.6;
	private volatile double minFtMs = 16.6;
	private volatile double maxFtMs = 16.6;
	private volatile boolean shaderActive;

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

		if (now - fpsWindowStartNanos >= FPS_WINDOW_NANOS && frameCountInWindow > 0) {
			double elapsedSec = (now - fpsWindowStartNanos) / 1_000_000_000.0;
			int calculatedFps = (int) Math.round(frameCountInWindow / Math.max(0.001, elapsedSec));
			liveFps = Math.max(1, calculatedFps);

			liveFtMs = frameNanosInWindow > 0L
				? frameNanosInWindow / (frameCountInWindow * 1_000_000.0)
				: 1000.0 / Math.max(1, liveFps);

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

			shaderActive = IrisCompat.isShaderActive();
			boolean shadowPass = IrisCompat.isShadowPass();
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
				false,
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
		if (!M3FrametimeMod.config().overlayEnabled || !enabled || !visible) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.textRenderer == null) {
			return;
		}

		TextRenderer tr = client.textRenderer;
		int x = 4;
		int y = 4;
		drawStrip(context, x, y, STRIP_W, STRIP_H);

		M3Config cfg = M3FrametimeMod.config();
		SpikeMonitor spikes = SpikeMonitor.get();
		MemoryPressureProbe memory = MemoryPressureProbe.get();
		SiliconCpuTopology topo = SiliconCpuTopology.get();
		StringBuilder sb = ScratchPool.get().stringBuilder();

		int tx = x + 5;
		int row1 = y + 4;
		int sparkX = x + STRIP_W - SPARK_BARS - 6;
		int sparkY = y + 5;

		sb.append("FPS ").append(liveFps).append("  FT ");
		ScratchPool.appendFixed(sb, liveFtMs, 1).append("ms");
		context.drawText(tr, sb.toString(), tx, row1, 0xFFFFE082, false);

		sb.setLength(0);
		sb.append("p95 ");
		ScratchPool.appendFixed(sb, spikes.rollingP95Ms(), 1).append("  p99 ");
		ScratchPool.appendFixed(sb, spikes.rollingP99Ms(), 1);
		context.drawText(tr, sb.toString(), tx, row1 + 10, 0xFFCBD5E1, false);

		drawSparkline(context, sparkX, sparkY, SPARK_H);

		int row2 = y + 42;
		context.fill(x + 5, row2 - 3, x + STRIP_W - 5, row2 - 2, 0x55334155);

		RamDiscipline ram = RamDiscipline.get();
		boolean heapUmaBad = ram.heapVsPhysicalUnhealthy();
		RamDiscipline.PressureLevel press = ram.pressureLevel();
		ShaderAutoThrottle.Level throttle = ShaderAutoThrottle.get().level();
		String profile = cfg.performanceProfile;
		if (profile == null || profile.isBlank()) {
			profile = "PLAYABLE";
		} else {
			profile = profile.trim().toUpperCase();
		}

		sb.setLength(0);
		sb.append(shortChipName(topo.getChipName())).append(' ').append(topo.getChipTier().name());
		sb.append("  ").append(profile);
		context.drawText(tr, trimToWidth(tr, sb.toString(), STRIP_W - 12), tx, row2, 0xFF00F2FE, false);

		sb.setLength(0);
		sb.append("HEAP ").append(memory.heapUsedMb()).append('/').append(memory.heapMaxMb());
		long umaFree = memory.freePhysicalMb();
		if (umaFree >= 0L) {
			sb.append("  UMA ").append(umaFree);
		} else {
			sb.append("  UMA n/a");
		}
		if (heapUmaBad) {
			sb.append(" mismatch");
		}
		context.drawText(tr, trimToWidth(tr, sb.toString(), STRIP_W - 12), tx, row2 + 10, heapUmaBad ? 0xFFF85149 : 0xFFFFCA28, false);

		sb.setLength(0);
		sb.append("JITTER ");
		ScratchPool.appendFixed(sb, FramePacer.get().jitterPercent(), 1).append('%');
		StutterErrorCode last = spikes.lastErrorCode();
		long ageMs = spikes.lastSpikeAgeMs();
		boolean recent = spikes.hasSpike() && ageMs >= 0L && ageMs < 5000L;
		sb.append("  ").append(last.getCode());
		if (spikes.hasSpike()) {
			sb.append(' ');
			appendAge(sb, ageMs);
		}
		context.drawText(tr, trimToWidth(tr, sb.toString(), STRIP_W - 12), tx, row2 + 20, recent ? 0xFFF85149 : 0xFFCBD5E1, false);

		sb.setLength(0);
		sb.append("THRTL ").append(throttle.name()).append("  PRESS ").append(press.name());
		int throttleColor = throttle == ShaderAutoThrottle.Level.L2 ? 0xFFF85149
			: (throttle == ShaderAutoThrottle.Level.L1 ? 0xFFFBBF24
				: (throttle == ShaderAutoThrottle.Level.L0 ? 0xFF00F2FE : 0xFF94A3B8));
		if (press == RamDiscipline.PressureLevel.PRESSURE) {
			throttleColor = 0xFFF85149;
		} else if (press == RamDiscipline.PressureLevel.CAUTION && throttleColor == 0xFF94A3B8) {
			throttleColor = 0xFFFBBF24;
		}
		context.drawText(tr, sb.toString(), tx, row2 + 30, throttleColor, false);

		int lineY = row2 + 40;
		if (IrisCompat.isIrisLoaded()) {
			TelemetrySnapshot snap = TelemetrySnapshot.current();
			sb.setLength(0);
			sb.append("SHADER ").append(snap.shaderActive() ? "on" : "off");
			sb.append("  SHADOW ").append(snap.shadowPass() ? "on" : "idle");
			context.drawText(tr, sb.toString(), tx, lineY, snap.shaderActive() ? 0xFF00F2FE : 0xFF94A3B8, false);
			lineY += 10;
		}

		String sodiumWarn = ChipPower.sodiumAllocatorWarning();
		if (sodiumWarn != null) {
			context.drawText(tr, trimToWidth(tr, "SODIUM not SWAP", STRIP_W - 12), tx, lineY, 0xFFFBBF24, false);
		}
	}

	private void drawSparkline(DrawContext context, int sparkX, int sparkY, int sparkH) {
		if (historyCount <= 0) {
			return;
		}
		double scale = 33.3;
		for (int i = 0; i < historyCount; i++) {
			int index = (historyIdx - historyCount + i) & (RING_SIZE - 1);
			double d = ftHistory[index];
			if (d > scale) {
				scale = d;
			}
		}
		int bars = Math.min(SPARK_BARS, historyCount);
		int start = historyCount - bars;
		for (int i = 0; i < bars; i++) {
			int index = (historyIdx - historyCount + start + i) & (RING_SIZE - 1);
			double d = ftHistory[index];
			int barH = (int) Math.round((d / scale) * sparkH);
			if (barH < 1) {
				barH = 1;
			} else if (barH > sparkH) {
				barH = sparkH;
			}
			int bx = sparkX + i;
			int by = sparkY + sparkH - barH;
			int color = d >= 33.3 ? 0xFFF85149 : (d >= 16.6 ? 0xFFFBBF24 : 0xFF38EF7D);
			context.fill(bx, by, bx + 1, sparkY + sparkH, color);
		}
	}

	private static void drawStrip(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, 0xF00A1118);
		context.fill(x, y, x + width, y + 1, 0xFF334155);
		context.fill(x, y + height - 1, x + width, y + height, 0xFF334155);
		context.fill(x, y, x + 1, y + height, 0xFF334155);
		context.fill(x + width - 1, y, x + width, y + height, 0xFF334155);
	}

	private static String shortChipName(String name) {
		if (name == null || name.isBlank()) {
			return "Unknown";
		}
		if (name.regionMatches(true, 0, "Apple ", 0, 6)) {
			return name.substring(6);
		}
		return name;
	}

	private static String trimToWidth(TextRenderer tr, String text, int maxWidth) {
		if (tr.getWidth(text) <= maxWidth) {
			return text;
		}
		while (text.length() > 1 && tr.getWidth(text) > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}

	private static void appendAge(StringBuilder sb, long ageMs) {
		if (ageMs < 0L) {
			sb.append("--");
			return;
		}
		if (ageMs < 1000L) {
			sb.append(ageMs).append("ms");
			return;
		}
		if (ageMs < 60_000L) {
			ScratchPool.appendFixed(sb, ageMs / 1000.0, 1).append("s");
			return;
		}
		long sec = ageMs / 1000L;
		sb.append(sec / 60L).append('m').append(sec % 60L).append('s');
	}
}
