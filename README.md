## TelegramPlugin

Lightweight Paper plugin that sends Minecraft server notifications to Telegram (join/leave, optional alerts, and low TPS monitor).

### Features
- Sends join/leave messages to a Telegram chat
- Asynchronous HTTP (non-blocking)
- URL-encoding and sensible timeouts
- Configuration validated at startup
- Fully customizable messages with placeholders and parse modes
- Optional alerts: kick, ban (login disallow), death, teleport, low TPS (cooldown + threshold)

No commands or permissions are added by this plugin.

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

Download
- Stable: get the latest release from GitHub Releases (Assets → the JAR file).
- Nightly: every push to main builds a JAR; download it from the Actions run (Artifacts).

Configuration (`plugins/TelegramPlugin/config.yml`):
```yaml
telegram:
  token: "YOUR_TELEGRAM_BOT_TOKEN"
  chat_id: "YOUR_CHAT_ID"

messages:
  enable_join: true
  enable_quit: true
  enable_kick: false
  enable_ban: false
  enable_death: false
  enable_teleport: false
  enable_low_tps: false
  parse_mode: none   # none | Markdown | MarkdownV2 | HTML
  # Available placeholders: {player}, {uuid}, {world}, {online}, {max}
  join: "[+] {player} joined the server."
  quit: "[-] {player} left the server."
  # Extra placeholders:
  #  Kick: {reason}
  #  Ban: {reason}
  #  Death: {cause}, {x}, {y}, {z}
  #  Teleport: {from_x},{from_y},{from_z},{from_world}, {to_x},{to_y},{to_z},{to_world}
  kick: "[ALERT] {player} was kicked: {reason}"
  ban: "[ALERT] {player} is banned: {reason}"
  death: "\u2620 {player} died to {cause} at {x},{y},{z} in {world}"
  teleport: "\u21A6 {player} teleported {from_world}({from_x},{from_y},{from_z}) \u2192 {to_world}({to_x},{to_y},{to_z})"

  # Low TPS monitoring
  low_tps_check_seconds: 15
  low_tps_threshold: 16.0
  low_tps_cooldown_seconds: 300
  # Placeholders: {tps1m}, {tps5m}, {tps15m}
  low_tps: "\u26A0 TPS low: {tps1m} (5m: {tps5m}, 15m: {tps15m})"
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

## Config upgrades
- The plugin writes default `config.yml` on first run and merges new defaults on upgrade (copyDefaults = true).
- A hidden `config_version` may be used to track defaults; new keys are added while your existing values are preserved.
- If we ever remove/rename keys, you may need to adjust your config manually.

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

Notes
- CI builds use Temurin JDK 21, but the plugin targets Java 17 bytecode (maven-compiler release=17). Running on Java 17+ is supported.

## Helper scripts (Linux/macOS)
- `bash scripts/bump_version.sh 1.1.0`
- `bash scripts/build.sh`
- `bash scripts/tag_and_push.sh`
- `bash scripts/release.sh`

## Troubleshooting
- Plugin disables on startup: placeholders in `config.yml` not replaced, or token/chat ID missing
- No messages received:
  - Check server console for `Telegram sendMessage failed: HTTP <code>`
  - Ensure the bot is present and allowed to post in the chat/group
  - Use the correct (possibly negative) group chat ID
  - Confirm outbound connectivity to `api.telegram.org`
  - If using MarkdownV2/HTML, ensure your message text escapes special characters per Telegram rules

Common Telegram API errors
- 400 Bad Request: often due to unescaped characters with `parse_mode` set
- 403 Forbidden: bot not a participant in the target chat, or blocked

## Compatibility
- Default: Paper 1.20.6, Java 17+
- For other versions, align `api-version` and Paper API dependency

## License
MIT License — see `LICENSE` for details.
