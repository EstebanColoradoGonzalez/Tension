# Historia de Usuario

**Como** ejecutante,
**Quiero** corregir los datos de la última serie que acabo de registrar mientras mi sesión de entrenamiento sigue en curso,
**Para** que un error de dedo al capturar a velocidad (por ejemplo, escribir 18 en vez de 10 repeticiones) no contamine mi historial, que es la fuente de verdad de mi progreso.

## Descripción

Registrar series es una acción rápida: peso, repeticiones, RIR y confirmar, a veces con una mano en la barra y el cansancio del set anterior encima. A esa velocidad los errores de dedo pasan, y hoy el sistema no da salida: la serie queda congelada con el valor equivocado y ese valor equivocado pasa a contar para el historial, la progresión, las alertas y el 1RM estimado. La alternativa actual es peor que no corregir: re-registrar no existe, borrar la serie no existe, y el dato falso se queda para siempre.

Esta historia introduce una **ventana de corrección**: tras confirmar el registro de una serie, esa serie puede corregirse —sus datos capturados con el teclado— mientras siga siendo **la última serie registrada de la sesión**. La ventana es **una sola y global**: no vive por ejercicio, vive en la sesión. Al registrar una serie nueva —de cualquier ejercicio, intercalado o no—, la ventana pasa a esa serie y la anterior queda inmutable, definitivamente.

La corrección no es un parche sobre el registro: el sistema **recalcula automáticamente todo lo que el valor errado ya contaminó** (1RM estimado, resumen de la sesión, y todo derivado que se reconstruye de las series), de modo que el historial queda **como si el error nunca hubiera existido**.

Lo que la historia **no** toca:

- **El implemento de la serie no es corregible.** Se registró con un equipamiento y con ese equipamiento cuenta. Cambiarlo no es corregir un despiste: movería la serie a la progresión de otro par (ejercicio + equipamiento), que es re-clasificar, no corregir.
- **La corrección solo existe mientras la sesión está en curso.** Al cerrar la sesión —completada o incompleta—, todo queda inmutable, exactamente como hoy. Esa inmutabilidad post-cierre es la que protege el historial a largo plazo: la corrección está pensada para el error que uno nota **en ese momento**, no para la revisión tardía.
- **El número de series no cambia.** Corregir no añade ni borra series: el estado de los ejercicios (No Iniciado / En Ejecución / Completado) y la completitud de la sesión no se alteran.

### Frontera de alcance

- **Una ventana global de sesión, no una ventana por ejercicio.** Solo la serie más recientemente registrada de toda la sesión es corregible. Si hay 3 series de elevaciones laterales, solo la tercera lo es; si luego registro la primera de press de banca, la ventana pasa a esa.
- **Corrección repetida dentro de la ventana, una sola vez cerrada.** Mientras la serie siga siendo la última, puede corregirse las veces que haga falta. Al registrar la siguiente —del mismo ejercicio o de otro—, la ventana se cierra sobre ella para siempre.
- **Campos corregibles:** peso, repeticiones (segundos sostenidos, en isométricos), RIR y unidad de captura (kg/lb). Implemento, número de serie, ejercicio, fecha y módulo quedan fijos.
- **Los derivados se recalculan.** El historial queda como si el error nunca existió; no hay "serie original + corrección" ni rastro del valor errado en el historial que el ejecutante consume.
- **Post-cierre: nada se corrige.** La sesión cerrada sigue siendo de solo lectura, como hasta ahora.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-47.01 — Aparece la acción de corregir sobre la serie recién registrada

- **Dado** que el ejecutante tiene una sesión activa
- **Cuando** confirma el registro de una serie de un ejercicio
- **Entonces** el ejercicio de la sesión muestra una acción **"Corregir"**
- **Y** esa acción está visible **solo** en ese ejercicio, porque es el que posee la última serie registrada
- **Y** los demás ejercicios de la sesión no muestran ninguna acción de corregir
- **Y** si antes de registrar otra serie el ejecutante vuelve a ese ejercicio, la acción sigue ahí

#### CA-47.02 — La ventana de corrección es global y se mueve al registrar

- **Dado** que la ventana de corrección está sobre la última serie de un ejercicio (por ejemplo, la serie 3 de elevaciones laterales)
- **Cuando** el ejecutante registra una serie de **otro** ejercicio (por ejemplo, la serie 1 de press de banca)
- **Entonces** la acción **"Corregir"** desaparece de elevaciones laterales y aparece sobre press de banca
- **Y** la última serie de elevaciones laterales queda **inmutable**: su acción de corregir no vuelve a aparecer mientras dure la sesión
- **Y** lo mismo ocurre si la siguiente serie es del **mismo** ejercicio: la ventana pasa a la serie nueva y la anterior se cierra
- **Y** el orden en que el ejecutante intercala ejercicios no importa: rige siempre la serie más recientemente registrada de la sesión

#### CA-47.03 — Corregir abre el formulario precargado con los datos registrados

- **Dado** que un ejercicio muestra la acción **"Corregir"**
- **Cuando** el ejecutante la toca
- **Entonces** el sistema abre el formulario de serie con los datos **precargados con los valores registrados** de esa serie: peso (en su unidad de captura), repeticiones o segundos sostenidos, RIR e implemento
- **Y** los campos corregibles son peso, repeticiones/segundos, RIR y unidad de captura
- **Y** el implemento se muestra **pero no es editable**: la serie conserva el equipamiento con el que se registró
- **Y** el título del formulario deja claro que se está **corrigiendo** la serie, no registrando una nueva
- **Y** el ejercicio, el número de serie y los metadatos (fecha, módulo, versión) no son parte del formulario

#### CA-47.04 — Confirmar la corrección actualiza la serie y recalcula los derivados

- **Dado** que el ejecutante cambió uno o varios valores del formulario de corrección y lo confirma con datos válidos
- **Cuando** el sistema persiste la corrección
- **Entonces** la serie registrada queda con los **nuevos valores**: peso, repeticiones/segundos, RIR y unidad, convertidos a la unidad canónica igual que en el registro original
- **Y** el sistema **recalcula automáticamente** los valores derivados que el valor errado ya había contaminado (1RM estimado, resumen de la sesión, y cualquier otro derivado que se construye a partir de las series)
- **Y** el historial queda **como si el error nunca hubiera existido**: no existe "valor original + corrección" visible, el dato es uno solo —el corregido—
- **Y** la ventana de corrección **sigue abierta sobre esa misma serie**: el ejecutante puede corregirla de nuevo (por ejemplo, si al corregir escribe 11 en vez de 10)

#### CA-47.05 — La corrección no altera la estructura de la sesión

- **Dado** que el ejecutante corrige una serie
- **Cuando** el sistema aplica la corrección
- **Entonces** la cantidad de series del ejercicio **no cambia**: corregir no añade ni borra series
- **Y** el estado del ejercicio (No Iniciado / En Ejecución / Completado) no cambia
- **Y** la completitud de la sesión y su cierre no se ven afectados por haber corregido
- **Y** la progresión del ejercicio y de sus pares se lee desde los valores corregidos

### Escenario 2: Validaciones

#### CA-47.06 — Las mismas validaciones del registro rigen en la corrección

- **Dado** que el ejecutante está corrigiendo una serie
- **Cuando** intenta confirmar con valores fuera de rango
- **Entonces** el sistema aplica **exactamente las mismas validaciones** que en el registro: peso ≥ 0 (0 solo para peso corporal e isométricos), repeticiones ≥ 1 (segundos sostenidos ≥ 1 en isométricos), RIR entre 0 y 2
- **Y** el campo de peso se bloquea en 0 para ejercicios de peso corporal e isométricos, salvo la captura de "Peso Añadido", con la misma regla que en el registro
- **Y** el botón de confirmar queda deshabilitado mientras cualquier campo sea inválido
- **Y** el teclado desplegado es numérico, como en el registro
- **Y** si el ejecutante cancela, la serie conserva sus valores y la ventana sigue abierta

#### CA-47.07 — Solo se corrige la última serie registrada

- **Dado** que la ventana de corrección está sobre la serie más recientemente registrada
- **Cuando** el ejecutante intenta corregir cualquier otra serie de la sesión —de cualquier ejercicio—
- **Entonces** el sistema **no lo permite**: no existe acción de corregir sobre esas series, ni en la sesión activa ni después
- **Y** la acción **"Corregir"** nunca aparece más de una vez en la sesión: siempre hay a lo sumo un ejercicio que la muestra
- **Y** una serie corregida previamente deja de ser corregible en el momento en que se registra una serie posterior, sin importar cuánto tiempo haya pasado dentro de la sesión

### Escenario 3: Casos Extremos

#### CA-47.08 — Cerrar la sesión cierra la ventana de corrección para siempre

- **Dado** que la ventana de corrección está abierta sobre la última serie registrada
- **Cuando** el ejecutante cierra la sesión —completada o incompleta—
- **Entonces** la acción **"Corregir"** desaparece
- **Y** **todas** las series de la sesión quedan inmutables, igual que hoy
- **Y** la detalla de la sesión pasada no expone ninguna acción de modificación
- **Y** si el ejecutante descubre un error ya cerrada la sesión, la historia no le da salida: esa es la frontera de esta historia, y se comunica como tal

#### CA-47.09 — Corrección en variantes de ejercicio

- **Dado** que la última serie registrada pertenece a un ejercicio de **peso corporal** o **isométrico**
- **Cuando** el ejecutante corrige esa serie
- **Entonces** el formulario se adapta igual que en el registro: peso fijo en 0 (salvo "Peso Añadido") y el campo de repeticiones se presenta como **segundos sostenidos** para isométricos
- **Y** las validaciones de la variante aplican igual (CA-47.06)
- **Y** la ventana global funciona igual: registrar una serie de cualquier otra clase de ejercicio la cierra

#### CA-47.10 — La ventana sobrevive al cierre inesperado de la aplicación

- **Dado** que el ejecutante registró una serie y la aplicación se cierra inesperadamente (cierre manual, falta de batería o crash)
- **Cuando** reabre la aplicación y retoma la sesión desde donde quedó
- **Entonces** la ventana de corrección **sigue disponible sobre la última serie registrada** antes del cierre
- **Y** la experiencia es la misma que si la aplicación no se hubiera cerrado: el ejercicio correspondiente muestra la acción **"Corregir"**
- **Y** al cerrar la sesión después de retomarla, aplica CA-47.08

#### CA-47.11 — El 1RM estimado se ajusta a lo que realmente se hizo

- **Dado** que la serie registrada contaminó el 1RM estimado (el valor equivocado era mayor que el real, y el récord se actualizó con él)
- **Cuando** el ejecutante corrige la serie al valor real
- **Entonces** el 1RM estimado queda ajustado a **lo que la serie corregida produciría** si el error nunca hubiera existido
- **Y** el récord puede **bajar**: el objetivo es el dato fiel, no el máximo registrado
- **Y** si el valor corregido también califica como nuevo máximo, el récord se actualiza con él
- **Y** la misma regla aplica en ambas direcciones para los demás derivados: recalculado, no acumulado

#### CA-47.12 — La corrección se ve en el resumen y en el historial

- **Dado** que el ejecutante corrigió una serie y la sesión se cierra
- **Cuando** consulta el resumen post-sesión y el historial de esa sesión
- **Entonces** los datos mostrados son los **corregidos**: peso, repeticiones/segundos, RIR y unidad, como si se hubieran registrado correctos a la primera
- **Y** los KPIs de la sesión y los derivados mostrados en el resumen usan esos mismos datos
- **Y** no existe vista que muestre el valor previo a la corrección

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — actor único del sistema. Aplicación single-user sin roles.
- **Permisos requeridos:** ninguno. Operación 100% local sobre la base de datos del dispositivo.
- **Valor de negocio:** el registro rápido a velocidad de gimnasio produce errores de dedo, y el historial es la fuente de verdad de todo el sistema (progresión, alertas, KPIs, 1RM). Un dato equivocado congelado degrada silenciosamente cada decisión futura. La corrección en el momento del error —que es cuando el ejecutante lo nota— mantiene el historial fiel sin abrir la puerta a la edición retrospectiva.

### Reglas de Negocio

1. **Una ventana global de sesión.** En todo momento, a lo sumo UNA serie de la sesión es corregible: la más recientemente registrada. No existe ventana por ejercicio.
2. **La ventana se cierra al registrar.** Registrar una serie nueva —de cualquier ejercicio— transfiere la ventana a esa serie y cierra la anterior definitivamente (dentro de la sesión).
3. **La ventana también se cierra al cerrar la sesión.** Post-cierre, toda la sesión es inmutable; no hay corrección posible, ni de la última serie ni de ninguna otra.
4. **Campos corregibles:** peso, repeticiones/segundos, RIR y unidad de captura (kg/lb). Implemento, número de serie, ejercicio, fecha y módulo **no** son corregibles.
5. **El implemento no se corrige.** La serie cuenta siempre para el par (ejercicio, equipamiento) con el que se registró.
6. **Corrección repetida dentro de la ventana.** Mientras la serie siga siendo la última registrada, puede corregirse cuantas veces haga falta; cada corrección deja la ventana abierta sobre esa misma serie.
7. **Recálculo automático de derivados.** Al confirmar, el sistema recalcula los valores derivados afectados (1RM estimado, resumen, KPIs y cualquier otro derivado construido desde las series), de modo que el historial queda como si el error nunca existió. El 1RM puede bajar: el objetivo es el dato fiel.
8. **Mismas validaciones que el registro.** Rango de peso, repeticiones/segundos, RIR 0–2, peso fijo en variantes de peso corporal/isométricos y teclado numérico: idénticas a las del registro original.
9. **Corregir no añade ni borra series.** El estado de los ejercicios y la completitud de la sesión no cambian.
10. **La ventana sobrevive al cierre inesperado.** Al reabrir la app y retomar la sesión, la última serie registrada sigue siendo la corregible.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| Sesión activa | `E1` | Nueva acción **"Corregir"** en el ejercicio que posee la última serie registrada; visible/móvil conforme se registran series |
| Registro de serie | `E2` | El formulario se reutiliza en modo corrección: precargado con los datos registrados, título distinto, implemento no editable |
| Resumen post-sesión | `E5` | Muestra los datos corregidos y los derivados recalculados |
| Detalle de sesión pasada | `F2-T1` | Muestra los datos corregidos; sin acción de corrección (post-cierre inmutable) |

#### Detalle de Interfaz de Usuario

- **Diseño general:** sin pantallas nuevas. La corrección se monta sobre la pantalla de la sesión activa y el formulario de serie ya existente, que pasa a tener un modo de corrección.
- **Campos y controles:**
  - **Acción "Corregir":** visible sobre la tarjeta del ejercicio de la sesión que posee la última serie registrada. Aparece al confirmar el registro de una serie, desaparece de ese ejercicio al registrar una serie en cualquier otro ejercicio, y vuelve a aparecer si la sesión se retoma tras un cierre inesperado. Nunca coexiste en dos ejercicios.
  - **Formulario de corrección:** el mismo formulario del registro (E2), precargado con los valores registrados de esa serie. El campo de implemento se muestra deshabilitado. El título deja claro que se está corrigiendo la serie (no registrando una nueva). Botón **"Confirmar"** (habilitado solo con datos válidos) y **"Cancelar"** (conserva los valores y deja la ventana abierta).
- **Flujo de navegación visual:** `Sesión activa → (registrar serie) → acción "Corregir" visible sobre el ejercicio → tocar → formulario precargado → Confirmar (se corrige, la ventana sigue abierta) | Cancelar (se vuelve a la sesión)`.
- **Mensajes y feedback:** al confirmar, confirmación breve de que la serie quedó corregida. Al intentar corregir tras registrar otra serie, no hay error que mostrar: la acción simplemente ya no existe sobre esa serie (la ventana es invisible por diseño, no un "no se puede" que hay que leer). Si el ejecutante busca corregir una serie ya cerrada de la sesión, la historia no da salida —la inmutabilidad post-cierre es la regla, y la acción no aparece en ninguna parte.

### Sistemas Externos

Ninguno. El sistema es local y sin integraciones.

### Disparadores

- El ejecutante confirma el registro de una serie en la sesión activa (abre la ventana sobre esa serie).
- El ejecutante pulsa "Corregir" sobre un ejercicio de la sesión activa (abre el formulario precargado; reprocesable: puede tocarse las veces que haga falta mientras la ventana esté sobre esa serie).
- El ejecutante confirma la corrección en el formulario (persiste los nuevos valores y recalcula los derivados; reprocesable mientras la serie siga siendo la última).
- El ejecutante cancela la corrección (la ventana queda intacta; reprocesable).
- El ejecutante cierra la sesión (cierra la ventana y congela toda la sesión).

### Entidades del Dominio

- Serie (la unidad de registro que se corrige).
- Ejercicio (el que la serie pertenece dentro de la sesión; porta la acción "Corregir").
- Sesión (posee la ventana global de corrección y su estado en curso/cerrada).
- Récord de 1RM estimado (derivado que se recalcula; puede bajar).
- Unidad de carga (preferencia de captura kg/lb, corregible con el peso).

### Alcance y Continuidad

- **Esta funcionalidad no existe hoy en ningún momento del sistema** —ni durante la sesión activa ni después de cerrarla— y esta historia la introduce por primera vez. El registro de serie (HU-06) es insertar-y-listo, y el ajuste de sesión del día (HU-43) solo añade y retira ejercicios, expresamente sin tocar series registradas.
- **Revierte de forma controlada una regla del sistema:** hoy la serie se declara inmutable tras su creación. Esta historia abre una excepción única y acotada —la última serie registrada, solo mientras la sesión está en curso— y mantiene el resto intacto: las demás series, todo lo que no es una serie y cualquier momento post-cierre conservan la inmutabilidad actual.
- **La corrección es un dato, no un rastro:** el historial que consume el ejecutante (resumen, detalle de sesión pasada, KPIs) muestra solo el valor corregido; el valor errado no deja rastro consumible.
- **100% local:** la corrección ocurre íntegramente en el dispositivo; no hay sistemas externos que involucrar.

### Preview de Interfaz

**Preview:** [`47.preview.txt`](./47.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — entidades `session`, `session_exercise`, `exercise_set`, `exercise_progression`, `session_exercise_progression`, `exercise_one_rm`; convención de inmutabilidad de `exercise_set` y de sesiones cerradas.
- `docs/architecture/interfaces_contract.md` — triggers `E1-T1`, `E2-T1`, `E5-T1`, `F2-T1`; regla declarada de inmutabilidad de series y de sesiones cerradas (F2 de solo lectura).
- `docs/architecture/architecture_blueprint.md` — decisión D-03 "Sesiones cerradas son inmutables" (se mantiene) y la regla de inmutabilidad de series registradas (se matiza con la ventana de esta historia).

**Historias relacionadas:**
- `HU-06` — registro de series en sesión activa. Esta historia se monta sobre su formulario (E2) y su pantalla de sesión (E1); el flujo de registro no cambia, solo gana una salida posterior: corregir.
- `HU-43` — ajustar la sesión del día. Precedente directo de "la sesión en curso admite ajustes temporales que no tocan el plan"; esta historia extiende esa filosofía a los datos de la serie, con la misma disciplina: temporal (mientras la sesión vive) y acotado (solo la última registrada).
- `HU-42` — 1RM estimado. El récord se escribe al registrar la serie; esta historia obliga a que también se recalcule al corregirla, en ambas direcciones.
- `HU-09` — cierre de sesión. El cierre sigue congelando todo: la ventana de corrección muere con la sesión en curso.
- `HU-30` — unidad de carga kg/lb. La unidad de captura es corregible junto con el peso, con la misma conversión a la unidad canónica.
- `HU-13`, `HU-15` — resumen post-sesión y KPIs. Consumen los datos corregidos y los derivados recalculados; no ganan ninguna funcionalidad propia.

**Lecciones aprendidas:**
- `HU-34` retiró la sustitución por grupo muscular porque mantenía dos identidades del mismo ejercicio en la sesión. La misma lección rige aquí: la corrección no crea un "registro de corrección" ni un valor original en paralelo — hay un solo dato, el actualizado, y los derivados se recalculan de él.
- `HU-43` estableció la frontera "nada con series registradas se retira" por la inmutabilidad. Esta historia no rompe esa frontera al retiro (seguir sin poder retirar ejercicios con series); la matiza solo donde el PO lo decidió: la última serie registrada, en sesión en curso.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente (idénticas a las del registro, CA-47.06)
- [ ] Mensajes implementados
- [ ] Verificado que la ventana es una sola y global: nunca dos ejercicios muestran "Corregir" a la vez (CA-47.01, CA-47.02)
- [ ] Verificado que al registrar una serie nueva la ventana anterior se cierra definitivamente, del mismo ejercicio o de otro (CA-47.02, CA-47.07)
- [ ] Verificado que el implemento, el número de serie y los metadatos no son corregibles (CA-47.03)
- [ ] Verificado el recálculo de derivados, incluido el 1RM en ambas direcciones (CA-47.04, CA-47.11)
- [ ] Verificado que cerrar la sesión cierra la ventana y congela todo (CA-47.08) y que el detalle de sesión pasada sigue siendo de solo lectura
- [ ] Verificado que la ventana sobrevive al cierre inesperado y al retomar la sesión (CA-47.10)
- [ ] Verificado que corregir no altera estados de ejercicio ni completitud de sesión (CA-47.05)
- [ ] Documentación de arquitectura actualizada: matiz de inmutabilidad de `exercise_set` (ventana de última serie en sesión en curso), contrato de interfaces (acción E1 + modo corrección E2) y decisión D-03 (post-cierre sin cambios)
- [ ] `story_mapping_index.md` actualizado con la historia y sus relaciones
