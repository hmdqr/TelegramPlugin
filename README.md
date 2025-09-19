# TelegramPlugin

[![Build](https://github.com/hmdqr/TelegramPlugin/actions/workflows/release.yml/badge.svg)](https://github.com/hmdqr/TelegramPlugin/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Paper](https://img.shields.io/badge/Paper-1.20.6%2B-orange)
[![PayPal](https://img.shields.io/badge/PayPal-donate-00457C?logo=paypal&logoColor=white)](https://paypal.me/hmdqr/)

*Note: This project was fully coded using GitHub Copilot.*

Lightweight, friendly Paper plugin that sends Minecraft server notifications to Telegram — join/leave, optional alerts, and a low TPS monitor. Clear setup, sane defaults, and no fuss.

Quick links: [Releases](https://github.com/hmdqr/TelegramPlugin/releases) · [Actions/Artifacts](https://github.com/hmdqr/TelegramPlugin/actions) · [Issues](https://github.com/hmdqr/TelegramPlugin/issues)

<details>
  <summary><strong>Table of contents</strong></summary>

* [Quick start](#quick-start)
* [Features](#features)
* [Requirements](#requirements)
* [Installation](#installation)
* [Configuration](#configuration)
* [Telegram setup](#telegram-setup)
* [Build from source](#build-from-source)
* [Helper scripts](#helper-scripts)
* [Troubleshooting](#troubleshooting)
* [Known limitations](#known-limitations)
* [Security](#security)
* [Compatibility](#compatibility)
* [Contributing](#contributing)
* [License](#license)

</details>

## Quick start
1) Download the latest shaded JAR from Releases (Assets → file ending with -shaded.jar).
2) Copy it to your server's `plugins/` folder.
3) Start the server once to generate `plugins/TelegramPlugin/config.yml`.
4) Edit `config.yml` and set `telegram.token` and `telegram.chat_id`.
5) Restart the server.

## Features
- Sends join/leave messages to a Telegram chat
- Optional alerts: kick, ban (login disallow), death, teleport, low TPS (cooldown + threshold)
- Asynchronous HTTP (non-blocking) with URL-encoding and timeouts
- Config validated at startup; disables plugin if token/chat ID are missing
- Fully customizable messages with placeholders and parse modes (none/Markdown/MarkdownV2/HTML)

No commands or permissions are added by this plugin.

## Requirements
- Java 17+
- Maven 3.8+
- Paper 1.20.6 (recommended)

Using a different Paper version? Update both:
- `plugin.yml` → `api-version`
- `pom.xml` → `io.papermc.paper:paper-api` version

PaperMC docs: https://docs.papermc.io/

## Installation
- Stable: get the latest release from GitHub Releases → https://github.com/hmdqr/TelegramPlugin/releases (Assets → shaded JAR)
- Nightly: every push to main builds a JAR; download from GitHub Actions → https://github.com/hmdqr/TelegramPlugin/actions (Artifacts)

Then:
1) Place the JAR into `plugins/`.
2) Start once to generate `plugins/TelegramPlugin/config.yml`.
3) Set `telegram.token` and `telegram.chat_id`.
4) Restart.

## Configuration
The configuration file is written to `plugins/TelegramPlugin/config.yml` on first run.

YAML excerpt:
```yaml
# Internal config version for upgrades
config_version: 2

telegram:
  token: "YOUR_TELEGRAM_BOT_TOKEN"
  chat_id: "YOUR_CHAT_ID"

messages:
  # Enable/disable notifications
  enable_join: true
  enable_quit: true
  enable_kick: false
  enable_ban: false
  enable_death: false
  enable_teleport: false
  enable_low_tps: false

  # Text formatting: none | Markdown | MarkdownV2 | HTML
  parse_mode: none

  # Templates (placeholders: {player}, {uuid}, {world}, {online}, {max})
  join: "[+] {player} joined the server."
  quit: "[-] {player} left the server."

  # Extra placeholders by event:
  # Kick: {reason}
  kick: "[ALERT] {player} was kicked: {reason}"
  # Ban: {reason}
  ban: "[ALERT] {player} is banned: {reason}"
  # Death: {cause}, {x}, {y}, {z}
  death: "\u2620 {player} died to {cause} at {x},{y},{z} in {world}"
  # Teleport: {from_x},{from_y},{from_z},{from_world}, {to_x},{to_y},{to_z},{to_world}
  teleport: "\u21A6 {player} teleported {from_world}({from_x},{from_y},{from_z}) \u2192 {to_world}({to_x},{to_y},{to_z})"

  # Low TPS monitoring
  low_tps_check_seconds: 15
  low_tps_threshold: 16.0
  low_tps_cooldown_seconds: 300
  # Placeholders: {tps1m}, {tps5m}, {tps15m}
  low_tps: "\u26A0 TPS low: {tps1m} (5m: {tps5m}, 15m: {tps15m})"
```

### Quick reference
- telegram.token (string): required, from @BotFather.
- telegram.chat_id (string/int): required. Groups often use a negative ID like -1001234567890.
- messages.enable_join|quit|kick|ban|death|teleport|low_tps (bool): toggles per event.
- messages.parse_mode: none | Markdown | MarkdownV2 | HTML.
- messages.join|quit|kick|ban|death|teleport|low_tps: templates with placeholders.
- messages.low_tps_check_seconds (int): check interval seconds.
- messages.low_tps_threshold (double): alert when 1m TPS below this.
- messages.low_tps_cooldown_seconds (int): minimum seconds between alerts.
- config_version: used internally to merge new defaults on upgrade.

### Placeholders
- Common: {player}, {uuid}, {world}, {online}, {max}
- Kick: {reason}
- Ban: {reason}
- Death: {cause}, {x}, {y}, {z}
- Teleport: {from_x},{from_y},{from_z},{from_world}, {to_x},{to_y},{to_z},{to_world}

### Parse mode gotchas
- MarkdownV2 and HTML require proper escaping. If a message fails with HTTP 400, first suspect unescaped characters.
- If in doubt, use `parse_mode: none` for plain text.

### Examples
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

Windows (Command Prompt):
```bat
mvn -q -DskipTests package
```

Artifacts appear in `target/`.

Notes
- CI builds use Temurin JDK 21, but the plugin targets Java 17 bytecode (maven-compiler release=17). Running on Java 17+ is supported.

## Helper scripts
Linux/macOS
- `bash scripts/bump_version.sh 1.1.0`
- `bash scripts/build.sh`
- `bash scripts/tag_and_push.sh`
- `bash scripts/release.sh`

Windows
- `scripts\bump_version.bat 1.1.0`
- `scripts\build.bat`
- `scripts\tag_and_push.bat`
- `scripts\release.bat`
## Known limitations
- Single target chat_id (per plugin instance)
- No proxy configuration
- Only text messages are sent (no photos/files)
- Telegram rate limits apply; the plugin does not queue during downtime

## Security
- Treat your bot token as a secret; do not commit `config.yml` with a real token
- Limit who can access your server files and console


Tip: If you prefer not to install Maven globally, add the Maven Wrapper (`mvnw`, `mvnw.cmd`) — happy to include it.

## Troubleshooting
- Plugin disables on startup: placeholders in `config.yml` not replaced, or token/chat ID missing
- No messages received:
  - Check server console for messages like: Telegram sendMessage failed: HTTP {status_code}
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
PRs and issues are welcome — whether it’s a typo fix, a feature request, or a bug report.

When filing bugs, include:
- Server version (Paper build) and Java version
- Plugin version
- Reproduction steps and relevant console logs

## Support this project
If this plugin saved you time or made your server friendlier, consider supporting development:
- PayPal: https://paypal.me/hmdqr/

Even small tips help keep the work going — thank you!

## License
MIT License — see `LICENSE` for details.
