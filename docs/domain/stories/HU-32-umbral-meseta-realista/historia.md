# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que el sistema entienda que tres sesiones con el mismo peso no son un estancamiento, y que no todos los ejercicios ni todas las personas progresan al mismo ritmo,
**Para** dejar de recibir señales falsas de meseta que me empujan a subir carga antes de tiempo.

## Descripción

Hoy el sistema clasifica un ejercicio como "en meseta" tras **3 sesiones consecutivas** de mantenimiento o regresión. El umbral es un valor único, global e idéntico para todos los ejercicios.

Eso no refleja la realidad del entrenamiento. Tres sesiones son muy poco para subir el peso en cualquier ejercicio. Además hay ejercicios en los que progresar es intrínsecamente difícil — una elevación lateral avanza mucho más lento que una prensa, entre otras cosas porque el salto mínimo disponible representa un porcentaje mucho mayor de la carga. Y por encima de todo eso, el ritmo de progresión depende de cada persona: su metabolismo, su genética y su condición corporal.

Esta historia modela esas dos realidades:

- **La dificultad del ejercicio** pasa a ser un atributo del propio ejercicio, editable desde el Diccionario, que multiplica el umbral.
- **El ritmo de la persona** se modela como un umbral base configurable desde Ajustes, con 5 sesiones por defecto.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-32.01 — Umbral base de meseta ampliado

- **Dado** que El Ejecutante mantiene la misma carga en un ejercicio sesión tras sesión
- **Cuando** el sistema evalúa la condición de meseta
- **Entonces** el umbral base es de **5 sesiones consecutivas** de mantenimiento o regresión
- **Y** no de 3 como en el comportamiento actual

#### CA-32.02 — Dificultad de progresión como atributo del ejercicio

- **Dado** que los ejercicios difieren en su capacidad de progresión
- **Cuando** El Ejecutante consulta un ejercicio en el Diccionario
- **Entonces** el ejercicio expone un atributo **Dificultad de progresión** con valores `Baja`, `Media` o `Alta`
- **Y** El Ejecutante puede editarlo desde el detalle del ejercicio

#### CA-32.03 — Multiplicador por dificultad

- **Dado** que un ejercicio tiene una dificultad de progresión asignada
- **Cuando** el sistema evalúa la condición de meseta para ese ejercicio
- **Entonces** aplica el umbral base multiplicado según la dificultad, redondeando hacia arriba:

| Dificultad | Multiplicador | Sesiones con umbral base 5 |
|---|:---:|:---:|
| `Baja` | ×1 | 5 |
| `Media` | ×1.5 | 8 |
| `Alta` | ×2 | 10 |

### Escenario 2: Validaciones

#### CA-32.04 — Umbral base configurable

- **Dado** que cada persona progresa a un ritmo distinto según su metabolismo, genética y condición corporal
- **Cuando** El Ejecutante accede a Ajustes
- **Entonces** puede modificar el umbral base de meseta dentro del rango de **3 a 15 sesiones**
- **Y** el valor por defecto es 5
- **Y** el cambio aplica a las evaluaciones posteriores, sin recalcular estados de progresión ya asignados

#### CA-32.05 — Valor por defecto de dificultad

- **Dado** que se registran los ejercicios del catálogo seed y los que El Ejecutante crea manualmente
- **Cuando** no se especifica una dificultad de progresión
- **Entonces** el ejercicio recibe `Media` como valor por defecto
- **Y** ningún ejercicio queda sin dificultad asignada

#### CA-32.06 — Clasificación seed coherente

- **Dado** que el catálogo seed contiene ejercicios de aislamiento y compuestos
- **Cuando** se ejecuta el seeder en una instalación fresca
- **Entonces** los ejercicios de aislamiento de zonas pequeñas (elevaciones laterales, curls, extensiones de tríceps, face pull, vuelos posteriores) quedan clasificados como dificultad `Alta`
- **Y** los compuestos multiarticulares de tren inferior y de empuje o tracción pesada (prensa inclinada, sentadilla hack, peso muerto rumano, press de banca, remo T) quedan como `Baja`
- **Y** el resto queda como `Media`

### Escenario 3: Casos Extremos

#### CA-32.07 — Cambio de dificultad con evaluación en curso

- **Dado** que un ejercicio acumula 6 sesiones de mantenimiento con dificultad `Alta` (umbral 10)
- **Cuando** El Ejecutante cambia su dificultad a `Baja` (umbral 5)
- **Entonces** el sistema reevalúa la condición en la siguiente evaluación de progresión
- **Y** el contador acumulado se conserva, no se reinicia

#### CA-32.08 — El contador se reinicia con progresión positiva

- **Dado** que un ejercicio acumula sesiones de mantenimiento sin alcanzar su umbral
- **Cuando** El Ejecutante registra una sesión con progresión positiva
- **Entonces** el contador se reinicia a cero
- **Y** el ejercicio vuelve al estado de progresión activa
- **Y** este comportamiento no sufre regresión respecto al actual

#### CA-32.09 — Estados excluidos de la evaluación

- **Dado** que un ejercicio se encuentra en descarga o marcado como dominado
- **Cuando** el sistema evalúa la progresión
- **Entonces** la lógica de umbral de meseta no aplica
- **Y** el comportamiento actual para esos estados se conserva sin cambios

#### CA-32.10 — Documentación actualizada

- **Dado** que se incorpora el atributo de dificultad de progresión al ejercicio y el umbral configurable
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/domain_and_state_model.md` refleja el nuevo atributo en la entidad de ejercicio, su dominio cerrado de valores y su valor por defecto
- **Y** documenta el umbral base configurable como parámetro del sistema

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** Una alerta de meseta prematura empuja a subir carga antes de estar listo, lo que degrada la técnica y aumenta el riesgo de lesión. Peor aún, un sistema que emite señales falsas de forma sistemática deja de ser creíble: El Ejecutante empieza a ignorar todas sus alertas, incluidas las verdaderas.

### Reglas de Negocio

1. **La progresión no es uniforme entre ejercicios.** La dificultad de progresión es una propiedad del ejercicio: depende de su naturaleza (aislamiento o compuesto) y del salto mínimo disponible en su implemento respecto a la carga habitual.
2. **La progresión no es uniforme entre personas.** El ritmo depende del metabolismo, la genética y la condición corporal. Por eso el umbral base es configurable y no una constante del sistema.
3. **El umbral efectivo es la composición de ambas.** `umbral efectivo = techo(umbral base × multiplicador de dificultad)`.
4. **Toda dificultad tiene valor.** Ningún ejercicio queda sin clasificar; `Media` es el valor por defecto.
5. **Cambiar el umbral no reescribe el pasado.** Los estados de progresión ya asignados se conservan; el cambio rige desde la siguiente evaluación.
6. **La progresión positiva reinicia el contador.** Regla vigente que se conserva sin cambios.

### Interfaz

No se crea ninguna pantalla nueva.

- **Detalle de Ejercicio (Diccionario):** nuevo selector de **Dificultad de progresión** con tres opciones, editable.
- **Crear Ejercicio:** el mismo selector, con `Media` preseleccionado.
- **Ajustes:** nuevo control numérico para el **umbral base de meseta**, rango 3 a 15, valor por defecto 5, con texto explicativo de su efecto.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Se conserva la estructura de las pantallas de Diccionario y Ajustes. Los controles se integran en sus secciones existentes.
- **Campos y controles:** Selector de tres opciones para la dificultad de progresión, con área táctil mínima de 48×48 dp. Control numérico acotado para el umbral base en Ajustes.
- **Flujo de navegación visual:** Sin rutas nuevas.
- **Mensajes y feedback:** Texto explicativo junto al control de umbral base indicando qué significa y a qué afecta. Validación del rango 3 a 15 con mensaje específico si se sale de él.

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14).

### Preview de Interfaz

**Preview:** [`32.preview.txt`](./32.preview.txt) | **Formato:** ASCII (wireframe de texto)

Aplican los mockups existentes de Detalle de Ejercicio, Crear Ejercicio y Ajustes, con los controles descritos incorporados.

---

## Contexto y Referencias

**Dependencias:** **habilita a HU-33**, cuya familia de alertas de progresión consume este umbral dinámico.

**Arquitectura:** `docs/architecture/domain_and_state_model.md` (entidades `exercise`, `exercise_progression`; sección 4 Dominios Cerrados; sección 5.3 Ciclo de Vida de `exercise_progression`)

**Historias relacionadas:**

- **HU-12** — Motor de detección de regresión, meseta y necesidad de descarga. Esta historia modifica su umbral de meseta.
- **HU-10** — Evaluación y clasificación de progresión post-sesión. Su clasificación alimenta el contador.
- **HU-11** — Prescripción de carga por Doble Umbral. Consume el estado de progresión.
- **HU-14** — Protocolo de descarga. Estado excluido de la evaluación de meseta.
- **HU-03 y HU-24** — Diccionario de Ejercicios. Pantalla donde se edita el nuevo atributo.
- **HU-33** — Alertas comprensibles y accionables. Depende de esta historia.
- **HU-29** — Plan y catálogo actualizados. Su seeder debe incorporar la clasificación de dificultad.

**Restricciones transversales aplicables:**

- RNF29 — Motor de reglas de progresión como módulo independiente, testeable sin dependencias de Android
- RNF30 — Pruebas unitarias para todas las reglas de negocio críticas, incluida la de meseta
- RNF31 — Seed data en recursos versionados
- **Beta sin migración:** la base de datos se reinicia; los cambios de esquema se validan sobre instalación fresca. Excepción documentada a RNF19, limitada a esta historia.

**Lecciones aprendidas:** Un umbral global aplicado sobre una población heterogénea de ejercicios, con capacidades de progresión muy distintas entre sí, produce falsos positivos de forma sistemática. El coste de esos falsos positivos no es solo la señal errónea: es la pérdida de credibilidad de todo el sistema de alertas.

---

## Definición de Terminado (Inicial)

- [ ] Umbral base de meseta en 5, configurable desde Ajustes en el rango 3 a 15
- [ ] Atributo `Dificultad de progresión` en la entidad de ejercicio, con dominio cerrado y valor por defecto `Media`
- [ ] Atributo editable desde el detalle del ejercicio y asignable al crearlo
- [ ] Multiplicador ×1 / ×1.5 / ×2 aplicado con redondeo hacia arriba
- [ ] Clasificación de dificultad aplicada a los ejercicios del catálogo seed
- [ ] Contador conservado al cambiar la dificultad; reiniciado ante progresión positiva
- [ ] Estados de descarga y dominado excluidos, sin regresión
- [ ] Cambio de umbral sin recálculo de estados ya asignados
- [ ] Regla de umbral efectivo cubierta por pruebas unitarias
- [ ] `domain_and_state_model.md` actualizado
