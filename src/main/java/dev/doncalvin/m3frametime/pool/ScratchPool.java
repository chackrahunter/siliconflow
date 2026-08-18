package dev.doncalvin.m3frametime.pool;

/**
 * Thread-local scratch for hot paths owned by this mod.
 * Capacity is capped — never grow unbounded StringBuilders under RAM pressure.
 */
public final class ScratchPool {
	private static final ThreadLocal<ScratchPool> TL = ThreadLocal.withInitial(ScratchPool::new);
	private static final int SB_INITIAL = 160;
	private static final int SB_SOFT_CAP = 256;
	private static final long[] POW10 = {
		1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L
	};

	public final StringBuilder stringBuilder = new StringBuilder(SB_INITIAL);
	public final StringBuilder logBuilder = new StringBuilder(SB_INITIAL);

	private ScratchPool() {}

	public static ScratchPool get() {
		return TL.get();
	}

	/** Clear length + shrink StringBuilders that grew past soft cap. No the JVM collector. */
	public void releaseEphemeral() {
		shrinkBuilder(stringBuilder);
		shrinkBuilder(logBuilder);
	}

	/** Releases only the caller's pool; never mutates another thread's scratch state. */
	public static void releaseAllEphemeral() {
		get().releaseEphemeral();
	}

	private static void shrinkBuilder(StringBuilder sb) {
		sb.setLength(0);
		if (sb.capacity() > SB_SOFT_CAP) {
			sb.trimToSize();
			sb.ensureCapacity(SB_INITIAL);
		}
	}

	public StringBuilder stringBuilder() {
		stringBuilder.setLength(0);
		return stringBuilder;
	}

	public StringBuilder logBuilder() {
		logBuilder.setLength(0);
		return logBuilder;
	}

	/** Append a fixed-point decimal without boxing or String.format. */
	public static StringBuilder appendFixed(StringBuilder sb, double value, int decimals) {
		if (decimals < 0) {
			decimals = 0;
		} else if (decimals >= POW10.length) {
			decimals = POW10.length - 1;
		}
		if (Double.isNaN(value)) {
			return sb.append("NaN");
		}
		if (Double.isInfinite(value)) {
			return sb.append(value > 0 ? "Inf" : "-Inf");
		}
		boolean neg = value < 0.0;
		if (neg) {
			value = -value;
		}
		long scale = POW10[decimals];
		long scaled = Math.round(value * (double) scale);
		if (neg && scaled != 0L) {
			sb.append('-');
		}
		if (decimals == 0) {
			return sb.append(scaled);
		}
		long ip = scaled / scale;
		long fp = scaled % scale;
		sb.append(ip).append('.');
		for (int i = decimals - 1; i >= 0; i--) {
			long digit = (fp / POW10[i]) % 10L;
			sb.append((char) ('0' + (int) digit));
		}
		return sb;
	}
}
