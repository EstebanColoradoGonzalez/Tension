# Historia de Usuario

**Como** ejecutante,
**Quiero** que el sistema determine automáticamente si debo subir de peso en un ejercicio según la Regla de Doble Umbral y me prescriba la carga exacta para la próxima sesión,
**Para** progresar de forma sostenible basándome en evidencia propia y no en sensaciones subjetivas.

## Descripción

Esta historia implementa la Regla de Doble Umbral (Double Threshold Rule) para determinar cuándo un ejercicio está listo para incrementar carga. El sistema evalúa al cerrar una sesión si el ejecutante alcanzó ≥ 12 repeticiones en al menos 3 de las 4 series Y el RIR promedio de las 4 series fue ≥ 2. Si ambas condiciones se cumplen simultáneamente, prescribe un incremento de carga: +2.5 Kg para Módulos A y B (tren superior), +5 Kg para Módulo C (tren inferior). Si no se cumple, mantiene la misma carga. Ejercicios de peso corporal e isométricos quedan excluidos.

---

## Criterios de Aceptación

### CA-11.01 — Detección de cumplimiento del Doble Umbral

**Dado que** el sistema evalúa un ejercicio al cerrar una sesión,
**cuando** el ejecutante alcanzó ≥ 12 repeticiones en al menos 3 de las 4 series **y** el RIR promedio de las 4 series fue ≥ 2,
**entonces** el sistema señala que el ejercicio está listo para incrementar carga, cumpliendo ambas condiciones de la Regla de Doble Umbral simultáneamente.

### CA-11.02 — Exigencia de ambas condiciones

**Dado que** el sistema evalúa un ejercicio al cerrar una sesión,
**cuando** solo una de las dos condiciones se cumple (ej: ≥ 12 reps en 3/4 series pero RIR promedio < 2, o RIR promedio ≥ 2 pero sin alcanzar 12 reps en 3/4 series),
**entonces** el sistema no señala el ejercicio como listo para incrementar carga; ambas condiciones deben cumplirse simultáneamente.

### CA-11.03 — Incremento de carga para tren superior (Módulos A y B)

**Dado que** un ejercicio de los Módulos A o B cumple la Regla de Doble Umbral,
**cuando** el sistema calcula la carga objetivo para la próxima sesión,
**entonces** prescribe la carga actual + 2.5 Kg como nueva carga objetivo.

### CA-11.04 — Incremento de carga para tren inferior (Módulo C)

**Dado que** un ejercicio del Módulo C cumple la Regla de Doble Umbral,
**cuando** el sistema calcula la carga objetivo para la próxima sesión,
**entonces** prescribe la carga actual + 5 Kg como nueva carga objetivo.

### CA-11.05 — Reinicio del objetivo de repeticiones tras incremento

**Dado que** el sistema prescribe un incremento de carga para un ejercicio,
**cuando** determina los parámetros para la próxima sesión,
**entonces** el objetivo de repeticiones se restablece al límite inferior del rango (8 repeticiones), esperando que el ejecutante progrese de 8 hacia 12 con la nueva carga.

### CA-11.06 — Mantenimiento de carga cuando no se cumple el Doble Umbral

**Dado que** un ejercicio no cumple la Regla de Doble Umbral al cerrar la sesión (no alcanzó ≥ 12 reps en 3/4 series, o el RIR promedio fue < 2, o ambas),
**cuando** el sistema calcula la carga objetivo para la próxima sesión,
**entonces** prescribe la misma carga utilizada en la sesión actual, priorizando la progresión en repeticiones dentro del rango de 8 a 12.

### CA-11.07 — Persistencia de la carga prescrita

**Dado que** el sistema ha calculado la carga objetivo para la próxima sesión de un ejercicio,
**cuando** completa el procesamiento post-sesión,
**entonces** persiste la carga prescrita de manera que al iniciar la próxima sesión que incluya ese ejercicio (HU-05), la carga objetivo se muestre correctamente.

### CA-11.08 — No aplica a ejercicios de peso corporal

**Dado que** un ejercicio es de peso corporal (Peso = 0 Kg),
**cuando** el sistema evalúa si aplica la Regla de Doble Umbral,
**entonces** no aplica la regla ni prescribe incremento de carga, ya que la progresión de estos ejercicios se mide exclusivamente por repeticiones totales (CA-10.10).

> **Nota:** Cubre RF31 (exclusión de bodyweight del Doble Umbral). Criterio originalmente en HU-08 (ex CA-08.03), ya cubierto por este CA-11.08.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite progresión automática y basada en evidencia, eliminando la subjetividad en la prescripción de carga.

### Reglas de Negocio

1. **Doble Umbral requiere AMBAS condiciones simultáneamente:** ≥ 12 reps en ≥ 3 de 4 series AND RIR promedio ≥ 2. Si solo una se cumple, no hay incremento (CA-11.02).
2. **Incrementos diferenciados por segmento:** +2.5 Kg para Módulos A y B (tren superior), +5 Kg para Módulo C (tren inferior). Los valores provienen de `module.load_increment_kg` en la BD, no de constantes hardcodeadas.
3. **Bodyweight e isométricos excluidos:** No aplica el Doble Umbral a ejercicios de peso corporal. `prescribed_load_kg` permanece `NULL` permanentemente para estos ejercicios.
4. **La prescripción se ejecuta siempre:** Si se cumple el Doble Umbral → carga + incremento; si no se cumple → misma carga (mantenimiento). Para ejercicios estándar, el valor NUNCA queda `NULL` después de la primera sesión con sets.
5. **La "carga actual" es `avgWeightKg` de los sets de la sesión actual:** No el `prescribed_load_kg` anterior. El peso real registrado es la fuente de verdad, ya que el ejecutante puede no haber usado la carga prescrita.
6. **Reinicio a 8 reps es derivable:** No hay columna `target_reps` en `exercise_progression`. El rango `8-12` ya está en `plan_assignment.reps`. Cuando `prescribed_load_kg` sube, la UI de E1 ya muestra el rango — el ejecutante sabe que debe apuntar a 8 reps con la nueva carga.
7. **Se requieren 4 series completas:** CA-11.01 y RF25 dicen "3 de las 4 series", fijando 4 como cantidad requerida. Si el ejercicio tiene <4 sets (sesión incompleta), el Doble Umbral no se evalúa.

### Interfaz

- **HU-11 no tiene pantalla propia.** Es lógica pura de backend que produce un dato (`prescribed_load_kg`) consumido por:
  - E1 (HU-05): carga objetivo al iniciar sesión — muestra `prescribedLoadKg` cuando no es null.
  - E5 (HU-13): Resumen Post-Sesión — muestra "Subir carga → X Kg" cuando `prescribed_load_kg` > carga actual, o "Mantener carga" cuando son iguales.
- No hay UI, ViewModel ni pantalla nuevos.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante. La regla pura opera en Kotlin sin dependencias de framework.

### Archivos Nuevos

| Archivo | Propósito |
|---|---|
| `domain/rules/DoubleThresholdRule.kt` | Regla pura: evaluación Doble Umbral + prescripción de carga |
| `test/.../domain/rules/DoubleThresholdRuleTest.kt` | Tests unitarios de la regla |

### Archivos Modificados

| Archivo | Acción | Sección |
|---|---|---|
| `SessionExerciseDao.kt` | Agregar `loadIncrementKg` al DTO + JOIN a `module` en query | `getSessionExercisesForProgression()` |
| `SessionRepositoryImpl.kt` | Agregar cálculo de `prescribedLoadKg` + import `DoubleThresholdRule` | `evaluateProgression()` |

### Archivos NO Tocados

| Archivo | Razón |
|---|---|
| `ProgressionClassificationRule.kt` | Sin cambios — HU-11 es una regla independiente |
| `ExerciseSessionData.kt` | Sin cambios — `avgWeightKg`, `avgRir`, `setCount`, `sets` ya existen |
| `SetData.kt` | Sin cambios — `reps`, `rir`, `weightKg` ya existen |
| `ProgressionClassification.kt` | Sin cambios |
| `ExerciseProgressionEntity.kt` | Sin cambios — `prescribedLoadKg: Double?` ya existe |
| `ExerciseProgressionDao.kt` | Sin cambios — `update()` ya acepta el entity completo |
| `ExerciseSetDao.kt` | Sin cambios — queries de HU-10 son suficientes |
| `SessionRepository.kt` | Sin cambios — interfaz mantiene misma firma |
| `CloseSessionUseCase.kt` | Sin cambios — sigue delegando a `closeSession()` |
| `SessionExerciseWithDetails` | Sin cambios — ya incluye `prescribedLoadKg` por JOIN con `exercise_progression` |
| `ActiveSessionViewModel.kt` | Sin cambios — HU-11 no tiene UI |
| `ActiveSessionScreen.kt` | Sin cambios — ya muestra `prescribedLoadKg` cuando no es null (HU-05) |

---

## Predecesoras (datos que HU-11 consume)

- **HU-05:** Creó `ExerciseProgressionEntity` con `prescribedLoadKg`. Implementó el JOIN en `getBySessionIdWithDetails` que muestra `prescribedLoadKg` en E1. Infraestructura de persistencia ya lista.
- **HU-06:** Persistió datos de `exercise_set` (peso, reps, RIR) que HU-11 evalúa para el Doble Umbral.
- **HU-10:** Estableció `evaluateProgression()` con el loop por ejercicio, los datos de sesión (`currentData`, `exercise.moduleCode`) y la llamada a `exerciseProgressionDao.update()` que HU-11 extiende.

## Sucesoras (dependen de HU-11)

- **HU-13:** E5 (Resumen Post-Sesión) — mostrará "Subir carga → X Kg" cuando `prescribed_load_kg` > carga actual, o "Mantener carga" cuando son iguales. Leerá `exercise_progression.prescribed_load_kg`.
- **HU-15:** Recomendaciones escalonadas — sesión 4 recomendará microincremento (MDS R3: "1.25 Kg si está disponible, o añadir 1 rep por serie"). Podrá usar `prescribed_load_kg` como base.
- **HU-17:** Protocolo de descarga — post-descarga la carga se reinicia al 90% de la pre-descarga (CA-17.05). HU-17 escribirá directamente sobre `prescribed_load_kg`.

## Consumidoras Indirectas

- **HU-20:** Métricas globales — podrá derivar progresión de carga comparando `prescribed_load_kg` a lo largo del tiempo.
- **HU-22:** KPIs por ejercicio — tasa de progresión de carga.
- **HU-23:** Historial de ejercicio (F3) — podrá mostrar evolución de carga prescrita.
