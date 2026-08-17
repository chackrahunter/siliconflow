package dev.doncalvin.m3frametime.mixin.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.compat.StackCompat;
import dev.doncalvin.m3frametime.compat.IrisCompat;
import dev.doncalvin.m3frametime.config.M3Config;
import dev.doncalvin.m3frametime.telemetry.RamDiscipline;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.block.entity.ConduitBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * High-performance block entity cull: distance gating + sign/banner detail throttling.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@Shadow
	public net.minecraft.client.render.Camera camera;

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private <E extends BlockEntity> void m3frametime$cullBe(
		E blockEntity,
		float tickDelta,
		MatrixStack matrices,
		VertexConsumerProvider vertexConsumers,
		CallbackInfo ci
	) {
		M3Config cfg = M3FrametimeMod.config();
		if (!cfg.blockEntityCull || blockEntity == null || this.camera == null) {
			return;
		}
		BlockPos pos = blockEntity.getPos();
		Vec3d cam = this.camera.getPos();
		double dx = pos.getX() + 0.5 - cam.x;
		double dy = pos.getY() + 0.5 - cam.y;
		double dz = pos.getZ() + 0.5 - cam.z;
		double distSq = dx * dx + dy * dy + dz * dz;


		// Specialized sign / banner distance culling (chests / spawners stay up to blockEntityCullDistance)
		if (cfg.skipFarSignText && blockEntity instanceof SignBlockEntity) {
			double signDist = cfg.farSignDistance;
			if (distSq > signDist * signDist) {
				ci.cancel();
				return;
			}
		}
		if (cfg.skipFarBannerPatterns && blockEntity instanceof BannerBlockEntity) {
			double bannerDist = cfg.farBannerDistance;
			if (distSq > bannerDist * bannerDist) {
				ci.cancel();
				return;
			}
		}

		double max = RamDiscipline.get().effectiveBlockEntityCullDistance();
		if (distSq > max * max) {
			ci.cancel();
		}
	}
}
