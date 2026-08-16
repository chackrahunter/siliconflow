package dev.doncalvin.m3frametime.threading;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single-producer single-consumer ring of preallocated slots. Zero allocation in the hot path.
 */
public final class SpscRingBuffer<T> {
	private final T[] slots;
	private final int mask;
	private final AtomicInteger writeIdx = new AtomicInteger();
	private final AtomicInteger readIdx = new AtomicInteger();

	@SuppressWarnings("unchecked")
	public SpscRingBuffer(int capacityPowerOfTwo, T[] prefilled) {
		if (Integer.bitCount(capacityPowerOfTwo) != 1) {
			throw new IllegalArgumentException("capacity must be power of two");
		}
		if (prefilled.length != capacityPowerOfTwo) {
			throw new IllegalArgumentException("prefilled length must match capacity");
		}
		this.slots = prefilled;
		this.mask = capacityPowerOfTwo - 1;
	}

	public T claimWriteSlot() {
		int w = writeIdx.get();
		int r = readIdx.get();
		if (((w + 1) & mask) == (r & mask)) {
			return null; // full — drop oldest by advancing read
		}
		return slots[w & mask];
	}

	public void publishWrite() {
		writeIdx.lazySet((writeIdx.get() + 1) & mask);
	}

	/** Force publish even when full by dropping one unread event. */
	public T forceClaimWriteSlot() {
		int w = writeIdx.get();
		int next = (w + 1) & mask;
		if (next == (readIdx.get() & mask)) {
			readIdx.lazySet((readIdx.get() + 1) & mask);
		}
		return slots[w & mask];
	}

	public T poll() {
		int r = readIdx.get();
		if (r == writeIdx.get()) {
			return null;
		}
		T value = slots[r & mask];
		readIdx.lazySet((r + 1) & mask);
		return value;
	}

	public int size() {
		return (writeIdx.get() - readIdx.get()) & mask;
	}
}
