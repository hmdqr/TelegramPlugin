package com.hmdqr.telegramplugin.commands;

import com.hmdqr.telegramplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class HmdqrTelegramCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList("reload", "toggle", "enable", "disable", "list");
    private static final List<String> TOGGLE_KEYS = Arrays.asList(
            "enable_join", "enable_quit", "enable_kick", "enable_ban",
            "enable_death", "enable_teleport", "enable_low_tps"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload":
                return doReload(sender);
            case "toggle":
                return doToggle(sender, args, label);
            case "enable":
                return doEnableDisable(sender, args, true, label);
            case "disable":
                return doEnableDisable(sender, args, false, label);
            case "list":
                return doList(sender);
            default:
                sendUsage(sender, label);
                return true;
        }
    }

    private boolean doReload(CommandSender sender) {
        if (!sender.hasPermission("hmdqr.telegram.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }
        long start = System.currentTimeMillis();
        boolean ok = Main.getInstance().reloadPluginConfigAndRuntime();
        long took = System.currentTimeMillis() - start;
        if (ok) {
            sender.sendMessage(ChatColor.GREEN + "TelegramPlugin reloaded in " + took + "ms.");
            // Notify Telegram asynchronously with who reloaded
            String who = (sender instanceof Player) ? sender.getName() : "CONSOLE";
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () ->
                    Main.getInstance().getTelegramManager().sendMessage(
                            "✅ TelegramPlugin configuration reloaded by " + who + ".",
                            Main.getInstance().getParseMode()
                    )
            );
        } else {
            sender.sendMessage(ChatColor.RED + "Reload failed. Check console for details.");
        }
        return true;
    }

    private boolean doToggle(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("hmdqr.telegram.toggle")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " toggle <" + String.join("|", TOGGLE_KEYS) + "|monitor> <on|off>");
            return true;
        }
        String argKey = args[1].toLowerCase(Locale.ROOT);
        String key = mapAliasToKey(argKey);
        if (!TOGGLE_KEYS.contains(key)) {
            sender.sendMessage(ChatColor.RED + "Unknown feature: " + argKey);
            return true;
        }
        String val = args[2].toLowerCase(Locale.ROOT);
        Boolean newState = null;
        if (val.equals("on") || val.equals("true") || val.equals("enable")) newState = true;
        if (val.equals("off") || val.equals("false") || val.equals("disable")) newState = false;
        if (newState == null) {
            sender.sendMessage(ChatColor.YELLOW + "Use 'on' or 'off'.");
            return true;
        }

        Main main = Main.getInstance();
        // Update runtime and config (in-memory + save)
        main.getConfig().set("messages." + key, newState);
        main.saveConfig();
        // Apply runtime changes where needed
        main.applyRuntimeFlagsFromConfig();

        sender.sendMessage(ChatColor.GREEN + "Set " + key + " = " + newState + ".");
        return true;
    }

    private boolean doEnableDisable(CommandSender sender, String[] args, boolean enable, String label) {
        if (!sender.hasPermission("hmdqr.telegram.toggle")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + (enable ? " enable" : " disable") + " <" + String.join("|", TOGGLE_KEYS) + "|monitor>");
            return true;
        }
        String argKey = args[1].toLowerCase(Locale.ROOT);
        String key = mapAliasToKey(argKey);
        if (!TOGGLE_KEYS.contains(key)) {
            sender.sendMessage(ChatColor.RED + "Unknown feature: " + argKey);
            return true;
        }

        Main main = Main.getInstance();
        main.getConfig().set("messages." + key, enable);
        main.saveConfig();
        main.applyRuntimeFlagsFromConfig();
        sender.sendMessage(ChatColor.GREEN + ((enable ? "Enabled " : "Disabled ") + key + "."));
        return true;
    }

    private String mapAliasToKey(String input) {
        if (input.equals("monitor") || input.equals("low_tps") || input.equals("tps")) return "enable_low_tps";
        return input;
    }

    private boolean doList(CommandSender sender) {
        if (!sender.hasPermission("hmdqr.telegram.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }
        Main m = Main.getInstance();
        sender.sendMessage(ChatColor.AQUA + "TelegramPlugin features:");
        for (String key : TOGGLE_KEYS) {
            boolean val = m.getConfig().getBoolean("messages." + key, false);
            sender.sendMessage(ChatColor.GRAY + " - " + key + ": " + (val ? ChatColor.GREEN + "on" : ChatColor.RED + "off"));
        }
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
    sender.sendMessage(ChatColor.YELLOW + "Usage:");
    sender.sendMessage(ChatColor.YELLOW + " /hmdqr help");
    sender.sendMessage(ChatColor.YELLOW + " /hmdqr version");
    sender.sendMessage(ChatColor.YELLOW + " /hmdqr telegram|tg reload");
    sender.sendMessage(ChatColor.YELLOW + " /hmdqr telegram|tg list");
    sender.sendMessage(ChatColor.YELLOW + " /hmdqr telegram|tg toggle <" + String.join("|", TOGGLE_KEYS) + "|monitor> <on|off>");
    sender.sendMessage(ChatColor.YELLOW + " /hmdqr telegram|tg enable <feature|monitor>");
    sender.sendMessage(ChatColor.YELLOW + " /hmdqr telegram|tg disable <feature|monitor>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefixFilter(SUBS, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable"))) {
            List<String> keys = new ArrayList<>(TOGGLE_KEYS);
            keys.add("monitor");
            return prefixFilter(keys, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("toggle")) {
            return prefixFilter(Arrays.asList("on", "off"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> prefixFilter(List<String> options, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : options) if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        return out;
    }
}
