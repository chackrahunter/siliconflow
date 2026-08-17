package dev.doncalvin.m3frametime.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Native macOS Darwin Mach kernel scheduler binding for Apple Silicon (M1/M2/M3/M4).
 * Requests a higher Darwin QoS class for the calling thread when available. Core placement and real-time scheduling are not guaranteed by macOS.
 * Uses pthread_set_qos_class_self_np (QOS_CLASS_USER_INTERACTIVE 0x21) as a best-effort request.
 * Does not force Mach affinity or real-time policies.


 */
public final class DarwinQos {
	public static final int QOS_CLASS_USER_INTERACTIVE = 0x21;
	public static final int QOS_CLASS_USER_INITIATED = 0x19;
	public static final int QOS_CLASS_UTILITY = 0x11;


	private static volatile boolean initialized;
	private static boolean available;
	private static Object libcProxy;
	private static MethodHandle setQosHandle;

	private DarwinQos() {}

	private static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		String os = System.getProperty("os.name", "").toLowerCase();
		if (!os.contains("mac")) {
			return;
		}

		try {
			Class<?> nativeClass = Class.forName("com.sun.jna.Native");
			Class<?> libcInterface = Class.forName("dev.doncalvin.m3frametime.client.DarwinQos$CLibrary");
			Method loadMethod = nativeClass.getMethod("load", String.class, Class.class);
			libcProxy = loadMethod.invoke(null, "c", libcInterface);
			if (libcProxy != null) {
				MethodHandles.Lookup lookup = MethodHandles.lookup();

				Method setQosMethod = libcInterface.getMethod("pthread_set_qos_class_self_np", int.class, int.class);
				setQosHandle = lookup.unreflect(setQosMethod).bindTo(libcProxy);

				available = true;
				M3FrametimeMod.LOGGER.info("DarwinQos: Native Apple Silicon Mach kernel scheduler initialized successfully");
			}
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.debug("DarwinQos: Native dynamic binding not available: {}", t.toString());
		}
	}

	public interface CLibrary {
		int pthread_set_qos_class_self_np(int qosClass, int relativePriority);
		long pthread_self();
	}

	public static boolean setQosSelf(int qosClass) {
		if (!initialized) {
			init();
		}
		if (!available || setQosHandle == null) {
			return false;
		}
		try {
			int result = (int) setQosHandle.invokeExact(qosClass, 0);
			if (result != 0) {
				M3FrametimeMod.LOGGER.warn("DarwinQos: pthread_set_qos_class_self_np failed with code {}", result);
			}
			return result == 0;
		} catch (Throwable t) {
			M3FrametimeMod.LOGGER.warn("DarwinQos: pthread_set_qos_class_self_np threw exception: {}", t.toString());
			return false;
		}
	}

	/** Deprecated no-op: Mach affinity/time constraints are unsafe for a game render thread. */
	public static void applyTimeConstraint(int periodNanos, int computeNanos, int constraintNanos) { }

	/** QoS only; never applies Mach affinity or real-time constraints. */
	public static void lockRenderThreadToPCores() {
		setQosSelf(QOS_CLASS_USER_INTERACTIVE);
	}

	public static void boostRenderThread() {
		setQosSelf(QOS_CLASS_USER_INTERACTIVE);
	}

	public static void boostWorkerThread() {
		setQosSelf(QOS_CLASS_USER_INITIATED);
	}
}
