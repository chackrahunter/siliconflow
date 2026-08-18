package dev.doncalvin.m3frametime.telemetry;

/**
 * Named phase markers for spike attribution. Nest-safe stack; zero-alloc after warmup.
 */
public final class SpikeScope {
	public enum Phase {
		NONE,
		WORLD_RENDER,
		CHUNK_UPLOAD,
		RENDER_WAIT,
		ENTITY_TICK,
		PARTICLE,
		SOUND,
		GLINT,
		LIGHTMAP,
		WEATHER,
		HUD,
		SHADOW_ENTITY,
		PARTICLE_TRIM
	}

	private static final ThreadLocal<SpikeScope> TL = ThreadLocal.withInitial(SpikeScope::new);

	private final Phase[] stack = new Phase[16];
	private final long[] startNanos = new long[16];
	private int depth;
	private int overflow;
	private Phase dominant = Phase.NONE;
	private long dominantNanos;

	public static SpikeScope get() {
		return TL.get();
	}

	public void push(Phase phase) {
		if (phase == null) {
			phase = Phase.NONE;
		}
		if (depth < stack.length) {
			stack[depth] = phase;
			startNanos[depth] = System.nanoTime();
			depth++;
			return;
		}
		overflow++;
	}

	/** Nest-safe pop using the matching push timestamp. */
	public void pop(Phase phase) {
		if (overflow > 0) {
			overflow--;
			return;
		}
		if (depth <= 0) {
			return;
		}
		int index = depth - 1;
		if (phase != null) {
			for (int i = depth - 1; i >= 0; i--) {
				if (stack[i] == phase) {
					index = i;
					break;
				}
			}
		}
		long duration = System.nanoTime() - startNanos[index];
		Phase recorded = stack[index];
		if (index < depth - 1) {
			int moved = depth - 1 - index;
			System.arraycopy(stack, index + 1, stack, index, moved);
			System.arraycopy(startNanos, index + 1, startNanos, index, moved);
		}
		depth--;
		record(recorded, duration);
	}

	private void record(Phase phase, long durationNanos) {
		if (durationNanos > dominantNanos) {
			dominantNanos = durationNanos;
			dominant = phase == null ? Phase.NONE : phase;
		}
	}

	public Phase dominant() {
		return dominant;
	}

	public void resetFrame() {
		depth = 0;
		overflow = 0;
		dominant = Phase.NONE;
		dominantNanos = 0;
	}
}
