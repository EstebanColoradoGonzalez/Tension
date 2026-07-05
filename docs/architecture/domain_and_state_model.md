# Modelo de Dominio y Estado

> Este documento define la arquitectura estructural de la memoria del sistema y el ciclo de vida de sus entidades. Actúa simultáneamente como Modelo Entidad-Relación, Diccionario de Datos y Máquina de Estados. Se utiliza una sintaxis declarativa (pseudo-código estilo Prisma/TypeScript) para definir las estructuras, utilizando los comentarios inline como el diccionario de datos.
>
> **Versión de esquema:** 13 (migraciones registradas: v1 → v2 → … → v12 → v13).

---

## 1. Convenciones Base (Dominios Universales)

*Normas absolutas aplicadas en el almacenamiento y manejo de la información en todo el sistema para garantizar la homeostasis de los datos.*

- **Manejo de Tiempos y Fechas:** `Todas las fechas se almacenan como TEXT en formato ISO 8601. Fechas simples usan "YYYY-MM-DD"; marcas de tiempo completas usan "YYYY-MM-DDTHH:MM:SS". Ningún valor de fecha se almacena como INTEGER epoch ni como tipo nativo de la base de datos.`
- **Manejo de Estados Lógicos (Booleanos):** `El motor de persistencia (SQLite) no tiene tipo BOOLEAN nativo. Todos los valores lógicos binarios se almacenan como INTEGER NOT NULL con DEFAULT 0, donde 0 = false y 1 = true. Ninguna columna lógica admite NULL — un valor nulo en una columna booleana es un estado inválido.`
- **Manejo de Valores de Alta Precisión (Peso en Kg):** `Los valores de peso corporal y carga de ejercicio se almacenan como REAL (punto flotante de 64 bits). El incremento mínimo del sistema es 0.5 Kg, por lo que la precisión de REAL es suficiente sin recurrir a enteros escalados. Los valores calculados derivados (tonelaje, promedios, tendencias) NO se persisten — se computan en la capa de aplicación a partir de los datos base en cada consulta.`
- **Convenciones de Nomenclatura de Esquema:** `Nombres de tablas y columnas en snake_case en inglés (ej: routine_version, exercise_id). Claves primarias autoincrement: id INTEGER PRIMARY KEY AUTOINCREMENT. Claves foráneas: sufijo _id para referencias a PKs enteras. Datos de catálogo (seed) usan ON DELETE RESTRICT; datos transaccionales usan ON DELETE CASCADE donde aplica.`
- **Cardinalidad de Instancias:** `El sistema es single-user. Las tablas profile y rotation_state son de fila única (id = 1 siempre). Todas las demás tablas crecen indefinidamente con el historial del ejecutante.`

---

## 2. Esquema de Estructuras y Diccionario Integrado

*Inventario ontológico del sistema. Cada bloque model define una entidad, sus propiedades y su semántica. Los comentarios inline actúan como el Diccionario de Datos estricto.*

```
// ==========================================
// ENTIDAD: routine
// PROPÓSITO: Rutina de entrenamiento definida por el ejecutante.
// Unidad organizativa del plan. El ejecutante crea las que necesite
// con el nombre y orden que prefiera.
// ==========================================
model routine {
  id          INTEGER  @id @autoincrement           // PK. Identificador único.
  name        TEXT     @unique @notNull             // Nombre libre: "Pull + Abs", "Pierna", "Push". Obligatorio. Sin duplicados.
  sort_order  INTEGER  @notNull @check(">= 1")      // Posición en la rotación cíclica. 1-based. Determina el orden de ejecución dentro del microciclo.
  created_at  TEXT     @notNull                     // Fecha de creación ISO 8601. Inmutable tras la creación.
}

// ==========================================
// ENTIDAD: muscle_zone
// PROPÓSITO: Catálogo de 20 zonas musculares específicas.
// Clasifican cada ejercicio y permiten la agregación de KPIs por
// grupo muscular. Inmutable (solo el ejecutante puede ampliar
// mediante ejercicios personalizados).
// ==========================================
model muscle_zone {
  id            INTEGER  @id @autoincrement         // PK. Identificador único.
  name          TEXT     @unique @notNull           // Nombre específico de la zona: "Pecho Medio", "Dorsal Ancho", "Cuádriceps". 20 valores seed.
  muscle_group  TEXT     @notNull                   // Grupo muscular padre para agregación de KPIs: "Pecho", "Espalda", "Abdomen", "Hombro", "Tríceps", "Bíceps", "Cuádriceps", "Isquiotibiales", "Glúteos", "Aductores", "Abductores", "Gemelos", "Antebrazo", "Cuello".
}

// ==========================================
// ENTIDAD: equipment_type
// PROPÓSITO: Catálogo de 23 tipos de equipamiento requeridos por
// los ejercicios. Normaliza el valor repetido y habilita el filtro
// por equipo en el Diccionario de Ejercicios.
// ==========================================
model equipment_type {
  id    INTEGER  @id @autoincrement                 // PK. Identificador único.
  name  TEXT     @unique @notNull                   // Nombre del tipo: "Máquina", "Mancuernas", "Barra de Pesas", "Polea", "Cuerpo", etc. 23 valores seed.
}

// ==========================================
// ENTIDAD: exercise
// PROPÓSITO: Catálogo de ejercicios del sistema.
// Contiene 43 ejercicios precargados (seed) y se extiende
// con ejercicios creados por el ejecutante (RF-62).
// Agnóstico de rutina — se asigna libremente al plan.
// Entidad central: referenciada por plan, sesiones,
// registros de series, progresión y alertas.
// ==========================================
model exercise {
  id                       INTEGER  @id @autoincrement                  // PK. Identificador único.
  name                     TEXT     @notNull                            // Nombre del ejercicio: "Press de Banca", "Flexiones". Puede repetirse si difiere el equipment_type.
  equipment_type_id        INTEGER  @fk(equipment_type.id) @notNull    // FK → equipment_type. Tipo de equipamiento requerido. ON DELETE RESTRICT.
  is_bodyweight            INTEGER  @notNull @default(0)               // Booleano. 1 = peso corporal (Peso = 0 Kg). Progresión por repeticiones totales, sin Doble Umbral.
  is_isometric             INTEGER  @notNull @default(0)               // Booleano. 1 = isométrico (Plancha, Plancha Lateral). Progresión por segundos (rango 30-45s). "Dominado" al ≥ 45s en todas las series.
  is_to_technical_failure  INTEGER  @notNull @default(0)               // Booleano. 1 = sin límite superior de repeticiones (solo Flexiones). Mutuamente excluyente con is_isometric.
  is_custom                INTEGER  @notNull @default(0)               // Booleano. 1 = creado por el ejecutante (RF-62). 0 = seed precargado.
  media_resource           TEXT     @optional                          // Para seed: nombre del asset PNG empaquetado (ej: "press_de_banca_maquina"). Para custom: ruta absoluta al archivo de imagen en almacenamiento interno. NULL si no se ha asociado imagen.
  // CONSTRAINT ÚNICO: UNIQUE(name, equipment_type_id) — clave natural compuesta.
  // CONSTRAINT: is_isometric=1 implica is_bodyweight=1. is_to_technical_failure=1 implica is_bodyweight=1.
}

// ==========================================
// ENTIDAD: exercise_muscle_zone
// PROPÓSITO: Tabla de unión N:M entre exercise y muscle_zone.
// Un ejercicio trabaja 1+ zonas. Crítica para KPIs de
// tonelaje por grupo muscular y distribución de volumen.
// ==========================================
model exercise_muscle_zone {
  exercise_id     INTEGER  @fk(exercise.id) @notNull     // FK → exercise. ON DELETE RESTRICT. Parte de PK compuesta.
  muscle_zone_id  INTEGER  @fk(muscle_zone.id) @notNull  // FK → muscle_zone. ON DELETE RESTRICT. Parte de PK compuesta.
  // PK COMPUESTA: (exercise_id, muscle_zone_id).
}

// ==========================================
// ENTIDAD: routine_version
// PROPÓSITO: Versión de una rutina del plan.
// Las versiones rotan dentro de cada rutina (V1→V2→V1…)
// para diversificar el estímulo. El ejecutante define
// cuántas versiones tiene cada rutina.
// ==========================================
model routine_version {
  id              INTEGER  @id @autoincrement               // PK. Identificador único.
  routine_id      INTEGER  @fk(routine.id) @notNull        // FK → routine. ON DELETE CASCADE. La versión desaparece si se elimina la rutina.
  version_number  INTEGER  @notNull @check(">= 1")          // Número de versión: 1, 2, 3... Único por rutina.
  // CONSTRAINT: UNIQUE(routine_id, version_number).
}

// ==========================================
// ENTIDAD: plan_assignment
// PROPÓSITO: Define qué ejercicios componen cada
// combinación rutina-versión, con series, repeticiones
// y orden sugerido. Es el "Plan de Entrenamiento" en
// forma relacional — la receta consultada al iniciar sesión.
// ==========================================
model plan_assignment {
  routine_version_id  INTEGER  @fk(routine_version.id) @notNull  // FK → routine_version. ON DELETE CASCADE. Parte de PK compuesta.
  exercise_id         INTEGER  @fk(exercise.id) @notNull         // FK → exercise. ON DELETE RESTRICT. Parte de PK compuesta. Un ejercicio no puede duplicarse en la misma versión.
  sets                INTEGER  @notNull @check("> 0")             // Series prescritas. Siempre 4 en la implementación actual.
  reps                TEXT     @notNull                           // Rango de repeticiones: "8-12", "TO_TECHNICAL_FAILURE" (Flexiones), "30-45_SEC" (isométricos). Validado en capa Domain.
  sort_order          INTEGER  @notNull @default(0)              // Orden sugerido de ejecución dentro de la versión. 1-based. Orientativo, no restrictivo.
  slot                INTEGER  @notNull @default(0)              // Número de puesto (slot). Múltiples ejercicios con el mismo slot son alternativas equivalentes para ese puesto. Añadido en v12, corregido en v13.
  // PK COMPUESTA: (routine_version_id, exercise_id).
}

// ==========================================
// ENTIDAD: profile
// PROPÓSITO: Datos del ejecutante y su configuración.
// Tabla de fila única — sistema single-user.
// Se crea al completar el onboarding y se actualiza
// desde las vistas de Perfil y Ajustes.
// El peso corporal actual NO se almacena aquí —
// se obtiene del registro más reciente en weight_record.
// ==========================================
model profile {
  id                INTEGER  @id @default(1) @check("= 1")                       // Siempre 1. Garantiza fila única.
  height_m          REAL     @notNull @check("> 0")                               // Altura en metros. Inmutable en la práctica (raramente cambia).
  experience_level  TEXT     @notNull @check("IN ('BEGINNER','INTERMEDIATE','ADVANCED')")  // Nivel de experiencia declarado. Ver Enum ExperienceLevel.
  weekly_frequency  INTEGER  @notNull @default(6) @check(">= 4 AND <= 6")         // Objetivo de sesiones semanales: 4, 5 o 6. Denominador del KPI de Adherencia.
  created_at        TEXT     @notNull                                             // Fecha de creación del perfil ISO 8601. Inmutable.
}

// ==========================================
// ENTIDAD: weight_record
// PROPÓSITO: Historial de registros de peso corporal.
// Cada actualización desde el Perfil genera un nuevo
// registro — los anteriores no se sobrescriben ni eliminan.
// El peso actual = registro más reciente (ORDER BY date DESC LIMIT 1).
// ==========================================
model weight_record {
  id         INTEGER  @id @autoincrement        // PK. Identificador único.
  weight_kg  REAL     @notNull @check("> 0")    // Peso registrado en Kg. Estrictamente positivo.
  date       TEXT     @notNull                  // Fecha del registro ISO 8601. Indexada para consulta cronológica descendente.
}

// ==========================================
// ENTIDAD: session
// PROPÓSITO: Sesión de entrenamiento — ejecución de una
// combinación rutina-versión en una fecha.
// Entidad transaccional central. Ciclo de vida:
// IN_PROGRESS → COMPLETED o INCOMPLETE.
// Una vez cerrada es inmutable.
// Solo puede existir una sesión IN_PROGRESS a la vez.
// ==========================================
model session {
  id                  INTEGER  @id @autoincrement                      // PK. Identificador único.
  routine_version_id  INTEGER  @fk(routine_version.id) @notNull       // FK → routine_version. ON DELETE RESTRICT. Rutina y versión ejecutada. Determinada por la rotación al iniciar.
  deload_id           INTEGER  @fk(deload.id) @optional               // FK → deload. NULL si no es sesión de descarga. NOT NULL si pertenece a un ciclo de descarga activo. ON DELETE RESTRICT.
  date                TEXT     @notNull                                // Fecha de la sesión ISO 8601 ("YYYY-MM-DD").
  status              TEXT     @notNull @default("IN_PROGRESS")        // Estado del ciclo de vida. Ver Enum SessionStatus.
  // El tonelaje total no se almacena — se calcula: SUM(exercise_set.weight_kg * exercise_set.reps).
}

// ==========================================
// ENTIDAD: session_exercise
// PROPÓSITO: Ejercicio dentro de una sesión activa.
// Vincula sesión con ejercicio ejecutado. Gestiona
// sustituciones puntuales e intercambio de alternativas.
// El estado (No Iniciado / En Ejecución / Completado) se
// deriva: is_finalized=1 → Completado; 0 series → No Iniciado;
// ≥1 serie sin finalizar → En Ejecución.
// ==========================================
model session_exercise {
  id                        INTEGER  @id @autoincrement                       // PK. Identificador único.
  session_id                INTEGER  @fk(session.id) @notNull                // FK → session. ON DELETE CASCADE. La sesión es el contenedor; sus ejercicios se eliminan con ella.
  exercise_id               INTEGER  @fk(exercise.id) @optional              // FK → exercise. Ejercicio efectivamente ejecutado (puede ser sustituto o alternativa). ON DELETE RESTRICT.
  original_exercise_id      INTEGER  @fk(exercise.id) @optional              // FK → exercise. Ejercicio original del plan que fue sustituido. NULL si no hubo sustitución (o si fue intercambio de alternativa). ON DELETE RESTRICT.
  is_finalized              INTEGER  @notNull @default(0)                    // Booleano. 1 = ejercicio finalizado (explícitamente o al cerrar sesión). Determina estado Completado.
  pending_selection         INTEGER  @notNull @default(0)                    // Columna legacy. Siempre 0. Mantenida por compatibilidad de esquema (v12).
  slot                      INTEGER  @notNull @default(0)                    // Número de puesto (slot) correspondiente a plan_assignment.slot. Permite identificar alternativas disponibles.
  progression_classification TEXT    @optional                               // Clasificación asignada al cierre: "POSITIVE_PROGRESSION", "MAINTENANCE", "REGRESSION". NULL si sin historial previo. Ver Enum ProgressionClassification.
  // CONSTRAINT: UNIQUE(session_id, exercise_id).
  // INTEGRIDAD: exercise_id sustituto debe compartir zona muscular con original_exercise_id.
  // INTEGRIDAD: sustitución solo posible con 0 series registradas.
}

// ==========================================
// ENTIDAD: exercise_set
// PROPÓSITO: Serie individual — unidad atómica de datos.
// Corresponde al concepto "Log" del dominio.
// Inmutable tras su creación. Cada serie se registra
// exactamente una vez.
// ==========================================
model exercise_set {
  id                   INTEGER  @id @autoincrement                              // PK. Identificador único.
  session_exercise_id  INTEGER  @fk(session_exercise.id) @notNull              // FK → session_exercise. ON DELETE CASCADE. La serie pertenece al ejercicio-en-sesión.
  set_number           INTEGER  @notNull @check(">= 1")                         // Número secuencial de la serie: 1, 2, 3, 4... Asignado automáticamente. Único por session_exercise.
  weight_kg            REAL     @notNull @check(">= 0")                         // Peso en Kg. 0 para ejercicios de peso corporal e isométricos. Obligatorio.
  reps                 INTEGER  @notNull @check(">= 1")                         // Repeticiones completadas. Para isométricos: segundos sostenidos. Mínimo 1. Mismo tipo de dato — la interpretación depende de exercise.is_isometric.
  rir                  INTEGER  @notNull @check(">= 0 AND <= 2")                // Repeticiones en Reserva (RIR). Escala entera [0, 2]. 0 = fallo técnico alcanzado. 2 = reserva moderada.
  // CONSTRAINT: UNIQUE(session_exercise_id, set_number).
}

// ==========================================
// ENTIDAD: exercise_progression
// PROPÓSITO: Estado persistente de progresión y carga
// prescrita de cada ejercicio. Se crea al registrar la
// primera serie del ejercicio. Se actualiza automáticamente
// al cierre de cada sesión por el motor de reglas.
// Ciclo de vida: NO_HISTORY → IN_PROGRESSION ⇄ IN_PLATEAU
// → IN_DELOAD → IN_PROGRESSION (o MASTERED para isométricos).
// ==========================================
model exercise_progression {
  exercise_id                  INTEGER  @id @fk(exercise.id)                          // PK y FK → exercise. Relación 1:1. ON DELETE RESTRICT.
  status                       TEXT     @notNull @default("NO_HISTORY")               // Estado actual del ciclo de vida. Ver Enum ExerciseProgressionStatus.
  prescribed_load_kg           REAL     @optional @check(">= 0")                      // Carga objetivo para la próxima sesión en Kg. Calculada por el motor de Doble Umbral. NULL para ejercicios de peso corporal e isométricos. Post-descarga: 90% de la carga pre-descarga.
  sessions_without_progression INTEGER  @notNull @default(0) @check(">= 0")           // Contador de sesiones consecutivas sin progresión. Se incrementa con MAINTENANCE o REGRESSION; se resetea a 0 con POSITIVE_PROGRESSION. Umbral de meseta: 3. Umbrales de acción escalonada: 4 y 6.
}

// ==========================================
// ENTIDAD: rotation_state
// PROPÓSITO: Estado completo de la rotación cíclica y
// conteo de microciclos. Tabla de fila única.
// Persiste la posición en el plan de forma inmune a
// ausencias del ejecutante — nunca se reinicia por el
// paso del tiempo.
// ==========================================
model rotation_state {
  id                  INTEGER  @id @default(1) @check("= 1")         // Siempre 1. Garantiza fila única.
  microcycle_position INTEGER  @notNull @default(1) @check(">= 1")   // Posición actual en la secuencia de rutinas: 1 a N (N = total de rutinas). Determina qué rutina toca. Avanza tras cada cierre de sesión; vuelve a 1 al completar la última.
  microcycle_count    INTEGER  @notNull @default(0) @check(">= 0")   // Número de microciclos completados. Incrementa al cerrar la sesión de la última posición.
}

// ==========================================
// ENTIDAD: routine_current_version
// PROPÓSITO: Persiste la versión actual de cada rutina
// para el microciclo en curso. Reemplaza columnas fijas
// del modelo anterior para soportar número dinámico de rutinas.
// ==========================================
model routine_current_version {
  routine_id              INTEGER  @id @fk(routine.id)                      // PK y FK → routine. ON DELETE CASCADE.
  current_version_number  INTEGER  @notNull @default(1) @check(">= 1")      // Versión de la rutina en curso para el microciclo actual. Rota V1→V2→…→VN→V1 al inicio de cada nuevo microciclo.
}

// ==========================================
// ENTIDAD: deload
// PROPÓSITO: Ciclo de descarga (Deload).
// Se activa cuando el motor detecta fatiga acumulada
// y el ejecutante confirma. Dura 1 microciclo completo.
// Parámetros fijos: carga al 60%, reinicio al 90%.
// Solo puede existir una descarga ACTIVE a la vez.
// ==========================================
model deload {
  id               INTEGER  @id @autoincrement     // PK. Identificador único.
  status           TEXT     @notNull @default("ACTIVE")  // Estado del ciclo. Ver Enum DeloadStatus.
  activation_date  TEXT     @notNull               // Fecha de activación ISO 8601. Inmutable.
  completion_date  TEXT     @optional              // Fecha de finalización ISO 8601. NULL mientras está ACTIVE.
  // El número de sesiones completadas de la descarga no se almacena:
  // se calcula como COUNT(*) FROM session WHERE deload_id = ? AND status IN ('COMPLETED','INCOMPLETE').
  // La carga habitual pre-descarga no se almacena: se deriva del último exercise_set por ejercicio antes de activation_date.
}

// ==========================================
// ENTIDAD: deload_frozen_version
// PROPÓSITO: Versiones de cada rutina congeladas al
// momento de activar una descarga. Las versiones no
// rotan durante el ciclo de descarga. Se restauran
// al finalizar.
// ==========================================
model deload_frozen_version {
  deload_id              INTEGER  @fk(deload.id) @notNull    // FK → deload. ON DELETE CASCADE. Parte de PK compuesta.
  routine_id             INTEGER  @fk(routine.id) @notNull   // FK → routine. ON DELETE RESTRICT. Parte de PK compuesta.
  frozen_version_number  INTEGER  @notNull @check(">= 1")    // Versión congelada al momento de activar la descarga. Se restaura en routine_current_version al finalizar.
  // PK COMPUESTA: (deload_id, routine_id).
}

// ==========================================
// ENTIDAD: alert
// PROPÓSITO: Alertas generadas por el motor de reglas.
// Informativas y no bloqueantes. Se generan al cierre
// de cada sesión. Se resuelven automáticamente cuando
// la condición que las disparó deja de cumplirse.
// El historial de alertas resueltas se conserva (is_active = 0).
// ==========================================
model alert {
  id           INTEGER  @id @autoincrement         // PK. Identificador único.
  type         TEXT     @notNull                   // Tipo de alerta. Ver Enum AlertType.
  level        TEXT     @notNull                   // Nivel de severidad. Ver Enum AlertLevel.
  exercise_id  INTEGER  @fk(exercise.id) @optional // FK → exercise. Poblado para tipos PLATEAU y LOW_PROGRESSION_RATE. NULL en otros tipos. ON DELETE RESTRICT.
  routine_id   INTEGER  @fk(routine.id) @optional  // FK → routine. Poblado para tipos RIR_OUT_OF_RANGE, ROUTINE_INACTIVITY, ROUTINE_REQUIRES_DELOAD. NULL en otros tipos. ON DELETE CASCADE.
  muscle_group TEXT     @optional                  // Grupo muscular afectado. Poblado solo para tipo TONNAGE_DROP. Valores: los 14 grupos definidos en muscle_zone.muscle_group.
  message      TEXT     @notNull                   // Descripción legible para el ejecutante: "3 sesiones sin progresión", "RIR 1.2 — riesgo de fatiga".
  is_active    INTEGER  @notNull @default(1)       // Booleano. 1 = alerta vigente; 0 = resuelta automáticamente. Las resueltas se conservan como histórico.
  created_at   TEXT     @notNull                   // Fecha de generación ISO 8601.
  resolved_at  TEXT     @optional                  // Fecha de resolución automática ISO 8601. NULL mientras está activa.
}
```

---

## 3. Matriz de Relaciones y Topología

*Define explícitamente las reglas de existencia, dependencia y cardinalidad entre las entidades, así como el comportamiento de las eliminaciones.*

| **Entidad Origen** | **Cardinalidad** | **Entidad Destino** | **Verbo / Naturaleza** | **Regla de Integridad (Comportamiento en Cascada)** |
| --- | :---: | --- | --- | --- |
| `routine` | `1 : N` | `routine_version` | "Tiene versiones" | CASCADE: si se elimina una rutina, sus versiones se eliminan. |
| `routine` | `1 : 1` | `routine_current_version` | "Tiene versión actual" | CASCADE: si se elimina la rutina, se elimina su estado de versión. |
| `equipment_type` | `1 : N` | `exercise` | "Es requerido por" | RESTRICT: no se puede eliminar un tipo de equipo si hay ejercicios que lo usan. |
| `exercise` | `N : M` | `muscle_zone` | "Trabaja zonas" | Resuelta por `exercise_muscle_zone`. RESTRICT en ambos lados: ningún ejercicio ni zona se puede eliminar si existe la relación. |
| `routine_version` | `1 : N` | `plan_assignment` | "Prescribe ejercicios" | CASCADE: si se elimina una versión, sus asignaciones se eliminan. |
| `exercise` | `1 : N` | `plan_assignment` | "Es asignado en versiones" | RESTRICT: un ejercicio no se puede eliminar si está asignado al plan. |
| `routine_version` | `1 : N` | `session` | "Es ejecutada en sesiones" | RESTRICT: una rutina-versión no se puede eliminar si tiene sesiones asociadas. |
| `deload` | `1 : N` | `session` | "Contiene sesiones de descarga" | RESTRICT: un ciclo de descarga no se puede eliminar si tiene sesiones asociadas. |
| `session` | `1 : N` | `session_exercise` | "Contiene ejercicios" | CASCADE: si se elimina una sesión, sus ejercicios-en-sesión se eliminan. |
| `exercise` | `1 : N` | `session_exercise (exercise_id)` | "Es ejecutado en sesiones" | RESTRICT: un ejercicio no se puede eliminar si tiene registros en sesiones. |
| `exercise` | `1 : N` | `session_exercise (original_exercise_id)` | "Fue sustituido en sesiones" | RESTRICT: un ejercicio no se puede eliminar si fue sustituido en sesiones. |
| `session_exercise` | `1 : N` | `exercise_set` | "Registra series" | CASCADE: si se elimina un ejercicio-en-sesión, sus series se eliminan. |
| `exercise` | `1 : 1` | `exercise_progression` | "Tiene estado de progresión" | RESTRICT: el estado de progresión es inseparable del ejercicio. Se crea al primer registro. |
| `deload` | `1 : N` | `deload_frozen_version` | "Congela versiones de rutinas" | CASCADE: si se elimina un ciclo de descarga, sus versiones congeladas se eliminan. |
| `routine` | `1 : N` | `deload_frozen_version` | "Tiene versiones congeladas en descargas" | RESTRICT: una rutina no se puede eliminar si tiene versiones congeladas en un ciclo de descarga. |
| `exercise` | `1 : N` | `alert (exercise_id)` | "Genera alertas de progresión" | RESTRICT: un ejercicio no se puede eliminar si tiene alertas asociadas. |
| `routine` | `1 : N` | `alert (routine_id)` | "Genera alertas de rutina" | CASCADE: si se elimina una rutina, sus alertas se eliminan. |

---

## 4. Dominios Cerrados (Enums y Catálogos)

*Conjuntos de valores estáticos predefinidos que limitan la entrada de datos, previniendo la entropía y asegurando la consistencia semántica. Implementados como TEXT con CHECK constraint en el esquema y como enum class en la capa de aplicación.*

```
enum ExperienceLevel {
  BEGINNER      // Principiante. Menos de 1 año de entrenamiento estructurado. Contexto de métricas de rendimiento.
  INTERMEDIATE  // Intermedio. 1-3 años de entrenamiento estructurado.
  ADVANCED      // Avanzado. Más de 3 años de entrenamiento estructurado con progresión documentada.
}

enum SessionStatus {
  IN_PROGRESS  // Sesión iniciada. El ejecutante está registrando series. Solo puede existir una a la vez.
  COMPLETED    // Sesión cerrada como completada. Todas las series de todos los ejercicios fueron registradas. Inmutable.
  INCOMPLETE   // Sesión cerrada sin completar todos los ejercicios. Los datos parciales se conservan. Inmutable.
}

enum ProgressionClassification {
  POSITIVE_PROGRESSION  // Progresión positiva: aumentó carga y/o repeticiones con RIR estable o mejorado respecto a la sesión anterior del mismo ejercicio.
  MAINTENANCE           // Mantenimiento: misma carga y repeticiones, RIR estable. Sin retroceso pero sin avance.
  REGRESSION            // Regresión: disminuyó carga o repeticiones, o el RIR promedió subió ≥ 1.5 puntos con la misma carga.
  // NULL es válido: indica que el ejercicio no tiene historial previo ("Sin Historial"). No se emite clasificación.
}

enum ExerciseProgressionStatus {
  NO_HISTORY    // Estado inicial. El ejercicio se ha ejecutado 0 o 1 vez. Sin base de comparación suficiente.
  IN_PROGRESSION // El ejercicio muestra progresión positiva entre sesiones consecutivas. Estado saludable.
  IN_PLATEAU    // 3+ sesiones consecutivas sin progresión positiva. El motor ha emitido alerta y recomienda acciones correctivas escalonadas.
  IN_DELOAD     // El ejercicio está en un microciclo de descarga planificada. No se espera progresión. Carga al 60%.
  MASTERED      // Estado terminal exclusivo para ejercicios isométricos. Todas las series prescritas alcanzaron ≥ 45 segundos. El ejercicio ya no ofrece estímulo progresivo suficiente en su variante actual.
}

enum DeloadStatus {
  ACTIVE     // Ciclo de descarga en curso. Solo puede existir uno a la vez. Las versiones están congeladas.
  COMPLETED  // Ciclo finalizado. Las cargas han sido reiniciadas al 90% de la carga pre-descarga.
}

enum RepsMode {
  STANDARD           // "8-12" — rango estándar de repeticiones para la mayoría de ejercicios. Se aplica el Doble Umbral.
  TO_TECHNICAL_FAILURE // "TO_TECHNICAL_FAILURE" — sin límite superior. Solo para Flexiones (is_to_technical_failure=1). Progresión por repeticiones totales.
  ISOMETRIC_SECONDS  // "30-45_SEC" — rango en segundos para ejercicios isométricos (is_isometric=1). Progresión por segundos sostenidos.
}

enum AlertType {
  PLATEAU                   // Ejercicio en estado de meseta (3+ sesiones sin progresión). Entidad afectada: exercise_id.
  LOW_PROGRESSION_RATE      // Tasa de progresión del ejercicio < 40% (MEDIUM_ALERT) o < 20% (CRISIS) en 4 semanas. Entidad: exercise_id.
  RIR_OUT_OF_RANGE          // RIR promedio de la rutina < 0.5 (riesgo fatiga) o > 1.8 (carga conservadora) durante 2+ sesiones. Entidad: routine_id.
  LOW_ADHERENCE             // Adherencia semanal < 60%. Sin entidad específica — alerta global.
  TONNAGE_DROP              // Tonelaje del grupo muscular cayó > 10% (MEDIUM_ALERT) o > 20% (CRISIS) respecto al microciclo anterior. Entidad: muscle_group.
  ROUTINE_INACTIVITY        // Sin ejecutar una rutina > 10 días (MEDIUM_ALERT) o > 14 días (CRISIS). Entidad: routine_id.
  ROUTINE_REQUIRES_DELOAD   // ≥ 50% de los ejercicios de la rutina en Meseta o Regresión simultánea. Recomienda activar descarga. Entidad: routine_id.
}

enum AlertLevel {
  CRISIS       // Condición crítica que requiere acción inmediata. Ej: tasa de progresión < 20%, inactividad > 14 días.
  HIGH_ALERT   // Condición grave que requiere atención pronta. Ej: meseta activa, rutina requiere descarga.
  MEDIUM_ALERT // Condición informativa que merece seguimiento. Ej: tasa de progresión < 40%, inactividad > 10 días.
}

enum MuscleGroup {
  Pecho          // Agrupa: Pecho Medio, Pecho Superior, Pecho Inferior.
  Espalda        // Agrupa: Espalda Media, Dorsal Ancho, Espalda Alta, Trapecio, Espalda Baja.
  Abdomen        // Zona única. Sin subdivisión.
  Hombro         // Zona única. Sin subdivisión.
  Tríceps        // Zona única. Sin subdivisión.
  Bíceps         // Zona única. Sin subdivisión.
  Cuádriceps     // Zona única. Sin subdivisión.
  Isquiotibiales // Zona única. Sin subdivisión.
  Glúteos        // Zona única. Sin subdivisión.
  Aductores      // Zona única. Sin subdivisión.
  Abductores     // Zona única. Sin subdivisión.
  Gemelos        // Zona única. Sin subdivisión.
  Antebrazo      // Zona única. Sin subdivisión.
  Cuello         // Zona única. Sin subdivisión.
}
```

---

## 5. Máquina de Estados (Ciclos de Vida)

*Para las entidades que mutan con el tiempo, define estrictamente los estados posibles y las transiciones legales.*

### 5.1. Ciclo de Vida de: `session`

- **Estado Inicial (Nacimiento):** `IN_PROGRESS` — se asigna al crear la sesión.
- **Estados Finales (Terminación):** `COMPLETED`, `INCOMPLETE` — una vez alcanzados, la sesión y todos sus datos son **inmutables**.

| **Estado Origen** | **Evento / Trigger** | **Estado Destino** | **Condiciones / Validaciones Previas** |
| --- | --- | --- | --- |
| `IN_PROGRESS` | El ejecutante cierra la sesión habiendo completado todos los ejercicios | `COMPLETED` | Todos los ejercicios de la sesión tienen `is_finalized = 1`. |
| `IN_PROGRESS` | El ejecutante cierra la sesión sin completar todos los ejercicios | `INCOMPLETE` | Al menos un ejercicio no tiene `is_finalized = 1`. Los datos parciales se conservan. |
| `IN_PROGRESS` | La app se cierra inesperadamente (crash) | `IN_PROGRESS` (persiste) | El estado se mantiene. Al reabrir, se detecta la sesión activa y se ofrece reanudarla. |

### 5.2. Ciclo de Vida de: `exercise` (estado dentro de una sesión — `session_exercise`)

- **Estado Inicial (Nacimiento):** `NOT_STARTED` — al crear la sesión, todos los ejercicios se crean con 0 series y `is_finalized = 0`.
- **Estado Final (Terminación):** `COMPLETED` — una vez finalizado, no acepta más series.
- **Nota:** Este ciclo de vida es derivado — no existe una columna de estado explícita; se calcula a partir de `is_finalized` y el conteo de series.

| **Estado Origen** | **Evento / Trigger** | **Estado Destino** | **Condiciones / Validaciones Previas** |
| --- | --- | --- | --- |
| `NOT_STARTED` | El ejecutante registra la primera serie del ejercicio | `IN_EXECUTION` | La serie cumple: weight_kg ≥ 0, reps ≥ 1, rir ∈ [0,2]. |
| `IN_EXECUTION` | El ejecutante registra más series del ejercicio | `IN_EXECUTION` | Cada serie adicional cumple las mismas restricciones. |
| `IN_EXECUTION` | El ejecutante finaliza el ejercicio explícitamente | `COMPLETED` | `is_finalized` se establece a 1. No se permiten más series. |
| `NOT_STARTED` / `IN_EXECUTION` | El ejecutante cierra la sesión | `COMPLETED` | Todos los ejercicios no finalizados se marcan `is_finalized = 1` automáticamente al cerrar. |

### 5.3. Ciclo de Vida de: `exercise_progression`

- **Estado Inicial (Nacimiento):** `NO_HISTORY` — se crea al registrar la primera serie del ejercicio.
- **Estado Final (Terminación):** `MASTERED` — exclusivo para ejercicios isométricos. Una vez alcanzado, no retrocede.

| **Estado Origen** | **Evento / Trigger** | **Estado Destino** | **Condiciones / Validaciones Previas** |
| --- | --- | --- | --- |
| `NO_HISTORY` | Se cierra una sesión con ≥ 2 registros históricos del ejercicio | `IN_PROGRESSION` o `IN_PLATEAU` | Con al menos 2 sesiones del ejercicio, el motor puede calcular progresión. Aplica la clasificación correspondiente. |
| `IN_PROGRESSION` | Se cierra sesión con clasificación `POSITIVE_PROGRESSION` | `IN_PROGRESSION` | `sessions_without_progression` se resetea a 0. |
| `IN_PROGRESSION` | Se cierra sesión con clasificación `MAINTENANCE` o `REGRESSION` | `IN_PROGRESSION` o `IN_PLATEAU` | `sessions_without_progression` se incrementa. Si llega a 3 → transición a `IN_PLATEAU`. |
| `IN_PLATEAU` | Se cierra sesión con clasificación `POSITIVE_PROGRESSION` | `IN_PROGRESSION` | `sessions_without_progression` se resetea a 0. La meseta se resuelve. |
| `IN_PLATEAU` | Se cierra sesión con clasificación `MAINTENANCE` o `REGRESSION` | `IN_PLATEAU` | `sessions_without_progression` continúa acumulando. Se emiten acciones correctivas escalonadas (umbral 4, umbral 6). |
| `IN_PROGRESSION` / `IN_PLATEAU` | El ejecutante activa un ciclo de descarga | `IN_DELOAD` | `deload.status = 'ACTIVE'`. Carga prescrita pasa a 60% de la habitual. |
| `IN_DELOAD` | El ciclo de descarga se completa | `IN_PROGRESSION` | `deload.status = 'COMPLETED'`. La carga prescrita se reinicia al 90% de la pre-descarga. |
| `IN_EXECUTION` (isométrico) | Se cierra sesión con todas las series ≥ 45 segundos | `MASTERED` | Solo para ejercicios con `is_isometric = 1`. Estado terminal: el ejercicio no ofrece más estímulo progresivo en su variante actual. |

### 5.4. Ciclo de Vida de: `deload`

- **Estado Inicial (Nacimiento):** `ACTIVE` — se crea al confirmar la activación de la descarga.
- **Estado Final (Terminación):** `COMPLETED` — al completar una pasada por todas las rutinas del plan.

| **Estado Origen** | **Evento / Trigger** | **Estado Destino** | **Condiciones / Validaciones Previas** |
| --- | --- | --- | --- |
| `ACTIVE` | Se cierran sesiones de todas las rutinas del plan | `COMPLETED` | `COUNT(session WHERE deload_id = ?) = N (total de rutinas)`. Las versiones descongeladas se restauran en `routine_current_version`. Las cargas de cada ejercicio se reinician al 90%. |

---

## 6. Datos Semilla y Retención (Termodinámica del Dato)

### 6.1. Condiciones de Inicialización (Big Bang)

*Registros que deben existir en el momento cero del despliegue, antes de la primera interacción del ejecutante.*

- **`muscle_zone` (20 filas):** Catálogo completo de zonas musculares precargado. Valores:

  | id | name | muscle_group |
  |----|------|--------------|
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

- **`equipment_type` (23 filas):** Catálogo completo de tipos de equipamiento precargado. Valores: Máquina, Mancuernas, Barra de Pesas, Cuerpo, Mancuerna, Polea, Pesa, Mancuerna o Pesa Rusa, Máquina Multiestación, Polea con Cuerda, Polea con Barra en V, Mancuerna o Polea, Mancuerna o Polea o Barra, Barra o Mancuernas, Mancuernas o Polea, Banda Elástica, Kettlebell, Barra EZ, TRX/Suspensión, Balón Medicinal, Rodillo de Abdomen, Paralelas/Dip Station, Barra Fija. *Nota: "Mancuerna" (singular, un implemento) y "Mancuernas" (plural, dos implementos) son tipos distintos.*

- **`exercise` (43 filas):** Catálogo base de ejercicios precargados con sus flags correspondientes:

  | name | equipment_type | is_bodyweight | is_isometric | is_to_technical_failure |
  |------|----------------|:---:|:---:|:---:|
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

- **`exercise_muscle_zone` (48 filas):** Cada uno de los 43 ejercicios seed está vinculado a su(s) zona(s) muscular(es). 38 ejercicios tienen 1 zona. 5 ejercicios tienen 2 zonas: Sentadilla de Sumo (Cuádriceps + Aductores), Sentadilla Búlgara Dividida (Cuádriceps + Glúteos), Subir Escalones (Cuádriceps + Glúteos), Zancada hacia atrás (Glúteos + Cuádriceps), Avanzada de Zancadas (Cuádriceps + Glúteos).

- **`rotation_state` (1 fila):** Se inicializa con `id=1, microcycle_position=1, microcycle_count=0` junto con el perfil del ejecutante al completar el onboarding.

- **`routine_current_version` (1 fila por rutina del plan):** Se inicializa con `current_version_number=1` para cada rutina que el ejecutante crea al configurar su plan.

- **Plan de entrenamiento de referencia del ejecutante:** El ejecutante configura su plan manualmente al iniciar el sistema. El siguiente es el plan actual que sirve como referencia de validación de la lógica de rotación, prescripción y progresión:
  - **Rutina 1 — Pierna (Leg):** 2 versiones. V1: Aductores, Curl de Isquiotibiales Sentado, Prensa Inclinada, Sentadilla Hack, Extensión de Cuádriceps, Elevación de Pantorrilla en Máquina de Pie. V2: Aductores, Curl de Isquiotibiales Sentado, Peso Muerto Rumano, Hip Thrust / Sentadilla Búlgara / Sentadilla de Zumo (slot con alternativas), Extensión de Cuádriceps, Elevación de Pantorrilla en Máquina de Pie.
  - **Rutina 2 — Push (Pecho, Hombro, Tríceps):** 1 versión. Ejercicios: Elevación Lateral, Press de Banca Inclinado, Press de Banca Plano, Vuelos Posteriores, Extensión de Tríceps por encima de la Cabeza, Cruce de Polea Alta, Extensión de Tríceps en Polea (Pushdown).
  - **Rutina 3 — Pull (Espalda, Bíceps, Abdomen):** 1 versión. Ejercicios: Curl de Concentración / Curl de Predicador (slot con alternativas), Tirón de Dorsales, Remo T Inclinado, Face Pull, Curl Bayesian en Banco Inclinado, Curl de Martillo Cruzado, Crunch Abdominal, Press Pallof.
  - **Rotación del microciclo:** Pierna (V1) → Push → Pull → cierre de microciclo → Pierna (V2) → Push → Pull → cierre → Pierna (V1) → …

### 6.2. Volumen y Depuración (Purge / Archiving)

- **Tasa de Crecimiento Esperada:** `El dato de mayor crecimiento es exercise_set. Con 4 sesiones semanales × 3 rutinas × 7 ejercicios × 4 series = aprox. 336 registros por semana. En 2 años: ~35,000 registros en exercise_set. El volumen total estimado de la base de datos para 2 años de uso intensivo es inferior a 5 MB — dentro de los límites de SQLite local sin necesidad de particionamiento.`
- **Políticas de Depuración:** `El sistema no implementa ninguna política de depuración automática. Los datos de sesiones, series y progresión son inmutables y se conservan indefinidamente — son la fuente de verdad histórica del ejecutante. La gestión del almacenamiento se realiza mediante el mecanismo de backup/restore: el ejecutante puede exportar el historial completo y restaurarlo en cualquier momento. No existe un mecanismo de archiving ni de borrado por antigüedad.`
- **Alertas resueltas:** `Las alertas con is_active = 0 se conservan como histórico indefinidamente. No se purgan. Representan el registro de condiciones que el sistema detectó y resolvió a lo largo del tiempo.`
