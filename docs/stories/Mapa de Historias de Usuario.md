# Mapa de Historias de Usuario — Tension

## 1. Propósito

Este documento define el mapa completo de historias de usuario del sistema Tension. Cada historia está vinculada a uno o más requisitos funcionales (RF) y/o no funcionales (RNF) del documento de Requerimientos, garantizando trazabilidad total sin dejar ningún requisito huérfano.

Las decisiones técnicas de implementación (arquitectura, frameworks, patrones) no se expresan como historias separadas; emergerán durante el refinamiento de cada historia según las restricciones del proyecto.

---

## 2. Inventario de Requisitos

| Tipo | Cantidad | Rango |
|------|----------|-------|
| Requisitos Funcionales (RF) | 64 | RF01 — RF64 |
| Requisitos No Funcionales (RNF) | 37 | RNF01 — RNF37 |
| **Total** | **101** | |

---

## 3. Requisitos No Funcionales Transversales

Los siguientes 23 RNFs son restricciones de calidad que aplican al sistema completo. No se asignan a una historia específica porque permean todas las historias — serán criterios de aceptación implícitos durante el refinamiento y la implementación de cada una.

| ID | Requisito | Categoría |
|----|-----------|-----------|
| RNF01 | Rendimiento fluido; operaciones de cálculo y persistencia no bloquean la interfaz de usuario | Rendimiento |
| RNF06 | Elementos interactivos mínimo 48×48 dp (Material Design) | Usabilidad |
| RNF07 | Solo modo vertical (portrait) | Usabilidad |
| RNF08 | Interfaz en español, sin soporte multiidioma | Usabilidad |
| RNF09 | 100% offline, sin conexión a internet para funcionalidad core | Disponibilidad |
| RNF11 | Transacciones atómicas; no se pierden datos bajo uso normal | Confiabilidad |
| RNF14 | Base de datos local SQLite mediante Room | Persistencia |
| RNF19 | Migraciones de esquema automáticas y sin pérdida de datos | Persistencia |
| RNF20 | Compatible con Android 8.0+ (API 26) | Compatibilidad |
| RNF21 | Pantallas de 5" a 7", layout responsivo, sin soporte para tablets | Compatibilidad |
| RNF22 | Resoluciones desde 720p (HD) hasta 1440p (QHD) | Compatibilidad |
| RNF23 | Tema claro/oscuro automático según configuración del sistema operativo | Compatibilidad |
| RNF25 | Base de datos sin cifrado (datos no sensibles, uso personal) | Seguridad |
| RNF28 | Arquitectura MVVM (View–ViewModel–Model) | Mantenibilidad |
| RNF29 | Motor de reglas de progresión como módulo independiente, testeable sin dependencias de Android | Mantenibilidad |
| RNF30 | Pruebas unitarias para todas las reglas de negocio críticas (Doble Umbral, progresión, meseta, fatiga, descarga) | Mantenibilidad |
| RNF31 | Seed data (Diccionario, Plan, assets multimedia) en recursos versionados, no hardcodeado | Mantenibilidad |
| RNF32 | Inyección de dependencias para facilitar testing y reemplazo de implementaciones | Mantenibilidad |
| RNF33 | Kotlin como lenguaje principal | Restricción Técnica |
| RNF34 | Jetpack Compose para la interfaz de usuario | Restricción Técnica |
| RNF35 | Room como ORM con integración de coroutines y Flow | Restricción Técnica |
| RNF36 | Gradle con version catalog (libs.versions.toml) | Restricción Técnica |
| RNF37 | APK firmado para instalación directa, sin requerir Google Play Store | Restricción Técnica |

---

## 4. Mapa de Historias de Usuario

| ID | Historia de Usuario | Descripción Funcional | RF Cubiertos | RNF Específicos |
|----|--------------------|-----------------------|--------------|-----------------|
| HU-01 | Registrar y actualizar perfil del ejecutante | Registrar peso corporal (Kg), altura (m) y nivel de experiencia (principiante, intermedio, avanzado). Permitir actualización en cualquier momento. Campos numéricos con teclado numérico. Validación de rangos antes de persistir. | RF01, RF02 | RNF03, RNF12 |
| HU-02 | Consultar historial de peso corporal | Almacenar historial de cambios del peso corporal con fecha de cada actualización. Permitir al ejecutante consultar su evolución de peso en el tiempo. | RF03 | — |
| HU-03 | Consultar y gestionar Diccionario de Ejercicios con media visual | Diccionario precargado con 43 ejercicios base clasificados por módulo (A, B, C), tipo de equipo y zona muscular. Filtros por módulo, tipo de equipo y zona muscular. Cada ejercicio precargado muestra imagen estática (PNG 3D minimalista con fondo blanco) de ejecución correcta, accesible desde la consulta del diccionario y durante el registro de series en sesión activa. El ejecutante puede crear nuevos ejercicios proporcionando nombre, módulo, tipo de equipo, zona(s) muscular(es) y flags opcionales (peso corporal, isométrico, al fallo técnico); opcionalmente puede seleccionar una imagen desde la galería del dispositivo. La imagen de cualquier ejercicio (precargado o custom) puede agregarse o modificarse en cualquier momento desde el detalle (D2). Assets multimedia optimizados para dispositivos móviles. En futuras iteraciones, las imágenes podrán ser reemplazadas por video/GIF. | RF04, RF07, RF61, RF62 | RNF24 |
| HU-04 | Consultar y gestionar Plan de Entrenamiento | Plan precargado con 3 módulos (A, B, C), sus versiones (A×3, B×3, C×3) y los ejercicios asignados a cada combinación módulo-versión. Consulta mostrando ejercicios con zona muscular, tipo de equipo, series (4) y rango de repeticiones (8-12 o condición especial). Los ejercicios se presentan como listado sin orden obligatorio. El ejecutante puede asignar ejercicios del Diccionario a versiones del plan y desasignarlos, sin alterar el catálogo base. | RF05, RF06, RF08, RF63, RF64 | — |
| HU-05 | Determinar e iniciar sesión según rotación cíclica | Determinar automáticamente el módulo (A→B→C→A→B→C) y la versión (V1→V2→V3→V1) que corresponde, basándose exclusivamente en la secuencia de sesiones completadas, sin considerar el calendario. Persistir la posición en la rotación de módulos y la secuencia de versiones de forma indefinida, sin reiniciarla por ausencias. Al iniciar sesión, mostrar: módulo y versión, lista de ejercicios prescritos y carga objetivo derivada del último registro histórico de cada ejercicio. El estado de rotación debe sobrevivir a reinicios de app, dispositivo y actualizaciones. | RF09, RF10, RF11, RF12 | RNF13 |
| HU-06 | Registrar series de ejercicios en sesión activa | Capturar por cada serie: Peso en Kg (≥ 0), Repeticiones logradas (≥ 1) y RIR (0-5). Asociar automáticamente cada registro con Fecha, Módulo, Versión, Ejercicio ejecutado y Número de Serie secuencial (1-4). Permitir registrar ejercicios en cualquier orden. Vincular datos al ejercicio realmente ejecutado (incluyendo sustituciones). Mostrar estado de cada ejercicio: No Iniciado (0 series), En Ejecución (1-3 series), Completado (4 series). Registro en máximo 3 toques tras seleccionar ejercicio. Teclado numérico para campos de entrada. Pre-cargar último peso utilizado. Preservar datos de sesión en progreso si la app se cierra inesperadamente. Validar rangos antes de persistir. | RF13, RF14, RF15, RF17, RF22 | RNF02, RNF03, RNF04, RNF10, RNF12 |
| HU-07 | Sustituir ejercicio puntualmente en sesión activa | Permitir reemplazar un ejercicio prescrito por otro del mismo módulo (disponible en cualquier versión del módulo) durante la sesión activa. La sustitución es puntual: no modifica el Plan de Entrenamiento original para futuras sesiones. | RF16 | — |
| HU-08 | Registrar ejercicios de peso corporal e isométricos | Para ejercicios de peso corporal (Peso = 0 Kg): medir progresión exclusivamente por repeticiones totales en 4 series, sin aplicar Regla de Doble Umbral. Para ejercicios isométricos (Plancha, Plancha Lateral): registrar duración en segundos en lugar de repeticiones, medir progresión por segundos sostenidos dentro del rango 30-45 segundos. Marcar isométrico como "dominado" cuando 4 series alcancen ≥ 45 segundos. Validar rangos específicos de cada tipo. | RF31, RF32, RF33 | RNF12 |
| HU-09 | Cerrar sesión y avanzar rotación | Cerrar sesión como "Completada" (todas las series de todos los ejercicios registradas) o "Incompleta" (datos parciales conservados). Calcular tonelaje de sesión automáticamente al cerrar (Σ Peso × Repeticiones de todas las series). Avanzar posición en rotación cíclica de módulos y secuencia de versiones únicamente al cerrar sesión. Preservar datos si la app se cierra inesperadamente durante el cierre. | RF18, RF19, RF20, RF21 | RNF10 |
| HU-10 | Evaluar y clasificar progresión post-sesión | Al cerrar sesión, comparar datos de cada ejercicio contra su último registro histórico del mismo ejercicio (independientemente del módulo-versión). Clasificar progresión: "Progresión positiva" (aumentó carga y/o repeticiones con RIR estable), "Mantenimiento" (misma carga y repeticiones, RIR estable) o "Regresión" (disminuyó carga o repeticiones, o RIR subió ≥ 1.5 con misma carga). Calcular y almacenar RIR promedio de las 4 series como dato derivado. Actualizar estado persistente de progresión del ejercicio (Sin Historial, En Progresión, En Meseta, En Descarga). | RF23, RF24, RF28, RF43 | — |
| HU-11 | Prescribir carga objetivo según Regla de Doble Umbral | Señalar que un ejercicio está listo para incrementar carga cuando: ≥ 12 repeticiones en al menos 3 de 4 series **Y** RIR promedio de las 4 series ≥ 2. Calcular carga objetivo incrementada: +2.5 Kg para Módulos A y B (tren superior), +5 Kg para Módulo C (tren inferior). Si no se cumple la Regla de Doble Umbral, prescribir misma carga, priorizando progresión en repeticiones dentro del rango 8-12. | RF25, RF26, RF27 | — |
| HU-12 | Detectar regresión y fatiga acumulada | Detectar regresión en un ejercicio cuando: peso igual pero repeticiones caen en ≥ 2 de 4 series, o RIR promedio sube ≥ 1.5 puntos con misma carga y repeticiones similares. Detectar fatiga acumulada del módulo cuando se identifica regresión simultánea en ≥ 50% de los ejercicios de una misma sesión. | RF29, RF30 | — |
| HU-13 | Mostrar resumen post-sesión con señales de acción | Al cerrar sesión, mostrar resumen incluyendo: tonelaje total, cantidad de ejercicios completados, clasificación de progresión por ejercicio y señales de acción para la próxima sesión (subir carga, mantener o descargar). Las señales de progresión (↑ Progresión, = Mantenimiento, ↓ Regresión) deben ser visualmente distinguibles mediante colores e iconografía, sin depender únicamente del texto. | RF59 | RNF05 |
| HU-14 | Detectar y alertar meseta en ejercicios | Detectar estado de "Meseta" cuando no se registra progresión positiva (ni en carga ni en repeticiones) durante 3 sesiones consecutivas del mismo ejercicio. Emitir alerta de meseta con análisis causal: RIR consistentemente bajo (0-1) indica límite de carga, RIR alto (3+) indica carga conservadora, estancamiento grupal indica fatiga sistémica del grupo muscular. Señales visuales diferenciadas con colores e iconografía. | RF34, RF35 | RNF05 |
| HU-15 | Recomendar acciones correctivas escalonadas ante meseta | Sesión 4 sin progreso: recomendar microincremento de carga o extensión de repeticiones. Sesión 6 sin progreso: recomendar rotar a otra versión del módulo. | RF36 | — |
| HU-16 | Detectar necesidad de descarga por módulo | Detectar que un módulo requiere descarga cuando ≥ 50% de sus ejercicios están simultáneamente en estado de Meseta o Regresión, o cuando se detecta fatiga acumulada del módulo (vía RF30). | RF37 | — |
| HU-17 | Activar y gestionar ciclo de descarga completo | Activar modo Descarga (Deload) ajustando prescripción: carga al 60% de la habitual, 4 series, repeticiones en límite inferior (8), RIR objetivo 4-5. Mantener descarga activa durante 1 microciclo completo (A-B-C-A-B-C), sin cambiar versión del módulo. Calcular carga de reinicio post-descarga al 90% de la última carga pre-descarga para cada ejercicio. | RF38, RF39, RF40 | — |
| HU-18 | Contar microciclos completados | Llevar conteo de microciclos completados, incrementándolo cada vez que los 6 módulos de la rotación (A-B-C-A-B-C) hayan sido ejecutados. | RF41 | — |
| HU-19 | Calcular KPIs de rendimiento por ejercicio | Tasa de Progresión: porcentaje de sesiones con progresión positiva respecto al total de sesiones del ejercicio, evaluación por defecto cada 4 semanas. Velocidad de Progresión de Carga: diferencia peso actual - peso inicial, dividida por número de sesiones intermedias. | RF44, RF48 | — |
| HU-20 | Calcular KPIs de volumen por grupo muscular | Tonelaje Acumulado por Grupo Muscular por microciclo: Σ(Peso × Repeticiones) de todas las series de ejercicios del grupo muscular. Distribución de Volumen por Zona Muscular por microciclo: porcentaje de series totales de cada zona respecto al total de series del módulo. | RF45, RF49 | — |
| HU-21 | Calcular KPIs de intensidad y adherencia | RIR Promedio por Módulo: promedio aritmético de todos los RIR de todas las series del módulo en un período dado. Índice de Adherencia semanal: sesiones completadas / sesiones planificadas (objetivo 4-6) × 100. | RF46, RF47 | — |
| HU-22 | Monitorear tendencia de progresión por grupo muscular | Evaluar trayectoria del tonelaje acumulado y la tasa de progresión de los ejercicios asociados a cada grupo muscular a lo largo de los últimos 4 a 6 microciclos completados. | RF42 | — |
| HU-23 | Consultar historial y tendencia de carga de un ejercicio | Historial completo de registros de un ejercicio: fecha, peso, repeticiones, RIR y clasificación de progresión por sesión, ordenados cronológicamente. Visualización de la tendencia de evolución de carga a lo largo del tiempo. | RF50, RF51 | — |
| HU-24 | Consultar historial de sesiones pasadas | Para cada sesión pasada mostrar: fecha, módulo, versión, estado (Completada/Incompleta), tonelaje total y ejercicios ejecutados con sus datos. | RF60 | — |
| HU-25 | Consultar evolución de tonelaje por grupo muscular | Tonelaje acumulado por grupo muscular a lo largo de los microciclos. Identificar tendencias ascendentes, estables o en caída. | RF52 | — |
| HU-26 | Alertas de tasa de progresión baja | Emitir alerta cuando la Tasa de Progresión de un ejercicio sea < 40% en 4 semanas (umbral de alerta). Emitir alerta de crisis cuando sea < 20% en el mismo período. Señales visualmente distinguibles con colores e iconografía. | RF53 | RNF05 |
| HU-27 | Alertas de RIR por módulo | Emitir alerta cuando RIR Promedio de un módulo sea < 1.5 durante 2+ sesiones, recomendando descarga. Emitir alerta cuando RIR Promedio sea > 3.5 durante 2+ sesiones, recomendando incrementar carga. Señales visualmente distinguibles. | RF54, RF55 | RNF05 |
| HU-28 | Alertas de adherencia baja | Emitir alerta informativa cuando Adherencia Semanal sea < 60% en una semana. Emitir alerta de crisis si < 60% se mantiene durante 2+ semanas consecutivas. Señales visualmente distinguibles. | RF56 | RNF05 |
| HU-29 | Alertas de caída de tonelaje por grupo muscular | Emitir alerta cuando tonelaje de un grupo muscular caiga > 10% respecto al microciclo anterior. Emitir crisis cuando caiga > 20%. Verificar si la caída corresponde a descarga planificada o regresión no intencional. Señales visualmente distinguibles. | RF57 | RNF05 |
| HU-30 | Alertas de inactividad por módulo | Emitir alerta cuando transcurran > 10 días naturales sin ejecutar un módulo. Emitir crisis cuando superen los 14 días. Informar que el grupo muscular asociado puede estar perdiendo adaptaciones. Señales visualmente distinguibles. | RF58 | RNF05 |
| HU-31 | Exportar respaldo de datos | Generar archivo de backup autodescriptivo (JSON o SQLite exportado) con metadatos de versión para migraciones futuras. Almacenable en almacenamiento externo o compartible vía apps del sistema (Drive, correo). Proceso < 10 segundos para historial de hasta 2 años. Sin cifrado pero con advertencia al usuario. Solo permisos de almacenamiento. | — | RNF15, RNF17, RNF18, RNF26, RNF27 |
| HU-32 | Importar respaldo de datos | Cargar archivo de backup previamente exportado. Reemplazar datos actuales previa confirmación del usuario. Formato autodescriptivo con metadatos de versión para migraciones. Proceso < 10 segundos para historial de hasta 2 años. | — | RNF16, RNF17, RNF18 |

---

## 5. Matriz de Trazabilidad — Requisitos Funcionales → Historias de Usuario

| RF | HU | RF | HU | RF | HU |
|----|----|----|----|----|----|
| RF01 | HU-01 | RF21 | HU-09 | RF41 | HU-18 |
| RF02 | HU-01 | RF22 | HU-06 | RF42 | HU-22 |
| RF03 | HU-02 | RF23 | HU-10 | RF43 | HU-10 |
| RF04 | HU-03 | RF24 | HU-10 | RF44 | HU-19 |
| RF05 | HU-04 | RF25 | HU-11 | RF45 | HU-20 |
| RF06 | HU-04 | RF26 | HU-11 | RF46 | HU-21 |
| RF07 | HU-03 | RF27 | HU-11 | RF47 | HU-21 |
| RF08 | HU-04 | RF28 | HU-10 | RF48 | HU-19 |
| RF09 | HU-05 | RF29 | HU-12 | RF49 | HU-20 |
| RF10 | HU-05 | RF30 | HU-12 | RF50 | HU-23 |
| RF11 | HU-05 | RF31 | HU-08 | RF51 | HU-23 |
| RF12 | HU-05 | RF32 | HU-08 | RF52 | HU-25 |
| RF13 | HU-06 | RF33 | HU-08 | RF53 | HU-26 |
| RF14 | HU-06 | RF34 | HU-14 | RF54 | HU-27 |
| RF15 | HU-06 | RF35 | HU-14 | RF55 | HU-27 |
| RF16 | HU-07 | RF36 | HU-15 | RF56 | HU-28 |
| RF17 | HU-06 | RF37 | HU-16 | RF57 | HU-29 |
| RF18 | HU-09 | RF38 | HU-17 | RF58 | HU-30 |
| RF19 | HU-09 | RF39 | HU-17 | RF59 | HU-13 |
| RF20 | HU-09 | RF40 | HU-17 | RF60 | HU-24 |
| | | | | RF61 | HU-03 |
| | | | | RF62 | HU-03 |
| | | | | RF63 | HU-04 |
| | | | | RF64 | HU-04 |

**Resultado: 64/64 RF cubiertos → 0 huérfanos** ✅

---

## 6. Matriz de Trazabilidad — Requisitos No Funcionales → Historias de Usuario

### 6.1 RNFs Específicos (asignados a historias concretas)

| RNF | Historia(s) | Categoría |
|-----|-------------|-----------|
| RNF02 | HU-06 | Usabilidad |
| RNF03 | HU-01, HU-06 | Usabilidad |
| RNF04 | HU-06 | Usabilidad |
| RNF05 | HU-13, HU-14, HU-26, HU-27, HU-28, HU-29, HU-30 | Usabilidad |
| RNF10 | HU-06, HU-09 | Confiabilidad |
| RNF12 | HU-01, HU-06, HU-08 | Confiabilidad |
| RNF13 | HU-05 | Confiabilidad |
| RNF15 | HU-31 | Persistencia |
| RNF16 | HU-32 | Persistencia |
| RNF17 | HU-31, HU-32 | Persistencia |
| RNF18 | HU-31, HU-32 | Persistencia |
| RNF24 | HU-03 | Compatibilidad |
| RNF26 | HU-31 | Seguridad |
| RNF27 | HU-31 | Seguridad |

**Resultado: 14 RNFs asignados a historias específicas** ✅

### 6.2 RNFs Transversales (aplicables a todas las historias)

Los 23 RNFs listados en la §3 de este documento son restricciones de calidad del sistema completo. Se verifican implícitamente durante el refinamiento e implementación de cada historia.

**Resultado: 23 RNFs transversales declarados en §3** ✅

### 6.3 Totalización

| Tipo | Cantidad |
|------|----------|
| RNFs específicos (asignados a historias) | 14 |
| RNFs transversales (aplican a todo el sistema) | 23 |
| **Total** | **37/37 RNF cubiertos → 0 huérfanos** ✅ |

---

## 7. Resumen de Cobertura

| Tipo | Total | Cubiertos | Huérfanos |
|------|-------|-----------|-----------|
| Requisitos Funcionales (RF) | 64 | 64 | 0 |
| Requisitos No Funcionales (RNF) | 37 | 37 | 0 |
| **Total Requisitos** | **101** | **101** | **0** |

| Métrica | Valor |
|---------|-------|
| Historias de Usuario | 32 |
| RFs por historia (promedio) | 2.0 |
| RFs por historia (máximo) | 4 (HU-05, HU-09) |
| Historias sin RF (solo RNFs) | 2 (HU-31, HU-32) |
| Historias sin RNF específicos | 18 (cubiertas por RNFs transversales) |
