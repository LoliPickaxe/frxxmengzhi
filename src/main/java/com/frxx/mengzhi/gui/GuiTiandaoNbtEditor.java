package com.frxx.mengzhi.gui;

import com.frxx.mengzhi.FanRenXiuXianMengZhi;
import com.frxx.mengzhi.handler.TiandaoClientHandler;
import com.frxx.mengzhi.network.TiandaoNbtDocPacket;
import com.frxx.mengzhi.network.TiandaoPanelPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 天道 · NBT 修改器：打开即获取目标全部 NBT，像文本文档一样编辑（可修改、可复制）。
 * 每行格式：键 = 值（字符串带双引号；数字/布尔直接写）。
 */
@SideOnly(Side.CLIENT)
public class GuiTiandaoNbtEditor extends GuiScreen {

    private static final int LINE_HEIGHT = 12;

    private List<String> lines = new ArrayList<String>(Arrays.asList(""));
    private int curLine = 0;
    private int curCol = 0;
    private int scroll = 0;
    private boolean fetched = false;
    private String docTarget = "";

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height - 26, 110, 20,
            TextFormatting.GREEN + "保存并应用"));
        buttonList.add(new GuiButton(1, width / 2 + 14, height - 26, 110, 20,
            TextFormatting.GRAY + "返回面板 (Esc)"));
        requestDoc();
    }

    /** 向服务端请求目标全部 NBT */
    private void requestDoc() {
        int id = TiandaoClientHandler.currentTargetId;
        if (TiandaoNbtDocPacket.cachedTargetId == id || id == -1) {
            docTarget = id == -1 ? "自身" : TiandaoNbtDocPacket.cachedName;
        } else {
            docTarget = "";
        }
        FanRenXiuXianMengZhi.NETWORK.sendToServer(
            new TiandaoPanelPacket(TiandaoPanelPacket.ACTION_FETCH_NBT_DOC, id, 0, 0.0));
    }

    private void backToPanel() {
        mc.displayGuiScreen(new GuiTiandaoPanel());
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // 等待服务端返回文档
        if (!fetched && TiandaoNbtDocPacket.cachedTargetId == TiandaoClientHandler.currentTargetId
            && TiandaoNbtDocPacket.cachedTargetId != -1) {
            docTarget = TiandaoNbtDocPacket.cachedName;
            lines.clear();
            String doc = TiandaoNbtDocPacket.cachedDoc;
            if (doc == null || doc.isEmpty()) {
                lines.add("");
            } else {
                String[] arr = doc.split("\n", -1);
                lines.addAll(Arrays.asList(arr));
            }
            curLine = Math.min(curLine, Math.max(0, lines.size() - 1));
            curCol = 0;
            scroll = 0;
            fetched = true;
        }
    }

    private boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            backToPanel();
            return;
        }
        if (isCtrlDown() && keyCode == Keyboard.KEY_C) {
            copyAll();
            return;
        }
        if (isCtrlDown() && keyCode == Keyboard.KEY_V) {
            pasteAll();
            return;
        }
        if (isCtrlDown() && keyCode == Keyboard.KEY_X) {
            copyAll();
            return;
        }
        if (isCtrlDown() && keyCode == Keyboard.KEY_S) {
            saveAll();
            return;
        }
        if (keyCode == Keyboard.KEY_LEFT) {
            moveLeft();
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            moveRight();
            return;
        }
        if (keyCode == Keyboard.KEY_UP) {
            moveUp();
            return;
        }
        if (keyCode == Keyboard.KEY_DOWN) {
            moveDown();
            return;
        }
        if (keyCode == Keyboard.KEY_HOME) {
            curCol = 0;
            return;
        }
        if (keyCode == Keyboard.KEY_END) {
            curCol = lines.get(curLine).length();
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            backspace();
            return;
        }
        if (keyCode == Keyboard.KEY_DELETE) {
            delete();
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN) {
            insertNewline();
            return;
        }
        if (typedChar >= ' ') {
            insertChar(typedChar);
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void insertChar(char c) {
        String line = lines.get(curLine);
        StringBuilder sb = new StringBuilder(line);
        sb.insert(Math.min(curCol, sb.length()), c);
        lines.set(curLine, sb.toString());
        curCol++;
    }

    private void insertNewline() {
        String line = lines.get(curLine);
        String tail = line.substring(Math.min(curCol, line.length()));
        String head = line.substring(0, Math.min(curCol, line.length()));
        lines.set(curLine, head);
        if (curCol >= head.length()) {
            curLine++;
            lines.add(curLine, tail);
            curCol = 0;
        }
    }

    private void backspace() {
        if (curCol > 0) {
            String line = lines.get(curLine);
            line = line.substring(0, curCol - 1) + line.substring(curCol);
            lines.set(curLine, line);
            curCol--;
        } else if (curLine > 0) {
            String prev = lines.get(curLine - 1);
            String cur = lines.get(curLine);
            lines.set(curLine - 1, prev + cur);
            lines.remove(curLine);
            curLine--;
            curCol = prev.length();
        }
    }

    private void delete() {
        String line = lines.get(curLine);
        if (curCol < line.length()) {
            lines.set(curLine, line.substring(0, curCol) + line.substring(curCol + 1));
        } else if (curLine < lines.size() - 1) {
            lines.set(curLine, line + lines.get(curLine + 1));
            lines.remove(curLine + 1);
        }
    }

    private void moveLeft() {
        if (curCol > 0) {
            curCol--;
        } else if (curLine > 0) {
            curLine--;
            curCol = lines.get(curLine).length();
        }
    }

    private void moveRight() {
        String line = lines.get(curLine);
        if (curCol < line.length()) {
            curCol++;
        } else if (curLine < lines.size() - 1) {
            curLine++;
            curCol = 0;
        }
    }

    private void moveUp() {
        if (curLine > 0) {
            curLine--;
            curCol = Math.min(curCol, lines.get(curLine).length());
        }
    }

    private void moveDown() {
        if (curLine < lines.size() - 1) {
            curLine++;
            curCol = Math.min(curCol, lines.get(curLine).length());
        }
    }

    private String wholeText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    /** 复制全部（Ctrl+A 全选后 Ctrl+C 复制整篇） */
    private void copyAll() {
        try {
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            clip.setContents(new StringSelection(wholeText()), null);
        } catch (Exception ignored) {
        }
    }

    private void pasteAll() {
        try {
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            String text = (String) clip.getData(DataFlavor.stringFlavor);
            if (text != null && !text.isEmpty()) {
                lines.clear();
                String[] arr = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
                lines.addAll(Arrays.asList(arr));
                if (lines.isEmpty()) {
                    lines.add("");
                }
                curLine = 0;
                curCol = 0;
                scroll = 0;
            }
        } catch (Exception ignored) {
        }
    }

    private void saveAll() {
        if (TiandaoClientHandler.currentTargetId == -1) {
            mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.RED + "【天道】请先在面板锁定目标"));
            return;
        }
        FanRenXiuXianMengZhi.NETWORK.sendToServer(
            new TiandaoPanelPacket(TiandaoPanelPacket.ACTION_SAVE_NBT_DOC,
                TiandaoClientHandler.currentTargetId, 0, 0.0, "doc", wholeText()));
        backToPanel();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            saveAll();
        } else if (button.id == 1) {
            backToPanel();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer,
            TextFormatting.GOLD + "天道 · NBT 修改器 —— 目标: "
                + (docTarget.isEmpty() ? TextFormatting.YELLOW + "获取中…" : TextFormatting.YELLOW + docTarget),
            width / 2, 6, 0xFFFFFF);
        drawString(fontRenderer, "每行格式： 键 = 值   字符串值加双引号，数字/布尔直接写",
            width / 2, 20, 0x888888);

        int top = 34;
        int bottom = height - 34;
        int visible = (bottom - top) / LINE_HEIGHT;
        if (curLine < scroll) scroll = curLine;
        if (curLine >= scroll + visible) scroll = curLine - visible + 1;
        if (scroll < 0) scroll = 0;

        drawRect(8, top, width - 8, bottom, 0xAA000000);
        for (int i = 0; i < visible && scroll + i < lines.size(); i++) {
            String text = fontRenderer.trimStringToWidth(lines.get(scroll + i), width - 24);
            drawString(fontRenderer, text, 12, top + i * LINE_HEIGHT + 1, 0xDDDDDD);
        }

        // 光标闪烁
        if ((System.currentTimeMillis() / 500) % 2 == 0 && curLine >= scroll && curLine < scroll + visible) {
            String line = lines.get(curLine);
            int x = 12 + fontRenderer.getStringWidth(line.substring(0, Math.min(curCol, line.length())));
            int y = top + (curLine - scroll) * LINE_HEIGHT + 2;
            drawRect(x, y, x + 1, y + LINE_HEIGHT - 3, 0xFFFFFFFF);
        }

        drawCenteredString(fontRenderer,
            "Ctrl+A 全选 / Ctrl+C 复制 / Ctrl+V 粘贴（整篇文档），可自由修改任意键值",
            width / 2, height - 14, 0x999999);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            pasteAll();
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}