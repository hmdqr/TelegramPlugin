package com.hmdqr.telegramplugin.events;

import com.hmdqr.telegramplugin.TelegramManager;
import com.hmdqr.telegramplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {

    private final TelegramManager telegramManager;
    private final Plugin plugin;

    public PlayerJoinLeaveListener(TelegramManager telegramManager, Plugin plugin) {
        this.telegramManager = telegramManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    Main main = Main.getInstance();
    if (!main.isEnableJoin()) return;
    String msg = applyTemplate(main.getJoinTemplate(), event.getPlayer().getName());
    // Run async to avoid blocking the main thread
    String parse = main.getParseMode();
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegramManager.sendMessage(msg, parse));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Main main = Main.getInstance();
        if (!main.isEnableQuit()) return;
        String msg = applyTemplate(main.getQuitTemplate(), event.getPlayer().getName());
        String parse = main.getParseMode();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegramManager.sendMessage(msg, parse));
    }

    private String applyTemplate(String template, String playerName) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        return template
                .replace("{player}", playerName)
                .replace("{uuid}", Bukkit.getPlayer(playerName) != null ? Bukkit.getPlayer(playerName).getUniqueId().toString() : "")
                .replace("{world}", Bukkit.getPlayer(playerName) != null ? Bukkit.getPlayer(playerName).getWorld().getName() : "")
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));
    }
}
