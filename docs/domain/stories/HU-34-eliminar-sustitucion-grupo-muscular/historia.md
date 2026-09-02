# Historia de Usuario

**Como** El Ejecutante,
**Quiero** contar con un único mecanismo confiable para cambiar de ejercicio durante una sesión,
**Para** dejar de usar uno que depende de una catalogación que nunca será lo bastante precisa y que me ofrece sustitutos que no trabajan lo mismo.

## Descripción

El sistema ofrece hoy dos formas de cambiar un ejercicio en sesión:

1. **Alternativa por slot (sistema "o", HU-26).** El plan declara explícitamente qué ejercicio puede reemplazar a cuál. Funciona bien porque la decisión ya está tomada por criterio de entrenamiento.
2. **Sustitución por grupo muscular (HU-07).** El sistema ofrece cualquier ejercicio del catálogo que comparta zona muscular con el actual.

El segundo mecanismo está roto de raíz, por tres motivos que se refuerzan entre sí:

- Depende de que **todos** los ejercicios estén perfectamente catalogados, algo que la propia HU-29 demuestra que no ocurre.
- Los grupos musculares son demasiado amplios: dos ejercicios de hombro pueden trabajar zonas completamente distintas del mismo músculo y no ser intercambiables.
- Requiere un diccionario con gran variedad de ejercicios por zona para que la oferta sea útil.

No es un defecto corregible: es una consecuencia del enfoque. Esta historia lo **elimina** y deja el sistema de alternativas por slot como único mecanismo.

Como efecto colateral, **HU-20** — que era precisamente la corrección de este sistema — queda sin objeto y se cancela. Esta historia aprovecha para corregir además las inconsistencias acumuladas en el mapa de historias.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-34.01 — El sistema de sustitución por zona muscular se elimina

- **Dado** que existe una funcionalidad para reemplazar un ejercicio de la sesión por otro de la misma zona muscular
- **Cuando** se aplica esta historia
- **Entonces** esa funcionalidad se elimina por completo: su punto de entrada, su pantalla y su ruta de navegación
- **Y** El Ejecutante ya no puede acceder a ella desde la sesión activa

#### CA-34.02 — El sistema de alternativas por slot se conserva

- **Dado** que un ejercicio de la sesión pertenece a un slot con alternativa declarada en el plan
- **Cuando** El Ejecutante lo intercambia antes de registrar su primera serie
- **Entonces** el comportamiento definido en HU-26 funciona sin regresión
- **Y** es el único mecanismo disponible para cambiar un ejercicio durante la sesión

### Escenario 2: Validaciones

#### CA-34.03 — Ausencia de rastros en la interfaz

- **Dado** que El Ejecutante recorre la sesión activa, el historial de sesiones y el detalle de sesión
- **Cuando** visualiza cualquier ejercicio
- **Entonces** no aparecen controles, etiquetas ni indicadores referidos a la sustitución por grupo muscular
- **Y** los ejercicios de slots simples se muestran sin ningún control de cambio

#### CA-34.04 — Personalización del plan no afectada

- **Dado** que El Ejecutante quiere cambiar un ejercicio de forma permanente
- **Cuando** accede a la gestión del plan
- **Entonces** puede seguir asignando, removiendo y sustituyendo ejercicios en sus versiones de rutina
- **Y** esta historia no afecta a la personalización del plan, solo al cambio puntual durante la sesión

### Escenario 3: Casos Extremos

#### CA-34.05 — Mapa de historias corregido

- **Dado** que el mapa de historias presenta inconsistencias acumuladas
- **Cuando** se actualiza `docs/domain/stories/story_mapping_index.md`
- **Entonces** se incorpora **HU-27**, que hoy no figura en el índice
- **Y** se corrige el estado de **HU-18**, que figura como `Todo` pese a estar implementada
- **Y** **HU-20** queda marcada como `Cancelada — superada por HU-34`
- **Y** **HU-07** queda anotada como funcionalidad retirada
- **Y** el **RF16** se reasigna al sistema de alternativas por slot de HU-26
- **Y** se incorporan **HU-28 a HU-36** con su épica, estado y prioridad

#### CA-34.06 — Documentación arquitectónica actualizada

- **Dado** que se retira un componente del sistema
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/architecture_blueprint.md` refleja la eliminación del componente de sustitución por grupo muscular
- **Y** `docs/architecture/interfaces_contract.md` refleja la eliminación de la pantalla y su ruta de navegación

#### CA-34.07 — Sin código huérfano

- **Dado** que se elimina la funcionalidad
- **Cuando** se completa el retiro
- **Entonces** no permanecen en el proyecto pantallas, casos de uso, consultas ni rutas que solo servían a la sustitución por grupo muscular
- **Y** las consultas por zona muscular que sí usan otras funcionalidades (métricas, alertas de tonelaje) se conservan intactas

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** Un mecanismo que ofrece sustitutos inadecuados es peor que no ofrecer ninguno: induce a cambiar un ejercicio por otro que no trabaja lo mismo, degradando el estímulo de la sesión sin que El Ejecutante lo perciba. Retirarlo deja un único camino, explícito y confiable.

### Reglas de Negocio

1. **Un único mecanismo de cambio de ejercicio en sesión.** Tras esta historia, el sistema de alternativas por slot es la única vía.
2. **La alternativa la decide el plan, no el catálogo.** Un ejercicio solo puede reemplazar a otro si esa equivalencia fue declarada explícitamente al definir el plan.
3. **Cambio puntual y cambio permanente son cosas distintas.** Esta historia solo retira el cambio puntual por zona muscular; la personalización del plan permanece intacta.
4. **Retirar es retirar.** No queda funcionalidad latente ni accesos ocultos.
5. **La zona muscular sigue siendo útil para otras cosas.** Las métricas por grupo muscular y las alertas de caída de tonelaje siguen dependiendo de ella y no se ven afectadas.

### Interfaz

- **Sesión activa:** desaparece el punto de entrada a la sustitución por grupo muscular. Los slots simples quedan sin control de cambio; los slots duales conservan su control de intercambio de HU-26.
- **Pantalla de sustitución por grupo muscular:** se elimina, junto con su ruta de navegación.
- **Historial y detalle de sesión:** desaparece cualquier indicador referido a este mecanismo.
- **Gestión del plan:** sin cambios.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Se conserva la estructura de la sesión activa. La eliminación del control no debe dejar espacios vacíos ni desalinear la tarjeta del ejercicio.
- **Campos y controles:** Se retira el control de sustitución. Se conserva el control de intercambio de alternativa de slot.
- **Flujo de navegación visual:** Se elimina una ruta. Ninguna otra ruta cambia.
- **Mensajes y feedback:** Se retiran los mensajes propios de la funcionalidad eliminada.

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14).

### Preview de Interfaz

**Preview:** [`34.preview.txt`](./34.preview.txt) | **Formato:** ASCII (wireframe de texto)

Aplica el mockup existente de Sesión Activa sin el control de sustitución. Se retira el mockup de la pantalla de sustitución por grupo muscular.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md`, `docs/architecture/interfaces_contract.md`

**Historias relacionadas:**

- **HU-07** — Sustituir ejercicio puntualmente en sesión activa. **Funcionalidad eliminada por esta historia.**
- **HU-20** — Corrección de sustitución de ejercicios en sesión activa. **Cancelada por esta historia**: corregía un sistema que deja de existir.
- **HU-26** — Alternativas por puesto en el plan. Mecanismo que se conserva como único.
- **HU-29** — Plan y catálogo actualizados. Evidencia el problema de catalogación que motiva esta eliminación.
- **HU-15** — Analítica y KPIs. Consume zona muscular; no se ve afectada.
- **HU-33** — Alertas comprensibles. Su familia de tonelaje consume zona muscular; no se ve afectada.

**Trazabilidad de requisitos:** el **RF16** (sustituir ejercicio puntualmente en sesión activa) deja de ser satisfecho por HU-07 y se reasigna a HU-26, que cubre la misma necesidad de negocio mediante alternativas declaradas en el plan.

**Restricciones transversales aplicables:**

- RNF28 — Arquitectura MVVM
- **Beta sin migración:** la base de datos se reinicia; no se requiere migración de datos ni tratamiento de sesiones históricas con sustituciones previas.

**Lecciones aprendidas:** Una funcionalidad cuya calidad depende de la exhaustividad y precisión de un catálogo mantenido a mano es frágil por diseño, y ninguna corrección puntual la arregla. El sistema de alternativas explícitas por slot resuelve la misma necesidad de negocio trasladando la decisión al momento en que se define el plan, donde hay criterio y tiempo para tomarla bien.

---

## Definición de Terminado (Inicial)

- [ ] Punto de entrada, pantalla y ruta de sustitución por grupo muscular eliminados
- [ ] Sin controles, etiquetas ni indicadores residuales en sesión activa, historial y detalle de sesión
- [ ] Sistema de alternativas por slot funcionando sin regresión
- [ ] Personalización del plan sin regresión
- [ ] Sin código huérfano; consultas por zona muscular usadas por métricas y alertas conservadas
- [ ] `architecture_blueprint.md` e `interfaces_contract.md` actualizados
- [ ] `story_mapping_index.md` corregido: HU-27 incorporada, HU-18 con su estado real, HU-20 cancelada, HU-07 anotada como retirada, RF16 reasignado a HU-26, HU-28 a HU-36 mapeadas
