# Historia de Usuario

**Como** ejecutante,
**Quiero** poder visualizar el detalle completo de mi próxima sesión prescrita desde la pantalla de inicio sin iniciarla formalmente, y que los ejercicios por tiempo ofrezcan un cronómetro integrado en la pantalla de registro de serie,
**Para** planificar mi entrenamiento evaluando qué me espera antes de comprometerme a ejecutar la sesión, y eliminar la necesidad de cronometrar externamente y transcribir manualmente los segundos, reduciendo la fricción de registro y mejorando la precisión del dato capturado.

---

## Descripción

Como ejecutante, necesito dos mejoras que reducen la fricción operativa y mejoran la calidad de mi interacción con el sistema: (1) poder visualizar el detalle completo de mi próxima sesión prescrita — módulo, versión, ejercicios y cargas objetivo — desde la pantalla de inicio sin verme obligado a iniciarla formalmente, para planificar mi entrenamiento y evaluar qué me espera antes de comprometerme a ejecutar la sesión; y (2) que los ejercicios cuyo rango de repeticiones está definido en segundos (isométricos y ejercicios por tiempo) ofrezcan un cronómetro integrado directamente en la pantalla de registro de serie, con un tiempo máximo gobernado por el plan y la posibilidad de detenerlo a partir del tiempo mínimo prescrito, capturando el dato automáticamente — para eliminar la necesidad de cronometrar externamente y luego transcribir manualmente los segundos, reduciendo la fricción de registro y mejorando la precisión del dato capturado.

---

## Narrativa de Negocio

### Contexto y justificación

#### Mejora A — Preview de sesión sin iniciar

##### Problema actual

Actualmente, la pantalla de inicio (Home/B1) muestra una card de "Próxima Sesión" que indica el módulo y la versión que corresponde ejecutar. Sin embargo, para ver el detalle de qué ejercicios componen esa sesión y con qué cargas objetivo, el ejecutante tiene dos caminos:

1. **Iniciar la sesión formalmente** — lo cual crea registros en base de datos (INSERT en `session` + N INSERTs en `session_exercise`), activa la sesión y compromete al ejecutante a ejecutarla o cerrarla como Incompleta. Esto es inadecuado si el ejecutante solo quiere consultar qué le toca, por ejemplo para decidir si tiene tiempo suficiente, verificar qué equipamiento necesitará, o simplemente prepararse mentalmente.
2. **Navegar al Diccionario/Plan de Entrenamiento** — ir a la sección de consulta del plan, buscar el módulo correspondiente y la versión específica. Este camino es funcional pero indirecto (múltiples navegaciones), no muestra cargas objetivo personalizadas (solo la prescripción genérica del plan), y rompe el flujo natural del ejecutante que ya está en Home.

##### Solución propuesta

La pantalla de inicio debe permitir visualizar la prescripción completa de la próxima sesión (ejercicios + cargas objetivo derivadas del historial) en modo **solo lectura**, sin crear ningún registro en base de datos ni activar la sesión. La sesión solo se inicia cuando el ejecutante lo decide explícitamente mediante una acción clara (botón "Iniciar Sesión" o equivalente).

Esto alinea Home como el **punto único de decisión operativa** del ejecutante: "veo qué me toca → decido si entreno → inicio cuando estoy listo". El principio rector 4.8 de la Visión del Producto ("La fricción de registro debe tender a cero") aplica también al momento previo al registro: consultar qué toca no debería requerir compromiso ni navegación compleja.

#### Mejora B — Cronómetro integrado para ejercicios por tiempo

##### Problema actual

Los ejercicios isométricos (Plancha, Plancha Lateral) y los ejercicios cuyo rango se define en segundos tienen un rango prescrito de 30-45 segundos. Actualmente, el flujo de registro de una serie isométrica requiere que el ejecutante:

1. Abra una aplicación de cronómetro externa en su teléfono (o use un reloj).
2. Inicie el cronómetro, ejecute la plancha, lo detenga.
3. Memorice o anote el tiempo.
4. Regrese a Tension, seleccione la serie del ejercicio, y transcriba manualmente los segundos en el campo "Segundos sostenidos".

Este flujo viola directamente el principio rector 4.8 ("La fricción de registro debe tender a cero" — RNF02). Son múltiples cambios de contexto entre aplicaciones, transcripción manual propensa a error, y una experiencia de usuario que compite con el entrenamiento en lugar de asistirlo.

##### Solución propuesta

Para ejercicios cuyo rango de repeticiones está definido en segundos, la pantalla de registro de serie (E2) debe reemplazar el campo numérico manual de "Segundos sostenidos" por un **cronómetro integrado** con las siguientes características:

- **Tiempo máximo** gobernado por el valor superior del rango prescrito en el plan (ej: 45 segundos para Plancha).
- **Tiempo mínimo** para detener, gobernado por el valor inferior del rango prescrito (ej: 30 segundos para Plancha).
- Captura automática del tiempo al detener, sin transcripción manual.
- Flujo de registro reducido a: iniciar cronómetro → ejecutar → detener → confirmar.

### Principio rector

Ambas mejoras están al servicio del principio 4.8 de la Visión del Producto: **"La fricción de registro debe tender a cero."** El preview reduce la fricción de consulta (no tener que iniciar para ver). El cronómetro reduce la fricción de captura (no tener que cronometrar externamente y transcribir). Cada segundo que el ejecutante ahorra en interactuar con la aplicación es un segundo que invierte en su entrenamiento o su descanso entre series.

---

## Criterios de Aceptación

### Bloque A — Preview de sesión desde Home (Mejora A)

#### CA-22.01 — Visualización de la próxima sesión sin iniciarla

**Dado que** el ejecutante se encuentra en la pantalla de inicio (Home) y no tiene una sesión activa en progreso,
**y** el sistema ha determinado el módulo y versión que corresponde según la rotación cíclica,
**cuando** el ejecutante interactúa con la card o componente de "Próxima Sesión" para ver el detalle,
**entonces** el sistema muestra la prescripción completa de la sesión en modo solo lectura:
- Identificador del módulo y versión asignados (ej: "Módulo A — V1").
- Lista completa de ejercicios prescritos para esa combinación módulo-versión.
- Carga objetivo para cada ejercicio derivada de su último registro histórico (idéntica a la que se mostraría al iniciar la sesión formalmente — CA-05.08 de HU-05).
- Ejercicios sin historial previo se muestran sin carga objetivo precargada, con la indicación de que el ejecutante establecerá su carga inicial (idéntico a CA-05.07 de HU-05).

**El sistema no crea ningún registro en base de datos (ni en `session`, ni en `session_exercise`, ni en ninguna otra tabla) durante esta visualización.** La consulta es puramente de lectura.

#### CA-22.02 — Inicio explícito de sesión desde el preview

**Dado que** el ejecutante está visualizando el preview de la próxima sesión en modo solo lectura,
**cuando** decide iniciar la sesión,
**entonces** el sistema ofrece una acción explícita (botón "Iniciar Sesión" o equivalente) que al activarse crea la sesión formalmente en base de datos y transiciona al flujo de sesión activa existente (pantalla E1). La sesión solo se crea cuando el ejecutante ejecuta esta acción, nunca por la mera visualización del preview.

#### CA-22.03 — Navegación de retorno desde el preview sin consecuencias

**Dado que** el ejecutante está visualizando el preview de la próxima sesión,
**cuando** decide volver a Home sin iniciar la sesión (botón atrás, navegación, o cierre del preview),
**entonces** el sistema regresa a Home sin haber creado ningún registro, sin haber modificado el estado de rotación, y sin ningún efecto secundario. La próxima vez que consulte el preview, verá exactamente la misma prescripción (mismo módulo, misma versión, mismas cargas objetivo).

#### CA-22.04 — Preview muestra el orden sugerido de ejecución

**Dado que** el plan de entrenamiento tiene un orden sugerido de ejecución por módulo (definido en HU-21),
**cuando** el ejecutante visualiza el preview de la sesión,
**entonces** los ejercicios se listan siguiendo el orden sugerido (Espalda → Bíceps → Abdomen para A; Pecho → Hombro → Tríceps para B; Cuádriceps compuestos → Isquiotibiales → Glúteos → Aductores/Abductores → Gemelos para C), consistente con la presentación que verá una vez que la sesión se inicie.

#### CA-22.05 — Preview del Módulo A muestra indicación de abdomen ejecutable fuera del gym

**Dado que** el Módulo A contiene ejercicios de abdomen clasificados como ejecutables fuera del gimnasio (definido en HU-21),
**cuando** el ejecutante visualiza el preview de una sesión del Módulo A,
**entonces** los ejercicios de abdomen se presentan con la misma indicación visual de "ejecutable fuera del gym" que se muestra en la sesión activa, para que el ejecutante pueda planificar dónde ejecutará cada porción del entrenamiento antes de iniciar la sesión.

#### CA-22.06 — Coexistencia con sesión activa en progreso

**Dado que** el ejecutante tiene una sesión activa en progreso (estado "En Progreso"),
**cuando** accede a la pantalla Home,
**entonces** el sistema prioriza mostrar la opción de reanudar la sesión activa (comportamiento existente de HU-05, crash recovery) y **no muestra el preview** de la próxima sesión. El preview solo está disponible cuando no hay sesión activa pendiente. Esto evita confusión entre la sesión actual y la siguiente.

#### CA-22.07 — Consistencia de cargas objetivo entre preview y sesión real

**Dado que** el ejecutante visualiza el preview de la próxima sesión y luego la inicia,
**cuando** la sesión se crea formalmente,
**entonces** las cargas objetivo mostradas en la sesión activa son **idénticas** a las que se mostraron en el preview, siempre que no haya ocurrido un cierre de otra sesión entre la visualización del preview y el inicio (lo cual podría actualizar el historial de algún ejercicio). En caso de que el historial haya cambiado entre el preview y el inicio, las cargas se recalculan a partir del historial actualizado.

---

### Bloque B — Cronómetro integrado para ejercicios por tiempo (Mejora B)

#### CA-22.08 — Cronómetro en lugar de input manual para ejercicios isométricos

**Dado que** el ejecutante está registrando una serie de un ejercicio cuyo rango de repeticiones está definido en segundos (actualmente: Plancha con rango 30-45 seg, Plancha Lateral con rango 30-45 seg),
**cuando** accede al formulario de registro de serie (pantalla E2),
**entonces** en lugar del campo de input numérico manual de "Segundos sostenidos" que existe actualmente, el sistema presenta un **cronómetro visual** que:
- Muestra un contador de tiempo en formato visible (segundos o mm:ss).
- Tiene un botón de "Iniciar" para comenzar la cuenta.
- Cuenta de forma ascendente desde 0 hasta el tiempo máximo definido por el rango del ejercicio.
- El campo de RIR permanece como input numérico manual, no se modifica.

#### CA-22.09 — Tiempo máximo gobernado por el plan

**Dado que** el ejercicio tiene un rango de repeticiones prescrito en segundos (ej: 30-45 seg),
**cuando** el cronómetro está en ejecución y alcanza el valor superior del rango (ej: 45 segundos),
**entonces** el cronómetro se **detiene automáticamente** al alcanzar el tiempo máximo y registra ese valor como el dato de la serie. El ejecutante no necesita intervenir; si alcanza el máximo, la serie se registra automáticamente con el tiempo máximo.

#### CA-22.10 — Detención habilitada a partir del tiempo mínimo

**Dado que** el ejercicio tiene un rango prescrito (ej: 30-45 seg) y el cronómetro está en ejecución,
**cuando** el cronómetro ha alcanzado o superado el valor inferior del rango (ej: 30 segundos),
**entonces** el botón de "Detener" se habilita (se vuelve interactivo/visible) y el ejecutante puede detener el cronómetro en cualquier momento entre el tiempo mínimo y el tiempo máximo. El tiempo capturado al detener se registra como el dato de la serie.

#### CA-22.11 — Detención anticipada (antes del tiempo mínimo)

**Dado que** el cronómetro está en ejecución pero aún no ha alcanzado el tiempo mínimo del rango prescrito (ej: menos de 30 segundos),
**cuando** el ejecutante necesita detener la serie antes del mínimo (por fatiga, dolor o cualquier razón),
**entonces** el sistema permite detener el cronómetro en cualquier momento (el botón de detener siempre está funcional), pero muestra una indicación visual clara de que el tiempo registrado está **por debajo del rango prescrito**. El dato se registra tal cual — no se bloquea — respetando el principio de que el sistema informa pero no bloquea al ejecutante (Visión del Producto §4: "el ejecutante siempre tiene la última palabra").

#### CA-22.12 — Captura automática del tiempo sin transcripción manual

**Dado que** el cronómetro se detiene (por acción del ejecutante o por alcanzar el tiempo máximo),
**cuando** el sistema registra la serie,
**entonces** el valor de segundos capturado por el cronómetro se asigna automáticamente al campo de duración de la serie, sin que el ejecutante tenga que escribir, transcribir ni ingresar manualmente ningún número. El flujo de registro se reduce a: Iniciar cronómetro → Ejecutar ejercicio → Detener cronómetro → Ingresar RIR → Confirmar.

#### CA-22.13 — Indicación visual de progreso dentro del rango prescrito

**Dado que** el cronómetro está en ejecución durante el registro de una serie isométrica,
**cuando** el tiempo transcurre,
**entonces** el cronómetro muestra una indicación visual del progreso relativo al rango prescrito:
- **Zona roja / pre-mínimo:** Desde 0 hasta el tiempo mínimo (ej: 0-29s) — indicación clara de que aún no se ha alcanzado el umbral mínimo.
- **Zona verde / dentro del rango:** Desde el tiempo mínimo hasta el tiempo máximo (ej: 30-45s) — indicación de que el ejecutante está dentro del rango prescrito.
- El mecanismo visual exacto (colores, barra de progreso, cambio de estado del botón, u otro) queda a criterio del Arquitecto en su diseño y del Desarrollador en su implementación, pero debe ser inequívoco y perceptible sin distraer al ejecutante durante la ejecución del ejercicio.

#### CA-22.14 — Peso fijo en 0 Kg durante el cronómetro (consistencia con ejercicios isométricos)

**Dado que** los ejercicios isométricos son un subconjunto de ejercicios de peso corporal (is_isometric = 1 implica is_bodyweight = 1),
**cuando** el formulario de registro de serie presenta el cronómetro para un ejercicio isométrico,
**entonces** el campo de peso sigue mostrando 0 Kg como valor fijo no editable (comportamiento existente de CA-08.01), y el cronómetro reemplaza exclusivamente el campo de "Segundos sostenidos" — los demás campos (peso, RIR) no se modifican.

#### CA-22.15 — Compatibilidad con ejercicios custom isométricos

**Dado que** el ejecutante puede crear ejercicios custom con el flag "isométrico" activado (RF62),
**cuando** un ejercicio custom isométrico tiene asignado un rango de repeticiones en segundos en alguna versión del plan,
**entonces** el cronómetro se comporta de manera idéntica que para los ejercicios precargados: el tiempo mínimo y máximo se derivan del rango prescrito del ejercicio en el plan, y la experiencia de registro es la misma.

#### CA-22.16 — Fallback para ejercicios isométricos sin rango definido

**Dado que** un ejercicio isométrico podría no tener un rango de repeticiones en segundos explícitamente definido en el plan (ej: ejercicio custom isométrico asignado con el rango genérico "8-12" por error, o ejercicio isométrico usado como sustituto sin asignación en plan),
**cuando** el ejecutante registra una serie de ese ejercicio,
**entonces** el sistema utiliza un rango por defecto razonable (30-60 segundos) como referencia para el cronómetro, o permite al ejecutante detener el cronómetro en cualquier momento sin restricción de mínimo, con una indicación de que no hay rango prescrito específico. El mecanismo exacto de fallback queda a criterio del Arquitecto en su diseño.

#### CA-22.17 — El cronómetro no bloquea la navegación ni pierde datos ante interrupciones

**Dado que** el cronómetro está en ejecución durante el registro de una serie isométrica,
**cuando** ocurre una interrupción (el ejecutante navega hacia atrás, la app pasa a segundo plano, o la app se cierra inesperadamente),
**entonces:**
- Si el ejecutante navega hacia atrás: el cronómetro se cancela y no se registra dato para esa serie. El ejercicio permanece en el estado anterior (misma cantidad de series registradas).
- Si la app pasa a segundo plano: el cronómetro sigue contando (el tiempo real transcurrido se preserva). Al regresar a primer plano, el cronómetro muestra el tiempo actual correcto.
- Si la app se cierra inesperadamente: no se registra dato para esa serie en progreso (consistente con RNF10 — se preservan datos ya confirmados, no datos en medio de captura).

---

### Bloque C — Consistencia y transversalidad

#### CA-22.18 — Ejercicios no isométricos no se ven afectados

**Dado que** las mejoras de esta historia son específicas para el preview de sesión y para ejercicios con rango en segundos,
**cuando** el ejecutante registra series de ejercicios estándar (rango en repeticiones: 8-12) o ejercicios de peso corporal no isométricos (Abdominales, Escalador, Giro Ruso, Flexiones, Sentadilla a cuerpo libre),
**entonces** el flujo de registro de serie permanece exactamente igual al actual (campo numérico manual de repeticiones + campo de RIR), sin ningún cambio. El cronómetro solo se activa para ejercicios cuyo rango está definido en segundos.

#### CA-22.19 — Datos registrados con cronómetro son compatibles con el motor de progresión existente

**Dado que** el cronómetro captura automáticamente un valor en segundos,
**cuando** ese valor se registra como dato de la serie,
**entonces** se almacena en el mismo campo y con el mismo formato que los datos registrados manualmente con el input actual (campo `reps` almacena segundos para isométricos — patrón existente de HU-06/HU-08). El motor de progresión (HU-10), la Regla de Doble Umbral (HU-11, con su exclusión para isométricos), la detección de dominio (CA-10.12 de HU-10) y los KPIs (HU-15) operan sin modificación sobre estos datos.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Reduce la fricción operativa del ejecutante en dos dimensiones: consulta de planificación (preview) y captura de datos de series isométricas (cronómetro). Ambas mejoran la precisión del dato y reducen el tiempo de interacción con la aplicación.

### Reglas de Negocio

1. **Preview puramente de lectura:** La visualización del preview de sesión NO crea ningún registro en base de datos. La sesión solo se crea cuando el ejecutante ejecuta explícitamente la acción "Iniciar Sesión".
2. **Preview solo sin sesión activa:** El preview no se muestra cuando hay una sesión activa en progreso (estado "En Progreso"). Se prioriza la reanudación de la sesión activa existente.
3. **Cargas objetivo consistentes:** Las cargas mostradas en el preview deben ser idénticas a las que se muestran en la sesión activa al iniciar, siempre que no haya ocurrido un cierre de otra sesión entre ambas consultas.
4. **Cronómetro por rango, no por flag:** El cronómetro se activa cuando el rango prescrito (`reps`) está en formato de segundos (`_SEC`), independientemente del flag `is_isometric`. Esto desacopla el cronómetro del tipo de ejercicio para permitir futuros ejercicios con rango en segundos.
5. **Fallback para ejercicios sin rango definido:** Si un ejercicio isométrico no tiene rango `_SEC` definido, se usa un rango por defecto de 30-60 segundos con indicación de rango por defecto.
6. **Captura automática sin transcripción:** El cronómetro asigna automáticamente el valor capturado al campo `reps` de la serie, sin intervención manual del ejecutante.
7. **Datos compatibles con motor de progresión:** Los segundos capturados por el cronómetro se almacenan en el mismo campo `exercise_set.reps` con el mismo formato entero que el registro manual existente.
8. **Interrupciones manejadas consistentemente:** Navegación hacia atrás cancela el cronómetro sin registrar dato. App en segundo plano preserva el tiempo real. Cierre inesperado no registra datos en progreso (RNF10).

### Interfaz

- **B1 — `HomeScreen` (modificación):** `NextSessionCard` se hace clickable → navega al preview. Botón "Iniciar Sesión" se mantiene como acción rápida alternativa.
- **Preview — `SessionPreviewScreen` (nuevo):** Pantalla Compose read-only con Bottom Navigation visible. Header con módulo y versión. Lista de ejercicios con cargas objetivo. Botón CTA "Iniciar Sesión". Pantalla separada (no bottom sheet) porque la lista de 8-12 ejercicios con cargas excede el espacio de un bottom sheet.
- **E2 — `RegisterSetScreen` (modificación):** Cambio condicional: cuando el ejercicio tiene rango en segundos, reemplazar `RepsField` por `IsometricChronometer`. El `RirSelector` y `WeightField` permanecen inalterados.
- **`IsometricChronometer` (nuevo):** Composable con display grande de segundos, `CircularProgressIndicator`, colores semánticos (error pre-mínimo → progressionPositive en rango), texto de estado dinámico ("⚠️ Bajo el mínimo" / "✅ En rango"), botones "Iniciar" / "Detener" / "Reiniciar".

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante. El cronómetro usa `SystemClock.elapsedRealtime()` del SDK de Android.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` | `docs/architecture/domain_and_state_model.md`

**Entidades afectadas:** `plan_assignment`, `exercise_progression`, `exercise_set`, `session`, `session_exercise`, `deload`, `module` (ver `docs/architecture/domain_and_state_model.md`)

**Interfaces de referencia:** `HomeScreen` (B1), `SessionPreviewScreen` (nuevo), `RegisterSetScreen` (E2), `IsometricChronometer` (componente nuevo) (ver `docs/architecture/interfaces_contract.md`)

**Requisitos cubiertos:** RF12 (preview sesión), RF32 (isométricos 30-45s), RF38 (parámetros de descarga), RNF02 (max 3 toques), RNF05 (señales por color + ícono + texto), RNF10 (preservación datos confirmados)

**Épica / Módulo:** `EPIC-02: Experiencia de Entrenamiento`

**Prioridad:** Media

**Historias relacionadas:**
- HU-04: `getDetailsByModuleVersionId()` en `PlanAssignmentDao`, `mapRepsToDisplay()` con mapeo de formatos especiales
- HU-05: Creación de sesión (`startSession()`), crash recovery, carga objetivo (`prescribedLoadKg`)
- HU-06: Registro de serie (RegisterSetScreen/E2), campo `reps` almacena segundos para isométricos
- HU-07: Sustitución de ejercicios — sustitutos no tienen `plan_assignment`, afecta fallback del cronómetro
- HU-08: Cross-cutting isométrico (peso=0, "Segundos sostenidos", referencia "30-45 seg")
- HU-09: Cierre de sesión → `evaluateProgression()` actualiza `prescribed_load_kg`, relevante para consistencia CA-22.07
- HU-10: Motor de progresión (opera sobre `exercise_set.reps` — compatible con cronómetro)
- HU-11: Regla Doble Umbral produce `prescribed_load_kg`; isométricos excluidos (NULL permanente)
- HU-14: Protocolo de descarga: versiones congeladas, cargas al 60% (`DeloadLoadRule`), indicador visual, 6 sesiones, reinicio al 90%. **Crítico para el preview.**
- HU-16: Migración con formato `'30-45_SEC'` en PlanSeeder
- HU-21: `sort_order` en `plan_assignment`, ORDER BY COALESCE, badge "Fuera del gym"

**Nota arquitectónica:** HU-22 introduce dos mejoras funcionales independientes que tocan múltiples capas (DAO, Repository, UseCase, ViewModel, Screen, Navigation). El Preview requiere un componente vertical completo nuevo (7 componentes: DTO, query, modelo, repo, UseCase, ViewModel, Screen). El Cronómetro requiere lógica de timer con `SystemClock.elapsedRealtime()`, manejo de estados (IDLE/RUNNING/STOPPED), señalización visual accesible con colores semánticos, y compatibilidad con background. Ambas mejoras interactúan con deload (HU-14), sustitución (HU-07), ejercicios custom (RF62) y reps especiales (`_SEC`, `TO_TECHNICAL_FAILURE`).

---

## Definición de Terminado (Inicial)

- [x] Preview de sesión desde Home sin iniciarla (modo solo lectura, sin registros DB)
- [x] Inicio explícito de sesión desde el preview (botón "Iniciar Sesión")
- [x] Navegación de retorno desde preview sin consecuencias
- [x] Preview muestra orden sugerido de ejecución (HU-21)
- [x] Preview del Módulo A indica ejercicios ejecutables fuera del gym
- [x] Preview no se muestra cuando hay sesión activa en progreso
- [x] Consistencia de cargas objetivo entre preview y sesión real
- [x] Cronómetro integrado para ejercicios con rango en segundos
- [x] Tiempo máximo gobernado por el plan (auto-stop)
- [x] Detención habilitada a partir del tiempo mínimo
- [x] Detención anticipada permitida con indicación visual
- [x] Captura automática sin transcripción manual
- [x] Indicación visual de progreso (zona roja / zona verde)
- [x] Peso fijo en 0 Kg durante el cronómetro
- [x] Compatibilidad con ejercicios custom isométricos
- [x] Fallback para ejercicios isométricos sin rango definido
- [x] Cronómetro no pierde datos ante interrupciones
- [x] Ejercicios no isométricos no se ven afectados
- [x] Datos del cronómetro compatibles con motor de progresión
