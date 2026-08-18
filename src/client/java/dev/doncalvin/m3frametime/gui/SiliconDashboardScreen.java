package dev.doncalvin.m3frametime.gui;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.ChipPower;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.engine.ShaderAutoThrottle;
import dev.doncalvin.m3frametime.engine.SiliconCpuTopology;
import dev.doncalvin.m3frametime.telemetry.DebugHud;
import dev.doncalvin.m3frametime.telemetry.GcProbe;
import dev.doncalvin.m3frametime.telemetry.MemoryPressureProbe;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import dev.doncalvin.m3frametime.telemetry.SpikeMonitor;
import dev.doncalvin.m3frametime.telemetry.StutterErrorCode;
import dev.doncalvin.m3frametime.pool.ScratchPool;
import dev.doncalvin.m3frametime.version.VersionDetector;
import net.minecraft.SharedConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * SiliconFlow settings: profiles, live tuning sliders, read-only topology, diagnostics.
 */
public final class SiliconDashboardScreen extends Screen {
	private enum Tab {
		PROFILES("screen.m3-frametime.tab.profiles"),
		TUNING("screen.m3-frametime.tab.tuning"),
		TOPOLOGY("screen.m3-frametime.tab.topology"),
		DIAGNOSTICS("screen.m3-frametime.tab.diagnostics");

		final String titleKey;
		Tab(String titleKey) {
			this.titleKey = titleKey;
		}
	}

	private static final String[] PROFILE_IDS = {
		"PLAYABLE", "SHADER", "BALANCED", "MAX", "TELEMETRY"
	};
	private static final int FOOTER_H = 28;
	private static final int SLIDER_H = 16;
	private static final int SLIDER_GAP = 2;
	private static final int TOGGLE_H = 18;
	private static final int TOGGLE_GAP = 4;

	private final Screen parent;
	private Tab currentTab = Tab.PROFILES;

	private final List<ToggleWidget> toggles = new ArrayList<>();
	private final List<SliderWidget> sliders = new ArrayList<>();
	private final List<ButtonWidget> profileButtons = new ArrayList<>();
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
		this.cardW = Math.max(1, Math.min(720, this.width - 24));
		this.cardH = Math.max(1, this.height - 16);
		this.cardX = (this.width - this.cardW) / 2;
		this.cardY = Math.max(4, (this.height - this.cardH) / 2);
	}

	private void ensureWidgetsInitialized() {
		if (this.toggles.isEmpty() || this.profileButtons.isEmpty()) {
			init();
		}
	}

	private M3Config liveConfig() {
		return M3FrametimeMod.config();
	}

	private String activeProfile() {
		String profile = liveConfig().performanceProfile;
		return profile == null || profile.isBlank() ? "PLAYABLE" : profile.trim().toUpperCase();
	}

	public SiliconDashboardScreen(Screen parent) {
		super(Text.translatable("screen.m3-frametime.title"));
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
		if (this.lastProfileChangeMillis == 0L) {
			return t("screen.m3-frametime.profiles.none");
		}
		long seconds = Math.max(0L, (System.currentTimeMillis() - this.lastProfileChangeMillis) / 1000L);
		return seconds == 0L ? t("screen.m3-frametime.profiles.just_now") : t("screen.m3-frametime.profiles.seconds_ago", seconds);
	}

	@Override
	protected void init() {
		this.clearChildren();
		this.toggles.clear();
		this.sliders.clear();
		this.profileButtons.clear();
		calculateLayout();

		for (String profile : PROFILE_IDS) {
			ButtonWidget button = ButtonWidget.builder(Text.literal(profile), b -> applyProfile(profile))
				.dimensions(0, 0, 80, 22)
				.build();
			this.profileButtons.add(this.addDrawableChild(button));
		}
		this.backButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.m3-frametime.back"), b -> close())
			.dimensions(0, 0, 66, 20).build());

		sliders.add(new SliderWidget("screen.m3-frametime.slider.entity_cull", 16.0, 192.0, false,
			() -> liveConfig().entityCullDistance, v -> liveConfig().entityCullDistance = v));
		sliders.add(new SliderWidget("screen.m3-frametime.slider.particle_cull", 8.0, 128.0, false,
			() -> liveConfig().particleCullDistance, v -> liveConfig().particleCullDistance = v));
		sliders.add(new SliderWidget("screen.m3-frametime.slider.block_entity_cull", 8.0, 128.0, false,
			() -> liveConfig().blockEntityCullDistance, v -> liveConfig().blockEntityCullDistance = v));
		sliders.add(new SliderWidget("screen.m3-frametime.slider.max_particles", 16.0, 4096.0, true,
			() -> (double) liveConfig().maxParticles, v -> liveConfig().maxParticles = (int) Math.round(v)));
		sliders.add(new SliderWidget("screen.m3-frametime.slider.far_sound", 16.0, 128.0, false,
			() -> liveConfig().farSoundDistance, v -> liveConfig().farSoundDistance = v));

		toggles.add(new ToggleWidget("screen.m3-frametime.toggle.lightmap",
			() -> M3FrametimeMod.config().lightmapThrottle, v -> {
				M3Config current = M3FrametimeMod.config();
				current.lightmapThrottle = v;
				current.save();
			}));
		toggles.add(new ToggleWidget("screen.m3-frametime.toggle.overlay",
			() -> M3FrametimeMod.config().overlayEnabled, v -> {
				M3Config current = M3FrametimeMod.config();
				current.overlayEnabled = v;
				current.save();
			}));
		toggles.add(new ToggleWidget("screen.m3-frametime.toggle.farsound",
			() -> M3FrametimeMod.config().farSoundSkip, v -> {
				M3Config current = M3FrametimeMod.config();
				current.farSoundSkip = v;
				current.save();
			}));
		toggles.add(new ToggleWidget("screen.m3-frametime.toggle.recorder",
			() -> M3FrametimeMod.config().performanceRecorderEnabled, v -> {
				M3Config current = M3FrametimeMod.config();
				current.performanceRecorderEnabled = v;
				current.save();
			}));

		layoutWidgets();
	}

	private void layoutWidgets() {
		int btnW = Math.max(70, (cardW - 32) / 3);
		int btnH = 22;
		int startY = cardY + 72;
		for (int i = 0; i < profileButtons.size(); i++) {
			int col = i % 3;
			int row = i / 3;
			ButtonWidget button = profileButtons.get(i);
			button.setPosition(cardX + 10 + col * (btnW + 6), startY + row * (btnH + 6));
			button.setWidth(btnW);
		}
		if (this.backButton != null) {
			this.backButton.setPosition(cardX + cardW - 76, cardY + cardH - 24);
		}

		int sliderY = cardY + 62;
		int sliderW = cardW - 20;
		for (SliderWidget slider : sliders) {
			slider.setBounds(cardX + 10, sliderY, sliderW, SLIDER_H);
			sliderY += SLIDER_H + SLIDER_GAP;
		}
		int contentBottom = cardY + cardH - FOOTER_H;
		int rows = (toggles.size() + 1) / 2;
		int toggleBlockH = rows <= 0 ? 0 : rows * TOGGLE_H + Math.max(0, rows - 1) * TOGGLE_GAP;
		int togY = sliderY + 16;
		if (togY + toggleBlockH > contentBottom) {
			togY = Math.max(sliderY + 4, contentBottom - toggleBlockH);
		}
		int colW = (cardW - 30) / 2;
		for (int i = 0; i < toggles.size(); i++) {
			int col = i % 2;
			int row = i / 2;
			toggles.get(i).setBounds(cardX + 10 + col * (colW + 10), togY + row * (TOGGLE_H + TOGGLE_GAP), colW, TOGGLE_H);
		}
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
		// Layout is computed once in init() (Minecraft re-runs init() on every resize),
		// so the per-frame recompute that used to sit here was pure waste.
		observeProfileChange();
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		if (this.client != null && this.client.world == null) {
			this.renderPanoramaBackground(context, delta);
		}
		this.applyBlur();
		renderDashboard(context, mouseX, mouseY);
	}

	private void renderDashboard(DrawContext context, int mouseX, int mouseY) {
		context.fillGradient(0, 0, this.width, this.height, 0xD00A0E17, 0xF005080E);

		int cardW = this.cardW;
		int cardH = this.cardH;
		int cardX = this.cardX;
		int cardY = this.cardY;

		context.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xCC111827);
		context.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + cardH - 1, 0xEE0B0F19);

		TextRenderer tr = this.textRenderer;
		context.drawText(tr, t("screen.m3-frametime.title"), cardX + 14, cardY + 12, 0xFF00F2FE, true);

		SiliconCpuTopology topo = SiliconCpuTopology.get();
		String versionInfo = "MC " + liveMinecraftVersion() + " · " + topo.getChipName() + " " + topo.getChipTier().name();
		context.drawText(tr, versionInfo, cardX + cardW - tr.getWidth(versionInfo) - 14, cardY + 12, 0xFF94A3B8, false);

		int tabY = cardY + 26;
		int tabW = (cardW - 20) / 4;
		Tab[] allTabs = Tab.values();
		for (int i = 0; i < allTabs.length; i++) {
			Tab tab = allTabs[i];
			int tx = cardX + 10 + (i * tabW);
			boolean active = tab == currentTab;
			boolean hovered = mouseX >= tx && mouseX <= tx + tabW - 4 && mouseY >= tabY && mouseY <= tabY + 18;
			int tabBg = active ? 0xFF0284C7 : (hovered ? 0x44334155 : 0x221E293B);
			context.fill(tx, tabY, tx + tabW - 4, tabY + 18, tabBg);
			context.drawBorder(tx, tabY, tabW - 4, 18, active ? 0xFF38BDF8 : 0x33334155);
			String title = t(tab.titleKey);
			int strX = tx + Math.max(2, (tabW - 4 - tr.getWidth(title)) / 2);
			context.drawText(tr, title, strX, tabY + 5, active ? 0xFFFFFFFF : (hovered ? 0xFFF1F5F9 : 0xFF94A3B8), active);
		}

		for (ButtonWidget control : profileButtons) {
			control.visible = currentTab == Tab.PROFILES;
		}
		refreshProfileButtonLabels();

		switch (currentTab) {
			case PROFILES -> renderProfilesTab(context, tr, cardX, cardY, cardW);
			case TUNING -> renderTuningTab(context, tr, mouseX, mouseY);
			case TOPOLOGY -> renderTopologyTab(context, tr, cardX, cardY, cardW);
			case DIAGNOSTICS -> renderDiagnosticsTab(context, tr, cardX, cardY, cardW);
		}
	}

	private void refreshProfileButtonLabels() {
		String active = activeProfile();
		for (int i = 0; i < profileButtons.size(); i++) {
			String id = PROFILE_IDS[i];
			String label = id;
			if (id.equalsIgnoreCase(active)) {
				label = "[" + id + "]";
			} else if (id.equals(pendingProfile)) {
				label = id + "...";
			}
			profileButtons.get(i).setMessage(Text.literal(label));
		}
	}

	private void renderProfilesTab(DrawContext ctx, TextRenderer tr, int cardX, int cardY, int cardW) {
		String active = activeProfile();
		ctx.drawText(tr, t("screen.m3-frametime.profiles.heading"), cardX + 12, cardY + 50, 0xFFE2E8F0, true);
		ctx.drawText(tr, t("screen.m3-frametime.profiles.blurb"), cardX + 12, cardY + 62, 0xFF94A3B8, false);

		int statusY = cardY + 128;
		int statusH = 72;
		ctx.fill(cardX + 10, statusY, cardX + cardW - 10, statusY + statusH, 0x5530364A);
		ctx.drawBorder(cardX + 10, statusY, cardW - 20, statusH, 0xFF38BDF8);
		ctx.drawText(tr, t("screen.m3-frametime.profiles.status"), cardX + 18, statusY + 6, 0xFF38BDF8, true);
		ctx.drawText(tr, t("screen.m3-frametime.profiles.active", active), cardX + 18, statusY + 20, 0xFFFFFFFF, true);
		ctx.drawText(tr, t("screen.m3-frametime.profiles.previous", previousProfile == null ? t("screen.m3-frametime.profiles.none") : previousProfile), cardX + 18, statusY + 34, 0xFFCBD5E1, false);
		ctx.drawText(tr, t("screen.m3-frametime.profiles.last_change", lastProfileChangeText()), cardX + 18, statusY + 48, 0xFFCBD5E1, false);

		int descY = statusY + statusH + 8;
		ctx.drawText(tr, t("screen.m3-frametime.profile.playable.desc"), cardX + 12, descY, 0xFFCBD5E1, false);
		ctx.drawText(tr, t("screen.m3-frametime.profile.shader.desc"), cardX + 12, descY + 12, 0xFFCBD5E1, false);
		ctx.drawText(tr, t("screen.m3-frametime.profile.balanced.desc"), cardX + 12, descY + 24, 0xFFCBD5E1, false);
		ctx.drawText(tr, t("screen.m3-frametime.profile.max.desc"), cardX + 12, descY + 36, 0xFFCBD5E1, false);
		ctx.drawText(tr, t("screen.m3-frametime.profile.telemetry.desc"), cardX + 12, descY + 48, 0xFFCBD5E1, false);

		String profileHelp = pendingProfile == null
			? t("screen.m3-frametime.profiles.confirm")
			: t("screen.m3-frametime.profiles.pending", pendingProfile);
		ctx.drawText(tr, profileHelp, cardX + 12, descY + 64, pendingProfile == null ? 0xFF94A3B8 : 0xFFFBBF24, false);

		int footerY = cardY + cardH - 48;
		int liveFps = DebugHud.get().getLiveFps();
		double liveFt = DebugHud.get().getLiveFrametimeMs();
		StringBuilder footerSb = ScratchPool.get().logBuilder();
		footerSb.append("Telemetry | FPS ").append(liveFps).append(" | FT ");
		ScratchPool.appendFixed(footerSb, liveFt, 2).append(" ms");
		ctx.fill(cardX + 10, footerY, cardX + cardW - 10, footerY + 16, 0x33059669);
		ctx.drawText(tr, footerSb.toString(), cardX + 18, footerY + 4, 0xFF34D399, false);
		ctx.drawText(tr, t("screen.m3-frametime.profiles.footer_hint"), cardX + 12, footerY + 20, 0xFF94A3B8, false);
	}

	private void renderTuningTab(DrawContext ctx, TextRenderer tr, int mx, int my) {
		ctx.drawText(tr, t("screen.m3-frametime.tuning.blurb"), cardX + 12, cardY + 50, 0xFF94A3B8, false);
		for (SliderWidget slider : sliders) {
			slider.render(ctx, tr, mx, my);
		}
		int hintY = sliders.isEmpty()
			? cardY + 64
			: sliders.get(sliders.size() - 1).y + sliders.get(sliders.size() - 1).h + 5;
		ctx.drawText(tr, t("screen.m3-frametime.tuning.shader_hint"), cardX + 12, hintY, 0xFF38BDF8, false);
		for (ToggleWidget toggle : toggles) {
			toggle.render(ctx, tr, mx, my);
		}
	}

	private void renderTopologyTab(DrawContext ctx, TextRenderer tr, int cardX, int cardY, int cardW) {
		SiliconCpuTopology topo = SiliconCpuTopology.get();
		int pCores = topo.getEstimatedPCores();
		int eCores = topo.getEstimatedECores();

		ctx.drawText(tr, t("screen.m3-frametime.topology.heading"), cardX + 12, cardY + 54, 0xFF38BDF8, true);
		ctx.drawText(tr, t("screen.m3-frametime.topology.blurb"), cardX + 12, cardY + 68, 0xFF94A3B8, false);

		int boxY = cardY + 84;
		ctx.fill(cardX + 10, boxY, cardX + cardW - 10, boxY + 96, 0x440F172A);
		ctx.drawBorder(cardX + 10, boxY, cardW - 20, 96, 0x3338BDF8);

		ctx.drawText(tr, t("screen.m3-frametime.topology.chip", topo.getChipName(), topo.getChipTier().name(), topo.getChipGeneration(), topo.getGpuCoreCount()), cardX + 20, boxY + 12, 0xFF00F2FE, false);
		ctx.drawText(tr, t("screen.m3-frametime.topology.tier", topo.getChipTier().name()), cardX + 20, boxY + 26, 0xFF34D399, false);
		ctx.drawText(tr, pCores > 0 && eCores > 0
			? t("screen.m3-frametime.topology.pe_unavailable_counts")
			: t("screen.m3-frametime.topology.pe_unavailable_probes"), cardX + 20, boxY + 42, 0xFF94A3B8, false);
		ctx.drawText(tr, t("screen.m3-frametime.topology.qos"), cardX + 20, boxY + 60, 0xFF4ADE80, false);
		ctx.drawText(tr, t("screen.m3-frametime.topology.gpu_util"), cardX + 20, boxY + 74, 0xFF94A3B8, false);

		int ramY = boxY + 106;
		ctx.fill(cardX + 10, ramY, cardX + cardW - 10, ramY + 60, 0x440F172A);
		ctx.drawBorder(cardX + 10, ramY, cardW - 20, 60, 0x3338BDF8);

		MemoryPressureProbe memory = MemoryPressureProbe.get();
		long maxMb = memory.heapMaxMb();
		long usedMb = memory.heapUsedMb();
		String heap = usedMb >= 0 && maxMb > 0 ? usedMb + " / " + maxMb + " MB" : t("screen.m3-frametime.unavailable");
		String uma = memory.freePhysicalMb() >= 0 ? memory.freePhysicalMb() + " MB" : t("screen.m3-frametime.unavailable");
		ctx.drawText(tr, t("screen.m3-frametime.topology.heap", heap, uma), cardX + 20, ramY + 14, 0xFFFBBF24, false);
		String policy = RamDiscipline.get().pressureMode()
			? t("screen.m3-frametime.topology.policy_trim")
			: t("screen.m3-frametime.topology.policy_normal");
		ctx.drawText(tr, t("screen.m3-frametime.topology.gc", GcProbe.get().frameGcDeltaMs(), policy), cardX + 20, ramY + 32, 0xFFCBD5E1, false);
		ctx.drawText(tr, t("screen.m3-frametime.topology.uma"), cardX + 20, ramY + 48, 0xFF94A3B8, false);
	}

	private void renderDiagnosticsTab(DrawContext ctx, TextRenderer tr, int cardX, int cardY, int cardW) {
		SpikeMonitor sm = SpikeMonitor.get();
		StutterErrorCode lastErr = sm.lastErrorCode();

		ctx.drawText(tr, t("screen.m3-frametime.diagnostics.heading"), cardX + 12, cardY + 54, 0xFFA855F7, true);
		ctx.drawText(tr, t("screen.m3-frametime.diagnostics.blurb"), cardX + 12, cardY + 68, 0xFF94A3B8, false);

		int boxY = cardY + 84;
		ctx.fill(cardX + 10, boxY, cardX + cardW - 10, boxY + 196, 0x440F172A);
		ctx.drawBorder(cardX + 10, boxY, cardW - 20, 196, 0x33A855F7);

		ctx.drawText(tr, t("screen.m3-frametime.diagnostics.last_code", lastErr.getCode(), lastErr.getTitle()), cardX + 20, boxY + 14, 0xFFFBBF24, false);
		ctx.drawText(tr, t("screen.m3-frametime.diagnostics.description", lastErr.getDescription()), cardX + 20, boxY + 32, 0xFF94A3B8, false);
		ctx.drawText(tr, t("screen.m3-frametime.diagnostics.spikes", sm.spikeCount()), cardX + 20, boxY + 52, 0xFFCBD5E1, false);

		boolean recorderOn = liveConfig().performanceRecorderEnabled;
		ctx.drawText(tr, t(recorderOn ? "screen.m3-frametime.diagnostics.recorder_on" : "screen.m3-frametime.diagnostics.recorder_off"), cardX + 20, boxY + 72, recorderOn ? 0xFF4ADE80 : 0xFF94A3B8, false);

		ShaderAutoThrottle.Level throttle = ShaderAutoThrottle.get().level();
		ctx.drawText(tr, t("screen.m3-frametime.diagnostics.throttle", throttle.name()), cardX + 20, boxY + 90, throttle == ShaderAutoThrottle.Level.NONE ? 0xFF94A3B8 : 0xFFFBBF24, false);

		RamDiscipline ram = RamDiscipline.get();
		int ramColor = ram.pressureLevel() == RamDiscipline.PressureLevel.PRESSURE ? 0xFFF85149
			: (ram.pressureLevel() == RamDiscipline.PressureLevel.CAUTION ? 0xFFFBBF24 : 0xFF4ADE80);
		ctx.drawText(tr, t("screen.m3-frametime.diagnostics.ram_level", ram.pressureLevel().name()), cardX + 20, boxY + 108, ramColor, false);
		ctx.drawText(tr, t(ram.heapVsPhysicalUnhealthy() ? "screen.m3-frametime.diagnostics.heap_uma_bad" : "screen.m3-frametime.diagnostics.heap_uma_ok"), cardX + 20, boxY + 126, ram.heapVsPhysicalUnhealthy() ? 0xFFF85149 : 0xFF94A3B8, false);

		String sodiumWarn = ChipPower.sodiumAllocatorWarning();
		if (sodiumWarn != null) {
			ctx.drawText(tr, t("screen.m3-frametime.diagnostics.sodium_warn", sodiumWarn), cardX + 20, boxY + 144, 0xFFFBBF24, false);
		} else {
			ctx.drawText(tr, t("screen.m3-frametime.diagnostics.sodium_ok"), cardX + 20, boxY + 144, 0xFF94A3B8, false);
		}
		ctx.drawText(tr, t("screen.m3-frametime.diagnostics.f8_hint"), cardX + 20, boxY + 168, 0xFF38BDF8, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		ensureWidgetsInitialized();
		if (button == 0) {
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

			if (currentTab == Tab.TUNING) {
				for (SliderWidget slider : sliders) {
					if (slider.mouseClicked(mouseX, mouseY)) {
						return true;
					}
				}
				for (ToggleWidget toggle : toggles) {
					if (toggle.mouseClicked(mouseX, mouseY)) {
						return true;
					}
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (currentTab == Tab.TUNING && button == 0) {
			for (SliderWidget slider : sliders) {
				if (slider.mouseDragged(mouseX)) {
					return true;
				}
			}
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean saved = false;
		if (button == 0) {
			for (SliderWidget slider : sliders) {
				if (slider.mouseReleased()) {
					saved = true;
				}
			}
		}
		return saved || super.mouseReleased(mouseX, mouseY, button);
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

	private static String liveMinecraftVersion() {
		String name = SharedConstants.getGameVersion().getName();
		if (name != null && !name.isBlank()) {
			return name;
		}
		VersionDetector detector = VersionDetector.get();
		String raw = detector.getRawVersion();
		if (raw != null && !raw.isBlank() && !"unknown".equalsIgnoreCase(raw)) {
			return raw;
		}
		return detector.getMajor() + "." + detector.getMinor() + "." + detector.getPatch();
	}

	private static String t(String key) {
		return Text.translatable(key).getString();
	}

	private static String t(String key, Object... args) {
		return Text.translatable(key, args).getString();
	}

	private static class SliderWidget {
		int x, y, w, h;
		final String labelKey;
		final double min;
		final double max;
		final boolean integer;
		final Supplier<Double> getter;
		final Consumer<Double> setter;
		boolean dragging;

		SliderWidget(String labelKey, double min, double max, boolean integer, Supplier<Double> getter, Consumer<Double> setter) {
			this.labelKey = labelKey;
			this.min = min;
			this.max = max;
			this.integer = integer;
			this.getter = getter;
			this.setter = setter;
		}

		void setBounds(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		void render(DrawContext ctx, TextRenderer tr, int mx, int my) {
			boolean hovered = dragging || (mx >= x && mx <= x + w && my >= y && my <= y + h);
			ctx.fill(x, y, x + w, y + h, hovered ? 0x33334155 : 0x221E293B);
			ctx.drawBorder(x, y, w, h, hovered ? 0x66475569 : 0x33334155);

			double value = getter.get();
			double t = (value - min) / (max - min);
			if (t < 0.0) t = 0.0;
			if (t > 1.0) t = 1.0;
			int trackX = x + 4;
			int trackW = w - 8;
			int trackY = y + h - 5;
			ctx.fill(trackX, trackY, trackX + trackW, trackY + 2, 0xFF334155);
			int filled = (int) Math.round(trackW * t);
			if (filled > 0) {
				ctx.fill(trackX, trackY, trackX + filled, trackY + 2, 0xFF38BDF8);
			}

			StringBuilder sb = ScratchPool.get().stringBuilder();
			sb.append(Text.translatable(labelKey).getString()).append(": ");
			if (integer) {
				sb.append(Integer.toString((int) Math.round(value)));
			} else {
				ScratchPool.appendFixed(sb, value, 1);
			}
			ctx.drawText(tr, sb.toString(), x + 6, y + 2, 0xFFF8FAFC, false);
		}

		boolean mouseClicked(double mx, double my) {
			if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
				dragging = true;
				applyFromMouse(mx);
				return true;
			}
			return false;
		}

		boolean mouseDragged(double mx) {
			if (!dragging) {
				return false;
			}
			applyFromMouse(mx);
			return true;
		}

		boolean mouseReleased() {
			if (!dragging) {
				return false;
			}
			dragging = false;
			M3FrametimeMod.config().save();
			return true;
		}

		private void applyFromMouse(double mx) {
			int trackX = x + 4;
			int trackW = Math.max(1, w - 8);
			double t = (mx - trackX) / (double) trackW;
			if (t < 0.0) t = 0.0;
			if (t > 1.0) t = 1.0;
			double raw = min + t * (max - min);
			if (integer) {
				raw = Math.round(raw);
			}
			setter.accept(raw);
		}
	}

	private static class ToggleWidget {
		int x, y, w, h;
		final String labelKey;
		final Supplier<Boolean> getter;
		final Consumer<Boolean> setter;

		ToggleWidget(String labelKey, Supplier<Boolean> getter, Consumer<Boolean> setter) {
			this.labelKey = labelKey;
			this.getter = getter;
			this.setter = setter;
		}

		void setBounds(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
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

			String label = Text.translatable(labelKey).getString();
			int maxLabelW = Math.max(8, switchX - x - 8);
			ctx.drawText(tr, tr.trimToWidth(label, maxLabelW), x + 6, y + (h - 8) / 2, state ? 0xFFF8FAFC : 0xFF94A3B8, false);
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
