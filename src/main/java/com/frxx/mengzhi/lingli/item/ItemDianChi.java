package com.frxx.mengzhi.lingli.item;

import com.frxx.mengzhi.lingli.LingLiConstants;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class ItemDianChi extends Item {

    public static final String TAG_CHARGING = "Charging";
    public static final String TAG_ENERGY = "Energy";

    private final int tier;

    public ItemDianChi(int tier) {
        this.tier = tier;
        setMaxStackSize(1);
    }

    public int getTier() {
        return tier;
    }

    public int getCapacity() {
        return LingLiConstants.BATTERY_CAPACITY[tier - 1];
    }

    public int getRate() {
        return LingLiConstants.BATTERY_RATE[tier - 1];
    }

    public int getRegenPerSecond() {
        return LingLiConstants.BATTERY_REGEN_PER_SECOND[tier - 1];
    }

    public int getRatio() {
        return LingLiConstants.BATTERY_RATIO[tier - 1];
    }

    private static NBTTagCompound ensureTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    public static boolean isChargingMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(TAG_CHARGING);
    }

    public static int getEnergy(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getInteger(TAG_ENERGY);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        ensureTag(stack).setInteger(TAG_ENERGY, Math.max(0, energy));
    }

    private static class StackEnergyStorage extends EnergyStorage {

        private final ItemStack stack;

        StackEnergyStorage(ItemStack stack, int capacity, int maxTransfer) {
            super(capacity, maxTransfer, maxTransfer, getEnergy(stack));
            this.stack = stack;
        }

        private void save() {
            setEnergy(stack, energy);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                save();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                save();
            }
            return extracted;
        }
    }

    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return new net.minecraftforge.common.capabilities.ICapabilityProvider() {
            @Override
            public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, EnumFacing facing) {
                if (capability == CapabilityEnergy.ENERGY) {
                    return CapabilityEnergy.ENERGY.cast(new StackEnergyStorage(stack, getCapacity(), getRate()));
                }
                return null;
            }

            @Override
            public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY;
            }
        };
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        RayTraceResult hit = rayTrace(worldIn, playerIn, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            TileEntity tile = worldIn.getTileEntity(hit.getBlockPos());
            if (tile != null) {
                if (!tile.hasCapability(CapabilityEnergy.ENERGY, hit.sideHit)) {
                    return new ActionResult<>(EnumActionResult.PASS, stack);
                }
                IEnergyStorage machine = tile.getCapability(CapabilityEnergy.ENERGY, hit.sideHit);
                IEnergyStorage battery = new StackEnergyStorage(stack, getCapacity(), getRate());
                int maxTransfer;
                if (playerIn.isSneaking()) {
                    if (!machine.canExtract() || !battery.canReceive()) {
                        return new ActionResult<>(EnumActionResult.PASS, stack);
                    }
                    maxTransfer = Math.min(getRate(), battery.getMaxEnergyStored() - battery.getEnergyStored());
                    int extracted = machine.extractEnergy(maxTransfer, false);
                    battery.receiveEnergy(extracted, false);
                } else {
                    if (!battery.canExtract() || !machine.canReceive()) {
                        return new ActionResult<>(EnumActionResult.PASS, stack);
                    }
                    maxTransfer = Math.min(getRate(), machine.getMaxEnergyStored() - machine.getEnergyStored());
                    int extracted = battery.extractEnergy(maxTransfer, false);
                    machine.receiveEnergy(extracted, false);
                }
                playerIn.swingArm(handIn);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        }
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if (!worldIn.isRemote) {
            NBTTagCompound tag = ensureTag(stack);
            boolean charging = !tag.getBoolean(TAG_CHARGING);
            tag.setBoolean(TAG_CHARGING, charging);
            if (charging) {
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "电池已开启，每秒回复 " + getRegenPerSecond() + " 点灵力（1FE:" + getRatio() + "灵力）"));
            } else {
                playerIn.sendMessage(new TextComponentString(TextFormatting.GRAY + "电池已关闭"));
            }
            playerIn.swingArm(handIn);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.YELLOW + "FE: " + getEnergy(stack) + " / " + getCapacity());
        if (isChargingMode(stack)) {
            tooltip.add(TextFormatting.GREEN + "模式: 已开启（每秒回复" + getRegenPerSecond() + "灵力）");
        } else {
            tooltip.add(TextFormatting.GRAY + "模式: 已关闭");
        }
        tooltip.add(TextFormatting.GRAY + "右键机器: 放电 | 潜行右键: 充电 | 右键空气: 开关");
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getEnergy(stack) < getCapacity();
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0D - getEnergy(stack) / (double) getCapacity();
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0xFFD700;
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
        for (ItemStack stack : player.inventory.mainInventory) {
            tickBattery(player, stack);
        }
        for (ItemStack stack : player.inventory.armorInventory) {
            tickBattery(player, stack);
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            tickBattery(player, stack);
        }
    }

    private static void tickBattery(EntityPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemDianChi)) {
            return;
        }
        ItemDianChi battery = (ItemDianChi) stack.getItem();
        if (!isChargingMode(stack)) {
            return;
        }
        NBTTagCompound data = player.getEntityData();
        double power = data.getDouble("Power");
        double powerMax = data.getDouble("PowerMax");
        if (powerMax <= 0) {
            return;
        }
        double maxGain = powerMax - power;
        if (maxGain <= 0) {
            return;
        }
        int energy = getEnergy(stack);
        if (energy <= 0) {
            return;
        }
        double ratePerTick = battery.getRegenPerSecond() / 20.0D;
        double desiredGain = Math.min(ratePerTick, maxGain);
        int feNeeded = (int) Math.ceil(desiredGain / battery.getRatio());
        feNeeded = Math.min(feNeeded, energy);
        if (feNeeded <= 0) {
            return;
        }
        IEnergyStorage batteryCap = new StackEnergyStorage(stack, battery.getCapacity(), battery.getRate());
        int feUsed = batteryCap.extractEnergy(feNeeded, false);
        if (feUsed <= 0) {
            return;
        }
        double actualGain = Math.min(feUsed * battery.getRatio(), maxGain);
        data.setDouble("Power", power + actualGain);
    }
}