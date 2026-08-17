package dev.doncalvin.m3frametime.engine;

public final class SiliconChipInfo {
	private static volatile ChipTier chipTier = ChipTier.BASE;
	private static volatile int maxParticles = 160;
	private static volatile double entityCullDistance = 72.0;

	private SiliconChipInfo() {}

	public static ChipTier getChipTier() { return chipTier; }
	public static int getMaxParticles() { return maxParticles; }
	public static double getEntityCullDistance() { return entityCullDistance; }

	public static void register(ChipTier tier, int particles, double cullDistance) {
		chipTier = tier;
		maxParticles = particles;
		entityCullDistance = cullDistance;
	}
}
