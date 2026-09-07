# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que el sistema compare mi peso únicamente contra el del mismo implemento y consolide la progresión del ejercicio,
**Para** que estrenar una polea no se lea como un bache ni me baje la carga prescrita.

> **Hija 2 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/index.md), [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/index.md), [`HU-42`](../HU-42-1rm-estimado/index.md), [`HU-43`](../HU-43-ajustar-sesion-del-dia/index.md). **Depende de `HU-39`, que debe estar implementada antes.**

## Descripción

`HU-39` ya registra con qué implemento se hizo cada serie. Pero el motor de decisión sigue comparando **por ejercicio**, y ahí está el defecto que motiva toda la partición.

El peso que se maneja en un mismo movimiento **no es comparable entre implementos**: la tensión de una polea, la estabilización que exige una mancuerna y la trayectoria guiada de una máquina dan cargas efectivas distintas para el mismo esfuerzo. Cuando El Ejecutante entrena con polea un día en que normalmente usa mancuerna, registra un peso menor por razones puramente mecánicas. El sistema lo compara contra el histórico del ejercicio y lo clasifica como `REGRESSION`, incrementa `sessions_without_progression`, acerca la meseta y baja la carga prescrita. **Diagnostica un bache que no existe.**

Esta historia cambia la **unidad de comparación** del motor: todo lo que compare pesos entre sesiones pasa a resolverse por **par (ejercicio, equipamiento)**. El estado de progresión, la carga prescrita del Doble Umbral, el contador de estancamiento, la clasificación de la sesión, la memoria del último peso, la unidad de captura preseleccionada y la reducción de descarga dejan de ser atributos del ejercicio y pasan a ser atributos del par.

Pero aislar los pares sin más tendría un coste: alternando dos implementos, cada historia avanzaría a la mitad de velocidad y la meseta tardaría el doble en detectarse. Por eso la historia añade una **lectura consolidada por ejercicio**, con dos reglas booleanas complementarias:

- **El ejercicio progresa si alguno de sus pares progresó** en su última ejecución — disyunción. Es lo que impide que alternar implementos diluya la progresión general.
- **El ejercicio entra en meseta solo si todos sus pares están estancados** — conjunción. Es lo que impide declarar una meseta que un implemento desmiente.

Los KPIs comparativos, la tasa de progresión y las alertas leen esa consolidación, no los pares sueltos.

**No se introduce ningún factor de equivalencia entre implementos.** El sistema no intenta traducir 20 kg de polea a un peso equivalente en mancuerna. Los pares se comparan consigo mismos y se relacionan por regla booleana, no por conversión de carga: cualquier factor exigiría una calibración manual por ejercicio que nadie puede sostener con fiabilidad.

### Frontera de alcance

- **El tonelaje total no se segmenta.** El volumen es volumen: sigue sumando todas las series sin distinguir implemento. Solo cambian los indicadores que **comparan** peso entre sesiones.
- **Ningún indicador cambia de fórmula.** Cambia la unidad de comparación, no la definición del KPI.
- **El reparto de tonelaje por zona muscular** —y por tanto la jerarquía principal/secundaria— es alcance de [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/historia.md).
- **La dificultad de progresión sigue siendo del ejercicio**, no del implemento: es una propiedad del movimiento.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-40.01 — La progresión se persiste por par

- **Dado** que El Ejecutante registra la primera serie de un ejercicio con un equipamiento dado
- **Cuando** el sistema crea el estado de progresión
- **Entonces** lo crea para el **par (ejercicio, equipamiento)**, no para el ejercicio
- **Y** son **por par**: el estado del ciclo de vida (`NO_HISTORY` / `IN_PROGRESSION` / `IN_PLATEAU` / `IN_DELOAD` / `MASTERED`), la carga prescrita del Doble Umbral y el contador de sesiones sin progresión
- **Y** el umbral efectivo de meseta se sigue calculando con la **dificultad de progresión del ejercicio** —es un atributo del movimiento, no del implemento—, pero se **compara contra el contador de cada par**
- **Y** un ejercicio entrenado con tres implementos mantiene **tres** estados de progresión independientes

#### CA-40.02 — Clasificación por par: sin regresión falsa

- **Dado** un ejercicio con historial en `Mancuerna` y ninguna serie previa en `Polea`
- **Cuando** El Ejecutante lo entrena por primera vez con `Polea`, con un peso menor
- **Entonces** al cerrar la sesión la clasificación de ese ejercicio es **`NULL` (Sin Historial)** y **nunca `REGRESSION`**
- **Y** el contador de sesiones sin progresión del par `(ejercicio, Polea)` arranca en **0**
- **Y** el estado del par `(ejercicio, Mancuerna)` **no se altera** por esa sesión
- **Y** la carga prescrita de `Mancuerna` **no se reduce**
- **Y** en las sesiones siguientes, la clasificación del ejercicio compara **solo contra la última sesión del mismo par**
- **Y** si una misma sesión registra series de dos implementos del mismo ejercicio, la clasificación se resuelve **por cada par** y se consolida según CA-40.04

#### CA-40.03 — Precedencia del valor precargado y de la unidad, por par

- **Dado** que El Ejecutante abre el formulario de registro de serie con un equipamiento seleccionado
- **Cuando** el sistema resuelve el **peso precargado**
- **Entonces** aplica la precedencia vigente, pero **resuelta sobre el par (ejercicio, equipamiento)** y no sobre el ejercicio:
  1. la carga prescrita por el Doble Umbral **para ese par**, mientras siga activa
  2. el peso de la serie anterior **del mismo par** en la sesión actual
  3. el peso de la última serie **del mismo par** en su sesión cerrada más reciente
  4. campo vacío
- **Y** la **unidad de captura** (`Kg` / `Lb`) se preselecciona con la de la última serie **del mismo par**
- **Y** al cambiar de equipamiento en el selector, el peso precargado y la unidad **se recalculan** para el par nuevo
- **Y** si el par no tiene historial, el campo queda **vacío** — nunca se precarga el peso de otro equipamiento
- **Y** en microciclo de descarga, la carga de descarga del par conserva su prioridad sobre la memoria
- **Y** el valor precargado sigue siendo **siempre editable**: el sistema sugiere, no impone

#### CA-40.04 — Lectura consolidada por ejercicio

- **Dado** un ejercicio entrenado con más de un equipamiento
- **Cuando** el sistema necesita saber si **el ejercicio** progresa —para KPIs, tasa de progresión y alertas—
- **Entonces** lo considera **en progresión si alguno** de sus pares progresó en su última ejecución
- **Y** esa lectura consolidada es la que consumen la tasa de progresión, los KPIs comparativos y las alertas `LOW_PROGRESSION_RATE` y `PLATEAU`
- **Y** alternar implementos **no diluye** la progresión general del ejercicio
- **Y** la consolidación **no crea** un estado persistido adicional: se deriva de los estados de los pares

#### CA-40.05 — Meseta solo si todos los pares están estancados

- **Dado** un ejercicio con dos pares, uno estancado por encima de su umbral efectivo y otro en progresión
- **Cuando** el motor evalúa la condición de meseta al cierre de sesión
- **Entonces** el ejercicio **no** se declara en meseta
- **Y** **no** se emite alerta `PLATEAU`
- **Pero cuando** **todos** los pares del ejercicio han alcanzado su umbral efectivo
- **Entonces** el ejercicio se declara en meseta y se emite la alerta
- **Y** la alerta **identifica qué pares** están estancados, para que la recomendación sea accionable
- **Y** la alerta `ROUTINE_REQUIRES_DELOAD` cuenta como estancado únicamente el ejercicio cuya condición consolidada de meseta se cumple

#### CA-40.06 — Descarga por par

- **Dado** un microciclo de descarga activo
- **Cuando** se aplica la reducción de carga
- **Entonces** el 60% de descarga y el 90% de reinicio posterior se calculan **sobre la carga de cada par**, de forma independiente
- **Y** las series de sesiones de descarga se siguen **excluyendo** de la memoria del último peso, ahora por par
- **Y** un par sin historial no recibe carga de descarga: no hay carga previa que reducir

#### CA-40.07 — KPIs y tonelaje

- **Dado** el panel de Métricas y el volumen por grupo muscular
- **Cuando** se agregan los indicadores
- **Entonces** el **tonelaje total** sigue sumando todas las series, sin distinguir equipamiento — el volumen es volumen
- **Y** los indicadores que **comparan peso entre sesiones** (tasa de progresión, tendencia de progresión) se calculan **por par** y se consolidan según CA-40.04
- **Y** ningún indicador existente cambia de definición: cambia la **unidad de comparación**, no la fórmula
- **Y** los KPIs siguen respetando sus reglas vigentes de suficiencia de datos: un par sin historial suficiente se presenta como tal, no como cero

---

### Escenario 2: Casos Extremos

#### CA-40.08 — Ejercicio entrenado con un solo equipamiento

- **Dado** un ejercicio que admite cuatro equipamientos pero que El Ejecutante siempre ha hecho con uno
- **Cuando** consulta su progresión, su carga prescrita y sus métricas
- **Entonces** ve **un solo** par, sin entradas vacías ni estados sin datos
- **Y** la lectura consolidada del ejercicio (CA-40.04) coincide con la de su único par
- **Y** la condición de meseta (CA-40.05) se evalúa sobre ese único par
- **Y** el comportamiento observable es **idéntico** al del sistema anterior a esta historia: para quien no alterna implementos, nada cambia

#### CA-40.09 — Respaldo y restauración

- **Dado** el mecanismo de respaldo y restauración (`J2-T1`, `J3-T1`)
- **Cuando** se exporta un respaldo tras esta historia
- **Entonces** el respaldo incluye la **progresión de cada par**: estado, carga prescrita y contador de sesiones sin progresión
- **Y** un respaldo restaurado reproduce el estado del motor de decisión **par por par**, sin recalcularlo
- **Y** los respaldos del formato anterior a `HU-39` siguen siendo **incompatibles** y se rechazan con mensaje explícito

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — actor único del sistema. Aplicación *single-user* sin roles.
- **Permisos requeridos:** ninguno. No existe modelo de permisos.
- **Valor de negocio:** **aquí se paga el valor central de la partición.** Se elimina el falso diagnóstico de regresión que hoy baja la carga prescrita y declara mesetas inexistentes. El motor de decisión vuelve a comparar cosas comparables, y la progresión general no se penaliza por entrenar con el implemento que el gimnasio permita ese día.

### Reglas de Negocio

1. **La unidad de comparación de peso es el par (ejercicio, equipamiento).** Todo lo que compare pesos entre sesiones se resuelve por par.
2. **Estrenar un implemento nunca es regresión.** Sin historial en el par, la clasificación es «Sin Historial».
3. **La progresión general se consolida por disyunción.** El ejercicio progresa si alguno de sus pares progresó.
4. **La meseta se consolida por conjunción.** El ejercicio entra en meseta solo si todos sus pares están estancados.
5. **Sin factores de equivalencia.** El sistema no traduce carga entre implementos.
6. **La dificultad de progresión es del ejercicio.** El umbral efectivo se calcula con ella, pero se compara contra el contador de cada par.
7. **El volumen no se segmenta.** El tonelaje total suma todas las series con independencia del implemento.
8. **La consolidación se deriva, no se persiste.** No hay un estado consolidado adicional que pueda desincronizarse.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| Registro de serie | `E2-T1` | El peso precargado y la unidad se resuelven por par y se recalculan al cambiar de implemento |
| Resumen post-sesión | `E5-T1` | La clasificación mostrada proviene de la comparación por par |
| Cierre de sesión | `E4-T1` | El motor evalúa progresión, meseta y carga prescrita por par |
| Panel de Métricas | `G1-T1` | La tasa de progresión consume la lectura consolidada |
| Tendencia de progresión | `G3-T1` | Comparación por par, consolidada por ejercicio |
| Centro de Alertas | `H1-T1`, `H2-T1` | `PLATEAU` y `LOW_PROGRESSION_RATE` leen la consolidación; el detalle nombra los pares |
| Estado de Descarga | `I1-T2` | Reducción y reinicio por par |
| Respaldo / Restauración | `J2-T1`, `J3-T1` | Formato ampliado con la progresión por par |

#### Detalle de Interfaz de Usuario

- **Diseño general:** ninguna pantalla nueva. Los cambios son de **contenido y de origen del dato** en pantallas existentes de los flujos E (Sesión Activa), G (Métricas), H (Alertas) e I (Descarga).
- **Campos y controles:** sin controles nuevos. El campo de peso y el selector de unidad ya existen; lo que cambia es el valor con el que se precargan y su recálculo al cambiar de implemento.
- **Flujo de navegación visual:** sin rutas nuevas.
- **Mensajes y feedback:** el detalle de la alerta `PLATEAU` nombra los pares estancados; la clasificación «Sin Historial» se muestra al estrenar un implemento, en lugar de una regresión.

### Sistemas Externos

Ninguno. El sistema es local y sin integraciones.

### Preview de Interfaz

**Preview:** [`40.preview.txt`](./40.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — entidades `exercise_progression`, `exercise_set`, `session_exercise`, `deload`; enums `ProgressionClassification`, `ExerciseProgressionStatus`, `WeightUnit`; máquina de estados §5.3.
- `docs/architecture/interfaces_contract.md` — triggers `E2-T1`, `E4-T1`, `E5-T1`, `G1-T1`, `G3-T1`, `H1-T1`, `H2-T1`, `I1-T2`, `J2-T1`, `J3-T1`.
- **ADR-019** — cambio de esquema resuelto por instalación fresca. Criterio heredado de `HU-39` (CA-39.11).

**Historias hermanas:**
- [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/historia.md) — **prerrequisito obligatorio.** Aporta el equipamiento persistido en la serie, sin el cual no existe el par.
- [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/historia.md) — aporta el reparto de tonelaje por zona con jerarquía. Independiente de esta.
- [`HU-42`](../HU-42-1rm-estimado/historia.md) — independiente. Paralelizable.
- [`HU-43`](../HU-43-ajustar-sesion-del-dia/historia.md) — independiente de esta.

**Historias relacionadas:**
- `HU-10` — evaluación y clasificación de progresión post-sesión. **Aquí vive el defecto que motiva la historia.**
- `HU-11` — prescripción de carga por Regla de Doble Umbral. Pasa a resolverse por par.
- `HU-12` — motor de detección de regresión, meseta y necesidad de descarga.
- `HU-14` — protocolo de descarga y conteo de microciclos.
- `HU-15`, `HU-35` — KPIs y métricas comprensibles.
- `HU-18`, `HU-33` — sistema de alertas y alertas accionables.
- `HU-30` — captura en Kg/Lb. La unidad preseleccionada pasa a resolverse por par.
- `HU-31` — memoria del último peso. Pasa a resolverse por par.
- `HU-32` — umbral de meseta realista por ejercicio. La dificultad sigue siendo del ejercicio; el contador pasa a ser del par.
- `HU-34` — dejó `session_exercise.exercise_id` como el ejercicio efectivamente ejecutado, lo que simplifica la resolución del par.

**Lecciones aprendidas:**
- `HU-31` estableció que la precarga debe **acompañar la progresión** en lugar de volver a un valor obsoleto. La precedencia por par extiende ese mismo principio: nunca precargar el peso de otro implemento.
- `HU-32` mostró que un contador de estancamiento **agnóstico del umbral** —que acumula siempre y solo se compara al cierre— permite cambiar la dificultad sin descartar lo acumulado. La segmentación por par preserva esa propiedad.
- `HU-33` enseñó que una alerta sin la entidad concreta que la origina no es accionable. De ahí que `PLATEAU` deba nombrar los pares estancados.

---

## Definición de Terminado (Inicial)

- [x] Funcionalidad implementada según criterios de aceptación
- [x] Validaciones funcionando correctamente
- [x] Mensajes implementados
- [~] Verificado el caso de CA-40.02 de extremo a extremo: primera serie en un implemento nuevo con peso menor no produce `REGRESSION` ni reduce la carga prescrita del otro par — cubierto por `ProgresionPorEquipamientoTest`, que **compila pero no se ejecutó** (sin emulador en la sesión)
- [x] Verificadas las dos reglas de consolidación: disyunción para progresión (CA-40.04) y conjunción para meseta (CA-40.05) — `ProgressionConsolidationRuleTest`, en JVM y pasando
- [x] Verificado que el comportamiento observable no cambia para un ejercicio con un solo implemento entrenado (CA-40.08) — resumen, velocidad de carga y narrativa de alerta, en JVM y pasando
- [x] Documentación de arquitectura actualizada: modelo de dominio, contrato de interfaces y versión de esquema
- [x] `story_mapping_index.md` actualizado con la historia, sus hermanas y sus dependencias
