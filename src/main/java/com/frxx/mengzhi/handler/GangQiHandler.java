package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.GangQiActionPacket;
import com.frxx.mengzhi.network.GangQiStatePacket;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 * 罡气系统：
 *  - 小键盘5 开关，小键盘6 切换模式（普通/全开）
 *  - 普通模式：每 10 tick(0.5s) 攻击一次，每秒耗 10 灵力
 *  - 全开模式：每 4 tick(0.2s) 攻击一次，每秒耗 100 灵力
 *  - 攻击 3x3x3 范围内除玩家外的生物，(物攻+法攻)*0.2 伤害，横扫之刃粒子
 *  - 灵力不足自动关闭
 */
public class GangQiHandler {

    public static final String TAG_ON = "GangQiOn";
    public static final String TAG_MODE = "GangQiMode";

    public static final int MODE_NORMAL = 0;
    public static final int MODE_FULL = 1;

    private static final int NORMAL_INTERVAL_TICKS = 10;   // 0.5s
    private static final int FULL_INTERVAL_TICKS = 4;      // 0.2s
    private static final double NORMAL_COST_PER_SEC = 10.0;
    private static final double FULL_COST_PER_SEC = 100.0;

    private static final double DMG_RATIO = 0.2;
    private static final double RANGE = 1.5; // 3x3x3: 以玩家为中心 +/-1.5

    private static final DamageSource GANGQI_DAMAGE = new DamageSource("gangqi").setDamageBypassesArmor();

    public static void handleAction(EntityPlayerMP player, GangQiActionPacket.Action action) {
        NBTTagCompound data = player.getEntityData();
        if (!GuardHandler.hasRealm(data)) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u60a8\u672a\u4fee\u4ed9\u4e0d\u53ef\u4f7f\u7528\u7f61\u6c14"));
            return;
        }
        switch (action) {
            case TOGGLE: {
                boolean on = data.getBoolean(TAG_ON);
                data.setBoolean(TAG_ON, !on);
                if (!on) {
                    int mode = data.getInteger(TAG_MODE);
                    double cost = mode == MODE_FULL ? FULL_COST_PER_SEC : NORMAL_COST_PER_SEC;
                    String modeName = mode == MODE_FULL ? "\u5168\u5f00\u6a21\u5f0f" : "\u666e\u901a\u6a21\u5f0f";
                    player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "[\u7f61\u6c14] \u5df2\u5f00\u542f\uff0c\u5f53\u524d\u6a21\u5f0f\uff1a" + modeName
                            + "\uff0c\u8017\u7075 " + (long) cost + "/\u79d2"));
                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.GRAY + "[\u7f61\u6c14] \u5df2\u5173\u95ed"));
                }
                break;
            }
            case MODE: {
                if (!data.getBoolean(TAG_ON)) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "[\u7f61\u6c14] \u8bf7\u5148\u5f00\u542f\u7f61\u6c14"));
                    return;
                }
                int mode = data.getInteger(TAG_MODE);
                mode = (mode == MODE_FULL) ? MODE_NORMAL : MODE_FULL;
                data.setInteger(TAG_MODE, mode);
                double cost = mode == MODE_FULL ? FULL_COST_PER_SEC : NORMAL_COST_PER_SEC;
                String modeName = mode == MODE_FULL ? "\u5168\u5f00\u6a21\u5f0f" : "\u666e\u901a\u6a21\u5f0f";
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "[\u7f61\u6c14] \u5207\u6362\u81f3\uff1a" + modeName
                        + "\uff0c\u8017\u7075 " + (long) cost + "/\u79d2"));
                break;
            }
            default:
                break;
        }
        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = event.player;
        if (player.world.isRemote) {
            return;
        }
        NBTTagCompound data = player.getEntityData();
        if (!data.getBoolean(TAG_ON)) {
            return;
        }
        int mode = data.getInteger(TAG_MODE);
        double costPerTick = (mode == MODE_FULL ? FULL_COST_PER_SEC : NORMAL_COST_PER_SEC) / 20.0;

        // 扣灵力
        double power = data.getDouble("Power");
        if (power < costPerTick) {
            data.setBoolean(TAG_ON, false);
            player.sendMessage(new TextComponentString(TextFormatting.RED + "[\u7f61\u6c14] \u7075\u529b\u4e0d\u8db3\uff0c\u7f61\u6c14\u5df2\u81ea\u52a8\u5173\u95ed"));
            if (player instanceof EntityPlayerMP) {
                sync((EntityPlayerMP) player);
            }
            return;
        }
        data.setDouble("Power", power - costPerTick);

        // 攻击频率
        int interval = (mode == MODE_FULL) ? FULL_INTERVAL_TICKS : NORMAL_INTERVAL_TICKS;
        long worldTime = player.world.getTotalWorldTime();
        if (worldTime % interval != 0) {
            return;
        }

        WorldServer world = (WorldServer) player.world;
        double cx = player.posX;
        double cy = player.posY;
        double cz = player.posZ;
        AxisAlignedBB aabb = new AxisAlignedBB(cx - RANGE, cy - RANGE, cz - RANGE, cx + RANGE, cy + RANGE, cz + RANGE);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb);

        double damage = (data.getDouble("Attack") + data.getDouble("MagicAttack")) * DMG_RATIO;
        if (damage <= 0) {
            damage = 1.0;
        }
        for (EntityLivingBase target : targets) {
            if (target == player) {
                continue;
            }
            if (target.isDead || !target.isEntityAlive()) {
                continue;
            }
            // 攻击实体（可被其他 mod 拦截）
            target.attackEntityFrom(GANGQI_DAMAGE, (float) damage);
        }
        // 横扫之刃粒子（SWEEP_ATTACK）
        double yaw = player.rotationYaw * 0.017453292F;
        double px = cx - MathHelper.sin((float) yaw) * 0.5;
        double pz = cz + MathHelper.cos((float) yaw) * 0.5;
        world.spawnParticle(EnumParticleTypes.SWEEP_ATTACK, px, cy, pz, 0.0D, 0.0D, 0.0D);
    }

    private static void sync(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        FanRenXiuXianMengZhi.NETWORK.sendTo(
            new GangQiStatePacket(data.getBoolean(TAG_ON), data.getInteger(TAG_MODE)), player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player.world.isRemote) {
            return;
        }
        sync((EntityPlayerMP) event.player);
    }
}
