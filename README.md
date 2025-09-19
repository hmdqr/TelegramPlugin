## TelegramPlugin

[![Build](https://github.com/hmdqr/TelegramPlugin/actions/workflows/release.yml/badge.svg)](https://github.com/hmdqr/TelegramPlugin/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Paper](https://img.shields.io/badge/Paper-1.20.6%2B-orange)
[![PayPal](https://img.shields.io/badge/PayPal-donate-00457C?logo=paypal&logoColor=white)](https://paypal.me/hmdqr/)

Lightweight, friendly Paper plugin that sends Minecraft server notifications to Telegram — join/leave, optional alerts, and a low TPS monitor. Clear setup, sane defaults, and no fuss.

Quick links: [Releases](https://github.com/hmdqr/TelegramPlugin/releases) · [Actions/Artifacts](https://github.com/hmdqr/TelegramPlugin/actions) · [Issues](https://github.com/hmdqr/TelegramPlugin/issues)

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

PaperMC docs: https://docs.papermc.io/

## Installation
1. Place the plugin JAR into the server `plugins/` directory.
2. Start the server once to generate `plugins/TelegramPlugin/config.yml`.
3. Edit `config.yml` and set the bot token and chat ID.
4. Restart the server.

Download
- Stable: get the latest release from GitHub Releases → https://github.com/hmdqr/TelegramPlugin/releases (Assets → the shaded JAR)
- Nightly: every push to main builds a JAR; grab it from GitHub Actions → https://github.com/hmdqr/TelegramPlugin/actions (Artifacts)

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

Useful docs
- Bot API sendMessage: https://core.telegram.org/bots/api#sendmessage
- MarkdownV2 rules: https://core.telegram.org/bots/api#markdownv2-style
- HTML style: https://core.telegram.org/bots/api#html-style

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

Artifacts appear in `target/`.

Notes
- CI builds use Temurin JDK 21, but the plugin targets Java 17 bytecode (maven-compiler release=17). Running on Java 17+ is supported.

## Helper scripts (Linux/macOS)
- `bash scripts/bump_version.sh 1.1.0`
- `bash scripts/build.sh`
- `bash scripts/tag_and_push.sh`
- `bash scripts/release.sh`

Tip: If you prefer not to install Maven globally, add the Maven Wrapper (`mvnw`, `mvnw.cmd`) — happy to include it.

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

## Contributing
PRs and issues are welcome — whether it’s a typo fix, a feature request, or a bug report. Please include server version, plugin version, and reproduction steps when filing bugs.

## Support this project
If this plugin saved you time or made your server friendlier, consider supporting development:
- PayPal: https://paypal.me/hmdqr/

Even small tips help keep the work going — thank you!

## License
MIT License — see `LICENSE` for details.
