## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-08-30

---

### Contexto

Seed exclusivo de instalación fresca (`PrepopulateCallback.onCreate()` / `onDestructiveMigration()`). Patrón vigente: `PrepopulateFacade` → `BaseDataSeeder` + `ExerciseSeeder` + `PlanSeeder`, objetos Kotlin que insertan `ContentValues` con `CONFLICT_REPLACE` dentro de una única transacción.

**Sin cambio de esquema.** La base sigue en versión 13 y no se agrega migración: `Migrations.kt` queda intacto (sus referencias a *Tirón de Dorsales* son estados históricos del esquema y no deben reescribirse). La validación es sobre instalación fresca (desinstalar / borrar datos).

Reglas de nomenclatura del plan heredadas de HU-27:
- `/` entre nombres = mismo ejercicio (alias) → un registro, slot simple.
- **o** entre nombres = ejercicios distintos → slot dual (modelo HU-26: misma `slot`, distinto `sort_order`, primario el de menor `sort_order`).
- Variante de equipamiento ("Barra o Mancuernas") = un solo ejercicio; el nombre no incorpora el equipamiento.

#### Decisión técnica: extracción de los datos semilla a estructuras puras

Los tests unitarios del proyecto corren en JVM (`testOptions.unitTests.isReturnDefaultValues = true`, sin Robolectric). `ContentValues` y `SQLiteDatabase` son clases del framework Android sin implementación en JVM, por lo que los seeders actuales **no son verificables por tests**.

Se extraen los datos a estructuras Kotlin puras y los seeders quedan como mapeadores:

```
data/local/seed/
├── model/SeedExercise.kt        (nuevo) data class pura
├── model/SeedRoutine.kt         (nuevo) data class pura
├── model/SeedAssignment.kt      (nuevo) data class pura
├── ExerciseCatalog.kt           (nuevo) los 37 ejercicios + sus zonas musculares
├── DefaultPlan.kt               (nuevo) 6 rutinas + 35 asignaciones
├── ExerciseSeeder.kt            (modificado) itera ExerciseCatalog → ContentValues
└── PlanSeeder.kt                (modificado) itera DefaultPlan → ContentValues
```

Beneficio: los criterios CA-29.01 a CA-29.08 quedan verificados por tests JVM sin emulador. Coherente con RNF31 (seed versionado) y con la regla de dependencia arquitectónica: las estructuras no dependen de Android.

#### Conteos objetivo

| Tabla | Antes (HU-27) | Después (HU-29) |
|---|---|---|
| `exercise` | 33 | **37** |
| `exercise_muscle_zone` | 38 | **41** |
| `routine` / `routine_version` | 6 / 6 | 6 / 6 (sin cambio) |
| `plan_assignment` | 31 | **35** |

---

### Tareas de Implementación

#### Fase 1 — Modelos puros de datos semilla

- [x] **T1: Crear `SeedExercise`** — `data/local/seed/model/SeedExercise.kt`

  Campos: `id: Long`, `name: String`, `equipmentTypeId: Long`, `muscleZoneIds: List<Long>`, `mediaResource: String`, `isBodyweight: Boolean = false`, `isIsometric: Boolean = false`, `isToTechnicalFailure: Boolean = false`.

- [x] **T2: Crear `SeedRoutine` y `SeedAssignment`** — `data/local/seed/model/SeedRoutine.kt`, `data/local/seed/model/SeedAssignment.kt`

  `SeedRoutine(id, name, sortOrder)` · `SeedAssignment(routineVersionId, exerciseId, sets, reps, sortOrder, slot)`.

#### Fase 2 — Catálogo de ejercicios (CA-29.02, CA-29.03, CA-29.06, CA-29.07, CA-29.08)

- [x] **T3: Crear `ExerciseCatalog` con los 37 ejercicios** — `data/local/seed/ExerciseCatalog.kt` (Base: `ExerciseSeeder.seedExercises()` + `seedExerciseMuscleZones()`)

  Migrar los 33 ejercicios vigentes sin alterar ids, equipamiento ni `media_resource`, salvo:

  | ID | Cambio |
  |----|--------|
  | 25 | `name`: *Tirón de Dorsales* → **Jalón al Pecho**. Conserva id, `equipment_type_id = 6`, `media_resource = tiron_de_dorsales_polea`, zona 5 (Dorsal Ancho) e historial. |
  | 27 | *Remo al Mentón*: zonas `[7 Hombro, 17 Trapecio]` → **`[16 Espalda Alta]`**. |

  Agregar los 4 nuevos:

  | ID | Nombre | equipment_type_id | Zona muscular | is_bodyweight | media_resource |
  |----|--------|-------------------|---------------|:---:|----------------|
  | 34 | Press Militar | 2 (Mancuernas) | 7 (Hombro) | 0 | `press_militar_mancuernas` |
  | 35 | Dominadas | 23 (Barra Fija) | 5 (Dorsal Ancho) | **1** | `dominadas_barra_fija` |
  | 36 | Remo Unilateral en Polea Baja | 6 (Polea) | 4 (Espalda Media) | 0 | `remo_unilateral_en_polea_baja_polea` |
  | 37 | Remo Unilateral en Polea Alta | 6 (Polea) | 16 (Espalda Alta) | 0 | `remo_unilateral_en_polea_alta_polea` |

  Ningún ejercicio se elimina: *Remo al Mentón* (27), *Zancadas* (33) y *Extensión de Cuádriceps* (11) permanecen en el catálogo aunque salgan del plan por defecto.

  Resultado: **37 ejercicios**, **41 relaciones ejercicio-zona** (33 con 1 zona + 4 con 2 zonas: Peso Muerto Rumano, Sentadilla Búlgara, Sentadilla de Zumo, Zancadas).

- [x] **T4: Reescribir `ExerciseSeeder` como mapeador** — `data/local/seed/ExerciseSeeder.kt`

  `seed(db)` itera `ExerciseCatalog.ALL` insertando en `exercise` y, por cada `muscleZoneIds`, en `exercise_muscle_zone`. Se conserva `CONFLICT_REPLACE`. Desaparecen los `@Suppress("LongMethod")`.

#### Fase 3 — Plan por defecto (CA-29.01, CA-29.04, CA-29.05)

- [x] **T5: Crear `DefaultPlan.ROUTINES`** — `data/local/seed/DefaultPlan.kt` (Base: `PlanSeeder.seedRoutines()`)

  | ID | Nombre | sort_order |
  |----|--------|-----------|
  | 1 | Lunes: Pecho y Hombro (Push - Foco Deltoides Lateral y Medio) | 1 |
  | 2 | Martes: Espalda, Bíceps y Abdomen (Pull - Foco Dorsal Ancho) | 2 |
  | 3 | Miércoles: Pierna (Lower - Foco Cuádriceps) | 3 |
  | 4 | Jueves: Pecho y Tríceps (Push - Foco Tríceps) | 4 |
  | 5 | Viernes: Espalda, Bíceps y Abdomen (Pull - Foco Trapecios y Espalda Media) | 5 |
  | 6 | Sábado: Pierna (Lower - Foco Isquiotibiales y Glúteo) | 6 |

  Se mantiene el formato vigente `Día: Grupos (Tipo - Foco X)`; solo cambia el foco de las rutinas 1, 5 y 6. `routine_version` (6) y `routine_current_version` (6) sin cambios.

- [x] **T6: Crear `DefaultPlan.ASSIGNMENTS` — 35 asignaciones** — `data/local/seed/DefaultPlan.kt` (Base: `PlanSeeder.seedPlanAssignments()`)

  Todas con `reps = "8-12"`.

  **Rutina 1 — Lunes, Push Foco Deltoides Lateral y Medio (rv = 1):**

  | sort_order | slot | exercise_id | sets | ejercicio |
  |---|---|---|---|---|
  | 1 | 1 | 10 | 4 | Elevación Lateral |
  | 2 | 2 | 18 | 3 | Press de Banca Inclinado (primario slot 2) |
  | 3 | 2 | 34 | 3 | Press Militar (alternativa slot 2) |
  | 4 | 3 | 19 | 3 | Press de Banca Plano |
  | 5 | 4 | 28 | 3 | Aperturas |

  **Rutina 2 — Martes, Pull Foco Dorsal Ancho (rv = 2):**

  | sort_order | slot | exercise_id | sets | ejercicio |
  |---|---|---|---|---|
  | 1 | 1 | 25 | 4 | Jalón al Pecho (primario slot 1) |
  | 2 | 1 | 35 | 4 | Dominadas (alternativa slot 1) |
  | 3 | 2 | 30 | 3 | Curl Martillo |
  | 4 | 3 | 36 | 3 | Remo Unilateral en Polea Baja |
  | 5 | 4 | 4 | 3 | Curl Bayesian en Banco Inclinado (ejercicio único, no dual) |
  | 6 | 5 | 29 | 3 | Pull-Over |
  | 7 | 6 | 3 | 3 | Crunch Abdominal |

  **Rutina 3 — Miércoles, Lower Foco Cuádriceps (rv = 3):**

  | sort_order | slot | exercise_id | sets | ejercicio |
  |---|---|---|---|---|
  | 1 | 1 | 11 | 4 | Extensión de Cuádriceps |
  | 2 | 2 | 24 | 3 | Sentadilla Hack (primario slot 2) |
  | 3 | 2 | 17 | 3 | Prensa Inclinada (alternativa slot 2) |
  | 4 | 3 | 22 | 3 | Sentadilla Búlgara |
  | 5 | 4 | 1 | 3 | Aductores |
  | 6 | 5 | 9 | 3 | Elevación de Pantorrilla en Máquina de Pie |

  **Rutina 4 — Jueves, Push Foco Tríceps (rv = 4):**

  | sort_order | slot | exercise_id | sets | ejercicio |
  |---|---|---|---|---|
  | 1 | 1 | 13 | 4 | Extensión de Tríceps por encima de la Cabeza |
  | 2 | 2 | 19 | 3 | Press de Banca Plano |
  | 3 | 3 | 28 | 3 | Aperturas |
  | 4 | 4 | 12 | 3 | Extensión de Tríceps en Polea (Pushdown) |
  | 5 | 5 | 31 | 3 | Rompecráneos |

  **Rutina 5 — Viernes, Pull Foco Trapecios y Espalda Media (rv = 5):**

  | sort_order | slot | exercise_id | sets | ejercicio |
  |---|---|---|---|---|
  | 1 | 1 | 21 | 4 | Remo T Inclinado |
  | 2 | 2 | 14 | 3 | Face Pull (primario slot 2) |
  | 3 | 2 | 26 | 3 | Vuelos Posteriores (alternativa slot 2) |
  | 4 | 3 | 32 | 3 | Remo Horizontal |
  | 5 | 4 | 37 | 3 | Remo Unilateral en Polea Alta |
  | 6 | 5 | 8 | 3 | Curl de Predicador |
  | 7 | 6 | 3 | 3 | Crunch Abdominal |

  **Rutina 6 — Sábado, Lower Foco Isquiotibiales y Glúteo (rv = 6):**

  | sort_order | slot | exercise_id | sets | ejercicio |
  |---|---|---|---|---|
  | 1 | 1 | 6 | 4 | Curl de Isquiotibiales Sentado |
  | 2 | 2 | 16 | 3 | Peso Muerto Rumano |
  | 3 | 3 | 15 | 3 | Hip Thrust |
  | 4 | 4 | 1 | 3 | Aductores |
  | 5 | 5 | 9 | 3 | Elevación de Pantorrilla en Máquina de Pie |

  Salidas del plan por defecto (permanecen en el Diccionario): *Remo al Mentón* (Lunes), *Zancadas* y *Extensión de Cuádriceps* (Sábado).

  Slots duales resultantes: **4** — Lunes slot 2, Martes slot 1, Miércoles slot 2, Viernes slot 2. Cada par comparte `sets` y `reps`.

- [x] **T7: Reescribir `PlanSeeder` como mapeador** — `data/local/seed/PlanSeeder.kt`

  `seed(db)` itera `DefaultPlan.ROUTINES` (tabla `routine`), deriva `routine_version` y `routine_current_version` (una versión por rutina, `version_number = 1`) e itera `DefaultPlan.ASSIGNMENTS` para `plan_assignment`. Se conserva `CONFLICT_REPLACE`.

#### Fase 4 — Tests unitarios (JVM, sin emulador)

- [x] **T8: Crear `ExerciseCatalogTest`** — `test/.../data/local/seed/ExerciseCatalogTest.kt` (Base: `domain/util/RepsRangeParserTest.kt`)

  - 37 ejercicios · ids únicos · pares (nombre, equipamiento) únicos — CA-29.02
  - Total de relaciones ejercicio-zona = 41; todo `muscleZoneIds` no vacío y con ids en 1..20 — CA-29.02
  - Existe *Jalón al Pecho* con id 25, media `tiron_de_dorsales_polea` y zona 5; no existe *Tirón de Dorsales*; no hay duplicado del movimiento — CA-29.03
  - Los 4 nuevos (34–37) con equipamiento, zona y `media_resource` exactos; *Dominadas* con `isBodyweight = true` y el resto `false` — CA-29.02, CA-29.06
  - *Remo al Mentón* (27) tiene exactamente `[16]`, sin 7 ni 17 — CA-29.08
  - Los ejercicios fuera del plan siguen en el catálogo: 27, 33, 11 — CA-29.07
  - Ningún nombre incorpora la mención del equipamiento — CA-29.05

- [x] **T9: Crear `SeedAssetsTest`** — `test/.../data/local/seed/SeedAssetsTest.kt`

  - Cada `mediaResource` del catálogo tiene su PNG en `src/main/assets/exercises/` — CA-29.06
  - La carpeta contiene exactamente 37 PNG, sin archivos huérfanos — CA-29.06

- [x] **T10: Crear `DefaultPlanTest`** — `test/.../data/local/seed/DefaultPlanTest.kt`

  - 6 rutinas con `sort_order` 1..6 y el foco de cada nombre según CA-29.01
  - 35 asignaciones, todas con `reps = "8-12"` — CA-29.01
  - Composición exacta por rutina: secuencia de `(exerciseId, sets, slot)` ordenada por `sort_order` — CA-29.01
  - Exactamente 4 slots duales; ningún slot con más de 2 ejercicios; cada par comparte `sets` y `reps`; el primario es el de menor `sort_order` — CA-29.04
  - *Curl Bayesian en Banco Inclinado* (4) ocupa un slot propio de un solo ejercicio — CA-29.04
  - Todo `exerciseId` referenciado existe en `ExerciseCatalog` — integridad referencial
  - Sin `(routineVersionId, exerciseId)` duplicado — respeta la PK compuesta de `plan_assignment`

- [x] **T11: Ejecutar la suite completa** — `./gradlew test`

  Verificar además que no hay regresión en los tests de gestión del plan: `AddAlternativeToSlotUseCaseTest`, `UpdatePlanAssignmentUseCaseTest` — CA-29.09

#### Fase 5 — Documentación (CA-29.10)

- [x] **T12: Actualizar `domain_and_state_model.md` §6.1** — `docs/architecture/domain_and_state_model.md`
  - `exercise (33 filas)` → **37 filas**: agregar los 4 nuevos y renombrar *Tirón de Dorsales* → *Jalón al Pecho*; *Dominadas* con `is_bodyweight = 1`
  - `exercise_muscle_zone (38 filas)` → **41 filas**: 33 ejercicios con 1 zona, 4 con 2 zonas (Peso Muerto Rumano, Sentadilla Búlgara, Sentadilla de Zumo, Zancadas); *Remo al Mentón* sale del listado multi-zona
  - Plan de entrenamiento predeterminado: reemplazar la composición de las 6 rutinas por la de T6, marcando los 4 slots duales

- [x] **T13: Actualizar `architecture_blueprint.md`** — `docs/architecture/architecture_blueprint.md`
  - Línea 63: `exercise` 33 → **37 filas**, `exercise_muscle_zone` 38 → **41 filas**
  - Línea 167 (módulo Seed Data): `ExerciseSeeder` 33 → **37 ejercicios**, relaciones ejercicio-zona 38 → **41**; mencionar `ExerciseCatalog` y `DefaultPlan` como fuente de datos

- [x] **T14: Registrar la auditoría de catalogación en `cambios.md`** — `docs/domain/stories/HU-29-plan-catalogo-actualizados/cambios.md`

  Dejar constancia del barrido completo de las 37 asignaciones ejercicio-zona bajo criterio biomecánico y de la única corrección aplicada (*Remo al Mentón* → Espalda Alta), cerrando el gap abierto en CA-29.08.

---

### Validación manual (no automatizable)

Instalación fresca — desinstalar la app o borrar sus datos, ya que `DatabaseModule` no declara `fallbackToDestructiveMigration` — y verificar: las 6 rutinas con su composición, la imagen correcta de los 4 ejercicios nuevos en Diccionario y Sesión Activa, y que crear versión / asignar / remover / agregar alternativa siguen operando — CA-29.09, CA-29.06.
