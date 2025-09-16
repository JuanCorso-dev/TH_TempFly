# TH_TempFly

A comprehensive temporary flight plugin for Minecraft servers with SQL/Redis support, PlaceholderAPI integration, and customizable messages.

## 🚀 Features

- **Temporary Flight System**: Give players limited flight time
- **Infinite Flight Permission**: Special permission for unlimited flight
- **Database Support**: SQLite and MySQL support with HikariCP
- **Redis Synchronization**: Multi-server synchronization support
- **PlaceholderAPI Integration**: Display flight time in other plugins
- **Customizable Messages**: Fully customizable messages in English
- **Permission System**: Granular permission control
- **Auto-save**: Automatic data persistence

## 📋 Requirements

- **Minecraft**: 1.20+
- **Java**: 17+
- **Server Software**: Paper, Spigot, or Bukkit
- **Optional**: PlaceholderAPI for placeholders

## 📦 Installation

1. Download the latest `TH_TempFly-1.1.1.jar` from releases
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/TH_TempFly/config.yml`

## 🎮 Commands

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

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `thtempfly.use` | Use admin commands | op |
| `thtempfly.admin` | Manage other players' flight time | op |
| `thtempfly.fly.use` | Use /fly command | true |
| `thtempfly.fly.infinite` | Infinite flight permission | op |

## 📊 Placeholders (PlaceholderAPI)

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%thtempfly_time%` | Formatted remaining time | "1h 30m" or "Infinite" |
| `%thtempfly_time_seconds%` | Time in seconds | "3600" or "∞" |
| `%thtempfly_time_minutes%` | Time in minutes | "90" or "∞" |
| `%thtempfly_time_hours%` | Time in hours | "2" or "∞" |
| `%thtempfly_has_time%` | Has flight time | "true" or "false" |
| `%thtempfly_can_fly%` | Can fly | "true" or "false" |
| `%thtempfly_status%` | Flight status | "flying", "can_fly", or "no_time" |

## ⚙️ Configuration

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
    password: ""
  sync:
    enabled: true
    interval: 60
    channel: "tempfly"
```

### Message Customization
Edit `plugins/TH_TempFly/messages.yml` to customize all plugin messages:

```yaml
fly:
  enabled: "&aFlight enabled. Time remaining: &e{time}"
  infinite-enabled: "&aInfinite flight enabled!"
  disabled: "&cFlight disabled."
```

## 🛠️ Building

1. Clone the repository
2. Run `mvn clean package`
3. Find the compiled JAR in `target/TH_TempFly-1.1.1.jar`

## 📝 Changelog

### v1.1.1
- ✅ All messages converted to English
- ✅ Improved Redis configuration
- ✅ Enhanced PlaceholderAPI support
- ✅ Infinite flight permission system
- ✅ Customizable message system

### v1.0.0
- ✅ Initial release
- ✅ Basic flight system
- ✅ SQLite/MySQL support
- ✅ PlaceholderAPI integration

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**JuanCorso**
- GitHub: [@djkingcraftero89](https://github.com/djkingcraftero89)

## 🙏 Acknowledgments

- PaperMC for the excellent API
- PlaceholderAPI team for the placeholder system
- HikariCP for the connection pooling
- Lettuce for Redis support

---

**⭐ If you like this plugin, please give it a star!**
