package com.frxx.mengzhi.lingli.tile;

import com.frxx.mengzhi.lingli.LingLiConstants;
import com.frxx.mengzhi.lingli.block.BlockLingLiGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

public class TileLingLiGenerator extends TileLingLiBase {

    private int tier = 1;
    private int burnTicks;
    private int burnResidual;
    private int burnPerTick;

    public final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, net.minecraft.item.ItemStack stack) {
            return LingLiConstants.isFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    public TileLingLiGenerator() {
        this.storage = createStorage(getTier());
    }

    public TileLingLiGenerator(int tier) {
        this.tier = tier;
        this.storage = createStorage(getTier());
    }

    public static TileLingLiGenerator create(int tier) {
        switch (tier) {
            case 2:
                return new TileLingLiGenerator2();
            case 3:
                return new TileLingLiGenerator3();
            case 4:
                return new TileLingLiGenerator4();
            case 5:
                return new TileLingLiGenerator5();
            default:
                return new TileLingLiGenerator1();
        }
    }

    @Override
    public int getTier() {
        if (tier <= 0) {
            Block block = getBlockType();
            if (block instanceof BlockLingLiGenerator) {
                tier = ((BlockLingLiGenerator) block).getTier();
            }
        }
        return Math.max(1, Math.min(5, tier));
    }

    @Override
    public PropertyBool getWorkingProperty() {
        return BlockLingLiGenerator.WORKING;
    }

    @Override
    protected LingLiEnergyStorage createStorage(int t) {
        int idx = Math.max(1, Math.min(5, t)) - 1;
        return new LingLiEnergyStorage(LingLiConstants.GENERATOR_CAPACITY[idx], 0, LingLiConstants.GENERATOR_MAX_OUTPUT[idx]);
    }

    public int getMaxOutput() {
        return LingLiConstants.GENERATOR_MAX_OUTPUT[getTier() - 1];
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        int gap = storage.getMaxEnergyStored() - storage.getEnergyStored();
        if (gap > 0) {
            if (burnResidual <= 0 && !fuelHandler.getStackInSlot(0).isEmpty()) {
                int value = LingLiConstants.fuelValue(fuelHandler.getStackInSlot(0));
                if (value > 0) {
                    fuelHandler.extractItem(0, 1, false);
                    int duration = LingLiConstants.GENERATOR_BURN_TICKS[getTier() - 1];
                    burnResidual = value;
                    burnPerTick = Math.max(1, (value + duration - 1) / duration);
                    burnTicks = duration;
                    markDirty();
                }
            }
            if (burnResidual > 0) {
                int amount = Math.min(burnResidual, burnPerTick);
                int added = ((LingLiEnergyStorage) storage).addEnergy(amount, false);
                burnResidual -= added;
                if (burnResidual <= 0) {
                    burnTicks = 0;
                } else if (burnTicks > 0) {
                    burnTicks--;
                }
                markDirty();
            }
        } else if (burnResidual <= 0) {
            burnTicks = 0;
        }
        boolean hadEnergy = storage.getEnergyStored() > 0;
        boolean outputting = false;
        if (wireless && hadEnergy) {
            outputting = wirelessSupply(getMaxOutput()) > 0;
        }
        setWorking(hadEnergy && outputting);
        tickCharge(getMaxOutput());
    }

    @Override
    public int getFieldCount() {
        return 5;
    }

    @Override
    public int getField(int id) {
        if (id == FIELD_BURNING) {
            return burnTicks > 0 ? 1 : 0;
        }
        return super.getField(id);
    }

    @Override
    public void setField(int id, int value) {
        if (id == FIELD_BURNING) {
            displayBurning = value != 0;
            return;
        }
        super.setField(id, value);
    }

    @Override
    public boolean getBurning() {
        return world != null && world.isRemote ? displayBurning : burnTicks > 0;
    }

    @Override
    protected void writeExtraNBT(NBTTagCompound tag) {
        tag.setTag("Fuel", fuelHandler.serializeNBT());
        tag.setInteger("BurnTicks", burnTicks);
        tag.setInteger("BurnResidual", burnResidual);
        tag.setInteger("BurnPerTick", burnPerTick);
    }

    @Override
    protected void readExtraNBT(NBTTagCompound compound) {
        fuelHandler.deserializeNBT(compound.getCompoundTag("Fuel"));
        burnTicks = compound.getInteger("BurnTicks");
        burnResidual = compound.getInteger("BurnResidual");
        burnPerTick = compound.getInteger("BurnPerTick");
        if (burnPerTick <= 0 && burnResidual > 0) {
            burnPerTick = Math.max(1, burnResidual);
        }
    }
}