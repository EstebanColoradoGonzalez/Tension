# Refinamiento Técnico (Developer)

## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-15

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-10 con 10 decisiones de diseño, 7 archivos (4 nuevos + 3 modificados), y ~30 escenarios de test combinatorios. La complejidad reside en la regla pura con 3 clasificadores diferenciados, una máquina de estados de 5 estados, y la integración dentro de la transacción de `closeSession()`. No hay componentes UI.

**Nivel de complejidad:**
MEDIA-ALTA — 7 archivos (4 nuevos + 3 modificados). La complejidad reside en la regla pura con 3 clasificadores diferenciados, una máquina de estados de 5 estados, y ~30 escenarios de test combinatorios. La integración es directa (1 método privado dentro de transacción existente). No hay componentes UI.

**Riesgos técnicos conocidos:**
1. **N+1 queries en `evaluateProgression()`:** ~45 queries máximas. En SQLite local < 50ms — no se justifica optimización batch.
2. **`.first()` en `ExerciseProgressionDao.getByExerciseId()`:** Patrón ya validado en `startSession()` y `closeSession()`. Room garantiza snapshot transaccional.
3. **Comparaciones de punto flotante:** Mitigado con `WEIGHT_TOLERANCE = 0.01`.

**Patrones y convenciones del equipo:**
- Naming: `{Nombre}Rule` para reglas del motor (§5.2) — ej: `ProgressionClassificationRule`
- DTOs de DAO con sufijo descriptivo: `SessionExerciseForProgression`, `ExerciseSetData`
- Funciones puras en `domain/rules/` sin dependencias Android (ADR-06, RNF29)
- `object` singletons para reglas — patrón `RotationResolver`
- Transacciones atómicas con `database.withTransaction`

**Dependencias nuevas:** Ninguna.

**Estrategia de testing:** JUnit 4 (sin MockK para la regla — función pura) | `ProgressionClassificationRuleTest` con ~30 escenarios | Foco en la regla pura por mayor densidad lógica.

### Código existente verificado (HU-01 a HU-09 implementados)

| Componente | Archivo | Estado |
| --- | --- | --- |
| `SessionExerciseDao` | `data/local/dao/SessionExerciseDao.kt` (132 líneas) | Existe — DTOs `SessionExerciseWithDetails`/`SetExerciseInfo`/`SessionExerciseForSubstitution` como patrones. Se modifica: +DTO + 2 queries |
| `ExerciseSetDao` | `data/local/dao/ExerciseSetDao.kt` (34 líneas) | Existe — queries suspend simples como patrón. Se modifica: +DTO + 2 queries |
| `SessionRepositoryImpl` | `data/repository/SessionRepositoryImpl.kt` (305 líneas) | Existe — `closeSession()` L268-305 con extensión point entre `updateStatus` L281 y `rotationStateDao` L283. Imports de `ExerciseProgressionDao`/`ExerciseSetDao` presentes. Se modifica: +`evaluateProgression()` + insertar Step 2 |
| `ExerciseProgressionDao` | `data/local/dao/ExerciseProgressionDao.kt` (26 líneas) | Existe — `getByExerciseId()` (Flow), `update()`. No se modifica |
| `ExerciseProgressionEntity` | `data/local/entity/ExerciseProgressionEntity.kt` | Existe — PK=exerciseId, status default "NO_HISTORY", sessionsWithoutProgression default 0, prescribedLoadKg nullable. No se modifica |
| `SessionExerciseEntity` | `data/local/entity/SessionExerciseEntity.kt` | Existe — `progressionClassification: String? = null`. No se modifica |
| `ExerciseEntity` | `data/local/entity/ExerciseEntity.kt` | Existe — `module_code`, `is_bodyweight`, `is_isometric`. No se modifica |
| `RotationResolver` | `domain/model/RotationResolver.kt` (40 líneas) | Existe — `object` singleton patrón exacto para `ProgressionClassificationRule` |

## 6. Refinamiento Técnico

<!-- SECCIÓN AGREGADA POR: Workflow refinamiento-tecnico -->
<!-- ETAPA: Refinamiento Técnico -->
<!-- RESPONSABLE: Developer -->
<!-- BASE: Análisis Arquitectónico (Arquitecto) - Ver sección arriba -->
<!-- FECHA: 2026-02-15 -->
<!-- ESTADO: Refinado (Developer) - Basado en Análisis Arquitectónico -->
<!-- AUDITORÍA PROFUNDA (2026-02-15): Scope: 7 docs arquitectura + 5 docs business + 32 HUs + 10 archivos de código fuente. CONTRADICCIONES DOCUMENTALES (3): C1-BAJA MDS R3 "misma versión" vs RF34/MdD§3.13 global — Divergencia DELIBERADA Nota 9 ✅. C2-BAJA RF28 "almacenarlo" vs MdD§2 "no se almacenan" — avgRir in-memory ✅. C3-MEDIA CA-10.04 totalReps vs RF29/MDS R4 per-series — Diferido HU-12 Nota 8 ✅. CORRECCIONES APLICADAS (2): Test bodyweight "reps↑+RIR↑≥1.5→PP" corregido a "→MAINTENANCE"; test isometric misma corrección. RESULTADO FINAL: 0 HIGH, 0 MEDIUM, 1 LOW (boundary HU-12), 0 INFO. VERIFICACIÓN LÓGICA: classifyStandard 18/18 escenarios, classifyBodyweight 6/6, classifyIsometric 6/6, isIsometricMastered 7 edge cases, classify() dispatcher prioridad, resolveNewProgressionState 18 combinaciones — todos verificados. -->

### Consideraciones Generales

**Nivel de complejidad:**
MEDIA-ALTA — 7 archivos (4 nuevos + 3 modificados). La complejidad reside en la regla pura con 3 clasificadores diferenciados, una máquina de estados de 5 estados, y ~30 escenarios de test combinatorios. La integración es directa (1 método privado dentro de transacción existente). No hay componentes UI.

**Riesgos técnicos conocidos:**
1. **N+1 queries en `evaluateProgression()`:** ~45 queries máximas. En SQLite local < 50ms — no se justifica optimización batch.
2. **`.first()` en `ExerciseProgressionDao.getByExerciseId()`:** Patrón ya validado en `startSession()` y `closeSession()`. Room garantiza snapshot transaccional.
3. **Comparaciones de punto flotante:** Mitigado con `WEIGHT_TOLERANCE = 0.01`.

**Patrones y convenciones del equipo:**
- Naming: `{Nombre}Rule` para reglas del motor (§5.2) — ej: `ProgressionClassificationRule`
- DTOs de DAO con sufijo descriptivo: `SessionExerciseForProgression`, `ExerciseSetData`
- Funciones puras en `domain/rules/` sin dependencias Android (ADR-06, RNF29)
- `object` singletons para reglas — patrón `RotationResolver`
- Transacciones atómicas con `database.withTransaction`

**Dependencias nuevas:** Ninguna.

**Estrategia de testing:** JUnit 4 (sin MockK para la regla — función pura) | `ProgressionClassificationRuleTest` con ~30 escenarios | Foco en la regla pura por mayor densidad lógica.

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
HU-09 (closeSession) — patrón de extensión en transacción. HU-05 (ExerciseProgressionEntity) — estructura de entidad base. HU-06 (ExerciseSetEntity) — patrón de persistencia de sets.

**Patrones de código reutilizados:**
- Patrón `RotationResolver` (object singleton) → `ProgressionClassificationRule`
- DTOs de DAO con sufijo descriptivo → `SessionExerciseForProgression`, `ExerciseSetData`
- `database.withTransaction` para transacciones atómicas
- Funciones puras en `domain/rules/` sin dependencias Android

**Mejores prácticas aplicadas:**
- Regla pura testeable sin emulador (ADR-06, RNF29, RNF30)
- Tolerancia en comparaciones de punto flotante
- Query independiente de versión (ADR D-06)
- Machine of states pura para transiciones de progreso
- Proactivo: incluir `moduleCode` para HU-11

## 7. Tareas de Implementación

### Fase 1: Domain — Modelos de dominio puros

> Basado en Hito #1 del Análisis Arquitectónico

- [ ] **Crear `ProgressionClassification` enum** (infraestructura)
  - [ ] Crear `domain/model/ProgressionClassification.kt`. Enum con 3 valores: `POSITIVE_PROGRESSION`, `MAINTENANCE`, `REGRESSION`. `.name` produce strings exactos para persistencia.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ProgressionClassification.kt`

- [ ] **Crear `ExerciseSessionData` y `SetData`** (infraestructura)
  - [ ] `data class SetData(val weightKg: Double, val reps: Int, val rir: Int)`.
  - [ ] `data class ExerciseSessionData(val sets: List<SetData>)` con propiedades derivadas: `setCount`, `avgWeightKg`, `totalReps`, `avgRir`. Ver implementación completa en §5 Componentes Nuevos.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/model/ExerciseSessionData.kt`

### Fase 2: Domain — Regla pura + tests exhaustivos

> Basado en Hito #2 del Análisis Arquitectónico

- [ ] **Crear `ProgressionClassificationRule`** (AC: 02, 03, 04, 07, 08, 10, 11, 12)
  - [ ] Crear directorio `domain/rules/` (primer archivo del paquete — establece precedente para HU-11/HU-12/HU-14).
  - [ ] `object ProgressionClassificationRule`. Constantes: `RIR_SIGNIFICANT_RISE = 1.5`, `ISOMETRIC_MASTERED_THRESHOLD = 45`, `PLATEAU_THRESHOLD = 3`, `WEIGHT_TOLERANCE = 0.01`.
  - [ ] `fun classify(current, previous, isBodyweight, isIsometric): ProgressionClassification?` — dispatcher principal. Ver implementación completa en §5 Componentes Nuevos.
  - [ ] `private fun classifyStandard(current, previous)`, `classifyBodyweight(current, previous)`, `classifyIsometric(current, previous)`.
  - [ ] `fun isIsometricMastered(current): Boolean`.
  - [ ] `fun resolveNewProgressionState(currentStatus, currentCounter, classification, isIsometric, isMastered): Pair<String, Int>` — máquina de estados.
  - [ ] helpers: `isWeightEqual`, `isWeightHigher`, `isWeightLower` con tolerancia.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/domain/rules/ProgressionClassificationRule.kt`

- [ ] **Test unitario `ProgressionClassificationRuleTest`** (AC: 02, 03, 04, 07, 08, 10, 11, 12)
  - [ ] **classifyStandard tests (~8):** Peso subió + RIR estable → PP; mismo peso + reps subieron + RIR estable → PP; peso subió + RIR ≥ 1.5 → MAINTENANCE; mismo peso + reps iguales + RIR estable → MAINTENANCE; peso bajó → REGRESSION; mismo peso + reps bajaron → REGRESSION; mismo peso + RIR ≥ 1.5 → REGRESSION; peso subió + reps bajaron + RIR estable → PP (peso domina).
  - [ ] **classifyBodyweight tests (~5):** Reps ↑ + RIR estable → PP; reps iguales + RIR estable → MAINTENANCE; reps ↓ → REGRESSION; RIR ↑ ≥ 1.5 + reps iguales → REGRESSION; reps ↑ + RIR ↑ ≥ 1.5 → MAINTENANCE (RIR penaliza).
  - [ ] **classifyIsometric tests (~5):** Segundos ↑ + RIR estable → PP; segundos iguales + RIR estable → MAINTENANCE; segundos ↓ → REGRESSION; RIR ↑ ≥ 1.5 + segundos iguales → REGRESSION; segundos ↑ + RIR ↑ ≥ 1.5 → MAINTENANCE.
  - [ ] **classify dispatcher tests (~4):** `previous == null` → `null` (CA-10.07); `previous.sets` vacío → `null`; `current.sets` vacío → `null`; prioridad isIsometric > isBodyweight > estándar.
  - [ ] **isIsometricMastered tests (~3):** 4 sets todos ≥ 45s → `true`; 4 sets con 1 < 45s → `false`; 3 sets (incompleto) → `false`.
  - [ ] **resolveNewProgressionState tests (~8):** Isométrico mastered → MASTERED; `null` classification → no change; `IN_DELOAD` → no change; `MASTERED` → no change; PP → IN_PROGRESSION counter=0; NO_HISTORY + MAINTENANCE → IN_PROGRESSION counter=1; IN_PROGRESSION + MAINTENANCE ×2 → counter=2; IN_PROGRESSION + REGRESSION ×3 → IN_PLATEAU counter=3.
  - Archivo: `app/src/test/java/com/estebancoloradogonzalez/tension/domain/rules/ProgressionClassificationRuleTest.kt`

### Fase 3: Data Layer — DAOs

> Basado en Hito #3 del Análisis Arquitectónico

- [ ] **Modificar `SessionExerciseDao`** (AC: 01, 09)
  - [ ] Agregar DTO `SessionExerciseForProgression` con campos: `sessionExerciseId`, `exerciseId`, `isBodyweight`, `isIsometric`, `moduleCode`.
  - [ ] Agregar query `getSessionExercisesForProgression(sessionId)` — JOIN con `exercise` para flags.
  - [ ] Agregar query `updateProgressionClassification(sessionExerciseId, classification)`.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/SessionExerciseDao.kt`

- [ ] **Modificar `ExerciseSetDao`** (AC: 01, 09)
  - [ ] Agregar DTO `ExerciseSetData` con campos: `weightKg`, `reps`, `rir`.
  - [ ] Agregar query `getSetsForSessionExercise(sessionExerciseId)` — ordenado por `set_number`.
  - [ ] Agregar query `getLastHistoricalSets(exerciseId, currentSessionId)` — subconsulta independiente de versión.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/local/dao/ExerciseSetDao.kt`

### Fase 4: Data Layer — Repository

> Basado en Hito #4 del Análisis Arquitectónico

- [ ] **Modificar `SessionRepositoryImpl`** (AC: 01 a 12 — todos)
  - [ ] Crear método privado `evaluateProgression(sessionId)`.
  - [ ] Modificar `closeSession()` para insertar Step 2 (evaluateProgression) entre Step 1 (updateStatus) y Step 3 (advanceRotation).
  - [ ] Dentro de `evaluateProgression()`: para cada ejercicio, obtener sets actuales e históricos, construir `ExerciseSessionData`, invocar `ProgressionClassificationRule.classify()`, actualizar clasificación, resolver nuevo estado de progresión.
  - Archivo: `app/src/main/java/com/estebancoloradogonzalez/tension/data/repository/SessionRepositoryImpl.kt`
