# Documento de Requerimientos del Sistema (Funcionales y No Funcionales)

> Este documento traduce la definición abstracta del negocio (SDD) en mandatos accionables, evaluables y estrictos para las máquinas. Las descripciones deben ser atómicas y verificables para que el agente *Architect* y el *Peer-Reviewer* puedan usarlas como checklist.
>

## 1. Capacidades Funcionales Base (Requerimientos Funcionales)

*Esta sección define QUÉ capacidades debe tener el sistema a nivel modular. El detalle exacto de CÓMO el usuario interactúa con ellas pertenece a las Historias de Usuario.*

### Módulo: Perfil del Ejecutante

- **[RF-01] Registro de Perfil:** El sistema debe permitir registrar el perfil del ejecutante con los siguientes datos: peso corporal (Kg), altura (m) y nivel de experiencia (principiante, intermedio, avanzado).
- **[RF-02] Actualización de Perfil:** El sistema debe permitir actualizar los datos del perfil del ejecutante en cualquier momento.
- **[RF-03] Historial de Peso Corporal:** El sistema debe almacenar un historial de cambios del peso corporal con la fecha de cada actualización, permitiendo al ejecutante consultar su evolución de peso en el tiempo.

### Módulo: Diccionario de Ejercicios

- **[RF-04] Diccionario Precargado:** El sistema debe contener precargado el Diccionario de Ejercicios con un catálogo base de ejercicios clasificados por tipo de equipo y zona muscular. El ejecutante puede ampliar el diccionario con ejercicios propios (RF-62).
- **[RF-07] Filtros del Diccionario:** El sistema debe permitir consultar el Diccionario de Ejercicios con filtros por tipo de equipo y zona muscular.
- **[RF-61] Imagen de Referencia de Ejercicio:** El sistema debe mostrar para cada ejercicio del Diccionario una imagen estática (PNG) que ilustre la ejecución correcta del movimiento, accesible desde la consulta del diccionario y durante el registro de series en una sesión activa. En futuras iteraciones, las imágenes estáticas podrán ser reemplazadas por videos o animaciones sin cambios en la arquitectura.
- **[RF-62] Creación de Ejercicio Personalizado:** El sistema debe permitir al ejecutante crear un nuevo ejercicio en el Diccionario proporcionando: nombre, tipo de equipo (seleccionable de los tipos existentes), zona(s) muscular(es) objetivo (seleccionable de las zonas existentes), e indicación opcional de si es de peso corporal, isométrico o al fallo técnico. El ejercicio creado se integra al catálogo de forma permanente y queda disponible para sustitución y asignación. Opcionalmente, el ejecutante puede asociar una imagen desde la galería del dispositivo; si no selecciona imagen, se muestra un marcador de posición (placeholder).

### Módulo: Plan de Entrenamiento

- **[RF-05] Creación y Edición del Plan:** El sistema debe permitir al ejecutante crear y editar su propio Plan de Entrenamiento, definiendo cualquier número de rutinas y versiones, y asignando ejercicios libremente a cada rutina/versión junto con el número de series y el rango de repeticiones.
- **[RF-06] Listado sin Orden Obligatorio:** El sistema debe presentar los ejercicios de cada versión del plan como un listado sin orden obligatorio de ejecución; el listado no implica secuencia.
- **[RF-08] Consulta del Plan:** El sistema debe permitir consultar el Plan de Entrenamiento mostrando, para cada rutina y versión, los ejercicios asignados con su zona muscular, tipo de equipo, series y rango de repeticiones.
- **[RF-63] Asignación de Ejercicio a Versión:** El sistema debe permitir al ejecutante asignar un ejercicio del Diccionario (precargado o creado) a una versión específica de cualquier rutina del Plan de Entrenamiento, especificando series (por defecto 4) y rango de repeticiones (por defecto 8-12). La asignación no altera las asignaciones existentes de otras versiones.
- **[RF-64] Desasignación de Ejercicio del Plan:** El sistema debe permitir al ejecutante desasignar (remover) un ejercicio de una versión específica de cualquier rutina del Plan de Entrenamiento. Al desasignar un ejercicio que pertenece a un puesto con alternativas, se eliminan todas las asignaciones del puesto. La desasignación no elimina los ejercicios del Diccionario ni afecta su historial registrado. No se permite desasignar mientras haya una sesión activa de esa versión.
- **[RF-65] Ejercicios Alternativos por Puesto:** El sistema debe permitir al ejecutante agregar ejercicios alternativos a un puesto (slot) del plan. Las alternativas se muestran como una sola fila con los nombres concatenados con "ó" y comparten series y repeticiones. Al iniciar sesión, se asigna el ejercicio primario del slot; el ejecutante puede intercambiar a otra alternativa del puesto mediante un control de selección mientras el ejercicio esté sin iniciar.

### Módulo: Rotación y Prescripción

- **[RF-09] Determinación de Rutina por Rotación:** El sistema debe determinar automáticamente qué rutina corresponde ejecutar según la posición actual en la rotación cíclica definida por el ejecutante, basándose únicamente en la secuencia de sesiones completadas, sin considerar el día de la semana ni la fecha del calendario.
- **[RF-10] Determinación de Versión por Secuencia:** El sistema debe determinar automáticamente qué versión de la rutina corresponde según la secuencia de versiones definida por el ejecutante para cada rutina.
- **[RF-11] Persistencia de la Rotación:** El sistema debe persistir la posición en la rotación de rutinas y la secuencia de versiones de forma indefinida en el almacenamiento local, sin reiniciarla por ausencias del ejecutante independientemente de su duración.

### Módulo: Registro de Sesión

- **[RF-12] Inicio de Sesión con Prescripción:** El sistema debe permitir iniciar una nueva sesión mostrando: la rutina y versión que corresponde, la lista de ejercicios prescritos para esa combinación, y la carga objetivo para cada ejercicio derivada de su último registro histórico.
- **[RF-13] Registro de Series:** El sistema debe permitir registrar los datos de cada serie de un ejercicio durante una sesión activa, capturando: Peso en Kg (≥ 0), Repeticiones logradas (≥ 1) y RIR en escala de 0 a 2.
- **[RF-14] Asociación Automática de Metadatos:** El sistema debe asociar automáticamente cada registro de serie con la fecha actual, la rutina, la versión, el ejercicio ejecutado y el número de serie secuencial (1, 2, 3, 4...), sin requerir que el ejecutante ingrese estos datos manualmente.
- **[RF-15] Orden Libre de Ejercicios:** El sistema debe permitir al ejecutante registrar los ejercicios de la sesión en cualquier orden, sin imponer la secuencia en que aparecen en el Plan de Entrenamiento.
- **[RF-16] Sustitución Puntual de Ejercicio:** El sistema debe permitir sustituir puntualmente un ejercicio prescrito por otro ejercicio de la misma zona muscular durante una sesión activa, sin modificar el Plan de Entrenamiento original para futuras sesiones.
- **[RF-17] Vinculación al Ejercicio Ejecutado:** El sistema debe vincular los datos registrados de cada serie al ejercicio que realmente se ejecutó (incluyendo sustituciones), garantizando que la toma de datos sea coherente con lo que efectivamente se realizó en la sesión.
- **[RF-22] Estado del Ejercicio en Sesión:** El sistema debe mostrar durante una sesión activa el estado de cada ejercicio: No Iniciado (0 series registradas), En Ejecución (al menos 1 serie registrada sin finalizar) o Completado (ejercicio finalizado explícitamente o automáticamente al cerrar la sesión). El ejecutante puede registrar series extra antes de finalizar.
- **[RF-18] Cierre de Sesión Completada:** El sistema debe permitir cerrar una sesión como "Completada" cuando todas las series de todos los ejercicios de la sesión hayan sido registradas.
- **[RF-19] Cierre de Sesión Incompleta:** El sistema debe permitir cerrar una sesión como "Incompleta" cuando el ejecutante no pueda finalizar todos los ejercicios, conservando todos los datos parciales registrados hasta ese momento.
- **[RF-20] Cálculo de Tonelaje de Sesión:** El sistema debe calcular automáticamente el tonelaje de la sesión al cerrarla, sumando el producto de Peso × Repeticiones de todas las series registradas en la sesión.
- **[RF-21] Actualización de Rotación al Cerrar:** El sistema debe actualizar la posición en la rotación cíclica de rutinas y la secuencia de versiones únicamente al cerrar una sesión (ya sea Completada o Incompleta), avanzando a la siguiente rutina y registrando la versión ejecutada.
- **[RF-59] Resumen de Sesión al Cierre:** El sistema debe mostrar al ejecutante un resumen de la sesión al cerrarla, incluyendo: tonelaje total, cantidad de ejercicios completados, clasificación de progresión por ejercicio y señales de acción para la próxima sesión (subir carga, mantener o descargar).

### Módulo: Análisis de Progresión

- **[RF-23] Comparación Histórica por Ejercicio:** El sistema debe comparar, al cerrar una sesión, los datos de cada ejercicio contra su último registro histórico del mismo ejercicio (independientemente de la rutina-versión), evaluando si hubo aumento de carga, aumento de repeticiones o variación significativa del RIR.
- **[RF-24] Clasificación de Progresión:** El sistema debe clasificar automáticamente la progresión de cada ejercicio en una sesión como: "Progresión positiva" (aumentó carga y/o repeticiones con RIR estable), "Mantenimiento" (misma carga y repeticiones, RIR estable) o "Regresión" (disminuyó carga o repeticiones, o el RIR subió ≥ 1.5 puntos con la misma carga).
- **[RF-25] Detección del Doble Umbral:** El sistema debe señalar que un ejercicio está listo para incrementar carga cuando en la sesión más reciente se alcanzaron ≥ 12 repeticiones en al menos 3 de las series prescritas y el RIR promedio de las series registradas fue ≥ 2 (Regla de Doble Umbral).
- **[RF-26] Prescripción de Incremento de Carga:** El sistema debe calcular y prescribir la carga objetivo para la próxima sesión de un ejercicio que cumplió la Regla de Doble Umbral, incrementando la carga actual en 2.5 Kg para ejercicios de tren superior y en 5 Kg para ejercicios de tren inferior.
- **[RF-27] Prescripción de Mantenimiento de Carga:** El sistema debe prescribir la misma carga para la próxima sesión de un ejercicio que no cumplió la Regla de Doble Umbral, manteniendo el objetivo de progresar en repeticiones dentro del rango de 8 a 12.
- **[RF-28] Cálculo de RIR Promedio:** El sistema debe calcular el RIR promedio de las series registradas de un ejercicio en cada sesión y almacenarlo como dato derivado para su uso en reglas de decisión y KPIs.
- **[RF-29] Detección de Regresión:** El sistema debe detectar regresión en un ejercicio cuando, comparado con su última sesión, el peso es igual pero las repeticiones caen en ≥ 2 de las series prescritas, o el RIR promedio sube en ≥ 1.5 puntos con la misma carga y repeticiones similares.
- **[RF-30] Detección de Fatiga Acumulada de Rutina:** El sistema debe detectar fatiga acumulada de una rutina cuando se identifica regresión simultánea en ≥ 50% de los ejercicios de una misma sesión.
- **[RF-31] Progresión de Ejercicios de Peso Corporal:** Para ejercicios de peso corporal (Peso = 0 Kg), el sistema debe medir la progresión exclusivamente por el total de repeticiones logradas en las series registradas, sin aplicar la Regla de Doble Umbral de carga.
- **[RF-32] Registro de Ejercicios Isométricos:** Para ejercicios isométricos (Plancha, Plancha Lateral), el sistema debe registrar la duración en segundos en lugar de repeticiones y medir la progresión por los segundos sostenidos dentro del rango prescrito de 30 a 45 segundos.
- **[RF-33] Dominio de Ejercicio Isométrico:** El sistema debe marcar un ejercicio isométrico como "dominado" cuando todas las series prescritas alcancen ≥ 45 segundos, indicando al ejecutante que el ejercicio ya no ofrece estímulo progresivo suficiente.
- **[RF-43] Almacenamiento del Estado de Progresión:** El sistema debe almacenar el estado de progresión de cada ejercicio a lo largo del tiempo: Sin Historial, En Progresión, En Meseta o En Descarga, actualizándolo automáticamente según los eventos de sesión y las reglas de negocio.

### Módulo: Detección de Mesetas

- **[RF-34] Detección de Meseta:** El sistema debe detectar que un ejercicio está en estado de "Meseta" cuando no se ha registrado progresión positiva (ni en carga ni en repeticiones) durante 3 sesiones consecutivas del mismo ejercicio.
- **[RF-35] Alerta de Meseta con Análisis Causal:** El sistema debe emitir una alerta de meseta al ejecutante cuando un ejercicio entra en estado de Meseta, incluyendo un análisis causal basado en los datos: RIR consistentemente bajo (0) indica límite de carga, RIR alto (> 1.8) indica carga conservadora, y estancamiento grupal indica fatiga sistémica del grupo muscular.
- **[RF-36] Acciones Correctivas Escalonadas:** El sistema debe recomendar acciones correctivas escalonadas ante una meseta: en la sesión 4 sin progreso, recomendar microincremento de carga o extensión de repeticiones; en la sesión 6 sin progreso, recomendar rotar a otra versión de la rutina.
- **[RF-37] Detección de Necesidad de Descarga:** El sistema debe detectar que una rutina requiere descarga cuando ≥ 50% de sus ejercicios están simultáneamente en estado de Meseta o Regresión, o cuando se detecta fatiga acumulada de la rutina (RF-30).

### Módulo: Protocolo de Descarga

- **[RF-38] Activación del Modo Descarga:** El sistema debe permitir activar un modo de Descarga (Deload) que ajuste los parámetros de la prescripción de sesión: carga al 60% de la carga habitual, mantener series prescritas, repeticiones en el límite inferior del rango (8) y RIR objetivo de 2.
- **[RF-39] Duración del Período de Descarga:** El sistema debe mantener la descarga activa durante 1 microciclo completo (todas las rutinas del plan), sin cambiar la versión de cada rutina durante el período de descarga.
- **[RF-40] Carga de Reinicio Post-Descarga:** El sistema debe calcular automáticamente la carga de reinicio post-descarga al 90% de la última carga de trabajo pre-descarga para cada ejercicio, y utilizarla como carga objetivo para la primera sesión del nuevo mesociclo.

### Módulo: KPIs y Métricas

- **[RF-41] Conteo de Microciclos:** El sistema debe llevar un conteo de microciclos completados, incrementándolo cada vez que todas las rutinas creadas por el ejecutante hayan sido ejecutadas en la rotación.
- **[RF-42] Monitoreo de Tendencia por Grupo Muscular:** El sistema debe monitorear la tendencia de progresión de cada grupo muscular a lo largo de los últimos 4 a 6 microciclos completados, evaluando la trayectoria del tonelaje acumulado y la tasa de progresión de los ejercicios asociados.
- **[RF-44] Tasa de Progresión por Ejercicio (KPI-1):** El sistema debe calcular la Tasa de Progresión por Ejercicio: porcentaje de sesiones con progresión positiva respecto al total de sesiones del ejercicio en un período configurable, con evaluación por defecto cada 4 semanas.
- **[RF-45] Tonelaje Acumulado por Grupo Muscular (KPI-2):** El sistema debe calcular el Tonelaje Acumulado por Grupo Muscular por microciclo, sumando el producto de Peso × Repeticiones de todas las series de todos los ejercicios que trabajan ese grupo muscular.
- **[RF-46] RIR Promedio por Rutina (KPI-3):** El sistema debe calcular el RIR Promedio por Rutina, promediando aritméticamente todos los valores de RIR registrados en todas las series de las sesiones de la rutina en un período dado.
- **[RF-47] Índice de Adherencia Semanal (KPI-4):** El sistema debe calcular el Índice de Adherencia semanal: sesiones completadas en la semana divididas por las sesiones planificadas (objetivo de frecuencia del ejecutante, entre 4 y 6), expresado como porcentaje.
- **[RF-48] Velocidad de Progresión de Carga (KPI-5):** El sistema debe calcular la Velocidad de Progresión de Carga por ejercicio: diferencia entre el peso actual y el peso inicial, dividida por el número de sesiones intermedias.
- **[RF-49] Distribución de Volumen por Zona Muscular (KPI-6):** El sistema debe calcular la Distribución de Volumen por Zona Muscular por microciclo: porcentaje de series totales de cada zona muscular respecto al total de series de la rutina.

### Módulo: Historial y Consultas Analíticas

- **[RF-50] Historial de Registros por Ejercicio:** El sistema debe permitir al ejecutante consultar el historial completo de registros de un ejercicio específico, mostrando para cada sesión: fecha, peso, repeticiones, RIR y clasificación de progresión, ordenados cronológicamente.
- **[RF-51] Tendencia de Carga por Ejercicio:** El sistema debe permitir al ejecutante visualizar la tendencia de carga de un ejercicio a lo largo del tiempo, mostrando la evolución del peso utilizado en sus sesiones históricas.
- **[RF-52] Tonelaje por Grupo Muscular (Consulta):** El sistema debe permitir al ejecutante consultar el tonelaje acumulado por grupo muscular a lo largo de los microciclos, identificando tendencias ascendentes, estables o en caída.
- **[RF-60] Historial de Sesiones:** El sistema debe permitir al ejecutante consultar el historial de sesiones pasadas, mostrando para cada sesión: fecha, rutina, versión, estado (Completada/Incompleta), tonelaje total y los ejercicios ejecutados con sus datos.

### Módulo: Sistema de Alertas

- **[RF-53] Alerta de Tasa de Progresión Baja:** El sistema debe emitir una alerta cuando la Tasa de Progresión de un ejercicio sea < 40% en un período de 4 semanas (umbral de alerta) y una alerta de crisis cuando sea < 20% en el mismo período.
- **[RF-54] Alerta de RIR Promedio Bajo:** El sistema debe emitir una alerta cuando el RIR Promedio de una rutina sea < 0.5 de forma sostenida durante 2 o más sesiones, recomendando prescribir una descarga.
- **[RF-55] Alerta de RIR Promedio Alto:** El sistema debe emitir una alerta cuando el RIR Promedio de una rutina sea > 1.8 de forma sostenida durante 2 o más sesiones, recomendando incrementar la carga de los ejercicios de la rutina.
- **[RF-56] Alerta de Adherencia Baja:** El sistema debe emitir una alerta informativa cuando la Adherencia Semanal sea < 60% en una semana, y una alerta de crisis si se mantiene < 60% durante 2 o más semanas consecutivas.
- **[RF-57] Alerta de Caída de Tonelaje:** El sistema debe emitir una alerta cuando el Tonelaje de un Grupo Muscular caiga > 10% respecto al microciclo anterior (alerta) o > 20% (crisis), verificando si la caída corresponde a una descarga planificada o a una regresión no intencional.
- **[RF-58] Alerta de Inactividad por Rutina:** El sistema debe emitir una alerta cuando transcurran más de 10 días naturales sin ejecutar una rutina específica, y una alerta de crisis si superan los 14 días, informando al ejecutante que el grupo muscular asociado puede estar perdiendo adaptaciones.

---

## 2. Atributos de Calidad (Requerimientos No Funcionales)

*Esta sección define las restricciones físicas, temporales y de calidad bajo las cuales las capacidades funcionales están obligadas a operar.*

### Rendimiento

- **[RNF-01] Fluidez de Operaciones:** El sistema debe mantener un rendimiento fluido en todas las operaciones de usuario (registro de series, cálculos de progresión, consultas de historial, cierre de sesión). Las operaciones de cálculo y persistencia no deben bloquear la interfaz de usuario.

### Usabilidad

- **[RNF-02] Registro en Máximo 3 Toques:** El registro de una serie debe requerir un máximo de 3 toques después de seleccionar el ejercicio: ingresar peso, ingresar repeticiones, ingresar RIR y confirmar.
- **[RNF-03] Teclado Numérico Optimizado:** Los campos de entrada numérica (peso, repeticiones, RIR) deben mostrar un teclado numérico optimizado, no el teclado alfanumérico completo.
- **[RNF-04] Precarga del Último Peso:** El sistema debe pre-cargar el último peso utilizado en cada campo de peso, permitiendo al ejecutante confirmar rápidamente si la carga no cambió.
- **[RNF-05] Señales Visuales de Progresión Distinguibles:** Las señales de progresión (↑ Progresión, = Mantenimiento, ↓ Regresión) deben ser visualmente distinguibles mediante colores e iconografía, sin depender únicamente del texto.
- **[RNF-06] Tamaño Mínimo de Elementos Interactivos:** El tamaño mínimo de los elementos interactivos (botones, campos) debe ser de 48×48 dp, siguiendo las guías de accesibilidad de Material Design.
- **[RNF-07] Modo Vertical Exclusivo:** El sistema debe funcionar únicamente en modo vertical (portrait). No se requiere soporte para modo horizontal (landscape).
- **[RNF-08] Idioma Español:** Toda la interfaz de usuario debe estar en idioma español. No se requiere soporte multiidioma.

### Disponibilidad

- **[RNF-09] Funcionamiento 100% Offline:** El sistema debe funcionar 100% offline, sin requerir conexión a internet para ninguna funcionalidad core (registro, consulta, cálculos).

### Confiabilidad

- **[RNF-10] Resiliencia ante Cierre Inesperado:** El sistema debe preservar todos los datos de una sesión en progreso si la aplicación se cierra inesperadamente (por cierre del usuario, falta de batería o crash). Al reabrir, la sesión debe poder continuarse.
- **[RNF-11] Integridad de Datos Garantizada:** El sistema no debe perder datos registrados bajo ninguna circunstancia de uso normal. El mecanismo de persistencia debe usar transacciones atómicas para garantizar consistencia.
- **[RNF-12] Validación de Entradas:** El sistema debe validar todos los datos de entrada antes de persistirlos, rechazando valores fuera de los rangos permitidos (peso < 0, RIR > 2, etc.) con mensajes de error claros.
- **[RNF-13] Durabilidad del Estado de Rotación:** El estado de rotación (rutina actual, versión por rutina) debe persistir de forma durable y sobrevivir a reinicios de la aplicación, reinicios del dispositivo y actualizaciones de la app.

### Persistencia

- **[RNF-14] Almacenamiento Local Estructurado:** Todos los datos del sistema (perfil, sesiones, registros, estado de rotación, progresión de ejercicios) deben almacenarse en una base de datos local mediante un mecanismo de persistencia estructurado y transaccional.
- **[RNF-15] Exportación de Respaldo (Backup):** El sistema debe proporcionar una funcionalidad de exportación de respaldo que genere un archivo con todos los datos del ejecutante, almacenable en el almacenamiento externo del dispositivo o compartible mediante aplicaciones del sistema.
- **[RNF-16] Importación de Respaldo (Restore):** El sistema debe proporcionar una funcionalidad de importación de respaldo que permita cargar un archivo de backup previamente exportado, reemplazando los datos actuales previa confirmación explícita del ejecutante.
- **[RNF-17] Formato Autodescriptivo del Backup:** El formato del archivo de backup debe ser autodescriptivo (JSON u otro formato estructurado estándar) e incluir metadatos de versión para permitir migraciones futuras del esquema.
- **[RNF-18] Rendimiento del Proceso Backup/Restore:** El proceso de backup/restore debe completarse en menos de 10 segundos para un historial de hasta 2 años de datos.
- **[RNF-19] Migración Automática de Esquema:** El sistema debe manejar migraciones de esquema de la base de datos de forma automática y sin pérdida de datos cuando se actualice la aplicación.

### Compatibilidad

- **[RNF-20] Versión Mínima de Plataforma:** El sistema debe ser compatible con Android 8.0 (API nivel 26) como versión mínima, cubriendo aproximadamente el 95% de dispositivos Android activos.
- **[RNF-21] Rango de Pantallas Soportadas:** El sistema debe soportar pantallas desde 5 pulgadas hasta 7 pulgadas, adaptando el layout de forma responsiva. No se requiere soporte para tablets.
- **[RNF-22] Rango de Resoluciones Soportadas:** El sistema debe funcionar correctamente en dispositivos con resoluciones desde 720p (HD) hasta 1440p (QHD).
- **[RNF-23] Respeto del Tema del Sistema Operativo:** El sistema debe respetar la configuración de tema del sistema operativo (claro/oscuro), adaptando automáticamente los colores de la interfaz según el modo activo del dispositivo, incluyendo el modo dinámico basado en horario.
- **[RNF-24] Tamaño Máximo del Paquete Distribuible:** El paquete distribuible no debe exceder los 150 MB de tamaño, considerando que incluye assets multimedia (imágenes PNG) para los ejercicios precargados del Diccionario. Los assets deben estar optimizados para dispositivos móviles.

### Seguridad

- **[RNF-25] Sin Cifrado de Datos Locales Requerido:** Los datos almacenados en la base de datos local no requieren cifrado, dado que es una aplicación de uso personal y los datos no son sensibles (no hay información financiera, médica protegida ni credenciales).
- **[RNF-26] Advertencia en Archivo de Backup:** El archivo de backup exportado no requiere cifrado, pero debe incluir una advertencia al ejecutante de que contiene sus datos de entrenamiento.
- **[RNF-27] Permisos Mínimos Necesarios:** El sistema no debe requerir permisos más allá de los estrictamente necesarios: almacenamiento (para backup/restore) y ninguno más para funcionalidad core.

### Mantenibilidad

- **[RNF-28] Arquitectura en Capas Separadas:** El código debe seguir una arquitectura de tres capas: presentación (gestiona la visualización), lógica de presentación (gestiona el estado y la coordinación) y modelo (encapsula la lógica de negocio y el acceso a datos).
- **[RNF-29] Motor de Reglas Independiente y Testeable:** El motor de reglas de progresión (Reglas 1-7 del dominio) debe estar implementado como un módulo independiente, testeable unitariamente sin dependencias de la plataforma de ejecución.
- **[RNF-30] Cobertura de Pruebas Unitarias de Reglas Críticas:** El sistema debe incluir pruebas unitarias para todas las reglas de negocio críticas: Regla de Doble Umbral, clasificación de progresión, detección de meseta, detección de fatiga y protocolo de descarga.
- **[RNF-31] Datos Semilla Centralizados:** El conjunto de datos de inicialización (Diccionario de Ejercicios, Plan de Entrenamiento de referencia y assets multimedia de ejercicios) debe estar definido en archivos de recursos o código fuente versionado, no disperso en múltiples lugares, permitiendo actualizaciones centralizadas.
- **[RNF-32] Inyección de Dependencias:** El sistema debe usar inyección de dependencias para facilitar el testing y el reemplazo de implementaciones.

### Restricciones Técnicas

- **[RNF-33] Lenguaje Principal:** El sistema debe desarrollarse en Kotlin como lenguaje principal, aprovechando las características del lenguaje (seguridad ante nulos, corrutinas, funciones de extensión).
- **[RNF-34] Sistema de Composición de UI:** La interfaz de usuario debe implementarse con Jetpack Compose, no con el sistema de vistas XML tradicional.
- **[RNF-35] ORM para Persistencia Local:** El sistema debe usar Room como mecanismo de mapeo objeto-relacional para la persistencia local, aprovechando la integración con corrutinas y flujos reactivos para operaciones asíncronas.
- **[RNF-36] Gestión Centralizada de Dependencias:** El sistema debe usar el sistema de construcción Gradle con catálogo de versiones (libs.versions.toml) para gestión centralizada de dependencias.
- **[RNF-37] Distribución como APK Firmado:** El sistema debe ser distribuible como APK firmado para instalación directa, sin requerir publicación en tienda de aplicaciones para su uso inicial.

---

## 3. Glosario del Dominio (Ubiquitous Language)

*Esta sección establece el lenguaje común y unificado entre negocio, desarrolladores y agentes de IA. Evita sinonimias; si aquí se define un término, ese término debe usarse idénticamente en todo el código, esquemas e historias de usuario.*

### Conceptos del Dominio

- **Hipertrofia Muscular:** El aumento del tamaño de las fibras musculares mediante el estímulo de síntesis de proteínas, impulsado principalmente por la tensión mecánica. Objetivo fisiológico central que justifica la existencia del sistema.
- **Sobrecarga Progresiva:** El requerimiento fundamental de aumentar gradualmente el estrés impuesto al cuerpo (vía carga, repeticiones o calidad técnica) para forzar adaptaciones continuas. Es el principio rector de todo el sistema.
- **RIR (Reps In Reserve):** Medida subjetiva de intensidad. Es el número de repeticiones que el ejecutante estima que podría haber realizado antes de llegar al Fallo Técnico. Se registra en una escala entera de 0 a 2, donde 0 significa fallo técnico alcanzado y 2 indica esfuerzo con reserva moderada.
- **Fallo Técnico:** El punto exacto donde el ejecutante no puede completar otra repetición con la técnica prescrita. Es el límite máximo de esfuerzo; el sistema nunca prescribe entrenar con técnica degradada.
- **Repetición:** Una ejecución completa del rango de movimiento de un ejercicio, compuesta por una fase concéntrica (acortamiento muscular) y una fase excéntrica (alargamiento muscular).
- **Peso (Carga):** La resistencia externa medida en kilogramos (Kg) que el ejecutante mueve durante un ejercicio. Para ejercicios de peso corporal, se registra 0 Kg y la progresión se mide por repeticiones logradas.
- **Tempo:** El ritmo de ejecución de una repetición, dividido en cuatro fases (excéntrica - isométrica inferior - concéntrica - isométrica superior). Ejemplo: 3-1-1-0 significa 3 segundos bajando, 1 segundo abajo, 1 segundo subiendo, 0 pausa arriba.
- **Tonelaje:** El volumen total de carga movida en un contexto dado. Se calcula como Peso × Repeticiones × Series. Puede medirse por ejercicio, por sesión o por grupo muscular.
- **Volumen Semanal:** La cantidad total de series efectivas realizadas por grupo muscular en un período de 7 días (o un microciclo completo). Es el principal regulador de hipertrofia a nivel de programación.
- **Mesociclo:** Un bloque de entrenamiento (típicamente de 4 a 8 semanas) con un objetivo específico (acumulación de volumen o intensificación). Culmina con una semana de Descarga.
- **Descarga (Deload):** Una reducción planificada y temporal del volumen o la intensidad de entrenamiento (típicamente 1 microciclo completo), diseñada para permitir la recuperación y prevenir el sobreentrenamiento. Los parámetros son fijos: carga al 60% y reinicio al 90% post-descarga.
- **Rotación Cíclica:** El patrón secuencial en que se ejecutan las rutinas del ejecutante. Es agnóstica al calendario: al sistema solo le importa "¿Qué rutina y versión toca ahora?", no qué día de la semana es. La posición se persiste indefinidamente y no se reinicia por ausencias.
- **Zona Muscular:** La subdivisión anatómica específica dentro de un grupo muscular (ej.: Pecho Superior, Pecho Medio, Pecho Inferior son zonas musculares dentro del grupo muscular "Pecho").
- **Grupo Muscular:** La agrupación anatómica principal que un ejercicio trabaja (ej.: Pecho, Espalda, Cuádriceps, Glúteos).
- **Sesión:** La ejecución de una rutina en una versión específica en una fecha determinada. Contiene las series de los ejercicios realizados. El orden de ejecución de los ejercicios dentro de la sesión es libre.
- **Sustitución de Ejercicio:** El reemplazo puntual de un ejercicio prescrito por otro ejercicio de la misma zona muscular durante una sesión activa. La sustitución es temporal y no modifica el Plan de Entrenamiento original.
- **Meseta (Plateau):** Un estado donde un ejercicio no muestra progresión medible durante 3 sesiones consecutivas a pesar de mantener adherencia y esfuerzo consistentes. Activa el protocolo de acciones correctivas escalonadas.
- **Doble Umbral:** La condición dual que debe cumplirse simultáneamente para prescribir un incremento de carga en un ejercicio: ≥ 12 repeticiones en al menos 3 de las series prescritas Y RIR promedio ≥ 2 en la misma sesión.
- **Microciclo:** La secuencia completa de ejecución de todas las rutinas del ejecutante (una pasada completa por el plan). Su duración en días naturales depende de la frecuencia real de asistencia.
- **Ejecutante:** La persona que realiza el entrenamiento físico y es el operador y beneficiario principal del sistema. Diseña su propio plan, registra sus datos y toma decisiones finales sobre su entrenamiento.

### Catálogo de Ejercicios Precargados (Seed Data)

*26 ejercicios precargados en el Diccionario de Ejercicios. Cada uno es agnóstico de rutina y puede ser asignado libremente por el ejecutante a cualquier rutina o versión de su plan.*

- **Aductores:** Tipo — Máquina. Zona Muscular — Aductores.
- **Cruce de Polea Alta:** Tipo — Polea. Zona Muscular — Pecho Inferior.
- **Crunch Abdominal:** Tipo — Polea. Zona Muscular — Abdomen.
- **Curl Bayesian en Banco Inclinado:** Tipo — Mancuernas. Zona Muscular — Bíceps.
- **Curl de Concentración:** Tipo — Mancuerna. Zona Muscular — Bíceps.
- **Curl de Isquiotibiales Sentado:** Tipo — Máquina. Zona Muscular — Isquiotibiales.
- **Curl de Martillo Cruzado:** Tipo — Mancuernas. Zona Muscular — Bíceps.
- **Curl de Predicador:** Tipo — Mancuerna. Zona Muscular — Bíceps.
- **Elevación de Pantorrilla en Máquina de Pie:** Tipo — Máquina. Zona Muscular — Gemelos.
- **Elevación Lateral:** Tipo — Mancuernas o Polea. Zona Muscular — Hombro.
- **Extensión de Cuádriceps:** Tipo — Máquina. Zona Muscular — Cuádriceps.
- **Extensión de Tríceps en Polea (Pushdown):** Tipo — Polea con Cuerda o Polea con Barra en V. Zona Muscular — Tríceps.
- **Extensión de Tríceps por encima de la Cabeza:** Tipo — Mancuernas o Polea. Zona Muscular — Tríceps.
- **Face Pull:** Tipo — Polea con Cuerda. Zona Muscular — Espalda Alta.
- **Hip Thrust:** Tipo — Máquina. Zona Muscular — Glúteos.
- **Peso Muerto Rumano:** Tipo — Barra. Zona Muscular — Isquiotibiales, Glúteos.
- **Prensa Inclinada:** Tipo — Máquina. Zona Muscular — Cuádriceps.
- **Press de Banca Inclinado:** Tipo — Mancuerna o Polea o Barra. Zona Muscular — Pecho Superior.
- **Press de Banca Plano:** Tipo — Barra o Mancuernas. Zona Muscular — Pecho Medio.
- **Press Pallof:** Tipo — Polea. Zona Muscular — Abdomen.
- **Remo T Inclinado:** Tipo — Máquina. Zona Muscular — Espalda Media.
- **Sentadilla Búlgara:** Tipo — Mancuernas. Zona Muscular — Cuádriceps, Glúteos.
- **Sentadilla de Zumo:** Tipo — Mancuerna. Zona Muscular — Cuádriceps, Aductores.
- **Sentadilla Hack:** Tipo — Máquina. Zona Muscular — Cuádriceps.
- **Tirón de Dorsales:** Tipo — Polea. Zona Muscular — Dorsal Ancho.
- **Vuelos Posteriores:** Tipo — Mancuernas. Zona Muscular — Hombro.
