package com.frxx.mengzhi.lingjie;

import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

public class LingJieDimension {

    public static final String NAME = "灵界";

    public static int DIMENSION_ID = -1;

    private LingJieDimension() {
    }

    public static void register() {
        DimensionType type = null;
        int id = DimensionManager.getNextFreeDimId();
        while (type == null) {
            if (!DimensionManager.isDimensionRegistered(id)) {
                try {
                    type = DimensionType.register("LINGJIE", "_lingjie", id, WorldProviderLingJie.class, false);
                } catch (IllegalArgumentException e) {
                    type = null;
                    id = DimensionManager.getNextFreeDimId();
                }
            } else {
                id = DimensionManager.getNextFreeDimId();
            }
        }
        DimensionManager.registerDimension(id, type);
        DIMENSION_ID = id;
    }
}