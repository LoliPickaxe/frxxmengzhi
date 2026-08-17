package com.frxx.mengzhi.handler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 投掷物面板伤害开关（默认关闭）：
 * 开启后，所有由玩家发射/投掷的原版投射物（箭矢、雪球、末影珍珠等）
 * 命中伤害 = 玩家面板（物理100% + 法术50%）x 70%，
 * 不再受原版投射物伤害上限（附魔、蓄力）限制。
 */
public class ProjectilePanelHandler {

    public static final String TAG_PANEL_DMG = "FrxxProjectilePanelDmg";
    private static final double PANEL_RATIO = 0.7;

    /** 切换投掷物面板伤害开关（按键 3 触发），状态保存在玩家 NBT，默认关闭 */
    public static void togglePanelDamage(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        boolean on = !data.getBoolean(TAG_PANEL_DMG);
        data.setBoolean(TAG_PANEL_DMG, on);
        if (on) {
            player.sendMessage(new TextComponentString(TextFormatting.GOLD
                + "\u6295\u629b\u7269\u9762\u677f\u4f24\u5bb3\u5df2\u5f00\u542f\uff1a\u547d\u4e2d = \uff08\u7269\u7406100% + \u6cd5\u672f50%\uff09x 70%"));
        } else {
            player.sendMessage(new TextComponentString(TextFormatting.GRAY
                + "\u6295\u629b\u7269\u9762\u677f\u4f24\u5bb3\u5df2\u5173\u95ed\uff08\u56de\u590d\u539f\u7248\u4f24\u5bb3\uff09"));
        }
    }

    /** 拦截原版投射物命中：来源为投射物且发射者是玩家时，替换伤害为面板公式 */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (!source.isProjectile()) {
            return;
        }
        if (!(source.getTrueSource() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) source.getTrueSource();
        if (!player.getEntityData().getBoolean(TAG_PANEL_DMG)) {
            return;
        }
        NBTTagCompound data = player.getEntityData();
        double dmg = (data.getDouble("Attack") + data.getDouble("MagicAttack") * 0.5) * PANEL_RATIO;
        event.setAmount((float) dmg);
    }
}
