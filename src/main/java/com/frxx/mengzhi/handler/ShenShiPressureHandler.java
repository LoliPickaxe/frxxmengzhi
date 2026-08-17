package com.frxx.mengzhi.handler;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class ShenShiPressureHandler {

    public static final String CD_TAG = "FrxxShenShiPressureCd";
    public static final int COOLDOWN_TICKS = 100;

    public static final int RANGE_SIGHT = 32;
    public static final int RADIUS_SURROUND = 3;
    /** 天道威压：环绕范围扩展到 100×100×100，视线距离也扩展到 100 */
    public static final int RADIUS_TIANDAO = 100;
    public static final int RANGE_SIGHT_TIANDAO = 100;

    private static final DamageSource PRESSURE_KILL = new DamageSource("shenshiPressure")
        .setDamageBypassesArmor().setDamageIsAbsolute();

    private static final int DUR_2X = 400;
    private static final int DUR_3X = 800;
    private static final int DUR_4X = 2400;

    private static final double SCARE_CHANCE = 0.15;
    /** 天道威压：被压制者大概率被无边神念抹尽 */
    private static final double TIANDO_SCARE_CHANCE = 0.6;

    public static void cast(EntityPlayerMP player, boolean sneak) {
        NBTTagCompound data = player.getEntityData();
        boolean tiandao = TiandaoHandler.isTiandao(player);
        // 天道境：无边神念，零冷却
        if (!tiandao && data.getInteger(CD_TAG) > 0) {
            int remain = data.getInteger(CD_TAG);
            player.sendMessage(new TextComponentString(TextFormatting.GRAY
                + "\u795e\u8bc6\u5a01\u538b\u51b7\u5374\u4e2d\uff0c\u5269\u4f59 " + ((remain + 19) / 20) + " \u79d2"));
            return;
        }
        WorldServer world = player.getServerWorld();
        List<EntityLivingBase> targets = sneak ? nearby(world, player, tiandao) : sightTargets(world, player, tiandao);
        if (targets.isEmpty()) {
            player.sendMessage(new TextComponentString(TextFormatting.GRAY
                + "\u795e\u8bc6\u5a01\u538b\u672a\u627e\u5230\u53ef\u4f5c\u7528\u7684\u76ee\u6807"));
            return;
        }

        int affected = 0;
        boolean noCultivation = false;
        for (EntityLivingBase target : targets) {
            if (target == player || !target.isEntityAlive()) continue;
            if (target instanceof EntityPlayer && TiandaoHandler.isTiandao((EntityPlayer) target)) continue;
            if (tiandao) {
                // 天道神念无边无尽：无视血量差距与对方神识，直接最高档压制，大概率神念抹杀
                applyTier(target, 4, DUR_4X);
                if (world.rand.nextDouble() < TIANDO_SCARE_CHANCE) {
                    scareToDeath(world, player, target);
                }
                affected++;
                continue;
            }
            if (target instanceof EntityPlayer && player.getEntityData().getDouble("ShenShi") <= 0.0) {
                noCultivation = true;
                continue;
            }
            double ratio = ratioOf(player, target);
            if (ratio < 0.0) continue;

            int tier;
            if (ratio >= 4.0) {
                tier = 4;
            } else if (ratio >= 3.0) {
                tier = 3;
            } else if (ratio >= 2.0) {
                tier = 2;
            } else {
                // 目标更强：反噬
                applyBacklash(player, 1.0 / ratio);
                affected++;
                continue;
            }

            int duration = tier == 4 ? DUR_4X : (tier == 3 ? DUR_3X : DUR_2X);
            applyTier(target, tier, duration);
            if (tier == 4 && world.rand.nextDouble() < SCARE_CHANCE) {
                scareToDeath(world, player, target);
            }
            affected++;
        }

        if (affected <= 0) {
            if (noCultivation) {
                player.sendMessage(new TextComponentString(TextFormatting.GRAY
                    + "\u60a8\u7684\u795e\u8bc6\u5c1a\u4e14\u5f31\u5c0f\uff0c\u65e0\u6cd5\u65bd\u5c55\u5a01\u538b"));
            } else {
                player.sendMessage(new TextComponentString(TextFormatting.GRAY
                    + "\u795e\u8bc6\u5a01\u538b\u672a\u627e\u5230\u53ef\u4f5c\u7528\u7684\u76ee\u6807"));
            }
            return;
        }
        data.setInteger(CD_TAG, tiandao ? 0 : COOLDOWN_TICKS);
        player.sendMessage(new TextComponentString(TextFormatting.GOLD
            + "\u795e\u8bc6\u5a01\u538b\u5df2\u91ca\u653e\uff0c\u6ce2\u53ca " + affected + " \u4e2a\u76ee\u6807"));
    }

    /** PVE 用最大生命，PVP 用双方神识属性 */
    private static double ratioOf(EntityLivingBase player, EntityLivingBase target) {
        if (target instanceof EntityPlayer) {
            double mine = player.getEntityData().getDouble("ShenShi");
            if (mine <= 0.0) return -1.0;
            double theirs = target.getEntityData().getDouble("ShenShi");
            return mine / Math.max(theirs, 0.001);
        }
        double theirs = target.getMaxHealth();
        if (theirs <= 0.0) return -1.0;
        return player.getMaxHealth() / theirs;
    }

    private static void applyTier(EntityLivingBase victim, int tier, int duration) {
        int slow, blind, nausea, poison;
        switch (tier) {
            case 4:
                slow = 9; blind = 9; nausea = 9; poison = 9;
                break;
            case 3:
                slow = 4; blind = 3; nausea = 2; poison = 2;
                break;
            default:
                slow = 2; blind = -1; nausea = 1; poison = 1;
                break;
        }
        victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, duration, slow));
        if (blind >= 0) {
            victim.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, duration, blind));
        }
        victim.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, duration, nausea));
        victim.addPotionEffect(new PotionEffect(MobEffects.POISON, duration, poison));
    }

    /** 反噬：对应等级效果，时长与差距成正比（20~120秒） */
    private static void applyBacklash(EntityPlayerMP player, double reverse) {
        int tier;
        if (reverse >= 4.0) {
            tier = 4;
        } else if (reverse >= 3.0) {
            tier = 3;
        } else {
            tier = 2;
        }
        int seconds = (int) Math.round(20.0 + (Math.min(reverse, 4.0) - 2.0) * 50.0);
        seconds = Math.max(20, Math.min(120, seconds));
        applyTier(player, tier, seconds * 20);
        player.sendMessage(new TextComponentString(TextFormatting.RED
            + "\u60a8\u7684\u795e\u8bc6\u4e0d\u5982\u5bf9\u65b9\uff0c\u906d\u5230\u53cd\u566c\uff01"));
    }

    private static void scareToDeath(WorldServer world, EntityPlayerMP player, EntityLivingBase target) {
        if (!target.attackEntityFrom(PRESSURE_KILL, Float.MAX_VALUE)) {
            target.setHealth(0.0F);
            target.setDead();
        }
        world.getMinecraftServer().getPlayerList().sendMessage(new TextComponentString(
            TextFormatting.DARK_RED + "[\u795e\u8bc6\u5a01\u538b] " + target.getName() + " \u88ab "
                + player.getName() + " \u7684\u6050\u6016\u795e\u8bc6\u5413\u6b7b\u4e86\uff01"));
    }

    private static List<EntityLivingBase> nearby(WorldServer world, EntityPlayerMP player, boolean tiandao) {
        int r = tiandao ? RADIUS_TIANDAO : RADIUS_SURROUND;
        return world.getEntitiesWithinAABB(EntityLivingBase.class,
            new AxisAlignedBB(player.posX - r, player.posY - r, player.posZ - r,
                player.posX + r + 1.0, player.posY + r + 1.0, player.posZ + r + 1.0));
    }

    /** 视角锥（90°）内未被方块遮挡、距离 32 格内的目标（天道 100 格） */
    private static List<EntityLivingBase> sightTargets(WorldServer world, EntityPlayerMP player, boolean tiandao) {
        int range = tiandao ? RANGE_SIGHT_TIANDAO : RANGE_SIGHT;
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        AxisAlignedBB bb = new AxisAlignedBB(eye.x - range, eye.y - range, eye.z - range,
            eye.x + range, eye.y + range, eye.z + range);
        double cosHalf = Math.cos(Math.toRadians(45.0));
        List<EntityLivingBase> result = new ArrayList<EntityLivingBase>();
        for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class, bb)) {
            if (target == player) continue;
            Vec3d aim = new Vec3d(target.posX, target.posY + target.getEyeHeight() * 0.5, target.posZ).subtract(eye);
            double dist = aim.lengthVector();
            if (dist > range) continue;
            if (aim.dotProduct(look) / dist < cosHalf) continue;
            RayTraceResult hit = world.rayTraceBlocks(eye,
                new Vec3d(target.posX, target.posY + target.getEyeHeight() * 0.5, target.posZ));
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK
                && eye.distanceTo(hit.hitVec) + 0.5 < dist) {
                continue;
            }
            result.add(target);
        }
        return result;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        NBTTagCompound data = event.player.getEntityData();
        int cd = data.getInteger(CD_TAG);
        if (cd > 0) {
            data.setInteger(CD_TAG, cd - 1);
        }
    }
}