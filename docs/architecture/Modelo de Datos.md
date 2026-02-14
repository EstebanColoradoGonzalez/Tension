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

| Convención | Descripción |
| ---------- | ----------- |
| **Nombres de tabla** | snake_case en inglés (ej: `module_version`, `plan_assignment`) |
| **Nombres de columna** | snake_case en inglés (ej: `module_code`, `equipment_type_id`) |
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

### 3.1. module

**Propósito:** Representa los 3 módulos de entrenamiento que dividen el trabajo muscular. Es la entidad raíz del catálogo — todo ejercicio pertenece a exactamente un módulo, y la rotación cíclica opera sobre estos 3 módulos.

**Clave natural como PK:** Se usa `code TEXT` como clave primaria en lugar de un entero autoincremental. Justificación: solo existen 3 filas inmutables ("A", "B", "C"), son identificadores naturales del dominio usados en toda la documentación, y como FK hacen que los datos sean legibles sin necesidad de joins adicionales.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| code | TEXT | **PK** | Identificador del módulo: `"A"`, `"B"`, `"C"` |
| name | TEXT | NOT NULL | Nombre descriptivo: `"Módulo A — Superior"`, `"Módulo B — Superior"`, `"Módulo C — Inferior"` |
| group_description | TEXT | NOT NULL | Grupos musculares cubiertos: `"Pecho, Espalda, Abdomen"`, `"Hombro, Tríceps, Bíceps"`, `"Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos"` |
| load_increment_kg | REAL | NOT NULL | Incremento mínimo de carga prescrito por la Regla de Doble Umbral (R1): `2.5` para A y B (tren superior), `5.0` para C (tren inferior) |

**Restricciones adicionales:**

- `CHECK(code IN ('A', 'B', 'C'))`
- `CHECK(load_increment_kg > 0)`

**Seed data (3 filas):**

| code | name | group_description | load_increment_kg |
| ---- | ---- | ----------------- | ----------------- |
| A | Módulo A — Superior | Pecho, Espalda, Abdomen | 2.5 |
| B | Módulo B — Superior | Hombro, Tríceps, Bíceps | 2.5 |
| C | Módulo C — Inferior | Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos | 5.0 |

---

### 3.2. muscle_zone

**Propósito:** Catálogo de las 15 zonas musculares específicas que clasifican cada ejercicio. Cada zona pertenece a un grupo muscular más amplio (12 grupos) que se usa para la agregación de KPIs (tonelaje, distribución de volumen, tendencias).

**Jerarquía zona → grupo:** Las zonas con subvariantes (Pecho Medio, Pecho Superior, Pecho Inferior) se agrupan bajo un grupo muscular padre (Pecho). Para los 10 grupos restantes (Abdomen, Hombro, Tríceps, Bíceps, etc.), la zona y el grupo comparten el mismo nombre (relación 1:1).

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| name | TEXT | NOT NULL, UNIQUE | Nombre específico de la zona: `"Pecho Medio"`, `"Dorsal Ancho"`, `"Cuádriceps"`, etc. |
| muscle_group | TEXT | NOT NULL | Grupo muscular padre para agregación de KPIs. Valores posibles: `"Pecho"`, `"Espalda"`, `"Abdomen"`, `"Hombro"`, `"Tríceps"`, `"Bíceps"`, `"Cuádriceps"`, `"Isquiotibiales"`, `"Glúteos"`, `"Aductores"`, `"Abductores"`, `"Gemelos"` |

**Índices:**

- Índice en `muscle_group` para consultas de agregación de KPIs.

**Seed data (15 filas):**

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

---

### 3.3. equipment_type

**Propósito:** Catálogo de los 9 tipos de equipamiento utilizados por los ejercicios. Normaliza el valor repetido en `exercise` y habilita el filtro por equipo en la vista D1 (Diccionario de Ejercicios).

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| name | TEXT | NOT NULL, UNIQUE | Nombre del tipo de equipo |

**Seed data (9 filas):**

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

**Nota sobre `Mancuerna` vs `Mancuernas`:** Son tipos de equipo distintos en el dominio. `Mancuerna` (singular) indica que el ejercicio se ejecuta con una mancuerna. `Mancuernas` (plural) indica que se usan dos mancuernas simultáneamente. El Diccionario de Ejercicios los trata como entradas distintas.

---

### 3.4. exercise

**Propósito:** Catálogo de ejercicios del sistema. Contiene un conjunto base de 43 ejercicios precargados (seed data) y puede ser ampliado por el ejecutante con ejercicios propios (RF62). Es la entidad central — referenciada por el plan de entrenamiento, las sesiones, los registros de series y los KPIs. Todo ejercicio registrado en una sesión debe existir en este catálogo (Restricción de Integridad #2 del MDS).

**Clave natural compuesta:** La combinación `(name, equipment_type_id)` es única. Dos ejercicios pueden compartir nombre si difieren en tipo de equipo (ej: `"Sentadilla"` con `Cuerpo` vs `"Sentadilla"` con `Máquina Multiestación`). Se usa un `id` autoincremental como PK para eficiencia en FKs.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| name | TEXT | NOT NULL | Nombre del ejercicio: `"Press de banca"`, `"Flexiones"`, etc. |
| module_code | TEXT | NOT NULL, **FK** → `module(code)` | Módulo al que pertenece el ejercicio. Determina elegibilidad de sustitución (mismo módulo) e incremento de carga |
| equipment_type_id | INTEGER | NOT NULL, **FK** → `equipment_type(id)` | Tipo de equipamiento requerido |
| is_bodyweight | INTEGER | NOT NULL, DEFAULT 0 | `1` si el ejercicio usa peso corporal (Peso fijo = 0 Kg). Se aplica R6: progresión por repeticiones totales, sin Doble Umbral |
| is_isometric | INTEGER | NOT NULL, DEFAULT 0 | `1` si el ejercicio es isométrico (Plancha, Plancha Lateral). Se aplica R7: progresión por segundos (30-45s), "dominado" cuando 4 series ≥ 45s |
| is_to_technical_failure | INTEGER | NOT NULL, DEFAULT 0 | `1` si el rango de repeticiones es "Al fallo técnico" (solo Flexiones). Sin límite superior de repeticiones |
| is_custom | INTEGER | NOT NULL, DEFAULT 0 | `1` si el ejercicio fue creado por el ejecutante (RF62). `0` para los 43 ejercicios precargados (seed data). Los ejercicios custom pueden tener imagen seleccionada desde la galería del dispositivo (almacenada en `filesDir/exercise_images/`) o no (se muestra logo placeholder con opción de agregar) |
| media_resource | TEXT | NULL | Para ejercicios seed: nombre normalizado del asset multimedia empaquetado en el APK que ilustra la ejecución correcta del ejercicio (RF61), cargado desde `assets/exercises/module-{code}/{media_resource}.png`. Para ejercicios custom o ejercicios con imagen personalizada: ruta absoluta al archivo de imagen en almacenamiento interno (`filesDir/exercise_images/exercise_{UUID}.jpg`). `NULL` cuando no se ha asociado imagen (se muestra logo de la app como placeholder con ícono AddAPhoto y texto "Toca para agregar imagen"). La imagen puede actualizarse en cualquier momento desde D2 (detalle de ejercicio) para cualquier ejercicio (seed o custom). Convención de naming para seed: nombre del ejercicio + tipo de equipo, normalizado (lowercase, sin acentos, underscores, sin paréntesis). Ej: `"press_de_banca_maquina"`, `"curl_de_biceps_mancuerna"`. Imagen 3D minimalista con fondo blanco. En futuras iteraciones se podrá migrar a video/GIF (el campo es genérico). Referenciado desde D1 (Diccionario), D2 (Detalle) y E1 (Registro de Series) |

**Restricciones adicionales:**

- `UNIQUE(name, equipment_type_id)` — clave natural compuesta.
- `FOREIGN KEY(module_code) REFERENCES module(code) ON DELETE RESTRICT`
- `FOREIGN KEY(equipment_type_id) REFERENCES equipment_type(id) ON DELETE RESTRICT`
- Las flags booleanas son mutuamente consistentes con las reglas del MDS:
  - Si `is_isometric = 1` → `is_bodyweight = 1` (todo isométrico es peso corporal).
  - Si `is_to_technical_failure = 1` → `is_bodyweight = 1` (solo aplica a peso corporal).
  - `is_isometric` y `is_to_technical_failure` son mutuamente excluyentes (ningún ejercicio es ambos).

**Índices:**

- Índice en `module_code` para filtrar ejercicios por módulo (vista D1, sustitución E3).
- Índice en `equipment_type_id` para filtrar por equipo (vista D1).

**Seed data (43 filas):**

**Módulo A (15 ejercicios):**

| name | equipment_type | is_bodyweight | is_isometric | is_to_technical_failure |
| ------ | ----------- | :-: | :-: | :-: |
| Press de banca | Máquina | 0 | 0 | 0 |
| Press de mancuerna | Mancuernas | 0 | 0 | 0 |
| Press de banca inclinada | Máquina | 0 | 0 | 0 |
| Flexiones | Cuerpo | 1 | 0 | 1 |
| Cruce en polea alta | Máquina | 0 | 0 | 0 |
| Apertura de pecho sentado | Máquina | 0 | 0 | 0 |
| Apertura de pecho inclinado | Máquina | 0 | 0 | 0 |
| Remo con Inclinación | Barra de Pesas | 0 | 0 | 0 |
| Remo con un solo brazo doblado | Mancuerna | 0 | 0 | 0 |
| Tiro de dorsales (Agarre ancho) | Máquina | 0 | 0 | 0 |
| Abdominales | Cuerpo | 1 | 0 | 0 |
| Escalador | Cuerpo | 1 | 0 | 0 |
| Giro Ruso | Cuerpo | 1 | 0 | 0 |
| Plancha | Cuerpo | 1 | 1 | 0 |
| Plancha Lateral | Cuerpo | 1 | 1 | 0 |

**Módulo B (14 ejercicios):**

| name | equipment_type | is_bodyweight | is_isometric | is_to_technical_failure |
| ------ | ----------- | :-: | :-: | :-: |
| Curl de bíceps | Mancuerna | 0 | 0 | 0 |
| Curl de bíceps | Polea | 0 | 0 | 0 |
| Curl de martillo cruzado | Mancuerna | 0 | 0 | 0 |
| Curl de martillo | Mancuerna | 0 | 0 | 0 |
| Curl de Contracción | Mancuerna | 0 | 0 | 0 |
| Dominada de tríceps banco | Pesa | 0 | 0 | 0 |
| Extensión de tríceps por encima de la cabeza | Mancuerna | 0 | 0 | 0 |
| Flexión de tríceps con cuerda | Máquina | 0 | 0 | 0 |
| Elevación frontal | Mancuerna | 0 | 0 | 0 |
| Elevación lateral | Mancuerna | 0 | 0 | 0 |
| Elevación de hombros con mancuernas | Mancuerna | 0 | 0 | 0 |
| Press de elevación sentado | Mancuerna | 0 | 0 | 0 |
| Remo vertical | Barra de Pesas | 0 | 0 | 0 |
| Remo vertical con cable | Máquina | 0 | 0 | 0 |

**Módulo C (14 ejercicios):**

| name | equipment_type | is_bodyweight | is_isometric | is_to_technical_failure |
| ------ | ----------- | :-: | :-: | :-: |
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

**Ejercicios con múltiples zonas musculares (todos en Módulo C):**

| Ejercicio | Zonas |
| --------- | ----- |
| Sentadilla de Sumo | Cuádriceps, Aductores |
| Sentadilla Búlgara Dividida | Cuádriceps, Glúteos |
| Subir Escalones | Cuádriceps, Glúteos |
| Zancada hacia atrás | Glúteos, Cuádriceps |
| Avanzada de Zancadas | Cuádriceps, Glúteos |

> Los 38 ejercicios restantes tienen exactamente 1 zona muscular asociada.

---

### 3.6. module_version

**Propósito:** Representa las 9 combinaciones módulo-versión que estructuran el plan de entrenamiento. Las versiones rotan dentro de cada módulo (V1→V2→V3→V1) para diversificar el estímulo muscular.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| module_code | TEXT | NOT NULL, **FK** → `module(code)` | Módulo al que pertenece esta versión |
| version_number | INTEGER | NOT NULL | Número de versión: `1`, `2` o `3` |

**Restricciones adicionales:**

- `UNIQUE(module_code, version_number)` — no puede haber dos versiones con el mismo número para el mismo módulo.
- `FOREIGN KEY(module_code) REFERENCES module(code) ON DELETE RESTRICT`
- `CHECK(version_number >= 1 AND version_number <= 3)`

**Índice:**

- Índice en `module_code` para consultar las versiones de un módulo.

**Seed data (9 filas):**

| id | module_code | version_number |
| -- | ----------- | -------------- |
| 1 | A | 1 |
| 2 | A | 2 |
| 3 | A | 3 |
| 4 | B | 1 |
| 5 | B | 2 |
| 6 | B | 3 |
| 7 | C | 1 |
| 8 | C | 2 |
| 9 | C | 3 |

---

### 3.7. plan_assignment

**Propósito:** Define qué ejercicios componen cada combinación módulo-versión, junto con la prescripción de series y repeticiones. Es la tabla del "Plan de Entrenamiento" en forma relacional — la receta que el sistema consulta al iniciar una sesión. Contiene 93 filas de seed data (plan precargado) y puede crecer con asignaciones del ejecutante (RF63).

Un ejercicio puede aparecer en múltiples versiones del mismo módulo (los ejercicios "fijos" se repiten en todas las versiones). La prescripción de series (siempre 4) y repeticiones (varía según tipo de ejercicio) se almacena como atributos de la asignación para que el Plan sea autocontenido y consultable sin derivar valores de las propiedades del ejercicio.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| module_version_id | INTEGER | NOT NULL, **FK** → `module_version(id)`, **PK** (compuesta) | Combinación módulo-versión |
| exercise_id | INTEGER | NOT NULL, **FK** → `exercise(id)`, **PK** (compuesta) | Ejercicio asignado |
| sets | INTEGER | NOT NULL | Número de series prescritas. Siempre `4` en el MVP |
| reps | TEXT | NOT NULL | Rango de repeticiones prescrito: `"8-12"`, `"TO_TECHNICAL_FAILURE"` o `"30-45_SEC"`. La UI mapea estos valores a texto en español para el ejecutante |

**Restricciones adicionales:**

- `PRIMARY KEY(module_version_id, exercise_id)` — clave primaria compuesta. Un ejercicio no puede estar duplicado en la misma versión.
- `FOREIGN KEY(module_version_id) REFERENCES module_version(id) ON DELETE RESTRICT`
- `FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `CHECK(sets > 0)`
- `CHECK(reps IN ('8-12', 'TO_TECHNICAL_FAILURE', '30-45_SEC'))`

**Validación de integridad cruzada:** El `exercise_id` referenciado debe pertenecer al mismo módulo que el `module_version_id`. Esta restricción no es expresable como CHECK en SQLite y se valida a nivel de la aplicación o del seed data.

**Seed data (93 filas):** A: 3 versiones × 11 ejercicios = 33; B: 3 versiones × 11 ejercicios = 33; C: 3 versiones × 9 ejercicios = 27.

**Ejemplo — Módulo A, Versión 1 (11 filas):**

| module_version_id | exercise (nombre referencial) | sets | reps |
| ----------------- | ----------------------------- | ---- | ---- |
| 1 (A-V1) | Press de banca | 4 | 8-12 |
| 1 (A-V1) | Press de banca inclinada | 4 | 8-12 |
| 1 (A-V1) | Apertura de pecho sentado | 4 | 8-12 |
| 1 (A-V1) | Flexiones | 4 | TO_TECHNICAL_FAILURE |
| 1 (A-V1) | Tiro de dorsales (Agarre ancho) | 4 | 8-12 |
| 1 (A-V1) | Remo con Inclinación | 4 | 8-12 |
| 1 (A-V1) | Remo con un solo brazo doblado | 4 | 8-12 |
| 1 (A-V1) | Abdominales | 4 | 8-12 |
| 1 (A-V1) | Escalador | 4 | 8-12 |
| 1 (A-V1) | Giro Ruso | 4 | 8-12 |
| 1 (A-V1) | Plancha | 4 | 30-45_SEC |

> Las 93 filas completas del seed data están determinadas por el documento "Plan de Entrenamiento" y se implementan en el `RoomDatabase.Callback`.

---

### 3.8. profile

**Propósito:** Almacena los datos del ejecutante y su configuración. Es una tabla de fila única — la aplicación es single-user y no soporta múltiples perfiles. Se crea al completar el registro inicial (vista A1) y se actualiza desde C1 (Perfil) y J1 (Configuración).

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

**Propósito:** Representa una sesión de entrenamiento — la ejecución de una combinación módulo-versión en una fecha. Es la entidad transaccional central del sistema. Su ciclo de vida es: `IN_PROGRESS` → `COMPLETED` o `INCOMPLETE`. Una vez cerrada, la sesión y todos sus datos son inmutables (Restricción de Integridad del MDS).

Solo puede existir una sesión con estado `IN_PROGRESS` en cualquier momento. Si la app se cierra inesperadamente (crash), la sesión en progreso se detecta al reabrir (crash recovery, RNF10) y se ofrece reanudarla (B1 — Card "Reanudar Sesión").

El tonelaje total de la sesión no se almacena — se calcula como `SUM(exercise_set.weight_kg * exercise_set.reps)` desde las series asociadas vía `session_exercise`.

La relación con descarga se modela mediante `deload_id` (FK nullable) en vez de un boolean. Una sesión pertenece a un ciclo de descarga si `deload_id IS NOT NULL`. Esto permite derivar el conteo de sesiones de la descarga directamente: `SELECT COUNT(*) FROM session WHERE deload_id = ?`.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| module_version_id | INTEGER | NOT NULL, **FK** → `module_version(id)` | Combinación módulo-versión entrenada. Determinada por la rotación cíclica al iniciar la sesión |
| deload_id | INTEGER | NULL, **FK** → `deload(id)` | Ciclo de descarga al que pertenece esta sesión. `NULL` si la sesión no es de descarga. Permite derivar si es descarga (`IS NOT NULL`) y contar sesiones del ciclo |
| date | TEXT | NOT NULL | Fecha de la sesión (ISO 8601, `"YYYY-MM-DD"`) |
| status | TEXT | NOT NULL, DEFAULT 'IN_PROGRESS' | Estado del ciclo de vida: `"IN_PROGRESS"`, `"COMPLETED"`, `"INCOMPLETE"` |

**Restricciones adicionales:**

- `FOREIGN KEY(module_version_id) REFERENCES module_version(id) ON DELETE RESTRICT`
- `FOREIGN KEY(deload_id) REFERENCES deload(id) ON DELETE RESTRICT`
- `CHECK(status IN ('IN_PROGRESS', 'COMPLETED', 'INCOMPLETE'))`

**Índices:**

- Índice en `date` para ordenamiento cronológico en F1 (Historial de Sesiones).
- Índice en `module_version_id` para consultas de sesiones por módulo.
- Índice en `status` para localizar rápidamente la sesión activa (`IN_PROGRESS`) en crash recovery.
- Índice en `deload_id` para contar sesiones de un ciclo de descarga.

---

### 3.11. session_exercise

**Propósito:** Representa un ejercicio dentro de una sesión activa. Vincula la sesión con cada ejercicio que la compone y gestiona las sustituciones puntuales.

Al iniciar una sesión, se crea una fila por cada ejercicio prescrito en `plan_assignment` para el `module_version_id` correspondiente. Si el ejecutante sustituye un ejercicio antes de registrar series (CA-07.06: solo si 0 series), el campo `original_exercise_id` registra cuál ejercicio del plan fue reemplazado.

El estado del ejercicio dentro de la sesión (No Iniciado / En Ejecución / Completado) **no se almacena** — se deriva en la capa de aplicación contando las series asociadas: 0 series = No Iniciado, 1-3 series = En Ejecución, 4 series = Completado.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| session_id | INTEGER | NOT NULL, **FK** → `session(id)` | Sesión a la que pertenece |
| exercise_id | INTEGER | NOT NULL, **FK** → `exercise(id)` | Ejercicio que se ejecuta efectivamente. Si no hubo sustitución, es el ejercicio del plan. Si hubo sustitución, es el ejercicio sustituto |
| original_exercise_id | INTEGER | NULL, **FK** → `exercise(id)` | Ejercicio original del plan que fue reemplazado. `NULL` si no hubo sustitución. Cuando no es NULL, indica que `exercise_id` sustituyó a `original_exercise_id` en esta sesión |
| progression_classification | TEXT | NULL | Clasificación de progresión asignada al cierre de sesión: `"POSITIVE_PROGRESSION"`, `"MAINTENANCE"`, `"REGRESSION"`. `NULL` si el ejercicio no tiene historial previo (CA-10.07: "Sin Historial", no se emite clasificación). Persistida para consulta futura en historial (CA-10.09, CA-23.01, CA-24.02) y para reglas de decisión (conteo de sesiones sin progresión para detección de mesetas, CA-14.01) |

**Restricciones adicionales:**

- `UNIQUE(session_id, exercise_id)` — un ejercicio no puede aparecer duplicado en la misma sesión.
- `FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE`
- `FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `FOREIGN KEY(original_exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `CHECK(progression_classification IN ('POSITIVE_PROGRESSION', 'MAINTENANCE', 'REGRESSION') OR progression_classification IS NULL)`

**Validación de integridad a nivel de aplicación:**

- El `exercise_id` debe pertenecer al mismo módulo que el `module_version_id` de la sesión (CA-07.01: sustitución solo del mismo módulo).
- El `exercise_id` no puede ser un ejercicio que ya esté prescrito en la sesión activa (CA-07.01: excluye los ya prescritos).
- La sustitución solo es posible si el ejercicio tiene 0 series registradas (CA-07.06).

**Índices:**

- Índice en `session_id` para listar ejercicios de una sesión.
- Índice en `exercise_id` para consultas de historial por ejercicio (F3).

---

### 3.12. exercise_set

**Propósito:** Representa una serie individual registrada — la unidad atómica de datos del sistema. Corresponde al concepto "Log" del Manifiesto de Dominio Sistémico: la captura de un set ininterrumpido de repeticiones con peso, cantidad y percepción de esfuerzo (RIR).

Cada serie se registra exactamente una vez y es inmutable tras su creación (Restricción de Integridad del MDS: no duplicados, no edición retroactiva). El número de serie se asigna automáticamente de forma secuencial (CA-06.09).

Los metadatos contextuales (fecha, módulo, versión, ejercicio) se obtienen por la cadena de relaciones: `exercise_set` → `session_exercise` → `session` → `module_version`.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| session_exercise_id | INTEGER | NOT NULL, **FK** → `session_exercise(id)` | Ejercicio-en-sesión al que pertenece esta serie |
| set_number | INTEGER | NOT NULL | Número secuencial de la serie: `1`, `2`, `3` o `4`. Asignado automáticamente por el sistema (CA-06.09) |
| weight_kg | REAL | NOT NULL | Peso utilizado en Kg. `0` para ejercicios de peso corporal e isométricos (CA-08.01). Validación: ≥ 0 |
| reps | INTEGER | NOT NULL | Repeticiones completadas. Para ejercicios isométricos, representa los segundos sostenidos (CA-08.05). Validación: ≥ 1 |
| rir | INTEGER | NOT NULL | Repeticiones en Reserva (RIR): percepción subjetiva de cuántas repeticiones adicionales podría haber realizado el ejecutante. Rango: `0` a `5` |

**Restricciones adicionales:**

- `UNIQUE(session_exercise_id, set_number)` — un número de serie no puede repetirse para el mismo ejercicio-en-sesión.
- `FOREIGN KEY(session_exercise_id) REFERENCES session_exercise(id) ON DELETE CASCADE`
- `CHECK(set_number >= 1 AND set_number <= 4)`
- `CHECK(weight_kg >= 0)`
- `CHECK(reps >= 1)`
- `CHECK(rir >= 0 AND rir <= 5)`

**Índices:**

- Índice en `session_exercise_id` para obtener las 4 series de un ejercicio.

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
5. Si ejercicio isométrico con 4 series ≥ 45s: status = `MASTERED`.
6. Calcular y persistir `prescribed_load_kg` según Doble Umbral (R1) o mantenimiento (R2).

---

### 3.14. rotation_state

**Propósito:** Tabla de fila única que persiste el estado completo de la rotación cíclica y el conteo de microciclos. La rotación es el mecanismo central del sistema: determina qué módulo-versión corresponde a la próxima sesión y cuándo se ha completado un microciclo.

La secuencia dentro de un microciclo es fija: A → B → C → A → B → C (6 sesiones). `microcycle_position` (1-6) determina qué módulo corresponde: posiciones 1,4 = A; 2,5 = B; 3,6 = C. Al completar la posición 6, el microciclo se cierra, `microcycle_count` se incrementa y las versiones avanzan para el siguiente microciclo.

Este estado es inmune a ausencias del ejecutante (Restricción de Integridad del MDS): si el ejecutante deja de entrenar 2 semanas, al volver la rotación continúa exactamente donde se detuvo.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, DEFAULT 1 | Siempre `1`. Fila única |
| microcycle_position | INTEGER | NOT NULL, DEFAULT 1 | Posición actual en la secuencia A-B-C-A-B-C: `1` a `6`. Determina qué módulo corresponde a la próxima sesión. Avanza en 1 tras cada cierre de sesión. Al llegar a 6 y cerrar, vuelve a 1 |
| current_version_module_a | INTEGER | NOT NULL, DEFAULT 1 | Versión actual del Módulo A para el microciclo en curso. Rota V1→V2→V3→V1 al inicio de cada nuevo microciclo |
| current_version_module_b | INTEGER | NOT NULL, DEFAULT 1 | Versión actual del Módulo B para el microciclo en curso. Rota V1→V2→V3→V1 |
| current_version_module_c | INTEGER | NOT NULL, DEFAULT 1 | Versión actual del Módulo C para el microciclo en curso. Rota V1→V2→V3→V1 |
| microcycle_count | INTEGER | NOT NULL, DEFAULT 0 | Número de microciclos completados. Se incrementa al cerrar la sesión de posición 6. Mostrado en B1 (HU-18, CA-18.05) |

**Restricciones adicionales:**

- `CHECK(id = 1)` — garantiza fila única.
- `CHECK(microcycle_position >= 1 AND microcycle_position <= 6)`
- `CHECK(current_version_module_a >= 1 AND current_version_module_a <= 3)`
- `CHECK(current_version_module_b >= 1 AND current_version_module_b <= 3)`
- `CHECK(current_version_module_c >= 1 AND current_version_module_c <= 3)`
- `CHECK(microcycle_count >= 0)`

**Inicialización:** Se crea junto con el perfil (fila 1 por defecto). Todos los valores iniciales corresponden al inicio del primer microciclo: posición 1, todas las versiones en 1, conteo en 0.

**Lógica de avance (ejecutada al cierre de cada sesión):**

1. Si `microcycle_position < 6`: incrementar posición en 1.
2. Si `microcycle_position = 6`: resetear posición a 1, incrementar `microcycle_count`, avanzar la versión de cada módulo (con wrap-around según cantidad de versiones del módulo).

---

### 3.15. deload

**Propósito:** Representa un ciclo de descarga (deload). La descarga es un protocolo de reducción temporal de carga que se activa cuando el motor de reglas detecta fatiga acumulada (≥ 50% de ejercicios en meseta/regresión) y el ejecutante confirma su activación (la señal es informativa, CA-16.04).

El protocolo dura exactamente 1 microciclo (6 sesiones). Durante la descarga: cargas al 60% del valor habitual, 4 series, 8 repeticiones, RIR objetivo 4-5, y las versiones de cada módulo se congelan (no rotan al cambiar de microciclo). Al completar las 6 sesiones, la descarga finaliza y las cargas se reinician al 90% del valor pre-descarga.

Solo puede existir una descarga activa a la vez. Se mantiene el historial de descargas anteriores completadas.

El número de sesiones completadas de la descarga no se almacena — se calcula como `SELECT COUNT(*) FROM session WHERE deload_id = ? AND status IN ('COMPLETED', 'INCOMPLETE')`. Cuando este conteo llega a 6, la descarga se marca como `COMPLETED`.

La carga habitual de cada ejercicio antes de la descarga (necesaria para calcular 60% durante y 90% post-descarga) no se almacena — se deriva consultando el último `exercise_set.weight_kg` registrado por ejercicio en sesiones anteriores a `activation_date` que no sean de descarga.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| status | TEXT | NOT NULL, DEFAULT 'ACTIVE' | Estado del ciclo: `"ACTIVE"`, `"COMPLETED"` |
| activation_date | TEXT | NOT NULL | Fecha en que el ejecutante activó la descarga (ISO 8601) |
| completion_date | TEXT | NULL | Fecha en que se completó el ciclo de 6 sesiones. `NULL` mientras está activa |
| frozen_version_module_a | INTEGER | NOT NULL | Versión del Módulo A congelada al momento de activar la descarga. Se restaura al finalizar |
| frozen_version_module_b | INTEGER | NOT NULL | Versión del Módulo B congelada |
| frozen_version_module_c | INTEGER | NOT NULL | Versión del Módulo C congelada |

**Restricciones adicionales:**

- `CHECK(status IN ('ACTIVE', 'COMPLETED'))`
- `CHECK(frozen_version_module_a >= 1 AND frozen_version_module_a <= 3)`
- `CHECK(frozen_version_module_b >= 1 AND frozen_version_module_b <= 3)`
- `CHECK(frozen_version_module_c >= 1 AND frozen_version_module_c <= 3)`

**Índices:**

- Índice en `status` para localizar rápidamente la descarga activa.

---

### 3.16. alert

**Propósito:** Persiste las alertas generadas por el motor de reglas. Las alertas son señales informativas (no bloqueantes, CA-15.04) que notifican al ejecutante sobre condiciones que requieren atención: mesetas, fatiga, regresión, baja adherencia, etc.

Las alertas se generan automáticamente al cierre de cada sesión (el motor de reglas evalúa KPIs y condiciones) y se resuelven automáticamente cuando la condición que las disparó deja de cumplirse. El ejecutante puede consultarlas en H1 (Centro de Alertas) y H2 (Detalle de Alerta).

Cada tipo de alerta afecta a una entidad diferente (ejercicio, módulo o grupo muscular). Se usan columnas nullable para referenciar la entidad afectada según el tipo.

Los datos que dispararon la alerta (mostrados en H2) y las recomendaciones escalonadas no se almacenan — se recalculan dinámicamente en la capa de aplicación a partir de las series, sesiones y la lógica del motor de reglas. Dado que las sesiones cerradas son inmutables, el recálculo siempre produce el mismo resultado.

| Columna | Tipo | Restricciones | Descripción |
| ------- | ---- | ------------- | ----------- |
| id | INTEGER | **PK**, AUTOINCREMENT | Identificador único |
| type | TEXT | NOT NULL | Tipo de alerta. Valores: `"PLATEAU"`, `"LOW_PROGRESSION_RATE"`, `"RIR_OUT_OF_RANGE"`, `"LOW_ADHERENCE"`, `"TONNAGE_DROP"`, `"MODULE_INACTIVITY"`, `"MODULE_REQUIRES_DELOAD"` |
| level | TEXT | NOT NULL | Nivel de severidad: `"CRISIS"`, `"HIGH_ALERT"`, `"MEDIUM_ALERT"` |
| exercise_id | INTEGER | NULL, **FK** → `exercise(id)` | Ejercicio afectado. Poblado cuando `type` ∈ {`PLATEAU`, `LOW_PROGRESSION_RATE`} |
| module_code | TEXT | NULL, **FK** → `module(code)` | Módulo afectado. Poblado cuando `type` ∈ {`RIR_OUT_OF_RANGE`, `MODULE_INACTIVITY`, `MODULE_REQUIRES_DELOAD`} |
| muscle_group | TEXT | NULL | Grupo muscular afectado. Poblado cuando `type` = `TONNAGE_DROP`. Valores: los 12 grupos definidos en `muscle_zone.muscle_group` |
| message | TEXT | NOT NULL | Descripción legible de la alerta para mostrar en H1. Ej: `"3 sesiones sin progresión"`, `"RIR 1.2 — riesgo de fatiga"` |
| is_active | INTEGER | NOT NULL, DEFAULT 1 | `1` si la alerta está vigente; `0` si fue resuelta automáticamente. Las alertas resueltas se conservan como histórico |
| created_at | TEXT | NOT NULL | Fecha de generación de la alerta (ISO 8601) |
| resolved_at | TEXT | NULL | Fecha en que la condición dejó de cumplirse y la alerta se resolvió automáticamente. `NULL` mientras está activa |

**Restricciones adicionales:**

- `CHECK(type IN ('PLATEAU', 'LOW_PROGRESSION_RATE', 'RIR_OUT_OF_RANGE', 'LOW_ADHERENCE', 'TONNAGE_DROP', 'MODULE_INACTIVITY', 'MODULE_REQUIRES_DELOAD'))`
- `CHECK(level IN ('CRISIS', 'HIGH_ALERT', 'MEDIUM_ALERT'))`
- `FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE RESTRICT`
- `FOREIGN KEY(module_code) REFERENCES module(code) ON DELETE RESTRICT`

**Índices:**

- Índice en `is_active` para filtrar alertas vigentes (badge en B1, lista en H1).
- Índice en `type` para agrupación por tipo.
- Índice en `exercise_id` para alertas por ejercicio.
- Índice en `module_code` para alertas por módulo.

**Tipos de alerta y entidad afectada:**

| Tipo | Entidad afectada | Columna poblada | Niveles posibles |
| ---- | ---------------- | --------------- | ---------------- |
| `PLATEAU` | Ejercicio | `exercise_id` | `HIGH_ALERT` |
| `LOW_PROGRESSION_RATE` | Ejercicio | `exercise_id` | `MEDIUM_ALERT` (< 40%), `CRISIS` (< 20%) |
| `RIR_OUT_OF_RANGE` | Módulo | `module_code` | `MEDIUM_ALERT` |
| `LOW_ADHERENCE` | Semanal (global) | ninguna | `MEDIUM_ALERT` (1 semana), `CRISIS` (2+ semanas) |
| `TONNAGE_DROP` | Grupo muscular | `muscle_group` | `MEDIUM_ALERT` (> 10%), `CRISIS` (> 20%) |
| `MODULE_INACTIVITY` | Módulo | `module_code` | `MEDIUM_ALERT` (> 10 días), `CRISIS` (> 14 días) |
| `MODULE_REQUIRES_DELOAD` | Módulo | `module_code` | `HIGH_ALERT` |

---

## 4. Relaciones y Cardinalidades

| Relación | Cardinalidad | Descripción |
| -------- | :----------: | ----------- |
| module → exercise | 1 : N | Un módulo contiene muchos ejercicios; cada ejercicio pertenece a exactamente 1 módulo |
| module → module_version | 1 : N | Un módulo tiene 2 o 3 versiones; cada versión pertenece a exactamente 1 módulo |
| equipment_type → exercise | 1 : N | Un tipo de equipo es usado por varios ejercicios; cada ejercicio usa exactamente 1 tipo |
| exercise ↔ muscle_zone | N : M | Un ejercicio trabaja 1+ zonas musculares; una zona es trabajada por múltiples ejercicios. Resuelta por `exercise_muscle_zone` |
| module_version → plan_assignment | 1 : N | Una versión prescribe 9-11 ejercicios |
| exercise → plan_assignment | 1 : N | Un ejercicio puede estar asignado en múltiples versiones (del mismo módulo) |
| module_version → session | 1 : N | Una combinación módulo-versión puede tener muchas sesiones a lo largo del tiempo; cada sesión pertenece a exactamente 1 módulo-versión |
| session → session_exercise | 1 : N | Una sesión contiene 9-11 ejercicios; cada ejercicio-en-sesión pertenece a exactamente 1 sesión |
| exercise → session_exercise | 1 : N | Un ejercicio puede aparecer en múltiples sesiones; `exercise_id` referencia el ejercicio efectivamente ejecutado |
| exercise → session_exercise (original) | 1 : N | Un ejercicio puede haber sido reemplazado en múltiples sesiones; `original_exercise_id` referencia el ejercicio del plan que fue sustituido (nullable) |
| session_exercise → exercise_set | 1 : N | Un ejercicio-en-sesión tiene 0 a 4 series; cada serie pertenece a exactamente 1 ejercicio-en-sesión |
| exercise → exercise_progression | 1 : 1 | Un ejercicio tiene exactamente 0 o 1 estado de progresión; se crea al primer registro del ejercicio |
| deload → session | 1 : N | Un ciclo de descarga puede tener hasta 6 sesiones; cada sesión de descarga pertenece a exactamente 1 ciclo (nullable — solo sesiones de descarga) |
| exercise → alert | 1 : N | Un ejercicio puede tener múltiples alertas (meseta, progresión baja); cada alerta de ejercicio referencia exactamente 1 ejercicio (nullable) |
| module → alert | 1 : N | Un módulo puede tener múltiples alertas (RIR, inactividad, descarga); cada alerta de módulo referencia exactamente 1 módulo (nullable) |

---

## 5. Diagrama Entidad-Relación

```mermaid
erDiagram
    module {
        TEXT code PK "A, B, C"
        TEXT name
        TEXT group_description
        REAL load_increment_kg
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
        TEXT module_code FK
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

    module_version {
        INTEGER id PK
        TEXT module_code FK
        INTEGER version_number
    }

    plan_assignment {
        INTEGER module_version_id PK_FK
        INTEGER exercise_id PK_FK
        INTEGER sets
        TEXT reps
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
        INTEGER module_version_id FK
        INTEGER deload_id FK "nullable"
        TEXT date
        TEXT status
    }

    session_exercise {
        INTEGER id PK
        INTEGER session_id FK
        INTEGER exercise_id FK
        INTEGER original_exercise_id FK "nullable"
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
        INTEGER current_version_module_a
        INTEGER current_version_module_b
        INTEGER current_version_module_c
        INTEGER microcycle_count
    }

    deload {
        INTEGER id PK
        TEXT status
        TEXT activation_date
        TEXT completion_date "nullable"
        INTEGER frozen_version_module_a
        INTEGER frozen_version_module_b
        INTEGER frozen_version_module_c
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
        TEXT module_code FK "nullable"
        TEXT muscle_group "nullable"
        TEXT message
        INTEGER is_active
        TEXT created_at
        TEXT resolved_at "nullable"
    }

    module ||--o{ exercise : "contains"
    module ||--o{ module_version : "has versions"
    equipment_type ||--o{ exercise : "used by"
    exercise ||--o{ exercise_muscle_zone : "works"
    muscle_zone ||--o{ exercise_muscle_zone : "worked by"
    module_version ||--o{ plan_assignment : "prescribes"
    exercise ||--o{ plan_assignment : "assigned in"
    module_version ||--o{ session : "executed in"
    session ||--o{ session_exercise : "contains"
    exercise ||--o{ session_exercise : "executed in"
    exercise ||--o{ session_exercise : "substituted in"
    session_exercise ||--o{ exercise_set : "records"
    exercise ||--|| exercise_progression : "progression of"
    deload ||--o{ session : "contains"
    exercise ||--o{ alert : "alerted on"
    module ||--o{ alert : "alerted on"
```
