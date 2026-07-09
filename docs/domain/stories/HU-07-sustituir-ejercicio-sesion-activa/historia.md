# Historia de Usuario: `HU-07` — Sustituir ejercicio puntualmente en sesión activa

> Este documento define una unidad atómica de valor. El código desarrollado debe cumplir **estrictamente** con los Criterios de Aceptación listados aquí. No se deben añadir funcionalidades no descritas en este documento (evitar *scope creep*).

## 1. Metadatos

- **ID:** `HU-07`
- **Épica / Módulo:** `EPIC-02: Ciclo de Entrenamiento`
- **Estado:** `Done`
- **Prioridad:** `Alta`
- **Agente Asignado:** `Developer`
- **Requisitos cubiertos:** RF16

## 2. Narrativa de Negocio

- **Como** ejecutante,
- **Quiero** poder sustituir puntualmente un ejercicio prescrito por otro ejercicio del mismo módulo durante una sesión activa sin modificar el Plan de Entrenamiento,
- **Para** adaptar el entrenamiento a las circunstancias del gimnasio (equipo no disponible, dolor articular) sin comprometer la cobertura muscular ni corromper mi plan original.

## 3. Criterios de Aceptación (BDD)

### CA-07.01 — Sustitución durante sesión activa

**Dado que** el ejecutante tiene una sesión activa y un ejercicio prescrito no puede ejecutarse (equipo ocupado, en mantenimiento o genera dolor articular),
**cuando** selecciona la opción de sustituir ese ejercicio,
**entonces** el sistema presenta la lista de ejercicios elegibles como sustitutos: todos los ejercicios del mismo módulo (A, B o C) del Diccionario — incluyendo tanto los precargados como los creados por el ejecutante — disponibles en cualquier versión del módulo, excluyendo los ya prescritos en la sesión activa.

### CA-07.02 — Restricción al mismo módulo

**Dado que** el ejecutante está seleccionando un ejercicio sustituto,
**cuando** el sistema presenta los ejercicios disponibles,
**entonces** únicamente muestra ejercicios que pertenecen al mismo módulo de la sesión activa; ningún ejercicio de otro módulo es elegible como sustituto.

### CA-07.03 — Sustitución puntual sin alterar el plan

**Dado que** el ejecutante realiza una sustitución de un ejercicio en la sesión activa,
**cuando** la sesión se cierra (Completada o Incompleta),
**entonces** el Plan de Entrenamiento original permanece intacto e inalterado para todas las sesiones futuras; la sustitución solo afecta la sesión en curso.

### CA-07.04 — Sesión futura sin rastro de sustitución anterior

**Dado que** el ejecutante sustituyó un ejercicio en una sesión anterior,
**cuando** inicia la próxima sesión del mismo módulo y versión,
**entonces** el sistema prescribe los ejercicios originales del Plan de Entrenamiento, sin arrastrar la sustitución de la sesión anterior.

### CA-07.05 — Sustitución de ejercicio no iniciado

**Dado que** el ejecutante desea sustituir un ejercicio que aún está en estado "No Iniciado" (0 series registradas),
**cuando** ejecuta la sustitución,
**entonces** el sistema reemplaza el ejercicio prescrito por el sustituto en la sesión activa, y las series registradas posteriormente se vincularán al ejercicio sustituto.

### CA-07.06 — Restricción de sustitución con series registradas

**Dado que** el ejecutante desea sustituir un ejercicio que ya tiene series registradas ("En Ejecución" o "Completado"),
**cuando** intenta ejecutar la sustitución,
**entonces** el sistema no permite la sustitución, ya que las series ya registradas están vinculadas al ejercicio original y cambiar el ejercicio después de registrar datos corrompería la integridad de los registros.

## 4. Dependencias Técnicas e Integración

### 4.1. Modelo de Dominio y Estado

- **Entidades Afectadas:** `SessionExerciseEntity` (UPDATE de `exercise_id` y `original_exercise_id`), `ExerciseEntity` (lectura filtrada por módulo). No se crean entidades nuevas — `session_exercise` ya tiene la columna `original_exercise_id` desde HU-05 (Referencia a `docs/architecture/domain_and_state_model.md`).
- **Mutaciones de Estado:** UPDATE en `session_exercise`: `exercise_id = newExerciseId`, `original_exercise_id = originalExerciseId`. `plan_assignment` permanece intacta (CA-07.03).

### 4.2. Contrato de Interfaces (Triggers / API)

- **Trigger / Endpoint consumido:** `E1 ActiveSessionScreen — Botón "Sustituir" (solo en NotStartedExerciseRow) → E3 SubstituteExerciseScreen` (Referencia a `docs/architecture/interfaces_contract.md`).
- **Payload requerido:** `sessionExerciseId: Long` (argumento de navegación E1→E3).

### 4.3. UI / Assets

- **Componente Visual:** `E3 — SubstituteExerciseScreen` (nuevo — lista de sustitutos elegibles + diálogo de confirmación). Sin Bottom Navigation (Arquitectura Técnica §4.5.1: E3 siempre oculta).
