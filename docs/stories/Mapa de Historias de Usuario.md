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
| HU-08 | Registrar ejercicios de peso corporal e isométricos | Para ejercicios de peso corporal (Peso = 0 Kg): registrar con peso fijo en 0, campo no editable. Para ejercicios isométricos (Plancha, Plancha Lateral): registrar duración en segundos en lugar de repeticiones con referencia visual 30-45s. Validar rangos específicos de cada tipo. CAs de registro pre-implementados en HU-06. CAs de progresión redistribuidos a HU-10 y HU-11. | — | RNF12 |
| HU-09 | Cerrar sesión y avanzar rotación | Cerrar sesión como "Completada" (todas las series de todos los ejercicios registradas) o "Incompleta" (datos parciales conservados). Calcular tonelaje de sesión automáticamente al cerrar (Σ Peso × Repeticiones de todas las series). Avanzar posición en rotación cíclica de módulos y secuencia de versiones únicamente al cerrar sesión. Preservar datos si la app se cierra inesperadamente durante el cierre. | RF18, RF19, RF20, RF21 | RNF10 |
| HU-10 | Evaluar y clasificar progresión post-sesión | Al cerrar sesión, comparar datos de cada ejercicio contra su último registro histórico del mismo ejercicio (independientemente del módulo-versión). Clasificar progresión: "Progresión positiva" (aumentó carga y/o repeticiones con RIR estable), "Mantenimiento" (misma carga y repeticiones, RIR estable) o "Regresión" (disminuyó carga o repeticiones, o RIR subió ≥ 1.5 con misma carga). Calcular y almacenar RIR promedio de las 4 series como dato derivado. Actualizar estado persistente de progresión del ejercicio (Sin Historial, En Progresión, En Meseta, En Descarga). Para peso corporal: progresión por repeticiones totales. Para isométricos: progresión por segundos sostenidos y marcado "dominado" (4 series ≥ 45s). | RF23, RF24, RF28, RF31, RF32, RF33, RF43 | — |
| HU-11 | Prescribir carga objetivo según Regla de Doble Umbral | Señalar que un ejercicio está listo para incrementar carga cuando: ≥ 12 repeticiones en al menos 3 de 4 series **Y** RIR promedio de las 4 series ≥ 2. Calcular carga objetivo incrementada: +2.5 Kg para Módulos A y B (tren superior), +5 Kg para Módulo C (tren inferior). Si no se cumple la Regla de Doble Umbral, prescribir misma carga, priorizando progresión en repeticiones dentro del rango 8-12. No aplica a ejercicios de peso corporal (Δmin = 0). | RF25, RF26, RF27, RF31 | — |
| HU-12 | Motor de Detección: Regresión, Meseta y Necesidad de Descarga | Detectar regresión por caída de repeticiones, aumento de RIR o caída de carga. Detectar fatiga acumulada del módulo (regresión simultánea ≥ 50%). Detectar meseta por 3 sesiones sin progresión con análisis causal (RIR bajo = límite de carga, RIR alto = carga conservadora, estancamiento grupal = fatiga sistémica). Recomendar acciones correctivas escalonadas: sesión 4 (microincremento o extensión de reps), sesión 6 (rotar versión). Detectar necesidad de descarga por módulo cuando ≥ 50% de ejercicios en meseta/regresión. Señales visuales diferenciadas e informativas, no bloqueantes. **Consolida HU-12, HU-14, HU-15, HU-16 originales.** | RF29, RF30, RF34, RF35, RF36, RF37 | RNF05 |
| HU-13 | Mostrar resumen post-sesión con señales de acción | Al cerrar sesión, mostrar resumen incluyendo: tonelaje total, cantidad de ejercicios completados, clasificación de progresión por ejercicio y señales de acción para la próxima sesión (subir carga, mantener o descargar). Las señales de progresión (↑ Progresión, = Mantenimiento, ↓ Regresión) deben ser visualmente distinguibles mediante colores e iconografía, sin depender únicamente del texto. | RF59 | RNF05 |
| HU-14 | Protocolo de Descarga y Conteo de Microciclos | Activar modo Descarga (Deload) ajustando prescripción: carga al 60% de la habitual, 4 series, repeticiones en límite inferior (8), RIR objetivo 4-5. Mantener descarga activa durante 1 microciclo completo (A-B-C-A-B-C), versiones congeladas. Calcular carga de reinicio post-descarga al 90% de la carga pre-descarga con redondeo protector. Ejercicios bodyweight e isométricos con parámetros de descarga específicos. Llevar conteo persistente de microciclos completados, incluyendo durante descarga. **Consolida HU-17 y HU-18 originales.** | RF38, RF39, RF40, RF41 | — |
| HU-15 | Analítica y KPIs del Entrenamiento | Panel de analítica completo: Tasa de Progresión y Velocidad de Carga por ejercicio (período configurable, 4 semanas por defecto). Tonelaje Acumulado y Distribución de Volumen por grupo muscular por microciclo, con soporte para ejercicios multi-zona. RIR Promedio por Módulo con interpretación contextual e Índice de Adherencia semanal. Tendencia de progresión por grupo muscular (últimos 4-6 microciclos, clasificación Ascendente/Estable/En declive). Evolución temporal del tonelaje con desglose por los 12 grupos musculares. Manejo de datos insuficientes y exclusiones para peso corporal. **Consolida HU-19, HU-20, HU-21, HU-22 y HU-25 originales.** | RF42, RF44, RF45, RF46, RF47, RF48, RF49, RF52 | — |
| HU-16 | Historial de Ejercicios y Sesiones | Historial completo de registros de un ejercicio: fecha, peso, repeticiones, RIR y clasificación de progresión por sesión, orden cronológico descendente, independiente del módulo-versión, con tendencia de carga (o repeticiones para bodyweight). Listado de sesiones pasadas con fecha, módulo, versión, estado y tonelaje. Detalle de sesión con ejercicios ejecutados, series, sustituciones reflejadas. Inmutabilidad de datos. Manejo de historial vacío y ejercicios sin registros. **Consolida HU-23 y HU-24 originales.** | RF50, RF51, RF60 | — |
| HU-17 | Sistema de Alertas | Sistema de alertas proactivas con dos niveles de severidad (alerta y crisis): tasa de progresión baja (< 40% alerta, < 20% crisis, evaluación periódica). RIR por módulo fuera de rango (< 1.5 o > 3.5 sostenido 2+ sesiones, retiro automático). Adherencia semanal baja (< 60% una semana alerta, 2+ semanas crisis). Caída de tonelaje por grupo muscular (> 10% alerta, > 20% crisis, verificación de descarga planificada). Inactividad por módulo (> 10 días alerta, > 14 días crisis, con referencia a grupos musculares afectados). Todas informativas y no bloqueantes, con diferenciación visual por colores e iconografía. **Consolida HU-26, HU-27, HU-28, HU-29 y HU-30 originales.** | RF53, RF54, RF55, RF56, RF57, RF58 | RNF05 |
| HU-18 | Backup y Restauración | Exportar respaldo completo autodescriptivo (JSON o SQLite) con metadatos de versión. Almacenable en almacenamiento externo o compartible vía apps del sistema. Importar respaldo con validación de formato, confirmación de reemplazo, migración de versiones de esquema y rollback ante error. Proceso < 10 segundos para historial de hasta 2 años. Sin cifrado pero con advertencia. Solo permisos de almacenamiento. **Consolida HU-31 y HU-32 originales.** | — | RNF15, RNF16, RNF17, RNF18, RNF26, RNF27 |

---

## 5. Consolidación de Historias

Las historias HU-12 a HU-18 (post-consolidación) absorben las 21 historias originales HU-12 a HU-32, reduciendo el total de 32 a 18 historias sin perder ningún criterio de aceptación ni requisito funcional. La siguiente tabla documenta la trazabilidad:

| HU Consolidada | HUs Originales Absorbidas | CAs Totales |
|----------------|--------------------------|-------------|
| HU-12 | HU-12, HU-14, HU-15, HU-16 | 24 |
| HU-13 | HU-13 (sin cambios) | 7 |
| HU-14 | HU-17, HU-18 | 14 |
| HU-15 | HU-19, HU-20, HU-21, HU-22, HU-25 | 28 |
| HU-16 | HU-23, HU-24 | 12 |
| HU-17 | HU-26, HU-27, HU-28, HU-29, HU-30 | 29 |
| HU-18 | HU-31, HU-32 | 15 |
| **Total** | **21 → 7** | **129** |

---

## 6. Matriz de Trazabilidad — Requisitos Funcionales → Historias de Usuario

| RF | HU | RF | HU | RF | HU |
|----|----|----|----|----|----|
| RF01 | HU-01 | RF21 | HU-09 | RF41 | HU-14 |
| RF02 | HU-01 | RF22 | HU-06 | RF42 | HU-15 |
| RF03 | HU-02 | RF23 | HU-10 | RF43 | HU-10 |
| RF04 | HU-03 | RF24 | HU-10 | RF44 | HU-15 |
| RF05 | HU-04 | RF25 | HU-11 | RF45 | HU-15 |
| RF06 | HU-04 | RF26 | HU-11 | RF46 | HU-15 |
| RF07 | HU-03 | RF27 | HU-11 | RF47 | HU-15 |
| RF08 | HU-04 | RF28 | HU-10 | RF48 | HU-15 |
| RF09 | HU-05 | RF29 | HU-12 | RF49 | HU-15 |
| RF10 | HU-05 | RF30 | HU-12 | RF50 | HU-16 |
| RF11 | HU-05 | RF31 | HU-10, HU-11 | RF51 | HU-16 |
| RF12 | HU-05 | RF32 | HU-10 | RF52 | HU-15 |
| RF13 | HU-06 | RF33 | HU-10 | RF53 | HU-17 |
| RF14 | HU-06 | RF34 | HU-12 | RF54 | HU-17 |
| RF15 | HU-06 | RF35 | HU-12 | RF55 | HU-17 |
| RF16 | HU-07 | RF36 | HU-12 | RF56 | HU-17 |
| RF17 | HU-06 | RF37 | HU-12 | RF57 | HU-17 |
| RF18 | HU-09 | RF38 | HU-14 | RF58 | HU-17 |
| RF19 | HU-09 | RF39 | HU-14 | RF59 | HU-13 |
| RF20 | HU-09 | RF40 | HU-14 | RF60 | HU-16 |
| | | | | RF61 | HU-03 |
| | | | | RF62 | HU-03 |
| | | | | RF63 | HU-04 |
| | | | | RF64 | HU-04 |

**Resultado: 64/64 RF cubiertos → 0 huérfanos** ✅

---

## 7. Matriz de Trazabilidad — Requisitos No Funcionales → Historias de Usuario

### 7.1 RNFs Específicos (asignados a historias concretas)

| RNF | Historia(s) | Categoría |
|-----|-------------|-----------|
| RNF02 | HU-06 | Usabilidad |
| RNF03 | HU-01, HU-06 | Usabilidad |
| RNF04 | HU-06 | Usabilidad |
| RNF05 | HU-12, HU-13, HU-17 | Usabilidad |
| RNF10 | HU-06, HU-09 | Confiabilidad |
| RNF12 | HU-01, HU-06, HU-08 | Confiabilidad |
| RNF13 | HU-05 | Confiabilidad |
| RNF15 | HU-18 | Persistencia |
| RNF16 | HU-18 | Persistencia |
| RNF17 | HU-18 | Persistencia |
| RNF18 | HU-18 | Persistencia |
| RNF24 | HU-03 | Compatibilidad |
| RNF26 | HU-18 | Seguridad |
| RNF27 | HU-18 | Seguridad |

**Resultado: 14 RNFs asignados a historias específicas** ✅

### 7.2 RNFs Transversales (aplicables a todas las historias)

Los 23 RNFs listados en la §3 de este documento son restricciones de calidad del sistema completo. Se verifican implícitamente durante el refinamiento e implementación de cada historia.

**Resultado: 23 RNFs transversales declarados en §3** ✅

### 7.3 Totalización

| Tipo | Cantidad |
|------|----------|
| RNFs específicos (asignados a historias) | 14 |
| RNFs transversales (aplican a todo el sistema) | 23 |
| **Total** | **37/37 RNF cubiertos → 0 huérfanos** ✅ |

---

## 8. Resumen de Cobertura

| Tipo | Total | Cubiertos | Huérfanos |
|------|-------|-----------|-----------|
| Requisitos Funcionales (RF) | 64 | 64 | 0 |
| Requisitos No Funcionales (RNF) | 37 | 37 | 0 |
| **Total Requisitos** | **101** | **101** | **0** |

| Métrica | Valor |
|---------|-------|
| Historias de Usuario | 18 |
| Historias implementadas (HU-01 a HU-11) | 11 |
| Historias pendientes (HU-12 a HU-18) | 7 |
| CAs en historias pendientes | 129 |
| RFs por historia (promedio) | 3.6 |
| RFs por historia (máximo) | 8 (HU-15) |
| Historias sin RF (solo RNFs) | 1 (HU-18) |
| Historias sin RNF específicos | 11 (cubiertas por RNFs transversales) |
