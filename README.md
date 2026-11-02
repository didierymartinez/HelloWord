# Entreno

App Android para el gym que te guía en la rutina y te avisa en el **Xiaomi Watch S1 Active**.

El S1 Active no admite apps propias, así que la app corre en el teléfono y el reloj
recibe los avisos como notificaciones (por medio de Mi Fitness): qué serie toca, cuándo
termina el descanso y cuándo acaba la bici.

| Inicio | Serie | Descanso | Resumen |
|---|---|---|---|
| ![Inicio](docs/capturas/home.png) | ![Serie](docs/capturas/strength.png) | ![Descanso](docs/capturas/rest.png) | ![Resumen](docs/capturas/summary.png) |

## Qué hace

- **Hoy toca:** sugiere el grupo según tu rotación (Pecho → Espalda → Brazo → Pierna → Hombro).
  No depende del día de la semana: si faltas, no se descuadra. Puedes elegir otro grupo,
  o varios, si el entrenador cambia la rutina.
- **Sesión completa:** 🚴 10 min de bici → pesas → 🚴 15 min de bici. Los tiempos se
  cambian en Ajustes.
- **Pesas:** serie, reps y peso objetivo. Recuerda lo que hiciste la última vez, marca la
  serie con un toque y arranca el descanso solo.
- **Cambios sobre la marcha:** cambiar un ejercicio por otro del mismo músculo (máquina
  ocupada), añadir lo que mande el entrenador, saltar, reordenar o deshacer la última serie.
- **Avisos al reloj:** "▶ Press banca · 2/4 — 10 × 60 kg", "⏱ 10 s de descanso",
  "🚴 Calentamiento terminado".
- **Para el entrenador:** historial, mapa de descanso muscular y un botón para compartir
  los últimos 14 días por WhatsApp.
- **Resumen:** duración, series, volumen, músculos trabajados y récords personales.

## Instalación

1. Descarga `app-debug.apk`: lo genera GitHub Actions en cada push
   (pestaña **Actions** → último build → *Artifacts*).
2. Ábrelo en el teléfono y permite instalar apps de esta fuente.
3. Acepta el permiso de notificaciones al abrir la app.

## Configurar el reloj (una sola vez)

1. **Mi Fitness** → tu reloj → **Notificaciones de apps** → activa y marca **Entreno**.
2. Ajustes de Android → Batería: quita la optimización de batería a **Entreno** y a
   **Mi Fitness** (la app tiene un botón para abrir esos ajustes).
3. En la app: **Ajustes → Enviar aviso de prueba al reloj**, con el teléfono bloqueado.
   Si vibra el reloj, ya está.

> Consejo: deja apagado "Mantener la pantalla encendida". Mi Fitness suele reenviar mejor
> los avisos con la pantalla apagada y así evitas toques sin querer en el bolsillo.
> Desde la pantalla de bloqueo puedes marcar "✓ Serie hecha", "+30 s" o "Saltar".

**Pulso y calorías:** la app no puede leer el pulso del reloj. Si los quieres, inicia
también en el reloj "Bicicleta estática" o "Entrenamiento de fuerza".

## Compilar

Requiere JDK 17+ y el SDK de Android (API 35).

```bash
./gradlew testDebugUnitTest   # tests
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug/
```

Diseño y próximos pasos: [docs/DISENO.md](docs/DISENO.md).
