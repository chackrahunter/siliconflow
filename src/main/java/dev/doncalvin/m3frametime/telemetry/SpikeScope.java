package dev.doncalvin.m3frametime.telemetry;

/**
 * Named phase markers for spike attribution. Stack-like, zero-alloc after warmup.
 */
public final class SpikeScope {
	public enum Phase {
		NONE,
		WORLD_RENDER,
		CHUNK_UPLOAD,
		RENDER_WAIT,
		ENTITY_TICK,
		PARTICLE,
		SOUND
	}

	private static final ThreadLocal<SpikeScope> TL = ThreadLocal.withInitial(SpikeScope::new);

	private final Phase[] stack = new Phase[16];
	private int depth;
	private Phase dominant = Phase.NONE;
	private long dominantNanos;

	public static SpikeScope get() {
		return TL.get();
	}

	public void push(Phase phase) {
		if (depth < stack.length) {
			stack[depth++] = phase;
		}
	}

	public void pop(Phase phase, long durationNanos) {
		if (depth > 0 && stack[depth - 1] == phase) {
			depth--;
		} else if (depth > 0) {
			depth--;
		}
		if (durationNanos > dominantNanos) {
			dominantNanos = durationNanos;
			dominant = phase;
		}
	}

	public Phase dominant() {
		return dominant;
	}

	public long dominantNanos() {
		return dominantNanos;
	}

	public void resetFrame() {
		depth = 0;
		dominant = Phase.NONE;
		dominantNanos = 0;
	}

	public Phase current() {
		return depth == 0 ? Phase.NONE : stack[depth - 1];
	}
}
