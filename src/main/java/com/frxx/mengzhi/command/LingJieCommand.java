package com.frxx.mengzhi.command;

import com.frxx.mengzhi.lingjie.LingJieDimension;
import com.frxx.mengzhi.lingjie.LingJieTeleport;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Collections;
import java.util.List;

/**
 * 灵界传送指令：/frxxlingjie [玩家名]
 */
public class LingJieCommand extends CommandBase {

    @Override
    public String getName() {
        return "frxxlingjie";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("flj");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/frxxlingjie [玩家名]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP target;
        if (args.length >= 1) {
            target = (EntityPlayerMP) getPlayer(server, sender, args[0]);
        } else {
            if (!(sender instanceof EntityPlayerMP)) {
                throw new WrongUsageException(getUsage(sender));
            }
            target = (EntityPlayerMP) sender;
        }

        int id = LingJieDimension.DIMENSION_ID;
        if (target.dimension != id) {
            if (LingJieTeleport.to(target, id)) {
                target.sendMessage(new TextComponentString(TextFormatting.AQUA + "【梦指】已传送至灵界（维度 " + id + "）"));
            } else {
                target.sendMessage(new TextComponentString(TextFormatting.RED + "【梦指】传送失败：灵界世界加载异常，请查看服务器日志"));
            }
        } else {
            if (LingJieTeleport.to(target, 0)) {
                target.sendMessage(new TextComponentString(TextFormatting.AQUA + "【梦指】已返回人界（维度 0）"));
            } else {
                target.sendMessage(new TextComponentString(TextFormatting.RED + "【梦指】返回人界失败，请查看服务器日志"));
            }
        }
    }
}
