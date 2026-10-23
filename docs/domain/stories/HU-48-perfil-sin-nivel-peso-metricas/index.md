---
story_number: 48
title: "Perfil sin nivel de experiencia y peso corporal en Métricas"
slug: "perfil-sin-nivel-peso-metricas"
estado: "Borrador (PO)"
autor: "esteban.colorado"
fecha_creacion: "2026-10-22"
es_resultado_slicing: true
historia_origen: "Historia original previa a la partición: «Eliminar el nivel de experiencia y usar el peso corporal en las métricas» (17 CAs, creada como HU-48 el 2026-10-22 y dividida en HU-48 y HU-49)"
historias_hermanas: ['HU-49']
orden_implementacion: "1 de 2 — independiente de HU-49; puede implementarse en cualquier orden."
slicing_justificacion: "Score INVEST 4/6 de la historia original (falla Small: 17 CAs en 3 dominios, complejidad Alta en pantallas, flujos, entidades y lógica). Corte vertical por frente de valor: esta hija retira el nivel de experiencia del perfil y lleva el peso corporal a Métricas con registro rápido e IMC sin categoría. Valor íntegro sin la carga real: el gráfico usa el tonelaje vigente del sistema."
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

# Historia #48: Perfil sin nivel de experiencia y peso corporal en Métricas

## Fases del Ciclo de Vida

| Fase | Estado | Fecha | Responsable |
|------|--------|-------|-------------|
| Creación HU | ✅ Completada | 2026-10-22 | esteban.colorado (PO) |
| Medición COSMIC | ⏳ Pendiente | | PO/Analista |
| Medición PNF | ⏳ Pendiente | | Developer |
| Verificación cruzada | ⏳ Pendiente | | Developer |
| Análisis Arquitectónico | ⏳ Pendiente | | Arquitecto |
| Refinamiento Técnico | ✅ Completada | 2026-10-23 | esteban.colorado |
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
| Desarrollo | 2026-10-23 14:35 | |

---

> **Método Ceiba IDE** | Usuario: esteban.colorado | Fecha: 2026-10-22
