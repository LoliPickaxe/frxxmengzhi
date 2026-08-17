package com.frxx.mengzhi.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class KnockbackHandler {

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        Entity target = event.getEntity();
        Entity attacker = event.getAttacker();

        if (!target.getEntityData().hasKey("JingJieNum")) return;

        double targetMaxHp = target.getEntityData().getDouble("HealthMax");
        if (targetMaxHp <= 0) targetMaxHp = ((net.minecraft.entity.EntityLivingBase) target).getMaxHealth();
        double damage = event.getOriginalStrength();

        if (attacker instanceof EntityPlayer) {
            double attackerJingJie = attacker.getEntityData().getDouble("JingJieNum");
            double targetJingJie = target.getEntityData().getDouble("JingJieNum");
            double gap = attackerJingJie - targetJingJie;
            if (damage <= targetMaxHp * 0.1) {
                event.setStrength((float) (damage * (1.0 - Math.min(Math.max(gap, -1.0), 1.0) * 0.5)));
            }
        } else {
            if (damage <= targetMaxHp * 0.15) {
                event.setStrength((float) (damage * 0.5));
            }
        }
    }
}
