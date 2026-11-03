# Diseño de Entreno

## Contexto

- **Reloj:** Xiaomi Watch S1 Active. Su sistema es cerrado, sin Vela OS ni SDK, así que no se
  pueden instalar apps propias. Solo se puede aprovechar que **muestra y vibra con las
  notificaciones** que reenvía Mi Fitness.
- **Teléfono:** Android.
- **Rutina del usuario:** bici estática 10 min al inicio y 15 al final. La rutina diaria
  va por grupos (Pecho, Espalda, Brazo, Pierna…), pero cambia según el entrenador, los
  días que falta o las máquinas ocupadas.

## Decisiones

| Decisión | Motivo |
|---|---|
| App nativa Android (Kotlin + Compose) | Hace falta un servicio en primer plano para que los temporizadores y avisos funcionen con la pantalla apagada. Una web no lo garantiza. |
| Avisos como notificaciones de alta prioridad, una por evento | Es lo que Mi Fitness reenvía al reloj. Las notificaciones fijas ("ongoing") normalmente no se reenvían, por eso la de la sesión es aparte y silenciosa. |
| Rotación en vez de calendario | Si un día no va, la sugerencia sigue en orden. Se saltan los grupos trabajados hace menos de 2 días. |
| Plantillas por grupo y plan editable en vivo | El plan es solo una propuesta: se cambia, añade, salta y reordena durante la sesión. Lo que se guarda es lo que se hizo de verdad. |
| Datos en JSON local | Simple y sin cuentas. Escritura atómica, y un archivo dañado se aparta en vez de sobrescribirse. |
| Lógica pura en `SessionEngine`, `Stats` y `Heart` | Se prueba con tests de JVM sin emulador. |
| Pulso desde Health Connect, solo lectura y al ver la sesión | El S1 Active no da el pulso en vivo. Mi Fitness lo copia a Health Connect al sincronizar, así que se lee después de entrenar y se guarda en la sesión. |
| Estadísticas del pulso con los datos originales, gráfica con promedios de 15 s | Promediar aplana los picos de cada serie. Así el máximo es exacto y el historial no crece demasiado (unos 6 KB por sesión). |

## Estructura

```
app/src/main/java/io/github/didierymartinez/entreno/
├── data/      Modelo, catálogo de ejercicios y plantillas, repositorio JSON
├── domain/    SessionEngine (máquina de estados de la sesión), Stats, formato
├── session/   SessionManager (estado compartido), WorkoutService (primer plano), Notifier,
│              HealthConnect (lectura de pulso y calorías)
└── ui/        Pantallas Compose: inicio, sesión, historial, mapa de descanso, ajustes
```

Fases de la sesión: `WARMUP → STRENGTH ⇄ REST → COOLDOWN → SUMMARY`.

## Avisos que llegan al reloj

| Momento | Título | Texto |
|---|---|---|
| Mitad de la bici | 🚴 Mitad del calentamiento | Quedan 5:00 |
| Fin de la bici | 🚴 Calentamiento terminado | Ahora: Press banca · 4×10 |
| 10 s antes del fin del descanso (opcional) | ⏱ 10 s de descanso | Sig.: Press banca · Serie 2/4 |
| Fin del descanso | ▶ Press banca · 2/4 | ¡Vamos! 10 × 60 kg |
| Fin de la bici final | 🚴 Bici final terminada | ¡Buen entreno! Guarda la sesión |

## Limitaciones conocidas

- **Pulso solo después de entrenar:** el S1 Active no comparte el pulso en tiempo real con
  otras apps. Llega por Health Connect cuando Mi Fitness sincroniza.
- **Health Connect solo deja leer los 30 días anteriores al permiso.** Las sesiones más
  antiguas que eso no tendrán pulso.
- **Sin conteo automático de repeticiones:** no hay acceso al acelerómetro del reloj.
- **Marcar la serie se hace en el teléfono,** en la app o en la notificación de la pantalla de
  bloqueo. Los botones de las notificaciones no se pueden pulsar desde el reloj.
- **El reenvío depende de Mi Fitness** y de los ajustes de batería del teléfono. Hay que
  probarlo con el botón de aviso de prueba.

## Próximos pasos

1. **Editor de plantillas** por grupo (ejercicios, series, reps y descanso por defecto).
2. **Gráficas de progreso** por ejercicio (peso máximo y volumen semanal por músculo).
3. **Zonas de pulso** (con la edad o el pulso máximo) y tiempo en cada zona.
4. Exportar e importar el historial (copia de seguridad).
