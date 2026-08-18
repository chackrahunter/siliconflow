package dev.doncalvin.m3frametime.engine;

/**
 * Detected Apple Silicon chip tier, published once at startup by {@link SiliconCpuTopology}.
 * {@link #isRegistered()} lets config derivation skip chip scaling until real detection has run,
 * so placeholder values can never leak into the persisted config.
 */
public final class SiliconChipInfo {
	private static volatile ChipTier chipTier = ChipTier.BASE;
	private static volatile boolean registered = false;

	private SiliconChipInfo() {}

	public static ChipTier getChipTier() {
		return chipTier;
	}

	public static boolean isRegistered() {
		return registered;
	}

	public static void register(ChipTier tier) {
		if (tier != null) {
			chipTier = tier;
		}
		registered = true;
	}
}
