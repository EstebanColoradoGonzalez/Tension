## Análisis Arquitectónico

> **Hallazgo Principal — HU-08 es una historia de naturaleza transversal (cross-cutting), no una funcionalidad independiente.**

HU-08 define 8 Criterios de Aceptación que se dividen en dos categorías:

1. **CAs de registro (CA-08.01, CA-08.04, CA-08.05, CA-08.08):** Cómo el formulario E2 adapta su interfaz y validación según el tipo de ejercicio. **Ya completamente implementados en HU-06.**

2. **CAs de progresión (CA-08.02, CA-08.03, CA-08.06, CA-08.07):** Cómo el motor de progresión evalúa ejercicios de peso corporal e isométricos con reglas diferentes al estándar. **Diferidos a HU-10/HU-11.**

**Conclusión: HU-08 no requiere trabajo de implementación autónomo.**

### Verificación Exhaustiva CA por CA

#### CAs de Registro — Ya implementados en HU-06

**CA-08.01 — Peso = 0 para ejercicios de peso corporal:**

| Capa | Implementación | Archivo | Evidencia |
|------|----------------|---------|-----------|
| ViewModel | `weightKg = "0"` cuando `info.isBodyweight == true` | `RegisterSetViewModel.kt` | `info.isBodyweight \|\| info.isIsometric -> "0"` |
| ViewModel | `isWeightEditable = false` | `RegisterSetViewModel.kt` | `!info.isBodyweight && !info.isIsometric` |
| Screen | Label cambia a "Peso (Kg) (Peso corporal)" | `RegisterSetScreen.kt` | `stringResource(R.string.register_set_weight_bodyweight_label)` |
| Screen | Campo deshabilitado con fondo `surfaceContainerHighest.copy(alpha = 0.5f)` | `RegisterSetScreen.kt` | `enabled = uiState.isWeightEditable` con colors custom |
| Repository | `lastWeightKg = 0.0` para bodyweight | `SessionRepositoryImpl.kt` | `if (info.isBodyweight == 1 \|\| info.isIsometric == 1) 0.0` |
| Strings | `register_set_weight_bodyweight_label` | `strings.xml` | `"Peso (Kg) (Peso corporal)"` |

**CA-08.04 — Registro de isométricos en segundos:**

| Capa | Implementación | Archivo | Evidencia |
|------|----------------|---------|-----------|
| Screen | Label cambia a "Segundos sostenidos" | `RegisterSetScreen.kt` | `if (uiState.isIsometric) stringResource(R.string.register_set_seconds_label)` |
| Screen | Suffix cambia a "seg" | `RegisterSetScreen.kt` | `if (uiState.isIsometric) stringResource(R.string.register_set_seconds_suffix)` |
| Screen | Label cambia a "Peso (Kg) (Isométrico)" | `RegisterSetScreen.kt` | `stringResource(R.string.register_set_weight_isometric_label)`. **Fix aplicado en auditoría:** el `when` original verificaba `isBodyweight` antes de `isIsometric`. Se invirtió el orden para que `isIsometric` se evalúe primero — los isométricos son subconjunto de bodyweight (`is_isometric = 1 → is_bodyweight = 1` en el modelo de datos). |
| UiState | Campo `isIsometric: Boolean` disponible | `RegisterSetUiState.kt` | `val isIsometric: Boolean = false` |
| Strings | `register_set_seconds_label`, `register_set_seconds_suffix` | `strings.xml` | `"Segundos sostenidos"`, `"seg"` |

**CA-08.05 — Validación ≥ 1 segundo + referencia visual 30-45s:**

| Capa | Implementación | Archivo | Evidencia |
|------|----------------|---------|-----------|
| ViewModel | Validación `parsed < 1` → `error_seconds_min` | `RegisterSetViewModel.kt` | `context.getString(R.string.error_seconds_min)` |
| Screen | `supportingText` muestra referencia cuando isometric y sin error | `RegisterSetScreen.kt` | `stringResource(R.string.register_set_seconds_reference)` |
| UseCase | `require(reps >= 1)` (campo `reps` almacena segundos para isométricos) | `RegisterSetUseCase.kt` | Validación de dominio |
| Strings | `register_set_seconds_reference`, `error_seconds_min` | `strings.xml` | `"(Referencia: 30–45 seg)"`, `"La duración debe ser ≥ 1 segundo"` |

**CA-08.08 — Validación de datos de entrada:**

| Capa | Implementación | Archivo | Evidencia |
|------|----------------|---------|-----------|
| UseCase | `require(weightKg >= 0)`, `require(reps >= 1)`, `require(rir in 0..5)` | `RegisterSetUseCase.kt` | 3 validaciones de dominio |
| ViewModel | Validación inline en `onWeightChanged` (peso < 0) | `RegisterSetViewModel.kt` | `error_weight_negative` |
| ViewModel | Validación inline en `onRepsChanged` (reps/seconds < 1) | `RegisterSetViewModel.kt` | Error diferenciado por tipo |
| Screen | RIR es selector fijo 0-5, imposible ingresar fuera de rango | `RegisterSetScreen.kt` | `for (rir in 0..5)` chips circulares |

#### CAs de Progresión — Diferidos a HU-10/HU-11

**CA-08.02 — Progresión bodyweight por repeticiones totales:** Responsabilidad de **HU-10** (Evaluar y clasificar progresión post-sesión). Motor comparará `SUM(reps)` de las 4 series del ejercicio bodyweight contra la sesión anterior. Regla 6 del MDS §6-A. **Infraestructura lista:** `exercise_set.reps` × 4 series por `session_exercise` ya se persisten correctamente.

**CA-08.03 — Exclusión de bodyweight del Doble Umbral:** Responsabilidad de **HU-11** (Motor de Doble Umbral). Para bodyweight, `Δmin = 0`. La exclusión se implementará como `if (!exercise.isBodyweight) applyDoubleThreshold()`. **Infraestructura lista:** `ExerciseEntity.isBodyweight` existe. `ExerciseProgressionEntity.prescribedLoadKg` es nullable (null = no aplica para peso corporal).

**CA-08.06 — Progresión isométrica por tiempo sostenido:** Responsabilidad de **HU-10**. Motor comparará los segundos sostenidos contra el rango prescrito (30-45 seg). Regla 7 del MDS §6-A. **Infraestructura lista:** Los segundos se almacenan en `reps` (Modelo de Datos §3.12: "Para ejercicios isométricos, este campo almacena los segundos sostenidos").

**CA-08.07 — Marcado de isométrico como "dominado":** Responsabilidad de **HU-10**. Cuando 4 de 4 series alcanzan ≥ 45 segundos, `ExerciseProgressionEntity.status` pasa a `"MASTERED"`. **Infraestructura lista:** `ExerciseProgressionEntity.status` existe con CHECK que incluye `'MASTERED'`.

### Validación de Impacto

**Código verificado (post HU-07):**

| Archivo | Estado | Resultado |
|---|---|---|
| `RegisterSetViewModel.kt` | 146 líneas | CA-08.01, CA-08.04, CA-08.05, CA-08.08 completamente cubiertos |
| `RegisterSetScreen.kt` | 307 líneas | 3 variantes de E2 (estándar, peso corporal, isométrico) implementadas |
| `RegisterSetUiState.kt` | 27 líneas | Campos `isBodyweight`, `isIsometric`, `isWeightEditable` presentes |
| `RegisterSetUseCase.kt` | ~20 líneas | 3 validaciones: peso ≥ 0, reps ≥ 1, RIR 0-5 |
| `SessionRepositoryImpl.kt` | 268 líneas | `getRegisterSetInfo` retorna `lastWeightKg = 0.0` para bodyweight/isometric |
| `ExerciseProgressionEntity.kt` | 33 líneas | Schema preparado: `status` con MASTERED, `prescribedLoadKg` nullable, `sessionsWithoutProgression` con default 0 |
| `strings.xml` | ~100 líneas | 7 strings de E2 para bodyweight/isometric presentes |

**Impacto en HU-10/HU-11:** Cuando se implemente el motor de progresión, deberá:
1. Detectar si el ejercicio es `isBodyweight` o `isIsometric` (flags ya persisten en `exercise`).
2. Para bodyweight: comparar `SUM(reps)` de las 4 series contra la sesión anterior (Regla 6).
3. Para isometric: comparar segundos sostenidos en rango 30-45s contra sesión anterior (Regla 7).
4. Para isometric: si las 4 series ≥ 45s → `status = "MASTERED"` (CA-08.07).
5. Para bodyweight: NO aplicar Doble Umbral (`prescribedLoadKg` permanece null).

### Notas Técnicas

1. **El campo `exercise_set.reps` almacena tanto repeticiones como segundos.** El Modelo de Datos §3.12 documenta que "Para ejercicios isométricos, este campo almacena los segundos sostenidos en lugar de repeticiones." No se requiere una columna separada. La interpretación del valor depende de `exercise.is_isometric`.
2. **HU-08 como historia transversal.** A diferencia de HU-01 a HU-07, que mapean 1:1 a funcionalidades concretas, HU-08 refina el comportamiento de E2 (HU-06) y del motor de progresión (HU-10/HU-11) para tipos de ejercicio especiales.
3. **La decisión de pre-implementar los CAs de registro en HU-06 fue correcta y deliberada.** Implementar las variantes de E2 junto con el formulario base evitó: (a) duplicar la creación de test scenarios de E2, (b) reabrir un composable ya validado, (c) introducir complejidad de merge entre branches.
4. **Orden `when` en WeightField:** `isIsometric` se evalúa antes de `isBodyweight` en toda la capa UI porque en el modelo de datos los isométricos son subconjunto de bodyweight (`is_isometric = 1 → is_bodyweight = 1`). Consistente con `ActiveSessionViewModel` en E1.
5. **Relación con la Especificación Visual.** La Especificación Visual §8 E2 documenta explícitamente las 3 variantes de E2 con su trazabilidad: *"HU-06 (CA-06.01 a CA-06.09) · HU-08 (CA-08.01, CA-08.04, CA-08.05, CA-08.08)"*. Ambas HUs contribuyen a la misma pantalla, pero la implementación completa está en HU-06.
