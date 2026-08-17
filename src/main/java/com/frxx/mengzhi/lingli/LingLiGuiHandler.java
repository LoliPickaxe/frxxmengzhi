package com.frxx.mengzhi.lingli;

import com.frxx.mengzhi.lingli.gui.ContainerChuDianQi;
import com.frxx.mengzhi.lingli.gui.ContainerLingLiGenerator;
import com.frxx.mengzhi.lingli.gui.GuiChuDianQi;
import com.frxx.mengzhi.lingli.gui.GuiLingLiGenerator;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LingLiGuiHandler implements IGuiHandler {

    public static final int GUI_ID_GENERATOR = 0;
    public static final int GUI_ID_STORAGE = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        switch (id) {
            case GUI_ID_GENERATOR:
                return tile instanceof TileLingLiGenerator
                    ? new ContainerLingLiGenerator(player.inventory, (TileLingLiGenerator) tile)
                    : null;
            case GUI_ID_STORAGE:
                return tile instanceof TileChuDianQi
                    ? new ContainerChuDianQi(player.inventory, (TileChuDianQi) tile)
                    : null;
            default:
                return null;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        switch (id) {
            case GUI_ID_GENERATOR:
                return tile instanceof TileLingLiGenerator
                    ? new GuiLingLiGenerator((TileLingLiGenerator) tile)
                    : null;
            case GUI_ID_STORAGE:
                return tile instanceof TileChuDianQi
                    ? new GuiChuDianQi((TileChuDianQi) tile)
                    : null;
            default:
                return null;
        }
    }
}