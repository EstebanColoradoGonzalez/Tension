## Dev Agent Record — Dev-Rápido

**Autor**: Esteban Colorado González | **Fecha**: 2026-09-11

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Defecto latente que la granularidad destapa | **Seis consultas elegían «el» grupo muscular del ejercicio con `LIMIT 1` y sin `ORDER BY`** (`ExerciseDao:71, 103, 138`, `PlanAssignmentDao:104`, `SessionExerciseDao:146, 301` y `getPrimaryMuscleGroupByExercise`). Acertaban por casualidad: casi todo ejercicio tenía una zona. Con hasta cuatro, SQLite elegiría una cualquiera y **el grupo al que se atribuye una caída de tonelaje pasaría a depender del plan de ejecución** — de esa consulta cuelga la alerta `TONNAGE_DROP` | Todas ordenan por `emz.is_primary DESC, mz.sort_order ASC`. El nombre `getPrimaryMuscleGroupByExercise` prometía «principal» desde antes de que la jerarquía existiera; ahora es cierto. Es una corrección **aprovechada, no alcance colado**: CA-41.04 exige que `TONNAGE_DROP` siga evaluándose sobre los mismos grupos, lo que presupone que el grupo esté determinado. Registrado como D6 |
| 2 | Error propio, detectado por un test que yo mismo escribí | El test *«toda sugerencia es la primera opción que el ejercicio lista»* falló en `Aperturas`: lista `Mancuerna` primero y el plan sugiere `Máquina`. Revisadas las 30, **11 incumplen la supuesta regla** — `Peso Muerto Rumano` lista `Barra` y sugiere `Mancuerna` | La regla del *valor atómico de la primera opción* de `cambios.md` hablaba de **las tablas que el requerimiento aportó** —donde `Aperturas` decía *Máquina Contractor*— y no del orden de `ExerciseCatalog`. Son cosas distintas. El test se sustituyó por su contrario, `a suggestion need not be the first option the exercise lists`, con el porqué en KDoc, para que nadie vuelva a deducirlo. La autoridad es la tabla de CA-41.07, verificada renglón a renglón |
| 3 | Ambigüedad de CA, resuelta con el preview y marcada al PO | **CA-41.05 dice que la sugerencia preselecciona el selector y que «el último cambio manda»**, sin acotar si «las series siguientes» es dentro de la sesión o para siempre. Con la segunda lectura, un plan que sugiere `Barra` no la preseleccionaría nunca más después del primer día que se usara `Polea` — CA-41.07 quedaría sin efecto observable | Precedencia de tres niveles: último implemento **de este `session_exercise`** → sugerencia del plan → CA-39.04 intacto. Es la lectura literal del preview, que rotula *«Primera serie del ejercicio en la sesión»* sobre la sugerencia. **Se presentó al PO antes de implementar** y se aprobó. Registrado como D5; si se revirtiera, el cambio se localiza en `resolveEquipmentPreselection` y su test |
| 4 | Decisión de esquema sin precedente directo | `EquipmentCatalog` resolvió el orden de presentación **encodándolo en el `id`**. Aquí no se puede: `HU-29` fijó que un renombrado conserva su identificador, así que el `id` encoda la historia del catálogo —17 zonas conservan el suyo y tres quedan retiradas— y no su orden anatómico. Los dos criterios no caben en una columna | `muscle_zone.sort_order`, `NOT NULL` y sin default. Los ids **4, 7 y 19** de las zonas retiradas **quedan libres y no se reutilizan**: reasignarlos haría que cualquier referencia antigua —un comentario, un respaldo, un volcado— apuntara en silencio a otra cosa. Registrado como D3 |
| 5 | Cambio de alcance del PO, a mitad de refinamiento | El cuarto puesto del viernes debía pasar a `Remo al Mentón` según la historia; el PO pidió sustituirlo por un ejercicio nuevo. Tres CAs cambiaban y el catálogo subía de 37 a 38 | Registrado como D13, con los totales recalculados y el `historia.md` y el preview actualizados en T48 — un catálogo que el documento describe mal es la deuda que `HU-29` enseñó a no aceptar. Dos mapeos al catálogo de CA-41.01 quedaron declarados como interpretación mía y no del PO: «Trapecio Medio» → `Trapecio` y «Romboides mayor y menor» → `Romboides`, para no abrir la tabla cerrada de 33 zonas |
| 6 | Defecto propio, detectado al releer | En `ExerciseDetailViewModel.onEquipmentToggled` el `when` no bindeaba el sujeto, así que la rama nueva **volvía a invocar el caso de uso** para leer su resultado — una segunda escritura por cada rechazo | `when (val result = removeExerciseEquipmentUseCase(...))`, con `result.routineNames` por smart cast |
| 7 | Defecto propio, destapado por lint | `SetSuggestedEquipmentUseCase` rechaza con `require`, cuyo mensaje es **inglés de uso interno** por estándar del proyecto. `PlanVersionDetailViewModel` lo volcaba en `_userMessage`, que va a la pantalla | Lo delató `UnusedResources`: la cadena española del rechazo nunca se usaba. El ViewModel la traduce ahora con el implemento y el ejercicio nombrados. Se retiraron además dos cadenas realmente muertas (`exercise_field_muscle_zone`, sustituida por los dos campos, y `plan_suggested_equipment_required`, innecesaria porque el botón queda deshabilitado) |
| 8 | Límite del lenguaje, resuelto sin castear | La ficha del ejercicio pasó a necesitar **siete** flujos y `combine` tipado llega hasta cinco. La primera versión cayó al `combine` de vararg con `Array<Any?>` y un `@Suppress("UNCHECKED_CAST")` por elemento | Se reescribió con dos `combine` anidados y un `DetailSources` privado que agrupa los cinco primeros. Conserva los tipos y la comprobación del compilador justo donde más barata es |
| 9 | Entorno | `./gradlew` aborta con *"Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7"* — el `JAVA_HOME` del sistema apunta a `C:\apps\java\jdk1.7.0_79`. Mismo hallazgo que HU-36 a HU-40 | Se ejecutó el build exportando `JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot`. No se modificó ninguna configuración del proyecto |

### Completion Notes

- ⚡ **Dev-Rápido:** el catálogo pasa de 20 zonas planas a **33 con granularidad anatómica y jerarquía principal/secundaria**, los 37 ejercicios se reclasifican por completo, entra el ejercicio 38 y el plan por defecto gana el delta de orden y sus **35 sugerencias de implemento**. Ninguna regla de decisión cambió de fórmula.
- **El hallazgo que decidió el diseño fue que no había nada que hacer donde más parecía haberlo.** `getTonnageDataBySessionIds` y `getSetDistributionBySessionIds` ya hacen `INNER JOIN exercise_muscle_zone` **sin filtro y sin ponderación**, agregando por `muscle_group`: eso es literalmente CA-41.04. Las zonas finas caben dentro de los 14 grupos, así que el eje no se mueve y **la CA se cumple por no tocar nada**. Lo que la historia aporta ahí es el KDoc que declara la invariante y los tests que la fijan, para que el día que alguien quiera ponderar por jerarquía el intento falle en rojo y no en silencio.
- **La feature análoga fue `HU-39` y trajo tres formas completas.** `EquipmentCatalog` —el catálogo cerrado extraído del seeder a Kotlin puro para que sea verificable en JVM—, la tabla de unión con `RESTRICT` en ambos lados e índice sobre la columna que no encabeza la PK, y el `sealed interface Result` que devuelve el motivo del rechazo en vez de lanzarlo, al que CA-41.08 solo tuvo que añadirle un caso.
- **La PK hace irrepresentable la contradicción de CA-41.09.** *«Una zona no puede ser principal y secundaria del mismo ejercicio»* no se valida: `(exercise_id, muscle_zone_id)` sigue siendo la clave, así que una zona es **una fila** y la fila lleva un solo `is_primary`. Los `require` del dominio existen para dar el mensaje, no para proteger el dato.
- **La sugerencia del plan no lleva FK compuesta a `exercise_equipment`**, aunque la invariante es cierta. `RESTRICT` sobre una tabla que la ficha del ejercicio edita en caliente convertiría un rechazo que el caso de uso explica con palabras en una excepción de SQLite. La validación vive donde se puede explicar, y en las dos direcciones: al fijar la sugerencia y al retirar del ejercicio una opción que el plan usa.
- **El impedimento inverso nombra la rutina.** `RemoveExerciseEquipmentUseCase` devuelve `SuggestedByPlan(routineNames)` y no un booleano: un «no se puede» sin decir dónde obliga al ejecutante a buscar a mano. El orden de comprobación es el del coste de reparación —última opción, series registradas, sugerencia del plan—, porque el motivo que se muestra debe ser el irreparable.
- **El selector de zona es un diálogo y no una lista embebida.** Las 15 casillas de `EquipmentMultiSelector` caben en el formulario; 33 filas con encabezados de grupo no. El diálogo trae buscador y marca las zonas ya elegidas en el otro campo como no seleccionables, **sin esconderlas**: esconderlas dejaría al ejecutante buscando una zona que parece no existir.
- **El error de CA-41.09 vive en el campo de principales**, no al pie del formulario. Es la razón por la que son dos campos y no una lista única con marca: con 33 zonas, la obligatoriedad tiene que leerse donde se incumple.
- **Esquema v21 → v22** con tres columnas `NOT NULL` sin default y **ninguna tabla nueva**. Sin `MIGRATION_21_22` y sin `fallbackToDestructiveMigration`, por ADR-019. `22.json` exportado y versionado. **`LAST_MIGRATED_VERSION` no se movió**: sigue en 19 y `MigrationV16ToV19Test` pasa sin tocarse. Respaldo `SCHEMA_VERSION` 14 → 15, que rechaza el 14 porque la jerarquía y la sugerencia **son decisiones, no cálculos**, y CA-41.10 exige reproducir catálogo y plan sin recalcularlos.
- **Tests: 3 archivos nuevos, 13 ampliados. 865 → 880 (+15), 0 fallos.** Las tres tablas cerradas se transcriben **dos veces de forma independiente** —una en el seed y otra en el test, desde el texto de la historia y no desde el código— porque un id equivocado no lanza nada: produce un catálogo plausible y una métrica silenciosamente falsa. La verificación que más aporta es la cruzada de `DefaultPlanTest`: *toda sugerencia pertenece a `exercise_equipment` de su ejercicio* cruza CA-41.07 con CA-39.06, escritas en historias distintas.
- **El único test que encontró un defecto real encontró uno mío**, no del dato (ver Debug Log #2). Los datos coincidían con CA-41.07 renglón a renglón; lo que no existía era la regla que yo había deducido de `cambios.md`.
- **`CatalogoSembradoTest` gana seis invariantes, no cifras.** Ninguna relación sin `is_primary` válido, ningún ejercicio sin principal, ninguna asignación sin sugerencia, toda sugerencia dentro de `exercise_equipment`, `sort_order` único y la comprobación de que la PK de `exercise_muscle_zone` sigue siendo la que hace irrepresentable la contradicción. **Compila, pero no se ejecutó: no hay emulador en esta sesión.**
- **Lint: 106 warnings, 0 errores — la misma cifra que antes de la historia.** Las tres cadenas que `UnusedResources` marcó se resolvieron de verdad y no silenciándolo: dos eran muertas y se borraron, y la tercera destapó el defecto #7.
- **La validación manual de los 12 escenarios no se ejecutó en esta sesión.** Requiere dispositivo o emulador. Queda registrada en `refinamiento.md` § *Validación manual (no automatizable)*.

### Métricas Dev-Rápido

- Tiempo sesión IA: **92 min** (2026-09-11 15:26 → 16:58)
- Tareas manuales DoD: **0 min**
- Tiempo total: **92 min**

> Los 12 escenarios de validación manual y `CatalogoSembradoTest` quedan **pendientes de ejecución**: requieren emulador o dispositivo, que no hubo en esta sesión. El `androidTest` compila. Están enumerados en `refinamiento.md` § *Validación manual (no automatizable)*.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| **Creado** | `data/local/seed/MuscleZoneCatalog.kt` | Las 33 zonas con su grupo y su orden declarado, en Kotlin puro. Ids nombrados, retirados declarados (T1, D2) |
| **Creado** | `data/local/seed/model/SeedMuscleZone.kt` | Estructura del catálogo de zonas (T1) |
| **Creado** | `domain/model/PreselectionOrigin.kt` | De dónde viene el implemento preseleccionado, para el rótulo de `E2-T1` (T22, D5) |
| **Creado** | `domain/usecase/catalog/SetExerciseMuscleZonesUseCase.kt` | Reemplaza las zonas y su jerarquía; devuelve el motivo en vez de lanzarlo (T26) |
| **Creado** | `domain/usecase/catalog/GetExerciseMuscleZoneIdsUseCase.kt` | Zonas del ejercicio partidas por jerarquía, para la ficha (T26) |
| **Creado** | `domain/usecase/catalog/GetExerciseEquipmentOptionsUseCase.kt` | Implementos admitidos **de ahora**, para acotar el selector de sugerencia (T33) |
| **Creado** | `domain/usecase/plan/SetSuggestedEquipmentUseCase.kt` | Fija la sugerencia validando contra `exercise_equipment` (T28, D4) |
| **Creado** | `ui/catalog/components/MuscleZoneHierarchySelector.kt` | Dos campos con filas quitables, más el resumen de la ficha (T30, D12) |
| **Creado** | `ui/catalog/components/MuscleZonePickerDialog.kt` | Diálogo de 33 zonas agrupadas, con buscador y doble pertenencia bloqueada (T30, D12) |
| **Creado** | `app/src/main/assets/exercises/trapecios_con_apoyo_banco_inclinado_mancuernas.png` | Recurso visual del ejercicio 38. **Aportado por el PO** (T10, D13) |
| **Creado** | `app/schemas/…TensionDatabase/22.json` | Esquema exportado por el build y versionado (T7) |
| **Modificado** | `data/local/entity/MuscleZoneEntity.kt` | Gana `sort_order` (T2, D3) |
| **Modificado** | `data/local/entity/ExerciseMuscleZoneEntity.kt` | Gana `is_primary`; la PK no cambia y es la que impide la contradicción (T3, D1) |
| **Modificado** | `data/local/entity/PlanAssignmentEntity.kt` | Gana `suggested_equipment_type_id` con FK, índice y sin default (T4, D4) |
| **Modificado** | `data/local/database/TensionDatabase.kt` | `version = 22`, sin migración (T6, D11) |
| **Modificado** | `data/local/database/Migrations.kt` | KDoc de `LAST_MIGRATED_VERSION` extendido con v22. **La constante no se movió** (T6, D11) |
| **Modificado** | `data/local/seed/BaseDataSeeder.kt` | Las 20 llamadas inline desaparecen; mapea `MuscleZoneCatalog` (T5, D2) |
| **Modificado** | `data/local/seed/ExerciseCatalog.kt` | 38 ejercicios, 87 relaciones con jerarquía. Equipamiento y dificultad de los 37 intactos (T9, D13) |
| **Modificado** | `data/local/seed/model/SeedExercise.kt` | Principales y secundarias separadas (T8, D9) |
| **Modificado** | `data/local/seed/ExerciseSeeder.kt` | Escribe `is_primary` en dos recorridos (T11) |
| **Modificado** | `data/local/seed/DefaultPlan.kt` | Delta de orden y las 35 sugerencias. `Aductores` primero el miércoles y el sábado (T13) |
| **Modificado** | `data/local/seed/model/SeedAssignment.kt`, `data/local/seed/PlanSeeder.kt` | Transportan y persisten la sugerencia (T12, T14) |
| **Modificado** | `data/local/dao/MuscleZoneDao.kt` | Ordena por `sort_order` y no por nombre (T15, D3) |
| **Modificado** | `data/local/dao/ExerciseDao.kt` | Zonas partidas por jerarquía, ids de equipamiento y `replaceMuscleZones` (T16, T17) |
| **Modificado** | `data/local/dao/PlanAssignmentDao.kt` | Sugerencia en las proyecciones, más las consultas de edición y del impedimento inverso (T17-T19, D8) |
| **Modificado** | `data/local/dao/SessionExerciseDao.kt` | Grupo muscular determinista y KDoc de la invariante de `G3-T1` (T17, T20, D6) |
| **Modificado** | `data/local/dao/ExerciseSetDao.kt` | Último implemento **en la sesión**, más el KDoc de CA-41.04 (T20, T29, D7) |
| **Modificado** | `domain/model/Exercise.kt`, `PlanExercise.kt`, `MuscleZone.kt`, `RegisterSetInfo.kt` | Jerarquía, orden, sugerencia y origen de preselección (T21, T22, D9) |
| **Modificado** | `domain/repository/ExerciseRepository.kt`, `PlanRepository.kt` | Contratos de zonas y de sugerencia (T23) |
| **Modificado** | `data/repository/ExerciseRepositoryImpl.kt`, `PlanRepositoryImpl.kt` | Implementación de ambos (T24) |
| **Modificado** | `data/repository/SessionRepositoryImpl.kt` | `resolveEquipmentPreselection` con la precedencia de tres niveles (T29, D5) |
| **Modificado** | `domain/usecase/catalog/CreateExerciseUseCase.kt` | Exige una principal y prohíbe la doble pertenencia (T25) |
| **Modificado** | `domain/usecase/catalog/RemoveExerciseEquipmentUseCase.kt` | Cuarto motivo: `SuggestedByPlan` con la rutina nombrada (T27, D8) |
| **Modificado** | `domain/usecase/plan/AssignExerciseToVersionUseCase.kt`, `AddAlternativeToSlotUseCase.kt` | Sugerencia obligatoria y validada (T28) |
| **Modificado** | `ui/catalog/CreateExerciseScreen.kt`, `CreateExerciseUiState.kt`, `CreateExerciseViewModel.kt` | Los dos campos; `canSave` exige principal (T31) |
| **Modificado** | `ui/catalog/ExerciseDetailScreen.kt`, `ExerciseDetailUiState.kt`, `ExerciseDetailViewModel.kt` | Jerarquía visible y editable al instante; el nuevo rechazo como error del campo (T32) |
| **Modificado** | `ui/catalog/PlanVersionDetailScreen.kt`, `PlanVersionDetailUiState.kt`, `PlanVersionDetailViewModel.kt` | Sugerencia por asignación —una por alternativa de puesto dual—, selector acotado y obligatoria al asignar (T33) |
| **Modificado** | `ui/session/RegisterSetScreen.kt`, `RegisterSetUiState.kt`, `RegisterSetViewModel.kt` | Rótulo del origen; desaparece al elegir a mano (T34, D5) |
| **Modificado** | `res/values/strings.xml` | Cadenas nuevas; dos muertas retiradas (T35) |
| **Modificado** | `data/repository/BackupRepositoryImpl.kt` | `SCHEMA_VERSION` 15; rechaza el 14 (T36, D10) |
| **Creado** | `test/…/data/local/seed/MuscleZoneCatalogTest.kt` | La tabla de CA-41.01 transcrita desde la historia (T37) |
| **Creado** | `test/…/domain/usecase/catalog/SetExerciseMuscleZonesUseCaseTest.kt` | Las tres validaciones de CA-41.09, y que el rechazo **no escribe** (T42) |
| **Creado** | `test/…/domain/usecase/plan/SetSuggestedEquipmentUseCaseTest.kt` | La sugerencia no admitida, incluida la carrera contra la edición del ejercicio (T43) |
| **Modificado** | `test/…/data/local/seed/ExerciseCatalogTest.kt` | La tabla de CA-41.03 por nombre; 87 relaciones, 52 principales, 35 secundarias (T38) |
| **Modificado** | `test/…/data/local/seed/DefaultPlanTest.kt` | Composiciones, las 35 sugerencias y la verificación cruzada con CA-39.06 (T40) |
| **Modificado** | `test/…/data/local/seed/SeedAssetsTest.kt` | 37 → 38 PNG (T39) |
| **Modificado** | `test/…/domain/rules/TonnageRuleTest.kt`, `VolumeDistributionRuleTest.kt` | La invariante de CA-41.04, fijada (T41) |
| **Modificado** | `test/…/domain/usecase/catalog/RemoveExerciseEquipmentUseCaseTest.kt` | El impedimento inverso y su orden de precedencia (T43) |
| **Modificado** | `test/…/ui/session/RegisterSetViewModelTest.kt` | Los tres orígenes y la desaparición del rótulo (T44) |
| **Modificado** | 8 archivos de test más | Firmas que cambian: zonas partidas, `sortOrder`, sugerencia, origen (T46) |
| **Modificado** | `androidTest/…/CatalogoSembradoTest.kt` | Seis invariantes del sembrado. `MigrationV16ToV19Test` **no se tocó** (T47) |
| **Modificado** | `HU-41-…/historia.md`, `41.preview.txt` | CA-41.03, CA-41.06 y CA-41.07 con el ejercicio 38 (T48, D13) |
| **Modificado** | `docs/architecture/domain_and_state_model.md` | Esquema 22, las 33 zonas de §6.1, jerarquía, sugerencia y plan por defecto (T49) |
| **Modificado** | `docs/architecture/interfaces_contract.md` | `D2-T1`, `D5-T1`, `D6-T1a` (nuevo), `E2-T1`, `G2-T1`, `G3-T1`, `J2-T1` y cinco errores nuevos (T50) |
| **Modificado** | `docs/architecture/architecture_blueprint.md` | Esquema 22, ADR-019 con v22, `MuscleZoneCatalog` y su nota de orden (T51) |
| **Modificado** | `docs/domain/stories/story_mapping_index.md`, `HU-41-…/index.md`, `cambios.md` | Estado, fases, métricas y el cambio de alcance (T52) |
