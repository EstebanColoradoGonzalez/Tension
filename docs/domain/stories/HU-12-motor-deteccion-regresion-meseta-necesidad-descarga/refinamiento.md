## Refinamiento Técnico (Developer)
**Autor**: Esteban Colorado González | **Fecha**: 2026-02-12

---

### Consideraciones Generales

**Basado en análisis arquitectónico:**
HU-12 consolida 4 historias originales (HU-12, HU-14, HU-15, HU-16) que forman una cadena de decisión inseparable. Extensión del pipeline de cierre de sesión (HU-10/HU-11) + reglas puras en `domain/rules/` (ADR-06) + nueva capa de persistencia de alertas.

**Nivel de complejidad:**
ALTA — Esta historia implementa el motor de decisión completo: detección de regresión, fatiga acumulada, mesetas con análisis causal, acciones correctivas escalonadas y detección de necesidad de descarga por módulo. Requiere 4 reglas puras nuevas, infraestructura completa de alertas (AlertEntity + AlertDao), y modificación significativa del pipeline de cierre de sesión.

**Riesgos técnicos conocidos:**
1. Transacción de `closeSession()` ya compleja — agregar 6 pasos de evaluación post-loop sin romper atomicidad.
2. Guardia de descarga: `session.deload_id != null` debe omitir correctamente la detección de fatiga/deload durante descarga activa.
3. Deduplicación de alertas: verificar alertas activas antes de insertar PLATEAU y MODULE_REQUIRES_DELOAD.
4. Dos denominadores diferentes para el 50% (CA-12.05 vs CA-12.22) — riesgo de confusión.
5. Resolución automática de alertas debe sincronizarse con salida de `IN_PLATEAU` y umbral de descarga.

**Patrones y convenciones del equipo:**
- Código fuente en inglés, UI y datos de dominio en español (Arquitectura Técnica §5.1)
- Naming: `{Feature}Rule` para reglas puras, `{Entidad}Entity` para entities, `{Entidad}Dao` para DAOs
- Reglas puras: `object` Kotlin con funciones estáticas, sin dependencias Android
- Sealed classes para estados de progresión y tipos de alerta
- `operator fun invoke()` en Use Cases

**Dependencias nuevas a instalar:**
Ninguna — solo código existente (Room, KSP, Hilt, JUnit).

**Estrategia de testing:**
JUnit 4 (ADR-18) + MockK + kotlinx-coroutines-test | Tests unitarios para 4 reglas puras (domain) | Tests de integración para AlertDao | Cobertura: 100% reglas, guardia de descarga, deduplicación, resolución automática

---

### Historias Relacionadas Consultadas

**Implementaciones similares analizadas:**
HU-10 (evaluateProgression loop) — base de extensión para HU-12. HU-05 (ExerciseProgressionEntity) — estructura de estados de progresión.

**Patrones de código reutilizados:**
Loop `evaluateProgression()` de HU-10 como base. Patrón de reglas puras ADR-06. Patrón de alertas (infraestructura base).

**Mejores prácticas aplicadas:**
- Reglas puras sin dependencias para testabilidad sin emulador
- Separación write-time/read-time según Modelo de Datos §3.16
- Deduplicación y resolución automática de alertas
- Guardia de descarga para evitar falsos positivos durante descarga activa
- Mensajes genéricos en alertas, nombres resueltos por JOIN
