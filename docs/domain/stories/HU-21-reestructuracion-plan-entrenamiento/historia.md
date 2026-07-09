# Historia #21: Reestructuración del plan de entrenamiento y orden sugerido de ejecución

## §1 Metadatos

| Campo | Valor |
|---|---|
| **ID Canónico** | HU-21 |
| **ID Legado** | HU-20 |
| **Título** | Reestructuración del plan de entrenamiento y orden sugerido de ejecución |
| **Requisitos** | RF04, RF05, RF06 (modificación de seed data y comportamiento de presentación) |
| **Pantallas** | D4 (PlanVersionDetailScreen), E1 (ActiveSessionScreen), E5 (SessionSummaryScreen), F2 (SessionDetailScreen) |
| **Estado** | Desarrollado |
| **Dependencias** | HU-16 (precedente de migración Room MIGRATION_6_7), HU-04, HU-05, HU-20 |
| **Dependientes** | HU-22 (Preview de Sesión — CA-22.04 y CA-22.05 asumen HU-21 completada) |
| **Migración DB** | Sí — MIGRATION_7_8: ALTER TABLE plan_assignment ADD COLUMN sort_order + DELETE + 82 INSERTs. DB versión 7 → 8 |
| **Última actualización** | 2026-03-04 |

---

## §2 Narrativa de Negocio

Como ejecutante, necesito que el plan de entrenamiento se reestructure para reflejar la realidad operativa y fisiológica de mis sesiones — reduciendo el volumen excesivo en módulos donde la fatiga acumulada limita la calidad del estímulo, fortaleciendo la cobertura de espalda en la versión 1 del Módulo A, flexibilizando la ejecución de abdomen para que pueda realizarse fuera del gimnasio, y estableciendo un orden sugerido de ejecución por módulo basado en priorización biomecánica — para que cada sesión sea ejecutable en tiempo y energía reales, maximice el estímulo efectivo por grupo muscular, y me guíe hacia la secuencia óptima de ejercicios sin restringir mi libertad de adaptación a las circunstancias del gimnasio.

### Problemas identificados con el plan actual

#### Módulo A — Déficit de espalda en V1 y rigidez de abdomen

La Versión 1 del Módulo A prescribe solo 3 ejercicios de espalda, mientras que V2 y V3 prescriben 4 (incluyen Elevación de hombros con mancuernas). Con la nueva concepción de que los ejercicios de abdomen pueden ejecutarse fuera del gimnasio, **espalda y bíceps son los únicos grupos obligatorios en el gym para el Módulo A**. Que V1 tenga solo 3 ejercicios de espalda deja la parte obligatoria de gym insuficiente en volumen. La solución es agregar "Elevación de hombros con mancuernas" también a V1, haciéndolo transversal a las 3 versiones.

Todos los ejercicios de abdomen (Abdominales, Escalador, Giro Ruso, Plancha, Plancha Lateral) son de tipo **Cuerpo** — no requieren ningún equipamiento de gimnasio. El ejecutante necesita la flexibilidad de ejecutarlos en su hogar u otro espacio antes de ir al gym, completando la sesión completa sin dividirla en sub-sesiones.

#### Módulo B — Volumen excesivo para un natural

El Módulo B actual prescribe 11 ejercicios por versión (4 Pecho + 4 Hombro + 3 Tríceps). Esto es excesivo por tres razones científicas:

1. **Fatiga acumulada del SNC:** Los movimientos de empuje comparten sinergistas (deltoides anterior, tríceps). Después de 3-4 ejercicios compuestos de pecho, los deltoides y tríceps ya están significativamente pre-fatigados.
2. **Rendimientos decrecientes:** La literatura (Schoenfeld et al., 2017) indica que pasado cierto umbral de volumen por sesión, cada serie adicional aporta menos hipertrofia y más fatiga sistémica. Para un atleta natural, 8 ejercicios en un push day es el punto óptimo.
3. **Viabilidad operativa:** Disponibilidad de máquinas, tiempo total de sesión (<90 min) y fatiga percibida hacen que completar 11 ejercicios con calidad sea impráctico.

La reducción a **3 Pecho + 3 Hombro + 2 Tríceps = 8 ejercicios** mantiene la cobertura de zonas musculares y reduce el tríceps a 2 ejercicios directos dado que ya recibe estímulo indirecto significativo en todos los presses de pecho y hombro.

#### Módulo C — Un ejercicio por encima del umbral práctico

El Módulo C prescribe 9 ejercicios por versión. Con 6 grupos musculares a cubrir (Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores, Gemelos), 8 ejercicios son suficientes para garantizar cobertura completa aprovechando los ejercicios multiarticulares que trabajan múltiples zonas simultáneamente.

#### Ausencia de orden sugerido

El sistema actual no sugiere ningún orden de ejecución. Si bien la libertad de orden es un principio rector del sistema (Restricción de Integridad §6.B.11 del MDS), la ausencia total de orientación puede llevar a secuencias subóptimas — por ejemplo, ejecutar bíceps antes que espalda, lo cual pre-fatiga los flexores del codo y limita la carga en los compuestos de tracción.

### Diferencia clave con HU-16

HU-16 fue una migración de la estructura de módulos (reasignación de `module_code` de ejercicios). HU-21 **no modifica la pertenencia de ejercicios a módulos** — los ejercicios siguen en sus módulos actuales. Lo que cambia es la **asignación de ejercicios a versiones del plan** (tabla `plan_assignment`): cuáles ejercicios se incluyen en cada versión, en qué orden, y cuántos.

---

## §3 Criterios de Aceptación (BDD)

### Bloque A — Módulo A: Fortalecimiento de espalda y flexibilización de abdomen

#### CA-21.01 — V1 incorpora Elevación de hombros con mancuernas (4 ejercicios de espalda)

**Dado que** la Versión 1 del Módulo A actualmente prescribe solo 3 ejercicios de espalda,
**y** no existen otros ejercicios de espalda en el Diccionario además de los 4 ya catalogados,
**cuando** se aplica la reestructuración del plan,
**entonces** la Versión 1 incorpora "Elevación de hombros con mancuernas" como 4° ejercicio de espalda, igualando a V2 y V3, y el ejercicio pasa a ser fijo transversal a las 3 versiones del Módulo A.

#### CA-21.02 — Composición definitiva del Módulo A — Versión 1 (12 ejercicios)

**Dado que** la V1 del Módulo A se reestructura,
**cuando** se consulta el plan actualizado,
**entonces** la V1 contiene exactamente los siguientes 12 ejercicios en este orden:

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Dorsal Ancho | Tiro de dorsales (Agarre ancho) | Maquina | 4 | 8-12 |
| 2 | Espalda Media | Remo con Inclinación | Barra de Pesas | 4 | 8-12 |
| 3 | Dorsal Ancho | Remo con un solo brazo doblado | Mancuerna | 4 | 8-12 |
| 4 | Espalda Media | Elevación de hombros con mancuernas | Mancuerna | 4 | 8-12 |
| 5 | Biceps | Curl de biceps | Mancuerna | 4 | 8-12 |
| 6 | Biceps | Curl de martillo cruzado | Mancuerna | 4 | 8-12 |
| 7 | Biceps | Curl de biceps | Polea | 4 | 8-12 |
| 8 | Biceps | Curl de martillo | Mancuerna | 4 | 8-12 |
| 9 | Abdomen | Abdominales | Cuerpo | 4 | 8-12 |
| 10 | Abdomen | Escalador | Cuerpo | 4 | 8-12 |
| 11 | Abdomen | Giro Ruso | Cuerpo | 4 | 8-12 |
| 12 | Abdomen | Plancha | Cuerpo | 4 | 30-45 seg |

**Cambios respecto a la V1 anterior:** Se agrega Elevación de hombros con mancuernas en la posición 4. Los demás ejercicios se desplazan una posición. La V1 pasa de 11 a 12 ejercicios.

#### CA-21.03 — Composición definitiva del Módulo A — Versión 2 (11 ejercicios, sin cambios de contenido)

**Dado que** la V2 ya contiene 4 ejercicios de espalda,
**cuando** se consulta el plan actualizado,
**entonces** la V2 contiene exactamente los siguientes 11 ejercicios en este orden (mismo contenido, orden ajustado a Espalda → Bíceps → Abdomen):

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Dorsal Ancho | Tiro de dorsales (Agarre ancho) | Maquina | 4 | 8-12 |
| 2 | Espalda Media | Remo con Inclinación | Barra de Pesas | 4 | 8-12 |
| 3 | Dorsal Ancho | Remo con un solo brazo doblado | Mancuerna | 4 | 8-12 |
| 4 | Espalda Media | Elevación de hombros con mancuernas | Mancuerna | 4 | 8-12 |
| 5 | Biceps | Curl de biceps | Mancuerna | 4 | 8-12 |
| 6 | Biceps | Curl de martillo cruzado | Mancuerna | 4 | 8-12 |
| 7 | Biceps | Curl de biceps | Polea | 4 | 8-12 |
| 8 | Biceps | Curl de Contracción | Mancuerna | 4 | 8-12 |
| 9 | Abdomen | Abdominales | Cuerpo | 4 | 8-12 |
| 10 | Abdomen | Plancha | Cuerpo | 4 | 30-45 seg |
| 11 | Abdomen | Plancha Lateral | Cuerpo | 4 | 30-45 seg |

**Cambios respecto a la V2 anterior:** Ningún cambio de composición. Solo se verifica que el orden sigue la secuencia Espalda → Bíceps → Abdomen (ya la cumplía).

#### CA-21.04 — Composición definitiva del Módulo A — Versión 3 (11 ejercicios, sin cambios de contenido)

**Dado que** la V3 ya contiene 4 ejercicios de espalda,
**cuando** se consulta el plan actualizado,
**entonces** la V3 contiene exactamente los siguientes 11 ejercicios en este orden:

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Dorsal Ancho | Tiro de dorsales (Agarre ancho) | Maquina | 4 | 8-12 |
| 2 | Espalda Media | Remo con Inclinación | Barra de Pesas | 4 | 8-12 |
| 3 | Dorsal Ancho | Remo con un solo brazo doblado | Mancuerna | 4 | 8-12 |
| 4 | Espalda Media | Elevación de hombros con mancuernas | Mancuerna | 4 | 8-12 |
| 5 | Biceps | Curl de biceps | Mancuerna | 4 | 8-12 |
| 6 | Biceps | Curl de martillo | Mancuerna | 4 | 8-12 |
| 7 | Biceps | Curl de biceps | Polea | 4 | 8-12 |
| 8 | Biceps | Curl de Contracción | Mancuerna | 4 | 8-12 |
| 9 | Abdomen | Abdominales | Cuerpo | 4 | 8-12 |
| 10 | Abdomen | Giro Ruso | Cuerpo | 4 | 8-12 |
| 11 | Abdomen | Plancha | Cuerpo | 4 | 30-45 seg |

**Cambios respecto a la V3 anterior:** Ningún cambio de composición ni de orden (ya cumplía la secuencia Espalda → Bíceps → Abdomen).

#### CA-21.05 — Abdomen clasificado como grupo muscular ejecutable fuera del gimnasio

**Dado que** todos los ejercicios de abdomen del Módulo A (Abdominales, Escalador, Giro Ruso, Plancha, Plancha Lateral) son de tipo "Cuerpo" y no requieren equipamiento de gimnasio,
**cuando** el ejecutante consulta el plan o se encuentra en una sesión activa del Módulo A,
**entonces** los ejercicios de abdomen se presentan con una indicación visual clara de que son **ejecutables fuera del gimnasio**, diferenciándolos de los ejercicios de espalda y bíceps que requieren equipamiento. La sesión no se divide en sub-sesiones: sigue siendo una sola unidad, pero el ejecutante puede ejecutar la porción de abdomen en una ubicación diferente (hogar, parque, u otro espacio) antes o después de la porción de gym.

#### CA-21.06 — Distribución actualizada del Módulo A

**Dado que** se aplica la reestructuración,
**cuando** se evalúa la distribución de ejercicios del Módulo A,
**entonces:**
- **Espalda:** 4 ejercicios fijos transversales a las 3 versiones (Tiro de dorsales, Remo con Inclinación, Remo con un solo brazo doblado, Elevación de hombros con mancuernas).
- **Bíceps:** 2 fijos (Curl de bíceps Mancuerna, Curl de bíceps Polea) + 3 rotativos (Curl de martillo cruzado, Curl de martillo, Curl de Contracción) de los cuales se seleccionan 2 por versión = 4 bíceps por versión.
- **Abdomen:** 2 fijos (Abdominales, Plancha) + 3 rotativos (Escalador, Giro Ruso, Plancha Lateral) de los cuales se seleccionan 2 en V1 y 1 en V2/V3 = 4 abdomen en V1, 3 en V2/V3.
- **Totales por versión:** V1 = 12, V2 = 11, V3 = 11.

### Bloque B — Módulo B: Reducción a 8 ejercicios por versión (3P + 3H + 2T)

#### CA-21.07 — Estructura 3 Pecho + 3 Hombro + 2 Tríceps

**Dado que** el Módulo B actual prescribe 11 ejercicios por versión generando fatiga excesiva e inviabilidad operativa,
**cuando** se aplica la reestructuración,
**entonces** cada versión del Módulo B contiene exactamente **8 ejercicios**: 3 de Pecho, 3 de Hombro y 2 de Tríceps. La reducción de tríceps de 3 a 2 ejercicios directos se justifica fisiológicamente porque el tríceps ya recibe estímulo indirecto significativo como sinergista en todos los presses de pecho y hombro.

#### CA-21.08 — Composición definitiva del Módulo B — Versión 1 (8 ejercicios)

**Dado que** la V1 del Módulo B se reestructura,
**cuando** se consulta el plan actualizado,
**entonces** la V1 contiene exactamente los siguientes 8 ejercicios en este orden:

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Pecho Medio | Press de banca | Maquina | 4 | 8-12 |
| 2 | Pecho Superior | Press de banca inclinada | Maquina | 4 | 8-12 |
| 3 | Pecho Medio | Apertura de pecho sentado | Maquina | 4 | 8-12 |
| 4 | Hombro | Press de elevación sentado | Mancuerna | 4 | 8-12 |
| 5 | Hombro | Elevación lateral | Mancuerna | 4 | 8-12 |
| 6 | Hombro | Elevacion frontal | Mancuerna | 4 | 8-12 |
| 7 | Triceps | Extensión de triceps por encima de la cabeza | Mancuerna | 4 | 8-12 |
| 8 | Triceps | Flexión de triceps con cuerda | Maquina | 4 | 8-12 |

**Justificación de selección V1:** Pecho orientado a máquinas con cobertura de zona media + superior. Hombro con los 3 movimientos fundamentales de deltoides (press compuesto, elevación lateral para medial, frontal para anterior). Tríceps con overhead extension (cabeza larga en estiramiento) + cable pushdown (cabeza lateral).

**Ejercicios removidos de la V1 anterior:** Flexiones (Pecho Inferior), Remo vertical (Hombro), Dominada de triceps banco (Tríceps).

#### CA-21.09 — Composición definitiva del Módulo B — Versión 2 (8 ejercicios)

**Dado que** la V2 del Módulo B se reestructura,
**cuando** se consulta el plan actualizado,
**entonces** la V2 contiene exactamente los siguientes 8 ejercicios en este orden:

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Pecho Medio | Press de banca | Maquina | 4 | 8-12 |
| 2 | Pecho Superior | Apertura de pecho inclinado | Maquina | 4 | 8-12 |
| 3 | Pecho Inferior | Cruce en polea alta | Maquina | 4 | 8-12 |
| 4 | Hombro | Press de elevación sentado | Mancuerna | 4 | 8-12 |
| 5 | Hombro | Elevación lateral | Mancuerna | 4 | 8-12 |
| 6 | Hombro | Remo vertical con cable | Maquina | 4 | 8-12 |
| 7 | Triceps | Dominada de triceps banco | Pesa | 4 | 8-12 |
| 8 | Triceps | Flexión de triceps con cuerda | Maquina | 4 | 8-12 |

**Justificación de selección V2:** Pecho con máxima variedad de zonas (medio + superior + inferior) usando máquinas/cables. Hombro con press compuesto + elevación lateral (medial) + remo vertical con cable (variante del V1). Tríceps con bench dip + cable pushdown (pairing diferente al V1).

**Ejercicios removidos de la V2 anterior:** Apertura de pecho sentado (Pecho Medio), Press de mancuerna (Pecho Medio), Extensión de triceps por encima de la cabeza (Tríceps).

#### CA-21.10 — Composición definitiva del Módulo B — Versión 3 (8 ejercicios)

**Dado que** la V3 del Módulo B se reestructura,
**cuando** se consulta el plan actualizado,
**entonces** la V3 contiene exactamente los siguientes 8 ejercicios en este orden:

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Pecho Medio | Press de banca | Maquina | 4 | 8-12 |
| 2 | Pecho Medio | Press de mancuerna | Mancuernas | 4 | 8-12 |
| 3 | Pecho Inferior | Flexiones | Cuerpo | 4 | Al fallo técnico |
| 4 | Hombro | Press de elevación sentado | Mancuerna | 4 | 8-12 |
| 5 | Hombro | Elevacion frontal | Mancuerna | 4 | 8-12 |
| 6 | Hombro | Remo vertical | Barra de Pesas | 4 | 8-12 |
| 7 | Triceps | Dominada de triceps banco | Pesa | 4 | 8-12 |
| 8 | Triceps | Extensión de triceps por encima de la cabeza | Mancuerna | 4 | 8-12 |

**Justificación de selección V3:** Pecho con variedad de implementos (máquina + mancuerna + cuerpo) trabajando zona media + inferior. Hombro con press compuesto + frontal (anterior) + remo vertical con barra (variante del V2). Tríceps con bench dip (diferente del V1) + overhead extension (rotado del V1).

**Ejercicios removidos de la V3 anterior:** Press de banca inclinada (Pecho Superior), Apertura de pecho sentado (Pecho Medio), Cruce en polea alta (Pecho Inferior), Flexión de triceps con cuerda (Tríceps).

#### CA-21.11 — Distribución actualizada del Módulo B y cobertura completa del Diccionario

**Dado que** se aplica la reestructuración del Módulo B,
**cuando** se evalúa la distribución de los 15 ejercicios del Diccionario del Módulo B a través de las 3 versiones,
**entonces:**

**Pecho (7 del Diccionario → 3 por versión):**
| Ejercicio | V1 | V2 | V3 |
|-----------|----|----|-----|
| Press de banca | ✅ | ✅ | ✅ |
| Press de banca inclinada | ✅ | — | — |
| Apertura de pecho sentado | ✅ | — | — |
| Apertura de pecho inclinado | — | ✅ | — |
| Cruce en polea alta | — | ✅ | — |
| Press de mancuerna | — | — | ✅ |
| Flexiones | — | — | ✅ |

**Hombro (5 del Diccionario → 3 por versión):**
| Ejercicio | V1 | V2 | V3 |
|-----------|----|----|-----|
| Press de elevación sentado | ✅ | ✅ | ✅ |
| Elevación lateral | ✅ | ✅ | — |
| Elevacion frontal | ✅ | — | ✅ |
| Remo vertical con cable | — | ✅ | — |
| Remo vertical | — | — | ✅ |

**Tríceps (3 del Diccionario → 2 por versión):**
| Ejercicio | V1 | V2 | V3 |
|-----------|----|----|-----|
| Extensión de triceps por encima de la cabeza | ✅ | — | ✅ |
| Flexión de triceps con cuerda | ✅ | ✅ | — |
| Dominada de triceps banco | — | ✅ | ✅ |

**Resultado:** Los 15 ejercicios del Módulo B están distribuidos en al menos 1 versión. 0 ejercicios del Diccionario del Módulo B quedan sin versión asignada.

### Bloque C — Módulo C: Reducción a 8 ejercicios por versión

#### CA-21.12 — Máximo 8 ejercicios con cobertura de los 6 grupos musculares

**Dado que** el Módulo C actual prescribe 9 ejercicios por versión,
**cuando** se aplica la reestructuración,
**entonces** cada versión del Módulo C contiene exactamente **8 ejercicios**, cubriendo los 6 grupos musculares del tren inferior: Cuádriceps, Isquiotibiales, Glúteos, Aductores, Abductores y Gemelos. La cobertura de aductores puede lograrse mediante ejercicios multiarticulares (ej: Sentadilla de Sumo cubre Cuádriceps + Aductores) sin necesidad de un ejercicio de aislamiento dedicado en todas las versiones.

#### CA-21.13 — Composición definitiva del Módulo C — Versión 1 (8 ejercicios)

**Dado que** la V1 del Módulo C se reestructura,
**cuando** se consulta el plan actualizado,
**entonces** la V1 contiene exactamente los siguientes 8 ejercicios en este orden:

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Cuádriceps | Sentadilla | Maquina Multiestación | 4 | 8-12 |
| 2 | Cuádriceps | Press de Pierna | Maquina | 4 | 8-12 |
| 3 | Cuádriceps | Extensión de Cuadriceps | Maquina | 4 | 8-12 |
| 4 | Isquiotibiales | Curl Femoral Tumbado | Maquina | 4 | 8-12 |
| 5 | Glúteos | Empuje de Cadera | Maquina | 4 | 8-12 |
| 6 | Aductores | Aductor de Cadera | Maquina | 4 | 8-12 |
| 7 | Abductores | Abductor de Cadera | Maquina | 4 | 8-12 |
| 8 | Gemelos (Sóleo) | Elevación de Gemelos Sentado | Maquina | 4 | 8-12 |

**Cobertura de zonas:** Cuádriceps ✅, Isquiotibiales ✅, Glúteos ✅, Aductores ✅ (dedicado), Abductores ✅, Gemelos ✅.

**Ejercicio removido:** Avanzada de Zancadas (redundante con Sentadilla + Press de Pierna para cobertura de cuádriceps/glúteos).

#### CA-21.14 — Composición definitiva del Módulo C — Versión 2 (8 ejercicios)

**Dado que** la V2 del Módulo C se reestructura,
**cuando** se consulta el plan actualizado,
**entonces** la V2 contiene exactamente los siguientes 8 ejercicios en este orden:

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Cuádriceps, Aductores | Sentadilla de Sumo | Mancuerna o Pesa Rusa | 4 | 8-12 |
| 2 | Cuádriceps, Glúteos | Sentadilla Bulgara Dividida | Mancuernas | 4 | 8-12 |
| 3 | Cuádriceps | Press de Pierna | Maquina | 4 | 8-12 |
| 4 | Cuádriceps | Extensión de Cuadriceps | Maquina | 4 | 8-12 |
| 5 | Isquiotibiales | Curl Femoral Tumbado | Maquina | 4 | 8-12 |
| 6 | Glúteos, Cuádriceps | Zancada hacia atras | Mancuernas | 4 | 8-12 |
| 7 | Abductores | Abductor de Cadera | Maquina | 4 | 8-12 |
| 8 | Gemelos (Sóleo) | Elevación de Gemelos Sentado | Maquina | 4 | 8-12 |

**Cobertura de zonas:** Cuádriceps ✅, Isquiotibiales ✅, Glúteos ✅ (Búlgara + Zancada atrás), Aductores ✅ (Sentadilla de Sumo multi-zona), Abductores ✅, Gemelos ✅.

**Ejercicio removido:** Avanzada de Zancadas (redundante con Sentadilla Búlgara + Zancada hacia atrás).

#### CA-21.15 — Composición definitiva del Módulo C — Versión 3 (8 ejercicios)

**Dado que** la V3 del Módulo C se reestructura,
**cuando** se consulta el plan actualizado,
**entonces** la V3 contiene exactamente los siguientes 8 ejercicios en este orden:

| # | Zona Muscular | Ejercicio | Tipo | Series | Repeticiones |
|---|---|---|---|---|---|
| 1 | Cuádriceps, Glúteos | Sentadilla Bulgara Dividida | Mancuernas | 4 | 8-12 |
| 2 | Cuádriceps, Glúteos | Subir Escalones | Maquina | 4 | 8-12 |
| 3 | Cuádriceps | Sentadilla | Cuerpo | 4 | 8-12 |
| 4 | Cuádriceps | Extensión de Cuadriceps | Maquina | 4 | 8-12 |
| 5 | Isquiotibiales | Curl Femoral Tumbado | Maquina | 4 | 8-12 |
| 6 | Aductores | Aductor de Cadera | Maquina | 4 | 8-12 |
| 7 | Abductores | Abductor de Cadera | Maquina | 4 | 8-12 |
| 8 | Gemelos (Sóleo) | Elevación de Gemelos Sentado | Maquina | 4 | 8-12 |

**Cobertura de zonas:** Cuádriceps ✅, Isquiotibiales ✅, Glúteos ✅ (Búlgara + Subir Escalones), Aductores ✅ (dedicado), Abductores ✅, Gemelos ✅.

**Ejercicio removido:** Avanzada de Zancadas (redundante con Sentadilla Búlgara + Subir Escalones).

#### CA-21.16 — Distribución del Módulo C y nota sobre Avanzada de Zancadas

**Dado que** la Avanzada de Zancadas se remueve de las 3 versiones del Módulo C,
**cuando** se evalúa la distribución,
**entonces:**

- **Avanzada de Zancadas permanece en el Diccionario de Ejercicios** del Módulo C y sigue disponible para sustitución puntual en sesiones activas (RF16). No se elimina del sistema.
- 13 de los 14 ejercicios del Módulo C en el Diccionario están asignados a al menos 1 versión del plan.
- **4 ejercicios fijos** transversales a las 3 versiones: Extensión de Cuádriceps, Curl Femoral Tumbado, Abductor de Cadera, Elevación de Gemelos Sentado.
- **4 ejercicios variables** por versión, cubriendo siempre los 6 grupos musculares del tren inferior.

### Bloque D — Orden sugerido de ejecución por módulo

#### CA-21.17 — Principio: el orden sugerido es orientativo, no obligatorio

**Dado que** el sistema establece un orden sugerido de ejecución por módulo basado en priorización biomecánica,
**cuando** el ejecutante consulta el plan o se encuentra en una sesión activa,
**entonces** el orden sugerido se comunica como una **recomendación informativa** (a través del ordenamiento de la lista de ejercicios) y **no restringe** la libertad del ejecutante para ejecutar los ejercicios en cualquier orden según las circunstancias del gimnasio. La Restricción de Integridad §6.B.11 del MDS ("El orden de ejecución de ejercicios dentro de una sesión es libre") sigue plenamente vigente.

#### CA-21.18 — Orden sugerido del Módulo A: Espalda → Bíceps → Abdomen

**Dado que** el ejecutante consulta o ejecuta una sesión del Módulo A,
**cuando** se presentan los ejercicios,
**entonces** el orden sugerido es:

1. **Espalda** (ejercicios 1-4): Primero, porque los compuestos de tracción son los de mayor demanda neural del módulo y el bíceps actúa como sinergista — ejecutarlos con el SNC fresco maximiza la carga y el estímulo en el grupo muscular más grande.
2. **Bíceps** (ejercicios 5-8): Segundo, aprovechando la pre-activación del bíceps durante los ejercicios de espalda.
3. **Abdomen** (ejercicios 9-12 o 9-11 según versión): Al final si se ejecuta en el gym. **Si el ejecutante elige hacer abdomen fuera del gym, lo ideal es ejecutarlo al inicio, antes de la sesión en el gimnasio**, como activación del core previo a los ejercicios de tracción.

#### CA-21.19 — Orden sugerido del Módulo B: Pecho → Hombro → Tríceps

**Dado que** el ejecutante consulta o ejecuta una sesión del Módulo B,
**cuando** se presentan los ejercicios,
**entonces** el orden sugerido es:

1. **Pecho** (ejercicios 1-3): Primero, porque es el grupo muscular más grande del módulo de empuje y los presses de pecho permiten la mayor expresión de fuerza.
2. **Hombro** (ejercicios 4-6): Segundo, ejecutando el press de hombro compuesto mientras la capacidad neural aún es razonable, seguido por las elevaciones de aislamiento.
3. **Tríceps** (ejercicios 7-8): Al final, porque el tríceps ya está parcialmente fatigado por todos los presses anteriores. Los 2 ejercicios directos completan el estímulo sobre un músculo ya pre-activado.

#### CA-21.20 — Orden sugerido del Módulo C: Cuádriceps compuestos → Isquiotibiales → Glúteos → Aductores/Abductores → Gemelos

**Dado que** el ejecutante consulta o ejecuta una sesión del Módulo C,
**cuando** se presentan los ejercicios,
**entonces** el orden sugerido es:

1. **Cuádriceps — compuestos** (Sentadilla, Press de Pierna): Primero, porque son los ejercicios de mayor demanda sistémica sobre el SNC. La sentadilla y el press de pierna movilizan las mayores cargas absolutas y requieren capacidad neural máxima.
2. **Cuádriceps — aislamiento** (Extensión de Cuádriceps): Después de los compuestos.
3. **Isquiotibiales** (Curl Femoral Tumbado): Cadena posterior.
4. **Glúteos** (Empuje de Cadera, Zancada hacia atrás, u otros): Ya pre-activados por los compuestos. Los ejercicios multiarticulares que combinan glúteos+cuádriceps se ubican según su zona muscular primaria.
5. **Aductores/Abductores**: Ejercicios de aislamiento en máquina con baja demanda sistémica.
6. **Gemelos** (Elevación de Gemelos Sentado): Al final, porque el sóleo es un músculo postural con predominancia de fibras tipo I, extremamente resiliente a la fatiga.

### Bloque E — Actualización documental ✅ Completado

> **Estado:** ✅ Completado por el PO el 2026-03-04. Los documentos de negocio fueron actualizados y verificados en coherencia inter-documental.

#### CA-21.21 — Plan de Entrenamiento actualizado ✅

La documentación `docs/business_definition/Plan de Entrenamiento.md` refleja: nota del Módulo A (4 espalda fijos, abdomen fuera del gym), V1 del Módulo A con 12 ejercicios, nota del Módulo B (nueva distribución 3P+3H+2T), las 3 versiones del Módulo B con 8 ejercicios cada una, nota del Módulo C (4 fijos + 4 variables, salida de Avanzada de Zancadas), las 3 versiones del Módulo C con 8 ejercicios cada una, y el orden de los ejercicios en cada tabla reflejando el orden sugerido por módulo.

#### CA-21.22 — Manifiesto de Dominio Sistémico actualizado ✅

El documento incorpora en la Ontología del Dominio (§4.A): definición de "Orden Sugerido de Ejecución" y "Ejercicio sin Requisito de Equipamiento". En la Dinámica del Sistema (§5.A): referencia al orden sugerido como guía y mención de abdomen fuera del gym en Módulo A. Se preserva íntegramente la Restricción de Integridad §6.B.11 con aclaración de que el orden sugerido es informativo.

#### CA-21.23 — Coherencia inter-documental ✅

El Diccionario de Ejercicios no requiere cambios. El Plan de Entrenamiento es coherente con el Manifiesto. Los Requerimientos RF04/RF05/RF06 siguen siendo válidos. Cada ejercicio referenciado en las versiones del Plan existe en el Diccionario con el módulo correcto.

### Bloque F — Análisis técnico, diseño e implementación

#### CA-21.24 — Seed data del plan refleja la documentación actualizada

**Dado que** el PO ha actualizado la documentación y el Arquitecto ha completado el análisis y diseño técnico,
**cuando** el Desarrollador implementa los cambios en el código fuente,
**entonces** el seed data (tabla `plan_assignment`) refleja exactamente las composiciones, cantidades y orden de ejercicios definidos en la documentación para cada combinación módulo-versión:
- Módulo A: V1 = 12 asignaciones, V2 = 11 asignaciones, V3 = 11 asignaciones.
- Módulo B: V1 = 8, V2 = 8, V3 = 8.
- Módulo C: V1 = 8, V2 = 8, V3 = 8.
- El campo `sort_order` de cada asignación refleja el orden sugerido por módulo.

#### CA-21.25 — Presentación de ejercicios en la UI refleja el orden sugerido

**Dado que** el orden sugerido está definido en el seed data a través de la posición de cada ejercicio (`sort_order`),
**cuando** el ejecutante consulta el Plan de Entrenamiento (pantalla D4) o se encuentra en una sesión activa (pantalla E1),
**entonces** los ejercicios se listan en el orden sugerido (Espalda → Bíceps → Abdomen para A; Pecho → Hombro → Tríceps para B; Cuádriceps compuestos → Isquiotibiales → Glúteos → Aductores/Abductores → Gemelos para C). Este orden es la presentación por defecto; no impide que el ejecutante ejecute y registre series en cualquier orden.

#### CA-21.26 — Indicación visual de "ejecutable fuera del gym" para ejercicios de abdomen

**Dado que** los ejercicios de abdomen del Módulo A pueden realizarse fuera del gimnasio,
**cuando** el ejecutante consulta la sesión del Módulo A (tanto en preview como en sesión activa),
**entonces** los ejercicios de abdomen se presentan con una diferenciación visual (badge "Fuera del gym" con color `secondaryContainer`) que indica que son ejecutables fuera del gym. El mecanismo visual se implementa basado en la lógica `moduleCode == "A" && isBodyweight == true`.

#### CA-21.27 — Migración de datos: sesiones existentes se preservan íntegramente

**Dado que** el plan cambia y puede haber sesiones históricas registradas con las composiciones anteriores,
**cuando** se aplica la actualización del seed data,
**entonces:**
- Las sesiones ya completadas (estado Completada o Incompleta) **no se modifican**. Sus registros de series están vinculados a `exercise_id` y `session_id`, no a `plan_assignment`. Los datos históricos se preservan íntegramente.
- Si existe una sesión en estado "En Progreso" al momento de la actualización, conserva sus ejercicios tal como fueron creados. El LEFT JOIN a plan_assignment puede retornar sort_order nuevo para ejercicios del plan, o NULL para removidos (fallback 9999 — ejercicio aparece al final). La sesión es completable sin intervención.
- Las **nuevas sesiones** iniciadas después de la actualización utilizan las composiciones actualizadas.
- El estado de rotación (`rotation_state`) **no se reinicia**. Si al ejecutante le tocaba Módulo B V2, le sigue tocando Módulo B V2 — simplemente con la nueva composición de V2.
