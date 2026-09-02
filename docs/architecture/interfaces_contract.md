# Contrato de Interfaces e Interacciones

> Este documento define los "puertos" de comunicación del sistema. Detalla estrictamente cómo el entorno interactúa con el sistema (entradas) y cómo el sistema responde (salidas). **Se mantiene agnóstico a la implementación visual**, enfocándose en la estructura de los datos, eventos, comandos y reglas de validación que gobiernan cada interacción.
>
> **Alcance:** 26 vistas distribuidas en 10 flujos funcionales. Cada vista define sus triggers de entrada (acciones del ejecutante o eventos del sistema), su payload de datos y su respuesta esperada.

---

## 1. Protocolos y Canales de Comunicación

*Define los canales a través de los cuales el sistema escucha y emite información.*

- **Canal Principal:** `Sistema de Input táctil de Android (gestos de toque, deslizamiento y selección sobre la interfaz de usuario compuesta con Jetpack Compose). El sistema opera exclusivamente en modo retrato (portrait). No existen canales de red, API REST, WebSockets ni CLI.`
- **Formato de Intercambio Base:** `UI Events + StateFlow. El ejecutante emite acciones discretas (Intent / UIEvent) desde la capa de presentación; el ViewModel las procesa y emite nuevo estado (UIState) como flujo reactivo hacia la capa de composición. El formato de persistencia interna es SQLite (Room). El formato de exportación/importación de datos es JSON con metadatos de versión.`
- **Autenticación y Autorización:** `Ninguna. La aplicación es de uso personal y opera en modo completamente local (100% offline). No existe autenticación de usuario, sesión de red, ni modelo de permisos de acceso a datos. El único permiso del sistema operativo requerido es almacenamiento externo (para backup/restore).`

---

## 2. Catálogo de Triggers e Interacciones

*Por cada acción o evento que el sistema puede recibir, define el contrato exacto de entrada y salida. Organizado por flujo funcional.*

---

### 2.1. Módulo: `Flujo A — Onboarding`

---

#### `A1-T1`: Registrar Perfil del Ejecutante

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el botón "Registrar" en la vista A1 (Registro de Perfil). Solo disponible al primer lanzamiento de la app sin perfil previo.`
- **Descripción:** El sistema valida los datos del formulario de perfil y, si son válidos, persiste el perfil del ejecutante junto con el primer registro de peso y el estado inicial de rotación. Navega a B1 (Home).

**Payload / Parámetros (Input):**

```json
{
  "weight_kg": "REAL > 0 // Obligatorio. Peso corporal inicial del ejecutante en kilogramos.",
  "height_m": "REAL > 0 // Obligatorio. Altura del ejecutante en metros.",
  "experience_level": "TEXT // Obligatorio. Uno de: 'BEGINNER', 'INTERMEDIATE', 'ADVANCED'."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Perfil creado. Primer registro de peso creado. rotation_state inicializado con microcycle_position=1, microcycle_count=0. weekly_frequency por defecto = 6. Navegación automática a B1.`

```json
{
  "profile_id": 1,
  "weight_record_id": "INTEGER // ID del primer registro de peso creado",
  "navigation": "B1"
}
```

- **Estado de Error:** `Los campos inválidos se marcan con mensaje inline. El botón "Registrar" permanece deshabilitado. No se persiste ningún dato parcial.`

---

### 2.2. Módulo: `Flujo B — Inicio`

---

#### `B1-T1`: Iniciar Nueva Sesión

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el botón "Iniciar Sesión" en la vista B1 (Home). Solo disponible si no hay sesión IN_PROGRESS existente y si el día de hoy tiene rutina resuelta.`
- **Descripción:** El sistema determina la rutina por el **día de la semana** — `week_day.routine_id` del día de hoy, sustituido por `daily_routine_override.routine_id` si existe una reasignación cuya fecha es hoy (`B1-T3`) —, resuelve su versión vigente (o la congelada si hay descarga activa), crea una nueva sesión con status `IN_PROGRESS`, crea una fila `session_exercise` por cada slot del plan de la rutina-versión asignada con el ejercicio primario de cada slot, y navega a E1.
- **Determinación sin propuesta:** si el día no tiene rutina asignada y no hay reasignación vigente, B1 presenta la tarjeta de **día de descanso** en lugar del botón de inicio, con la acción de reasignación como única vía para entrenar (`B1-T3`).
- **Día ya resuelto:** si existe una sesión cerrada con la fecha de hoy o el día está registrado en `day_skip`, B1 **no ofrece nada iniciable**. Presenta la sesión del siguiente día con rutina —saltando los de descanso— con el botón de inicio deshabilitado y la indicación de cuándo estará disponible. Ni el inicio, ni la reasignación temporal (`B1-T3`), ni la omisión (`B1-T5`) se ofrecen: es lo que impide ejecutar varias sesiones el mismo día. El preview (B2) aplica el mismo bloqueo.
- **Frontera con la rotación:** la reasignación se agota en la determinación. El inicio de sesión no distingue si la rutina vino del día o de la reasignación, y `rotation_state` no participa en esta resolución — su avance ocurre íntegramente en `E4-T1`.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Nueva sesión creada. session.status = 'IN_PROGRESS'. session_exercise creados para cada slot del plan. Navegación a E1 con la sesión activa.`

```json
{
  "session_id": "INTEGER // ID de la sesión recién creada",
  "routine_version_id": "INTEGER // Determinado por week_day.routine_id del día de hoy, o por daily_routine_override si aplica",
  "week_day": "TEXT // Dominio cerrado WeekDay: día de hoy",
  "routine_name": "TEXT",
  "version_number": "INTEGER",
  "is_temporary_override": "INTEGER // 0 = relación permanente del día, 1 = reasignación temporal vigente",
  "navigation": "E1"
}
```

---

#### `B1-T2`: Reanudar Sesión Existente

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el botón "Reanudar Sesión" en la vista B1. Solo visible si existe una sesión con status = 'IN_PROGRESS' (crash recovery).`
- **Descripción:** El sistema localiza la sesión activa existente y navega a E1 con su estado parcial intacto. No crea datos nuevos.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Navegación a E1 con la sesión activa reanudada. Todos los datos previos (series registradas, ejercicios finalizados) están intactos.`

```json
{
  "session_id": "INTEGER // ID de la sesión activa existente",
  "navigation": "E1"
}
```

---

#### `B1-T3`: Reasignar Temporalmente la Rutina de Hoy

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Hacer otra rutina hoy" en la tarjeta de sesión propuesta de B1 o en B2 (Preview), o "Entrenar de todas formas" en la tarjeta de día de descanso. Abre un selector sobre la pantalla actual — no hay ruta nueva. Solo disponible si NO existe sesión con status = 'IN_PROGRESS'.`
- **Descripción:** El ejecutante elige una rutina distinta a la que su día tiene asignada y confirma. El sistema persiste la reasignación en `daily_routine_override` (fila única) con la fecha de hoy. La propuesta de B1 y B2 se recompone con la rutina elegida y su versión vigente; la relación permanente `week_day.routine_id` **no se modifica**.
- **Opciones ofrecidas:** toda rutina cuya versión vigente tenga al menos un ejercicio asignado, no solo las que tienen día. La rutina que ya correspondía a hoy se marca como *actual* y **no se excluye** — elegirla es válido y su comportamiento es idéntico a no reasignar.
- **Alcance temporal:** la reasignación aplica únicamente a ese día. No se borra al cerrar la sesión: deja de honrarse cuando su `date` no coincide con la fecha de hoy.

**Payload / Parámetros (Input):**

```json
{
  "routine_id": "INTEGER // Rutina elegida en el selector"
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `daily_routine_override actualizado con date = hoy y la rutina elegida. La propuesta de B1/B2 se recompone. rotation_state NO se modifica. week_day NO se modifica.`

```json
{
  "week_day": "TEXT // Día de hoy — conserva su relación permanente intacta",
  "routine_id": "INTEGER // Rutina que se ejecutará hoy",
  "routine_version_id": "INTEGER // Versión vigente de la rutina reasignada, o la congelada si hay descarga activa",
  "is_temporary_override": 1,
  "navigation": "ninguna — se resuelve sobre la pantalla actual"
}
```

- **Estado de Error:** `ERR_REASSIGN_SESSION_ACTIVE` — se intenta reasignar con una sesión ya iniciada. La rutina queda fijada al iniciar la sesión.

---

#### `B1-T4`: Deshacer Reasignación Temporal

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Deshacer" en la tarjeta de sesión propuesta de B1 o en B2, visible solo cuando hay una reasignación vigente y no hay sesión iniciada.`
- **Descripción:** El sistema borra la fila de `daily_routine_override` y el día vuelve a proponer la rutina de su relación permanente. Es la contrapartida explícita de `B1-T3`; la reversión por cambio de día ocurre igualmente sin esta acción.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `daily_routine_override vacía. La propuesta de B1/B2 vuelve a week_day.routine_id del día de hoy. rotation_state NO se modifica.`

```json
{
  "week_day": "TEXT // Día de hoy",
  "routine_id": "INTEGER | null // Rutina permanente del día; null si es día sin rutina asignada",
  "is_temporary_override": 0,
  "navigation": "ninguna"
}
```

---

#### `B1-T5`: Omitir el Día

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Hoy no entreno" en B1. Se muestra mientras el día no esté resuelto, tanto en la tarjeta de sesión propuesta como en la de reanudar sesión.`
- **Descripción:** El ejecutante declara que hoy no entrena. El sistema registra la fecha en `day_skip` (fila única) y el día queda resuelto: B1 pasa a informar qué toca el siguiente día con rutina, sin permitir iniciarlo.
- **Es la única forma de cancelar el día.** Si hay una sesión `IN_PROGRESS` **sin series**, la acción la descarta: se abrió y no se entrenó nada, y dejarla viva bloquearía el inicio de las siguientes.
- **Se bloquea con la primera serie.** La acción se presenta **deshabilitada** —no ausente— en cuanto la sesión tiene al menos una fila en `exercise_set`, indicando que la salida es reanudar y cerrar como `INCOMPLETE`. A partir de ahí hubo entrenamiento real y cancelarlo lo borraría.
- **No crea sesión.** Cancelar un día es exactamente no haber entrenado: no aparece en el historial, no cuenta en `countSessionsInWeek` y no actualiza la última fecha de ejecución de ninguna rutina.
- **La rotación no se toca.** No hubo sesión, así que `rotation_state` no avanza y el conteo de microciclos no cambia.
- **Caducidad:** la omisión solo se honra el día de su fecha. Al cambiar el día deja de aplicar sin borrado programado, igual que la reasignación temporal.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `day_skip con date = hoy. La sesión IN_PROGRESS sin series, si la había, se borra. No se crea ninguna. rotation_state NO se modifica.`

```json
{
  "week_day": "TEXT // Día de hoy",
  "day_outcome": "SKIPPED",
  "upcoming_week_day": "TEXT // Siguiente día con rutina, saltando los de descanso",
  "upcoming_routine_name": "TEXT",
  "navigation": "ninguna"
}
```

- **Estado de Error:** `ERR_SKIP_SESSION_HAS_SETS` — se intenta cancelar el día con series ya registradas. Esa sesión se resuelve cerrándola.

---

#### `B1-T7`: Resolver la Sesión del Día Anterior (automático)

- **Tipo de Trigger (Entrada):** `Automático: al arrancar la aplicación y al cruzar la medianoche local con la aplicación abierta. Sin intervención del ejecutante.`
- **Descripción:** Una sesión `IN_PROGRESS` cuya `date` es anterior a hoy pertenece a un día que terminó y no puede continuarse; mientras siga viva tapa la propuesta del día nuevo con la tarjeta de reanudar. El sistema hace por el ejecutante lo que él habría hecho:
  - **Con al menos una serie registrada:** se ejecuta el protocolo completo de `E4-T1` y la sesión queda `INCOMPLETE`. Conserva su `date` original — el día que sí se entrenó —, de modo que la adherencia la cuenta donde corresponde y la rotación avanza como en cualquier cierre.
  - **Sin ninguna serie:** se borra. No hubo entrenamiento, así que no llega al historial, no cuenta como adherencia y la rotación no avanza.
- **El día no entrenado no deja registro.** Su ausencia de sesión ya es lo que leen el historial, la adherencia (`countSessionsInWeek`) y la alerta de inactividad (`ROUTINE_INACTIVITY`). No se escribe nada en `day_skip` para días pasados: esa tabla es de fila única y solo describe el día en curso.
- **Limitación declarada:** con la aplicación cerrada no se ejecuta nada. Android no ofrece un temporizador fiable en segundo plano para esto y no se añade uno: el barrido ocurre en cuanto la aplicación vuelve a abrirse, y el resultado observable es el mismo porque el ejecutante solo ve la aplicación cuando la abre.
- **El árbol de entrenamiento se recalcula después de este barrido, nunca antes** (`N1-T1`). El barrido cierra la sesión de ayer **conservando su `date` original**; recalcular primero leería una fecha de último entrenamiento desactualizada y marchitaría el árbol de alguien que sí entrenó. El orden es secuencial dentro de la misma corrutina, no una coincidencia entre dos observadores.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Ninguna sesión IN_PROGRESS de un día anterior. B1 propone la sesión del día en curso.`

```json
{
  "resolved_session_id": "INTEGER | null // Sesión resuelta, o null si no había ninguna",
  "resolution": "TEXT | null // 'CLOSED_AS_INCOMPLETE' o 'DISCARDED'",
  "navigation": "ninguna"
}
```

- **Modo de fallo:** el barrido es *best-effort*. Si falla, la sesión sigue en curso y el ejecutante puede reanudarla y cerrarla a mano; no se interrumpe el arranque.

---

#### `B1-T6`: Deshacer la Omisión del Día

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Sí voy a entrenar" en la tarjeta del día resuelto de B1. Visible solo cuando el día se resolvió por omisión, no por haber entrenado.`
- **Descripción:** Borra la fila de `day_skip` y el día vuelve a proponer su sesión. No existe contrapartida para un día resuelto por entrenamiento: una sesión cerrada no se reabre.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `day_skip vacía. B1 vuelve a proponer la sesión del día.`

```json
{
  "week_day": "TEXT // Día de hoy",
  "day_outcome": null,
  "navigation": "ninguna"
}
```

---

#### `B1-T8`: Abrir el Árbol de Entrenamiento

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca la tarjeta "Tu árbol" de B1, ubicada debajo de la tarjeta de sesión del día.`
- **Descripción:** Navega a la pantalla dedicada del árbol (`N1`). La tarjeta **se compone siempre** — es la única de B1 sin condición de visibilidad, porque el árbol existe desde antes de la primera sesión, aunque sea como semilla — y refleja el estado actual mediante el ícono de su etapa teñido según la salud, más una línea de texto que cambia con el estado. La tarjeta es **nativa de forma permanente**: nunca renderiza contenido web, para no penalizar el arranque de B1 (RNF01).
- **Precondiciones:** Ninguna. Está disponible con sesión activa, con el día resuelto y en día de descanso.
- **Efecto sobre el estado del sistema:** Ninguno. La navegación no escribe nada; el recálculo lo dispara la pantalla de destino (`N1-T1`).

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Navegación a N1. Ruta nueva, sin pestaña propia en la barra de navegación inferior.`

```json
{
  "growth_stage": "TEXT // SEED | SPROUT | YOUNG | MATURE — gobierna la FORMA del ícono",
  "health_score": "INTEGER // 0-100 — gobierna el COLOR del ícono",
  "navigation": "N1"
}
```

---

### 2.3. Módulo: `Flujo C — Perfil del Ejecutante`

---

#### `C1-T1`: Actualizar Perfil del Ejecutante

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el botón "Guardar" en la vista C1 (Perfil del Ejecutante). Solo disponible si hay cambios válidos respecto al perfil actual (dirty state).`
- **Descripción:** El sistema valida los campos modificados. Si el peso cambió, crea un nuevo registro en `weight_record`. Actualiza `profile` con los valores modificados.

**Payload / Parámetros (Input):**

```json
{
  "weight_kg": "REAL > 0 // Opcional. Nuevo peso corporal. Si difiere del actual, genera nuevo weight_record.",
  "height_m": "REAL > 0 // Opcional. Nueva altura.",
  "experience_level": "TEXT // Opcional. Uno de: 'BEGINNER', 'INTERMEDIATE', 'ADVANCED'."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Perfil actualizado. Si peso cambió: nuevo weight_record creado con fecha actual. Retorno a C1 con datos actualizados. Botón "Guardar" vuelve a estado deshabilitado (no dirty).`

```json
{
  "profile_updated": true,
  "weight_record_created": "BOOLEAN // true si weight_kg cambió"
}
```

---

#### `C2-T1`: Consultar Historial de Peso Corporal

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Ver historial de peso" en C1. Transición de lectura.`
- **Descripción:** El sistema recupera todos los registros de `weight_record` ordenados cronológicamente de más reciente a más antiguo y los presenta en C2.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Lista de registros de peso, ordenada descendente por fecha.`

```json
{
  "records": [
    {
      "id": "INTEGER",
      "weight_kg": "REAL",
      "date": "TEXT // ISO 8601",
      "is_initial_record": "BOOLEAN // true para el registro más antiguo"
    }
  ]
}
```

---

### 2.4. Módulo: `Flujo D — Catálogo (Diccionario y Plan)`

---

#### `D1-T1`: Filtrar Diccionario de Ejercicios

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: selecciona un valor en los dropdowns de filtro de la vista D1. El filtro se aplica en tiempo real al cambiar cualquier dropdown.`
- **Descripción:** El sistema consulta `exercise` JOIN `exercise_muscle_zone` JOIN `muscle_zone` y `equipment_type` aplicando los filtros activos. Devuelve la lista filtrada.

**Payload / Parámetros (Input):**

```json
{
  "equipment_type_id": "INTEGER | null // null = sin filtro de equipo ('Todos')",
  "muscle_zone_id": "INTEGER | null // null = sin filtro de zona ('Todos')"
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Lista de ejercicios que cumplen TODOS los filtros activos.`

```json
{
  "exercises": [
    {
      "id": "INTEGER",
      "name": "TEXT",
      "equipment_type": "TEXT",
      "muscle_zones": ["TEXT"],
      "is_custom": "BOOLEAN",
      "is_bodyweight": "BOOLEAN",
      "is_isometric": "BOOLEAN",
      "is_to_technical_failure": "BOOLEAN",
      "media_resource": "TEXT | null"
    }
  ],
  "total_count": "INTEGER // Total en diccionario (sin filtro)",
  "filtered_count": "INTEGER // Ejercicios que cumplen el filtro"
}
```

---

#### `D2-T1`: Consultar Detalle de Ejercicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca un ejercicio en D1, D4, E1 o F3 para ver su ficha completa.`
- **Descripción:** El sistema recupera los datos completos del ejercicio seleccionado incluyendo zonas musculares asociadas y ruta de media visual.

**Payload / Parámetros (Input):**

```json
{
  "exercise_id": "INTEGER // Obligatorio. ID del ejercicio a mostrar."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Datos completos del ejercicio.`

```json
{
  "id": "INTEGER",
  "name": "TEXT",
  "equipment_type": "TEXT",
  "muscle_zones": ["TEXT"],
  "is_custom": "BOOLEAN",
  "is_bodyweight": "BOOLEAN",
  "is_isometric": "BOOLEAN",
  "is_to_technical_failure": "BOOLEAN",
  "media_resource": "TEXT | null // Ruta asset o archivo. null = mostrar placeholder",
  "progression_difficulty": "TEXT ['LOW','MEDIUM','HIGH'] // Dificultad de progresión. Nunca null.",
  "effective_plateau_threshold": "INTEGER // Derivado, no persistido: techo(umbral base del perfil x multiplicador de la dificultad). Se muestra como 'Se considerará estancado tras N sesiones sin progresar'."
}
```

---

#### `D2-T2`: Editar Dificultad de Progresión del Ejercicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: selecciona una opción del selector segmentado de dificultad de progresión en D2.`
- **Descripción:** El sistema persiste `exercise.progression_difficulty` de inmediato, sin botón de guardado — mismo patrón que el cambio de imagen del ejercicio. El umbral efectivo mostrado en la ficha se recalcula en vivo. El contador acumulado de sesiones sin progresión **no** se reinicia: el cambio se aplica en la siguiente evaluación de progresión.

**Payload / Parámetros (Input):**

```json
{
  "exercise_id": "INTEGER // Obligatorio.",
  "progression_difficulty": "TEXT ['LOW','MEDIUM','HIGH'] // Obligatorio. Dominio cerrado."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `exercise.progression_difficulty actualizado. D2 refleja la opción seleccionada y el nuevo umbral efectivo.`

---

#### `D5-T1`: Crear Ejercicio Personalizado

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el botón "Crear" en D5 (Crear Ejercicio) con datos válidos.`
- **Descripción:** El sistema valida la unicidad del par `(name, equipment_type_id)` y persiste el nuevo ejercicio con `is_custom = 1`. Crea las entradas correspondientes en `exercise_muscle_zone`. Si se seleccionó imagen, la copia al almacenamiento interno.

**Payload / Parámetros (Input):**

```json
{
  "name": "TEXT // Obligatorio. No vacío.",
  "equipment_type_id": "INTEGER // Obligatorio. FK válida a equipment_type.",
  "muscle_zone_ids": ["INTEGER"] ,
  "is_bodyweight": "BOOLEAN // Opcional. Default false.",
  "is_isometric": "BOOLEAN // Opcional. Default false. Si true, is_bodyweight debe ser true.",
  "is_to_technical_failure": "BOOLEAN // Opcional. Default false. Si true, is_bodyweight debe ser true. Mutuamente excluyente con is_isometric.",
  "progression_difficulty": "TEXT ['LOW','MEDIUM','HIGH'] // Opcional. Default 'MEDIUM', preseleccionado en el formulario.",
  "image_uri": "TEXT | null // Opcional. URI de galería del dispositivo. null = sin imagen."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Ejercicio creado. Navegación de retorno a D1. El ejercicio aparece en el diccionario con badge 'Personalizado'.`

```json
{
  "exercise_id": "INTEGER // ID del ejercicio creado",
  "navigation": "D1"
}
```

- **Estado de Error (unicidad):** `Snackbar con mensaje de error. No se persiste el ejercicio. El formulario permanece con los datos ingresados.`

---

#### `D3-T1`: Consultar Plan de Entrenamiento

> **Conteo de ejercicios:** la cifra que acompaña a cada versión cuenta **slots**, no asignaciones. Un slot dual son dos ejercicios que se alternan y **un** ejercicio de la sesión: o se hace uno o se hace el otro. Es la misma unidad que usan el preview de sesión, la creación de `session_exercise` al iniciar y el protocolo de descarga.


- **Tipo de Trigger (Entrada):** `Evento de sistema: carga de la vista D3 (Plan de Entrenamiento) al acceder desde la pestaña "Plan".`
- **Descripción:** El sistema recupera todas las rutinas con sus versiones y el conteo de ejercicios por versión, ordenadas por `sort_order`.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "routines": [
    {
      "id": "INTEGER",
      "name": "TEXT",
      "sort_order": "INTEGER",
      "versions": [
        {
          "routine_version_id": "INTEGER",
          "version_number": "INTEGER",
          "exercise_count": "INTEGER"
        }
      ]
    }
  ]
}
```

---

#### `D4-T1`: Consultar Detalle de Versión del Plan

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: selecciona una rutina-versión en D3.`
- **Descripción:** El sistema recupera los ejercicios asignados a la `routine_version_id` seleccionada, agrupados por `slot` y ordenados por `sort_order`.

**Payload / Parámetros (Input):**

```json
{
  "routine_version_id": "INTEGER // Obligatorio."
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "routine_name": "TEXT",
  "version_number": "INTEGER",
  "slots": [
    {
      "slot": "INTEGER",
      "sort_order": "INTEGER",
      "sets": "INTEGER",
      "reps": "TEXT // '8-12', 'TO_TECHNICAL_FAILURE' o '30-45_SEC'",
      "exercises": [
        {
          "exercise_id": "INTEGER",
          "name": "TEXT",
          "equipment_type": "TEXT",
          "muscle_zones": ["TEXT"],
          "is_bodyweight": "BOOLEAN",
          "is_isometric": "BOOLEAN",
          "is_to_technical_failure": "BOOLEAN"
        }
      ]
    }
  ]
}
```

---

#### `D4-T2`: Desasignar Ejercicio (Slot) del Plan

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el ícono de eliminar en una fila de ejercicio en D4. Requiere confirmación. Solo disponible si no hay sesión activa de esa versión.`
- **Descripción:** El sistema elimina todas las entradas de `plan_assignment` que pertenecen al mismo `slot` en la `routine_version_id`. No elimina el ejercicio del diccionario ni afecta su historial.

**Payload / Parámetros (Input):**

```json
{
  "routine_version_id": "INTEGER",
  "slot": "INTEGER // Todas las alternativas del slot se eliminan."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Slot eliminado. Vista D4 actualizada sin el slot.`

---

#### `D6-T1`: Crear o Editar Rutina

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el botón de guardar en D6 (Crear/Editar Rutina).`
- **Descripción:** Para creación: persiste nueva `routine` con `sort_order = MAX(sort_order) + 1` y crea una `routine_version` con `version_number = 1` y su `routine_current_version` inicial. Para edición: actualiza `routine.name`.

**Payload / Parámetros (Input):**

```json
{
  "routine_id": "INTEGER | null // null = crear nueva. ID válido = editar existente.",
  "name": "TEXT // Obligatorio. Max 50 caracteres. UNIQUE."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito (creación):** `Rutina creada con versión 1. routine_current_version inicializada con current_version_number=1. Retorno a D3.`
- **Estado de Éxito (edición):** `Nombre actualizado. Retorno a D3.`

---

#### `D6-T2`: Asignar Días de la Semana a una Rutina

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el control de días en la fila de una rutina, dentro de la gestión del plan (D6). Abre un selector múltiple de los 7 días sobre la pantalla actual.`
- **Descripción:** El ejecutante marca los días que ejecutan esa rutina y confirma. El sistema fija `week_day.routine_id` de cada día seleccionado a esa rutina, y libera los días que la rutina tenía y ya no están marcados. Es la edición **permanente** de la relación, distinta de la reasignación temporal de una sesión (`B1-T3`).
- **Cardinalidad:** una rutina puede ocupar **varios días**; un día ocupa **una sola rutina**. Marcar un día que pertenece a otra rutina lo **mueve**: aquella rutina lo pierde. El selector lo advierte antes de confirmar.
- **Días liberados:** un día que deja de tener rutina queda de descanso y así se presenta en el plan y en el inicio.
- **Restricción:** no disponible durante una descarga activa. La descarga cierra al ejecutar tantas sesiones como versiones congeladas, y dejar sin días a una rutina congelada haría que su sesión no se propusiera nunca.

**Payload / Parámetros (Input):**

```json
{
  "routine_id": "INTEGER // Rutina cuyos días se están fijando",
  "week_days": "TEXT[] // Dominio cerrado WeekDay. Vacío deja la rutina sin días"
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `week_day actualizado en una transacción. Los días marcados apuntan a la rutina; los que la rutina tenía y no fueron marcados quedan en NULL. rotation_state NO se modifica. daily_routine_override NO se modifica.`

```json
{
  "routine_id": "INTEGER",
  "week_days": "TEXT[] // Días que ejecutan la rutina tras el cambio",
  "released_week_days": "TEXT[] // Días que quedaron de descanso",
  "navigation": "ninguna — se resuelve sobre la pantalla actual"
}
```

- **Estado de Error:** `ERR_WEEK_DAYS_DELOAD_ACTIVE` — se intenta cambiar los días durante una descarga activa.

---

### 2.5. Módulo: `Flujo E — Sesión Activa`

---

#### `E1-T1`: Intercambiar Alternativa de Ejercicio (Swap)

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el ícono de intercambio (⇄) en un ejercicio con estado 'No Iniciado' que tiene múltiples alternativas en su slot. Despliega bottom sheet con las alternativas disponibles.`
- **Descripción:** El sistema actualiza `session_exercise.exercise_id` al ejercicio alternativo seleccionado. La fila no conserva rastro del ejercicio anterior: la alternativa no es una desviación del plan, es una de sus opciones declaradas. Solo posible con 0 series registradas. Desde HU-34 es el **único** mecanismo de cambio de ejercicio durante la sesión.

**Payload / Parámetros (Input):**

```json
{
  "session_exercise_id": "INTEGER",
  "new_exercise_id": "INTEGER // Obligatorio. Debe pertenecer al mismo slot en plan_assignment."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `session_exercise.exercise_id actualizado. Vista E1 refleja el nuevo ejercicio seleccionado. La carga objetivo se recalcula para el nuevo ejercicio.`

---

#### `E2-T1`: Registrar Serie de Ejercicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: completa el formulario E2 (Registro de Serie) y toca el botón de confirmar.`
- **Descripción:** El sistema valida los datos y persiste un nuevo `exercise_set`. Asigna automáticamente `set_number = COUNT(series previas del ejercicio en la sesión) + 1`. Si es la primera serie del ejercicio, crea `exercise_progression` si no existe.

  El **valor precargado** en el campo de peso se resuelve con una precedencia estricta: (1) la carga prescrita por el motor de Doble Umbral mientras siga **activa**, (2) el peso de la serie anterior del mismo ejercicio en la sesión actual, (3) el peso de la última serie del mismo ejercicio en su sesión cerrada más reciente, (4) campo vacío. Una prescripción está *activa* mientras supere el último peso efectivamente manejado por más de 0.01 Kg: representa un aumento que el ejecutante aún no ha alcanzado. Una vez alcanzada o superada, la prescripción queda consumida y la memoria del último peso toma el relevo, de modo que la precarga acompaña la progresión en lugar de volver a un valor obsoleto. La memoria se resuelve sobre el **ejercicio efectivamente ejecutado** (`session_exercise.exercise_id`) y excluye las sesiones de descarga; la prescripción se resuelve sobre el mismo `session_exercise.exercise_id`, porque `exercise_progression` es una tabla por slot y desde HU-34 el slot **es** el ejercicio que la sesión sostiene: la sustitución por grupo muscular era la única forma de que ambos divergieran y ya no existe. En un microciclo de descarga, la carga de descarga calculada conserva su prioridad sobre la memoria. El valor precargado es siempre editable: el sistema sugiere, no impone.

  El formulario E2 ofrece un **selector de unidad de captura** (`Kg` / `Lb`) junto al campo de peso, más controles de incremento y decremento cuyo paso depende de la unidad activa (0.5 Kg en kilogramos, 1 lb en libras). El selector se preselecciona con la unidad de la última serie registrada del mismo ejercicio y se oculta para ejercicios de peso corporal e isométricos. La conversión a kilogramos ocurre en la capa de presentación antes de invocar el trigger: `weight_kg` llega **siempre en la unidad canónica**.

**Payload / Parámetros (Input):**

```json
{
  "session_exercise_id": "INTEGER // Obligatorio.",
  "weight_kg": "REAL [0, 500] // Obligatorio. Valor canónico en kilogramos, ya convertido desde capture_unit. 0 para ejercicios de peso corporal e isométricos.",
  "reps": "INTEGER >= 1 // Obligatorio. Para isométricos: segundos sostenidos.",
  "rir": "INTEGER [0, 1, 2] // Obligatorio. Reserva de esfuerzo percibida.",
  "capture_unit": "TEXT ['KG', 'LB'] // Obligatorio. Unidad en la que el ejecutante capturó el valor. Se persiste para preseleccionar el selector y mostrarla en el detalle de la serie. Se fuerza a 'KG' en ejercicios de peso corporal e isométricos."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `exercise_set creado. set_number asignado automáticamente. Estado del session_exercise pasa a 'IN_EXECUTION' si es la primera serie. Retorno a E1 con el estado actualizado.`

```json
{
  "exercise_set_id": "INTEGER",
  "set_number": "INTEGER",
  "navigation": "E1"
}
```

- **Estado de Error:** `Valores fuera de rango no se persisten. El formulario muestra error inline en el campo inválido. El botón de confirmar permanece deshabilitado. La validación del peso se evalúa siempre sobre el valor ya convertido a kilogramos, nunca sobre el valor capturado: no numérico, negativo o superior a 500 Kg equivalentes. El mensaje de máximo expresa el límite en la unidad activa (500 Kg / 1102.3 lb).`

---

#### `E4-T1`: Cerrar Sesión

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: confirma el cierre en el diálogo E4 (Confirmación de Cierre de Sesión). La confirmación está deshabilitada mientras la sesión no tenga ninguna serie registrada.`
- **Precondición — al menos una serie:** cerrar da por terminado lo entrenado, y sin ninguna fila en `exercise_set` no hay nada que terminar. La sesión permanece `IN_PROGRESS` y es reanudable; para resolver el día sin entrenar, la vía es `B1-T5`.
- **Descripción:** El sistema ejecuta el protocolo de cierre de sesión: (1) finaliza todos los `session_exercise` no finalizados, (2) calcula tonelaje, (3) ejecuta el motor de reglas por cada ejercicio (comparación histórica, clasificación de progresión, actualización de `exercise_progression`, detección de mesetas y alertas), (4) actualiza `rotation_state`, (5) determina el status de sesión, (6) navega a E5.

**Payload / Parámetros (Input):**

```json
{
  "session_id": "INTEGER",
  "close_as": "TEXT // 'COMPLETED' si todos los ejercicios están finalizados; 'INCOMPLETE' si hay ejercicios sin finalizar. Determinado por el sistema, confirmado por el ejecutante."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `session.status actualizado. Todas las exercise_progression actualizadas. Alertas generadas si aplica. rotation_state avanzado. Navegación automática a E5.`

```json
{
  "session_status": "TEXT // 'COMPLETED' o 'INCOMPLETE'",
  "tonnage_total": "REAL // Σ (weight_kg × reps) de todas las series",
  "exercise_summaries": [
    {
      "exercise_id": "INTEGER",
      "name": "TEXT",
      "progression_classification": "TEXT | null",
      "prescribed_load_next": "REAL | null",
      "action_signal": "TEXT // 'INCREASE_LOAD', 'MAINTAIN', 'DELOAD_RECOMMENDED', 'NO_HISTORY', 'MASTERED'"
    }
  ],
  "navigation": "E5"
}
```

---

#### `E5-T1`: Consultar Resumen Post-Sesión

- **Tipo de Trigger (Entrada):** `Evento de sistema: navegación automática desde E4 tras cierre exitoso de sesión.`
- **Descripción:** El sistema presenta el resumen calculado durante el cierre de sesión. Vista de solo lectura.

**Payload / Parámetros (Input):**

```json
{
  "session_id": "INTEGER // ID de la sesión recién cerrada."
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "session_status": "TEXT",
  "date": "TEXT",
  "routine_name": "TEXT",
  "version_number": "INTEGER",
  "tonnage_total": "REAL",
  "exercises_completed": "INTEGER",
  "exercises_total": "INTEGER",
  "exercise_summaries": [
    {
      "exercise_id": "INTEGER",
      "name": "TEXT",
      "progression_classification": "TEXT | null",
      "prescribed_load_next": "REAL | null",
      "action_signal": "TEXT",
      "is_mastered": "BOOLEAN // true para isométricos dominados"
    }
  ]
}
```

---

### 2.6. Módulo: `Flujo F — Historial`

---

#### `F1-T1`: Consultar Historial de Sesiones

- **Tipo de Trigger (Entrada):** `Evento de sistema: carga de la vista F1 (Historial de Sesiones).`
- **Descripción:** El sistema recupera todas las sesiones cerradas (`status != 'IN_PROGRESS'`) ordenadas cronológicamente descendente.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "sessions": [
    {
      "id": "INTEGER",
      "date": "TEXT",
      "routine_name": "TEXT",
      "version_number": "INTEGER",
      "status": "TEXT // 'COMPLETED' o 'INCOMPLETE'",
      "tonnage_total": "REAL // Calculado: Σ (weight_kg × reps)"
    }
  ]
}
```

---

#### `F2-T1`: Consultar Detalle de Sesión Pasada

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca una sesión en F1.`
- **Descripción:** El sistema recupera la sesión completa con sus ejercicios y series. Refleja el ejercicio que realmente se ejecutó — el del plan, o la alternativa del slot si el ejecutante la intercambió (HU-26).

**Payload / Parámetros (Input):**

```json
{
  "session_id": "INTEGER"
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "session_id": "INTEGER",
  "date": "TEXT",
  "routine_name": "TEXT",
  "version_number": "INTEGER",
  "status": "TEXT",
  "tonnage_total": "REAL",
  "exercises": [
    {
      "exercise_id": "INTEGER",
      "name": "TEXT",
      "progression_classification": "TEXT | null",
      "sets": [
        {
          "set_number": "INTEGER",
          "weight_kg": "REAL",
          "reps": "INTEGER",
          "rir": "INTEGER",
          "capture_unit": "TEXT ['KG', 'LB'] // Única ubicación de la app donde la unidad de captura se presenta. Cuando es 'LB', el detalle añade el valor original en libras bajo el valor en kilogramos. El tonelaje y todos los agregados permanecen en kilogramos."
        }
      ]
    }
  ]
}
```

---

#### `F3-T1`: Consultar Historial de Ejercicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: navega a F3 desde D2, E5, F2, G1 o H2 con un ejercicio específico.`
- **Descripción:** El sistema recupera todos los registros históricos del ejercicio. Calcula la tendencia de carga.

**Payload / Parámetros (Input):**

```json
{
  "exercise_id": "INTEGER // Obligatorio."
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "exercise_id": "INTEGER",
  "name": "TEXT",
  "progression_status": "TEXT // Estado actual: 'NO_HISTORY', 'IN_PROGRESSION', 'IN_PLATEAU', 'IN_DELOAD', 'MASTERED'",
  "prescribed_load_next": "REAL | null",
  "sessions_without_progression": "INTEGER",
  "history": [
    {
      "session_id": "INTEGER",
      "date": "TEXT",
      "routine_name": "TEXT",
      "version_number": "INTEGER",
      "weight_kg_avg": "REAL",
      "reps_avg": "REAL",
      "rir_avg": "REAL",
      "tonnage": "REAL",
      "progression_classification": "TEXT | null"
    }
  ],
  "load_trend": [
    {
      "date": "TEXT",
      "weight_kg": "REAL // Peso máximo registrado en esa sesión para este ejercicio"
    }
  ]
}
```

---

### 2.7. Módulo: `Flujo G — Métricas y KPIs`

---

#### `G1-T1`: Consultar Panel de Métricas (KPIs)

- **Tipo de Trigger (Entrada):** `Evento de sistema: carga de la vista G1 (Panel de Métricas).`
- **Descripción:** El sistema calcula los 4 KPIs principales para todos los ejercicios y rutinas del ejecutante y los entrega como indicadores autoexplicativos: etiqueta, valor, unidad, descripción, período y estado.

**Payload / Parámetros (Input):**

```json
{
  "period_weeks": "INTEGER // Ventana de la tasa de progresión y de la velocidad de carga. Default: 4 semanas.",
  "rir_session_limit": "INTEGER // Ventana del RIR promedio, en sesiones. Default: 2."
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "progression_rates": [
    {
      "exercise_id": "INTEGER",
      "exercise_name": "TEXT",
      "rate_pct": "REAL // % sesiones con progresión positiva en el período",
      "observations": "INTEGER // Clasificaciones sobre las que se calculó. Evidencia de suficiencia, no entra en el cálculo",
      "label": "TEXT // 'Tasa de progresión'",
      "unit": "TEXT // 'PERCENTAGE'",
      "description": "TEXT // 'Sesiones en las que subiste carga'",
      "period": "TEXT // 'últimas N semanas'",
      "state": "TEXT // 'AVAILABLE' | 'INSUFFICIENT' | 'NOT_APPLICABLE'",
      "requirement": {
        "kind": "TEXT // 'EXERCISE_OBSERVATIONS'. Solo cuando state = 'INSUFFICIENT'",
        "available": "INTEGER",
        "needed": "INTEGER"
      }
    }
  ],
  "load_velocity": [
    {
      "exercise_id": "INTEGER",
      "exercise_name": "TEXT",
      "kg_per_session": "REAL // (peso_actual - peso_inicial) / sesiones_intermedias",
      "session_count": "INTEGER // Sesiones sobre las que se calculó. Evidencia de suficiencia",
      "label": "TEXT // 'Velocidad de carga'",
      "unit": "TEXT // 'KILOGRAM_PER_SESSION'",
      "description": "TEXT // 'Carga que sumas de media en cada sesión'",
      "period": "TEXT // 'últimas N semanas'",
      "state": "TEXT // 'NOT_APPLICABLE' en ejercicios de peso corporal e isométricos",
      "requirement": {
        "kind": "TEXT // 'EXERCISE_SESSIONS'",
        "available": "INTEGER",
        "needed": "INTEGER"
      }
    }
  ],
  "rir_averages": [
    {
      "routine_id": "INTEGER",
      "routine_name": "TEXT",
      "rir_avg": "REAL",
      "interpretation": "TEXT // 'OPTIMAL' (≈1), 'TOO_LOW' (<0.5), 'TOO_HIGH' (>1.8)",
      "recorded_sets": "INTEGER // Series con RIR registrado en la ventana. Evidencia de suficiencia",
      "label": "TEXT // 'RIR promedio por módulo'",
      "unit": "TEXT // 'RIR'",
      "description": "TEXT // 'Repeticiones que te quedaban en reserva al terminar cada serie'",
      "period": "TEXT // 'últimas N sesiones'",
      "state": "TEXT",
      "requirement": {
        "kind": "TEXT // 'ROUTINE_SETS'",
        "available": "INTEGER",
        "needed": "INTEGER"
      }
    }
  ],
  "adherence": {
    "completed_this_week": "INTEGER",
    "planned_this_week": "INTEGER",
    "adherence_pct": "REAL",
    "label": "TEXT // 'Adherencia semanal'",
    "unit": "TEXT // 'PERCENTAGE'",
    "description": "TEXT // 'N de M sesiones completadas'",
    "period": "TEXT // 'semana actual'",
    "state": "TEXT",
    "requirement": {
      "kind": "TEXT // 'WEEKLY_TARGET'",
      "available": "INTEGER",
      "needed": "INTEGER"
    }
  },
  "microcycle_count": "INTEGER"
}
```

**Agrupación en secciones (CA-35.02):** `ADHERENCIA` (adherencia semanal), `INTENSIDAD` (RIR promedio por módulo) y `PROGRESIÓN` (tasa de progresión y velocidad de carga).

---

#### `G2-T1`: Consultar Volumen por Grupo Muscular

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Volumen por Grupo Muscular" en G1.`
- **Descripción:** El sistema calcula el tonelaje acumulado y la distribución de volumen del microciclo seleccionado, más la evolución del tonelaje a lo largo de todos los microciclos.

**Payload / Parámetros (Input):**

```json
{
  "microcycle_number": "INTEGER // Microciclo consultado. Default: el último registrado."
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "sessions_in_selected_microcycle": "INTEGER // Evidencia de suficiencia del tonelaje y de la distribución",
  "tonnage_by_group": {
    "label": "TEXT // 'Tonelaje por grupo muscular'",
    "unit": "TEXT // 'KILOGRAM'",
    "description": "TEXT // 'Peso total que levantaste en cada grupo'",
    "period": "TEXT // 'microciclo N'",
    "state": "TEXT // 'INSUFFICIENT' cuando el microciclo no tiene ninguna sesión",
    "requirement": {
      "kind": "TEXT // 'MICROCYCLE_SESSIONS'",
      "available": "INTEGER",
      "needed": "INTEGER"
    },
    "items": [
      {
        "muscle_group": "TEXT",
        "tonnage_kg": "REAL // Cero legítimo cuando el grupo no se entrenó en un microciclo con sesiones"
      }
    ]
  },
  "volume_distribution": {
    "label": "TEXT // 'Distribución de volumen'",
    "unit": "TEXT // 'PERCENTAGE'",
    "description": "TEXT // 'Reparto de tus series entre las zonas de cada grupo'",
    "period": "TEXT // 'microciclo N'",
    "state": "TEXT",
    "items": [
      {
        "muscle_zone": "TEXT",
        "muscle_group": "TEXT",
        "pct_of_routine": "REAL"
      }
    ]
  },
  "tonnage_evolution": {
    "label": "TEXT // 'Evolución del tonelaje'",
    "unit": "TEXT // 'KILOGRAM'",
    "description": "TEXT // 'Peso total levantado en cada microciclo'",
    "period": "TEXT // 'todos los microciclos'",
    "state": "TEXT // 'INSUFFICIENT' por debajo de 2 microciclos",
    "requirement": {
      "kind": "TEXT // 'COMPLETE_MICROCYCLES'",
      "available": "INTEGER",
      "needed": "INTEGER"
    },
    "series": [
      {
        "microcycle_number": "INTEGER",
        "tonnage_by_group": "MAP<TEXT, REAL>"
      }
    ]
  }
}
```

**Gráfica (CA-35.06):** el eje Y se rotula con la unidad (`kg`), el eje X con su significado (`microciclo`) y cada punto con la etiqueta `mcN`. Las series y las etiquetas de eje toman su color del tema, con paleta propia para claro y oscuro.

---

#### `G3-T1`: Consultar Tendencia de Progresión por Grupo Muscular

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Tendencia de Progresión" en G1.`
- **Descripción:** El sistema evalúa la trayectoria de tonelaje y tasa de progresión de cada grupo muscular en los últimos microciclos completos.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "label": "TEXT // 'Tendencia por grupo muscular'",
  "description": "TEXT // 'Hacia dónde va tu tonelaje y tu progresión en cada grupo'",
  "period": "TEXT // 'últimos N microciclos completos'",
  "evaluated_microcycles": "INTEGER // Ventana efectiva de clasificación",
  "state": "TEXT // 'INSUFFICIENT' por debajo de 4 microciclos completos",
  "requirement": {
    "kind": "TEXT // 'COMPLETE_MICROCYCLES'",
    "available": "INTEGER",
    "needed": "INTEGER"
  },
  "trends": [
    {
      "muscle_group": "TEXT",
      "classification": "TEXT // 'ASCENDING', 'STABLE', 'DECLINING'"
    }
  ]
}
```

---

#### Estados de un indicador y suficiencia de datos

Todo indicador de analítica se presenta en exactamente uno de tres estados, mutuamente excluyentes:

| Estado | Significado | Presentación |
| --- | --- | --- |
| `AVAILABLE` | El indicador se calculó. El valor puede ser cero de forma legítima | Valor con su unidad, en la tipografía dominante de la tarjeta |
| `INSUFFICIENT` | No hay historial suficiente para calcularlo | Marcador neutro más la frase de qué falta y cuánto. **Nunca** un cero ni un guion |
| `NOT_APPLICABLE` | El indicador no aplica al elemento — carga externa en ejercicios de peso corporal o isométricos | "No aplica" |

**El cero calculado y el dato ausente son estados distintos** (CA-35.07): un grupo muscular sin entrenar dentro de un microciclo con sesiones vale `0 kg`; un microciclo sin ninguna sesión no vale cero, declara qué le falta.

Los umbrales de suficiencia no son una calibración nueva: cada uno transcribe la guarda que la regla de cálculo correspondiente ya ejecutaba antes de devolver `0.0`. Viven en `MetricSufficiencyRules` y esta tabla es su transcripción.

| Indicador | Guarda de la regla de cálculo | Evidencia propagada | Mínimo |
| --- | --- | --- | :---: |
| Tasa de progresión | `ProgressionRateRule`: `totalCount == 0` | `observations` | 1 |
| Velocidad de carga | `LoadVelocityRule`: `sessionCount <= 1` | `session_count` | 2 |
| RIR promedio | `AvgRirRule`: lista de valores vacía | `recorded_sets` | 1 |
| Adherencia semanal | `AdherenceRule`: `plannedSessions == 0` | `planned_this_week` | 1 |
| Tonelaje por grupo | Microciclo sin sesiones | `sessions_in_selected_microcycle` | 1 |
| Distribución de volumen | `VolumeDistributionRule`: `totalSets == 0` | `sessions_in_selected_microcycle` | 1 |
| Evolución del tonelaje | Serie de un solo punto | `microcycle_count` | 2 |
| Tendencia por grupo | `GetMuscleGroupTrendUseCase`: `< 4` microciclos completos | microciclos completos | 4 |

**Unidad de presentación.** El kilogramo es la unidad de toda carga y todo tonelaje agregado de la analítica, con independencia de la unidad de captura (`exercise_set.capture_unit`, HU-30). La conversión solo interviene en captura y en el detalle de sesión pasada; ningún agregado de este flujo pasa por ella. El símbolo se escribe `kg`.

---

### 2.8. Módulo: `Flujo H — Alertas`

---

#### `H1-T1`: Consultar Centro de Alertas

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el badge de alertas en B1.`
- **Descripción:** El sistema recupera todas las alertas activas (`is_active = 1`) ordenadas por nivel de severidad descendente (CRISIS primero).

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "active_alerts": [
    {
      "id": "INTEGER",
      "type": "TEXT",
      "level": "TEXT // 'CRISIS', 'HIGH_ALERT', 'MEDIUM_ALERT'",
      "entity_name": "TEXT // Nombre del ejercicio, rutina o grupo muscular afectado",
      "message": "TEXT // Frase en lenguaje natural: qué se detectó y sobre qué elemento, con la cifra que lo originó. Sin identificadores internos ni terminología del motor",
      "created_at": "TEXT"
    }
  ],
  "total_count": "INTEGER"
}
```

---

#### `H2-T1`: Consultar Detalle de Alerta

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca una alerta en H1.`
- **Descripción:** El sistema recupera el detalle completo de la alerta, recalcula dinámicamente los datos que la dispararon, redacta la explicación causal y resuelve la acción sugerida. **Toda alerta lleva acción sugerida**: ninguna se limita a describir el problema.

**Payload / Parámetros (Input):**

```json
{
  "alert_id": "INTEGER"
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "alert_id": "INTEGER",
  "type": "TEXT",
  "level": "TEXT",
  "entity_name": "TEXT",
  "message": "TEXT",
  "trigger_data": {},
  "causal_analysis": "TEXT // Explicación en lenguaje natural. Cuando el umbral depende de la dificultad del ejercicio, justifica por qué el sistema esperó lo que esperó",
  "suggested_action": {
    "kind": "TEXT // Acción concreta resuelta por el motor",
    "text": "TEXT // Redacción en segunda persona de lo que el ejecutante puede hacer",
    "target": "OBJECT | null // Destino del acceso directo. null cuando la acción no es navegable (revisar técnica, dejar repeticiones en reserva)"
  }
}
```

**Destinos posibles de `suggested_action.target`** — todos reutilizan rutas existentes, no se crean pantallas nuevas:

| Destino | Ruta | Se propone cuando |
| --- | --- | --- |
| `ExerciseHistory` | `exercise-history/{exerciseId}` | La acción es sobre un ejercicio concreto (subir carga, extender repeticiones) |
| `TrainingPlan` | `training-plan` | La acción es sobre la composición o el volumen del plan (cambiar por la alternativa del puesto, rotar versión, ajustar volumen, retomar el módulo) |
| `DeloadManagement` | `deload` | La acción es activar el protocolo de descarga |

---

#### Umbrales, ventanas y justificación de las cinco familias

Los valores viven en un único punto del código, `AlertThresholdRule`. Esta tabla es su transcripción.

| Familia | Alerta | Crisis | Ventana | Justificación |
| --- | --- | --- | --- | --- |
| **Tasa de progresión** | Ponderada por dificultad del ejercicio | Ponderada por dificultad | 6 semanas, mínimo 3 sesiones clasificadas | Cuatro semanas no distinguen una racha mala de un estancamiento. El mínimo de observaciones evita que una tasa calculada sobre una o dos sesiones se trate como tendencia |
| **RIR fuera de rango** | RIR promedio < 0.5 o > 1.8 | — (nivel único) | 3 sesiones consecutivas | Con dos sesiones, una jornada de mal sueño ya bastaba para disparar. Sobre los umbrales, ver la nota de escala más abajo |
| **Adherencia semanal** | < 60% durante 2 semanas consecutivas | < 60% durante 3+ semanas | Semanal, mirando 4 semanas atrás | Una semana mala no es un problema de adherencia; es una semana mala |
| **Caída de tonelaje** | Caída > 15% | Caída > 25% | 2 microciclos consecutivos | El 10% anterior cabía dentro de la fluctuación normal entre microciclos. Una descarga planificada nunca levanta esta familia |
| **Inactividad por módulo** | > 14 días naturales | > 21 días naturales | Días naturales | A los 10 días aún no hay pérdida de adaptación que reportar |

**Ponderación de la tasa de progresión por dificultad del ejercicio.** La dificultad es el atributo `exercise.progression_difficulty` introducido en HU-32. Un ejercicio difícil de progresar no se juzga con la vara de uno fácil:

| Dificultad | Umbral de alerta | Umbral de crisis |
| --- | :---: | :---: |
| `LOW` | < 40% | < 20% |
| `MEDIUM` | < 35% | < 15% |
| `HIGH` | < 25% | < 10% |

**Nota de escala en la familia RIR.** El criterio de origen de HU-33 fijaba la condición en `RIR promedio < 1.5 o > 3.5`, valores propios de una escala de RIR 0–5. En este sistema el RIR se captura en escala **0–2** (`RegisterSetUseCase` valida `rir in 0..2`). Aplicados literalmente, `> 3.5` sería inalcanzable —la mitad de "estímulo insuficiente" de la familia dejaría de emitir en silencio— y `< 1.5` cubriría el rango de trabajo normal, disparando en casi toda rutina. Se conservan por tanto los umbrales `0.5` y `1.8`, que son los calibrados contra la escala real, y de la revisión se aplica el cambio que sí corresponde al criterio: la ampliación de la ventana de 2 a 3 sesiones consecutivas.

**Ventana incompleta.** Cuando no existe historial suficiente para completar la ventana de una familia, esa familia no evalúa y **no** se emite ninguna alerta de datos insuficientes.

---

### 2.9. Módulo: `Flujo I — Gestión de Descarga`

---

#### `I1-T1`: Activar Modo Descarga

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Activar Descarga" en I1 y confirma. Solo disponible si no hay descarga activa y el motor recomienda descarga.`
- **Descripción:** El sistema crea un nuevo registro `deload` con `status = 'ACTIVE'` y `activation_date = hoy`. Congela las versiones actuales de cada rutina en `deload_frozen_version`. Las sesiones subsiguientes usarán parámetros de descarga (carga al 60%, 8 reps, RIR 2).

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `deload creado con status='ACTIVE'. deload_frozen_version creados para cada rutina. Vista I1 muestra el estado de descarga activa.`

```json
{
  "deload_id": "INTEGER",
  "activation_date": "TEXT",
  "total_routines": "INTEGER // N sesiones que durará la descarga",
  "frozen_versions": [
    {
      "routine_name": "TEXT",
      "frozen_version_number": "INTEGER"
    }
  ]
}
```

---

#### `I1-T2`: Consultar Estado de Descarga

- **Tipo de Trigger (Entrada):** `Evento de sistema: carga de la vista I1.`
- **Descripción:** El sistema determina si hay descarga activa y presenta el estado correspondiente.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "has_active_deload": "BOOLEAN",
  "deload_id": "INTEGER | null",
  "sessions_completed": "INTEGER | null",
  "sessions_total": "INTEGER | null",
  "restart_loads": [
    {
      "exercise_name": "TEXT",
      "restart_load_kg": "REAL // 90% de la carga pre-descarga. Solo disponible al finalizar."
    }
  ],
  "routines_requiring_deload": [
    {
      "routine_id": "INTEGER",
      "routine_name": "TEXT"
    }
  ]
}
```

---

### 2.10. Módulo: `Flujo J — Ajustes y Respaldo`

---

#### `J1-T1`: Actualizar Frecuencia Semanal Objetivo

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: selecciona un nuevo valor en el selector de frecuencia semanal en J1.`
- **Descripción:** El sistema actualiza `profile.weekly_frequency`. El cambio afecta inmediatamente el cálculo del Índice de Adherencia.

**Payload / Parámetros (Input):**

```json
{
  "weekly_frequency": "INTEGER [4, 5, 6] // Obligatorio."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `profile.weekly_frequency actualizado. Vista J1 refleja el nuevo valor seleccionado.`

---

#### `J1-T2`: Actualizar Umbral Base de Estancamiento

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca los controles − / + del umbral base de estancamiento en la sección Entrenamiento de J1.`
- **Descripción:** El sistema actualiza `profile.plateau_base_threshold` en pasos de 1 dentro del rango 3 a 15. Los controles se deshabilitan en los extremos del rango. El desglose por dificultad (Baja / Media / Alta) se recalcula en vivo. El cambio rige desde la siguiente evaluación de progresión: **no** recalcula estados ya asignados ni reinicia contadores acumulados.

**Payload / Parámetros (Input):**

```json
{
  "plateau_base_threshold": "INTEGER [3..15] // Obligatorio. Default 5."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `profile.plateau_base_threshold actualizado. J1 refleja el nuevo valor y el desglose recalculado: Baja x1, Media x1.5 y Alta x2, redondeando hacia arriba.`

- **Estado de Error (fuera de rango):** `El valor se rechaza en la capa de dominio, no se persiste y J1 muestra el mensaje "El umbral debe estar entre 3 y 15".`

---

#### `J2-T1`: Exportar Respaldo (Backup)

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Exportar datos" en J2 tras leer la advertencia de contenido no cifrado.`
- **Descripción:** El sistema serializa todos los datos de la base de datos local en formato JSON con metadatos de versión y genera un archivo de backup en el almacenamiento del dispositivo. El proceso debe completarse en menos de 10 segundos para historial de hasta 2 años.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Archivo JSON generado con todos los datos. Metadatos incluyen versión del esquema (13) y fecha de exportación. Opciones para compartir el archivo vía apps del sistema.`

```json
{
  "file_path": "TEXT // Ruta del archivo generado",
  "file_size_kb": "INTEGER",
  "schema_version": 13,
  "export_date": "TEXT // ISO 8601",
  "record_counts": {
    "sessions": "INTEGER",
    "exercise_sets": "INTEGER",
    "alerts": "INTEGER"
  }
}
```

- **Estado de Error:** `Indicador de error con mensaje. Los datos originales permanecen intactos.`

---

#### `J3-T1`: Importar Respaldo (Restore)

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: selecciona un archivo de backup, lo valida y confirma explícitamente el reemplazo de todos los datos actuales en J3.`
- **Descripción:** El sistema valida el formato y la versión del esquema del archivo. Si es válido y el ejecutante confirma, reemplaza todos los datos actuales con los del backup. Si el proceso falla, ejecuta rollback automático preservando los datos originales. El proceso debe completarse en menos de 10 segundos.

**Payload / Parámetros (Input):**

```json
{
  "file_uri": "TEXT // URI del archivo de backup seleccionado desde el sistema de archivos.",
  "confirmed": "BOOLEAN // Obligatorio: true. El ejecutante debe confirmar explícitamente el reemplazo."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Todos los datos restaurados. Navegación a B1 con los datos del backup. El sistema opera con el estado restaurado.`

```json
{
  "restored_schema_version": "INTEGER",
  "navigation": "B1"
}
```

- **Estado de Error (formato inválido):** `Mensaje de error al validar. No se ejecuta la restauración. Los datos actuales no se alteran.`
- **Estado de Error (fallo durante restauración):** `Rollback automático. Los datos originales se preservan. Mensaje de error al ejecutante.`
- **Compatibilidad de formato:** `Se aceptan el formato vigente (12), el inmediatamente anterior (11) y el legado (8). Un respaldo v11 no trae tree_state; la restauración lo reconstruye desde el historial restaurado (N1-T1), de modo que el árbol queda en un estado válido sin necesidad de que el respaldo lo traiga.`

---

### 2.11. Módulo: `Flujo N — Árbol de Entrenamiento`

*Flujo de una sola pantalla, alcanzable exclusivamente desde `B1-T8`. **No produce efectos sobre ningún otro contenedor:** no altera la determinación de sesión, no genera alertas, no modifica ningún KPI y ningún componente del motor de decisión lee su estado. Es la excepción de alcance declarada en ADR-020 — puramente visual y de dependencia unidireccional.*

---

#### `N1-T1`: Recalcular y Mostrar el Estado del Árbol

- **Tipo de Trigger (Entrada):** `Automático: al componerse N1. También se dispara al cerrar una sesión (E4-T1), en cada emisión del cambio de día —después de B1-T7— y tras restaurar un respaldo (J3-T1).`
- **Descripción:** Deriva las dos dimensiones del árbol del historial de sesiones y las persiste antes de presentarlas. **Recalcular antes de observar** es lo que garantiza que lo mostrado nunca sea un valor rancio, aunque la aplicación llevara horas abierta sin cruzar la medianoche.
  - **Etapa (la forma):** por total de sesiones `COMPLETED` e `INCOMPLETE` — Semilla 0 · Brote 1–9 · Joven 10–29 · Maduro 30+. **Nunca retrocede** cualquiera que sea la salud.
  - **Salud (el color):** por días naturales `d` desde la última sesión — `d ≤ 2` → 100 · `2 < d < 14` → descenso lineal · `d ≥ 14` → 0. El corte de 14 se alinea con el umbral de crisis de `ROUTINE_INACTIVITY`, que mide inactividad **por rutina** frente a la medida **global** del árbol: son complementarias y no se acoplan.
- **Qué cuenta como entrenamiento:** sesiones `COMPLETED` e `INCOMPLETE`; **no** las `IN_PROGRESS`. Los días registrados en `day_skip` **no protegen al árbol** — omitir un día lo marchita igual que no abrir la aplicación. Una sesión reasignada temporalmente (`B1-T3`) cuenta como cualquier otra: al árbol le da igual **qué** rutina se entrenó.
- **Sin historial:** la etapa es Semilla y **no se presenta conteo de días** — no hay referencia contra la cual contar, y la salud se muestra en 100 para no castigar a quien todavía no ha tenido oportunidad de entrenar.
- **Modo de fallo:** *best-effort*. Si el recálculo falla, se presenta lo último persistido, que sigue siendo una lectura válida del historial; la pantalla nunca queda vacía y el fallo no interrumpe el cierre de sesión ni el arranque. El cálculo no bloquea la interfaz (RNF01).

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `N1 presenta el árbol en el área principal, su etapa, el puntaje de salud, los días desde el último entrenamiento y un mensaje contextual.`

```json
{
  "growth_stage": "TEXT // SEED | SPROUT | YOUNG | MATURE",
  "health_score": "INTEGER // 0-100",
  "days_since_last_session": "INTEGER | null // null = sin historial; entonces la línea de días no se presenta",
  "navigation": "ninguna"
}
```

---

#### `N1-T2`: Volver a Inicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: retroceso nativo de la barra superior de N1.`
- **Descripción:** **Única acción de navegación de la pantalla.** N1 no tiene formularios, ni modales, ni acciones destructivas: aquí no se decide nada, solo se mira.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Navegación a B1. Ningún cambio de estado del sistema.`

```json
{
  "navigation": "B1"
}
```

---

## 3. Manejo de Errores y Excepciones

*Define la estructura estandarizada que el sistema devolverá cuando una interacción falle, y los códigos específicos de error.*

### 3.1. Estructura Estándar de Error

*Formato unificado presentado al ejecutante en caso de fallo, sin importar el trigger.*

```json
{
  "error_code": "TEXT // Código interno del error (ver §3.2)",
  "message": "TEXT // Mensaje legible en español para mostrar al ejecutante",
  "field": "TEXT | null // Campo específico que falló la validación (para errores de formulario). null para errores de sistema."
}
```

### 3.2. Diccionario de Códigos de Error

| **Código de Error** | **Escenario de Fallo** | **Acción Sugerida (para la capa de presentación)** |
| --- | --- | --- |
| `ERR_VALIDATION_WEIGHT` | Peso ingresado es < 0 o no es un número válido | Mostrar error inline bajo el campo. Deshabilitar botón de confirmar. |
| `ERR_VALIDATION_REPS` | Repeticiones ingresadas son < 1 o no son entero válido | Mostrar error inline bajo el campo. Deshabilitar botón de confirmar. |
| `ERR_VALIDATION_RIR` | RIR no está en el rango [0, 2] | Mostrar error inline. El selector de chips RIR previene este estado; solo ocurre en entrada manual. |
| `ERR_VALIDATION_PROFILE` | Peso o altura ≤ 0, o nivel de experiencia no reconocido | Mostrar error inline bajo el campo afectado. |
| `ERR_SESSION_ALREADY_ACTIVE` | Se intenta iniciar una sesión cuando ya existe una `IN_PROGRESS` | Mostrar la tarjeta "Reanudar Sesión" en B1. Deshabilitar "Iniciar Sesión". |
| `ERR_REASSIGN_SESSION_ACTIVE` | Se intenta reasignar temporalmente la rutina (`B1-T3`) con una sesión `IN_PROGRESS` | La acción no se compone con sesión activa: B1 muestra "Reanudar Sesión", que no la aloja, y E1 no la ofrece. Si llega a la capa de datos, mostrar Snackbar: la rutina queda fijada al iniciar la sesión. |
| `ERR_SKIP_SESSION_HAS_SETS` | Se intenta cancelar el día (`B1-T5`) con al menos una serie registrada | B1 ya presenta la acción deshabilitada con el motivo. Si llega a la capa de datos, mostrar Snackbar: reanudar la sesión y cerrarla como incompleta. |
| `ERR_CLOSE_WITHOUT_SETS` | Se intenta cerrar (`E4-T1`) una sesión sin ninguna serie | E4 ya presenta el botón de confirmar deshabilitado con el motivo. Si llega a la capa de datos, mostrar Snackbar: cancelar el día desde B1. |
| `ERR_WEEK_DAYS_DELOAD_ACTIVE` | Se intentan cambiar los días de una rutina (`D6-T2`) con una descarga `ACTIVE` | Mostrar Snackbar en D6 explicando que el plan no se reorganiza durante una descarga. Consistente con la creación, el borrado y el reordenamiento de rutinas. |
| `ERR_EXERCISE_NOT_IN_DICT` | Se intenta registrar un ejercicio que no existe en `exercise` | Estado inválido — previsto por la arquitectura. Log de error interno. |
| `ERR_DEASSIGN_SESSION_ACTIVE` | Se intenta desasignar un ejercicio del plan con sesión activa de esa versión | Deshabilitar el botón de eliminar en D4. Mostrar tooltip explicativo. |
| `ERR_BACKUP_FORMAT_INVALID` | El archivo de backup seleccionado no tiene el formato JSON esperado o está corrupto | Mostrar mensaje de error en J3. No ejecutar restauración. |
| `ERR_BACKUP_VERSION_UNSUPPORTED` | La versión del esquema del backup es incompatible con la versión actual | Mostrar mensaje de error con la versión detectada. No ejecutar restauración. |
| `ERR_EXERCISE_NAME_DUPLICATE` | Se intenta crear un ejercicio con un par (nombre, tipo de equipo) ya existente | Mostrar error en D5 vía Snackbar: "Ya existe un ejercicio con ese nombre y tipo de equipo." |

---

## 4. Limitaciones y Restricciones de Interfaz

*Define las barreras de protección de las interfaces para evitar abusos o inconsistencias sistémicas.*

- **Sesión única concurrente:** `El sistema solo permite una sesión con status = 'IN_PROGRESS' en cualquier momento. Si existe una sesión activa, el botón "Iniciar Sesión" en B1 se reemplaza por el botón "Reanudar Sesión". No es posible crear una nueva sesión sin cerrar la activa.`
- **Descarga única concurrente:** `Solo puede existir un registro `deload` con status = 'ACTIVE' a la vez. El botón "Activar Descarga" en I1 se deshabilita si hay descarga activa.`
- **Tamaño mínimo de elemento interactivo:** `Todo elemento táctil (botón, campo, fila de lista, chip) debe tener un área de toque mínima de 48 × 48 dp (RNF-06). Los elementos visualmente más pequeños amplían su área de toque mediante padding invisible.`
- **Un día, una rutina:** `week_day.routine_id apunta a una rutina o a ninguna. Una rutina puede ocupar varios días de la semana; un día no puede ejecutar dos rutinas. Asignar a una rutina un día que pertenecía a otra lo traslada, y la rutina de origen lo pierde.`
- **Ninguna sesión sobrevive a su día:** `Una sesion IN_PROGRESS de un dia anterior se resuelve automaticamente (B1-T7) al arrancar la aplicacion o al cruzar la medianoche con ella abierta. No es posible reanudar la sesion de ayer.`
- **Una sesión por día:** `Un día se resuelve una sola vez, al cerrar una sesión con esa fecha o al cancelarlo. Resuelto, ni B1 ni B2 ofrecen iniciar nada hasta que llegue el siguiente día con rutina. No existe forma de ejecutar dos sesiones en la misma jornada.`
- **Reasignación única y temporal:** `Solo puede existir una fila en daily_routine_override. La reasignación aplica al día de su fecha y a ninguno más: no se ofrece reasignar por adelantado ni para varios días. Cambiar de forma permanente qué rutina corresponde a qué día no está expuesto en ninguna interfaz.`
- **Reasignación no disponible con sesión iniciada:** `La acción de reasignación vive en la tarjeta de sesión propuesta y en la de día de descanso, ninguna de las cuales se compone cuando existe una sesión IN_PROGRESS. No se presenta deshabilitada: no existe en ese estado.`
- **Tiempo máximo de ejecución de Backup/Restore:** `El proceso de exportación e importación de datos debe completarse en menos de 10 segundos para un historial de hasta 2 años (estimado: ~35,000 registros en exercise_set, base de datos < 5 MB) (RNF-18).`
- **Inmutabilidad de sesiones cerradas:** `Las sesiones con status = 'COMPLETED' o 'INCOMPLETE' no tienen ningún trigger de edición disponible. F2 (Detalle de Sesión Pasada) es estrictamente de solo lectura. Ninguna interfaz expone acciones de modificación retroactiva de series ya registradas.`
- **Restricción de navegación durante sesión activa:** `Cuando el flujo E (Sesión Activa) está en curso, la barra de navegación global (Bottom Navigation) se oculta completamente. El único canal de salida del flujo de sesión es E4 (Confirmación de Cierre). No existe ningún trigger que permita abandonar la sesión sin cerrarla formalmente.`
- **Restricción de orientación:** `La interfaz opera exclusivamente en orientación vertical (portrait). El sistema no soporta modo horizontal (landscape). Si el dispositivo se rota, la vista se mantiene en portrait (RNF-07).`
- **El árbol no decide nada:** `N1 es de solo lectura y su estado no alimenta ninguna decision del sistema. Ningun componente del motor de decision —prescripcion de carga, Doble Umbral, meseta, regresion, fatiga, protocolo de descarga, rotacion ciclica— lee el puntaje ni la etapa; el arbol no genera alertas ni modifica ROUTINE_INACTIVITY o LOW_ADHERENCE, y la adherencia semanal se calcula exactamente igual. La dependencia es unidireccional: el arbol lee del historial y nada del sistema lee del arbol (ADR-020).`
- **El árbol no tiene pestaña propia:** `N1 es una ruta nueva alcanzable solo desde la tarjeta de B1 (B1-T8). No se añade a la barra de navegacion inferior, que permanece visible durante la pantalla.`
- **Idioma único:** `Toda la interfaz opera exclusivamente en español. No existe selector de idioma ni soporte para internacionalización (RNF-08).`
