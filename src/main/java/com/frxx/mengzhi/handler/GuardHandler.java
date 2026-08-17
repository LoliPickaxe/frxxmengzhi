package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.elixir.ElixirTickHandler;
import com.frxx.mengzhi.network.GuardActionPacket;
import com.frxx.mengzhi.network.GuardStatePacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GuardHandler {

    public static final int COMBAT_TICKS = 100;
    // 高频攻击拦截间隔（tick）：两次攻击间隔小于该值时不扣除护盾值
    public static final int ATTACK_INTERVAL_TICKS = 4;

    private static final double REGEN_COMBAT = 0.0002;
    private static final double REGEN_COMBAT_MIN = 0.005;
    private static final double REGEN_OUT_COMBAT = 0.002;
    private static final double REGEN_OUT_COMBAT_MIN = 0.05;
    // 回复每 tick 上限（500/tick = 10000/秒），避免大上限时攻击消耗被瞬间回满掩盖
    private static final double REGEN_CAP_PER_TICK = 500.0;
    // 每 tick 同步，移除间隔限制

    private static final Map<UUID, Integer> syncTicks = new HashMap<UUID, Integer>();

    // 溢出伤害重放防递归/防间隔误拦截：处理中的玩家 UUID（服务端单线程安全）
    private static final Set<UUID> overflowProcessing = new HashSet<UUID>();

    public static boolean hasRealm(NBTTagCompound data) {
        return data.getDouble("JingJieNum") >= 1.0;
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player.world.isRemote) {
            return;
        }
        NBTTagCompound data = player.getEntityData();
        if (!data.getBoolean("GuardOn")) {
            return;
        }
        double guard = data.getDouble("Guard");
        if (guard <= 0) {
            return;
        }

        // 溢出伤害重放（方案A）：本类重放的 attackEntityFrom 直接放行，走原版伤害流程
        if (!overflowProcessing.add(player.getUniqueID())) {
            return;
        }
        try {
            float amount = event.getAmount();

            // 高频攻击拦截：两次攻击间隔小于 4 tick 时拦截攻击与伤害，但不扣除护盾值
            long nowTick = player.world.getTotalWorldTime();
            long lastTick = data.getLong("GuardLastAttackTick");
            if (lastTick > 0 && (nowTick - lastTick) < ATTACK_INTERVAL_TICKS) {
                event.setCanceled(true);
                playShieldHit(player);
                sync((EntityPlayerMP) player);
                return;
            }
            data.setLong("GuardLastAttackTick", nowTick);

            // 计算护盾吸收：damage / absorption = shield cost
            double absorption = ElixirTickHandler.getEffectiveShieldAbsorption(player);
            double shieldCost = amount / absorption;
            int shieldCostInt = (int) Math.ceil(shieldCost);
            if (shieldCostInt <= 0) {
                shieldCostInt = 1; // 至少消耗 1 点
            }

            // 方案A：被护盾接下的攻击一律在 LivingAttack 层整体取消。
            // 1.12.2 原版在 damageEntity（内含 onLivingHurt）之后无条件设置
            // hurtTime/受击动画/受击音效/0.4 击退——仅取消 LivingHurt 拦不住
            // 这些表现，必须先于原版动画流程拦截。
            event.setCanceled(true);
            double absorbCapacity = guard * absorption;
            double newGuard = Math.max(0.0, guard - shieldCostInt);
            data.setDouble("Guard", newGuard);
            data.setDouble("CombatTimer", COMBAT_TICKS);
            if (newGuard <= 0) {
                data.setBoolean("GuardOn", false);
                data.setBoolean("GuardBroken", true);
                playShieldBreak(player);
            } else {
                playShieldHit(player);
            }

            // 溢出伤害规则（默认开启）：护盾吸收上限之外的部分按真实伤害重放，
            // 防止 /kill 这类巨量伤害被护盾凭空吞掉；重放的攻击走完整原版流程，
            // 受击反馈只对真实溢出部分生效。FrxxHurtApplied 告知 SRPGuardHandler
            // 该次扣血为合法攻击，跳过无事件掉血回滚。
            if (ShieldOverflowData.get(player.world).getRule() == ShieldOverflowData.RULE_OVERFLOW_APPLIES) {
                float overflow = (float) Math.max(0.0, amount - absorbCapacity);
                if (overflow > 0.0f) {
                    data.setLong("FrxxHurtApplied", nowTick);
                    player.attackEntityFrom(event.getSource(), overflow);
                }
            }
            sync((EntityPlayerMP) player);
        } finally {
            overflowProcessing.remove(player.getUniqueID());
        }
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
        double guardMax = computeEffectiveGuardMax(data);
        data.setDouble("GuardMax", guardMax);

        if (guardMax > 0) {
            double guard = data.getDouble("Guard");
            double combatTimer = data.getDouble("CombatTimer");
            if (combatTimer > 0) {
                data.setDouble("CombatTimer", combatTimer - 1);
            }
            double regen;
            if (combatTimer > 0) {
                // 战斗期间仅极小恒定回复（0.1/秒），保证攻击消耗可见
                regen = REGEN_COMBAT_MIN;
            } else {
                // 脱战：比例回复与小助手设定回复取较大者
                double proportional = Math.max(guardMax * REGEN_OUT_COMBAT, REGEN_OUT_COMBAT_MIN);
                regen = proportional;
                if (data.getDouble("GuardRegenOverride") > 0) {
                    regen = Math.max(proportional, data.getDouble("GuardRegenOverride") / 20.0);
                }
            }
            // 回复上限钳制：防止大上限瞬间回满导致攻击消耗不可见
            regen = Math.min(regen, REGEN_CAP_PER_TICK);
            data.setDouble("Guard", Math.min(guardMax, guard + regen));

            // 仅当护盾被打碎（GuardBroken）时回满自动重新开启；手动关闭不自动打开
            if (!data.getBoolean("GuardOn") && data.getBoolean("GuardBroken")
                && data.getDouble("Guard") >= guardMax - 0.01) {
                data.setBoolean("GuardOn", true);
                data.setBoolean("GuardBroken", false);
            }
        }

        // 每 tick 同步，移除间隔限制
        sync((EntityPlayerMP) player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player.world.isRemote) {
            return;
        }
        syncTicks.remove(event.player.getUniqueID());
        sync((EntityPlayerMP) event.player);
    }

    public static void handleAction(EntityPlayerMP player, GuardActionPacket.Action action) {
        NBTTagCompound data = player.getEntityData();
        if (!hasRealm(data)) {
            data.setBoolean("GuardOn", false);
            player.sendMessage(new TextComponentString("\u60a8\u672a\u4fee\u4ed9\u4e0d\u53ef\u7528"));
            sync(player);
            return;
        }
        switch (action) {
            case TOGGLE:
                data.setBoolean("GuardOn", !data.getBoolean("GuardOn"));
                // 手动切换后清除"被打碎"标记，避免自动重开
                data.setBoolean("GuardBroken", false);
                break;
            case CHARGE:
                double guardMax = computeGuardMax(data);
                if (guardMax > 0) {
                    double guard = data.getDouble("Guard");
                    if (guard < guardMax) {
                        double power = data.getDouble("Power");
                        double need = (guardMax - guard) / 0.5;
                        double used = Math.min(power, need);
                        if (used > 0) {
                            data.setDouble("Power", power - used);
                            data.setDouble("Guard", guard + used * 0.5);
                        }
                    }
                }
                break;
            default:
                break;
        }
        sync(player);
    }

    private static void sync(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        FanRenXiuXianMengZhi.NETWORK.sendTo(
            new GuardStatePacket(
                data.getBoolean("GuardOn"),
                data.getDouble("Guard"),
                data.getDouble("GuardMax")),
            player);
    }

    private static void playShieldHit(EntityPlayer player) {
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.9F, 1.0F);
    }

    private static void playShieldBreak(EntityPlayer player) {
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0F, 0.9F);
    }

    public static double computeGuardMax(NBTTagCompound data) {
        // 小助手覆盖值优先：设置 >0 时使用该值，取消可用 0
        double override = data.getDouble("GuardMaxOverride");
        if (override > 0) {
            return override;
        }
        double jingJie = data.getDouble("JingJieNum");
        double layer = data.getDouble("LayerNum");
        if (jingJie < 1.0) {
            return 0.0;
        }
        return 20.0 * Math.pow(3.0, jingJie - 1.0) * (1.0 + (layer - 1.0) * 0.5);
    }

    /** 基础上限 + 永久丹药加成 + 临时丹药加成（唯一计算入口，消除双写） */
    public static double computeEffectiveGuardMax(NBTTagCompound data) {
        double base = computeGuardMax(data);
        double bonus = data.getDouble("GuardMaxBonus");
        double temp = 0;
        for (String key : data.getKeySet()) {
            if (key.startsWith("TempElixir_")) {
                net.minecraft.nbt.NBTTagCompound buff = data.getCompoundTag(key);
                if (buff.hasKey("Type") && buff.getInteger("Type") == 0) { // SHIELD_MAX
                    temp += buff.getInteger("Value");
                }
            }
        }
        return base + bonus + temp;
    }
}
