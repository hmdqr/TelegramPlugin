package com.hmdqr.telegramplugin.commands;

import com.hmdqr.telegramplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.*;

import java.util.*;

public class HmdqrRootCommand implements CommandExecutor, TabCompleter {

    private static final List<String> FIRST = Arrays.asList("telegram", "tg", "help", "version");
    private final HmdqrTelegramCommand telegramHandler = new HmdqrTelegramCommand();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        if (first.equals("telegram") || first.equals("tg")) {
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            return telegramHandler.onCommand(sender, command, "hmdqr-telegram", rest);
        } else if (first.equals("help")) {
            sendUsage(sender, label);
            return true;
        } else if (first.equals("version")) {
            String ver = Main.getInstance() != null ? Main.getInstance().getDescription().getVersion() : "";
            sender.sendMessage(ChatColor.GREEN + "TelegramPlugin version " + ver);
            return true;
        }
        sendUsage(sender, label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
            return prefix(FIRST, args[0]);
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        if (first.equals("telegram") || first.equals("tg")) {
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            return telegramHandler.onTabComplete(sender, command, alias, rest);
        }
        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
    sender.sendMessage(ChatColor.YELLOW + " /" + label + " help");
    sender.sendMessage(ChatColor.YELLOW + " /" + label + " version");
    sender.sendMessage(ChatColor.YELLOW + " /" + label + " telegram|tg reload");
    sender.sendMessage(ChatColor.YELLOW + " /" + label + " telegram|tg list");
    sender.sendMessage(ChatColor.YELLOW + " /" + label + " telegram|tg toggle <feature> <on|off>");
    sender.sendMessage(ChatColor.YELLOW + " /" + label + " telegram|tg enable <feature>");
    sender.sendMessage(ChatColor.YELLOW + " /" + label + " telegram|tg disable <feature>");
    }

    private List<String> prefix(List<String> options, String p) {
        String s = p == null ? "" : p.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String opt : options) if (opt.startsWith(s)) out.add(opt);
        return out;
    }
}
