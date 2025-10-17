# TH_TempFly Plugin - User Guide

## Installation

1. Copy the JAR file `TH_TempFly-1.1.0.jar` to your server's `plugins` folder
2. Restart the server to load the plugin
3. Verify in the console that initialization messages appear
4. Customize messages by editing the `messages.yml` file in the plugin folder

## Configuration

The plugin will automatically create a `config.yml` file in the plugin folder with default configuration.

### Database Configuration
- Default: SQLite (`data.db` file in the plugin folder)
- For MySQL: Change `storage.type` to `MYSQL` and configure connection details

## Commands

### For Administrators:
- `/tempfly add <player> <time>` - Add flight time to player
- `/tempfly give <player> <time>` - Set flight time for player
- `/tempfly remove <player> <time>` - Remove flight time from player
- `/tempfly check <player>` - Check remaining time for player
- `/tempfly reload` - Reload configuration
- `/atempfly debug <true|false>` - Toggle debug mode

### For Players:
- `/fly` - Toggle flight mode (if they have remaining time)

### Time Format Examples:
- `30s` - 30 seconds
- `5m` - 5 minutes
- `2h` - 2 hours
- `1d` - 1 day
- `30` - 30 seconds (without suffix)

## Permissions

- `thtempfly.use` - Use admin commands (default: op)
- `thtempfly.admin` - Manage other players' flight time (default: op)
- `thtempfly.fly.use` - Use the /fly command (default: true)
- `thtempfly.fly.infinite` - Infinite flight time (default: op)

## Placeholders (PlaceholderAPI)

If you have PlaceholderAPI installed, you can use these placeholders:

- `%thtempfly_time%` - Formatted remaining time (e.g., "1h 30m" or "Infinite")
- `%thtempfly_time_seconds%` - Remaining time in seconds (-1 for infinite)
- `%thtempfly_time_minutes%` - Remaining time in minutes (-1 for infinite)
- `%thtempfly_time_hours%` - Remaining time in hours (-1 for infinite)
- `%thtempfly_has_time%` - "true" if has time, "false" if not
- `%thtempfly_can_fly%` - "true" if can fly, "false" if not
- `%thtempfly_status%` - Status: "flying", "can_fly", or "no_time"

## Message Customization

The plugin includes a `messages.yml` file that you can customize:

1. Edit the `messages.yml` file in the plugin folder
2. Use Minecraft color codes (&) to customize messages
3. Reload with `/tempfly reload` to apply changes
4. Available variables: `{player}`, `{time}`, `{change}`, `{seconds}`

### Customization Example:
```yaml
fly:
  enabled: "&aFlight enabled! Time remaining: &e{time}"
  infinite-enabled: "&a&lInfinite flight enabled!"
```

## Redis Configuration

Redis configuration for multi-server synchronization:

```yaml
redis:
  enabled: false
  server-name: "server1"  # Change for each server
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

## Troubleshooting

### Plugin does not load:
1. Verify the JAR file is in the `plugins` folder
2. Check the server console for errors
3. Ensure the server is compatible (1.20+)

### Plugin does not create data folder:
1. Check write permissions in the server folder
2. Review server logs for database errors

### Commands do not work:
1. Verify you have the necessary permissions
2. Use `/tempfly` without arguments to see help

## Debug Logging

The plugin includes detailed logging. In the server console you will see:
- "Starting TH_TempFly plugin..."
- "Configuration loaded successfully"
- "Database type: SQLite/MySQL"
- "Database initialized successfully"
- "Commands registered successfully"
- "TH_TempFly plugin enabled successfully!"

If you see any errors, review the full stack trace to identify the problem.

## Additional Features

### Title Warning System
- Visual countdown warnings when fly time is running low
- Configurable warning threshold
- Customizable title messages

### Time Freeze Mode
- Option to freeze time when players are offline
- Time only counts down while playing
- Configurable in config.yml

### bStats Integration
- Anonymous usage statistics
- Helps improve the plugin
- Can be disabled in `plugins/bStats/config.yml`
