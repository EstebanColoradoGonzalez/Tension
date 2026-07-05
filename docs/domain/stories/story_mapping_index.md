# Mapa y Manifiesto de Historias (Story Mapping Index)

> Este documento actúa como el enrutador central y el mapa topológico del progreso. No contiene los detalles de implementación de cada historia; su propósito es organizar las unidades de trabajo en épicas lógicas, definir el orden cronológico del "Viaje del Agente" y establecer las fases de entrega (Releases). Para ver los detalles, criterios de aceptación o dependencias, navega al archivo `.md` enlazado de cada historia.
>
> **Nota de migración — Normalización de IDs:** Los IDs de historias han sido normalizados — la HU-15.5 pasa a ser HU-16 y los IDs subsiguientes se desplazan en +1 hasta HU-26. La columna "ID Legacy" en §2 preserva la referencia al ID original para trazabilidad completa.

---

## 1. Módulos y Épicas (El Viaje del Agente)

*Las épicas agrupan las historias por valor funcional entregado. Siguen un orden cronológico que refleja la secuencia natural de construcción del sistema.*

- **[EPIC-01] Perfil y Catálogos:** Configuración inicial del ejecutante y administración de los catálogos base (Diccionario de Ejercicios y Plan de Entrenamiento). Precondición de todo lo demás; sin perfil ni catálogo no hay sesión posible.
- **[EPIC-02] Ciclo de Entrenamiento:** Núcleo de la experiencia — determinar e iniciar sesión por rotación cíclica, registrar series (incluyendo peso corporal e isométricos), sustituir ejercicios puntualmente y cerrar sesión avanzando la rotación. Es el flujo más crítico del sistema.
- **[EPIC-03] Motor de Decisión:** Evaluación de progresión post-sesión, prescripción de carga por Regla de Doble Umbral, y detección de regresiones, mesetas y necesidad de descarga. Convierte datos crudos en decisiones informadas.
- **[EPIC-04] Retroalimentación y Control:** Resumen post-sesión con señales de acción, protocolo de descarga con conteo de microciclos, y analítica de KPIs completa. Cierra el ciclo de mejora continua.
- **[EPIC-05] Backlog de Consolidación:** Historias del backlog original pendientes: migración de estructura de módulos a división Pull/Push/Legs, historial de ejercicios y sesiones, sistema de alertas proactivas y backup/restauración.
- **[EPIC-06] Estabilización y Correcciones:** Correcciones de defectos detectados post-producción y mejoras de experiencia de usuario: filtro de sustituciones, reestructuración del plan y preview de sesión con cronómetro integrado.
- **[EPIC-07] Evolución del Plan:** Transición a plan completamente libre definido por el usuario (eliminación de módulos fijos A/B/C), actualización del catálogo de ejercicios y plan por defecto, ajustes de usabilidad y alternativas por puesto en el plan.

---

## 2. Índice de Historias de Usuario (Story Router)

*Catálogo completo de historias estructurado por épicas. La columna "ID Legacy" referencia el ID en el archivo fuente `docs_legacy/stories/`. Cada título es un enlace directo al archivo de la historia migrada.*

| ID Destino | Título | ID Legacy | Épica | Estado | Prioridad |
|---|---|---|---|---|---|
| **[HU-01](./HU-01.md)** | Registrar y actualizar perfil del ejecutante | HU-01 | EPIC-01 | `Done` | Alta |
| **[HU-02](./HU-02.md)** | Consultar historial de peso corporal | HU-02 | EPIC-01 | `Done` | Media |
| **[HU-03](./HU-03.md)** | Consultar y gestionar Diccionario de Ejercicios con media visual | HU-03 | EPIC-01 | `Done` | Alta |
| **[HU-04](./HU-04.md)** | Consultar y gestionar Plan de Entrenamiento | HU-04 | EPIC-01 | `Done` | Alta |
| **[HU-05](./HU-05.md)** | Determinar e iniciar sesión según rotación cíclica | HU-05 | EPIC-02 | `Done` | Crítica |
| **[HU-06](./HU-06.md)** | Registrar series de ejercicios en sesión activa | HU-06 | EPIC-02 | `Done` | Crítica |
| **[HU-07](./HU-07.md)** | Sustituir ejercicio puntualmente en sesión activa | HU-07 | EPIC-02 | `Done` | Media |
| **[HU-08](./HU-08.md)** | Registrar ejercicios de peso corporal e isométricos | HU-08 | EPIC-02 | `Done` | Alta |
| **[HU-09](./HU-09.md)** | Cerrar sesión y avanzar rotación | HU-09 | EPIC-02 | `Done` | Crítica |
| **[HU-10](./HU-10.md)** | Evaluar y clasificar progresión post-sesión | HU-10 | EPIC-03 | `Done` | Crítica |
| **[HU-11](./HU-11.md)** | Prescribir carga objetivo según Regla de Doble Umbral | HU-11 | EPIC-03 | `Done` | Crítica |
| **[HU-12](./HU-12.md)** | Motor de Detección: Regresión, Meseta y Necesidad de Descarga | HU-12 | EPIC-03 | `Done` | Alta |
| **[HU-13](./HU-13.md)** | Mostrar resumen post-sesión con señales de acción | HU-13 | EPIC-04 | `Done` | Alta |
| **[HU-14](./HU-14.md)** | Protocolo de Descarga y Conteo de Microciclos | HU-14 | EPIC-04 | `Done` | Alta |
| **[HU-15](./HU-15.md)** | Analítica y KPIs del Entrenamiento | HU-15 | EPIC-04 | `Done` | Alta |
| **[HU-16](./HU-16.md)** | Migración a división Pull / Push / Legs | HU-15.5 | EPIC-05 | `Todo` | Alta |
| **[HU-17](./HU-17.md)** | Historial de Ejercicios y Sesiones | HU-16 | EPIC-05 | `Todo` | Alta |
| **[HU-18](./HU-18.md)** | Sistema de Alertas | HU-17 | EPIC-05 | `Todo` | Alta |
| **[HU-19](./HU-19.md)** | Backup y Restauración | HU-18 | EPIC-05 | `Todo` | Alta |
| **[HU-20](./HU-20.md)** | Corrección de sustitución de ejercicios en sesión activa | HU-19 | EPIC-06 | `Todo` | Media |
| **[HU-21](./HU-21.md)** | Reestructuración del plan de entrenamiento y orden sugerido de ejecución | HU-20 | EPIC-06 | `Todo` | Media |
| **[HU-22](./HU-22.md)** | Preview de sesión sin iniciar y cronómetro para ejercicios por tiempo | HU-21 | EPIC-06 | `Done` | Media |
| **[HU-23](./HU-23.md)** | Transición a plan, rutinas y versiones 100% definidas por el usuario | HU-22 | EPIC-07 | `Done` | Alta |
| **[HU-24](./HU-24.md)** | Actualización del Diccionario de Ejercicios y Plan de Entrenamiento por defecto | HU-23 | EPIC-07 | `Done` | Alta |
| **[HU-25](./HU-25.md)** | Ajustes de usabilidad, rango de RIR, personalización de series/repeticiones y correcciones de datos | HU-24 | EPIC-07 | `Done` | Alta |
| **[HU-26](./HU-26.md)** | Alternativas por puesto en el plan de entrenamiento | HU-25 | EPIC-07 | `Done` | Alta |

---

## 3. Restricciones Transversales (RNF Implícitos)

*Restricciones de calidad del sistema que aplican a la totalidad de las historias. Los equipos de Desarrollo y QA deben aplicar estos criterios como validadores implícitos durante el refinamiento e implementación de cada historia.*

| ID | Restricción | Categoría |
|----|-------------|-----------|
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

**Total: 23 RNFs transversales declarados** ✅

---

## 4. Fases de Entrega (Release Plan / Slices)

### 4.1. Release 1.0 — MVP (EPIC-01 al EPIC-04)

- **Objetivo:** Sistema funcional completo para el ciclo de entrenamiento con Motor de Decisión activo: el ejecutante puede configurar su plan, registrar sesiones y recibir prescripciones de carga derivadas exclusivamente de su historial. Cero improvisación, cero estancamiento invisible.
- **Historias incluidas:** `HU-01`, `HU-02`, `HU-03`, `HU-04`, `HU-05`, `HU-06`, `HU-07`, `HU-08`, `HU-09`, `HU-10`, `HU-11`, `HU-12`, `HU-13`, `HU-14`, `HU-15`
- **Estado:** `Done` — 15 historias completadas.

### 4.2. Release 1.1 — Consolidación (EPIC-05)

- **Objetivo:** Completar las historias pendientes del backlog original: migración de la estructura de módulos a división Pull/Push/Legs, historial navegable de ejercicios y sesiones, sistema de alertas proactivas por niveles de severidad, y backup/restauración completo del historial.
- **Historias incluidas:** `HU-16`, `HU-17`, `HU-18`, `HU-19`
- **Estado:** `Todo` — 4 historias pendientes.

### 4.3. Release 1.2 — Estabilización (EPIC-06)

- **Objetivo:** Correcciones de defectos detectados en el comportamiento de sustitución de ejercicios, reestructuración del plan con orden sugerido biomecánico, y mejoras de experiencia de usuario (preview de sesión sin iniciarla, cronómetro integrado para isométricos).
- **Historias incluidas:** `HU-20`, `HU-21`, `HU-22`
- **Estado:** Parcial — `HU-22` Done; `HU-20`, `HU-21` pendientes.

### 4.4. Release 1.3 — Evolución del Plan (EPIC-07)

- **Objetivo:** Eliminar la estructura fija de módulos (A/B/C) y permitir al ejecutante definir sus propias rutinas, versiones y asignaciones de ejercicios completamente libres. Incluye actualización del catálogo y plan por defecto, ajustes de usabilidad globales y alternativas por puesto en el plan.
- **Historias incluidas:** `HU-23`, `HU-24`, `HU-25`, `HU-26`
- **Estado:** `Done` — 4 historias completadas.

---

## 5. Inventario de Requisitos

| Tipo | Cantidad | Rango |
|------|----------|-------|
| Requisitos Funcionales (RF) | 65 | RF01 — RF65 |
| Requisitos No Funcionales (RNF) | 37 | RNF01 — RNF37 |
| **Total** | **102** | |

---

## 6. Consolidación de Historias

Las historias HU-12 al HU-15 y HU-17 al HU-19 (IDs destino) absorbieron historias del diseño original, reduciendo el total de 32 a 26 sin perder ningún criterio de aceptación ni requisito funcional. La siguiente tabla documenta la trazabilidad de la consolidación:

| HU Consolidada (ID Destino) | ID Legacy | HUs Originales Pre-consolidación Absorbidas | CAs Totales |
|---|---|---|---|
| HU-12 | HU-12 | HU-12, HU-14, HU-15, HU-16 originales | 24 |
| HU-13 | HU-13 | HU-13 original (sin cambios) | 7 |
| HU-14 | HU-14 | HU-17, HU-18 originales | 14 |
| HU-15 | HU-15 | HU-19, HU-20, HU-21, HU-22, HU-25 originales (pre-renumeración) | 28 |
| HU-17 | HU-16 | HU-23, HU-24 originales (pre-renumeración; las HU-24 y HU-25 destino son historias independientes) | 12 |
| HU-18 | HU-17 | HU-26, HU-27, HU-28, HU-29, HU-30 originales | 29 |
| HU-19 | HU-18 | HU-31, HU-32 originales | 15 |
| **Total** | | **21 historias originales → 7 historias consolidadas** | **129** |

---

## 7. Matriz de Trazabilidad — Requisitos Funcionales → Historias

*Todos los IDs de Historia son IDs Destino (normalizados). Los RFs marcados como "(mod.)" cubren modificaciones o extensiones de una funcionalidad ya implementada en la historia primaria.*

| RF | HU | RF | HU | RF | HU |
|----|----|----|----|----|----|
| RF01 | HU-01 | RF22 | HU-06 | RF43 | HU-10 |
| RF02 | HU-01 | RF23 | HU-10 | RF44 | HU-15 |
| RF03 | HU-02 | RF24 | HU-10 | RF45 | HU-15 |
| RF04 | HU-03 | RF25 | HU-11 | RF46 | HU-15 |
| RF05 | HU-04 | RF26 | HU-11 | RF47 | HU-15 |
| RF06 | HU-04 | RF27 | HU-11 | RF48 | HU-15 |
| RF07 | HU-03 | RF28 | HU-10 | RF49 | HU-15 |
| RF08 | HU-04 | RF29 | HU-12 | RF50 | HU-17 |
| RF09 | HU-05 | RF30 | HU-12 | RF51 | HU-17 |
| RF10 | HU-05 | RF31 | HU-10, HU-11 | RF52 | HU-15 |
| RF11 | HU-05 | RF32 | HU-10 | RF53 | HU-18 |
| RF12 | HU-05 | RF33 | HU-10 | RF54 | HU-18 |
| RF13 | HU-06 | RF34 | HU-12 | RF55 | HU-18 |
| RF14 | HU-06 | RF35 | HU-12 | RF56 | HU-18 |
| RF15 | HU-06 | RF36 | HU-12 | RF57 | HU-18 |
| RF16 | HU-07 | RF37 | HU-12 | RF58 | HU-18 |
| RF17 | HU-06 | RF38 | HU-14 | RF59 | HU-13 |
| RF18 | HU-09 | RF39 | HU-14 | RF60 | HU-17 |
| RF19 | HU-09 | RF40 | HU-14 | RF61 | HU-03 |
| RF20 | HU-09 | RF41 | HU-14 | RF62 | HU-03 |
| RF21 | HU-09 | RF42 | HU-15 | RF63 | HU-04 |
| | | | | RF64 | HU-04 |
| | | | | RF65 | HU-26 |

**Resultado: 65/65 RF cubiertos → 0 huérfanos** ✅

---

## 8. Matriz de Trazabilidad — Requisitos No Funcionales → Historias

### 8.1. RNFs Específicos (asignados a historias concretas)

| RNF | Historia(s) (ID Destino) | Categoría |
|-----|--------------------------|-----------|
| RNF02 | HU-06, HU-22 | Usabilidad |
| RNF03 | HU-01, HU-06 | Usabilidad |
| RNF04 | HU-06 | Usabilidad |
| RNF05 | HU-12, HU-13, HU-18 | Usabilidad |
| RNF10 | HU-06, HU-09 | Confiabilidad |
| RNF12 | HU-01, HU-06, HU-08 | Confiabilidad |
| RNF13 | HU-05 | Confiabilidad |
| RNF15 | HU-19 | Persistencia |
| RNF16 | HU-19 | Persistencia |
| RNF17 | HU-19 | Persistencia |
| RNF18 | HU-19 | Persistencia |
| RNF24 | HU-03 | Compatibilidad |
| RNF26 | HU-19 | Seguridad |
| RNF27 | HU-19 | Seguridad |
| RNF31 | HU-21, HU-25 | Mantenibilidad |

**Resultado: 15 RNFs asignados a historias específicas** ✅

### 8.2. RNFs Transversales (aplicables a todas las historias)

Los 23 RNFs listados en §3 son restricciones de calidad del sistema completo. Se verifican implícitamente durante el refinamiento e implementación de cada historia.

**Resultado: 23 RNFs transversales declarados en §3** ✅

### 8.3. Totalización

| Tipo | Cantidad |
|------|----------|
| RNFs específicos (asignados a historias) | 15 |
| RNFs transversales (aplican a todo el sistema) | 23 |
| **Total cubierto** | **37/37 RNF → 0 huérfanos** ✅ |

---

## 9. Resumen de Cobertura

| Tipo | Total | Cubiertos | Huérfanos |
|------|-------|-----------|-----------|
| Requisitos Funcionales (RF) | 65 | 65 | 0 |
| Requisitos No Funcionales (RNF) | 37 | 37 | 0 |
| **Total Requisitos** | **102** | **102** | **0** |

| Métrica | Valor |
|---------|-------|
| Historias de Usuario | 26 (HU-01 a HU-26, IDs normalizados — sin IDs decimales) |
| Historias Done | 19 (HU-01 a HU-15, HU-22 a HU-26) |
| Historias Todo | 7 (HU-16 a HU-21, excepto HU-22) |
| RFs por historia (promedio) | 2.5 |
| RFs por historia (máximo) | 8 (HU-15 — Analítica y KPIs) |
| Historias sin RF (solo RNFs o correcciones) | 2 (HU-08, HU-19) |
| Historias sin RNF específicos | 11 (cubiertas por RNFs transversales) |

---

## 10. Cobertura de Requisitos por Historia (Tabla de Referencia)

*Tabla de referencia para auditoría. Sirve como fuente canónica de los RF/RNF que cada historia individual debe verificar. Los marcadores "(mod.)" y "(ext.)" indican que la historia modifica o extiende una funcionalidad previamente implementada.*

| ID Destino | RF Cubiertos | RNF Específicos |
|---|---|---|
| HU-01 | RF01, RF02 | RNF03, RNF12 |
| HU-02 | RF03 | — |
| HU-03 | RF04, RF07, RF61, RF62 | RNF24 |
| HU-04 | RF05, RF06, RF08, RF63, RF64 | — |
| HU-05 | RF09, RF10, RF11, RF12 | RNF13 |
| HU-06 | RF13, RF14, RF15, RF17, RF22 | RNF02, RNF03, RNF04, RNF10, RNF12 |
| HU-07 | RF16 | — |
| HU-08 | — | RNF12 |
| HU-09 | RF18, RF19, RF20, RF21 | RNF10 |
| HU-10 | RF23, RF24, RF28, RF31, RF32, RF33, RF43 | — |
| HU-11 | RF25, RF26, RF27, RF31 | — |
| HU-12 | RF29, RF30, RF34, RF35, RF36, RF37 | RNF05 |
| HU-13 | RF59 | RNF05 |
| HU-14 | RF38, RF39, RF40, RF41 | — |
| HU-15 | RF42, RF44, RF45, RF46, RF47, RF48, RF49, RF52 | — |
| HU-16 | RF04, RF05, RF08 (mod.) | RNF19 |
| HU-17 | RF50, RF51, RF60 | — |
| HU-18 | RF53, RF54, RF55, RF56, RF57, RF58 | RNF05 |
| HU-19 | — | RNF15, RNF16, RNF17, RNF18, RNF26, RNF27 |
| HU-20 | RF16 (corrección) | — |
| HU-21 | RF04, RF05, RF06 (mod.) | RNF31 |
| HU-22 | RF12, RF32 (ext.) | RNF02 |
| HU-23 | RF04, RF05, RF07, RF08, RF09, RF10, RF11, RF12, RF14, RF16, RF21, RF26, RF30, RF36, RF37, RF39, RF41, RF46, RF49, RF58, RF60, RF62, RF63, RF64 (mod.) | RNF09, RNF14, RNF19, RNF28, RNF29, RNF31 |
| HU-24 | RF04, RF05 (mod.) | — |
| HU-25 | RF13, RF22, RF25, RF28, RF29, RF31, RF33, RF35, RF38, RF54, RF55 (mod.) | RNF02, RNF31 |
| HU-26 | RF65 | — |
