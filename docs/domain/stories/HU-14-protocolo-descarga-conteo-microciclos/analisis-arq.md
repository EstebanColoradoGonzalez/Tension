# Análisis Arquitectónico

## Patrón Arquitectónico

**MVVM con orquestación de dominio en múltiples capas — entidad nueva + reglas puras + modificación de flujos existentes (inicio/cierre de sesión, rotación).**

HU-14 es la historia más transversal del sistema: introduce una entidad nueva (`deload`), crea dos reglas de dominio (`DeloadLoadRule`), modifica el flujo de inicio de sesión (`startSession()` con `deloadId`), modifica el cierre de sesión (`closeSession()` con completación de deload), modifica la rotación (`advanceRotation()` con congelamiento de versiones), modifica la progresión (`evaluateProgression()` con guards durante deload), y añade una pantalla nueva (I1). Su correcta implementación es crítica porque un error en el flujo de descarga produce prescripciones de carga incorrectas que podrían causar lesiones.

## Decisiones de Diseño

### Decisión 1 — HU-14 opera en tres fases temporalmente distintas: activación → ciclo → finalización.

La activación (CA-14.01) crea un registro `deload` con estado ACTIVE y congela las versiones del momento. El ciclo (CA-14.03) dura 6 sesiones — cada sesión se crea con `deload_id` apuntando al deload activo, las cargas se prescriben al 60% (CA-14.02), y la rotación avanza posición pero NO versiones (CA-14.04). La finalización (CA-14.05) ocurre al cerrar la sexta sesión: el deload pasa a COMPLETED, las cargas se reinician al 90% de la carga pre-descarga (CA-14.06), y los estados de progresión de ejercicios se transicionan de `IN_DELOAD` a `IN_PROGRESSION` (CA-14.08).

### Decisión 2 — `DeloadEntity` persiste el ciclo de descarga; el conteo de sesiones no se almacena.

El Modelo de Datos §3.15 define la tabla `deload` con: `id`, `status` (ACTIVE/COMPLETED), `activation_date`, `completion_date`, `frozen_version_module_a/b/c`. El conteo de sesiones del deload **no se almacena** — se calcula como `SELECT COUNT(*) FROM session WHERE deload_id = ? AND status IN ('COMPLETED', 'INCOMPLETE')`. Solo puede existir un deload con `status = 'ACTIVE'` a la vez.

### Decisión 3 — `DeloadLoadRule` es una función pura en `domain/rules/` (ADR-06) con dos funciones.

- `calculateDeloadLoad(lastWeightKg, loadIncrementKg)`: calcula 60% de la carga habitual, redondeado hacia abajo al incremento más cercano. Fórmula: `floor(lastWeightKg * 0.60 / loadIncrementKg) * loadIncrementKg`.
- `calculateResetLoad(preDeloadWeightKg, loadIncrementKg)`: calcula 90% de la última carga pre-descarga, redondeado hacia abajo. Fórmula: `floor(preDeloadWeightKg * 0.90 / loadIncrementKg) * loadIncrementKg`.
- `loadIncrementKg` recibido como parámetro (2.5 para módulos A/B, 5.0 para C) — de `module.load_increment_kg` (HU-11 Decisión 4).
- Para bodyweight e isométricos: no se calcula carga de descarga ni reinicio (CA-14.09, `prescribed_load_kg` siempre NULL).

### Decisión 4 — La "última carga de trabajo habitual" se deriva de datos existentes, no se almacena.

Modelo de Datos §3.15: "La carga habitual de cada ejercicio antes de la descarga se deriva consultando el último `exercise_set.weight_kg` registrado por ejercicio en sesiones anteriores a `activation_date` que no sean de descarga." La query `getPreDeloadAvgWeight(exerciseId, activationDate)` reutiliza patrón nested subquery.

### Decisión 5 — `RotationResolver.advanceRotation()` recibe `isDeload: Boolean = false`.

HU-09 Decisión 6 anticipó esta modificación. Cuando `isDeload = true` y `position == 6`: la posición vuelve a 1, `microcycleCount` se incrementa (CA-14.13), las versiones **NO avanzan** (CA-14.04). Cuando `isDeload = false`, el comportamiento permanece idéntico al actual. Los 6 tests existentes de `RotationResolver` se mantienen válidos (default false).

### Decisión 6 — `startSession()` consulta deload activo y asigna `deloadId`.

Actualmente, `startSession()` siempre crea `SessionEntity(deloadId = null)`. Con HU-14, antes de crear la sesión se consulta `deloadDao.getActiveDeloadOnce()`. Si retorna un deload activo: `SessionEntity.deloadId` se asigna con el ID del deload activo; las versiones se toman de `deload.frozen_version_module_X`, no de `rotation_state.current_version_module_X`.

### Decisión 7 — `closeSession()` se amplía con lógica de finalización de deload.

Cuando la sesión cerrada tiene `deloadId != null`, después de `evaluateProgression()` y `advanceRotation()`:
1. Contar sesiones del deload: `sessionDao.countDeloadSessions(deloadId)`.
2. Si el conteo == 6: marcar COMPLETED, reset loads 90%, transicionar IN_DELOAD → IN_PROGRESSION, resolver alertas MODULE_REQUIRES_DELOAD.

### Decisión 8 — La activación del deload transiciona TODOS los ejercicios a estado `IN_DELOAD`.

MDS R5: "Ejercicios: Mantener los mismos del plan." Al activar: (1) crear `DeloadEntity(ACTIVE)` con versiones congeladas; (2) `exerciseProgressionDao.transitionToDeload()` — UPDATE masivo WHERE status NOT IN ('NO_HISTORY', 'MASTERED'). El `sessions_without_progression` se resetea a 0 al FINALIZAR el deload, no al activar.

### Decisión 9 — Pantalla I1 tiene 3 estados mutuamente excluyentes.

- **Estado A (Descarga requerida):** No hay deload activo pero existe alerta `MODULE_REQUIRES_DELOAD`. Muestra protocolo + botón "Activar Descarga".
- **Estado B (Descarga activa):** Deload activo con progreso N/6 sesiones. Barra de progreso + parámetros + versiones congeladas.
- **Estado C (Post-descarga):** Deload recién completado, primera visita post-deload. Muestra cargas de reinicio (90%) por ejercicio estándar.
- **Estado implícito:** Sin descarga requerida ni activa — mensaje "No hay descarga pendiente".

### Decisión 10 — El Bloque B (CA-14.10 a CA-14.14) ya está implementado en HU-09.

`RotationResolver.advanceRotation()` ya incrementa `microcycleCount` al cerrar posición 6. `GetMicrocycleCountUseCase` ya existe. `HomeUiState.microcycleCount` ya se muestra en B1. Solo se agrega test nuevo para caso `isDeload=true`.

### Decisión 11 — Versiones congeladas en 2 puntos: `getNextModuleVersionId()` y `GetNextSessionInfoUseCase`.

Ambos deben consultar deload activo y, si existe, usar `deload.frozen_version_module_X` en vez de `rotation_state.current_version_module_X`. Se implementa vía `flatMapLatest` con `deloadDao.getActiveDeload()`.

### Decisión 12 — Guards en `evaluateProgression()` durante deload.

- **Guard 1 (`prescribeLoad`):** Si `isDeloadSession`, preservar `currentProgression.prescribedLoadKg` en vez de recalcular — el Doble Umbral sobreescribiría la carga incorrectamente.
- **Guard 2 (alertas PLATEAU):** Si `isDeloadSession`, saltar toda la gestión de alertas PLATEAU (`if (!isDeloadSession)` wrapper). El guard en L442 (module-level detection) ya existía y se mantiene.

### Decisión 13 — Redondeo es hacia ABAJO (protector).

CA-14.06: `floor(value / increment) * increment`. Ejemplos:
- 60% de 60 Kg, increment=2.5 → `floor(36.0 / 2.5) * 2.5 = 35.0 Kg`
- 60% de 55 Kg, increment=2.5 → `floor(33.0 / 2.5) * 2.5 = 32.5 Kg`
- 90% de 60 Kg, increment=2.5 → `floor(54.0 / 2.5) * 2.5 = 52.5 Kg`
- 60% de 100 Kg, increment=5.0 → `floor(60.0 / 5.0) * 5.0 = 60.0 Kg`

### Decisión 14 — Pantalla I1 usa `DeloadManagementViewModel` con navegación desde B1/H2.

El ViewModel carga estado via `getDeloadStateUseCase()`. Acciones: `activateDeload()`. Navegación B1→I1 habilitada cuando el card "Estado de Descarga" es visible. Ruta: `deload` (Arquitectura Técnica §4.3 L405).

## Componentes Afectados

### Componentes nuevos

| # | Componente | Capa | Responsabilidad |
|---|---|---|---|
| 1 | `DeloadEntity` | Data (entity) | Room entity para tabla `deload`: id, status, activation_date, completion_date, frozen_version_a/b/c |
| 2 | `DeloadDao` | Data (dao) | `insert()`, `getActiveDeload()` (Flow), `getActiveDeloadOnce()` (suspend), `complete()`, `getById()`, `getLastCompletedDeload()` |
| 3 | `DeloadLoadRule` | Domain (rules) | `calculateDeloadLoad()` (60%, floor) y `calculateResetLoad()` (90%, floor) |
| 4 | `Deload` | Domain (model) | Domain model mapeado de `DeloadEntity` — evita layer violation al exponer entity desde domain |
| 5 | `DeloadState` | Domain (model) | Sealed interface: `NoDeloadNeeded`, `DeloadRequired(modules)`, `DeloadActive(progress, frozenVersions)`, `DeloadCompleted(resetLoads)` + `ExerciseResetLoad` |
| 6 | `DeloadHomeState` | Domain (model) | Sealed interface: `Active(progress, moduleCode)`, `Required(moduleCode)` — info mínima para card B1 |
| 7 | `ActivateDeloadUseCase` | Domain (usecase/deload) | `sessionRepository.activateDeload()` |
| 8 | `GetDeloadStateUseCase` | Domain (usecase/deload) | `sessionRepository.getDeloadState()` → `DeloadState` |
| 9 | `DeloadManagementUiState` | UI (deload) | `Loading`, `Content(deloadState)`, `Error(message)` |
| 10 | `DeloadManagementViewModel` | UI (deload) | `getDeloadStateUseCase()`, `activateDeload()`, `StateFlow<DeloadManagementUiState>` |
| 11 | `DeloadManagementScreen` | UI (deload) | Composable I1: 3 estados + Bottom Navigation |

### Componentes modificados

| # | Componente | Modificación | Nivel |
|---|---|---|---|
| 1 | `TensionDatabase` | Agregar `DeloadEntity`, bump v5→v6, agregar `deloadDao()` | Mayor |
| 2 | `SessionEntity` | FK lógica a `deload(id)` ya existe — sin cambio de schema | Menor |
| 3 | `RotationResolver.advanceRotation()` | Agregar `isDeload: Boolean = false` — versiones NO avanzan cuando isDeload=true en posición 6 | Mayor |
| 4 | `SessionRepositoryImpl.startSession()` | Consultar deload activo → asignar `deloadId` | Mayor |
| 5 | `SessionRepositoryImpl.closeSession()` | Pasar `isDeload` a `advanceRotation()` + bloque finalización (COMPLETED, reset 90%, IN_DELOAD→IN_PROGRESSION, resolver alertas) | Mayor |
| 6 | `SessionRepositoryImpl.evaluateProgression()` | Guards: no recalcular `prescribed_load_kg` durante deload, no crear/resolver alertas PLATEAU durante deload | Medio |
| 7 | `SessionRepositoryImpl.getNextModuleVersionId()` | Usar versiones congeladas si deload activo | Medio |
| 8 | `GetNextSessionInfoUseCase` | Usar versiones congeladas si deload activo | Medio |
| 9 | `ExerciseSetDao.getLastWeightForExercise()` | Agregar `s.deload_id IS NULL` para excluir sesiones deload | Menor |
| 10 | `ExerciseProgressionDao` | Agregar `transitionToDeload()`, `getAllInDeload()`, `getAllWithPrescribedLoad()` | Menor |
| 11 | `AlertDao` | Agregar `getActiveAlertsByType()`, `resolveAllByType()` | Menor |
| 12 | `SessionDao` | Agregar `countDeloadSessions()`, `hasSessionAfterDeload()` | Menor |
| 13 | `SessionRepository` | Agregar `activateDeload()`, `getDeloadState()`, `getActiveDeload()`, `getDeloadIdBySessionId()`, `countDeloadSessions()` | Menor |
| 14 | `HomeUiState` | Agregar `deloadState: DeloadHomeState?` | Menor |
| 15 | `HomeViewModel` | Cargar estado deload para card B1 | Menor |
| 16 | `HomeScreen` | Agregar `DeloadStatusCard` condicional con enlace → I1 | Medio |
| 17 | `ActiveSessionUiState` | Agregar `isDeloadSession: Boolean`, `deloadProgress: String` | Menor |
| 18 | `ActiveSessionViewModel` | Detectar sesión deload + calcular cargas 60% con `DeloadLoadRule` | Medio |
| 19 | `ActiveSessionScreen` | Badge AssistChip "Descarga · Sesión N/6" + `LoadText` con color azul descarga + 3 variantes deload | Medio |
| 20 | `TensionNavHost` | Registrar composable I1 en ruta `deload`, enlace B1→I1 | Menor |
| 21 | `NavigationRoutes` | Agregar `DELOAD_MANAGEMENT = "deload"` | Menor |
| 22 | `DatabaseModule` | Proveer `DeloadDao` | Menor |

## Notas Técnicas

### Nota 1 — FK de `session.deload_id` a `deload.id` es lógica (no constrainted).

`SessionEntity` tiene la columna `deload_id` con index pero sin FK constraint (la tabla `deload` no existía hasta esta HU). La migration 5→6 crea la tabla `deload`. En SQLite, no se puede `ALTER TABLE ADD CONSTRAINT` — se necesita recrear la tabla session. Se mantiene FK lógica (verificada a nivel de aplicación). Consistente con el patrón existente.

### Nota 2 — `getNextModuleVersionId()` y `GetNextSessionInfoUseCase` deben ser consistentes.

Ambos usan `flatMapLatest` para combinar `rotationStateDao.getRotationState()` con `deloadDao.getActiveDeload()`. El `Deload` domain model evita layer violation al exponer el tipo correcto desde `SessionRepository.getActiveDeload(): Flow<Deload?>`.

### Nota 3 — El "Estado C: Post-descarga" de I1 es efímero.

El Estado C se muestra cuando el último deload es COMPLETED y `sessionDao.hasSessionAfterDeload(deloadId)` retorna `false`. Una vez que el ejecutante inicia la siguiente sesión, el Estado C desaparece y la pantalla muestra "No hay descarga pendiente".

### Nota 4 — Bodyweight e isométricos durante deload: prescripción visual, no persistida.

CA-14.09: bodyweight → 8 reps, RIR 4-5; isométricos → 30 seg, RIR 4-5. Estos parámetros son de prescripción visual (E1, I1). `prescribed_load_kg` sigue NULL para estos tipos. La UI de E1 muestra variantes deload via `DeloadLoadRule`-aware `loadText`.

### Nota 5 — Sesión incompleta durante deload sí cuenta para el progreso.

`countDeloadSessions(deloadId)` usa `status IN ('COMPLETED', 'INCOMPLETE')`. Una sesión cerrada como incompleta durante deload avanza el progreso (N/6 → N+1/6).

### Nota 6 — Solo puede haber una descarga activa a la vez.

`activateDeload()` verifica que no exista deload con `status = 'ACTIVE'` antes de crear uno nuevo. El botón "Activar Descarga" solo aparece en Estado A (sin deload activo).

### Nota 7 — Migration 5→6: solo `CREATE TABLE IF NOT EXISTS deload (...)`.

`fallbackToDestructiveMigration()` está activo en `DatabaseModule` — no se necesita migration manual. Room destruye y recrea la BD al cambiar versión. Aceptable en pre-release.

### Nota 8 — Precarga E2 post-deload: Opción A (excluir sesiones deload).

HU-06 Nota 9 difiere decisión a HU-14. Trade-off: UX post-deload (precarga correcta 90%) se prioriza sobre UX durante-deload (E2 precarga peso pre-descarga durante 6 sesiones, no la carga al 60%). Solución: agregar `s.deload_id IS NULL` a `getLastWeightForExercise()`. El usuario en deload ya sabe las cargas objetivo gracias al badge E1 (Fase 8.5).

### Nota 9 — Señales E5 durante deload son inocuas.

`ActionSignalRule` puede generar señales semánticamente incorrectas durante deload (comparación con series al 60%). Sin embargo, `prescribed_load_kg` está protegido por Guard 1 y los estados no cambian. Las señales no se persisten. Decisión: no modificar E5 en esta HU.

## Verificación Cruzada de CAs

| CA | Estado | Mecanismo |
|---|---|---|
| CA-14.01 | Done | `ActivateDeloadUseCase` → crear `DeloadEntity(ACTIVE)` + `transitionToDeload()` |
| CA-14.02 | Done | `DeloadLoadRule.calculateDeloadLoad()`. Mostrado en E1 via Fase 8.5 (badge + cargas 60%) |
| CA-14.03 | Done | `countDeloadSessions(deloadId) == 6` → `deloadDao.complete()` en `closeSession()` |
| CA-14.04 | Done | `advanceRotation(isDeload=true)` versiones NO avanzan + `startSession()` usa versiones congeladas |
| CA-14.05 | Done | `DeloadLoadRule.calculateResetLoad(preDeloadWeight, increment)` → 90% round-down |
| CA-14.06 | Done | `floor(value / increment) * increment` con `module.load_increment_kg` |
| CA-14.07 | Done | I1 Estado B (progreso N/6) + B1 Card Descarga + E1 badge AssistChip + cargas 60% en LoadText |
| CA-14.08 | Done | Finalización: `exercise_progression.status → IN_PROGRESSION`, `sessionsWithoutProgression = 0`, `prescribedLoadKg = resetLoad` |
| CA-14.09 | Done | Guard `if (isBodyweight || isIsometric)` en finalización — solo transicionar status, no calcular carga |
| CA-14.10 | ✅ HU-09 | `advanceRotation()` incrementa `microcycleCount` al cerrar posición 6 |
| CA-14.11 | ✅ HU-09 | Rotación agnóstica al calendario |
| CA-14.12 | ✅ HU-09 | `rotation_state.microcycle_count` persistido en Room |
| CA-14.13 | Done | Nuevo test: `advanceRotation(isDeload=true)` posición 6 → microcycleCount++ |
| CA-14.14 | ✅ HU-09 | `HomeScreen` muestra `microcycleCount` |

## Cadenas de Invocación

### Activación:
```
DeloadManagementScreen → onActivateDeload()
  → DeloadManagementViewModel.activateDeload()
    → ActivateDeloadUseCase()
      → sessionRepository.activateDeload()
        → database.withTransaction {
            rotationState = rotationStateDao.getRotationState().first()
            deloadDao.insert(DeloadEntity(ACTIVE, today, frozenVersionA/B/C))
            exerciseProgressionDao.transitionToDeload()
          }
```

### Ciclo (inicio de sesión con deload activo):
```
HomeScreen → onStartSession(moduleVersionId)
  → StartSessionUseCase → sessionRepository.startSession(moduleVersionId)
    → deload = deloadDao.getActiveDeloadOnce()
    → sessionDao.insert(SessionEntity(moduleVersionId, date, "IN_PROGRESS", deloadId = deload?.id))
```

### Finalización (cierre sesión 6):
```
closeSessionUseCase(sessionId)
  → sessionRepository.closeSession(sessionId)
    → evaluateProgression(sessionId, moduleVersionId, isDeloadSession=true)
        → [clasificación per-exercise preservada, prescribed_load_kg NO recalculado (Guard 1), alertas PLATEAU NO creadas (Guard 2)]
    → advanceRotation(currentRotation, isDeload=true)
        → [posición avanza, microcycleCount++, versiones NO rotan]
    → sessionCount = sessionDao.countDeloadSessions(deloadId)
    → if (sessionCount == 6) {
          deloadDao.complete(deloadId, today)
          for each exercise IN_DELOAD:
            if standard: resetLoad = DeloadLoadRule.calculateResetLoad(preDeloadWeight, loadIncrementKg)
            exerciseProgressionDao.update(status="IN_PROGRESSION", prescribedLoadKg=resetLoad, sessionsWithoutProgression=0)
          alertDao.resolveAllByType("MODULE_REQUIRES_DELOAD", today)
      }
```

## Referencias y Validación

**Documentación consultada:**
- Manifiesto de Dominio Sistémico §6-A R5 — Protocolo Descarga (60%, 4 series, 8 reps, RIR 4-5, 1 microciclo, post-descarga 90%)
- Modelo de Datos §3.10 (session.deload_id), §3.13 (exercise_progression.status IN_DELOAD), §3.14 (rotation_state.microcycle_count), §3.15 (deload entity — 7 columnas)
- ADR-06 — Motor de reglas Kotlin puro; ADR D-08 — Descarga 1 microciclo, versiones congeladas
- Arquitectura Técnica §4.3 L405: ruta `deload`; §5.2: naming `DeloadLoadRule`, `DeloadManagementScreen`
- Wireframes I1 (3 estados + defensivo), Especificación Visual §I1 (LinearProgressIndicator, azul descarga #1565C0/#64B5F6)
- Wireframes B1, Especificación Visual §B1: Card Descarga Secondary Container, enlace "Ver gestión →"
- Especificación Visual §E1: badge AssistChip deload, 3 variantes `loadText` deload
- Mapa de Navegación §6 (I1): B1→I1, H2→I1
- Requerimientos RF38-41

**Validado por:** esteban.colorado | **Fecha:** 2026-02-17 | **Enfoque:** Exploratorio

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-02-17
