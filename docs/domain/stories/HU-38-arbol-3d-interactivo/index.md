---
story_number: 38
title: 'El árbol en 3D'
slug: 'arbol-3d-interactivo'
estado: 'Borrador (PO)'
autor: 'Esteban Colorado González'
fecha_creacion: '2026-09-02'
es_resultado_slicing: true
historia_origen: 'HU-37 — Árbol de progreso del entrenamiento (versión previa a la partición, 16 CAs)'
historias_hermanas: ['HU-37']
orden_implementacion: '2 de 2 — depende de HU-37, que debe estar implementada antes'
slicing_justificacion: 'Score INVEST 4/6. Falla Small y presenta complejidad Alta en integraciones externas: WebView y Three.js sin ningún precedente en el proyecto. Esta hija aísla el riesgo técnico completo de la funcionalidad, de modo que su eventual complicación no bloquee la entrega del árbol, ya operativa con HU-37.'
---

# Historia #38: El árbol en 3D

> **Hija 2 de 2** de la partición de *Árbol de progreso del entrenamiento*. Hermana: [`HU-37`](../HU-37-arbol-progreso-entrenamiento/index.md). **Depende de HU-37**, que debe implementarse primero.

## Fases del Ciclo de Vida

| Fase                    | Estado        | Fecha      | Responsable                    |
| ----------------------- | ------------- | ---------- | ------------------------------ |
| Creación HU             | ✅ Completada | 2026-09-02 | Esteban Colorado González (PO) |
| Análisis Arquitectónico | ⏳ Pendiente  |            | Arquitecto                     |
| Refinamiento Técnico    | ⏳ Pendiente  |            | Developer                      |
| Estimación              | ⏳ Pendiente  |            | Developer                      |
| Desarrollo              | ⏳ Pendiente  |            | Developer                      |

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
| Creación HU | 2026-09-02 00:08 | 2026-09-02 00:33 |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-09-02
