## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-12

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Análisis Arquitectónico de HU-11 con 8 decisiones de diseño, 3 hitos de implementación, 3 componentes (1 nuevo + 2 modificados). Hallazgos principales: (1) HU-11 es lógica pura de backend — no hay UI, ViewModel ni pantalla nueva. (2) La Regla de Doble Umbral es función pura en `domain/rules/DoubleThresholdRule.kt` (ADR-06, Arquitectura Técnica §5.2). (3) Los incrementos de carga provienen de `module.load_increment_kg` en la BD, no de constantes hardcodeadas (reinterpretación ADR D-07). (4) `prescribeLoad()` siempre retorna valor para ejercicios estándar: carga+incremento (si cumple) o carga actual (si no cumple). (5) Bodyweight e isométricos quedan excluidos con guard (`prescribedLoadKg = null`). (6) La evaluación se integra dentro del loop existente de `evaluateProgression()` como paso 5b, extendiendo el `copy()` del `exerciseProgressionDao.update()`. (7) CA-11.05 (reinicio a 8 reps) es derivable — sin persistencia adicional. (8) El Doble Umbral es INDEPENDIENTE de la clasificación de progresión (HU-10).

**Nivel de complejidad:**
BAJA — HU-11 toca 4 archivos (2 nuevos + 2 modificados) con lógica sencilla y predecible. La regla pura tiene 2 funciones simples (`meetsDoubleThreshold` + `prescribeLoad`), sin ramificación compleja ni máquina de estados. La integración extiende código existente (HU-10) con adiciones mínimas: 1 campo nuevo en DTO, 1 JOIN adicional en query, ~10 líneas de lógica en `evaluateProgression()`. Los tests son combinatorios pero de baja complejidad (~12 escenarios). No hay componentes UI.

**Riesgos técnicos conocidos:**

1. **JOIN adicional a `module` en query de progresión:** La query `getSessionExercisesForProgression` agrega un JOIN a `module` (via `exercise.module_code = module.code`). El costo es marginal — lookup por PK string, tabla `module` tiene solo 3 filas. El JOIN ya existe implícitamente en la cadena `exercise → module` y los índices están presentes (`module_code` indexado en `exercise`).
2. **`avgWeightKg` como base de prescripción:** En escenarios normales, todas las series usan el mismo peso, por lo que `avgWeightKg` = peso real. En caso extremo de pesos diferentes intra-ejercicio, el promedio podría producir un valor no estándar (ej: 47.5 Kg). Esto es aceptable — la UI muestra el valor con un decimal y el ejecutante ajusta al equipo disponible.
3. **Prescripción de carga durante `IN_DELOAD`:** HU-11 prescribe carga normalmente durante descarga. El valor se sobrescribirá por HU-17 (90% de pre-descarga, CA-17.05) al finalizar el protocolo. No hay conflicto — el flujo es secuencial.

**Patrones y convenciones del equipo (establecidos en HU-01—HU-10):**

- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Nombre}Rule` para reglas del motor (§5.2) — `DoubleThresholdRule`
- `object` singleton para reglas puras (patrón `ProgressionClassificationRule`, `RotationResolver`)
- DTOs de DAO como `data class` con sufijo descriptivo (patrón `SessionExerciseForProgression`)
- Funciones puras testeables sin emulador, mocks ni coroutines (ADR-06)
- Constantes como `const val` dentro del `object` (patrón `RIR_SIGNIFICANT_RISE = 1.5` en `ProgressionClassificationRule`)
- `exerciseProgressionDao.update(currentProgression.copy(...))` — patrón ya establecido en HU-10 L372-375

**Dependencias nuevas a instalar:**
Ninguna.

**Estrategia de testing:**
JUnit 4 (sin MockK — función pura) | `DoubleThresholdRuleTest` con ~12 escenarios: `meetsDoubleThreshold()` × (cumple ambas / solo reps / solo RIR / ninguna / <4 sets / boundary 12 reps / boundary 2.0 RIR) + `prescribeLoad()` × (cumple → incremento / no cumple → mantiene / diferentes incrementos / carga 0). Foco en cobertura de condiciones del Doble Umbral y valores de prescripción.

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**

- **HU-10 (ProgressionClassificationRule)** — Implementó `evaluateProgression()` con el loop por ejercicio, creó `ProgressionClassificationRule` como `object` en `domain/rules/`, y estableció el patrón `exerciseProgressionDao.update(currentProgression.copy(status=..., sessionsWithoutProgression=...))`. HU-11 extiende este `copy()` agregando `prescribedLoadKg`. El `moduleCode` fue proactivamente incluido en `SessionExerciseForProgression` (HU-10 Nota 5) — ahora se complementa con `loadIncrementKg` (lectura directa del valor desde BD).
- **HU-05 (SessionExerciseWithDetails)** — Implementó el LEFT JOIN a `exercise_progression` en `getBySessionIdWithDetails` que ya lee `ep.prescribed_load_kg AS prescribedLoadKg`. Este es el consumer final del valor que HU-11 escribe: al iniciar la próxima sesión, E1 muestra la carga objetivo.
- **HU-06 (RegisterSet)** — Creó `ExerciseProgressionEntity` con `insertIfNotExists()` (L221 SessionRepositoryImpl). Garantiza que la entidad existe antes de que HU-10/HU-11 la lean/actualicen.

**Patrones de código reutilizados:**

- `object` singleton con funciones puras: `ProgressionClassificationRule.classify()` → `DoubleThresholdRule.meetsDoubleThreshold()` + `prescribeLoad()`
- Extension del `copy()` en `exerciseProgressionDao.update()`: HU-10 ya hace `copy(status=..., sessionsWithoutProgression=...)` → HU-11 agrega `prescribedLoadKg=...`
- DTO extension con JOIN adicional: `SessionExerciseForProgression` ya tiene `moduleCode` → se agrega `loadIncrementKg` con JOIN a `module`
- Helper factories en tests: `sessionData(vararg sets)` + `set(weightKg, reps, rir)` — patrón de `ProgressionClassificationRuleTest`

**Mejores prácticas aplicadas:**

- Incremento de carga leído de BD (`module.load_increment_kg`) en vez de constantes Kotlin — una sola fuente de verdad
- Guard `isBodyweight || isIsometric` antes de invocar regla — separación: repositorio decide SI, regla decide CUÁNTO
- `prescribeLoad()` recibe incremento como parámetro → testeable con cualquier valor de incremento sin DB
- `meetsDoubleThreshold()` requiere `setCount >= 4` — previene incremento prematuro en sesiones incompletas

---

### Tareas de Implementación

#### Fase 1: Domain — Regla pura + tests exhaustivos

##### Domain Rules

- [x] **Crear `DoubleThresholdRule`** (AC: 1, 2, 3, 4, 6)
  - [x] Crear archivo: `domain/rules/DoubleThresholdRule.kt`
  - [x] Constantes: `REP_THRESHOLD = 12`, `MIN_SERIES_MEETING_REP_THRESHOLD = 3`, `REQUIRED_SETS = 4`, `RIR_THRESHOLD = 2.0`
  - [x] `fun meetsDoubleThreshold(current: ExerciseSessionData): Boolean` — guard `setCount < 4` → false, luego verifica reps ≥ 12 en ≥ 3 series AND avgRir ≥ 2.0
  - [x] `fun prescribeLoad(currentAvgWeightKg: Double, loadIncrementKg: Double, meetsThreshold: Boolean): Double` — si cumple: carga + incremento, si no: carga actual
- [x] **Test unitario `DoubleThresholdRuleTest`** (AC: 1, 2, 3, 4, 6)
  - [x] Crear archivo: `src/test/java/com/estebancoloradogonzalez/tension/domain/rules/DoubleThresholdRuleTest.kt`
  - [x] Reutilizar helpers: `sessionData(vararg sets)` + `set(weightKg, reps, rir)`
  - [x] **meetsDoubleThreshold tests (~7):**
    - [x] 4 sets × 12 reps × RIR 2 → `true` (ambas condiciones OK, CA-11.01)
    - [x] 3 sets × 12 reps + 1 set × 11 reps, RIR avg ≥ 2 → `true` (3/4 es suficiente, CA-11.01)
    - [x] 4 sets × 12 reps pero RIR avg < 2 → `false` (solo reps, CA-11.02)
    - [x] 4 sets × 10 reps pero RIR avg ≥ 2 → `false` (solo RIR, CA-11.02)
    - [x] 3 sets × 10 reps + 1 set × 12 reps, RIR avg ≥ 2 → `false` (solo 1/4 series, CA-11.02)
    - [x] 3 sets únicamente (sesión incompleta) → `false` (guard setCount < 4)
    - [x] Boundary: 4 sets con RIR [2,2,2,1] → avg 1.75 → `false` (< 2.0, Nota 4)
  - [x] **prescribeLoad tests (~4):**
    - [x] Cumple + incremento 2.5 → 40.0 + 2.5 = 42.5 (CA-11.03, módulo A/B)
    - [x] Cumple + incremento 5.0 → 60.0 + 5.0 = 65.0 (CA-11.04, módulo C)
    - [x] No cumple → 40.0 (misma carga, CA-11.06)
    - [x] Cumple + carga 0.0 + incremento 2.5 → 2.5 (edge case primer peso)

#### Fase 2: Data — DTO + query extension

##### Data Layer

- [x] **Extender DTO y query en `SessionExerciseDao`** (AC: 3, 4)
  - [x] Agregar campo `loadIncrementKg: Double` al data class `SessionExerciseForProgression`
  - [x] Modificar query `getSessionExercisesForProgression`: agregar `INNER JOIN module m ON e.module_code = m.code` y seleccionar `m.load_increment_kg AS loadIncrementKg`

#### Fase 3: Data — Integración en evaluateProgression()

##### Repository

- [x] **Extender `evaluateProgression()` en `SessionRepositoryImpl`** (AC: 7, 8)
  - [x] Agregar import: `com.estebancoloradogonzalez.tension.domain.rules.DoubleThresholdRule`
  - [x] Agregar paso 5b entre resolveNewProgressionState y exerciseProgressionDao.update(): calcular `prescribedLoadKg`
  - [x] Guard: `if (isBodyweight || isIsometric) → prescribedLoadKg = null` (CA-11.08)
  - [x] Else: `val meetsThreshold = DoubleThresholdRule.meetsDoubleThreshold(currentData)` + `val prescribedLoadKg = DoubleThresholdRule.prescribeLoad(currentData.avgWeightKg, exercise.loadIncrementKg, meetsThreshold)`
  - [x] Extender el `copy()` existente en `exerciseProgressionDao.update()`: agregar `prescribedLoadKg = prescribedLoadKg`

#### Fase N: QA y Deployment

- [x] **Ejecutar Agente Peer Review** (MANUAL)
- [x] **Resolver incidentes del Peer Review** (MANUAL, condicional)
- [x] **Crear Pull Request** (MANUAL)
- [x] **Ejecutar pipeline deployment DEV** (MANUAL)
- [x] **Diseñar set de pruebas manuales** (MANUAL)
- [x] **Ejecutar pruebas manuales** (MANUAL)

---

**Notas sobre vinculación con Criterios de Aceptación:**

- CA-11.01 → Fase 1 (`meetsDoubleThreshold()` — ≥12 reps en ≥3/4 series AND avgRir ≥2, ambas simultáneas) + tests
- CA-11.02 → Fase 1 (`meetsRepCondition && meetsRirCondition` — operador AND exige ambas) + tests (solo reps / solo RIR → false)
- CA-11.03 → Fase 1 (`prescribeLoad(loadIncrementKg=2.5)`) + Fase 2 (`module.load_increment_kg` = 2.5 para A/B, leído de BD via JOIN) + tests
- CA-11.04 → Fase 1 (`prescribeLoad(loadIncrementKg=5.0)`) + Fase 2 (`module.load_increment_kg` = 5.0 para C, leído de BD via JOIN) + tests
- CA-11.05 → Ya cubierto (derivable — rango `8-12` en `plan_assignment.reps`, mostrado por `SessionExerciseWithDetails` en E1, HU-05)
- CA-11.06 → Fase 1 (`prescribeLoad(meetsThreshold=false)` → retorna `currentAvgWeightKg`) + tests
- CA-11.07 → Fase 3 (`exerciseProgressionDao.update(copy(prescribedLoadKg=...))` — persistido en `exercise_progression.prescribed_load_kg`, consumido por HU-05 en próxima sesión)
- CA-11.08 → Fase 3 (guard `isBodyweight || isIsometric → null` — skip antes de invocar regla, RF31)
