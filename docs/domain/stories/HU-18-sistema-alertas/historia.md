# Historia de Usuario

**Como** ejecutante,
**Quiero** que el sistema emita alertas proactivas con dos niveles de severidad (alerta y crisis) cuando detecte condiciones que requieran mi atención — tasa de progresión baja, RIR fuera de rango por módulo, adherencia semanal insuficiente, caída de tonelaje por grupo muscular o inactividad prolongada por módulo —
**Para** recibir señales tempranas y objetivas de problemas antes de que comprometan mis adaptaciones, sin que estas señales bloqueen mi autonomía de entrenamiento.

## Descripción

Esta historia define el **Sistema de Alertas** de Tension. El sistema emite alertas proactivas al detectar condiciones que requieren atención del ejecutante.

HU-18 tiene dos caras:
- **Write-side:** extiende `SessionRepositoryImpl.evaluateProgression()` con 5 bloques de evaluación nuevos (Steps 7-11) que crean/resuelven alertas al cierre de cada sesión, reutilizando las Rules de HU-15.
- **Read-side:** crea 2 pantallas nuevas (H1 Centro de Alertas, H2 Detalle de Alerta) de solo lectura con recálculo dinámico de datos.

### Historias originales consolidadas

HU-18 consolida 5 historias originales en una sola:

- **HU-26 original** — Alertas de tasa de progresión baja (RF53)
- **HU-27 original** — Alertas de RIR por módulo (RF54, RF55)
- **HU-28 original** — Alertas de adherencia baja (RF56)
- **HU-29 original** — Alertas de caída de tonelaje por grupo muscular (RF57)
- **HU-30 original** — Alertas de inactividad por módulo (RF58)

**Patrón arquitectónico:** Motor de Alertas Write-Time en Pipeline Existente + MVVM Read-Only para 2 pantallas (H1, H2) con `AlertRepository` dedicado.

---

## Criterios de Aceptación

### Bloque A — Alertas de Tasa de Progresión Baja (RF53)

#### CA-18.01 — Alerta por tasa de progresión < 40%

**Dado que** el sistema calcula la Tasa de Progresión de un ejercicio en un período de 4 semanas,
**cuando** el resultado es menor al 40%,
**entonces** emite una alerta informativa indicando el nombre del ejercicio, su tasa de progresión actual y que está por debajo del umbral de alerta.

#### CA-18.02 — Alerta de crisis por tasa de progresión < 20%

**Dado que** el sistema calcula la Tasa de Progresión de un ejercicio en un período de 4 semanas,
**cuando** el resultado es menor al 20%,
**entonces** emite una alerta de crisis con mayor urgencia visual, indicando el nombre del ejercicio, su tasa de progresión actual y que está en estado crítico de estancamiento.

#### CA-18.03 — Diferenciación visual entre alerta y crisis de progresión

**Dado que** el sistema emite una alerta de tasa de progresión,
**cuando** el ejecutante visualiza la alerta,
**entonces** la alerta de nivel normal (< 40%) y la de crisis (< 20%) son visualmente distinguibles entre sí mediante colores e iconografía diferenciada, sin depender únicamente del texto.

#### CA-18.04 — Alertas de progresión informativas, no bloqueantes

**Dado que** el sistema emite una alerta de tasa de progresión baja,
**cuando** el ejecutante interactúa con el sistema,
**entonces** la alerta es informativa y no impide iniciar sesiones, registrar series ni cerrar sesiones; el ejecutante mantiene autonomía total.

#### CA-18.05 — Evaluación periódica de progresión

**Dado que** el sistema evalúa la tasa de progresión,
**cuando** se completa un período de 4 semanas o un microciclo,
**entonces** recalcula la tasa y emite o retira alertas según corresponda al estado actual de cada ejercicio.

---

### Bloque B — Alertas de RIR por Módulo (RF54, RF55)

#### CA-18.06 — Alerta por RIR Promedio < 1.5 (intensidad excesiva)

**Dado que** el sistema calcula el RIR Promedio de un módulo,
**cuando** el resultado es < 1.5 de forma sostenida durante 2 o más sesiones del mismo módulo,
**entonces** emite una alerta al ejecutante indicando: el módulo afectado, el RIR Promedio actual, que está entrenando demasiado cerca del fallo técnico de forma sostenida, y recomienda prescribir una descarga para permitir recuperación del SNC.

#### CA-18.07 — Alerta por RIR Promedio > 3.5 (intensidad insuficiente)

**Dado que** el sistema calcula el RIR Promedio de un módulo,
**cuando** el resultado es > 3.5 de forma sostenida durante 2 o más sesiones del mismo módulo,
**entonces** emite una alerta al ejecutante indicando: el módulo afectado, el RIR Promedio actual, que el estímulo puede ser insuficiente para generar adaptación, y recomienda incrementar la carga de los ejercicios del módulo.

#### CA-18.08 — Condición de "sostenido" durante 2+ sesiones

**Dado que** el sistema evalúa si el RIR Promedio está fuera de rango de forma sostenida,
**cuando** determina si emitir la alerta,
**entonces** verifica que la condición (< 1.5 o > 3.5) se cumple en las últimas 2 o más sesiones consecutivas del mismo módulo; una sola sesión fuera de rango no dispara la alerta.

#### CA-18.09 — Diferenciación visual entre alertas de RIR

**Dado que** el sistema emite alertas de RIR por módulo,
**cuando** el ejecutante visualiza las alertas,
**entonces** la alerta por RIR bajo (< 1.5, riesgo de fatiga) y la alerta por RIR alto (> 3.5, estímulo insuficiente) son visualmente distinguibles entre sí mediante colores e iconografía diferenciada.

#### CA-18.10 — Alertas de RIR informativas, no bloqueantes

**Dado que** el sistema emite una alerta de RIR por módulo,
**cuando** el ejecutante interactúa con el sistema,
**entonces** la alerta es informativa; no impide entrenar ni fuerza una descarga automática. El ejecutante decide si seguir la recomendación.

#### CA-18.11 — Retiro automático de alerta de RIR

**Dado que** el sistema ha emitido una alerta de RIR por módulo,
**cuando** las sesiones posteriores del módulo retornan el RIR Promedio a la zona óptima (1.5-3.5),
**entonces** el sistema retira la alerta activa para ese módulo.

---

### Bloque C — Alertas de Adherencia Baja (RF56)

#### CA-18.12 — Alerta informativa por adherencia < 60% en una semana

**Dado que** el sistema calcula el Índice de Adherencia al finalizar una semana natural,
**cuando** el resultado es < 60%,
**entonces** emite una alerta informativa indicando la adherencia de la semana, cuántas sesiones se completaron respecto al objetivo, y que la baja frecuencia puede afectar la resolución temporal de las señales del sistema.

#### CA-18.13 — Alerta de crisis por adherencia < 60% durante 2+ semanas consecutivas

**Dado que** el sistema calcula el Índice de Adherencia semanal,
**cuando** el resultado es < 60% durante 2 o más semanas consecutivas,
**entonces** emite una alerta de crisis con mayor urgencia visual, indicando la racha de semanas con baja adherencia y advirtiendo que las comparaciones entre sesiones pierden validez por el excesivo tiempo entre ellas.

#### CA-18.14 — Diferenciación visual entre alerta y crisis de adherencia

**Dado que** el sistema emite alertas de adherencia,
**cuando** el ejecutante visualiza las alertas,
**entonces** la alerta informativa (1 semana < 60%) y la de crisis (2+ semanas < 60%) son visualmente distinguibles mediante colores e iconografía diferenciada.

#### CA-18.15 — Alertas de adherencia informativas, no bloqueantes

**Dado que** el sistema emite una alerta de adherencia baja,
**cuando** el ejecutante interactúa con el sistema,
**entonces** la alerta es informativa; no impide entrenar, registrar sesiones ni acceder a ninguna funcionalidad. El ejecutante es autónomo.

#### CA-18.16 — Retiro de alerta de crisis de adherencia

**Dado que** el sistema ha emitido una alerta de crisis por adherencia,
**cuando** el ejecutante completa una semana con adherencia ≥ 60%,
**entonces** el sistema retira la alerta de crisis, manteniendo el registro histórico de las semanas con baja adherencia.

---

### Bloque D — Alertas de Caída de Tonelaje por Grupo Muscular (RF57)

#### CA-18.17 — Alerta por caída de tonelaje > 10%

**Dado que** el sistema calcula el Tonelaje por Grupo Muscular al completar un microciclo,
**cuando** el tonelaje de un grupo muscular cae más del 10% respecto al microciclo anterior,
**entonces** emite una alerta indicando el grupo muscular afectado, el porcentaje de caída y el tonelaje comparado de ambos microciclos.

#### CA-18.18 — Alerta de crisis por caída de tonelaje > 20%

**Dado que** el sistema calcula el Tonelaje por Grupo Muscular al completar un microciclo,
**cuando** el tonelaje de un grupo muscular cae más del 20% respecto al microciclo anterior,
**entonces** emite una alerta de crisis con mayor urgencia visual, indicando que la pérdida de volumen es severa y puede comprometer las adaptaciones del grupo muscular.

#### CA-18.19 — Verificación de descarga planificada

**Dado que** el sistema detecta una caída de tonelaje en un grupo muscular,
**cuando** analiza la causa de la caída,
**entonces** verifica si el microciclo evaluado corresponde a un período de descarga activa (HU-14); si es descarga planificada, la alerta se contextualiza como "Caída esperada por descarga" y no se clasifica como crisis.

#### CA-18.20 — Diferenciación entre descarga y regresión

**Dado que** el sistema emite una alerta de caída de tonelaje,
**cuando** la caída corresponde a una descarga planificada,
**entonces** la alerta indica claramente "Descarga planificada — caída de tonelaje esperada y controlada", diferenciándola de una regresión no intencional.

#### CA-18.21 — Diferenciación visual entre alerta y crisis de tonelaje

**Dado que** el sistema emite alertas de tonelaje,
**cuando** el ejecutante visualiza las alertas,
**entonces** la alerta de nivel normal (> 10%) y la de crisis (> 20%) son visualmente distinguibles mediante colores e iconografía diferenciada.

#### CA-18.22 — Alertas de tonelaje informativas, no bloqueantes

**Dado que** el sistema emite una alerta de caída de tonelaje,
**cuando** el ejecutante interactúa con el sistema,
**entonces** la alerta es informativa y no impide ninguna operación del ejecutante.

---

### Bloque E — Alertas de Inactividad por Módulo (RF58)

#### CA-18.23 — Alerta por inactividad > 10 días naturales

**Dado que** el sistema monitorea la última fecha de ejecución de cada módulo,
**cuando** transcurren más de 10 días naturales sin que se ejecute un módulo específico (A, B o C),
**entonces** emite una alerta indicando el módulo inactivo, la cantidad de días transcurridos desde su última ejecución y los grupos musculares asociados que pueden verse afectados.

#### CA-18.24 — Alerta de crisis por inactividad > 14 días naturales

**Dado que** el sistema monitorea la última fecha de ejecución de cada módulo,
**cuando** transcurren más de 14 días naturales sin que se ejecute un módulo específico,
**entonces** emite una alerta de crisis con mayor urgencia visual, informando al ejecutante que el grupo muscular asociado puede estar perdiendo adaptaciones musculares y que se recomienda priorizar ese módulo.

#### CA-18.25 — Referencia a los grupos musculares afectados

**Dado que** el sistema emite una alerta de inactividad por módulo,
**cuando** presenta la alerta al ejecutante,
**entonces** detalla los grupos musculares asociados al módulo inactivo: Módulo A = Espalda, Bíceps, Abdomen; Módulo B = Pecho, Hombro, Tríceps; Módulo C = Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos.

#### CA-18.26 — Conteo basado en días naturales

**Dado que** el sistema evalúa la inactividad de un módulo,
**cuando** cuenta los días transcurridos,
**entonces** utiliza días naturales del calendario (no sesiones ni microciclos), ya que la pérdida de adaptaciones musculares se correlaciona con el tiempo absoluto sin estímulo.

#### CA-18.27 — Diferenciación visual entre alerta y crisis de inactividad

**Dado que** el sistema emite alertas de inactividad,
**cuando** el ejecutante visualiza las alertas,
**entonces** la alerta de nivel normal (> 10 días) y la de crisis (> 14 días) son visualmente distinguibles mediante colores e iconografía diferenciada.

#### CA-18.28 — Alertas de inactividad informativas, no bloqueantes

**Dado que** el sistema emite una alerta de inactividad por módulo,
**cuando** el ejecutante interactúa con el sistema,
**entonces** la alerta es informativa; no altera la rotación cíclica ni fuerza un cambio de módulo. El ejecutante sigue la rotación normal.

#### CA-18.29 — Retiro automático de alerta de inactividad

**Dado que** el sistema ha emitido una alerta de inactividad para un módulo,
**cuando** el ejecutante completa una sesión de ese módulo,
**entonces** el sistema retira la alerta de inactividad y reinicia el conteo de días para ese módulo.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite al sistema detectar condiciones que requieren atención del ejecutante y emitirlas como señales tempranas, sin bloquear su autonomía.

### Reglas de Negocio

1. **Dos niveles de severidad:** Cada tipo de alerta tiene umbral de "alerta" (nivel medio) y "crisis" (nivel alto). Ejemplo: progresión < 40% = alerta, < 20% = crisis.
2. **Alertas informativas, no bloqueantes:** Ninguna alerta impide al ejecutante entrenar, registrar series, iniciar o cerrar sesiones. El ejecutante mantiene autonomía total.
3. **Deduplicación de alertas:** El pipeline verifica `existsActiveByExercise/Module/MuscleGroup()` antes de insertar. Si la condición cambia de nivel, la alerta existente se resuelve y se crea una nueva (resolve + insert, no update).
4. **Escalamiento de nivel:** Al subir: MEDIUM_ALERT → resolver + crear CRISIS. Al bajar: CRISIS → resolver + crear MEDIUM_ALERT. Al desaparecer: resolver todas las alertas activas del tipo para la entidad.
5. **Condición sostenida para RIR:** La alerta de RIR requiere que la condición se cumpla en 2+ sesiones consecutivas del mismo módulo.
6. **Adherencia semanal:** Se calcula al finalizar una semana natural. 1 semana < 60% = alerta, 2+ semanas < 60% = crisis.
7. **Verificación de descarga planificada:** Si la caída de tonelaje corresponde a un microciclo de descarga activa (HU-14), la alerta se contextualiza como "Caída esperada por descarga" y no se clasifica como crisis.
8. **Días naturales para inactividad:** El conteo de inactividad usa días naturales del calendario, no sesiones ni microciclos.
9. **Grupos musculares por módulo (datos fijos):** A = Espalda, Bíceps, Abdomen; B = Pecho, Hombro, Tríceps; C = Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos.
10. **Mapeo tipo de alerta → nivel visual (H1):** 3 niveles: 🔴 Crisis (rojo/Error Container), 🟠 Alerta alta (naranja), 🟡 Alerta media (amarillo). Mapeo: `CRISIS` → 🔴, `HIGH_ALERT` → 🟠, `MEDIUM_ALERT` → 🟡.

### Interfaz

- **H1 — Centro de Alertas (`AlertCenterScreen`):** Pantalla de solo lectura con 2 secciones: "Crisis" (solo 🔴) y "Alertas" (🟠 + 🟡). Muestra lista de alertas activas con diferenciación visual por severidad. Bottom Navigation con "Inicio" activo.
- **H2 — Detalle de Alerta (`AlertDetailScreen`):** Pantalla de solo lectura con recálculo dinámico de datos. Muestra información detallada de una alerta específica, análisis causal según tipo, y links de acción condicionales. Bottom Navigation con "Inicio" activo.
- **Bottom Navigation:** `showBottomBar` en `TensionNavHost` ya mostrará el Bottom Nav (alert-center/alert-detail no están en el blocklist). Solo falta agregar `childRoutePrefixes = setOf("alert-center", "alert-detail")` al tab HOME en `BottomNavigationBar.kt`.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Notas Técnicas

**Nota 1 — Evaluación de tonelaje reutiliza lógica de `GetMicrocycleMapUseCase`.**
`closedSessions.chunked(6)` → microciclos. Tonelaje con `TonnageRule.calculateForMuscleGroup()`. Verificación de descarga: `currentMicrocycle.any { it.deloadId != null }` → nivel MEDIUM_ALERT (no CRISIS) y mensaje contextualizado.

**Nota 2 — `getSessionIdsByModuleInRange()` ya filtra sesiones de descarga.**
El query existente incluye `AND s.deload_id IS NULL` — garantiza que CA-18.08 evalúe solo sesiones normales.

**Nota 3 — Corrección crítica: Steps 9 y 11 se ejecutan ANTES del deload guard.**
CA-18.12 evalúa adherencia — una sesión de descarga SÍ cuenta para la frecuencia semanal. CA-18.29 dice que "completar una sesión de ese módulo" resuelve la inactividad — incluyendo sesiones de descarga. Steps 9 y 11 se mueven ANTES de `if (isDeloadSession) return`. Steps 7, 8, 10 permanecen protegidos por el guard (correcto).
