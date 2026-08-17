package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.network.LingLiSkillPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 轻功（服务端判定）：
 *  - 双击移动键（W/A/S/D）：沿该方向冲刺约 3~4 格，消耗 2 点饥饿值；
 *  - 双击空格：多段跳（空中二次起跳），消耗 2 点饥饿值；飞行中（创造/御剑等 isFlying）禁用；
 *  - 境界限制：必须 练气（JingJieNum >= 1）及以上，凡人无法使用；
 *  - 消费饥饿值：每次 2 点（LingLiSkillHandler.COST）。
 */
public class LingLiSkillHandler {

    public static final int COST = 2;
    public static final double DASH_SPEED = 1.2;
    @SuppressWarnings("unused")
    public static final double JUMP_SPEED = 0.55;
    public static final long DASH_COOLDOWN_TICKS = 20;
    public static final long JUMP_COOLDOWN_TICKS = 10;

    private static final Map<UUID, long[]> lastUse = new HashMap<>();

    private static String skillName(int type) {
        return type == LingLiSkillPacket.TYPE_JUMP ? "多段跳" : "轻功";
    }

    public static void handle(EntityPlayerMP player, int type, int dir) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        if (!GuardHandler.hasRealm(player.getEntityData())) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "【" + skillName(type) + "】境界不足：需要 练气 及以上，凡人无法使用"));
            return;
        }
        if (type != LingLiSkillPacket.TYPE_DASH && type != LingLiSkillPacket.TYPE_JUMP) {
            return;
        }
        if (player.getFoodStats().getFoodLevel() < COST) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "【" + skillName(type) + "】饥饿值不足：需要 " + COST + " 点"));
            return;
        }
        long now = player.world.getTotalWorldTime();
        long[] last = lastUse.computeIfAbsent(player.getUniqueID(), k -> new long[2]);
        long cooldown = type == LingLiSkillPacket.TYPE_JUMP ? JUMP_COOLDOWN_TICKS : DASH_COOLDOWN_TICKS;
        if (now - last[type] < cooldown) {
            return;
        }
        last[type] = now;
        if (type == LingLiSkillPacket.TYPE_JUMP) {
            if (player.capabilities.isFlying || player.capabilities.allowFlying || player.isElytraFlying()) {
                return;
            }
            player.motionY = JUMP_SPEED;
            player.fallDistance = 0.0F;
        } else {
            double[] v = dirVector(player, dir);
            player.motionX = v[0] * DASH_SPEED;
            player.motionZ = v[1] * DASH_SPEED;
            player.fallDistance = 0.0F;
        }
        player.getFoodStats().setFoodLevel(player.getFoodStats().getFoodLevel() - COST);
        player.velocityChanged = true;
    }

    /** 以玩家朝向为基准：0=前 1=后 2=左 3=右 的水平单位向量。 */
    private static double[] dirVector(EntityPlayerMP player, int dir) {
        float yaw = (float) Math.toRadians(player.rotationYaw);
        float sin = MathHelper.sin(yaw);
        float cos = MathHelper.cos(yaw);
        switch (dir) {
            case 1:
                return new double[] { sin, -cos };
            case 2:
                return new double[] { cos, sin };
            case 3:
                return new double[] { -cos, -sin };
            default:
                return new double[] { -sin, cos };
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        lastUse.remove(event.player.getUniqueID());
    }
}