package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.elixir.ElixirTickHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionApplicableEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 * 寄生体（Scape and Run: Parasites, com.dhanantry.scapeandrunparasites）专用
 * 护盾穿透防护。该模组的伤害三类旁路不触发 LivingAttackEvent/Hurt：
 *  1) PotionNeedler / EntityBiomass 直接 setHealth 直改血量；
 *  2) PotionNeedler 直接调用 onDeath 处决；
 *  3) 碰撞直接 addPotionEffect 贴污染/出血/刺针异常。
 * 本处理器按 SRP 类名前缀识别，仅在“护盾开启且护盾值 > 0”时生效：
 *  - 拒绝 SRP 药水施加（状态效果免疫到破盾）；
 *  - 拦截 SRP 关联的死亡处决（挡下后破盾保命）；
 *  - 玩家身带 SRP 药水或被 SRP 实体近身时，回滚无事件的直改掉血，差额按
 *    吸收率折算扣除护盾值（护盾击穿后自动放行）。
 */
public class SRPGuardHandler {

    public static final String SRP_PACKAGE = "com.dhanantry.scapeandrunparasites";
    private static final double NEARBY_RANGE = 3.0;
    private static final double ROLLBACK_THRESHOLD = 0.5;
    private static final long HURT_GRACE_TICKS = 2;

    private static boolean isSRPClass(Object o) {
        return o != null && o.getClass().getName().startsWith(SRP_PACKAGE);
    }

    /** 玩家当前是否带有 SRP 的药水效果（Bleed/Needler/Contamination 等）。 */
    private static boolean hasSRPEffect(EntityPlayer player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (isSRPClass(effect.getPotion())) {
                return true;
            }
        }
        return false;
    }

    /** 玩家身边 3 格内是否有 SRP 实体。 */
    private static boolean nearbySRP(EntityPlayer player) {
        List<Entity> list = player.world.getEntitiesWithinAABB(Entity.class,
            new AxisAlignedBB(
                player.posX - NEARBY_RANGE, player.posY - NEARBY_RANGE, player.posZ - NEARBY_RANGE,
                player.posX + NEARBY_RANGE, player.posY + NEARBY_RANGE + 3.0, player.posZ + NEARBY_RANGE));
        for (Entity e : list) {
            if (e != player && e.isEntityAlive() && isSRPClass(e)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shieldActive(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        return data.getBoolean("GuardOn") && data.getDouble("Guard") > 0;
    }

    /** 护盾开启时：拒绝 SRP 的负面药水直贴（出血/刺针/污染）。 */
    @SubscribeEvent
    public static void onPotionApplicable(PotionApplicableEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player.world.isRemote || !shieldActive(player)) {
            return;
        }
        if (isSRPClass(event.getPotionEffect().getPotion())) {
            // PotionApplicableEvent 不可 setCanceled（1.12.2 无 @Cancelable），
            // 用 Result.DENY 令 isPotionApplicable 返回 false，阻止施加
            event.setResult(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        }
    }

    /** 护盾开启时：拦截 SRP 关联的死亡处决（PotionNeedler 的 onDeath 直处决）。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player.world.isRemote || !shieldActive(player)) {
            return;
        }
        Entity source = event.getSource().getTrueSource();
        boolean srpRelated = isSRPClass(source) || hasSRPEffect(player) || nearbySRP(player);
        if (!srpRelated) {
            return;
        }
        event.setCanceled(true);
        keepAlive(player);
        // 处决豁免代价：护盾一次扣光（破盾保命）
        NBTTagCompound data = player.getEntityData();
        data.setDouble("Guard", 0.0);
        data.setBoolean("GuardOn", false);
        data.setBoolean("GuardBroken", true);
        data.setDouble("SrpHp", 0.0);
    }

    /** 死亡拦截后保命：至少留 1 血，防 0 血触发 onDeathUpdate 自动死亡。 */
    private static void keepAlive(EntityPlayer player) {
        player.setHealth(Math.max(1.0F, player.getHealth()));
        player.hurtResistantTime = 40;
    }

    /** 直改血量回滚兜底：仅当 SRP 药水在身 / SRP 实体近身时启用。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        NBTTagCompound data = player.getEntityData();
        boolean active = shieldActive(player)
            && (hasSRPEffect(player) || nearbySRP(player));
        float health = player.getHealth();
        double prev = data.getDouble("SrpHp");

        if (!active) {
            data.setDouble("SrpHp", 0.0);
            return;
        }
        // 护盾溢出规则下合法的 Hurt 扣血：跳过本次判定，刷新基准
        long hurtApplied = data.getLong("FrxxHurtApplied");
        if (hurtApplied > 0 && player.world.getTotalWorldTime() - hurtApplied <= HURT_GRACE_TICKS) {
            data.setLong("FrxxHurtApplied", 0L);
            data.setDouble("SrpHp", health);
            return;
        }
        if (prev > 0 && health < prev - ROLLBACK_THRESHOLD) {
            // 无事件的直改掉血：回滚血量，差额按吸收率折算扣护盾
            double delta = prev - health;
            player.setHealth((float) prev);
            double absorption = ElixirTickHandler.getEffectiveShieldAbsorption(player);
            int cost = (int) Math.ceil(delta / absorption);
            if (cost < 1) {
                cost = 1;
            }
            double guard = data.getDouble("Guard");
            double ng = Math.max(0.0, guard - cost);
            data.setDouble("Guard", ng);
            if (ng <= 0) {
                data.setBoolean("GuardOn", false);
                data.setBoolean("GuardBroken", true);
                data.setDouble("SrpHp", 0.0);
            }
            return;
        }
        data.setDouble("SrpHp", health);
    }
}