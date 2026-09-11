---
story_number: 41
title: 'Catálogo anatómico y plan por defecto alineado'
slug: 'catalogo-anatomico-plan-alineado'
estado: 'Lista para Revisión'
autor: 'Esteban Colorado González'
fecha_creacion: '2026-09-06'
es_resultado_slicing: true
historia_origen: 'HU-39 — Equipamiento por ejercicio, 1RM estimado y sesión editable (versión previa a la partición, 40 CAs)'
historias_hermanas: ['HU-39', 'HU-40', 'HU-42', 'HU-43']
orden_implementacion: '3 de 5 — depende de HU-39, que debe estar implementada antes. Debe implementarse antes que HU-43, que reutiliza su formulario de ejercicio.'
slicing_justificacion: 'Score INVEST 4/6 de la historia original. Corte vertical que aísla el frente de catalogación: 33 zonas musculares con granularidad anatómica que reemplazan a las 20 actuales, jerarquía principal/secundaria, reclasificación completa de los 37 ejercicios y delta del plan por defecto con sus 35 sugerencias de equipamiento. Es un frente de datos y presentación, sin lógica de decisión, y replica el alcance ya entregado por HU-29 — lo que lo hace estimable y verificable por sí solo. Su valor propio: métricas por grupo muscular fieles y un plan que propone lo que El Ejecutante realmente entrena.'
---

# Historia #41: Catálogo anatómico y plan por defecto alineado

> **Hija 3 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/index.md), [`HU-40`](../HU-40-progresion-por-equipamiento/index.md), [`HU-42`](../HU-42-1rm-estimado/index.md), [`HU-43`](../HU-43-ajustar-sesion-del-dia/index.md). **Depende de `HU-39`.**

## Fases del Ciclo de Vida

| Fase                    | Estado        | Fecha      | Responsable                    |
| ----------------------- | ------------- | ---------- | ------------------------------ |
| Creación HU             | ✅ Completada | 2026-09-06 | Esteban Colorado González (PO) |
| Análisis Arquitectónico | ⏭️ Omitida    | 2026-09-11 | Ruta dev-rápido                 |
| Refinamiento Técnico    | ✅ Completada | 2026-09-11 | Esteban Colorado González (Dev) |
| Estimación              | ⏭️ Omitida    | 2026-09-11 | Ruta dev-rápido                 |
| Desarrollo              | ✅ Completada | 2026-09-11 | Esteban Colorado González (Dev) |
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
| Refinamiento Técnico | 2026-09-11 15:26 | 2026-09-11 15:52 |
| Desarrollo | 2026-09-11 15:53 | 2026-09-11 16:58 |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-09-06
