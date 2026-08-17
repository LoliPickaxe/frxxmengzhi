package com.frxx.mengzhi.lingli.gui;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.lingli.LingLiConstants;
import com.frxx.mengzhi.lingli.network.LingLiWirelessTogglePacket;
import com.frxx.mengzhi.lingli.tile.TileChuDianQi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiChuDianQi extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("lingli", "textures/gui/chudianqi.png");

    private final TileChuDianQi te;
    private final int tier;
    private GuiButton wirelessButton;

    public GuiChuDianQi(TileChuDianQi te) {
        super(new ContainerChuDianQi(Minecraft.getMinecraft().player.inventory, te));
        this.te = te;
        this.tier = te.getTier();
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.wirelessButton = new GuiButton(0, guiLeft + 16, guiTop + 60, 76, 20, "");
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
        this.fontRenderer.drawString("储电器·" + tier + "阶", 8, 6, 4210752);
        this.fontRenderer.drawString("FE: " + te.getGuiEnergy(), 16, 17, 4210752);
        this.fontRenderer.drawString("速率: " + LingLiConstants.STORAGE_RATE[tier - 1] + " FE/t", 16, 26, 4210752);
        this.fontRenderer.drawString(te.isWorking() ? "状态: 无线充电中" : "状态: 待机", 16, 35, 4210752);
        this.fontRenderer.drawString("充电格", 128, 8, 4210752);
        this.fontRenderer.drawString("满电取出", 128, 44, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        this.fontRenderer.drawString("↓", 141, 39, 0xFF8A8A8A);
        drawEnergyTank(guiLeft + 156, guiTop + 20, 10, 58);
    }

    private void drawEnergyTank(int x, int y, int width, int height) {
        int energy = te.getGuiEnergy();
        int max = te.getMaxEnergyStored();
        int fill = max > 0 ? (int) (height * energy / (float) max) : 0;
        if (fill > 0) {
            for (int i = 0; i < fill && i < height; i++) {
                float f = i / (float) height;
                int r = (int) (255 - f * 80);
                int g = (int) (200 - f * 60);
                int b = 40;
                drawRect(x, y + height - 1 - i, x + width, y + height - i, (0xFF << 24) | (r << 16) | (g << 8) | b);
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
        if (isPointInRegion(156, 20, 10, 58, mouseX, mouseY)) {
            this.drawHoveringText("FE: " + te.getGuiEnergy() + " / " + te.getMaxEnergyStored(), mouseX, mouseY);
        }
        if (isPointInRegion(134, 17, 18, 18, mouseX, mouseY)) {
            this.drawHoveringText("放入电池/电力物品，充满自动下移", mouseX, mouseY);
        }
        if (isPointInRegion(134, 53, 18, 18, mouseX, mouseY)) {
            this.drawHoveringText("取下已充满的物品", mouseX, mouseY);
        }
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
