package dev.doncalvin.m3frametime.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.threading.DarwinQos;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Reports runtime architecture facts without claiming scheduler or Sodium ownership. */
public final class ChipPower {
	// Touched from the mixin/render thread and the background worker (sodiumAllocatorWarning()),
	// so every mutable static here is volatile to avoid torn reads / lost updates.
	private static volatile boolean reported;
	private static volatile boolean renderPriorityRequested;
	private static volatile boolean renderQosNative;
	private static volatile boolean sodiumWorkersNoted;
	private static volatile String sodiumAllocatorWarning;
	private static volatile long sodiumAllocatorCheckedNanos;

	private ChipPower() {}

	public static void applyOnce() {
		if (reported) {
			return;
		}
		reported = true;
		M3FrametimeMod.LOGGER.info("Runtime facts: os={} arch={} vm={} logicalProcessors={} (scheduler ownership unchanged)",
			System.getProperty("os.name", "?"), System.getProperty("os.arch", "?"),
			System.getProperty("java.vm.name", "?"), Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Best-effort USER_INTERACTIVE QoS request on the client/render thread.
	 * Failure is debug-logged. Never claims a P-core lock.
	 */
	public static void reinforceRenderPriority() {
		if (renderPriorityRequested) {
			return;
		}
		renderPriorityRequested = true;
		DarwinQos.Result result = DarwinQos.requestSelf(DarwinQos.USER_INTERACTIVE);
		renderQosNative = result == DarwinQos.Result.NATIVE;
		switch (result) {
			case NATIVE -> M3FrametimeMod.LOGGER.debug("Render thread requested Darwin QoS USER_INTERACTIVE (scheduler remains macOS-owned)");
			case JAVA_PRIORITY -> M3FrametimeMod.LOGGER.debug("Render thread QoS native path unavailable; using Thread.setPriority fallback");
			case FAILED -> M3FrametimeMod.LOGGER.debug("Render thread QoS request failed; continuing without it");
			case UNSUPPORTED -> M3FrametimeMod.LOGGER.debug("Darwin QoS skipped — not macOS");
		}
	}

	/** Sodium worker threads stay user-owned. Never mutate Sodium. */
	public static void tryBoostSodiumWorkers() {
		if (sodiumWorkersNoted) {
			return;
		}
		sodiumWorkersNoted = true;
		if (StackCompat.sodium()) {
			M3FrametimeMod.LOGGER.info("Sodium workers are user-owned — SiliconFlow will not change them");
		}
	}

	public static boolean renderQosRequested() {
		return renderPriorityRequested;
	}

	public static boolean renderQosNative() {
		return renderQosNative;
	}

	/**
	 * Warning when Sodium's chunk allocator is readable and is not SWAP. Never writes Sodium config.
	 *
	 * @return warn string, or {@code null} if Sodium is absent, SWAP is set, or the value cannot be read
	 */
	public static String sodiumAllocatorWarning() {
		if (!StackCompat.sodium()) {
			return null;
		}
		long now = System.nanoTime();
		if (sodiumAllocatorCheckedNanos != 0L && now - sodiumAllocatorCheckedNanos < 5_000_000_000L) {
			return sodiumAllocatorWarning;
		}
		sodiumAllocatorCheckedNanos = now;
		String allocator = readSodiumAllocator();
		if (allocator == null || allocator.isBlank()) {
			sodiumAllocatorWarning = null;
			return null;
		}
		if ("SWAP".equalsIgnoreCase(allocator)) {
			sodiumAllocatorWarning = null;
			return null;
		}
		sodiumAllocatorWarning = "Sodium chunk allocator is " + allocator + " (try SWAP on Apple Silicon; user-owned, not applied)";
		return sodiumAllocatorWarning;
	}

	private static String readSodiumAllocator() {
		String reflected = readSodiumAllocatorReflectively();
		if (reflected != null) {
			return reflected;
		}
		return readSodiumAllocatorFromJson();
	}

	private static String readSodiumAllocatorReflectively() {
		String[] owners = {
			"net.caffeinemc.mods.sodium.client.SodiumClientMod",
			"me.jellysquid.mods.sodium.client.SodiumClientMod"
		};
		for (String owner : owners) {
			try {
				Class<?> clz = Class.forName(owner);
				Object options = invokeOptions(clz);
				if (options == null) {
					continue;
				}
				Object advanced = readField(options, "advanced");
				Object allocator = advanced != null
					? firstNonNull(readField(advanced, "chunkMemoryAllocator"), readField(advanced, "chunk_memory_allocator"))
					: null;
				if (allocator == null) {
					allocator = firstNonNull(
						readField(options, "chunkMemoryAllocator"),
						invokeNoArg(options, "getChunkMemoryAllocator")
					);
				}
				if (allocator != null) {
					return allocator instanceof Enum<?> e ? e.name() : allocator.toString();
				}
			} catch (Throwable t) {
				M3FrametimeMod.LOGGER.debug("Sodium allocator reflection skipped for {}: {}", owner, t.toString());
			}
		}
		return null;
	}

	private static Object invokeOptions(Class<?> clz) {
		for (String name : new String[] {"options", "getOptions"}) {
			try {
				Method m = clz.getMethod(name);
				return m.invoke(null);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static Object invokeNoArg(Object target, String name) {
		try {
			return target.getClass().getMethod(name).invoke(target);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static Object readField(Object target, String name) {
		if (target == null) {
			return null;
		}
		Class<?> c = target.getClass();
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(target);
			} catch (NoSuchFieldException ignored) {
				c = c.getSuperclass();
			} catch (Throwable t) {
				return null;
			}
		}
		return null;
	}

	private static Object firstNonNull(Object a, Object b) {
		return a != null ? a : b;
	}

	private static String readSodiumAllocatorFromJson() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("sodium-options.json");
		if (!Files.isRegularFile(path)) {
			return null;
		}
		try {
			JsonElement root = JsonParser.parseString(Files.readString(path));
			if (!root.isJsonObject()) {
				return null;
			}
			String found = findAllocatorValue(root.getAsJsonObject());
			if (found != null) {
				return found;
			}
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("Sodium allocator JSON read skipped: {}", t.toString());
		}
		return null;
	}

	private static String findAllocatorValue(JsonObject obj) {
		for (var entry : obj.entrySet()) {
			String key = entry.getKey();
			JsonElement value = entry.getValue();
			if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
				String text = value.getAsString();
				if (key.toLowerCase(Locale.ROOT).contains("allocator")
					|| "SWAP".equalsIgnoreCase(text)
					|| "ASYNC".equalsIgnoreCase(text)) {
					return text;
				}
			} else if (value != null && value.isJsonObject()) {
				String nested = findAllocatorValue(value.getAsJsonObject());
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}
}
