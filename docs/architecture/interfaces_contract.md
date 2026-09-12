# Contrato de Interfaces e Interacciones

> Este documento define los "puertos" de comunicación del sistema. Detalla estrictamente cómo el entorno interactúa con el sistema (entradas) y cómo el sistema responde (salidas). **Se mantiene agnóstico a la implementación visual**, enfocándose en la estructura de los datos, eventos, comandos y reglas de validación que gobiernan cada interacción.
>
> **Alcance:** 26 vistas distribuidas en 10 flujos funcionales. Cada vista define sus triggers de entrada (acciones del ejecutante o eventos del sistema), su payload de datos y su respuesta esperada.

---

## 1. Protocolos y Canales de Comunicación

*Define los canales a través de los cuales el sistema escucha y emite información.*

- **Canal Principal:** `Sistema de Input táctil de Android (gestos de toque, deslizamiento y selección sobre la interfaz de usuario compuesta con Jetpack Compose). El sistema opera exclusivamente en modo retrato (portrait). No existen canales de red, API REST, WebSockets ni CLI.`
- **Formato de Intercambio Base:** `UI Events + StateFlow. El ejecutante emite acciones discretas (Intent / UIEvent) desde la capa de presentación; el ViewModel las procesa y emite nuevo estado (UIState) como flujo reactivo hacia la capa de composición. El formato de persistencia interna es SQLite (Room). El formato de exportación/importación de datos es JSON con metadatos de versión.`
- **Canal Interno Nativo ↔ Web (HU-38):** `Único canal no-Compose del sistema. La pantalla del arbol (N1) aloja un WebView que renderiza el arbol en 3D desde un asset local empaquetado. La direccion nativo -> web usa WebView.evaluateJavascript e invoca window.tensionTree.setState(healthScore, stageCode) con exactamente dos parametros. La direccion web -> nativo usa un objeto @JavascriptInterface llamado TreeBridge con onReady() y onFailure(reason), y no transporta ningun dato de dominio. No es un canal de red: son dos motores dentro del mismo proceso sobre un archivo local, y la aplicacion no declara el permiso INTERNET.`
- **Autenticación y Autorización:** `Ninguna. La aplicación es de uso personal y opera en modo completamente local (100% offline). No existe autenticación de usuario, sesión de red, ni modelo de permisos de acceso a datos. El único permiso del sistema operativo requerido es almacenamiento externo (para backup/restore). En particular **no se declara el permiso `INTERNET`**, lo que hace imposible por sistema operativo que el WebView de N1 cargue contenido remoto.`

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
- **Descripción:** El sistema consulta `exercise` con sus dos relaciones N:M —`exercise_muscle_zone` y `exercise_equipment`— aplicando los filtros activos. Devuelve la lista filtrada. El filtro por equipamiento coincide si **alguna** de las opciones que el ejercicio admite es la filtrada, no si es «la» suya: desde HU-39 un ejercicio declara una lista.

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
      "equipment_types": ["TEXT"] ,
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
- **Descripción:** El sistema recupera los datos completos del ejercicio seleccionado incluyendo **todos** los implementos que admite, sus zonas musculares **con jerarquía** y la ruta de media visual.
- **Las zonas se presentan en dos bloques visiblemente distintos** (HU-41): *Zona principal* —la que ejecuta el movimiento— primero y destacada, y *Zonas secundarias* —las que asisten— después y en menor peso visual. Un ejercicio tiene al menos una principal y puede no tener ninguna secundaria; en ese caso el bloque muestra «Ninguna» en lugar de desaparecer, para que la ficha no cambie de forma según el ejercicio.
- **La jerarquía es editable desde la ficha**, con el mismo patrón que el equipamiento, la imagen y la dificultad: **persiste al instante, sin botón de guardar**. Un intento rechazado —quedarse sin principal, o poner una zona en los dos campos— **no escribe nada**, así que el campo vuelve a lo que había y el error aparece junto a él.

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
  "equipment_types": ["TEXT"] ,
  "equipment_types_with_sets": ["INTEGER"] ,
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

> `equipment_types` son **todas** las opciones que el ejercicio admite, en el orden del catálogo, y `equipment_types_with_sets` los identificadores de las que ya tienen series registradas — las que la ficha marca con candado y no se pueden retirar (`D2-T3`).

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

#### `D2-T3`: Editar Equipamiento Admitido del Ejercicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: marca o desmarca una casilla de la lista de equipamiento admitido en D2.`
- **Descripción:** El sistema persiste la relación `exercise_equipment` de inmediato, **sin botón de guardado** — mismo patrón que el cambio de imagen (`D2-T2`) y el de dificultad de progresión. Marcar añade la fila; desmarcar **valida antes de escribir** y, si el motivo lo impide, no escribe nada y la casilla vuelve a su sitio, porque el estado se deriva del flujo de Room y ese flujo no cambió.
- **Aplica igual** a los 37 ejercicios seed y a los creados por el ejecutante.

**Payload / Parámetros (Input):**

```json
{
  "exercise_id": "INTEGER // Obligatorio.",
  "equipment_type_id": "INTEGER // Obligatorio. FK válida a equipment_type.",
  "action": "TEXT ['ADD', 'REMOVE'] // Determinado por el estado de la casilla."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `exercise_equipment actualizado. D2 refleja la lista nueva, su contador y sus candados.`
- **Estado de Error (última opción):** `ERR_EQUIPMENT_REQUIRED. No se persiste. Mensaje inline al pie de la lista: el atributo es obligatorio y no admite lista vacía.`
- **Estado de Error (con historial):** `ERR_EQUIPMENT_HAS_SETS. No se persiste. Mensaje inline que nombra el implemento: existen series registradas con él, y una serie ya escrita no puede quedar apuntando a un equipamiento que el ejercicio dejó de admitir.`

> La comprobación de la última opción va **antes** que la del historial: con una sola opción el motivo es que quedaría vacía, y anunciar el historial cuando el problema es otro haría buscar donde no está.

---

#### `D5-T1`: Crear Ejercicio Personalizado

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el botón "Crear" en D5 (Crear Ejercicio) con datos válidos.`
- **Descripción:** El sistema valida la unicidad de `name` —el nombre es único **por sí solo** desde HU-39: el implemento no forma parte de la identidad del ejercicio— y persiste el nuevo ejercicio con `is_custom = 1`. Crea las entradas correspondientes en `exercise_muscle_zone` y en `exercise_equipment`, en la misma transacción. Si se seleccionó imagen, la copia al almacenamiento interno.
- **El equipamiento se elige en una lista de casillas** con los 15 tipos del catálogo, en su orden declarado, con un contador de seleccionados al pie. Se exige **al menos uno** para poder guardar, y el botón permanece deshabilitado mientras la selección esté vacía.
- **Las zonas musculares se eligen en dos campos separados** (HU-41): *Zonas principales*, obligatorio, y *Zonas secundarias*, opcional. Cada uno lista lo ya elegido como filas con acción de quitar, más un botón **«+ Añadir zona»** que abre un diálogo con las **33 zonas agrupadas por sus 14 grupos musculares**, en orden anatómico y con buscador. Son dos campos y no una lista única con marca porque **la obligatoriedad se lee donde se incumple**: el aviso de «al menos una principal» vive en el campo que lo exige, y con 33 zonas un error al pie del formulario quedaría lejos del sitio donde hay que arreglarlo.
- **Una zona no puede ser principal y secundaria a la vez.** El diálogo muestra las ya elegidas en el otro campo marcadas y no seleccionables —esconderlas dejaría al ejecutante buscando una zona que parece no existir— y la clave primaria de `exercise_muscle_zone` hace el estado contradictorio irrepresentable.
- El botón de guardar permanece **deshabilitado** mientras no haya al menos una zona principal.

**Payload / Parámetros (Input):**

```json
{
  "name": "TEXT // Obligatorio. No vacío. Único por sí solo en el catálogo.",
  "equipment_type_ids": ["INTEGER"] ,
  "primary_muscle_zone_ids": ["INTEGER"] // Obligatorio. Al menos una. Sin intersección con las secundarias.,
  "secondary_muscle_zone_ids": ["INTEGER"] // Opcional. Puede venir vacía.,
  "is_bodyweight": "BOOLEAN // Opcional. Default false.",
  "is_isometric": "BOOLEAN // Opcional. Default false. Si true, is_bodyweight debe ser true.",
  "is_to_technical_failure": "BOOLEAN // Opcional. Default false. Si true, is_bodyweight debe ser true. Mutuamente excluyente con is_isometric.",
  "progression_difficulty": "TEXT ['LOW','MEDIUM','HIGH'] // Opcional. Default 'MEDIUM', preseleccionado en el formulario.",
  "image_uri": "TEXT | null // Opcional. URI de galería del dispositivo. null = sin imagen."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Ejercicio creado. Navegación de retorno a D1. El ejercicio aparece en el diccionario con badge 'Personalizado'.`

> **Invocable desde la sesión activa (HU-43).** `E1-T2` abre **este mismo formulario**, con los mismos campos y las mismas validaciones, pasándole la sesión de origen. Lo único que cambia es la etiqueta del botón —«Guardar y añadir»— y qué ocurre al guardar: el ejercicio queda en el Diccionario **y** añadido a la sesión, en un solo gesto.
>
> **El destino es asimétrico y deliberado:** el ejercicio **no entra al plan**, pero **sí queda en el Diccionario**, disponible para asignarlo más adelante si resulta que merece un puesto fijo. Permanece allí aunque la sesión se cierre, se abandone o el ejercicio se retire de ella — así un descubrimiento del día no se pierde. Por eso la creación y el añadido **no comparten transacción**: envolverlos produciría justo el resultado prohibido, un ejercicio creado que se desvanece si el añadido falla.

```json
{
  "exercise_id": "INTEGER // ID del ejercicio creado",
  "navigation": "D1"
}
```

- **Estado de Error (unicidad):** `ERR_EXERCISE_NAME_DUPLICATE. Snackbar con mensaje de error. No se persiste el ejercicio. El formulario permanece con los datos ingresados.`
- **Estado de Error (equipamiento vacío):** `ERR_EQUIPMENT_REQUIRED. Error inline en el campo de equipamiento. El botón de guardar permanece deshabilitado mientras la selección esté vacía.`

> `equipment_type_ids` es obligatorio y no vacío. Los identificadores repetidos se colapsan.

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

#### `D6-T1a`: Editar el Equipamiento Sugerido de una Asignación

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el renglón del implemento sugerido de una asignación en D4 y confirma otro en el diálogo.`
- **Descripción:** El sistema fija `plan_assignment.suggested_equipment_type_id` (HU-41). El selector ofrece **solo los implementos que el ejercicio admite**, resueltos al abrir el diálogo y no al pintar la lista: el ejercicio pudo ganar o perder opciones desde entonces. La capa Domain vuelve a comprobarlo antes de escribir.
- **No se propaga al puesto**, a diferencia de series y repeticiones: las alternativas de un puesto dual comparten prescripción pero no implemento, porque son ejercicios distintos con opciones distintas. Cada uno se edita por separado desde su propio renglón.

**Payload / Parámetros (Input):**

```json
{
  "routine_version_id": "INTEGER // Obligatorio.",
  "exercise_id": "INTEGER // Obligatorio.",
  "equipment_type_id": "INTEGER // Obligatorio. Debe pertenecer a exercise_equipment del ejercicio."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Sugerencia actualizada. D4 repinta el renglón.`
- **Estado de Error:** `ERR_SUGGESTION_NOT_ADMITTED` — el implemento no está entre los que el ejercicio admite. No se escribe nada.

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

> **HU-43 no lo sustituye.** El ajuste de la sesión —`E1-T2` y `E1-T3`— resuelve una necesidad distinta: entrenar algo que el plan no trajo. El intercambio de alternativa sigue siendo el único mecanismo para cambiar **dentro de un puesto**, y solo los ejercicios del plan tienen puesto. Un ejercicio añadido queda fuera de esa estructura y por tanto **no ofrece este trigger**.

---

#### `E1-T2`: Añadir Ejercicio a la Sesión

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca «+ Añadir ejercicio» al final de la lista de E1 y elige un ejercicio del Diccionario, o crea uno nuevo con D5-T1 desde la misma hoja.`
- **Descripción:** El sistema inserta un `session_exercise` con `is_extra = 1` en la sesión en curso. El ejercicio entra **al final** de la lista y **sin puesto**: queda fuera de la estructura de `slot` y, en consecuencia, sin alternativas intercambiables. No lo elige el ejecutante — es lo que implica no venir del plan.

  **El ajuste es temporal.** El plan por defecto no se modifica en ningún punto de esta cadena: ni la composición, ni el orden, ni los puestos, ni las alternativas. La siguiente sesión de esa misma rutina vuelve a proponer su composición original.

  **Prescripción por defecto: 3 series de 8 a 12 repeticiones**, por el enfoque de hipertrofia del sistema, y la misma para un ejercicio del Diccionario y para uno creado en el momento. El rango se ajusta al modo del ejercicio con la misma regla que el plan: los isométricos en segundos (`30-45_SEC`) y los de fallo técnico sin límite superior (`TO_TECHNICAL_FAILURE`). Se **persiste** en `session_exercise.prescribed_sets` y `prescribed_reps` en lugar de derivarse en cada consulta, de modo que la regla decide una vez y la fila queda diciendo con qué se comprometió el ejecutante.

  **Un ejercicio que ya está en la sesión no se puede añadir otra vez.** Entrenar el mismo movimiento con otro implemento se resuelve con el selector de equipamiento de `E2-T1`, no duplicando el ejercicio. La hoja de selección los muestra **atenuados y etiquetados**, no ocultos: esconderlos dejaría buscando algo que sí existe.

  **Reponer es añadir.** No hay trigger de reposición: un ejercicio retirado vuelve por este mismo camino, eligiéndolo del Diccionario, y vuelve **al final de la lista y con la prescripción por defecto**, no a su puesto ni a la prescripción que el plan le daba.

- **Precondición:** la sesión está `IN_PROGRESS`. Después de cerrarla la acción no existe.

**Payload / Parámetros (Input):**

```json
{
  "session_id": "INTEGER",
  "exercise_id": "INTEGER // Obligatorio. No puede estar ya en la sesión."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `session_exercise creado con is_extra = 1. E1 lo muestra al final de la lista, con su marca de añadido, sin número de puesto y sin ícono de intercambio. El presupuesto de retiro sube en uno.`
- **Estados de Error:** `ERR_EXERCISE_ALREADY_IN_SESSION`, `ERR_SESSION_NOT_IN_PROGRESS`.

---

#### `E1-T3`: Retirar Ejercicio de la Sesión

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: abre el menú contextual (⋮) de un ejercicio en E1 y toca «Retirar de la sesión». Confirma en el diálogo.`
- **Descripción:** El sistema **borra** la fila de `session_exercise`. Con 0 series no hay nada inmutable que destruir, y el borrado saca al ejercicio de golpe del cierre, de la clasificación, del tonelaje y del historial. Si el ejercicio lo trajo el plan, el retiro queda registrado en `session_withdrawal` **antes** del borrado y en la misma transacción; si era un añadido, quitarlo es *deshacer* y no deja constancia.

  **Presupuesto — uno entra, uno puede salir (CA-43.04):** se pueden retirar tantos ejercicios del plan como ejercicios se hayan añadido. Sin ningún añadido no se puede retirar nada, y la acción se presenta **deshabilitada con su causa**.

  **Invariante (CA-43.05):** el número de añadidos **nunca** queda por debajo del de retirados. De ahí que deshacer un añadido exija reponer primero el ejercicio que su incorporación permitió retirar, y que el mensaje lo **nombre**. El nombre siempre existe: el veredicto negativo implica que queda al menos un retiro sin reponer.

  **Nada con series registradas se retira (CA-43.06),** ni del plan ni añadido, y por la misma razón: la serie es inmutable y fuente de verdad histórica, así que retirar el ejercicio implicaría borrar registros. La causa de las series se evalúa **antes** que la del presupuesto — es la única que no se levanta añadiendo nada.

- **Precondición:** la sesión está `IN_PROGRESS` y el ejercicio tiene 0 series.
- **Presentación:** la acción se muestra siempre; cuando no se puede ejecutar aparece deshabilitada con la causa como línea de apoyo. Decirlo antes de tocar es lo que convierte la restricción en explicación.

**Payload / Parámetros (Input):**

```json
{
  "session_exercise_id": "INTEGER"
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Fila de session_exercise eliminada. Si era del plan, session_withdrawal registra el retiro. E1 refleja la lista sin el ejercicio y recalcula el presupuesto.`
- **Estados de Error:** `ERR_WITHDRAW_HAS_SETS`, `ERR_WITHDRAW_BUDGET_EXHAUSTED`, `ERR_WITHDRAW_BREAKS_INVARIANT`, `ERR_SESSION_NOT_IN_PROGRESS`.

---

#### `E2-T1`: Registrar Serie de Ejercicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: completa el formulario E2 (Registro de Serie) y toca el botón de confirmar.`
- **Descripción:** El sistema valida los datos y persiste un nuevo `exercise_set`. Asigna automáticamente `set_number = COUNT(series previas del ejercicio en la sesión) + 1`. Si es la primera serie **del par `(ejercicio, equipamiento)`**, crea su `exercise_progression` si no existe: un ejercicio entrenado con tres implementos mantiene tres estados de progresión independientes (CA-40.01).

  El **valor precargado** en el campo de peso se resuelve con una precedencia estricta, y **desde HU-40 cada uno de sus términos se resuelve sobre el par `(ejercicio, equipamiento)`**: (1) la carga prescrita por el motor de Doble Umbral **para ese par** mientras siga **activa**, (2) el peso de la serie anterior **del mismo par** en la sesión actual, (3) el peso de la última serie **del mismo par** en su sesión cerrada más reciente, (4) campo vacío. Una prescripción está *activa* mientras supere el último peso efectivamente manejado por más de 0.01 Kg: representa un aumento que el ejecutante aún no ha alcanzado. Una vez alcanzada o superada, la prescripción queda consumida y la memoria del último peso toma el relevo, de modo que la precarga acompaña la progresión en lugar de volver a un valor obsoleto. La memoria se resuelve sobre el **ejercicio efectivamente ejecutado** (`session_exercise.exercise_id`) y excluye las sesiones de descarga. En un microciclo de descarga, la carga de descarga calculada **del par** conserva su prioridad sobre la memoria. El valor precargado es siempre editable: el sistema sugiere, no impone.

  **Al cambiar de implemento en el selector, el peso precargado y la unidad se recalculan para el par nuevo.** Si el par no tiene historial, el campo queda **vacío**: nunca se hereda el peso de otro equipamiento, porque es el peso de otra cosa. El recálculo se cancela en cuanto el ejecutante toca el campo — una sugerencia que llega tarde y pisa lo ya tecleado impondría en vez de sugerir.

  La sesión anterior de un par es la sesión cerrada más reciente **en la que ese par se entrenó**, no la última sesión del ejercicio: una sesión hecha solo con polea no es la sesión anterior del par mancuerna, y tratarla como tal dejaría a la mancuerna sin término de comparación.

  El formulario E2 ofrece un **selector de unidad de captura** (`Kg` / `Lb`) junto al campo de peso, más controles de incremento y decremento cuyo paso depende de la unidad activa (0.5 Kg en kilogramos, 1 lb en libras). El selector se preselecciona con la unidad de la última serie registrada **del mismo par** —la unidad es la etiqueta de la máquina, y dos implementos son dos máquinas— y se oculta cuando no hay carga externa que capturar. La conversión a kilogramos ocurre en la capa de presentación antes de invocar el trigger: `weight_kg` llega **siempre en la unidad canónica**.

  El formulario ofrece además un **selector de equipamiento**, situado sobre el campo de peso porque lo gobierna. Está limitado a los implementos que el ejercicio admite (`exercise_equipment`) y **desde HU-41 su preselección sigue tres niveles**, en orden: (1) el implemento de la última serie registrada **de este ejercicio en esta sesión** —el último cambio manda—, (2) el **equipamiento sugerido por el plan** para ese puesto, si el ejercicio tiene asignación en la versión de rutina de la sesión y la sugerencia sigue admitida, y (3) CA-39.04 sin cambios: el último implemento usado en cualquier sesión y, si nunca se usó, la primera opción admitida. Cada nivel comprueba que el candidato **siga admitido**, o el selector nacería con un valor que no está entre sus opciones.

  **Un rótulo bajo el selector dice de dónde viene la preselección**: *sugerido por el plan* en el nivel 2 y *último implemento usado* en el 1 y el 3. En el nivel 3 con la primera opción admitida no hay rótulo: es el estado más común de un ejercicio nuevo y no hay nada que explicar. El rótulo **desaparece en cuanto el ejecutante elige a mano**: a partir de ahí el implemento es suyo y un aviso que siguiera diciendo «sugerido por el plan» sería falso. La sugerencia sugiere, no impone. Cuando el ejercicio admite **una sola** opción se presenta resuelto, como etiqueta y sin interacción: un control que se puede tocar para no elegir nada informa menos que un texto. El equipamiento es **obligatorio** y dos series del mismo ejercicio en la misma sesión pueden llevar implementos distintos. La serie sigue siendo inmutable tras su creación: el equipamiento registrado no se corrige después.

  **Efecto colateral persistido, sin nada visible (HU-42).** Si la serie es la **de referencia** —exactamente 10 repeticiones, RIR exactamente 1, sobre carga externa y con peso mayor que 0—, el sistema calcula `1RM = Peso / [1.0278 − (0.0278 × Reps)]`, que a 10 repeticiones equivale a `Peso × 1.3337`, y lo persiste en `exercise_one_rm` para el **par** de la serie, **solo si supera el valor guardado**. El cálculo corre **dentro de la misma transacción** que inserta la serie, y no como un paso posterior best-effort al modo del árbol (`N1-T1`): el 1RM es un récord acumulado que no se puede reconstruir —no hay retro-cálculo y el respaldo lo restaura sin recalcularlo—, así que una escritura perdida sería un máximo perdido para siempre. E2 **no cambia**: no se avisa, no se navega y no se muestra el resultado; el ejecutante lo consulta en `O1` cuando quiere. Nada de lo que este trigger decide depende del 1RM, que se escribe y no se lee.

  **El implemento decide si hay carga externa que capturar** (`ExternalLoadRule`), y lo decide él y no solo la marca del ejercicio: `Peso Corporal` registra 0 en cualquier ejercicio; `Peso Añadido` es lo único que habilita la captura sobre un ejercicio de peso corporal, y exige un valor **estrictamente mayor que 0** porque representa exclusivamente la carga externa; sobre un ejercicio de peso corporal, `Barra Fija` —la dominada estricta— y `Máquina` —la asistida, cuya carga es un contrapeso que **resta** esfuerzo y que registrado como peso invertiría el significado del dato— registran 0. Sobre cualquier otro ejercicio, `Máquina` sí es carga. Cuando la captura está deshabilitada, el campo de peso permanece visible y bloqueado en 0 y el selector de unidad se oculta.

**Payload / Parámetros (Input):**

```json
{
  "session_exercise_id": "INTEGER // Obligatorio.",
  "weight_kg": "REAL [0, 500] // Obligatorio. Valor canónico en kilogramos, ya convertido desde capture_unit. 0 para ejercicios de peso corporal e isométricos.",
  "reps": "INTEGER >= 1 // Obligatorio. Para isométricos: segundos sostenidos.",
  "rir": "INTEGER [0, 1, 2] // Obligatorio. Reserva de esfuerzo percibida.",
  "capture_unit": "TEXT ['KG', 'LB'] // Obligatorio. Unidad en la que el ejecutante capturó el valor. Se persiste para preseleccionar el selector y mostrarla en el detalle de la serie. Se fuerza a 'KG' cuando no hay carga externa.",
  "equipment_type_id": "INTEGER // Obligatorio. FK válida a equipment_type y presente en exercise_equipment del ejercicio. Sin valor por defecto: es el dato que la serie existe para registrar."
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
- **Estado de Error (sin equipamiento):** `ERR_EQUIPMENT_REQUIRED. No se persiste. El botón de confirmar permanece deshabilitado mientras no haya implemento elegido.`
- **Estado de Error (lastre en cero):** `ERR_VALIDATION_WEIGHT. Con 'Peso Añadido' un lastre de 0 no es lastre: error inline en el campo de peso y nada se persiste.`

> Al cambiar de implemento, el campo de peso se ajusta a lo que ese implemento significa: pasando a uno sin carga externa se fija en 0, y pasando a `Peso Añadido` se **limpia** en vez de heredar, porque lo anterior era el peso de otra cosa y no un lastre.

---

#### `E4-T1`: Cerrar Sesión

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: confirma el cierre en el diálogo E4 (Confirmación de Cierre de Sesión). La confirmación está deshabilitada mientras la sesión no tenga ninguna serie registrada.`
- **Precondición — al menos una serie:** cerrar da por terminado lo entrenado, y sin ninguna fila en `exercise_set` no hay nada que terminar. La sesión permanece `IN_PROGRESS` y es reanudable; para resolver el día sin entrenar, la vía es `B1-T5`.
- **Descripción:** El sistema ejecuta el protocolo de cierre de sesión: (1) finaliza todos los `session_exercise` no finalizados, (2) calcula tonelaje, (3) ejecuta el motor de reglas **por cada par `(ejercicio, equipamiento)` entrenado** (comparación histórica del par, clasificación del par, actualización de su `exercise_progression`, carga del Doble Umbral del par), (4) consolida la lectura del ejercicio y evalúa mesetas y alertas, (5) actualiza `rotation_state`, (6) determina el status de sesión, (7) navega a E5.

  **La unidad de comparación es el par.** Una sesión con mancuerna y polea del mismo ejercicio produce **dos** evaluaciones independientes, cada una contra el histórico de su implemento. Estrenar uno no tiene contra qué compararse: su clasificación es `NULL` (Sin Historial) y **nunca `REGRESSION`**, su contador arranca en 0 y el estado del otro par no se altera.

  **La consolidación por ejercicio se deriva, no se persiste como estado.** La clasificación de cada par se guarda en `session_exercise_progression`; `session_exercise.progression_classification` guarda la **consolidada**, con el orden `POSITIVE_PROGRESSION > MAINTENANCE > REGRESSION > NULL`. Eso hace verdadera la disyunción de CA-40.04 —el ejercicio progresa si alguno de sus pares progresó— y es lo que consumen sin cambio alguno la tasa de progresión (`G1-T1`), la tendencia por grupo muscular (`G3-T1`), la alerta `LOW_PROGRESSION_RATE` y el conteo de slots afectados para la descarga.

  **La meseta se declara por conjunción:** el ejercicio entra en meseta —y se emite `PLATEAU`— solo cuando **todos** sus pares han alcanzado el umbral efectivo. Un implemento todavía en progresión desmiente la meseta. `ROUTINE_REQUIRES_DELOAD` cuenta como estancado únicamente el ejercicio cuya condición consolidada se cumple.

  **Los ejercicios añadidos cuentan para la completitud (HU-43).** El status se decide comparando ejercicios finalizados contra ejercicios de la sesión, y un añadido es un `session_exercise` más: haberlo añadido es haberlo comprometido. Un añadido sin ninguna serie deja la sesión en `INCOMPLETE` aunque todo lo que trajo el plan esté completo. La vía para no penalizar la completitud es **retirarlo antes de cerrar** (`E1-T3`), cuando la invariante lo permita. Un ejercicio **retirado** no se considera en ningún término de esa comparación: su fila ya no existe.

  **El ajuste no altera la rotación.** `rotation_state` avanza exactamente igual que en una sesión sin ajustar, y el conteo de microciclos tampoco cambia. Las series de los ejercicios añadidos entran en el motor de progresión, el tonelaje, los KPIs y el 1RM **como cualquier otra**: ninguna consulta de agregación filtra por `is_extra`.

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
      "is_extra": "BOOLEAN // true = añadido a aquella sesión, no traído por el plan (HU-43).",
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

  **Desde HU-40 el resumen presenta un renglón por implemento efectivamente usado**, con la clasificación y la señal de ese par. Con un solo implemento el renglón se colapsa en la línea del ejercicio y la pantalla es **idéntica** a la anterior a esta historia (CA-40.08). La clasificación que acompaña al nombre del ejercicio es la **consolidada**.

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
      "progression_classification": "TEXT | null // Lectura CONSOLIDADA del ejercicio (CA-40.04)",
      "prescribed_load_next": "REAL | null // El objetivo más alto entre los pares",
      "action_signal": "TEXT",
      "is_mastered": "BOOLEAN // true para isométricos dominados",
      "pairs": [
        {
          "equipment_type_id": "INTEGER",
          "equipment_type_name": "TEXT",
          "progression_classification": "TEXT | null // null = Sin Historial de ese implemento, jamás una regresión",
          "prescribed_load_next": "REAL | null // La carga que el Doble Umbral prescribió A ESE PAR",
          "action_signal": "TEXT",
          "avg_weight_kg": "REAL",
          "completed_sets": "INTEGER"
        }
      ]
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
- **Descripción:** El sistema recupera la sesión completa con sus ejercicios y series. Refleja el ejercicio que realmente se ejecutó — el del plan, la alternativa del slot si el ejecutante la intercambió (HU-26), o uno **añadido** a aquella sesión (HU-43) — y, por cada serie, **con qué implemento** se ejecutó. El implemento se presenta en su propia línea bajo el peso, las repeticiones y el RIR: dos series del mismo ejercicio en la misma sesión pueden llevar implementos distintos, y la diferencia tiene que verse de un barrido. El tonelaje del ejercicio suma todas las series, cualquiera que fuera el implemento.

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
  "withdrawn_exercise_names": ["TEXT"],
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
          "capture_unit": "TEXT ['KG', 'LB'] // Única ubicación de la app donde la unidad de captura se presenta. Cuando es 'LB', el detalle añade el valor original en libras bajo el valor en kilogramos. El tonelaje y todos los agregados permanecen en kilogramos.",
          "equipment_type": "TEXT // Implemento con el que se ejecutó la serie. Nunca null."
        }
      ]
    }
  ]
}
```

---

#### `F3-T1`: Consultar Historial de Ejercicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: navega a F3 desde D2, E5, F2, G1 o H2 con un ejercicio específico.`
- **Descripción:** El sistema recupera los registros históricos del ejercicio **segmentados por implemento**: una entrada por sesión **y por implemento**, de modo que una sesión en la que se usaron mancuerna y polea produce dos entradas con la misma fecha, cada una con su propio promedio de peso. La segmentación ocurre en la agregación de la consulta y no en la presentación, porque los promedios se calculan ahí: agrupar solo por sesión mezclaría el peso de los dos implementos antes de que el dato saliera de la base. **Una serie de un implemento nunca se presenta en la misma serie temporal de comparación de peso que la de otro.**

  `equipment_options` son los implementos con los que el ejercicio se ha entrenado **de verdad**, no los que admite: un ejercicio que admite varios pero se ha hecho con uno solo muestra ese uno como etiqueta, sin selector vacío ni agrupaciones sin datos. Con más de uno, la pantalla ofrece el selector y `history` y `load_trend` corresponden **únicamente** al implemento seleccionado.

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
  "equipment_options": ["TEXT"] ,
  "selected_equipment": "TEXT | null // El implemento de la lectura. null solo cuando no hay historial.",
  "history": [
    {
      "session_id": "INTEGER",
      "date": "TEXT",
      "routine_name": "TEXT",
      "version_number": "INTEGER",
      "equipment_type": "TEXT // Implemento de las series que promedia esta entrada.",
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

  **Desde HU-40, los indicadores que comparan peso entre sesiones se calculan por par `(ejercicio, equipamiento)` y se consolidan por ejercicio; ninguno cambia de fórmula, cambia la unidad de comparación.** La **tasa de progresión** consume la clasificación consolidada que el cierre de sesión ya persiste, de modo que alternar implementos no la diluye. La **velocidad de carga** traza su pendiente sobre cada par por separado —entre 20 Kg de polea y 12 de mancuerna no hay pendiente, hay dos magnitudes distintas— y se informa la **mayor** de ellas, con el `session_count` de ese par: es la traducción numérica de la disyunción de CA-40.04, porque promediarlas dejaría que un implemento parado borrase a uno que avanza. El **tonelaje total** y el volumen por grupo muscular **no se segmentan**: el volumen es volumen.

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

**Accesos a las pantallas de detalle:** bajo las secciones, `G1` presenta tres entradas hermanas —`G1-T4` (1RM estimado), `G2-T1` (volumen por grupo muscular) y `G3-T1` (tendencia de progresión)—, compuestas con el mismo elemento para que no puedan divergir. Ninguna de las tres añade pestaña a la barra de navegación inferior.

---

#### `G1-T4`: Abrir el 1RM Estimado

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca la entrada "1RM estimado" en G1.`
- **Descripción:** Navega a `O1`. **La entrada se compone siempre**, incluso sin ninguna sesión registrada y sin ninguna serie que haya calificado: el estado vacío lo resuelve la pantalla de destino, no este trigger. Condicionar la entrada a que haya datos obligaría a `G1` a consultar el 1RM para decidir si se dibuja, que es precisamente la lectura que la frontera de HU-42 no admite.
- **Área táctil:** al menos 48 × 48 dp (RNF06).
- **Sin efecto de estado:** no escribe, no calcula y no altera ningún KPI. La entrada **no** se añade a `B1` (Inicio) ni a la barra de navegación inferior.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Navegación a O1. Ningún cambio de estado del sistema.`

```json
{
  "navigation": "O1"
}
```

---

#### `G2-T1`: Consultar Volumen por Grupo Muscular

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Volumen por Grupo Muscular" en G1.`
- **Descripción:** El sistema calcula el tonelaje acumulado y la distribución de volumen del microciclo seleccionado, más la evolución del tonelaje a lo largo de todos los microciclos.
- **Todas las zonas del ejercicio cuentan, principales y secundarias, sin ponderación** (HU-41). La jerarquía es informativa para el ejecutante, no un peso de cálculo: una serie de un ejercicio de tres zonas aporta su tonelaje **íntegro** a cada uno de los tres grupos. Es la misma agregación de antes de HU-41 — lo que cambió es que las zonas son más finas. **El eje sigue siendo los 14 grupos musculares**, que no cambian: la granularidad fina cabe íntegra dentro de ellos y ningún KPI cambió de definición.

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
- **Todas las zonas del ejercicio cuentan, principales y secundarias, sin ponderación** (HU-41). La jerarquía es informativa para el ejecutante, no un peso de cálculo: una serie de un ejercicio de tres zonas aporta su tonelaje **íntegro** a cada uno de los tres grupos. Es la misma agregación de antes de HU-41 — lo que cambió es que las zonas son más finas. **El eje sigue siendo los 14 grupos musculares**, que no cambian: la granularidad fina cabe íntegra dentro de ellos y ningún KPI cambió de definición.
- La alerta `TONNAGE_DROP` sigue evaluándose **sobre los mismos grupos**, con la misma ventana y el mismo umbral. Lo que HU-41 sí corrigió es el grupo al que se atribuye una caída: hasta entonces se elegía con `LIMIT 1` **sin orden**, correcto solo por casualidad porque casi todo ejercicio tenía una zona. Ahora es el de su **primera zona principal**, y por tanto determinista.

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

  **`PLATEAU` identifica qué implementos están estancados** (CA-40.05). Se derivan de los contadores vigentes de los pares del ejercicio en lugar de persistirse con la alerta: entre que se emite y se lee, los contadores pueden moverse, y lo que hay que enseñar es el estado de hoy. Con un solo implemento estancado el bloque no se presenta y el texto es el de siempre. El contador que la narrativa cita es el **mayor** entre los pares: la meseta del ejercicio es una conjunción, así que se alcanza cuando el último par cruza el umbral.

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
  "trigger_data": {
    "stalled_pairs": [
      {
        "equipment_type_name": "TEXT // Solo en PLATEAU. Los implementos que alcanzaron el umbral efectivo",
        "sessions_without_progression": "INTEGER"
      }
    ]
  },
  "causal_analysis": "TEXT // Explicación en lenguaje natural. Cuando el umbral depende de la dificultad del ejercicio, justifica por qué el sistema esperó lo que esperó. En PLATEAU con más de un implemento estancado, los nombra: una meseta sin la entidad concreta que la origina no es accionable",
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

  **Desde HU-40 la reducción y el reinicio se calculan sobre la carga de cada par `(ejercicio, equipamiento)`, de forma independiente** (CA-40.06). El listado de reinicio presenta un renglón por par, nombrando el implemento. Un par sin historial anterior a la descarga **no aparece**: no hay carga previa que reducir ni que reiniciar.

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
      "equipment_type_name": "TEXT // El implemento del par: un ejercicio hecho con dos aparece dos veces",
      "restart_load_kg": "REAL // 90% de la carga pre-descarga DEL PAR. Solo disponible al finalizar."
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
- **Formato `schemaVersion: 16` desde HU-42.** El respaldo incluye `exercise_one_rm` —el 1RM de cada par `(ejercicio, equipamiento)`—, que entra al volcado detrás de `exercise` y de `equipment_type`, como sus claves foráneas exigen. El formato 15 de HU-41 ya incluía el catálogo de zonas con su orden, la **jerarquía principal/secundaria** de cada ejercicio y el **equipamiento sugerido** de cada asignación del plan.
- **Los formatos anteriores se rechazan**, el 15 incluido, y con un mensaje que nombra su causa propia: no lleva el 1RM. El 1RM es un **récord acumulado**, no un derivado reconstruible —recalcularlo desde las series podría perder un máximo alcanzado en una serie ya purgada o restaurada parcialmente—, y CA-42.08 exige que el respaldo lo reproduzca **sin recalcularlo**. Aceptar un v15 dejaría la vista de `O1` vacía sobre un historial lleno de series que sí calificaron. Los formatos más antiguos siguen cayendo por lo primero que les falta: el equipamiento de las series (CA-39.12).

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Archivo JSON generado con todos los datos. Metadatos incluyen versión del esquema (16) y fecha de exportación. Opciones para compartir el archivo vía apps del sistema.`
- **Ampliación de HU-39:** el respaldo incluye `exercise_equipment` —las opciones de equipamiento de cada ejercicio— y el `equipment_type_id` de cada fila de `exercise_set`, que viaja con el resto de sus columnas. La tabla se inserta después de `exercise` y de `equipment_type`, como su clave foránea exige.
- **Ampliación de HU-40:** el respaldo incluye la **progresión de cada par** —estado, carga prescrita y contador de sesiones sin progresión de cada `(ejercicio, equipamiento)`— y la tabla `session_exercise_progression` con la clasificación por implemento de cada sesión. Esta última se inserta después de `session_exercise` y de `equipment_type`. Un respaldo restaurado reproduce el estado del motor **par por par, sin recalcularlo**.
- **Ampliación de HU-42:** el respaldo incluye `exercise_one_rm`. Un respaldo restaurado reproduce los récords **tal cual, sin recalcularlos** desde el historial. Un archivo del formato vigente al que le falte la tabla se rechaza por incompleto, igual que si le faltara cualquier otra.

```json
{
  "file_path": "TEXT // Ruta del archivo generado",
  "file_size_kb": "INTEGER",
  "schema_version": 16,
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
- **Compatibilidad de formato:** `Se acepta ÚNICAMENTE el formato vigente (14). Todo formato anterior se rechaza por completo con un mensaje explícito, en lugar de importarse parcialmente.`
- **Estado de Error (formato anterior):** `ERR_BACKUP_NO_EQUIPMENT. El mensaje nombra la causa: el respaldo se generó con una versión anterior y no incluye el equipamiento de las series ni la progresión por par, de modo que no puede restaurarse. Nada se importa.`
- **Estado de Error (formato posterior):** `ERR_BACKUP_VERSION_UNSUPPORTED. Un respaldo de un build más nuevo no tiene esa causa concreta y se rechaza por número de versión, que es lo único que se sabe de él.`

> **Por qué el rechazo es total.** Hasta HU-38 se aceptaban los formatos 11 y 8 porque lo que les faltaba era derivable: `tree_state` se reconstruye entero desde el historial restaurado (`N1-T1`), así que restaurar sin él nunca dejaba un estado inválido. **El equipamiento de la serie no se deriva de nada**: un respaldo anterior no dice con qué implemento se hizo cada serie, y `exercise_set.equipment_type_id` es `NOT NULL`. Aceptarlo exigiría inventar un valor por serie, que es exactamente la importación parcial que HU-39 prohíbe. Con el rechazo, los caminos de importación de v8 y v11/v12 se retiraron del código: importación inalcanzable es una promesa que la aplicación ya no cumple.
>
> **El formato 13 se rechaza por el mismo argumento.** Su `exercise_progression` está claveada solo por `exercise_id` y no dice con qué implemento se acumuló cada estado. Ahora `equipment_type_id` es `NOT NULL` y encabeza la clave primaria junto a `exercise_id`, así que aceptarlo obligaría a inventar un implemento por fila — y la restauración tiene que reproducir el estado del motor par por par **sin recalcularlo**.

---

### 2.11. Módulo: `Flujo N — Árbol de Entrenamiento`

*Flujo de una sola pantalla, alcanzable exclusivamente desde `B1-T8`. **No produce efectos sobre ningún otro contenedor:** no altera la determinación de sesión, no genera alertas, no modifica ningún KPI y ningún componente del motor de decisión lee su estado. Es la excepción de alcance declarada en ADR-020 — puramente visual y de dependencia unidireccional. **Desde HU-38** la pantalla presenta el árbol como modelo tridimensional con una representación nativa de respaldo, y gana la única interacción de cámara del sistema (`N1-T3`); ninguna de las dos cosas amplía la frontera: el estado que se presenta y su origen no cambiaron (ver ADR-021).*

---

#### `N1-T1`: Recalcular y Mostrar el Estado del Árbol

- **Tipo de Trigger (Entrada):** `Automático: al componerse N1. También se dispara al cerrar una sesión (E4-T1), en cada emisión del cambio de día —después de B1-T7— y tras restaurar un respaldo (J3-T1).`
- **Descripción:** Deriva las dos dimensiones del árbol del historial de sesiones y las persiste antes de presentarlas. **Recalcular antes de observar** es lo que garantiza que lo mostrado nunca sea un valor rancio, aunque la aplicación llevara horas abierta sin cruzar la medianoche.
  - **Etapa (la forma):** por total de sesiones `COMPLETED` e `INCOMPLETE` — Semilla 0 · Brote 1–9 · Joven 10–29 · Maduro 30+. **Nunca retrocede** cualquiera que sea la salud.
  - **Salud (el color):** por días naturales `d` desde la última sesión — `d ≤ 2` → 100 · `2 < d < 14` → descenso lineal · `d ≥ 14` → 0. El corte de 14 se alinea con el umbral de crisis de `ROUTINE_INACTIVITY`, que mide inactividad **por rutina** frente a la medida **global** del árbol: son complementarias y no se acoplan.
- **Qué cuenta como entrenamiento:** sesiones `COMPLETED` e `INCOMPLETE`; **no** las `IN_PROGRESS`. Los días registrados en `day_skip` **no protegen al árbol** — omitir un día lo marchita igual que no abrir la aplicación. Una sesión reasignada temporalmente (`B1-T3`) cuenta como cualquier otra: al árbol le da igual **qué** rutina se entrenó.
- **Sin historial:** la etapa es Semilla y **no se presenta conteo de días** — no hay referencia contra la cual contar, y la salud se muestra en 100 para no castigar a quien todavía no ha tenido oportunidad de entrenar.
- **Modo de fallo:** *best-effort*. Si el recálculo falla, se presenta lo último persistido, que sigue siendo una lectura válida del historial; la pantalla nunca queda vacía y el fallo no interrumpe el cierre de sesión ni el arranque. El cálculo no bloquea la interfaz (RNF01).
- **Dos representaciones del mismo estado (HU-38):** El estado que este trigger produce **no cambió**. Lo que cambió es que la pantalla lo dibuja de dos formas posibles, y la elección no la hace el ejecutante ni el dominio:
  - **Modelo 3D** (por defecto) — WebView con Three.js empaquetado, rotable, con interpolación continua entre estados de salud.
  - **Ícono vectorial nativo** (fallback **permanente**) — la representación de HU-37, cuando el WebView no está disponible, su versión es anterior a la mínima soportada o el render falla.

  La sustitución es **silenciosa y sin mensaje de error**: el ícono está presente desde el primer instante y el modelo 3D aparece encima con un fundido cuando reporta su primer fotograma. La pantalla nunca queda en blanco y el puntaje, los días y el mensaje contextual se presentan igual en ambos casos. **La tarjeta de Inicio (`B1-T8`) es siempre nativa** — nunca aloja un WebView (RNF01).
- **Contrato del puente nativo ↔ web (HU-38):**

  | Dirección | Mecanismo | Firma | Comportamiento ante fallo |
  | --- | --- | --- | --- |
  | Nativo → Web | `WebView.evaluateJavascript` | `window.tensionTree.setState(healthScore: Int 0-100, stageCode: 'SEED'\|'SPROUT'\|'YOUNG'\|'MATURE')` | Un código de etapa desconocido cae en `SEED`, igual que `TreeGrowthStage.fromCode`. Una excepción dentro del render se reporta como `onFailure`. |
  | Web → Nativo | Objeto `@JavascriptInterface` `TreeBridge` | `onReady()` · `onFailure(reason: String)` | `onReady` habilita el modelo 3D. `onFailure` — incluso después de `onReady`, si muere el proceso de render — devuelve la pantalla al ícono nativo. `reason` es diagnóstico y **no se presenta al ejecutante**. |

  El puente **no transporta datos de dominio** en dirección web → nativo: el código web no devuelve información al sistema, solo dos señales sobre la disponibilidad del render. La calidad de render y el tema claro/oscuro **no** viajan por `setState`: entran como *query string* al cargar el asset, porque son propiedades del dispositivo y del sistema y se fijan una vez. Si `onReady` no llega dentro del margen previsto, el fallback se activa por timeout — es la última red, para los fallos que ni el WebView ni el JavaScript alcanzaron a reportar.

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

#### `N1-T3`: Rotar la Cámara del Árbol

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: arrastre de un dedo o pinza de dos dedos sobre el área del árbol en N1. Solo disponible con el modelo 3D presente — con el fallback nativo el área no es interactiva.`
- **Descripción:** Mueve la cámara alrededor del árbol. **No produce ningún efecto de estado**: no escribe, no navega y no altera lo que la pantalla presenta. Es la única interacción nueva que HU-38 introduce en todo el sistema.
  - **Arrastre horizontal:** órbita alrededor del eje del árbol, **sin límite** — girar en redondo no pierde nada de vista.
  - **Arrastre vertical:** elevación, **acotada** entre −10° y +55°. Ni por debajo del suelo ni cenital.
  - **Pinza:** zoom, **acotado** a una banda proporcional al encuadre de la etapa. El extremo cercano queda muy por encima del radio de la copa, de modo que **la cámara no puede atravesar el árbol**.
  - **Paneo:** **no existe.** El objetivo de la cámara es fijo en el eje del árbol. Un paneo acotado y un paneo ausente son indistinguibles para el ejecutante, y el ausente no puede tener un defecto de límites.
- **Aislamiento del gesto:** El arrastre **no propaga scroll** ni al documento web —barras ocultas, scroll bloqueado— ni a la pantalla nativa, que además no tiene contenedor desplazable.
- **Persistencia de la cámara:** **ninguna, por construcción.** El estado de la cámara vive en el WebView, el WebView se destruye al salir de la pantalla y al volver se crea uno nuevo. **Al reentrar, la cámara está en su posición inicial** porque no hay nada que sobreviva, no porque se reinicie.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `La cámara se desplaza dentro de sus límites. Ningún cambio de estado del sistema, ninguna navegación, ninguna escritura.`

```json
{
  "navigation": "ninguna",
  "state_change": "ninguno"
}
```

---

### 2.12. Módulo: `Flujo O — 1RM Estimado`

*Flujo de una sola pantalla, alcanzable exclusivamente desde `G1-T4`. **No produce efectos sobre ningún otro contenedor:** no escribe nada, no genera alertas, no modifica ningún KPI y ningún componente del motor de decisión lee su estado. Sigue el modelo de aislamiento que ADR-020 declaró para el árbol, con una diferencia que conviene tener presente: el valor que presenta **no es derivable en caliente**, se acumula al registrar cada serie (`E2-T1`) y por eso el respaldo lo transporta en lugar de recalcularlo.*

---

#### `O1-T1`: Consultar el 1RM Estimado

- **Tipo de Trigger (Entrada):** `Automático: al componerse O1.`
- **Descripción:** Presenta el 1RM de cada par `(ejercicio, equipamiento)` que tiene récord, **una tarjeta por ejercicio** y en orden **alfabético** por nombre. No recalcula nada: el récord ya está persistido desde la serie que lo produjo, así que aquí solo se lee.
  - **Solo lo entrenado, y solo lo que califica.** La lista son exactamente las filas de `exercise_one_rm`. Un ejercicio del Diccionario sin ninguna serie no aparece; un ejercicio entrenado cuyas series nunca cumplieron la condición tampoco; y un implemento entrenado sin serie que califique **no se presenta como opción**, aunque otro implemento del mismo ejercicio sí tenga valor. **Nunca se muestra un cero ni un guion:** si no hay dato, no hay fila.
  - **Implementos dentro de la tarjeta.** Se presentan en el orden declarado del catálogo de equipamiento, el mismo de `D5-T1`. El primero queda activo al abrir. Un ejercicio con **un solo** implemento con récord lo presenta como etiqueta no interactiva y muestra su valor directamente, sin exigir selección.
  - **Doble unidad siempre.** El valor se presenta en kilogramos **y** en libras a la vez, sin conmutador, con el factor vigente del sistema (0.45359237). La presentación **no depende** de la `capture_unit` con la que se registró la serie.
  - **Solo el número.** No se presenta el peso, ni las repeticiones, ni la fecha de la serie que produjo el récord.
- **Estado vacío:** cuando no hay ninguna fila —sin sesiones, o con sesiones cuyas series nunca calificaron— la pantalla **enuncia la condición de cálculo** (10 repeticiones con RIR 1) en lugar de dejar una lista en blanco. El vacío real se distingue de la carga: el mensaje no se presenta antes de saber si hay datos.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `O1 presenta una tarjeta por ejercicio con récord, con sus implementos y el valor del activo.`

```json
{
  "exercises": [
    {
      "exercise_id": "INTEGER",
      "exercise_name": "TEXT",
      "equipment": [
        {
          "equipment_type_id": "INTEGER",
          "equipment_type_name": "TEXT",
          "one_rm_kg": "REAL // Canónico. Las libras se derivan en presentación."
        }
      ]
    }
  ],
  "navigation": "ninguna"
}
```

---

#### `O1-T2`: Seleccionar Implemento dentro de una Tarjeta

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca un chip de implemento en una tarjeta de O1. Solo disponible cuando el ejercicio tiene dos o más implementos con récord.`
- **Descripción:** Cambia el valor que la tarjeta muestra, **en su sitio**. No navega, no escribe y no recompone la lista: un ejercicio sigue siendo una tarjeta y la lista no se multiplica por implemento. La selección es estado de presentación y **no se persiste**; al volver a entrar, cada tarjeta arranca de nuevo en su primer implemento.

**Payload / Parámetros (Input):**

```json
{
  "exercise_id": "INTEGER",
  "equipment_type_id": "INTEGER"
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `La tarjeta presenta el 1RM del par elegido. Ningún cambio de estado del sistema, ninguna navegación, ninguna escritura.`

```json
{
  "navigation": "ninguna",
  "state_change": "ninguno"
}
```

---

#### `O1-T3`: Volver a Métricas

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: retroceso nativo de la barra superior de O1.`
- **Descripción:** **Única acción de navegación de la pantalla.** O1 no tiene formularios, ni modales, ni acciones destructivas: aquí no se decide nada, solo se mira.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Navegación a G1. Ningún cambio de estado del sistema.`

```json
{
  "navigation": "G1"
}
```

---

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
| `ERR_EXERCISE_NAME_DUPLICATE` | Se intenta crear un ejercicio con un nombre ya existente | Mostrar error en D5 vía Snackbar. Desde HU-39 el nombre es único **por sí solo**: el implemento no forma parte de la identidad del ejercicio, así que un nombre repetido con otro equipamiento tampoco se admite. |
| `ERR_PRIMARY_ZONE_REQUIRED` | Se intenta guardar un ejercicio sin ninguna zona muscular principal (`D5-T1`, `D2-T1`) | El botón de guardar ya está deshabilitado sin principales. Si llega a la capa de datos, mostrar error inline **en el campo de zonas principales**, que es donde se incumple: «Elige al menos una zona principal». Las secundarias son opcionales. |
| `ERR_ZONE_IN_BOTH_LISTS` | Se intenta poner una zona como principal y secundaria del mismo ejercicio (`D5-T1`, `D2-T1`) | El diálogo ya la ofrece marcada y no seleccionable. Si llega a la capa de datos, mostrar error inline: la clave primaria de `exercise_muscle_zone` hace el estado irrepresentable, así que el rechazo protege el mensaje, no el dato. |
| `ERR_SUGGESTION_NOT_ADMITTED` | Se intenta fijar como equipamiento sugerido del plan uno que el ejercicio no admite (`D6-T1a`, `D4-T2`) | El selector solo ofrece las opciones admitidas. Si llega a la capa de datos, rechazar sin escribir y nombrar el implemento y el ejercicio. |
| `ERR_SUGGESTION_REQUIRED` | Se intenta asignar un ejercicio al plan sin elegir equipamiento sugerido (`D4-T2`) | El botón de asignar ya está deshabilitado sin sugerencia. La columna es `NOT NULL`: no se persiste la asignación sin ella. |
| `ERR_EQUIPMENT_SUGGESTED_BY_PLAN` | Se intenta retirar de un ejercicio una opción de equipamiento que alguna asignación del plan tiene como sugerencia (`D2-T1`) | Impedir mientras esa asignación exista, y **nombrar la rutina**: un «no se puede» sin decir dónde obliga a buscar a mano. Se comprueba después de la última opción y de las series registradas, por orden de coste de reparación. |
| `ERR_EQUIPMENT_REQUIRED` | Se intenta guardar un ejercicio sin equipamiento (`D5-T1`), retirar la última opción de uno existente (`D2-T3`) o registrar una serie sin implemento (`E2-T1`) | El botón correspondiente ya está deshabilitado con la selección vacía. Si llega a la capa de datos, mostrar error inline en el campo de equipamiento: el atributo es obligatorio y no admite lista vacía. |
| `ERR_EQUIPMENT_HAS_SETS` | Se intenta retirar de un ejercicio un equipamiento con el que ya hay series registradas (`D2-T3`) | La casilla se presenta con candado. Mostrar mensaje inline que nombra el implemento: la serie es inmutable y no puede quedar apuntando a un equipamiento que el ejercicio dejó de admitir. |
| `ERR_BACKUP_NO_EQUIPMENT` | El respaldo seleccionado se generó con un formato anterior a 13 y no incluye el equipamiento de las series | Mostrar mensaje explícito en J3 nombrando la causa. No ejecutar restauración, ni total ni parcial. |

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
- **El WebView del árbol no sale de su archivo local:** `El WebView de N1 carga exclusivamente assets/tree/tree.html, empaquetado en el APK. Toda navegacion se rechaza en shouldOverrideUrlLoading y las cargas de red estan bloqueadas, pero la garantia de fondo no es esa: la aplicacion no declara el permiso INTERNET, de modo que la imposibilidad de alcanzar contenido remoto la impone el sistema operativo y no la configuracion. Three.js y el HTML viajan dentro del APK, sin CDN ni descarga en tiempo de ejecucion (RNF-09, ADR-021).`
- **La tarjeta de Inicio nunca aloja un WebView:** `B1-T8 presenta el arbol como icono vectorial nativo de forma permanente, tambien despues de HU-38. Es una restriccion de rendimiento (RNF-01), no una limitacion provisional: el WebView vive exclusivamente dentro de N1.`
- **El presupuesto de render manda sobre la fidelidad visual:** `Si el dispositivo no completa la carga y el render inicial del arbol 3D en menos de 1 segundo, o no sostiene la fluidez del gesto, el sistema degrada los graficos por codigo -sombras, luego esferas de la copa, luego segmentos del tronco, luego poligonos por primitiva- hasta cumplirlo. La degradacion es automatica y no se ofrece como ajuste al ejecutante. Ante conflicto entre fluidez y fidelidad, gana la fluidez (CA-38.06).`
- **La representación nativa del árbol no se elimina:** `El icono vectorial de HU-37 se conserva en el codigo de forma permanente como fallback de N1. Un dispositivo sin WebView, con WebView desactualizado o incapaz de renderizar contenido 3D presenta el icono sin mensaje de error, sin pantalla en blanco y sin perder ninguna informacion de la pantalla (CA-38.05, RNF-20).`
- **El árbol no tiene pestaña propia:** `N1 es una ruta nueva alcanzable solo desde la tarjeta de B1 (B1-T8). No se añade a la barra de navegacion inferior, que permanece visible durante la pantalla.`
- **Un tipo, un implemento:** `El catálogo equipment_type no admite valores compuestos ni disyunciones: cada fila nombra un implemento y no una alternativa entre varios. La alternativa vive en exercise_equipment —los implementos que el ejercicio admite— y la elección en exercise_set.equipment_type_id. Los agarres no son equipamiento: cuerda y barra en V son Polea. No existe interfaz para crear, renombrar ni eliminar tipos de equipamiento: la tabla es cerrada y sembrada.`
- **Ningún ejercicio sin equipamiento:** `Todo ejercicio declara al menos un implemento admitido, en la creación y en la edición. La validación vive en la capa Domain, la interfaz deshabilita el guardado con la selección vacía y la relación no se puede dejar vacía retirando opciones una a una.`
- **El equipamiento de la serie no se corrige:** `exercise_set es inmutable tras su creación, y el implemento registrado no es la excepción. Ninguna interfaz expone su edición. Por eso tampoco se puede retirar de un ejercicio un implemento con el que ya se registró una serie: la fila quedaría apuntando a algo que el ejercicio dejó de admitir.`
- **Idioma único:** `Toda la interfaz opera exclusivamente en español. No existe selector de idioma ni soporte para internacionalización (RNF-08).`
