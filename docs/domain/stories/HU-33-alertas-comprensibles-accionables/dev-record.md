## Dev Agent Record — Dev-Rápido

**Autor**: Esteban Colorado González | **Fecha**: 2026-08-31

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Criterio | CA-33.03 fija el umbral de RIR en `< 1.5` / `> 3.5`, valores de una escala 0–5. El sistema captura RIR en 0–2 (`RegisterSetUseCase:22`, `RegisterSetScreen:425`): `> 3.5` sería inalcanzable y `< 1.5` dispararía en casi toda rutina | Se consultó con el PO antes de escribir el plan. Decisión: conservar `0.5` / `1.8` —los calibrados a la escala real— y aplicar de CA-33.03 la ampliación de ventana a 3 sesiones. Documentado como D3 y transcrito en `interfaces_contract.md` |
| 2 | Alcance | HU-32 dejó levantado que `CorrectiveActionRule` (umbrales absolutos 4 y 6) quedó descalibrada frente a los umbrales efectivos 5/8/10, y señaló a HU-33 como su dueño natural | Se confirmó con el PO su inclusión. Los escalones pasan a desplazamientos relativos al umbral efectivo (D9) |
| 3 | Entorno | `./gradlew` aborta con *"Gradle 8.11.1 requires Java 1.8 or later. You are currently using Java 1.7"* — el `JAVA_HOME` del sistema apunta a `C:\apps\java\jdk1.7.0_79` | Se ejecutó el build exportando `JAVA_HOME=C:/apps/java/JDK_17.0.5`, igual que en HU-32. No se modificó ninguna configuración del proyecto |
| 4 | Modelo | `SessionExerciseForProgression` unía por `se.exercise_id`, de modo que `e.name` habría devuelto el nombre del ejercicio sustituto y no el del ejercicio al que apunta la alerta | El nombre se proyecta con un subselect sobre `COALESCE(se.original_exercise_id, se.exercise_id)`, coherente con el `exerciseId` que la alerta persiste |

Ningún test ajeno al sistema de alertas requirió cambios: HU-10, HU-11, HU-12, HU-14 y HU-15 sin regresión.

### Completion Notes

- ⚡ **Dev-Rápido:** las cinco familias de alerta dejan de hablar en términos del motor. Cambian tres capas superpuestas sobre el mismo mecanismo de emisión —los números contra los que se compara, el idioma del hallazgo y la existencia obligatoria de una acción— sin tocar el ciclo de vida de la alerta.
- **Sin cambio de esquema.** La ponderación consume `exercise.progression_difficulty` (v15, HU-32); la acción sugerida se deriva en lectura; la narración se compone en emisión sobre el `message` que ya existía. No hay migración.
- **`AlertThresholdRule` reescrito** como declaración única de umbral **y** ventana por familia (CA-33.03, D2). La progresión pasa a ponderarse por dificultad —`LOW` 40/20, `MEDIUM` 35/15, `HIGH` 25/10 (CA-33.04)— y `tonnageLevel` devuelve `null` en descarga, cerrando en la regla lo que antes solo sostenían los llamadores (CA-33.08, D8).
- **Ventanas movidas**: progresión 4 → 6 semanas, RIR 2 → 3 sesiones consecutivas, adherencia 1 → 2 semanas para alerta y 2 → 3 para crisis, tonelaje 10/20 → 15/25 %, inactividad 10/14 → 14/21 días.
- **CA-33.09 requirió dos guardas nuevas**: mínimo de 3 sesiones clasificadas en la ventana de progresión y 14 días de historial en adherencia. Las otras tres familias ya cortaban por construcción y se cubrieron con test.
- **`AlertNarrativeRule`** (regla pura) es dueña de todo texto de alerta: titulares que se persisten en emisión y explicaciones que se recomponen en lectura, en un solo sitio para que no diverjan (D5). Vive en `domain/rules/` porque el `message` se persiste —debe existir antes de que haya UI— y porque ni Domain ni Data alcanzan recursos Android.
- **`SuggestedActionRule` + `AlertNarrativeRule.suggestedActionText`** parten la acción sugerida en decisión pura y redacción (D6). CA-33.10 queda concentrada en una entrada explícita, `hasSlotAlternative`: la alternativa del puesto solo se propone cuando el puesto la tiene, y la rama por defecto garantiza que ninguna alerta se quede sin acción.
- **`AlertDetail` colapsa tres campos en uno**: `recommendations` + `showExerciseHistoryLink` + `showDeloadLink` → `suggestedAction: SuggestedAction` con `target` nulable (D7). El modelo hace inexpresable la alerta sin acción y el botón sin destino.
- **UI**: bloque diferenciado "Qué puedes hacer" con acceso directo de 48 dp que reutiliza rutas existentes (historial de ejercicio, plan, descarga). Los títulos de tipo, hasta ahora duplicados literales en `AlertCard` y `AlertDetailScreen`, bajan a los `alert_type_*` de `strings.xml` que llevaban declarados sin uso, y se reescriben al registro del ejecutante.
- **Severidad intacta** (CA-33.06): ni los `when (alert.level)` de color, ni la elevación de crisis, ni `AlertLevelIndicator` se tocaron.
- **Observación levantada, no corregida:** `buildTonnageDropTrigger` y `buildCausalAnalysis` trocean microciclos sobre listas filtradas de forma distinta, lo que puede producir un `isDeload` discrepante entre el dato mostrado y su explicación. Defecto preexistente de HU-18, fuera del alcance de todo CA de esta historia.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `domain/model/SuggestedActionKind.kt` | Once acciones concretas que una alerta puede proponer |
| Creado | `domain/model/SuggestedAction.kt` | Acción + destino nulable; `SuggestedActionTarget` con los tres destinos existentes |
| Creado | `domain/rules/SuggestedActionRule.kt` | Decisión pura de qué acción aplica y si es navegable; sede de CA-33.10 |
| Creado | `domain/rules/AlertNarrativeRule.kt` | Titulares, explicaciones y textos de acción de las siete alertas |
| Creado | `ui/alerts/SuggestedActionCard.kt` | Bloque "Qué puedes hacer" con acceso directo de 48 dp |
| Creado | `ui/alerts/AlertTypeLabel.kt` | Título de alerta desde `strings.xml`, sin duplicación entre pantallas |
| Modificado | `domain/rules/AlertThresholdRule.kt` | Umbral y ventana por familia; progresión ponderada; descarga excluida del tonelaje |
| Modificado | `domain/rules/CorrectiveActionRule.kt` | Escalones relativos al umbral efectivo en lugar de 4 y 6 absolutos |
| Modificado | `domain/model/AlertDetail.kt` | `suggestedAction` sustituye a `recommendations` y a los dos booleanos de enlace |
| Modificado | `domain/model/ClassificationCount.kt` | Campo `progressionDifficulty` |
| Modificado | `data/local/dao/SessionExerciseDao.kt` | Dificultad en las dos proyecciones de clasificación; nombre del ejercicio en la de progresión |
| Modificado | `data/local/dao/PlanAssignmentDao.kt` | `hasSlotAlternative` — entrada de CA-33.10 |
| Modificado | `data/repository/SessionRepositoryImpl.kt` | Ventanas, ponderación, guardas de datos insuficientes y titulares en las siete emisiones |
| Modificado | `data/repository/AlertRepositoryImpl.kt` | `buildSuggestedAction`, explicaciones delegadas a la narración, ventana de lectura alineada |
| Modificado | `ui/alerts/AlertDetailScreen.kt` | Bloque de acción sugerida en lugar de viñetas y enlaces sueltos; destino de plan |
| Modificado | `ui/alerts/AlertCard.kt` | Título desde recursos |
| Modificado | `ui/navigation/TensionNavHost.kt` | Cableado de `onNavigateToTrainingPlan` en el detalle de alerta |
| Modificado | `ui/session/SessionSummaryScreen.kt` | Señales de acción de HU-13 al mismo registro de redacción |
| Modificado | `res/values/strings.xml` | Títulos de tipo reescritos; textos del bloque de acción y de sus tres accesos directos |
| Modificado | `test/…/domain/rules/AlertThresholdRuleTest.kt` | 27 casos: ponderación por dificultad, ventanas y las cinco familias |
| Creado | `test/…/domain/rules/SuggestedActionRuleTest.kt` | 14 casos, incluida la ausencia de alternativa en el puesto |
| Creado | `test/…/domain/rules/AlertNarrativeRuleTest.kt` | 9 casos: sin identificadores, sin códigos, sin nombres de regla, con elemento y cifra |
| Modificado | `test/…/domain/rules/CorrectiveActionRuleTest.kt` | 9 casos con umbral explícito 3, 5 y 10 |
| Modificado | `test/…/domain/usecase/alerts/GetAlertDetailUseCaseTest.kt` | Fixture actualizada al modelo nuevo |
| Modificado | `test/…/ui/alerts/AlertDetailViewModelTest.kt` | Fixture actualizada al modelo nuevo |
| Modificado | `docs/architecture/interfaces_contract.md` | `H1-T1` y `H2-T1` con la acción sugerida; tabla de umbrales, ventanas y justificación; nota de escala del RIR |
| Modificado | `docs/architecture/domain_and_state_model.md` | `alert.message` documentado como frase en lenguaje natural |
| Creado | `docs/domain/stories/HU-33-alertas-comprensibles-accionables/refinamiento.md` | Plan técnico, 34 tareas en 8 fases, 11 decisiones |
| Creado | `docs/domain/stories/HU-33-alertas-comprensibles-accionables/dev-record.md` | Este registro |
| Modificado | `docs/domain/stories/HU-33-alertas-comprensibles-accionables/index.md` | Fases de Refinamiento y Desarrollo |
| Modificado | `docs/domain/stories/HU-33-alertas-comprensibles-accionables/cambios.md` | Entradas de refinamiento y desarrollo |

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew testDebugUnitTest` | **546 tests · 0 fallos · 0 errores · 0 omitidos** |
| `./gradlew build` (debug + release + lint + tests) | **BUILD SUCCESSFUL** |
| Versión de esquema | Sin cambio: v15. No se generó `16.json` ni migración |

La validación manual de los 13 escenarios queda registrada en `refinamiento.md` § *Validación manual (no automatizable)* y no se ejecutó en esta sesión.

### Métricas Dev-Rápido

- Tiempo sesión IA: 22 min
- Tareas manuales DoD: 0 min
- Tiempo total: 22 min
