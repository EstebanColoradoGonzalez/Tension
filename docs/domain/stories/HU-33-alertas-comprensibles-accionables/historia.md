# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que cada alerta del sistema me diga en lenguaje claro qué detectó, sobre qué, y qué debería hacer al respecto, con umbrales que reflejen cómo funciona el entrenamiento real,
**Para** que una alerta se traduzca en una decisión concreta en lugar de en ruido que termino ignorando.

## Descripción

El sistema de alertas (HU-18) emite señales en cinco familias: tasa de progresión, RIR fuera de rango, adherencia semanal, caída de tonelaje por grupo muscular e inactividad por módulo. Tiene tres problemas de fondo:

**1. Umbrales irreales.** Las ventanas de observación son demasiado cortas. Una semana mala de adherencia no es un problema de adherencia; una caída puntual de tonelaje entre dos microciclos no es una regresión. El sistema convierte fluctuación normal en alertas.

**2. Textos que no se entienden.** Las alertas describen la condición en términos del motor que la detectó, no en términos de lo que le pasa a El Ejecutante.

**3. No dicen qué hacer.** Una alerta informa de un problema pero no propone un siguiente paso, dejando en El Ejecutante el trabajo de interpretar la señal y decidir la acción.

Esta historia corrige los tres. Los umbrales de la familia de progresión se apoyan en el umbral dinámico que introduce HU-32; los de las otras cuatro familias se amplían con el mismo criterio de realismo.

Lo que **no** cambia: la diferenciación visual entre alerta y crisis se conserva, y las alertas siguen siendo estrictamente informativas, sin bloquear ninguna operación.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-33.01 — Texto comprensible

- **Dado** que el sistema emite cualquier alerta
- **Cuando** El Ejecutante la visualiza en el Centro de Alertas o en su detalle
- **Entonces** el texto explica en lenguaje natural **qué se detectó** y **sobre qué elemento** (ejercicio, rutina, grupo muscular o período)
- **Y** no expone identificadores internos, nombres de reglas ni terminología no explicada

#### CA-33.02 — Acción sugerida obligatoria

- **Dado** que el sistema emite cualquier alerta de cualquiera de las cinco familias
- **Cuando** El Ejecutante abre su detalle
- **Entonces** la alerta incluye un bloque diferenciado **"Qué puedes hacer"** con una acción sugerida concreta (ej. subir carga, reducir volumen, revisar técnica, cambiar por la alternativa del slot, iniciar descarga, retomar el módulo)
- **Y** cuando la acción es navegable, el bloque incluye un **acceso directo** que lleva a ejecutarla
- **Y** cuando la acción no es navegable (revisar técnica, descansar más), el bloque presenta únicamente el texto
- **Y** ninguna alerta se limita a describir el problema sin proponer un siguiente paso

#### CA-33.03 — Umbrales revisados de las cinco familias

- **Dado** que cada familia de alertas evalúa una condición sobre una ventana de observación
- **Cuando** se aplica esta historia
- **Entonces** los umbrales y ventanas quedan establecidos así:

| Familia | Alerta | Crisis | Ventana |
|---|---|---|---|
| **Tasa de progresión** | < 40% ponderado por dificultad del ejercicio | < 20% ponderado | 6 semanas (antes 4) |
| **RIR fuera de rango** | RIR promedio < 1.5 o > 3.5 | Condición sostenida | 3 sesiones consecutivas (antes 2) |
| **Adherencia semanal** | < 60% durante 2 semanas consecutivas (antes 1) | < 60% durante 3+ semanas consecutivas (antes 2) | Semanal |
| **Caída de tonelaje** | Caída > 15% (antes 10%) | Caída > 25% (antes 20%) | 2 microciclos consecutivos |
| **Inactividad por módulo** | > 14 días naturales (antes 10) | > 21 días naturales (antes 14) | Días naturales |

- **Y** cada familia declara explícitamente su umbral, su ventana y su justificación en la documentación

#### CA-33.04 — Ponderación de la tasa de progresión por dificultad

- **Dado** que un ejercicio tiene una dificultad de progresión asignada (HU-32)
- **Cuando** el sistema evalúa su tasa de progresión
- **Entonces** aplica el umbral ponderado según esa dificultad:

| Dificultad | Umbral de alerta | Umbral de crisis |
|---|:---:|:---:|
| `Baja` | < 40% | < 20% |
| `Media` | < 35% | < 15% |
| `Alta` | < 25% | < 10% |

- **Y** un ejercicio difícil de progresar no genera alerta por avanzar al ritmo que le corresponde

### Escenario 2: Validaciones

#### CA-33.05 — Alertas no bloqueantes

- **Dado** que existen alertas activas de cualquier familia y severidad
- **Cuando** El Ejecutante inicia una sesión, registra una serie o cierra una sesión
- **Entonces** ninguna operación queda bloqueada
- **Y** se conserva el comportamiento definido en HU-18

#### CA-33.06 — Severidad conservada

- **Dado** que el sistema define dos niveles de severidad, alerta y crisis, con diferenciación visual por color e iconografía
- **Cuando** se aplican los cambios de esta historia
- **Entonces** la distinción visual entre ambos niveles se conserva sin regresión
- **Y** la severidad sigue sin depender únicamente del texto

#### CA-33.07 — Retiro automático conservado

- **Dado** que una alerta activa deja de cumplir su condición
- **Cuando** el sistema reevalúa al cierre de la siguiente sesión
- **Entonces** la alerta se resuelve automáticamente
- **Y** el comportamiento de retiro definido en HU-18 no sufre regresión
- **Y** las alertas resueltas se conservan como histórico

### Escenario 3: Casos Extremos

#### CA-33.08 — Descarga planificada no genera alerta de tonelaje

- **Dado** que El Ejecutante se encuentra en un protocolo de descarga planificado
- **Cuando** el tonelaje cae por encima del umbral de alerta
- **Entonces** el sistema no emite alerta de caída de tonelaje
- **Y** el comportamiento de diferenciación entre descarga y regresión definido en HU-18 se conserva

#### CA-33.09 — Datos insuficientes para evaluar

- **Dado** que no existe historial suficiente para completar la ventana de observación de una familia
- **Cuando** el sistema evalúa esa familia
- **Entonces** no emite alerta
- **Y** no genera una alerta de "datos insuficientes"

#### CA-33.10 — Acción sugerida coherente con el estado del sistema

- **Dado** que una alerta sugiere cambiar un ejercicio por la alternativa de su slot
- **Cuando** ese ejercicio no pertenece a un slot con alternativa
- **Entonces** el sistema sugiere una acción aplicable en su lugar
- **Y** ninguna alerta propone una acción que El Ejecutante no pueda ejecutar

#### CA-33.11 — Documentación actualizada

- **Dado** que cambian los umbrales, los textos y el contenido de las alertas
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/interfaces_contract.md` refleja el contenido de la alerta incluyendo la acción sugerida
- **Y** los umbrales y ventanas de las cinco familias quedan documentados con su justificación

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** El sistema de alertas es el mecanismo por el que la aplicación aporta criterio y no solo registro. Una alerta que llega demasiado pronto, que no se entiende o que no propone nada no aporta criterio: entrena a El Ejecutante a ignorarla. Cuando eso ocurre, también se pierden las alertas que sí importaban.

### Reglas de Negocio

1. **Toda alerta propone una acción.** Una alerta que solo describe un problema no cumple su propósito. La acción sugerida es obligatoria en las cinco familias.
2. **La acción sugerida debe ser ejecutable.** El sistema no propone acciones que El Ejecutante no pueda realizar en su contexto actual.
3. **La ventana de observación debe ser suficiente.** Ninguna familia emite alertas con una ventana menor a la necesaria para que la señal sea distinguible de la fluctuación normal.
4. **La progresión se pondera por dificultad.** Un ejercicio de progresión difícil no se juzga con la vara de uno fácil.
5. **Las alertas nunca bloquean.** Se conserva la autonomía total de El Ejecutante sobre su entrenamiento.
6. **El lenguaje es el del ejecutante, no el del motor.** Las alertas se redactan en términos de lo que le ocurre a la persona, no de la regla que disparó.
7. **La descarga planificada no es una regresión.** Se conserva la exclusión vigente.

### Interfaz

No se crea ninguna pantalla nueva.

- **Centro de Alertas:** textos reescritos en todas las familias. Se conserva la organización, el filtrado y la diferenciación visual de severidad.
- **Detalle de Alerta:** se incorpora la sección de **acción sugerida**, obligatoria en toda alerta.
- **Resumen post-sesión:** las señales de acción heredan la misma redacción y criterio.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Se conserva la estructura de las pantallas de alertas. El cambio principal es de contenido, no de disposición, salvo por la sección de acción sugerida en el detalle.
- **Campos y controles:** Bloque diferenciado "Qué puedes hacer" dentro del detalle de la alerta, con el texto de la acción y, cuando la acción es navegable (ir al ejercicio, abrir el plan, iniciar descarga), un **acceso directo** que lleva a ejecutarla. Cuando la acción no es navegable (revisar técnica, descansar más), el bloque queda solo con texto. Área táctil mínima de 48×48 dp en el acceso directo.
- **Flujo de navegación visual:** Sin rutas nuevas. El acceso directo de la acción sugerida reutiliza rutas ya existentes (detalle de ejercicio, plan de entrenamiento, protocolo de descarga).
- **Mensajes y feedback:** Todos los textos de alerta se reescriben. La acción sugerida es visible sin necesidad de desplazamiento adicional en el detalle.

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14).

### Preview de Interfaz

**Preview:** [`33.preview.txt`](./33.preview.txt) | **Formato:** ASCII (wireframe de texto)

Aplican los mockups existentes de Centro de Alertas y Detalle de Alerta, con la sección de acción sugerida incorporada.

---

## Contexto y Referencias

**Dependencias:** **depende de HU-32** (CA-33.04: la ponderación de la tasa de progresión consume el atributo de dificultad del ejercicio).

**Arquitectura:** `docs/architecture/domain_and_state_model.md` (entidad `alert`), `docs/architecture/interfaces_contract.md`

**Historias relacionadas:**

- **HU-32** — Umbral de meseta realista por ejercicio. **Precondición.**
- **HU-18** — Sistema de Alertas. Esta historia revisa sus cinco familias. *Nota: HU-18 figura como `Todo` en el mapa de historias pese a estar implementada; la corrección de ese estado la realiza HU-34.*
- **HU-12** — Motor de detección de regresión, meseta y necesidad de descarga. Alimenta la familia de progresión.
- **HU-13** — Resumen post-sesión con señales de acción. Hereda el criterio de redacción.
- **HU-14** — Protocolo de descarga. Su condición excluye la alerta de caída de tonelaje.
- **HU-15** — Analítica y KPIs. Provee los cálculos que consumen las reglas de alerta.
- **HU-26** — Alternativas por slot. Determina si la acción sugerida de cambio de ejercicio es ejecutable.

**Restricciones transversales aplicables:**

- RNF05 — Señales del sistema visibles y comprensibles
- RNF08 — Interfaz en español
- RNF29 — Motor de reglas testeable sin dependencias de Android
- RNF30 — Pruebas unitarias para las reglas de alerta
- **Beta sin migración:** la base de datos se reinicia; no se requiere migración de datos.

**Lecciones aprendidas:** El coste de un falso positivo en un sistema de alertas no se paga en esa alerta, sino en las siguientes: una vez que El Ejecutante aprende que las alertas se disparan sin motivo, deja de leerlas todas. La calibración de umbrales es tan importante como la detección misma.

---

## Definición de Terminado (Inicial)

- [ ] Los umbrales y ventanas de las cinco familias actualizados según CA-33.03
- [ ] Tasa de progresión ponderada por dificultad del ejercicio según CA-33.04
- [ ] Todos los textos de alerta reescritos en lenguaje natural, sin identificadores ni terminología interna
- [ ] Bloque "Qué puedes hacer" presente en toda alerta de las cinco familias, con acceso directo cuando la acción es navegable
- [ ] Diferenciación visual de severidad conservada, sin regresión
- [ ] Comportamiento no bloqueante conservado, sin regresión
- [ ] Retiro automático de alertas conservado, sin regresión
- [ ] Exclusión de descarga planificada en la alerta de tonelaje, sin regresión
- [ ] Sin alertas emitidas cuando la ventana de observación está incompleta
- [ ] Reglas de umbral cubiertas por pruebas unitarias
- [ ] `interfaces_contract.md` actualizado con el contenido de la alerta y los umbrales justificados
