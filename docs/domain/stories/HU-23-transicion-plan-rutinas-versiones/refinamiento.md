# Refinamiento Técnico — HU-23

<!-- ============================================================================ -->
<!-- ETAPA: Refinamiento Técnico                                                  -->
<!-- RESPONSABLE: Developer                                                       -->
<!-- FECHA: 2026-05-06                                                            -->
<!-- ESTADO: Refinado (Developer)                                                 -->
<!-- ============================================================================ -->

## Consideraciones Generales

**Nivel de complejidad:** MUY ALTA — Transformación completa del modelo de dominio. Migración Room v8→v9 con 7 tablas recreadas, 4 tablas nuevas, 2 eliminadas. 12 entidades Room afectadas (4 nuevas, 6 modificadas, 2 eliminadas), 9 DAOs (4 nuevos, 5 modificados, 2 eliminados), 6 Repositories (1 nuevo, 5 modificados), 14 UseCases (6 nuevos, 8+ modificados), 15+ modelos de dominio, 17+ pantallas UI (incluyendo 3 eliminadas). 37 CAs a cubrir. `SessionRepositoryImpl` (~1040 líneas) sufre rewrite parcial (~300 líneas). `RotationResolver` rewrite completo para N rutinas dinámicas. 59 tests existentes potencialmente afectados.

**Riesgos técnicos conocidos:**
1. **Migración SQL corrompe datos (CRÍTICO):** MIGRATION_8_9 es la más extensa del proyecto. Mitigación: `MigrationTestHelper` + datos representativos v8.
2. **Ventana de queries inconsistentes:** Si la DB migra a v9 pero algún query sigue referenciando `module_version`, crash. Mitigación: todos los DAOs se modifican en el mismo hito que las entidades.
3. **Deload activo durante migración:** Versiones congeladas deben preservarse. Mitigación: Fase 2 de migración SQL copia `frozen_version_module_a/b/c` a `deload_frozen_version`.
4. **`loadIncrementKg` calculado vs en SQL:** Ejercicios multi-zona podrían recibir incremento incorrecto. Mitigación: `LoadIncrementResolver` usa la primera zona muscular. Ningún ejercicio actual cruza tren superior e inferior.
5. **Rotación con N heterogéneo de versiones:** Mitigación: wrap-around independiente por rutina `(current % count) + 1`. Tests para N=1, N=2, N=5 y combinaciones heterogéneas.
6. **Sesión activa IN_PROGRESS durante upgrade APK:** Migración esquemática. `routine_version.id == module_version.id` → sesión sigue funcionando.
7. **Backup v8 importado en app v9:** Mitigación: `ImportBackupUseCase` detecta versión y transforma v8→v9.
8. **Seeders para nuevas instalaciones v9+:** Mitigación: `PrepopulateCallbackV9` con seeders actualizados.

**Dependencias nuevas:**
`room-testing` (`androidx.room:room-testing`) — solo `androidTestImplementation`. Agregar a `libs.versions.toml` y `app/build.gradle.kts`. Requiere `exportSchema = true` + `room.schemaLocation` en KSP args.

**Estrategia de testing:**
JUnit 4 + MockK | Tests unitarios: 100% `LoadIncrementResolver` + `RotationResolver` rewrite, UseCases nuevos de rutinas | Tests modificados: UseCases existentes y ViewModels | Test instrumentado: `MigrationTestHelper` para MIGRATION_8_9 (MANUAL)

---

## Historias Relacionadas Consultadas

- HU-04: `PlanAssignmentDao.getDetailsByModuleVersionId()`, `PlanVersionDetailViewModel` con assign/unassign.
- HU-05: `startSession()` / `StartSessionUseCase`, `SessionEntity.moduleVersionId`.
- HU-07: Sustitución — `getEligibleSubstitutesForSession(moduleCode, sessionId)` filtra por módulo.
- HU-09: `closeSession()` con `RotationResolver.advanceRotation()`, `evaluateProgression()`.
- HU-11: Regla Doble Umbral — `loadIncrementKg` de `module.load_increment_kg`.
- HU-12: `ModuleFatigueRule`, `CorrectiveActionRule` con `ROTATE_VERSION` → ahora por rutina (CA-23.27).
- HU-13: `SessionSummaryInfo.moduleCode`, `alertDao.existsActiveByModule()`.
- HU-14: Deload — `DeloadEntity.frozenVersionModuleA/B/C`, `countDeloadSessions() == 6`.
- HU-15: KPIs — `getRirValuesByModule(moduleCode)`, `getSessionIdsByModuleInRange()`.
- HU-18: KPIs y alertas — `AlertThresholdRule.MUSCLE_GROUPS_BY_MODULE`, evaluación por módulo hardcoded.
- HU-19: Backup — esquema JSON con tablas `module`/`module_version`.
- HU-22: Preview — `SessionPreviewExercise.moduleCode`, `LoadDisplayMapper`.

**Patrones de código reutilizados:**
- Room migration con `CREATE TABLE _new → INSERT INTO _new SELECT FROM old → DROP TABLE old → ALTER TABLE _new RENAME TO` — patrón de `MIGRATION_7_8`.
- `RoutineDao` CRUD sigue patrón de `ModuleDao` + `ModuleVersionDao` existentes.
- `LoadIncrementResolver` como regla pura sigue patrón de `DeloadLoadRule`, `DoubleThresholdRule`.
- Deload frozen versions en tabla dinámica sigue patrón de `exercise_muscle_zone` (PK compuesta).

**Decisiones validadas:**
- `RoutineRepository` separado para cohesión funcional.
- `PrepopulateCallbackV9` para nuevas instalaciones sin romper migraciones históricas.
- Empty state en Home con CTA para crear plan — UX sin wizard, confía en flujo natural.
- Zonas musculares dinámicas en vez de mapa hardcoded `MUSCLE_GROUPS_BY_MODULE` — escalable a N rutinas.
