package com.frxx.mengzhi.lingjie;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 灵界寿元倍率（设计文档 §4）：
 *  - 进入灵界时按当前境界（本体 JingJieNum）结算一次"灵界加成"：
 *    加成 = 对应境界基准寿元 × (倍率 - 1)，写入 ShouYuan；
 *  - 永久增益：离开灵界后保留，重复进入不重复叠加（记忆标志）；
 *  - 重进存档直接身处灵界时由玩家 tick 兜底结算。
 */
public class LingJieShouYuanHandler {

    private static final String KEY_SHOUYUAN = "ShouYuan";
    private static final String KEY_JINGJIENUM = "JingJieNum";
    private static final String KEY_SETTLED = "LingJieShouYuanSettled";
    private static final String KEY_BONUS = "LingJieShouYuanBonus";

    private static final double[] BASE_BY_REALM = {
        50.0D,      // 凡人
        100.0D,     // 练气
        200.0D,     // 筑基
        500.0D,     // 结丹
        1000.0D,    // 元婴
        2000.0D     // 化神及以上
    };

    private static final double[] MULT_BY_REALM = {
        1.5D,       // 凡人
        2.5D,       // 练气
        2.5D,       // 筑基
        3.5D,       // 结丹
        4.5D,       // 元婴
        5.5D        // 化神及以上
    };

    private static String realmName(int realm) {
        String[] names = {"凡人", "练气", "筑基", "结丹", "元婴", "化神"};
        return names[Math.min(Math.max(realm, 0), names.length - 1)];
    }

    private static void settle(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (data.getBoolean(KEY_SETTLED)) {
            return;
        }
        int realm = data.hasKey(KEY_JINGJIENUM) ? (int) data.getDouble(KEY_JINGJIENUM) : 0;
        int idx = Math.min(Math.max(realm, 0), BASE_BY_REALM.length - 1);
        double base = BASE_BY_REALM[idx];
        double mult = MULT_BY_REALM[idx];
        double bonus = base * (mult - 1.0D);
        double cur = data.hasKey(KEY_SHOUYUAN) ? data.getDouble(KEY_SHOUYUAN) : 0.0D;
        data.setDouble(KEY_SHOUYUAN, cur + bonus);
        data.setDouble(KEY_BONUS, bonus);
        data.setBoolean(KEY_SETTLED, true);
        if (FanRenXiuXianMengZhi.logger != null) {
            FanRenXiuXianMengZhi.logger.info("玩家 {} 进入灵界结算寿元加成：境界 {}，基准 {}，倍率 {}，加成 +{}",
                player.getName(), realmName(realm), (int) base, mult, (int) bonus);
        }
        player.sendMessage(new TextComponentString(TextFormatting.GOLD
            + "【寿元】灵界历练：境界 " + realmName(realm) + "，寿元基准 " + (int) base
            + " × 倍率 " + mult + "，结算灵界加成 +" + (int) bonus + " 年！"));
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player.world.isRemote) {
            return;
        }
        if (event.toDim == LingJieDimension.DIMENSION_ID) {
            settle(event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        if (event.player.dimension == LingJieDimension.DIMENSION_ID) {
            settle(event.player);
        }
    }
}