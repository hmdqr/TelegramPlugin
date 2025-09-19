package com.hmdqr.telegramplugin;

import com.hmdqr.telegramplugin.events.PlayerJoinLeaveListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private TelegramManager telegramManager;
    private boolean enableJoin;
    private boolean enableQuit;
    private String parseMode;
    private String joinTemplate;
    private String quitTemplate;

    @Override
    public void onEnable() {
        instance = this;

        // Load config
    saveDefaultConfig();
    // Merge new defaults for users upgrading from older versions
    getConfig().options().copyDefaults(true);
    saveConfig();

        // Read config
        String token = getConfig().getString("telegram.token");
        String chatId = getConfig().getString("telegram.chat_id");

    // Basic validation
        if (token == null || token.isBlank() || token.contains("YOUR_TELEGRAM_BOT_TOKEN") ||
                chatId == null || chatId.isBlank() || chatId.contains("YOUR_CHAT_ID")) {
            getLogger().severe("Invalid Telegram configuration. Please set telegram.token and telegram.chat_id in config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

    // Init Telegram Manager
        telegramManager = new TelegramManager(token, chatId);

    // Message settings
    enableJoin = getConfig().getBoolean("messages.enable_join", true);
    enableQuit = getConfig().getBoolean("messages.enable_quit", true);
    parseMode = getConfig().getString("messages.parse_mode", "none");
    joinTemplate = getConfig().getString("messages.join", "[+] {player} joined the server.");
    quitTemplate = getConfig().getString("messages.quit", "[-] {player} left the server.");

    // Register Events
    getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(telegramManager, this), this);
    getServer().getPluginManager().registerEvents(new com.hmdqr.telegramplugin.events.AlertsListener(telegramManager, this), this);

        // Optional low TPS monitor
        if (getConfig().getBoolean("messages.enable_low_tps", false)) {
            new com.hmdqr.telegramplugin.tasks.TPSMonitor(this, telegramManager).start();
        }

        getLogger().info("Telegram Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Telegram Plugin disabled.");
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
