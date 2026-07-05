## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-07-05

---

### Contexto

Seed exclusivo de instalaciones frescas (`RoomDatabase.Callback.onCreate()`). Sin migración Room. Patrón: `PrepopulateFacade` → `ExerciseSeeder` + `PlanSeeder`.

Regla de nomenclatura del plan:
- `/` entre nombres = mismo ejercicio (alias). Un solo registro, slot simple.
- `o` entre nombres = ejercicios distintos. Slot dual (sistema HU-26).

Slots duales resultantes: Sentadilla Hack **o** Prensa Inclinada (Miérc.), Face Pull **o** Vuelos Posteriores (Vie.).  
"Curl Inclinado / Bayesian" = mismo ejercicio → id=4 existente, slot simple.

---

### Tareas de Implementación

#### Fase 1 — Nuevos ejercicios (ExerciseSeeder.kt)

- [ ] **T1: Agregar 7 ejercicios en `seedExercises()`** — `data/local/seed/ExerciseSeeder.kt`

  | ID | Nombre | equipment_type_id | media_resource |
  |----|--------|-------------------|----------------|
  | 27 | Remo al Mentón | 13 (Barra) | `remo_al_menton_barra` |
  | 28 | Aperturas | 1 (Máquina) | `aperturas_contractor` |
  | 29 | Pull-Over | 6 (Polea) | `pull_over_polea` |
  | 30 | Curl Martillo | 2 (Mancuernas) | `curl_martillo_mancuernas` |
  | 31 | Rompecráneos | 13 (Barra) | `rompecraneos_barra` |
  | 32 | Remo Horizontal | 6 (Polea) | `remo_horizontal_polea` |
  | 33 | Zancadas | 2 (Mancuernas) | `zancadas_mancuernas` |

- [ ] **T2: Agregar zonas musculares en `seedExerciseMuscleZones()`** — `data/local/seed/ExerciseSeeder.kt`

  | exercise_id | muscle_zone_id(s) |
  |-------------|-------------------|
  | 27 | 7 (Hombro), 17 (Trapecio) |
  | 28 | 1 (Pecho Medio) |
  | 29 | 5 (Dorsal Ancho) |
  | 30 | 9 (Bíceps) |
  | 31 | 8 (Tríceps) |
  | 32 | 4 (Espalda Media) |
  | 33 | 10 (Cuádriceps), 15 (Glúteos) |

  Conteo final: **33 ejercicios**, **38 relaciones exercise_muscle_zone** (29 previas + 9 nuevas).

#### Fase 2 — Reemplazar PlanSeeder.kt

- [ ] **T3: Reemplazar `seedRoutines()`** — 3 rutinas → 6 rutinas — `data/local/seed/PlanSeeder.kt`

  | ID | Nombre | sort_order |
  |----|--------|-----------|
  | 1 | Push — Foco Deltoides Lateral | 1 |
  | 2 | Pull — Foco Dorsal Ancho | 2 |
  | 3 | Lower — Foco Cuádriceps | 3 |
  | 4 | Push — Foco Tríceps | 4 |
  | 5 | Pull — Foco Espalda Alta | 5 |
  | 6 | Lower — Foco Isquiotibiales | 6 |

- [ ] **T4: Reemplazar `seedRoutineVersions()`** — 4 versiones → 6 (una por rutina) — `data/local/seed/PlanSeeder.kt`

  | rv_id | routine_id | version_number |
  |-------|-----------|----------------|
  | 1 | 1 | 1 |
  | 2 | 2 | 1 |
  | 3 | 3 | 1 |
  | 4 | 4 | 1 |
  | 5 | 5 | 1 |
  | 6 | 6 | 1 |

- [ ] **T5: Reemplazar `seedRoutineCurrentVersions()`** — 3 → 6 registros — `data/local/seed/PlanSeeder.kt`

- [ ] **T6: Reemplazar `seedPlanAssignments()` y ajustar firma de `pa()`** — `data/local/seed/PlanSeeder.kt`

  Añadir parámetro `sets: Int` a `pa()` (plan mezcla 3 y 4 series). Eliminar constante `SETS = 4`.

  **Rutina 1 — Push Foco Deltoides (rv=1):**
  | sortOrder | slot | exercise_id | sets | ejercicio |
  |-----------|------|-------------|------|-----------|
  | 1 | 1 | 10 | 4 | Elevación Lateral |
  | 2 | 2 | 18 | 3 | Press de Banca Inclinado |
  | 3 | 3 | 19 | 3 | Press de Banca Plano |
  | 4 | 4 | 27 | 3 | Remo al Mentón |
  | 5 | 5 | 28 | 3 | Aperturas |

  **Rutina 2 — Pull Foco Dorsal Ancho (rv=2):**
  | sortOrder | slot | exercise_id | sets | ejercicio |
  |-----------|------|-------------|------|-----------|
  | 1 | 1 | 25 | 4 | Tirón de Dorsales |
  | 2 | 2 | 29 | 3 | Pull-Over |
  | 3 | 3 | 30 | 3 | Curl Martillo |
  | 4 | 4 | 4  | 3 | Curl Bayesian en Banco Inclinado |
  | 5 | 5 | 3  | 3 | Crunch Abdominal |

  **Rutina 3 — Lower Foco Cuádriceps (rv=3):**
  | sortOrder | slot | exercise_id | sets | ejercicio |
  |-----------|------|-------------|------|-----------|
  | 1 | 1 | 1  | 3 | Aductores |
  | 2 | 2 | 11 | 4 | Extensión de Cuádriceps |
  | 3 | 3 | 24 | 3 | Sentadilla Hack (primario slot 3) |
  | 4 | 3 | 17 | 3 | Prensa Inclinada (alternativa slot 3) |
  | 5 | 4 | 22 | 3 | Sentadilla Búlgara |
  | 6 | 5 | 9  | 3 | Elevación de Pantorrilla |

  **Rutina 4 — Push Foco Tríceps (rv=4):**
  | sortOrder | slot | exercise_id | sets | ejercicio |
  |-----------|------|-------------|------|-----------|
  | 1 | 1 | 13 | 4 | Extensión de Tríceps por encima de la Cabeza |
  | 2 | 2 | 19 | 3 | Press de Banca Plano |
  | 3 | 3 | 28 | 3 | Aperturas |
  | 4 | 4 | 12 | 3 | Extensión de Tríceps en Polea (Pushdown) |
  | 5 | 5 | 31 | 3 | Rompecráneos |

  **Rutina 5 — Pull Foco Espalda Alta (rv=5):**
  | sortOrder | slot | exercise_id | sets | ejercicio |
  |-----------|------|-------------|------|-----------|
  | 1 | 1 | 21 | 4 | Remo T Inclinado |
  | 2 | 2 | 32 | 3 | Remo Horizontal |
  | 3 | 3 | 14 | 3 | Face Pull (primario slot 3) |
  | 4 | 3 | 26 | 3 | Vuelos Posteriores (alternativa slot 3) |
  | 5 | 4 | 8  | 3 | Curl de Predicador |
  | 6 | 5 | 3  | 3 | Crunch Abdominal |

  **Rutina 6 — Lower Foco Isquiotibiales (rv=6):**
  | sortOrder | slot | exercise_id | sets | ejercicio |
  |-----------|------|-------------|------|-----------|
  | 1 | 1 | 1  | 3 | Aductores |
  | 2 | 2 | 6  | 4 | Curl de Isquiotibiales Sentado |
  | 3 | 3 | 16 | 3 | Peso Muerto Rumano |
  | 4 | 4 | 33 | 3 | Zancadas |
  | 5 | 5 | 11 | 3 | Extensión de Cuádriceps |
  | 6 | 6 | 9  | 3 | Elevación de Pantorrilla |

#### Fase 3 — Documentación

- [ ] **T7: Actualizar `architecture_blueprint.md`** (sección `data.local.seed`) — `docs/architecture/architecture_blueprint.md`
  - `exercise`: 43 → **33 filas**
  - `exercise_muscle_zone`: 48 → **38 filas**

- [ ] **T8: Actualizar `domain_and_state_model.md`** (sección seed data) — `docs/architecture/domain_and_state_model.md`
  - `exercise (43 filas)` → `exercise (33 filas)`: actualizar listado de ejercicios
  - `exercise_muscle_zone (48 filas)` → `exercise_muscle_zone (38 filas)`: actualizar descripción de multi-zona
