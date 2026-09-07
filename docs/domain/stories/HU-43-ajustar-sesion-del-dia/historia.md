# Historia de Usuario

**Como** El Ejecutante,
**Quiero** añadir a la sesión activa un ejercicio del Diccionario o uno nuevo, y retirar a cambio uno de los que trajo el plan,
**Para** entrenar lo que el gimnasio me permita ese día sin alterar mi plan.

> **Hija 5 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/index.md), [`HU-40`](../HU-40-progresion-por-equipamiento/index.md), [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/index.md), [`HU-42`](../HU-42-1rm-estimado/index.md). **Depende de `HU-39` y de `HU-41`.**

## Descripción

El sistema permite hoy crear ejercicios en el Diccionario y asignarlos a una rutina del plan. Lo que **no** permite es tocar la sesión que ya está en curso: la composición que el plan trajo es la composición con la que hay que entrenar. Si la máquina está ocupada, si aparece la oportunidad de hacer algo que no estaba previsto, o si simplemente el día pide otra cosa, El Ejecutante no tiene forma de reflejarlo. Solo puede intercambiar la alternativa de un puesto, y solo si ese puesto tiene una definida.

Esta historia abre la sesión activa a dos operaciones **temporales**:

- **Añadir** un ejercicio, elegido del Diccionario o **creado en el momento** con el mismo formulario del Diccionario.
- **Retirar** un ejercicio de los que trajo el plan, dentro de un presupuesto: **uno entra, uno puede salir**.

Lo temporal es la clave. El plan por defecto **no se toca**: la siguiente sesión de esa misma rutina vuelve a proponer su composición original. Pero el ajuste **sí queda en el historial**, porque el historial debe registrar lo que realmente se entrenó, y las series de los ejercicios añadidos cuentan para la progresión, el tonelaje, los KPIs y el 1RM exactamente como cualquier otra.

El ejercicio **creado** desde la sesión tiene un destino asimétrico y deliberado: **no** entra al plan, pero **sí** queda registrado en el Diccionario, disponible para asignarlo al plan más adelante si resulta que merece un puesto fijo. Es la forma de que un descubrimiento del día no se pierda al cerrar la sesión.

El presupuesto tiene una consecuencia que conviene enunciar: para **deshacer** un añadido hay que **reponer primero** el ejercicio que su incorporación permitió retirar. El número de ejercicios añadidos nunca puede quedar por debajo del número de retirados, así que la reposición se hace con la misma acción de añadir, eligiendo del Diccionario el que se había quitado.

### Frontera de alcance

- **El plan no se modifica nunca.** Ni la composición, ni el orden, ni los puestos, ni las alternativas.
- **El ejercicio añadido no ocupa puesto.** Queda fuera de la estructura de puestos y por tanto **sin alternativas intercambiables**. No lo elige El Ejecutante: es la consecuencia de estar fuera del plan.
- **Nada con series registradas se retira.** La serie es inmutable y no se borra con el ejercicio que la contiene.
- **No sustituye al intercambio de alternativa de puesto** (`E1-T1`), que sigue funcionando como hasta ahora.
- **Frente ajeno al equipamiento.** Esta historia no cambia nada de la progresión por par ni del 1RM; depende de sus hermanas solo por el formulario de creación de ejercicio.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-43.01 — Añadir un ejercicio del Diccionario a la sesión

- **Dado** que El Ejecutante tiene una sesión activa
- **Cuando** usa la acción de **añadir ejercicio** desde la pantalla de la sesión (`E1`)
- **Entonces** puede elegir un ejercicio **ya existente** en el Diccionario
- **Y** el ejercicio se incorpora **al final** de la lista de ejercicios de la sesión
- **Y** queda disponible para registrar series como cualquier otro, con su selector de equipamiento (CA-39.04)
- **Y** **no ocupa puesto**: queda fuera de la estructura de puestos y por tanto **sin alternativas intercambiables**
- **Y** puede añadirse **más de un** ejercicio en la misma sesión
- **Y** un ejercicio que **ya está** en la sesión no puede añadirse otra vez: entrenar el mismo movimiento con otro implemento se resuelve con el selector de equipamiento, no duplicando el ejercicio
- **Y** la acción está disponible mientras la sesión esté en curso, y **no** después de cerrarla

#### CA-43.02 — Crear un ejercicio nuevo desde la sesión

- **Dado** que El Ejecutante quiere añadir un ejercicio que no existe en el Diccionario
- **Cuando** elige crearlo desde la sesión
- **Entonces** el sistema presenta el **mismo formulario** que la creación de ejercicio personalizado del Diccionario (`D5-T1`), con los mismos campos y las mismas validaciones: nombre, equipamiento(s) admitidos, zonas musculares con su jerarquía, dificultad de progresión y recurso visual
- **Y** aplican íntegras las validaciones de sus hermanas: al menos un equipamiento (CA-39.09) y al menos una zona muscular principal (CA-41.09)
- **Y** al guardarlo, el ejercicio **queda registrado en el Diccionario** como ejercicio personalizado, disponible para asignarlo al plan en el futuro
- **Y** queda **añadido a la sesión activa** en el mismo gesto, sin exigir un segundo paso
- **Y** el plan de entrenamiento **no** se modifica
- **Y** el ejercicio **permanece en el Diccionario** aunque la sesión se cierre, se abandone o el ejercicio se retire de ella

#### CA-43.03 — Prescripción por defecto del ejercicio añadido

- **Dado** un ejercicio que se añade a la sesión
- **Cuando** el sistema lo incorpora
- **Entonces** le propone **3 series de 8 a 12 repeticiones**, por el enfoque de hipertrofia del sistema
- **Y** la propuesta es la misma para un ejercicio del Diccionario y para uno creado en el momento
- **Y** el rango de repeticiones se ajusta al modo del ejercicio cuando corresponda, con la misma regla que el plan: los isométricos en segundos y los de fallo técnico sin límite superior

#### CA-43.04 — Presupuesto de retiro: uno entra, uno puede salir

- **Dado** que El Ejecutante ha añadido **N** ejercicios a la sesión activa
- **Cuando** intenta retirar ejercicios que el plan trajo
- **Entonces** puede retirar **hasta N** de ellos
- **Y** si no ha añadido ninguno, **no puede retirar ninguno**: la acción de retirar está **deshabilitada**
- **Y** si añade dos, puede retirar hasta dos
- **Y** al agotarse el presupuesto, el sistema explica que hace falta **añadir un ejercicio primero**
- **Y** el ejercicio retirado desaparece de la sesión y **no** se considera en su cierre ni en su clasificación

#### CA-43.05 — Deshacer un añadido exige reponer el retirado

- **Dado** que El Ejecutante añadió un ejercicio y, gracias a ese presupuesto, retiró uno que el plan traía
- **Cuando** intenta **quitar el ejercicio añadido** —que aún tiene 0 series—
- **Entonces** el sistema **lo impide** mientras el retiro que ese añadido habilitó siga vigente
- **Y** explica que primero debe **reponer** el ejercicio retirado
- **Y** la reposición se hace con la **misma acción de añadir**, eligiendo del Diccionario el ejercicio que se había quitado
- **Y** repuesto el ejercicio, el añadido original ya puede quitarse
- **Y** la invariante que el sistema mantiene en todo momento es: **el número de ejercicios añadidos nunca es menor que el de retirados**

---

### Escenario 2: Validaciones

#### CA-43.06 — No se retira un ejercicio con series registradas

- **Dado** un ejercicio de la sesión con **al menos una serie** registrada
- **Cuando** El Ejecutante intenta retirarlo
- **Entonces** el sistema **lo impide** y explica que ya tiene series registradas
- **Y** la restricción aplica igual a los ejercicios del plan y a los añadidos en la sesión
- **Y** la razón es la inmutabilidad de la serie: retirar el ejercicio implicaría borrar registros que son fuente de verdad histórica
- **Y** un ejercicio con **cero** series sí puede retirarse, dentro del presupuesto de CA-43.04 y de la invariante de CA-43.05

---

### Escenario 3: Casos Extremos

#### CA-43.07 — Cierre de sesión con un ejercicio añadido sin series

- **Dado** que El Ejecutante añadió un ejercicio a la sesión y **no llegó a registrarle ninguna serie**
- **Cuando** cierra la sesión
- **Entonces** la sesión se cierra como **`INCOMPLETE`**
- **Y** el ejercicio añadido **cuenta como cualquier otro** para la completitud: haberlo añadido es haberlo comprometido
- **Y** los datos parciales se conservan, como en cualquier sesión `INCOMPLETE`
- **Y** si El Ejecutante prefiere no penalizar la completitud, la vía es **retirarlo** antes de cerrar, cuando la invariante de CA-43.05 lo permita

#### CA-43.08 — Alcance temporal del ajuste

- **Dado** que El Ejecutante añadió y retiró ejercicios en una sesión
- **Cuando** la sesión se cierra
- **Entonces** el **plan de entrenamiento por defecto queda intacto**: la sesión siguiente de esa misma rutina vuelve a proponer su composición original, con su orden, sus puestos y sus alternativas
- **Y** el ajuste queda **registrado en el historial** de esa sesión concreta: el detalle de la sesión pasada (`F2-T1`) muestra los ejercicios que realmente se ejecutaron
- **Y** las series de los ejercicios añadidos **cuentan** para la progresión, la clasificación, el tonelaje, los KPIs y el 1RM, exactamente como las de cualquier otro ejercicio
- **Y** el ejercicio creado desde la sesión **sí permanece** en el Diccionario después del cierre
- **Y** la rotación cíclica y el conteo de microciclos **no se alteran** por haber ajustado la composición

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — actor único del sistema. Aplicación *single-user* sin roles.
- **Permisos requeridos:** ninguno. No existe modelo de permisos.
- **Valor de negocio:** el gimnasio real no siempre permite entrenar lo que el plan dice. Hoy esa fricción no tiene salida: o se entrena lo previsto o se pierde el registro. Esta historia deja que la sesión se adapte al día sin que el plan se degrade, y captura en el Diccionario los ejercicios que se descubren por el camino.

### Reglas de Negocio

1. **El ajuste es temporal.** El plan por defecto no se modifica nunca.
2. **Uno entra, uno puede salir.** El retiro está presupuestado por los añadidos.
3. **La invariante nunca se rompe:** el número de añadidos nunca es menor que el de retirados. Deshacer un añadido exige reponer el retirado.
4. **El añadido no ocupa puesto** y por tanto no tiene alternativas intercambiables.
5. **Prescripción por defecto 3 × 8-12**, por el enfoque de hipertrofia del sistema.
6. **Nada con series registradas se retira.** La serie es inmutable y fuente de verdad histórica.
7. **Un ejercicio ya presente en la sesión no se añade dos veces.** El mismo movimiento con otro implemento se resuelve con el selector de equipamiento.
8. **El ejercicio creado en la sesión queda en el Diccionario, pero no en el plan.** Asimetría deliberada.
9. **El historial registra lo ejecutado**, no lo planificado.
10. **Lo añadido cuenta como lo demás:** para progresión, tonelaje, KPIs, 1RM y para la completitud del cierre.
11. **Haber añadido es haber comprometido.** Un añadido sin series cierra la sesión como `INCOMPLETE`.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| Sesión activa | `E1` | Nuevas acciones de **añadir** y **retirar** ejercicio |
| Crear ejercicio | `D5-T1` | El formulario se reutiliza tal cual, invocado desde la sesión |
| Cierre de sesión | `E4-T1` | La completitud considera los ejercicios añadidos |
| Detalle de sesión pasada | `F2-T1` | Muestra los ejercicios realmente ejecutados, incluidos los añadidos |
| Diccionario | `D1-T1` | El ejercicio creado desde la sesión aparece como personalizado |

#### Detalle de Interfaz de Usuario

- **Diseño general:** ninguna pantalla nueva. Se añaden dos acciones a la pantalla de la sesión activa, y se **reutiliza sin cambios** el formulario de creación de ejercicio del Diccionario.
- **Campos y controles:**
  - **Añadir:** un botón **«+ Añadir ejercicio»** situado **al final de la lista**, tras el último ejercicio de la sesión, con dos caminos —elegir del Diccionario o crear nuevo—. Su posición refuerza que el ejercicio entra al final y fuera de la estructura de puestos.
  - **Retirar:** acción **«Retirar de la sesión»** dentro del **menú contextual de cada ejercicio**. Se eligió sobre un modo de edición de la sesión y sobre un menú único en la barra superior porque **el retiro vive junto al ejercicio que va a desaparecer**: no hay que elegir el objetivo en una segunda lista ni entrar y salir de un modo.
  - La acción de retirar se muestra **deshabilitada, con su causa**, cuando el ejercicio tiene series registradas, cuando el presupuesto está agotado o cuando quitarlo rompería la invariante de CA-43.05.
  - La lista de la sesión **distingue los ejercicios añadidos** de los que trajo el plan, con una marca y una línea que lo enuncia.
- **Flujo de navegación visual:** `Sesión activa → Añadir ejercicio → (elegir del Diccionario | crear nuevo con el formulario D5-T1) → vuelta a la sesión con el ejercicio al final de la lista`.
- **Mensajes y feedback:** al agotarse el presupuesto, indicación de que hace falta añadir un ejercicio primero; al intentar quitar un añadido con un retiro vigente, mensaje que pide reponer el ejercicio retirado; al intentar retirar un ejercicio con series, mensaje que nombra la causa.

### Sistemas Externos

Ninguno. El sistema es local y sin integraciones.

### Preview de Interfaz

**Preview:** [`43.preview.txt`](./43.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — entidades `session`, `session_exercise`, `exercise_set`, `exercise`, `plan_assignment`; enum `SessionStatus`, `RepsMode`; máquinas de estado §5.1 y §5.2.
- `docs/architecture/interfaces_contract.md` — triggers `E1-T1`, `E2-T1`, `E4-T1`, `D1-T1`, `D5-T1`, `F2-T1`.
- **ADR-019** — cambio de esquema resuelto por instalación fresca. Criterio heredado de `HU-39` (CA-39.11).

**Historias hermanas:**
- [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/historia.md) — **prerrequisito.** Aporta el formulario de ejercicio con selección múltiple de equipamiento y su validación.
- [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/historia.md) — **prerrequisito.** Aporta la jerarquía de zonas musculares en ese mismo formulario. Sin ella, el ejercicio creado desde la sesión nacería a medio catalogar.
- [`HU-40`](../HU-40-progresion-por-equipamiento/historia.md) — independiente. Las series del ejercicio añadido se clasifican con las reglas vigentes.
- [`HU-42`](../HU-42-1rm-estimado/historia.md) — independiente. Las series del ejercicio añadido califican para el 1RM con la misma regla.

**Historias relacionadas:**
- `HU-05` — determinación e inicio de sesión por rotación. La composición inicial sigue viniendo del plan.
- `HU-06` — registro de series en sesión activa. El ejercicio añadido registra series igual que los demás.
- `HU-09` — cierre de sesión y avance de rotación. La completitud considera los añadidos; la rotación no se altera.
- `HU-07`, `HU-20`, `HU-34` — **precedente en negativo.** La sustitución por grupo muscular se implementó, se corrigió y finalmente se **eliminó** por HU-34. Esta historia resuelve la misma necesidad —adaptar la sesión al día— sin reintroducir una segunda identidad del ejercicio dentro de la sesión.
- `HU-26` — alternativas por puesto. Sigue funcionando como hasta ahora; esta historia no la sustituye.
- `HU-03` — Diccionario de Ejercicios y creación de ejercicios personalizados. Su formulario se reutiliza.
- `HU-17` — historial de ejercicios y sesiones. Debe reflejar lo ejecutado.

**Lecciones aprendidas:**
- `HU-34` retiró la sustitución por grupo muscular porque mantenía **dos identidades** del mismo ejercicio dentro de la sesión (`original_exercise_id` frente a `exercise_id`), lo que complicaba cada consulta de progresión. Esta historia lo evita: el ejercicio añadido es simplemente un `session_exercise` más, sin puesto y sin ejercicio de origen.
- `HU-26` mostró que el intercambio de alternativa solo es seguro **con 0 series registradas**. La misma frontera se aplica aquí al retiro, por la misma razón.
- `HU-22` estableció que El Ejecutante quiere ver la sesión antes de comprometerse con ella. El presupuesto de esta historia extiende ese control al momento en que la sesión ya está en curso.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente
- [ ] Mensajes implementados
- [ ] Verificada la invariante de CA-43.05: el número de añadidos nunca queda por debajo del de retirados, en todas las secuencias de añadir y retirar
- [ ] Verificado que el plan por defecto queda intacto tras una sesión ajustada (CA-43.08)
- [ ] Verificado que el ejercicio creado desde la sesión permanece en el Diccionario y **no** en el plan
- [ ] Verificado que un añadido sin series cierra la sesión como `INCOMPLETE` (CA-43.07)
- [ ] Verificado que ningún ejercicio con series registradas puede retirarse
- [ ] Documentación de arquitectura actualizada: modelo de dominio, contrato de interfaces y versión de esquema
- [ ] `story_mapping_index.md` actualizado con la historia, sus hermanas y sus dependencias
