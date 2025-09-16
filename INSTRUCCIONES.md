# TH_TempFly Plugin - Instrucciones de Uso

## 📦 Instalación

1. **Copia el archivo JAR**: `TH_TempFly-1.1.1.jar` a la carpeta `plugins` de tu servidor
2. **Reinicia el servidor** para cargar el plugin
3. **Verifica en la consola** que aparezcan los mensajes de inicialización
4. **Personaliza mensajes** editando el archivo `messages.yml` en la carpeta del plugin

## 🔧 Configuración

El plugin creará automáticamente un archivo `config.yml` en la carpeta del plugin con la configuración por defecto.

### Configuración de Base de Datos
- **Por defecto**: SQLite (archivo `data.db` en la carpeta del plugin)
- **Para MySQL**: Cambia `storage.type` a `MYSQL` y configura los datos de conexión

## 🎮 Comandos

### Para Administradores:
- `/tempfly add <jugador> <tiempo>` - Agregar tiempo de vuelo
- `/tempfly give <jugador> <tiempo>` - Establecer tiempo de vuelo
- `/tempfly remove <jugador> <tiempo>` - Quitar tiempo de vuelo
- `/tempfly check <jugador>` - Ver tiempo restante
- `/tempfly reload` - Recargar configuración

### Para Jugadores:
- `/fly` - Activar/desactivar vuelo (si tienen tiempo restante)

### Ejemplos de tiempo:
- `30s` - 30 segundos
- `5m` - 5 minutos
- `2h` - 2 horas
- `1d` - 1 día
- `30` - 30 segundos (sin sufijo)

## 🔐 Permisos

- `thtempfly.use` - Usar comandos de administración (default: op)
- `thtempfly.admin` - Gestionar tiempo de otros jugadores (default: op)
- `thtempfly.fly.use` - Usar el comando /fly (default: true)
- `thtempfly.fly.infinite` - **NUEVO**: Vuelo infinito (default: op)

## 📊 Placeholders (PlaceholderAPI)

Si tienes PlaceholderAPI instalado, puedes usar estos placeholders:

- `%thtempfly_time%` - Tiempo restante formateado (ej: "1h 30m" o "Infinite")
- `%thtempfly_time_seconds%` - Tiempo restante en segundos (∞ para infinito)
- `%thtempfly_time_minutes%` - Tiempo restante en minutos (∞ para infinito)
- `%thtempfly_time_hours%` - Tiempo restante en horas (∞ para infinito)
- `%thtempfly_has_time%` - "true" si tiene tiempo, "false" si no
- `%thtempfly_can_fly%` - "true" si puede volar, "false" si no
- `%thtempfly_status%` - Estado: "flying", "can_fly", o "no_time"

## 🎨 Personalización de Mensajes

**NUEVO**: El plugin ahora incluye un archivo `messages.yml` que puedes personalizar:

1. **Edita** el archivo `messages.yml` en la carpeta del plugin
2. **Usa códigos de color** de Minecraft (&) para personalizar los mensajes
3. **Recarga** con `/tempfly reload` para aplicar cambios
4. **Variables disponibles**: `{player}`, `{time}`, `{change}`

### Ejemplo de personalización:
```yaml
fly:
  enabled: "&aFlight enabled! Time remaining: &e{time}"
  infinite-enabled: "&a&lInfinite flight enabled!"
```

## 🔧 Configuración de Redis

**NUEVO**: Configuración de Redis mejorada basada en el plugin TH_Backpacks:

```yaml
redis:
  enabled: false
  server-name: "server1"  # Cambia para cada servidor
  credentials:
    host: "localhost"
    port: 6379
    password: ""
    use_ssl: false
  sync:
    enabled: true
    interval: 60
    channel: "tempfly"
```

## 🐛 Solución de Problemas

### El plugin no se carga:
1. Verifica que el archivo JAR esté en la carpeta `plugins`
2. Revisa la consola del servidor para errores
3. Asegúrate de que el servidor sea compatible (1.20+)

### El plugin no crea la carpeta de datos:
1. Verifica los permisos de escritura en la carpeta del servidor
2. Revisa los logs del servidor para errores de base de datos

### Los comandos no funcionan:
1. Verifica que tengas los permisos necesarios
2. Usa `/tempfly` sin argumentos para ver la ayuda

## 📝 Logs de Debug

El plugin ahora incluye logging detallado. En la consola del servidor verás:
- "Iniciando TH_TempFly plugin..."
- "Configuración cargada correctamente"
- "Tipo de base de datos: SQLite/MySQL"
- "Base de datos inicializada correctamente"
- "Comandos registrados correctamente"
- "TH_TempFly plugin habilitado correctamente!"

Si ves algún error, revisa el stack trace completo para identificar el problema.
