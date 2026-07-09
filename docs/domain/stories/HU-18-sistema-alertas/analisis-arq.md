## Análisis Arquitectónico

> HU-18 consolida 5 historias originales (HU-26 a HU-30) en un sistema de alertas integral con dos caras: write-side y read-side.

**Patrón arquitectónico:** Motor de Alertas Write-Time en Pipeline Existente + MVVM Read-Only para 2 pantallas (H1, H2) con `AlertRepository` dedicado.

### Componentes afectados

#### 1. Write-Side — Extensión de Pipeline de Evaluación

Extiende `SessionRepositoryImpl.evaluateProgression()` con 5 bloques de evaluación nuevos (Steps 7-11) que crean/resuelven alertas al cierre de cada sesión.

- **SessionRepositoryImpl:** Agregar 5 bloques de evaluación que reutilizan las Rules existentes de HU-15.
- **Nota crítica:** Steps 9 (adherencia) y 11 (inactividad) se ejecutan ANTES del `if (isDeloadSession) return`. Steps 7, 8, 10 permanecen protegidos por el guard.

#### 2. Read-Side — Pantalla Centro de Alertas (H1)

Paquete: `ui.alerts`.

- **`AlertCenterScreen`:** Pantalla de solo lectura con 2 secciones: "Crisis" (solo 🔴) y "Alertas" (🟠 + 🟡). Mapeo: `CRISIS` → 🔴, `HIGH_ALERT` → 🟠, `MEDIUM_ALERT` → 🟡. Bottom Navigation con "Inicio" activo.
- **`AlertCenterViewModel`:** `@HiltViewModel`. Recolecta alertas activas con `AlertRepository.getActiveAlerts()`. Badge B1 con `AlertDao.countActive()` (reactivo).
- **Bottom Navigation:** Agregar `childRoutePrefixes = setOf("alert-center", "alert-detail")` al tab HOME en `BottomNavigationBar.kt`.

#### 3. Read-Side — Pantalla Detalle de Alerta (H2)

Paquete: `ui.alerts`.

- **`AlertDetailScreen`:** Pantalla de solo lectura con recálculo dinámico de datos. Muestra información detallada, análisis causal según tipo de alerta, y links de acción condicionales.
- **`AlertDetailViewModel`:** `@HiltViewModel`. Carga datos de alerta específica y recálcula datos dinámicos.
- **Análisis causal varía por tipo:**
  - PLATEAU: `PlateauCausalAnalysisRule.analyze()` + `CorrectiveActionRule.recommend()` (HU-12).
  - LOW_PROGRESSION_RATE: texto fijo según nivel.
  - RIR_OUT_OF_RANGE: texto condicional (< 1.5 → fatiga, > 3.5 → estímulo insuficiente).
  - LOW_ADHERENCE: datos de semana(s) afectada(s).
  - TONNAGE_DROP: descarga planificada (azul) vs regresión.
  - MODULE_INACTIVITY: días transcurridos + grupos musculares.
- **Links de acción condicionales:**
  - "Ver historial del ejercicio →" (→ F3): visible si `type ∈ {PLATEAU, LOW_PROGRESSION_RATE}` y `exercise_id != null`.
  - "Gestionar descarga →" (→ I1): visible si `type ∈ {MODULE_REQUIRES_DELOAD}` o si `RIR_OUT_OF_RANGE` con RIR < 1.5.

#### 4. Data Layer — Alert Entity (NO modificado)

- **`AlertEntity`:** No se modifica (0 cambios de esquema, 0 migraciones). Versión de `TensionDatabase.kt` se mantiene en 7. `AlertDao` ya está registrado.

#### 5. Deduplicación y Escalamiento de Alertas

- **Deduplicación:** El pipeline verifica `existsActiveByExercise/Module/MuscleGroup()` antes de insertar.
- **Escalamiento:** Al subir de nivel (ej: inactividad pasa de 10 a 14 días) → resolver alerta existente + crear nueva con nivel correcto. Al bajar → CRISIS → resolver + crear MEDIUM_ALERT. Al desaparecer → resolver todas las alertas activas del tipo para la entidad.

---

### Dependencias Técnicas

#### Rules reutilizadas (sin modificación)

| # | Rule | Uso en HU-18 |
|---|---|---|
| R1 | `ProgressionRateRule` | Tasa de progresión para CA-18.01/02 (write-side) y recálculo H2 |
| R2 | `AvgRirRule` | RIR promedio por módulo para CA-18.06/07 (write-side) y H2 |
| R3 | `AdherenceRule` | Adherencia semanal para CA-18.12/13 (write-side) y H2 |
| R4 | `TonnageRule` | Tonelaje por grupo muscular para CA-18.17/18 (write-side) y H2 |
| R5 | `PlateauCausalAnalysisRule` | Análisis causal de mesetas en H2 (HU-12) |
| R6 | `CorrectiveActionRule` | Recomendaciones escalonadas en H2 (HU-12) |
| R7 | `ModuleFatigueRule` | Detección de fatiga acumulada — contextualiza alertas meseta grupal (HU-12) |

#### Queries DAO reutilizados (sin modificación)

| # | Query | Uso |
|---|---|---|
| R8 | `AlertDao.insert()` | Inserción de alertas (HU-12) |
| R9 | `AlertDao.existsActiveByExercise()` | Deduplicación por ejercicio (HU-12) |
| R10 | `AlertDao.existsActiveByModule()` | Deduplicación por módulo (HU-12) |
| R11 | `AlertDao.resolveByExerciseAndType()` | Resolución por ejercicio (HU-12) |
| R12 | `AlertDao.resolveByModuleAndType()` | Resolución por módulo (HU-12) |
| R13 | `AlertDao.countActive()` | Conteo reactivo badge B1 (HU-12) |
| R14 | `AlertDao.getActiveAlerts()` | Lista de alertas activas (HU-12) |
| R15 | `SessionDao.getSessionIdsByModuleInRange()` | Últimas N sesiones de módulo para RIR (con `deload_id IS NULL`) |
| R16 | `SessionDao.countSessionsInWeek()` | Sesiones completadas en semana para adherencia |
| R17 | `ExerciseSetDao.getRirValuesBySessionIds()` | Valores RIR para cálculo RIR promedio |

---

### Componentes NO tocados

- `data/local/entity/` — `AlertEntity` no se modifica (0 cambios de esquema, 0 migraciones)
- `TensionDatabase.kt` — versión se mantiene en 7; `AlertDao` ya está registrado
- `data/local/seed/` — no se toca ningún seeder
- Flujos E, C, D, F, G, I, J — no se modifican sus pantallas

---

### Notas Técnicas

**Nota 1 — Mapeo tipo de alerta → nivel visual (H1).**
3 niveles: 🔴 Crisis (rojo/Error Container), 🟠 Alerta alta (naranja), 🟡 Alerta media (amarillo). Mapeo: `CRISIS` → 🔴, `HIGH_ALERT` → 🟠, `MEDIUM_ALERT` → 🟡. H1 agrupa en 2 secciones: "Crisis" (solo 🔴) y "Alertas" (🟠 + 🟡).

**Nota 2 — H2: Análisis causal varía por tipo de alerta.**
PLATEAU: `PlateauCausalAnalysisRule.analyze()` + `CorrectiveActionRule.recommend()` (HU-12). LOW_PROGRESSION_RATE: texto fijo según nivel. RIR_OUT_OF_RANGE: texto condicional (< 1.5 → fatiga, > 3.5 → estímulo insuficiente). LOW_ADHERENCE: datos de semana(s) afectada(s). TONNAGE_DROP: descarga planificada (azul) vs regresión. MODULE_INACTIVITY: días transcurridos + grupos musculares (CA-18.25).

**Nota 3 — H2: Links de acción condicionales.**
"Ver historial del ejercicio →" (→ F3): visible si `type ∈ {PLATEAU, LOW_PROGRESSION_RATE}` y `exercise_id != null`. "Gestionar descarga →" (→ I1): visible si `type ∈ {MODULE_REQUIRES_DELOAD}` o si `RIR_OUT_OF_RANGE` con RIR < 1.5.

**Nota 4 — Deduplicación de alertas.**
El pipeline verifica `existsActiveByExercise/Module/MuscleGroup()` antes de insertar. Si la condición cambia de nivel (ej: inactividad pasa de 10 a 14 días), la alerta MEDIUM_ALERT se resuelve y se crea nueva CRISIS. Patrón: resolve + insert (no update).

**Nota 5 — Escalamiento de nivel (MEDIUM_ALERT → CRISIS).**
Pipeline: (1) resolver alerta existente, (2) crear nueva con nivel correcto. Al bajar: CRISIS → resolver + crear MEDIUM_ALERT. Al desaparecer: resolver todas las alertas activas del tipo para la entidad.

**Nota 6 — Grupos musculares por módulo para CA-18.25.**
Datos fijos del dominio: A = Espalda, Bíceps, Abdomen; B = Pecho, Hombro, Tríceps; C = Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos. Codificados como constante en `AlertThresholdRule.MUSCLE_GROUPS_BY_MODULE`.

**Nota 7 — Bottom Navigation en H1 y H2.**
El wireframe muestra "Inicio" activo en H1/H2. `showBottomBar` en `TensionNavHost` ya mostrará el Bottom Nav (alert-center/alert-detail no están en el blocklist). Solo falta agregar `childRoutePrefixes = setOf("alert-center", "alert-detail")` al tab HOME en `BottomNavigationBar.kt`.

**Nota 8 — Evaluación de tonelaje reutiliza lógica de `GetMicrocycleMapUseCase`.**
`closedSessions.chunked(6)` → microciclos. Tonelaje con `TonnageRule.calculateForMuscleGroup()`. Verificación de descarga: `currentMicrocycle.any { it.deloadId != null }` → nivel MEDIUM_ALERT (no CRISIS) y mensaje contextualizado.

**Nota 9 — `getSessionIdsByModuleInRange()` ya filtra sesiones de descarga.**
El query existente incluye `AND s.deload_id IS NULL` — garantiza que CA-18.08 evalúe solo sesiones normales.

**Nota 10 — Corrección crítica: Steps 9 y 11 se ejecutan ANTES del deload guard.**
El Análisis Arquitectónico asumía que todos los Steps se ejecutan después del deload guard. Corrección: CA-18.12 evalúa adherencia — una sesión de descarga SÍ cuenta para la frecuencia semanal. CA-18.29 dice que "completar una sesión de ese módulo" resuelve la inactividad — incluyendo sesiones de descarga. Steps 9 y 11 se mueven ANTES de `if (isDeloadSession) return`. Steps 7, 8, 10 permanecen protegidos por el guard (correcto).
