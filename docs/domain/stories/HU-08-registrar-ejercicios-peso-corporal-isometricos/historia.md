# Historia de Usuario: `HU-08` — Registrar ejercicios de peso corporal e isométricos

> Este documento define una unidad atómica de valor. El código desarrollado debe cumplir **estrictamente** con los Criterios de Aceptación listados aquí. No se deben añadir funcionalidades no descritas en este documento (evitar *scope creep*).

## 1. Metadatos

- **ID:** `HU-08`
- **Épica / Módulo:** `EPIC-02: Ciclo de Entrenamiento`
- **Estado:** `Done`
- **Prioridad:** `Alta`
- **Agente Asignado:** `Architect`
- **Requisitos cubiertos:** RNF12

> **Nota:** Los requisitos RF31, RF32 y RF33 que originalmente pertenecían a esta historia fueron redistribuidos a HU-10 y HU-11, donde se implementarán junto con el motor de progresión. Los CAs de registro (CA-08.01, CA-08.04, CA-08.05, CA-08.08) fueron pre-implementados en HU-06 como parte del formulario E2 completo.

## 2. Narrativa de Negocio

- **Como** ejecutante,
- **Quiero** que el sistema maneje correctamente el registro de ejercicios de peso corporal y ejercicios isométricos con sus reglas de interfaz y validación específicas,
- **Para** que la captura de datos refleje las características biomecánicas particulares de cada tipo de ejercicio.

> **Nota histórica:** Esta historia originalmente incluía 8 CAs (4 de registro + 4 de progresión). Los 4 CAs de registro fueron pre-implementados en HU-06 como parte del formulario E2. Los 4 CAs de progresión (CA-08.02, CA-08.03, CA-08.06, CA-08.07) fueron redistribuidos a HU-10 (como CA-10.10, CA-10.11, CA-10.12) y HU-11 (ya cubierto por CA-11.08), donde serán implementados junto con el motor de progresión que les da contexto de ejecución.

## 3. Criterios de Aceptación (BDD)

### CA-08.01 — Registro de ejercicios de peso corporal con Peso = 0

**Dado que** el ejecutante registra una serie de un ejercicio de peso corporal (Flexiones, Sentadilla a cuerpo libre, Abdominales, Escalador, Giro Ruso),
**cuando** completa el formulario de registro,
**entonces** el campo de peso se establece en 0 Kg, y el sistema registra las repeticiones logradas y el RIR normalmente.

> **Estado:** Implementado en HU-06 (RegisterSetViewModel, RegisterSetScreen).

### CA-08.04 — Registro de ejercicios isométricos en segundos

**Dado que** el ejecutante registra una serie de un ejercicio isométrico (Plancha, Plancha Lateral),
**cuando** completa el formulario de registro,
**entonces** el sistema solicita la duración en segundos sostenidos en lugar de repeticiones, y el campo de entrada refleja claramente que se trata de segundos, junto con el RIR correspondiente.

> **Estado:** Implementado en HU-06 (RegisterSetScreen, variante isométrica).

### CA-08.05 — Validación de rango isométrico

**Dado que** el ejecutante registra la duración de una serie de un ejercicio isométrico,
**cuando** ingresa un valor,
**entonces** el sistema acepta valores ≥ 1 segundo y muestra una referencia visual del rango prescrito de 30 a 45 segundos para orientar al ejecutante.

> **Estado:** Implementado en HU-06 (RegisterSetViewModel validación, RegisterSetScreen referencia visual).

### CA-08.08 — Validación de datos de entrada

**Dado que** el ejecutante registra una serie de cualquier tipo de ejercicio (peso corporal o isométrico),
**cuando** ingresa valores fuera de los rangos permitidos (repeticiones < 1, duración en segundos < 1, RIR fuera de 0-5),
**entonces** el sistema rechaza el registro con un mensaje de error claro antes de persistir.

> **Estado:** Implementado en HU-06 (RegisterSetUseCase require, RegisterSetViewModel validación inline).

---

### CA-08.02 — Progresión de peso corporal por repeticiones totales

**Dado que** el ejecutante ha registrado las 4 series de un ejercicio de peso corporal,
**cuando** el sistema evalúa la progresión al cerrar la sesión,
**entonces** evalúa la progresión por el total de repeticiones de las 4 series (Regla 6 del MDS §6-A), no por incremento de carga.

> **Estado:** Diferido a HU-10 (motor de clasificación de progresión — Regla 6 MDS).

### CA-08.03 — Regla de Doble Umbral no aplica a peso corporal

**Dado que** el ejecutante completa un ejercicio de peso corporal,
**cuando** el motor de Doble Umbral evalúa si debe incrementar carga,
**entonces** el motor NO prescribe incremento de carga para ejercicios de peso corporal (Δmin = 0).

> **Estado:** Diferido a HU-11 (motor de Doble Umbral — Regla 1 MDS, exclusión bodyweight).

### CA-08.06 — Progresión de isométricos por tiempo sostenido

**Dado que** el ejecutante ha registrado las 4 series de un ejercicio isométrico,
**cuando** el sistema evalúa la progresión al cerrar la sesión,
**entonces** evalúa la progresión por los segundos sostenidos en el rango 30-45s comparados contra la sesión anterior (Regla 7 del MDS §6-A).

> **Estado:** Diferido a HU-10 (motor de clasificación de progresión — Regla 7 MDS).

### CA-08.07 — Marcado de isométrico como "dominado"

**Dado que** el ejecutante sostiene 4 de 4 series de un ejercicio isométrico ≥ 45 segundos,
**cuando** el sistema evalúa la progresión al cerrar la sesión,
**entonces** el sistema marca el ejercicio con estado "Dominado" (`MASTERED`) y lo presenta visualmente con un badge 🏆 en el resumen post-sesión.

> **Estado:** Diferido a HU-10 (Regla 7 MDS — transición `status → MASTERED`).

## 4. Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite capturar datos de ejercicios con características biomecánicas particulares (peso corporal fijo, duración en lugar de repeticiones para isométricos).

### Reglas de Negocio

1. **Peso = 0 para ejercicios de peso corporal:** Los ejercicios de peso corporal (Flexiones, Sentadilla a cuerpo libre, Abdominales, Escalador, Giro Ruso) siempre registran peso = 0 Kg, campo no editable.
2. **Segundos en lugar de repeticiones para isométricos:** Los ejercicios isométricos (Plancha, Plancha Lateral) registran duración en segundos en lugar de repeticiones.
3. **Validación de rango isométrico:** Se acepta valores ≥ 1 segundo, con referencia visual del rango prescrito de 30-45 segundos.
4. **Validación de datos:** Repeticiones ≥ 1, RIR en rango 0-5, peso ≥ 0.
5. **Campo `reps` dual:** El campo `exercise_set.reps` almacena tanto repeticiones como segundos. La interpretación depende de `exercise.is_isometric`.

### Interfaz

- **Componente Visual (ya implementado en HU-06):** `E2 RegisterSetScreen` — 3 variantes:
  - **Estándar:** Peso editable en Kg, repeticiones.
  - **Peso corporal:** Peso = 0 no editable, label "Peso (Kg) (Peso corporal)", repeticiones.
  - **Isométrico:** Label "Segundos sostenidos", suffix "seg", referencia 30-45 seg, RIR.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Preview de Interfaz

Ver `Especificación Visual §8 E2` para RegisterSetScreen. La especificación documenta explícitamente las 3 variantes de E2 con su trazabilidad: *"HU-06 (CA-06.01 a CA-06.09) · HU-08 (CA-08.01, CA-08.04, CA-08.05, CA-08.08)"*.
