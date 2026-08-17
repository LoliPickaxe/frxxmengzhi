package com.frxx.mengzhi.lingli.tile;

import com.frxx.mengzhi.lingli.LingLiConstants;
import com.frxx.mengzhi.lingli.block.BlockChuDianQi;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;

public class TileChuDianQi extends TileLingLiBase {

    private int tier = 1;

    public TileChuDianQi() {
        this.storage = createStorage(getTier());
    }

    public TileChuDianQi(int tier) {
        this.tier = tier;
        this.storage = createStorage(getTier());
    }

    public static TileChuDianQi create(int tier) {
        switch (tier) {
            case 2:
                return new TileChuDianQi2();
            case 3:
                return new TileChuDianQi3();
            case 4:
                return new TileChuDianQi4();
            case 5:
                return new TileChuDianQi5();
            default:
                return new TileChuDianQi1();
        }
    }

    @Override
    public int getTier() {
        if (tier <= 0) {
            Block block = getBlockType();
            if (block instanceof BlockChuDianQi) {
                tier = ((BlockChuDianQi) block).getTier();
            }
        }
        return Math.max(1, Math.min(5, tier));
    }

    @Override
    public PropertyBool getWorkingProperty() {
        return BlockChuDianQi.WORKING;
    }

    @Override
    protected LingLiEnergyStorage createStorage(int t) {
        int idx = Math.max(1, Math.min(5, t)) - 1;
        return new LingLiEnergyStorage(LingLiConstants.STORAGE_CAPACITY[idx], LingLiConstants.STORAGE_RATE[idx], LingLiConstants.STORAGE_RATE[idx]);
    }

    public int getTransferRate() {
        return LingLiConstants.STORAGE_RATE[getTier() - 1];
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        if (wireless && storage.getEnergyStored() > 0) {
            wirelessSupply(getTransferRate());
        }
        tickCharge(getTransferRate());
        setWorking(wireless);
    }
}