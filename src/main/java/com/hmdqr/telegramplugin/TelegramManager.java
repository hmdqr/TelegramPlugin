package com.hmdqr.telegramplugin;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TelegramManager {
    private final String botToken;
    private final String chatId;

    public TelegramManager(String botToken, String chatId) {
        this.botToken = botToken;
        this.chatId = chatId;
    }

    /**
     * Send a text message to Telegram chat (safe to call from async context).
     */
    public void sendMessage(String text) {
        sendMessage(text, null);
    }

    /**
     * Send a text message with optional parse mode (Markdown, MarkdownV2, HTML). Null or "none" disables parse mode.
     */
    public void sendMessage(String text, String parseMode) {
        String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        try {
            StringBuilder payload = new StringBuilder();
            payload.append("chat_id=").append(urlEncode(chatId));
            payload.append("&text=").append(urlEncode(text));
            if (parseMode != null && !parseMode.isBlank() && !parseMode.equalsIgnoreCase("none")) {
                payload.append("&parse_mode=").append(urlEncode(parseMode));
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String body = readSafely(conn.getErrorStream());
                Bukkit.getLogger().warning("Telegram sendMessage failed: HTTP " + code + " - " + body);
            } else {
                // Drain and close input to free connection
                readSafely(conn.getInputStream());
            }
            conn.disconnect();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Telegram sendMessage error: " + e.getMessage());
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "";
        }
    }

    private static String readSafely(InputStream is) throws IOException {
        if (is == null) return "";
        byte[] buf = is.readAllBytes();
        is.close();
        return new String(buf, StandardCharsets.UTF_8);
    }
}
