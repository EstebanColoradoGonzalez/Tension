## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Decisión técnica | Los seeders construían `ContentValues` inline, clase del framework Android sin implementación en JVM. Con `testOptions.unitTests.isReturnDefaultValues = true` y sin Robolectric, los datos semilla eran imposibles de verificar por tests. | Extracción de los datos a estructuras Kotlin puras (`ExerciseCatalog`, `DefaultPlan` + `model/`). Los seeders quedan como mapeadores a `ContentValues`. Aprobado por el usuario en el refinamiento. |
| 2 | Entorno | `./gradlew` abortaba con "Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7" — `JAVA_HOME` apuntaba a `C:\apps\java\jdk1.7.0_79`. | Ejecución con `JAVA_HOME` apuntando al JDK 17 de Temurin ya instalado. Sin cambios en el repositorio. |
| 3 | Aclaración | Auditoría de catalogación muscular (CA-29.08): detectada una candidata discutible más allá de *Remo al Mentón* — *Curl Martillo* y *Curl de Martillo Cruzado* reclutan braquiorradial y solo figuran en Bíceps. | El usuario aprobó el plan sin incluirla. Queda registrada en `cambios.md` como detectada y no aplicada. |

### Completion Notes

- ⚡ **Dev-Rápido:** delta sobre el seed de HU-27 para alinear el dato base con el plan definitivo de El Ejecutante. Los datos semilla se extrajeron a estructuras Kotlin puras (`ExerciseCatalog`, `DefaultPlan`) y los seeders quedaron reducidos a mapeadores de `ContentValues`, lo que hizo verificables por tests JVM los criterios CA-29.01 a CA-29.08.
- **Catálogo:** 33 → **37 ejercicios**. Cuatro altas (Press Militar, Dominadas —único `is_bodyweight` del seed—, Remo Unilateral en Polea Baja y Alta) con su equipamiento, zona muscular y recurso visual. *Tirón de Dorsales* renombrado a **Jalón al Pecho** conservando id 25, equipamiento, zona, imagen e historial. Ningún ejercicio eliminado.
- **Catalogación muscular:** auditadas las 37 asignaciones ejercicio-zona bajo criterio biomecánico. Única corrección: *Remo al Mentón* de Hombro + Trapecio a **Espalda Alta**. Relaciones: 38 → **41**.
- **Plan por defecto:** 6 rutinas reescritas, 31 → **35 asignaciones**, **4 slots duales** (Lunes, Martes, Miércoles, Viernes). Salen *Remo al Mentón*, *Zancadas* y la *Extensión de Cuádriceps* del sábado; entra *Hip Thrust*. Los tres permanecen en el Diccionario como alternativas elegibles.
- **Sin cambio de esquema:** la base sigue en versión 13 y `Migrations.kt` quedó intacto. El delta aplica solo a instalación fresca, conforme a la excepción beta documentada en la historia.
- **Tests:** 42 tests nuevos en 3 suites. Suite completa **420/420 en verde**; `assembleDebug` y `lintDebug` sin errores.
- **Pendiente de validación manual:** instalación fresca (desinstalar o borrar datos, ya que `DatabaseModule` no declara `fallbackToDestructiveMigration`) para verificar CA-29.06 y CA-29.09 en dispositivo.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `Tension/app/src/main/java/.../data/local/seed/model/SeedExercise.kt` | Data class pura del ejercicio semilla, con flags y zonas musculares |
| Creado | `Tension/app/src/main/java/.../data/local/seed/model/SeedRoutine.kt` | Data class pura de la rutina del plan predeterminado |
| Creado | `Tension/app/src/main/java/.../data/local/seed/model/SeedAssignment.kt` | Data class pura de la asignación ejercicio-versión de rutina |
| Creado | `Tension/app/src/main/java/.../data/local/seed/ExerciseCatalog.kt` | Los 37 ejercicios del catálogo base con sus 41 relaciones ejercicio-zona |
| Creado | `Tension/app/src/main/java/.../data/local/seed/DefaultPlan.kt` | 6 rutinas y 35 asignaciones del plan predeterminado, con 4 slots duales |
| Modificado | `Tension/app/src/main/java/.../data/local/seed/ExerciseSeeder.kt` | Reducido a mapeador: itera `ExerciseCatalog` hacia `exercise` y `exercise_muscle_zone` |
| Modificado | `Tension/app/src/main/java/.../data/local/seed/PlanSeeder.kt` | Reducido a mapeador: itera `DefaultPlan` hacia `routine`, `routine_version`, `routine_current_version` y `plan_assignment` |
| Creado | `Tension/app/src/test/java/.../data/local/seed/ExerciseCatalogTest.kt` | 18 tests — conteos, unicidad, renombrado, altas, bodyweight, recatalogación, preservación del diccionario |
| Creado | `Tension/app/src/test/java/.../data/local/seed/DefaultPlanTest.kt` | 20 tests — composición exacta de las 6 rutinas, slots duales, integridad referencial |
| Creado | `Tension/app/src/test/java/.../data/local/seed/SeedAssetsTest.kt` | 4 tests — correspondencia 1:1 entre `media_resource` y los 37 PNG de `assets/exercises/` |
| Modificado | `docs/architecture/domain_and_state_model.md` | §6.1: `exercise` 33 → 37, `exercise_muscle_zone` 38 → 41, composición del plan predeterminado |
| Modificado | `docs/architecture/architecture_blueprint.md` | Conteos del seed y módulo Seed Data con `ExerciseCatalog` / `DefaultPlan` |
| Creado | `docs/domain/stories/HU-29-plan-catalogo-actualizados/refinamiento.md` | Plan técnico aprobado — 14 tareas en 5 fases |
| Modificado | `docs/domain/stories/HU-29-plan-catalogo-actualizados/cambios.md` | Auditoría de catalogación y bitácora de desarrollo |
| Modificado | `docs/domain/stories/HU-29-plan-catalogo-actualizados/index.md` | Fases Refinamiento Técnico y Desarrollo → ✅ Completada |
| Creado | `docs/domain/stories/HU-29-plan-catalogo-actualizados/dev-record.md` | Este registro |

*Los 4 recursos PNG en `Tension/app/src/main/assets/exercises/` fueron aportados por el PO antes del desarrollo (precondición cumplida el 2026-08-30); esta sesión no los modificó.*

### Métricas Dev-Rápido

- Tiempo sesión IA: 19 min
- Tareas manuales DoD: 0 min
- Tiempo total: 19 min
