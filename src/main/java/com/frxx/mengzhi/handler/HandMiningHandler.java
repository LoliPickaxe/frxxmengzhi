package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.network.HandMiningSyncPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class HandMiningHandler {

    public static final String TAG_NORMAL = "FrxxHandMining";
    public static final String TAG_ALL = "FrxxHandMiningAll";
    public static final String MSG_CD_TAG = "FrxxHandMiningMsgCd";
    private static final int MSG_CD_TICKS = 40;

    public static class MiningInfo {
        public final int level;
        public final float speed;
        public final int cost;

        public MiningInfo(int level, float speed, int cost) {
            this.level = level;
            this.speed = speed;
            this.cost = cost;
        }
    }

    /** 境界档次(0练气/1筑基/2结丹/3元婴+) x 模式(0普通/1全开) */
    private static final MiningInfo[][] TABLE = {
        { new MiningInfo(2, 6.0F, 5), new MiningInfo(3, 6.0F, 10) },   // 练气
        { new MiningInfo(3, 6.0F, 4), new MiningInfo(4, 12.0F, 8) },   // 筑基
        { new MiningInfo(4, 12.0F, 1), new MiningInfo(5, 20.0F, 2) },  // 结丹
        { new MiningInfo(5, 12.0F, 0), new MiningInfo(5, 100.0F, 0) }  // 元婴及以上
    };

    public static MiningInfo table(int tier, boolean all) {
        return TABLE[Math.max(0, Math.min(3, tier))][all ? 1 : 0];
    }

    private static int tierOf(EntityPlayer player) {
        double realm = player.getEntityData().getDouble("JingJieNum");
        if (realm >= 4.0) return 3;
        if (realm >= 3.0) return 2;
        if (realm >= 2.0) return 1;
        return 0;
    }

    /** 服务端权威数据（NBT），未开启或未修炼返回 null */
    private static MiningInfo serverInfo(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (data.getDouble("JingJieNum") < 1.0) return null;
        boolean normal = data.getDouble(TAG_NORMAL) >= 0.5;
        boolean all = data.getDouble(TAG_ALL) >= 0.5;
        if (!normal && !all) return null;
        return table(tierOf(player), all);
    }

    /** 手持真工具时不生效；空手或手持非工具物品（如火把）视为空手 */
    private static boolean handEligible(EntityPlayer player) {
        ItemStack stack = player.getHeldItemMainhand();
        if (stack.isEmpty()) return true;
        Item item = stack.getItem();
        return !(item instanceof ItemTool || item instanceof ItemHoe || item instanceof ItemSword)
            && item.getToolClasses(stack).isEmpty();
    }

    private static double powerOf(EntityPlayer player) {
        return player.world.isRemote ? HandMiningClientHandler.cachedPower() : player.getEntityData().getDouble("Power");
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null) return;
        MiningInfo info = player.world.isRemote ? HandMiningClientHandler.cached(player) : serverInfo(player);
        if (info == null || !handEligible(player)) return;
        if (info.cost > 0 && powerOf(player) < info.cost) {
            event.setNewSpeed(0.0F);
            return;
        }
        event.setNewSpeed(info.speed);
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null) return;
        MiningInfo info = player.world.isRemote ? HandMiningClientHandler.cached(player) : serverInfo(player);
        if (info == null || !handEligible(player)) return;
        if (info.cost > 0 && powerOf(player) < info.cost) return;
        event.setCanHarvest(true);
    }

    /** 服务端权威：灵力不足无法破坏方块，成功则扣减灵力 */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;
        EntityPlayer player = event.getPlayer();
        if (player == null) return;
        MiningInfo info = serverInfo(player);
        if (info == null || info.cost <= 0 || !handEligible(player)) return;
        NBTTagCompound data = player.getEntityData();
        double power = data.getDouble("Power");
        if (power < info.cost) {
            event.setCanceled(true);
            if (data.getInteger(MSG_CD_TAG) <= 0) {
                data.setInteger(MSG_CD_TAG, MSG_CD_TICKS);
                player.sendMessage(new TextComponentString(TextFormatting.RED
                    + "灵力不足，无法破坏方块！需要 " + info.cost + " 点灵力"));
            }
        } else {
            data.setDouble("Power", power - info.cost);
        }
        if (player instanceof EntityPlayerMP) {
            sendSync((EntityPlayerMP) player);
        }
    }

    public static void toggle(EntityPlayerMP player, byte mode) {
        NBTTagCompound data = player.getEntityData();
        if (data.getDouble("JingJieNum") < 1.0) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "您尚未修仙，无法使用空手挖矿！"));
            return;
        }
        boolean all = mode != 0;
        String tag = all ? TAG_ALL : TAG_NORMAL;
        String otherTag = all ? TAG_NORMAL : TAG_ALL;
        boolean on = data.getDouble(tag) < 0.5;
        data.setDouble(tag, on ? 1.0 : 0.0);
        if (on) {
            data.setDouble(otherTag, 0.0);
        }
        String modeName = all ? "全开" : "普通";
        if (on) {
            MiningInfo info = table(tierOf(player), all);
            player.sendMessage(new TextComponentString(TextFormatting.GOLD
                + "空手挖矿·" + modeName + " 已开启：挖掘等级 " + info.level + "，速度 " + (int) info.speed
                + "，每方块消耗灵力 " + info.cost));
        } else {
            player.sendMessage(new TextComponentString(TextFormatting.GRAY + "空手挖矿·" + modeName + " 已关闭"));
        }
        sendSync(player);
    }

    private static void sendSync(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        FanRenXiuXianMengZhi.NETWORK.sendTo(new HandMiningSyncPacket(
            tierOf(player),
            data.getDouble(TAG_NORMAL) >= 0.5,
            data.getDouble(TAG_ALL) >= 0.5,
            (int) data.getDouble("Power")), player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            sendSync((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        NBTTagCompound data = player.getEntityData();
        int cd = data.getInteger(MSG_CD_TAG);
        if (cd > 0) data.setInteger(MSG_CD_TAG, cd - 1);
        if ((data.getDouble(TAG_NORMAL) >= 0.5 || data.getDouble(TAG_ALL) >= 0.5)
            && player.getServerWorld().getTotalWorldTime() % 40L == 0L) {
            sendSync(player);
        }
        if (data.getDouble("JingJieNum") >= 1.0) {
            PotionEffect effect = player.getActivePotionEffect(MobEffects.NIGHT_VISION);
            if (effect == null || effect.getDuration() < 560) {
                player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 600, 0, false, false));
            }
        }
    }
}
