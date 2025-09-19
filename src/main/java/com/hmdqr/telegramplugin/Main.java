package com.hmdqr.telegramplugin;

import com.hmdqr.telegramplugin.events.PlayerJoinLeaveListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private TelegramManager telegramManager;

    @Override
    public void onEnable() {
        instance = this;

        // Load config
        saveDefaultConfig();

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

    // Register Events
    getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(telegramManager, this), this);

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
}
