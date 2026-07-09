# Historia de Usuario

**Como** ejecutante,
**quiero** que el sistema calcule y presente un panel de analítica completo con métricas de rendimiento por ejercicio (Tasa de Progresión y Velocidad de Carga), volumen por grupo muscular (Tonelaje Acumulado y Distribución), intensidad y adherencia (RIR Promedio por Módulo e Índice de Adherencia Semanal), tendencias de progresión por grupo muscular a lo largo de múltiples microciclos, y la evolución temporal del tonelaje,
**para** entender objetivamente cómo voy, identificar desequilibrios entre grupos musculares, validar que mi intensidad está en la zona óptima, y tomar decisiones de programación informadas a nivel macro.

---

## Criterios de Aceptación

### Bloque A — KPIs de Rendimiento por Ejercicio (RF44, RF48)

#### CA-15.01 — Cálculo de la Tasa de Progresión por Ejercicio

**Dado que** un ejercicio tiene historial de múltiples sesiones registradas,
**cuando** el sistema calcula la Tasa de Progresión,
**entonces** aplica la fórmula: (Sesiones con progresión positiva / Total de sesiones del ejercicio) × 100, expresando el resultado como porcentaje.

#### CA-15.02 — Período de evaluación configurable con valor por defecto

**Dado que** el sistema calcula la Tasa de Progresión de un ejercicio,
**cuando** determina el rango temporal de evaluación,
**entonces** utiliza por defecto un período de 4 semanas, pero permite configurar períodos alternativos (8/12 semanas) para el análisis.

#### CA-15.03 — Cálculo de la Velocidad de Progresión de Carga

**Dado que** un ejercicio tiene historial con al menos 2 sesiones registradas donde se utilizó carga (Peso > 0),
**cuando** el sistema calcula la Velocidad de Progresión de Carga,
**entonces** aplica la fórmula: (Peso actual − Peso inicial) / Número de sesiones intermedias, expresando el resultado en Kg por sesión.

#### CA-15.04 — Velocidad de Progresión para ejercicios sin incremento

**Dado que** un ejercicio tiene historial pero no ha incrementado carga (Peso actual = Peso inicial),
**cuando** el sistema calcula la Velocidad de Progresión de Carga,
**entonces** el resultado es 0 Kg/sesión, confirmando numéricamente una meseta activa en carga.

#### CA-15.05 — Ejercicios de peso corporal excluidos de Velocidad de Carga

**Dado que** un ejercicio es de peso corporal (Peso = 0 Kg),
**cuando** el sistema calcula KPIs de rendimiento,
**entonces** calcula la Tasa de Progresión normalmente pero no calcula la Velocidad de Progresión de Carga (no aplica ya que no hay variación de peso).

#### CA-15.06 — Presentación de KPIs de rendimiento al ejecutante

**Dado que** el sistema ha calculado los KPIs de rendimiento para un ejercicio,
**cuando** el ejecutante consulta las métricas del ejercicio,
**entonces** muestra la Tasa de Progresión (%) y la Velocidad de Progresión de Carga (Kg/sesión) de forma clara con el período de evaluación utilizado.

### Bloque B — KPIs de Volumen por Grupo Muscular (RF45, RF49)

#### CA-15.07 — Cálculo del Tonelaje Acumulado por Grupo Muscular

**Dado que** un microciclo incluye sesiones con ejercicios registrados,
**cuando** el sistema calcula el Tonelaje Acumulado por Grupo Muscular,
**entonces** suma el producto de Peso × Repeticiones de todas las series de todos los ejercicios que trabajan cada grupo muscular dentro del microciclo, expresando el resultado en kilogramos por grupo muscular.

#### CA-15.08 — Agrupación por zona muscular del Diccionario

**Dado que** el sistema calcula el tonelaje por grupo muscular,
**cuando** asocia cada ejercicio a su grupo muscular,
**entonces** utiliza la clasificación de zona muscular definida en el Diccionario de Ejercicios como fuente de verdad, respetando que un ejercicio puede trabajar múltiples zonas musculares (ej: Sentadilla Búlgara = Cuádriceps + Glúteos).

#### CA-15.09 — Ejercicios con múltiples zonas musculares

**Dado que** un ejercicio trabaja múltiples zonas musculares según el Diccionario,
**cuando** el sistema calcula el tonelaje acumulado,
**entonces** contabiliza el tonelaje de ese ejercicio en cada una de las zonas musculares asociadas (100% en cada grupo, sin dividir).

#### CA-15.10 — Cálculo de la Distribución de Volumen por Zona Muscular

**Dado que** un microciclo tiene sesiones registradas para un módulo,
**cuando** el sistema calcula la Distribución de Volumen por Zona Muscular,
**entonces** calcula el porcentaje de series totales de cada zona muscular respecto al total de series del módulo, expresando el resultado como porcentaje por zona.

#### CA-15.11 — Cálculo por microciclo completado

**Dado que** el sistema calcula KPIs de volumen,
**cuando** determina el alcance temporal del cálculo,
**entonces** lo calcula por microciclo completado, asociando los resultados al número de microciclo correspondiente para permitir comparación entre microciclos.

#### CA-15.12 — Presentación de KPIs de volumen al ejecutante

**Dado que** el sistema ha calculado los KPIs de volumen,
**cuando** el ejecutante consulta las métricas de volumen,
**entonces** muestra el Tonelaje Acumulado desglosado por grupo muscular y la Distribución de Volumen (%) por zona muscular, ambos referenciados al microciclo consultado.

### Bloque C — KPIs de Intensidad y Adherencia (RF46, RF47)

#### CA-15.13 — Cálculo del RIR Promedio por Módulo

**Dado que** un módulo tiene sesiones registradas en un período dado,
**cuando** el sistema calcula el RIR Promedio por Módulo,
**entonces** promedia aritméticamente todos los valores de RIR registrados en todas las series de todas las sesiones del módulo en el período, expresando el resultado con un decimal.

#### CA-15.14 — Período de evaluación del RIR Promedio

**Dado que** el sistema calcula el RIR Promedio por Módulo,
**cuando** determina el rango temporal,
**entonces** evalúa por defecto las últimas 2 ejecuciones del mismo módulo, permitiendo al ejecutante consultar períodos más amplios (4/6 sesiones).

#### CA-15.15 — Interpretación del RIR Promedio

**Dado que** el sistema muestra el RIR Promedio de un módulo al ejecutante,
**cuando** presenta el resultado,
**entonces** incluye una referencia de interpretación: RIR 2-3 = zona óptima, RIR < 1.5 = demasiado cerca del fallo (riesgo de fatiga del SNC), RIR > 3.5 = estímulo posiblemente insuficiente.

#### CA-15.16 — Cálculo del Índice de Adherencia Semanal

**Dado que** una semana natural ha transcurrido,
**cuando** el sistema calcula el Índice de Adherencia semanal,
**entonces** aplica la fórmula: (Sesiones completadas en la semana / Sesiones planificadas) × 100, donde "sesiones planificadas" es el objetivo de frecuencia del ejecutante (entre 4 y 6), expresando el resultado como porcentaje.

#### CA-15.17 — Objetivo de frecuencia del ejecutante

**Dado que** el sistema calcula el Índice de Adherencia,
**cuando** necesita el denominador (sesiones planificadas),
**entonces** utiliza el objetivo de frecuencia definido por el ejecutante en su perfil, aceptando valores entre 4 y 6 sesiones semanales.

#### CA-15.18 — Sesiones contabilizadas para adherencia

**Dado que** el sistema calcula las sesiones completadas en la semana,
**cuando** contabiliza sesiones,
**entonces** incluye sesiones cerradas tanto como Completadas como Incompletas, ya que ambos tipos representan asistencia al gimnasio y ambos avanzan la rotación.

#### CA-15.19 — Presentación de KPIs de intensidad y adherencia al ejecutante

**Dado que** el sistema ha calculado los KPIs de intensidad y adherencia,
**cuando** el ejecutante consulta las métricas,
**entonces** muestra el RIR Promedio por Módulo (para cada módulo A, B, C) y el Índice de Adherencia semanal con el período evaluado y la referencia de interpretación.

### Bloque D — Tendencia de Progresión por Grupo Muscular (RF42)

#### CA-15.20 — Evaluación de tendencia por grupo muscular

**Dado que** el ejecutante tiene al menos 4 microciclos completados,
**cuando** el sistema evalúa la tendencia de progresión de un grupo muscular,
**entonces** analiza la trayectoria del tonelaje acumulado y la tasa de progresión de los ejercicios asociados al grupo muscular a lo largo de los últimos 4 a 6 microciclos.

#### CA-15.21 — Clasificación de la tendencia

**Dado que** el sistema analiza la tendencia de un grupo muscular,
**cuando** presenta los resultados al ejecutante,
**entonces** clasifica la tendencia como: "Ascendente" (tonelaje y/o tasa de progresión crecientes), "Estable" (valores sin cambio significativo) o "En declive" (tonelaje y/o tasa de progresión decrecientes).

#### CA-15.22 — Rango de microciclos analizado

**Dado que** el sistema evalúa la tendencia,
**cuando** determina el rango temporal,
**entonces** analiza por defecto los últimos 4 a 6 microciclos completados, priorizando los microciclos más recientes para reflejar el estado actual del ejecutante.

#### CA-15.23 — Datos insuficientes para tendencia

**Dado que** el ejecutante tiene menos de 4 microciclos completados,
**cuando** intenta consultar la tendencia de un grupo muscular,
**entonces** el sistema indica que no hay datos suficientes para evaluar una tendencia significativa y muestra el número de microciclos faltantes para habilitar el análisis.

#### CA-15.24 — Presentación por grupo muscular

**Dado que** el sistema ha evaluado la tendencia de todos los grupos musculares con datos suficientes,
**cuando** el ejecutante consulta las tendencias,
**entonces** muestra la tendencia de cada grupo muscular de forma desglosada, permitiendo comparar el comportamiento entre grupos musculares.

### Bloque E — Evolución de Tonelaje por Grupo Muscular (RF52)

#### CA-15.25 — Visualización del tonelaje por grupo muscular a lo largo del tiempo

**Dado que** el ejecutante tiene múltiples microciclos completados,
**cuando** consulta la evolución de tonelaje por grupo muscular,
**entonces** el sistema muestra el tonelaje acumulado de cada grupo muscular en cada microciclo, permitiendo visualizar la trayectoria temporal.

#### CA-15.26 — Identificación de tendencias de tonelaje

**Dado que** el sistema presenta la evolución del tonelaje por grupo muscular,
**cuando** el ejecutante analiza los datos,
**entonces** la presentación permite identificar tendencias: ascendente, estable o en caída para cada grupo muscular.

#### CA-15.27 — Desglose por grupo muscular

**Dado que** el ejecutante consulta la evolución de tonelaje,
**cuando** visualiza los resultados,
**entonces** el sistema presenta los datos desglosados por cada grupo muscular del sistema (Pecho, Espalda, Abdomen, Hombro, Tríceps, Bíceps, Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos), permitiendo análisis individual.

#### CA-15.28 — Datos insuficientes para evolución de tonelaje

**Dado que** el ejecutante tiene menos de 2 microciclos completados,
**cuando** consulta la evolución de tonelaje,
**entonces** el sistema indica que se necesitan al menos 2 microciclos para mostrar una evolución comparativa.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite al ejecutante entender objetivamente su progreso, identificar desequilibrios musculares, validar intensidad y tomar decisiones de programación informadas.

### Reglas de Negocio

1. **Lectura pura — HU-15 no modifica ningún dato:** Solo consulta datos existentes para producir KPIs. No ejecuta lógica al cierre de sesión, no produce efectos colaterales.
2. **Exclusión de sesiones de deload en métricas de rendimiento:** Tasa de Progresión, Velocidad de Carga y RIR Promedio excluyen sesiones de deload (`deload_id IS NULL`). Tonelaje Acumulado y Adherencia SÍ incluyen deload.
3. **Agrupación por microciclo derivada en Kotlin:** No hay columna `microcycle_number` en la tabla `session`. La agrupación se deriva del orden de cierre: sesiones ordenadas por (date ASC, id ASC), cada grupo de 6 consecutivas forma un microciclo.
4. **Cálculo on-demand de KPIs:** Se calculan al abrir G1, no al cierre de sesión. Evita latencia en `closeSession()` y no persiste valores derivados que se invalidan con cada nueva sesión.
5. **Tendencia con regresión lineal simple:** `slope = Σ(xi - x̄)(yi - ȳ) / Σ(xi - x̄)²`. Umbral ±5% del valor medio para evitar clasificar ruido como tendencia. Combinación más conservadora: DECLINING > STABLE > ASCENDING.
6. **RIR Promedio usa semanas ISO 8601 (lunes a domingo) y N últimas sesiones del módulo.**
7. **Tonelaje multi-zona:** Un ejercicio que trabaja múltiples zonas se contabiliza al 100% en cada grupo muscular asociado.
8. **Distribución de Volumen por zona muscular (15), no por grupo (12).** Permite detectar desequilibrios intra-grupo.
9. **12 grupos musculares canónicos:** Pecho, Espalda, Abdomen, Hombro, Tríceps, Bíceps, Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos. Padding a 0.0 para grupos sin datos.

### Interfaz

- **Pantalla G1 — `MetricsScreen`:** Ruta `metrics`. Reemplaza `PlaceholderScreen` en TensionNavHost. 4 secciones: Adherencia, RIR Promedio, Tasa de Progresión, Velocidad de Carga + 2 quick links → G2, G3.
- **Pantalla G2 — `VolumeScreen`:** Ruta `muscle-volume`. Selector microciclo stepper ◀▶ + barras tonelaje + distribución % + gráfico multilínea de evolución temporal.
- **Pantalla G3 — `TrendScreen`:** Ruta `progression-trend`. Lista 12 grupos con clasificación ASCENDING/STABLE/DECLINING.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

---

## §4 Dependencias Técnicas e Integración

### Datos consumidos (lectura pura — HU-15 no modifica ningún dato)

| Tabla | Campo clave | Uso |
|---|---|---|
| `session` | `status`, `date`, `deload_id`, `module_version_id` | Filtro (COMPLETED/INCOMPLETE), exclusión deload, agrupación por módulo |
| `session_exercise` | `exercise_id`, `progression_classification`, `session_id` | Tasa de Progresión, Distribución |
| `exercise_set` | `weight_kg`, `reps`, `rir` | Tonelaje (Σ peso×reps), RIR promedio |
| `exercise` | `name`, `is_bodyweight`, `is_isometric`, `module_code` | Clasificación ejercicio, filtro bodyweight |
| `exercise_muscle_zone` + `muscle_zone` | `exercise_id`, `muscle_zone_id`, `muscle_group`, `name` | JOIN para tonelaje y distribución por grupo/zona |
| `profile` | `weekly_frequency` | Denominador del Índice de Adherencia |
| `rotation_state` | `microcycle_count`, `microcycle_position` | Agrupación temporal por microciclo |
| `module_version` | `module_code` | JOIN para filtrar sesiones por módulo |

### Pantallas producidas

| Ruta | Pantalla | Estado actual |
|---|---|---|
| `metrics` | G1 — Panel de Métricas | Reemplaza `PlaceholderScreen` en TensionNavHost L244-245 |
| `muscle-volume` | G2 — Volumen por Grupo Muscular | Nueva ruta |
| `progression-trend` | G3 — Tendencia de Progresión | Nueva ruta |

### Exclusión de sesiones de deload

| Métrica | Incluye deload | Justificación |
|---|---|---|
| Tasa de Progresión | No (`s.deload_id IS NULL`) | Clasifica con cargas 60% → posiblemente REGRESSION → penaliza tasa |
| Velocidad de Carga | No (`s.deload_id IS NULL`) | Cargas 60% no reflejan progresión real |
| RIR Promedio | No (`s.deload_id IS NULL`) | RIR objetivo 4-5 inflaría el promedio artificialmente |
| Tonelaje Acumulado | Sí | El dip del 60% durante deload es información valiosa para G2 |
| Adherencia semanal | Sí | Una sesión de deload es asistencia real al gimnasio |
| Tendencia de tonelaje | Sí (tonnage) / No (tasa) | Ver `GetMuscleGroupTrendUseCase` — lógica `mapNotNull` |
