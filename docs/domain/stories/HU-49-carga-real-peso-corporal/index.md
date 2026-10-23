---
story_number: 49
title: "Carga real con peso corporal en tonelaje, 1RM y fuerza relativa"
slug: "carga-real-peso-corporal"
estado: "Borrador (PO)"
autor: "esteban.colorado"
fecha_creacion: "2026-10-22"
es_resultado_slicing: true
historia_origen: "Historia original previa a la partición: «Eliminar el nivel de experiencia y usar el peso corporal en las métricas» (17 CAs, creada como HU-48 el 2026-10-22 y dividida en HU-48 y HU-49)"
historias_hermanas: ['HU-48']
orden_implementacion: "2 de 2 — independiente de HU-48; puede implementarse en cualquier orden."
slicing_justificacion: "Score INVEST 4/6 de la historia original (falla Small). Corte vertical que aísla el riesgo técnico: carga real (porcentaje por ejercicio × peso vigente por fecha) en todas las vistas de tonelaje, la alerta de caída de tonelaje y el 1RM, más la fuerza relativa y el recálculo histórico al cambiar el porcentaje. Solo depende de HU-02, ya implementada. No toca el motor de decisión."
tracker_tool: "ninguna"
tracker_key:
tracker_status: "ninguna"
sprint: ""
cosmic_cfp:
cosmic_classification:
cosmic_confidence:
cosmic_status: "Pendiente"
cosmic_measured_at:
cosmic_source_hash:
cosmic_strategy_version:
cosmic_policy_version:
cosmic_engine_version:
cosmic_result_hash:
cosmic_complexity_ref:
cosmic_vigencia: "SIN_MEDICION"
pnf_total:
pnf_by_dimension:
pnf_status: "Pendiente"
pnf_measured_at:
pnf_source_hash:
pnf_rulebook_version:
pnf_engine_version:
pnf_result_hash:
pnf_vigencia: "SIN_MEDICION"
verificacion_status: "Pendiente"
verificacion_at:
pnf_cosmic_ref:
medicion_attempt_status:
medicion_attempt_at:
medicion_attempt_reason:
normalization_contract_hash:
dev_ia_session_minutes:
dev_manual_minutes:
dev_total_minutes:
dev_responsable:
dev_closed_at:
---

# Historia #49: Carga real con peso corporal en tonelaje, 1RM y fuerza relativa

## Fases del Ciclo de Vida

| Fase | Estado | Fecha | Responsable |
|------|--------|-------|-------------|
| Creación HU | ✅ Completada | 2026-10-22 | esteban.colorado (PO) |
| Medición COSMIC | ⏳ Pendiente | | PO/Analista |
| Medición PNF | ⏳ Pendiente | | Developer |
| Verificación cruzada | ⏳ Pendiente | | Developer |
| Análisis Arquitectónico | ⏳ Pendiente | | Arquitecto |
| Refinamiento Técnico | ⏳ Pendiente | | Developer |
| Estimación | ⏳ Pendiente | | Developer |
| Desarrollo | ⏳ Pendiente | | Developer |

## Archivos de esta Historia

| Archivo | Contenido | Workflow |
|---------|-----------|----------|
| `historia.md` | Narrativa, ACs, info recopilada, UI detail | crear-historia-usuario |
| `qa.md` | Casos de prueba y datos de prueba | crear-historia-usuario |
| `especificacion.md` | Consideraciones técnicas extra para el desarrollador (opcional) | crear-historia-usuario |
| `medicion-cosmic.md` | Tamaño funcional (CFP COSMIC) | medir-historia-cosmic |
| `medicion-pnf.md` | Tamaño no funcional (PNF, incl. PNF-5 Mantenibilidad) | medir-historia-pnf |
| `verificacion.json` | Verificación cruzada CFP↔PNF (anti-doble-conteo) | medir-historia |
| `analisis-arq.md` | Decisiones arquitectónicas | analizar-disenar |
| `refinamiento.md` | Tareas de implementación | refinamiento-tecnico |
| `estimacion.md` | Tabla de estimación por seniority | estimar-historia-usuario |
| `dev-record.md` | Progreso desarrollo, file list | dev-rapido |
| `cambios.md` | Registro cronológico de cambios | todos |

## Métricas de Tiempo

| Fase        | Inicio                    | Fin |
| ----------- | ------------------------- | --- |
| Creación HU | 2026-10-22 18:47 | 2026-10-22 20:59 |

---

> **Método Ceiba IDE** | Usuario: esteban.colorado | Fecha: 2026-10-22
