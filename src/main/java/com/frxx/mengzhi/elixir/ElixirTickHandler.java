package com.frxx.mengzhi.elixir;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

public class ElixirTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        NBTTagCompound data = player.getEntityData();

        processTemporaryBuffs(player, data);
        syncGuardMaxIfNeeded(player, data);
    }

    private static void processTemporaryBuffs(EntityPlayer player, NBTTagCompound data) {
        Set<String> keys = data.getKeySet();
        for (String key : keys) {
            if (key.startsWith("TempElixir_")) {
                NBTTagCompound buff = data.getCompoundTag(key);
                if (!buff.hasKey("Duration")) continue;

                int duration = buff.getInteger("Duration");
                duration -= 1;
                
                if (duration <= 0) {
                    data.removeTag(key);
                    if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                        FanRenXiuXianMengZhi.NETWORK.sendTo(
                            new com.frxx.mengzhi.network.GuardStatePacket(
                                data.getBoolean("GuardOn"),
                                data.getDouble("Guard"),
                                data.getDouble("GuardMax")
                            ),
                            (net.minecraft.entity.player.EntityPlayerMP) player
                        );
                    }
                } else {
                    buff.setInteger("Duration", duration);
                }
            }
        }
    }

    private static void syncGuardMaxIfNeeded(EntityPlayer player, NBTTagCompound data) {
        // GuardMax 已由 GuardHandler 每 tick 统一计算（含永久/临时丹药加成），这里仅负责同步
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            FanRenXiuXianMengZhi.NETWORK.sendTo(
                new com.frxx.mengzhi.network.GuardStatePacket(
                    data.getBoolean("GuardOn"),
                    data.getDouble("Guard"),
                    data.getDouble("GuardMax")
                ),
                (net.minecraft.entity.player.EntityPlayerMP) player
            );
        }
    }

    // Effective shield regen (base + permanent + temporary)
    public static double getEffectiveShieldRegen(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        double baseRegen = data.getDouble("GuardRegen");
        double bonusRegen = data.getDouble("GuardRegenBonus");
        
        double tempRegen = 0;
        NBTTagCompound nbt = player.getEntityData();
        Set<String> keys = nbt.getKeySet();
        for (String key : keys) {
            if (key.startsWith("TempElixir_")) {
                NBTTagCompound buff = nbt.getCompoundTag(key);
                if (buff.hasKey("Type") && buff.getInteger("Type") == 1) { // SHIELD_REGEN
                    tempRegen += buff.getInteger("Value");
                }
            }
        }
        
        return baseRegen + bonusRegen + tempRegen;
    }

    // Effective shield absorption ratio (how many damage points 1 shield absorbs)
    // Returns multiplier (e.g., 2.0 = 1 shield absorbs 2 damage)
    public static double getEffectiveShieldAbsorption(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        double baseAbsorption = data.getDouble("GuardAbsorption");
        double bonusAbsorption = data.getDouble("GuardAbsorptionBonus");
        
        double tempAbsorption = 0;
        NBTTagCompound nbt = player.getEntityData();
        Set<String> keys = nbt.getKeySet();
        for (String key : keys) {
            if (key.startsWith("TempElixir_")) {
                NBTTagCompound buff = nbt.getCompoundTag(key);
                if (buff.hasKey("Type") && buff.getInteger("Type") == 2) { // SHIELD_ABSORPTION
                    tempAbsorption += buff.getInteger("Value") / 100.0; // Stored as *100
                }
            }
        }
        
        double total = baseAbsorption + bonusAbsorption + tempAbsorption;
        return Math.max(1.0, total); // Minimum 1.0 (1 shield = 1 damage)
    }
}