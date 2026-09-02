# Historia de Usuario

**Como** El Ejecutante,
**Quiero** elegir si registro el peso de cada ejercicio en kilogramos o en libras, según la unidad que marque la máquina o el implemento que estoy usando,
**Para** dejar de hacer conversiones mentales en mitad de la serie y registrar exactamente el número que tengo delante.

## Descripción

En el gimnasio la unidad no es uniforme: algunas máquinas y herramientas están rotuladas en libras y otras en kilogramos. Hoy el sistema solicita siempre el peso en kilogramos, lo que obliga a El Ejecutante a convertir de cabeza cada vez que usa un implemento en libras — justo en el momento de menor disponibilidad para hacer cuentas.

Esta historia introduce un **selector de unidad por ejercicio** en la pantalla de registro de serie. El sistema sigue manejando internamente **todo en kilogramos**: la unidad elegida es únicamente una preferencia de captura y presentación, y la conversión ocurre bajo el capó.

La unidad se recuerda por ejercicio, porque la máquina de un ejercicio no cambia de rótulo entre sesiones: si el press de banca del gimnasio está en libras, seguirá estándolo la próxima vez.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-30.01 — Selector de unidad por ejercicio

- **Dado** que El Ejecutante está registrando una serie de un ejercicio con carga externa
- **Cuando** visualiza el campo de peso
- **Entonces** dispone de un selector de unidad con dos opciones, `Kg` y `Lb`, junto al campo
- **Y** la unidad seleccionada aplica únicamente a ese ejercicio, sin afectar a los demás ejercicios de la sesión

#### CA-30.02 — Almacenamiento canónico en kilogramos

- **Dado** que El Ejecutante captura un peso con la unidad `Lb` seleccionada
- **Cuando** confirma el registro de la serie
- **Entonces** el sistema convierte el valor a kilogramos usando el factor 1 lb = 0.45359237 kg
- **Y** persiste el resultado en kilogramos con 2 decimales de precisión
- **Y** NO se re-redondea el resultado al múltiplo de 0.5 kg del incremento del sistema

#### CA-30.03 — Persistencia de la unidad por ejercicio

- **Dado** que El Ejecutante registró previamente una serie de un ejercicio en libras
- **Cuando** vuelve a registrar una serie de ese mismo ejercicio, en la misma sesión o en una posterior
- **Entonces** el selector aparece preseleccionado en `Lb`
- **Y** un ejercicio sin unidad registrada previamente aparece preseleccionado en `Kg`

### Escenario 2: Validaciones

#### CA-30.04 — Incremento según la unidad activa

- **Dado** que El Ejecutante usa los controles de incremento o decremento del campo de peso
- **Cuando** la unidad activa es `Lb`
- **Entonces** cada pulsación ajusta el valor en 1 lb
- **Y** cuando la unidad activa es `Kg`, cada pulsación ajusta el valor en 0.5 kg

#### CA-30.05 — Entrada inválida en el campo de peso

- **Dado** que El Ejecutante escribe un valor no numérico, negativo, o superior a 500 kg equivalentes
- **Cuando** intenta confirmar el registro de la serie
- **Entonces** el sistema muestra un mensaje de validación específico junto al campo
- **Y** no persiste la serie
- **Y** la validación evalúa el valor **ya convertido a kilogramos**, no el valor capturado en libras

#### CA-30.06 — Ejercicios sin carga externa

- **Dado** que el ejercicio está marcado como de peso corporal o isométrico
- **Cuando** El Ejecutante abre la pantalla de registro de serie
- **Entonces** el selector de unidad no se muestra
- **Y** el comportamiento actual se conserva sin cambios: campo de peso en cero y no editable

### Escenario 3: Casos Extremos

#### CA-30.07 — Unidad canónica en valores agregados

- **Dado** que existen series registradas en libras y en kilogramos dentro de la misma sesión
- **Cuando** El Ejecutante consulta el tonelaje de sesión, el historial, las métricas o el contenido de una alerta
- **Entonces** todos esos valores se expresan **siempre en kilogramos**
- **Y** la unidad de captura original se muestra únicamente en el detalle de la serie individual

#### CA-30.08 — Cambio de unidad a mitad de ejercicio

- **Dado** que El Ejecutante registró la serie 1 de un ejercicio en libras
- **Cuando** cambia el selector a `Kg` antes de registrar la serie 2
- **Entonces** el sistema acepta el cambio sin error
- **Y** ambas series conviven en el mismo ejercicio, cada una con su unidad de captura registrada
- **Y** el tonelaje del ejercicio se calcula correctamente sumando los valores en kilogramos

#### CA-30.09 — Precisión de la conversión

- **Dado** que El Ejecutante registra 45 lb
- **Cuando** el sistema convierte y persiste el valor
- **Entonces** almacena 20.41 kg
- **Y** al consultar esa serie en el historial, el valor mostrado en kilogramos es 20.4 kg, sin redondeo al múltiplo de 0.5

#### CA-30.10 — Documentación actualizada

- **Dado** que se incorpora la unidad de captura al registro de la serie
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/domain_and_state_model.md` refleja el nuevo atributo de unidad de captura en la entidad de serie y la convención de kilogramo como unidad canónica
- **Y** `docs/architecture/interfaces_contract.md` refleja el selector de unidad en la pantalla de registro de serie

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** Elimina el paso de conversión mental entre el implemento y la aplicación. Reduce el tiempo de registro entre series y elimina una fuente de error de captura que contamina todo el historial de progresión aguas abajo.

### Reglas de Negocio

1. **Kilogramo como unidad canónica.** La unidad de captura es una preferencia de presentación; toda persistencia, cálculo, agregación y comparación ocurre en kilogramos.
2. **Factor de conversión fijo.** 1 lb = 0.45359237 kg. Se aplica en el momento del registro, con 2 decimales de precisión.
3. **Sin ajuste al incremento.** El valor convertido no se redondea al múltiplo de 0.5 kg. El incremento del sistema rige los controles de ajuste, no la precisión del dato.
4. **La unidad es del ejercicio, no de la sesión ni del perfil.** Refleja la realidad física: cada máquina tiene su rótulo y no cambia entre sesiones.
5. **Incremento coherente con la unidad activa.** 1 lb en modo libras, 0.5 kg en modo kilogramos.
6. **Validación sobre el valor canónico.** Los límites de rango se evalúan siempre en kilogramos, después de convertir.

### Interfaz

Los cambios se concentran en la pantalla de **Registro de Serie**. No se crea ninguna pantalla nueva.

- **Registro de serie:** selector de unidad `Kg`/`Lb` adyacente al campo de peso; incremento variable según la unidad activa; mensaje de validación específico.
- **Detalle de serie en historial:** se muestra la unidad de captura original junto al valor.
- **Resto de la aplicación:** sin cambios visibles — todos los agregados siguen expresándose en kilogramos.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Se conserva el diseño actual de la pantalla de registro de serie. El selector se integra junto al campo de peso sin desplazar el resto de controles.
- **Campos y controles:** Selector segmentado de dos opciones (`Kg` / `Lb`), con área táctil mínima de 48×48 dp conforme a RNF06. El campo numérico de peso conserva su comportamiento actual salvo el incremento variable.
- **Flujo de navegación visual:** Sin rutas nuevas.
- **Mensajes y feedback:** Mensaje de validación específico bajo el campo de peso, evaluado sobre el valor convertido a kilogramos. El selector refleja visualmente la unidad activa en todo momento.

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14).

### Preview de Interfaz

**Preview:** [`30.preview.txt`](./30.preview.txt) | **Formato:** ASCII (wireframe de texto)

Aplica el mockup existente de Registro de Serie, con el selector de unidad incorporado junto al campo de peso.

---

## Contexto y Referencias

**Dependencias:** **habilita a HU-31**, que requiere la coherencia entre el valor precargado y la unidad activa.

**Arquitectura:** `docs/architecture/domain_and_state_model.md` (entidad `exercise_set`, sección 1 Convenciones Base — Manejo de Valores de Alta Precisión), `docs/architecture/interfaces_contract.md`

**Historias relacionadas:**

- **HU-06** — Registrar series de ejercicios en sesión activa. Pantalla base de esta historia.
- **HU-08** — Registrar ejercicios de peso corporal e isométricos. Casos excluidos del selector.
- **HU-11** — Prescripción de carga por Doble Umbral. Su carga prescrita se expresa en kilogramos y debe presentarse en la unidad activa del ejercicio.
- **HU-15** — Analítica y KPIs. Consume tonelaje, siempre en kilogramos.
- **HU-31** — Memoria del último peso manejado. Depende de esta historia.

**Restricciones transversales aplicables:**

- RNF06 — Elementos interactivos mínimo 48×48 dp
- RNF08 — Interfaz en español
- RNF11 — Transacciones atómicas
- RNF29 — Motor de reglas testeable sin dependencias de Android — la conversión de unidades es una regla pura
- RNF30 — Pruebas unitarias para reglas de negocio críticas
- **Beta sin migración:** la base de datos se reinicia; los cambios de esquema se validan sobre instalación fresca. Excepción documentada a RNF19, limitada a esta historia.

**Lecciones aprendidas:** El sistema ya establece el kilogramo como unidad canónica en sus convenciones base. Introducir una unidad alternativa debe hacerse en la frontera de captura y presentación, nunca propagándola al modelo de datos ni a los cálculos, o se contamina toda la analítica y el motor de progresión.

---

## Definición de Terminado (Inicial)

- [ ] Selector `Kg`/`Lb` operativo en la pantalla de registro de serie
- [ ] Unidad persistida por ejercicio y preseleccionada en registros posteriores
- [ ] Conversión a kilogramos con factor 0.45359237 y 2 decimales, sin redondeo al múltiplo de 0.5
- [ ] Incremento de 1 lb en modo libras y 0.5 kg en modo kilogramos
- [ ] Validación de rango (negativo, no numérico, máximo 500 kg) evaluada sobre el valor convertido
- [ ] Selector oculto para ejercicios de peso corporal e isométricos
- [ ] Tonelaje, historial, métricas y alertas expresados siempre en kilogramos
- [ ] Unidad de captura visible en el detalle de la serie individual
- [ ] Regla de conversión cubierta por pruebas unitarias
- [ ] `domain_and_state_model.md` e `interfaces_contract.md` actualizados
