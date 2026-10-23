# Casos de Prueba — Historia #49: Carga real con peso corporal en tonelaje, 1RM y fuerza relativa

**Historia:** [historia.md](./historia.md) | **Autor:** esteban.colorado | **Fecha:** 2026-10-22

## Alcance de las Pruebas

Se prueba:
- el cálculo de la carga real por serie: porcentaje del ejercicio × peso vigente en la fecha + carga externa;
- la tabla de implementos que suman o no suman peso corporal, y las exclusiones (0 %, isométricos, Dominadas asistidas);
- que la carga real llegue a todas las vistas de tonelaje y a la alerta de caída de tonelaje;
- el 1RM sobre carga real con su fecha, y la fuerza relativa con el peso de esa fecha;
- el atributo "Peso corporal movido" en el Diccionario: valores precargados, validación y recálculo del historial al cambiarlo;
- la corrección de la última serie (`HU-47`) aplicada a la carga real;
- el respaldo;
- que el motor de decisión no cambie.

**Fuera de alcance (no se prueban aquí):**
- la sección "Peso corporal" de Métricas, el registro rápido de peso y la eliminación del nivel de experiencia (`HU-48`);
- la fórmula base del 1RM y la serie de referencia (10 repeticiones, RIR 1), ya cubiertas por `HU-42`;
- la migración de datos anteriores (instalación fresca, ADR-019).

## Precondiciones y Datos Base

- **Fecha de referencia ("hoy"):** 2026-10-22.
- **Historial de peso corporal:**
  - 2026-09-02 → 78.0 Kg (registro inicial)
  - 2026-10-10 → 80.0 Kg
- **Peso vigente esperado:**
  - sesión del 2026-10-05 → **78.0 Kg**
  - sesión del 2026-10-14 → **80.0 Kg**
- **Porcentajes del catálogo (instalación fresca):**
  - Dominadas 100 %
  - Sentadilla Búlgara 85 %
  - Zancadas (Lunges) 85 %
  - Crunch Abdominal 35 %
  - Press de Banca, Remo y el resto 0 %
- **Ejercicio personalizado isométrico:** "Plancha Lateral", creado por el tester, isométrico.
- **Factor del 1RM** (10 repeticiones): 1 ÷ (1.0278 − 0.0278 × 10) = **1.33369**.
- **Conversión:** 1 Kg = 2.20462 Lb.
- Salvo que el caso diga otra cosa, las sesiones se registran en **2026-10-14**, con peso vigente de 80.0 Kg.

## Matriz de Cobertura

| Criterio / Regla | Casos |
| --- | --- |
| CA-49.01 — Tonelaje con peso corporal movido | QA-01, QA-04, QA-05 |
| CA-49.02 — 1RM sobre carga real, con fecha | QA-07, QA-08, QA-15 |
| CA-49.03 — Fuerza relativa junto al 1RM | QA-07, QA-09 |
| CA-49.04 — Porcentaje editable, valores precargados | QA-10 |
| CA-49.05 — Validación del porcentaje | QA-11 |
| CA-49.06 — Peso vigente por fecha | QA-06, QA-09 |
| CA-49.07 — Series que no suman peso corporal | QA-02, QA-03 |
| CA-49.08 — Cambiar el porcentaje recalcula el historial | QA-12 |
| CA-49.09 — El motor de decisión no cambia | QA-13 |
| CA-49.10 — Respaldo con porcentaje y fecha del 1RM | QA-16 |
| Regla 1 — Peso vigente en una fecha | QA-06, QA-09 |
| Regla 2 — Porcentaje por ejercicio | QA-10, QA-11 |
| Regla 3 — Implementos que suman y que no suman | QA-01, QA-02 |
| Regla 4 — Fórmula de la carga real | QA-01, QA-02, QA-03 |
| Regla 5 — Carga real en todas las vistas de tonelaje | QA-04, QA-05 |
| Regla 6 — 1RM sobre carga real, con fecha | QA-07, QA-08 |
| Regla 7 — Fuerza relativa | QA-07, QA-09 |
| Regla 8 — Cambio de porcentaje en todo el historial | QA-12 |
| Regla 9 — El motor de decisión no cambia | QA-13, QA-14 |

---

## Casos de Prueba

### QA-01: Los implementos que suman peso corporal añaden la carga real al tonelaje

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-49.01, Reglas 3 y 4
- **Datos de prueba:** peso vigente 80.0 Kg.

  | Ejercicio (%) | Implemento | Carga registrada | Reps | Tonelaje esperado |
  | ------------- | ---------- | ---------------- | ---- | ----------------- |
  | Dominadas (100) | Barra Fija | 0 Kg | 8 | 80 × 8 = **640 Kg** |
  | Dominadas (100) | Peso Añadido | 10 Kg | 6 | (80 + 10) × 6 = **540 Kg** |
  | Sentadilla Búlgara (85) | Mancuerna | 20 Kg (total) | 10 | (68 + 20) × 10 = **880 Kg** |
  | Sentadilla Búlgara (85) | Peso Corporal | 0 Kg | 12 | 68 × 12 = **816 Kg** |
  | Zancadas (85) | Barra | 40 Kg | 10 | (68 + 40) × 10 = **1,080 Kg** |
  | Crunch Abdominal (35) | Peso Corporal | 0 Kg | 15 | 28 × 15 = **420 Kg** |

- **Pasos:**
  1. Registrar una serie por fila en una sesión del 2026-10-14 y cerrar la sesión.
  2. Leer en el resumen post-sesión el tonelaje de cada ejercicio e implemento.
- **Resultado esperado:** el tonelaje de cada serie coincide con la última columna, y el tonelaje total de la sesión es su suma.

### QA-02: Los implementos que no suman peso corporal cuentan solo la carga externa

- **Tipo:** Negativo
- **Prioridad:** Alta
- **Cubre:** CA-49.07, Reglas 3 y 4
- **Datos de prueba:** peso vigente 80.0 Kg.

  | Ejercicio (%) | Implemento | Carga registrada | Reps | Tonelaje esperado |
  | ------------- | ---------- | ---------------- | ---- | ----------------- |
  | Crunch Abdominal (35) | Polea | 30 Kg | 12 | **360 Kg** (sin peso corporal) |
  | Crunch Abdominal (35) | Máquina | 25 Kg | 12 | **300 Kg** (sin peso corporal) |
  | Dominadas (100) | Máquina (asistida) | sin carga capturable | 10 | **0 Kg** |

- **Pasos:**
  1. Registrar una serie por fila y cerrar la sesión.
  2. Leer el tonelaje en el resumen post-sesión y consultar la pantalla de 1RM.
- **Resultado esperado:**
  - El tonelaje coincide con la tabla, igual que antes de esta historia.
  - Dominadas en Máquina (asistida) no genera tarjeta de 1RM ni aunque se haga a 10 repeticiones con RIR 1.

### QA-03: Ejercicios con 0 % e isométricos no cambian

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-49.07, Regla 4
- **Datos de prueba:**

  | Ejercicio | Tipo | Carga | Reps / segundos | Resultado esperado |
  | --------- | ---- | ----- | --------------- | ------------------ |
  | Press de Banca (0 %) | Barra | 60 Kg | 10 reps | Tonelaje **600 Kg**, igual que hoy |
  | Plancha Lateral | Isométrico | 0 Kg | 45 s | Tonelaje igual que hoy; no suma peso corporal |

- **Pasos:**
  1. Abrir el detalle de "Plancha Lateral" en el Diccionario.
  2. Registrar las dos series y cerrar la sesión.
- **Resultado esperado:**
  - El detalle de "Plancha Lateral" no muestra el campo "Peso corporal movido".
  - Los tonelajes coinciden con la tabla.

### QA-04: La carga real es la misma en todas las vistas de tonelaje

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-49.01, Regla 5
- **Precondiciones:** Una sola sesión, del 2026-10-14, con Dominadas en Barra Fija de 3 series (8, 8 y 7 repeticiones), es decir 23 × 80 = **1,840 Kg**, y Remo con Barra a 30 Kg de 3 series (10, 9 y 8 repeticiones), es decir **810 Kg**.
- **Pasos:**
  1. Revisar el resumen post-sesión.
  2. Revisar el tonelaje en el panel de Métricas.
  3. Revisar Volumen por grupo muscular.
  4. Revisar Tendencia.
  5. Revisar la evolución del tonelaje.
- **Resultado esperado:**
  - El resumen muestra un total de **2,650 Kg**: 1,840 de Dominadas y 810 de Remo.
  - Las demás vistas reflejan esos mismos valores agregados por su criterio (grupo muscular, período).
  - Las Dominadas aportan 1,840 Kg, no 0, en todas las vistas.

### QA-05: La alerta de caída de tonelaje usa la carga real

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-49.01, Regla 5
- **Datos de prueba:** grupo muscular de espalda, peso vigente 80.0 Kg en los dos microciclos.

  | Microciclo | Dominadas Barra Fija | Remo con Barra | Tonelaje del grupo |
  | ---------- | -------------------- | -------------- | ------------------ |
  | Anterior | 3 × 8 → 1,920 Kg | 600 Kg | **2,520 Kg** |
  | Actual | 3 × 6 → 1,440 Kg | 600 Kg | **2,040 Kg** |

- **Pasos:**
  1. Completar los dos microciclos con esos datos.
  2. Revisar el Centro de Alertas y el detalle de la alerta.
- **Resultado esperado:**
  - Se emite la alerta de caída de tonelaje: −19.0 %, por encima del umbral de 10 % y por debajo del de crisis de 20 %.
  - El detalle muestra los tonelajes con carga real (2,520 → 2,040).
  - Contraste: con el cálculo anterior (600 → 600) no habría alerta.

### QA-06: Cada sesión usa el peso vigente en su fecha

- **Tipo:** Borde
- **Prioridad:** Alta
- **Cubre:** CA-49.06, Regla 1
- **Datos de prueba:** Dominadas en Barra Fija, 8 repeticiones por sesión.

  | Fecha de la sesión | Historial de peso aplicable | Peso vigente | Tonelaje esperado |
  | ------------------ | --------------------------- | ------------ | ----------------- |
  | 2026-10-05 | 78.0 (02/09) | 78.0 Kg | **624 Kg** |
  | 2026-10-10 | 78.0 (02/09) y 80.0 (10/10, mismo día) | 80.0 Kg | **640 Kg** |
  | 2026-10-14 | 80.0 (10/10) | 80.0 Kg | **640 Kg** |
  | 2026-10-10 con dos registros ese día, 79.0 y luego 80.0 | el último del día | 80.0 Kg | **640 Kg** |

- **Pasos:**
  1. Registrar las sesiones en sus fechas.
  2. Leer su tonelaje en el historial o el resumen.
  3. Registrar un peso nuevo de 82.0 Kg el 2026-10-22.
  4. Volver a leer el tonelaje de las tres sesiones.
- **Resultado esperado:**
  - Los tonelajes coinciden con la tabla.
  - Registrar 82.0 Kg no cambia ninguno: sigue 624, 640 y 640. Solo afectará a sesiones del 22/10 en adelante.

### QA-07: El 1RM se calcula sobre la carga real y muestra la fuerza relativa

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-49.02, CA-49.03, Reglas 6 y 7
- **Datos de prueba:** serie de referencia de 10 repeticiones con RIR 1, en sesión del 2026-10-14 (peso vigente 80.0 Kg).

  | Ejercicio · implemento | Carga real | 1RM (Kg · Lb) | Fuerza relativa |
  | ---------------------- | ---------- | ------------- | --------------- |
  | Dominadas · Barra Fija | 80.0 | **106.7 Kg · 235.2 Lb** | **1.33×** |
  | Dominadas · Peso Añadido 10 Kg | 90.0 | **120.0 Kg · 264.6 Lb** | **1.50×** |
  | Sentadilla Búlgara · Mancuerna 20 Kg | 88.0 | **117.4 Kg · 258.7 Lb** | **1.47×** |

- **Pasos:**
  1. Registrar cada serie de referencia.
  2. Abrir Métricas y luego "1RM estimado".
- **Resultado esperado:**
  - Cada par tiene su tarjeta con los valores de la tabla.
  - Bajo el 1RM aparece "N.NN× tu peso corporal" y "con 80.0 Kg el 14/10/2026".
  - Dominadas en Barra Fija aparece. Antes de esta historia no generaba 1RM.

### QA-08: El 1RM sigue siendo el máximo histórico, con su fecha

- **Tipo:** Borde
- **Prioridad:** Alta
- **Cubre:** CA-49.02, Regla 6
- **Datos de prueba:** Dominadas en Barra Fija, 10 repeticiones con RIR 1.

  | Sesión | Peso vigente | 1RM de la serie | Récord mostrado |
  | ------ | ------------ | --------------- | --------------- |
  | 2026-10-05 | 78.0 | 104.0 Kg | 104.0 Kg · fecha 05/10/2026 |
  | 2026-10-14 | 80.0 | 106.7 Kg | **106.7 Kg · fecha 14/10/2026** (sube) |
  | 2026-10-22, con peso registrado de 77.0 | 77.0 | 102.7 Kg | **106.7 Kg · fecha 14/10/2026** (se conserva) |
  | 2026-10-22, serie de 9 repeticiones o RIR 2 | — | no califica | sin cambios |

- **Pasos:**
  1. Registrar las series en orden.
  2. Consultar la tarjeta tras cada sesión.
- **Resultado esperado:** el récord y su fecha coinciden con la última columna en cada paso.

### QA-09: La fuerza relativa usa el peso de la fecha del 1RM, no el actual

- **Tipo:** Borde
- **Prioridad:** Alta
- **Cubre:** CA-49.03, CA-49.06, Reglas 1 y 7
- **Datos de prueba:**

  | Campo | Valor | Nota |
  | ----- | ----- | ---- |
  | Ejercicio | Press de Banca (0 %) · Barra | Aísla el uso de la fecha |
  | Serie de referencia | 70 Kg × 10 reps, RIR 1 | Sesión del 2026-10-05 |
  | 1RM | 93.4 Kg | 70 × 1.33369 |
  | Peso vigente el 05/10 | 78.0 Kg | |
  | Peso actual | 80.0 Kg | |

- **Pasos:**
  1. Registrar la serie el 2026-10-05.
  2. Consultar la tarjeta de 1RM con el peso actual en 80.0 Kg.
- **Resultado esperado:**
  - Muestra **"1.20× tu peso corporal"**, "con 78.0 Kg el 05/10/2026".
  - No muestra 1.17×, que sería el valor con el peso actual.

### QA-10: "Peso corporal movido" en el Diccionario, con los valores precargados

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-49.04, Regla 2
- **Datos de prueba:**

  | Ejercicio | Valor esperado |
  | --------- | -------------- |
  | Dominadas | 100 % |
  | Sentadilla Búlgara | 85 % |
  | Zancadas (Lunges) | 85 % |
  | Crunch Abdominal | 35 % |
  | Press de Banca | 0 % |
  | Ejercicio nuevo "Fondos en Banco", no isométrico | 0 % |

- **Pasos:**
  1. En una instalación fresca, abrir el detalle de cada ejercicio de la tabla.
  2. Crear "Fondos en Banco", abrir su detalle, cambiar el valor a 70 y guardar.
- **Resultado esperado:**
  - Cada detalle muestra "Peso corporal movido" con el valor de la tabla.
  - "Fondos en Banco" nace en 0 % y queda en 70 % tras guardar, con una confirmación breve.

### QA-11: Validación del porcentaje

- **Tipo:** Negativo
- **Prioridad:** Media
- **Cubre:** CA-49.05, Regla 2
- **Datos de prueba:**

  | Valor | Resultado esperado |
  | ----- | ------------------ |
  | 0 | Válido (límite inferior) |
  | 100 | Válido (límite superior) |
  | -1 | Error en línea; no se guarda |
  | 101 | Error en línea; no se guarda |
  | 50.5 | Error en línea (decimal); no se guarda |
  | (vacío) | Error en línea; no se guarda; el ejercicio conserva su valor anterior |

- **Pasos:**
  1. En el detalle de Sentadilla Búlgara, introducir cada valor e intentar guardar.
- **Resultado esperado:** según la tabla. Ningún ejercicio queda sin porcentaje.

### QA-12: Cambiar el porcentaje recalcula el tonelaje y el 1RM del ejercicio

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-49.08, Regla 8 (disparador relanzable)
- **Precondiciones:** Sentadilla Búlgara al 85 %, con dos sesiones en Mancuerna 20 Kg × 10 repeticiones con RIR 1:
  - 2026-10-05 (78.0 Kg): (66.3 + 20) × 10 = 863 Kg; 1RM 115.1 Kg
  - 2026-10-14 (80.0 Kg): 880 Kg; 1RM 117.4 Kg, que es el récord
- **Datos de prueba:**

  | Campo | Antes | Después |
  | ----- | ----- | ------- |
  | Porcentaje | 85 % | 80 % |
  | Tonelaje 05/10 | 863 Kg | (62.4 + 20) × 10 = **824 Kg** |
  | Tonelaje 14/10 | 880 Kg | (64 + 20) × 10 = **840 Kg** |
  | 1RM (récord) | 117.4 Kg · 1.47× | **112.0 Kg · 1.40×**, del 14/10 (baja) |

- **Pasos:**
  1. Cambiar el porcentaje de Sentadilla Búlgara a 80 y guardar.
  2. Revisar el tonelaje de ambas sesiones en el historial y en Métricas.
  3. Revisar la tarjeta de 1RM.
  4. Guardar de nuevo 80 sin cambiarlo.
- **Resultado esperado:**
  - Los valores pasan a la columna "Después" en todas las vistas.
  - El 1RM baja y la fuerza relativa se actualiza.
  - Guardar el mismo valor otra vez no produce ningún cambio adicional.

### QA-13: El motor de decisión produce el mismo resultado

- **Tipo:** Borde
- **Prioridad:** Alta
- **Cubre:** CA-49.09, Regla 9
- **Datos de prueba:**

  | Ejercicio · implemento | Datos de la sesión | Lo que se comprueba |
  | ---------------------- | ------------------ | ------------------- |
  | Dominadas · Barra Fija | 3 series de 12, 12 y 13 reps | La clasificación y la señal siguen basadas en repeticiones (RF-31) |
  | Sentadilla Búlgara · Mancuerna 20 Kg | 3 series de 6, 7 y 6 reps | La señal "bajar peso" aparece igual que hoy |
  | Sentadilla Búlgara · Mancuerna 20 Kg | sesión siguiente | La carga prescrita y precargada en el registro de serie es **20 Kg**, no 88 Kg |

- **Pasos:**
  1. Registrar y cerrar las sesiones.
  2. Revisar el resumen, las señales, el estado de progresión y la vista previa de la sesión siguiente.
- **Resultado esperado:** las clasificaciones, señales, contadores hacia la subida, la meseta y las cargas prescritas son idénticos a los de la misma secuencia sin esta historia. Solo cambia el tonelaje mostrado.

### QA-14: La serie sigue mostrando la carga registrada

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-49.01, Regla 9
- **Precondiciones:** La sesión de QA-01 cerrada.
- **Pasos:**
  1. Abrir el detalle de esa sesión pasada.
- **Resultado esperado:**
  - Cada serie muestra la carga tecleada: 0 Kg en Barra Fija y Peso Corporal, 10 Kg de lastre en Peso Añadido, 20 Kg en Mancuerna.
  - Solo los tonelajes reflejan la carga real.

### QA-15: Corregir la última serie recalcula la carga real y el 1RM

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-49.02 (disparador de corrección de `HU-47`)
- **Precondiciones:** Sesión activa del 2026-10-22 con peso vigente de 80.0 Kg. Última serie registrada: Dominadas · Barra Fija, 8 repeticiones con RIR 1. Sin 1RM previo en ese par.
- **Pasos:**
  1. Corregir la serie a 10 repeticiones con RIR 1.
  2. Consultar la pantalla de 1RM.
  3. Corregirla de nuevo a 9 repeticiones.
- **Resultado esperado:**
  - Tras el paso 1 aparece la tarjeta con 106.7 Kg y 1.33×.
  - Tras el paso 3 la tarjeta desaparece, porque la serie ya no califica.
  - El tonelaje de la serie pasa de 640 a 800 y luego a 720 Kg.

### QA-16: El respaldo conserva el porcentaje y la fecha del 1RM

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-49.10
- **Precondiciones:** Estado final de QA-12 (Sentadilla Búlgara al 80 %) y "Fondos en Banco" al 70 % (QA-10).
- **Pasos:**
  1. Exportar el respaldo.
  2. Importarlo en una instalación fresca.
  3. Revisar el Diccionario, la pantalla de 1RM y los tonelajes.
- **Resultado esperado:**
  - Sentadilla Búlgara está al 80 % y "Fondos en Banco" al 70 %.
  - El 1RM de Búlgara · Mancuerna es 112.0 Kg, 1.40×, "con 80.0 Kg el 14/10/2026".
  - Los tonelajes (824 y 840 Kg) son idénticos a los del dispositivo de origen.

---

**Total de casos:** 16
