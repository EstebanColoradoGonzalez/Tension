# Historia de Usuario: `HU-10` — Evaluar y clasificar progresión post-sesión

> Este documento define una unidad atómica de valor. El código desarrollado debe cumplir **estrictamente** con los Criterios de Aceptación listados aquí. No se deben añadir funcionalidades no descritas en este documento (evitar *scope creep*).

## 1. Metadatos

- **ID:** `HU-10`
- **Épica / Módulo:** `EPIC-03: Motor de Decisión`
- **Estado:** `Done`
- **Prioridad:** `Alta`
- **Agente Asignado:** `Developer`
- **Requisitos cubiertos:** RF23, RF24, RF28, RF31, RF32, RF33, RF43

## 2. Narrativa de Negocio

- **Como** ejecutante,
- **Quiero** que el sistema evalúe automáticamente la progresión de cada ejercicio al cerrar una sesión, comparando mis datos contra el historial, clasificando el resultado y actualizando el estado de progresión,
- **Para** tener un diagnóstico objetivo de mi rendimiento sin interpretaciones subjetivas.

## 3. Criterios de Aceptación (BDD)

### CA-10.01 — Comparación contra último registro histórico

**Dado que** el ejecutante cierra una sesión (Completada o Incompleta),
**cuando** el sistema ejecuta el análisis de progresión,
**entonces** compara los datos de cada ejercicio registrado en la sesión contra su último registro histórico del mismo ejercicio, independientemente del módulo-versión en que se haya ejecutado anteriormente, evaluando: variación de carga (peso), variación de repeticiones y variación del RIR.

### CA-10.02 — Clasificación como Progresión positiva

**Dado que** el sistema compara un ejercicio contra su historial,
**cuando** detecta que el ejecutante aumentó la carga y/o las repeticiones manteniendo el RIR estable (sin subir significativamente),
**entonces** clasifica la progresión del ejercicio en esta sesión como "Progresión positiva".

### CA-10.03 — Clasificación como Mantenimiento

**Dado que** el sistema compara un ejercicio contra su historial,
**cuando** detecta que el ejecutante mantuvo la misma carga y las mismas repeticiones con RIR estable,
**entonces** clasifica la progresión del ejercicio en esta sesión como "Mantenimiento".

### CA-10.04 — Clasificación como Regresión

**Dado que** el sistema compara un ejercicio contra su historial,
**cuando** detecta que el ejecutante disminuyó la carga o las repeticiones, o que el RIR promedio subió ≥ 1.5 puntos con la misma carga,
**entonces** clasifica la progresión del ejercicio en esta sesión como "Regresión".

### CA-10.05 — Cálculo y almacenamiento del RIR promedio

**Dado que** el ejecutante ha registrado las 4 series de un ejercicio en la sesión,
**cuando** el sistema ejecuta el análisis post-sesión,
**entonces** calcula el RIR promedio aritmético de las 4 series del ejercicio y lo almacena como dato derivado, disponible para reglas de decisión y cálculos de KPIs posteriores.

### CA-10.06 — RIR promedio con series parciales

**Dado que** el ejecutante registró menos de 4 series de un ejercicio (sesión incompleta),
**cuando** el sistema calcula el RIR promedio,
**entonces** lo calcula con las series efectivamente registradas (promedio de las series disponibles) y lo almacena indicando que es un dato parcial.

### CA-10.07 — Ejercicio sin historial previo

**Dado que** un ejercicio registrado en la sesión no tiene registros históricos previos,
**cuando** el sistema ejecuta el análisis de progresión,
**entonces** no emite clasificación de progresión para ese ejercicio (no hay base de comparación) y lo marca con estado "Sin Historial".

### CA-10.08 — Actualización del estado persistente de progresión

**Dado que** el sistema ha clasificado la progresión de un ejercicio en la sesión,
**cuando** completa el análisis,
**entonces** actualiza el estado persistente de progresión del ejercicio según la siguiente lógica:
- Si estaba "Sin Historial" y hay clasificación → pasa a "En Progresión"
- Si tenía progresión positiva → se mantiene "En Progresión"
- Si acumula 3 sesiones sin progresión → pasa a "En Meseta"
- Si está en período de descarga → se mantiene "En Descarga"

### CA-10.09 — Almacenamiento de la clasificación por sesión

**Dado que** el sistema ha clasificado la progresión de cada ejercicio,
**cuando** completa el análisis post-sesión,
**entonces** persiste la clasificación (Progresión positiva, Mantenimiento o Regresión) vinculada a la sesión y al ejercicio, para consulta futura en el historial.

### CA-10.10 — Progresión de peso corporal por repeticiones totales

**Dado que** el ejecutante ha completado las 4 series de un ejercicio de peso corporal,
**cuando** el sistema evalúa la progresión de ese ejercicio,
**entonces** mide la progresión exclusivamente por el total de repeticiones logradas en las 4 series comparado con la sesión anterior del mismo ejercicio, sin aplicar la Regla de Doble Umbral de carga.

> **Origen:** Redistribuido desde HU-08 (ex CA-08.02). Cubre RF31.

### CA-10.11 — Progresión de isométricos por tiempo sostenido

**Dado que** el ejecutante ha completado las 4 series de un ejercicio isométrico,
**cuando** el sistema evalúa la progresión,
**entonces** mide la progresión por los segundos sostenidos dentro del rango prescrito (30-45 segundos), comparando con la sesión anterior del mismo ejercicio.

> **Origen:** Redistribuido desde HU-08 (ex CA-08.06). Cubre RF32.

### CA-10.12 — Marcado de isométrico como "dominado"

**Dado que** el ejecutante completa las 4 series de un ejercicio isométrico y las 4 series alcanzaron ≥ 45 segundos,
**cuando** el sistema evalúa la progresión del ejercicio,
**entonces** marca el ejercicio como "dominado" e indica al ejecutante que el ejercicio ya no ofrece estímulo progresivo suficiente en su forma actual.

> **Origen:** Redistribuido desde HU-08 (ex CA-08.07). Cubre RF33.

## 4. Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite al sistema conocer el contexto físico y de experiencia del ejecutante para calcular métricas y prescripciones personalizadas.

### Reglas de Negocio

1. **Transacción atómica en `closeSession()`:** La evaluación de progresión ocurre dentro de la misma transacción que actualiza el estado de la sesión y avanza la rotación. Si falla cualquier paso, se revierte todo.
2. **Comparación independiente de versión:** La comparación se hace contra el último registro del mismo `exercise_id`, sin filtrar por `module_version_id` (ADR D-06).
3. **RIR promedio NO se almacena en BD:** Se computa in-memory a partir de los sets fetched. Misma resolución que CA-09.04 (tonelaje).
4. **Umbral de "RIR significativamente elevado":** Constante `RIR_SIGNIFICANT_RISE = 1.5`.
5. **Tres tipos de ejercicio con lógica diferenciada:** Estándar (peso + reps + RIR), peso corporal (reps totales), isométrico (segundos sostenidos).
6. **Estado `MASTERED` es terminal para isométricos:** Si las 4 series ≥ 45 segundos, no hay transición de salida.
7. **Ejercicios sin historial reciben `null` como clasificación:** Sin datos previos → no se emite clasificación → `session_exercise.progression_classification` queda `NULL`.
8. **El contador de meseta no tiene cap en 3:** Sigue incrementándose (4, 5, 6...) para soportar la lógica de acción escalonada de HU-15.
9. **La evaluación opera sobre TODOS los ejercicios con sets:** Sustituciones son transparentes — no requieren lógica condicional.
10. **Deload awareness:** Si el ejercicio está en descarga, la clasificación se persiste pero el estado no cambia.

### Interfaz

- **Ninguna — HU-10 es lógica pura de backend.** No tiene pantalla propia.
- **Representación indirecta:** Los datos producidos los consumen E5 (HU-13), F2 (HU-24) y F3 (HU-23).

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Preview de Interfaz

Ver Mapa de Navegación §5: "representación indirecta (output en pantallas existentes)".
