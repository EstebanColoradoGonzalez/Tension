## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-12

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
HU-08 es una historia de naturaleza transversal (cross-cutting), no una funcionalidad independiente. Define 8 Criterios de Aceptación que se dividen en dos categorías:

1. **CAs de registro (CA-08.01, CA-08.04, CA-08.05, CA-08.08):** Ya completamente implementados en HU-06 como parte del formulario E2.
2. **CAs de progresión (CA-08.02, CA-08.03, CA-08.06, CA-08.07):** Diferidos a HU-10/HU-11 junto con el motor de progresión.

**Nivel de complejidad:**
BAJA — No requiere trabajo de implementación autónomo. Los CAs de registro ya están implementados en HU-06. Los CAs de progresión se implementarán cuando se aborden HU-10 y HU-11.

**Riesgos técnicos conocidos:**
1. Los CAs de progresión deben implementarse junto con el motor de progresión (HU-10/HU-11) para tener contexto de ejecución.
2. El campo `exercise_set.reps` es dual: almacena repeticiones o segundos según `exercise.is_isometric`. La interpretación debe ser consistente en todas las capas.
3. El orden de evaluación `isIsometric` antes de `isBodyweight` es crítico porque los isométricos son subconjunto de bodyweight (`is_isometric = 1 → is_bodyweight = 1`).

**Patrones y convenciones del equipo:**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Screen`, `{Feature}ViewModel`, `{Acción}{Entidad}UseCase` (§5.2)
- Estructura Composable: hiltViewModel() + collectAsStateWithLifecycle() + LaunchedEffect para eventos (§5.3)
- Sealed classes para UiState y Events
- `operator fun invoke()` en Use Cases

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
HU-06 (CA-06.01 a CA-06.09) — Implementación completa del formulario E2 con variantes estándar, peso corporal e isométrico.

**Patrones de código reutilizados:**
- RegisterSetViewModel: manejo de estado dual (repeticiones/segundos)
- RegisterSetScreen: 3 variantes de UI según tipo de ejercicio
- RegisterSetUseCase: validaciones de dominio centralizadas

**Mejores prácticas aplicadas:**
- Pre-implementación de CAs de registro en HU-06 para evitar duplicación y complejidad de merge
- Validaciones centralizadas en Use Cases (no en ViewModels ni UI)
- Campo `reps` dual para evitar columnas separadas
- Orden de evaluación `isIsometric` antes de `isBodyweight`

---

### Tareas de Implementación

#### CAs de Registro — Ya implementados en HU-06

No hay tareas pendientes. Verificación de implementación:

| Archivo | Líneas | CAs Cubiertos |
|---------|--------|---------------|
| `RegisterSetViewModel.kt` | 146 | CA-08.01, CA-08.04, CA-08.05, CA-08.08 |
| `RegisterSetScreen.kt` | 307 | CA-08.01, CA-08.04, CA-08.05, CA-08.08 |
| `RegisterSetUiState.kt` | 27 | CA-08.01, CA-08.04 |
| `RegisterSetUseCase.kt` | ~20 | CA-08.05, CA-08.08 |
| `SessionRepositoryImpl.kt` | 268 | CA-08.01 |
| `ExerciseProgressionEntity.kt` | 33 | CA-08.07 (schema preparado) |
| `strings.xml` | ~100 | CA-08.01, CA-08.04, CA-08.05 |

#### CAs de Progresión — Diferidos a HU-10/HU-11

**CA-08.02 — Progresión bodyweight por repeticiones totales (HU-10):**
- [ ] Detectar ejercicios bodyweight (flag `isBodyweight`)
- [ ] Calcular `SUM(reps)` de las 4 series del ejercicio
- [ ] Comparar contra la sesión anterior
- [ ] Implementar Regla 6 del MDS §6-A

**CA-08.03 — Exclusión de bodyweight del Doble Umbral (HU-11):**
- [ ] Implementar exclusión: `if (!exercise.isBodyweight) applyDoubleThreshold()`
- [ ] `prescribedLoadKg` permanece null para bodyweight
- [ ] Implementar Regla 1 del MDS, exclusión bodyweight

**CA-08.06 — Progresión isométrica por tiempo sostenido (HU-10):**
- [ ] Leer segundos desde `exercise_set.reps` (interpretación isométrica)
- [ ] Comparar contra rango prescrito 30-45s
- [ ] Comparar contra sesión anterior
- [ ] Implementar Regla 7 del MDS §6-A

**CA-08.07 — Marcado de isométrico como "dominado" (HU-10):**
- [ ] Verificar 4 de 4 series ≥ 45 segundos
- [ ] Transición `status → MASTERED` en `ExerciseProgressionEntity`
- [ ] Presentar badge 🏆 en resumen post-sesión
- [ ] Implementar Regla 7 del MDS

---

### Infraestructura Requerida para Progresión

| Componente | Estado | Responsable |
|------------|--------|-------------|
| `ExerciseEntity.isBodyweight` | ✅ Existente | HU-06 |
| `ExerciseEntity.isIsometric` | ✅ Existente | HU-06 |
| `exercise_set.reps` dual (reps/segundos) | ✅ Existente | HU-06 |
| `ExerciseProgressionEntity.status` con MASTERED | ✅ Existente | HU-06 |
| `ExerciseProgressionEntity.prescribedLoadKg` nullable | ✅ Existente | HU-06 |
| Motor de clasificación de progresión | ⏳ Pendiente | HU-10 |
| Motor de Doble Umbral | ⏳ Pendiente | HU-11 |
