## Análisis Arquitectónico

> Esta historia es una corrección de defecto (bug fix). No redefine la funcionalidad de HU-07 — la especificación original es correcta.

**Patrón arquitectónico:** Subconsulta SQL en DAO — reemplazo de snapshot estático por subconsulta reactiva embebida en Room.

### Componentes afectados

#### 1. Data Layer — DAO (Modificación Mayor)

- **`ExerciseDao`**: Agregar nuevo método `@Query` `getEligibleSubstitutesForSession(moduleCode: String, sessionId: Long): Flow<List<ExerciseWithDetails>>` con subconsulta SQL embebida. Eliminar `getByModuleCodeNotInIds()` (código muerto).
- **`SessionExerciseDao`**: Eliminar `getExerciseIdsForSession()` — código muerto.

#### 2. Domain Layer — Interfaz Repository (Modificación Mayor)

- **`ExerciseRepository`**: Cambiar firma de `getEligibleSubstitutes()` de `(moduleCode, excludedExerciseIds: List<Long>)` a `(moduleCode, sessionId: Long)`. La responsabilidad de calcular exclusiones se delega completamente al SQL.
- **`SessionRepository`**: Eliminar declaración `getExerciseIdsForSession()` — código muerto.

#### 3. Data Layer — Implementación Repository (Modificación Menor)

- **`ExerciseRepositoryImpl`**: Delegar al nuevo método DAO. El mapeo `.map { list -> list.map { it.toDomainModel() } }` se mantiene sin cambios.

#### 4. UI Layer — ViewModel (Modificación Mayor)

- **`SubstituteExerciseViewModel`**: Eliminar snapshot estático `excludedIds` del `init`. Eliminar invocación a `sessionRepository.getExerciseIdsForSession()`. Cambiar invocación de `exerciseRepository.getEligibleSubstitutes(info.moduleCode, excludedIds)` a `exerciseRepository.getEligibleSubstitutes(info.moduleCode, info.sessionId)`. Mantener inyección de `sessionRepository` (se usa para `getSubstituteExerciseInfo()`).

#### 5. Data Layer — Implementación Repository (Limpieza)

- **`SessionRepositoryImpl`**: Eliminar implementación `getExerciseIdsForSession()` (líneas ~293-295).

### Componentes NO modificados

- `substituteExercise()` en `SessionRepositoryImpl` — la mecánica de escritura de la sustitución no cambia
- `updateExerciseId()` en `SessionExerciseDao` — no se ve afectado
- `SubstituteExerciseUseCase` — sin cambios en la lógica de negocio
- Toda la capa UI (pantalla E3) — sin cambios visuales
- Todas las entidades y el esquema de BD — cero migraciones

### Integraciones

| Interfaz / Contrato | Productor | Consumidor | Descripción |
| --- | --- | --- | --- |
| `Flow<List<ExerciseWithDetails>>` | `ExerciseDao.getEligibleSubstitutesForSession()` | `ExerciseRepositoryImpl` | Query reactiva con subconsulta SQL embebida. Room rastrea `exercise` + `session_exercise` para invalidación automática |
| `Flow<List<Exercise>>` | `ExerciseRepositoryImpl` | `SubstituteExerciseViewModel` | Repositorio delega al DAO, mapea a dominio |
| `StateFlow<UiState>` | `SubstituteExerciseViewModel` | `SubstituteExerciseScreen` | Estado de la pantalla E3: isLoading, eligibleExercises, selectedExercise, etc. |

### Causa Raíz Identificada

La causa raíz de ambos defectos es que `SubstituteExerciseViewModel` computa `excludedIds` como una lista estática en el bloque `init`, y luego la pasa como parámetro externo (`List<Long>`) a `ExerciseDao.getByModuleCodeNotInIds()`.

Room solo invalida un `Flow` cuando detecta cambios en las tablas referenciadas **dentro del SQL de la query**. Al recibir los IDs como parámetro externo, Room no puede detectar cambios en `session_exercise` y el `Flow` nunca se re-emite.

**Cadena de invocación defectuosa:**
`SubstituteExerciseViewModel.init` → `SessionRepository.getExerciseIdsForSession()` (snapshot estático) → `ExerciseRepository.getEligibleSubstitutes(moduleCode, excludedIds)` → `ExerciseDao.getByModuleCodeNotInIds(moduleCode, excludedIds)`

**Cadena de invocación corregida:**
`SubstituteExerciseViewModel.init` → `ExerciseRepository.getEligibleSubstitutes(moduleCode, sessionId)` → `ExerciseDao.getEligibleSubstitutesForSession(moduleCode, sessionId)`

Se elimina un eslabón completo (SessionRepository) y se reducen los parámetros de 3 a 2.

### Análisis de Impacto en Performance

La subconsulta SQL es más eficiente que la solución actual (dos queries separadas: una para obtener IDs, otra para filtrar). Se reduce a una sola query con subconsulta que SQLite optimiza nativamente. No se introducen nuevos índices — la FK existente en `session_exercise.session_id` es suficiente.

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
| --- | --- | --- | --- |
| Eliminación de `getByModuleCodeNotInIds()`: si algún código no detectado lo consume, la compilación fallará | Baja | Medio | Verificado con grep — único consumidor era `ExerciseRepositoryImpl` |
| Eliminación de `getExerciseIdsForSession()`: mismo riesgo | Baja | Medio | Verificado con grep — único consumidor era `SubstituteExerciseViewModel` |
| Room invalidation tracking con subconsulta: Room no detecte la tabla `session_exercise` | Baja | Alto | Comportamiento estándar de Room — las tablas referenciadas en subconsultas se rastrean para invalidación (ADR-03) |

### Hitos de implementación

| Hito | Contenido | Dependencias |
| --- | --- | --- |
| 1 | Data Layer — DAO: nuevo `getEligibleSubstitutesForSession()`, eliminar `getByModuleCodeNotInIds()` | — |
| 2 | Domain Layer — Repository: cambiar firma de `getEligibleSubstitutes()` | Hito 1 |
| 3 | Data Layer — Repository Impl: delegar al nuevo método DAO | Hito 1, Hito 2 |
| 4 | UI Layer — ViewModel: eliminar snapshot estático, simplificar `init` | Hito 2, Hito 3 |
| 5 | Limpieza: eliminar `getExerciseIdsForSession()` de DAO, Repository, RepositoryImpl | Hito 4 |
| 6 | Tests: crear `SubstituteExerciseViewModelTest` con 7 tests | Hito 4 |

### Notas de auditoría

1. **El ejercicio siendo sustituido se excluye naturalmente.** Al abrir la pantalla de sustitución para un ejercicio X, ese ejercicio X todavía está asignado en `session_exercise` (la sustitución no ha ocurrido aún). Por lo tanto, la subconsulta `NOT IN (SELECT exercise_id FROM session_exercise WHERE session_id = :sessionId)` ya excluye a X automáticamente. No se requiere un parámetro adicional `excludeExerciseId`.

2. **Preservación de `original_exercise_id`.** No se ve afectado. La operación `updateExerciseId()` en `SessionExerciseDao` no cambia `original_exercise_id`. El cambio propuesto solo afecta la consulta de elegibles, no la escritura de la sustitución.

3. **Alternativa descartada: `Flow<List<Long>>` reactivo.** Se descartó convertir `getExerciseIdsForSession` a `Flow<List<Long>>` y combinar con `flatMapLatest` en el ViewModel porque: (1) introduce complejidad de coroutines innecesaria, (2) requiere dos queries separadas en lugar de una, (3) mantiene la separación de responsabilidad en el lugar equivocado (el ViewModel no debería ser responsable del filtrado SQL).

4. **Tipo de retorno `ExerciseWithDetails`.** El nuevo método del DAO retorna `ExerciseWithDetails` (data class definida en `ExerciseDao.kt` con JOINs a `module`, `equipment_type` y `muscle_zone`), consistente con los demás métodos de consulta del DAO. `ExerciseRepositoryImpl` mapea a `Exercise` de dominio vía `toDomainModel()` sin cambios.

5. **Room invalidation tracking con subconsulta.** Room detecta la tabla `session_exercise` en la subconsulta `NOT IN (SELECT se.exercise_id FROM session_exercise se WHERE se.session_id = :sessionId)` para auto-invalidar el `Flow`. Esto está documentado en el comportamiento de Room: las tablas referenciadas en el SQL completo (incluyendo subconsultas) se rastrean para invalidación.

### SQL del nuevo método DAO

```sql
-- getEligibleSubstitutesForSession
SELECT e.id, e.name, e.module_code, e.is_bodyweight, e.is_isometric, e.is_custom,
       e.media_resource, m.id AS moduleId, m.code AS moduleCode, m.name AS moduleName,
       et.id AS equipmentTypeId, et.name AS equipmentTypeName,
       emz.muscle_zone_id, mz.name AS muscleZoneName, emz.is_primary
FROM exercise e
INNER JOIN module m ON e.module_code = m.code
LEFT JOIN equipment_type et ON e.equipment_type_id = et.id
LEFT JOIN exercise_muscle_zone emz ON e.id = emz.exercise_id
LEFT JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
WHERE e.module_code = :moduleCode
  AND e.id NOT IN (
      SELECT se.exercise_id FROM session_exercise se WHERE se.session_id = :sessionId
  )
GROUP BY e.id
ORDER BY e.name ASC
```

**Room rastreo de tablas:** `exercise` + `session_exercise` → el `Flow` se re-emite automáticamente cuando cualquiera de estas tablas cambia. Esto incluye el momento en que `updateExerciseId()` modifica un registro de `session_exercise` durante una sustitución.
