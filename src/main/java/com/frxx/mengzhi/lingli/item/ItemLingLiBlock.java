package com.frxx.mengzhi.lingli.item;

import com.frxx.mengzhi.lingli.LingLiConstants;
import com.frxx.mengzhi.lingli.block.BlockChuDianQi;
import com.frxx.mengzhi.lingli.block.BlockLingLiGenerator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemLingLiBlock extends ItemBlock {

    public ItemLingLiBlock(Block block) {
        super(block);
        setMaxStackSize(1);
    }

    public int getStoredEnergy(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return 0;
        }
        if (tag.hasKey("LingLiEnergy")) {
            return tag.getInteger("LingLiEnergy");
        }
        NBTTagCompound teTag = tag.getCompoundTag("BlockEntityTag");
        return teTag == null ? 0 : teTag.getInteger("Energy");
    }

    public int getMaxEnergy(ItemStack stack) {
        Block block = getBlock();
        if (block instanceof BlockLingLiGenerator) {
            return LingLiConstants.GENERATOR_CAPACITY[((BlockLingLiGenerator) block).getTier() - 1];
        }
        if (block instanceof BlockChuDianQi) {
            return LingLiConstants.STORAGE_CAPACITY[((BlockChuDianQi) block).getTier() - 1];
        }
        return 0;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getStoredEnergy(stack) > 0 || getStoredEnergy(stack) < getMaxEnergy(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        int max = getMaxEnergy(stack);
        if (max <= 0) {
            return 1.0D;
        }
        return 1.0D - getStoredEnergy(stack) / (double) max;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0x3399FF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.YELLOW + "FE: " + getStoredEnergy(stack) + " / " + getMaxEnergy(stack));
        tooltip.add(TextFormatting.GRAY + "破坏后电力与物品自动保留");
    }
}
