# Registro de Cambios — Historia #35

| Fecha | Fase | Descripción | Autor |
|-------|------|-------------|-------|
| 2026-08-30 | Creación | Historia creada | Esteban Colorado González (PO) |
| 2026-08-30 | Creación | Preview de interfaz generado en formato ASCII: `35.preview.txt` | Esteban Colorado González (PO) |
| 2026-08-30 | Creación | Mapeada en `story_mapping_index.md` §2 bajo EPIC-08 y Release 1.4 | Esteban Colorado González (PO) |
| 2026-08-30 | Creación | **Fase Creación HU cerrada** (2026-08-30 21:34). Estado: Borrador (PO) — lista para análisis arquitectónico. | Esteban Colorado González (PO) |
| 2026-08-31 | Refinamiento | Plan técnico generado: 20 tareas en 9 fases, 8 decisiones. Hallazgo raíz: las siete reglas de cálculo de HU-15 devuelven `0.0` ante datos insuficientes | Esteban Colorado González (Developer) |
| 2026-08-31 | Refinamiento | **Fase Refinamiento Técnico cerrada.** Plan aprobado por el PO sin ajustes | Esteban Colorado González (Developer) |
| 2026-08-31 | Desarrollo | Regla de presentación pura creada: `MetricValue` de tres estados y `MetricSufficiencyRules` con los umbrales transcritos de cada regla de cálculo | Esteban Colorado González (Developer) |
| 2026-08-31 | Desarrollo | Anatomía de tarjeta compartida (`MetricCard.kt`) y colores de gráfica y dato ausente añadidos al tema | Esteban Colorado González (Developer) |
| 2026-08-31 | Desarrollo | Evidencia de suficiencia propagada en tres modelos de dominio sin tocar ningún cálculo | Esteban Colorado González (Developer) |
| 2026-08-31 | Desarrollo | Métricas, Volumen y Tendencia recompuestas en secciones de tarjetas; período elevado al `UiState`; once colores literales sustituidos por los del tema | Esteban Colorado González (Developer) |
| 2026-08-31 | Desarrollo | Gráfica de tonelaje rotulada con unidad, significado de ejes y etiquetas `mcN`; paleta con variante clara y oscura | Esteban Colorado González (Developer) |
| 2026-08-31 | Desarrollo | 41 tests nuevos. Suite completa **579/579 verde**; ninguna regla de cálculo ni su test requirió ajuste (CA-35.05) | Esteban Colorado González (Developer) |
| 2026-08-31 | Desarrollo | `interfaces_contract.md` Flujo G reescrito con la estructura de presentación de los indicadores (CA-35.08) | Esteban Colorado González (Developer) |
| 2026-08-31 | Desarrollo | **Fase Desarrollo cerrada.** Build completo verde, Lint sin errores, sin cambio de esquema | Esteban Colorado González (Developer) |
| 2026-08-31 | Dev-Rápido | ⚡ Implementado: cada indicador de Métricas, Volumen y Tendencia se presenta con etiqueta, valor dominante, unidad, descripción y período, agrupado en secciones; el dato ausente se declara en lugar de mostrarse como cero, distinguible del cero calculado. Sin cambios de cálculo ni de esquema. Estado: **Lista para Revisión** | Esteban Colorado González |
