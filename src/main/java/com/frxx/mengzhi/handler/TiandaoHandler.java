package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.network.TiandaoPanelPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;

/**
 * 第十二境界 · 天道（境界 >= 12 时生效）：
 *  1. 生命锁定为 100 且始终满格，任何外部修改/伤害均无法覆写；
 *  2. 全面拦截攻击伤害事件与死亡事件（最高优先，先于 base 模组处理），绝对不死不灭；
 *  3. 自带被动：背包全部槽位（主手/副手/盔甲/物品栏/快捷栏）带耐久物品自动修满；
 *  4. 天道面板（小键盘 . ）：切换创造/生存/冒险、制造真实天劫（参照凡人修仙传：
 *     云层轰鸣 + 多道天雷随机落地区域打击，非单体）、修改任意生物/玩家的属性
 *     （生命/攻击/速度/境界等，写入 base 模组同款 NBT 键并持久化防覆写）。
 */
public class TiandaoHandler {

    public static final double TIANDAO_REALM = 12.0;
    public static final double LOCKED_MAX_HEALTH = 100.0;
    /** 天道灵力：永远一千万（当前与上限同值，满条） */
    public static final double LING_LI_VALUE = 10_000_000.0;
    /** 天道真元：上限一亿且永远满（base 真元=Base，上限=NiangJiu30） */
    public static final double ZHEN_YUAN_MAX = 100_000_000.0;

    private static final int CAST_COOLDOWN_TICKS = 120;
    private static final String TAG_CAST_CD = "FrxxTiandaoCastCd";
    /** 天道之力开关（攻击/右键/潜行右键范围抹除） */
    private static final String TAG_FORCE_ON = "FrxxTiandaoForce";
    /** 天道自动攻击开关（每tick 9×9×9 范围清场） */
    private static final String TAG_AUTO_ON = "FrxxTiandaoAuto";
    private static final DamageSource TIANDAO_SOURCE =
        new DamageSource("frxxTiandao").setDamageBypassesArmor().setDamageIsAbsolute();
    private static final String TAG_ATTR_PREFIX = "FrxxAttr";

    /** base 模组 1~11 重大境界（凡人修仙传：炼气 筑基 结丹 元婴 化神 炼虚 合体 大乘 真仙 金仙 道祖） */
    private static final String[] REALM_NAMES = {
        "炼气", "筑基", "结丹", "元婴", "化神",
        "炼虚", "合体", "大乘", "真仙", "金仙", "道祖"
    };
    private static final String[] SUB_STAGE_NAMES = {"初期", "中期", "后期", "圆满"};

    private static final Random RNG = new Random();

    /** 天道判定：境界编号 >=12，或 base 面板显示串为"天道"（防止编号被 base 服务端钳制后失效） */
    public static boolean isTiandao(EntityPlayer player) {
        NBTTagCompound nbt = player.getEntityData();
        return nbt.getDouble("JingJieNum") >= TIANDAO_REALM
            || "天道".equals(nbt.getString("JingJie"));
    }

    private static void send(EntityPlayer player, String msg) {
        player.sendMessage(new TextComponentString(msg));
    }

    /** 天道面板动作入口（服务端） */
    public static void handle(EntityPlayerMP player, TiandaoPanelPacket packet) {
        // 境界字段（8）与 NBT 写 JingJieNum 不受天道门禁限制，允许未达 12 的玩家把自己提升到天道
        boolean unlockRoute = (packet.action == TiandaoPanelPacket.ACTION_APPLY_ATTR && packet.attrIndex == 8)
            || (packet.action == TiandaoPanelPacket.ACTION_SET_NBT && "JingJieNum".equals(packet.key));
        if (!isTiandao(player) && !unlockRoute) {
            send(player, TextFormatting.RED + "【天道】尚未达到天道境界（第12重），无法使用天道权能");
            return;
        }
        switch (packet.action) {
            case TiandaoPanelPacket.ACTION_SWITCH_MODE:
                switchGameMode(player);
                break;
            case TiandaoPanelPacket.ACTION_CAST_TRIBULATION:
                castTribulation(player, packet.targetId);
                break;
            case TiandaoPanelPacket.ACTION_APPLY_ATTR:
                applyAttribute(player, packet.targetId, packet.attrIndex, packet.value);
                break;
            case TiandaoPanelPacket.ACTION_TOGGLE_FORCE:
                toggleFlag(player, TAG_FORCE_ON, "天道之力",
                    "攻击/右键抹除目标，潜行右键抹除周围100×100×100范围");
                break;
            case TiandaoPanelPacket.ACTION_TOGGLE_AUTO:
                toggleFlag(player, TAG_AUTO_ON, "天道自动攻击", "每刻自动清除周围9×9×9范围所有生物");
                break;
            case TiandaoPanelPacket.ACTION_CLEAR_EFFECTS:
                player.clearActivePotions();
                send(player, TextFormatting.GREEN + "【天道】已净化自身所有状态效果（含正面）");
                break;
            case TiandaoPanelPacket.ACTION_SET_NBT:
                applyNbt(player, packet.targetId, packet.key, packet.strValue);
                break;
            case TiandaoPanelPacket.ACTION_FETCH_NBT_DOC:
                fetchNbtDoc(player, packet.targetId);
                break;
            case TiandaoPanelPacket.ACTION_SAVE_NBT_DOC:
                saveNbtDoc(player, packet.targetId, packet.strValue);
                break;
            default:
                break;
        }
    }

    private static boolean isFlagOn(EntityPlayer player, String tag) {
        return player.getEntityData().getInteger(tag) == 1;
    }

    private static void toggleFlag(EntityPlayerMP player, String tag, String name, String desc) {
        boolean now = player.getEntityData().getInteger(tag) != 1;
        player.getEntityData().setInteger(tag, now ? 1 : 0);
        send(player, TextFormatting.GREEN + "【天道】" + name + (now ? " 已激活：" : " 已关闭。")
            + (now ? desc : ""));
    }

    /** 读取目标全部 NBT（ForgeData），生成"键 = 值"文本文档发给客户端 */
    public static void fetchNbtDoc(EntityPlayerMP player, int targetId) {
        Entity target = targetId == -1 ? player : player.getServerWorld().getEntityByID(targetId);
        if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()) {
            send(player, TextFormatting.RED + "【天道】目标不存在或已消失");
            return;
        }
        NBTTagCompound forge = target.getEntityData();
        java.util.List<String> keys = new java.util.ArrayList<String>(forge.getKeySet());
        java.util.Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(key).append(" = ").append(nbtToText(forge.getTag(key)));
        }
        com.frxx.mengzhi.FanRenXiuXianMengZhi.NETWORK.sendTo(
            new com.frxx.mengzhi.network.TiandaoNbtDocPacket(targetId, target.getName(), sb.toString()), player);
    }

    private static String nbtToText(net.minecraft.nbt.NBTBase tag) {
        if (tag instanceof net.minecraft.nbt.NBTTagString) {
            return "\"" + ((net.minecraft.nbt.NBTTagString) tag).getString() + "\"";
        }
        return tag.toString();
    }

    /** 保存：解析文本"键 = 值"行写回目标 NBT（数字/布尔/字符串自动识别） */
    public static void saveNbtDoc(EntityPlayerMP player, int targetId, String doc) {
        Entity target = targetId == -1 ? player : player.getServerWorld().getEntityByID(targetId);
        if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()) {
            send(player, TextFormatting.RED + "【天道】目标不存在或已消失");
            return;
        }
        NBTTagCompound forge = target.getEntityData();
        String[] lines = doc == null ? new String[0] : doc.split("\n", -1);
        int applied = 0;
        for (String line : lines) {
            String ln = line.trim();
            if (ln.isEmpty()) {
                continue;
            }
            int eq = ln.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = ln.substring(0, eq).trim();
            String v = ln.substring(eq + 1).trim();
            if (k.isEmpty()) {
                continue;
            }
            if (v.length() >= 2 && v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"') {
                forge.setString(k, v.substring(1, v.length() - 1));
            } else if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) {
                forge.setBoolean(k, Boolean.parseBoolean(v));
            } else {
                try {
                    forge.setDouble(k, Double.parseDouble(v));
                } catch (NumberFormatException e) {
                    forge.setString(k, v);
                }
            }
            applied++;
        }
        if (forge.hasKey("JingJieNum")) {
            syncRealmDisplay((EntityLivingBase) target, (int) Math.floor(forge.getDouble("JingJieNum")));
        }
        send(player, TextFormatting.GREEN + "【天道】已写入 " + target.getName() + " 的 NBT 共 "
            + applied + " 个字段");
    }

    /** 天道之力：写入任意 NBT 键（数字/文本/布尔自动识别），等效内置实体修改器 */
    private static void applyNbt(EntityPlayerMP player, int targetId, String key, String rawValue) {
        String k = key == null ? "" : key.trim();
        if (k.isEmpty()) {
            send(player, TextFormatting.RED + "【天道】NBT键名不能为空");
            return;
        }
        String v = rawValue == null ? "" : rawValue.trim();
        if (v.isEmpty()) {
            send(player, TextFormatting.RED + "【天道】NBT值不能为空");
            return;
        }
        Entity target = targetId == -1 ? player : player.getServerWorld().getEntityByID(targetId);
        if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()) {
            send(player, TextFormatting.RED + "【天道】目标不存在或已消失");
            return;
        }
        NBTTagCompound nbt = target.getEntityData();
        String kind;
        if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) {
            nbt.setBoolean(k, Boolean.parseBoolean(v));
            kind = "布尔";
        } else {
            try {
                double d = Double.parseDouble(v);
                nbt.setDouble(k, d);
                kind = "数字";
            } catch (NumberFormatException e) {
                nbt.setString(k, v);
                kind = "文本";
            }
        }
        // 写境界键时同步 base 面板显示串
        if (k.equals("JingJieNum")) {
            syncRealmDisplay((net.minecraft.entity.EntityLivingBase) target, (int) Math.floor(nbt.getDouble(k)));
        }
        send(player, TextFormatting.GREEN + "【天道】已写入 " + target.getName()
            + " 的NBT " + k + " = " + v + "（" + kind + "）");
    }

    // ==================== base 模组命名同步 ====================

    /**
     * 把境界等级同步成 base 模组面板能显示的字符串：
     * base 的 GuiKGui 读 "JingJie" + "XiaoJingJie" 两个字符串键拼出「化神初期」，
     * 只写 JingJieNum 面板不刷新，这里同时补齐显示键。
     */
    public static void syncRealmDisplay(EntityLivingBase living, double level) {
        NBTTagCompound nbt = living.getEntityData();
        int realm = (int) Math.floor(level);
        int sub = (int) Math.floor(nbt.getDouble("XiaoJingJieNum"));
        if (sub < 1 || sub > SUB_STAGE_NAMES.length) {
            sub = 1;
        }
        String big;
        if (realm >= TIANDAO_REALM) {
            big = "天道";
        } else {
            big = REALM_NAMES[Math.max(0, Math.min(realm - 1, REALM_NAMES.length - 1))];
        }
        nbt.setString("JingJie", big);
        // 天道没有小境界，小境界显示留空（base 面板拼接显示为"天道"）
        nbt.setString("XiaoJingJie", realm >= TIANDAO_REALM ? "" : SUB_STAGE_NAMES[(sub - 1) % SUB_STAGE_NAMES.length]);
        nbt.setDouble("JingJieNum", realm);
        nbt.setDouble("XiaoJingJieNum", sub);
    }

    // ==================== 被动：锁血 / 免伤 / 免死 / 自动修复 ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
EntityPlayer player = event.player;
        if (!isTiandao(player)) {
            return;
        }
        // 兜底：天道玩家若被任何路径真正杀死，下一个 tick 立即自动重生（避免卡死亡）
        if (player.isDead) {
            ((EntityPlayerMP) player).respawnPlayer();
            return;
        }

        // 防 base 服务端把境界编号钳回 1~11：恒定为 12（天道）并同步显示串
        if (player.getEntityData().getDouble("JingJieNum") < TIANDAO_REALM) {
            syncRealmDisplay(player, TIANDAO_REALM);
        }

        // 天道可普通飞行（双击空格起飞，与创造模式一致）
        player.capabilities.allowFlying = true;
        if (player.world.getTotalWorldTime() % 5 == 0) {
            player.sendPlayerAbilities();
        }

        // 灵力/真元恒满（base HUD 键：灵力 Power/PowerMax，真元 Base/NiangJiu30）
        NBTTagCompound nbt = player.getEntityData();
        nbt.setDouble("Power", LING_LI_VALUE);
        nbt.setDouble("PowerMax", LING_LI_VALUE);
        nbt.setDouble("Base", ZHEN_YUAN_MAX);
        nbt.setDouble("NiangJiu30", ZHEN_YUAN_MAX);

        // 无视一切负面状态：先收集副本再移除，避免遍历中修改导致崩溃
        java.util.List<net.minecraft.potion.Potion> bad = new java.util.ArrayList<net.minecraft.potion.Potion>();
        for (net.minecraft.potion.PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion().isBadEffect()) {
                bad.add(effect.getPotion());
            }
        }
        for (net.minecraft.potion.Potion potion : bad) {
            player.removeActivePotionEffect(potion);
        }

        // base 模组每 tick 会按境界重算属性并覆写 生命上限/攻击 等，天道玩家在此回写自定义属性
        applyPersistedAttrs(player);

        // 生命锁定：上限归一化到 100 且始终满格
        if (player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH) != null) {
            player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(LOCKED_MAX_HEALTH);
        }
        if (player.getHealth() < LOCKED_MAX_HEALTH) {
            player.setHealth((float) LOCKED_MAX_HEALTH);
        }

        // 自动修复：主手/副手/盔甲/物品栏/快捷栏全部槽位
        boolean repaired = false;
        repaired |= repairList(player.inventory.mainInventory);
        repaired |= repairList(player.inventory.armorInventory);
        repaired |= repairList(player.inventory.offHandInventory);
        if (repaired && player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }

        // 天道自动攻击：每tick 自动锁定周围 9×9×9 范围所有生物，强制归零并触发死亡
        if (isFlagOn(player, TAG_AUTO_ON)) {
            double x = player.posX;
            double y = player.posY;
            double z = player.posZ;
            List<EntityLivingBase> all = player.world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(x - 4.5, y - 4.5, z - 4.5, x + 4.5, y + 4.5, z + 4.5));
            for (EntityLivingBase e : all) {
                if (e == player || !e.isEntityAlive()) {
                    continue;
                }
                if (e instanceof EntityPlayer && isTiandao((EntityPlayer) e)) {
                    continue;
                }
                forcefulKill(e);
            }
        }
    }

    private static boolean repairList(List<ItemStack> stacks) {
        boolean changed = false;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && stack.isItemStackDamageable() && stack.getItemDamage() > 0) {
                stack.setItemDamage(0);
                changed = true;
            }
        }
        return changed;
    }

    /** 拦截一切攻击来源伤害：与伤害应用双保险，最高优先先于 base 模组处理 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity() instanceof EntityPlayer && isTiandao((EntityPlayer) event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** 拦截伤害计算：最高优先，保证先于 base 模组本身的伤害处理 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getEntity() instanceof EntityPlayer && isTiandao((EntityPlayer) event.getEntity())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
        }
    }

    /**
     * 天道之力：天道玩家攻击任意实体时，强制目标血量归零并走完整死亡事件。
     * 原“拦截自身伤害”分支保留在最前。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof EntityPlayer && isTiandao((EntityPlayer) event.getEntity())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            Entity attacker = event.getSource().getTrueSource();
            if (isTiandao((EntityPlayer) attacker) && isFlagOn((EntityPlayer) attacker, TAG_FORCE_ON)) {
                EntityLivingBase victim = event.getEntityLiving();
                if (event.getEntity() == attacker
                    || (victim instanceof EntityPlayer && isTiandao((EntityPlayer) victim))) {
                    return;
                }
                event.setCanceled(true);
                event.setAmount(0.0F);
                forcefulKill(victim);
            }
        }
    }

    /**
     * 天道之力 · 强制抹除：主伤害流转后先归零后补刀，保证死亡事件完整触发。
     */
    private static boolean forcefulKill(EntityLivingBase victim) {
        if (victim == null || victim.isDead || victim.world == null) {
            return false;
        }
        if (victim.attackEntityFrom(TIANDAO_SOURCE, Float.MAX_VALUE)) {
            if (!victim.isEntityAlive() && victim.isDead) {
                return true;
            }
        }
        if (victim.isEntityAlive()) {
            victim.setHealth(0.0F);
        }
        if (victim.isEntityAlive() && victim.world != null) {
            victim.onDeath(TIANDAO_SOURCE);
            victim.setDead();
        }
        return true;
    }

    /** 天道之力 · 右键抹除目标；潜行右键抹除周围 100×100×100 范围（无需对准目标） */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!isTiandao(player) || !isFlagOn(player, TAG_FORCE_ON)) {
            return;
        }
        event.setCanceled(true);
        if (player.isSneaking()) {
            forceAreaKill((EntityPlayerMP) player);
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof EntityLivingBase && target != player) {
            if (target instanceof EntityPlayer && isTiandao((EntityPlayer) target)) {
                send(player, TextFormatting.GRAY + "【天道之力】无法抹除其他天道");
                return;
            }
            if (forcefulKill((EntityLivingBase) target)) {
                send(player, TextFormatting.RED + "【天道之力】已抹除实体：" + target.getName());
            }
        }
    }

    /** 潜行右键空气/方块同样触发范围抹除（无需对准目标） */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlank(net.minecraftforge.event.entity.player.PlayerInteractEvent event) {
        if (event.getWorld().isRemote || !(event instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickEmpty)
            && !(event instanceof net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!isTiandao(player) || !isFlagOn(player, TAG_FORCE_ON) || !player.isSneaking()) {
            return;
        }
        event.setCanceled(true);
        forceAreaKill((EntityPlayerMP) player);
    }

    /** 范围抹除：以玩家为中心 100×100×100 立方格 */
    private static void forceAreaKill(EntityPlayerMP player) {
        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;
        List<EntityLivingBase> all = player.world.getEntitiesWithinAABB(EntityLivingBase.class,
            new AxisAlignedBB(x - 50.0, y - 50.0, z - 50.0, x + 50.0, y + 50.0, z + 50.0));
        int killed = 0;
        for (EntityLivingBase e : all) {
            if (e == player || !e.isEntityAlive()) {
                continue;
            }
            if (e instanceof EntityPlayer && isTiandao((EntityPlayer) e)) {
                continue;
            }
            if (forcefulKill(e)) {
                killed++;
            }
        }
        send(player, TextFormatting.RED + "【天道之力】已抹除周围 100×100×100 范围内 "
            + killed + " 个实体");
    }

    /**
     * 天道免疫一切负面药水：在附加判定阶段直接拒绝（Result.DENY）。
     * 比每tick事后清除更彻底——base 等其他模组每tick重新附加同样会被拦截。
     */
    @SubscribeEvent
    public static void onPotionApplicable(net.minecraftforge.event.entity.living.PotionEvent.PotionApplicableEvent event) {
        net.minecraft.entity.EntityLivingBase living = event.getEntityLiving();
        if (living instanceof EntityPlayer && isTiandao((EntityPlayer) living)
            && event.getPotionEffect().getPotion().isBadEffect()) {
            event.setResult(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        }
    }

    /** 拦截一切死亡：绝对不死不灭 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof EntityPlayer && isTiandao((EntityPlayer) event.getEntity())) {
            event.setCanceled(true);
            ((EntityLivingBase) event.getEntity()).setHealth((float) LOCKED_MAX_HEALTH);
        }
    }

    // ==================== 功能一：切换模式 ====================

    /** 创造 -> 生存 -> 冒险 -> 创造 循环 */
    private static void switchGameMode(EntityPlayerMP player) {
        GameType current = player.interactionManager.getGameType();
        GameType next;
        if (current == GameType.CREATIVE) {
            next = GameType.SURVIVAL;
        } else if (current == GameType.SURVIVAL) {
            next = GameType.ADVENTURE;
        } else {
            next = GameType.CREATIVE;
        }
        player.setGameType(next);
        send(player, TextFormatting.GREEN + "【天道】游戏模式已切换为：" + next.getName());
    }

    // ==================== 功能二：制造真实天劫（凡人修仙传式） ====================

    /**
     * 对锁定区域降下真实天劫，参照 base 模组 ProcedureLeiJie 的真实机制：
     * 云层闷雷（thunder 音效）+ 多道天雷在目标周围随机落点（非单体）+ 范围真实伤害。
     */
    private static void castTribulation(EntityPlayerMP player, int targetId) {
        long time = player.getServerWorld().getTotalWorldTime();
        NBTTagCompound data = player.getEntityData();
        long cd = data.getLong(TAG_CAST_CD);
        if (time < cd) {
            send(player, TextFormatting.GRAY + "【天道】天劫充能中，剩余 " + (cd - time) + " tick");
            return;
        }
        Entity center = targetId == -1 ? player : player.getServerWorld().getEntityByID(targetId);
        if (!(center instanceof EntityLivingBase) || !center.isEntityAlive()) {
            send(player, TextFormatting.RED + "【天道】未锁定有效目标，请先看向目标再开启面板");
            return;
        }
        data.setLong(TAG_CAST_CD, time + CAST_COOLDOWN_TICKS);
        World world = center.world;
        if (world.isRemote) {
            return;
        }

        double cx = center.posX;
        double cy = center.posY;
        double cz = center.posZ;

        // 雷劫本体：三次雷电在三波中轰下，落点在天劫中心 ±8 格随机（参照 base：随机 ±10 格）
        for (int i = 0; i < 3; i++) {
            double dx = cx + (RNG.nextDouble() * 16.0 - 8.0);
            double dz = cz + (RNG.nextDouble() * 16.0 - 8.0);
            world.spawnEntity(new EntityLightningBolt(world, dx, cy, dz, false));
            world.playSound((EntityPlayer) null, cx, cy + 18.0, cz,
                SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 10.0F,
                0.85F + RNG.nextFloat() * 0.3F);
        }

        double panelAttack = data.getDouble("Attack") + data.getDouble("MagicAttack") * 0.5;
        double physical = Math.round(panelAttack * 1.5);
        double magic = Math.round(panelAttack * 0.5);
        double total = physical + magic;

        // 区域真实伤害：以天劫中心 ±6 格半径波及范围内所有生物（非单体）
        AxisAlignedBB box = new AxisAlignedBB(cx - 6.0, cy - 6.0, cz - 6.0,
            cx + 6.0, cy + 6.0, cz + 6.0);
        List<EntityLivingBase> victims = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        for (EntityLivingBase victim : victims) {
            if (victim == player) {
                continue;
            }
            victim.attackEntityFrom(TIANDAO_SOURCE, (float) total);
            if (victim instanceof EntityPlayer) {
((EntityPlayer) victim).sendMessage(new TextComponentString(
                    TextFormatting.RED + "逆天而行，天雷降临！对你造成 " + (int) physical + " 物理伤害 + "
                        + (int) magic + " 雷电伤害！"));
            }
        }
        send(player, TextFormatting.RED + "【天道】真实天劫已降：「" + center.getName()
            + "」区域 " + Math.round(total) + " 真实伤害（多道天雷覆盖）");
    }

    // ==================== 功能三：改属性 ====================

    /** 属性索引：0=生命上限 1=当前生命 2=攻击 3=攻击速度(倍率) 4=移动速度(倍率)
     *  5=护甲 6=幸运 7=护甲韧性 8=境界(第几重) */
    private static void applyAttribute(EntityPlayerMP player, int targetId, int attrIndex, double value) {
        Entity target = targetId == -1 ? player : player.getServerWorld().getEntityByID(targetId);
        if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()) {
            send(player, TextFormatting.RED + "【天道】目标不存在或已消失");
            return;
        }
        if (Double.isNaN(value)) {
            send(player, TextFormatting.RED + "【天道】属性数值无效，请输入非负数字");
            return;
        }
        if (Double.isInfinite(value) || value < 0.0) {
            send(player, TextFormatting.RED + "【天道】属性数值无效，请输入非负数字");
            return;
        }
        EntityLivingBase living = (EntityLivingBase) target;
        NBTTagCompound nbt = living.getEntityData();
        String name = target.getName();
        switch (attrIndex) {
            case 0: // 生命上限（同步 base 面板键 + 持久化）
                if (living.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH) != null) {
                    living.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(Math.max(1.0, value));
                    living.setHealth((float) Math.min(value, 9999.0));
                    nbt.setDouble("HealthMaxUpd", value);
                    persistAttr(living, 0, value);
                    send(player, "已将 " + name + " 的生命上限设为 " + (int) value);
                }
                break;
            case 1: // 当前生命
                living.setHealth((float) Math.min(value, 9999.0));
                send(player, "已将 " + name + " 的当前生命设为 " + (int) value);
                break;
            case 2: // 攻击（同步 base 面板 Power / PowerMax 键，持久化防 base 每tick覆写）
                if (living.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE) != null) {
                    living.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(value);
                    nbt.setDouble("Power", value);
                    nbt.setDouble("PowerAttack", value);
                    persistAttr(living, 2, value);
                    send(player, "已将 " + name + " 的攻击设为 " + (int) value);
                }
                break;
            case 3: // 攻击速度
                if (living.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED) != null) {
                    living.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).setBaseValue(4.0 * value);
                    persistAttr(living, 1, value);
                    send(player, "已将 " + name + " 的攻击速度设为 " + value + " 倍");
                }
                break;
            case 4: // 移动速度（实际0.1=1倍）
                if (living.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED) != null) {
                    living.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.1 * value);
                    persistAttr(living, 3, value);
                    send(player, "已将 " + name + " 的移动速度设为 " + value + " 倍");
                }
                break;
            case 5: // 护甲
                if (living.getEntityAttribute(SharedMonsterAttributes.ARMOR) != null) {
                    living.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(value);
                    persistAttr(living, 4, value);
                    send(player, "已将 " + name + " 的护甲设为 " + (int) value);
                }
                break;
            case 6: // 幸运
                if (living.getEntityAttribute(SharedMonsterAttributes.LUCK) != null) {
                    living.getEntityAttribute(SharedMonsterAttributes.LUCK).setBaseValue(value);
                    persistAttr(living, 5, value);
                    send(player, "已将 " + name + " 的幸运设为 " + (int) value);
                }
                break;
            case 7: // 护甲韧性
                if (living.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS) != null) {
                    living.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(value);
                    persistAttr(living, 6, value);
                    send(player, "已将 " + name + " 的护甲韧性设为 " + (int) value);
                }
                break;
            case 8: // 境界（同步 base 面板字符串显示）
                living.getEntityData().setDouble("JingJieNum", value);
                syncRealmDisplay(living, (int) value);
                send(player, "已将 " + name + " 的境界设为 " + (int) value);
                break;
            default:
                break;
        }
    }

    /** 持久化自定义属性到 NBT，防空覆写回读 */
    private static void persistAttr(EntityLivingBase living, int slot, double value) {
        living.getEntityData().setDouble(TAG_ATTR_PREFIX + slot, value);
    }

    /**
     * base 模组每 tick 重算攻击/生命（依据境界 1~11），天道玩家在此回写
     * 面板设定值。只对带有持久化标记的实体生效，避免覆盖其他玩家。
     */
    private static void applyPersistedAttrs(EntityPlayer target) {
        NBTTagCompound nbt = target.getEntityData();
        if (nbt.getDouble(TAG_ATTR_PREFIX + "0") != 0) {
            double v = nbt.getDouble(TAG_ATTR_PREFIX + "0");
            if (target.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH) != null) {
                target.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(Math.max(1.0, v));
                nbt.setDouble("HealthMaxUpd", v);
            }
        }
        if (nbt.hasKey(TAG_ATTR_PREFIX + "2") && nbt.getDouble(TAG_ATTR_PREFIX + "2") != 0) {
            double v = nbt.getDouble(TAG_ATTR_PREFIX + "2");
            if (target.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE) != null) {
                target.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(v);
                nbt.setDouble("Power", v);
                nbt.setDouble("PowerAttack", v);
            }
        }
        // 界面已允许编辑攻击速度/移动速度/护甲/幸运/韧性，联动恢复
        restoreAttrDouble(nbt, target, "1", SharedMonsterAttributes.ATTACK_SPEED, 4.0);
        restoreAttrDouble(nbt, target, "3", SharedMonsterAttributes.MOVEMENT_SPEED, 0.1);
        restoreAttrDouble(nbt, target, "4", SharedMonsterAttributes.ARMOR, 1.0);
        restoreAttrDouble(nbt, target, "5", SharedMonsterAttributes.LUCK, 1.0);
        restoreAttrDouble(nbt, target, "6", SharedMonsterAttributes.ARMOR_TOUGHNESS, 1.0);
    }

    private static void restoreAttrDouble(NBTTagCompound nbt, EntityLivingBase target,
                                          String slot, net.minecraft.entity.ai.attributes.IAttribute attr, double scale) {
        if (nbt.getDouble(TAG_ATTR_PREFIX + slot) == 0) {
            return;
        }
        double v = nbt.getDouble(TAG_ATTR_PREFIX + slot) * scale;
        if (target.getEntityAttribute(attr) != null) {
            target.getEntityAttribute(attr).setBaseValue(v);
        }
    }
}