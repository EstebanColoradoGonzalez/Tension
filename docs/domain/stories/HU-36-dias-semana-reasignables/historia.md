# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que cada día de la semana tenga su propia identidad relacionada con la rutina que le toca, y poder reasignar puntualmente esa relación cuando falto un día,
**Para** continuar donde quedé sin desordenar mi programa ni perder el hilo de mi rotación.

## Descripción

El plan está organizado en 6 días de la semana y cada día tiene su propio enfoque. Hoy esa información vive **dentro del nombre de la rutina** — "Martes: Espalda, Bíceps y Abdomen (Pull - Foco Dorsal Ancho)" —, lo que la vuelve un dato de texto en lugar de una relación del modelo.

La consecuencia práctica es que no hay forma de expresar "hoy es jueves pero voy a hacer lo del miércoles porque ayer falté". El Ejecutante no puede continuar donde quedó.

Esta historia convierte el **día de la semana en una entidad del dominio**, relacionada con la rutina y el enfoque que le corresponden, y añade la capacidad de **reasignar esa relación temporalmente** para una sesión concreta.

Hay dos límites deliberados:

- **La rotación cíclica no se toca.** El sistema seguirá avanzando la posición del microciclo exactamente igual que hoy. La reasignación es una capa de conveniencia sobre la rotación, nunca una alteración de su estado. Esto protege el conteo de microciclos y el protocolo de descarga.
- **La reasignación es temporal.** Aplica solo a esa sesión y ese día. La relación permanente día → rutina no se modifica; cambiarla de forma permanente queda fuera de alcance.

Se modelan los **7 días**, con el domingo explícitamente como día sin rutina asignada.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-36.01 — El día es una entidad relacionada

- **Dado** que hoy el día de la semana forma parte del nombre textual de la rutina
- **Cuando** se aplica esta historia
- **Entonces** el día existe como entidad propia del dominio, relacionada con la rutina y el enfoque que le corresponden
- **Y** existen los **7 días de la semana**
- **Y** el domingo queda registrado como día **sin rutina asignada**
- **Y** el nombre de la rutina deja de depender del día para expresar a qué día pertenece

#### CA-36.02 — Reasignación temporal de la rutina de un día

- **Dado** que El Ejecutante faltó un día y quiere continuar donde quedó
- **Cuando** reasigna temporalmente la rutina de otro día a la sesión que va a iniciar
- **Entonces** esa sesión se genera con los ejercicios de la rutina reasignada
- **Y** la reasignación aplica **únicamente a esa sesión y ese día**, sin modificar la relación permanente día → rutina

#### CA-36.03 — La rotación no se altera

- **Dado** que El Ejecutante realiza una reasignación temporal
- **Cuando** cierra la sesión
- **Entonces** la posición del microciclo y el conteo de microciclos avanzan según la regla de rotación cíclica vigente
- **Y** el protocolo de descarga y el conteo de microciclos de HU-14 no se ven afectados

### Escenario 2: Validaciones

#### CA-36.04 — Reversión automática

- **Dado** que El Ejecutante reasignó temporalmente la rutina de un día
- **Cuando** inicia la siguiente sesión
- **Entonces** el sistema propone de nuevo la relación permanente día → rutina
- **Y** la reasignación anterior no persiste como configuración

#### CA-36.05 — Impacto visual mínimo

- **Dado** que El Ejecutante usa la aplicación con normalidad
- **Cuando** navega el inicio, el preview de sesión, la sesión activa y el plan
- **Entonces** la presentación de las sesiones se mantiene equivalente a la actual
- **Y** la reasignación temporal se ofrece como una acción explícita, no como un cambio en el flujo principal

#### CA-36.06 — Reasignación solo antes de iniciar

- **Dado** que El Ejecutante ya inició una sesión
- **Cuando** intenta reasignar la rutina de ese día
- **Entonces** el sistema no lo permite
- **Y** la reasignación solo está disponible antes de iniciar la sesión

### Escenario 3: Casos Extremos

#### CA-36.07 — Reasignar la rutina del domingo

- **Dado** que el domingo no tiene rutina asignada
- **Cuando** El Ejecutante quiere entrenar un domingo
- **Entonces** puede reasignar temporalmente la rutina de cualquier otro día a esa sesión
- **Y** la sesión se genera con normalidad
- **Y** el domingo conserva su condición de día sin rutina asignada

#### CA-36.08 — Reasignar a la rutina que ya correspondía

- **Dado** que El Ejecutante abre la reasignación y elige la misma rutina que ya correspondía a ese día
- **Cuando** confirma
- **Entonces** el sistema procede sin error
- **Y** el comportamiento es idéntico a no haber reasignado

#### CA-36.09 — Historial y métricas por rutina ejecutada

- **Dado** que una sesión se generó a partir de una rutina reasignada temporalmente
- **Cuando** El Ejecutante consulta el historial, el detalle de sesión o las métricas
- **Entonces** la sesión aparece asociada a la **rutina efectivamente ejecutada**
- **Y** no a la rutina que correspondía al día por relación permanente

#### CA-36.10 — Documentación actualizada

- **Dado** que se incorpora una entidad nueva al dominio
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/domain_and_state_model.md` refleja la entidad de día de la semana, su relación con la rutina, el dominio cerrado de sus valores y la semántica del override temporal
- **Y** `docs/architecture/architecture_blueprint.md` refleja el nuevo componente
- **Y** `docs/architecture/interfaces_contract.md` refleja la acción de reasignación temporal

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** Faltar un día es lo normal, no la excepción. Un sistema que no lo contempla obliga a El Ejecutante a elegir entre saltarse una rutina o desalinearse de su programa. Poder correr un día resuelve el caso más frecuente de la vida real sin cargar al ejecutante con la reorganización mental.

### Reglas de Negocio

1. **El día es una entidad, no una etiqueta.** Deja de vivir dentro del nombre textual de la rutina y pasa a ser una relación explícita del modelo.
2. **Existen los 7 días.** El domingo se modela explícitamente como día sin rutina, no como ausencia. Así el descanso es un concepto visible y el modelo queda abierto a asignarle rutina en el futuro sin cambiar su forma.
3. **La rotación cíclica es intocable.** La reasignación temporal es una capa de conveniencia sobre la rotación, nunca una alteración de su estado. La posición y el conteo de microciclos avanzan siempre según la regla vigente.
4. **Temporal significa una sesión.** La reasignación no persiste: la siguiente sesión vuelve a proponer la relación permanente.
5. **Solo antes de iniciar.** Una vez iniciada la sesión, la rutina queda fijada.
6. **El historial registra lo ejecutado.** Una sesión reasignada se asocia siempre a la rutina que realmente se entrenó.
7. **La reasignación permanente queda fuera de alcance.** Cambiar de forma estable qué rutina corresponde a qué día no forma parte de esta historia.

### Interfaz

No se crea ninguna pantalla nueva. Se añade una acción y se ajusta la presentación del día.

- **Inicio:** el día y la rutina propuesta se presentan como información relacionada. Se incorpora la acción de **reasignar la rutina de hoy**, disponible antes de iniciar la sesión.
- **Preview de sesión:** misma acción disponible.
- **Sesión activa:** sin cambios visibles. La acción ya no está disponible.
- **Pestaña Plan:** la relación día → rutina se presenta de forma explícita en lugar de embebida en el nombre.
- **Historial y detalle de sesión:** muestran la rutina efectivamente ejecutada.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Se conserva la presentación actual de las sesiones. El cambio de modelo no debe traducirse en un rediseño de las pantallas de inicio o sesión.
- **Campos y controles:** Acción explícita de reasignación, con área táctil mínima de 48×48 dp, que abre un selector de las rutinas disponibles.
- **Flujo de navegación visual:** Sin rutas nuevas. La reasignación se resuelve en un selector sobre la pantalla actual.
- **Mensajes y feedback:** Confirmación visible de que la reasignación aplica solo a la sesión de hoy. Indicación clara en el inicio y el preview cuando la sesión propuesta proviene de una reasignación temporal.

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14).

### Preview de Interfaz

**Preview:** [`36.preview.txt`](./36.preview.txt) | **Formato:** ASCII (wireframe de texto)

Aplican los mockups existentes de Inicio, Preview de Sesión y Plan de Entrenamiento, con la acción de reasignación incorporada y la relación día → rutina presentada de forma explícita.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/domain_and_state_model.md` (entidades `routine`, `session`, `rotation_state`; sección 4 Dominios Cerrados; sección 5.1 Ciclo de Vida de `session`), `docs/architecture/architecture_blueprint.md`, `docs/architecture/interfaces_contract.md`

**Historias relacionadas:**

- **HU-05** — Determinar e iniciar sesión según rotación cíclica. Su lógica de determinación incorpora el override temporal sin alterar la rotación.
- **HU-09** — Cerrar sesión y avanzar rotación. Debe avanzar igual con o sin reasignación.
- **HU-14** — Protocolo de descarga y conteo de microciclos. **No debe verse afectado.** Es el principal riesgo de regresión de esta historia.
- **HU-22** — Preview de sesión sin iniciar. Una de las pantallas donde se ofrece la reasignación.
- **HU-23** — Rutinas y versiones definidas por el usuario. Base del modelo de rutinas sobre el que se relaciona el día.
- **HU-17** — Historial de ejercicios y sesiones. Debe reflejar la rutina ejecutada.
- **HU-29** — Plan y catálogo actualizados. Establece los nombres de rutina que esta historia desacopla del día.

**Restricciones transversales aplicables:**

- RNF13 — Determinación de sesión confiable
- RNF28 — Arquitectura MVVM
- RNF29 — Motor de reglas testeable sin dependencias de Android — la resolución día → rutina con override es una regla pura
- RNF30 — Pruebas unitarias para las reglas de determinación de sesión y avance de rotación
- **Beta sin migración:** la base de datos se reinicia; los cambios de esquema se validan sobre instalación fresca. Excepción documentada a RNF19, limitada a esta historia.

**Lecciones aprendidas:** El acoplamiento del día de la semana al nombre textual de la rutina fue una simplificación válida mientras el plan era fijo. Desacoplarlo es la única forma de expresar excepciones sin recurrir a renombrar rutinas, pero debe hacerse sin tocar la rotación cíclica: esa es la pieza de la que dependen el conteo de microciclos y el protocolo de descarga.

**Riesgo declarado:** esta historia es la de mayor incertidumbre arquitectónica del conjunto. La frontera entre la relación permanente día → rutina, el override temporal y el estado de rotación debe quedar definida en el análisis arquitectónico antes de estimar.

---

## Definición de Terminado (Inicial)

- [ ] Día de la semana como entidad del dominio, con los 7 días y el domingo sin rutina asignada
- [ ] Relación explícita día → rutina, desacoplada del nombre textual de la rutina
- [ ] Reasignación temporal disponible antes de iniciar la sesión, en Inicio y Preview
- [ ] Reasignación limitada a una sola sesión, con reversión automática en la siguiente
- [ ] Posición y conteo de microciclos avanzando igual con y sin reasignación
- [ ] Protocolo de descarga de HU-14 sin regresión
- [ ] Reasignación no disponible con sesión ya iniciada
- [ ] Entrenar en domingo posible mediante reasignación, conservando su condición de día sin rutina
- [ ] Historial, detalle de sesión y métricas asociando la sesión a la rutina ejecutada
- [ ] Presentación de las sesiones equivalente a la actual, sin rediseño
- [ ] Reglas de resolución día → rutina y de avance de rotación cubiertas por pruebas unitarias
- [ ] `domain_and_state_model.md`, `architecture_blueprint.md` e `interfaces_contract.md` actualizados
