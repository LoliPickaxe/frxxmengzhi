package com.frxx.mengzhi.handler;

import com.frxx.mengzhi.network.SpiritBombPlacePacket;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpiritBombHandler {

    public static final int MAX_BOMBS = 16;

    private static final DamageSource BOMB_EXPLOSION = new DamageSource("spiritBomb").setExplosion();
    private static final DamageSource BOMB_SOUL = new DamageSource("spiritBombSoul").setDamageBypassesArmor().setDamageIsAbsolute();

    private static final Map<UUID, List<PlacedBomb>> bombs = new ConcurrentHashMap<UUID, List<PlacedBomb>>();

    public static class PlacedBomb {
        final int dimension;
        final byte type;
        final int targetId;
        final int x;
        final int y;
        final int z;
        final double savedAttack;
        final boolean itemHasDurability;
        final int itemDurability;
        final Item storedItem;

        PlacedBomb(int dimension, byte type, int targetId, int x, int y, int z,
                   double savedAttack, boolean itemHasDurability, int itemDurability, Item storedItem) {
            this.dimension = dimension;
            this.type = type;
            this.targetId = targetId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.savedAttack = savedAttack;
            this.itemHasDurability = itemHasDurability;
            this.itemDurability = itemDurability;
            this.storedItem = storedItem;
        }
    }

    public static void placeBomb(EntityPlayerMP player, byte bombType, int targetId, int x, int y, int z) {
        NBTTagCompound data = player.getEntityData();
        if (data.getDouble("JingJieNum") < 1.0) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u60a8\u5c1a\u672a\u4fee\u4ed9\uff0c\u65e0\u6cd5\u5e03\u7f6e\u7075\u6c14\u70b8\u5f39"));
            return;
        }
        double power = data.getDouble("Power");
        if (power < 1.0) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u7075\u529b\u4e0d\u8db3\uff0c\u65e0\u6cd5\u5e03\u7f6e\u7075\u6c14\u70b8\u5f39"));
            return;
        }
        UUID key = player.getUniqueID();
        List<PlacedBomb> list = bombs.get(key);
        if (list != null && list.size() >= MAX_BOMBS) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "\u7075\u6c14\u70b8\u5f39\u6570\u91cf\u5df2\u8fbe\u4e0a\u9650\uff08" + MAX_BOMBS + "\uff09\uff0c\u8bf7\u5148\u5f15\u7206"));
            return;
        }
        WorldServer world = player.getServerWorld();

        if (bombType == SpiritBombPlacePacket.TYPE_ENTITY) {
            Entity target = world.getEntityByID(targetId);
            if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "\u76ee\u6807\u4e0d\u5b58\u5728\u6216\u5df2\u6d88\u5931"));
                return;
            }
            EntityLivingBase living = (EntityLivingBase) target;
            double rate = successRate(living.getHealth(), player.getHealth());
            if (rate <= 0) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "\u76ee\u6807\u5b9e\u529b\u8fc7\u4e8e\u60ac\u6b8a\uff0c\u65e0\u6cd5\u5e03\u7f6e\u7075\u6c14\u70b8\u5f39\uff01"));
                return;
            }
            boolean ok = world.rand.nextDouble() * 100.0 < rate;
            double cost = power * (ok ? 0.10 : 0.02);
            data.setDouble("Power", power - cost);
            if (!ok) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "\u5e03\u7f6e\u7075\u6c14\u70b8\u5f39\u5931\u8d25\uff0c\u76ee\u6807\u5b9e\u529b\u8fc7\u5f3a\uff08\u6d88\u8017 2% \u7075\u529b\uff09"));
                return;
            }
            addBomb(key, new PlacedBomb(world.provider.getDimension(), SpiritBombPlacePacket.TYPE_ENTITY,
                targetId, 0, 0, 0, 0, false, 0, null));
            player.sendMessage(new TextComponentString(TextFormatting.GOLD + "\u7075\u6c14\u70b8\u5f39\u5df2\u5e03\u7f6e\u5728 "
                + living.getName() + " \u4e0a\uff08\u6210\u529f\u7387 " + (int) rate + "%\uff09\uff01\u6f5c\u884c+G \u5f15\u7206"));
        } else if (bombType == SpiritBombPlacePacket.TYPE_BLOCK) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.BARRIER) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "\u8be5\u4f4d\u7f6e\u65e0\u6cd5\u5e03\u7f6e\u7075\u6c14\u70b8\u5f39"));
                return;
            }
            double cost = power * 0.20;
            data.setDouble("Power", power - cost);
            addBomb(key, new PlacedBomb(world.provider.getDimension(), SpiritBombPlacePacket.TYPE_BLOCK,
                0, x, y, z, data.getDouble("Attack"), false, 0, null));
            player.sendMessage(new TextComponentString(TextFormatting.GOLD + "\u7075\u6c14\u70b8\u5f39\u5df2\u5e03\u7f6e\u5728\u65b9\u5757\u4e0a\uff01"
                + "\u6f5c\u884c+G \u5f15\u7206\uff08\u4fdd\u5b58\u653b\u51fb " + (int) data.getDouble("Attack") + "\uff09"));
        } else {
            boolean heldItem = targetId == -1;
            ItemStack stack;
            int posX;
            int posY;
            int posZ;
            String targetName;
            if (heldItem) {
                stack = player.getHeldItemMainhand();
                if (stack.isEmpty()) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "\u8bf7\u624b\u6301\u7269\u54c1\u540e\u518d\u5e03\u7f6e\u7075\u6c14\u70b8\u5f39"));
                    return;
                }
                posX = (int) Math.floor(player.posX);
                posY = (int) Math.floor(player.posY);
                posZ = (int) Math.floor(player.posZ);
                targetName = "\u624b\u4e2d\u7684\u7269\u54c1";
            } else {
                Entity target = world.getEntityByID(targetId);
                if (!(target instanceof EntityItem) || !target.isEntityAlive()) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "\u76ee\u6807\u4e0d\u5b58\u5728\u6216\u5df2\u6d88\u5931"));
                    return;
                }
                EntityItem itemEntity = (EntityItem) target;
                stack = itemEntity.getItem();
                posX = (int) Math.floor(itemEntity.posX);
                posY = (int) Math.floor(itemEntity.posY);
                posZ = (int) Math.floor(itemEntity.posZ);
                targetName = "\u6389\u843d\u7269";
            }
            double cost = power * 0.20;
            data.setDouble("Power", power - cost);
            boolean durable = stack.isItemStackDamageable();
            int durability = durable ? stack.getMaxDamage() - stack.getItemDamage() : 0;
            addBomb(key, new PlacedBomb(world.provider.getDimension(), SpiritBombPlacePacket.TYPE_ITEM, targetId,
                posX, posY, posZ, data.getDouble("Attack"), durable, durability, stack.getItem()));
            if (durable) {
                player.sendMessage(new TextComponentString(TextFormatting.GOLD + "\u7075\u6c14\u70b8\u5f39\u5df2\u5e03\u7f6e\u5728"
                    + targetName + "\u4e0a\uff08\u8010\u4e45 " + durability + "\uff09\uff01\u70b8\u5f39\u4f1a\u8ddf\u968f\u7269\u54c1\uff0c\u53ef\u6254\u51fa\u6216\u8f6c\u4ea4\uff0c\u6f5c\u884c+G \u5f15\u7206"));
            } else {
                player.sendMessage(new TextComponentString(TextFormatting.GOLD + "\u7075\u6c14\u70b8\u5f39\u5df2\u5e03\u7f6e\u5728"
                    + targetName + "\u4e0a\uff01\u70b8\u5f39\u4f1a\u8ddf\u968f\u7269\u54c1\uff0c\u53ef\u6254\u51fa\u6216\u8f6c\u4ea4\uff0c\u6f5c\u884c+G \u5f15\u7206"));
            }
        }
    }

    public static void detonateAll(EntityPlayerMP player) {
        UUID key = player.getUniqueID();
        List<PlacedBomb> list = bombs.remove(key);
        if (list == null || list.isEmpty()) {
            player.sendMessage(new TextComponentString(TextFormatting.GRAY + "\u6ca1\u6709\u53ef\u5f15\u7206\u7684\u7075\u6c14\u70b8\u5f39"));
            return;
        }
        WorldServer world = player.getServerWorld();
        int detonated = 0;
        int fizzled = 0;
        for (PlacedBomb bomb : list) {
            if (bomb.dimension != world.provider.getDimension()) {
                fizzled++;
                continue;
            }
            if (bomb.type == SpiritBombPlacePacket.TYPE_ENTITY) {
                Entity target = world.getEntityByID(bomb.targetId);
                if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()) {
                    fizzled++;
                    continue;
                }
                EntityLivingBase living = (EntityLivingBase) target;
                double hp = living.getHealth();
                if (hp <= 0) {
                    fizzled++;
                    continue;
                }
                double cx = living.posX;
                double cy = living.posY + living.height * 0.5;
                double cz = living.posZ;
                explodeVisual(world, cx, cy, cz, bombRadius(player, hp));
                living.attackEntityFrom(BOMB_EXPLOSION, (float) (hp * 1.0));
                living.attackEntityFrom(BOMB_SOUL, (float) (hp * 0.2));
                detonated++;
            } else if (bomb.type == SpiritBombPlacePacket.TYPE_BLOCK) {
                int radius = realmHalfRadius(player);
                double cx = bomb.x + 0.5;
                double cy = bomb.y + 0.5;
                double cz = bomb.z + 0.5;
                explodeVisual(world, cx, cy, cz, radius);
                damageAoE(player, world, cx, cy, cz, radius, bomb.savedAttack, false, 0);
                SelfDestructHandler.destroySphereAsync(world, cx, cy, cz, radius);
                detonated++;
            } else {
                int radius = realmHalfRadius(player);
                double cx;
                double cy;
                double cz;
                if (bomb.targetId == -1) {
                    if (sameItem(player.getHeldItemMainhand(), bomb.storedItem)) {
                        cx = player.posX;
                        cy = player.posY;
                        cz = player.posZ;
                    } else {
                        double[] pos = locateItem(world, bomb.storedItem, player, bomb.x, bomb.y, bomb.z);
                        cx = pos[0];
                        cy = pos[1];
                        cz = pos[2];
                    }
                } else {
                    Entity target = world.getEntityByID(bomb.targetId);
                    if (target instanceof EntityItem && target.isEntityAlive()) {
                        cx = target.posX;
                        cy = target.posY;
                        cz = target.posZ;
                    } else {
                        double[] pos = locateItem(world, bomb.storedItem, null, bomb.x, bomb.y, bomb.z);
                        cx = pos[0];
                        cy = pos[1];
                        cz = pos[2];
                    }
                }
                explodeVisual(world, cx, cy, cz, radius);
                if (bomb.itemHasDurability) {
                    double dmg = bomb.itemDurability * 0.2;
                    damageAoE(player, world, cx, cy, cz, radius, dmg, true, dmg);
                } else {
                    damageAoE(player, world, cx, cy, cz, radius, bomb.savedAttack, false, 0);
                }
                detonated++;
            }
        }
        String extra = fizzled > 0 ? "\uff08" + fizzled + " \u679a\u843d\u7a7a\uff09" : "";
        player.sendMessage(new TextComponentString(TextFormatting.GOLD + "\u5f15\u7206\u4e86 " + detonated
            + " \u679a\u7075\u6c14\u70b8\u5f39" + extra + "\uff01"));
    }

    /** 成功率: 目标血量/玩家血量 <=1/3 必成, <=1/2 至少50%, 相等约10%, 达到2倍即0%无法布置 */
    private static double successRate(double targetHp, double playerHp) {
        if (targetHp <= 0) return 100.0;
        double r = playerHp <= 0 ? 2.0 : targetHp / playerHp;
        if (r >= 2.0) return 0.0;
        if (r <= 1.0 / 3.0) return 100.0;
        if (r <= 0.5) return 100.0 - (r - 1.0 / 3.0) / (1.0 / 6.0) * 50.0;
        if (r <= 1.0) return 50.0 - (r - 0.5) / 0.5 * 40.0;
        return 10.0 - (r - 1.0) * 10.0;
    }

    /** 实体炸弹半径: 基础2格 + 每20血+1格; 目标血量每10倍于玩家血量翻倍; 上限32 */
    private static int bombRadius(EntityPlayerMP player, double targetHp) {
        int r = 2 + (int) (targetHp / 20.0);
        double playerHp = player.getHealth();
        double mult = targetHp;
        int factor = 1;
        while (playerHp > 0 && mult >= playerHp * 10.0 && factor < 8) {
            factor *= 2;
            mult /= 10.0;
        }
        return Math.min(32, r * factor);
    }

    /** 方块/物品炸弹半径: 境界标准半径的一半 (元婴16 -> 8) */
    private static int realmHalfRadius(EntityPlayerMP player) {
        return Math.max(1, SelfDestructHandler.getBlastRadius(player.getEntityData()) / 2);
    }

    private static void explodeVisual(WorldServer world, double cx, double cy, double cz, int radius) {
        world.playSound(null, cx, cy, cz, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, 0.9F);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, cx, cy, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        int burst = Math.min(400, Math.max(24, radius * 8));
        for (int i = 0; i < burst; i++) {
            double theta = world.rand.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * world.rand.nextDouble() - 1);
            double dx = Math.sin(phi) * Math.cos(theta);
            double dy = Math.cos(phi);
            double dz = Math.sin(phi) * Math.sin(theta);
            double d = 0.5 + world.rand.nextDouble() * radius;
            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                cx + dx * d, cy + dy * d, cz + dz * d, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void damageAoE(EntityPlayerMP placer, WorldServer world, double cx, double cy, double cz,
                                  int radius, double physical, boolean withMagic, double magic) {
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class,
            new AxisAlignedBB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius));
        for (EntityLivingBase target : targets) {
            if (target == placer) continue;
            double dx = target.posX - cx;
            double dy = target.posY - cy;
            double dz = target.posZ - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > radius) continue;
            if (dist < 0.01) dist = 0.01;
            double factor = Math.max(0.6, 1.0 - 0.05 * dist);
            if (physical > 0) target.attackEntityFrom(BOMB_EXPLOSION, (float) (physical * factor));
            if (withMagic && magic > 0) target.attackEntityFrom(BOMB_SOUL, (float) (magic * factor));
            double kb = (1.0 - dist / radius) * 1.2;
            target.motionX += dx / dist * kb;
            target.motionY += 0.2 + 0.3 * (1.0 - dist / radius);
            target.motionZ += dz / dist * kb;
            target.velocityChanged = true;
        }
    }

    private static void addBomb(UUID key, PlacedBomb bomb) {
        List<PlacedBomb> list = bombs.get(key);
        if (list == null) {
            list = new ArrayList<PlacedBomb>();
            bombs.put(key, list);
        }
        list.add(bomb);
    }

    private static boolean sameItem(ItemStack stack, Item item) {
        return item != null && stack != null && !stack.isEmpty() && stack.getItem() == item;
    }

    /** 炸弹附着在物品上：先在掉落物中找，再找玩家手持（可转交给好友），最后回退布置时位置 */
    private static double[] locateItem(WorldServer world, Item item, EntityPlayer exclude,
                                       int fallbackX, int fallbackY, int fallbackZ) {
        for (Object o : world.loadedEntityList) {
            if (o instanceof EntityItem) {
                EntityItem ei = (EntityItem) o;
                if (ei.isEntityAlive() && sameItem(ei.getItem(), item)) {
                    return new double[] { ei.posX, ei.posY, ei.posZ };
                }
            }
        }
        for (Object o : world.playerEntities) {
            EntityPlayer p = (EntityPlayer) o;
            if (p == exclude || p.isDead) continue;
            if (sameItem(p.getHeldItemMainhand(), item)) {
                return new double[] { p.posX, p.posY, p.posZ };
            }
        }
        return new double[] { fallbackX + 0.5, fallbackY + 0.5, fallbackZ + 0.5 };
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        bombs.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (bombs.remove(event.player.getUniqueID()) != null) {
            event.player.sendMessage(new TextComponentString(TextFormatting.GRAY + "\u7075\u6c14\u70b8\u5f39\u5df2\u6d88\u6563"));
        }
    }
}
