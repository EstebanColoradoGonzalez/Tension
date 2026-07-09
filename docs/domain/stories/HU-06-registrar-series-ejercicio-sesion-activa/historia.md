# Historia de Usuario

**Como** ejecutante,
**Quiero** registrar los datos de cada serie de cada ejercicio durante una sesión activa de forma rápida, en cualquier orden y con los datos precargados de mi última sesión,
**Para** capturar mi entrenamiento con la menor fricción posible mientras mantengo precisión y completitud en los registros.

---

## Criterios de Aceptación

### CA-06.01 — Captura de datos por serie

**Dado que** el ejecutante tiene una sesión activa y selecciona un ejercicio para registrar una serie,
**cuando** accede al formulario de registro de serie,
**entonces** el sistema solicita exactamente tres datos: Peso en Kg, Repeticiones logradas y RIR (Reps In Reserve), todos obligatorios para completar el registro.

### CA-06.02 — Registro en máximo 3 toques

**Dado que** el ejecutante ha seleccionado un ejercicio en la sesión activa,
**cuando** registra una serie,
**entonces** el flujo de registro requiere un máximo de 3 toques para completarse: ingresar peso, ingresar repeticiones, ingresar RIR y confirmar, aprovechando la precarga de datos y minimizando la fricción.

### CA-06.03 — Teclado numérico para campos de entrada

**Dado que** el ejecutante está registrando una serie durante la sesión activa,
**cuando** activa cualquier campo de entrada numérica (peso, repeticiones o RIR),
**entonces** el sistema despliega un teclado numérico optimizado, no el teclado alfanumérico completo.

### CA-06.04 — Precarga del último peso utilizado

**Dado que** el ejecutante tiene historial previo para el ejercicio que está registrando,
**cuando** accede al formulario de registro de serie para ese ejercicio,
**entonces** el campo de peso se precarga automáticamente con el último peso utilizado para ese ejercicio, permitiendo al ejecutante confirmar rápidamente si la carga no cambió.

### CA-06.05 — Validación de peso

**Dado que** el ejecutante ingresa un valor de peso en el registro de serie,
**cuando** el valor es menor que 0,
**entonces** el sistema rechaza el registro y muestra un mensaje de error claro indicando que el peso debe ser ≥ 0 Kg (donde 0 es válido exclusivamente para ejercicios de peso corporal).

### CA-06.06 — Validación de repeticiones

**Dado que** el ejecutante ingresa un valor de repeticiones en el registro de serie,
**cuando** el valor es menor que 1,
**entonces** el sistema rechaza el registro y muestra un mensaje de error claro indicando que las repeticiones deben ser ≥ 1.

### CA-06.07 — Validación de RIR

**Dado que** el ejecutante ingresa un valor de RIR en el registro de serie,
**cuando** el valor está fuera del rango 0 a 5,
**entonces** el sistema rechaza el registro y muestra un mensaje de error claro indicando que el RIR debe estar entre 0 y 5.

### CA-06.08 — Asociación automática de metadatos

**Dado que** el ejecutante confirma el registro de una serie con datos válidos,
**cuando** el sistema persiste el registro,
**entonces** asocia automáticamente la serie con: la Fecha actual, el Módulo de la sesión, la Versión del módulo, el Ejercicio ejecutado y el Número de Serie secuencial (1, 2, 3 o 4), sin requerir que el ejecutante ingrese ninguno de estos datos manualmente.

### CA-06.09 — Secuencia de series por ejercicio

**Dado que** el ejecutante registra series de un ejercicio durante la sesión activa,
**cuando** registra cada serie,
**entonces** el sistema asigna automáticamente el número de serie secuencial: la primera serie registrada es 1, la segunda es 2, la tercera es 3 y la cuarta es 4, sin permitir más de 4 series por ejercicio.

### CA-06.10 — Orden libre de registro de ejercicios

**Dado que** el ejecutante tiene una sesión activa con múltiples ejercicios prescritos,
**cuando** decide registrar series,
**entonces** el sistema permite registrar los ejercicios en cualquier orden, sin imponer la secuencia en que aparecen en el Plan de Entrenamiento. El ejecutante puede empezar por cualquier ejercicio, registrar series parciales de uno, cambiar a otro y volver.

### CA-06.11 — Vinculación al ejercicio realmente ejecutado

**Dado que** el ejecutante ha realizado una sustitución puntual de un ejercicio durante la sesión,
**cuando** registra las series del ejercicio sustituto,
**entonces** el sistema vincula los datos registrados al ejercicio que realmente se ejecutó (el sustituto), no al ejercicio originalmente prescrito, garantizando coherencia entre lo registrado y lo efectivamente realizado.

### CA-06.12 — Estado visual de ejercicios en la sesión

**Dado que** el ejecutante tiene una sesión activa,
**cuando** visualiza la lista de ejercicios de la sesión,
**entonces** el sistema muestra el estado de cada ejercicio: "No Iniciado" cuando tiene 0 series registradas, "En Ejecución" cuando tiene entre 1 y 3 series registradas, o "Completado" cuando tiene 4 series registradas.

### CA-06.13 — Preservación de datos ante cierre inesperado

**Dado que** el ejecutante tiene una sesión activa con series registradas,
**cuando** la aplicación se cierra inesperadamente (cierre por el usuario, falta de batería o crash),
**entonces** el sistema preserva todos los datos de la sesión en progreso y todas las series registradas hasta ese momento. Al reabrir la aplicación, la sesión puede continuarse desde el punto donde quedó.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite capturar el entrenamiento del ejecutante con precisión y mínima fricción, manteniendo datos completos para análisis de progresión.

### Reglas de Negocio

1. **Transacción atómica en registro de serie:** Al registrar una serie, se insertan atómicamente en `exercise_set` y se crea idempotentemente en `exercise_progression`. Si falla cualquier INSERT, se revierte todo. Transición: serie registrada con número secuencial asignado.
2. **Número de serie secuencial:** La primera serie es 1, segunda es 2, tercera es 3, cuarta es 4. Máximo 4 series por ejercicio en MVP. Calculado como `COUNT(*) + 1` dentro de transacción.
3. **Precarga cross-session del último peso:** Busca el último peso en TODAS las sesiones anteriores para el ejercicio. Permite precarga correcta al iniciar nueva sesión.
4. **Validaciones en 2 capas:** UI (feedback inmediato en ViewModel) + UseCase (`require()`) como última línea de defensa.
5. **Peso corporal/isométrico fijo:** Para ejercicios de peso corporal o isométricos, el peso es fijo en 0 y no editable.
6. **RIR por chips:** Selector visual con chips 0-5, single select, rango garantizado por construcción.
7. **Metadatos automáticos por contexto relacional:** La cadena `exercise_set → session_exercise → session → module_version` proporciona Fecha, Módulo, Versión, Ejercicio y Número de serie automáticamente.

### Interfaz

- **Formulario E2 — `RegisterSetScreen`:** Pantalla de registro de serie con 3 variantes: estándar, peso corporal, isométrico. `CenterAlignedTopAppBar` con cierre (✕) + título (nombre del ejercicio + "Serie N de 4"). Body con `Column` padding 16 dp:
  - **Campo Peso (Kg):** `OutlinedTextField` con 3 variantes — estándar (editable, `KeyboardType.Decimal`, precargado), peso corporal (fijo "0", disabled), isométrico (fijo "0", disabled).
  - **Campo Repeticiones/Segundos:** `OutlinedTextField` — estándar/peso corporal: "Repeticiones" con `KeyboardType.Number`; isométrico: "Segundos sostenidos" con `supportingText = "(Referencia: 30–45 seg)"`.
  - **Selector RIR:** 6 chips circulares `Box` 48×48 dp, single select, valores 0-5.
  - **Botón "Confirmar":** Full width, habilitado solo si todos los campos son válidos.
  - **"Cancelar":** `TextButton` centrado.
  - Sin Bottom Navigation.
- **Payload requerido:** `sessionExerciseId: Long` (argumento de navegación E1→E2).

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Preview de Interfaz

Ver `Especificación Visual §8 E2` para RegisterSetScreen. Los mockups definen colores, tipografía y layout del formulario de registro de serie con sus 3 variantes.

---

## Dependencias Técnicas e Integración

### 4.1. Modelo de Dominio y Estado

- **Entidades Afectadas:** `ExerciseSetEntity` (INSERT), `ExerciseProgressionEntity` (INSERT idempotente), `SessionExerciseEntity` (lectura JOIN), `SessionEntity` (lectura JOIN), `PlanAssignmentEntity` (lectura JOIN).
- **Mutaciones de Estado:** INSERT en `exercise_set` (la serie registrada), INSERT idempotente en `exercise_progression` (crea fila si primera serie del ejercicio). No se crean entidades Room nuevas — las 4 tablas de sesión ya existen desde HU-05.

### 4.2. Contrato de Interfaces (Triggers / API)

- **Trigger / Endpoint consumido:** `E1 ActiveSessionScreen — Botón "Registrar" (por ejercicio) → E2 RegisterSetScreen`.
- **Payload requerido:** `sessionExerciseId: Long` (argumento de navegación E1→E2).

### 4.3. UI / Assets

- **Componente Visual:** `E2 — RegisterSetScreen` (nuevo — formulario de registro con 3 variantes: estándar, peso corporal, isométrico). Sin Bottom Navigation.
