package com.frxx.mengzhi.lingjie;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 主世界高空灵界天威：
 *  - 仅主世界（dimension 0）生效；
 *  - 高度 10000 ~ 50000 格 之间，每秒承受一次伤害；
 *  - 高度超过 50000 格，直接破界进入灵界；
 *  - 伤害 = 玩家面板伤害（物攻 Attack + 法攻 MagicAttack）× 300%；
 *  - 面板键来自凡人修仙基础模组 player.getEntityData() 的 "Attack"（物攻）与 "MagicAttack"（法攻）。
 */
public class LingJieHeightDamageHandler {

    private static final double MIN_Y = 10000.0;
    private static final double MAX_Y = 50000.0;
    private static final double DAMAGE_MULTIPLIER = 3.0;

    private static final DamageSource LINGJIE_DAMAGE = new DamageSource("lingjie")
        .setDamageBypassesArmor()
        .setDamageIsAbsolute();

    private static final Map<UUID, Integer> TICK_COUNTERS = new HashMap<>();
    private static final Set<UUID> WARNED = new HashSet<>();
    private static final Set<UUID> ENTERED = new HashSet<>();

    private static void teleportToLingJie(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        UUID uuid = player.getUniqueID();
        if (!ENTERED.add(uuid)) {
            return;
        }
        try {
            EntityPlayerMP mp = (EntityPlayerMP) player;
            int id = LingJieDimension.DIMENSION_ID;
            if (!LingJieTeleport.to(mp, id)) {
                ENTERED.remove(uuid);
                if (FanRenXiuXianMengZhi.logger != null) {
                    FanRenXiuXianMengZhi.logger.warn("玩家 {} 在 y={} 破界进入灵界失败", player.getName(), player.posY);
                }
                return;
            }
            if (FanRenXiuXianMengZhi.logger != null) {
                FanRenXiuXianMengZhi.logger.info("玩家 {} 在 y={} 破界进入灵界 id={}", player.getName(), player.posY, id);
            }
            player.sendMessage(new TextComponentString(TextFormatting.AQUA
                + "【灵界】你已冲破人界高空壁障（5 万格），进入灵界（维度 " + id + "）！"));
        } catch (Exception e) {
            ENTERED.remove(uuid);
            if (FanRenXiuXianMengZhi.logger != null) {
                FanRenXiuXianMengZhi.logger.error("玩家 {} 进入灵界失败", player.getName(), e);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        UUID uuid = player.getUniqueID();
        double y = player.posY;
        if (player.dimension != 0) {
            WARNED.remove(uuid);
            TICK_COUNTERS.remove(uuid);
            ENTERED.remove(uuid);
            return;
        }
        // 高度达到或超过 5 万格：进入灵界
        if (y >= MAX_Y) {
            WARNED.remove(uuid);
            TICK_COUNTERS.remove(uuid);
            teleportToLingJie(player);
            return;
        }
        if (y < MIN_Y) {
            WARNED.remove(uuid);
            TICK_COUNTERS.remove(uuid);
            return;
        }
        int counter = TICK_COUNTERS.getOrDefault(uuid, 0) + 1;
        if (counter >= 20) {
            counter = 0;
            if (WARNED.add(uuid)) {
                player.sendMessage(new TextComponentString(TextFormatting.YELLOW
                    + "【灵界天威】你已进入主世界高空灵界领域（1万~5万格），每秒承受面板伤害×3的侵蚀！"));
            }
            NBTTagCompound data = player.getEntityData();
            double wu = data.hasKey("Attack") ? data.getDouble("Attack") : 0.0;
            double fa = data.hasKey("MagicAttack") ? data.getDouble("MagicAttack") : 0.0;
            float damage = (float) ((wu + fa) * DAMAGE_MULTIPLIER);
            // 手持穿梭器：免疫 80% 灵界天威伤害
            if (ItemChuanSuoQi.isHolding(player)) {
                damage *= ItemChuanSuoQi.DAMAGE_TAKEN;
            }
            if (damage > 0.0F) {
                player.attackEntityFrom(LINGJIE_DAMAGE, damage);
            }
        }
        TICK_COUNTERS.put(uuid, counter);
    }
}
