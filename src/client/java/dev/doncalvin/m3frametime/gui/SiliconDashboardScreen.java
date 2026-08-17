package dev.doncalvin.m3frametime.gui;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ChipPower;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.engine.SiliconBenchmark;
import dev.doncalvin.m3frametime.engine.SiliconCpuTopology;
import dev.doncalvin.m3frametime.telemetry.DebugHud;
import dev.doncalvin.m3frametime.telemetry.GcProbe;
import dev.doncalvin.m3frametime.telemetry.MemoryPressureProbe;
import dev.doncalvin.m3frametime.telemetry.SpikeMonitor;
import dev.doncalvin.m3frametime.telemetry.StutterErrorCode;
import dev.doncalvin.m3frametime.version.VersionDetector;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * SiliconFlow Master Control Center & Performance Dashboard 2.0.
 * Multi-Tab Apple Silicon Suite featuring 1-Click Tuning Profiles,
 * CPU Core Topology Visualizer, In-Game Hardware Benchmark Runner, and Flight Diagnostics.
 */
public final class SiliconDashboardScreen extends Screen {
	private enum Tab {
		PROFILES("🚀 Performance & Profiles"),
		TOPOLOGY("🍎 Apple Silicon Topology"),
		BENCHMARK("🏎️ Hardware Benchmark"),
		DIAGNOSTICS("🛰️ Flight Recorder");

		final String title;
		Tab(String title) {
			this.title = title;
		}
	}

	private final Screen parent;
	private Tab currentTab = Tab.PROFILES;

	private final List<ToggleWidget> toggles = new ArrayList<>();
	private final List<ButtonWidget> vanillaControls = new ArrayList<>();
	private ButtonWidget backButton;
	private String pendingProfile;
	private String observedProfile;
	private String previousProfile;
	private long lastProfileChangeMillis;
	private int cardX;
	private int cardY;
	private int cardW;
	private int cardH;

	private void calculateLayout() {
		this.cardW = Math.max(1, Math.min(680, this.width - 40));
		this.cardH = Math.max(1, Math.min(390, this.height - 44));
		this.cardX = (this.width - this.cardW) / 2;
		this.cardY = Math.max(12, (this.height - this.cardH) / 2);
	}

	private void ensureWidgetsInitialized() {
		if (this.toggles.isEmpty() || this.vanillaControls.isEmpty()) {
			init();
		}
	}

	private void addVanillaControl(ButtonWidget button) {
		this.vanillaControls.add(this.addDrawableChild(button));
	}

	private M3Config liveConfig() {
		return M3FrametimeMod.config();
	}

	private String activeProfile() {
		String profile = liveConfig().performanceProfile;
		return profile == null || profile.isBlank() ? "PLAYABLE" : profile.trim().toUpperCase();
	}

	public SiliconDashboardScreen(Screen parent) {
		super(Text.literal("SiliconFlow Dashboard"));
		this.parent = parent;
		this.observedProfile = activeProfile();
	}

	private void observeProfileChange() {
		String current = activeProfile();
		if (this.observedProfile == null) {
			this.observedProfile = current;
		} else if (!this.observedProfile.equals(current)) {
			this.previousProfile = this.observedProfile;
			this.observedProfile = current;
			this.lastProfileChangeMillis = System.currentTimeMillis();
		}
	}

	private String lastProfileChangeText() {
		if (this.lastProfileChangeMillis == 0L) return "none in this session";
		long seconds = Math.max(0L, (System.currentTimeMillis() - this.lastProfileChangeMillis) / 1000L);
		return seconds == 0L ? "just now" : seconds + "s ago";
	}

	@Override
	protected void init() {
		this.toggles.clear();
		this.vanillaControls.clear();
		calculateLayout();

		int cardW = this.cardW;
		int cardX = this.cardX;
		int cardY = this.cardY;

		// Vanilla ButtonWidgets are the actual profile controls; custom cards below provide status styling.
		int btnW = (cardW - 30) / 4;
		int btnY = cardY + 70;
		addVanillaControl(ButtonWidget.builder(Text.literal("PLAYABLE"), b -> applyProfile("PLAYABLE"))
			.dimensions(cardX + 10, btnY, btnW, 24).build());
		addVanillaControl(ButtonWidget.builder(Text.literal("MAX"), b -> applyProfile("MAX"))
			.dimensions(cardX + 15 + btnW, btnY, btnW, 24).build());
		addVanillaControl(ButtonWidget.builder(Text.literal("BALANCED"), b -> applyProfile("BALANCED"))
			.dimensions(cardX + 20 + btnW * 2, btnY, btnW, 24).build());
		addVanillaControl(ButtonWidget.builder(Text.literal("TELEMETRY"), b -> applyProfile("TELEMETRY"))
			.dimensions(cardX + 25 + btnW * 3, btnY, btnW, 24).build());
		this.backButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
			.dimensions(cardX + cardW - 76, cardY + cardH - 28, 66, 20).build());

		// Interactive Feature Toggles (Tab 1)
		int togY = cardY + 222;
		int colW = (cardW - 30) / 2;

		toggles.add(new ToggleWidget(cardX + 10, togY, colW, 20, "Mod FastMath Helpers", () -> M3FrametimeMod.config().useFastMath, v -> {
			M3Config current = M3FrametimeMod.config();
			current.useFastMath = v;
			current.save();
		}));

		toggles.add(new ToggleWidget(cardX + 10, togY + 72, colW, 20, "F8 Live Telemetry HUD", () -> M3FrametimeMod.config().overlayEnabled, v -> {
			M3Config current = M3FrametimeMod.config();
			current.overlayEnabled = v;
			current.save();
		}));
		toggles.add(new ToggleWidget(cardX + 20 + colW, togY + 72, colW, 20, "Far Positional Sound Culling", () -> M3FrametimeMod.config().farSoundSkip, v -> {
			M3Config current = M3FrametimeMod.config();
			current.farSoundSkip = v;
			current.save();
		}));

		toggles.add(new ToggleWidget(cardX + 10, togY + 96, colW, 20, "Diagnostics recorder", () -> M3FrametimeMod.config().performanceRecorderEnabled, v -> {
			M3Config current = M3FrametimeMod.config();
			current.performanceRecorderEnabled = v;
			current.save();
		}));
	}

	private void applyProfile(String profile) {
		observeProfileChange();
		if (profile.equalsIgnoreCase(activeProfile())) {
			return;
		}
		if (profile.equals(pendingProfile)) {
			M3Config cfg = M3FrametimeMod.config();
			cfg.performanceProfile = profile;
			cfg.applyProfile();
			cfg.save();
			pendingProfile = null;
		} else {
			pendingProfile = profile;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ensureWidgetsInitialized();
		calculateLayout();
		observeProfileChange();
		context.fillGradient(0, 0, this.width, this.height, 0xD00A0E17, 0xF005080E);

		int cardW = this.cardW;
		int cardH = this.cardH;
		int cardX = this.cardX;
		int cardY = this.cardY;

		// Main Glass Card Border & Fill
		context.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xCC111827);
		context.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + cardH - 1, 0xEE0B0F19);

		TextRenderer tr = this.textRenderer;

		// Header Title
		String title = "⚡ SILICONFLOW MASTER CONTROL CENTER";
		context.drawText(tr, title, cardX + 14, cardY + 12, 0xFF00F2FE, true);

		String versionInfo = "MC " + VersionDetector.get().getRawVersion() + " · " + SiliconCpuTopology.get().getChipName();
		context.drawText(tr, versionInfo, cardX + cardW - tr.getWidth(versionInfo) - 14, cardY + 12, 0xFF94A3B8, false);

		// Navigation Tabs Strip
		int tabY = cardY + 26;
		int tabW = (cardW - 20) / 4;
		Tab[] allTabs = Tab.values();
		for (int i = 0; i < allTabs.length; i++) {
			Tab t = allTabs[i];
			int tx = cardX + 10 + (i * tabW);
			boolean active = t == currentTab;
			boolean hovered = mouseX >= tx && mouseX <= tx + tabW - 4 && mouseY >= tabY && mouseY <= tabY + 18;

			int tabBg = active ? 0xFF0284C7 : (hovered ? 0x44334155 : 0x221E293B);
			context.fill(tx, tabY, tx + tabW - 4, tabY + 18, tabBg);
			context.drawBorder(tx, tabY, tabW - 4, 18, active ? 0xFF38BDF8 : 0x33334155);

			int strX = tx + (tabW - 4 - tr.getWidth(t.title)) / 2;
			context.drawText(tr, t.title, strX, tabY + 5, active ? 0xFFFFFFFF : (hovered ? 0xFFF1F5F9 : 0xFF94A3B8), active);
		}

		// Vanilla controls remain real children, but only profile controls are visible on this tab.
		for (ButtonWidget control : vanillaControls) {
			control.visible = currentTab == Tab.PROFILES;
		}

		// Render Tab Contents
		switch (currentTab) {
			case PROFILES -> renderProfilesTab(context, tr, cardX, cardY, cardW, mouseX, mouseY);
			case TOPOLOGY -> renderTopologyTab(context, tr, cardX, cardY, cardW, mouseX, mouseY);
			case BENCHMARK -> renderBenchmarkTab(context, tr, cardX, cardY, cardW, mouseX, mouseY);
			case DIAGNOSTICS -> renderDiagnosticsTab(context, tr, cardX, cardY, cardW, mouseX, mouseY);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	private void renderProfilesTab(DrawContext ctx, TextRenderer tr, int cardX, int cardY, int cardW, int mx, int my) {
		M3Config cfg = liveConfig();
		String active = activeProfile();
		ctx.drawText(tr, "PERFORMANCE PROFILES", cardX + 12, cardY + 54, 0xFFE2E8F0, true);
		ctx.drawText(tr, "Choose a preset for mod-owned optimizations. Minecraft video and shader options stay yours.", cardX + 12, cardY + 66, 0xFF94A3B8, false);
		// Visible runtime status panel: values come directly from the live config and session memory.
		int statusX = cardX + Math.max(300, cardW / 2);
		int statusY = cardY + 104;
		int statusW = cardX + cardW - 12 - statusX;
		ctx.fill(statusX, statusY, statusX + statusW, statusY + 106, 0x5530364A);
		ctx.drawBorder(statusX, statusY, statusW, 106, 0xFF38BDF8);
		ctx.drawText(tr, "PROFILE STATUS", statusX + 8, statusY + 8, 0xFF38BDF8, true);
		ctx.drawText(tr, "ACTIVE PROFILE: " + active, statusX + 8, statusY + 25, 0xFFFFFFFF, true);
		ctx.drawText(tr, "PREVIOUS PROFILE: " + (previousProfile == null ? "none in this session" : previousProfile), statusX + 8, statusY + 43, 0xFFCBD5E1, false);
		ctx.drawText(tr, "LAST PROFILE CHANGE: " + lastProfileChangeText(), statusX + 8, statusY + 61, 0xFFCBD5E1, false);
		ctx.drawText(tr, "Config is live; history is memory-only.", statusX + 8, statusY + 82, 0xFF94A3B8, false);

		ctx.drawText(tr, "PROFILE DESCRIPTIONS", cardX + 12, cardY + 108, 0xFFE2E8F0, true);
		ctx.drawText(tr, "PLAYABLE: visual-first, safe culling, smooth play.", cardX + 12, cardY + 122, 0xFFCBD5E1, false);
		ctx.drawText(tr, "MAX: strongest trimming for maximum headroom.", cardX + 12, cardY + 136, 0xFFCBD5E1, false);
		ctx.drawText(tr, "BALANCED: moderate culling, more ambience.", cardX + 12, cardY + 150, 0xFFCBD5E1, false);
		ctx.drawText(tr, "TELEMETRY: measurement only; no visual trimming.", cardX + 12, cardY + 164, 0xFFCBD5E1, false);

		String profileHelp = pendingProfile == null
			? "Two-click confirmation: click a different preset once, then click it again to apply."
			: "PENDING PROFILE: " + pendingProfile + " — click the same preset again to apply.";
		ctx.drawText(tr, profileHelp, cardX + 12, cardY + 184, pendingProfile == null ? 0xFF94A3B8 : 0xFFFBBF24, false);

		ctx.drawText(tr, "DIAGNOSTICS & SETTINGS", cardX + 12, cardY + 204, 0xFFE2E8F0, true);
		for (ToggleWidget tw : toggles) {
			tw.render(ctx, tr, mx, my);
		}

		int footerY = cardY + cardH - 48;
		int liveFps = DebugHud.get().getLiveFps();
		double liveFt = DebugHud.get().getLiveFrametimeMs();
		String qos = "USER / MINECRAFT";
		String footer = String.format("Telemetry | Live FPS: %d | Frametime: %.2f ms | Presentation: %s", liveFps, liveFt, qos);
		ctx.fill(cardX + 10, footerY, cardX + cardW - 10, footerY + 20, 0x33059669);
		ctx.drawText(tr, footer, cardX + 18, footerY + 6, 0xFF34D399, false);
		ctx.drawText(tr, "Profile changes save safely to the existing config file; no history is claimed across restarts.", cardX + 12, footerY + 25, 0xFF94A3B8, false);
	}

	private void renderTopologyTab(DrawContext ctx, TextRenderer tr, int cardX, int cardY, int cardW, int mx, int my) {
		SiliconCpuTopology topo = SiliconCpuTopology.get();
		int pCores = topo.getEstimatedPCores();
		int eCores = topo.getEstimatedECores();

		ctx.drawText(tr, "APPLE SILICON OPTIMIZATION", cardX + 12, cardY + 54, 0xFF38BDF8, true);
		ctx.drawText(tr, "Hardware facts are read-only; macOS retains scheduling and unified-memory authority.", cardX + 12, cardY + 68, 0xFF94A3B8, false);

		int boxY = cardY + 84;
		ctx.fill(cardX + 10, boxY, cardX + cardW - 10, boxY + 80, 0x440F172A);
		ctx.drawBorder(cardX + 10, boxY, cardW - 20, 80, 0x3338BDF8);

		ctx.drawText(tr, "⚡ Chip: §f" + topo.getChipName() + "§r (" + topo.getChipTier() + ") · " + "M" + topo.getChipGeneration() + " · GPU: §f" + topo.getGpuCoreCount() + " Cores§r", cardX + 20, boxY + 12, 0xFF00F2FE, false);
		ctx.drawText(tr, pCores > 0 && eCores > 0 ? "  ↳ P/E-core counts: unavailable (macOS scheduling owns placement)" : "  ↳ P/E-core topology: unavailable from safe JVM probes", cardX + 20, boxY + 26, 0xFF94A3B8, false);

		ctx.drawText(tr, "  ↳ QoS request: " + ("user-owned") + " (scheduler-owned; SiliconFlow does not request QoS)", cardX + 20, boxY + 44, 0xFF4ADE80, false);
		ctx.drawText(tr, "  ↳ GPU utilization: unavailable from the current graphics API", cardX + 20, boxY + 58, 0xFF94A3B8, false);

		// RAM Allocation
		int ramY = boxY + 90;
		ctx.fill(cardX + 10, ramY, cardX + cardW - 10, ramY + 60, 0x440F172A);
		ctx.drawBorder(cardX + 10, ramY, cardW - 20, 60, 0x3338BDF8);

		MemoryPressureProbe memory = MemoryPressureProbe.get();
		long maxMb = memory.heapMaxMb();
		long usedMb = memory.heapUsedMb();
		String heap = usedMb >= 0 && maxMb > 0 ? usedMb + " / " + maxMb + " MB" : "unavailable";
		ctx.drawText(tr, "🧠 JVM heap: §f" + heap + "§r · physical free: §f" + memory.freePhysicalMb() + " MB§r", cardX + 20, ramY + 14, 0xFFFBBF24, false);
		ctx.drawText(tr, "  ↳ GC pause: §f" + GcProbe.get().frameGcDeltaMs() + " ms§r | policy: §f" + (dev.doncalvin.m3frametime.telemetry.RamDiscipline.get().pressureMode() ? "MOD-OWNED TRIM" : "NORMAL") + "§r", cardX + 20, ramY + 32, 0xFFCBD5E1, false);
		ctx.drawText(tr, "  ↳ UMA/VRAM split: §funavailable§r (macOS unified memory; no VRAM measurement)", cardX + 20, ramY + 48, 0xFF94A3B8, false);
	}

	private void renderBenchmarkTab(DrawContext ctx, TextRenderer tr, int cardX, int cardY, int cardW, int mx, int my) {
		SiliconBenchmark bench = SiliconBenchmark.get();

		ctx.drawText(tr, "BENCHMARK", cardX + 12, cardY + 54, 0xFFF59E0B, true);
		ctx.drawText(tr, "Optional local measurement of ARM64 math and memory throughput; not an FPS guarantee.", cardX + 12, cardY + 68, 0xFF94A3B8, false);

		int boxY = cardY + 84;
		ctx.fill(cardX + 10, boxY, cardX + cardW - 10, boxY + 120, 0x440F172A);
		ctx.drawBorder(cardX + 10, boxY, cardW - 20, 120, 0x33F59E0B);

		if (bench.isRunning()) {
			ctx.drawText(tr, "⏳ Benchmark Running... (" + (int) (bench.getProgress() * 100) + "%)", cardX + 20, boxY + 16, 0xFFFBBF24, true);
			int barW = cardW - 60;
			ctx.fill(cardX + 20, boxY + 36, cardX + 20 + barW, boxY + 48, 0x661E293B);
			ctx.fill(cardX + 20, boxY + 36, cardX + 20 + (int) (barW * bench.getProgress()), boxY + 48, 0xFF00F2FE);
		} else {
			ctx.drawText(tr, "SiliconFlow Performance Index (SPI): §f" + String.format("%.1f", bench.getPerformanceIndex()) + " / 100§r", cardX + 20, boxY + 16, 0xFF00F2FE, true);
			ctx.drawText(tr, "Rating: §a" + bench.getResultRating() + "§r", cardX + 20, boxY + 34, 0xFF4ADE80, false);

			String mathStr = bench.getFastMathOpsPerSec() > 0 ? String.format("%,d ops/sec", bench.getFastMathOpsPerSec()) : "--";
			String memStr = bench.getMemoryBandwidthGbSec() > 0 ? String.format("%.2f GB/s", bench.getMemoryBandwidthGbSec()) : "--";

			ctx.drawText(tr, "• ARM64 FastMath Throughput: §f" + mathStr + "§r", cardX + 20, boxY + 54, 0xFFCBD5E1, false);
			ctx.drawText(tr, "• Memory Cache Transfer Bandwidth: §f" + memStr + "§r", cardX + 20, boxY + 70, 0xFFCBD5E1, false);
			ctx.drawText(tr, "• Render pacing: §funavailable§r (no synthetic score)", cardX + 20, boxY + 86, 0xFFCBD5E1, false);
		}

		// Benchmark Button
		int btnW = 220;
		int btnH = 26;
		int btnX = cardX + (cardW - btnW) / 2;
		int btnY = boxY + 132;
		boolean hovered = mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH;

		ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, bench.isRunning() ? 0x44334155 : (hovered ? 0xFF0284C7 : 0xFF0369A1));
		ctx.drawBorder(btnX, btnY, btnW, btnH, hovered ? 0xFF38BDF8 : 0xFF0284C7);

		String btnText = bench.isRunning() ? "Benchmarking..." : "▶ Start Hardware Benchmark";
		int tx = btnX + (btnW - tr.getWidth(btnText)) / 2;
		ctx.drawText(tr, btnText, tx, btnY + 9, 0xFFFFFFFF, true);
	}

	private void renderDiagnosticsTab(DrawContext ctx, TextRenderer tr, int cardX, int cardY, int cardW, int mx, int my) {
		SpikeMonitor sm = SpikeMonitor.get();
		StutterErrorCode lastErr = sm.lastErrorCode();

		ctx.drawText(tr, "DIAGNOSTICS & SETTINGS", cardX + 12, cardY + 54, 0xFFA855F7, true);
		ctx.drawText(tr, "Telemetry is opt-in and local. Changes save immediately; some Minecraft options may need a restart.", cardX + 12, cardY + 68, 0xFF94A3B8, false);

		int boxY = cardY + 84;
		ctx.fill(cardX + 10, boxY, cardX + cardW - 10, boxY + 150, 0x440F172A);
		ctx.drawBorder(cardX + 10, boxY, cardW - 20, 150, 0x33A855F7);

		ctx.drawText(tr, "Diagnostic catalog: §f" + StutterErrorCode.totalCount() + " definitions§r", cardX + 20, boxY + 14, 0xFFC084FC, true);
		ctx.drawText(tr, "Total Frame Spikes Detected: §f" + sm.spikeCount() + "§r", cardX + 20, boxY + 32, 0xFFCBD5E1, false);

		ctx.drawText(tr, "Last Error Code: §e[" + lastErr.getCode() + "] " + lastErr.getTitle() + "§r", cardX + 20, boxY + 52, 0xFFFBBF24, false);
		ctx.drawText(tr, "Description: §7" + lastErr.getDescription() + "§r", cardX + 20, boxY + 68, 0xFF94A3B8, false);

		ctx.drawText(tr, "Runtime quality changes: §fdisabled§r (profile settings remain user-controlled)", cardX + 20, boxY + 90, 0xFF4ADE80, false);
		ctx.drawText(tr, "Status: §fmeasured telemetry only§r", cardX + 20, boxY + 110, 0xFF38BDF8, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		ensureWidgetsInitialized();
		calculateLayout();
		if (button == 0) {
			if (currentTab == Tab.PROFILES) {
				for (ToggleWidget toggle : toggles) {
					if (toggle.mouseClicked(mouseX, mouseY)) {
						return true;
					}
				}
			}

			int cardW = this.cardW;
			int cardX = this.cardX;
			int cardY = this.cardY;
			int tabY = cardY + 26;
			int tabW = (cardW - 20) / 4;
			Tab[] allTabs = Tab.values();
			for (int i = 0; i < allTabs.length; i++) {
				int tx = cardX + 10 + (i * tabW);
				if (mouseX >= tx && mouseX <= tx + tabW - 4 && mouseY >= tabY && mouseY <= tabY + 18) {
					currentTab = allTabs[i];
					return true;
				}
			}

			if (currentTab == Tab.BENCHMARK) {
				int boxY = cardY + 84;
				int btnW = 220;
				int btnH = 26;
				int btnX = cardX + (cardW - btnW) / 2;
				int btnY = boxY + 132;
				if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
					SiliconBenchmark.get().runBenchmarkAsync();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_F7) {
			this.close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}

	// -------------------------------------------------------------
	// Helper Components
	// -------------------------------------------------------------
	private static class ProfileButton {
		final int x, y, w, h;
		final String profileName;
		final String label;

		ProfileButton(int x, int y, int w, int h, String profileName, String label) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.profileName = profileName;
			this.label = label;
		}

		void render(DrawContext ctx, TextRenderer tr, int mx, int my) {
			boolean active = profileName.equalsIgnoreCase(M3FrametimeMod.config().performanceProfile);
			boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;

			int bg = active ? 0xFF0284C7 : (hovered ? 0x66334155 : 0x441E293B);
			int border = active ? 0xFF38BDF8 : (hovered ? 0xFF64748B : 0xFF334155);

			ctx.fill(x, y, x + w, y + h, bg);
			ctx.drawBorder(x, y, w, h, border);

			int textColor = active ? 0xFFFFFFFF : (hovered ? 0xFFF1F5F9 : 0xFF94A3B8);
			int tx = x + (w - tr.getWidth(label)) / 2;
			int ty = y + (h - 8) / 2;
			ctx.drawText(tr, label, tx, ty, textColor, active);
		}

		boolean mouseClicked(double mx, double my) {
			return mx >= x && mx <= x + w && my >= y && my <= y + h;
		}
	}

	private static class ToggleWidget {
		final int x, y, w, h;
		final String label;
		final java.util.function.Supplier<Boolean> getter;
		final java.util.function.Consumer<Boolean> setter;

		ToggleWidget(int x, int y, int w, int h, String label, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.label = label;
			this.getter = getter;
			this.setter = setter;
		}

		void render(DrawContext ctx, TextRenderer tr, int mx, int my) {
			boolean state = Boolean.TRUE.equals(getter.get());
			boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;

			ctx.fill(x, y, x + w, y + h, hovered ? 0x33334155 : 0x221E293B);
			ctx.drawBorder(x, y, w, h, hovered ? 0x66475569 : 0x33334155);

			int switchW = 32;
			int switchH = 14;
			int switchX = x + w - switchW - 4;
			int switchY = y + (h - switchH) / 2;

			ctx.fill(switchX, switchY, switchX + switchW, switchY + switchH, state ? 0xFF059669 : 0xFF475569);
			int knobX = state ? (switchX + switchW - 12) : (switchX + 2);
			ctx.fill(knobX, switchY + 2, knobX + 10, switchY + switchH - 2, 0xFFFFFFFF);

			ctx.drawText(tr, label, x + 6, y + (h - 8) / 2, state ? 0xFFF8FAFC : 0xFF94A3B8, false);
		}

		boolean mouseClicked(double mx, double my) {
			if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
				setter.accept(!Boolean.TRUE.equals(getter.get()));
				return true;
			}
			return false;
		}
	}
}
