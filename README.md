# TH_TempFly

[![Version](https://img.shields.io/github/v/release/JuanCorso-dev/TH_TempFly?label=version&color=blue)](https://github.com/JuanCorso-dev/TH_TempFly/releases/latest)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21%20%E2%80%93%2026.x-brightgreen)](https://papermc.io/downloads/paper)
[![Java](https://img.shields.io/badge/java-21%2B%20%7C%2025%2B%20on%2026.x-orange)](https://adoptium.net/)
[![Paper API](https://img.shields.io/badge/paper--api-1.21.4-0288d1)](https://papermc.io/)
[![License](https://img.shields.io/github/license/JuanCorso-dev/TH_TempFly?color=lightgrey)](LICENSE)
[![bStats](https://img.shields.io/badge/bStats-27511-ff6600)](https://bstats.org/plugin/bukkit/TH_TempFly/27511)
[![Build](https://github.com/JuanCorso-dev/TH_TempFly/actions/workflows/build.yml/badge.svg)](https://github.com/JuanCorso-dev/TH_TempFly/actions/workflows/build.yml)

A comprehensive temporary flight plugin for Minecraft servers with SQL/Redis support, PlaceholderAPI integration, WorldGuard region restrictions, and customizable messages.

A single build supports both Minecraft version schemes: the legacy `1.21.x`
numbering and the year-based `26.x` drops introduced in 2026.

## Features

- **Temporary Flight System**: Give players limited flight time
- **Infinite Flight Permission**: Special permission for unlimited flight
- **WorldGuard Integration**: Block flight in specific regions or entire worlds
- **Database Support**: SQLite and MySQL support with HikariCP
- **Redis Synchronization**: Multi-server synchronization support
- **PlaceholderAPI Integration**: Display flight time in other plugins
- **Customizable Messages**: Fully customizable messages in English
- **Permission System**: Granular permission control
- **Auto-save**: Automatic data persistence
- **bStats Integration**: Anonymous usage statistics to help improve the plugin
- **Title Warnings**: Visual countdown warnings when fly time is running out
- **Time Freeze Mode**: Option to freeze time when players are offline
- **Flight Restrictions**: Automatically disable flight when entering blocked zones

## Requirements

- **Minecraft**: 1.21+ or 26.1+ (both schemes supported by the same JAR)
- **Java**: 21+ on 1.21.x, 25+ on 26.x (required by Minecraft itself)
- **Server Software**: Paper (or a Paper fork such as Purpur)
- **Optional**: PlaceholderAPI for placeholders
- **Optional**: WorldGuard for region-based flight restrictions

The plugin checks the Java and Minecraft versions on startup and disables
itself with an explanatory log message when they are not met.

> **Moving to 26.x?** Minecraft requires Java 25 from 26.1 onwards, so the
> server will not start on Java 21 no matter which plugins are installed.
> That requirement comes from Minecraft, not from this plugin, which asks
> only for Java 21 and runs fine on newer runtimes.

> **Note on server software:** Paper is required, not merely recommended. The
> plugin uses the Paper-only `ServerBuildInfo` API, so it will not run on
> plain Spigot or CraftBukkit.

### Minecraft version support

Minecraft moved to year-based versioning in 2026, so both schemes are in use:

| Scheme | Versions | Java required by the server | Supported |
|--------|----------|-----------------------------|-----------|
| Legacy | 1.21, 1.21.4, 1.21.11 | 21+ | Yes |
| Year-based | 26.1, 26.2, 26.3 and later | 25+ | Yes |
| Legacy | 1.20.x and older | — | No |

The 2026 drops are 26.1 (Tiny Takeover), 26.2 (Chaos Cubed) and 26.3
(Wilderness Bound).

`plugin.yml` declares `api-version: 1.21` on purpose. That field is a minimum
floor rather than a target: a server older than the declared version refuses
to load the plugin, so keeping it at `1.21` is what preserves compatibility
with 1.21.x while 26.x servers accept it without issue.

## Installation

1. Download the latest `TH_TempFly-1.2.4.jar` from releases
2. Place it in your server's `plugins` folder
3. (Optional) Install WorldGuard if you want region-based restrictions
4. Restart your server
5. Configure the plugin in `plugins/TH_TempFly/config.yml`

## Commands

### Admin Commands
- `/tempfly give <player> <time>` - Set player's flight time
- `/tempfly add <player> <time>` - Add flight time to player
- `/tempfly remove <player> <time>` - Remove flight time from player
- `/tempfly check <player>` - Check player's remaining flight time
- `/tempfly reload` - Reload plugin configuration
- `/tempfly version` - Check current version and available updates
- `/tempfly migrate` - Migrate flight data from another plugin
- `/atempfly debug <true|false>` - Toggle debug logging

Alias: `/tfly` can be used instead of `/tempfly`.

### Player Commands
- `/fly` - Toggle flight mode (if you have time/permission)
- `/fly on` - Enable flight
- `/fly off` - Disable flight
- `/fly check` - Check your remaining flight time

### Time Formats
- `30s` - 30 seconds
- `5m` - 5 minutes
- `2h` - 2 hours
- `1d` - 1 day
- `30` - 30 seconds (no suffix)

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `thtempfly.use` | Use admin commands | op |
| `thtempfly.admin` | Manage other players' flight time | op |
| `thtempfly.fly.use` | Use /fly command | true |
| `thtempfly.fly.infinite` | Infinite flight permission | op |
| `thtempfly.fly.bypass` | Bypass flight restrictions in blocked worlds and regions | op |

## Placeholders (PlaceholderAPI)

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%thtempfly_time%` | Formatted remaining time | "1h 30m" or "Infinite" |
| `%thtempfly_time_seconds%` | Time in seconds | "3600" or "-1" |
| `%thtempfly_time_minutes%` | Time in minutes | "90" or "-1" |
| `%thtempfly_time_hours%` | Time in hours | "2" or "-1" |
| `%thtempfly_has_time%` | Has flight time | "true" or "false" |
| `%thtempfly_can_fly%` | Can fly | "true" or "false" |
| `%thtempfly_status%` | Flight status | "flying", "can_fly", or "no_time" |

## Configuration

### Database Configuration
```yaml
storage:
  type: SQLITE  # or MYSQL
  sqlite:
    file: data.db
  mysql:
    host: localhost
    port: 3306
    database: tempfly
    username: root
    password: password
```

### Redis Configuration
```yaml
redis:
  enabled: false
  server-name: "server1"
  credentials:
    host: "localhost"
    port: 6379
    username: ""
    password: ""
    database: 0
  sync:
    channel: "TH-TempFly:updates"
    log-received: false
    full-broadcast-enabled: false
```

### Message Customization
Edit `plugins/TH_TempFly/messages.yml` to customize all plugin messages:

```yaml
fly:
  enabled: "&aFlight enabled. Time remaining: &e{time}"
  infinite-enabled: "&aInfinite flight enabled!"
  disabled: "&cFlight disabled."
  restrictions:
    world-blocked: "&cFlight is disabled in this world: &e{world}"
    region-blocked: "&cFlight is disabled in this region: &e{region}"
```

### Flight Restrictions Configuration

```yaml
fly:
  restrictions:
    enabled: true
    blocked-worlds: ["world_nether", "pvp_arena"]
    blocked-regions: ["spawn", "safezone"]
```

**Bypass Permissions:**
- Players with `thtempfly.fly.bypass` permission can fly in blocked worlds and regions
- This permission is useful for staff members who need to access restricted areas
- Default: Only operators have this permission

### Performance Tuning

Region and permission lookups are cached, which matters on servers with many
concurrent players. The defaults suit most setups:

```yaml
fly:
  performance:
    # How long a permission lookup stays cached, in milliseconds.
    # Lower reacts faster to permission changes; higher costs less.
    permission-cache-duration-ms: 1000
  restrictions:
    # Run the expensive region check only after the player moves this many
    # blocks. 10 suits large servers, 5 suits smaller ones.
    check-interval-blocks: 10
    # Number of chunks to keep region data cached for.
    region-cache-size: 1000
    # How long cached region data stays valid, in seconds.
    region-cache-ttl-seconds: 30
```

## Building

### Development Build (No version change)
```bash
mvn clean package
```

### Release Build (Auto-increments version)
```bash
mvn clean package -Prelease
```
This will automatically increment the version (e.g., 1.2.0 → 1.2.1) and build the JAR.

Find the compiled JAR in `target/TH_TempFly-[version].jar`

## Changelog

### v1.2.4 (Minecraft 26.x support)
- Support for the year-based Minecraft version scheme (26.1, 26.2, 26.3 and later)
- The same JAR still runs on 1.21.x; no separate build is required
- Fixed the startup version check, which compared both version schemes on a
  single numeric line and accepted meaningless versions such as `2.0`
- Restored the `cache` package, which an unanchored `.gitignore` rule had been
  excluding from the repository, breaking the build on a fresh clone
- Documented the performance tuning keys in this README
- Documented that Minecraft requires Java 25 from 26.1 onwards. The plugin
  still targets Java 21 so that 1.21.x servers keep working; a Java 21 build
  runs unchanged on newer runtimes.

### v1.2.0 (WorldGuard Integration & Update Checker)
- Added WorldGuard integration for region-based restrictions
- Block flight in specific worlds (configurable list)
- Block flight in specific WorldGuard regions
- Automatic flight disable when entering blocked zones
- Enhanced messages for restriction notifications
- Auto-update checker from GitHub releases
- Auto-version increment system for release builds
- Added `/tempfly version` command
- Admin notifications for new updates

### v1.1.0 (Stable Release)
- All code and comments translated to English
- Removed unused code and optimized performance
- Improved Redis configuration and synchronization
- Enhanced PlaceholderAPI support with more placeholders
- Infinite flight permission system
- Title warning system when fly time is running out
- Freeze time mode for offline players
- Full message customization system
- Debug mode for troubleshooting
- Optimized database operations with HikariCP
- bStats integration for anonymous usage statistics

### v1.0.0
- Initial release
- Basic flight system
- SQLite/MySQL support
- PlaceholderAPI integration

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

**JuanCorso**
- GitHub: [@djkingcraftero89](https://github.com/djkingcraftero89)

## Statistics

This plugin uses bStats to collect anonymous usage statistics. This helps us understand how the plugin is being used and improve it. You can opt-out by editing `plugins/bStats/config.yml` if you prefer.

View our statistics: [bStats Page](https://bstats.org/plugin/bukkit/TH_TempFly/27511)

## Acknowledgments

- PaperMC for the excellent API
- PlaceholderAPI team for the placeholder system
- HikariCP for the connection pooling
- Lettuce for Redis support
- bStats for the metrics system

---

If you like this plugin, please give it a star on GitHub!
