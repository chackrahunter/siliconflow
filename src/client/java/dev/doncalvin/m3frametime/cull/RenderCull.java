package dev.doncalvin.m3frametime.cull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Shared camera-distance helpers for additive skip/cull mixins.
 */
public final class RenderCull {
	private RenderCull() {}

	public static MinecraftClient client() {
		return MinecraftClient.getInstance();
	}

	public static Vec3d cameraPos() {
		MinecraftClient client = client();
		if (client == null || client.gameRenderer == null) {
			return null;
		}
		Camera camera = client.gameRenderer.getCamera();
		return camera == null ? null : camera.getPos();
	}

	public static boolean isClient(Entity entity) {
		if (entity == null) {
			return false;
		}
		World world = entity.getWorld();
		return world != null && world.isClient();
	}

	public static boolean isPlayer(Entity entity) {
		return entity != null && (entity.isPlayer() || entity instanceof PlayerEntity);
	}

	public static boolean fartherThan(double x, double y, double z, double maxDistance) {
		Vec3d cam = cameraPos();
		if (cam == null) {
			return false;
		}
		double dx = cam.x - x;
		double dy = cam.y - y;
		double dz = cam.z - z;
		return (dx * dx + dy * dy + dz * dz) > (maxDistance * maxDistance);
	}

	public static boolean fartherThan(Entity entity, double maxDistance) {
		return entity != null && fartherThan(entity.getX(), entity.getY(), entity.getZ(), maxDistance);
	}

	public static boolean fartherThan(BlockPos pos, double maxDistance) {
		if (pos == null) {
			return false;
		}
		Vec3d cam = cameraPos();
		if (cam == null) {
			return false;
		}
		return pos.getSquaredDistance(cam.x, cam.y, cam.z) > (maxDistance * maxDistance);
	}
}
