package dev.doncalvin.m3frametime.threading;

import dev.doncalvin.m3frametime.M3FrametimeMod;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Best-effort Darwin QoS request. macOS remains scheduler owner — this never pins P-cores.
 * Native path uses JNA or LWJGL {@code pthread_set_qos_class_self_np} when present;
 * otherwise {@link Thread#setPriority(int)} as a weak fallback.
 */
public final class DarwinQos {
	/** qos_class_t from {@code <sys/qos.h>}. */
	public static final int USER_INTERACTIVE = 0x21;
	public static final int USER_INITIATED = 0x19;
	public static final int UTILITY = 0x11;

	public enum Result {
		NATIVE,
		JAVA_PRIORITY,
		FAILED,
		UNSUPPORTED
	}

	private DarwinQos() {}

	public static boolean isMacOS() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return os.contains("mac") || os.contains("darwin");
	}

	/**
	 * Request a QoS class for the calling thread. Failure is non-fatal.
	 */
	public static Result requestSelf(int qosClass) {
		if (!isMacOS()) {
			return Result.UNSUPPORTED;
		}
		if (tryNative(qosClass)) {
			return Result.NATIVE;
		}
		if (applyJavaPriority(qosClass)) {
			return Result.JAVA_PRIORITY;
		}
		return Result.FAILED;
	}

	private static boolean tryNative(int qosClass) {
		if (tryJna(qosClass)) {
			return true;
		}
		return tryLwjgl(qosClass);
	}

	private static boolean tryJna(int qosClass) {
		try {
			Class<?> functionClass = Class.forName("com.sun.jna.Function");
			Method getFunction = functionClass.getMethod("getFunction", String.class, String.class);
			Object fn = null;
			for (String lib : new String[] {"System", "c"}) {
				try {
					fn = getFunction.invoke(null, lib, "pthread_set_qos_class_self_np");
					if (fn != null) {
						break;
					}
				} catch (Throwable ignored) {
				}
			}
			if (fn == null) {
				return false;
			}
			Integer rc = invokeJnaInt(functionClass, fn, qosClass);
			return rc != null && rc == 0;
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("Darwin QoS JNA path skipped: {}", t.toString());
			return false;
		}
	}

	private static Integer invokeJnaInt(Class<?> functionClass, Object fn, int qosClass) {
		try {
			Method invokeInt = functionClass.getMethod("invokeInt", Object[].class);
			Object result = invokeInt.invoke(fn, (Object) new Object[] {qosClass, 0});
			if (result instanceof Integer i) {
				return i;
			}
		} catch (NoSuchMethodException ignored) {
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("Darwin QoS JNA invokeInt failed: {}", t.toString());
		}
		try {
			Method invoke = functionClass.getMethod("invoke", Class.class, Object[].class);
			Object result = invoke.invoke(fn, int.class, new Object[] {qosClass, 0});
			if (result instanceof Integer i) {
				return i;
			}
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("Darwin QoS JNA invoke failed: {}", t.toString());
		}
		return null;
	}

	private static boolean tryLwjgl(int qosClass) {
		try {
			Class<?> sharedLibrary = Class.forName("org.lwjgl.system.SharedLibrary");
			Object lib = resolveLwjglSystemLibrary();
			if (lib == null) {
				return false;
			}
			Method getFunctionAddress = sharedLibrary.getMethod("getFunctionAddress", CharSequence.class);
			Object addrObj = getFunctionAddress.invoke(lib, "pthread_set_qos_class_self_np");
			long addr = addrObj instanceof Long l ? l : 0L;
			if (addr == 0L) {
				return false;
			}
			Class<?> jni = Class.forName("org.lwjgl.system.JNI");
			Method invokeII = jni.getMethod("invokeII", int.class, int.class, long.class);
			Object rc = invokeII.invoke(null, qosClass, 0, addr);
			return rc instanceof Integer i && i == 0;
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("Darwin QoS LWJGL path skipped: {}", t.toString());
			return false;
		}
	}

	private static Object resolveLwjglSystemLibrary() {
		String[] owners = {
			"org.lwjgl.system.macosx.CoreFoundation",
			"org.lwjgl.system.macosx.ObjCRuntime",
			"org.lwjgl.system.macosx.LibC"
		};
		for (String owner : owners) {
			try {
				Class<?> clz = Class.forName(owner);
				try {
					Object lib = clz.getMethod("getLibrary").invoke(null);
					if (lib != null) {
						return lib;
					}
				} catch (NoSuchMethodException ignored) {
					Object lib = clz.getField("LIBRARY").get(null);
					if (lib != null) {
						return lib;
					}
				}
			} catch (Throwable ignored) {
			}
		}
		try {
			Class<?> library = Class.forName("org.lwjgl.system.Library");
			for (Method m : library.getMethods()) {
				if (!"loadNative".equals(m.getName())) {
					continue;
				}
				Class<?>[] p = m.getParameterTypes();
				if (p.length == 3 && p[0] == Class.class && p[1] == String.class && p[2] == String.class) {
					return m.invoke(null, DarwinQos.class, "org.lwjgl", "System");
				}
			}
		} catch (Throwable ignored) {
		}
		return null;
	}

	private static boolean applyJavaPriority(int qosClass) {
		try {
			Thread t = Thread.currentThread();
			int target = javaPriority(qosClass);
			if (t.getPriority() != target) {
				t.setPriority(target);
			}
			return true;
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("Darwin QoS Java priority fallback failed: {}", t.toString());
			return false;
		}
	}

	private static int javaPriority(int qosClass) {
		if (qosClass >= USER_INTERACTIVE) {
			return Thread.MAX_PRIORITY;
		}
		if (qosClass >= USER_INITIATED) {
			return Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1);
		}
		if (qosClass <= UTILITY) {
			return Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1);
		}
		return Thread.NORM_PRIORITY;
	}
}
