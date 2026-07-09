# Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-03-04

---

## Consideraciones Generales

**Nivel de complejidad:** MEDIA — Cambios concentrados en capa Data (1 migración DDL+Data con 82 INSERT SQL, 4 queries ORDER BY con subconsulta COALESCE, 1 reescritura completa de seeder) con propagación menor a UI (1 indicador visual en 2 pantallas). Sin cambios en Domain Layer. Complejidad principal en la correctitud de los 82 INSERT SQL (IDs de ejercicios, sort_order, reps) y en las subconsultas ORDER BY con fallback para ejercicios sustituidos e históricos.

**Patrones y convenciones del equipo:**
- Migración Room DDL+Data: `db.execSQL()` con SQL raw (patrón `MIGRATION_6_7` en `Migrations.kt`)
- Seeder con ContentValues: `db.insert("tabla", CONFLICT_REPLACE, values)` (patrón `PlanSeeder.pa()`)
- DAO subconsulta correlacionada con COALESCE (HU-20: `getEligibleSubstitutesForSession`)
- UI badge via Surface + RoundedCornerShape (patrón badge `isCustom` en `PlanVersionDetailScreen`)
- Entity con default Kotlin para backward compatibility: `val field: Type = default`
- ColumnInfo con snake_case: `@ColumnInfo(name = "snake_case")`

**Dependencias nuevas a instalar:** Ninguna.

**Estrategia de testing:** JUnit 4 + AndroidJUnit4 (Room in-memory DB). Test instrumentado de migración con 7 tests validando las 82 asignaciones (conteos, sort_order secuencial, integridad referencial, tablas históricas intactas).

---

## Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
- HU-16: Migración de estructura de módulos — patrón de migración DDL+Data establecido en `MIGRATION_6_7`.
- HU-20: Sustitución de ejercicios — patrón de subconsulta correlacionada con COALESCE en DAOs.

**Patrones de código reutilizados:**
- Patrón `MIGRATION_N_N+1` con `ALTER TABLE` + `DELETE` + `INSERT` en `Migrations.kt`
- Patrón helper `pa()` en seeder con `ContentValues` para INSERTs masivos
- Patrón badge visual en composables (patrón `isCustom` en `PlanVersionDetailScreen`)
- Patrón entity con default Kotlin para backward compatibility

**Mejores prácticas aplicadas:**
- `sort_order` solo en `plan_assignment` (no duplicar en `session_exercise`)
- COALESCE con fallback 9999 para ejercicios sin plan_assignment
- Sesiones existentes preservadas íntegramente (no modificar `session`, `session_exercise`, `exercise_set`)
- 1-based sort_order consistente con seed data
