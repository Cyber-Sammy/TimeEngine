# Time Engine

Minimal Minecraft 1.21.1 mod prototype using NeoForge and Java 21.

## Development

- Build: `./gradlew build`
- Client: `./gradlew runClient`
- Dedicated server: `./gradlew runServer`

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Configuration

NeoForge generates the configuration files after the first launch:

- `config/time_engine-common.toml` controls diagnostic logging.
- `<world>/serverconfig/time_engine-server.toml` controls session duration, cooldown, time scale, radius, and snapshot limits.

Set `diagnosticLogging = true` to enable Time Engine lifecycle logs. Warnings and errors remain enabled regardless of this flag.
