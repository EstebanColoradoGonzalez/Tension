# Historia de Usuario

**Como** ejecutante,
**Quiero** poder definir ejercicios alternativos para un mismo puesto dentro de mi plan de entrenamiento,
**Para** que al iniciar una sesión pueda intercambiar cuál de ellos realizar en ese momento.

## Descripción

Como ejecutante, necesito poder definir ejercicios alternativos para un mismo puesto dentro de mi plan de entrenamiento, de forma que al iniciar una sesión pueda intercambiar cuál de ellos realizar en ese momento. Esto me permite mantener variedad controlada en mi entrenamiento sin tener que modificar el plan manualmente, y refleja la práctica habitual de prescribir opciones equivalentes (e.g., "Hip Thrust ó Sentadilla Búlgara ó Sentadilla de Zumo") para un mismo slot de trabajo.

### Contexto de dominio

Un **slot** (`slot: Int`) en `plan_assignment` agrupa ejercicios del mismo puesto funcional dentro de una versión de rutina. El slot no es PK ni campo único: múltiples ejercicios comparten el mismo valor de `slot` dentro de la misma `routine_version_id`. La PK de `plan_assignment` sigue siendo `(routine_version_id, exercise_id)`, garantizando unicidad de ejercicio por versión.

Al iniciar sesión, cada slot crea **un solo** `session_exercise` con el ejercicio primario (primer `sort_order` del slot). El usuario puede intercambiar cuál ejercicio del slot desea realizar antes de registrar cualquier serie — una vez que registra la primera serie, el intercambio queda bloqueado.

### Problema que resuelve

Sin alternativas por slot, el usuario que quiere variar su entrenamiento dentro del plan debe editar manualmente la versión de rutina, romper la coherencia de progresión y reiniciar el historial del ejercicio. Con esta historia, puede prescribir múltiples opciones equivalentes en diseño de plan y elegir en tiempo real sin alterar la estructura del programa.

---

## Criterios de Aceptación

### Bloque A — Definición de alternativas en el plan (CA-26.01–CA-26.07)

**CA-26.01**
```
DADO que estoy en la vista de detalle de una versión de rutina (pantalla D4)
CUANDO visualizo la lista de ejercicios asignados
ENTONCES cada ejercicio asignado muestra un botón "+" que permite agregar una alternativa para ese mismo puesto (slot)
```

**CA-26.02**
```
DADO que pulso el botón "+" de un ejercicio en el plan (D4)
CUANDO se abre el bottom sheet de selección
ENTONCES puedo seleccionar un ejercicio del catálogo para agregarlo como alternativa
  Y el ejercicio seleccionado hereda automáticamente el número de series y repeticiones del ejercicio principal del slot
```

**CA-26.03**
```
DADO que un slot tiene múltiples ejercicios asignados
CUANDO visualizo el plan (D4)
ENTONCES los ejercicios que comparten el mismo slot se muestran como UNA SOLA FILA
  Y el título de la fila concatena los nombres con " ó " (e.g., "Hip Thrust ó Sentadilla Búlgara ó Sentadilla de Zumo")
  Y la lista del plan muestra una fila por slot (no una fila por ejercicio)
```

**CA-26.04**
```
DADO que visualizo una fila de slot en el plan (D4)
CUANDO interactúo con los controles de la fila
ENTONCES la fila tiene su propio botón de agregar alternativa (+)
  Y botón de edición de series/repeticiones (✏️)
  Y botón de eliminación (🗑️)
  Y la edición de series/repeticiones se propaga automáticamente a TODAS las alternativas del slot
```

**CA-26.05**
```
DADO que pulso el botón de eliminación (🗑️) en una fila de slot con múltiples ejercicios
CUANDO confirmo la eliminación
ENTONCES se eliminan TODOS los ejercicios de ese slot (el principal y todas las alternativas)
```

**CA-26.06**
```
DADO que un slot tiene un único ejercicio asignado (sin alternativas)
CUANDO elimino ese ejercicio
ENTONCES el comportamiento es idéntico al existente antes de HU-26
```

**CA-26.07**
```
DADO que intento agregar un ejercicio como alternativa a un slot
CUANDO el ejercicio ya está asignado a esa versión de rutina (mismo exercise_id, cualquier slot)
ENTONCES la operación es rechazada
  Y no se permite agregar el mismo exercise_id dos veces en la misma versión de rutina, independientemente del slot
```

### Bloque B — Intercambio de ejercicio durante la sesión (CA-26.08–CA-26.14)

**CA-26.08**
```
DADO que inicio una sesión a partir de una versión de rutina con slots de múltiples ejercicios
CUANDO se generan los session_exercise
ENTONCES cada slot crea UN SOLO session_exercise con el ejercicio primario (primer sort_order del slot)
  Y todos los ejercicios quedan con exercise_id asignado y pending_selection = 0
```

**CA-26.09**
```
DADO que un slot tiene un único ejercicio asignado
CUANDO se genera el session_exercise al iniciar sesión
ENTONCES el comportamiento es exactamente igual que antes de HU-26 (sin cambios para slots simples)
```

**CA-26.10**
```
DADO que estoy en la pantalla de sesión activa (E1)
  Y un ejercicio tiene alternativas en su slot (alternativesInSlot > 1)
  Y el ejercicio está en estado NOT_STARTED (completedSets == 0)
CUANDO visualizo el ejercicio
ENTONCES se muestra un icono de intercambio (↔ SwapHoriz) junto a los botones "Registrar serie" y "Sustituir"
CUANDO el ejercicio tiene al menos una serie registrada (completedSets > 0)
ENTONCES el icono de intercambio desaparece
```

**CA-26.11**
```
DADO que pulso el icono de intercambio (↔) en la sesión activa (E1)
CUANDO se abre el bottom sheet de alternativas
ENTONCES la lista muestra todas las alternativas del slot con nombre, equipamiento y resumen de zonas musculares
  Y puedo seleccionar una y confirmar
```

**CA-26.12**
```
DADO que confirmo la selección de una alternativa en el bottom sheet de intercambio
CUANDO se ejecuta el intercambio
ENTONCES el session_exercise se actualiza: exercise_id cambia al ejercicio elegido
  Y original_exercise_id se limpia a NULL (cambio entre ejercicios del plan, no sustitución)
  Y la fila de sesión se actualiza con el nombre y datos del nuevo ejercicio
```

**CA-26.13**
```
DADO que el usuario intenta intercambiar la alternativa de un ejercicio
CUANDO el ejercicio ya tiene al menos una serie registrada (completedSets > 0)
ENTONCES el intercambio NO está disponible (icono SwapHoriz no se muestra)
```

**CA-26.14**
```
DADO que la sesión contiene slots con alternativas disponibles
CUANDO el usuario intenta cerrar la sesión
ENTONCES el cierre NO se bloquea por la presencia de alternativas
  Y el usuario puede cerrar la sesión en cualquier momento
```

### Bloque C — Integridad de datos y migración (CA-26.15–CA-26.17)

**CA-26.15**
```
DADO que la app se actualiza con HU-26
CUANDO se ejecutan las migraciones de base de datos
ENTONCES la DB queda en versión 13
  Y MIGRATION_11_12 agrega la columna slot en plan_assignment
  Y MIGRATION_11_12 agrega las columnas pending_selection y slot en session_exercise
  Y MIGRATION_12_13 corrige los valores de slot en plan_assignment y session_exercise para instalaciones donde el seeder no estableció slot correctamente
```

**CA-26.16**
```
DADO que existen registros en plan_assignment antes de la migración
CUANDO se ejecuta MIGRATION_11_12
ENTONCES la columna slot se inicializa con el valor de sort_order de cada registro
  Y se preserva el orden actual de los ejercicios en el plan
```

**CA-26.17**
```
DADO que existen registros en session_exercise antes de la migración
CUANDO se ejecuta MIGRATION_11_12
ENTONCES pending_selection queda inicializado a 0 en todos los registros existentes
  Y slot queda inicializado con el slot correspondiente al ejercicio del plan de esa sesión
```

### Bloque D — Restricciones de concurrencia (CA-26.18)

**CA-26.18**
```
DADO que existe una sesión activa (IN_PROGRESS) para una versión de rutina
CUANDO el usuario intenta agregar una alternativa, asignar un ejercicio o eliminar un slot de esa versión
ENTONCES la operación es rechazada por los use cases
  Y AddAlternativeToSlotUseCase valida hasActiveSessionForVersion(routineVersionId)
  Y AssignExerciseToVersionUseCase valida hasActiveSessionForVersion(routineVersionId)
  Y UnassignExerciseFromVersionUseCase valida hasActiveSessionForVersion(routineVersionId)
  Y el usuario recibe un mensaje de error via Snackbar informando que hay una sesión activa
```
