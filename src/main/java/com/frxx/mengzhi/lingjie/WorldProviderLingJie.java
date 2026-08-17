package com.frxx.mengzhi.lingjie;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;

public class WorldProviderLingJie extends WorldProvider {

    @Override
    public DimensionType getDimensionType() {
        return DimensionType.byName("LINGJIE");
    }

    @Override
    public String getSaveFolder() {
        return "LingJie";
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public boolean isSurfaceWorld() {
        return true;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorOverworld(this.world, this.world.getSeed(), true, "");
    }
}
