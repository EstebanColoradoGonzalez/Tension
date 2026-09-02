---
story_number: 37
title: 'El árbol de mi entrenamiento'
slug: 'arbol-progreso-entrenamiento'
estado: 'Lista para Revisión'
autor: 'Esteban Colorado González'
fecha_creacion: '2026-09-02'
es_resultado_slicing: true
historia_origen: 'HU-37 — Árbol de progreso del entrenamiento (versión previa a la partición, 16 CAs)'
historias_hermanas: ['HU-38']
orden_implementacion: '1 de 2 — debe implementarse antes que HU-38'
slicing_justificacion: 'Score INVEST 4/6. Falla Small (16 CAs, entidad nueva, migración de esquema, cambio de formato de respaldo, pantalla nueva, asset web con Three.js, fallback nativo y suite de pruebas). Complejidad Alta en lógica e integraciones externas. Corte vertical: esta hija entrega el árbol completo con representación nativa —valor íntegro, cero tecnología nueva— y concentra toda la infraestructura (persistencia, cálculo, navegación, respaldo). HU-38 aísla el riesgo del primer WebView y de Three.js.'
---

# Historia #37: El árbol de mi entrenamiento

> **Hija 1 de 2** de la partición de *Árbol de progreso del entrenamiento*. Hermana: [`HU-38`](../HU-38-arbol-3d-interactivo/index.md). Se implementa **primero**.

## Fases del Ciclo de Vida

| Fase                    | Estado        | Fecha      | Responsable                    |
| ----------------------- | ------------- | ---------- | ------------------------------ |
| Creación HU             | ✅ Completada | 2026-09-02 | Esteban Colorado González (PO) |
| Análisis Arquitectónico | ⏳ Pendiente  |            | Arquitecto                     |
| Refinamiento Técnico    | ✅ Completada | 2026-09-02 | Esteban Colorado González (Dev) |
| Estimación              | ⏳ Pendiente  |            | Developer                      |
| Desarrollo              | ✅ Completada (Dev-Rápido) | 2026-09-02 | Esteban Colorado González (Dev) |
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
| Creación HU | 2026-09-02 00:08 | 2026-09-02 00:33 |
| Refinamiento Técnico | 2026-09-02 00:40 | 2026-09-02 01:05 |
| Desarrollo | 2026-09-02 01:05 | 2026-09-02 02:10 |

---

> **Método Ceiba IDE** | Usuario: Esteban Colorado González | Fecha: 2026-09-02
