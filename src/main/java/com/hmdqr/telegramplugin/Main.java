package com.hmdqr.telegramplugin;

import com.hmdqr.telegramplugin.commands.HmdqrRootCommand;
import com.hmdqr.telegramplugin.commands.HmdqrTelegramCommand;
import com.hmdqr.telegramplugin.events.PlayerJoinLeaveListener;
import com.hmdqr.telegramplugin.tasks.TPSMonitor;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private TelegramManager telegramManager;
    private boolean enableJoin;
    private boolean enableQuit;
    private String parseMode;
    private String joinTemplate;
    private String quitTemplate;

    private TPSMonitor tpsMonitor; // track monitor for reload/cancel

    @Override
    public void onEnable() {
        instance = this;

        // Ensure config exists and merge defaults
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (!initializeFromConfig()) {
            // disable if bad config
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register events
        Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(telegramManager, this), this);
        Bukkit.getPluginManager().registerEvents(new com.hmdqr.telegramplugin.events.AlertsListener(telegramManager, this), this);

        // Optional low TPS monitor
        if (getConfig().getBoolean("messages.enable_low_tps", false)) {
            tpsMonitor = new TPSMonitor(this, telegramManager);
            tpsMonitor.start();
        }

        // Register command
    if (getCommand("hmdqr") != null) {
            HmdqrRootCommand root = new HmdqrRootCommand();
            getCommand("hmdqr").setExecutor(root);
            getCommand("hmdqr").setTabCompleter(root);
        }

        getLogger().info("Telegram Plugin enabled.");
    }

    @Override
    public void onDisable() {
        // Unregister listeners
        HandlerList.unregisterAll(this);
        // Stop background tasks
        if (tpsMonitor != null) {
            tpsMonitor.stop();
            tpsMonitor = null;
        }
        // No persistent resources to close right now
        getLogger().info("Telegram Plugin disabled.");
    }

    /**
     * Reload configuration and reinitialize runtime parts safely.
     * @return true on success, false if configuration invalid and plugin left in previous state.
     */
    public boolean reloadPluginConfigAndRuntime() {
        try {
            // Load fresh config file
            reloadConfig();

            // Validate essentials first
            String token = getConfig().getString("telegram.token");
            String chatId = getConfig().getString("telegram.chat_id");
            if (token == null || token.isBlank() || token.contains("YOUR_TELEGRAM_BOT_TOKEN") ||
                    chatId == null || chatId.isBlank() || chatId.contains("YOUR_CHAT_ID")) {
                getLogger().severe("Invalid Telegram configuration on reload. Keeping previous settings.");
                return false;
            }

            // Cancel/Unregister previous listeners
            HandlerList.unregisterAll(this);

            // Rebuild manager and fields
            this.telegramManager = new TelegramManager(token, chatId);
            applyRuntimeFlagsFromConfig();

            // Re-register listeners
            Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(telegramManager, this), this);
            Bukkit.getPluginManager().registerEvents(new com.hmdqr.telegramplugin.events.AlertsListener(telegramManager, this), this);

            // Restart TPS monitor according to config
            if (tpsMonitor != null) {
                tpsMonitor.stop();
                tpsMonitor = null;
            }
            if (getConfig().getBoolean("messages.enable_low_tps", false)) {
                tpsMonitor = new TPSMonitor(this, telegramManager);
                tpsMonitor.start();
            } else {
                tpsMonitor = null;
            }
            return true;
        } catch (Exception e) {
            getLogger().severe("Reload failed: " + e.getMessage());
            return false;
        }
    }

    private boolean initializeFromConfig() {
        String token = getConfig().getString("telegram.token");
        String chatId = getConfig().getString("telegram.chat_id");
        if (token == null || token.isBlank() || token.contains("YOUR_TELEGRAM_BOT_TOKEN") ||
                chatId == null || chatId.isBlank() || chatId.contains("YOUR_CHAT_ID")) {
            getLogger().severe("Invalid Telegram configuration. Please set telegram.token and telegram.chat_id in config.yml");
            return false;
        }

        this.telegramManager = new TelegramManager(token, chatId);
        applyRuntimeFlagsFromConfig();
        return true;
    }

    public void applyRuntimeFlagsFromConfig() {
        this.enableJoin = getConfig().getBoolean("messages.enable_join", true);
        this.enableQuit = getConfig().getBoolean("messages.enable_quit", true);
        this.parseMode = getConfig().getString("messages.parse_mode", "none");
        this.joinTemplate = getConfig().getString("messages.join", "[+] {player} joined the server.");
        this.quitTemplate = getConfig().getString("messages.quit", "[-] {player} left the server.");
        // restart/stop TPS monitor based on config without full reload
        if (getConfig().getBoolean("messages.enable_low_tps", false)) {
            if (tpsMonitor == null) {
                tpsMonitor = new TPSMonitor(this, telegramManager);
                tpsMonitor.start();
            }
        } else {
            if (tpsMonitor != null) {
                tpsMonitor.stop();
                tpsMonitor = null;
            }
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public TelegramManager getTelegramManager() {
        return telegramManager;
    }

    public boolean isEnableJoin() { return enableJoin; }
    public boolean isEnableQuit() { return enableQuit; }
    public String getParseMode() { return parseMode; }
    public String getJoinTemplate() { return joinTemplate; }
    public String getQuitTemplate() { return quitTemplate; }
}
