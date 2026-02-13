# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TH_TempFly is a Minecraft Spigot/Paper plugin that provides a temporary flight system with multi-server synchronization. Players can be granted limited flight time, which is tracked across server restarts and synchronized across multiple servers via Redis.

**Base Package**: `com.github.djkingcraftero89.TH_TempFly`

## Build Commands

### Standard Development Build
```bash
mvn clean package
```
Output: `target/TH_TempFly-{version}.jar`

### Release Build (Auto-increments version)
```bash
mvn clean package -Prelease
```
This automatically increments the patch version (e.g., 1.2.0 → 1.2.1).

### Testing
This project does not currently have automated tests. Testing is done manually on a Minecraft server.

### Installation for Testing
1. Build the JAR using `mvn clean package`
2. Copy `target/TH_TempFly-{version}.jar` to your test server's `plugins/` folder
3. Start/restart the server
4. Check console logs for initialization messages
5. Configuration files will be auto-generated in `plugins/TH_TempFly/`

## Architecture

### Core Components

**TH_TempFly** (main plugin class)
- Entry point for the plugin
- Initializes all managers and services
- Creates HikariCP database connection pool from config
- Sets up Redis if enabled
- Registers commands, listeners, and PlaceholderAPI expansion

**FlyManager** (`fly/FlyManager.java`)
- Central manager for all flight time logic
- Maintains in-memory cache of player flight times (`playerToRemainingSeconds`)
- Runs scheduled task to tick down flight time
- Two time modes: "frozen" (only counts down while online) or "continuous" (always counts down)
- Handles flight activation/deactivation and title warnings
- Synchronizes changes to Redis for multi-server setups
- Integrates with VulcanIntegration to exempt flying players from anti-cheat

**DataStore** (`storage/DataStore.java` interface, `storage/SQLDataStore.java` implementation)
- Persistence layer for flight time data
- Supports SQLite (default) and MySQL
- Uses HikariCP connection pooling
- Simple schema: player UUID → remaining seconds

**RedisService** (`redis/RedisService.java`)
- Handles Redis pub/sub for cross-server synchronization
- Publishes `PLAYER_SYNC:{uuid}:{seconds}:{servername}` messages when flight time changes
- Subscribes to same channel and updates local cache from other servers
- Uses Lettuce Redis client library

**FlightRestrictionManager** (`restriction/FlightRestrictionManager.java`)
- Manages world-based and region-based flight restrictions
- Integrates with WorldGuard to check if player is in a blocked region
- Players with `thtempfly.fly.bypass` permission can fly anywhere
- Returns `RestrictionResult` with detailed restriction info (world, region, or allowed)

**VulcanIntegration** (`integration/VulcanIntegration.java`)
- Integrates with Vulcan Anti-Cheat plugin (optional soft dependency)
- When a player activates fly, adds exemptions for Flight, Elytra, Speed, and Motion checks
- When fly is deactivated or time expires, removes exemptions
- Prevents legitimate fly users from being flagged as hackers

**MessageManager** (`util/MessageManager.java`)
- Loads and manages all player-facing messages from `messages.yml`
- Supports color codes and placeholders
- Sends both chat messages and title/subtitle messages

**UpdateChecker** (`util/UpdateChecker.java`)
- Checks GitHub releases for new versions on startup
- Notifies console and admins when updates are available
- Repository: JuanCorso-dev/TH_TempFly

### Commands

**TempFlyCommand** (`command/TempFlyCommand.java`)
- `/tempfly give <player> <time>` - Set player's flight time
- `/tempfly add <player> <time>` - Add to player's flight time
- `/tempfly remove <player> <time>` - Subtract from player's flight time
- `/tempfly check <player>` - Check player's remaining time
- `/tempfly reload` - Reload config and messages
- `/tempfly version` - Check current version and available updates
- `/atempfly debug <true|false>` - Toggle debug logging (static flag)

**FlyCommand** (`command/FlyCommand.java`)
- `/fly` - Toggle flight mode for the player
- Checks flight restrictions (worlds, regions)
- Handles infinite flight permission (`thtempfly.fly.infinite`)
- Activates/deactivates flight and updates Vulcan exemptions

### Time Format Parsing

**TimeParser** (`util/TimeParser.java`)
- Parses time strings like `30s`, `5m`, `2h`, `1d` into seconds
- Formats seconds back into human-readable format (e.g., "1h 30m")
- No suffix defaults to seconds

### Data Flow

1. **Player joins**:
   - `PlayerListener` loads player data from database via `FlyManager.loadPlayer()`
   - If `fly.enable-on-join-if-leftover` is true and player has time, auto-enables flight
   - Syncs current time to Redis

2. **Player uses `/fly`**:
   - `FlyCommand` checks restrictions via `FlightRestrictionManager`
   - Toggles flight state
   - `VulcanIntegration` adds/removes anti-cheat exemptions
   - Change is synced to Redis

3. **Tick loop (FlyManager)**:
   - Every `fly.tick-interval-ticks` (default 20 = 1 second)
   - Deducts time from flying players (or only online players if freeze mode is on)
   - Shows title warnings when time is low
   - Auto-disables flight when time reaches 0
   - Periodically saves to database and syncs to Redis

4. **Admin modifies time**:
   - `TempFlyCommand` calls `FlyManager.setRemaining()`
   - Change is written to database asynchronously
   - Change is synced to Redis immediately
   - Other servers receive update and apply it

5. **Player quits**:
   - `PlayerListener` calls `FlyManager.unloadPlayer()`
   - Final state is saved to database
   - Final state is synced to Redis
   - In-memory data is cleaned up

### Multi-Server Synchronization

**Redis Message Format**: `PLAYER_SYNC:{uuid}:{seconds}:{servername}`

- When flight time changes, `FlyManager` publishes to Redis channel
- All servers subscribed to channel receive the message
- Receiving server checks if message is from a different server (ignores own messages)
- If different, updates local cache with new time value
- Debug mode logs all sync operations

**Deduplication**: `lastSyncedSeconds` map prevents sending duplicate values to Redis.

### Vulcan Anti-Cheat Integration

- **Soft dependency**: Works with or without Vulcan installed
- **Detection**: Checks if Vulcan plugin is loaded in `VulcanIntegration` constructor
- **Exempted checks**: Flight, Elytra, Speed, Motion
- **Lifecycle**:
  - Exemptions added when flight is enabled (via `/fly`, auto-enable on join, or time granted)
  - Exemptions removed when flight is disabled (via `/fly`, time expires, or quit)

### Configuration Structure

**config.yml**:
- `storage.type`: SQLITE or MYSQL
- `redis.enabled`: Enable multi-server sync
- `fly.tick-interval-ticks`: How often to deduct time (default 20 = 1 second)
- `fly.save-interval-seconds`: How often to save to database (default 60)
- `fly.freeze-time-when-offline`: Only count down time while player is online
- `fly.titles.enabled`: Show countdown titles when time is low
- `fly.restrictions.enabled`: Enable world/region restrictions
- `fly.restrictions.blocked-worlds`: List of world names where flight is disabled
- `fly.restrictions.blocked-regions`: List of WorldGuard region names where flight is disabled
- `update-checker.enabled`: Check for updates on startup

**messages.yml**: All player-facing messages (auto-generated, fully customizable)

**plugin.yml**:
- Soft dependencies: PlaceholderAPI, WorldGuard, Vulcan
- API version: 1.20

## Dependencies

### Runtime (Shaded into JAR)
- **HikariCP** 5.1.0 - Database connection pooling
- **Lettuce** 6.3.2.RELEASE - Redis client
- **bStats** 3.0.2 - Anonymous usage statistics
- **SQLite JDBC** 3.46.0.0 - SQLite database driver (runtime)
- **MySQL Connector** 8.4.0 - MySQL database driver (runtime)

### Provided (Must be on server)
- **Paper API** 1.20.6 - Minecraft server API
- **PlaceholderAPI** 2.11.6 - Optional placeholder expansion
- **WorldGuard** 7.0.9 - Optional region restrictions
- **Vulcan API** 2.8.8 - Optional anti-cheat integration (commented out in pom.xml by default)

## Permissions

- `thtempfly.use` - Base admin command (default: op)
- `thtempfly.admin` - Admin commands and debug mode (default: op)
- `thtempfly.fly.use` - Use /fly command (default: true)
- `thtempfly.fly.infinite` - Infinite flight time (default: op)
- `thtempfly.fly.bypass` - Bypass world/region restrictions (default: op)

## PlaceholderAPI Expansions

Registered in `TempFlyPlaceholder.java`:
- `%thtempfly_time%` - Formatted remaining time ("1h 30m" or "Infinite")
- `%thtempfly_time_seconds%` - Time in seconds (or -1 for infinite)
- `%thtempfly_time_minutes%` - Time in minutes (or -1 for infinite)
- `%thtempfly_time_hours%` - Time in hours (or -1 for infinite)
- `%thtempfly_has_time%` - "true" or "false"
- `%thtempfly_can_fly%` - "true" or "false"
- `%thtempfly_status%` - "flying", "can_fly", or "no_time"

## Debug Mode

Toggle with `/atempfly debug <true|false>` (requires `thtempfly.admin` permission)

Debug logs include:
- Player loading/unloading
- Database reads/writes
- Redis publishes and receives
- Time synchronization between servers
- Deduplication decisions

Debug mode uses a static boolean flag in `TempFlyCommand.isDebugMode()` to avoid passing the value through every method.

## Important Implementation Notes

### Infinite Flight
Players with `thtempfly.fly.infinite` permission have special handling:
- `FlyManager.getRemaining()` returns -1 for infinite players
- Time is never deducted for infinite players
- Database still stores their time, but permission takes precedence

### Time Freeze Mode
When `fly.freeze-time-when-offline` is true:
- Uses `tickOnlinePlayersOnly()` instead of `tickAllPlayers()`
- Only deducts time from players currently online and flying
- Offline players' time is frozen and preserved

### Restriction System
Flight can be blocked by:
1. **Worlds**: Checked via world name in config list
2. **Regions**: Checked via WorldGuard API (requires WorldGuard)
3. **Bypass**: Players with `thtempfly.fly.bypass` can fly anywhere

When entering a blocked zone while flying:
- Flight is immediately disabled
- Player receives notification with specific reason (world or region name)

### Vulcan Integration Notes
- Vulcan dependency is **commented out** in pom.xml by default
- To enable Vulcan compilation, install Vulcan.jar to local Maven repo (see pom.xml comments)
- Plugin still works without Vulcan - integration gracefully degrades
- When Vulcan is detected, console shows: "Integración con Vulcan Anti-Cheat activada correctamente!"
- When Vulcan is not detected, console shows: "Vulcan Anti-Cheat no encontrado, integración deshabilitada."

### Database Schema
Single table (auto-created):
```sql
CREATE TABLE IF NOT EXISTS player_fly_time (
    player_uuid VARCHAR(36) PRIMARY KEY,
    remaining_seconds BIGINT NOT NULL
)
```

### Maven Profiles
- **Default profile**: Standard build, no version changes
- **Release profile** (`-Prelease`): Auto-increments patch version using `versions-maven-plugin` and `build-helper-maven-plugin`

## Common Workflows

### Adding a new command
1. Add command to `plugin.yml`
2. Create or update command executor class in `command/` package
3. Register executor in `TH_TempFly.onEnable()`
4. Add permission to `plugin.yml` if needed
5. Add messages to `src/main/resources/messages.yml`

### Adding a new placeholder
1. Add method to `TempFlyPlaceholder.onPlaceholderRequest()`
2. Return string value based on player and identifier
3. Update README.md with new placeholder documentation

### Adding a new restriction type
1. Add check logic to `FlightRestrictionManager`
2. Create new `RestrictionType` enum value if needed
3. Add configuration options to `config.yml`
4. Add restriction messages to `messages.yml`
5. Call check in `FlyCommand` and `PlayerListener`

### Modifying Redis sync behavior
1. Edit message format in `FlyManager.syncToRedis()`
2. Update parsing logic in `TH_TempFly.onEnable()` Redis subscription handler
3. Ensure all servers are updated to same version to avoid incompatibility

## Code Style Notes

- Messages use `&` color codes (converted to § by MessageManager)
- All player-facing strings come from MessageManager (no hardcoded messages in logic)
- Database operations are async where possible (via `Bukkit.getScheduler().runTaskAsynchronously()`)
- Redis sync uses deduplication to avoid spamming identical updates
- Debug logging checks `TempFlyCommand.isDebugMode()` before logging expensive operations
