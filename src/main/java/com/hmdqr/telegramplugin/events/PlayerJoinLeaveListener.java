package com.hmdqr.telegramplugin.events;

import com.hmdqr.telegramplugin.TelegramManager;
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
        String msg = "[+] " + event.getPlayer().getName() + " joined the server.";
    // Run async to avoid blocking the main thread
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegramManager.sendMessage(msg));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String msg = "[-] " + event.getPlayer().getName() + " left the server.";
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegramManager.sendMessage(msg));
    }
}
