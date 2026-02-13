# Integración con Vulcan Anti-Cheat

## ¿Qué es esta integración?

TH_TempFly ahora incluye soporte automático para **Vulcan Anti-Cheat**. Cuando un jugador tiene fly activo (temporal o infinito), el plugin automáticamente le indica a Vulcan que **NO** detecte ese vuelo como hack.

## ¿Cómo funciona?

La integración es **completamente automática**:

1. **Cuando un jugador activa el fly** (con `/fly` o al conectarse con tiempo restante):
   - TH_TempFly añade excepciones en Vulcan para los checks de Flight, Elytra, Speed y Motion
   - El jugador puede volar libremente sin ser detectado como hacker

2. **Cuando un jugador desactiva el fly** (con `/fly`, cuando se acaba el tiempo, o al salir del servidor):
   - TH_TempFly remueve las excepciones en Vulcan
   - Vulcan vuelve a detectar hacks normalmente para ese jugador

## Instalación

### Paso 1: Agregar Vulcan API al proyecto (ya está hecho)

El `pom.xml` ya incluye la dependencia:

```xml
<dependency>
  <groupId>com.github.freppp</groupId>
  <artifactId>vulcan-api</artifactId>
  <version>2.8.8</version>
  <scope>provided</scope>
</dependency>
```

### Paso 2: Instalar Vulcan en el servidor

1. Descarga Vulcan Anti-Cheat desde SpigotMC o tu fuente preferida
2. Coloca `Vulcan.jar` en la carpeta `plugins/` de tu servidor
3. Coloca `TH_TempFly.jar` en la carpeta `plugins/` de tu servidor
4. Reinicia el servidor

### Paso 3: Verificar la integración

Al iniciar el servidor, busca este mensaje en los logs:

```
[TH_TempFly] Integración con Vulcan Anti-Cheat activada correctamente!
```

Si ves este mensaje, ¡la integración está funcionando!

Si Vulcan no está instalado, verás:

```
[TH_TempFly] Vulcan Anti-Cheat no encontrado, integración deshabilitada.
```

## Configuración

**¡No se requiere configuración!** La integración funciona automáticamente.

El archivo `config.yml` incluye una sección informativa:

```yaml
# Vulcan Anti-Cheat Integration
# Automatically exempts players with active fly from Vulcan checks
# No configuration needed - works automatically when Vulcan is installed
vulcan:
  # Integration info: This plugin automatically detects Vulcan and enables exemptions
  # When players have fly active, they won't be detected as hackers by Vulcan
  # No additional setup required!
```

## Checks de Vulcan Exceptuados

Cuando un jugador tiene fly activo, se exceptúan los siguientes checks de Vulcan:

- **Flight**: Detección principal de vuelo no autorizado
- **Elytra**: Detección de vuelo con elytras
- **Speed**: Detección de velocidad de vuelo
- **Motion**: Detección de movimientos anormales en el aire

## Compatibilidad

- **Versión de Vulcan**: 2.8.8 (compatible con versiones anteriores y posteriores)
- **Versión de Minecraft**: 1.20+ (según tu configuración de Paper/Spigot)
- **Multi-servidor**: La integración funciona en configuraciones multi-servidor con Redis

## Ejemplo de Uso

### Caso 1: Jugador con fly temporal

```
1. Admin ejecuta: /tempfly give Steve 1h
2. Steve ejecuta: /fly
3. TH_TempFly automáticamente añade excepciones en Vulcan para Steve
4. Steve puede volar sin ser detectado
5. Cuando se acaba el tiempo, TH_TempFly remueve las excepciones
6. Vulcan vuelve a detectar si Steve intenta usar hacks de fly
```

### Caso 2: Jugador con fly infinito

```
1. Admin da permiso: thtempfly.fly.infinite a Steve
2. Steve ejecuta: /fly
3. TH_TempFly automáticamente añade excepciones en Vulcan para Steve
4. Steve puede volar mientras tenga el permiso
5. Cuando Steve ejecuta /fly de nuevo para desactivarlo:
   - TH_TempFly remueve las excepciones
   - Vulcan vuelve a detectar normalmente
```

## Troubleshooting

### El jugador es expulsado por Vulcan aunque tenga fly

**Solución 1**: Verifica que Vulcan esté instalado y la integración activada:
```
Busca en los logs: "Integración con Vulcan Anti-Cheat activada correctamente!"
```

**Solución 2**: Verifica que el jugador tenga fly activo:
```
/tempfly check <jugador>
```

**Solución 3**: Activa el modo debug para ver logs detallados:
```
/atempfly debug true
```

### La integración no se activa

1. Verifica que Vulcan esté en la carpeta `plugins/`
2. Verifica que Vulcan se cargue ANTES de TH_TempFly
3. Revisa el archivo `plugin.yml` - debe incluir `softdepend: [PlaceholderAPI, WorldGuard, Vulcan]`

## Soporte Multi-Servidor con Redis

La integración con Vulcan funciona perfectamente en configuraciones multi-servidor:

- Cuando un jugador cambia de servidor, su tiempo de fly se sincroniza vía Redis
- TH_TempFly en el nuevo servidor automáticamente añade las excepciones en Vulcan
- No se requiere configuración adicional

## Código Técnico

La implementación se encuentra en:
- `VulcanIntegration.java`: Maneja la comunicación con Vulcan API
- `FlyManager.java`: Llama a VulcanIntegration cuando se activa/desactiva fly
- `FlyCommand.java`: Llama a VulcanIntegration en el comando /fly
- `TH_TempFly.java`: Inicializa la integración al arrancar el plugin

## Notas Importantes

1. Las excepciones solo se aplican cuando el jugador tiene fly **ACTIVO**
2. Si el fly se desactiva (por cualquier razón), las excepciones se remueven inmediatamente
3. La integración funciona con permisos de fly infinito (`thtempfly.fly.infinite`)
4. Es compatible con restricciones de mundos y regiones de WorldGuard

## Créditos

- **Vulcan Anti-Cheat**: [freppp](https://github.com/freppp)
- **Configuración de referencia**: [Vulcan Config Gist](https://gist.github.com/freppp/6f669d666835011f3661025c2d9533e2)

