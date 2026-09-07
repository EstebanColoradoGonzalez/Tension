---
story_number: 42
title: 'Mi 1RM estimado'
slug: '1rm-estimado'
estado: 'Borrador (PO)'
autor: 'Esteban Colorado González'
fecha_creacion: '2026-09-06'
es_resultado_slicing: true
historia_origen: 'HU-39 — Equipamiento por ejercicio, 1RM estimado y sesión editable (versión previa a la partición, 40 CAs)'
historias_hermanas: ['HU-39', 'HU-40', 'HU-41', 'HU-43']
orden_implementacion: '4 de 5 — depende solo de HU-39. Independiente de HU-40 y HU-41: puede paralelizarse tras HU-39 sin riesgo.'
slicing_justificacion: 'Score INVEST 4/6 de la historia original. Corte vertical de capacidad nueva y autocontenida: pantalla dedicada, entidad de persistencia propia y una regla de cálculo aislada. No toca el motor de decisión, ni el catálogo, ni el plan — la dependencia es unidireccional, lee del historial y nadie lee de ella, siguiendo el modelo de aislamiento que HU-37 estableció. Es la única hija paralelizable, y su valor propio es íntegro: una cifra de fuerza máxima por ejercicio e implemento que hoy no existe en ninguna forma.'
---

# Historia #42: Mi 1RM estimado

> **Hija 4 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/index.md), [`HU-40`](../HU-40-progresion-por-equipamiento/index.md), [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/index.md), [`HU-43`](../HU-43-ajustar-sesion-del-dia/index.md). **Depende de `HU-39`. Paralelizable.**

## Fases del Ciclo de Vida

| Fase                    | Estado        | Fecha      | Responsable                    |
| ----------------------- | ------------- | ---------- | ------------------------------ |
| Creación HU             | ✅ Completada | 2026-09-06 | Esteban Colorado González (PO) |
| Análisis Arquitectónico | ⏳ Pendiente  |            | Arquitecto                     |
| Refinamiento Técnico    | ⏳ Pendiente  |            | Developer                      |
| Estimación              | ⏳ Pendiente  |            | Developer                      |
| Desarrollo              | ⏳ Pendiente  |            | Developer                      |
| Revisión                | ⏳ Pendiente  |            | Revisor                        |

## Archivos de esta Historia

| Archivo           | Contenido                                  | Workflow                 |
| ----------------- | ------------------------------------------ | ------------------------ |
| `historia.md`     | Narrativa, ACs, info recopilada, UI detail | crear-historia-usuario   |
| `analisis-arq.md` | Decisiones arquitectónicas                 | analizar-disenar         |
| `refinamiento.md` | Tareas de implementación                   | refinamiento-tecnico     |
| `estimacion.md`   | Tabla de estimación por seniority          | estimar-historia-usuario |
| `dev-record.md`   | Progreso desarrollo, file list             | dev-rapido               |
| `cambios.md`      | Registro cronológico de cambios            | todos                    |

## Métricas de Tiempo

| Fase        | Inicio           | Fin |
| ----------- | ---------------- | --- |
| Creación HU | 2026-09-06 23:23 | 2026-09-07 00:49 |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-09-06
