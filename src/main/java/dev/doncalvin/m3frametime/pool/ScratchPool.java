package dev.doncalvin.m3frametime.pool;

import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-local scratch for hot paths owned by this mod. Avoids ephemeral JOML/BlockPos/String churn.
 * Capacity is capped — never grow unbounded byte[] / StringBuilder under RAM pressure.
 */
public final class ScratchPool {
	private static final ThreadLocal<ScratchPool> TL = ThreadLocal.withInitial(ScratchPool::new);
	/** Soft registry so RamDiscipline can trim without touching every ThreadLocal blindly. */
	private static final List<ScratchPool> LIVE = new ArrayList<>(4);
	private static final Object LIVE_LOCK = new Object();

	private static final int SB_INITIAL = 160;
	private static final int SB_SOFT_CAP = 256;
	private static final int SOUND_BYTES = 4096;
	private static final long[] POW10 = {
		1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L
	};

	public final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
	public final Vector3f vec3f = new Vector3f();
	public final Matrix4f matrix4f = new Matrix4f();
	public final float[] mat16 = new float[16];
	public final double[] vec3d = new double[3];
	public final StringBuilder stringBuilder = new StringBuilder(SB_INITIAL);
	public final StringBuilder logBuilder = new StringBuilder(SB_INITIAL);
	/** Fixed-size; never reallocated. */
	public final byte[] soundScratch = new byte[SOUND_BYTES];
	public final ByteBuffer soundDirect = ByteBuffer.allocateDirect(SOUND_BYTES).order(ByteOrder.nativeOrder());

	private ScratchPool() {
		synchronized (LIVE_LOCK) {
			LIVE.add(this);
		}
	}

	public static ScratchPool get() {
		return TL.get();
	}

	/** Clear length + shrink StringBuilders that grew past soft cap. No System.gc(). */
	public void releaseEphemeral() {
		shrinkBuilder(stringBuilder);
		shrinkBuilder(logBuilder);
		soundDirect.clear();
	}

	public static void releaseAllEphemeral() {
		ScratchPool local = TL.get();
		local.releaseEphemeral();
		synchronized (LIVE_LOCK) {
			for (int i = 0; i < LIVE.size(); i++) {
				ScratchPool p = LIVE.get(i);
				if (p != local) {
					p.releaseEphemeral();
				}
			}
		}
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

	public ByteBuffer soundDirectCleared() {
		soundDirect.clear();
		return soundDirect;
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

	public static StringBuilder appendLong(StringBuilder sb, long value) {
		return sb.append(value);
	}

	public static StringBuilder appendInt(StringBuilder sb, int value) {
		return sb.append(value);
	}
}
