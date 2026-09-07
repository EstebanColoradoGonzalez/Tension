# Historia de Usuario

**Como** El Ejecutante,
**Quiero** consultar mi 1RM estimado por ejercicio y por implemento, en kilogramos y en libras a la vez,
**Para** saber la fuerza real que tengo con cada implemento sin tener que calcularlo a mano.

> **Hija 4 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/index.md), [`HU-40`](../HU-40-progresion-por-equipamiento/index.md), [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/index.md), [`HU-43`](../HU-43-ajustar-sesion-del-dia/index.md). **Depende de `HU-39`. Independiente de `HU-40` y `HU-41` — paralelizable.**

## Descripción

El sistema registra cada serie con su peso, sus repeticiones y su RIR, pero **nunca traduce esos datos a una medida de fuerza máxima**. El Ejecutante puede ver que hoy movió 20 kg × 10 repeticiones, y que hace un mes movía 17.5 kg × 10, pero no tiene ninguna cifra que exprese *cuánta fuerza tiene*. El 1RM —la carga máxima que podría mover en una sola repetición— es esa cifra, y es el lenguaje habitual con el que se habla de fuerza en el gimnasio.

Esta historia añade una **vista de 1RM estimado**, alcanzada desde el panel de Métricas, que lista los ejercicios que El Ejecutante ha entrenado de verdad y, dentro de cada uno, los implementos que ha usado de verdad. No es un catálogo: si un ejercicio admite cuatro implementos y solo se ha hecho con uno, solo aparece ese. Si solo se ha entrenado una sesión, solo aparecen los ejercicios de esa sesión.

El 1RM se resuelve por **par (ejercicio, equipamiento)**, por la misma razón que motiva toda la partición: 20 kg en polea y 20 kg en mancuerna no son la misma fuerza, así que un solo número por ejercicio mezclaría magnitudes incomparables.

El cálculo usa la fórmula `1RM = Peso / [1.0278 − (0.0278 × Repeticiones)]`, pero **solo se dispara en la serie de referencia**: exactamente **10 repeticiones con RIR exactamente 1**. Esa condición no es una simplificación, es lo que hace fiable la estimación —es la combinación recomendada para estimarla— y tiene dos consecuencias felices:

- La fórmula opera siempre en `Peso × 1.3337`, muy lejos de su indeterminación (el denominador se anula en 36.97 repeticiones y se vuelve negativo por encima).
- Como con 10 repeticiones fijas el resultado es monótono en el peso, las dos reglas que El Ejecutante pidió —*recalcular cuando se sube el peso* y *conservar el valor más alto alcanzado*— colapsan en una sola: **el 1RM es el máximo histórico de las series que califican**. Nunca decrece, y repetir una sesión idéntica no produce ningún recálculo observable.

Donde no hay carga externa no hay 1RM: los ejercicios de peso corporal puro y los isométricos quedan fuera. En los de peso corporal **con lastre** sí se calcula, sobre la carga externa únicamente.

### Frontera de alcance

- **Pantalla nueva autocontenida.** No modifica la lógica del motor de decisión, ni el catálogo, ni el plan. Solo lee del historial y persiste su propio valor.
- **La entrada va en Métricas**, no en Inicio ni en la barra de navegación inferior.
- **Sin retro-cálculo.** Como cada hija de la partición extiende el esquema y eso se resuelve por instalación fresca (ADR-019), al llegar esta historia no hay historial previo que rellenar. El 1RM se calcula **solo hacia adelante**.
- **Sin dato de origen.** La vista muestra el número, no el peso, las repeticiones ni la fecha de la serie que lo produjo.
- **El 1RM no alimenta ninguna decisión.** Ni la prescripción de carga, ni el Doble Umbral, ni la detección de meseta, ni las alertas leen este valor. La dependencia es unidireccional: el 1RM lee del historial; nada del sistema lee del 1RM.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-42.01 — Entrada desde el panel de Métricas

- **Dado** que El Ejecutante está en el panel de Métricas (`Flujo G`)
- **Cuando** observa la pantalla
- **Entonces** ve una **entrada al 1RM estimado**
- **Y** al tocarla, el sistema navega a la vista de 1RM
- **Y** el área táctil es de al menos 48×48 dp (RNF06)
- **Y** la entrada **no** se añade a Inicio ni a la barra de navegación inferior
- **Y** la entrada **se compone siempre**, incluso sin sesiones registradas: la vista de destino resuelve el estado vacío (CA-42.07)

#### CA-42.02 — La vista de 1RM lista solo lo entrenado

- **Dado** que El Ejecutante abre la vista de 1RM
- **Cuando** el sistema compone la lista
- **Entonces** muestra **únicamente los ejercicios que ha entrenado realmente**, según el historial de sesiones
- **Y** dentro de cada ejercicio, **únicamente los equipamientos con los que ha entrenado** — si el ejercicio admite tres opciones y solo usó una, solo aparece esa
- **Y** al seleccionar un equipamiento se muestra el **1RM correspondiente a ese par**
- **Y** un ejercicio con un único equipamiento entrenado muestra su valor directamente, sin exigir selección
- **Y** la lista se ordena **alfabéticamente** por nombre de ejercicio
- **Y** si El Ejecutante solo ha entrenado una sesión, solo aparecen los ejercicios de esa sesión

#### CA-42.03 — Condición de cálculo y fórmula

- **Dado** que El Ejecutante registra una serie
- **Cuando** la serie tiene **exactamente 10 repeticiones** y **RIR exactamente 1**
- **Entonces** el sistema calcula el 1RM del par con la fórmula `1RM = Peso / [1.0278 − (0.0278 × Repeticiones)]`, que para 10 repeticiones equivale a `Peso × 1.3337`
- **Y** el resultado se persiste asociado al **par (ejercicio, equipamiento)** de esa serie
- **Y** el valor canónico se persiste en **kilogramos**, coherente con `weight_kg`
- **Pero cuando** la serie **no** cumple ambas condiciones a la vez
- **Entonces** el sistema **no calcula ni actualiza** el 1RM de ese par, y el valor previo —si existe— se conserva intacto
- **Y** la condición se evalúa **serie por serie**, tanto en la primera serie de un ejercicio como en cualquier serie posterior de la misma sesión o de sesiones futuras
- **Y** al exigirse exactamente 10 repeticiones, la fórmula **nunca** opera cerca de su indeterminación (denominador cero en 36.97 repeticiones, negativo por encima)

#### CA-42.04 — El 1RM es un récord que nunca baja

- **Dado** un par con un 1RM ya calculado
- **Cuando** una serie nueva califica y produce un 1RM **mayor**
- **Entonces** el valor se **actualiza** al nuevo
- **Pero cuando** produce un 1RM **igual o menor**
- **Entonces** el valor guardado **se conserva**
- **Y** si en una misma sesión **varias** series califican, **gana el 1RM más alto** resultante
- **Y** repetir sesiones con el mismo peso, repeticiones y RIR **no** produce ningún recálculo observable
- **Y** el 1RM de un par **nunca decrece** a lo largo del tiempo
- **Y** las series de sesiones de **descarga** también califican si cumplen la condición: al ser el 1RM un máximo, una carga reducida no puede deteriorarlo

#### CA-42.05 — Exclusiones del cálculo

- **Dado** un ejercicio de **peso corporal** ejecutado con un equipamiento que no captura carga (`Peso Corporal`, `Barra Fija` o `Máquina` asistida, según CA-39.05)
- **Cuando** registra una serie que cumpliría la condición de 10 repeticiones y RIR 1
- **Entonces** el sistema **no calcula** 1RM, porque no hay carga externa que medir
- **Y** el par **no aparece** en la vista de 1RM
- **Pero cuando** el mismo ejercicio se ejecuta con `Peso Añadido`
- **Entonces** sí se calcula, **sobre la carga externa únicamente** — el peso corporal de El Ejecutante **no** se suma
- **Y** los ejercicios **isométricos** quedan **excluidos**: en ellos las repeticiones son segundos sostenidos, así que la condición de «10 repeticiones» no tiene significado
- **Y** los ejercicios **sin límite superior de repeticiones** (a fallo técnico) participan con la misma regla que el resto: solo califican si la serie da exactamente 10 repeticiones con RIR 1

#### CA-42.06 — Doble unidad siempre visible

- **Dado** un par con 1RM calculado
- **Cuando** se muestra en la vista
- **Entonces** el valor aparece **simultáneamente en kilogramos y en libras**, ambos visibles a la vez, sin conmutador
- **Y** la conversión usa el factor vigente del sistema (0.45359237) con la precisión de presentación ya establecida
- **Y** se muestra **solo el número** del 1RM: la vista no expone el peso, las repeticiones ni la fecha de la serie que lo produjo
- **Y** la presentación **no depende** de la `capture_unit` con la que se registró la serie: ambas unidades se muestran siempre

---

### Escenario 2: Casos Extremos

#### CA-42.07 — Ejercicio nunca entrenado y estado vacío

- **Dado** un ejercicio presente en el Diccionario pero sin ninguna serie registrada
- **Cuando** El Ejecutante abre la vista de 1RM
- **Entonces** el ejercicio **no aparece** en la lista
- **Y** tampoco aparece un ejercicio entrenado cuyas series **nunca** cumplieron la condición de cálculo: no se muestra con valor cero ni con un guion
- **Y** tampoco aparece un par entrenado sin serie que califique, aunque otro par del mismo ejercicio sí tenga valor
- **Y** si El Ejecutante no ha entrenado ninguna sesión, o ninguna serie ha calificado nunca, la vista muestra un **estado vacío explicativo** que enuncia la condición de cálculo (10 repeticiones con RIR 1), no una lista en blanco

#### CA-42.08 — Respaldo y restauración

- **Dado** el mecanismo de respaldo y restauración (`J2-T1`, `J3-T1`)
- **Cuando** se exporta un respaldo tras esta historia
- **Entonces** el respaldo incluye el **1RM de cada par**
- **Y** un respaldo restaurado reproduce los valores **sin recalcularlos** desde el historial: el 1RM es un récord acumulado, y recalcularlo desde las series podría perder un máximo alcanzado en una serie ya purgada o restaurada parcialmente
- **Y** los respaldos del formato anterior a `HU-39` siguen siendo **incompatibles** y se rechazan con mensaje explícito
- **Y** el criterio de instalación fresca de **ADR-019** (CA-39.11) aplica también a esta historia

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — actor único del sistema. Aplicación *single-user* sin roles.
- **Permisos requeridos:** ninguno. No existe modelo de permisos.
- **Valor de negocio:** El Ejecutante obtiene, por primera vez, una cifra de **fuerza máxima** por ejercicio e implemento, en el lenguaje con el que se habla de fuerza en el gimnasio y en las dos unidades en que están rotulados los implementos. Hoy tendría que calcularla a mano serie por serie.

### Reglas de Negocio

1. **El 1RM se resuelve por par (ejercicio, equipamiento).** Un solo número por ejercicio mezclaría magnitudes incomparables.
2. **Solo la serie de referencia dispara el cálculo:** exactamente 10 repeticiones con RIR exactamente 1.
3. **El 1RM es un récord monótono.** Es el máximo histórico de las series que califican; nunca decrece.
4. **Ante varias series que califican, gana la más alta.**
5. **Sin carga externa no hay 1RM.** Peso corporal puro e isométricos quedan excluidos.
6. **Con lastre, el 1RM es sobre la carga externa únicamente.** El peso corporal no se suma.
7. **Solo aparece lo entrenado.** Ni ejercicios sin series, ni pares sin serie que califique.
8. **Doble unidad siempre visible.** Kilogramos y libras a la vez, sin conmutador.
9. **Solo el número.** La vista no expone el peso, las repeticiones ni la fecha de origen.
10. **El 1RM no alimenta ninguna decisión.** Dependencia unidireccional: lee del historial, nadie lee de él.
11. **El valor canónico se persiste en kilogramos**, coherente con `weight_kg`.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| Panel de Métricas | `G1-T1` | Nueva entrada al 1RM estimado |
| 1RM estimado | **pantalla nueva** | Lista alfabética de ejercicios entrenados, con selección de equipamiento y doble unidad |
| Registro de serie | `E2-T1` | Sin cambio visible: el cálculo se dispara al persistir la serie que califica |
| Respaldo / Restauración | `J2-T1`, `J3-T1` | Formato ampliado con el 1RM por par |

#### Detalle de Interfaz de Usuario

- **Diseño general:** **una pantalla nueva** alcanzada desde el panel de Métricas, sin pestaña propia en la barra de navegación inferior. Su unidad de composición es **una tarjeta por ejercicio**, en orden alfabético. Cada tarjeta lleva el nombre del ejercicio, sus implementos entrenados como **chips seleccionables** y, debajo, el valor del 1RM del chip activo. Se eligió sobre la lista plana de pares y sobre la lista con detalle en pantalla aparte porque conserva **un ejercicio, una tarjeta** —la lista no se multiplica por implemento— y deja el número **siempre visible**, sin un toque de navegación intermedio.
- **Campos y controles:** chips de implemento dentro de cada tarjeta, limitados a los efectivamente usados; el primero queda activo al abrir. Un ejercicio con un solo implemento entrenado muestra ese implemento como etiqueta no interactiva y su valor directamente. El valor del 1RM se presenta en kilogramos y en libras en la misma línea, ambos visibles a la vez.
- **Flujo de navegación visual:** `Métricas → 1RM estimado → selección de implemento → valor del par → volver a Métricas`.
- **Mensajes y feedback:** estado vacío que enuncia la condición de cálculo (10 repeticiones con RIR 1) cuando no hay ningún par con valor. Un ejercicio sin serie que califique simplemente **no se lista**, en lugar de mostrarse con cero o con un guion.

### Sistemas Externos

Ninguno. El sistema es local y sin integraciones.

### Preview de Interfaz

**Preview:** [`42.preview.txt`](./42.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — entidades `exercise_set`, `exercise`, `session`, `weight_record`; enum `WeightUnit`; convenciones base §1.
- `docs/architecture/interfaces_contract.md` — triggers `E2-T1`, `G1-T1`, `J2-T1`, `J3-T1`. La pantalla nueva exige un módulo de flujo propio, al modo del `Flujo N` que introdujo `HU-37`.
- **ADR-019** — cambio de esquema resuelto por instalación fresca. Criterio heredado de `HU-39` (CA-39.11).

**Historias hermanas:**
- [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/historia.md) — **prerrequisito obligatorio.** Aporta el equipamiento persistido en la serie, sin el cual no existe el par, y la regla de captura de carga en peso corporal (CA-39.05) que gobierna las exclusiones de CA-42.05.
- [`HU-40`](../HU-40-progresion-por-equipamiento/historia.md) — **independiente.** El 1RM no consume el estado de progresión ni la carga prescrita.
- [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/historia.md) — **independiente.** El 1RM no consume zonas musculares ni el plan.
- [`HU-43`](../HU-43-ajustar-sesion-del-dia/historia.md) — independiente. Las series de un ejercicio añadido a la sesión califican con la misma regla.

**Historias relacionadas:**
- `HU-06` — registro de series. Momento en que se dispara el cálculo.
- `HU-15`, `HU-35` — panel de Métricas y métricas comprensibles. Punto de entrada de la vista.
- `HU-17` — historial de ejercicios y sesiones. Fuente de la lista de pares entrenados.
- `HU-19` — respaldo y restauración. Formato ampliado.
- `HU-22`, `HU-37` — precedentes de **pantalla dedicada colgada de una pantalla existente, sin pestaña propia**.
- `HU-30` — captura en Kg/Lb y factor de conversión 0.45359237. Base de la doble unidad.
- `HU-08` — ejercicios de peso corporal e isométricos. Origen de las exclusiones.

**Lecciones aprendidas:**
- `HU-30` estableció el patrón de **valor canónico único con unidad de presentación**. El 1RM lo reutiliza: se persiste en kilogramos y se presenta en ambas unidades.
- `HU-35` mostró que una métrica sin contexto no se entiende. De ahí que el estado vacío **enuncie la condición de cálculo** en lugar de dejar la lista en blanco, y que un par sin serie que califique no se muestre con un cero engañoso.
- `HU-37` estableció el patrón de una funcionalidad **puramente visual y aislada**, con dependencia unidireccional hacia el historial y sin que ningún componente de decisión la lea. El 1RM sigue exactamente ese modelo.
- `HU-37` también mostró, en su fase de refinamiento, que un valor **derivable** puede convenir persistirlo si su recálculo puede perder información. El 1RM es un máximo acumulado: por eso el respaldo lo restaura en lugar de recalcularlo.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente
- [ ] Mensajes implementados
- [ ] Verificada la condición exacta de disparo: series con 9 u 11 repeticiones, o con RIR 0 o 2, **no** producen cálculo
- [ ] Verificada la monotonía: una serie que califica con 1RM menor **no** deteriora el valor guardado
- [ ] Verificadas las exclusiones de CA-42.05 en las cuatro opciones de `Dominadas`
- [ ] Verificado que ningún componente del motor de decisión, alerta o KPI lee el 1RM
- [ ] Documentación de arquitectura actualizada: modelo de dominio, contrato de interfaces (módulo de flujo nuevo) y versión de esquema
- [ ] `story_mapping_index.md` actualizado con la historia, sus hermanas y sus dependencias
