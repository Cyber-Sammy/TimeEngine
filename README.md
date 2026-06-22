# Time Engine

Minimal Minecraft 1.21.1 mod prototype using NeoForge and Java 21.

## Development

- Build: `./gradlew build`
- Client: `./gradlew runClient`
- Dedicated server: `./gradlew runServer`
- Format Java sources: `./gradlew spotlessApply`
- Verify formatting and tests: `./gradlew check`

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Configuration

NeoForge generates the configuration files after the first launch:

- `config/time_engine-common.toml` controls diagnostic logging.
- `<world>/serverconfig/time_engine-server.toml` controls sessions, snapshots, networking, phantom combat, afterimages, and temporal intercept limits.

Set `diagnosticLogging = true` to enable Time Engine lifecycle logs. Warnings and errors remain enabled regardless of this flag.

## Debug commands

The commands require permission level 2:

- `/timeengine session` shows the executing player's session, server tick, perceived tick, cooldown, and active session count.
- `/timeengine snapshots <player>` shows the target's buffer usage, latest snapshot tick, and whether an interpolated snapshot exists at the executing player's perceived tick.
- `/timeengine intercepts` shows temporal blocks and successfully intercepted targets for the executing player's active session.
- `/timeengine config` opens the live server configuration panel for an authorized player.
