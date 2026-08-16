package dev.doncalvin.m3frametime.mixin;

import dev.doncalvin.m3frametime.version.VersionDetector;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Universal Multi-Version Mixin Controller for SiliconFlow.
 * Dynamically queries the running Minecraft version using pure metadata and enables/disables mixins
 * without loading any target classes into the KnotClassLoader prematurely.
 */
public final class M3MixinPlugin implements IMixinConfigPlugin {

	@Override
	public void onLoad(String mixinPackage) {}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		VersionDetector vd = VersionDetector.get();

		// 1. RenderState DTO architecture check (Minecraft 1.21.2 - 1.21.4+)
		if (mixinClassName.endsWith("EntityRendererMixin")
			|| mixinClassName.endsWith("EntityShadowMixin")
			|| mixinClassName.endsWith("ItemFrameEntityRendererMixin")
			|| mixinClassName.endsWith("ArmorStandEntityRendererMixin")
			|| mixinClassName.endsWith("ExperienceOrbEntityRendererMixin")
			|| mixinClassName.endsWith("ItemEntityRendererMixin")) {
			if (!vd.isRenderStateEra()) {
				// Running on MC <= 1.21.1: cleanly bypass RenderState-specific mixins
				return false;
			}
		}

		// 2. DrawContext GUI architecture check (Minecraft 1.20+)
		if (mixinClassName.endsWith("InGameOverlayRendererMixin")
			|| mixinClassName.endsWith("BossBarHudMixin")
			|| mixinClassName.endsWith("ToastManagerMixin")) {
			if (!vd.isDrawContextEra()) {
				return false;
			}
		}

		// 3. Audio mod conflict soft-avoidance
		if (mixinClassName.endsWith("SoundSystemMixin")) {
			if (FabricLoader.getInstance().isModLoaded("soundphysics")
				|| FabricLoader.getInstance().isModLoaded("simplevoicechat")) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
