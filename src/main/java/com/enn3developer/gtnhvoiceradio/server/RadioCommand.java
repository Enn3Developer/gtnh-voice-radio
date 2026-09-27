package com.enn3developer.gtnhvoiceradio.server;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;

import com.enn3developer.gtnhvoiceradio.Config;

/** {@code /radio now} for everyone; {@code skip} and {@code reload} (re-reads the config) for ops. */
public class RadioCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "radio";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/radio <now|skip|reload>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String sub = args.length == 0 ? "now" : args[0];
        switch (sub) {
            case "now" -> {
                String title = RadioStation.INSTANCE.currentTitle();
                sender.addChatMessage(
                    title == null ? new ChatComponentTranslation("gtnhvoiceradio.chat.no_station")
                        : new ChatComponentTranslation("gtnhvoiceradio.chat.now", title));
            }
            case "skip" -> {
                requireOp(sender);
                RadioStation.INSTANCE.skip();
            }
            case "reload" -> {
                requireOp(sender);
                Config.reload();
                RadioStation.INSTANCE.reload();
                sender.addChatMessage(new ChatComponentTranslation("gtnhvoiceradio.chat.reloading"));
            }
            default -> throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "now", "skip", "reload") : null;
    }

    private void requireOp(ICommandSender sender) {
        if (!sender.canCommandSenderUseCommand(2, getCommandName()))
            throw new CommandException("commands.generic.permission");
    }
}
