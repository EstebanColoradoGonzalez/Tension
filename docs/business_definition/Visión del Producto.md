# Visión del Producto — Tension

---

## 1. Declaración de Visión

**Para** el ejecutante de entrenamiento de fuerza orientado a hipertrofia que registra sus datos con disciplina pero carece de un mecanismo formal para convertirlos en decisiones,

**que necesita** eliminar la subjetividad, el estancamiento invisible y la improvisación de su proceso de entrenamiento,

**Tension** es una aplicación Android personal y 100% offline

**que** transforma el registro de carga, repeticiones e intensidad percibida en un ciclo cerrado de retroalimentación que garantiza la sobrecarga progresiva continua.

**A diferencia de** las libretas de entrenamiento, hojas de cálculo o aplicaciones genéricas de fitness que almacenan datos sin interpretarlos,

**Tension** cierra la brecha entre el dato capturado y la decisión informada: prescribe qué hacer, detecta cuándo intervenir y demuestra por qué, sustentado exclusivamente en la evidencia propia del ejecutante.

---

## 2. El Problema

Un ejecutante puede registrar números durante años y aún así estancarse. El dato crudo sin interpretación no genera adaptación. Entrenar sin un mecanismo formal de análisis es operar a ciegas:

- **No sabe cuándo subir de peso.** Decide por sensación o por imitación, no por evidencia. Esto produce incrementos prematuros (lesión) o tardíos (estancamiento).
- **No detecta mesetas.** Puede repetir la misma carga durante semanas sin que nada le señale que el estímulo dejó de ser suficiente.
- **No distingue fatiga de debilidad.** Cuando el rendimiento cae, no sabe si necesita descargar o simplemente esforzarse más.
- **No mide lo que importa.** Tiene datos aislados pero no tendencias, ni tonelaje por grupo muscular, ni tasa de progresión, ni alertas.
- **Improvisa o se paraliza.** Sin una prescripción clara, cada sesión empieza con la pregunta "¿Qué hago hoy?" en lugar de "¿Qué me toca y con cuánto?"

El resultado es un ciclo abierto: esfuerzo → dato → olvido → más esfuerzo sin dirección. Tension existe para cerrar ese ciclo.

---

## 3. La Solución

Tension es un **sistema de decisiones de entrenamiento de fuerza** que opera como un ciclo cerrado de retroalimentación entre el esfuerzo del ejecutante y su progresión medible:

```text
Ejecutante entrena → Registra datos → Sistema analiza →
Sistema prescribe → Ejecutante entrena con dirección → ...
```

### Tres capacidades fundamentales

**Prescribir.** Antes de cada sesión, Tension determina automáticamente qué módulo y versión corresponde según la rotación cíclica, presenta los ejercicios prescritos y calcula la carga objetivo derivada del historial del ejecutante. No hay improvisación: cada variable está derivada de datos reales.

**Detectar.** Al cerrar cada sesión, Tension evalúa la progresión de cada ejercicio contra su historial, clasifica el resultado (progresión, mantenimiento o regresión), detecta mesetas, identifica fatiga acumulada y emite alertas cuando los indicadores están fuera de la zona óptima. El ejecutante nunca se estanca sin saberlo.

**Evidenciar.** Tension construye un historial analítico completo: tonelaje por grupo muscular, tasa de progresión por ejercicio, tendencias de carga, adherencia, distribución de volumen. Cualquier punto en el tiempo se puede comparar contra cualquier otro. La progresión no es una sensación; es un hecho medible.

---

## 4. Principios Rectores

Estos principios son inamovibles. Toda decisión de diseño, implementación o priorización debe pasar por este filtro.

### 4.1 — La evidencia propia gobierna las decisiones

Tension no prescribe basándose en promedios poblacionales, tablas genéricas ni consejos de internet. Cada prescripción de carga, cada alerta y cada recomendación se deriva exclusivamente de los registros históricos del ejecutante. Es su propio laboratorio, su propia línea base, su propia evidencia.

### 4.2 — El dato sin interpretación no tiene valor

Capturar peso, repeticiones y RIR no es el fin; es el medio. El valor de Tension no está en almacenar — está en transformar ese dato en una señal accionable: subir carga, mantener, descargar, rotar versión. Si el sistema solo registrara, sería una libreta con pantalla.

### 4.3 — La sobrecarga progresiva es el principio rector

Toda la lógica del sistema está al servicio de un único objetivo fisiológico: que el ejecutante aplique estímulo creciente a sus músculos de forma sostenible. La Regla de Doble Umbral, la detección de mesetas, el protocolo de descarga, la rotación de versiones — todo existe para garantizar que la progresión sea continua, medida y protegida.

### 4.4 — El sistema es agnóstico al calendario

A Tension no le importa si hoy es lunes o domingo, si el ejecutante se ausentó una semana o tres meses. La rotación cíclica (A→B→C) responde exclusivamente a la pregunta "¿Qué toca ahora?" basándose en la última sesión completada. Esto respeta la vida real del ejecutante sin castigar las interrupciones ni premiar la rigidez artificial.

### 4.5 — La sesión pertenece al ejecutante, no al sistema

Tension prescribe qué ejercicios hacer y con qué carga, pero no impone el orden de ejecución. El gimnasio es caótico: hay estaciones ocupadas, equipos en mantenimiento, colas impredecibles. El ejecutante decide la secuencia según la realidad del momento. El sistema registra lo que realmente ocurrió, no lo que idealmente debería haber ocurrido.

### 4.6 — La integridad del dato es sagrada

Un registro de serie es un hecho consumado que no se puede alterar retroactivamente. Las sesiones cerradas son inmutables. Las comparaciones históricas dependen de que los datos reflejen la realidad sin manipulación posterior. Violar la integridad de los datos corrompe las decisiones del sistema: basura entra, basura sale.

### 4.7 — Offline ante todo

Tension funciona en el gimnasio, donde la conectividad es irrelevante e impredecible. Cero dependencia de red para cualquier funcionalidad core. Los datos viven en el dispositivo del ejecutante. La resiliencia no es un feature; es un requisito existencial.

### 4.8 — La fricción de registro debe tender a cero

Cada segundo que el ejecutante pasa registrando datos es un segundo que no está entrenando ni descansando entre series. El registro de una serie debe ser rápido, predecible y asistido: teclado numérico, precarga del último peso, máximo 3 toques. La captura no debe competir con el entrenamiento.

---

## 5. Motor de Reglas — El Diferenciador

Lo que distingue a Tension de cualquier rastreador de ejercicios es su **motor de reglas de progresión**: un conjunto de algoritmos deterministas que transforman datos crudos en decisiones concretas.

### Regla de Doble Umbral

El ejercicio sube de carga solo cuando se cumplen **ambas** condiciones simultáneamente: ≥ 12 repeticiones en al menos 3 de 4 series **y** RIR promedio ≥ 2. Esto protege al ejecutante de incrementos prematuros (cuando empuja al fallo) y de incrementos tardíos (cuando tiene reserva de sobra). El incremento es diferenciado: +2.5 Kg para tren superior, +5 Kg para tren inferior.

### Detección de Regresión y Fatiga

El sistema identifica regresión cuando el rendimiento cae con la misma carga, y fatiga acumulada del módulo cuando ≥ 50% de los ejercicios regresan simultáneamente. No se trata de una mala sesión; se trata de una señal fisiológica que requiere gestión.

### Detección de Mesetas

Tres sesiones consecutivas sin progresión en un ejercicio disparan el estado de meseta. El sistema diagnostica la causa probable (límite de carga, carga conservadora, fatiga sistémica) y prescribe acciones correctivas escalonadas: microincremento, extensión de repeticiones, rotación de versión.

### Protocolo de Descarga

Cuando la fatiga se acumula, el sistema prescribe un microciclo de descarga: carga al 60%, RIR 4-5, repeticiones mínimas. Al finalizar, reinicia al 90% para capturar la supercompensación. La descarga no es rendirse; es invertir en las ganancias futuras.

### Ejercicios Especiales

Peso corporal: progresión por repeticiones totales, sin Doble Umbral. Isométricos: progresión por segundos sostenidos, con marcado de "dominado" al alcanzar 45 segundos en las 4 series.

---

## 6. Experiencia del Ejecutante

### Antes de la sesión

El ejecutante abre Tension y ve exactamente qué le toca: Módulo B, Versión 2. Once ejercicios listados, cada uno con su carga objetivo derivada de su último registro. Si el Press de Elevación Sentado estuvo en 16 Kg la última vez y cumplió el Doble Umbral, hoy aparece con 18.5 Kg como objetivo. Sin preguntas, sin cálculos mentales.

### Durante la sesión

El ejecutante ejecuta los ejercicios en el orden que el gimnasio le permita. Termina una serie, toca el ejercicio, el peso ya está precargado, ingresa repeticiones, ingresa RIR, confirma. Tres toques. Si una estación está ocupada, sustituye el ejercicio por otro del mismo módulo desde el diccionario. El plan original no se toca.

### Al cerrar la sesión

Tension presenta un resumen inmediato: tonelaje total, progresión por ejercicio con señales visuales claras (verde = progresión, amarillo = mantenimiento, rojo = regresión), y las señales de acción para la próxima sesión. Si tres ejercicios muestran regresión, el ejecutante sabe que algo está pasando. Si el Press cumplió Doble Umbral, sabe que la próxima vez sube.

### Entre sesiones

Si el ejecutante quiere revisar su historial, ve tendencias de carga ascendentes, tasa de progresión por ejercicio, tonelaje por grupo muscular a lo largo de microciclos. Si lleva 12 días sin entrenar pierna, una alerta le recuerda que los cuádriceps pueden estar perdiendo adaptaciones.

---

## 7. Lo Que Tension No Es

- **No es un entrenador virtual.** No inventa rutinas, no sugiere ejercicios nuevos automáticamente, no adapta el plan dinámicamente por su cuenta. Ejecuta un plan definido y optimiza la progresión dentro de él. El ejecutante puede agregar ejercicios al Diccionario y asignarlos a versiones del plan manualmente.
- **No es una red social.** No tiene perfiles públicos, no comparte datos, no tiene leaderboards. Es una herramienta personal.
- **No es una app de nutrición.** No rastrea calorías, macros ni agua. Los factores de recuperación están fuera de su frontera.
- **No es una app de calendario.** No programa sesiones en días específicos. La rotación cíclica responde a "¿Qué toca?" no a "¿Qué día es?".
- **No requiere internet.** No tiene backend, no sincroniza en la nube, no depende de servicios externos. Es 100% local.
- **No es para principiantes sin plan.** Tension opera sobre un plan estructurado preexistente. Sin la disciplina del registro y la estructura del plan, el sistema no puede generar valor.

---

## 8. Métricas de Éxito del Producto

Tension cumple su propósito cuando:

| Indicador | Evidencia de éxito |
|-----------|-------------------|
| Cero improvisación | Cada sesión tiene módulo, versión, ejercicios y cargas prescritas antes de empezar |
| Cero estancamiento invisible | Toda meseta es detectada máximo en la 3ª sesión sin progresión y tiene diagnóstico causal |
| Trazabilidad completa | Cualquier ejercicio muestra su historial completo con clasificación de progresión por sesión |
| Decisiones basadas en evidencia | Toda prescripción de carga se deriva de la Regla de Doble Umbral, no de sensaciones |
| Registro sin fricción | Una serie se registra en máximo 3 toques con peso precargado |
| Resiliencia total | Datos inmunes a cierres inesperados, ausencias prolongadas y cambios de dispositivo |
| Autonomía del ejecutante | El sistema informa y recomienda pero nunca bloquea; el ejecutante siempre tiene la última palabra |

---

## 9. Alcance del Producto

### Incluido

- Registro y actualización del perfil del ejecutante (32 historias de usuario)
- Diccionario de 43 ejercicios precargados con media visual de ejecución correcta, extensible por el ejecutante
- Plan de entrenamiento precargado: 3 módulos × 9 versiones, rotación cíclica agnóstica al calendario
- Registro de sesiones completo: series, peso, repeticiones, RIR, sustituciones puntuales, ejercicios especiales (peso corporal, isométricos)
- Motor de reglas de progresión: Doble Umbral, detección de regresión/fatiga, mesetas con diagnóstico causal, acciones correctivas escalonadas
- Protocolo de descarga completo: activación, gestión del microciclo, reinicio post-descarga
- 6 KPIs: tasa de progresión, tonelaje por grupo muscular, distribución de volumen, RIR por módulo, adherencia semanal, velocidad de progresión de carga
- Historial analítico: por ejercicio, por sesión, tendencias de carga, evolución de tonelaje por grupo muscular
- Sistema de alertas: progresión baja, RIR fuera de zona, adherencia, caída de tonelaje, inactividad por módulo
- Backup y restore: exportar/importar datos con formato versionado

### Excluido

- Creación automática o generación inteligente de planes de entrenamiento (el sistema no genera planes; el ejecutante estructura el suyo)
- Tracking nutricional o de recuperación
- Funcionalidad social o de compartición
- Sincronización en la nube o backend remoto
- Soporte para tablets o modo horizontal
- Soporte multiidioma (solo español)
- Cifrado de datos (aplicación personal, datos no sensibles)

---

## 10. Contexto Técnico

| Aspecto | Decisión |
|---------|----------|
| Plataforma | Android nativo |
| Lenguaje | Kotlin |
| UI | Jetpack Compose |
| Persistencia | Room (SQLite local) |
| Arquitectura | MVVM (View–ViewModel–Model) |
| Motor de reglas | Módulo independiente, testeable sin dependencias de Android |
| Conectividad | 100% offline |
| Distribución | APK firmado, sin Google Play Store requerido |
| Compatibilidad | Android 8.0+ (API 26), pantallas 5"-7", 720p-1440p |
| Tema | Claro/oscuro automático según sistema operativo |

---

*Tension: donde el dato se convierte en decisión y la constancia se convierte en progreso.*
