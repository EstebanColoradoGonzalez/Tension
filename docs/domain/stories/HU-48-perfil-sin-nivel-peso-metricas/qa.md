# Casos de Prueba — Historia #48: Perfil sin nivel de experiencia y peso corporal en Métricas

**Historia:** [historia.md](./historia.md) | **Autor:** esteban.colorado | **Fecha:** 2026-10-22

## Alcance de las Pruebas

Se prueba:
- que el nivel de experiencia desaparezca del registro inicial (A1), del perfil (C1), del resto de pantallas y del respaldo;
- la sección "Peso corporal" de Métricas (G1): peso actual y fecha, variación del período con su selector, IMC secundario sin categoría y gráfico de peso junto al tonelaje;
- el registro rápido de peso: guardar, cancelar, validaciones y doble guardado;
- la nota de peso desactualizado y los estados de datos insuficientes;
- que nada de lo anterior altere el motor de decisión.

**Fuera de alcance (no se prueban aquí):**
- el cálculo del tonelaje con carga real (`HU-49`): el gráfico se verifica con el tonelaje vigente del sistema;
- el historial de peso como pantalla (`HU-02`), salvo que muestre las entradas creadas desde Métricas;
- la migración de bases o respaldos anteriores (instalación fresca, ADR-019).

## Precondiciones y Datos Base

- **Fecha de referencia ("hoy"):** 2026-10-22.
- **Instalación fresca** con esta historia aplicada.
- **Perfil base:** peso 78.5 Kg, altura 1.78 m.
- **Historial de peso base (B1):**
  - 2026-09-02 → 77.2 Kg (registro inicial)
  - 2026-10-01 → 77.9 Kg
  - 2026-10-20 → 78.5 Kg
- **Historial alternativo (B2, instalación fresca):** solo el registro inicial, 2026-10-22 → 78.5 Kg.
- **Sesiones base:** dos sesiones cerradas en las últimas 4 semanas (2026-10-08 y 2026-10-15), con tonelaje distinto de cero.
- **Inicio de los períodos desde "hoy":**
  - 4 semanas → 2026-09-24
  - 8 semanas → 2026-08-27
  - 12 semanas → 2026-07-30
- **IMC esperado con el perfil base:** 78.5 ÷ 1.78² = 24.78 → **24.8**.
- **Mensaje de error de peso (existente en el perfil):** "El peso debe ser un valor positivo".

## Matriz de Cobertura

| Criterio / Regla | Casos |
| --- | --- |
| CA-48.01 — Registro inicial sin nivel de experiencia | QA-01, QA-02 |
| CA-48.02 — Perfil sin nivel de experiencia | QA-03, QA-04 |
| CA-48.03 — Sección "Peso corporal" en Métricas | QA-05, QA-06, QA-07 |
| CA-48.04 — Registro rápido de peso | QA-08, QA-09, QA-11 |
| CA-48.05 — Validación del registro rápido | QA-09, QA-10 |
| CA-48.06 — Peso desactualizado | QA-12 |
| CA-48.07 — Datos insuficientes | QA-06, QA-13 |
| CA-48.08 — Sin rastro del nivel en el respaldo | QA-14 |
| Regla 1 — El nivel deja de existir | QA-01, QA-03, QA-04, QA-14 |
| Regla 2 — Perfil con peso y altura obligatorios > 0 | QA-02, QA-03 |
| Regla 3 — Peso actual = última entrada | QA-05, QA-08, QA-11 |
| Regla 4 — Variación del período | QA-06 |
| Regla 5 — IMC sin categoría | QA-05, QA-08 |
| Regla 6 — Registro rápido = entrada del historial | QA-08, QA-11 |
| Regla 7 — Aviso de peso desactualizado | QA-12 |
| Regla 8 — El gráfico usa el tonelaje del sistema | QA-07, QA-13 |
| Regla 9 — El motor de decisión no cambia | QA-15 |

---

## Casos de Prueba

### QA-01: El registro inicial pide solo peso y altura

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-48.01, Regla 1
- **Precondiciones:** Instalación fresca, primera apertura de la aplicación.
- **Datos de prueba:**

  | Campo | Valor | Nota |
  | ----- | ----- | ---- |
  | Peso corporal | 78.5 | Kg |
  | Altura | 1.78 | m |

- **Pasos:**
  1. Abrir la aplicación por primera vez.
  2. Revisar todos los campos del formulario "Configura tu perfil".
  3. Introducir peso y altura y pulsar "Registrar".
  4. Abrir el historial de peso.
- **Resultado esperado:**
  - El formulario muestra solo los campos "Peso corporal" (Kg) y "Altura" (m).
  - No hay campo, selector, opción ni texto de nivel de experiencia, ni "Principiante", "Intermedio" o "Avanzado".
  - "Registrar" se habilita al completar los dos campos y crea el perfil.
  - El historial de peso muestra 78.5 Kg con fecha 22/10/2026, etiquetado como "Registro inicial".

### QA-02: "Registrar" queda deshabilitado mientras peso o altura no sean válidos

- **Tipo:** Negativo
- **Prioridad:** Media
- **Cubre:** CA-48.01, Regla 2
- **Datos de prueba:**

  | Peso | Altura | Resultado esperado |
  | ---- | ------ | ------------------ |
  | 78.5 | (vacío) | Deshabilitado |
  | (vacío) | 1.78 | Deshabilitado |
  | 0 | 1.78 | Deshabilitado, con error de peso |
  | 78.5 | 0 | Deshabilitado, con error de altura |
  | 0.1 | 0.1 | Habilitado (límite mínimo válido) |

- **Pasos:**
  1. En el registro inicial, introducir cada combinación.
  2. Observar el estado del botón "Registrar" y los errores en línea.
- **Resultado esperado:** según la tabla. La habilitación no depende de ningún otro campo.

### QA-03: Editar el perfil sin nivel de experiencia

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-48.02, Reglas 1 y 2
- **Precondiciones:** Perfil base.
- **Datos de prueba:**

  | Campo | Valor | Nota |
  | ----- | ----- | ---- |
  | Altura | 1.80 | Cambio de 1.78 a 1.80 |

- **Pasos:**
  1. Abrir Ajustes y luego "Editar perfil".
  2. Revisar los campos disponibles.
  3. Cambiar la altura y pulsar "Guardar".
- **Resultado esperado:**
  - El perfil muestra solo "Peso corporal" y "Altura", más el enlace "Ver historial de peso".
  - No hay campo de nivel de experiencia.
  - El cambio se guarda con el mensaje de guardado existente.

### QA-04: El nivel de experiencia no aparece en ninguna otra pantalla

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-48.02, Regla 1
- **Pasos:**
  1. Recorrer Inicio, Métricas (incluidos Volumen, Tendencia y 1RM), Ajustes, Diccionario, Plan y Centro de Alertas.
- **Resultado esperado:** ninguna pantalla muestra, menciona ni permite elegir un nivel de experiencia.

### QA-05: La sección "Peso corporal" muestra peso actual, fecha e IMC sin categoría

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-48.03, Reglas 3 y 5
- **Precondiciones:** Historial B1.
- **Datos de prueba:**

  | Peso actual | Altura | IMC esperado | Nota |
  | ----------- | ------ | ------------ | ---- |
  | 78.5 Kg | 1.78 m | 24.8 | Perfil base |
  | 110.0 Kg | 1.70 m | 38.1 | Extremo alto: sin etiqueta "obesidad" |
  | 50.0 Kg | 1.80 m | 15.4 | Extremo bajo: sin etiqueta "bajo peso" |

- **Pasos:**
  1. Abrir Métricas y localizar la sección "Peso corporal".
  2. Para las filas 2 y 3, ajustar peso y altura desde el perfil y volver a Métricas.
- **Resultado esperado:**
  - Se muestra "Peso actual" con el último peso en Kg, "Registrado el 20/10/2026" en la fila 1, y el IMC con un decimal como dato secundario.
  - En ningún caso aparece una categoría ("normal", "sobrepeso", "obesidad", "bajo peso") ni un color o mensaje de valoración.
  - El IMC lleva una descripción breve que aclara que es un dato de contexto que no distingue músculo de grasa.
  - Cada indicador muestra etiqueta, valor, unidad y descripción.

### QA-06: La variación del peso responde al período seleccionado

- **Tipo:** Borde
- **Prioridad:** Alta
- **Cubre:** CA-48.03, CA-48.07, Regla 4
- **Precondiciones:** Historial B1, salvo donde la tabla indique otro.
- **Datos de prueba:**

  | Período | Peso de referencia (vigente al inicio) | Resultado esperado |
  | ------- | -------------------------------------- | ------------------ |
  | 4 semanas (desde 24/09) | 77.2 Kg (del 02/09) | **+1.3 Kg** |
  | 8 semanas (desde 27/08) | ninguno anterior | **Sin datos suficientes** |
  | 12 semanas (desde 30/07) | ninguno anterior | **Sin datos suficientes** |
  | 4 semanas con un registro añadido de 76.9 Kg el 22/10 | 77.2 Kg | **−0.3 Kg** |
  | 4 semanas con un registro añadido de 77.2 Kg el 22/10 | 77.2 Kg | **0.0 Kg**, un valor real con datos suficientes, no "Sin datos suficientes" |

- **Pasos:**
  1. En Métricas, cambiar el período con el selector común y leer "Variación del peso".
  2. Para las filas 4 y 5, registrar el peso indicado y volver a leer con 4 semanas.
- **Resultado esperado:** según la tabla. El valor lleva signo y unidad, y la etiqueta indica el período ("Últimas 4 semanas").

### QA-07: El gráfico muestra el peso por registro y el tonelaje por semana en el mismo eje temporal

- **Tipo:** Estado UI
- **Prioridad:** Media
- **Cubre:** CA-48.03, Regla 8
- **Precondiciones:** Historial B1. En lugar de las sesiones base, estas sesiones cerradas con su tonelaje:

  | Sesión | Semana ISO (lunes) | Tonelaje |
  | ------ | ------------------ | -------- |
  | 2026-10-08 | 2026-10-05 | 1,400 Kg |
  | 2026-10-13 | 2026-10-12 | 1,200 Kg |
  | 2026-10-15 | 2026-10-12 | 1,500 Kg |
  | 2026-10-21 | 2026-10-19 (semana en curso) | 1,300 Kg |

- **Pasos:**
  1. En Métricas, con 4 semanas (desde 24/09), observar el gráfico "Peso corporal y tonelaje".
  2. Cambiar a 12 semanas.
- **Resultado esperado:**
  - **Serie de peso:** un punto por cada registro dentro del período, el 01/10 (77.9 Kg) y el 20/10 (78.5 Kg).
  - **Serie de tonelaje:** un punto por semana con sesiones: semana del 05/10 → **1,400 Kg**; semana del 12/10 → **2,700 Kg** (suma de dos sesiones); semana del 19/10, en curso → **1,300 Kg**.
  - Las semanas del 21/09 y del 28/09, sin sesiones, no tienen punto de tonelaje ni se dibujan como cero.
  - Las dos series comparten eje temporal y período, y la leyenda distingue cada serie y su unidad.
  - Al cambiar a 12 semanas, las dos series se recalculan juntas para el nuevo período.

### QA-08: Registrar peso desde Métricas crea una entrada del historial y actualiza la sección

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-48.04, Reglas 3, 5 y 6
- **Precondiciones:** Historial B1.
- **Datos de prueba:**

  | Campo | Valor | Nota |
  | ----- | ----- | ---- |
  | Peso | 79.0 | Kg |

- **Pasos:**
  1. En la sección "Peso corporal", pulsar "Registrar peso".
  2. Comprobar el valor precargado del diálogo.
  3. Introducir 79.0 y pulsar "Guardar".
  4. Abrir el perfil y el historial de peso.
- **Resultado esperado:**
  - El diálogo se abre precargado con 78.5 Kg.
  - Al guardar aparece una confirmación breve.
  - La sección muestra de inmediato "79.0 Kg", "Registrado el 22/10/2026", variación de 4 semanas **+1.8 Kg**, IMC **24.9** y el nuevo punto en el gráfico.
  - El perfil muestra 79.0 Kg como peso actual.
  - El historial de peso muestra la entrada 22/10/2026 · 79.0 Kg como la más reciente.

### QA-09: Cancelar el registro rápido no crea ninguna entrada

- **Tipo:** Negativo
- **Prioridad:** Media
- **Cubre:** CA-48.04, CA-48.05
- **Precondiciones:** Historial B1.
- **Pasos:**
  1. Pulsar "Registrar peso", cambiar el valor a 80.0 y pulsar "Cancelar".
- **Resultado esperado:**
  - El diálogo se cierra.
  - El peso actual sigue en 78.5 Kg y la sección no cambia.
  - El historial de peso conserva sus 3 entradas.

### QA-10: Validación del peso en el diálogo

- **Tipo:** Negativo
- **Prioridad:** Alta
- **Cubre:** CA-48.05
- **Datos de prueba:**

  | Valor introducido | Resultado esperado |
  | ----------------- | ------------------ |
  | 0 | Error "El peso debe ser un valor positivo"; "Guardar" deshabilitado |
  | -2 | Mismo error; "Guardar" deshabilitado |
  | (vacío) | "Guardar" deshabilitado |
  | 0.1 | Válido (límite mínimo); "Guardar" habilitado |

- **Pasos:**
  1. Abrir el diálogo y comprobar que el teclado es numérico.
  2. Introducir cada valor.
- **Resultado esperado:** según la tabla. El mensaje y el criterio coinciden con los del peso en el perfil.

### QA-11: Dos registros rápidos el mismo día crean dos entradas

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-48.04, Reglas 3 y 6 (disparador relanzable)
- **Precondiciones:** Historial B1.
- **Datos de prueba:**

  | Registro | Valor |
  | -------- | ----- |
  | Primero | 79.0 Kg |
  | Segundo | 78.8 Kg |

- **Pasos:**
  1. Registrar 79.0 desde Métricas.
  2. Registrar 78.8 desde Métricas.
  3. Revisar la sección y el historial de peso.
- **Resultado esperado:**
  - El historial muestra dos entradas con fecha 22/10/2026, igual que si se hubiera actualizado el peso dos veces desde el perfil.
  - El peso actual es **78.8 Kg**, el último registrado.

### QA-12: Nota de peso desactualizado

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-48.06, Regla 7
- **Datos de prueba:**

  | Fecha del último peso | Días transcurridos | Resultado esperado |
  | --------------------- | ------------------ | ------------------ |
  | 2026-09-22 | 30 | Sin nota |
  | 2026-09-21 | 31 | Nota "Tu último peso es de hace 31 días" |
  | 2026-09-10 | 42 | Nota "Tu último peso es de hace 42 días" |

- **Pasos:**
  1. Con el último peso en cada fecha, abrir Métricas.
  2. En la fila de 42 días, pulsar el acceso de la nota y registrar 78.5.
  3. Abrir el Centro de Alertas.
- **Resultado esperado:**
  - La nota aparece según la tabla, como texto informativo en la sección.
  - El acceso de la nota abre el diálogo de registro rápido.
  - Tras guardar, la nota desaparece.
  - El Centro de Alertas no contiene ninguna alerta de peso y no se genera notificación.

### QA-13: Datos insuficientes en una instalación recién configurada

- **Tipo:** Estado UI
- **Prioridad:** Media
- **Cubre:** CA-48.07, Regla 8
- **Precondiciones:** Historial B2 (solo el registro inicial de hoy) y ninguna sesión registrada.
- **Pasos:**
  1. Abrir Métricas y revisar la sección "Peso corporal" con 4 semanas.
- **Resultado esperado:**
  - "Peso actual" muestra 78.5 Kg del 22/10/2026 y el IMC.
  - "Variación del peso" muestra "Sin datos suficientes", nunca "0 Kg".
  - El gráfico muestra la línea de peso con su único punto y comunica que no hay tonelaje en el período, sin barras ni línea en cero.

### QA-14: El respaldo no contiene el nivel y restaura el peso íntegro

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-48.08, Regla 1
- **Precondiciones:** Historial B1 más una entrada de 79.0 Kg del 22/10/2026 creada desde Métricas.
- **Pasos:**
  1. Exportar el respaldo desde Ajustes.
  2. Inspeccionar el contenido del respaldo exportado.
  3. Importarlo en una instalación fresca.
  4. Revisar el perfil, el historial de peso y la sección "Peso corporal".
- **Resultado esperado:**
  - El respaldo no contiene ningún dato de nivel de experiencia.
  - Tras importar, el perfil muestra 79.0 Kg y 1.78 m.
  - El historial muestra las 4 entradas con sus fechas.
  - La sección "Peso corporal" muestra los mismos valores que en el dispositivo de origen.

### QA-15: Registrar peso no cambia ninguna decisión del sistema

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** Regla 9
- **Precondiciones:** Una sesión del día por iniciar, con las cargas prescritas visibles en la vista previa.
- **Datos de prueba:**

  | Campo | Valor | Nota |
  | ----- | ----- | ---- |
  | Peso nuevo | 85.0 | Kg; cambio grande respecto a 78.5 |

- **Pasos:**
  1. Anotar las cargas prescritas, las señales de acción y las alertas activas.
  2. Registrar 85.0 Kg desde Métricas.
  3. Volver a revisar cargas, señales y alertas.
- **Resultado esperado:** las cargas prescritas, las señales y las alertas son idénticas antes y después del registro.

---

**Total de casos:** 15
