package com.hmdqr.telegramplugin.tasks;

import com.hmdqr.telegramplugin.Main;
import com.hmdqr.telegramplugin.TelegramManager;
import com.hmdqr.telegramplugin.util.MessageFormatter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class TPSMonitor implements Runnable {
    private final Plugin plugin;
    private final TelegramManager telegram;
    private long lastAlertNanos = 0L;
    private BukkitTask task;

    public TPSMonitor(Plugin plugin, TelegramManager telegram) {
        this.plugin = plugin;
        this.telegram = telegram;
    }

    public void start() {
        stop(); // ensure not double-started
        Main cfg = Main.getInstance();
        int checkSeconds = cfg.getConfig().getInt("messages.low_tps_check_seconds", 15);
        long periodTicks = Math.max(1, checkSeconds) * 20L;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, periodTicks, periodTicks);
    }

    public void stop() {
        if (this.task != null) {
            try {
                this.task.cancel();
            } catch (Exception ignored) {}
            this.task = null;
        }
    }

    @Override
    public void run() {
        Main cfg = Main.getInstance();
        if (!cfg.getConfig().getBoolean("messages.enable_low_tps", false)) return;

        double threshold = cfg.getConfig().getDouble("messages.low_tps_threshold", 16.0);
        long cooldownSec = cfg.getConfig().getLong("messages.low_tps_cooldown_seconds", 300);

        double[] tps = Bukkit.getServer().getTPS();
        double tps1 = tps.length > 0 ? tps[0] : 20.0;
        double tps5 = tps.length > 1 ? tps[1] : tps1;
        double tps15 = tps.length > 2 ? tps[2] : tps5;

        if (tps1 >= threshold) return;

        long now = System.nanoTime();
        if (lastAlertNanos > 0 && (now - lastAlertNanos) < cooldownSec * 1_000_000_000L) {
            return; // cooldown active
        }

        Map<String, String> vals = new HashMap<>();
        vals.put("tps1m", formatTps(tps1));
        vals.put("tps5m", formatTps(tps5));
        vals.put("tps15m", formatTps(tps15));

        String template = cfg.getConfig().getString("messages.low_tps", "\u26A0 TPS low: {tps1m} (5m: {tps5m}, 15m: {tps15m})");
        String msg = MessageFormatter.apply(template, vals);
        String parse = cfg.getParseMode();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> telegram.sendMessage(msg, parse));
        lastAlertNanos = now;
    }

    private static String formatTps(double t) {
        double capped = Math.min(20.0, t);
        return String.format("%.2f", capped);
    }
}
