# Casos de Prueba — Historia #46: Progresión por rango de hipertrofia y sesión acotada a la categoría del día

**Historia:** [historia.md](./historia.md) | **Autor:** esteban.colorado | **Fecha:** 2026-10-22

## Alcance de las Pruebas

**Se prueba:**
- La lectura de cada sesión por mayoría de series del par ejercicio–implemento.
- La señal de bajar peso, en directo y al cerrar la sesión.
- La señal de subir peso tras 4 sesiones consecutivas en el tope con el mismo peso, y el avance «N de 4».
- Que el número de series del plan y el RIR no intervienen en la decisión.
- Las excepciones de peso corporal, isométricos y descarga.
- El umbral base de estancamiento por defecto en 10.
- La categoría Empuje / Jalón / Pierna de ejercicios y rutinas, con sus datos iniciales y su obligatoriedad.
- El filtro por categoría al añadir a la sesión y al asignar en el plan, el bloqueo de cambios en conflicto y la conservación en el respaldo.

**Queda fuera:**
- El valor del incremento de carga al subir, que se conserva como está.
- Los multiplicadores por dificultad y el criterio de meseta, salvo su valor base por defecto.
- La progresión por segundos de los isométricos.
- La migración de datos y la importación de respaldos de versiones anteriores (beta con instalación fresca).

## Precondiciones y Datos Base

- **Instalación:** fresca, con el perfil de El Ejecutante dado de alta y el plan por defecto sembrado.
- **Fecha de referencia:** 2026-10-22 (jueves). Las sesiones previas se datan en días anteriores.
- **Rutinas iniciales:**

  | Rutina | Día | Categoría esperada |
  | ------ | --- | ------------------ |
  | Push — Foco Deltoides Lateral y Medio | Lunes | Empuje |
  | Pull — Foco Dorsal Ancho | Martes | Jalón |
  | Lower — Foco Cuádriceps | Miércoles | Pierna |
  | Push — Foco Tríceps | Jueves | Empuje |
  | Pull — Foco Trapecios y Espalda Media | Viernes | Jalón |
  | Lower — Foco Isquiotibiales y Glúteo | Sábado | Pierna |

- **Par P1:** Press de Banca Plano con Barra, 3 series en el plan y último peso 60 kg.
- **Par P2:** Extensión de Tríceps sobre Cabeza con Mancuerna, 4 series en el plan y último peso 12 kg.
- **Par P3:** Dominadas con Peso Corporal. El ejercicio admite «Peso Añadido».
- **Ejercicio personalizado E-ISO:** «Plancha Frontal», isométrico de categoría Jalón, creado antes de las pruebas.
- **Ejercicio personalizado E-PC:** «Fondos en Banco», de peso corporal y categoría Empuje, que **no** admite «Peso Añadido».

## Matriz de Cobertura

| Criterio / Regla | Casos |
| ---------------- | ----- |
| CA-46.01 Lectura por mayoría | QA-01, QA-02 |
| CA-46.02 Bajar peso al cerrar | QA-03 |
| CA-46.03 Subir tras 4 sesiones y peso de referencia | QA-04, QA-05, QA-34, QA-35 |
| CA-46.04 Avance N de 4 | QA-06 |
| CA-46.05 Aviso en directo | QA-07, QA-08, QA-09, QA-36 |
| CA-46.06 Series del plan y RIR no intervienen | QA-04, QA-10 |
| CA-46.07 Umbral base 10 | QA-11, QA-12 |
| CA-46.08 Categoría del ejercicio | QA-13 |
| CA-46.09 Categoría de la rutina | QA-14, QA-15 |
| CA-46.10 Añadir solo de la categoría del día | QA-16, QA-17 |
| CA-46.11 Creado desde la sesión hereda la categoría | QA-18 |
| CA-46.12 Plan restringido por categoría | QA-19, QA-20 |
| CA-46.13 Categoría obligatoria | QA-21 |
| CA-46.14 Cambio de categoría en conflicto | QA-22, QA-23, QA-24 |
| CA-46.15 Peso corporal | QA-25, QA-26, QA-27 |
| CA-46.16 Isométricos excluidos | QA-28 |
| CA-46.17 Continuidad y mismo peso | QA-29, QA-30, QA-31, QA-34 |
| CA-46.18 Descarga | QA-32 |
| CA-46.19 Respaldo | QA-33 |
| RN-1 RIR no condiciona | QA-10 |
| RN-2 Mayoría estricta, series del plan no intervienen | QA-01, QA-02, QA-04 |
| RN-3 Bajar inmediato, sin cantidad | QA-03, QA-07 |
| RN-4 Subir tras 4 consecutivas con el mismo peso (peso mayor) | QA-04, QA-05, QA-29, QA-30, QA-34, QA-35, QA-36 |
| RN-5 Nunca bajar salvo < 8 | QA-05 |
| RN-6 Peso corporal | QA-25, QA-26, QA-27 |
| RN-7 Isométricos fuera | QA-28 |
| RN-8 Umbral 10, rango 3–15, multiplicadores | QA-11, QA-12 |
| RN-9 Categoría única | QA-13, QA-14 |
| RN-10 Abdomen = Jalón | QA-13 |
| RN-11 Categoría del día y heredada | QA-15, QA-16, QA-18 |
| RN-12 Plan respeta la categoría | QA-19, QA-20 |
| RN-13 Descarga sin señales | QA-32 |

---

## Casos de Prueba

### QA-01: La sesión se clasifica por la mayoría estricta de las series del par

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.01 / RN-2
- **Datos de prueba:**

  | Series registradas (reps) | Nº series | Clasificación esperada | Nota |
  | ------------------------- | :-------: | ---------------------- | ---- |
  | 12 · 12 · 11 | 3 | Tope del rango | 2 de 3 ≥ 12 |
  | 12 · 11 · 9 | 3 | Dentro del rango | Solo 1 de 3 ≥ 12 |
  | 6 · 7 · 9 | 3 | Por debajo del rango | 2 de 3 < 8 |
  | 12 · 12 · 12 · 10 | 4 | Tope del rango | 3 de 4 ≥ 12 |
  | 13 | 1 | Tope del rango | 1 de 1 |
  | 7 · 8 | 2 | Dentro del rango | 1 de 2 no es mayoría |

- **Pasos:**
  1. Iniciar la sesión del día con P1.
  2. Registrar las series de la fila, todas con 60 kg y RIR 1.
  3. Cerrar la sesión y abrir el resumen.
- **Resultado esperado:** el renglón de P1 refleja la clasificación de la tabla: «Bajar peso» si queda por debajo del rango, «Mantener — 1 de 4 sesiones en el tope» si queda en el tope sin sesiones previas, y «Mantener — dentro del rango 8–12» si queda dentro.

### QA-02: Empate sin mayoría en el tope ni por debajo

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-46.01
- **Datos de prueba:**

  | Series registradas (reps) | Nota |
  | ------------------------- | ---- |
  | 12 · 12 · 6 · 7 | 2 en el tope y 2 por debajo, sin mayoría estricta |

- **Pasos:**
  1. Registrar las 4 series de P2 con 12 kg.
  2. Cerrar la sesión.
- **Resultado esperado:** la sesión se clasifica **dentro del rango**. El resumen muestra «Mantener — dentro del rango 8–12», sin señal de bajar ni de subir.

### QA-03: Señal de bajar peso al cerrar, sin cantidad y con reinicio del conteo

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.02 / RN-3
- **Precondiciones:** P1 lleva 2 sesiones consecutivas en el tope con 60 kg.
- **Datos de prueba:**

  | Campo | Valor |
  | ----- | ----- |
  | Series | 6 · 7 · 9 con 60 kg |

- **Pasos:**
  1. Registrar las series y cerrar la sesión.
  2. Abrir el resumen post-sesión.
  3. Iniciar la siguiente sesión que incluya Press de Banca Plano.
- **Resultado esperado:**
  - El resumen muestra «Bajar peso» sin ningún peso prescrito.
  - En la sesión siguiente, el peso propuesto lleva la indicación de bajar peso.
  - El conteo hacia la subida de P1 vuelve a 0.

### QA-04: Señal de subir en la cuarta sesión consecutiva, con un ejercicio de 3 series

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.03, CA-46.06 / RN-2, RN-4
- **Datos de prueba:**

  | Sesión | Series de P1 (60 kg) | Resumen esperado |
  | :----: | -------------------- | ---------------- |
  | 1 | 12 · 12 · 10 | Mantener — 1 de 4 sesiones en el tope |
  | 2 | 12 · 13 · 11 | Mantener — 2 de 4 sesiones en el tope |
  | 3 | 12 · 12 · 12 | Mantener — 3 de 4 sesiones en el tope |
  | 4 | 13 · 12 · 9 | Subir carga → 60 kg + el incremento vigente |

- **Pasos:**
  1. Cerrar las 4 sesiones de la tabla en orden.
  2. Iniciar la sesión siguiente con Press de Banca Plano.
- **Resultado esperado:**
  - Solo la cuarta sesión propone subir.
  - El peso propuesto para la sesión siguiente es el de «Subir carga».
  - Que el plan prescriba 3 series no impide la señal.

### QA-05: Si no se sube, la señal de subir se mantiene y nunca se propone bajar

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-46.03 / RN-5
- **Precondiciones:** P1 ya recibió «Subir carga» tras 4 sesiones con 60 kg.
- **Datos de prueba:**

  | Campo | Valor |
  | ----- | ----- |
  | Sesión 5 | 12 · 12 · 13 con 60 kg |

- **Pasos:**
  1. Cerrar la quinta sesión sin subir el peso.
- **Resultado esperado:** el resumen vuelve a mostrar «Subir carga → …» y en ningún caso propone bajar peso.

### QA-06: El avance N de 4 se ve en el resumen y junto al peso propuesto

- **Tipo:** Estado UI
- **Prioridad:** Media
- **Cubre:** CA-46.04
- **Precondiciones:** P2 lleva 2 sesiones consecutivas en el tope con 12 kg.
- **Pasos:**
  1. Abrir el resumen de la última sesión.
  2. Iniciar la siguiente sesión que incluya Extensión de Tríceps sobre Cabeza.
- **Resultado esperado:** el resumen muestra «Mantener — 2 de 4 sesiones en el tope», y la sesión siguiente muestra el mismo avance junto al peso propuesto.

### QA-07: Aviso en directo desde la primera serie por debajo de 8

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.05 / RN-3
- **Datos de prueba:**

  | Serie | Reps | Aviso esperado |
  | :---: | :--: | -------------- |
  | 1 | 7 | «Baja el peso en la próxima serie» |
  | 1 | 8 | Ninguno (límite inferior del rango) |

- **Pasos:**
  1. En una sesión nueva, registrar la primera serie de P1 con 60 kg y las repeticiones de la fila.
- **Resultado esperado:**
  - El aviso aparece o no según la tabla, sin bloquear el registro.
  - La serie queda registrada tal como se introdujo.
  - El formulario de la serie siguiente sigue utilizable.

### QA-08: Aviso informativo en directo en el tope del rango

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-46.05
- **Precondiciones:** P1 lleva 1 sesión consecutiva en el tope con 60 kg.
- **Datos de prueba:**

  | Reps | Aviso esperado |
  | :--: | -------------- |
  | 11 | Ninguno |
  | 12 | «Tope del rango — cuenta para la subida (sesión 2 de 4)» |
  | 15 | Mismo aviso informativo |

- **Pasos:**
  1. Registrar una serie de P1 con 60 kg y las repeticiones de la fila.
- **Resultado esperado:** el aviso de la tabla es **informativo**. En ningún momento aparece una orden de subir el peso durante la sesión.

### QA-09: Una serie entre 8 y 11 no genera aviso

- **Tipo:** Negativo
- **Prioridad:** Baja
- **Cubre:** CA-46.05
- **Datos de prueba:**

  | Reps |
  | :--: |
  | 8 |
  | 10 |
  | 11 |

- **Pasos:**
  1. Registrar una serie de P1 con cada valor.
- **Resultado esperado:** no aparece ningún aviso.

### QA-10: El RIR no condiciona la señal

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.06 / RN-1
- **Datos de prueba:**

  | Sesiones 1–4 de P2 (12 kg) | RIR de todas las series |
  | -------------------------- | :---------------------: |
  | 12 · 12 · 12 · 12 | 0 |
  | 12 · 12 · 12 · 12 | 3 |

- **Pasos:**
  1. Para cada variante, cerrar 4 sesiones consecutivas con los datos de la fila.
- **Resultado esperado:** en las dos variantes la cuarta sesión muestra «Subir carga». Ya no se exige un RIR medio de 2 o más.

### QA-11: Umbral base por defecto en 10 con su desglose

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.07 / RN-8
- **Pasos:**
  1. En una instalación fresca, completar el alta del perfil.
  2. Abrir Ajustes.
- **Resultado esperado:** el umbral base muestra **10 sesiones**, el desglose «Baja → 10 · Media → 15 · Alta → 20» y el rango permitido «3 a 15».

### QA-12: Límites del umbral base configurable

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-46.07 / RN-8
- **Datos de prueba:**

  | Acción desde el valor | Resultado esperado |
  | --------------------- | ------------------ |
  | Bajar desde 3 | No baja: control de disminuir deshabilitado |
  | Subir desde 15 | No sube: control de aumentar deshabilitado |
  | Subir desde 10 | 11, desglose 11 · 17 · 22 |

- **Pasos:**
  1. Llevar el umbral al valor inicial de la fila y aplicar la acción.
- **Resultado esperado:** el comportamiento de la tabla.

### QA-13: Los 38 ejercicios iniciales tienen su categoría y el abdomen es Jalón

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.08 / RN-9, RN-10
- **Datos de prueba:** la tabla de CA-46.08 completa (9 de Empuje, 18 de Jalón y 11 de Pierna). Muestra a revisar en pantalla:

  | Ejercicio | Categoría esperada |
  | --------- | ------------------ |
  | Elevación Lateral | Empuje |
  | Remo al Mentón | Jalón |
  | Crunch Abdominal | Jalón |
  | Press Pallof | Jalón |
  | Peso Muerto Rumano | Pierna |

- **Pasos:**
  1. Abrir el detalle de cada ejercicio del Diccionario.
- **Resultado esperado:** cada ejercicio muestra **una sola** categoría, que coincide con la tabla de CA-46.08, y ninguno queda sin categoría.

### QA-14: Las rutinas iniciales tienen su categoría

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.09 / RN-9
- **Pasos:**
  1. Abrir el plan de entrenamiento.
- **Resultado esperado:** cada rutina muestra la categoría de la tabla de datos base, independiente de su nombre.

### QA-15: La categoría del día sigue a la rutina reasignada temporalmente

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-46.09 / RN-11
- **Precondiciones:** hoy es jueves (Empuje).
- **Pasos:**
  1. Reasignar temporalmente la rutina de hoy a «Lower — Foco Cuádriceps».
  2. Iniciar la sesión y abrir «Añadir ejercicio».
- **Resultado esperado:** el listado muestra solo ejercicios de **Pierna** y la sesión se identifica como de categoría Pierna.

### QA-16: Añadir ejercicio muestra solo los de la categoría del día

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.10 / RN-11
- **Datos de prueba:**

  | Día (rutina) | Debe aparecer | No debe aparecer |
  | ------------ | ------------- | ---------------- |
  | Jueves (Empuje) | Elevación Lateral, Press Militar | Jalón al Pecho, Sentadilla Hack, Crunch Abdominal |
  | Martes (Jalón) | Face Pull, Press Pallof | Press Francés, Hip Thrust |
  | Miércoles (Pierna) | Zancadas (Lunges), Hip Thrust | Remo Horizontal, Aperturas |

- **Pasos:**
  1. Iniciar la sesión del día de la fila.
  2. Abrir «Añadir ejercicio» y recorrer el listado.
  3. Buscar por nombre cada ejercicio de la columna «No debe aparecer».
- **Resultado esperado:**
  - El listado indica «Solo ejercicios de {categoría}».
  - Muestra los ejercicios esperados.
  - Los de otras categorías no aparecen ni en el listado ni en la búsqueda.
  - Los ya presentes en la sesión siguen marcados como no añadibles.

### QA-17: Búsqueda sin resultados en la categoría del día

- **Tipo:** Estado UI
- **Prioridad:** Baja
- **Cubre:** CA-46.10
- **Datos de prueba:**

  | Día | Texto buscado |
  | --- | ------------- |
  | Jueves (Empuje) | «Sentadilla» |

- **Pasos:**
  1. Abrir «Añadir ejercicio» y escribir el texto.
- **Resultado esperado:** estado vacío «Ningún ejercicio de Empuje coincide.».

### QA-18: El ejercicio creado desde la sesión hereda la categoría del día

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.11 / RN-11
- **Datos de prueba:**

  | Campo | Valor |
  | ----- | ----- |
  | Día | Jueves (Empuje) |
  | Nombre | «Press Cerrado en Máquina» |

- **Pasos:**
  1. Desde «Añadir ejercicio», elegir «Crear ejercicio nuevo».
  2. Intentar cambiar la categoría.
  3. Completar el resto de campos obligatorios y guardar.
- **Resultado esperado:**
  - La categoría llega fijada en Empuje, con la línea «Categoría del día», y no se puede cambiar.
  - El ejercicio queda añadido a la sesión.
  - En el Diccionario aparece con categoría Empuje.

### QA-19: Asignar ejercicio a una rutina del plan solo ofrece los de su categoría

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.12 / RN-12
- **Pasos:**
  1. Abrir «Pull — Foco Dorsal Ancho» en el plan.
  2. Abrir el selector para asignar un ejercicio.
- **Resultado esperado:** el selector lista solo ejercicios de **Jalón**. No aparecen, por ejemplo, Press Militar ni Prensa Inclinada.

### QA-20: La alternativa de un puesto solo admite ejercicios de la categoría de la rutina

- **Tipo:** Negativo
- **Prioridad:** Media
- **Cubre:** CA-46.12 / RN-12
- **Pasos:**
  1. En «Push — Foco Deltoides Lateral y Medio», abrir el selector de alternativa de un puesto.
  2. Buscar «Jalón al Pecho».
- **Resultado esperado:** Jalón al Pecho no aparece. Solo se ofrecen ejercicios de Empuje, y el sistema no permite asignar uno de otra categoría.

### QA-21: La categoría es obligatoria al crear un ejercicio o una rutina

- **Tipo:** Negativo
- **Prioridad:** Alta
- **Cubre:** CA-46.13
- **Datos de prueba:**

  | Formulario | Nombre | Categoría |
  | ---------- | ------ | --------- |
  | Crear ejercicio desde el Diccionario | «Remo con Banda» | Sin elegir |
  | Crear rutina | «Torso Extra» | Sin elegir |

- **Pasos:**
  1. Completar el formulario de la fila sin elegir categoría.
  2. Pulsar Guardar.
- **Resultado esperado:** no se guarda y aparece el mensaje «Elige la categoría: Empuje, Jalón o Pierna». El selector no trae ninguna categoría preseleccionada.

### QA-22: No se puede cambiar la categoría de un ejercicio asignado a rutinas de otra categoría

- **Tipo:** Negativo
- **Prioridad:** Alta
- **Cubre:** CA-46.14
- **Datos de prueba:**

  | Ejercicio | Categoría actual | Nueva categoría |
  | --------- | ---------------- | --------------- |
  | Aperturas | Empuje | Jalón |

- **Pasos:**
  1. Abrir el detalle de Aperturas y cambiar la categoría a Jalón.
- **Resultado esperado:** el sistema impide el cambio y nombra las rutinas en conflicto: «Push — Foco Deltoides Lateral y Medio» y «Push — Foco Tríceps». La categoría sigue en Empuje.

### QA-23: Un ejercicio sin asignaciones en el plan cambia de categoría libremente

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-46.14
- **Datos de prueba:**

  | Ejercicio | Categoría actual | Nueva categoría |
  | --------- | ---------------- | --------------- |
  | Cruce de Polea Alta (fuera del plan) | Empuje | Jalón |

- **Pasos:**
  1. Cambiar la categoría desde el detalle.
- **Resultado esperado:** el cambio se guarda. Cruce de Polea Alta deja de aparecer al añadir ejercicio en un día de Empuje y aparece en un día de Jalón.

### QA-24: No se puede cambiar la categoría de una rutina con ejercicios de otra categoría

- **Tipo:** Negativo
- **Prioridad:** Media
- **Cubre:** CA-46.14
- **Pasos:**
  1. Editar «Lower — Foco Cuádriceps» y cambiar su categoría a Empuje.
  2. Repetir con una rutina nueva sin ejercicios asignados.
- **Resultado esperado:** en el primer caso el sistema lo impide e indica el conflicto con los ejercicios de Pierna asignados. En el segundo, el cambio se guarda.

### QA-25: Peso corporal nunca recibe la señal de bajar

- **Tipo:** Negativo
- **Prioridad:** Alta
- **Cubre:** CA-46.15 / RN-6
- **Datos de prueba:**

  | Par | Series (reps) |
  | --- | ------------- |
  | P3 Dominadas · Peso Corporal | 5 · 6 · 4 |

- **Pasos:**
  1. Registrar las series y cerrar la sesión.
- **Resultado esperado:** no aparece el aviso en directo de bajar peso ni la señal «Bajar peso» en el resumen.

### QA-26: Peso corporal con «Peso Añadido» admitido: propone añadir peso extra

- **Tipo:** Positivo
- **Prioridad:** Alta
- **Cubre:** CA-46.15 / RN-6
- **Datos de prueba:**

  | Sesión | Series de P3 con Peso Corporal (reps) |
  | :----: | ------------------------------------- |
  | 1 a 4 | 12 · 12 · 13 en cada una |

- **Pasos:**
  1. Cerrar las 4 sesiones consecutivas.
- **Resultado esperado:** las sesiones 1 a 3 muestran «Mantener — N de 4 sesiones en el tope» y la cuarta muestra «Añadir peso extra».

### QA-27: Peso corporal sin «Peso Añadido» y par con «Peso Añadido»

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-46.15 / RN-6
- **Datos de prueba:**

  | Par | Series | Resultado esperado |
  | --- | ------ | ------------------ |
  | E-PC Fondos en Banco · Peso Corporal | 4 sesiones de 12 · 12 · 12 | Ninguna señal de peso en ninguna sesión |
  | P3 Dominadas · Peso Añadido 10 kg | 5 · 6 · 7 | «Bajar peso» y aviso en directo desde la primera serie |

- **Pasos:**
  1. Registrar y cerrar las sesiones de cada fila.
- **Resultado esperado:** el de la tabla. El par con «Peso Añadido» sigue la regla completa sobre el peso añadido.

### QA-28: Isométricos fuera de la regla

- **Tipo:** Negativo
- **Prioridad:** Media
- **Cubre:** CA-46.16 / RN-7
- **Datos de prueba:**

  | Ejercicio | Series |
  | --------- | ------ |
  | E-ISO Plancha Frontal | 30 s · 35 s · 25 s |

- **Pasos:**
  1. Añadir E-ISO en un día de Jalón, registrar las series y cerrar.
- **Resultado esperado:** no hay avisos en directo ni señales de subir o bajar, y la progresión por segundos funciona como antes.

### QA-29: Una sesión dentro del rango rompe la racha hacia la subida

- **Tipo:** Borde
- **Prioridad:** Alta
- **Cubre:** CA-46.17 / RN-4
- **Datos de prueba:**

  | Sesión | Series de P1 (60 kg) | Resumen esperado |
  | :----: | -------------------- | ---------------- |
  | 1 | 12 · 12 · 12 | 1 de 4 |
  | 2 | 12 · 12 · 11 | 2 de 4 |
  | 3 | 11 · 10 · 12 | Mantener — dentro del rango (conteo a 0) |
  | 4 | 12 · 12 · 12 | 1 de 4 |

- **Pasos:**
  1. Cerrar las 4 sesiones en orden.
- **Resultado esperado:** el de la tabla. En la cuarta sesión no se propone subir.

### QA-30: Cambiar de peso reinicia la racha

- **Tipo:** Borde
- **Prioridad:** Alta
- **Cubre:** CA-46.17 / RN-4
- **Datos de prueba:**

  | Sesión | Peso | Series de P1 | Resumen esperado |
  | :----: | :--: | ------------ | ---------------- |
  | 1 | 60 kg | 12 · 12 · 12 | 1 de 4 |
  | 2 | 60 kg | 12 · 12 · 12 | 2 de 4 |
  | 3 | 62.5 kg | 12 · 12 · 12 | 1 de 4 |
  | 4 | 62.5 kg | 10 · 9 · 9 | Mantener — dentro del rango (conteo a 0) |

- **Pasos:**
  1. Cerrar las 4 sesiones en orden.
- **Resultado esperado:** el de la tabla.

### QA-31: Una sesión sin series del par no rompe ni suma a la racha

- **Tipo:** Borde
- **Prioridad:** Media
- **Cubre:** CA-46.17
- **Precondiciones:** P1 lleva 2 sesiones consecutivas en el tope con 60 kg.
- **Pasos:**
  1. Cerrar una sesión de Empuje en la que Press de Banca Plano queda con 0 series.
  2. Cerrar la siguiente sesión con P1 en 12 · 12 · 12 y 60 kg.
- **Resultado esperado:** el resumen de la segunda sesión muestra «Mantener — 3 de 4 sesiones en el tope».

### QA-32: Durante la descarga no hay señales y la racha queda congelada

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-46.18 / RN-13
- **Precondiciones:** P1 lleva 3 sesiones consecutivas en el tope con 60 kg.
- **Pasos:**
  1. Activar la descarga y registrar una serie de P1 con 6 repeticiones, y otra con 12.
  2. Cerrar la sesión de descarga.
  3. Terminar la descarga y cerrar una sesión normal con P1 en 12 · 12 · 12 y 60 kg.
- **Resultado esperado:**
  - Durante la descarga no aparece ningún aviso en directo ni señal en el resumen.
  - Tras la descarga, la sesión normal muestra «Subir carga», porque es la cuarta consecutiva y la descarga no sumó ni reinició.

### QA-33: El respaldo conserva categorías y racha

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-46.19
- **Precondiciones:**
  - «Press Cerrado en Máquina» creado como Empuje.
  - Rutina «Torso Extra» creada como Jalón.
  - P1 con 2 sesiones en el tope.
- **Pasos:**
  1. Exportar un respaldo.
  2. Reinstalar la app e importar el respaldo.
  3. Revisar el detalle del ejercicio, el plan y la siguiente sesión con P1.
- **Resultado esperado:** el ejercicio sigue en Empuje, la rutina en Jalón y P1 muestra «2 de 4 sesiones en el tope».

### QA-34: El peso de referencia de la sesión es el peso mayor, no el promedio

- **Tipo:** Borde
- **Prioridad:** Alta
- **Cubre:** CA-46.03, CA-46.17 / RN-4
- **Datos de prueba:**

  | Sesión | Series de P1 (peso × reps) | Peso de referencia | Resumen esperado |
  | :----: | -------------------------- | :----------------: | ---------------- |
  | 1 | 60 × 12 · 60 × 12 · 62.5 × 12 | 62.5 kg | 1 de 4 |
  | 2 | 62.5 × 12 · 60 × 13 · 60 × 12 | 62.5 kg | 2 de 4 (mismo peso de referencia aunque el promedio difiere) |
  | 3 | 60 × 12 · 60 × 12 · 60 × 12 | 60 kg | 1 de 4 (cambió el peso de referencia) |

- **Pasos:**
  1. Cerrar las 3 sesiones en orden.
- **Resultado esperado:** el de la tabla. La continuidad se juzga por el peso mayor de cada sesión, nunca por el promedio.

### QA-35: Subir carga parte del peso mayor de la sesión

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-46.03
- **Precondiciones:** P1 lleva 3 sesiones consecutivas en el tope con peso de referencia 62.5 kg.
- **Datos de prueba:**

  | Campo | Valor |
  | ----- | ----- |
  | Sesión 4 | 60 × 12 · 62.5 × 12 · 62.5 × 13 |

- **Pasos:**
  1. Cerrar la cuarta sesión.
- **Resultado esperado:** «Subir carga → 62.5 kg + el incremento vigente». No parte del promedio (61.7 kg).

### QA-36: Con la subida ya alcanzada, el aviso en directo dice «Ya toca subir el peso»

- **Tipo:** Positivo
- **Prioridad:** Media
- **Cubre:** CA-46.05 / RN-4
- **Precondiciones:** P1 ya recibió «Subir carga» tras 4 sesiones consecutivas en el tope con 60 kg.
- **Datos de prueba:**

  | Peso registrado | Reps | Aviso esperado |
  | :-------------: | :--: | -------------- |
  | 60 kg | 12 | «Ya toca subir el peso» |
  | 60 kg | 10 | Ninguno |

- **Pasos:**
  1. En la sesión siguiente, registrar una serie de P1 con los datos de la fila.
- **Resultado esperado:** el de la tabla. El aviso no bloquea el registro.

---

**Total de casos:** 36
