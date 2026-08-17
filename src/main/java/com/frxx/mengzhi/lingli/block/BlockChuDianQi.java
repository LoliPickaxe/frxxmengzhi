package com.frxx.mengzhi.lingli.block;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.lingli.LingLiGuiHandler;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi;
import com.frxx.mengzhi.lingli.tile.TileLingLiBase;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockChuDianQi extends Block implements ITileEntityProvider {

    public static final String ENERGY_TAG = "LingLiEnergy";

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool WORKING = PropertyBool.create("working");

    private final int tier;

    public BlockChuDianQi(int tier) {
        super(Material.IRON);
        this.tier = tier;
        setHardness(2.5F);
        setSoundType(SoundType.METAL);
        setDefaultState(this.blockState.getBaseState()
            .withProperty(FACING, EnumFacing.NORTH)
            .withProperty(WORKING, false));
    }

    public int getTier() {
        return tier;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, WORKING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
            .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
            .withProperty(WORKING, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(WORKING) ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        world.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(ENERGY_TAG)) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileLingLiBase) {
                ((TileLingLiBase) tile).restoreEnergy(stack.getTagCompound().getInteger(ENERGY_TAG));
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.getHeldItem(hand).hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null)) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(FanRenXiuXianMengZhi.INSTANCE, LingLiGuiHandler.GUI_ID_STORAGE, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, net.minecraft.world.IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        super.getDrops(drops, world, pos, state, fortune);
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileChuDianQi && !drops.isEmpty()) {
            TileChuDianQi storage = (TileChuDianQi) tile;
            boolean hasEnergy = storage.getEnergyStored() > 0;
            boolean hasCharge = !storage.chargeHandler.getStackInSlot(0).isEmpty() || !storage.chargeOutputHandler.getStackInSlot(0).isEmpty();
            if (hasEnergy || hasCharge) {
                ItemStack stack = drops.get(0);
                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    stack.setTagCompound(tag);
                }
                if (hasEnergy) {
                    tag.setInteger(ENERGY_TAG, storage.getEnergyStored());
                }
                NBTTagCompound teTag = tile.writeToNBT(new NBTTagCompound());
                teTag.removeTag("x");
                teTag.removeTag("y");
                teTag.removeTag("z");
                teTag.removeTag("id");
                tag.setTag("BlockEntityTag", teTag);
            }
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return TileChuDianQi.create(tier);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }
}