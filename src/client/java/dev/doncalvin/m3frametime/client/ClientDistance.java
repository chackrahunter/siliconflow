package dev.doncalvin.m3frametime.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Cached camera position for per-frame distance gates. Avoids repeated camera lookups.
 */
public final class ClientDistance {
	private static long stampNanos;
	private static double camX;
	private static double camY;
	private static double camZ;
	private static boolean valid;

	private ClientDistance() {}

	public static void invalidate() {
		valid = false;
	}

	public static boolean tooFarSq(double x, double y, double z, double maxDistSq) {
		if (!ensureCamera()) {
			return false;
		}
		double dx = x - camX;
		double dy = y - camY;
		double dz = z - camZ;
		return dx * dx + dy * dy + dz * dz > maxDistSq;
	}

	public static boolean tooFar(double x, double y, double z, double maxDist) {
		if (maxDist <= 0.0) {
			return false;
		}
		return tooFarSq(x, y, z, maxDist * maxDist);
	}

	private static boolean ensureCamera() {
		long now = System.nanoTime();
		// Refresh at most once per ~1 ms — enough for tick/render hot paths in one frame.
		if (valid && now - stampNanos < 1_000_000L) {
			return true;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.gameRenderer == null || client.gameRenderer.getCamera() == null) {
			valid = false;
			return false;
		}
		Vec3d pos = client.gameRenderer.getCamera().getPos();
		camX = pos.x;
		camY = pos.y;
		camZ = pos.z;
		stampNanos = now;
		valid = true;
		return true;
	}
}
