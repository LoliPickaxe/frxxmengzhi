package com.frxx.mengzhi.command;

import com.frxx.mengzhi.handler.TiandaoHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Collections;
import java.util.List;

/**
 * 天道境界指令：/frxxrealm <境界> [玩家]
 * 直接写入玩家 NBT 的 JingJieNum，不受 base 模组 1~11 的限制，
 * 数值 >= 12 即激活天道（第十二境界）。
 */
public class TiandaoRealmCommand extends CommandBase {

    @Override
    public String getName() {
        return "frxxrealm";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("frr");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/frxxrealm <境界> [玩家名]";
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
        EntityPlayer target;
        if (args.length >= 2) {
            EntityPlayer found = (EntityPlayer) getPlayer(server, sender, args[1]);
            target = found;
        } else {
            if (!(sender instanceof EntityPlayer)) {
                throw new WrongUsageException("请指定玩家名：/frxxrealm <境界> <玩家名>");
            }
            target = (EntityPlayer) sender;
        }

        double level = Math.max(1.0, parseDouble(args[0]));
        TiandaoHandler.syncRealmDisplay(target, level);

        String msg = "已设为 " + (int) level;
        if (level >= TiandaoHandler.TIANDAO_REALM) {
            msg += TextFormatting.GOLD + " —— 天道已激活（第十二境界）！";
        }
        target.sendMessage(new TextComponentString(TextFormatting.AQUA + "【梦指】" + target.getName() + " 的境界" + msg));
        if (target != sender) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "已将 " + target.getName() + " 的境界设为 " + (int) level));
        }
    }
}