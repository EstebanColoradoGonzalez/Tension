# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que el sistema me diga cuándo bajar o subir el peso según el rango de hipertrofia de 8 a 12 repeticiones, sin que el número de series del plan intervenga, y que la sesión y el plan solo me ofrezcan ejercicios de la categoría del día —empuje, jalón o pierna—,
**Para** trabajar siempre con un peso que me permita 8 a 12 repeticiones limpias, progresar cuando de verdad me he adaptado y no romper la división del día añadiendo ejercicios que no le corresponden.

## Descripción

La historia reúne tres correcciones al motor de entrenamiento que El Ejecutante detectó al usar la app.

**1. Progresión por rango de hipertrofia.** El objetivo de hipertrofia del sistema es trabajar con un peso que permita **8 repeticiones limpias con buena técnica** y, a partir de ahí, progresar hasta **12**, con **RIR 1** como referencia de esfuerzo. De ese principio salen dos señales:

- **Por debajo de 8**, el peso es demasiado alto: el sistema avisa de **bajar peso**. El aviso llega **en el momento**, desde la primera serie que se queda corta, para que El Ejecutante ajuste en la serie siguiente. La cantidad la decide él, porque depende del ejercicio y del implemento.
- **12 o más** significa que el peso se quedó corto, pero **no se sube de inmediato**. El sistema propone subir después de **4 sesiones consecutivas con el mismo peso** llegando a 12 o más, porque ese margen permite consolidar la adaptación al peso actual. Mientras tanto, El Ejecutante ve su avance hacia la subida.

Hoy el sistema hace algo distinto. Usa la Regla de Doble Umbral de `HU-11`: solo propone subir si se llega a 12 repeticiones en al menos 3 de **4 series** y el RIR medio es de 2 o más, y **nunca** propone bajar. Como casi todos los ejercicios del plan por defecto tienen **3 series**, en la práctica la propuesta de subir **no aparece nunca** para la mayoría de ellos. **Las series son las que dicta el plan.** No deben modificar la progresión ni intervenir en la decisión de subir o bajar.

**2. Umbral base de estancamiento.** El valor por defecto de `HU-32`, **5 sesiones**, es demasiado bajo. Pasa a **10**. Se mantienen los multiplicadores por dificultad del ejercicio y el rango configurable.

**3. Sesión y plan acotados a la categoría del día.** La división del plan es **Empuje / Jalón / Pierna** (*Push / Pull / Lower*), pero hoy esa división solo existe en el **nombre libre** de la rutina. Ni la rutina ni el ejercicio tienen categoría. Por eso la acción de **añadir ejercicio** a la sesión de `HU-43` ofrece el Diccionario entero, y en un día de empuje deja añadir un remo o una sentadilla. La categoría pasa a ser un dato del **ejercicio** y de la **rutina**. Con ella, la sesión del día y el plan solo ofrecen ejercicios de la categoría correspondiente.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-46.01 — Lectura de la sesión por mayoría de series

- **Dado** que El Ejecutante cierra una sesión en la que registró series de un ejercicio con un implemento
- **Cuando** el sistema evalúa la progresión de ese par ejercicio–implemento (criterio por par de `HU-40`)
- **Entonces** clasifica la sesión del par según la **mayoría estricta** de sus series, es decir, más de la mitad:
  - **Tope del rango**: la mayoría de las series llega a **12 o más** repeticiones.
  - **Por debajo del rango**: la mayoría de las series se queda en **menos de 8** repeticiones.
  - **Dentro del rango**: cualquier otro caso.
- **Y** la mayoría se calcula sobre las series **realmente registradas** de ese par en la sesión, sea cual sea el número de series que prescribe el plan. Por ejemplo, 2 de 3 o 3 de 4.
- **Y** el RIR **no condiciona** la clasificación. RIR 1 es la referencia de esfuerzo que se muestra a El Ejecutante.

#### CA-46.02 — Señal de bajar peso al cerrar la sesión

- **Dado** que la sesión de un par quedó **por debajo del rango** (CA-46.01)
- **Cuando** El Ejecutante consulta el resumen post-sesión (`E5-T1`)
- **Entonces** el renglón del par muestra la señal **«Bajar peso»**
- **Y** el sistema **no prescribe una cantidad**: la reducción la decide El Ejecutante según el ejercicio y el implemento
- **Y** al iniciar la siguiente sesión que incluya ese ejercicio, el peso propuesto lleva la misma indicación de bajar peso
- **Y** el conteo hacia la subida (CA-46.03) de ese par vuelve a **0**

#### CA-46.03 — Señal de subir peso tras 4 sesiones consecutivas en el tope

- **Dado** que un par acumula sesiones en el **tope del rango** (CA-46.01)
- **Cuando** completa la **cuarta sesión consecutiva** en el tope **con el mismo peso**
- **Entonces** el resumen post-sesión muestra **«Subir carga → X»**, donde X es el peso de referencia de la sesión más el incremento que el sistema ya aplica hoy
- **Y** ese peso queda como el propuesto para la siguiente sesión que incluya el ejercicio
- **Y** antes de la cuarta sesión consecutiva **no** se propone subir: se mantiene el peso
- **Y** si El Ejecutante no sube y vuelve a cerrar en el tope con el mismo peso, la señal de subir **se mantiene**
- **Y** el **peso de referencia** de la sesión de un par es el **peso mayor** con el que se registró alguna de sus series en esa sesión, **no el promedio**. Por ejemplo, con 60 · 60 · 62.5 kg el peso de referencia es 62.5 kg. «Mismo peso» significa mismo peso de referencia.

#### CA-46.04 — Avance visible hacia la subida

- **Dado** que un par lleva entre 1 y 3 sesiones consecutivas en el tope con el mismo peso
- **Cuando** El Ejecutante consulta el resumen post-sesión
- **Entonces** el renglón del par indica **«Mantener — N de 4 sesiones en el tope»**
- **Y** el mismo avance se muestra junto al peso propuesto al iniciar la siguiente sesión con ese ejercicio

#### CA-46.05 — Aviso en directo al registrar una serie

- **Dado** que El Ejecutante registra una serie de un ejercicio en la sesión activa (`E2-T1`)
- **Cuando** la serie queda en **menos de 8** repeticiones
- **Entonces** el sistema muestra en ese momento el aviso **«Baja el peso en la próxima serie»**, incluso si es la **primera serie** de la sesión
- **Y cuando** la serie llega a **12 o más** repeticiones, el sistema muestra un aviso **informativo**, por ejemplo «Tope del rango — cuenta para la subida (sesión N de 4)», y **no** una orden de subir peso
- **Y** si el par **ya alcanzó** la señal de subir (4 o más sesiones consecutivas en el tope con el mismo peso) y El Ejecutante sigue con ese peso, el aviso de una serie de 12 o más pasa a ser **«Ya toca subir el peso»**
- **Y** una serie entre 8 y 11 repeticiones no genera aviso
- **Y** ninguno de los dos avisos bloquea el registro ni modifica la serie registrada

#### CA-46.06 — Las series del plan no condicionan la progresión

- **Dado** un ejercicio cuya asignación del plan prescribe cualquier número de series (2, 3, 4…)
- **Cuando** el sistema decide si proponer subir, bajar o mantener el peso
- **Entonces** la decisión depende **solo** de CA-46.01 a CA-46.03
- **Y** desaparecen las dos condiciones de la Regla de Doble Umbral (`HU-11`): ya no se exigen **4 series** ni un **RIR medio de 2 o más**
- **Y** un ejercicio de 3 series puede recibir la señal de subir igual que uno de 4

#### CA-46.07 — Umbral base de estancamiento por defecto en 10

- **Dado** una instalación nueva de la aplicación
- **Cuando** se crea el perfil de El Ejecutante
- **Entonces** el umbral base de estancamiento es de **10 sesiones**
- **Y** se mantienen los multiplicadores por dificultad de `HU-32` (×1 / ×1.5 / ×2, redondeando hacia arriba), de modo que el umbral efectivo por defecto es **10** (Baja), **15** (Media) y **20** (Alta)
- **Y** el rango configurable desde Ajustes sigue siendo de **3 a 15**
- **Y** el desglose explicativo de Ajustes refleja los nuevos valores por defecto

#### CA-46.08 — Categoría de movimiento del ejercicio

- **Dado** el Diccionario de Ejercicios
- **Cuando** El Ejecutante consulta un ejercicio (`D2-T1`)
- **Entonces** el ejercicio tiene **una sola** categoría de movimiento: **Empuje**, **Jalón** o **Pierna**
- **Y** ningún ejercicio queda sin categoría
- **Y** todo ejercicio que trabaja **abdomen** se clasifica como **Jalón**
- **Y** los 38 ejercicios del catálogo inicial quedan clasificados así:

| Categoría | Ejercicios del catálogo inicial |
|---|---|
| **Empuje** (9) | Press de Banca Plano, Press de Banca Inclinado, Press Militar, Aperturas, Cruce de Polea Alta, Elevación Lateral, Extensión de Tríceps (Pushdown), Extensión de Tríceps sobre Cabeza, Press Francés |
| **Jalón** (18) | Jalón al Pecho, Dominadas, Remo T Inclinado, Remo Horizontal, Remo Unilateral Polea Baja, Remo Unilateral Polea Alta, Pull-Over, Face Pull, Vuelos Posteriores (Pájaros), Trapecios con Apoyo en Banco Inclinado, Remo al Mentón, Curl Martillo, Curl Bayesian en Banco Inclinado, Curl de Predicador, Curl de Concentración, Curl de Martillo Cruzado, Crunch Abdominal, Press Pallof |
| **Pierna** (11) | Sentadilla Hack, Prensa Inclinada, Sentadilla Búlgara, Sentadilla Sumo, Zancadas (Lunges), Extensión de Cuádriceps, Curl de Isquiotibiales Sentado, Peso Muerto Rumano, Hip Thrust, Aductores, Elevación de Pantorrilla de Pie |

#### CA-46.09 — Categoría de la rutina

- **Dado** el plan de entrenamiento
- **Cuando** El Ejecutante consulta o crea una rutina (`D3-T1`, `D6-T1`)
- **Entonces** cada rutina tiene **una sola** categoría: **Empuje**, **Jalón** o **Pierna**, independiente de su nombre libre
- **Y** las 6 rutinas del plan por defecto quedan así: Rutinas 1 y 4 (*Push*) → **Empuje**, Rutinas 2 y 5 (*Pull*) → **Jalón**, Rutinas 3 y 6 (*Lower*) → **Pierna**
- **Y** la **categoría del día** es la de la rutina de la sesión en curso, también cuando esa rutina viene de una reasignación temporal (`B1-T3`)

#### CA-46.10 — Añadir a la sesión solo ejercicios de la categoría del día

- **Dado** que El Ejecutante tiene una sesión activa de una rutina de categoría X
- **Cuando** abre la acción de **añadir ejercicio** (`E1-T2`, `HU-43`)
- **Entonces** el listado y la búsqueda muestran **solo** los ejercicios del Diccionario de categoría X
- **Y** los ejercicios de otras categorías **no aparecen**
- **Y** se conservan las reglas de `HU-43`: los ejercicios ya presentes en la sesión siguen marcados como no añadibles, se mantiene el presupuesto de retiro y el plan no se modifica

#### CA-46.11 — Ejercicio creado desde la sesión hereda la categoría del día

- **Dado** que El Ejecutante crea un ejercicio nuevo desde la sesión activa (`D5-T1` invocado desde `E1-T2`, CA-43.02)
- **Cuando** se presenta el formulario
- **Entonces** la categoría llega **fijada** a la categoría del día y **no se puede cambiar**
- **Y** el ejercicio queda en el Diccionario con esa categoría

#### CA-46.12 — Plan restringido por categoría

- **Dado** una rutina del plan de categoría X
- **Cuando** El Ejecutante asigna un ejercicio a esa rutina o lo define como **alternativa de un puesto** (`HU-26`)
- **Entonces** el selector ofrece **solo** ejercicios de categoría X
- **Y** el sistema rechaza cualquier asignación de un ejercicio de otra categoría

---

### Escenario 2: Validaciones

#### CA-46.13 — Categoría obligatoria al crear

- **Dado** que El Ejecutante crea un ejercicio desde el Diccionario (`D5-T1`) o crea una rutina (`D6-T1`)
- **Cuando** intenta guardar sin elegir categoría
- **Entonces** el sistema **no guarda** e indica que la categoría es obligatoria

#### CA-46.14 — Cambio de categoría que contradice el plan

- **Dado** un ejercicio asignado a una o más rutinas del plan, como puesto o como alternativa
- **Cuando** El Ejecutante intenta cambiarle la categoría desde su detalle a una distinta de la de esas rutinas
- **Entonces** el sistema **lo impide** y nombra las rutinas en conflicto
- **Y** de la misma forma, el sistema impide cambiar la categoría de una rutina que tiene ejercicios asignados de otra categoría
- **Y** un ejercicio **sin** asignaciones en el plan puede cambiar de categoría libremente

---

### Escenario 3: Casos Extremos

#### CA-46.15 — Ejercicios de peso corporal

- **Dado** un ejercicio de peso corporal, incluidos los de fallo técnico
- **Cuando** el sistema evalúa la progresión del par con el implemento **«Peso Corporal»**
- **Entonces** **nunca** emite la señal de bajar peso, porque al peso corporal no se le puede quitar peso
- **Y** si el ejercicio **admite el implemento «Peso Añadido»**, aplica la regla del tope (CA-46.03 y CA-46.04): tras 4 sesiones consecutivas en el tope propone **añadir peso extra**
- **Y** si el ejercicio **no** admite «Peso Añadido», no emite ninguna señal de peso
- **Y** las series registradas con el implemento «Peso Añadido» forman su propio par (`HU-40`) y siguen la regla completa, con sus dos señales, sobre el peso añadido

#### CA-46.16 — Ejercicios isométricos excluidos

- **Dado** un ejercicio isométrico (de tiempo, como la plancha)
- **Cuando** el sistema evalúa la progresión o se registra una serie
- **Entonces** no aplica ninguna señal de esta historia: el isométrico mide resistencia, no hipertrofia
- **Y** su progresión por segundos actual se conserva sin cambios

#### CA-46.17 — El conteo hacia la subida exige continuidad y mismo peso

- **Dado** que un par lleva N sesiones consecutivas en el tope
- **Cuando** cierra una sesión **dentro del rango** o **por debajo del rango**, o cierra en el tope con un **peso de referencia distinto** (el peso mayor de sus series, CA-46.03)
- **Entonces** el conteo hacia la subida vuelve a 0 o, si la sesión con el nuevo peso está en el tope, a 1
- **Y** una sesión en la que el par **no registró series** no rompe ni suma al conteo

#### CA-46.18 — Señales durante la descarga

- **Dado** que El Ejecutante está en un período de descarga activo (`HU-14`)
- **Cuando** registra series o cierra sesiones de descarga
- **Entonces** no se emiten las señales de bajar ni subir peso, porque la descarga prescribe su propia carga
- **Y** las sesiones de descarga **no suman ni reinician** el conteo hacia la subida

#### CA-46.19 — Respaldo y documentación

- **Dado** que la categoría del ejercicio, la categoría de la rutina y el conteo hacia la subida pasan a ser datos del sistema
- **Cuando** El Ejecutante exporta e importa un respaldo (`J2-T1`, `J3-T1`)
- **Entonces** esos datos se conservan íntegros
- **Y** la importación de respaldos de versiones anteriores, sin categorías, **queda fuera de alcance**: la app está en beta y siempre se prueba desde una instalación fresca
- **Y** la documentación de arquitectura (`domain_and_state_model.md`, `interfaces_contract.md`, `architecture_blueprint.md`) y `story_mapping_index.md` quedan actualizados

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante, actor único del sistema. Aplicación *single-user* sin roles.
- **Permisos requeridos:** ninguno. No existe modelo de permisos.
- **Valor de negocio:** una señal de progresión que nunca aparece (por la exigencia de 4 series) deja a El Ejecutante sin guía para subir peso. La falta de una señal de bajar peso le deja entrenar fuera del rango de hipertrofia sin saberlo. Un umbral de estancamiento demasiado bajo genera falsas alertas. Una sesión que admite cualquier ejercicio rompe la división Empuje / Jalón / Pierna que da sentido al plan.

### Reglas de Negocio

1. **Rango de hipertrofia: 8 a 12 repeticiones limpias, con RIR 1 como referencia.** El RIR no condiciona las señales.
2. **La sesión se lee por mayoría estricta de series** del par ejercicio–implemento. Las series las dicta el plan y **no intervienen** en la decisión.
3. **Bajar peso: inmediato.** Basta una serie por debajo de 8 para el aviso en directo, y una sesión con mayoría por debajo de 8 para la señal en el resumen. La cantidad la decide El Ejecutante.
4. **Subir peso: tras 4 sesiones consecutivas en el tope con el mismo peso.** El incremento es el que el sistema ya aplica hoy. Antes de la cuarta, se mantiene el peso y se muestra el avance. Alcanzada la cuarta, el aviso en directo de una serie en el tope dice «Ya toca subir el peso». El **peso de referencia** de la sesión es el **peso mayor** de las series del par en esa sesión, no el promedio, que puede ser inexacto.
5. **El peso nunca se baja salvo que no se lleguen a 8 repeticiones.**
6. **Peso corporal:** nunca recibe la señal de bajar. Solo recibe la de subir, como peso extra, si el ejercicio admite «Peso Añadido».
7. **Isométricos:** fuera de esta regla. Miden resistencia, no hipertrofia.
8. **Umbral base de estancamiento: 10 por defecto**, rango de 3 a 15 y multiplicadores ×1 / ×1.5 / ×2 por dificultad. Aplica a instalaciones nuevas: la app está en beta y siempre se prueba desde cero.
9. **Categoría única por ejercicio y por rutina:** Empuje, Jalón o Pierna. Un ejercicio no puede ser de empuje y de jalón a la vez, y pierna es una categoría completamente aislada.
10. **Todo lo que trabaja abdomen es Jalón.**
11. **La categoría del día es la de la rutina de la sesión.** La sesión solo admite añadir ejercicios de esa categoría. El ejercicio creado desde la sesión hereda la categoría y no puede cambiarla.
12. **El plan respeta la categoría:** una rutina solo admite ejercicios y alternativas de su misma categoría.
13. **Durante la descarga no hay señales de peso** y el conteo hacia la subida queda congelado.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| Registrar serie | `E2-T1` | Aviso en directo: «Baja el peso en la próxima serie» (< 8) o aviso informativo de tope (≥ 12) |
| Resumen post-sesión | `E5-T1` | Renglón por par: «Bajar peso», «Subir carga → X» o «Mantener — N de 4 sesiones en el tope» |
| Sesión activa | `E1` | El peso propuesto lleva la indicación de bajar o el avance hacia la subida. «Añadir ejercicio» (`E1-T2`) filtrado por la categoría del día |
| Crear ejercicio | `D5-T1` | Nuevo selector obligatorio de categoría. Fijo y no editable cuando se invoca desde la sesión |
| Detalle de ejercicio | `D2-T1` | Muestra la categoría y permite editarla, con la restricción de CA-46.14 |
| Crear o editar rutina | `D6-T1` | Nuevo selector obligatorio de categoría |
| Plan | `D3-T1`, `D4-T1` | La categoría se muestra por rutina. La asignación de ejercicios y alternativas se filtra por categoría |
| Ajustes | `J1-T2` | Valor por defecto 10 y desglose 10 / 15 / 20 |

#### Detalle de Interfaz de Usuario

- **Diseño general:** ninguna pantalla nueva. Se añaden avisos en el registro de serie, renglones de señal en el resumen post-sesión, un selector de categoría en los formularios de ejercicio y rutina, y filtros por categoría en los selectores de la sesión y del plan.
- **Campos y controles:**
  - **Selector de categoría** (Empuje / Jalón / Pierna) de selección única en `D5-T1` y `D6-T1`, con área táctil mínima de 48×48 dp y sin valor preseleccionado al crear desde el Diccionario o desde el plan. Desde la sesión llega fijado y deshabilitado, con la línea «Categoría del día».
  - **Etiqueta de categoría** en el detalle del ejercicio y en la fila de cada rutina del plan.
  - **Aviso en directo** tras guardar una serie: no bloqueante, visible sin tapar el formulario de la serie siguiente.
- **Flujo de navegación visual:** sin rutas nuevas. `Sesión activa → Registrar serie → aviso → siguiente serie`. `Sesión activa → Añadir ejercicio → listado filtrado por la categoría del día → (elegir | crear con la categoría fijada)`.
- **Mensajes y feedback:**
  - Serie con menos de 8: «Baja el peso en la próxima serie».
  - Serie con 12 o más: «Tope del rango — cuenta para la subida (sesión N de 4)».
  - Resumen: «Bajar peso», «Subir carga → X», «Mantener — N de 4 sesiones en el tope».
  - Al guardar sin categoría: «Elige la categoría: Empuje, Jalón o Pierna».
  - Al cambiar una categoría en conflicto: mensaje que nombra las rutinas del plan que lo impiden.

### Sistemas Externos

Ninguno. La aplicación opera 100 % offline sobre almacenamiento local.

### Disparadores

- El Ejecutante registra una serie en la sesión activa (`E2-T1`).
- El Ejecutante cierra la sesión (`E4-T1`) y el sistema evalúa la progresión de cada par ejercicio–implemento.
- El Ejecutante consulta el resumen post-sesión (`E5-T1`).
- El Ejecutante inicia o reanuda una sesión y ve el peso propuesto por ejercicio (`B1-T1`, `B1-T2`).
- El Ejecutante abre «Añadir ejercicio» en la sesión activa y busca en el listado (`E1-T2`). Se puede repetir cuantas veces quiera durante la sesión.
- El Ejecutante crea un ejercicio desde el Diccionario o desde la sesión (`D5-T1`).
- El Ejecutante cambia la categoría de un ejercicio desde su detalle (`D2`).
- El Ejecutante crea o edita una rutina (`D6-T1`).
- El Ejecutante asigna un ejercicio a una rutina del plan o como alternativa de un puesto.
- El Ejecutante completa el alta de su perfil en una instalación nueva (`A1-T1`). Es cuando se fija el umbral base por defecto.
- El Ejecutante exporta o importa un respaldo (`J2-T1`, `J3-T1`).

### Entidades del Dominio

- Ejercicio
- Rutina
- Serie
- Sesión
- Ejercicio de la sesión
- Asignación del plan
- Progresión del ejercicio (por par ejercicio–implemento)
- Perfil del Ejecutante (umbral base de estancamiento)

### Alcance y Continuidad

- Esta funcionalidad **modifica procesos ya existentes**, no crea uno nuevo. Reemplaza la Regla de Doble Umbral de `HU-11` en la decisión de subir peso y añade la de bajar peso, cambia el valor por defecto del umbral base de `HU-32` y acota el listado de «añadir ejercicio» de `HU-43` y la asignación de ejercicios del plan.
- La **categoría de movimiento** (Empuje / Jalón / Pierna) es un **atributo nuevo del Ejercicio y de la Rutina existentes**, no una entidad distinta.
- **Fuera de alcance:** cambiar el incremento de carga al subir (se conserva el actual), cambiar los multiplicadores por dificultad, el criterio de meseta de `HU-32` / `HU-40` (salvo el valor base por defecto), la progresión por segundos de los isométricos y la migración de datos existentes (beta con instalación fresca, criterio ADR-019).

### Preview de Interfaz

**Preview:** [`46.preview.txt`](./46.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md`: entidades `exercise`, `routine`, `plan_assignment`, `session_exercise`, `exercise_set`, `exercise_progression`, `profile`; datos semilla de `exercise` y del plan por defecto.
- `docs/architecture/interfaces_contract.md`: triggers `E1-T2`, `E2-T1`, `E4-T1`, `E5-T1`, `D2-T1`, `D5-T1`, `D6-T1`, `D4-T1`, `J1-T2`, `J2-T1`, `J3-T1`.
- **ADR-019**: cambio de esquema resuelto por instalación fresca.

**Historias relacionadas:**
- `HU-11`: Regla de Doble Umbral. **Esta historia la reemplaza** en la decisión de subir peso.
- `HU-10`: clasificación de progresión post-sesión. Su contador de meseta se conserva.
- `HU-13`: resumen post-sesión. Es la pantalla donde se muestran las nuevas señales.
- `HU-14`: protocolo de descarga. Durante la descarga no hay señales (CA-46.18).
- `HU-31`: memoria del último peso. El peso precargado convive con la indicación de bajar o el avance.
- `HU-32`: umbral de meseta configurable. **Esta historia cambia su valor por defecto** de 5 a 10.
- `HU-40`: progresión por par ejercicio–implemento. Las señales se evalúan por par.
- `HU-16` y `HU-36`: división Push / Pull / Lower y días de la semana. La categoría formaliza esa división.
- `HU-26`: alternativas por puesto. Quedan restringidas a la categoría de la rutina.
- `HU-43`: ajustar la sesión del día. Su listado de «añadir ejercicio» queda filtrado por categoría.
- `HU-03` y `HU-24`: Diccionario de Ejercicios. Es donde se muestra y edita la categoría.

**Lecciones aprendidas:**
- `HU-11` fijó una regla con un número de series (4) que el plan dejó de cumplir cuando la mayoría de los puestos pasó a 3 series. La señal de subir quedó muerta **en silencio**, sin que ningún test lo detectara. Una regla de progresión no debe depender de un parámetro que pertenece al plan.
- `HU-32` mostró que un umbral global y bajo genera falsos positivos que restan credibilidad a todo el sistema de alertas.
- Codificar una clasificación de negocio en un **nombre libre** (la rutina «Push — …») impide que el sistema la use para validar. La categoría debe ser un dato, no un texto.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente
- [ ] Mensajes implementados
- [ ] Verificado que un ejercicio de 3 series recibe la señal de subir tras 4 sesiones consecutivas en el tope (CA-46.06)
- [ ] Verificado que una sola serie con menos de 8 repeticiones dispara el aviso en directo desde la primera serie (CA-46.05)
- [ ] Verificado que la sesión de un día de Empuje no ofrece ejercicios de Jalón ni de Pierna (CA-46.10)
- [ ] Clasificación de los 38 ejercicios y las 6 rutinas iniciales verificada por test contra la tabla de CA-46.08 y CA-46.09
- [ ] Umbral base por defecto en 10 en instalación fresca (CA-46.07)
- [ ] Documentación de arquitectura y `story_mapping_index.md` actualizados
