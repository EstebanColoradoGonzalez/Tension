# Historia de Usuario

**Como** ejecutante,
**Quiero** que al sustituir ejercicios durante una sesión activa, la lista de ejercicios elegibles como sustitutos refleje correctamente el estado dinámico de la sesión en todo momento — incluyendo ejercicios descartados por sustituciones previas y mostrando la totalidad de ejercicios del módulo disponibles en el Diccionario,
**Para** poder realizar múltiples sustituciones en una misma sesión sin encontrar omisiones ni candidatos incorrectos que me obliguen a improvisar o quedarme sin alternativas reales.

## Descripción

Esta historia corrige dos defectos en la funcionalidad de sustitución puntual de ejercicios durante una sesión activa (HU-07):

1. **Filtro estático en sustituciones múltiples:** Cuando el ejecutante realiza una primera sustitución y luego intenta una segunda, la lista de candidatos no refleja el estado dinámico de la sesión — el sustituto recién seleccionado aparece como candidato disponible, y el ejercicio descartado no aparece como candidato.

2. **Lista incompleta de ejercicios del módulo:** La lista de candidatos no muestra todos los ejercicios que pertenecen al módulo activo en el Diccionario. Hay ejercicios registrados con el `module_code` correspondiente que no aparecen en la lista de sustitución.

La lista de candidatos elegibles para sustitución en cualquier momento de la sesión debe ser: todos los ejercicios del módulo en el Diccionario, menos los que están actualmente asignados en la sesión — ni más, ni menos.

Esta historia **no redefine** la funcionalidad de sustitución — la especificación original de HU-07 es correcta. Los criterios de aceptación CA-07.01 a CA-07.06 siguen siendo válidos. HU-20 corrige la implementación para que el comportamiento observable del sistema cumpla con lo que HU-07 ya especificaba, y extiende los criterios de aceptación para cubrir escenarios de sustitución múltiple.

---

## Criterios de Aceptación

### Bloque A — Filtro dinámico en sustituciones múltiples

#### CA-20.01 — Primera sustitución: el sustituto seleccionado se excluye de futuras listas

**Dado que** el ejecutante tiene una sesión activa con los ejercicios prescritos [E1, E2, E3, ..., En],
**y** realiza una sustitución reemplazando E1 por el ejercicio S1 (tomado de la lista de candidatos),
**cuando** posteriormente intenta sustituir otro ejercicio de la sesión (por ejemplo E2),
**entonces** el ejercicio S1 **no aparece** en la lista de candidatos, porque S1 ya está asignado a la sesión activa como sustituto de E1.

#### CA-20.02 — Primera sustitución: el ejercicio descartado se reincorpora a la lista de candidatos

**Dado que** el ejecutante tiene una sesión activa con los ejercicios prescritos [E1, E2, E3, ..., En],
**y** realiza una sustitución reemplazando E1 por el ejercicio S1,
**cuando** posteriormente intenta sustituir otro ejercicio de la sesión (por ejemplo E2),
**entonces** el ejercicio E1 (descartado en la sustitución anterior) **sí aparece** en la lista de candidatos, porque E1 ya no forma parte de la sesión activa y es un ejercicio válido del módulo.

#### CA-20.03 — Segunda sustitución consecutiva: estado acumulado correcto

**Dado que** el ejecutante realizó una primera sustitución (E1 → S1) y ahora realiza una segunda sustitución (E2 → S2),
**cuando** intenta sustituir un tercer ejercicio (por ejemplo E3),
**entonces** la lista de candidatos:
- **Excluye** todos los ejercicios actualmente en la sesión: los ejercicios prescritos no sustituidos (E3, E4, ..., En) más los sustitutos incorporados (S1, S2).
- **Incluye** todos los ejercicios descartados por sustituciones anteriores (E1, E2) y cualquier otro ejercicio del módulo que no esté en la sesión.

#### CA-20.04 — Sustitución con N reemplazos en la misma sesión

**Dado que** el ejecutante realiza múltiples sustituciones en la misma sesión (cualquier cantidad, limitada únicamente por la cantidad de ejercicios en estado "No Iniciado"),
**cuando** accede a la lista de candidatos para cualquier sustitución posterior,
**entonces** la lista refleja el **estado actual real** de la sesión en ese preciso momento: excluye exactamente los `exercise_id` que están actualmente asignados en la sesión (independientemente de si son originales o sustitutos) e incluye todo ejercicio del módulo del Diccionario que no esté actualmente asignado.

#### CA-20.05 — El ejercicio que se va a sustituir no aparece en su propia lista de candidatos

**Dado que** el ejecutante selecciona un ejercicio de la sesión (por ejemplo E3) para sustituirlo,
**cuando** el sistema presenta la lista de candidatos elegibles,
**entonces** el ejercicio E3 **no aparece** en la lista (no tiene sentido reemplazar un ejercicio por sí mismo), a pesar de que pertenece al módulo.

### Bloque B — Lista completa de ejercicios del módulo

#### CA-20.06 — Todos los ejercicios del módulo en el Diccionario son candidatos potenciales

**Dado que** el ejecutante accede a la pantalla de sustitución durante una sesión activa del módulo X (donde X es A, B o C),
**cuando** el sistema construye la lista de ejercicios candidatos,
**entonces** la lista incluye **todos** los ejercicios del Diccionario que tienen `module_code = X`, incluyendo:
- Ejercicios precargados (seed data) asignados a alguna versión del plan del módulo X.
- Ejercicios precargados (seed data) que pertenecen al módulo X pero que **no están asignados a ninguna versión específica** del plan (si existieran).
- Ejercicios creados por el ejecutante (custom) que pertenecen al módulo X, estén o no asignados a alguna versión del plan.

La única exclusión válida son los ejercicios que ya están actualmente asignados en la sesión activa (según el estado dinámico definido en el Bloque A).

#### CA-20.07 — Verificación concreta: Módulo A completo

**Dado que** el ejecutante tiene una sesión activa del Módulo A y la sesión tiene asignados, por ejemplo, 11 ejercicios,
**cuando** intenta sustituir cualquier ejercicio de la sesión,
**entonces** la lista de candidatos muestra exactamente: (total de ejercicios del Módulo A en el Diccionario) menos (los ejercicios actualmente asignados en la sesión), sin omitir ningún ejercicio que pertenezca al Módulo A.

*Ejemplo numérico con datos actuales del Diccionario: El Módulo A tiene 14 ejercicios (4 de espalda, 5 de bíceps, 5 de abdomen). Si la sesión tiene 11 ejercicios asignados, la lista de candidatos debe mostrar exactamente 3 ejercicios (14 − 11 = 3). Si la sesión tiene 10 ejercicios, la lista debe mostrar exactamente 4 (14 − 10 = 4).*

#### CA-20.08 — Verificación concreta: Módulo B completo

**Dado que** el ejecutante tiene una sesión activa del Módulo B,
**cuando** intenta sustituir cualquier ejercicio de la sesión,
**entonces** la lista de candidatos muestra todos los ejercicios del Módulo B en el Diccionario que no están actualmente asignados en la sesión.

*Ejemplo numérico: El Módulo B tiene 15 ejercicios (7 de pecho, 5 de hombro, 3 de tríceps). Si la sesión tiene 11 ejercicios asignados, la lista debe mostrar exactamente 4 ejercicios (15 − 11 = 4).*

#### CA-20.09 — Verificación concreta: Módulo C completo

**Dado que** el ejecutante tiene una sesión activa del Módulo C,
**cuando** intenta sustituir cualquier ejercicio de la sesión,
**entonces** la lista de candidatos muestra todos los ejercicios del Módulo C en el Diccionario que no están actualmente asignados en la sesión.

*Ejemplo numérico: El Módulo C tiene 14 ejercicios. Si la sesión tiene 9 ejercicios asignados, la lista debe mostrar exactamente 5 ejercicios (14 − 9 = 5).*

#### CA-20.10 — Ejercicios custom del ejecutante aparecen en la lista de sustitución

**Dado que** el ejecutante ha creado ejercicios propios (custom) asignados al módulo de la sesión activa,
**cuando** accede a la lista de candidatos para sustitución,
**entonces** los ejercicios custom del módulo aparecen en la lista junto con los precargados, siempre que no estén actualmente asignados en la sesión. No existe distinción de elegibilidad entre ejercicios precargados y custom para sustitución.

### Bloque C — Integridad y consistencia transversal

#### CA-20.11 — Combinación de ambos defectos: sustitución múltiple con lista completa

**Dado que** el ejecutante tiene una sesión activa del Módulo A (V1, con 11 ejercicios prescritos),
**y** realiza una primera sustitución: reemplaza el ejercicio "Tiro de dorsales (Agarre ancho)" por el ejercicio "Curl de Contracción" (ambos del Módulo A, Curl de Contracción no estaba en V1),
**cuando** intenta sustituir un segundo ejercicio (por ejemplo "Curl de martillo cruzado"),
**entonces** la lista de candidatos:
- **Incluye** "Tiro de dorsales (Agarre ancho)" (fue descartado de la sesión en la primera sustitución).
- **Excluye** "Curl de Contracción" (fue incorporado a la sesión en la primera sustitución).
- **Excluye** "Curl de martillo cruzado" (es el ejercicio que se está sustituyendo en este momento).
- **Incluye** todos los demás ejercicios del Módulo A del Diccionario que no están actualmente asignados en la sesión.
- No falta ningún ejercicio del Módulo A que cumpla las condiciones de elegibilidad.

#### CA-20.12 — Restricciones preexistentes de HU-07 se mantienen

**Dado que** HU-20 corrige el filtrado de candidatos sin modificar la mecánica de sustitución,
**entonces** todas las restricciones de HU-07 siguen vigentes:
- Solo se pueden sustituir ejercicios en estado "No Iniciado" (0 series registradas) — CA-07.06 sigue aplicando.
- La sustitución no altera el Plan de Entrenamiento original — CA-07.03 sigue aplicando.
- Solo ejercicios del mismo módulo son elegibles — CA-07.02 sigue aplicando.
- Las sesiones futuras no arrastran sustituciones anteriores — CA-07.04 sigue aplicando.
- El `original_exercise_id` se preserva correctamente — si E1 fue sustituido por S1, el `original_exercise_id` de esa fila sigue siendo E1, independientemente de cuántas otras sustituciones ocurran en la misma sesión.

#### CA-20.13 — Criterio de filtrado es por asignación actual en sesión, no por asignación en plan

**Dado que** el sistema construye la lista de candidatos elegibles para sustitución,
**cuando** aplica el filtro de exclusión,
**entonces** el criterio de exclusión es: *"excluir los ejercicios cuyos IDs están actualmente asignados como `exercise_id` en las filas de `session_exercise` de la sesión activa"*. El filtro **no** se basa en la tabla de asignaciones del plan (`plan_assignment`) ni en los ejercicios de una versión específica, sino en el estado vivo de la sesión en ese momento.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite al ejecutante realizar múltiples sustituciones en una misma sesión sin encontrar omisiones ni candidatos incorrectos.

### Reglas de Negocio

1. **Lista dinámica de candidatos:** La lista de ejercicios elegibles para sustitución debe reflejar el estado dinámico de la sesión en todo momento — excluyendo ejercicios actualmente asignados e incluyendo ejercicios descartados por sustituciones previas.
2. **Todos los ejercicios del módulo son candidatos:** La lista debe incluir todos los ejercicios del Diccionario del módulo activo, incluyendo precargados y custom, sin distinción.
3. **Filtrado por asignación actual:** El criterio de exclusión se basa en los ejercicios actualmente asignados en `session_exercise`, no en las asignaciones del plan.
4. **Restricciones HU-07 se mantienen:** Solo ejercicios en estado "No Iniciado" pueden sustituirse, la mecánica de escritura de la sustitución no cambia.

### Interfaz

- **E3 — `SubstituteExerciseScreen`:** Sin cambios visuales — corrección de comportamiento. La pantalla ya existe en HU-07.
- **Payload requerido:** `moduleCode: String`, `sessionId: Long`

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` | `docs/architecture/domain_and_state_model.md`

**Entidades afectadas:** `ExerciseEntity`, `SessionExerciseEntity` (sin cambios de esquema)

**Interfaces de referencia:** Pantalla E3 — `SubstituteExerciseScreen` (ver `docs/architecture/interfaces_contract.md`)

**Requisitos cubiertos:** RF16 (corrección de implementación)

**Épica / Módulo:** `EPIC-01: Perfil y Catálogos`

**Prioridad:** Alta

**Historias relacionadas:**
- HU-05 (Iniciar sesión — crea `session_exercise`, fuente de verdad para el filtrado dinámico)
- HU-06 (Registrar series ejercicio sesión activa)
- HU-07 (Sustitución Puntual de Ejercicio en Sesión Activa — historia origen con el defecto)

**Nota arquitectónica:** HU-20 es una corrección de defecto (bug fix) — no redefine la funcionalidad de HU-07. La especificación original de HU-07 es correcta. Los criterios de aceptación CA-07.01 a CA-07.06 siguen siendo válidos. El cambio es quirúrgico: reemplazar una query DAO con parámetros externos por una subconsulta SQL embebida en Room, actualizar la firma del Repository, simplificar el ViewModel eliminando el snapshot estático, y limpiar métodos de código muerto.

---

## Definición de Terminado (Inicial)

- [x] Subconsulta SQL embebida en `ExerciseDao.getEligibleSubstitutesForSession()` con Room tracking de tablas
- [x] Firma de `ExerciseRepository.getEligibleSubstitutes()` cambiada a `(moduleCode, sessionId: Long)`
- [x] `SubstituteExerciseViewModel` simplificado — eliminado snapshot estático `excludedIds` del `init`
- [x] `Flow` se re-emite automáticamente cuando `session_exercise` cambia (Room invalidation tracking)
- [x] 7 tests unitarios para `SubstituteExerciseViewModel` (13 CAs validados)
- [x] Eliminación de código muerto: `getByModuleCodeNotInIds()`, `getExerciseIdsForSession()`
- [x] BUILD SUCCESSFUL — 318 tests pasan (0 fallos), 0 errores de compilación
