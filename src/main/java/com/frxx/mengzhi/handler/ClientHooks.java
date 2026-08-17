package com.frxx.mengzhi.handler;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientHooks {

    public static KeyBinding keyGuardToggle;
    public static KeyBinding keyGuardCharge;
    public static KeyBinding keySelfDestructPanel;
    public static KeyBinding keySelfDestructExecute;
    public static KeyBinding keyShenShiPressure;
    public static KeyBinding keyHandMining;
    public static KeyBinding keySpiritBomb;
    public static KeyBinding keySpiritBolt;
    public static KeyBinding keySpiritBoltFire;
    public static KeyBinding keyProjectilePanel;
    public static KeyBinding keyGangQiToggle;
    public static KeyBinding keyGangQiMode;
    public static KeyBinding keyCaoKongToggle;
    public static KeyBinding keyDunGuang;
    public static KeyBinding keyTiandaoPanel;
    public static KeyBinding keyRepulsion;

    public static void registerKeyBindings() {
        keyGuardToggle = new KeyBinding("key.frxxmengzhi.guard_toggle", Keyboard.KEY_X, "key.categories.frxxmengzhi");
        keyGuardCharge = new KeyBinding("key.frxxmengzhi.guard_charge", Keyboard.KEY_C, "key.categories.frxxmengzhi");
        keySelfDestructPanel = new KeyBinding("key.frxxmengzhi.selfdestruct_panel", Keyboard.KEY_V, "key.categories.frxxmengzhi");
        keySelfDestructExecute = new KeyBinding("key.frxxmengzhi.selfdestruct_execute", Keyboard.KEY_G, "key.categories.frxxmengzhi");
        keyShenShiPressure = new KeyBinding("key.frxxmengzhi.shenshi_pressure", Keyboard.KEY_Z, "key.categories.frxxmengzhi");
        keyHandMining = new KeyBinding("key.frxxmengzhi.hand_mining", Keyboard.KEY_J, "key.categories.frxxmengzhi");
        keySpiritBomb = new KeyBinding("key.frxxmengzhi.spirit_bomb", Keyboard.KEY_N, "key.categories.frxxmengzhi");
        keySpiritBolt = new KeyBinding("key.frxxmengzhi.spirit_bolt", Keyboard.KEY_NUMPAD1, "key.categories.frxxmengzhi");
        keySpiritBoltFire = new KeyBinding("key.frxxmengzhi.spirit_bolt_fire", -99, "key.categories.frxxmengzhi");
        keyProjectilePanel = new KeyBinding("key.frxxmengzhi.projectile_panel", Keyboard.KEY_3, "key.categories.frxxmengzhi");
        keyGangQiToggle = new KeyBinding("key.frxxmengzhi.gangqi_toggle", Keyboard.KEY_NUMPAD5, "key.categories.frxxmengzhi");
        keyGangQiMode = new KeyBinding("key.frxxmengzhi.gangqi_mode", Keyboard.KEY_NUMPAD6, "key.categories.frxxmengzhi");
        keyCaoKongToggle = new KeyBinding("key.frxxmengzhi.caokong_toggle", Keyboard.KEY_BACKSLASH, "key.categories.frxxmengzhi");
        keyDunGuang = new KeyBinding("key.frxxmengzhi.dunguang_toggle", Keyboard.KEY_H, "key.categories.frxxmengzhi");
        keyTiandaoPanel = new KeyBinding("key.frxxmengzhi.tiandao_panel", Keyboard.KEY_DECIMAL, "key.categories.frxxmengzhi");
        keyRepulsion = new KeyBinding("key.frxxmengzhi.repulsion", Keyboard.KEY_NUMPAD8, "key.categories.frxxmengzhi");
        ClientRegistry.registerKeyBinding(keyGuardToggle);
        ClientRegistry.registerKeyBinding(keyGuardCharge);
        ClientRegistry.registerKeyBinding(keySelfDestructPanel);
        ClientRegistry.registerKeyBinding(keySelfDestructExecute);
        ClientRegistry.registerKeyBinding(keyShenShiPressure);
        ClientRegistry.registerKeyBinding(keyHandMining);
        ClientRegistry.registerKeyBinding(keySpiritBomb);
        ClientRegistry.registerKeyBinding(keySpiritBolt);
        ClientRegistry.registerKeyBinding(keySpiritBoltFire);
        ClientRegistry.registerKeyBinding(keyProjectilePanel);
        ClientRegistry.registerKeyBinding(keyGangQiToggle);
        ClientRegistry.registerKeyBinding(keyGangQiMode);
        ClientRegistry.registerKeyBinding(keyCaoKongToggle);
        ClientRegistry.registerKeyBinding(keyDunGuang);
        ClientRegistry.registerKeyBinding(keyTiandaoPanel);
        ClientRegistry.registerKeyBinding(keyRepulsion);
    }
}
