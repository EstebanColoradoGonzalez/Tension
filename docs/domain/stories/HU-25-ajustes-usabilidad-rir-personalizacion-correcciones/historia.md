# Historia de Usuario

**Como** ejecutante,
**Quiero** un conjunto de mejoras de usabilidad y correcciones en la aplicación,
**Para** que la experiencia de entrenamiento sea más precisa, flexible y coherente.

## Descripción

Como ejecutante, necesito un conjunto de mejoras de usabilidad y correcciones en la aplicación para que la experiencia de entrenamiento sea más precisa, flexible y coherente: poder modificar la cantidad de series y repeticiones de un ejercicio tanto en el plan oficial (afectando futuras sesiones) como puntualmente durante una sesión activa (sin afectar el plan); que el rango de RIR se ajuste a 0-2 para reflejar una escala fisiológicamente relevante; que los filtros del diccionario de ejercicios muestren únicamente las zonas musculares y tipos de equipamiento que efectivamente tienen ejercicios registrados; que el ejercicio Face Pull esté correctamente clasificado como Espalda Alta; que el botón de navegación "Configuración" se acorte a "Ajustes" para que quepa en la barra de navegación; y que el catálogo de zonas musculares y tipos de equipamiento disponibles al crear un ejercicio sea exhaustivo.

### Contexto y justificación

**Patrón Arquitectónico:** Migración DDL + Data (MIGRATION_10_11) + Cambios transversales en las 4 capas (Data, Domain, UI, strings). No introduce patrón arquitectónico nuevo. Los cambios se distribuyen en 7 bloques funcionales independientes que comparten una única migración de base de datos.

**Justificación:** HU-25 es una historia de **ajustes acumulados** — combina correcciones de datos (Face Pull, zonas/equipamiento), mejoras de usabilidad (edición de series/reps, finalización anticipada, series extra), reducción de rango RIR, filtros dinámicos y correcciones cosméticas ("Ajustes"). La migración MIGRATION_10_11 es significativamente más simple que MIGRATION_9_10 (HU-24): no hay recreación de tablas ni eliminación de ejercicios, solo 1 ALTER TABLE, INSERTs de catálogos y 1 UPDATE de reclasificación. Se sigue el mismo patrón establecido por MIGRATION_6_7 (HU-16), MIGRATION_7_8 (HU-21), MIGRATION_8_9 (HU-23) y MIGRATION_9_10 (HU-24).

**Hallazgo Crítico — `plan_assignment.reps` sin CHECK constraint en DDL real:**

El documento `Modelo de Datos.md` (línea 282) documenta `CHECK(reps IN ('8-12', 'TO_TECHNICAL_FAILURE', '30-45_SEC'))`, pero el esquema real de Room (schema v10, `10.json` línea 505) define `reps TEXT NOT NULL` **sin CHECK constraint**. Esto simplifica CA-25.03: no se requiere migración DDL para relajar el campo — `plan_assignment.reps` ya acepta cualquier valor TEXT.

**Hallazgo — Series extra/finalización anticipada requieren `is_finalized`:**

Actualmente la completitud de un ejercicio se determina por `completedSets >= prescribedSets → COMPLETED`. Este mecanismo impide series extra e impide finalización anticipada. La solución es agregar `session_exercise.is_finalized INTEGER NOT NULL DEFAULT 0`:

```
Nuevo modelo:
  is_finalized = 1                    → COMPLETED
  completedSets == 0 && !is_finalized → NOT_STARTED
  completedSets > 0  && !is_finalized → IN_PROGRESS
```

---

## Criterios de Aceptación

### Personalización de series y repeticiones en el plan oficial

- **CA-25.01:** En la vista de detalle de una versión de rutina (D4), cada ejercicio asignado muestra su cantidad de series y rango de repeticiones actual, y ofrece una opción para editarlos.
- **CA-25.02:** Al editar series/repeticiones de un ejercicio en el plan, el sistema actualiza el registro en `plan_assignment` (`sets` y `reps`) para esa combinación `routine_version_id` + `exercise_id`.
- **CA-25.03:** El campo de series acepta valores enteros entre 1 y 10. El campo de repeticiones acepta los formatos ya soportados: rango numérico (e.g. "8-12", "6-8"), "TO_TECHNICAL_FAILURE" y "30-45_SEC".
- **CA-25.04:** Los cambios en el plan oficial se reflejan inmediatamente en la prescripción de futuras sesiones. Las sesiones ya cerradas (completadas o incompletas) no se ven afectadas.
- **CA-25.05:** La edición de series/repeticiones en el plan NO modifica datos históricos: las series registradas en sesiones pasadas conservan sus valores originales.

### Personalización de series y repeticiones en sesión activa

- **CA-25.06:** Durante una sesión activa (E1), el ejecutante puede agregar series adicionales a un ejercicio más allá de las prescritas por el plan. Al registrar la serie prescrita final, el sistema ofrece la opción "Agregar serie extra" en lugar de marcar el ejercicio como completado.
- **CA-25.07:** Durante una sesión activa (E1), el ejecutante puede finalizar un ejercicio antes de completar todas las series prescritas. El sistema ofrece una opción "Finalizar ejercicio" en cualquier momento, marcándolo como completado con las series que haya registrado hasta ese punto.
- **CA-25.08:** Las series extra o la finalización anticipada de un ejercicio son puntuales: solo afectan la sesión actual y NO modifican la cantidad de series prescritas en `plan_assignment`.
- **CA-25.09:** El resumen de sesión (E5) muestra la cantidad real de series ejecutadas vs. las prescritas (e.g. "5/4 series" o "2/4 series"), permitiendo distinguir sesiones con series extra o incompletas.
- **CA-25.10:** Los KPIs, alertas y reglas de progresión se calculan con los datos realmente registrados en la sesión, independientemente de si se agregaron series extra o se finalizó anticipadamente.

### Reducción del rango de RIR de 0-5 a 0-2

- **CA-25.11:** El selector de RIR en la pantalla de registro de serie (E2, composable `RirSelector`) muestra únicamente los valores 0, 1 y 2 (en lugar de 0 a 5). Se renderizan 3 botones circulares en vez de 6.
- **CA-25.12:** El valor de RIR almacenado en `exercise_set.rir` queda restringido al rango 0-2 para nuevos registros. Los registros históricos con valores 3, 4 o 5 se preservan sin modificación.
- **CA-25.13:** Los umbrales de alerta de RIR en `AlertThresholdRule` se ajustan al nuevo rango:
  - `RIR_LOW_THRESHOLD` cambia de `1.5` a `0.5` (alerta cuando el promedio es menor a 0.5).
  - `RIR_HIGH_THRESHOLD` cambia de `3.5` a `1.8` (alerta cuando el promedio es mayor a 1.8).
- **CA-25.14:** La referencia de RIR en la pantalla de métricas (G1) se actualiza: el texto `metrics_rir_reference` cambia de "2–3 = óptimo · < 1.5 = riesgo · > 3.5 = insuficiente" a "1 = óptimo · < 0.5 = riesgo · > 1.8 = insuficiente".
- **CA-25.15:** El protocolo de descarga referencia RIR en su texto descriptivo. El string `deload_protocol_rir` cambia de "RIR 4–5" a "RIR 2" y el string `deload_params_rir` cambia de "RIR objetivo: 4–5" a "RIR objetivo: 2". La lógica de descarga que prescribe RIR durante sesiones de deload se ajusta para prescribir RIR 2 (el máximo del nuevo rango).
- **CA-25.16:** Los strings `deload_protocol_bodyweight` y `deload_protocol_isometric` que mencionan "RIR 4–5" se actualizan a "RIR 2".

### Filtros dinámicos en el diccionario de ejercicios

- **CA-25.17:** El filtro de "Zona" en el diccionario de ejercicios (D1) muestra únicamente las zonas musculares que tienen al menos un ejercicio registrado en la base de datos (precargado o custom). Si no hay ejercicios con zona "Abductores", esa opción no aparece en el dropdown.
- **CA-25.18:** El filtro de "Equipo" en el diccionario de ejercicios (D1) muestra únicamente los tipos de equipamiento que tienen al menos un ejercicio registrado en la base de datos. Si no hay ejercicios de tipo "Cuerpo", esa opción no aparece en el dropdown.
- **CA-25.19:** Los filtros se actualizan dinámicamente: al crear un ejercicio con una nueva zona muscular o tipo de equipamiento, esa opción aparece inmediatamente en los filtros sin reiniciar la app. Al eliminar el último ejercicio de una zona o equipo, esa opción desaparece.
- **CA-25.20:** El `GetFilterOptionsUseCase` se modifica para consultar las zonas musculares y tipos de equipamiento que efectivamente están asociados a ejercicios existentes, en lugar de listar todas las entradas de las tablas `muscle_zone` y `equipment_type`.
- **CA-25.21:** Los filtros en la pantalla de creación de ejercicio (D5) siguen mostrando TODAS las zonas musculares y tipos de equipamiento disponibles en el sistema (incluidos los que no tienen ejercicios asociados), ya que el usuario necesita poder seleccionar cualquier opción para su nuevo ejercicio. Solo los filtros del listado del diccionario (D1) se hacen dinámicos.

### Reclasificación del ejercicio Face Pull

- **CA-25.22:** El ejercicio "Face Pull" se reclasifica de zona muscular "Hombro" (ID 7) a "Espalda Alta" como zona principal. Se requiere crear una nueva zona muscular "Espalda Alta" (muscle_group = "Espalda") si no existe.
- **CA-25.23:** En el `BaseDataSeeder`, se agrega la nueva zona muscular "Espalda Alta" con `muscle_group = "Espalda"` usando el siguiente ID disponible.
- **CA-25.24:** En el `ExerciseSeeder`, la asignación de zona muscular del Face Pull (`emz`) se actualiza de `muscle_zone_id = 7` (Hombro) al ID de la nueva zona "Espalda Alta".
- **CA-25.25:** En la migración `MIGRATION_10_11` (HU-25), se inserta la nueva zona muscular y se actualiza la relación `exercise_muscle_zone` del Face Pull para apuntar a "Espalda Alta" en vez de "Hombro".
- **CA-25.26:** El Diccionario de Ejercicios (`docs/business_definition/Diccionario de Ejercicios.md`) se actualiza para reflejar la zona muscular correcta del Face Pull.

### Renombrar "Configuración" a "Ajustes" en navegación

- **CA-25.27:** El texto del botón de navegación inferior para la sección de configuración cambia de "Configuración" a "Ajustes". El string `nav_settings` cambia su valor de "Configuración" a "Ajustes".
- **CA-25.28:** El título de la pantalla de configuración (J1) también se actualiza: el string `settings_title` cambia de "Configuración" a "Ajustes".
- **CA-25.29:** La referencia en el string `import_backup_back_to_settings` cambia de "Volver a Configuración" a "Volver a Ajustes".
- **CA-25.30:** La ruta de navegación (`NavigationRoutes.SETTINGS`) y el nombre interno del módulo no cambian; solo se modifica el texto visible al usuario.

### Ampliación de zonas musculares y tipos de equipamiento

- **CA-25.31:** Se evalúan y agregan las siguientes zonas musculares faltantes al `BaseDataSeeder` (tabla `muscle_zone`), con su correspondiente `muscle_group`:

  | ID | Zona Muscular | Grupo Muscular |
  |----|---------------|----------------|
  | 16 | Espalda Alta | Espalda |
  | 17 | Trapecio | Espalda |
  | 18 | Espalda Baja | Espalda |
  | 19 | Antebrazo | Antebrazo |
  | 20 | Cuello | Cuello |

- **CA-25.32:** Se evalúan y agregan los siguientes tipos de equipamiento faltantes al `BaseDataSeeder` (tabla `equipment_type`):

  | ID | Tipo de Equipamiento |
  |----|---------------------|
  | 16 | Banda Elástica |
  | 17 | Kettlebell |
  | 18 | Barra EZ |
  | 19 | TRX / Suspensión |
  | 20 | Balón Medicinal |
  | 21 | Rodillo de Abdomen |
  | 22 | Paralelas / Dip Station |
  | 23 | Barra Fija |

- **CA-25.33:** Las nuevas zonas musculares y tipos de equipamiento se insertan tanto en el seed data (para instalaciones nuevas) como en la migración correspondiente (para usuarios existentes).
- **CA-25.34:** Las zonas musculares y tipos de equipamiento existentes (IDs 1-15 de zonas, IDs 1-15 de equipamiento) no se modifican ni se eliminan.

### Actualización de documentación

- **CA-25.35:** El documento `docs/business_definition/Diccionario de Ejercicios.md` se actualiza para reflejar la zona muscular corregida del Face Pull (Espalda Alta en vez de Hombro).
- **CA-25.36:** El documento `docs/business_definition/Requerimientos.md` se actualiza donde se mencione el rango de RIR, cambiando de 0-5 a 0-2 y ajustando los umbrales de alerta (RF54/RF55) al nuevo rango. Específicamente: RF13 ("RIR en escala de 0 a 5" → "0 a 2"), RF22 ("4 series = Completado" → descripción dinámica), RF54 ("< 1.5" → "< 0.5"), RF55 ("> 3.5" → "> 1.8").
- **CA-25.37:** El documento `docs/business_definition/Manifiesto de Dominio Sistémico.md` se actualiza donde se mencione el rango de RIR, ajustándolo de 0-5 a 0-2. Específicamente: §5 definición RIR "escala 0 a 5" (línea ~145), §6-A R5 deload "RIR objetivo 4-5" (línea ~359), §B tabla de umbrales "< 1.5" y "> 3.5" (líneas ~459-460, ~519-520).
- **CA-25.38:** El documento `docs/architecture/Modelo de Datos.md` se actualiza para reflejar: el nuevo rango de RIR (0-2), la nueva zona muscular "Espalda Alta" y las zonas/equipamientos adicionales, y la capacidad de modificar series/repeticiones tanto en plan como en sesión. Específicamente: §3.12 exercise_set campo rir "Rango: 0 a 5" → "0 a 2" (línea ~418), CHECK documentado "rir >= 0 AND rir <= 5" → "rir >= 0 AND rir <= 2" (línea ~427), §3.16 deload "RIR objetivo 4-5" → "RIR objetivo 2" (línea ~519), §3.11 session_exercise agregar campo `is_finalized` y actualizar la nota de derivación de estado, §4 relaciones "0 a 4 series" → "0 a N series" (línea ~628).
- **CA-25.39:** El documento `docs/architecture/Arquitectura Técnica.md` se actualiza donde corresponda para reflejar los cambios en reglas de negocio (umbrales de RIR) y nuevas funcionalidades (edición de series/reps).
- **CA-25.40:** El documento `docs/architecture/Especificación Visual.md` se actualiza para reflejar: el selector de RIR con 3 botones (0-2), el botón "Ajustes" en lugar de "Configuración", y los controles de edición de series/repeticiones.
- **CA-25.41:** El documento `docs/architecture/Wireframes.md` se actualiza para reflejar las nuevas interacciones (edición de series/reps en plan y sesión, selector de RIR reducido).
- **CA-25.42:** Cualquier otro documento en `docs/` que haga referencia al rango de RIR 0-5, al nombre "Configuración", a la clasificación del Face Pull como "Hombro", o a las zonas musculares/tipos de equipamiento originales, se actualiza para mantener coherencia con los cambios de esta historia.
