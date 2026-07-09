# Estimación — Historia #11: Prescribir carga objetivo según Regla de Doble Umbral

## Estimación por Seniority

| Seniority | Estimación | Descripción |
|-----------|-----------|-------------|
| Junior | 8-12 horas | Requiere guía en integración con `evaluateProgression()`, tests combinatorios, debug de JOIN |
| Mid | 4-6 horas | Implementación directa con patrón existente de HU-10, tests autónomos |
| Senior | 2-3 horas | Familiar con el código, conoce patrón `ProgressionClassificationRule`, integración trivial |

## Estimación por Fase

### Fase 1: Domain — Regla pura + tests

| Tarea | Junior | Mid | Senior |
|-------|--------|-----|--------|
| Crear `DoubleThresholdRule.kt` | 1h | 30min | 15min |
| Tests `meetsDoubleThreshold()` (7 escenarios) | 2h | 1h | 30min |
| Tests `prescribeLoad()` (4 escenarios) | 1h | 30min | 15min |
| **Subtotal** | **4h** | **2h** | **1h** |

### Fase 2: Data — DTO + query

| Tarea | Junior | Mid | Senior |
|-------|--------|-----|--------|
| Extender DTO + query con JOIN | 1h | 30min | 15min |
| Verificar query funciona | 1h | 15min | 10min |
| **Subtotal** | **2h** | **45min** | **25min** |

### Fase 3: Data — Integración

| Tarea | Junior | Mid | Senior |
|-------|--------|-----|--------|
| Extender `evaluateProgression()` | 2h | 1h | 30min |
| Integrar guard bodyweight/isométrico | 30min | 15min | 10min |
| Extender `copy()` con `prescribedLoadKg` | 30min | 15min | 10min |
| **Subtotal** | **3h** | **1h30min** | **50min** |

### Fase N: QA y Deployment

| Tarea | Estimación |
|-------|-----------|
| Peer Review | 30min |
| Resolver incidentes | 1-2h (condicional) |
| Crear PR | 15min |
| Tests manuales | 1h |
| **Subtotal** | **2-3h** |

## Total Estimado

| Seniority | Desarrollo | QA | Total |
|-----------|-----------|-----|-------|
| Junior | 9-12h | 2-3h | **11-15h** |
| Mid | 4-5h | 2-3h | **6-8h** |
| Senior | 2-2.5h | 2-3h | **4-5.5h** |

## Complejidad: BAJA

- 4 archivos tocados (2 nuevos + 2 modificados)
- Lógica predecible: 2 funciones puras + extensión de código existente
- Patrón establecido por HU-10 (mismo `object` singleton, mismo patrón de integración)
- Sin componentes UI
- Tests combinatorios pero de baja complejidad

## Riesgos que podrían aumentar la estimación

1. **Debug de JOIN a `module`:** Si hay problemas con el JOIN, podría tomar +1h para junior.
2. **Conflictos con HU-10:** Si el código de `evaluateProgression()` cambió desde HU-10, podría requerir resolución de conflictos.
3. **Tests edge cases:** Los boundaries (RIR exacto 2.0, sets exactamente 4) podrían requerir ajustes.
