---
story_number: 40
title: 'Cambiar de implemento no es retroceder'
slug: 'progresion-por-equipamiento'
estado: 'Lista para Revisión'
autor: 'Esteban Colorado González'
fecha_creacion: '2026-09-06'
es_resultado_slicing: true
historia_origen: 'HU-39 — Equipamiento por ejercicio, 1RM estimado y sesión editable (versión previa a la partición, 40 CAs)'
historias_hermanas: ['HU-39', 'HU-41', 'HU-42', 'HU-43']
orden_implementacion: '2 de 5 — depende de HU-39, que debe estar implementada antes. Orden crítico: no debe postergarse, porque es donde se paga el valor central de la historia original.'
slicing_justificacion: 'Score INVEST 4/6 de la historia original. Corte vertical que aísla el cambio de la unidad de comparación de todo el motor de decisión —clasificación, Doble Umbral, meseta, descarga, memoria del último peso y KPIs comparativos— más la consolidación booleana por ejercicio. Entrega el valor central de la historia original: la eliminación del falso REGRESSION por cambio de implemento. Se separa de HU-39 porque esta reescribe reglas de decisión ya implementadas y probadas, mientras HU-39 solo añade una dimensión al registro: son dos riesgos de naturaleza distinta.'
---

# Historia #40: Cambiar de implemento no es retroceder

> **Hija 2 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/index.md), [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/index.md), [`HU-42`](../HU-42-1rm-estimado/index.md), [`HU-43`](../HU-43-ajustar-sesion-del-dia/index.md). **Depende de `HU-39`.**

## Fases del Ciclo de Vida

| Fase                    | Estado        | Fecha      | Responsable                    |
| ----------------------- | ------------- | ---------- | ------------------------------ |
| Creación HU             | ✅ Completada | 2026-09-06 | Esteban Colorado González (PO) |
| Análisis Arquitectónico | ⏳ Pendiente  |            | Arquitecto                     |
| Refinamiento Técnico    | ✅ Completada | 2026-09-07 | Esteban Colorado González (Developer) |
| Estimación              | ⏳ Pendiente  |            | Developer                      |
| Desarrollo              | ✅ Completada | 2026-09-07 | Esteban Colorado González (Developer) |
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
| Refinamiento Técnico | 2026-09-07 16:31 | 2026-09-07 16:58 |
| Desarrollo | 2026-09-07 16:31 | 2026-09-07 17:41 |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-09-06
