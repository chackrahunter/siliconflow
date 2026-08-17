package dev.doncalvin.m3frametime.config;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Hot-reloads m3-frametime.json in real time whenever edited on disk.
 * Allows instant live parameter adjustments during active gameplay without game restarts.
 */
public final class LiveConfigWatcher {
	private static final LiveConfigWatcher INSTANCE = new LiveConfigWatcher();
	private static FileTime lastModified;
	private static long lastCheckNanos;

	private LiveConfigWatcher() {}

	public static LiveConfigWatcher get() {
		return INSTANCE;
	}

	public void checkHotReload(long nowNanos) {
		if (nowNanos - lastCheckNanos < 1_000_000_000L) { // Poll every 1 second
			return;
		}
		lastCheckNanos = nowNanos;

		Path path = M3Config.path();
		if (!Files.isRegularFile(path)) {
			return;
		}

		try {
			FileTime modified = Files.getLastModifiedTime(path);
			if (lastModified == null) {
				lastModified = modified;
				return;
			}
			if (!modified.equals(lastModified)) {
				lastModified = modified;
				try {
					AdaptiveWorkerPool.get().execute(() -> {
						try {
							M3Config loaded = M3Config.load();
							MinecraftClient client = MinecraftClient.getInstance();
							if (client != null) {
								client.execute(() -> {
									M3FrametimeMod.publishConfig(loaded);
									M3FrametimeMod.LOGGER.info("LiveConfigWatcher: applied config on the client thread");
								});
							}
						} catch (Throwable t) {
							M3FrametimeMod.LOGGER.warn("LiveConfigWatcher: config reload failed: {}", t.toString());
						}
					});
				} catch (RuntimeException e) {
					M3FrametimeMod.LOGGER.warn("LiveConfigWatcher: reload task rejected: {}", e.toString());
				}
			}
		} catch (Exception e) {
			M3FrametimeMod.LOGGER.debug("LiveConfigWatcher: file check failed: {}", e.toString());
		}
	}
}
