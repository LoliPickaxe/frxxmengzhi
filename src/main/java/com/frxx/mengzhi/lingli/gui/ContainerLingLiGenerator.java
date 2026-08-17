package com.frxx.mengzhi.lingli.gui;

import com.frxx.mengzhi.lingli.LingLiConstants;
import com.frxx.mengzhi.lingli.tile.TileLingLiBase;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerLingLiGenerator extends Container {

    public static final int FUEL_SLOT = 0;
    public static final int CHARGE_IN_SLOT = 1;
    public static final int CHARGE_OUT_SLOT = 2;

    private final TileLingLiGenerator te;
    private int lastEnergyLo = Integer.MIN_VALUE;
    private int lastEnergyHi = Integer.MIN_VALUE;
    private int lastWireless = Integer.MIN_VALUE;
    private int lastWorking = Integer.MIN_VALUE;
    private int lastBurning = Integer.MIN_VALUE;

    public ContainerLingLiGenerator(InventoryPlayer playerInventory, TileLingLiGenerator te) {
        this.te = te;
        addSlotToContainer(new SlotItemHandler(te.chargeHandler, 0, 56, 17) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null);
            }
        });
        addSlotToContainer(new SlotItemHandler(te.chargeOutputHandler, 0, 116, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null);
            }
        });
        addSlotToContainer(new SlotItemHandler(te.fuelHandler, 0, 56, 53) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return LingLiConstants.isFuel(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public ItemStack getChargeStack() {
        return inventorySlots.get(CHARGE_IN_SLOT).getStack();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int id = 0; id < te.getFieldCount(); id++) {
            int value = te.getField(id);
            int last = fieldLastValue(id);
            if (value != last) {
                for (IContainerListener listener : this.listeners) {
                    listener.sendWindowProperty(this, id, value);
                }
                setFieldLastValue(id, value);
            }
        }
    }

    private int fieldLastValue(int id) {
        switch (id) {
            case TileLingLiBase.FIELD_ENERGY_LO:
                return lastEnergyLo;
            case TileLingLiBase.FIELD_ENERGY_HI:
                return lastEnergyHi;
            case TileLingLiBase.FIELD_WIRELESS:
                return lastWireless;
            case TileLingLiBase.FIELD_WORKING:
                return lastWorking;
            case TileLingLiBase.FIELD_BURNING:
                return lastBurning;
            default:
                return Integer.MIN_VALUE;
        }
    }

    private void setFieldLastValue(int id, int value) {
        switch (id) {
            case TileLingLiBase.FIELD_ENERGY_LO:
                lastEnergyLo = value;
                break;
            case TileLingLiBase.FIELD_ENERGY_HI:
                lastEnergyHi = value;
                break;
            case TileLingLiBase.FIELD_WIRELESS:
                lastWireless = value;
                break;
            case TileLingLiBase.FIELD_WORKING:
                lastWorking = value;
                break;
            case TileLingLiBase.FIELD_BURNING:
                lastBurning = value;
                break;
            default:
                break;
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        te.setField(id, data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.getDistanceSq(te.getPos().getX() + 0.5D, te.getPos().getY() + 0.5D, te.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            itemstack = stack.copy();
            boolean energyItem = !stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null);
            if (index == FUEL_SLOT) {
                if (!this.mergeItemStack(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == CHARGE_IN_SLOT || index == CHARGE_OUT_SLOT) {
                if (!this.mergeItemStack(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (LingLiConstants.isFuel(stack)) {
                if (!this.mergeItemStack(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (energyItem) {
                if (!this.mergeItemStack(stack, CHARGE_IN_SLOT, CHARGE_IN_SLOT + 1, false)) {
                    if (!this.mergeItemStack(stack, CHARGE_OUT_SLOT, CHARGE_OUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }
}