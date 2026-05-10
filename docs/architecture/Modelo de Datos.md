# Modelo de Datos — Tension

---

## 1. Propósito

Este documento define el modelo de datos completo de Tension: todas las entidades (tablas), sus atributos, tipos, restricciones, relaciones por llaves foráneas y cardinalidades. El diseño está normalizado (3FN mínimo) y optimizado para Room/SQLite en una aplicación single-user offline.

Sirve como prerequisito directo para implementar la capa Model:

- Cada tabla descrita aquí mapea a un `@Entity` de Room.
- Cada relación por FK mapea a un `@ForeignKey` de Room.
- El seed data mapea a un `RoomDatabase.Callback` con pre-población.

---

## 2. Convenciones

**Versión actual de la base de datos: 13** (migraciones registradas: 1→2, 2→3, …, 11→12, 12→13).

| Convención | Descripción |
| ---------- | ----------- |
| **Nombres de tabla** | snake_case en inglés (ej: `routine_version`, `plan_assignment`) |
| **Nombres de columna** | snake_case en inglés (ej: `routine_id`, `equipment_type_id`) |
| **PK** | `id INTEGER PRIMARY KEY AUTOINCREMENT` salvo donde se indique una clave natural |
| **FK** | Sufijo `_id` o `_code` según el tipo de la PK referenciada |
| **Booleanos** | `INTEGER NOT NULL DEFAULT 0` (SQLite no tiene tipo BOOLEAN nativo; 0 = false, 1 = true) |
| **Fechas** | `TEXT NOT NULL` en formato ISO 8601 (`"YYYY-MM-DD"` o `"YYYY-MM-DDTHH:MM:SS"`) |
| **Decimales (Kg)** | `REAL` para valores de peso corporal y carga en kilogramos |
| **Enums** | `TEXT NOT NULL` con valores restringidos documentados en la columna. En Kotlin se implementan como `enum class` con `@TypeConverter` |
| **Cascadas** | Se documentan por relación. Seed data usa `ON DELETE RESTRICT` (nunca se elimina). Datos transaccionales usan `ON DELETE CASCADE` donde aplica |
| **Datos calculados** | No se almacenan en base de datos. Valores derivables (tonelaje, conteos, cargas previas, recomendaciones) se calculan en la capa de aplicación a partir de los datos base |

---

## 3. Catálogo de Entidades

### 3.1. routine

**Propósito:** Representa una rutina de entrenamiento definida por el ejecutante. Las rutinas son completamente configurables: el usuario les asigna un nombre, un orden de rotación y las zonas musculares que cubren. No existe un catálogo fijo — el ejecutante puede crear cualquier número de rutinas según su plan.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| name | TEXT | NOT NULL, UNIQUE | Nombre definido por el ejecutante: `"Pull + Abs"`, `"Push"`, `"Pierna"`, etc. |
| sort_order | INTEGER | NOT NULL | Posición de la rutina en la secuencia de rotación cíclica. 1-based. El ejecutante puede reordenarlas |
| created_at | TEXT | NOT NULL | Fecha de creación de la rutina (ISO 8601) |

**Restricciones adicionales:**

- `CHECK(sort_order >= 1)`

**Sin seed data fijo.** La configuración inicial sugerida (3 rutinas) se documenta en el Plan de Entrenamiento, pero el ejecutante la define al configurar su plan (HU-22).
---

### 3.2. muscle_zone

**Propósito:** Catálogo de las 20 zonas musculares específicas que clasifican cada ejercicio. Cada zona pertenece a un grupo muscular más amplio que se usa para la agregación de KPIs (tonelaje, distribución de volumen, tendencias).

**Jerarquía zona → grupo:** Las zonas con subvariantes (Pecho Medio, Pecho Superior, Pecho Inferior) se agrupan bajo un grupo muscular padre (Pecho). Las zonas de Espalda (Espalda Media, Dorsal Ancho, Espalda Alta, Espalda Baja, Trapecio) se agrupan bajo el grupo muscular padre (Espalda). Para los grupos restantes, la zona y el grupo comparten el mismo nombre (relación 1:1).

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| name | TEXT | NOT NULL, UNIQUE | Nombre específico de la zona: `"Pecho Medio"`, `"Dorsal Ancho"`, `"Cuádriceps"`, etc. |
| muscle_group | TEXT | NOT NULL | Grupo muscular padre para agregación de KPIs. Valores posibles: `"Pecho"`, `"Espalda"`, `"Abdomen"`, `"Hombro"`, `"Tríceps"`, `"Bíceps"`, `"Cuádriceps"`, `"Isquiotibiales"`, `"Glúteos"`, `"Aductores"`, `"Abductores"`, `"Gemelos"`, `"Antebrazo"`, `"Cuello"` |

**Índices:**

- Índice en `muscle_group` para consultas de agregación de KPIs.

**Seed data (20 filas):**

| id | name | muscle_group |
| -- | ---- | ------------ |
| 1 | Pecho Medio | Pecho |
| 2 | Pecho Superior | Pecho |
| 3 | Pecho Inferior | Pecho |
| 4 | Espalda Media | Espalda |
| 5 | Dorsal Ancho | Espalda |
| 6 | Abdomen | Abdomen |
| 7 | Hombro | Hombro |
| 8 | Tríceps | Tríceps |
| 9 | Bíceps | Bíceps |
| 10 | Cuádriceps | Cuádriceps |
| 11 | Isquiotibiales | Isquiotibiales |
| 12 | Aductores | Aductores |
| 13 | Abductores | Abductores |
| 14 | Gemelos | Gemelos |
| 15 | Glúteos | Glúteos |
| 16 | Espalda Alta | Espalda |
| 17 | Trapecio | Espalda |
| 18 | Espalda Baja | Espalda |
| 19 | Antebrazo | Antebrazo |
| 20 | Cuello | Cuello |

---

### 3.3. equipment_type

**Propósito:** Catálogo de los 23 tipos de equipamiento utilizados por los ejercicios. Normaliza el valor repetido en `exercise` y habilita el filtro por equipo en la vista D1 (Diccionario de Ejercicios).

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| name | TEXT | NOT NULL, UNIQUE | Nombre del tipo de equipo |

**Seed data (23 filas):**

| id | name |
| -- | ---- |
| 1 | Máquina |
| 2 | Mancuernas |
| 3 | Barra de Pesas |
| 4 | Cuerpo |
| 5 | Mancuerna |
| 6 | Polea |
| 7 | Pesa |
| 8 | Mancuerna o Pesa Rusa |
| 9 | Máquina Multiestación |
| 10 | Polea con Cuerda |
| 11 | Polea con Barra en V |
| 12 | Mancuerna o Polea |
| 13 | Mancuerna o Polea o Barra |
| 14 | Barra o Mancuernas |
| 15 | Mancuernas o Polea |
| 16 | Banda Elástica |
| 17 | Kettlebell |
| 18 | Barra EZ |
| 19 | TRX/Suspensión |
| 20 | Balón Medicinal |
| 21 | Rodillo de Abdomen |
| 22 | Paralelas/Dip Station |
| 23 | Barra Fija |

**Nota sobre `Mancuerna` vs `Mancuernas`:** Son tipos de equipo distintos en el dominio. `Mancuerna` (singular) indica que el ejercicio se ejecuta con una mancuerna. `Mancuernas` (plural) indica que se usan dos mancuernas simultáneamente. El Diccionario de Ejercicios los trata como entradas distintas.

---

### 3.4. exercise

**Propósito:** Catálogo de ejercicios del sistema. Contiene un conjunto base de 43 ejercicios precargados (seed data) y puede ser ampliado por el ejecutante con ejercicios propios (RF62). Es la entidad central — referenciada por el plan de entrenamiento, las sesiones, los registros de series y los KPIs. Todo ejercicio registrado en una sesión debe existir en este catálogo (Restricción de Integridad #2 del MDS).

**Clave natural compuesta:** La combinación `(name, equipment_type_id)` es única. Dos ejercicios pueden compartir nombre si difieren en tipo de equipo (ej: `"Sentadilla"` con `Cuerpo` vs `"Sentadilla"` con `Máquina Multiestación`). Se usa un `id` autoincremental como PK para eficiencia en FKs.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| name | TEXT | NOT NULL | Nombre del ejercicio: `"Press de banca"`, `"Flexiones"`, etc. |
| equipment_type_id | INTEGER | NOT NULL, **FK** → `equipment_type(id)` | Tipo de equipamiento requerido |
| is_bodyweight | INTEGER | NOT NULL, DEFAULT 0 | `1` si el ejercicio usa peso corporal (Peso fijo = 0 Kg). Se aplica R6: progresión por repeticiones totales, sin Doble Umbral |
| is_isometric | INTEGER | NOT NULL, DEFAULT 0 | `1` si el ejercicio es isométrico (Plancha, Plancha Lateral). Se aplica R7: progresión por segundos (30-45s), "dominado" cuando todas las series prescritas ≥ 45s |
| is_to_technical_failure | INTEGER | NOT NULL, DEFAULT 0 | `1` si el rango de repeticiones es "Al fallo técnico" (solo Flexiones). Sin límite superior de repeticiones |
| is_custom | INTEGER | NOT NULL, DEFAULT 0 | `1` si el ejercicio fue creado por el ejecutante (RF62). `0` para los 43 ejercicios precargados (seed data). Los ejercicios custom pueden tener imagen seleccionada desde la galería del dispositivo (almacenada en `filesDir/exercise_images/`) o no (se muestra logo placeholder con opción de agregar) |
| media_resource | TEXT | NULL | Para ejercicios seed: nombre normalizado del asset multimedia empaquetado en el APK que ilustra la ejecución correcta del ejercicio (RF61), cargado desde `assets/exercises/{media_resource}.png`. Para ejercicios custom o ejercicios con imagen personalizada: ruta absoluta al archivo de imagen en almacenamiento interno (`filesDir/exercise_images/exercise_{UUID}.jpg`). `NULL` cuando no se ha asociado imagen (se muestra logo de la app como placeholder con ícono AddAPhoto y texto "Toca para agregar imagen"). La imagen puede actualizarse en cualquier momento desde D2 (detalle de ejercicio) para cualquier ejercicio (seed o custom). Convención de naming para seed: nombre del ejercicio + tipo de equipo, normalizado (lowercase, sin acentos, underscores, sin paréntesis). Ej: `"press_de_banca_maquina"`, `"curl_de_biceps_mancuerna"`. Imagen 3D minimalista con fondo blanco. En futuras iteraciones se podrá migrar a video/GIF (el campo es genérico). Referenciado desde D1 (Diccionario), D2 (Detalle) y E1 (Registro de Series) |

**Restricciones adicionales:**

- `UNIQUE(name, equipment_type_id)` — clave natural compuesta.
- `FOREIGN KEY(equipment_type_id) REFERENCES equipment_type(id) ON DELETE RESTRICT`
- Las flags booleanas son mutuamente consistentes con las reglas del MDS:
  - Si `is_isometric = 1` → `is_bodyweight = 1` (todo isométrico es peso corporal).
  - Si `is_to_technical_failure = 1` → `is_bodyweight = 1` (solo aplica a peso corporal).
  - `is_isometric` y `is_to_technical_failure` son mutuamente excluyentes (ningún ejercicio es ambos).

**Nota sobre incremento de carga:** El incremento mínimo de carga ya no se deriva del módulo sino de la rutina: +2.5 Kg para ejercicios de tren superior (zona muscular ∈ {Pecho, Espalda, Abdomen, Hombro, Tríceps, Bíceps}), +5 Kg para ejercicios de tren inferior (zona muscular ∈ {Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos}). Esta lógica se resuelve en la capa de aplicación a partir de las zonas musculares asociadas al ejercicio en `exercise_muscle_zone`.

**Índices:**

- Índice en `equipment_type_id` para filtrar por equipo (vista D1).

**Seed data (43 filas):** Los ejercicios son agnósticos a la rutina — se asignan libremente a cualquier rutina y versión.

| name | equipment_type | is_bodyweight | is_isometric | is_to_technical_failure |
| ------ | ----------- | :-: | :-: | :-: |
| Remo con Inclinación | Barra de Pesas | 0 | 0 | 0 |
| Remo con un solo brazo doblado | Mancuerna | 0 | 0 | 0 |
| Tiro de dorsales (Agarre ancho) | Máquina | 0 | 0 | 0 |
| Elevación de hombros con mancuernas | Mancuerna | 0 | 0 | 0 |
| Curl de bíceps | Mancuerna | 0 | 0 | 0 |
| Curl de bíceps | Polea | 0 | 0 | 0 |
| Curl de martillo cruzado | Mancuerna | 0 | 0 | 0 |
| Curl de martillo | Mancuerna | 0 | 0 | 0 |
| Curl de Contracción | Mancuerna | 0 | 0 | 0 |
| Abdominales | Cuerpo | 1 | 0 | 0 |
| Escalador | Cuerpo | 1 | 0 | 0 |
| Giro Ruso | Cuerpo | 1 | 0 | 0 |
| Plancha | Cuerpo | 1 | 1 | 0 |
| Plancha Lateral | Cuerpo | 1 | 1 | 0 |
| Press de banca | Máquina | 0 | 0 | 0 |
| Press de mancuerna | Mancuernas | 0 | 0 | 0 |
| Press de banca inclinada | Máquina | 0 | 0 | 0 |
| Flexiones | Cuerpo | 1 | 0 | 1 |
| Cruce en polea alta | Máquina | 0 | 0 | 0 |
| Apertura de pecho sentado | Máquina | 0 | 0 | 0 |
| Apertura de pecho inclinado | Máquina | 0 | 0 | 0 |
| Elevación frontal | Mancuerna | 0 | 0 | 0 |
| Elevación lateral | Mancuerna | 0 | 0 | 0 |
| Press de elevación sentado | Mancuerna | 0 | 0 | 0 |
| Remo vertical | Barra de Pesas | 0 | 0 | 0 |
| Remo vertical con cable | Máquina | 0 | 0 | 0 |
| Dominada de tríceps banco | Pesa | 0 | 0 | 0 |
| Extensión de tríceps por encima de la cabeza | Mancuerna | 0 | 0 | 0 |
| Flexión de tríceps con cuerda | Máquina | 0 | 0 | 0 |
| Extensión de Cuádriceps | Máquina | 0 | 0 | 0 |
| Curl Femoral Tumbado | Máquina | 0 | 0 | 0 |
| Aductor de Cadera | Máquina | 0 | 0 | 0 |
| Abductor de Cadera | Máquina | 0 | 0 | 0 |
| Elevación de Gemelos Sentado | Máquina | 0 | 0 | 0 |
| Empuje de Cadera | Máquina | 0 | 0 | 0 |
| Sentadilla de Sumo | Mancuerna o Pesa Rusa | 0 | 0 | 0 |
| Sentadilla | Cuerpo | 1 | 0 | 0 |
| Sentadilla Búlgara Dividida | Mancuernas | 0 | 0 | 0 |
| Sentadilla | Máquina Multiestación | 0 | 0 | 0 |
| Subir Escalones | Máquina | 0 | 0 | 0 |
| Zancada hacia atrás | Mancuernas | 0 | 0 | 0 |
| Avanzada de Zancadas | Mancuernas | 0 | 0 | 0 |
| Press de Pierna | Máquina | 0 | 0 | 0 |

---

### 3.5. exercise_muscle_zone

**Propósito:** Tabla de unión que resuelve la relación muchos-a-muchos entre `exercise` y `muscle_zone`. Un ejercicio puede trabajar una o múltiples zonas musculares (ej: Sentadilla Búlgara Dividida → Cuádriceps + Glúteos), y una zona muscular es trabajada por múltiples ejercicios.

Esta relación es crítica para:

- Calcular el Tonelaje Acumulado por Grupo Muscular (HU-20, CA-20.02, CA-20.03).
- Calcular la Distribución de Volumen por Zona Muscular (HU-20, CA-20.04).
- Habilitar el filtro por zona muscular en D1 (Diccionario de Ejercicios).

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| exercise_id | INTEGER | NOT NULL, **FK** → `exercise(id)`, **PK** (compuesta) | Ejercicio |
| muscle_zone_id | INTEGER | NOT NULL, **FK** → `muscle_zone(id)`, **PK** (compuesta) | Zona muscular trabajada |

**Restricciones adicionales:**

- `PRIMARY KEY(exercise_id, muscle_zone_id)` — clave primaria compuesta.
- `FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `FOREIGN KEY(muscle_zone_id) REFERENCES muscle_zone(id) ON DELETE RESTRICT`

**Seed data (48 filas):** 38 ejercicios × 1 zona + 5 ejercicios × 2 zonas = 48 filas.

**Ejercicios con múltiples zonas musculares:**

| Ejercicio | Zonas |
| --------- | ----- |
| Sentadilla de Sumo | Cuádriceps, Aductores |
| Sentadilla Búlgara Dividida | Cuádriceps, Glúteos |
| Subir Escalones | Cuádriceps, Glúteos |
| Zancada hacia atrás | Glúteos, Cuádriceps |
| Avanzada de Zancadas | Cuádriceps, Glúteos |

> Los 38 ejercicios restantes tienen exactamente 1 zona muscular asociada.

---

### 3.6. routine_version

**Propósito:** Representa una versión de una rutina del plan de entrenamiento. Las versiones rotan dentro de cada rutina (V1→V2→V3→V1…) para diversificar el estímulo muscular. El ejecutante puede definir cualquier número de versiones por rutina.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| routine_id | INTEGER | NOT NULL, **FK** → `routine(id)` | Rutina a la que pertenece esta versión |
| version_number | INTEGER | NOT NULL | Número de versión: `1`, `2`, `3`, etc. |

**Restricciones adicionales:**

- `UNIQUE(routine_id, version_number)` — no puede haber dos versiones con el mismo número para la misma rutina.
- `FOREIGN KEY(routine_id) REFERENCES routine(id) ON DELETE CASCADE`
- `CHECK(version_number >= 1)`

**Índice:**

- Índice en `routine_id` para consultar las versiones de una rutina.

**Sin seed data fijo.** Las versiones se crean cuando el ejecutante configura su plan (HU-22).

---

### 3.7. plan_assignment

**Propósito:** Define qué ejercicios componen cada combinación rutina-versión, junto con la prescripción de series, repeticiones y orden sugerido de ejecución. Es la tabla del "Plan de Entrenamiento" en forma relacional — la receta que el sistema consulta al iniciar una sesión. Puede crecer con asignaciones del ejecutante (RF63).

Un ejercicio puede aparecer en múltiples versiones de la misma rutina o en rutinas distintas (los ejercicios son agnósticos a la rutina). La prescripción de series (siempre 4) y repeticiones (varía según tipo de ejercicio) se almacena como atributos de la asignación para que el Plan sea autocontenido y consultable sin derivar valores de las propiedades del ejercicio.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| routine_version_id | INTEGER | NOT NULL, **FK** → `routine_version(id)`, **PK** (compuesta) | Combinación rutina-versión |
| exercise_id | INTEGER | NOT NULL, **FK** → `exercise(id)`, **PK** (compuesta) | Ejercicio asignado |
| sets | INTEGER | NOT NULL | Número de series prescritas. Siempre `4` en el MVP |
| reps | TEXT | NOT NULL | Rango de repeticiones prescrito: `"8-12"`, `"TO_TECHNICAL_FAILURE"` o `"30-45_SEC"`. La UI mapea estos valores a texto en español para el ejecutante |
| sort_order | INTEGER | NOT NULL, DEFAULT 0 | Orden sugerido de ejecución dentro de la versión. 1-based secuencial. Para asignaciones manuales se computa como `MAX(sort_order) + 1` |
| slot | INTEGER | NOT NULL, DEFAULT 0 | Número de puesto (slot) dentro de la versión. Múltiples ejercicios pueden compartir el mismo `slot` — son considerados alternativas equivalentes para ese puesto. Se inicializa con `sort_order` al crear la asignación. Agregado en DB v12 (HU-25), corregido en DB v13 |

**Restricciones adicionales:**

- `PRIMARY KEY(routine_version_id, exercise_id)` — clave primaria compuesta. Un ejercicio no puede estar duplicado en la misma versión.
- `FOREIGN KEY(routine_version_id) REFERENCES routine_version(id) ON DELETE CASCADE`
- `FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `CHECK(sets > 0)`

**Nota:** El campo `reps` es `TEXT NOT NULL` sin CHECK constraint en DDL. La validación de valores válidos (`"8-12"`, `"TO_TECHNICAL_FAILURE"`, `"30-45_SEC"`) se realiza en la capa Domain.

**Sin seed data fijo.** Las asignaciones se crean cuando el ejecutante configura su plan (HU-22).

---

### 3.8. profile

**Propósito:** Almacena los datos del ejecutante y su configuración. Es una tabla de fila única — la aplicación es single-user y no soporta múltiples perfiles. Se crea al completar el registro inicial (vista A1) y se actualiza desde C1 (Perfil) y J1 (Ajustes).

El peso corporal actual no se almacena en esta tabla — se obtiene como el registro más reciente de `weight_record`: `SELECT weight_kg FROM weight_record ORDER BY date DESC LIMIT 1`. Al actualizar el peso desde C1, se inserta un nuevo registro en `weight_record` (CA-02.01).

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, DEFAULT 1 | Siempre `1`. Fila única |
| height_m | REAL | NOT NULL | Altura en metros. Validación: > 0 |
| experience_level | TEXT | NOT NULL | Nivel de experiencia: `"BEGINNER"`, `"INTERMEDIATE"`, `"ADVANCED"`. Selección obligatoria sin texto libre (CA-01.03). La UI mapea estos valores a sus equivalentes en español |
| weekly_frequency | INTEGER | NOT NULL, DEFAULT 6 | Objetivo de frecuencia semanal: `4`, `5` o `6` sesiones/semana. Configurable desde J1 (CA-21.05). Usado como denominador del Índice de Adherencia |
| created_at | TEXT | NOT NULL | Fecha de creación del perfil (ISO 8601). Se establece una sola vez al registro inicial |

**Restricciones adicionales:**

- `CHECK(id = 1)` — garantiza fila única.
- `CHECK(height_m > 0)`
- `CHECK(experience_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))`
- `CHECK(weekly_frequency >= 4 AND weekly_frequency <= 6)`

---

### 3.9. weight_record

**Propósito:** Almacena cada registro de peso corporal del ejecutante. Es la fuente de verdad única para el peso — el peso actual se obtiene consultando el registro más reciente. La primera entrada se crea junto con el perfil al completar el onboarding (A1). Cada actualización de peso desde C1 genera una nueva entrada. Las entradas son inmutables — no se eliminan ni se sobrescriben (CA-02.05).

El registro inicial (CA-02.04) se identifica como el registro con la fecha más antigua (`MIN(date)`), sin necesidad de un flag explícito.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| weight_kg | REAL | NOT NULL | Peso registrado en Kg |
| date | TEXT | NOT NULL | Fecha del registro (ISO 8601) |

**Restricciones adicionales:**

- `CHECK(weight_kg > 0)`

**Índices:**

- Índice en `date` para ordenamiento cronológico descendente en C2 y para obtener el peso actual (`ORDER BY date DESC LIMIT 1`).

---

### 3.10. session

**Propósito:** Representa una sesión de entrenamiento — la ejecución de una combinación rutina-versión en una fecha. Es la entidad transaccional central del sistema. Su ciclo de vida es: `IN_PROGRESS` → `COMPLETED` o `INCOMPLETE`. Una vez cerrada, la sesión y todos sus datos son inmutables (Restricción de Integridad del MDS).

Solo puede existir una sesión con estado `IN_PROGRESS` en cualquier momento. Si la app se cierra inesperadamente (crash), la sesión en progreso se detecta al reabrir (crash recovery, RNF10) y se ofrece reanudarla (B1 — Card "Reanudar Sesión").

El tonelaje total de la sesión no se almacena — se calcula como `SUM(exercise_set.weight_kg * exercise_set.reps)` desde las series asociadas vía `session_exercise`.

La relación con descarga se modela mediante `deload_id` (FK nullable) en vez de un boolean. Una sesión pertenece a un ciclo de descarga si `deload_id IS NOT NULL`. Esto permite derivar el conteo de sesiones de la descarga directamente: `SELECT COUNT(*) FROM session WHERE deload_id = ?`.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| routine_version_id | INTEGER | NOT NULL, **FK** → `routine_version(id)` | Combinación rutina-versión entrenada. Determinada por la rotación cíclica al iniciar la sesión |
| deload_id | INTEGER | NULL, **FK** → `deload(id)` | Ciclo de descarga al que pertenece esta sesión. `NULL` si la sesión no es de descarga. Permite derivar si es descarga (`IS NOT NULL`) y contar sesiones del ciclo |
| date | TEXT | NOT NULL | Fecha de la sesión (ISO 8601, `"YYYY-MM-DD"`) |
| status | TEXT | NOT NULL, DEFAULT 'IN_PROGRESS' | Estado del ciclo de vida: `"IN_PROGRESS"`, `"COMPLETED"`, `"INCOMPLETE"` |

**Restricciones adicionales:**

- `FOREIGN KEY(routine_version_id) REFERENCES routine_version(id) ON DELETE RESTRICT`
- `FOREIGN KEY(deload_id) REFERENCES deload(id) ON DELETE RESTRICT`
- `CHECK(status IN ('IN_PROGRESS', 'COMPLETED', 'INCOMPLETE'))`

**Índices:**

- Índice en `date` para ordenamiento cronológico en F1 (Historial de Sesiones).
- Índice en `routine_version_id` para consultas de sesiones por rutina.
- Índice en `status` para localizar rápidamente la sesión activa (`IN_PROGRESS`) en crash recovery.
- Índice en `deload_id` para contar sesiones de un ciclo de descarga.

---

### 3.11. session_exercise

**Propósito:** Representa un ejercicio dentro de una sesión activa. Vincula la sesión con cada ejercicio que la compone y gestiona las sustituciones puntuales y el intercambio de alternativas.

Al iniciar una sesión, se crea una fila por cada slot del plan con el ejercicio primario (el primero por `sort_order`). Todos los ejercicios se crean con `exercise_id` asignado y `pending_selection = 0`. Si el slot tiene alternativas en el plan, el usuario puede intercambiar el ejercicio desde la sesión antes de registrar series (HU-25). Si el ejecutante sustituye un ejercicio antes de registrar series (CA-07.06: solo si 0 series), el campo `original_exercise_id` registra cuál ejercicio del plan fue reemplazado.

El estado del ejercicio dentro de la sesión (No Iniciado / En Ejecución / Completado) se determina por el campo `is_finalized` y el conteo de series: `is_finalized = 1` → Completado, 0 series = No Iniciado, ≥1 serie sin finalizar = En Ejecución.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| session_id | INTEGER | NOT NULL, **FK** → `session(id)` | Sesión a la que pertenece |
| exercise_id | INTEGER | NULL, **FK** → `exercise(id)` | Ejercicio que se ejecuta efectivamente. Siempre tiene valor al crear la sesión (se asigna el ejercicio primario del slot). Si hubo sustitución, es el ejercicio sustituto. Si hubo intercambio de alternativa, es la alternativa seleccionada |
| original_exercise_id | INTEGER | NULL, **FK** → `exercise(id)` | Ejercicio original del plan que fue reemplazado. `NULL` si no hubo sustitución. Cuando no es NULL, indica que `exercise_id` sustituyó a `original_exercise_id` en esta sesión |
| is_finalized | INTEGER | NOT NULL, DEFAULT 0 | `1` si el ejercicio fue finalizado (explícitamente por el ejecutante o automáticamente al cerrar la sesión). Determina el estado Completado. Permite registrar series extra antes de finalizar |
| pending_selection | INTEGER | NOT NULL, DEFAULT 0 | Columna legacy, siempre `0` en la implementación actual. Originalmente diseñada para marcar slots pendientes de selección. Mantenida por compatibilidad de esquema. Agregado en DB v12 (HU-25) |
| slot | INTEGER | NOT NULL, DEFAULT 0 | Número de puesto (slot) al que pertenece esta entrada, correspondiente al `slot` de `plan_assignment`. Permite identificar las alternativas disponibles para mostrar al ejecutante. Agregado en DB v12 (HU-25), corregido en DB v13 |
| progression_classification | TEXT | NULL | Clasificación de progresión asignada al cierre de sesión: `"POSITIVE_PROGRESSION"`, `"MAINTENANCE"`, `"REGRESSION"`. `NULL` si el ejercicio no tiene historial previo (CA-10.07: "Sin Historial", no se emite clasificación). Persistida para consulta futura en historial (CA-10.09, CA-23.01, CA-24.02) y para reglas de decisión (conteo de sesiones sin progresión para detección de mesetas, CA-14.01) |

**Restricciones adicionales:**

- `UNIQUE(session_id, exercise_id)` — un ejercicio no puede aparecer duplicado en la misma sesión. Dado que un ejercicio solo puede pertenecer a un slot por versión de rutina, el intercambio de alternativas dentro de un slot no viola esta restricción.
- `FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE`
- `FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `FOREIGN KEY(original_exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `CHECK(progression_classification IN ('POSITIVE_PROGRESSION', 'MAINTENANCE', 'REGRESSION') OR progression_classification IS NULL)`

**Validación de integridad a nivel de aplicación:**

- El `exercise_id` sustituto debe compartir al menos una zona muscular con el ejercicio original (sustitución por misma zona muscular).
- El `exercise_id` no puede ser un ejercicio que ya esté prescrito en la sesión activa (CA-07.01: excluye los ya prescritos).
- La sustitución solo es posible si el ejercicio tiene 0 series registradas (CA-07.06).
- El intercambio de alternativa (swap) solo es posible si el ejercicio está NOT_STARTED y tiene 0 series registradas. Al intercambiar, `original_exercise_id` se limpia a `NULL` porque no es una sustitución sino un cambio entre ejercicios del plan.

**Índices:**

- Índice en `session_id` para listar ejercicios de una sesión.
- Índice en `exercise_id` para consultas de historial por ejercicio (F3).

---

### 3.12. exercise_set

**Propósito:** Representa una serie individual registrada — la unidad atómica de datos del sistema. Corresponde al concepto "Log" del Manifiesto de Dominio Sistémico: la captura de un set ininterrumpido de repeticiones con peso, cantidad y percepción de esfuerzo (RIR).

Cada serie se registra exactamente una vez y es inmutable tras su creación (Restricción de Integridad del MDS: no duplicados, no edición retroactiva). El número de serie se asigna automáticamente de forma secuencial (CA-06.09).

Los metadatos contextuales (fecha, rutina, versión, ejercicio) se obtienen por la cadena de relaciones: `exercise_set` → `session_exercise` → `session` → `routine_version`.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| session_exercise_id | INTEGER | NOT NULL, **FK** → `session_exercise(id)` | Ejercicio-en-sesión al que pertenece esta serie |
| set_number | INTEGER | NOT NULL | Número secuencial de la serie: `1`, `2`, `3` o `4`. Asignado automáticamente por el sistema (CA-06.09) |
| weight_kg | REAL | NOT NULL | Peso utilizado en Kg. `0` para ejercicios de peso corporal e isométricos (CA-08.01). Validación: ≥ 0 |
| reps | INTEGER | NOT NULL | Repeticiones completadas. Para ejercicios isométricos, representa los segundos sostenidos (CA-08.05). Validación: ≥ 1 |
| rir | INTEGER | NOT NULL | Repeticiones en Reserva (RIR): percepción subjetiva de cuántas repeticiones adicionales podría haber realizado el ejecutante. Rango: `0` a `2` |

**Restricciones adicionales:**

- `UNIQUE(session_exercise_id, set_number)` — un número de serie no puede repetirse para el mismo ejercicio-en-sesión.
- `FOREIGN KEY(session_exercise_id) REFERENCES session_exercise(id) ON DELETE CASCADE`
- `CHECK(set_number >= 1)`
- `CHECK(weight_kg >= 0)`
- `CHECK(reps >= 1)`
- `CHECK(rir >= 0 AND rir <= 2)`

**Índices:**

- Índice en `session_exercise_id` para obtener las series de un ejercicio.

**Nota sobre la columna `reps`:** Para ejercicios estándar y de peso corporal, el valor representa repeticiones. Para ejercicios isométricos (`exercise.is_isometric = 1`), el valor representa segundos sostenidos. La interpretación se resuelve en la capa de aplicación consultando las propiedades del ejercicio asociado. Se usa una sola columna porque el tipo de dato (INTEGER ≥ 1) y las operaciones (suma, comparación) son idénticos en ambos casos.

---

### 3.13. exercise_progression

**Propósito:** Almacena el estado persistente de progresión y la carga prescrita para cada ejercicio. Se crea una fila por ejercicio al registrar la primera serie del ejercicio. Se actualiza automáticamente al cierre de cada sesión por el motor de reglas (CA-10.08).

El ciclo de vida del estado de progresión sigue la máquina de estados del MDS §5.C: `NO_HISTORY` → `IN_PROGRESSION` ⇄ `IN_PLATEAU` → `IN_DELOAD` → `IN_PROGRESSION`. Los ejercicios isométricos pueden alcanzar un estado terminal `MASTERED` (CA-08.07). La carga prescrita se persiste para que al iniciar la próxima sesión (HU-05) se muestre correctamente sin recalcular (CA-11.07).

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| exercise_id | INTEGER | **PK**, **FK** → `exercise(id)` | Ejercicio al que pertenece este estado |
| status | TEXT | NOT NULL, DEFAULT 'NO_HISTORY' | Estado de progresión actual: `"NO_HISTORY"`, `"IN_PROGRESSION"`, `"IN_PLATEAU"`, `"IN_DELOAD"`, `"MASTERED"` (RF43, CA-10.08, CA-08.07) |
| prescribed_load_kg | REAL | NULL | Carga objetivo para la próxima sesión del ejercicio en Kg. Calculada por el motor de Doble Umbral al cierre de sesión y persistida (CA-11.07). `NULL` para ejercicios de peso corporal e isométricos. Tras descarga: 90% de la carga pre-descarga (CA-17.05) |
| sessions_without_progression | INTEGER | NOT NULL, DEFAULT 0 | Contador de sesiones consecutivas sin progresión positiva. Se incrementa al registrar `MAINTENANCE` o `REGRESSION`; se resetea a 0 al registrar `POSITIVE_PROGRESSION`. Umbral de meseta: 3 (CA-14.01). Umbrales de acción escalonada: 4 y 6 (CA-15.01, CA-15.02) |

**Restricciones adicionales:**

- `FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `CHECK(status IN ('NO_HISTORY', 'IN_PROGRESSION', 'IN_PLATEAU', 'IN_DELOAD', 'MASTERED'))`
- `CHECK(prescribed_load_kg >= 0 OR prescribed_load_kg IS NULL)`
- `CHECK(sessions_without_progression >= 0)`

**Lógica de actualización (ejecutada al cierre de cada sesión, por cada ejercicio registrado):**

1. Asignar `progression_classification` en `session_exercise` (POSITIVE_PROGRESSION / MAINTENANCE / REGRESSION / NULL).
2. Si clasificación = `POSITIVE_PROGRESSION`: `sessions_without_progression = 0`, status → `IN_PROGRESSION`.
3. Si clasificación = `MAINTENANCE` o `REGRESSION`: incrementar `sessions_without_progression`. Si llega a 3 → status = `IN_PLATEAU`.
4. Si hay descarga activa: status = `IN_DELOAD`.
5. Si ejercicio isométrico con todas las series prescritas ≥ 45s: status = `MASTERED`.
6. Calcular y persistir `prescribed_load_kg` según Doble Umbral (R1) o mantenimiento (R2).

---

### 3.14. rotation_state

**Propósito:** Tabla de fila única que persiste el estado completo de la rotación cíclica y el conteo de microciclos. La rotación es el mecanismo central del sistema: determina qué rutina-versión corresponde a la próxima sesión y cuándo se ha completado un microciclo.

La secuencia dentro de un microciclo recorre todas las rutinas del plan en su `sort_order`: rutina 1 → rutina 2 → ... → rutina N (N sesiones por microciclo). `microcycle_position` (1-N) determina qué rutina corresponde. Al completar la posición N, el microciclo se cierra, `microcycle_count` se incrementa y las versiones avanzan para el siguiente microciclo.

Este estado es inmune a ausencias del ejecutante (Restricción de Integridad del MDS): si el ejecutante deja de entrenar 2 semanas, al volver la rotación continúa exactamente donde se detuvo.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, DEFAULT 1 | Siempre `1`. Fila única |
| microcycle_position | INTEGER | NOT NULL, DEFAULT 1 | Posición actual en la secuencia de rutinas: `1` a `N` (donde N = número total de rutinas). Determina qué rutina corresponde a la próxima sesión. Avanza en 1 tras cada cierre de sesión. Al llegar a N y cerrar, vuelve a 1 |
| microcycle_count | INTEGER | NOT NULL, DEFAULT 0 | Número de microciclos completados. Se incrementa al cerrar la sesión de la última posición. Mostrado en B1 (HU-18, CA-18.05) |

**Restricciones adicionales:**

- `CHECK(id = 1)` — garantiza fila única.
- `CHECK(microcycle_position >= 1)`
- `CHECK(microcycle_count >= 0)`

**Inicialización:** Se crea junto con el perfil (fila 1 por defecto). Todos los valores iniciales corresponden al inicio del primer microciclo: posición 1, conteo en 0.

**Lógica de avance (ejecutada al cierre de cada sesión):**

1. Si `microcycle_position < N` (N = total de rutinas): incrementar posición en 1.
2. Si `microcycle_position = N`: resetear posición a 1, incrementar `microcycle_count`, avanzar la versión de cada rutina (con wrap-around según cantidad de versiones de la rutina).

---

### 3.14b. routine_current_version

**Propósito:** Persiste la versión actual de cada rutina para el microciclo en curso. Reemplaza las columnas fijas `current_version_module_a/b/c` del modelo anterior, permitiendo un número dinámico de rutinas.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| routine_id | INTEGER | **PK**, **FK** → `routine(id)` | Rutina a la que pertenece este estado |
| current_version_number | INTEGER | NOT NULL, DEFAULT 1 | Número de versión actual de la rutina para el microciclo en curso. Rota V1→V2→…→VN→V1 al inicio de cada nuevo microciclo |

**Restricciones adicionales:**

- `FOREIGN KEY(routine_id) REFERENCES routine(id) ON DELETE CASCADE`
- `CHECK(current_version_number >= 1)`

**Inicialización:** Se crea una fila por rutina con `current_version_number = 1` al crear cada rutina.

---

### 3.15. deload

**Propósito:** Representa un ciclo de descarga (deload). La descarga es un protocolo de reducción temporal de carga que se activa cuando el motor de reglas detecta fatiga acumulada (≥ 50% de ejercicios en meseta/regresión en una rutina) y el ejecutante confirma su activación (la señal es informativa, CA-16.04).

El protocolo dura exactamente 1 microciclo (todas las rutinas del plan). Durante la descarga: cargas al 60% del valor habitual, series prescritas, 8 repeticiones, RIR objetivo 2, y las versiones de cada rutina se congelan (no rotan al cambiar de microciclo). Al completar todas las rutinas, la descarga finaliza y las cargas se reinician al 90% del valor pre-descarga.

Solo puede existir una descarga activa a la vez. Se mantiene el historial de descargas anteriores completadas.

El número de sesiones completadas de la descarga no se almacena — se calcula como `SELECT COUNT(*) FROM session WHERE deload_id = ? AND status IN ('COMPLETED', 'INCOMPLETE')`. Cuando este conteo iguala al número de rutinas, la descarga se marca como `COMPLETED`.

La carga habitual de cada ejercicio antes de la descarga (necesaria para calcular 60% durante y 90% post-descarga) no se almacena — se deriva consultando el último `exercise_set.weight_kg` registrado por ejercicio en sesiones anteriores a `activation_date` que no sean de descarga.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| status | TEXT | NOT NULL, DEFAULT 'ACTIVE' | Estado del ciclo: `"ACTIVE"`, `"COMPLETED"` |
| activation_date | TEXT | NOT NULL | Fecha en que el ejecutante activó la descarga (ISO 8601) |
| completion_date | TEXT | NULL | Fecha en que se completó el ciclo de descarga. `NULL` mientras está activa |

**Restricciones adicionales:**

- `CHECK(status IN ('ACTIVE', 'COMPLETED'))`

**Índices:**

- Índice en `status` para localizar rápidamente la descarga activa.

---

### 3.15b. deload_frozen_version

**Propósito:** Persiste las versiones congeladas de cada rutina al momento de activar una descarga. Reemplaza las columnas fijas `frozen_version_module_a/b/c` del modelo anterior, permitiendo un número dinámico de rutinas.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| deload_id | INTEGER | NOT NULL, **FK** → `deload(id)`, **PK** (compuesta) | Ciclo de descarga |
| routine_id | INTEGER | NOT NULL, **FK** → `routine(id)`, **PK** (compuesta) | Rutina congelada |
| frozen_version_number | INTEGER | NOT NULL | Versión de la rutina congelada al momento de activar la descarga. Se restaura al finalizar |

**Restricciones adicionales:**

- `PRIMARY KEY(deload_id, routine_id)`
- `FOREIGN KEY(deload_id) REFERENCES deload(id) ON DELETE CASCADE`
- `FOREIGN KEY(routine_id) REFERENCES routine(id) ON DELETE RESTRICT`
- `CHECK(frozen_version_number >= 1)`

---

### 3.16. alert

**Propósito:** Persiste las alertas generadas por el motor de reglas. Las alertas son señales informativas (no bloqueantes, CA-15.04) que notifican al ejecutante sobre condiciones que requieren atención: mesetas, fatiga, regresión, baja adherencia, etc.

Las alertas se generan automáticamente al cierre de cada sesión (el motor de reglas evalúa KPIs y condiciones) y se resuelven automáticamente cuando la condición que las disparó deja de cumplirse. El ejecutante puede consultarlas en H1 (Centro de Alertas) y H2 (Detalle de Alerta).

Cada tipo de alerta afecta a una entidad diferente (ejercicio, rutina o grupo muscular). Se usan columnas nullable para referenciar la entidad afectada según el tipo.

Los datos que dispararon la alerta (mostrados en H2) y las recomendaciones escalonadas no se almacenan — se recalculan dinámicamente en la capa de aplicación a partir de las series, sesiones y la lógica del motor de reglas. Dado que las sesiones cerradas son inmutables, el recálculo siempre produce el mismo resultado.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| type | TEXT | NOT NULL | Tipo de alerta. Valores: `"PLATEAU"`, `"LOW_PROGRESSION_RATE"`, `"RIR_OUT_OF_RANGE"`, `"LOW_ADHERENCE"`, `"TONNAGE_DROP"`, `"ROUTINE_INACTIVITY"`, `"ROUTINE_REQUIRES_DELOAD"` |
| level | TEXT | NOT NULL | Nivel de severidad: `"CRISIS"`, `"HIGH_ALERT"`, `"MEDIUM_ALERT"` |
| exercise_id | INTEGER | NULL, **FK** → `exercise(id)` | Ejercicio afectado. Poblado cuando `type` ∈ {`PLATEAU`, `LOW_PROGRESSION_RATE`} |
| routine_id | INTEGER | NULL, **FK** → `routine(id)` | Rutina afectada. Poblado cuando `type` ∈ {`RIR_OUT_OF_RANGE`, `ROUTINE_INACTIVITY`, `ROUTINE_REQUIRES_DELOAD`} |
| muscle_group | TEXT | NULL | Grupo muscular afectado. Poblado cuando `type` = `TONNAGE_DROP`. Valores: los 12 grupos definidos en `muscle_zone.muscle_group` |
| message | TEXT | NOT NULL | Descripción legible de la alerta para mostrar en H1. Ej: `"3 sesiones sin progresión"`, `"RIR 1.2 — riesgo de fatiga"` |
| is_active | INTEGER | NOT NULL, DEFAULT 1 | `1` si la alerta está vigente; `0` si fue resuelta automáticamente. Las alertas resueltas se conservan como histórico |
| created_at | TEXT | NOT NULL | Fecha de generación de la alerta (ISO 8601) |
| resolved_at | TEXT | NULL | Fecha en que la condición dejó de cumplirse y la alerta se resolvió automáticamente. `NULL` mientras está activa |

**Restricciones adicionales:**

- `CHECK(type IN ('PLATEAU', 'LOW_PROGRESSION_RATE', 'RIR_OUT_OF_RANGE', 'LOW_ADHERENCE', 'TONNAGE_DROP', 'ROUTINE_INACTIVITY', 'ROUTINE_REQUIRES_DELOAD'))`
- `CHECK(level IN ('CRISIS', 'HIGH_ALERT', 'MEDIUM_ALERT'))`
- `FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `FOREIGN KEY(routine_id) REFERENCES routine(id) ON DELETE CASCADE`

**Índices:**

- Índice en `is_active` para filtrar alertas vigentes (badge en B1, lista en H1).
- Índice en `type` para agrupación por tipo.
- Índice en `exercise_id` para alertas por ejercicio.
- Índice en `routine_id` para alertas por rutina.

**Tipos de alerta y entidad afectada:**

| Tipo | Entidad afectada | Columna poblada | Niveles posibles |
| ---- | ---------------- | --------------- | ---------------- |
| `PLATEAU` | Ejercicio | `exercise_id` | `HIGH_ALERT` |
| `LOW_PROGRESSION_RATE` | Ejercicio | `exercise_id` | `MEDIUM_ALERT` (< 40%), `CRISIS` (< 20%) |
| `RIR_OUT_OF_RANGE` | Rutina | `routine_id` | `MEDIUM_ALERT` |
| `LOW_ADHERENCE` | Semanal (global) | ninguna | `MEDIUM_ALERT` (1 semana), `CRISIS` (2+ semanas) |
| `TONNAGE_DROP` | Grupo muscular | `muscle_group` | `MEDIUM_ALERT` (> 10%), `CRISIS` (> 20%) |
| `ROUTINE_INACTIVITY` | Rutina | `routine_id` | `MEDIUM_ALERT` (> 10 días), `CRISIS` (> 14 días) |
| `ROUTINE_REQUIRES_DELOAD` | Rutina | `routine_id` | `HIGH_ALERT` |

---

## 4. Relaciones y Cardinalidades

| Relación | Cardinalidad | Descripción |
| -------- | :----------: | ----------- |
| routine → routine_version | 1 : N | Una rutina tiene 1 o más versiones; cada versión pertenece a exactamente 1 rutina |
| routine → routine_current_version | 1 : 1 | Una rutina tiene exactamente 1 estado de versión actual |
| equipment_type → exercise | 1 : N | Un tipo de equipo es usado por varios ejercicios; cada ejercicio usa exactamente 1 tipo |
| exercise ↔ muscle_zone | N : M | Un ejercicio trabaja 1+ zonas musculares; una zona es trabajada por múltiples ejercicios. Resuelta por `exercise_muscle_zone` |
| routine_version → plan_assignment | 1 : N | Una versión prescribe N ejercicios |
| exercise → plan_assignment | 1 : N | Un ejercicio puede estar asignado en múltiples versiones de cualquier rutina |
| routine_version → session | 1 : N | Una combinación rutina-versión puede tener muchas sesiones a lo largo del tiempo; cada sesión pertenece a exactamente 1 rutina-versión |
| session → session_exercise | 1 : N | Una sesión contiene N ejercicios; cada ejercicio-en-sesión pertenece a exactamente 1 sesión |
| exercise → session_exercise | 1 : N | Un ejercicio puede aparecer en múltiples sesiones; `exercise_id` referencia el ejercicio efectivamente ejecutado |
| exercise → session_exercise (original) | 1 : N | Un ejercicio puede haber sido reemplazado en múltiples sesiones; `original_exercise_id` referencia el ejercicio del plan que fue sustituido (nullable) |
| session_exercise → exercise_set | 1 : N | Un ejercicio-en-sesión tiene 0 a N series (prescritas + extra opcionales); cada serie pertenece a exactamente 1 ejercicio-en-sesión |
| exercise → exercise_progression | 1 : 1 | Un ejercicio tiene exactamente 0 o 1 estado de progresión; se crea al primer registro del ejercicio |
| deload → session | 1 : N | Un ciclo de descarga puede tener hasta N sesiones (N = total de rutinas); cada sesión de descarga pertenece a exactamente 1 ciclo (nullable — solo sesiones de descarga) |
| deload → deload_frozen_version | 1 : N | Un ciclo de descarga congela la versión de cada rutina; cada versión congelada pertenece a exactamente 1 ciclo |
| routine → deload_frozen_version | 1 : N | Una rutina puede tener versiones congeladas en múltiples ciclos de descarga |
| exercise → alert | 1 : N | Un ejercicio puede tener múltiples alertas (meseta, progresión baja); cada alerta de ejercicio referencia exactamente 1 ejercicio (nullable) |
| routine → alert | 1 : N | Una rutina puede tener múltiples alertas (RIR, inactividad, descarga); cada alerta de rutina referencia exactamente 1 rutina (nullable) |

---

## 5. Diagrama Entidad-Relación

```mermaid
erDiagram
    routine {
        INTEGER id PK
        TEXT name UK
        INTEGER sort_order
        TEXT created_at
    }

    muscle_zone {
        INTEGER id PK
        TEXT name UK
        TEXT muscle_group
    }

    equipment_type {
        INTEGER id PK
        TEXT name UK
    }

    exercise {
        INTEGER id PK
        TEXT name
        INTEGER equipment_type_id FK
        INTEGER is_bodyweight
        INTEGER is_isometric
        INTEGER is_to_technical_failure
        INTEGER is_custom
        TEXT media_resource
    }

    exercise_muscle_zone {
        INTEGER exercise_id PK_FK
        INTEGER muscle_zone_id PK_FK
    }

    routine_version {
        INTEGER id PK
        INTEGER routine_id FK
        INTEGER version_number
    }

    plan_assignment {
        INTEGER routine_version_id PK_FK
        INTEGER exercise_id PK_FK
        INTEGER sets
        TEXT reps
        INTEGER sort_order
        INTEGER slot
    }

    profile {
        INTEGER id PK "siempre 1"
        REAL height_m
        TEXT experience_level
        INTEGER weekly_frequency
        TEXT created_at
    }

    weight_record {
        INTEGER id PK
        REAL weight_kg
        TEXT date
    }

    session {
        INTEGER id PK
        INTEGER routine_version_id FK
        INTEGER deload_id FK "nullable"
        TEXT date
        TEXT status
    }

    session_exercise {
        INTEGER id PK
        INTEGER session_id FK
        INTEGER exercise_id FK
        INTEGER original_exercise_id FK "nullable"
        INTEGER is_finalized
        INTEGER pending_selection
        INTEGER slot
        TEXT progression_classification "nullable"
    }

    exercise_set {
        INTEGER id PK
        INTEGER session_exercise_id FK
        INTEGER set_number
        REAL weight_kg
        INTEGER reps
        INTEGER rir
    }

    rotation_state {
        INTEGER id PK "siempre 1"
        INTEGER microcycle_position
        INTEGER microcycle_count
    }

    routine_current_version {
        INTEGER routine_id PK_FK
        INTEGER current_version_number
    }

    deload {
        INTEGER id PK
        TEXT status
        TEXT activation_date
        TEXT completion_date "nullable"
    }

    deload_frozen_version {
        INTEGER deload_id PK_FK
        INTEGER routine_id PK_FK
        INTEGER frozen_version_number
    }

    exercise_progression {
        INTEGER exercise_id PK_FK
        TEXT status
        REAL prescribed_load_kg "nullable"
        INTEGER sessions_without_progression
    }

    alert {
        INTEGER id PK
        TEXT type
        TEXT level
        INTEGER exercise_id FK "nullable"
        INTEGER routine_id FK "nullable"
        TEXT muscle_group "nullable"
        TEXT message
        INTEGER is_active
        TEXT created_at
        TEXT resolved_at "nullable"
    }

    routine ||--o{ routine_version : "has versions"
    routine ||--|| routine_current_version : "current version"
    equipment_type ||--o{ exercise : "used by"
    exercise ||--o{ exercise_muscle_zone : "works"
    muscle_zone ||--o{ exercise_muscle_zone : "worked by"
    routine_version ||--o{ plan_assignment : "prescribes"
    exercise ||--o{ plan_assignment : "assigned in"
    routine_version ||--o{ session : "executed in"
    session ||--o{ session_exercise : "contains"
    exercise ||--o{ session_exercise : "executed in"
    exercise ||--o{ session_exercise : "substituted in"
    session_exercise ||--o{ exercise_set : "records"
    exercise ||--|| exercise_progression : "progression of"
    deload ||--o{ session : "contains"
    deload ||--o{ deload_frozen_version : "freezes"
    routine ||--o{ deload_frozen_version : "frozen in"
    exercise ||--o{ alert : "alerted on"
    routine ||--o{ alert : "alerted on"
```
