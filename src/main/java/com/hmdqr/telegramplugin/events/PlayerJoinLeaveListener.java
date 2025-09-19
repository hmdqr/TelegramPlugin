package com.hmdqr.telegramplugin.events;

import com.hmdqr.telegramplugin.TelegramManager;
import com.hmdqr.telegramplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

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
        String msg = applyTemplate(main.getJoinTemplate(), event.getPlayer());
        // Run async to avoid blocking the main thread
        String parse = main.getParseMode();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegramManager.sendMessage(msg, parse));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Main main = Main.getInstance();
        if (!main.isEnableQuit()) return;
        String msg = applyTemplate(main.getQuitTemplate(), event.getPlayer());
        String parse = main.getParseMode();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegramManager.sendMessage(msg, parse));
    }

    private String applyTemplate(String template, Player player) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String uuid = player.getUniqueId().toString();
        String world = player.getWorld() != null ? player.getWorld().getName() : "";
        return template
                .replace("{player}", player.getName())
                .replace("{uuid}", uuid)
                .replace("{world}", world)
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));
    }
}
