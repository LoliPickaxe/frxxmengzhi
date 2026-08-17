package com.frxx.mengzhi.lingli.gui;

import com.frxx.mengzhi.lingli.tile.TileChuDianQi;
import com.frxx.mengzhi.lingli.tile.TileLingLiBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerChuDianQi extends Container {

    public static final int CHARGE_IN_SLOT = 0;
    public static final int CHARGE_OUT_SLOT = 1;

    private final TileChuDianQi te;
    private int lastEnergyLo = Integer.MIN_VALUE;
    private int lastEnergyHi = Integer.MIN_VALUE;
    private int lastWireless = Integer.MIN_VALUE;
    private int lastWorking = Integer.MIN_VALUE;

    public ContainerChuDianQi(InventoryPlayer playerInventory, TileChuDianQi te) {
        this.te = te;
        addSlotToContainer(new SlotItemHandler(te.chargeHandler, 0, 134, 17) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null);
            }
        });
        addSlotToContainer(new SlotItemHandler(te.chargeOutputHandler, 0, 134, 53) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null);
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
            if (index == CHARGE_IN_SLOT || index == CHARGE_OUT_SLOT) {
                if (!this.mergeItemStack(stack, 2, 38, true)) {
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