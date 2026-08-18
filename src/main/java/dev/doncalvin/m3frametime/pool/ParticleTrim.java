package dev.doncalvin.m3frametime.pool;

/**
 * Optional particle-queue trim hook. Mixins may {@link #register(Runnable)} a real
 * ParticleManager trim; without a hook this is a no-op. Never calls the JVM collector.
 */
public final class ParticleTrim {
	private static volatile Runnable hook;
	private static volatile boolean lastAggressive;

	private ParticleTrim() {}

	/** Mixin-owned ParticleManager trim. Replaces any previous hook. */
	public static void register(Runnable trim) {
		hook = trim;
	}

	/**
	 * Request a particle queue trim. Safe to call when no mixin has registered a hook.
	 *
	 * @param aggressive tighter trim under RAM PRESSURE
	 */
	public static void requestTrim(boolean aggressive) {
		lastAggressive = aggressive;
		Runnable r = hook;
		if (r != null) {
			r.run();
		}
	}

	public static boolean lastAggressive() {
		return lastAggressive;
	}
}
