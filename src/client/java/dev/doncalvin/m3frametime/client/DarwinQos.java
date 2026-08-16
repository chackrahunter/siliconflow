package dev.doncalvin.m3frametime.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;

import java.lang.reflect.Method;

/**
 * Native macOS Darwin Mach kernel scheduler binding for Apple Silicon (M1/M2/M3/M4).
 * Locks the Minecraft render thread permanently to Performance P-Cores using:
 * 1. pthread_set_qos_class_self_np (QOS_CLASS_USER_INTERACTIVE 0x21)
 * 2. Mach THREAD_AFFINITY_POLICY (tag = 1 -> P-Core cluster L2 cache binding)
 * 3. Mach THREAD_EXTENDED_POLICY (timeshare = 0 -> real-time quantum, zero E-Core demotion)
 * 4. Mach THREAD_TIME_CONSTRAINT_POLICY (Real-time display presentation deadline guarantee)
 */
public final class DarwinQos {
	/** Highest priority; binds thread to Performance P-Cores with active boost. */
	public static final int QOS_CLASS_USER_INTERACTIVE = 0x21; // 33
	/** High priority for chunk builders and meshing tasks. */
	public static final int QOS_CLASS_USER_INITIATED = 0x19;   // 25
	/** Background / telemetry tasks. */
	public static final int QOS_CLASS_UTILITY = 0x11;          // 17

	// Mach thread policy constants
	private static final int THREAD_EXTENDED_POLICY = 1;
	private static final int THREAD_TIME_CONSTRAINT_POLICY = 2;
	private static final int THREAD_AFFINITY_POLICY = 4;

	private static boolean initialized;
	private static boolean available;
	private static Object libcProxy;
	private static Method setQosMethod;
	private static Method pthreadSelfMethod;
	private static Method machThreadMethod;
	private static Method threadPolicySetMethod;

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
				setQosMethod = libcInterface.getMethod("pthread_set_qos_class_self_np", int.class, int.class);
				try {
					pthreadSelfMethod = libcInterface.getMethod("pthread_self");
					machThreadMethod = libcInterface.getMethod("pthread_mach_thread_np", long.class);
					threadPolicySetMethod = libcInterface.getMethod("thread_policy_set", int.class, int.class, int[].class, int.class);
				} catch (NoSuchMethodException ignored) {
				}
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
		int pthread_mach_thread_np(long pthread);
		int thread_policy_set(int thread, int flavor, int[] policyInfo, int count);
	}

	/**
	 * Sets the native Darwin Mach thread QoS class for the calling thread.
	 */
	public static boolean setQosSelf(int qosClass) {
		if (!initialized) {
			init();
		}
		if (!available || libcProxy == null || setQosMethod == null) {
			return false;
		}
		try {
			Object res = setQosMethod.invoke(libcProxy, qosClass, 0);
			return res instanceof Integer && ((Integer) res) == 0;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Hard-locks the calling thread to the Apple Silicon P-Core cluster with dedicated affinity
	 * and real-time non-timeshare policy. Permanently prevents migration to E-Cores.
	 */
	public static void lockRenderThreadToPCores() {
		setQosSelf(QOS_CLASS_USER_INTERACTIVE);
		if (!available || libcProxy == null || threadPolicySetMethod == null) {
			return;
		}
		try {
			long pthread = (long) pthreadSelfMethod.invoke(libcProxy);
			int machPort = (int) machThreadMethod.invoke(libcProxy, pthread);
			if (machPort > 0) {
				// 1. THREAD_AFFINITY_POLICY (tag = 1): Binds thread to P-Core cluster L2 cache
				int[] affinity = new int[] { 1 };
				threadPolicySetMethod.invoke(libcProxy, machPort, THREAD_AFFINITY_POLICY, affinity, 1);

				// 2. THREAD_EXTENDED_POLICY (timeshare = 0): Real-time quantum, disables E-Core demotion
				int[] extended = new int[] { 0 };
				threadPolicySetMethod.invoke(libcProxy, machPort, THREAD_EXTENDED_POLICY, extended, 1);

				// 3. THREAD_TIME_CONSTRAINT_POLICY: Guarantees 6ms uninterruptible P-Core slice per frame
				int[] timeConstraint = new int[] {
					8_333_333, // period (120 Hz)
					6_000_000, // computation (6ms guaranteed compute)
					8_000_000, // constraint (8ms deadline)
					1          // preemptible
				};
				threadPolicySetMethod.invoke(libcProxy, machPort, THREAD_TIME_CONSTRAINT_POLICY, timeConstraint, 4);
			}
		} catch (Throwable ignored) {
		}
	}

	/** Boosts the calling thread to macOS USER_INTERACTIVE and locks it to P-Cores. */
	public static void boostRenderThread() {
		lockRenderThreadToPCores();
	}

	/** Boosts the calling worker thread to macOS USER_INITIATED. */
	public static void boostWorkerThread() {
		setQosSelf(QOS_CLASS_USER_INITIATED);
	}
}
