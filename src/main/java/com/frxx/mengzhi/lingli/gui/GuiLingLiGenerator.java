package com.frxx.mengzhi.lingli.gui;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.lingli.LingLiConstants;
import com.frxx.mengzhi.lingli.network.LingLiWirelessTogglePacket;
import com.frxx.mengzhi.lingli.tile.TileLingLiGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiLingLiGenerator extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/furnace.png");

    private final TileLingLiGenerator te;
    private final int tier;
    private GuiButton wirelessButton;

    public GuiLingLiGenerator(TileLingLiGenerator te) {
        super(new ContainerLingLiGenerator(Minecraft.getMinecraft().player.inventory, te));
        this.te = te;
        this.tier = te.getTier();
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.wirelessButton = new GuiButton(0, guiLeft + 92, guiTop + 58, 76, 20, "");
        this.buttonList.add(this.wirelessButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            FanRenXiuXianMengZhi.NETWORK.sendToServer(new LingLiWirelessTogglePacket(te.getPos(), !te.isWireless()));
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString("灵力发电机·" + tier + "阶", 8, 6, 4210752);
        this.fontRenderer.drawString("FE: " + te.getGuiEnergy() + " / " + te.getMaxEnergyStored(), 8, 58, 4210752);
        this.fontRenderer.drawString("输出: " + LingLiConstants.GENERATOR_MAX_OUTPUT[tier - 1] + " FE/t", 8, 66, 4210752);
        this.fontRenderer.drawString(te.isWorking() ? "状态: 运行中" : "状态: 待机", 8, 74, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawEnergyBar(guiLeft + 8, guiTop + 16, 44, 5);
        if (te.getBurning()) {
            int i = 13;
            this.drawTexturedModalRect(guiLeft + 56, guiTop + 36 + 12 - i, 176, 12 - i, 14, i + 1);
        }
        drawChargeArrow();
    }

    private void drawChargeArrow() {
        int progress = 0;
        ItemStack stack = ((ContainerLingLiGenerator) this.inventorySlots).getChargeStack();
        if (!stack.isEmpty()) {
            IEnergyStorage cap = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (cap != null && cap.getMaxEnergyStored() > 0) {
                progress = cap.getEnergyStored() * 24 / cap.getMaxEnergyStored();
            }
        }
        if (progress > 0) {
            this.drawTexturedModalRect(guiLeft + 79, guiTop + 35, 176, 14, progress, 16);
        }
    }

    private void drawEnergyBar(int x, int y, int width, int height) {
        int energy = te.getGuiEnergy();
        int max = te.getMaxEnergyStored();
        int fill = max > 0 ? (int) (width * energy / (float) max) : 0;
        if (fill > 0) {
            for (int i = 0; i < fill && i < width; i++) {
                float f = i / (float) width;
                int r = (int) (255 - f * 80);
                int g = (int) (200 - f * 60);
                int b = 40;
                drawRect(x + i, y, x + i + 1, y + height, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        drawRect(x - 1, y - 1, x + width + 1, y, 0xFF404040);
        drawRect(x - 1, y + height, x + width + 1, y + height + 1, 0xFF404040);
        drawRect(x - 1, y - 1, x, y + height + 1, 0xFF404040);
        drawRect(x + width, y - 1, x + width + 1, y + height + 1, 0xFF404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.wirelessButton.displayString = te.isWireless() ? "无线供电: 开" : "无线供电: 关";
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (isPointInRegion(56, 17, 18, 18, mouseX, mouseY)) {
            this.drawHoveringText("电池充电格（充满自动移到右边）", mouseX, mouseY);
        }
        if (isPointInRegion(8, 16, 44, 5, mouseX, mouseY)) {
            this.drawHoveringText("FE: " + te.getGuiEnergy() + " / " + te.getMaxEnergyStored(), mouseX, mouseY);
        }
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}