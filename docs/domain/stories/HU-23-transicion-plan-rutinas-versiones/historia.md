# Historia #23: Transición a plan, rutinas y versiones 100% definidas por el usuario

---

## §1 Metadatos

| Campo              | Valor                                                                          |
|--------------------|--------------------------------------------------------------------------------|
| **ID**             | HU-23                                                                          |
| **Título**         | Transición a plan, rutinas y versiones 100% definidas por el usuario           |
| **Épica**          | Personalización del Plan                                                       |
| **Requisitos**     | RF04, RF05, RF07, RF08, RF09, RF10, RF11, RF12, RF14, RF16, RF21, RF26, RF30, RF36, RF37, RF39, RF41, RF46, RF49, RF58, RF60, RF62, RF63, RF64 (modificación) · RNF09, RNF14, RNF19, RNF28, RNF29, RNF31 |
| **Prioridad**      | Alta                                                                            |
| **Estado**         | Pendiente                                                                       |
| **Arquitecto**     | Analizada — 2026-05-06                                                         |
| **Developer**      | Refinada — 2026-05-06                                                          |
| **Implementación** | Pendiente                                                                       |
| **Migración DB**   | MIGRATION_8_9 — DB v8 → v9 (7 tablas recreadas, 4 nuevas, 2 eliminadas)       |

---

## §2 Narrativa de Negocio

### Descripción

Como ejecutante, necesito que el sistema me permita crear, editar y eliminar cualquier número de rutinas y versiones dentro de mi plan de entrenamiento, asignando ejercicios libremente a cada combinación rutina/versión, para diseñar un plan completamente personalizado que se adapte a mis objetivos, preferencias y disponibilidad — sin estar limitado a una estructura fija de módulos o versiones predefinidas.

El concepto de "módulo fijo" (A, B, C) desaparece y es reemplazado por "rutina", una entidad que el usuario nombra, ordena y configura libremente. Cada rutina puede tener cualquier cantidad de versiones. La lógica de rotación cíclica, microciclo, progresión, fatiga, descarga, KPIs y alertas se adapta automáticamente a la estructura definida por el usuario.

### Contexto y justificación

**Delta entre modelo actual (v8) y modelo objetivo (v9):**

| Concepto actual (v8) | Concepto nuevo (v9) | Tipo de cambio |
|---|---|---|
| `module` (tabla, PK=code String) | `routine` (tabla, PK=id Long autoincrement) | Nueva tabla, tabla anterior eliminada |
| `module_version` (FK→module.code) | `routine_version` (FK→routine.id) | Nueva tabla, tabla anterior eliminada |
| `exercise.module_code` FK→module | *(columna eliminada)* | Ejercicios agnósticos a la rutina (CA-23.15) |
| `module.load_increment_kg` | Calculado por zona muscular: tren sup=2.5, inf=5.0 (CA-23.23) | Lógica migra de tabla a regla de negocio |
| `rotation_state.current_version_module_a/b/c` | `routine_current_version` (tabla dinámica, 1 fila por rutina) | Nueva tabla, columnas fijas eliminadas |
| `deload.frozen_version_module_a/b/c` | `deload_frozen_version` (tabla dinámica, 1 fila por rutina por deload) | Nueva tabla, columnas fijas eliminadas |
| `alert.module_code` FK→module | `alert.routine_id` FK→routine | FK migra de String a Long |
| `session.module_version_id` FK→module_version | `session.routine_version_id` FK→routine_version | FK rename (IDs preservados) |
| `plan_assignment.module_version_id` | `plan_assignment.routine_version_id` | FK rename (IDs preservados) |
| `RotationResolver` (A/B/C hardcoded, 6 posiciones) | `RotationResolver` (N rutinas, N posiciones) | Rewrite completo |
| `ModuleSeeder`, `PlanSeeder` | *(no se invocan en v9+)* | Seeders obsoletos para nuevas instalaciones |

### Principio rector

HU-23 materializa la transición de un sistema con estructura fija (3 módulos hardcoded) a uno completamente personalizable por el usuario. Cada decisión de diseño prioriza la **compatibilidad hacia atrás** (migración sin pérdida de datos, CA-23.35/36/37) y la **escalabilidad** (N rutinas con M versiones heterogéneas). La lógica de negocio que antes dependía de constantes ("A", "B", "C") ahora opera sobre datos dinámicos consultados en runtime.

---

## §3 Criterios de Aceptación

### Gestión de Rutinas

- **CA-23.01:** El sistema permite al usuario crear una nueva rutina proporcionando un nombre descriptivo (texto libre, máximo 50 caracteres).
- **CA-23.02:** El sistema permite al usuario editar el nombre de una rutina existente en cualquier momento.
- **CA-23.03:** El sistema permite al usuario eliminar una rutina, siempre que no sea la única rutina del plan y no tenga una sesión activa en curso. Al eliminar, se solicita confirmación explícita.
- **CA-23.04:** El sistema permite al usuario reordenar las rutinas dentro del plan, definiendo así el orden de rotación cíclica.
- **CA-23.05:** No existe límite máximo de rutinas que el usuario puede crear.
- **CA-23.06:** El plan debe tener al menos una rutina con al menos una versión y al menos un ejercicio asignado para poder iniciar sesiones.

### Gestión de Versiones

- **CA-23.07:** El sistema permite al usuario crear una o más versiones dentro de cada rutina.
- **CA-23.08:** El sistema permite al usuario eliminar una versión de una rutina, siempre que no sea la única versión de esa rutina. Al eliminar, se solicita confirmación explícita.
- **CA-23.09:** Cada versión de una rutina puede tener un conjunto diferente de ejercicios asignados.
- **CA-23.10:** La rotación de versiones es independiente por rutina: cada rutina mantiene su propio índice de versión activa.
- **CA-23.11:** Al completar un microciclo (una pasada por todas las rutinas), la versión de cada rutina avanza a la siguiente en orden secuencial, volviendo a la primera al agotar todas.

### Asignación de Ejercicios

- **CA-23.12:** El sistema permite al usuario asignar cualquier ejercicio del Diccionario (precargado o creado por el usuario) a cualquier versión de cualquier rutina, especificando series (por defecto 4) y rango de repeticiones (por defecto 8-12).
- **CA-23.13:** El sistema permite al usuario desasignar (remover) un ejercicio de una versión de una rutina sin eliminar el ejercicio del Diccionario ni afectar su historial.
- **CA-23.14:** Un mismo ejercicio puede estar asignado a múltiples rutinas y/o versiones simultáneamente.
- **CA-23.15:** Los ejercicios son agnósticos de la rutina: no pertenecen inherentemente a ninguna rutina, sino que se asignan libremente.

### Rotación Cíclica y Microciclo

- **CA-23.16:** La rotación cíclica opera sobre las rutinas creadas por el usuario, en el orden definido por él.
- **CA-23.17:** Un microciclo se define como una pasada completa por todas las rutinas del plan, independientemente de cuántas sean.
- **CA-23.18:** La posición en la rotación de rutinas y la secuencia de versiones se persiste indefinidamente en la base de datos local, sin reiniciarse por ausencias del ejecutante.
- **CA-23.19:** Al cerrar una sesión (completada o incompleta), el sistema avanza a la siguiente rutina en la rotación.

### Prescripción y Sesión

- **CA-23.20:** Al iniciar una nueva sesión, el sistema muestra la rutina y versión que corresponde según la rotación, con la lista de ejercicios prescritos y la carga objetivo derivada del historial.
- **CA-23.21:** Cada registro de serie se asocia automáticamente con la rutina, versión, ejercicio y número de serie, sin intervención manual del usuario.
- **CA-23.22:** La sustitución puntual en sesión activa filtra ejercicios por zona muscular (no por rutina ni módulo), ofreciendo cualquier ejercicio del Diccionario de la misma zona que no esté actualmente asignado a la sesión.

### Progresión y Reglas de Negocio

- **CA-23.23:** El incremento de carga por Doble Umbral se determina por zona muscular del ejercicio: +2.5 Kg para zonas de tren superior, +5 Kg para zonas de tren inferior, independientemente de la rutina.
- **CA-23.24:** La detección de fatiga acumulada se evalúa por rutina (regresión simultánea en ≥ 50% de los ejercicios de una sesión de esa rutina).
- **CA-23.25:** La detección de necesidad de descarga se evalúa por rutina (≥ 50% de ejercicios en meseta o regresión en esa rutina).
- **CA-23.26:** La descarga se mantiene activa durante un microciclo completo (todas las rutinas del plan), sin cambiar versiones durante el período de descarga.
- **CA-23.27:** Las acciones correctivas de meseta incluyen rotar a otra versión de la misma rutina (no de un módulo fijo).

### KPIs y Alertas

- **CA-23.28:** El RIR Promedio se calcula por rutina (no por módulo).
- **CA-23.29:** La Distribución de Volumen por Zona Muscular se calcula respecto al total de series de la rutina.
- **CA-23.30:** El conteo de microciclos se incrementa al completar todas las rutinas del plan en la rotación.
- **CA-23.31:** La alerta de inactividad se emite cuando transcurren más de 10 días sin ejecutar una rutina específica (no un módulo), con referencia a las zonas musculares de esa rutina.

### Historial y Trazabilidad

- **CA-23.32:** El historial de sesiones pasadas muestra rutina y versión (no módulo) para cada sesión.
- **CA-23.33:** La progresión de cada ejercicio se evalúa contra su último registro histórico del mismo ejercicio, independientemente de la rutina o versión en que se ejecutó.
- **CA-23.34:** Los datos históricos previos a esta migración se preservan íntegramente; las sesiones anteriores mantienen su referencia al módulo original como identificador de rutina migrada.

### Migración de Datos

- **CA-23.35:** Al actualizar la app, los módulos existentes (A, B, C) se migran automáticamente como rutinas con sus nombres originales, preservando todas las versiones, asignaciones de ejercicios, posición en la rotación y datos históricos.
- **CA-23.36:** La migración es transparente para el usuario: al abrir la app tras la actualización, su plan existente funciona exactamente igual, pero ahora puede crear nuevas rutinas, editarlas o reorganizarlas.
- **CA-23.37:** La migración se ejecuta mediante una migración Room que transforma el esquema sin pérdida de datos.
