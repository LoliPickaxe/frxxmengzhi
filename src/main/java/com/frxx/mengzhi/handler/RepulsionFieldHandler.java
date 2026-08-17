package com.frxx.mengzhi.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 * 排斥力场（小键盘 8 开关）：
 *  - 玩家最大血量 >= 目标 3 倍：周围 11×11×11 内所有生物与投掷物被弹开，无法靠近；
 *  - 差距不足 3 倍（如 2 倍）：排斥范围缩小为 5×5×5；
 *  - 投掷物（箭/火球/雪球等）没有血量，无条件被弹开（按 11×11×11 处理）。
 */
public class RepulsionFieldHandler {

    public static final String TAG_ON = "FrxxRepulsionOn";
    public static final double RATIO_WIDE = 3.0;
    public static final double RANGE_WIDE = 5.5;   // 11×11×11
    public static final double RANGE_NARROW = 2.5; // 5×5×5

    public static boolean isOn(EntityPlayer player) {
        return player.getEntityData().getInteger(TAG_ON) == 1;
    }

    public static void toggle(EntityPlayerMP player) {
        if (!GuardHandler.hasRealm(player.getEntityData())) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "【排斥力场】境界不足：需要 练气 及以上，凡人无法开启"));
            return;
        }
        boolean now = player.getEntityData().getInteger(TAG_ON) != 1;
        player.getEntityData().setInteger(TAG_ON, now ? 1 : 0);
        player.sendMessage(new TextComponentString(
            (now ? TextFormatting.GREEN : TextFormatting.GRAY) + "【排斥力场】"
                + (now ? "已开启：差距3倍以上弹开 11×11×11 范围，不足3倍弹开 5×5×5"
                    : "已关闭")));
    }

    /** 目标类型判定：生物（EntityLiving，含玩家/怪物/动物）与投掷物（IProjectile）。
     *  与 ProjectE 排斥火把（WorldHelper.repelEntitiesInAABBFromPoint）一致。 */
    private static boolean isPushable(Entity e) {
        return e instanceof net.minecraft.entity.EntityLiving
            || e instanceof net.minecraft.entity.IProjectile;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        if (!isOn(player)) {
            return;
        }
        double px = player.posX;
        double py = player.posY + 1.0;
        double pz = player.posZ;
        double playerHealth = player.getMaxHealth();
        List<Entity> all = player.world.getEntitiesWithinAABB(Entity.class,
            new AxisAlignedBB(px - RANGE_WIDE, py - RANGE_WIDE, pz - RANGE_WIDE,
                px + RANGE_WIDE, py + RANGE_WIDE, pz + RANGE_WIDE));
        for (Entity e : all) {
            if (e == player || !isPushable(e)) {
                continue;
            }
            double r;
            if (e instanceof EntityLivingBase) {
                double mobHealth = ((EntityLivingBase) e).getMaxHealth();
                if (mobHealth <= 0.0) {
                    r = RANGE_WIDE;
                } else {
                    r = playerHealth / mobHealth >= RATIO_WIDE ? RANGE_WIDE : RANGE_NARROW;
                }
            } else {
                r = RANGE_WIDE;
            }
            // 射入地面/固定的投掷物不弹（防止插在地上的箭被弹飞，同 ProjectE）
            if (e instanceof net.minecraft.entity.IProjectile && e.onGround) {
                continue;
            }
            double dx = e.posX - px;
            double dy = e.posY - py;
            double dz = e.posZ - pz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist >= r || dist <= 0.001) {
                continue;
            }
            // 排斥机制与 ProjectE 一致：只叠加径向速度，位置交给物理碰撞引擎，
            // 实体绝不会被瞬移进墙里/地下（ProjectE: motion += dir / 1.5 / dist）
            e.motionX += dx / 1.5 / dist;
            e.motionY += dy / 1.5 / dist;
            e.motionZ += dz / 1.5 / dist;
            e.velocityChanged = true;
        }
    }
}