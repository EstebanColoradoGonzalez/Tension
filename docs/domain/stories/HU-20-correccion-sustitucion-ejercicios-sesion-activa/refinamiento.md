## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-03-04

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
Corrección de defecto — reemplazo de snapshot estático por subconsulta reactiva embebida en Room. La especificación original de HU-07 es correcta. Los criterios de aceptación CA-07.01 a CA-07.06 siguen siendo válidos.

**Nivel de complejidad:**
BAJA — El cambio es quirúrgico: reemplazar una query DAO con parámetros externos por una subconsulta SQL embebida, actualizar la firma del Repository, simplificar el ViewModel eliminando el snapshot estático, y limpiar métodos de código muerto. No hay nuevas entidades, nuevas pantallas, ni lógica de negocio nueva. La superficie de cambio es reducida (~7 archivos) y todos los patrones ya existen en el proyecto.

**Riesgos técnicos conocidos:**
1. Eliminación de `getByModuleCodeNotInIds()` — si algún código no detectado lo consume, la compilación fallará. Verificado con grep — único consumidor era `ExerciseRepositoryImpl`.
2. Eliminación de `getExerciseIdsForSession()` — mismo riesgo. Verificado con grep — único consumidor era `SubstituteExerciseViewModel`.

**Patrones y convenciones del equipo:**
- DAOs: queries `@Query` con JOINs completos retornando `ExerciseWithDetails`, `Flow<List<T>>` para consultas reactivas, `suspend fun` para operaciones puntuales
- Repository: interfaz en `domain/repository/`, implementación en `data/repository/Impl`, mapeo con `toDomainModel()`
- ViewModel: `@HiltViewModel` con `MutableStateFlow<UiState>` + `asStateFlow()`, carga reactiva en `init {}` via `viewModelScope.launch`
- Tests: JUnit 4 + MockK + `kotlinx.coroutines.test` (`runTest`, `StandardTestDispatcher`, `advanceUntilIdle()`)
- Naming: `get{Entities}For{Context}()` para DAOs, `getEligibleSubstitutes()` para Repository

**Dependencias nuevas a instalar:** Ninguna.

**Estrategia de testing:** JUnit 4 + MockK + Kotlin Coroutines Test. Tests unitarios para `SubstituteExerciseViewModel` (nuevo, 7 tests). `SubstituteExerciseUseCase` sin cambios. Cobertura: 13 CAs validados por la cadena de invocación corregida.

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
- **HU-07** ([HU-07.md](HU-07.md)): Historia origen con el defecto. Definió `SubstituteExerciseViewModel`, `ExerciseDao.getByModuleCodeNotInIds()`, `SubstituteExerciseUseCase` y la cadena con snapshot estático.
- **HU-05** ([HU-05.md](HU-05.md)): Definió la creación de `session_exercise` — fuente de verdad para el filtrado dinámico.
- **HU-19** ([HU-19.md](HU-19.md)): Último refinamiento completado. Patrón de ViewModel test con `StandardTestDispatcher` + `advanceUntilIdle()` reutilizado para `SubstituteExerciseViewModelTest`.

**Patrones de código reutilizados:**
- Subconsulta SQL `NOT IN (SELECT ... FROM ... WHERE ...)` — patrón ya usado en `ExerciseDao.getByModuleCodeNotInVersion()`
- `Flow<List<ExerciseWithDetails>>` como tipo de retorno reactivo en DAO
- ViewModel test con `StandardTestDispatcher` + `Dispatchers.setMain()` + `advanceUntilIdle()`

**Mejores prácticas aplicadas:**
- Subconsulta SQL embebida en lugar de parámetros externos para habilitar Room invalidation tracking
- Eliminación de código muerto para simplificar la superficie de cambio
- Patrón de ViewModel test con `StandardTestDispatcher` + `Dispatchers.setMain()` + `advanceUntilIdle()`

---

### Tareas de Implementación

#### Fase 1: Capa Data — DAO

**ACs vinculados:** CA-20.01, CA-20.02, CA-20.03, CA-20.04, CA-20.05, CA-20.06, CA-20.07, CA-20.08, CA-20.09, CA-20.10, CA-20.13

- [x] **Crear método `getEligibleSubstitutesForSession()` en `ExerciseDao`**
  - [x] Agregar nuevo método `@Query` en `data/local/dao/ExerciseDao.kt`
  - [x] SQL con subconsulta embebida:
    ```sql
    WHERE e.module_code = :moduleCode
      AND e.id NOT IN (
          SELECT se.exercise_id FROM session_exercise se WHERE se.session_id = :sessionId
      )
    GROUP BY e.id
    ORDER BY e.name ASC
    ```
  - [x] JOINs completos a `module`, `equipment_type`, `exercise_muscle_zone`, `muscle_zone` — idénticos al patrón de `getByModuleCodeNotInIds()`
  - [x] Firma: `fun getEligibleSubstitutesForSession(moduleCode: String, sessionId: Long): Flow<List<ExerciseWithDetails>>`
  - [x] Room rastreará tablas `exercise` y `session_exercise` para invalidación automática

#### Fase 2: Capa Domain — Interfaz Repository

**ACs vinculados:** CA-20.04, CA-20.06, CA-20.13

- [x] **Actualizar firma de `getEligibleSubstitutes()` en `ExerciseRepository`**
  - [x] Modificar: `domain/repository/ExerciseRepository.kt`
  - [x] Cambiar de: `fun getEligibleSubstitutes(moduleCode: String, excludedExerciseIds: List<Long>): Flow<List<Exercise>>`
  - [x] A: `fun getEligibleSubstitutes(moduleCode: String, sessionId: Long): Flow<List<Exercise>>`
  - [x] La responsabilidad de calcular exclusiones se delega completamente al SQL

#### Fase 3: Capa Data — Implementación Repository

**ACs vinculados:** CA-20.04, CA-20.06, CA-20.13

- [x] **Actualizar implementación de `getEligibleSubstitutes()` en `ExerciseRepositoryImpl`**
  - [x] Modificar: `data/repository/ExerciseRepositoryImpl.kt`
  - [x] Cambiar de: `exerciseDao.getByModuleCodeNotInIds(moduleCode, excludedExerciseIds)`
  - [x] A: `exerciseDao.getEligibleSubstitutesForSession(moduleCode, sessionId)`
  - [x] El mapeo `.map { list -> list.map { it.toDomainModel() } }` se mantiene sin cambios

#### Fase 4: Capa UI — ViewModel

**ACs vinculados:** CA-20.01, CA-20.02, CA-20.03, CA-20.04, CA-20.05, CA-20.11, CA-20.12

- [x] **Simplificar `SubstituteExerciseViewModel`**
  - [x] Modificar `init {}` en `ui/session/SubstituteExerciseViewModel.kt`
  - [x] Eliminar: `val excludedIds = sessionRepository.getExerciseIdsForSession(info.sessionId)`
  - [x] Cambiar invocación de: `exerciseRepository.getEligibleSubstitutes(info.moduleCode, excludedIds)`
  - [x] A: `exerciseRepository.getEligibleSubstitutes(info.moduleCode, info.sessionId)`
  - [x] Eliminar import de `SessionRepository` si ya no se usa para otro propósito en este ViewModel
  - [x] **Verificar:** `sessionRepository` sigue usándose para `getSubstituteExerciseInfo()` — mantener inyección

- [x] **Crear test unitario `SubstituteExerciseViewModelTest`** (CA-20.01, CA-20.02, CA-20.04, CA-20.05)
  - [x] Crear archivo: `test/java/com/estebancoloradogonzalez/tension/ui/session/SubstituteExerciseViewModelTest.kt`
  - [x] Patrón: `StandardTestDispatcher` + `Dispatchers.setMain()` + `advanceUntilIdle()` + MockK
  - [x] Test 1: estado inicial es `isLoading = true`
  - [x] Test 2: cuando `getSubstituteExerciseInfo` retorna null → emite `navigateBack` (fix: usar `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))` para collector activo antes de `advanceUntilIdle()`)
  - [x] Test 3: carga exitosa → `isLoading = false`, `eligibleExercises` con ejercicios del mock, `originalExerciseName` correcto
  - [x] Test 4: `getEligibleSubstitutes` se invoca con `(moduleCode, sessionId)` — **no** con `excludedIds`
  - [x] Test 5: `onExerciseSelected` actualiza `selectedExercise` y muestra dialog
  - [x] Test 6: `onConfirmSubstitution` invoca use case y emite `navigateBack`
  - [x] Test 7: `onDismissDialog` limpia `selectedExercise` y oculta dialog

#### Fase 5: Limpieza — Código Muerto

- [x] **Eliminar `getByModuleCodeNotInIds()` de `ExerciseDao`**
  - [x] Eliminar método y su `@Query` SQL de `data/local/dao/ExerciseDao.kt`
  - [x] Único consumidor era `ExerciseRepositoryImpl` (ya migrado) — confirmado por grep

- [x] **Eliminar `getExerciseIdsForSession()` de `SessionExerciseDao`**
  - [x] Eliminar método `@Query` en `data/local/dao/SessionExerciseDao.kt` (línea ~169)
  - [x] Único consumidor era `SessionRepositoryImpl` — confirmado por grep

- [x] **Eliminar `getExerciseIdsForSession()` de `SessionRepository`**
  - [x] Eliminar declaración en `domain/repository/SessionRepository.kt` (línea ~26)

- [x] **Eliminar implementación de `getExerciseIdsForSession()` en `SessionRepositoryImpl`**
  - [x] Eliminar override en `data/repository/SessionRepositoryImpl.kt` (líneas ~293-295)

#### Fase 6: QA y Deployment

- [ ] **Ejecutar Agente Peer Review** (MANUAL)
- [ ] **Resolver incidentes del Peer Review** (MANUAL, condicional)
- [ ] **Crear Pull Request** (MANUAL)
- [ ] **Ejecutar pipeline deployment DEV** (MANUAL)
- [ ] **Diseñar y ejecutar pruebas manuales** (MANUAL)

---

### Vinculación CAs → Fases

| CA | Fase(s) | Mecanismo |
|----|---------|-----------|
| CA-20.01 | 1, 4 | Subconsulta SQL embebida — Room re-emite `Flow` post-sustitución (S1 excluido porque está en `session_exercise`) |
| CA-20.02 | 1, 4 | E1 descartado ya no está en `session_exercise` → subconsulta no lo excluye → aparece en lista |
| CA-20.03 | 1, 4 | Estado acumulado: cada sustitución actualiza `session_exercise` → subconsulta refleja estado real |
| CA-20.04 | 1, 2, 3, 4 | N sustituciones consecutivas — Room invalida `Flow` tras cada `updateExerciseId()` en `session_exercise` |
| CA-20.05 | 1 | El ejercicio siendo sustituido ya está en `session_exercise` → subconsulta lo excluye naturalmente |
| CA-20.06 | 1 | `WHERE e.module_code = :moduleCode` sin restricción de `plan_assignment` → todos los ejercicios del módulo |
| CA-20.07 | 1 | Verificación Módulo A: 14 ejercicios totales − N asignados en sesión = candidatos exactos |
| CA-20.08 | 1 | Verificación Módulo B: 15 ejercicios totales − N asignados en sesión = candidatos exactos |
| CA-20.09 | 1 | Verificación Módulo C: 14 ejercicios totales − N asignados en sesión = candidatos exactos |
| CA-20.10 | 1 | Ejercicios custom tienen `module_code` — incluidos por la misma query sin distinción |
| CA-20.11 | 1, 4 | Combinación: E1 reincorporado + S1 excluido + ejercicio actual excluido naturalmente |
| CA-20.12 | — | Cumplido implícitamente: `substituteExercise()`, `updateExerciseId()` y validación de estado "No Iniciado" no se modifican |
| CA-20.13 | 1 | El filtro usa `session_exercise` (estado vivo de la sesión), **no** `plan_assignment` |

### File List (Resultado de Implementación)

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| M | `data/local/dao/ExerciseDao.kt` | Nuevo `getEligibleSubstitutesForSession()`, eliminado `getByModuleCodeNotInIds()` |
| M | `domain/repository/ExerciseRepository.kt` | Firma `getEligibleSubstitutes()` cambiada a `(moduleCode, sessionId: Long)` |
| M | `data/repository/ExerciseRepositoryImpl.kt` | Delegación al nuevo método DAO |
| M | `ui/session/SubstituteExerciseViewModel.kt` | Eliminado snapshot estático `excludedIds` en `init` |
| M | `data/local/dao/SessionExerciseDao.kt` | Eliminado `getExerciseIdsForSession()` |
| M | `domain/repository/SessionRepository.kt` | Eliminada declaración `getExerciseIdsForSession()` |
| M | `data/repository/SessionRepositoryImpl.kt` | Eliminada implementación `getExerciseIdsForSession()` (líneas ~293-295) |
| C | `test/.../ui/session/SubstituteExerciseViewModelTest.kt` | 7 tests unitarios para `SubstituteExerciseViewModel` |

**Resultado:** BUILD SUCCESSFUL — 318 tests pasan (0 fallos), 0 errores de compilación.
