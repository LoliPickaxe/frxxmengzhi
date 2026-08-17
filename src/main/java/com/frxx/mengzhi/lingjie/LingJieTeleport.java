package com.frxx.mengzhi.lingjie;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/**
 * 灵界地道传传送：
 *
 * 直接用 entity.changeDimension() 会因自定义维度缺少传送门而抛 NPE
 * （Teleporter.placeInExistingPortal），导致玩家被卡在中间（空白、无法移动）。
 * 这里复用凡人修仙本源已验证的写法：
 *   PlayerList.transferPlayerToDimension(player, dim, new TeleporterDirect(world))
 * 其中 TeleporterDirect 把三种传送门逻辑覆盖为空/直通。
 *
 * 重要：自定义维度在服务器启动时（loadAllWorlds）只对“已注册”的维度预创建世界，
 * 若目标世界尚未加载（例如首次进入），DimensionManager.getWorld() 会返回 null，
 * 必须先用 initDimension() 热加载，否则向玩家通告“已传送”但实际原地不动。
 */
public final class LingJieTeleport {

    private LingJieTeleport() {
    }

    /**
     * @return true 表示传送成功（玩家已处于目标维度）；false 表示失败（原样未动）
     */
    public static boolean to(EntityPlayerMP player, int dim) {
        if (player.getServer() == null
            || player.world.isRemote || player.isRiding() || player.isBeingRidden()) {
            return false;
        }
        if (!DimensionManager.isDimensionRegistered(dim)) {
            if (FanRenXiuXianMengZhi.logger != null) {
                FanRenXiuXianMengZhi.logger.error("灵界传送失败：维度 {} 未注册", dim);
            }
            return false;
        }
        WorldServer world = DimensionManager.getWorld(dim);
        if (world == null) {
            if (FanRenXiuXianMengZhi.logger != null) {
                FanRenXiuXianMengZhi.logger.warn("灵界世界未加载，热加载维度 {}", dim);
            }
            try {
                DimensionManager.initDimension(dim);
            } catch (Exception e) {
                if (FanRenXiuXianMengZhi.logger != null) {
                    FanRenXiuXianMengZhi.logger.error("灵界世界热加载异常 id={}", dim, e);
                }
                return false;
            }
            world = DimensionManager.getWorld(dim);
        }
        if (world == null) {
            if (FanRenXiuXianMengZhi.logger != null) {
                FanRenXiuXianMengZhi.logger.error("灵界世界创建后仍为空 id={}", dim);
            }
            return false;
        }
        player.getServer().getPlayerList()
            .transferPlayerToDimension(player, dim, new TeleporterDirect(world));
        BlockPos pos = world.getTopSolidOrLiquidBlock(world.getSpawnPoint());
        player.connection.setPlayerLocation(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
            player.rotationYaw, player.rotationPitch);
        if (FanRenXiuXianMengZhi.logger != null) {
            FanRenXiuXianMengZhi.logger.info("灵界传送完成：{} -> 维度 {}, 坐标 {},{},{}",
                player.getName(), player.dimension, (int) player.posX, (int) player.posY, (int) player.posZ);
        }
        return player.dimension == dim;
    }

    public static class TeleporterDirect extends Teleporter {
        public TeleporterDirect(WorldServer worldserver) {
            super(worldserver);
        }

        @Override
        public void placeInPortal(Entity entityIn, float rotationYaw) {
        }

        @Override
        public boolean makePortal(Entity entityIn) {
            return true;
        }
    }
}