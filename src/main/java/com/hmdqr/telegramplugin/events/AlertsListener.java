package com.hmdqr.telegramplugin.events;

import com.hmdqr.telegramplugin.Main;
import com.hmdqr.telegramplugin.TelegramManager;
import com.hmdqr.telegramplugin.util.MessageFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class AlertsListener implements Listener {
    private final TelegramManager telegram;
    private final Plugin plugin;

    public AlertsListener(TelegramManager telegram, Plugin plugin) {
        this.telegram = telegram;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        Main cfg = Main.getInstance();
        if (!cfg.getConfig().getBoolean("messages.enable_kick", false)) return;
        Player p = event.getPlayer();
        Map<String, String> vals = baseValues(p);
        vals.put("reason", event.getReason());
        String msg = MessageFormatter.apply(cfg.getConfig().getString("messages.kick", "[ALERT] {player} was kicked: {reason}"), vals);
        String parse = cfg.getParseMode();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegram.sendMessage(msg, parse));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        Main cfg = Main.getInstance();
        if (!cfg.getConfig().getBoolean("messages.enable_ban", false)) return;
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.KICK_BANNED) {
            Map<String, String> vals = new HashMap<>();
            vals.put("player", event.getName());
            vals.put("uuid", event.getUniqueId().toString());
            vals.put("reason", event.getKickMessage());
            String msg = MessageFormatter.apply(cfg.getConfig().getString("messages.ban", "[ALERT] {player} is banned: {reason}"), vals);
            String parse = cfg.getParseMode();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegram.sendMessage(msg, parse));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Main cfg = Main.getInstance();
        if (!cfg.getConfig().getBoolean("messages.enable_death", false)) return;
        Player p = event.getEntity();
        Map<String, String> vals = baseValues(p);
        vals.put("cause", MessageFormatter.plain(event.deathMessage()));
        Location l = p.getLocation();
        vals.put("x", String.valueOf(l.getBlockX()));
        vals.put("y", String.valueOf(l.getBlockY()));
        vals.put("z", String.valueOf(l.getBlockZ()));
        String msg = MessageFormatter.apply(cfg.getConfig().getString("messages.death", "☠ {player} died to {cause} at {x},{y},{z} in {world}"), vals);
        String parse = cfg.getParseMode();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegram.sendMessage(msg, parse));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Main cfg = Main.getInstance();
        if (!cfg.getConfig().getBoolean("messages.enable_teleport", false)) return;
        Player p = event.getPlayer();
        Map<String, String> vals = baseValues(p);
        Location from = event.getFrom();
        Location to = event.getTo();
        vals.put("from_x", String.valueOf(from.getBlockX()));
        vals.put("from_y", String.valueOf(from.getBlockY()));
        vals.put("from_z", String.valueOf(from.getBlockZ()));
        vals.put("from_world", from.getWorld() != null ? from.getWorld().getName() : "");
        vals.put("to_x", String.valueOf(to != null ? to.getBlockX() : 0));
        vals.put("to_y", String.valueOf(to != null ? to.getBlockY() : 0));
        vals.put("to_z", String.valueOf(to != null ? to.getBlockZ() : 0));
        vals.put("to_world", to != null && to.getWorld() != null ? to.getWorld().getName() : "");
        String msg = MessageFormatter.apply(cfg.getConfig().getString("messages.teleport", "↦ {player} teleported {from_world}({from_x},{from_y},{from_z}) → {to_world}({to_x},{to_y},{to_z})"), vals);
        String parse = cfg.getParseMode();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegram.sendMessage(msg, parse));
    }

    private Map<String, String> baseValues(Player p) {
        Map<String, String> vals = new HashMap<>();
        vals.put("player", p.getName());
        vals.put("uuid", p.getUniqueId().toString());
        vals.put("world", p.getWorld().getName());
        vals.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        vals.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        return vals;
    }
}
