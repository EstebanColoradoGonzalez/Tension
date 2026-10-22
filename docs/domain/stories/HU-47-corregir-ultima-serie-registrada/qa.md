# Casos de Prueba — Historia #47: Corregir los datos de la última serie registrada

**Historia:** [historia.md](./historia.md) | **Autor:** esteban.colorado | **Fecha:** 2026-10-22

## Alcance de las Pruebas

Se prueba la ventana global de corrección en sesión activa: aparición y movimiento de la acción "Corregir", el formulario de corrección (precarga, campos corregibles, implemento no editable), las validaciones (idénticas a las del registro), el recálculo de derivados (1RM en ambas direcciones, resumen, historial), el cierre de la ventana al registrar y al cerrar la sesión, y la supervivencia de la ventana tras un cierre inesperado.

**Fuera de alcance (no se prueban aquí):** corrección post-cierre (inmutabilidad — se verifica solo su ausencia), corrección del implemento (explícitamente no es corregible), cambio de número de series o de completitud por corregir, y el flujo de registro original de serie (HU-06, ya cubierto por sus propios casos).

## Precondiciones y Datos Base

- **Sesión activa:** rutina "Empuje — Pecho", Versión 1, miércoles 2026-10-22, estado en curso.
- **Ejercicios de la sesión (prescripción 3 × 8-12):**
  - `Press de Banca` — Barra. 0 series registradas.
  - `Elevación Lateral` — Mancuerna. 3 series registradas: S1 = 10 kg × 12 reps × RIR 1; S2 = 10 kg × 12 reps × RIR 1; S3 = **18 kg × 18 reps × RIR 1** (error de dedo: lo correcto era 10 kg × 10 reps).
  - `Press Inclinado` — Mancuerna. 0 series registradas.
- **1RM estimado de Elevación Lateral (Mancuerna):** 12 kg antes de esta sesión; la S3 equivocada (18 kg) lo actualizó a 18 kg.
- **Sesión para variantes (se prepara aparte):** `Plancha` (isométrico, peso corporal; 2 series, la última 45 s) y `Flexiones` (peso corporal que admite "Peso Añadido"; 1 serie, peso 0 kg).
- **Unidad de captura de trabajo:** kg. Para QA-20: lb.
- La app funciona 100% local; sin condiciones de red.

## Matriz de Cobertura

| Criterio / Regla | Casos |
| --- | --- |
| CA-47.01 — Acción "Corregir" visible sobre la última serie registrada | QA-01 |
| CA-47.02 — Ventana global que se mueve al registrar | QA-02, QA-03, QA-12 |
| CA-47.03 — Formulario precargado, implemento no editable | QA-04 |
| CA-47.04 — Confirmar actualiza la serie y recalcula derivados | QA-05, QA-06, QA-20 |
| CA-47.05 — No altera estructura de la sesión | QA-08 |
| CA-47.06 — Validaciones idénticas al registro | QA-09, QA-10, QA-11 |
| CA-47.07 — Solo la última serie es corregible | QA-12 |
| CA-47.08 — Cerrar la sesión cierra la ventana; post-cierre inmutable | QA-13 |
| CA-47.09 — Variantes (isométrico, peso corporal) | QA-14, QA-15 |
| CA-47.10 — La ventana sobrevive al cierre inesperado | QA-16 |
| CA-47.11 — El 1RM se ajusta (baja y sube) | QA-17, QA-18 |
| CA-47.12 — Resumen e historial muestran el dato corregido | QA-19 |
| Regla 1 — Una ventana global de sesión | QA-01, QA-02, QA-03 |
| Regla 2 — La ventana se cierra al registrar | QA-02, QA-03, QA-12 |
| Regla 3 — La ventana se cierra al cerrar la sesión | QA-13 |
| Regla 4 — Campos corregibles | QA-04, QA-20 |
| Regla 5 — El implemento no se corrige | QA-04 |
| Regla 6 — Corrección repetida dentro de la ventana | QA-06 |
| Regla 7 — Recálculo automático de derivados | QA-05, QA-17, QA-18, QA-19 |
| Regla 8 — Mismas validaciones que el registro | QA-09, QA-10, QA-11 |
| Regla 9 — Corregir no añade ni borra series | QA-08 |
| Regla 10 — La ventana sobrevive al cierre inesperado | QA-16 |

---

## Casos de Prueba

### QA-01: Al registrar una serie, la acción "Corregir" aparece solo sobre ese ejercicio

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.01, Regla 1
- **Precondiciones:** Sesión activa; `Elevación Lateral` tiene 2 series registradas (S1, S2); `Press de Banca` y `Press Inclinado` no tienen series.
- **Datos de prueba:**

  | Campo | Valor | Nota |
  | ----- | ----- | ---- |
  | Peso | 10 kg | Válido |
  | Repeticiones | 12 | Válido |
  | RIR | 1 | Válido |

- **Pasos:**
  1. Iniciar sesión (o retomar) y registrar la serie 3 de `Elevación Lateral` con los datos de prueba.
  2. Observar la lista de ejercicios de la sesión.
- **Resultado esperado:** La tarjeta de `Elevación Lateral` muestra la acción "✎ Corregir". Ninguna otra tarjeta (Press de Banca, Press Inclinado) muestra ninguna acción de corregir. No hay dos acciones "Corregir" visibles a la vez.

### QA-02: Al registrar una serie de OTRO ejercicio, la ventana se mueve y la anterior se cierra

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.02, Regla 1, Regla 2
- **Precondiciones:** Datos base: `Elevación Lateral` con sus 3 series (S3 = 18 kg × 18) y la acción "Corregir" visible sobre ella.
- **Pasos:**
  1. Registrar la serie 1 de `Press de Banca` (60 kg × 10 × RIR 1).
  2. Observar `Elevación Lateral`.
  3. Intentar acceder a la corrección de la S3 de `Elevación Lateral` desde cualquier punto de la interfaz.
- **Resultado esperado:** La acción "Corregir" desapareció de `Elevación Lateral` y aparece sobre `Press de Banca`. La S3 de `Elevación Lateral` no es corregible por ningún medio mientras dure la sesión: no existe acción, entrada ni menú que lo permita.

### QA-03: Al registrar la siguiente serie del MISMO ejercicio, la ventana se cierra sobre la anterior

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.02, Regla 2
- **Precondiciones:** `Press de Banca` con 1 serie registrada y la acción "Corregir" visible sobre ella.
- **Pasos:**
  1. Registrar la serie 2 de `Press de Banca` (60 kg × 10 × RIR 1).
  2. Observar la tarjeta de `Press de Banca`.
- **Resultado esperado:** La acción "Corregir" sigue visible sobre `Press de Banca`, pero ahora corresponde a la **serie 2**. Abrirla precarga los datos de la serie 2 (no de la serie 1). La serie 1 queda inmutable.

### QA-04: El formulario de corrección se precarga con lo registrado y no permite tocar el implemento

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.03, Regla 4, Regla 5
- **Precondiciones:** `Elevación Lateral` con S3 registrada (18 kg × 18 × RIR 1, Mancuerna) y la ventana sobre ella.
- **Pasos:**
  1. Tocar "✎ Corregir" sobre `Elevación Lateral`.
  2. Inspeccionar el formulario: título, campos precargados y editabilidad del implemento.
- **Resultado esperado:** El título deja claro que se está **corrigiendo** la serie (no registrando una nueva). Precarga exacta: peso 18 (kg), repeticiones 18, RIR 1, implemento Mancuerna. El campo implemento se muestra pero **no es editable**. No aparecen campos de número de serie, ejercicio, fecha ni módulo. Al cancelar, se vuelve a la sesión sin cambios.

### QA-05: Confirmar la corrección actualiza la serie y recalcula los derivados

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.04, Regla 7
- **Precondiciones:** Datos base: S3 de `Elevación Lateral` = 18 kg × 18 × RIR 1 (error); 1RM del par (Mancuerna) = 18 kg tras la S3 equivocada (antes era 12 kg).
- **Datos de prueba:**

  | Campo | Valor | Nota |
  | ----- | ----- | ---- |
  | Peso | 10 kg | Corrección real |
  | Repeticiones | 10 | Corrección real |
  | RIR | 1 | Sin cambio |
  | Implemento | Mancuerna | No editable |

- **Pasos:**
  1. Abrir la corrección de la S3 y cambiar peso a 10 kg y repeticiones a 10.
  2. Confirmar.
  3. Consultar el detalle de la serie 3 en la sesión.
- **Resultado esperado:** La S3 queda registrada como 10 kg × 10 × RIR 1, con implemento Mancuerna. Aparece una confirmación breve. El 1RM del par se recalcó (queda 12 kg — ver QA-17). No existe vista que muestre "18 × 18" como valor previo: el dato es uno solo, el corregido.

### QA-06: La corrección se puede repetir mientras la serie siga siendo la última

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.04, Regla 6
- **Precondiciones:** S3 de `Elevación Lateral` ya fue corregida a 10 kg × 10 (QA-05); no se ha registrado ninguna serie posterior.
- **Pasos:**
  1. Volver a tocar "✎ Corregir" sobre `Elevación Lateral`.
  2. Cambiar repeticiones a 11 y confirmar.
  3. Registrar la serie 1 de `Press de Banca` (50 kg × 10 × RIR 1).
  4. Intentar corregir la S3 de `Elevación Lateral` de nuevo.
- **Resultado esperado:** El paso 1 precarga 10 kg × 10 (el valor corregido, no el original 18). Tras el paso 2, la S3 queda 10 kg × 11. Tras el paso 3, el paso 4 es imposible: la ventana ya no está sobre esa serie y no hay ninguna entrada para corregirla.

### QA-07: Cancelar la corrección conserva los valores y deja la ventana abierta

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** Disparador "El ejecutante cancela la corrección" (reprocesable)
- **Precondiciones:** Ventana sobre la S3 de `Elevación Lateral` (18 kg × 18).
- **Pasos:**
  1. Abrir la corrección y cambiar peso a 10 kg.
  2. Tocar "Cancelar".
  3. Volver a tocar "✎ Corregir" sobre `Elevación Lateral`.
- **Resultado esperado:** Tras cancelar, la S3 conserva 18 kg × 18. La acción "Corregir" sigue visible sobre `Elevación Lateral`. Al reabrirla, la precarga vuelve a ser 18 kg × 18 (el valor vigente, sin rastro del 10 descartado).

### QA-08: Corregir no altera estados de ejercicio ni completitud de la sesión

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-47.05, Regla 9
- **Precondiciones:** `Elevación Lateral` completado (3/3) con la ventana sobre su S3.
- **Pasos:**
  1. Corregir la S3 (18×18 → 10×10) y confirmar.
  2. Observar el estado de `Elevación Lateral` y los contadores de la sesión.
- **Resultado esperado:** `Elevación Lateral` sigue "Completado" 3/3. El número de series del ejercicio no cambió. La completitud global de la sesión no se alteró. El contador de series de la sesión es el mismo antes y después de corregir.

### QA-09: Validación de peso en la corrección (partición y límites)

- **Tipo:** Negativo / Borde
- **Prioridad:** Alta
- **Cubre:** CA-47.06, Regla 8
- **Precondiciones:** Ventana sobre la S3 de `Press de Banca` (Barra, no es peso corporal), registrada 60 kg × 10.
- **Datos de prueba:**

  | Campo | Valor | Resultado esperado |
  | ----- | ----- | ------------------ |
  | Peso | −1 | Rechazado: mensaje de error indicando que el peso debe ser ≥ 0 |
  | Peso | 0 | Rechazado (ejercicio con carga externa): 0 solo es válido para peso corporal |
  | Peso | 0.5 | Aceptado (incremento mínimo del sistema) |
  | Peso | 82.5 | Aceptado |

- **Pasos:**
  1. Abrir la corrección y, por cada fila, introducir el valor de peso y observar validación y botón.
  2. Con un valor inválido, intentar confirmar.
- **Resultado esperado:** Con valores inválidos, el botón "Confirmar" queda deshabilitado y se muestra el mensaje de error correspondiente (mismo texto que en el registro). Con valores válidos, el botón se habilita. La serie nunca se guarda con peso inválido.

### QA-10: Validación de repeticiones y segundos sostenidos

- **Tipo:** Negativo / Borde
- **Prioridad:** Alta
- **Cubre:** CA-47.06, Regla 8
- **Precondiciones:** Ventana sobre la S3 de `Press de Banca` (estándar) y, en la sesión de variantes, sobre la última serie de `Plancha` (isométrico).
- **Datos de prueba:**

  | Ejercicio | Campo | Valor | Resultado esperado |
  | --------- | ----- | ----- | ------------------ |
  | Press de Banca | Repeticiones | 0 | Rechazado: deben ser ≥ 1 |
  | Press de Banca | Repeticiones | 1 | Aceptado (mínimo) |
  | Press de Banca | Repeticiones | 10 | Aceptado |
  | Plancha | Segundos sostenidos | 0 | Rechazado: ≥ 1 |
  | Plancha | Segundos sostenidos | 30 | Aceptado (dentro de referencia 30–45) |
  | Plancha | Segundos sostenidos | 60 | Aceptado (la referencia no es límite) |

- **Pasos:**
  1. En cada fila, abrir la corrección correspondiente, introducir el valor y observar.
- **Resultado esperado:** Los rechazos muestran el mismo mensaje de error que en el registro; el botón queda deshabilitado. Los aceptados permiten confirmar y persisten el valor.

### QA-11: RIR acotado a 0–2 y estado del botón con campos inválidos

- **Tipo:** Negativo / Estado UI
- **Prioridad:** Media
- **Cubre:** CA-47.06, Regla 8
- **Precondiciones:** Ventana sobre cualquier serie estándar.
- **Pasos:**
  1. Abrir la corrección e inspeccionar el selector de RIR.
  2. Intentar confirmar con peso inválido y RIR seleccionado; luego con todos los campos válidos.
- **Resultado esperado:** El selector muestra únicamente los chips 0, 1 y 2: no existe forma de elegir 3 o más (rango garantizado por construcción, igual que en el registro). Con cualquier otro campo inválido el botón "Confirmar" se deshabilita; con todos válidos se habilita. El teclado desplegado en los campos numéricos es numérico, no alfanumérico.

### QA-12: No se corrige ninguna serie que no sea la última registrada

- **Tipo:** Negativo
- **Prioridad:** Alta
- **Cubre:** CA-47.07, Regla 2
- **Precondiciones:** Datos base: `Elevación Lateral` 3/3 (S1, S2, S3); `Press de Banca` con S1 registrada y la ventana sobre ella.
- **Pasos:**
  1. Inspeccionar la tarjeta de `Elevación Lateral` (S1, S2, S3 anteriores a la S1 de Press de Banca).
  2. Explorar la interfaz (listas, menús contextuales, detalle de la serie) buscando cualquier entrada de corrección sobre las series de `Elevación Lateral`.
- **Resultado esperado:** No existe ninguna acción, menú o entrada que permita corregir la S1, S2 o S3 de `Elevación Lateral`: la ventana global está sobre la S1 de `Press de Banca` y ninguna otra serie de la sesión es corregible.

### QA-13: Cerrar la sesión cierra la ventana y congela todo; el detalle pasado es de solo lectura

- **Tipo:** Negativo
- **Prioridad:** Alta
- **Cubre:** CA-47.08, Regla 3
- **Precondiciones:** Ventana abierta sobre la última serie registrada (S1 de `Press de Banca`).
- **Pasos:**
  1. Cerrar la sesión.
  2. Inspeccionar la pantalla de resumen y el detalle de la sesión recién cerrada (F2-T1).
  3. Intentar, desde cualquier vista del historial, modificar la S1 de `Press de Banca` o cualquier otra serie.
- **Resultado esperado:** No hay acción "Corregir" en ninguna parte: ni en el resumen ni en el detalle de la sesión cerrada. Todas las series de la sesión son inmutables: no existe ninguna interfaz de modificación. La detalla muestra los datos de las series (corregidos si se corrigieron antes de cerrar).

### QA-14: Corrección de una serie isométrica (segundos sostenidos, peso fijo)

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-47.09, Regla 4
- **Precondiciones:** Sesión de variantes: `Plancha` con 2 series; la última = 0 kg × 45 s × RIR 2; ventana sobre ella.
- **Pasos:**
  1. Tocar "✎ Corregir" sobre `Plancha`.
  2. Inspeccionar campos.
  3. Cambiar segundos a 40 y confirmar.
- **Resultado esperado:** El formulario muestra peso fijo en 0 (no editable) y el campo de tiempo como "Segundos sostenidos" precargado en 45, con la referencia 30–45 s. Tras confirmar, la serie queda 0 kg × 40 s × RIR 2. La ventana sigue sobre esa serie.

### QA-15: Corrección de una serie de peso corporal con "Peso Añadido"

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-47.09, CA-47.06, Regla 4
- **Precondiciones:** Sesión de variantes: `Flexiones` (admite "Peso Añadido") con 1 serie registrada con implemento "Peso Añadido" y 5 kg; ventana sobre ella.
- **Pasos:**
  1. Tocar "✎ Corregir" sobre `Flexiones`.
  2. Cambiar peso a 2.5 kg y confirmar.
  3. Abrir la corrección de nuevo.
- **Resultado esperado:** El campo de peso es editable (carga añadida) y no queda bloqueado en 0, porque el implemento es "Peso Añadido" (misma regla que en el registro). Tras confirmar, la serie queda con 2.5 kg. La precarga al reabrir muestra 2.5 kg.

### QA-16: La ventana sobrevive al cierre inesperado y al retomar la sesión

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.10, Regla 10
- **Precondiciones:** Ventana abierta sobre la S1 de `Press de Banca` (60 kg × 10).
- **Pasos:**
  1. Cerrar la aplicación de forma abrupta (matar el proceso del sistema) sin cerrar la sesión.
  2. Reabrir la aplicación; la sesión se retoma desde donde quedó.
  3. Observar la tarjeta de `Press de Banca`.
  4. Tocar "✎ Corregir" y precargar/confirmar una corrección (60 → 50 kg).
- **Resultado esperado:** Al retomar, la acción "Corregir" está visible sobre `Press de Banca` (la S1 era la última registrada antes del cierre). La precarga muestra 60 kg × 10. La corrección persiste y la sesión continúa normalmente.

### QA-17: El 1RM estimado baja al corregir la serie que lo contaminó

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.11, Regla 7
- **Precondiciones:** Datos base: 1RM de `Elevación Lateral` (Mancuerna) = 12 kg antes de la sesión; la S3 equivocada (18 kg × 18) lo actualizó a 18 kg. Ventana sobre la S3.
- **Pasos:**
  1. Corregir la S3 a 10 kg × 10 y confirmar.
  2. Consultar la vista de 1RM estimado del par (Mancuerna).
- **Resultado esperado:** El 1RM del par queda en 12 kg —el máximo de las series que realmente califican, una vez que la serie de 18 kg dejó de existir como tal—. El récord **bajó**: el objetivo es el dato fiel, no el máximo registrado.

### QA-18: El 1RM estimado se actualiza con el valor corregido si también califica como máximo

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-47.11, Regla 7
- **Precondiciones:** 1RM de `Elevación Lateral` (Mancuerna) = 12 kg. Ventana sobre la S3 registrada 8 kg × 15 (no califica como récord).
- **Pasos:**
  1. Corregir la S3 a 14 kg × 10 y confirmar (el valor real era mayor que el registrado).
  2. Consultar la vista de 1RM estimado del par.
- **Resultado esperado:** El 1RM del par queda en 14 kg: el valor corregido calificó y actualizó el récord, como si la serie se hubiera registrado correcta a la primera.

### QA-19: El resumen post-sesión y el historial muestran el dato corregido, sin rastro del error

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-47.12, Regla 7
- **Precondiciones:** S3 de `Elevación Lateral` corregida de 18×18 a 10×10 (QA-05) mientras la sesión seguía en curso.
- **Pasos:**
  1. Cerrar la sesión.
  2. Consultar el resumen post-sesión (E5) y el detalle de la sesión en el historial (F2-T1).
  3. Consultar los KPIs de la sesión que incorporan tonelaje o repeticiones.
- **Resultado esperado:** El resumen y el detalle muestran la S3 como 10 kg × 10 × RIR 1 —como si se hubiera registrado correcta a la primera—. Los KPIs usan esos mismos datos. No existe ninguna vista (resumen, detalle, historial) que muestre el valor 18 × 18 ni una marca de "corregido".

### QA-20: Corregir el peso en libras convierte a la unidad canónica

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-47.04, Regla 4 (con la conversión de HU-30)
- **Precondiciones:** Ventana sobre la S1 de `Press de Banca`, registrada en lb: 132 lb (≈ 59.9 kg en canónica).
- **Pasos:**
  1. Abrir la corrección: el campo de peso muestra 132 con unidad lb.
  2. Cambiar a 110 lb y confirmar.
  3. Consultar el detalle de la serie.
- **Resultado esperado:** La serie queda con unidad de captura lb y peso 110 lb; el valor canónico en kilogramos se actualizó con la misma conversión del registro (110 lb ≈ 49.9 kg, 2 decimales, sin redondeo al múltiplo de 0.5). Los derivados recalculados usan el valor canónico convertido.

---

**Total de casos:** 20

## Vacíos Detectados

| # | Vacío | Afecta a |
| - | ----- | -------- |
| V1 | El texto exacto de la confirmación tras confirmar una corrección no está definido en `historia.md` (solo "confirmación breve"). QA-05 y QA-06 verifican la presencia de una confirmación, no su redacción. | CA-47.04 |
