# TH_TempFly

A comprehensive temporary flight plugin for Minecraft servers with SQL/Redis support, PlaceholderAPI integration, WorldGuard region restrictions, and customizable messages.

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

- **Minecraft**: 1.20+
- **Java**: 17+
- **Server Software**: Paper, Spigot, or Bukkit
- **Optional**: PlaceholderAPI for placeholders
- **Optional**: WorldGuard for region-based flight restrictions

## Installation

1. Download the latest `TH_TempFly-1.2.0.jar` from releases
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

### Player Commands
- `/fly` - Toggle flight mode (if you have time/permission)

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

## Building

1. Clone the repository
2. Run `mvn clean package`
3. Find the compiled JAR in `target/TH_TempFly-1.2.0.jar`

## Changelog

### v1.2.0 (WorldGuard Integration)
- Added WorldGuard integration for region-based restrictions
- Block flight in specific worlds (configurable list)
- Block flight in specific WorldGuard regions
- Automatic flight disable when entering blocked zones
- Enhanced messages for restriction notifications

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
