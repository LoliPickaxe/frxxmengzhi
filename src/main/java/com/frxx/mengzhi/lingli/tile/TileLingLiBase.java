package com.frxx.mengzhi.lingli.tile;

import com.frxx.mengzhi.lingli.LingLiConstants;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class TileLingLiBase extends TileEntity implements ITickable {

    public static class LingLiEnergyStorage extends EnergyStorage {

        public LingLiEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void setEnergyStored(int energy) {
            this.energy = Math.max(0, Math.min(energy, this.capacity));
        }

        public int addEnergy(int amount, boolean simulate) {
            if (amount <= 0) {
                return 0;
            }
            int added = Math.min(amount, this.capacity - this.energy);
            if (added <= 0) {
                return 0;
            }
            if (!simulate) {
                this.energy += added;
            }
            return added;
        }
    }

    public static final int FIELD_ENERGY_LO = 0;
    public static final int FIELD_ENERGY_HI = 1;
    public static final int FIELD_WIRELESS = 2;
    public static final int FIELD_WORKING = 3;
    public static final int FIELD_BURNING = 4;

    protected EnergyStorage storage;
    protected boolean wireless = true;
    protected boolean working;
    private int displayEnergy;
    protected boolean displayBurning;

    public final ItemStackHandler chargeHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    public final ItemStackHandler chargeOutputHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    public abstract int getTier();

    public abstract PropertyBool getWorkingProperty();

    protected abstract LingLiEnergyStorage createStorage(int tier);

    public int getEnergyStored() {
        return storage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return storage.getMaxEnergyStored();
    }

    public int getGuiEnergy() {
        return world != null && world.isRemote ? displayEnergy : getEnergyStored();
    }

    public boolean isWireless() {
        return wireless;
    }

    public boolean isWorking() {
        return working;
    }

    public void setWireless(boolean wireless) {
        if (this.wireless == wireless) {
            return;
        }
        this.wireless = wireless;
        markDirty();
    }

    public void restoreEnergy(int energy) {
        ((LingLiEnergyStorage) storage).setEnergyStored(energy);
        markDirty();
    }

    public void setWorking(boolean working) {
        if (this.working == working) {
            return;
        }
        this.working = working;
        IBlockState state = world.getBlockState(pos);
        world.setBlockState(pos, state.withProperty(getWorkingProperty(), working), 3);
        markDirty();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(storage);
        }
        return super.getCapability(capability, facing);
    }

    protected int wirelessSupply(int maxSend) {
        if (world == null || world.isRemote) {
            return 0;
        }
        if (storage.getEnergyStored() <= 0 || maxSend <= 0) {
            return 0;
        }
        List<IEnergyStorage> targets = new ArrayList<>();
        int gapSum = 0;
        int range = LingLiConstants.WIRELESS_RANGE;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos scanPos = pos.add(dx, dy, dz);
                    TileEntity tile = world.getTileEntity(scanPos);
                    if (tile == null || tile == this || tile.isInvalid()) {
                        continue;
                    }
                    IEnergyStorage cap = null;
                    if (tile.hasCapability(CapabilityEnergy.ENERGY, null)) {
                        cap = tile.getCapability(CapabilityEnergy.ENERGY, null);
                    }
                    if (cap == null) {
                        for (EnumFacing f : EnumFacing.values()) {
                            if (tile.hasCapability(CapabilityEnergy.ENERGY, f)) {
                                cap = tile.getCapability(CapabilityEnergy.ENERGY, f);
                                if (cap != null) {
                                    break;
                                }
                            }
                        }
                    }
                    if (cap == null || !cap.canReceive()) {
                        continue;
                    }
                    int gap = cap.getMaxEnergyStored() - cap.getEnergyStored();
                    if (gap <= 0) {
                        continue;
                    }
                    targets.add(cap);
                    gapSum += gap;
                }
            }
        }
        if (targets.isEmpty()) {
            return 0;
        }
        int total = Math.min(storage.extractEnergy(maxSend, true), gapSum);
        if (total <= 0) {
            return 0;
        }
        int sent = 0;
        int part = total / targets.size();
        for (IEnergyStorage target : targets) {
            int received = target.receiveEnergy(part, false);
            if (received > 0) {
                sent += received;
                storage.extractEnergy(received, false);
            }
        }
        if (sent < total) {
            int leftover = total - sent;
            for (IEnergyStorage target : targets) {
                if (leftover <= 0) {
                    break;
                }
                int received = target.receiveEnergy(leftover, false);
                if (received > 0) {
                    sent += received;
                    leftover -= received;
                    storage.extractEnergy(received, false);
                }
            }
        }
        return sent;
    }

    protected void tickCharge(int rate) {
        ItemStack in = chargeHandler.getStackInSlot(0);
        if (in.isEmpty()) {
            return;
        }
        IEnergyStorage cap = in.getCapability(CapabilityEnergy.ENERGY, null);
        if (cap == null) {
            return;
        }
        if (cap.getEnergyStored() < cap.getMaxEnergyStored() && storage.getEnergyStored() > 0) {
            int want = Math.min(rate, cap.getMaxEnergyStored() - cap.getEnergyStored());
            int available = storage.extractEnergy(want, true);
            if (available > 0) {
                int received = cap.receiveEnergy(available, false);
                if (received > 0) {
                    storage.extractEnergy(received, false);
                }
            }
        }
        if (cap.getEnergyStored() >= cap.getMaxEnergyStored()) {
            ItemStack out = chargeOutputHandler.getStackInSlot(0);
            if (out.isEmpty()) {
                chargeOutputHandler.setStackInSlot(0, in.copy());
                chargeHandler.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
    }

    public boolean getBurning() {
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound tag = super.writeToNBT(compound);
        tag.setInteger("Energy", storage.getEnergyStored());
        tag.setBoolean("Wireless", wireless);
        tag.setBoolean("Working", working);
        tag.setTag("Charge", chargeHandler.serializeNBT());
        tag.setTag("ChargeOut", chargeOutputHandler.serializeNBT());
        writeExtraNBT(tag);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.storage = createStorage(getTier());
        ((LingLiEnergyStorage) this.storage).setEnergyStored(compound.getInteger("Energy"));
        this.wireless = compound.getBoolean("Wireless");
        this.working = compound.getBoolean("Working");
        chargeHandler.deserializeNBT(compound.getCompoundTag("Charge"));
        chargeOutputHandler.deserializeNBT(compound.getCompoundTag("ChargeOut"));
        readExtraNBT(compound);
    }

    protected void writeExtraNBT(NBTTagCompound tag) {
    }

    protected void readExtraNBT(NBTTagCompound compound) {
    }

    public int getField(int id) {
        switch (id) {
            case FIELD_ENERGY_LO:
                return getEnergyStored() & 0xFFFF;
            case FIELD_ENERGY_HI:
                return (getEnergyStored() >>> 16) & 0xFFFF;
            case FIELD_WIRELESS:
                return wireless ? 1 : 0;
            case FIELD_WORKING:
                return working ? 1 : 0;
            default:
                return 0;
        }
    }

    public void setField(int id, int value) {
        switch (id) {
            case FIELD_ENERGY_LO:
                displayEnergy = (displayEnergy & 0xFFFF0000) | (value & 0xFFFF);
                break;
            case FIELD_ENERGY_HI:
                displayEnergy = (displayEnergy & 0x0000FFFF) | ((value & 0xFFFF) << 16);
                break;
            case FIELD_WIRELESS:
                wireless = value != 0;
                break;
            case FIELD_WORKING:
                working = value != 0;
                break;
            default:
                break;
        }
    }

    public int getFieldCount() {
        return 4;
    }
}