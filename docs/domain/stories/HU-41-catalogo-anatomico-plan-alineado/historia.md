# Historia de Usuario

**Como** El Ejecutante,
**Quiero** un catálogo de ejercicios clasificado por la anatomía real, con zonas principales y secundarias, y un plan por defecto con el orden y el implemento sugerido que de verdad uso,
**Para** que las métricas por grupo muscular sean fieles y la sesión que el sistema propone no necesite ajustes cada día.

> **Hija 3 de 5** de la partición de *Equipamiento por ejercicio, 1RM estimado y sesión editable* (40 CAs). Hermanas: [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/index.md), [`HU-40`](../HU-40-progresion-por-equipamiento/index.md), [`HU-42`](../HU-42-1rm-estimado/index.md), [`HU-43`](../HU-43-ajustar-sesion-del-dia/index.md). **Depende de `HU-39`.**

## Descripción

El catálogo muscular del sistema tiene una granularidad que no alcanza para describir lo que un ejercicio hace. `Elevación Lateral` y `Press Militar` figuran ambos como trabajo de «Hombro», cuando uno aísla el deltoides lateral y el otro el anterior. `Curl Martillo` y `Curl de Predicador` figuran ambos como «Bíceps», cuando el primero trabaja braquial y braquiorradial y el segundo la cabeza corta. Y la relación de un ejercicio con sus zonas es **plana**: no distingue el músculo que ejecuta el movimiento del que solo asiste.

Esta historia sustituye ese catálogo por **33 zonas con granularidad anatómica** —los tres deltoides y el manguito rotador en lugar de un genérico «Hombro»; las cabezas del bíceps y del tríceps; braquial y braquiorradial; las porciones del pectoral— y añade **jerarquía explícita** entre zona principal y secundaria. Los 37 ejercicios existentes se reclasifican por completo bajo criterio biomecánico: el músculo que ejecuta el movimiento, no la máquina ni la ubicación aparente.

La granularidad fina se acomoda **íntegramente dentro de los 14 grupos musculares existentes**: no se añade ninguno. Es lo que permite que los KPIs de tonelaje y volumen por grupo muscular sigan agregando exactamente como hoy, ganando precisión sin cambiar de definición.

La historia cubre además el **delta del plan por defecto**, que arrastra el mismo criterio de fidelidad:

- **Miércoles y sábado** cambian de orden: *Aductores* vuelve al primer puesto en ambos.
- **Viernes** sustituye *Remo Unilateral Polea Alta* por *Trapecios con Apoyo en Banco Inclinado* en el cuarto puesto. Es el **único ejercicio que la historia añade al catálogo**, que pasa de 37 a 38.
- Las **35 asignaciones** ganan su **equipamiento sugerido**, que preselecciona el selector al registrar la serie sin imponerlo.

Con el equipamiento sugerido, el plan deja de ser agnóstico del implemento y propone el que El Ejecutante realmente usa en cada puesto. La sugerencia **sugiere, no impone**: se puede cambiar, y una vez cambiada, el último implemento usado toma el relevo en las series siguientes.

> **Cambio de alcance (2026-09-11).** El cuarto puesto del viernes lo ocupa *Trapecios con Apoyo en Banco Inclinado*, ejercicio nuevo, y no *Remo al Mentón* como decía la redacción original. Afecta a CA-41.03, CA-41.06 y CA-41.07, y sube el catálogo de 37 a 38. Queda registrado en `cambios.md` y en `refinamiento.md` (D13).

### Frontera de alcance

- **Las opciones de equipamiento de cada ejercicio** ya vienen de [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/historia.md) (CA-39.06). Esta historia añade las **zonas musculares** y la **sugerencia del plan**, no la lista de implementos admitidos.
- **`Core` no se crea como zona.** No es un músculo sino una región; los movimientos anti-rotación se catalogan por `Oblicuos` y `Recto Abdominal`.
- **Ningún ejercicio se elimina.** *Remo Unilateral Polea Alta* sale del plan por defecto pero permanece en el Diccionario. *Remo al Mentón*, que una versión anterior de esta historia metía en el plan, tampoco entra y sigue disponible igual.
- **Se añade un ejercicio**: *Trapecios con Apoyo en Banco Inclinado*, para el cuarto puesto del viernes. Es la única alta del catálogo desde `HU-29`.
- **La dificultad de progresión no se modifica** en ningún ejercicio.
- **La relación de rutinas con los días de la semana no cambia.** Los días que nombran las tablas se refieren a la relación `week_day` establecida en `HU-36`, no al nombre de la rutina.
- **La consolidación de progresión por par** es alcance de [`HU-40`](../HU-40-progresion-por-equipamiento/historia.md). Esta historia solo aporta el reparto de tonelaje por zona.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal — Catálogo anatómico

#### CA-41.01 — Zonas musculares con granularidad anatómica

- **Dado** que la aplicación se instala por primera vez
- **Cuando** se siembra el catálogo de zonas musculares
- **Entonces** el catálogo contiene **33 zonas**, que **reemplazan** a las 20 anteriores:

| Zona | Grupo muscular | Origen |
|---|---|---|
| Pectoral Superior | Pecho | renombrada desde *Pecho Superior* |
| Pectoral Medio | Pecho | renombrada desde *Pecho Medio* |
| Pectoral Inferior | Pecho | renombrada desde *Pecho Inferior* |
| Pectoral Mayor | Pecho | **nueva** |
| Deltoides Anterior | Hombro | **nueva** |
| Deltoides Lateral | Hombro | **nueva** |
| Deltoides Posterior | Hombro | **nueva** |
| Manguito Rotador | Hombro | **nueva** |
| Dorsal Ancho | Espalda | existente |
| Espalda Alta | Espalda | existente |
| Trapecio | Espalda | existente |
| Trapecio Superior | Espalda | **nueva** |
| Trapecio Inferior | Espalda | **nueva** |
| Romboides | Espalda | **nueva** |
| Erectores Espinales | Espalda | renombrada desde *Espalda Baja* |
| Bíceps Braquial | Bíceps | renombrada desde *Bíceps* |
| Bíceps — Cabeza Larga | Bíceps | **nueva** |
| Bíceps — Cabeza Corta | Bíceps | **nueva** |
| Tríceps Braquial | Tríceps | renombrada desde *Tríceps* |
| Tríceps — Cabeza Larga | Tríceps | **nueva** |
| Tríceps — Cabeza Lateral | Tríceps | **nueva** |
| Tríceps — Cabeza Medial | Tríceps | **nueva** |
| Braquial | Antebrazo | **nueva** |
| Braquiorradial | Antebrazo | **nueva** |
| Recto Abdominal | Abdomen | renombrada desde *Abdomen* |
| Oblicuos | Abdomen | **nueva** |
| Cuádriceps | Cuádriceps | existente |
| Isquiotibiales | Isquiotibiales | existente |
| Glúteo Mayor | Glúteos | renombrada desde *Glúteos* |
| Aductores | Aductores | existente |
| Abductores | Abductores | existente — **conservada sin ejercicio** |
| Gastrocnemio | Gemelos | renombrada desde *Gemelos* |
| Cuello | Cuello | existente — **conservada sin ejercicio** |

- **Y** se **retiran** tres zonas por quedar cubiertas por zonas más específicas: `Hombro` (sustituida por los tres deltoides y el manguito rotador), `Antebrazo` (sustituida por braquial y braquiorradial) y `Espalda Media` (cubierta por trapecio, romboides y espalda alta)
- **Y** `Abductores` y `Cuello` **se conservan** aunque ningún ejercicio seed las use, para que El Ejecutante pueda catalogar ejercicios propios de esas zonas
- **Y** los **14 grupos musculares** actuales **no cambian**: la granularidad fina se acomoda íntegramente dentro de ellos, sin añadir ninguno
- **Y** `Core` **no** se crea como zona: no es un músculo, y los movimientos anti-rotación se catalogan por `Oblicuos` y `Recto Abdominal`

#### CA-41.02 — Jerarquía principal / secundaria

- **Dado** un ejercicio y sus zonas musculares
- **Cuando** se consulta la relación
- **Entonces** cada zona asociada está marcada como **principal** o **secundaria**
- **Y** un ejercicio tiene **al menos una** zona principal
- **Y** puede tener **varias** principales cuando el movimiento reparte el trabajo por igual (*Peso Muerto Rumano*: isquiotibiales y glúteo mayor)
- **Y** puede tener **cero** secundarias
- **Y** la jerarquía se muestra en el detalle del ejercicio (`D2-T1`) distinguiendo visiblemente unas de otras
- **Y** la jerarquía se declara en la creación y edición de ejercicios (`D5-T1`, `D2-T1`), tanto seed como personalizados
- **Y** el criterio de clasificación es **biomecánico**: el músculo que ejecuta el movimiento, no la máquina ni la ubicación aparente

#### CA-41.03 — Los 38 ejercicios y sus zonas musculares

- **Dado** que la aplicación se instala por primera vez
- **Cuando** se siembra la relación de ejercicios con zonas musculares
- **Entonces** los 38 ejercicios quedan clasificados exactamente así:

| # | Ejercicio | Zona(s) principal(es) | Zona(s) secundaria(s) |
|---|---|---|---|
| 1 | Aductores | Aductores | — |
| 2 | Cruce de Polea Alta | Pectoral Inferior | Deltoides Anterior |
| 3 | Crunch Abdominal | Recto Abdominal | — |
| 4 | Curl Bayesian en Banco Inclinado | Bíceps — Cabeza Larga | — |
| 5 | Curl de Concentración | Bíceps Braquial | — |
| 6 | Curl de Isquiotibiales Sentado | Isquiotibiales | — |
| 7 | Curl de Martillo Cruzado | Braquial, Braquiorradial | Bíceps Braquial |
| 8 | Curl de Predicador | Bíceps — Cabeza Corta | — |
| 9 | Elevación de Pantorrilla de Pie | Gastrocnemio | — |
| 10 | Elevación Lateral | Deltoides Lateral | — |
| 11 | Extensión de Cuádriceps | Cuádriceps | — |
| 12 | Extensión de Tríceps (Pushdown) | Tríceps — Cabeza Lateral, Tríceps — Cabeza Medial | — |
| 13 | Extensión de Tríceps sobre Cabeza | Tríceps — Cabeza Larga | — |
| 14 | Face Pull | Deltoides Posterior, Trapecio | Manguito Rotador |
| 15 | Hip Thrust | Glúteo Mayor | Isquiotibiales, Cuádriceps |
| 16 | Peso Muerto Rumano | Isquiotibiales, Glúteo Mayor | Erectores Espinales |
| 17 | Prensa Inclinada | Cuádriceps, Glúteo Mayor | Isquiotibiales |
| 18 | Press de Banca Inclinado | Pectoral Superior | Deltoides Anterior, Tríceps Braquial |
| 19 | Press de Banca Plano | Pectoral Medio | Deltoides Anterior, Tríceps Braquial |
| 20 | Press Pallof | Oblicuos | Recto Abdominal |
| 21 | Remo T Inclinado | Dorsal Ancho, Trapecio | Bíceps Braquial, Romboides |
| 22 | Sentadilla Búlgara | Cuádriceps, Glúteo Mayor | — |
| 23 | Sentadilla Sumo | Glúteo Mayor, Cuádriceps | Aductores |
| 24 | Sentadilla Hack | Cuádriceps | Glúteo Mayor |
| 25 | Jalón al Pecho | Dorsal Ancho | Bíceps Braquial, Espalda Alta |
| 26 | Vuelos Posteriores (Pájaros) | Deltoides Posterior | Trapecio, Romboides |
| 27 | Remo al Mentón | Deltoides Lateral, Trapecio Superior | Bíceps Braquial |
| 28 | Aperturas | Pectoral Mayor | Deltoides Anterior |
| 29 | Pull-Over | Dorsal Ancho | Pectoral Inferior |
| 30 | Curl Martillo | Braquial, Braquiorradial | Bíceps Braquial |
| 31 | Rompecráneos | Tríceps Braquial | — |
| 32 | Remo Horizontal | Dorsal Ancho | Trapecio, Romboides, Bíceps Braquial |
| 33 | Zancadas (Lunges) | Cuádriceps, Glúteo Mayor | — |
| 34 | Press Militar | Deltoides Anterior, Deltoides Lateral | Tríceps Braquial |
| 35 | Dominadas | Dorsal Ancho | Bíceps Braquial, Trapecio Inferior |
| 36 | Remo Unilateral Polea Baja | Dorsal Ancho | Bíceps Braquial, Romboides |
| 37 | Remo Unilateral Polea Alta | Dorsal Ancho, Espalda Alta | Bíceps Braquial |
| 38 | Trapecios con Apoyo en Banco Inclinado ✦ **nuevo** | Trapecio, Trapecio Inferior | Romboides, Deltoides Posterior |

- **Y** ningún ejercicio queda sin zona principal
- **Y** ninguna zona figura a la vez como principal y secundaria del mismo ejercicio
- **Y** las **opciones de equipamiento** de los 37 ejercicios existentes, sembradas por `HU-39` (CA-39.06), **no se modifican**
- **Y** la **dificultad de progresión** de los 37 existentes **no se modifica**
- **Y** el ejercicio 38 se da de alta con `Mancuerna`, `Barra` y `Máquina Smith` como implementos admitidos y dificultad de progresión **media**

#### CA-41.04 — Tonelaje por grupo muscular con todas las zonas

- **Dado** el volumen por grupo muscular (`G2-T1`) y la tendencia de progresión por grupo (`G3-T1`)
- **Cuando** se reparte el tonelaje de un ejercicio entre los grupos musculares
- **Entonces** **cuentan todas las zonas** del ejercicio, principales y secundarias, **sin ponderación**
- **Y** la jerarquía es informativa para El Ejecutante, no un peso en el cálculo
- **Y** los grupos musculares de agregación siguen siendo los **14 existentes**: ningún KPI cambia de definición ni de eje
- **Y** la alerta `TONNAGE_DROP` sigue evaluándose sobre los mismos grupos, con la misma ventana y el mismo umbral

---

### Escenario 2: Flujo Principal — Plan por defecto alineado

#### CA-41.05 — El plan sugiere el equipamiento

- **Dado** el plan de entrenamiento
- **Cuando** se consulta una asignación de ejercicio a una versión de rutina
- **Entonces** la asignación lleva un **equipamiento sugerido**, que es una de las opciones admitidas por ese ejercicio
- **Y** al registrar la serie (`E2-T1`), la sugerencia del plan **preselecciona** el selector de equipamiento
- **Y** la sugerencia **sugiere, no impone**: El Ejecutante puede cambiarla
- **Y** **el último cambio manda**: una vez que El Ejecutante elige otro equipamiento, la preselección de las series siguientes pasa a ser el último equipamiento efectivamente usado en ese ejercicio, no la sugerencia del plan
- **Y** la sugerencia se muestra en el detalle de la versión del plan (`D4-T1`)
- **Y** la sugerencia es editable desde la gestión del plan (`D6-T1`), igual que las series y las repeticiones
- **Y** cada ejercicio de un **puesto dual** lleva su **propia** sugerencia, porque son ejercicios distintos con opciones distintas
- **Y** un ejercicio **sin asignación de plan** —el que se añade o se crea dentro de la sesión, según [`HU-43`](../HU-43-ajustar-sesion-del-dia/historia.md)— no tiene sugerencia que heredar: su selector se preselecciona con el **último equipamiento usado** en ese ejercicio y, si nunca se usó, con su **primera opción admitida**, tal como establece CA-39.04

#### CA-41.06 — Delta de composición y orden del plan

- **Dado** que la aplicación se instala por primera vez
- **Cuando** se siembra el plan por defecto
- **Entonces** se aplican estos tres cambios sobre la composición vigente:

| Rutina | Cambio |
|---|---|
| **Lower — Foco Cuádriceps** (miércoles) | *Aductores* pasa del cuarto al **primer** puesto; el resto conserva su orden relativo |
| **Pull — Foco Trapecios y Espalda Media** (viernes) | *Remo Unilateral Polea Alta* **sale** del cuarto puesto y **entra Trapecios con Apoyo en Banco Inclinado** en su lugar |
| **Lower — Foco Isquiotibiales y Glúteo** (sábado) | *Aductores* pasa del cuarto al **primer** puesto; el resto conserva su orden relativo |

- **Y** las rutinas de **lunes, martes y jueves** conservan su composición y su orden
- **Y** el plan sigue teniendo **6 rutinas, 1 versión cada una y 35 asignaciones**
- **Y** los **cuatro puestos duales** se conservan: lunes puesto 2, martes puesto 1, miércoles puesto 3 y viernes puesto 2
- **Y** *Remo Unilateral Polea Alta* **permanece en el Diccionario** como ejercicio disponible para asignación manual o alternativa de puesto
- **Y** con ello quedan **ocho ejercicios fuera del plan por defecto y dentro del Diccionario**: *Cruce de Polea Alta*, *Curl de Concentración*, *Curl de Martillo Cruzado*, *Press Pallof*, *Sentadilla Sumo*, *Remo al Mentón*, *Zancadas (Lunges)* y *Remo Unilateral Polea Alta*
- **Y** la relación de rutinas con los días de la semana **no cambia**
- **Y** todas las asignaciones conservan el rango de repeticiones **8-12**

#### CA-41.07 — Las 35 asignaciones con su equipamiento sugerido

- **Dado** el plan por defecto sembrado
- **Cuando** se consulta cada asignación
- **Entonces** su equipamiento sugerido es exactamente este:

| Rutina (día) | # | Ejercicio | Series | Equipamiento sugerido |
|---|---|---|---|---|
| **Push — Deltoides Lateral y Medio** (lunes) | 1 | Elevación Lateral | 4 | Mancuerna |
| | 2 | Press de Banca Inclinado *(primario del puesto)* | 3 | Barra |
| | 2 | Press Militar *(alternativa del puesto)* | 3 | Barra |
| | 3 | Press de Banca Plano | 3 | Barra |
| | 4 | Aperturas | 3 | Máquina |
| **Pull — Dorsal Ancho** (martes) | 1 | Jalón al Pecho *(primario del puesto)* | 4 | Polea |
| | 1 | Dominadas *(alternativa del puesto)* | 4 | Barra Fija |
| | 2 | Curl Martillo | 3 | Mancuerna |
| | 3 | Remo Unilateral Polea Baja | 3 | Polea |
| | 4 | Curl Bayesian en Banco Inclinado | 3 | Mancuerna |
| | 5 | Pull-Over | 3 | Polea |
| | 6 | Crunch Abdominal | 3 | Polea |
| **Lower — Cuádriceps** (miércoles) | 1 | Aductores | 3 | Máquina |
| | 2 | Extensión de Cuádriceps | 4 | Máquina |
| | 3 | Sentadilla Hack *(primario del puesto)* | 3 | Máquina |
| | 3 | Prensa Inclinada *(alternativa del puesto)* | 3 | Máquina |
| | 4 | Sentadilla Búlgara | 3 | Mancuerna |
| | 5 | Elevación de Pantorrilla de Pie | 3 | Máquina |
| **Push — Tríceps** (jueves) | 1 | Extensión de Tríceps sobre Cabeza | 4 | Mancuerna |
| | 2 | Press de Banca Plano | 3 | Barra |
| | 3 | Aperturas | 3 | Máquina |
| | 4 | Extensión de Tríceps (Pushdown) | 3 | Polea |
| | 5 | Rompecráneos | 3 | Mancuerna |
| **Pull — Trapecios y Espalda Media** (viernes) | 1 | Remo T Inclinado | 4 | Máquina |
| | 2 | Face Pull *(primario del puesto)* | 3 | Polea |
| | 2 | Vuelos Posteriores (Pájaros) *(alternativa del puesto)* | 3 | Mancuerna |
| | 3 | Remo Horizontal | 3 | Polea |
| | 4 | Trapecios con Apoyo en Banco Inclinado | 3 | Mancuerna |
| | 5 | Curl de Predicador | 3 | Mancuerna |
| | 6 | Crunch Abdominal | 3 | Polea |
| **Lower — Isquiotibiales y Glúteo** (sábado) | 1 | Aductores | 3 | Máquina |
| | 2 | Curl de Isquiotibiales Sentado | 4 | Máquina |
| | 3 | Peso Muerto Rumano | 3 | Mancuerna |
| | 4 | Hip Thrust | 3 | Mancuerna |
| | 5 | Elevación de Pantorrilla de Pie | 3 | Máquina |

- **Y** en el puesto dual del martes, *Jalón al Pecho* sugiere `Polea` y *Dominadas* sugiere `Barra Fija` — no comparten sugerencia aunque compartan puesto
- **Y** toda sugerencia es una de las opciones admitidas por su ejercicio según CA-39.06 (verificado por CA-41.08)
- **Y** ninguna asignación queda sin equipamiento sugerido

---

### Escenario 3: Validaciones

#### CA-41.08 — Sugerencia fuera de las opciones del ejercicio

- **Dado** una asignación del plan
- **Cuando** se intenta fijar como equipamiento sugerido uno que el ejercicio **no admite**
- **Entonces** el sistema **rechaza** la operación
- **Y** el selector de sugerencia solo ofrece las opciones admitidas por ese ejercicio
- **Y** si al editar un ejercicio se **retira** una opción de equipamiento que alguna asignación del plan tenía como sugerencia, el sistema **lo impide** mientras esa asignación exista
- **Y** al asignar un ejercicio nuevo al plan, la sugerencia es **obligatoria**: no se persiste la asignación sin ella

#### CA-41.09 — Zonas musculares obligatorias

- **Dado** el formulario de creación o edición de ejercicio
- **Cuando** El Ejecutante intenta guardar sin ninguna zona muscular **principal**
- **Entonces** el sistema **no persiste** el ejercicio y muestra error
- **Y** el botón de guardar permanece **deshabilitado** mientras no haya al menos una principal
- **Y** las zonas secundarias son **opcionales**
- **Y** una misma zona **no** puede figurar a la vez como principal y como secundaria del mismo ejercicio

---

### Escenario 4: Casos Extremos

#### CA-41.10 — Respaldo y restauración

- **Dado** el mecanismo de respaldo y restauración (`J2-T1`, `J3-T1`)
- **Cuando** se exporta un respaldo tras esta historia
- **Entonces** el respaldo incluye el **catálogo de zonas musculares**, la **jerarquía principal/secundaria** de cada ejercicio y el **equipamiento sugerido** de cada asignación del plan
- **Y** un respaldo restaurado reproduce el catálogo y el plan sin recalcularlos
- **Y** los respaldos del formato anterior a `HU-39` siguen siendo **incompatibles** y se rechazan con mensaje explícito
- **Y** el criterio de instalación fresca de **ADR-019** (CA-39.11) aplica también a esta historia

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — actor único del sistema. Aplicación *single-user* sin roles.
- **Permisos requeridos:** ninguno. No existe modelo de permisos.
- **Valor de negocio:** las métricas de volumen y tonelaje por grupo muscular dejan de apoyarse en una clasificación gruesa que confunde movimientos distintos, y el plan por defecto propone el orden y el implemento que El Ejecutante realmente usa, eliminando el ajuste manual repetido en cada sesión.

### Reglas de Negocio

1. **Criterio biomecánico.** La zona muscular es el músculo que ejecuta el movimiento, no la máquina ni la ubicación aparente.
2. **Toda zona tiene un grupo padre entre los 14 existentes.** La granularidad fina no añade grupos.
3. **Al menos una zona principal por ejercicio.** Las secundarias son opcionales.
4. **Varias principales son legítimas** cuando el movimiento reparte el trabajo por igual.
5. **Ninguna zona es a la vez principal y secundaria** del mismo ejercicio.
6. **Todas las zonas cuentan para el tonelaje**, principales y secundarias, sin ponderación.
7. **`Core` no es una zona.** No es un músculo; los anti-rotación se catalogan por `Oblicuos` y `Recto Abdominal`.
8. **Las zonas sin ejercicio se conservan** si son anatómicamente legítimas (`Abductores`, `Cuello`), para no impedir ejercicios propios a futuro.
9. **El plan sugiere, El Ejecutante decide, y el último cambio manda.**
10. **La sugerencia debe ser una opción admitida por el ejercicio.** Es una validación, no una convención.
11. **Ningún ejercicio se elimina del catálogo** aunque salga del plan por defecto.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| Diccionario — filtro | `D1-T1` | El filtro por grupo muscular opera sobre las zonas nuevas |
| Diccionario — detalle | `D2-T1` | Muestra las zonas distinguiendo principales de secundarias |
| Diccionario — crear / editar | `D5-T1`, `D2-T1` | Selección de zonas con marca de principal/secundaria y validación de mínimo una principal |
| Plan — detalle de versión | `D4-T1` | Muestra el equipamiento sugerido por asignación |
| Plan — gestión | `D6-T1` | Permite editar el equipamiento sugerido, limitado a las opciones del ejercicio |
| Registro de serie | `E2-T1` | El selector se preselecciona con la sugerencia del plan, y luego con el último uso |
| Volumen por grupo muscular | `G2-T1` | Reparto sobre las zonas nuevas, todas contando |
| Tendencia por grupo muscular | `G3-T1` | Mismo eje de agregación, zonas nuevas |
| Respaldo / Restauración | `J2-T1`, `J3-T1` | Formato ampliado con zonas, jerarquía y sugerencia |

#### Detalle de Interfaz de Usuario

- **Diseño general:** ninguna pantalla nueva. Son modificaciones sobre pantallas existentes de los flujos D (Catálogo), E (Sesión Activa), G (Métricas) y J (Ajustes y Respaldo).
- **Campos y controles:**
  - **Formulario de ejercicio (`D5-T1`, `D2-T1`) — zonas musculares:** **dos campos separados**, *Zonas principales* (obligatorio, marcado con asterisco) y *Zonas secundarias* (opcional). Cada uno lista lo ya elegido como filas con acción de quitar, más una acción **«+ Añadir zona»** que abre el selector con las **33 zonas agrupadas por sus 14 grupos musculares**. La jerarquía queda implícita en el campo donde se elige, sin un control extra por zona. Se eligió sobre la lista única con marca y sobre el buscador con chips porque **la obligatoriedad se lee donde se incumple**: el aviso de «al menos una principal» vive en el campo que lo exige, y con 33 zonas eso evita que la validación aparezca lejos del error.
  - **Detalle del ejercicio (`D2-T1`):** las zonas principales se distinguen visiblemente de las secundarias.
  - **Asignación del plan (`D4-T1`, `D6-T1`):** selector de equipamiento sugerido, limitado a las opciones admitidas por el ejercicio y obligatorio para persistir la asignación.
- **Flujo de navegación visual:** sin rutas nuevas.
- **Mensajes y feedback:** error inline al guardar un ejercicio sin zona principal; rechazo explicado al intentar sugerir un equipamiento que el ejercicio no admite; impedimento explicado al retirar una opción de equipamiento que el plan usa como sugerencia.

### Sistemas Externos

Ninguno. El sistema es local y sin integraciones.

### Preview de Interfaz

**Preview:** [`41.preview.txt`](./41.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — entidades `muscle_zone`, `exercise_muscle_zone`, `exercise`, `plan_assignment`, `routine`, `routine_version`, `week_day`; enum `MuscleGroup`; datos semilla §6.1.
- `docs/architecture/interfaces_contract.md` — triggers `D1-T1`, `D2-T1`, `D4-T1`, `D5-T1`, `D6-T1`, `E2-T1`, `G2-T1`, `G3-T1`, `J2-T1`, `J3-T1`.
- **ADR-019** — cambio de esquema resuelto por instalación fresca. Criterio heredado de `HU-39` (CA-39.11).

**Historias hermanas:**
- [`HU-39`](../HU-39-equipamiento-multiple-por-ejercicio/historia.md) — **prerrequisito obligatorio.** Aporta las opciones de equipamiento de cada ejercicio, sobre las que se valida la sugerencia del plan.
- [`HU-40`](../HU-40-progresion-por-equipamiento/historia.md) — **independiente, y va antes.** Su CA-40.07 deja el reparto de tonelaje por zona **fuera de su frontera**; CA-41.04 lo completa después. No hay dependencia en ninguna dirección: `HU-40` funciona con las zonas vigentes y esta historia las sustituye sin tocar su lógica.
- [`HU-42`](../HU-42-1rm-estimado/historia.md) — independiente.
- [`HU-43`](../HU-43-ajustar-sesion-del-dia/historia.md) — **depende de esta**: reutiliza el formulario `D5-T1` ya con jerarquía de zonas.

**Historias relacionadas:**
- `HU-03` — Diccionario de Ejercicios: filtro, detalle y creación. Sus formularios cambian.
- `HU-04`, `HU-21`, `HU-23`, `HU-26`, `HU-27` — plan de entrenamiento, puestos y alternativas. `plan_assignment` gana el equipamiento sugerido.
- `HU-15`, `HU-35` — KPIs y métricas. El eje de agregación no cambia; sí las zonas que lo alimentan.
- `HU-24`, `HU-29` — **precedente directo**: recategorización de catálogo y delta de plan por defecto con tabla cerrada. Esta historia aplica un delta sobre el resultado de `HU-29`.
- `HU-36` — días de la semana como entidad reasignable. Los días que nombran estas tablas se refieren a la relación `week_day`, no al nombre de la rutina.

**Lecciones aprendidas:**
- `HU-29` demostró que una recategorización de catálogo conviene declararla como **tabla completa y cerrada** en el criterio de aceptación, no como delta narrado: evita ambigüedad en el sembrado. Las tablas de CA-41.03 y CA-41.07 siguen ese patrón.
- `HU-29` también estableció que un renombrado debe **conservar identificador, imagen, zona e historial**. `HU-39` (CA-39.07) aplica ese mismo criterio a los ocho renombrados que preceden a esta historia.
- `HU-35` mostró que una métrica es útil solo si su eje se entiende. Mantener los 14 grupos como eje de agregación —ganando precisión por debajo— preserva esa legibilidad.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente
- [ ] Mensajes implementados
- [ ] Catálogo de zonas sembrado exactamente como declara CA-41.01 (33 zonas, 3 retiradas, 14 grupos sin cambio)
- [ ] Los 38 ejercicios clasificados exactamente como declara CA-41.03, con al menos una zona principal cada uno
- [ ] Plan por defecto sembrado con el delta de CA-41.06 y las 35 sugerencias de CA-41.07
- [ ] Verificado que toda sugerencia del plan es una opción admitida por su ejercicio
- [ ] Verificado que el tonelaje por grupo muscular cuenta todas las zonas sin ponderación
- [ ] Documentación de arquitectura actualizada: modelo de dominio, contrato de interfaces y versión de esquema
- [ ] `story_mapping_index.md` actualizado con la historia, sus hermanas y sus dependencias
