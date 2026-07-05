# Contrato de Interfaces e Interacciones

> Este documento define los "puertos" de comunicación del sistema. Detalla estrictamente cómo el entorno interactúa con el sistema (entradas) y cómo el sistema responde (salidas). **Se mantiene agnóstico a la implementación visual**, enfocándose en la estructura de los datos, eventos, comandos y reglas de validación que gobiernan cada interacción.
>
> **Alcance:** 27 vistas distribuidas en 10 flujos funcionales. Cada vista define sus triggers de entrada (acciones del ejecutante o eventos del sistema), su payload de datos y su respuesta esperada.

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

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el botón "Iniciar Sesión" en la vista B1 (Home). Solo disponible si no hay sesión IN_PROGRESS existente.`
- **Descripción:** El sistema determina la rutina y versión correspondientes según el estado actual de rotación cíclica, crea una nueva sesión con status `IN_PROGRESS`, crea una fila `session_exercise` por cada slot del plan de la rutina-versión asignada con el ejercicio primario de cada slot, y navega a E1.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `Nueva sesión creada. session.status = 'IN_PROGRESS'. session_exercise creados para cada slot del plan. Navegación a E1 con la sesión activa.`

```json
{
  "session_id": "INTEGER // ID de la sesión recién creada",
  "routine_version_id": "INTEGER // Determinado por rotation_state.microcycle_position",
  "routine_name": "TEXT",
  "version_number": "INTEGER",
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
  "media_resource": "TEXT | null // Ruta asset o archivo. null = mostrar placeholder"
}
```

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

### 2.5. Módulo: `Flujo E — Sesión Activa`

---

#### `E1-T1`: Intercambiar Alternativa de Ejercicio (Swap)

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca el ícono de intercambio (⇄) en un ejercicio con estado 'No Iniciado' que tiene múltiples alternativas en su slot. Despliega bottom sheet con las alternativas disponibles.`
- **Descripción:** El sistema actualiza `session_exercise.exercise_id` al ejercicio alternativo seleccionado. `original_exercise_id` se mantiene `NULL` (no es sustitución, es selección entre equivalentes). Solo posible con 0 series registradas.

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

**Payload / Parámetros (Input):**

```json
{
  "session_exercise_id": "INTEGER // Obligatorio.",
  "weight_kg": "REAL >= 0 // Obligatorio. 0 para ejercicios de peso corporal e isométricos.",
  "reps": "INTEGER >= 1 // Obligatorio. Para isométricos: segundos sostenidos.",
  "rir": "INTEGER [0, 1, 2] // Obligatorio. Reserva de esfuerzo percibida."
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

- **Estado de Error:** `Valores fuera de rango no se persisten. El formulario muestra error inline en el campo inválido. El botón de confirmar permanece deshabilitado.`

---

#### `E3-T1`: Seleccionar Ejercicio Sustituto

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: selecciona un ejercicio de la lista en E3 y confirma la sustitución.`
- **Descripción:** El sistema actualiza `session_exercise.original_exercise_id = exercise_id_original` y `session_exercise.exercise_id = exercise_id_sustituto`. La carga objetivo se recalcula para el ejercicio sustituto. Solo posible si el ejercicio tiene 0 series.

**Payload / Parámetros (Input):**

```json
{
  "session_exercise_id": "INTEGER",
  "substitute_exercise_id": "INTEGER // Debe pertenecer a la misma zona muscular que el ejercicio original. No puede estar ya prescrito en la sesión activa."
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado de Éxito:** `session_exercise actualizado. original_exercise_id preserva el ejercicio original. exercise_id apunta al sustituto. Retorno a E1.`

---

#### `E4-T1`: Cerrar Sesión

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: confirma el cierre en el diálogo E4 (Confirmación de Cierre de Sesión).`
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
- **Descripción:** El sistema recupera la sesión completa con sus ejercicios y series. Refleja el ejercicio que realmente se ejecutó (incluyendo sustituciones).

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
      "was_substituted": "BOOLEAN",
      "original_exercise_name": "TEXT | null",
      "progression_classification": "TEXT | null",
      "sets": [
        {
          "set_number": "INTEGER",
          "weight_kg": "REAL",
          "reps": "INTEGER",
          "rir": "INTEGER"
        }
      ]
    }
  ]
}
```

---

#### `F3-T1`: Consultar Historial de Ejercicio

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: navega a F3 desde D2, E5, F2, G1 o H2 con un ejercicio específico.`
- **Descripción:** El sistema recupera todos los registros históricos del ejercicio, incluyendo sesiones donde fue ejecutado como sustituto. Calcula la tendencia de carga.

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
- **Descripción:** El sistema calcula los 4 KPIs principales para todos los ejercicios y rutinas del ejecutante.

**Payload / Parámetros (Input):**

```json
{
  "period_weeks": "INTEGER // Período de evaluación de KPIs. Default: 4 semanas."
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "progression_rates": [
    {
      "exercise_id": "INTEGER",
      "exercise_name": "TEXT",
      "rate_pct": "REAL // % sesiones con progresión positiva en el período"
    }
  ],
  "load_velocity": [
    {
      "exercise_id": "INTEGER",
      "exercise_name": "TEXT",
      "kg_per_session": "REAL // (peso_actual - peso_inicial) / sesiones_intermedias"
    }
  ],
  "rir_averages": [
    {
      "routine_id": "INTEGER",
      "routine_name": "TEXT",
      "rir_avg": "REAL",
      "interpretation": "TEXT // 'OPTIMAL' (≈1), 'TOO_LOW' (<0.5), 'TOO_HIGH' (>1.8)"
    }
  ],
  "adherence": {
    "completed_this_week": "INTEGER",
    "planned_this_week": "INTEGER",
    "adherence_pct": "REAL"
  },
  "microcycle_count": "INTEGER"
}
```

---

#### `G2-T1`: Consultar Volumen por Grupo Muscular

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Volumen por Grupo Muscular" en G1.`
- **Descripción:** El sistema calcula el tonelaje acumulado y la distribución de volumen por microciclo para cada grupo muscular.

**Payload / Parámetros (Input):**

```json
{}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "tonnage_by_group": [
    {
      "muscle_group": "TEXT",
      "microcycles": [
        {
          "microcycle_number": "INTEGER",
          "tonnage_kg": "REAL"
        }
      ],
      "trend": "TEXT // 'ASCENDING', 'STABLE', 'DECLINING'"
    }
  ],
  "volume_distribution": [
    {
      "muscle_zone": "TEXT",
      "muscle_group": "TEXT",
      "series_count": "INTEGER",
      "pct_of_routine": "REAL"
    }
  ]
}
```

---

#### `G3-T1`: Consultar Tendencia de Progresión por Grupo Muscular

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca "Tendencia de Progresión" en G1.`
- **Descripción:** El sistema evalúa la trayectoria de tonelaje y tasa de progresión de cada grupo muscular en los últimos 4-6 microciclos.

**Payload / Parámetros (Input):**

```json
{
  "microcycles_back": "INTEGER // Períodos evaluados. Default: entre 4 y 6."
}
```

**Respuesta / Salida (Output Esperado):**

```json
{
  "sufficient_data": "BOOLEAN // false si microcycle_count < 4",
  "microcycles_available": "INTEGER",
  "trends": [
    {
      "muscle_group": "TEXT",
      "classification": "TEXT // 'ASCENDING', 'STABLE', 'DECLINING'",
      "progression_rate_avg": "REAL"
    }
  ]
}
```

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
      "message": "TEXT",
      "created_at": "TEXT"
    }
  ],
  "total_count": "INTEGER"
}
```

---

#### `H2-T1`: Consultar Detalle de Alerta

- **Tipo de Trigger (Entrada):** `Acción del ejecutante: toca una alerta en H1.`
- **Descripción:** El sistema recupera el detalle completo de la alerta, recalcula dinámicamente los datos que la dispararon y genera las recomendaciones de acción correctiva según el tipo.

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
  "recommendations": [
    {
      "step": "INTEGER",
      "action": "TEXT",
      "condition": "TEXT"
    }
  ],
  "has_deload_link": "BOOLEAN // true para tipos ROUTINE_REQUIRES_DELOAD",
  "has_exercise_link": "BOOLEAN // true para tipos PLATEAU, LOW_PROGRESSION_RATE"
}
```

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
| `ERR_EXERCISE_NOT_IN_DICT` | Se intenta registrar un ejercicio que no existe en `exercise` | Estado inválido — previsto por la arquitectura. Log de error interno. |
| `ERR_SUBSTITUTION_INVALID_ZONE` | El ejercicio sustituto no pertenece a la misma zona muscular | Excluir el ejercicio de la lista E3. Nunca presentarlo como opción válida. |
| `ERR_SUBSTITUTION_HAS_SETS` | Se intenta sustituir un ejercicio que ya tiene series registradas | Ocultar el botón "Sustituir" si el ejercicio tiene ≥ 1 serie. |
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
- **Tiempo máximo de ejecución de Backup/Restore:** `El proceso de exportación e importación de datos debe completarse en menos de 10 segundos para un historial de hasta 2 años (estimado: ~35,000 registros en exercise_set, base de datos < 5 MB) (RNF-18).`
- **Inmutabilidad de sesiones cerradas:** `Las sesiones con status = 'COMPLETED' o 'INCOMPLETE' no tienen ningún trigger de edición disponible. F2 (Detalle de Sesión Pasada) es estrictamente de solo lectura. Ninguna interfaz expone acciones de modificación retroactiva de series ya registradas.`
- **Restricción de navegación durante sesión activa:** `Cuando el flujo E (Sesión Activa) está en curso, la barra de navegación global (Bottom Navigation) se oculta completamente. El único canal de salida del flujo de sesión es E4 (Confirmación de Cierre). No existe ningún trigger que permita abandonar la sesión sin cerrarla formalmente.`
- **Restricción de orientación:** `La interfaz opera exclusivamente en orientación vertical (portrait). El sistema no soporta modo horizontal (landscape). Si el dispositivo se rota, la vista se mantiene en portrait (RNF-07).`
- **Idioma único:** `Toda la interfaz opera exclusivamente en español. No existe selector de idioma ni soporte para internacionalización (RNF-08).`
