## TelegramPlugin

Send Minecraft join/leave notifications to Telegram. Lightweight, async, and simple to configure.

### Features
- Join/leave messages to a Telegram chat
- Asynchronous HTTP requests (won’t lag the server)
- URL-encoding and sane timeouts
- Config validation on startup

## Requirements
- Java 17+ (Java 21 OK)
- Maven 3.8+
- Paper server 1.20.6 (recommended)

If you run a different Paper version, update both:
- `plugin.yml` → `api-version`
- `pom.xml` → `io.papermc.paper:paper-api` version

## Build
Run from the project root (where `pom.xml` is).

Linux/macOS:
```bash
mvn -q -DskipTests package
```

Windows (cmd.exe):
```bat
mvn -q -DskipTests package
```

Artifacts are created in `target/` (look for `telegramplugin-<version>-shaded.jar`).

## Install
1) Copy the built JAR to your server’s `plugins/` folder.
2) Start the server once to generate `plugins/TelegramPlugin/config.yml`.
3) Edit `config.yml` and set your bot token and chat ID.
4) Restart the server.

Config (`plugins/TelegramPlugin/config.yml`):
```yaml
telegram:
  token: "YOUR_TELEGRAM_BOT_TOKEN"
  chat_id: "YOUR_CHAT_ID"
```

## Telegram setup
1) Create a bot via Telegram’s @BotFather → copy the HTTP API token.
2) Send a message to your bot (or add it to a group where the bot was added).
3) Get the chat ID:
   - Open in a browser: `https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates`
   - Look for `message.chat.id` in the JSON. Group/supergroup IDs are usually negative (e.g., `-1001234567890`).
   - Alternatively, DM `@userinfobot` for your personal chat id.

## How it works
- On player join/quit, the plugin queues a background task and sends a POST to Telegram’s `sendMessage` endpoint.
- Requests are URL-encoded, with a 5s connect/read timeout.
- Non-2xx responses are logged with the HTTP status code.

## Troubleshooting
- Plugin disabled at startup: ensure `config.yml` doesn’t contain placeholders and both token/chat_id are set.
- No messages:
  - Check console for: `Telegram sendMessage failed: HTTP <code>`.
  - Make sure the bot is in the group and not restricted from messaging.
  - For groups, use the (possibly negative) group chat ID.
  - Verify your server has internet access; confirm Telegram API is reachable from the host.
- Rate limits: Telegram may throttle rapid bursts. This plugin sends minimal messages, but large bursts could be limited by Telegram.

## Compatibility
- Default setup: Paper 1.20.6, Java 17+
- If using another Minecraft version, align `plugin.yml` api-version and `pom.xml` Paper API version accordingly.

## Development
- Sources: `src/main/java`
- Resources (plugin.yml, config.yml): `src/main/resources`
- Main class: `com.hmdqr.telegramplugin.Main`

Common tasks:
```bash
# Build without tests
mvn -q -DskipTests package

# Clean build
mvn -q clean package
```

## Security
- Don’t commit real tokens. The default config contains placeholders.
- The plugin never logs your token.

## License
Add a LICENSE file to clarify usage and distribution terms (MIT/Apache-2.0 are popular choices).

## Publishing
- Create a GitHub release and attach the built JAR from `target/`.
- For plugin portals (SpigotMC/PaperMC forums), include: description, requirements (Java/Paper versions), quick setup, and configuration snippet.
