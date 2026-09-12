---
story_number: 43
title: 'Ajustar la sesión del día'
slug: 'ajustar-sesion-del-dia'
estado: 'Lista para Revisión'
autor: 'Esteban Colorado González'
fecha_creacion: '2026-09-06'
es_resultado_slicing: true
historia_origen: 'HU-39 — Equipamiento por ejercicio, 1RM estimado y sesión editable (versión previa a la partición, 40 CAs)'
historias_hermanas: ['HU-39', 'HU-40', 'HU-41', 'HU-42']
orden_implementacion: '5 de 5 — depende de HU-39 y de HU-41, porque reutiliza el formulario de creación de ejercicio con equipamiento múltiple y jerarquía de zonas. Es la única hija prescindible sin dejar el sistema en un estado inconsistente.'
slicing_justificacion: 'Score INVEST 4/6 de la historia original. Corte vertical de un frente completamente ajeno al equipamiento: la sesión activa pasa a admitir añadir y retirar ejercicios de forma temporal, con presupuesto e invariante propios. No toca el motor de decisión, ni el catálogo, ni el plan, ni el 1RM. Depende de HU-39 y HU-41 solo por reutilizar su formulario de creación de ejercicio, no por su lógica. Su valor propio es íntegro y su aislamiento la hace la única hija prescindible si hubiera presión de tiempo.'
---

# Historia #43: Ajustar la sesión del día

> **Hija 5 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/index.md), [`HU-40`](../HU-40-progresion-por-equipamiento/index.md), [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/index.md), [`HU-42`](../HU-42-1rm-estimado/index.md). **Depende de `HU-39` y `HU-41`.**

## Fases del Ciclo de Vida

| Fase                    | Estado        | Fecha      | Responsable                    |
| ----------------------- | ------------- | ---------- | ------------------------------ |
| Creación HU             | ✅ Completada | 2026-09-06 | Esteban Colorado González (PO) |
| Análisis Arquitectónico | ✅ Absorbida por dev-rápido | 2026-09-11 | Esteban Colorado González (Developer) |
| Refinamiento Técnico    | ✅ Completada | 2026-09-11 | Esteban Colorado González (Developer) |
| Estimación              | ➖ No aplica (dev-rápido) |            | Developer                      |
| Desarrollo              | ✅ Completada | 2026-09-11 | Esteban Colorado González (Developer) |
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
| Refinamiento Técnico | 2026-09-11 20:47 | 2026-09-11 21:05 |
| Desarrollo | 2026-09-11 21:05 | 2026-09-11 21:26 |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-09-06
