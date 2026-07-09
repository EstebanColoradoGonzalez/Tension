# Historia de Usuario

> Este documento define una unidad atómica de valor. El código desarrollado debe cumplir **estrictamente** con los Criterios de Aceptación listados aquí. No se deben añadir funcionalidades no descritas en este documento (evitar *scope creep*).

**Como** ejecutante,
**Quiero** consultar el Plan de Entrenamiento completo, visualizando para cada módulo y versión los ejercicios asignados con sus detalles, y poder asignar o desasignar ejercicios de versiones del plan,
**Para** entender la estructura de mi programa, conocer las variantes disponibles, personalizar las versiones según mi necesidad y anticipar qué me espera en cada sesión.

---

## Criterios de Aceptación (BDD)

### CA-04.01 — Plan precargado y completo

**Dado que** el ejecutante abre la aplicación,
**cuando** accede a la consulta del Plan de Entrenamiento,
**entonces** el sistema muestra el plan precargado con los 3 módulos (A, B, C) y sus respectivas versiones: Módulo A con 3 versiones (V1, V2, V3), Módulo B con 3 versiones (V1, V2, V3) y Módulo C con 3 versiones (V1, V2, V3), sin requerir carga de datos externa ni conexión a internet.

### CA-04.02 — Detalle de ejercicios por módulo-versión

**Dado que** el ejecutante selecciona un módulo y una versión específica,
**cuando** consulta el detalle de esa combinación módulo-versión,
**entonces** el sistema muestra la lista completa de ejercicios asignados, con la siguiente información para cada uno: nombre del ejercicio, zona muscular objetivo, tipo de equipo, número de series (4) y rango de repeticiones (8-12, "Al fallo técnico" o rango en segundos para isométricos).

### CA-04.03 — Listado sin orden obligatorio

**Dado que** el ejecutante consulta los ejercicios de una versión del plan,
**cuando** visualiza la lista de ejercicios,
**entonces** el sistema presenta los ejercicios como un listado que no implica secuencia obligatoria de ejecución; el orden de presentación es referencial, no prescriptivo.

### CA-04.04 — Ejercicios con condiciones especiales identificados

**Dado que** el ejecutante consulta una versión del plan que contiene ejercicios con condiciones especiales,
**cuando** visualiza la lista de ejercicios,
**entonces** el sistema diferencia claramente los ejercicios con rango de repeticiones estándar (8-12) de aquellos con condición especial: "Al fallo técnico" para ejercicios de peso corporal (ej. Flexiones) y rango en segundos (30-45 seg) para ejercicios isométricos (ej. Plancha, Plancha Lateral).

### CA-04.05 — Consulta de todos los módulos y versiones

**Dado que** el ejecutante desea explorar el plan completo,
**cuando** navega entre módulos y versiones,
**entonces** el sistema permite acceder a cualquier combinación módulo-versión del plan (A-V1, A-V2, A-V3, B-V1, B-V2, B-V3, C-V1, C-V2, C-V3), mostrando los ejercicios correspondientes a cada una.

### CA-04.06 — Cantidad de ejercicios por versión

**Dado que** el ejecutante consulta una versión del plan,
**cuando** visualiza los ejercicios,
**entonces** el sistema muestra la cantidad correcta de ejercicios por versión según las asignaciones vigentes: inicialmente 11 ejercicios para cada versión del Módulo A, 11 ejercicios para cada versión del Módulo B y 9 ejercicios para cada versión del Módulo C. Estas cantidades pueden variar si el ejecutante asigna o desasigna ejercicios.

### CA-04.07 — Asignar ejercicio a una versión del plan

**Dado que** el ejecutante quiere agregar un ejercicio a una versión específica del plan,
**cuando** selecciona la opción de asignar ejercicio en el detalle de una versión (D4),
**entonces** el sistema presenta los ejercicios del Diccionario pertenecientes al mismo módulo que aún no están asignados a esa versión. El ejecutante selecciona uno, confirma series (por defecto 4) y rango de repeticiones (por defecto "8-12"), y la asignación se persiste. El nuevo ejercicio aparece inmediatamente en la lista de la versión. La asignación no afecta otras versiones del plan.

### CA-04.08 — Desasignar ejercicio de una versión del plan

**Dado que** el ejecutante quiere remover un ejercicio de una versión específica del plan,
**cuando** selecciona la opción de desasignar en un ejercicio de la versión (D4),
**entonces** el sistema solicita confirmación, y al confirmar, elimina todas las asignaciones del puesto (slot) — principal y alternativas. Los ejercicios permanecen en el Diccionario y su historial de registros no se ve afectado. La desasignación no afecta otras versiones donde los ejercicios estén asignados ni las sesiones pasadas. No se permite desasignar mientras haya una sesión activa de esa versión.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** Ejecutante (usuario final de la app).
- **Permisos requeridos:** Ninguno — el ejecutante tiene acceso completo a consultar y gestionar su propio plan.
- **Valor de negocio:** Permite al ejecutante comprender la estructura de su programa de entrenamiento, personalizarlo y anticipar cada sesión, mejorando la adherencia y experiencia de uso.

### Reglas de Negocio

1. **Plan precargado:** El sistema viene con 3 módulos (A, B, C), cada uno con 3 versiones (V1, V2, V3). Las asignaciones iniciales son: Módulo A = 11 ejercicios por versión, Módulo B = 11 ejercicios por versión, Módulo C = 9 ejercicios por versión.
2. **Sin orden obligatorio:** El listado de ejercicios no implica secuencia de ejecución. El orden es referencial.
3. **Condiciones especiales de repeticiones:**
   - Estándar: "8-12 reps"
   - Fallo técnico: "Al fallo técnico" (ejercicios de peso corporal como Flexiones)
   - Isométrico: "30-45 seg" (ejercicios como Plancha, Plancha Lateral)
4. **Asignación restringida al mismo módulo:** Solo se pueden asignar ejercicios del mismo módulo que la versión destino.
5. **Desasignación elimina slot completo:** Se eliminan todas las asignaciones del slot (principal + alternativas). Los ejercicios permanecen en el Diccionario.
6. **No desasignar con sesión activa:** No se permite desasignar mientras haya una sesión activa de esa versión.
7. **Aislamiento entre versiones:** Las asignaciones/desasignaciones no afectan otras versiones del plan.

### Interfaz

- **D3 — TrainingPlanScreen:** Reemplazo del stub existente. Muestra 3 módulos con sus 3 versiones y conteos de ejercicios. Top Bar con tabs (reutiliza estructura de HU-03).
- **D4 — PlanVersionDetailScreen:** Nueva vista. Muestra lista de ejercicios de una versión con prescripción, opciones de asignar/desasignar. FAB para asignar, IconButton Delete para desasignar, Bottom Sheet para selector de ejercicios, AlertDialog para confirmación.

### Sistemas Externos

Ninguno. Operación 100% local. Sin conexión a internet requerida (RNF09).

### Preview de Interfaz

Ver Wireframes D3 y D4, y Especificación Visual §8 D3/D4.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` (capas UI, Data, Domain), `docs/architecture/domain_and_state_model.md` (entidades `ModuleVersionEntity`, `PlanAssignmentEntity`, `ModuleEntity`, `ExerciseEntity`), `docs/architecture/interfaces_contract.md` (contratos D3/D4)

**Historias relacionadas:**
- HU-03 (Diccionario de Ejercicios — seed data de 9 module_versions, 93 plan_assignments, entidades existentes)
- HU-05 (Sesión de Entrenamiento — reutiliza query de detalle de versión)
- HU-19 (Backup/Restore — incluye `plan_assignment` y `module_version`)

**Lecciones aprendidas:** HU-04 es la cuarta historia en implementarse. Transforma el stub `TrainingPlanScreen` (D3) creado en HU-03 en una vista funcional y construye la nueva vista D4. Reutiliza la infraestructura de datos creada en HU-03 — las entidades ya existen con su seed data. El esfuerzo se concentra en nuevos DAOs con JOINs, repositorio, Use Cases y pantallas funcionales.

---

## Definición de Terminado (Inicial)
