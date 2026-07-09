# Historia de Usuario

**Como** ejecutante,
**Quiero** poder activar un modo de descarga (Deload) que ajuste automáticamente las cargas al 60%, mantenerlo durante un microciclo completo, que al finalizar el sistema calcule mis cargas de reinicio al 90%, y que el sistema lleve un conteo preciso de todos mis microciclos completados,
**Para** recuperar mi SNC y tejidos conectivos de forma estructurada, retomar el entrenamiento aprovechando la supercompensación, y tener una referencia clara de cuántos ciclos completos de entrenamiento llevo acumulados.

## Descripción

Esta historia define el protocolo de descarga (Deload) y el conteo de microciclos. El ejecutante puede activar un modo de descarga que ajusta automáticamente las cargas al 60% de la carga habitual, mantiene 4 series por ejercicio, repeticiones en el límite inferior del rango (8), y RIR objetivo de 4-5 durante exactamente 1 microciclo completo (6 sesiones). Al finalizar el microciclo de descarga, el sistema calcula las cargas de reinicio al 90% de la última carga pre-descarga, transiciona todos los ejercicios de `IN_DELOAD` a `IN_PROGRESSION`, y resuelve las alertas `MODULE_REQUIRES_DELOAD`. El sistema también lleva un conteo persistente de microciclos completados basado en la secuencia de sesiones (A-B-C-A-B-C), independientemente de los días naturales.

---

## Criterios de Aceptación

### Bloque A — Activación y Gestión del Ciclo de Descarga (RF38, RF39, RF40)

#### CA-14.01 — Activación del modo Descarga

**Dado que** el sistema ha señalado que un módulo requiere descarga (HU-12) o el ejecutante decide proactivamente descargar,
**cuando** el ejecutante activa el modo de Descarga,
**entonces** el sistema ajusta los parámetros de prescripción para todas las sesiones del período de descarga: carga al 60% de la carga habitual de cada ejercicio, mantener 4 series por ejercicio, repeticiones en el límite inferior del rango (8), y RIR objetivo de 4-5.

#### CA-14.02 — Cálculo del 60% de carga por ejercicio

**Dado que** el modo de Descarga está activo,
**cuando** el sistema prescribe la carga para un ejercicio en una sesión de descarga,
**entonces** calcula el 60% de la última carga de trabajo registrada para ese ejercicio, redondeando al incremento más cercano disponible (2.5 Kg para tren superior, 5 Kg para tren inferior).

#### CA-14.03 — Duración del modo Descarga: 1 microciclo completo

**Dado que** el modo de Descarga está activo,
**cuando** el ejecutante completa sesiones durante la descarga,
**entonces** el modo se mantiene activo durante exactamente 1 microciclo completo (A-B-C-A-B-C = 6 sesiones), desactivándose automáticamente al cerrar la sexta sesión del microciclo de descarga.

#### CA-14.04 — Versión congelada durante la descarga

**Dado que** el modo de Descarga está activo,
**cuando** el sistema determina la versión del módulo para cada sesión de descarga,
**entonces** mantiene la misma versión que estaba activa al momento de activar la descarga para cada módulo respectivo; la secuencia de versiones no avanza durante el período de descarga.

#### CA-14.05 — Cálculo de carga de reinicio post-descarga

**Dado que** el modo de Descarga ha finalizado tras completar el microciclo,
**cuando** el sistema calcula las cargas para el nuevo mesociclo,
**entonces** prescribe para cada ejercicio una carga de reinicio equivalente al 90% de la última carga de trabajo pre-descarga, no el 90% de la carga de descarga.

#### CA-14.06 — Redondeo de la carga de reinicio

**Dado que** el sistema calcula la carga de reinicio al 90%,
**cuando** el resultado no es un múltiplo exacto del incremento mínimo (2.5 Kg o 5 Kg),
**entonces** redondea al incremento más cercano disponible según el módulo del ejercicio (redondeando hacia abajo para proteger al ejecutante).

#### CA-14.07 — Indicación visual del modo Descarga

**Dado que** el modo de Descarga está activo,
**cuando** el ejecutante visualiza la sesión o la prescripción,
**entonces** el sistema indica claramente que se está en período de descarga, mostrando las cargas ajustadas y cuántas sesiones restan para completar el microciclo de descarga.

#### CA-14.08 — Transición al nuevo mesociclo

**Dado que** el modo de Descarga finaliza,
**cuando** el ejecutante inicia la primera sesión post-descarga,
**entonces** el sistema presenta las cargas de reinicio (90% pre-descarga) como cargas objetivo, la secuencia de versiones retoma su avance normal, y los estados de progresión de los ejercicios se actualizan de "En Descarga" al estado que corresponda según los nuevos registros.

#### CA-14.09 — Ejercicios de peso corporal e isométricos durante descarga

**Dado que** el modo de Descarga está activo y la sesión incluye ejercicios de peso corporal o isométricos,
**cuando** el sistema prescribe parámetros de descarga,
**entonces** para ejercicios de peso corporal prescribe 8 repeticiones con RIR 4-5 (sin ajuste de carga porque Peso = 0), y para isométricos prescribe 30 segundos con RIR 4-5.

### Bloque B — Conteo de Microciclos (RF41)

#### CA-14.10 — Incremento del contador al completar un microciclo

**Dado que** el ejecutante ha cerrado sesiones de entrenamiento,
**cuando** los 6 módulos de la rotación (A-B-C-A-B-C) han sido ejecutados desde el último incremento,
**entonces** el sistema incrementa el conteo de microciclos completados en 1.

#### CA-14.11 — Conteo basado en la rotación, no en el calendario

**Dado que** el sistema lleva el conteo de microciclos,
**cuando** evalúa si un microciclo se ha completado,
**entonces** se basa exclusivamente en la secuencia de sesiones ejecutadas (6 módulos: A, B, C, A, B, C), independientemente de cuántos días naturales haya tomado completarlos.

#### CA-14.12 — Conteo persistente

**Dado que** el sistema ha registrado microciclos completados,
**cuando** la aplicación se cierra, se reinicia el dispositivo o se actualiza la app,
**entonces** el conteo de microciclos persiste y se recupera correctamente al reabrir la aplicación.

#### CA-14.13 — Conteo durante período de descarga

**Dado que** el modo de Descarga está activo,
**cuando** el ejecutante completa las 6 sesiones del microciclo de descarga,
**entonces** el sistema incrementa el contador de microciclos igualmente, ya que un microciclo de descarga es un microciclo completo ejecutado.

#### CA-14.14 — Consulta del conteo

**Dado que** el ejecutante desea conocer su progreso temporal,
**cuando** consulta el conteo de microciclos,
**entonces** el sistema muestra el número total de microciclos completados desde el inicio del uso del sistema.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite al sistema gestionar la recuperación estructurada del SNC y tejidos conectivos mediante descargas programadas, y ofrece al ejecutante visibilidad de su progreso temporal acumulado.

### Reglas de Negocio

1. **Transacción atómica en activación:** Al activar el deload: (1) crear `DeloadEntity(ACTIVE)` con versiones congeladas; (2) transicionar TODOS los ejercicios a `IN_DELOAD` (excepto `NO_HISTORY` y `MASTERED`). `sessions_without_progression` se resetea al FINALIZAR, no al activar.
2. **60% de carga pre-descarga:** La carga de descarga es 60% de la última carga de trabajo registrada por ejercicio en sesiones NO deload, redondeada hacia abajo al incremento más cercano (2.5 Kg para A/B, 5.0 Kg para C).
3. **1 microciclo = 6 sesiones:** La descarga dura exactamente 6 sesiones (A-B-C-A-B-C). Una sesión incompleta SÍ cuenta para el progreso.
4. **Versiones congeladas:** Las versiones de módulo se congelan al activar la descarga y NO avanzan durante el ciclo de descarga. Se congelan en 2 puntos: `getNextModuleVersionId()` y `GetNextSessionInfoUseCase`.
5. **90% de carga pre-descarga (no 90% de descarga):** La carga de reinicio se calcula sobre la última carga pre-descarga, no sobre la carga de descarga (60%).
6. **Redondeo protector hacia abajo:** `floor(value / increment) * increment`. Ej: 60% de 60 Kg, incr=2.5 → 35.0 Kg.
7. **Conteo de microciclos basado en rotación:** Se incrementa al cerrar la posición 6 de la rotación (A-B-C-A-B-C), independientemente de los días naturales.
8. **Conteo durante deload:** Un microciclo de descarga SÍ incrementa el contador de microciclos.
9. **Solo un deload activo a la vez:** No se permite activar un nuevo deload si ya existe uno con `status = 'ACTIVE'`.
10. **Bodyweight e isométricos:** No se calcula carga de descarga ni reinicio (`prescribed_load_kg` siempre NULL). Prescripción visual: 8 reps con RIR 4-5 (bodyweight), 30 seg con RIR 4-5 (isométricos).

### Interfaz

- **Pantalla I1 — `DeloadManagementScreen`:** Pantalla de gestión de descarga con 3 estados mutuamente excluyentes:
  - **Estado A (Descarga requerida):** No hay deload activo pero existe alerta `MODULE_REQUIRES_DELOAD`. Muestra protocolo + botón "Activar Descarga".
  - **Estado B (Descarga activa):** Deload activo con progreso N/6 sesiones. Barra de progreso + parámetros + versiones congeladas.
  - **Estado C (Post-descarga):** Deload recién completado, primera visita post-deload. Muestra cargas de reinicio (90%) por ejercicio estándar.
  - **Estado implícito:** Sin descarga requerida ni activa — mensaje "No hay descarga pendiente".
  - Bottom Navigation presente (I1 SÍ tiene Bottom Navigation).
- **Card B1 — `DeloadStatusCard`:** Card condicional en HomeScreen con enlace → I1. Secondary Container, título "Descarga activa" o "Módulo requiere descarga", botón "Ver gestión →".
- **Badge E1 — ActiveSessionScreen:** AssistChip "Descarga · Sesión N/6" con ícono 🔄 color azul descarga (#1565C0/#64B5F6). `LoadText` con color azul descarga cuando `isDeloadSession`.
- **Payload requerido:** `loadIncrementKg: Double` (de `module.load_increment_kg`) para cálculos de redondeo.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Preview de Interfaz

Ver Wireframes I1 (3 estados + defensivo), Especificación Visual §I1 (LinearProgressIndicator, azul descarga #1565C0/#64B5F6), Wireframes B1, Especificación Visual §B1: Card Descarga Secondary Container, Especificación Visual §E1: badge AssistChip deload, 3 variantes `loadText` deload.

---

## Tablas Room involucradas

| Tabla | Operación | Descripción |
|---|---|---|
| `deload` | INSERT / UPDATE / SELECT | Nueva tabla (Modelo de Datos §3.15): id, status, activation_date, completion_date, frozen_version_module_a/b/c |
| `session` | SELECT + FK | `deload_id` ya existe con index — columna lógica FK a tabla `deload` |
| `exercise_progression` | UPDATE masivo | `transitionToDeload()`: status → IN_DELOAD al activar; IN_DELOAD → IN_PROGRESSION al finalizar |
| `exercise_set` | SELECT | `getLastWeightForExercise()` modificado para excluir sesiones deload; nueva query `getPreDeloadAvgWeight()` |
| `rotation_state` | SELECT | Versiones congeladas al activar deload |
| `alert` | UPDATE | `resolveAllByType("MODULE_REQUIRES_DELOAD", today)` al finalizar deload |
| `exercise` | SELECT | `getByIdOnce()` en finalización para determinar bodyweight/isométrico |
| `module` | SELECT | `getByCode()` en finalización para obtener `load_increment_kg` |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-02-17
