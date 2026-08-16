package dev.doncalvin.m3frametime.config;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.DarwinQos;
import dev.doncalvin.m3frametime.threading.AdaptiveWorkerPool;

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
				AdaptiveWorkerPool.get().execute(() -> {
					try {
						M3FrametimeMod.reloadConfig();
						M3FrametimeMod.LOGGER.info("LiveConfigWatcher: Hot-reloaded m3-frametime.json in real time!");
						if (M3FrametimeMod.config().boostDarwinQos) {
							DarwinQos.boostRenderThread();
						}
					} catch (Throwable ignored) {
					}
				});
			}
		} catch (Throwable ignored) {
		}
	}
}
