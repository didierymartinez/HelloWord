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
| Lógica pura en `SessionEngine` y `Stats` | Se prueba con tests de JVM sin emulador. |

## Estructura

```
app/src/main/java/io/github/didierymartinez/entreno/
├── data/      Modelo, catálogo de ejercicios y plantillas, repositorio JSON
├── domain/    SessionEngine (máquina de estados de la sesión), Stats, formato
├── session/   SessionManager (estado compartido), WorkoutService (primer plano), Notifier
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

- **Sin pulso en la app:** el S1 Active no comparte el pulso en tiempo real con otras apps.
- **Sin conteo automático de repeticiones:** no hay acceso al acelerómetro del reloj.
- **Marcar la serie se hace en el teléfono,** en la app o en la notificación de la pantalla de
  bloqueo. Los botones de las notificaciones no se pueden pulsar desde el reloj.
- **El reenvío depende de Mi Fitness** y de los ajustes de batería del teléfono. Hay que
  probarlo con el botón de aviso de prueba.

## Próximos pasos

1. **Health Connect:** importar el pulso y las calorías de las sesiones del reloj
   (Mi Fitness → Health Connect) y unirlos a cada entreno por hora.
2. **Editor de plantillas** por grupo (ejercicios, series, reps y descanso por defecto).
3. **Gráficas de progreso** por ejercicio (peso máximo y volumen semanal por músculo).
4. Exportar e importar el historial (copia de seguridad).
