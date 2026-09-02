## Dev Agent Record — Dev-Rápido

**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Alcance | `session_exercise.original_exercise_id` tiene un único escritor de valor no nulo (`updateExerciseId` ← `substituteExercise`) y HU-26 la anula de forma explícita. Al retirar la sustitución la columna queda inescribible y los 30 `COALESCE(original_exercise_id, exercise_id)` del sistema se vuelven idénticos a `exercise_id` | Se consultó con el PO antes de escribir el plan. Decisión: **retiro total, esquema incluido** — migración v15 → v16 que recrea la tabla, más la simplificación de los 30 sitios. Documentado como D2 |
| 2 | Regresión | `BackupRepositoryImpl.importFromJson` construye el `ContentValues` con **todas** las claves del JSON. Un backup exportado antes de esta historia contiene `original_exercise_id`; tras eliminar la columna, `db.insert` habría fallado con *"table session_exercise has no column named original_exercise_id"* y **ningún backup previo se habría podido restaurar**. Ninguna CA de la historia cubre este caso | Se descartó bumpear `SCHEMA_VERSION` (habría movido `LEGACY_SCHEMA_VERSION` a 9 y perdido la compatibilidad con backups v8). El importador consulta `PRAGMA table_info` y descarta las claves que la tabla ya no declara. Documentado como D8 y cubierto con tres casos JVM |
| 3 | Criterio | CA-34.05 pide incorporar HU-27 y las nueve historias de EPIC-08 al índice, pero todas **ya figuraban** en `story_mapping_index.md` §2. Lo ausente era su fila en §10 (Cobertura de Requisitos por Historia), y sus estados en §2 estaban obsoletos | Se ejecutó la intención completa del criterio en lugar de su letra: filas añadidas a §10, estados actualizados a su fase real, y §4.6 cerrada. La diferencia entre el índice que el criterio describe y el que existe queda transcrita en D10 para que no se lea como omisión |
| 4 | Entorno | `./gradlew` aborta con *"Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7"* — el `JAVA_HOME` del sistema apunta a `C:\apps\java\jdk1.7.0_79` | Se ejecutó el build exportando `JAVA_HOME=C:/apps/java/JDK_17.0.5`, igual que en HU-32 y HU-33. No se modificó ninguna configuración del proyecto |
| 5 | Compilación | `import io.mockk.anyConstructed` → *"Unresolved reference"*. En mockk 1.13.13 `anyConstructed<T>()` es miembro de `MockKMatcherScope`, no una función de nivel superior | Se retiró el import; la llamada dentro de `every { }` resuelve por receptor |

### Completion Notes

- ⚡ **Dev-Rápido:** el sistema pasa a tener **un único mecanismo de cambio de ejercicio durante la sesión**. La sustitución por grupo muscular se retira entera —pantalla, ViewModel, caso de uso, modelo, ruta, punto de entrada, consultas exclusivas y columna de esquema— y las alternativas por slot de HU-26 quedan como única vía.
- **El retiro llegó al esquema (D2).** `original_exercise_id` no sobrevive como columna latente: `MIGRATION_15_16` recrea `session_exercise` sin ella (SQLite 3.19 de `minSdk 26` no tiene `DROP COLUMN`, y la columna sostenía una FK y un índice). Con la columna fuera, Room verifica en compilación cada `@Query` contra la tabla real: **un `COALESCE` olvidado rompe el build**, lo que convirtió al compilador en la red de seguridad de las 30 simplificaciones — 23 en `SessionExerciseDao`, 5 en `ExerciseSetDao`, 1 en `PlanAssignmentDao` y 1 en `BackupRepositoryImpl`.
- **La simplificación es semánticamente neutra (D4).** `COALESCE(original_exercise_id, exercise_id)` distinguía plan de ejecutado solo cuando había sustitución; el intercambio de alternativa ya anulaba la columna. Con la sustitución fuera, ambas expresiones coinciden para toda fila existente y futura. La prescripción de carga, la clasificación de progresión, las métricas por grupo muscular y las alertas de tonelaje calculan igual que antes.
- **La regresión de backup era el hallazgo con más consecuencia y no la cubría ninguna CA (D8).** Sin el filtro por `PRAGMA table_info`, esta historia habría dejado inservible todo backup exportado hasta hoy. El filtro es genérico: cualquier columna que se retire en el futuro deja de romper la restauración. `SCHEMA_VERSION` permanece en 9 — el formato del archivo no cambia.
- **`getMuscleZoneIdsByExerciseId` fue la única consulta por zona muscular eliminada** (D6): sus tres llamadores eran los tres métodos de sustitución. Los `JOIN exercise_muscle_zone` de volumen por grupo (HU-15) y tonelaje (HU-33), y `getPrimaryMuscleGroupByExercise`, quedaron intactos — CA-34.07.
- **La tarjeta de ejercicio no iniciado consolidó sus controles en una sola fila** (D7). La segunda fila existía porque el botón "Sustituir" no dejaba ancho; retirado, `[Registrar] + [⇄ si hay alternativa] + [📷]` caben juntos. No se movió la imagen, ni la tipografía, ni los 48 dp de área táctil: el hueco del control desaparece sin rediseñar la tarjeta.
- **El indicador "Sustituyó a:" se retiró en los cinco niveles** (D5): `strings.xml`, `SessionDetailScreen`, `SessionDetailExercise`, `SessionDetailExerciseDto` y el `LEFT JOIN exercise oe` de la consulta de detalle. Un campo que nadie puede poblar es el mismo residuo que la columna.
- **Ninguna migración histórica se editó.** `MIGRATION_8_9`, `MIGRATION_9_10` y `MIGRATION_11_12` siguen nombrando `original_exercise_id`: describen el esquema de su época y una base antigua debe poder recorrer la cadena completa hasta v16.
- **Tests: la historia retira, y el plan de tests lo refleja.** Se eliminaron las dos suites propias (10 casos) y el caso que verificaba el indicador de sustitución; se ajustaron dos fixtures. Lo único con lógica nueva —el filtro de claves de importación— recibió 3 casos propios, incluido el de regresión de HU-19.
- **Sin test instrumentado de migración.** `MigrationV6ToV7Test` y `MigrationV7ToV8Test` no usan `MigrationTestHelper`: abren una base en memoria y verifican la semilla, no el camino de migración. Replicar ese patrón para v15 → v16 habría dado una prueba que no ejerce `MIGRATION_15_16`. La verificación de la migración queda como escenarios manuales 6 y 7 de `refinamiento.md`.
- **Observación levantada, no corregida:** el índice `UNIQUE(session_id, exercise_id)` sobrevive a la migración y era lo que impedía sustituir un ejercicio por otro ya presente en la sesión (`substituteExercise` lo duplicaba en Kotlin). Sigue protegiendo al intercambio de alternativas de HU-26, que puede colisionar igual, pero **ninguna CA de esta ni de aquella historia lo cubre con test**. Su dueño natural es la historia que revise las alternativas por slot.
- **Cifra no recalculada:** el blueprint declaraba "58 conexiones de navegación" en el contexto de ADR-13. No se pudo derivar la convención de conteo desde los documentos, de modo que **se retiró la cifra** en lugar de sustituirla por una estimación. Las vistas (27 → 26) y las rutas tipadas (25 → 24) sí se actualizaron: su cálculo es directo.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Eliminado | `ui/session/SubstituteExerciseScreen.kt` | Pantalla E3 de selección de sustituto |
| Eliminado | `ui/session/SubstituteExerciseViewModel.kt` | ViewModel, `UiState` y `UiItem` de la pantalla E3 |
| Eliminado | `domain/usecase/session/SubstituteExerciseUseCase.kt` | Caso de uso de sustitución |
| Eliminado | `domain/model/SubstituteExerciseInfo.kt` | Modelo de dominio de la sustitución |
| Eliminado | `test/…/domain/usecase/session/SubstituteExerciseUseCaseTest.kt` | 3 casos obsoletos |
| Eliminado | `test/…/ui/session/SubstituteExerciseViewModelTest.kt` | 7 casos obsoletos |
| Modificado | `ui/session/ActiveSessionScreen.kt` | Botón "Sustituir" retirado; controles consolidados en una fila (D7); `onNavigateToSubstitute`/`onSubstitute` fuera de las cuatro firmas |
| Modificado | `ui/history/SessionDetailScreen.kt` | Indicador "Sustituyó a:" retirado |
| Modificado | `ui/navigation/NavigationRoutes.kt` | `SUBSTITUTE_EXERCISE` y `substituteExerciseRoute` eliminados |
| Modificado | `ui/navigation/TensionNavHost.kt` | `composable` de E3, import, cableado de entrada y regla de bottom bar eliminados |
| Modificado | `res/values/strings.xml` | 10 strings de la funcionalidad retirada |
| Modificado | `domain/repository/SessionRepository.kt` | `getSubstituteExerciseInfo` y `substituteExercise` fuera del contrato |
| Modificado | `domain/repository/ExerciseRepository.kt` | `getEligibleSubstitutes` fuera del contrato |
| Modificado | `domain/model/SessionDetailExercise.kt` | Campo `originalExerciseName` retirado |
| Modificado | `data/repository/SessionRepositoryImpl.kt` | Ambos métodos de sustitución eliminados; `progressionExerciseId` resuelto sobre el ejercicio ejecutado en sus 3 sitios (D4); comentario de precedencia reescrito |
| Modificado | `data/repository/ExerciseRepositoryImpl.kt` | `getEligibleSubstitutes` y su filtrado por intersección de zonas eliminados |
| Modificado | `data/repository/BackupRepositoryImpl.kt` | `columnsOf` + filtro de claves ajenas en la importación (D8); `COALESCE` de la reparación de `slot` simplificado |
| Modificado | `data/local/dao/SessionExerciseDao.kt` | DTO y consulta de sustitución, `updateExerciseId` y el `LEFT JOIN` de detalle eliminados; `switchAlternativeExercise` deja de anular la columna; 23 `COALESCE` simplificados |
| Modificado | `data/local/dao/ExerciseDao.kt` | `getEligibleSubstitutesForSession` y `getMuscleZoneIdsByExerciseId` eliminadas (D6) |
| Modificado | `data/local/dao/ExerciseSetDao.kt` | 5 `COALESCE` simplificados; los `JOIN exercise_muscle_zone` conservados |
| Modificado | `data/local/dao/PlanAssignmentDao.kt` | `OR se.original_exercise_id = pa.exercise_id` retirado de `hasSlotAlternative` |
| Modificado | `data/local/entity/SessionExerciseEntity.kt` | Campo, `ForeignKey` e `Index` de `original_exercise_id` retirados |
| Modificado | `data/local/database/Migrations.kt` | `MIGRATION_15_16` — recreación de `session_exercise` sin la columna (D3) |
| Modificado | `data/local/database/TensionDatabase.kt` | `version = 16` |
| Modificado | `di/DatabaseModule.kt` | `MIGRATION_15_16` registrada en la cadena |
| Creado | `app/schemas/…/16.json` | Esquema v16 generado por KSP; sin la columna, su FK ni su índice |
| Modificado | `test/…/data/repository/BackupRepositoryImplTest.kt` | 3 casos del filtro de columnas, incluido el de regresión de HU-19 |
| Modificado | `test/…/domain/usecase/history/GetSessionDetailUseCaseTest.kt` | Fixtures sin `originalExerciseName`; caso del indicador de sustitución eliminado |
| Modificado | `test/…/ui/history/SessionDetailViewModelTest.kt` | Fixture actualizada al modelo nuevo |
| Modificado | `docs/architecture/architecture_blueprint.md` | Trazabilidad de `RF-16` reasignada a HU-26; `D-02` reescrita sobre el mecanismo vigente; conteos de vistas y rutas |
| Modificado | `docs/architecture/interfaces_contract.md` | `E3-T1` y los dos `ERR_SUBSTITUTION_*` eliminados; `F2-T1` sin `was_substituted`/`original_exercise_name`; `F3-T1` sin la mención al sustituto; precedencia de carga reescrita (D4) |
| Modificado | `docs/architecture/domain_and_state_model.md` | Columna, sus dos reglas de integridad y su relación `1:N` retiradas; esquema documentado en v16 |
| Modificado | `docs/domain/stories/story_mapping_index.md` | HU-18 → `Done`, HU-20 → `Cancelada`, HU-07 → `Retirada`, RF16 → HU-26; §10 completada con HU-27 y HU-28 a HU-36; §4.6, §8.1 y §9 actualizadas (D10) |
| Creado | `docs/domain/stories/HU-34-…/refinamiento.md` | Plan técnico: 40 tareas en 9 fases, 10 decisiones |
| Creado | `docs/domain/stories/HU-34-…/dev-record.md` | Este registro |
| Modificado | `docs/domain/stories/HU-34-…/index.md` | Fases de Refinamiento y Desarrollo |
| Modificado | `docs/domain/stories/HU-34-…/cambios.md` | Entradas de refinamiento y desarrollo |

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew testDebugUnitTest` | **538 tests · 0 fallos · 0 errores · 0 omitidos** |
| `./gradlew build` (debug + release + lint + tests) | **BUILD SUCCESSFUL** — release también 538/538 |
| Android Lint | 0 errores · 92 warnings, todos preexistentes. Ningún recurso quedó huérfano tras retirar los 10 strings |
| Versión de esquema | v15 → **v16**. `16.json` generado; `session_exercise` sin `original_exercise_id`, sin su FK y sin su índice |
| Barrido de residuos | Sin referencias a la sustitución fuera de `Migrations.kt` (17, históricas y deliberadas), dos comentarios explicativos y las fixtures del test de regresión de backup |

Balance de la suite: **−10** por las dos suites retiradas, **−1** por el caso del indicador de sustitución, **+3** por el filtro de columnas de la importación.

La validación manual de los 11 escenarios —incluida la migración v15 → v16 sobre una base con datos reales y la restauración de un backup previo a v16— queda registrada en `refinamiento.md` § *Validación manual (no automatizable)* y **no se ejecutó en esta sesión**.

### Métricas Dev-Rápido

- Tiempo sesión IA: 35 min
- Tareas manuales DoD: 0 min
- Tiempo total: 35 min
