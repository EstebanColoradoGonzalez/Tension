# Historia de Usuario

**Como** ejecutante,
**quiero** ver un resumen claro e inmediato al cerrar mi sesión con el tonelaje, la clasificación de progresión por ejercicio y las señales de acción para la próxima sesión,
**para** saber en ese mismo momento qué hice bien, dónde retrocedí y qué ajustar la próxima vez.

## Descripción

Esta historia define la presentación automática de un resumen post-sesión al cerrar una sesión (Completada o Incompleta). El resumen muestra el tonelaje total calculado, la cantidad de ejercicios completados, la clasificación de progresión por ejercicio con señales visualmente distinguibles, y las señales de acción para la próxima sesión. Toda la información se lee de datos ya persistidos por las HUs predecesoras (HU-09, HU-10, HU-11, HU-12).

---

## Criterios de Aceptación

### CA-13.01 — Presentación automática del resumen al cerrar

**Dado que** el ejecutante cierra una sesión (Completada o Incompleta),
**cuando** el sistema completa el procesamiento de cierre (cálculo de tonelaje, evaluación de progresión, actualización de rotación),
**entonces** presenta automáticamente un resumen de la sesión al ejecutante sin requerir navegación adicional.

### CA-13.02 — Contenido del resumen: tonelaje total

**Dado que** el resumen post-sesión se presenta al ejecutante,
**cuando** el ejecutante visualiza el resumen,
**entonces** muestra el tonelaje total de la sesión (Σ Peso × Repeticiones de todas las series registradas), expresado en kilogramos.

### CA-13.03 — Contenido del resumen: ejercicios completados

**Dado que** el resumen post-sesión se presenta al ejecutante,
**cuando** el ejecutante visualiza el resumen,
**entonces** muestra la cantidad de ejercicios completados (4 series registradas) respecto al total de ejercicios de la sesión (ej: "8/11 ejercicios completados").

### CA-13.04 — Contenido del resumen: clasificación de progresión por ejercicio

**Dado que** el resumen post-sesión se presenta al ejecutante,
**cuando** el ejecutante visualiza el resumen,
**entonces** muestra para cada ejercicio registrado en la sesión su clasificación de progresión: "Progresión positiva", "Mantenimiento" o "Regresión", junto con el nombre del ejercicio.

### CA-13.05 — Contenido del resumen: señales de acción

**Dado que** el resumen post-sesión se presenta al ejecutante,
**cuando** el ejecutante visualiza el resumen,
**entonces** muestra para cada ejercicio la señal de acción para la próxima sesión: "Subir carga" (si cumplió Doble Umbral), "Mantener carga" (si no cumplió) o "Considerar descarga" (si se detectó regresión o fatiga acumulada).

### CA-13.06 — Señales visualmente distinguibles

**Dado que** el resumen muestra clasificaciones de progresión y señales de acción,
**cuando** el ejecutante visualiza el resumen,
**entonces** las señales de progresión (↑ Progresión, = Mantenimiento, ↓ Regresión) son visualmente distinguibles mediante colores e iconografía diferenciada, sin depender únicamente del texto para transmitir su significado.

### CA-13.07 — Resumen de sesión incompleta

**Dado que** el ejecutante cierra una sesión como "Incompleta",
**cuando** el sistema presenta el resumen,
**entonces** muestra el estado "Incompleta", el tonelaje parcial calculado con las series registradas, la clasificación de progresión solo para los ejercicios que tienen registros, y las señales de acción correspondientes.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación de solo lectura sobre la base de datos local.
- **Valor de negocio:** Permite al ejecutante tomar decisiones informadas inmediatamente después de cada sesión, sin necesidad de navegar a otras pantallas.

### Reglas de Negocio

1. **Hu-13 es una historia de lectura pura — no modifica ningún dato.** Todos los datos ya fueron persistidos por las HUs predecesoras: `session.status` (HU-09), `session_exercise.progression_classification` (HU-10), `exercise_progression.prescribed_load_kg` (HU-11), alertas en tabla `alert` (HU-12).
2. **El tonelaje se calcula en la query, no se almacena.** Se calcula como `SUM(exercise_set.weight_kg * exercise_set.reps)` desde las series asociadas vía `session_exercise`.
3. **`ActionSignalRule` es una función pura en `domain/rules/`.** Encapsula siete variantes de señal (sealed interface `ActionSignal`) con lógica determinista.
4. **La derivación de "Considerar descarga" consume datos de HU-12 sin recalcular y solo aplica a ejercicios estándar.** Para ejercicios de peso corporal e isométricos con regresión, las señales tipo-específicas se mantienen.
5. **Solo se muestran ejercicios con al menos 1 serie registrada.** Los ejercicios sin series no aparecen en la lista.
6. **`ProgressionIndicator` es un composable reutilizable para E5, F2 y F3.** La combinación ícono (↑/=/↓) + color semántico + texto clasificación se repite en tres pantallas.
7. **La navegación E5 → F3 se condiciona a la existencia de la ruta.** F3 pertenece a HU-17. El click en cada ejercicio intenta navegar a F3; si la pantalla aún no tiene composable registrado, se mantiene como placeholder.
8. **Formato de tonelaje usa separador de miles** (ej: "12,450 Kg") y se muestra como entero redondeado.
9. **E5 no tiene Bottom Navigation ni botón de retorno.** La única salida es el botón "Ir al Inicio" que navega a B1.
10. **Señales para ejercicios sin historial previo:** Cuando `progression_classification` es NULL, E5 muestra: sin ícono de clasificación, sin color semántico, señal de acción "Primera sesión — sin referencia".

### Interfaz

- **E5 — `SessionSummaryScreen`:** Pantalla de resumen post-sesión. Card de estado/tonelaje + lista de progresión por ejercicio + botón "Ir al Inicio". `CenterAlignedTopAppBar` sin `navigationIcon` (no hay retorno), sin Bottom Navigation. Los ejercicios se muestran como `ListItem` con `ProgressionIndicator` (↑/=/↓ + color semántico) como `leadingContent`, nombre del ejercicio, clasificación y señal de acción.
- **`ProgressionIndicator` — Composable reutilizable:** Ícono Unicode literal (↑/=/↓) 24dp con color semántico + texto clasificación. Usado en E5, F2 y F3.
- **Colores semánticos:** ↑ verde (`ProgressionPositiveLight/Dark`), = ámbar (`MaintenanceLight/Dark`), ↓ rojo (`RegressionLight/Dark`).

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Preview de Interfaz

Ver `Especificación Visual §E5` para SessionSummaryScreen. El wireframe define la estructura: Card de estado/tonelaje en la parte superior, lista de ejercicios con clasificación y señales de acción, y botón "Ir al Inicio" en la parte inferior.

---

## Dependencias Técnicas e Integración

### Componentes que esta HU consume (predecesores)

| HU | Dato consumido | Tabla/columna |
|---|---|---|
| HU-09 | `closeSession()` + evento `navigateToSessionSummary` | `session.status`, `ActiveSessionViewModel.navigateToSessionSummary SharedFlow<Long>` |
| HU-10 | Clasificación de progresión por ejercicio | `session_exercise.progression_classification` |
| HU-10 | Estado de progresión + sesiones sin progresión | `exercise_progression.status`, `exercise_progression.sessions_without_progression` |
| HU-11 | Carga prescrita por Doble Umbral | `exercise_progression.prescribed_load_kg` |
| HU-12 | Alerta de descarga requerida por módulo | `alert` tabla — `existsActiveByModule(moduleCode, "MODULE_REQUIRES_DELOAD")` |
| HU-06 | Datos de series (peso × reps para tonelaje) | `exercise_set.weight_kg`, `exercise_set.reps` |
| HU-07 | Ejercicio efectivo post-sustitución | `session_exercise.exercise_id` |
| HU-08 | Dominado isométrico (CA-08.07: 4×45s) | `exercise_progression.status == 'MASTERED'` |
| HU-01/05 | Colores semánticos (progresión/mantenimiento/regresión) | `Color.kt`, `Theme.kt`, `LocalTensionSemanticColors` |

### Componentes que esta HU produce (sucesores)

| HU | Dato/componente producido |
|---|---|
| HU-17 | `ProgressionIndicator` composable reutilizable (E5, F2, F3) |
| HU-17 | Navegación E5 → F3 (click en ejercicio → historial) — forward-compatible |

### Tablas Room accedidas (solo lectura — HU-13 no escribe datos)

| Tabla | Operación | Query |
|---|---|---|
| `session` | SELECT | `getSessionSummaryInfo(sessionId)` — status, module_version_id |
| `module_version` | JOIN | vía session.module_version_id |
| `session_exercise` | SELECT + JOIN | conteos completados, progresión, clasificación |
| `exercise_set` | AGGREGATE | `SUM(weight_kg * reps)` para tonelaje |
| `exercise_progression` | LEFT JOIN | prescribed_load_kg, status |
| `alert` | EXISTS | `existsActiveByModule(moduleCode, "MODULE_REQUIRES_DELOAD")` |

### Rutas de navegación involucradas

| Ruta | Dirección | Estado en HU-13 |
|---|---|---|
| `active-session/{sessionId}` → `session-summary/{sessionId}` | E4 → E5 | Reemplaza TODO en `TensionNavHost` L262 |
| `session-summary/{sessionId}` → `home` | E5 → B1 | Botón "Ir al Inicio" (único punto de salida) |
| `session-summary/{sessionId}` → `exercise-history/{exerciseId}` | E5 → F3 | Forward-compatible (composable F3 registrado en HU-17) |

---

## Verificación Cruzada de CAs

| CA | Estado | Mecanismo | Implementado en |
|---|---|---|---|
| CA-13.01 | Done | `ActiveSessionViewModel.navigateToSessionSummary` → `TensionNavHost` → `SessionSummaryScreen` | HU-13 (Navegación + Screen) |
| CA-13.02 | Done | `SessionDao.getSessionSummaryInfo()` → `SUM(weight_kg * reps)` → `SessionSummary.totalTonnageKg` | HU-13 (DAO + Screen) |
| CA-13.03 | Done | `SessionDao.getSessionSummaryInfo()` → `completedExercises` / `totalExercises` | HU-13 (DAO + Screen) |
| CA-13.04 | Done | `SessionExerciseDao.getExercisesForSummary()` → `progression_classification` → `ProgressionIndicator` | HU-13 (DAO + Screen) |
| CA-13.05 | Done | `ActionSignalRule.resolve()` → sealed `ActionSignal` → texto de señal en `SessionSummaryScreen` | HU-13 (Rule + Screen) |
| CA-13.06 | Done | `ProgressionIndicator`: ↑ verde (#2E7D32/#81C784), = ámbar (#8D6E00/#FFD54F), ↓ rojo (#C62828/#EF9A9A) | HU-13 (Screen + Component) |
| CA-13.07 | Done | Query con `HAVING setCount > 0`, Card estado "Incompleta ⚠️", tonelaje parcial | HU-13 (DAO + Screen) |

---

## Definición de Terminado (Inicial)

- [x] Resumen post-sesión se presenta automáticamente al cerrar sesión (Completada o Incompleta)
- [x] Tonelaje total calculado como SUM(weight_kg × reps) mostrado en Kg con separador de miles
- [x] Cantidad de ejercicios completados mostrada (ej: "8/11 ejercicios completados")
- [x] Clasificación de progresión por ejercicio: "Progresión positiva", "Mantenimiento" o "Regresión"
- [x] Señales de acción por ejercicio: "Subir carga", "Mantener carga" o "Considerar descarga"
- [x] Señales visualmente distinguibles: ↑ verde, = ámbar, ↓ rojo con iconografía diferenciada
- [x] Sesión incompleta muestra estado "Incompleta ⚠️", tonelaje parcial, clasificación solo para ejercicios con registros
- [x] `ActionSignalRule` encapsula 8 variantes de señal (sealed interface)
- [x] `ProgressionIndicator` composable reutilizable para E5, F2, F3
- [x] Solo ejercicios con al menos 1 serie registrada aparecen en la lista
- [x] Navegación E5 → B1 con botón "Ir al Inicio" (popUpTo HOME)
- [x] E5 sin Bottom Navigation ni botón de retorno
- [x] Ejercicios sin historial muestran "Primera sesión — sin referencia"
- [x] "Considerar descarga" solo aplica a ejercicios estándar (no bodyweight/isométricos)
