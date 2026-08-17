package com.frxx.mengzhi.command;

import com.frxx.mengzhi.handler.ShieldOverflowData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Collections;
import java.util.List;

/**
 * 护盾规则指令：/frxxshield overflow <on|off|query>
 *  - on   （默认）：护盾被击穿后剩余溢出伤害照常造成伤害（/kill 巨量伤害会真实击杀）；
 *  - off ：旧行为，护盾全额吸收当前一击，溢出伤害消失。
 */
public class ShieldOverflowCommand extends CommandBase {

    @Override
    public String getName() {
        return "frxxshield";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("fsh");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/frxxshield overflow <on|off|query>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        if (!"overflow".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getUsage(sender));
        }
        if (args.length < 2) {
            throw new WrongUsageException(getUsage(sender));
        }
        ShieldOverflowData data = ShieldOverflowData.get(sender.getEntityWorld());
        String opt = args[1].toLowerCase();
        if ("on".equals(opt)) {
            data.setRule(ShieldOverflowData.RULE_OVERFLOW_APPLIES);
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA
                + "【护盾规则】溢出伤害计算已开启：护盾被击穿后，剩余伤害照常造成（默认）"));
        } else if ("off".equals(opt)) {
            data.setRule(ShieldOverflowData.RULE_SHIELD_ABSORBS_ALL);
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA
                + "【护盾规则】已关闭：护盾全额吸收当前一击，溢出伤害消失（旧行为）"));
        } else if ("query".equals(opt)) {
            boolean overflow = data.getRule() == ShieldOverflowData.RULE_OVERFLOW_APPLIES;
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA
                + "【护盾规则】当前："
                + (overflow ? "溢出伤害计算（护盾击穿后剩余伤害照常造成）"
                    : "全额吸收（溢出伤害消失）")));
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }
}