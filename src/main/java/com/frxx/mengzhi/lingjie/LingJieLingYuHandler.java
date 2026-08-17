package com.frxx.mengzhi.lingjie;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 灵界灵裕值（设计文档 §2）：
 *  - 进入灵界：记录原上限到独立标志位，灵裕上限提升为 100000；
 *  - 灵界内：独立计时键每 tick +1（不污染功法模组的灵裕值增加计时），
 *    计满 24000（1 游戏日）→ 灵裕 +10000，超出上限钳制；
 *  - 离开灵界：上限还原到记录的原值，当前值超过新上限则钳制。
 */
public class LingJieLingYuHandler {

    public static final double LINGJIE_MAX = 100000.0D;
    public static final double LINGJIE_DAILY = 10000.0D;
    private static final double DAY_TICKS = 24000.0D;

    private static final String KEY_CUR = "LingYuZhi";
    private static final String KEY_MAX = "LingYuZhiMax";
    private static final String KEY_TIMER = "LingJieLingYuTimer";
    private static final String KEY_ORG_MAX = "LingJieLingYuOrgMax";

    private static boolean inLingJie(EntityPlayer player) {
        return player.dimension == LingJieDimension.DIMENSION_ID;
    }

    private static void clampToMax(NBTTagCompound data) {
        double cur = data.hasKey(KEY_CUR) ? data.getDouble(KEY_CUR) : 0.0D;
        double max = data.hasKey(KEY_MAX) ? data.getDouble(KEY_MAX) : 0.0D;
        if (cur > max) {
            data.setDouble(KEY_CUR, max);
        }
    }

    private static void applyLingJieRule(NBTTagCompound data, EntityPlayer player) {
        if (!data.hasKey(KEY_ORG_MAX)) {
            double org = data.hasKey(KEY_MAX) ? data.getDouble(KEY_MAX) : 0.0D;
            data.setDouble(KEY_ORG_MAX, org);
            data.setDouble(KEY_MAX, LINGJIE_MAX);
            clampToMax(data);
            player.sendMessage(new TextComponentString(TextFormatting.YELLOW
                + "【灵裕】灵界灵气充沛：灵裕上限提升至 100000，每日回复 10000！"));
        }
        double cur = data.hasKey(KEY_CUR) ? data.getDouble(KEY_CUR) : 0.0D;
        double max = data.hasKey(KEY_MAX) ? data.getDouble(KEY_MAX) : 0.0D;
        if (cur < max) {
            double timer = (data.hasKey(KEY_TIMER) ? data.getDouble(KEY_TIMER) : 0.0D) + 1.0D;
            if (timer >= DAY_TICKS) {
                data.setDouble(KEY_CUR, cur + LINGJIE_DAILY);
                data.setDouble(KEY_TIMER, 0.0D);
                clampToMax(data);
            } else {
                data.setDouble(KEY_TIMER, timer);
            }
        }
    }

    private static void restoreOriginRule(NBTTagCompound data, EntityPlayer player) {
        if (!data.hasKey(KEY_ORG_MAX)) {
            return;
        }
        double org = data.getDouble(KEY_ORG_MAX);
        data.setDouble(KEY_MAX, org);
        data.setDouble(KEY_TIMER, 0.0D);
        clampToMax(data);
        data.removeTag(KEY_ORG_MAX);
        player.sendMessage(new TextComponentString(TextFormatting.GRAY
            + "【灵裕】已离开灵界：灵裕上限还原为 " + (int) org + "。"));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        NBTTagCompound data = player.getEntityData();
        if (inLingJie(player)) {
            applyLingJieRule(data, player);
        } else {
            restoreOriginRule(data, player);
        }
    }
}