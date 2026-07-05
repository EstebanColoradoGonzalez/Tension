# Documento de Definición del Sistema

> Este documento es la fuente de verdad absoluta del negocio. Debe redactarse utilizando lenguaje natural y conceptos de la Teoría General de Sistemas. **No incluir jerga técnica, nombres de bases de datos, frameworks o lenguajes de programación.**
>

## 1. Visión y Propósito (El Por qué)

### 1.1. Contexto y Nivel de Entropía

*El estado de desorden que el sistema viene a organizar.*

- **Problema actual:** Un ejecutante de entrenamiento de fuerza puede registrar datos durante años y aún así estancarse. El dato crudo sin interpretación no genera adaptación. Sin un mecanismo formal de análisis, el proceso opera a ciegas: el ejecutante decide por sensación o imitación cuándo subir carga, no detecta mesetas activas, no distingue fatiga acumulada de debilidad real, carece de tendencias, tonelajes por zona muscular o alertas, y comienza cada sesión con la pregunta "¿Qué hago hoy?" en lugar de "¿Qué me toca y con cuánto?". El resultado es un ciclo abierto: esfuerzo → dato → olvido → más esfuerzo sin dirección.
- **Impacto del problema:** Estancamiento invisible (el ejecutante no sabe que está estancado), riesgo de lesión por incrementos prematuros de carga basados en sensación, pérdida de adaptaciones musculares por ausencia de estímulo suficiente, y erosión de la motivación ante la falta de evidencia de progreso.

### 1.2. Propósito Central (La Misión Sistémica)

*La declaración inquebrantable de lo que el sistema debe lograr, independientemente de su medio de ejecución.*

- **Misión:** Transformar el registro disciplinado de datos de entrenamiento de fuerza — carga, repeticiones, intensidad percibida y adherencia — en un modelo de decisiones que garantice la sobrecarga progresiva continua del ejecutante, eliminando la subjetividad, el estancamiento invisible y la improvisación del proceso de hipertrofia; cerrando la brecha entre el dato capturado y la decisión informada.

### 1.3. Estados de Cierre e Indicadores de Eficacia

*Condiciones medibles que indican que el sistema cumple su propósito central.*

- **Cero improvisación:** Cada sesión tiene rutina, versión, ejercicios y cargas objetivo prescritas antes de comenzar, derivadas exclusivamente del historial del ejecutante.
- **Cero estancamiento invisible:** Toda meseta es detectada a más tardar en la tercera sesión consecutiva sin progresión y posee un diagnóstico causal asociado.
- **Trazabilidad completa:** Cualquier ejercicio puede mostrar su historial completo con clasificación de progresión por sesión en cualquier momento.
- **Decisiones basadas en evidencia propia:** Toda prescripción de carga se deriva de la Regla de Doble Umbral (condición objetiva de repeticiones y reserva), no de sensaciones.
- **Registro sin fricción:** Una serie se registra en máximo tres interacciones, con el peso de la última sesión precargado por el sistema.
- **Resiliencia total:** Los datos permanecen íntegros ante cierres inesperados, ausencias prolongadas del ejecutante y cambios de dispositivo.

---

## 2. Entorno y Fronteras (El Dónde)

### 2.1. Fronteras e Interfaces

*Define la "piel" del sistema: qué está bajo su jurisdicción y qué pertenece al exterior.*

- **Dentro del sistema (In-Scope):**
  - Herramientas para que el ejecutante diseñe su plan de entrenamiento (rutinas, versiones y asignación de ejercicios).
  - Prescripción de la rutina, versión, ejercicios y cargas objetivo para cada sesión.
  - Registro de datos por serie: fecha, rutina, versión, ejercicio, número de serie, peso, repeticiones y reserva de esfuerzo percibida.
  - Cálculos de progresión: tonelaje, tendencias de carga, detección de mesetas y clasificación de resultados.
  - Reglas de decisión: cuándo subir carga, cuándo mantener, cuándo ejecutar un protocolo de descarga y cuándo rotar versión de rutina.
  - Rotación cíclica de rutinas y secuenciación de versiones, persistente e inmune al calendario.
  - Sustitución puntual de ejercicios dentro de una sesión por indisponibilidad de equipo, sin alterar el plan original.
  - Emisión de alertas: estancamiento, fatiga acumulada, baja adherencia, inactividad por zona muscular.
  - Exportación e importación de datos de respaldo con formato versionado.

- **Fuera del sistema (Out-of-Scope):**
  - Genética, edad, nivel hormonal y respuesta individual a la adaptación muscular.
  - Calidad del sueño, nutrición, hidratación y estrés psicológico.
  - Disponibilidad real de equipamiento en el gimnasio.
  - Motivación, estado anímico y adherencia del ejecutante.
  - Lesiones imprevistas y condiciones médicas.
  - Días del calendario o frecuencia de asistencia.
  - Generación automática o inteligente de planes de entrenamiento (el sistema ejecuta y optimiza un plan definido por el propio ejecutante, no lo inventa).
  - Seguimiento nutricional, de recuperación o de composición corporal.
  - Funcionalidades sociales o de compartición de datos.
  - Sincronización con servicios externos o almacenamiento remoto.

### 2.2. Agentes y Roles

*Ejecutores autónomos que interactúan con el sistema, sus perfiles y niveles de autoridad.*

- **El Ejecutante:** La persona que realiza el entrenamiento físico. Es simultáneamente el operador y el beneficiario principal del sistema. Diseña su propio plan, registra sus datos con honestidad durante cada sesión, consulta las prescripciones del sistema antes de entrenar y decide el orden de ejecución de los ejercicios según las circunstancias reales del gimnasio. Su autoridad sobre el plan es total: puede crearlo, modificarlo y estructurarlo libremente en cualquier momento.
- **El Sistema:** El conjunto de reglas, cálculos y estructuras que procesan los datos del ejecutante y producen decisiones. No es una persona; cumple un rol activo automatizado: determina la rutina y versión correspondiente, calcula métricas derivadas, aplica las reglas de negocio para emitir señales de progresión y preserva la integridad completa del historial.
- **Asesor Externo (opcional):** Un entrenador, fisioterapeuta u otro profesional que puede consultar el historial del sistema para emitir recomendaciones complementarias (ajuste del plan por lesión, revisión de técnica). Este rol no modifica directamente las reglas ni los datos; solo emite sugerencias que el ejecutante decide integrar o no.

---

## 3. Ontología: Entidades Centrales (El Qué)

### 3.1. Entidades y Activos

*Elementos principales que el sistema manipula, consume o transforma.*

- **Ejecutante:** La persona que entrena. Posee atributos de perfil (peso corporal, altura, nivel de experiencia) que contextualizan sus métricas y pueden cambiar con el tiempo.
- **Rutina:** La unidad organizativa del plan de entrenamiento, creada y nombrada libremente por el ejecutante. Agrupa los ejercicios que trabajará en una misma sesión. Su estructura, nombre y composición son completamente libres.
- **Versión:** Una variante opcional de una rutina que usa diferentes ejercicios para atacar los mismos objetivos musculares, permitiendo variedad de estímulo y prevención de lesiones por sobreuso. Una rutina puede tener una o múltiples versiones.
- **Ejercicio:** La unidad de movimiento específica (ej.: Press de Banca). Agnóstico de rutina: no pertenece inherentemente a ninguna rutina. El ejecutante lo asigna libremente a las rutinas de su plan.
- **Serie:** El conjunto ininterrumpido de repeticiones de un ejercicio. Es la unidad mínima de registro. Produce un log con los datos: peso, repeticiones y reserva de esfuerzo.
- **Sesión:** La ejecución de una rutina en una versión específica en una fecha determinada. Contiene todas las series de los ejercicios realizados, incluyendo posibles sustituciones puntuales. El orden de ejecución de los ejercicios dentro de la sesión es libre.
- **Log (Registro):** La captura atómica de datos de una serie específica: fecha, rutina, versión, ejercicio, número de serie, peso, repeticiones y reserva de esfuerzo. Es la unidad fundamental de información del sistema; una vez registrada, es inmutable.
- **Microciclo:** La secuencia completa de ejecución de todas las rutinas del ejecutante (una pasada completa). Su duración en días naturales depende de la frecuencia real de asistencia.
- **Plan de Entrenamiento:** La estructura diseñada por el ejecutante que define sus rutinas, versiones y los ejercicios que componen cada combinación rutina-versión, con la cantidad de series y rangos de repeticiones objetivo. No prescribe orden de ejecución.
- **Diccionario de Ejercicios:** El catálogo de todos los movimientos disponibles en el sistema, clasificados por tipo de equipo y zona muscular. Incluye ejercicios precargados y ejercicios creados por el ejecutante.

### 3.2. Stocks (Acumulaciones)

*Inventarios que se acumulan o agotan en un momento dado.*

- **Historial de Logs:** La acumulación cronológica de todos los registros de series del ejecutante. Es el activo principal del sistema; toda capacidad analítica depende de su volumen y continuidad. Se acumula indefinidamente y no se puede reducir (los registros son inmutables).
- **Tonelaje por Zona Muscular:** La acumulación del volumen total de carga movida (peso × repeticiones × series) por cada zona muscular a través del tiempo. Indicador principal de la carga de estímulo recibida.
- **Posición en la Rotación Cíclica:** El estado persistente que indica en qué punto de la secuencia de rutinas y versiones se encuentra el ejecutante. Se acumula sesión a sesión y nunca se reinicia por ausencias.
- **Contador de Sesiones sin Progresión por Ejercicio:** Acumulación del número de sesiones consecutivas sin progresión positiva en un ejercicio dado. Cuando alcanza el umbral de tres, activa el estado de meseta.

---

## 4. Dinámica y Retroalimentación (El Cómo Sistémico)

### 4.1. Flujos y Bucles de Retroalimentación

*Cómo circulan la información y las acciones para generar crecimiento o mantener el equilibrio.*

- **Bucle de Refuerzo (Progresión):** El ejecutante entrena y registra datos → el sistema analiza los registros y calcula la progresión → el sistema prescribe la carga objetivo para la próxima sesión (incrementada si se cumplió el Doble Umbral) → el ejecutante entrena con mayor carga → genera nuevos registros con mayor tonelaje → el sistema confirma la progresión y vuelve a prescribir. Cada ciclo refuerza el anterior, produciendo acumulación de estímulo creciente.
- **Bucle de Balance (Descarga y Supercompensación):** Cuando el sistema detecta fatiga acumulada (regresión simultánea en ≥ 50% de los ejercicios de una rutina o estancamiento generalizado), prescribe un microciclo de descarga con carga reducida al 60% → el ejecutante se recupera → el sistema reinicia al 90% de la última carga pre-descarga, capturando la supercompensación → el ejecutante supera su marca anterior en 1-2 sesiones. Este bucle previene el sobreentrenamiento y mantiene la progresión sostenible a largo plazo.
- **Bucle de Corrección de Meseta:** Tres sesiones consecutivas sin progresión en un ejercicio activan el estado de meseta → el sistema diagnostica la causa probable (límite de carga, carga conservadora, fatiga sistémica) → prescribe acciones correctivas escalonadas (microincremento, extensión de repeticiones, rotación de versión, descarga) → el estímulo cambia → el ejercicio retoma progresión positiva.

### 4.2. Absorción de Anomalías (Resiliencia)

*Mecanismos de contingencia ante perturbaciones externas o internas.*

- **Perturbación esperada:** Cierre inesperado del sistema durante una sesión activa. → **Mecanismo:** Cada serie se persiste de forma atómica en el momento de su registro. No existe estado de "sesión guardada al final": el dato existe en el historial desde el instante en que el ejecutante lo confirma. Un cierre inesperado no puede borrar series ya registradas.
- **Perturbación esperada:** Ausencia prolongada del ejecutante (días, semanas o meses sin entrenar). → **Mecanismo:** La posición en la rotación cíclica se almacena indefinidamente. Al regresar, el ejecutante retoma exactamente la rutina y versión que le correspondía. El sistema no castiga las interrupciones ni reinicia la secuencia por el paso del tiempo.
- **Perturbación esperada:** Equipo del gimnasio no disponible durante una sesión. → **Mecanismo:** El sistema permite la sustitución puntual de cualquier ejercicio prescrito por otro de la misma zona muscular disponible en el Diccionario, sin alterar el plan original. La sesión continúa y los datos de la sustitución se registran vinculados al ejercicio sustituto.
- **Perturbación esperada:** Pérdida o cambio de dispositivo. → **Mecanismo:** El ejecutante puede exportar la totalidad de sus datos en un formato de respaldo versionado e importarlos en cualquier dispositivo compatible, restaurando el estado completo del sistema.

---

## 5. Leyes y Restricciones (Los Límites)

### 5.1. Reglas Absolutas (Invariantes)

*Leyes fundamentales e inquebrantables del dominio; si se rompen, los datos pierden validez y las decisiones del sistema se corrompen.*

- **Regla 1 — Inmutabilidad del registro:** Un log de serie es un hecho consumado. Una vez registrado, no puede ser alterado retroactivamente. Las sesiones cerradas son inmutables. Las comparaciones históricas y las prescripciones futuras dependen de que los datos reflejen la realidad sin manipulación posterior.
- **Regla 2 — Pertenencia obligatoria:** Todo registro debe pertenecer a una sesión válida con fecha, rutina, versión y ejercicio asociados. No puede existir un log huérfano.
- **Regla 3 — Existencia en el Diccionario:** Todo ejercicio registrado en una sesión debe existir en el Diccionario de Ejercicios. No se puede registrar un movimiento que el sistema no reconoce.
- **Regla 4 — Valores válidos del esfuerzo percibido:** La reserva de esfuerzo percibida (RIR) debe estar estrictamente en el rango cerrado [0, 2]. Valores fuera de este rango invalidan el registro.
- **Regla 5 — Peso no negativo:** El peso registrado debe ser ≥ 0 kilogramos. El valor cero es válido exclusivamente para ejercicios de peso corporal.
- **Regla 6 — Repeticiones mínimas:** Las repeticiones deben ser ≥ 1 (o ≥ 1 segundo para ejercicios isométricos). Un registro con cero repeticiones no es válido; si el ejecutante no completó ninguna, la serie no se registra.
- **Regla 7 — Persistencia de la rotación:** La secuencia de rotación cíclica de rutinas y versiones es persistente e inmune al calendario. No se reinicia por ausencias. La rutina de la sesión N+1 debe ser siempre la sucesora de la sesión N en la secuencia definida por el ejecutante.
- **Regla 8 — Integridad de las sustituciones:** Las sustituciones puntuales de ejercicios no alteran el Plan de Entrenamiento original. Solo afectan la sesión donde ocurren. El ejercicio sustituto debe pertenecer a la misma zona muscular del ejercicio sustituido.
- **Regla 9 — Unicidad del registro:** Cada serie se registra exactamente una vez. No se permiten duplicados (mismo ejercicio, misma serie, misma sesión) ni registros retroactivos que alteren sesiones pasadas.

### 5.2. Límites de Capacidad y Restricciones

*Restricciones finitas bajo las que el sistema está forzado a operar.*

- **Restricción 1 — Rango de esfuerzo percibido:** El sistema opera con una escala de reserva de esfuerzo de tres valores enteros: 0 (al fallo técnico), 1 (una repetición en reserva) y 2 (dos o más repeticiones en reserva). La escala no es continua ni admite valores decimales.
- **Restricción 2 — Umbral de progresión de carga:** El incremento mínimo de carga al cumplirse el Doble Umbral está fijado en +2.5 Kg para ejercicios de tren superior y +5 Kg para ejercicios de tren inferior. El sistema no permite incrementos menores a estos valores dentro de la lógica automática de progresión.
- **Restricción 3 — Umbral de detección de meseta:** El sistema no puede declarar meseta antes de la tercera sesión consecutiva sin progresión del mismo ejercicio. Dos sesiones sin progresión son insuficientes para clasificar el estado.
- **Restricción 4 — Parámetros de descarga:** Durante un microciclo de descarga, la carga de trabajo se reduce obligatoriamente al 60% de la carga habitual y la carga de reinicio post-descarga se establece al 90% de la última carga pre-descarga. Estos porcentajes son fijos; no son configurables por el ejecutante.
- **Restricción 5 — Funcionamiento sin conectividad:** El sistema opera en su totalidad de forma local en el dispositivo del ejecutante. No existe ninguna funcionalidad que requiera conexión a redes externas o servicios remotos.
- **Restricción 6 — Orientación y plataforma:** El sistema está diseñado exclusivamente para ser operado en posición vertical (retrato) sobre dispositivos móviles. No soporta orientación horizontal ni pantallas de escritorio o tableta.
- **Restricción 7 — Idioma único:** El sistema opera exclusivamente en español. No existe soporte para otros idiomas.
