package com.frxx.mengzhi.elixir;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public final class ElixirConsumeRules {

    // 各境界同境界丹药可服用次数: 练气6 / 筑基8 / 结丹12 / 元婴18 / 化神24
    private static final int[] LIMIT_BY_REALM = { 6, 8, 12, 18, 24 };

    // 高境界吃低境界丹药时配额倍率
    private static final int LOWER_REALM_MULTIPLIER = 2;

    private ElixirConsumeRules() {}

    public static int getJingJie(EntityPlayer player) {
        double jj = player.getEntityData().getDouble("JingJieNum");
        return Math.max(1, Math.min(5, (int) Math.floor(jj)));
    }

    /** 当前玩家食用某境界丹药的可服用配额（同境界=该境界次数；低境界=低境界次数×2） */
    public static int getQuota(EntityPlayer player, ElixirRealm realm) {
        int base = LIMIT_BY_REALM[realm.getLevel() - 1];
        if (realm.getLevel() < getJingJie(player)) {
            return base * LOWER_REALM_MULTIPLIER;
        }
        return base;
    }

    public static int getConsumedCount(EntityPlayer player, ElixirRealm realm) {
        return player.getEntityData().getInteger(getCountKey(realm));
    }

    private static String getCountKey(ElixirRealm realm) {
        return "ElixirConsumeCount_" + realm.getShortName();
    }

    /**
     * 修为境界检查：未修仙或修为低于丹药境界时拒绝。
     * 通过返回 null，否则返回原因字符串并已向玩家发送提示。
     */
    public static String checkRealm(EntityPlayer player, ElixirRealm realm) {
        NBTTagCompound data = player.getEntityData();
        double rawJingJie = data.getDouble("JingJieNum");
        int jingJie = (int) Math.floor(rawJingJie);
        if (jingJie < 1) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u672a\u4fee\u4ed9\u4e0d\u53ef\u670d\u7528\u4e39\u836f"));
            return "NO_REALM";
        }
        if (jingJie < realm.getLevel()) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u4fee\u4e3a\u4e0d\u8db3\uff01\u5fc5\u987b\u4fee\u4e3a\u81f3\u300c"
                + realm.getFullName() + "\u300d\u624d\u80fd\u670d\u7528\u8be5\u4e39\u836f"));
            return "REALM_TOO_LOW";
        }
        return null;
    }

    /**
     * 次数配额检查（仅限永久丹药）：当前境界该丹药境界的已用次数达到配额时拒绝。
     * 通过返回 null，否则返回原因字符串并已向玩家发送提示。
     */
    public static String checkConsumeQuota(EntityPlayer player, ElixirRealm realm) {
        NBTTagCompound data = player.getEntityData();
        int quota = getQuota(player, realm);
        if (data.getInteger(getCountKey(realm)) >= quota) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u8be5\u4e39\u836f\u672c\u5883\u754c\u53ef\u670d\u7528\u6b21\u6570\u5df2\u8fbe\u4e0a\u9650 ("
                + quota + " \u6b21)\uff0c\u5f53\u524d\u5883\u754c\u4e0d\u53ef\u518d\u670d\u7528"));
            return "LIMIT_REACHED";
        }
        return null;
    }

    public static void markConsumed(EntityPlayer player, ElixirRealm realm) {
        NBTTagCompound data = player.getEntityData();
        data.setInteger(getCountKey(realm), data.getInteger(getCountKey(realm)) + 1);
    }
}