package dev.doncalvin.m3frametime.mixin;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.version.VersionDetector;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class M3MixinPlugin implements IMixinConfigPlugin {

	@Override
	public void onLoad(String mixinPackage) {
		VersionDetector vd = VersionDetector.get();
		M3FrametimeMod.LOGGER.info("M3MixinPlugin: Active for MC {} (Exact target: {})",
			vd.getRawVersion(),
			vd.isExactArtifactTarget());
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		VersionDetector vd = VersionDetector.get();
		if (!vd.isExactArtifactTarget()) {
			M3FrametimeMod.LOGGER.error(
				"Skipping {}: this JAR targets Minecraft 1.21.4, runtime is {}",
				mixinClassName,
				vd.getRawVersion()
			);
			return false;
		}
		if (isSoundMixin(mixinClassName) && hasSoundConflict()) {
			M3FrametimeMod.LOGGER.info("Skipping {} because Sound Physics or Voice Chat is loaded", mixinClassName);
			return false;
		}
		return true;
	}

	private static boolean isSoundMixin(String mixinClassName) {
		return mixinClassName != null && mixinClassName.contains("SoundSystem");
	}

	private static boolean hasSoundConflict() {
		FabricLoader loader = FabricLoader.getInstance();
		return loader.isModLoaded("soundphysics")
			|| loader.isModLoaded("sound_physics")
			|| loader.isModLoaded("sound_physics_remastered")
			|| loader.isModLoaded("voicechat")
			|| loader.isModLoaded("simple-voice-chat");
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
