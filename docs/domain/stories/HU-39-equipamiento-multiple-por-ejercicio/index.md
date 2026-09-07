---
story_number: 39
title: 'Un solo ejercicio, varios implementos'
slug: 'equipamiento-multiple-por-ejercicio'
estado: 'Lista para Revisión'
autor: 'Esteban Colorado González'
fecha_creacion: '2026-09-06'
es_resultado_slicing: true
historia_origen: 'HU-39 — Equipamiento por ejercicio, 1RM estimado y sesión editable (versión previa a la partición, 40 CAs)'
historias_hermanas: ['HU-40', 'HU-41', 'HU-42', 'HU-43']
orden_implementacion: '1 de 5 — debe implementarse antes que HU-40, HU-41, HU-42 y HU-43'
slicing_justificacion: 'Score INVEST 4/6. Falla Negotiable y Small (40 CAs en 8 escenarios, cinco frentes con tablas cerradas, 11 puntos de interfaz, pantalla nueva, cambio de esquema, cambio de formato de respaldo y reescritura de la unidad de comparación del motor de decisión). Complejidad Alta en cuatro de las cinco dimensiones. Corte vertical: esta hija concentra toda la infraestructura —catálogo atómico, relación N:M ejercicio-equipamiento, equipamiento en la serie, formularios, esquema y respaldo— y entrega valor propio verificable sin sus hermanas: el registro y el historial dejan de ser ambiguos sobre el implemento y el catálogo deja de necesitar un ejercicio por combinación.'
---

# Historia #39: Un solo ejercicio, varios implementos

> **Hija 1 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-40`](../HU-40-progresion-por-equipamiento/index.md), [`HU-41`](../HU-41-catalogo-anatomico-plan-alineado/index.md), [`HU-42`](../HU-42-1rm-estimado/index.md), [`HU-43`](../HU-43-ajustar-sesion-del-dia/index.md). Se implementa **primero**.

## Fases del Ciclo de Vida

| Fase                    | Estado        | Fecha      | Responsable                    |
| ----------------------- | ------------- | ---------- | ------------------------------ |
| Creación HU             | ✅ Completada | 2026-09-06 | Esteban Colorado González (PO) |
| Análisis Arquitectónico | ⏳ Pendiente  |            | Arquitecto                     |
| Refinamiento Técnico    | ✅ Completada | 2026-09-07 | Esteban Colorado González (Dev) |
| Estimación              | ⏳ Pendiente  |            | Developer                      |
| Desarrollo              | ✅ Completada | 2026-09-07 | Esteban Colorado González (Dev) |
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
| Refinamiento | 2026-09-07 00:58 | 2026-09-07 01:02 |
| Desarrollo | 2026-09-07 01:02 | 2026-09-07 01:57 |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-09-06
