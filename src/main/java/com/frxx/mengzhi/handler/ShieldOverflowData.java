package com.frxx.mengzhi.handler;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/**
 * 护盾溢出伤害规则（全存档共享，存入 WorldSavedData）：
 *  - RULE_OVERFLOW_APPLIES（默认）：护盾被击穿后，剩余溢出伤害继续正常造成伤害
 *    （例：/kill 的巨量虚空伤害，护盾挡不住的部分会真实击杀玩家）；
 *  - RULE_SHIELD_ABSORBS_ALL（旧行为）：护盾全额吸收当前一击，
 *    溢出伤害直接消失不造成伤害。
 * 指令：/frxxshield overflow <on|off|query>
 */
public class ShieldOverflowData extends WorldSavedData {

    public static final String DATA_NAME = "FrxxShieldOverflow";
    public static final int RULE_OVERFLOW_APPLIES = 1;
    public static final int RULE_SHIELD_ABSORBS_ALL = 0;

    private int rule = RULE_OVERFLOW_APPLIES;

    public ShieldOverflowData() {
        super(DATA_NAME);
    }

    public ShieldOverflowData(String name) {
        super(name);
    }

    public static ShieldOverflowData get(World world) {
        ShieldOverflowData data = (ShieldOverflowData) world.loadData(ShieldOverflowData.class, DATA_NAME);
        if (data == null) {
            data = new ShieldOverflowData();
            data.markDirty();
            world.setData(DATA_NAME, data);
        }
        return data;
    }

    public int getRule() {
        return rule;
    }

    public void setRule(int rule) {
        this.rule = rule;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        rule = nbt.getInteger("Rule");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Rule", rule);
        return nbt;
    }
}