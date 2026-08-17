package com.frxx.mengzhi.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 境界呼吸系统（修改原版空中值上限，原版默认 300 tick = 15 秒）：
 *  - 练气（境界 1）：初期 5 分钟（6000）/ 中期 10 分钟（12000）/ 后期与圆满 20 分钟（24000）；
 *  - 筑基（境界 2）：初期/中期/后期/圆满 统一 1 小时（72000）；
 *  - 结丹及以上（境界 >= 3）：彻底质变，不再消耗氧气（每 tick 回满）。
 *  - 未入道：原版 15 秒。
 * 实现（接管式）：由本处理器每 tick 绝对覆盖 air 值，不依赖原版时序——
 * 头未入水 → 记下补水时刻并把 air 顶到境界满值；
 * 头入水 → air = 满值 - 已入水 tick 数（精确倒计时），耗光后交给原版溺水判定。
 */
public class BreathingHandler {

    private static final String TAG_REFILL_AT = "FrxxAirRefillAt";
    private static final int AIR_VANILLA = 300;
    private static final int AIR_QI_LIAN_CHU = 6000;    // 练气初期 5 分钟
    private static final int AIR_QI_LIAN_ZHONG = 12000; // 练气中期 10 分钟
    private static final int AIR_QI_LIAN_LATE = 24000;  // 练气后期/圆满 20 分钟
    private static final int AIR_ZHU_JI = 72000;        // 筑基 1 小时

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        NBTTagCompound data = player.getEntityData();
        int sub = (int) Math.floor(data.getDouble("XiaoJingJieNum"));
        if (sub < 1 || sub > 4) {
            sub = (int) Math.floor(data.getDouble("LayerNum"));
        }
        if (sub < 1 || sub > 4) {
            sub = 1;
        }
        int jj = (int) Math.floor(data.getDouble("JingJieNum"));
        boolean inWater = player.isInsideOfMaterial(net.minecraft.block.material.Material.WATER);
        long now = player.world.getTotalWorldTime();
        if (jj >= 3) {
            // 结丹及以上：不消耗（水下每 tick 回满）
            if (inWater && player.getAir() < AIR_VANILLA) {
                player.setAir(AIR_VANILLA);
            }
            return;
        }
        if (jj < 1) {
            return;
        }
        int cap;
        if (jj >= 2) {
            cap = AIR_ZHU_JI;
        } else if (sub <= 1) {
            cap = AIR_QI_LIAN_CHU;
        } else if (sub == 2) {
            cap = AIR_QI_LIAN_ZHONG;
        } else {
            cap = AIR_QI_LIAN_LATE;
        }
        if (!inWater) {
            // 头未入水：刷新补水时刻，air 顶满到境界满值（覆盖原版 300）
            data.setLong(TAG_REFILL_AT, now);
            if (player.getAir() != cap) {
                player.setAir(cap);
            }
            return;
        }
        // 头入水：接管倒计时 air = 满值 - 水下已耗 tick；首次入水不扣
        long refill = data.getLong(TAG_REFILL_AT);
        if (refill <= 0) {
            data.setLong(TAG_REFILL_AT, now);
            refill = now;
        }
        long burned = now - refill;
        if (burned < 0) {
            burned = 0;
        }
        int remaining = cap - (int) Math.min(burned, cap + 40L);
        if (remaining < 0) {
            remaining = 0;
        }
        if (player.getAir() != remaining) {
            player.setAir(remaining);
        }
    }
}