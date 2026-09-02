# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que el peso que la aplicación me precarga al registrar una serie sea el que realmente vengo manejando en ese ejercicio,
**Para** que el sistema acompañe mi progresión en lugar de devolverme una y otra vez el valor con el que empecé.

## Descripción

Al iniciar una sesión, el tonelaje que aparece por defecto en cada ejercicio es el que se registró la primera vez. Conforme El Ejecutante va aumentando la carga, esa precarga se queda atrás y deja de ser útil: hay que reescribirla en cada serie.

El comportamiento correcto es que el sistema **recuerde el último peso manejado**: el de la serie anterior si ya se registró alguna en la sesión actual, o el de la sesión anterior si se trata de la primera serie del ejercicio.

Hay un matiz importante. El sistema ya tiene un motor de prescripción de carga (Regla de Doble Umbral, HU-11) que decide cuándo subir el peso. Esa prescripción **conserva su prioridad**: la memoria del último peso no la sustituye, la respalda para cuando no hay prescripción activa. El defecto real no es que el motor prescriba, sino que cuando no lo hace el sistema cae a un valor obsoleto.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-31.01 — Precedencia del valor precargado

- **Dado** que El Ejecutante abre la pantalla de registro de una serie
- **Cuando** el sistema determina el peso a precargar
- **Entonces** aplica esta precedencia estricta:
  1. La carga prescrita por el motor de Doble Umbral, si existe y es mayor que cero
  2. El peso de la **serie anterior del mismo ejercicio en la sesión actual**
  3. El peso de la **última serie del mismo ejercicio en su sesión previa**
  4. Campo vacío
- **Y** la prescripción del motor de progresión conserva su prioridad y no queda desactivada

#### CA-31.02 — Memoria dentro de la sesión

- **Dado** que El Ejecutante registró la serie 1 de un ejercicio con 40 kg y no hay carga prescrita activa
- **Cuando** abre el registro de la serie 2 de ese mismo ejercicio
- **Entonces** el campo de peso aparece precargado con 40 kg
- **Y** si en la serie 2 registra 42.5 kg, la serie 3 aparece precargada con 42.5 kg

#### CA-31.03 — Memoria entre sesiones

- **Dado** que El Ejecutante inicia la primera serie de un ejercicio y no hay carga prescrita activa
- **Cuando** ese ejercicio tiene series registradas en una sesión anterior
- **Entonces** el campo aparece precargado con el peso de la **última serie registrada** de ese ejercicio en la sesión anterior
- **Y** no con el peso de la primera vez que se registró el ejercicio

### Escenario 2: Validaciones

#### CA-31.04 — La prescripción del motor tiene prioridad

- **Dado** que el motor de Doble Umbral prescribió una carga para un ejercicio
- **Cuando** El Ejecutante abre el registro de la primera serie de ese ejercicio
- **Entonces** el campo aparece precargado con la carga prescrita
- **Y** no con el peso de la sesión anterior, aunque este exista
- **Y** el comportamiento de prescripción definido en HU-11 no sufre regresión

#### CA-31.05 — Coherencia entre precarga y unidad activa

- **Dado** que un ejercicio tiene `Lb` como unidad persistida y su peso previo fue 20.41 kg
- **Cuando** el sistema precarga el valor
- **Entonces** lo muestra convertido a libras (45.0 lb) con la unidad `Lb` seleccionada
- **Y** el valor mostrado coincide con lo que El Ejecutante capturó originalmente

#### CA-31.06 — Ejercicios sin carga externa

- **Dado** que el ejercicio está marcado como de peso corporal o isométrico
- **Cuando** El Ejecutante abre la pantalla de registro de serie
- **Entonces** el comportamiento actual se conserva sin cambios: campo de peso en cero y no editable
- **Y** la lógica de memoria de peso no aplica

### Escenario 3: Casos Extremos

#### CA-31.07 — Sin historial previo

- **Dado** que El Ejecutante registra un ejercicio por primera vez y no existe carga prescrita
- **Cuando** abre la pantalla de registro de serie
- **Entonces** el campo de peso aparece vacío
- **Y** el sistema no impide el registro por esta condición

#### CA-31.08 — Ejercicio sustituido por su alternativa de slot

- **Dado** que El Ejecutante intercambia un ejercicio por la alternativa de su slot antes de registrar la primera serie
- **Cuando** abre el registro de serie del ejercicio alternativo
- **Entonces** la precarga se resuelve con el historial del **ejercicio alternativo**, no del primario
- **Y** si el alternativo no tiene historial, el campo aparece vacío

#### CA-31.09 — Sesión en descarga

- **Dado** que El Ejecutante se encuentra en un protocolo de descarga
- **Cuando** abre el registro de serie de un ejercicio
- **Entonces** la carga de descarga calculada conserva su prioridad sobre la memoria del último peso
- **Y** el comportamiento definido en HU-14 no sufre regresión

#### CA-31.10 — El valor precargado es editable

- **Dado** que el sistema precarga un peso por cualquiera de los cuatro niveles de precedencia
- **Cuando** El Ejecutante decide usar otra carga
- **Entonces** puede modificar libremente el valor antes de confirmar
- **Y** el sistema no impone ni bloquea el valor precargado

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** El registro de serie es la interacción más repetida de toda la aplicación: ocurre decenas de veces por sesión. Precargar un valor obsoleto obliga a reescribirlo cada vez. Precargar el valor correcto convierte el registro en una confirmación de un solo gesto.

### Reglas de Negocio

1. **La prescripción del motor manda.** El valor precargado prioriza siempre la carga prescrita por el motor de Doble Umbral. La memoria del último peso es un mecanismo de respaldo, no un reemplazo del motor de progresión.
2. **Memoria progresiva.** En ausencia de prescripción, el sistema siempre ofrece el último peso efectivamente manejado, no el primero registrado.
3. **La serie anterior gana a la sesión anterior.** Dentro de una sesión, el dato más reciente es el más relevante.
4. **El historial es del ejercicio, no del slot.** Si se intercambia un ejercicio por su alternativa, la precarga se resuelve con el historial del ejercicio efectivamente ejecutado.
5. **La precarga nunca es obligatoria.** El Ejecutante puede modificarla siempre. El sistema sugiere, no impone.
6. **La descarga conserva su regla.** El protocolo de descarga calcula su propia carga y esa decisión prevalece.

### Interfaz

Los cambios se concentran en la pantalla de **Registro de Serie**. No se crea ninguna pantalla nueva ni se agregan controles.

- **Registro de serie:** cambia únicamente el valor con el que aparece precargado el campo de peso, y su presentación en la unidad activa del ejercicio.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Sin cambios. El comportamiento es invisible salvo por el valor precargado.
- **Campos y controles:** Ninguno nuevo. El campo de peso conserva su comportamiento de edición y validación.
- **Flujo de navegación visual:** Sin cambios.
- **Mensajes y feedback:** Sin mensajes nuevos. El campo vacío cuando no hay historial es un estado válido, no un error.

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14).

### Preview de Interfaz

**Preview:** [`31.preview.txt`](./31.preview.txt) | **Formato:** ASCII (wireframe de texto)

Aplica el mockup existente de Registro de Serie, ya actualizado por HU-30 con el selector de unidad.

---

## Contexto y Referencias

**Dependencias:** **depende de HU-30** (CA-31.05: coherencia entre el valor precargado y la unidad activa del ejercicio).

**Arquitectura:** `docs/architecture/domain_and_state_model.md` (entidades `exercise_set`, `exercise_progression`, `session_exercise`, `deload`)

**Historias relacionadas:**

- **HU-30** — Captura de carga en kg/lb. **Precondición.**
- **HU-11** — Prescripción de carga por Doble Umbral. Conserva su prioridad en la precedencia.
- **HU-06** — Registrar series en sesión activa. Pantalla base de esta historia.
- **HU-08** — Registrar ejercicios de peso corporal e isométricos. Casos excluidos.
- **HU-14** — Protocolo de descarga. Su cálculo de carga prevalece sobre la memoria.
- **HU-26** — Alternativas por slot. Determina de qué ejercicio se resuelve el historial.
- **HU-10** — Evaluación y clasificación de progresión. Consume los pesos registrados.

**Restricciones transversales aplicables:**

- RNF01 — Operaciones de cálculo y persistencia no bloquean la interfaz
- RNF29 — Motor de reglas testeable sin dependencias de Android — la resolución de precedencia es una regla pura
- RNF30 — Pruebas unitarias para reglas de negocio críticas
- **Beta sin migración:** la base de datos se reinicia; no se requiere migración de datos.

**Lecciones aprendidas:** El defecto original no está en que el motor de progresión prescriba una carga, sino en el valor al que el sistema cae cuando no hay prescripción activa. Sustituir la prescripción por la memoria habría desactivado la funcionalidad más valiosa del producto para resolver un problema de respaldo.

---

## Definición de Terminado (Inicial)

- [ ] Precedencia implementada: prescripción → serie anterior en sesión → última serie de sesión anterior → vacío
- [ ] Memoria intra-sesión funcionando serie a serie
- [ ] Memoria inter-sesión tomando la última serie, no la primera
- [ ] Prescripción de HU-11 con prioridad conservada y sin regresión
- [ ] Carga de descarga de HU-14 con prioridad conservada y sin regresión
- [ ] Precarga presentada en la unidad activa del ejercicio
- [ ] Historial resuelto por ejercicio efectivamente ejecutado, no por slot
- [ ] Campo vacío como estado válido cuando no hay historial ni prescripción
- [ ] Valor precargado siempre editable
- [ ] Regla de precedencia cubierta por pruebas unitarias
