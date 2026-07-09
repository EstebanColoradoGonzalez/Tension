# Historia de Usuario

**Como** ejecutante,
**Quiero** que al instalar la aplicación por primera vez o al actualizar desde una versión anterior, el diccionario de ejercicios precargado y el plan de entrenamiento por defecto reflejen exactamente el catálogo definido en la documentación vigente,
**Para** comenzar a entrenar inmediatamente con un plan actualizado y coherente, sin perder la capacidad de crear ejercicios propios ni editar los de base.

## Descripción

Esta historia define la actualización del diccionario de ejercicios precargado y del plan de entrenamiento por defecto. El diccionario pasa de 43 ejercicios precargados a 26. Se eliminan 22 ejercicios obsoletos, se renombran varios existentes, se ajustan tipos de equipamiento y zonas musculares, y se agregan 14 ejercicios nuevos. El plan de entrenamiento por defecto pasa de 3 rutinas × 3 versiones cada una (9 versiones, 82 asignaciones) a 3 rutinas con 4 versiones totales (Pierna V1+V2, Push V1, Pull V1) y 27 asignaciones. Se introducen 6 nuevos tipos de equipamiento para reflejar la variabilidad de implementos que admiten ciertos ejercicios.

Esta historia es una historia de **datos puros** — no cambia lógica de negocio, UI (salvo 1 método de carga de imágenes), navegación ni reglas de dominio. El esquema de tablas no cambia (las 18 entidades de v9 se preservan con exactamente las mismas columnas y constraints). Lo que cambia es: (1) el catálogo de datos; (2) la migración para usuarios existentes con lógica condicional de borde; (3) los seeders para nuevas instalaciones; (4) los assets de imágenes.

Se basa en el estado post-HU-23 (v9) como punto de partida. Se sigue el mismo patrón de migración establecido por MIGRATION_6_7 (HU-16), MIGRATION_7_8 (HU-21) y MIGRATION_8_9 (HU-23).

---

## Criterios de Aceptación

### Nuevos Tipos de Equipamiento

- **CA-24.01:** El sistema agrega al catálogo de tipos de equipamiento (`equipment_type`) los siguientes 6 nuevos registros, tanto en el seed data para instalaciones nuevas como en la migración para usuarios existentes:
  - ID 10: "Mancuernas o Polea"
  - ID 11: "Polea con Cuerda"
  - ID 12: "Polea con Cuerda o Polea con Barra en V"
  - ID 13: "Barra"
  - ID 14: "Mancuerna o Polea o Barra"
  - ID 15: "Barra o Mancuernas"

- **CA-24.02:** Los tipos de equipamiento existentes (IDs 1-9) no se modifican ni se eliminan, ya que pueden estar referenciados por ejercicios personalizados del usuario.

### Diccionario de Ejercicios — Seed Data (Instalaciones Nuevas)

- **CA-24.03:** En una instalación nueva de la app, el `ExerciseSeeder` inserta exactamente 26 ejercicios precargados (`is_custom = 0`) con los siguientes datos:

  | ID | Nombre | equipment_type_id | Zona(s) Muscular(es) | is_bodyweight | is_isometric | is_to_technical_failure |
  |----|--------|-------------------|----------------------|---------------|--------------|------------------------|
  | 1 | Aductores | 1 (Máquina) | Aductores (12) | 0 | 0 | 0 |
  | 2 | Cruce de Polea Alta | 6 (Polea) | Pecho Inferior (3) | 0 | 0 | 0 |
  | 3 | Crunch Abdominal | 6 (Polea) | Abdomen (6) | 0 | 0 | 0 |
  | 4 | Curl Bayesian en Banco Inclinado | 2 (Mancuernas) | Bíceps (9) | 0 | 0 | 0 |
  | 5 | Curl de Concentración | 5 (Mancuerna) | Bíceps (9) | 0 | 0 | 0 |
  | 6 | Curl de Isquiotibiales Sentado | 1 (Máquina) | Isquiotibiales (11) | 0 | 0 | 0 |
  | 7 | Curl de Martillo Cruzado | 2 (Mancuernas) | Bíceps (9) | 0 | 0 | 0 |
  | 8 | Curl de Predicador | 5 (Mancuerna) | Bíceps (9) | 0 | 0 | 0 |
  | 9 | Elevación de Pantorrilla en Máquina de Pie | 1 (Máquina) | Gemelos (14) | 0 | 0 | 0 |
  | 10 | Elevación Lateral | 10 (Mancuernas o Polea) | Hombro (7) | 0 | 0 | 0 |
  | 11 | Extensión de Cuádriceps | 1 (Máquina) | Cuádriceps (10) | 0 | 0 | 0 |
  | 12 | Extensión de Tríceps en Polea (Pushdown) | 12 (Polea con Cuerda o Polea con Barra en V) | Tríceps (8) | 0 | 0 | 0 |
  | 13 | Extensión de Tríceps por encima de la Cabeza | 10 (Mancuernas o Polea) | Tríceps (8) | 0 | 0 | 0 |
  | 14 | Face Pull | 11 (Polea con Cuerda) | Espalda Alta (16) | 0 | 0 | 0 |
  | 15 | Hip Thrust | 1 (Máquina) | Glúteos (15) | 0 | 0 | 0 |
  | 16 | Peso Muerto Rumano | 13 (Barra) | Isquiotibiales (11), Glúteos (15) | 0 | 0 | 0 |
  | 17 | Prensa Inclinada | 1 (Máquina) | Cuádriceps (10) | 0 | 0 | 0 |
  | 18 | Press de Banca Inclinado | 14 (Mancuerna o Polea o Barra) | Pecho Superior (2) | 0 | 0 | 0 |
  | 19 | Press de Banca Plano | 15 (Barra o Mancuernas) | Pecho Medio (1) | 0 | 0 | 0 |
  | 20 | Press Pallof | 6 (Polea) | Abdomen (6) | 0 | 0 | 0 |
  | 21 | Remo T Inclinado | 1 (Máquina) | Espalda Media (4) | 0 | 0 | 0 |
  | 22 | Sentadilla Búlgara | 2 (Mancuernas) | Cuádriceps (10), Glúteos (15) | 0 | 0 | 0 |
  | 23 | Sentadilla de Zumo | 5 (Mancuerna) | Cuádriceps (10), Aductores (12) | 0 | 0 | 0 |
  | 24 | Sentadilla Hack | 1 (Máquina) | Cuádriceps (10) | 0 | 0 | 0 |
  | 25 | Tirón de Dorsales | 6 (Polea) | Dorsal Ancho (5) | 0 | 0 | 0 |
  | 26 | Vuelos Posteriores | 2 (Mancuernas) | Hombro (7) | 0 | 0 | 0 |

- **CA-24.04:** Los ejercicios con zonas musculares múltiples (Peso Muerto Rumano → Isquiotibiales + Glúteos; Sentadilla Búlgara → Cuádriceps + Glúteos; Sentadilla de Zumo → Cuádriceps + Aductores) insertan un registro por cada zona en la tabla `exercise_muscle_zone`.

- **CA-24.05:** Cada ejercicio precargado tiene `is_custom = 0` para distinguirlo de los ejercicios creados por el usuario.

- **CA-24.06:** Ningún ejercicio precargado es de tipo bodyweight (`is_bodyweight = 0`), isométrico (`is_isometric = 0`) ni al fallo técnico (`is_to_technical_failure = 0`) en este nuevo diccionario.

- **CA-24.07:** El campo `media_resource` de cada ejercicio precargado se genera como el nombre normalizado del ejercicio en snake_case concatenado con el nombre normalizado del tipo de equipamiento (ejemplo: `aductores_maquina`, `curl_bayesian_en_banco_inclinado_mancuernas`, `face_pull_polea_con_cuerda`). Para tipos compuestos con "o", se usa el primer tipo antes del "o" (ejemplo: `elevacion_lateral_mancuernas`, `press_de_banca_plano_barra`). El valor de `media_resource` corresponde al nombre del archivo `.png` (sin extensión) ubicado en `assets/exercises/`.

### Gestión de Imágenes de Ejercicios (Assets)

Las imágenes de los ejercicios precargados se almacenan como archivos `.png` en el directorio de assets del APK. La carpeta `assets/exercises/` ya fue reestructurada como carpeta plana (sin subdirectorios `module-a/b/c`) y las 12 imágenes nuevas (10 de ejercicios nuevos + 2 faltantes: Remo T Inclinado y Sentadilla Búlgara) ya fueron provistas antes del inicio del desarrollo. Durante el desarrollo de esta historia únicamente se debe eliminar las imágenes obsoletas y renombrar las reutilizadas.

#### Reestructuración de carpeta de assets

- **CA-24.07a:** La carpeta plana `assets/exercises/` ya está creada con todas las imágenes disponibles. Las subcarpetas `module-a/`, `module-b/` y `module-c/` ya no existen. No se requiere ninguna acción de migración de carpetas.

- **CA-24.07b:** El código de carga de imágenes en `ExerciseDetailScreen.kt` se actualiza para buscar la imagen en `exercises/{media_resource}.png` (carpeta plana) en lugar de iterar sobre las subcarpetas `module-a`, `module-b`, `module-c`. La lógica de fallback para imágenes custom (ruta absoluta en almacenamiento interno) se preserva sin cambios.

#### Imágenes a eliminar (ejercicios removidos del diccionario)

- **CA-24.07d:** Se eliminan del directorio `assets/exercises/` las 29 imágenes correspondientes a ejercicios que ya no forman parte del diccionario (27 del catálogo eliminado + 2 imágenes con nombre antiguo reemplazadas por nuevas):

  | Imagen a eliminar | Motivo |
  |---|---|
  | `abdominales_cuerpo.png` | Ejercicio eliminado |
  | `curl_de_biceps_mancuerna.png` | Ejercicio eliminado |
  | `curl_de_biceps_polea.png` | Ejercicio eliminado |
  | `curl_de_martillo_mancuerna.png` | Ejercicio eliminado |
  | `escalador_cuerpo.png` | Ejercicio eliminado |
  | `elevacion_de_hombros_con_mancuernas_mancuerna.png` | Ejercicio eliminado |
  | `giro_ruso_cuerpo.png` | Ejercicio eliminado |
  | `plancha_cuerpo.png` | Ejercicio eliminado |
  | `plancha_lateral_cuerpo.png` | Ejercicio eliminado |
  | `remo_con_un_solo_brazo_doblado_mancuerna.png` | Ejercicio eliminado |
  | `apertura_de_pecho_inclinado_maquina.png` | Ejercicio eliminado |
  | `apertura_de_pecho_sentado_maquina.png` | Ejercicio eliminado |
  | `dominada_de_triceps_banco_pesa.png` | Ejercicio eliminado |
  | `elevacion_frontal_mancuerna.png` | Ejercicio eliminado |
  | `flexiones_cuerpo.png` | Ejercicio eliminado |
  | `press_de_elevacion_sentado_mancuerna.png` | Ejercicio eliminado |
  | `press_de_mancuerna_mancuernas.png` | Ejercicio eliminado |
  | `remo_vertical_barra_de_pesas.png` | Ejercicio eliminado |
  | `remo_vertical_con_cable_maquina.png` | Ejercicio eliminado |
  | `abductor_de_cadera_maquina.png` | Ejercicio eliminado |
  | `avanzada_de_zancadas_mancuernas.png` | Ejercicio eliminado |
  | `curl_femoral_tumbado_maquina.png` | Ejercicio eliminado |
  | `press_de_pierna_maquina.png` | Ejercicio eliminado |
  | `sentadilla_cuerpo.png` | Ejercicio eliminado |
  | `sentadilla_maquina_multiestacion.png` | Ejercicio eliminado |
  | `subir_escalones_maquina.png` | Ejercicio eliminado |
  | `zancada_hacia_atras_mancuernas.png` | Ejercicio eliminado |
  | `remo_con_inclinacion_barra_de_pesas.png` | Reemplazada por nueva imagen `remo_t_inclinado_maquina.png` |
  | `sentadilla_bulgara_dividida_mancuernas.png` | Reemplazada por nueva imagen `sentadilla_bulgara_mancuernas.png` |

#### Imágenes a conservar y renombrar (ejercicios que permanecen)

- **CA-24.07e:** Se renombran 14 imágenes existentes que corresponden a ejercicios que permanecen en el diccionario. La imagen ya está en `assets/exercises/` (carpeta plana); la tarea es únicamente renombrarla según la nueva convención de `media_resource`:

  | Nombre actual | Nombre nuevo | Ejercicio |
  |---|---|---|
  | `aductor_de_cadera_maquina.png` | `aductores_maquina.png` | Aductores |
  | `cruce_en_polea_alta_maquina.png` | `cruce_de_polea_alta_polea.png` | Cruce de Polea Alta |
  | `curl_de_contraccion_mancuerna.png` | `curl_de_concentracion_mancuerna.png` | Curl de Concentración |
  | `curl_de_martillo_cruzado_mancuerna.png` | `curl_de_martillo_cruzado_mancuernas.png` | Curl de Martillo Cruzado |
  | `elevacion_lateral_mancuerna.png` | `elevacion_lateral_mancuernas.png` | Elevación Lateral |
  | `extension_de_cuadriceps_maquina.png` | `extension_de_cuadriceps_maquina.png` | Extensión de Cuádriceps *(sin cambio de nombre)* |
  | `flexion_de_triceps_con_cuerda_maquina.png` | `extension_de_triceps_en_polea_pushdown_polea_con_cuerda.png` | Extensión de Tríceps en Polea (Pushdown) |
  | `extension_de_triceps_por_encima_de_la_cabeza_mancuerna.png` | `extension_de_triceps_por_encima_de_la_cabeza_mancuernas.png` | Extensión de Tríceps por encima de la Cabeza |
  | `empuje_de_cadera_maquina.png` | `hip_thrust_maquina.png` | Hip Thrust |
  | `press_de_banca_inclinada_maquina.png` | `press_de_banca_inclinado_mancuerna.png` | Press de Banca Inclinado |
  | `press_de_banca_maquina.png` | `press_de_banca_plano_barra.png` | Press de Banca Plano |
  | `sentadilla_de_sumo_mancuerna_o_pesa_rusa.png` | `sentadilla_de_zumo_mancuerna.png` | Sentadilla de Zumo |
  | `tiro_de_dorsales_agarre_ancho_maquina.png` | `tiron_de_dorsales_polea.png` | Tirón de Dorsales |
  | `elevacion_de_gemelos_sentado_maquina.png` | `elevacion_de_pantorrilla_en_maquina_de_pie_maquina.png` | Elevación de Pantorrilla en Máquina de Pie |

#### Imágenes nuevas requeridas (ejercicios sin imagen previa)

- **CA-24.07f:** Las 12 imágenes nuevas ya fueron provistas antes del inicio del desarrollo y están disponibles en `assets/exercises/` con sus nombres finales correctos. No se requiere crear imágenes durante el desarrollo:

  | Imagen ya disponible | Ejercicio | Origen |
  |---|---|---|
  | `crunch_abdominal_polea.png` | Crunch Abdominal | Nueva |
  | `curl_bayesian_en_banco_inclinado_mancuernas.png` | Curl Bayesian en Banco Inclinado | Nueva |
  | `curl_de_isquiotibiales_sentado_maquina.png` | Curl de Isquiotibiales Sentado | Nueva |
  | `curl_de_predicador_mancuerna.png` | Curl de Predicador | Nueva |
  | `face_pull_polea_con_cuerda.png` | Face Pull | Nueva |
  | `peso_muerto_rumano_barra.png` | Peso Muerto Rumano | Nueva |
  | `press_pallof_polea.png` | Press Pallof | Nueva |
  | `prensa_inclinada_maquina.png` | Prensa Inclinada | Nueva |
  | `sentadilla_hack_maquina.png` | Sentadilla Hack | Nueva |
  | `vuelos_posteriores_mancuernas.png` | Vuelos Posteriores | Nueva |
  | `remo_t_inclinado_maquina.png` | Remo T Inclinado | Nueva (imagen anterior no reutilizable) |
  | `sentadilla_bulgara_mancuernas.png` | Sentadilla Búlgara | Nueva (imagen anterior no reutilizable) |

- **CA-24.07g:** Tras completar esta historia, el directorio `assets/exercises/` debe contener exactamente 26 archivos `.png` (14 renombrados + 12 nuevos), uno por cada ejercicio precargado del diccionario. No debe quedar ningún archivo con nombre obsoleto (ver CA-24.07d).

- **CA-24.07h:** Todas las imágenes están en formato PNG, resolución mínima recomendada de 512×512 px, y muestran claramente la posición o movimiento del ejercicio.

### Plan de Entrenamiento — Seed Data (Instalaciones Nuevas)

- **CA-24.08:** En una instalación nueva, el `PlanSeeder` crea exactamente 3 rutinas en la tabla `routine`:

  | ID | Nombre | sort_order |
  |----|--------|------------|
  | 1 | Pierna (Leg) | 1 |
  | 2 | Pecho, Hombro, Tríceps (Push) | 2 |
  | 3 | Espalda, Bíceps y Abdomen (Pull) | 3 |

- **CA-24.09:** El `PlanSeeder` crea exactamente 4 versiones en la tabla `routine_version`:

  | ID | routine_id | version_number |
  |----|-----------|----------------|
  | 1 | 1 (Pierna) | 1 |
  | 2 | 1 (Pierna) | 2 |
  | 3 | 2 (Push) | 1 |
  | 4 | 3 (Pull) | 1 |

- **CA-24.10:** El `PlanSeeder` inicializa la versión actual de cada rutina en `routine_current_version` con `current_version_number = 1` para las 3 rutinas.

- **CA-24.11:** El `PlanSeeder` crea exactamente 27 asignaciones en `plan_assignment` con 4 series y rango "8-12" cada una, distribuidas así:

  **Rutina 1: Pierna (Leg) — Versión 1** (routine_version_id = 1, 6 ejercicios)

  | sort_order | exercise_id | Ejercicio |
  |------------|-------------|-----------|
  | 1 | 1 | Aductores |
  | 2 | 6 | Curl de Isquiotibiales Sentado |
  | 3 | 17 | Prensa Inclinada |
  | 4 | 24 | Sentadilla Hack |
  | 5 | 11 | Extensión de Cuádriceps |
  | 6 | 9 | Elevación de Pantorrilla en Máquina de Pie |

  **Rutina 1: Pierna (Leg) — Versión 2** (routine_version_id = 2, 6 ejercicios)

  | sort_order | exercise_id | Ejercicio |
  |------------|-------------|-----------|
  | 1 | 1 | Aductores |
  | 2 | 6 | Curl de Isquiotibiales Sentado |
  | 3 | 16 | Peso Muerto Rumano |
  | 4 | 15 | Hip Thrust |
  | 5 | 11 | Extensión de Cuádriceps |
  | 6 | 9 | Elevación de Pantorrilla en Máquina de Pie |

  **Rutina 2: Pecho, Hombro, Tríceps (Push) — Versión 1** (routine_version_id = 3, 7 ejercicios)

  | sort_order | exercise_id | Ejercicio |
  |------------|-------------|-----------|
  | 1 | 10 | Elevación Lateral |
  | 2 | 18 | Press de Banca Inclinado |
  | 3 | 19 | Press de Banca Plano |
  | 4 | 26 | Vuelos Posteriores |
  | 5 | 13 | Extensión de Tríceps por encima de la Cabeza |
  | 6 | 2 | Cruce de Polea Alta |
  | 7 | 12 | Extensión de Tríceps en Polea (Pushdown) |

  **Rutina 3: Espalda, Bíceps y Abdomen (Pull) — Versión 1** (routine_version_id = 4, 8 ejercicios)

  | sort_order | exercise_id | Ejercicio |
  |------------|-------------|-----------|
  | 1 | 5 | Curl de Concentración |
  | 2 | 25 | Tirón de Dorsales |
  | 3 | 21 | Remo T Inclinado |
  | 4 | 14 | Face Pull |
  | 5 | 4 | Curl Bayesian en Banco Inclinado |
  | 6 | 7 | Curl de Martillo Cruzado |
  | 7 | 3 | Crunch Abdominal |
  | 8 | 20 | Press Pallof |

- **CA-24.12:** Para las posiciones del plan que documentan alternativas con "**o**" entre ejercicios distintos (Pierna V2 posición 4: "Hip Thrust **o** Sentadilla Búlgara **o** Sentadilla de Zumo"; Pull posición 1: "Curl de Concentración **o** Curl de Predicador"), el plan asigna por defecto el primer ejercicio listado. El usuario puede sustituirlo en sesión activa mediante la funcionalidad de sustitución puntual existente (que filtra por zona muscular).

### Migración de Datos (MIGRATION_9_10) — Usuarios Existentes

- **CA-24.13:** Se crea una migración Room `MIGRATION_9_10` que incrementa la versión de la base de datos de 9 a 10.

- **CA-24.14:** La migración inserta los 6 nuevos tipos de equipamiento (IDs 10-15) en la tabla `equipment_type`.

- **CA-24.15:** La migración elimina de la tabla `exercise` todos los ejercicios precargados (`is_custom = 0`) que no forman parte del nuevo diccionario de 26 ejercicios. Los ejercicios a eliminar son aquellos con IDs del seed anterior (1-43) que no tienen correspondencia en el nuevo diccionario. Antes de eliminar un ejercicio, la migración debe:
  - Eliminar sus registros en `exercise_muscle_zone`.
  - Eliminar sus asignaciones en `plan_assignment`.
  - Verificar si el ejercicio tiene registros históricos en `exercise_set` (a través de `session_exercise`). Si los tiene, el ejercicio NO se elimina sino que se marca como `is_custom = 1` para preservar el historial y se mantiene en el catálogo como ejercicio del usuario.

- **CA-24.16:** La migración inserta los 26 nuevos ejercicios precargados con los IDs, nombres, tipos de equipamiento y zonas musculares definidos en CA-24.03, usando IDs nuevos que no colisionen con ejercicios existentes (custom o migrados). Para evitar colisiones, se usan IDs a partir del `MAX(id) + 1` actual de la tabla `exercise`.

- **CA-24.17:** La migración recrea las rutinas: elimina las rutinas existentes que fueron creadas por el seed original (IDs 1, 2, 3) y las reemplaza con las 3 nuevas rutinas definidas en CA-24.08. Si el usuario creó rutinas adicionales (ID > 3), estas se preservan intactas con sus versiones y asignaciones.

- **CA-24.18:** La migración recrea las versiones de rutinas: elimina las versiones de las rutinas originales (IDs 1-9) y crea las 4 nuevas versiones definidas en CA-24.09.

- **CA-24.19:** La migración recrea las asignaciones del plan: elimina todas las asignaciones (`plan_assignment`) vinculadas a las versiones de rutinas originales y crea las 27 nuevas asignaciones definidas en CA-24.11.

- **CA-24.20:** La migración actualiza `routine_current_version` para las 3 rutinas del nuevo plan con `current_version_number = 1`.

- **CA-24.21:** La migración reinicia la posición en la rotación (`rotation_state.microcycle_position`) a 1 para reflejar que el nuevo plan comienza desde la primera rutina (Pierna). El contador de microciclos (`microcycle_count`) se preserva.

- **CA-24.22:** Si el usuario tiene una sesión activa (`status = 'IN_PROGRESS'`) al momento de la migración, la migración NO debe fallar. La sesión activa se preserva con su `routine_version_id` original. Si la versión referenciada fue eliminada, la sesión se marca como `COMPLETED` automáticamente para evitar inconsistencias de FK.

### Preservación de Datos del Usuario

- **CA-24.23:** Los ejercicios creados por el usuario (`is_custom = 1`) no se eliminan, no se modifican y permanecen disponibles en el catálogo tras la migración.

- **CA-24.24:** Las rutinas creadas por el usuario (IDs distintos a los del seed original: 1, 2, 3) no se eliminan ni se modifican. Sus versiones y asignaciones se preservan intactas.

- **CA-24.25:** El historial completo de sesiones pasadas (`session`, `session_exercise`, `exercise_set`) se preserva íntegramente. Las sesiones que referenciaban versiones de rutinas eliminadas mantienen su `routine_version_id` original; la integridad referencial se asegura reteniendo los registros de `routine_version` referenciados por sesiones históricas (no se eliminan versiones que tengan sesiones asociadas — en su lugar se marcan como versiones huérfanas pero válidas).

- **CA-24.26:** Los registros de progresión (`exercise_progression`) vinculados a ejercicios eliminados se eliminan junto con el ejercicio. Los vinculados a ejercicios que se preservaron (por tener historial) se mantienen.

- **CA-24.27:** Las alertas (`alert`) vinculadas a rutinas eliminadas se eliminan. Las alertas vinculadas a ejercicios se manejan según la preservación del ejercicio (CA-24.15).

- **CA-24.28:** Si el usuario tiene una descarga activa (`deload.status = 'ACTIVE'`), la migración la completa automáticamente (`status = 'COMPLETED'`, `completion_date = date('now')`) antes de eliminar las rutinas/versiones del seed original, para evitar inconsistencias en `deload_frozen_version`.

### Registro en DatabaseModule

- **CA-24.29:** La nueva migración `MIGRATION_9_10` se registra en `DatabaseModule` dentro de `.addMigrations(...)`, junto con las migraciones existentes (6→7, 7→8, 8→9).

### Consistencia del Seed para Reinstalaciones

- **CA-24.30:** Si el usuario desinstala y reinstala la app (base de datos destruida), el `PrepopulateCallback.onCreate` invoca `PrepopulateFacade.populate` que ejecuta el `BaseDataSeeder` (con los 15 tipos de equipamiento), el `ExerciseSeeder` (con los 26 ejercicios) y el `PlanSeeder` (con las 3 rutinas, 4 versiones y 27 asignaciones) del nuevo diccionario y plan.

- **CA-24.31:** Si la base de datos se recrea por `onDestructiveMigration`, el mismo seed actualizado se aplica automáticamente.

### Validación de Integridad

- **CA-24.32:** Tras la migración, la tabla `exercise` contiene exactamente 26 ejercicios con `is_custom = 0` (más los que se migraron a `is_custom = 1` por tener historial, más los que el usuario haya creado manualmente).

- **CA-24.33:** Tras la migración, cada ejercicio precargado tiene al menos una zona muscular asociada en `exercise_muscle_zone`, y los ejercicios multi-zona tienen exactamente 2 entradas.

- **CA-24.34:** Tras la migración, la tabla `plan_assignment` contiene exactamente 27 registros vinculados a las 4 versiones del nuevo plan (6+6+7+8), más las asignaciones de rutinas personalizadas del usuario.

- **CA-24.35:** Tras la migración, no existen registros huérfanos en `exercise_muscle_zone`, `plan_assignment` ni `session_exercise` que referencien ejercicios inexistentes.

- **CA-24.36:** El índice único `index_exercise_name_equipment_type_id` en la tabla `exercise` se respeta: no existen dos ejercicios con el mismo nombre y mismo `equipment_type_id`.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite al sistema tener un catálogo de ejercicios actualizado y un plan de entrenamiento por defecto coherente con la documentación vigente, para que el ejecutante pueda comenzar a entrenar inmediatamente sin configuración adicional.

### Reglas de Negocio

1. **Catálogo de 26 ejercicios:** El nuevo diccionario contiene exactamente 26 ejercicios precargados (`is_custom = 0`). Se eliminan 22 ejercicios obsoletos del diccionario anterior (43 → 26).
2. **Preservación de ejercicios custom:** Los ejercicios creados por el usuario (`is_custom = 1`) nunca se eliminan. Los ejercicios precargados con historial en `session_exercise` se migran a `is_custom = 1`.
3. **IDs dinámicos en migración:** En la migración, los nuevos ejercicios se insertan con IDs a partir de `MAX(id) + 1` para evitar colisiones con ejercicios existentes.
4. **Plan de entrenamiento simplificado:** De 9 versiones (3 rutinas × 3 versiones) a 4 versiones (Pierna V1+V2, Push V1, Pull V1). De 82 asignaciones a 27.
5. **Rotación reiniciada:** La migración reinicia `microcycle_position` a 1 para que el nuevo plan comience desde la primera rutina (Pierna). `microcycle_count` se preserva.
6. **Deload activo:** Si existe un deload activo, se completa automáticamente antes de la migración de rutinas.
7. **Sesión activa:** Si hay una sesión activa en una versión que será eliminada, se marca como `COMPLETED` automáticamente.

### Interfaz

- **Sin cambios de UI significativos:** La historia no introduce pantallas nuevas ni modifica flujos de navegación.
- **Única modificación de UI:** El método `rememberExerciseBitmap()` en `ExerciseDetailScreen.kt` se actualiza para buscar imágenes en la carpeta plana `assets/exercises/` en lugar de iterar sobre subdirectorios `module-a`, `module-b`, `module-c`. El fallback para imágenes custom se preserva.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Preview de Interfaz

No aplica — la historia no introduce cambios de UI significativos. La única modificación es interna en `ExerciseDetailScreen.rememberExerciseBitmap()`.

---

## Dependencias Técnicas e Integración

### Dependencias de historias anteriores

| Historia | Relación |
|----------|----------|
| **HU-16** | Introdujo MIGRATION_6_7 — primer patrón de migración del proyecto (DDL + data) |
| **HU-21** | Introdujo MIGRATION_7_8 — patrón DDL + data para `sort_order` en `plan_assignment` |
| **HU-23** | Introdujo MIGRATION_8_9 — migración más compleja del proyecto (esquema + datos). HU-24 opera sobre el estado post-HU-23 (v9) |
| **HU-19** | Backup y Restauración — potencialmente afectada si el esquema JSON de exportación incluye IDs de ejercicios fijos. Con IDs dinámicos en migración, los backups v9 importados a v10 deben mapearse correctamente |

### Dependencias de infraestructura

- **Sin dependencias nuevas de runtime:** La migración usa APIs Room estándar + `SupportSQLiteDatabase`.
- **Dependencias en DB Module:** MIGRATION_9_10 debe agregarse a la cadena `.addMigrations()` junto con MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9.

### Impacto en otros componentes

- **`ExerciseDetailScreen.rememberExerciseBitmap()`** — está **actualmente roto** post-reestructuración de assets (busca en subdirectorios que ya no existen). La corrección en Bloque E es retrocompatible.
- **`PrepopulateFacade`** y **`PrepopulateCallback`** — no requieren modificación. Invocan los 3 seeders actualizados automáticamente.
- **Entidades, DAOs y TypeConverters** — sin cambio. El esquema v9 soporta los nuevos datos sin modificación.
