## TelegramPlugin

Lightweight Paper plugin that sends Minecraft join/leave notifications to Telegram.

### Features
- Sends join/leave messages to a Telegram chat
- Asynchronous HTTP (non-blocking)
- URL-encoding and sensible timeouts
- Configuration validated at startup
- Fully customizable messages with placeholders and parse modes

## Requirements
- Java 17+
- Maven 3.8+
- Paper 1.20.6 (recommended)

Using another Paper version? Update both:
- `plugin.yml` → `api-version`
- `pom.xml` → `io.papermc.paper:paper-api` version

## Installation
1. Place the plugin JAR into the server `plugins/` directory.
2. Start the server once to generate `plugins/TelegramPlugin/config.yml`.
3. Edit `config.yml` and set the bot token and chat ID.
4. Restart the server.

Configuration (`plugins/TelegramPlugin/config.yml`):
```yaml
telegram:
  token: "YOUR_TELEGRAM_BOT_TOKEN"
  chat_id: "YOUR_CHAT_ID"

messages:
  enable_join: true
  enable_quit: true
  parse_mode: none   # none | Markdown | MarkdownV2 | HTML
  # Available placeholders: {player}, {uuid}, {world}, {online}, {max}
  join: "[+] {player} joined the server."
  quit: "[-] {player} left the server."
```

Examples:
- MarkdownV2 (remember to escape special characters):
  ```yaml
  messages:
    parse_mode: MarkdownV2
    join: "[+] *{player}* joined \\({online}\\/{max}\\)"
    quit: "[-] *{player}* left"
  ```
- HTML:
  ```yaml
  messages:
    parse_mode: HTML
    join: "[+] <i>{player}</i> joined (<code>{online}/{max}</code>)"
    quit: "[-] <i>{player}</i> left"
  ```

## Telegram setup
1. Create a bot via Telegram’s @BotFather and obtain the HTTP API token.
2. Send a message to the bot (or add the bot to a group).
3. Retrieve the chat ID:
   - Visit `https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates` and read `message.chat.id` in the JSON
   - Group/supergroup IDs are often negative (e.g., `-1001234567890`)

## Build from source
Run in the project root (where `pom.xml` resides).

Linux/macOS:
```bash
mvn -q -DskipTests package
```

Windows (cmd.exe):
```bat
mvn -q -DskipTests package
```

Artifacts appear in `target/`.

## Troubleshooting
- Plugin disables on startup: placeholders in `config.yml` not replaced, or token/chat ID missing
- No messages received:
  - Check server console for `Telegram sendMessage failed: HTTP <code>`
  - Ensure the bot is present and allowed to post in the chat/group
  - Use the correct (possibly negative) group chat ID
  - Confirm outbound connectivity to `api.telegram.org`

## Compatibility
- Default: Paper 1.20.6, Java 17+
- For other versions, align `api-version` and Paper API dependency

## License
MIT License — see `LICENSE` for details.
