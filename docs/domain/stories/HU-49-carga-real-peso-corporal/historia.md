# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que el tonelaje y el 1RM estimado incluyan el peso corporal que muevo en cada ejercicio y ver mi fuerza relativa a mi peso,
**Para** que mis números reflejen la carga que de verdad muevo, incluido mi propio cuerpo.

> **Historia resultante de partición.** Es la **segunda de dos** historias en que se dividió la historia original *Eliminar el nivel de experiencia y usar el peso corporal en las métricas* (17 CAs). Su hermana [`HU-48`](../HU-48-perfil-sin-nivel-peso-metricas/historia.md) retira el nivel de experiencia y lleva el peso corporal a Métricas. **Las dos son independientes y pueden implementarse en cualquier orden.**

## Descripción

El sistema calcula el tonelaje y el 1RM estimado **ignorando el cuerpo del ejecutante**:

- una dominada sin lastre aporta **0 Kg** al tonelaje y no produce 1RM;
- una dominada con 10 Kg de lastre cuenta solo los 10 Kg;
- una sentadilla búlgara con mancuernas de 10 Kg cuenta solo las mancuernas.

En los tres casos el ejecutante mueve su propio cuerpo, y esa carga no aparece en ningún número.

Esta historia introduce la **carga real** de una serie: cada ejercicio declara **qué porcentaje del peso corporal mueve**, y ese porcentaje, aplicado al peso corporal que el ejecutante tenía en la fecha de la sesión, se suma a la carga externa registrada. La carga real alimenta el tonelaje en todas sus vistas y el 1RM estimado. Junto a cada 1RM se muestra además la **fuerza relativa**: cuántas veces el peso corporal representa ese 1RM, usando el peso que el ejecutante tenía cuando lo alcanzó.

### Frontera de alcance

- **El motor de decisión no cambia.** Las señales de subir o bajar peso, la detección de meseta, la de regresión y fatiga, y el protocolo de descarga no leen el peso corporal, el porcentaje, la carga real ni la fuerza relativa. Los ejercicios con el propio cuerpo siguen progresando por repeticiones (RF-31). Solo la alerta de caída de tonelaje cambia, porque el tonelaje mismo cambia (ver CA-49.01).
- **La serie no cambia.** La serie sigue guardando la carga externa que el ejecutante tecleó. La carga real es un cálculo.
- **Peso corporal como complemento, no como juicio.** No se registra ni calcula composición corporal; el peso solo completa la carga movida.
- **Sin migración.** La aplicación está en beta y se valida sobre instalación fresca (ADR-019). Los respaldos se generan con la estructura resultante de esta historia, así que no se contempla compatibilidad con respaldos anteriores.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-49.01 — El tonelaje incluye el peso corporal movido

- **Dado** que un ejercicio declara un **porcentaje de peso corporal** mayor que 0
- **Y** que El Ejecutante registra una serie de ese ejercicio con un implemento que **suma peso corporal** (ver Regla de Negocio 3)
- **Cuando** el sistema calcula el tonelaje de esa serie
- **Entonces** la carga de la serie es **peso corporal vigente × porcentaje + carga externa registrada**
- **Y** ejemplos con un peso corporal de 80 Kg:
  - Dominadas en Barra Fija, 100 %, 8 repeticiones → 80 Kg × 8 = **640 Kg**
  - Dominadas con Peso Añadido de 10 Kg, 100 %, 6 repeticiones → (80 + 10) × 6 = **540 Kg**
  - Sentadilla Búlgara con Mancuernas de 20 Kg en total, 85 %, 10 repeticiones → (68 + 20) × 10 = **880 Kg**
- **Y** esa carga real se usa en **todas** las vistas de tonelaje: resumen post-sesión, panel de Métricas, Volumen por grupo muscular, Tendencia, la evolución del tonelaje y la alerta de caída de tonelaje
- **Y** el peso registrado en la serie no cambia: la serie sigue guardando lo que El Ejecutante tecleó (0 Kg con el propio cuerpo, el lastre, las mancuernas), y la carga real es un cálculo

#### CA-49.02 — El 1RM estimado usa la carga real

- **Dado** que El Ejecutante registra una serie de referencia del 1RM (exactamente 10 repeticiones con RIR 1, `HU-42`) en un ejercicio con porcentaje de peso corporal mayor que 0 y un implemento que suma peso corporal
- **Cuando** el sistema estima el 1RM de esa serie
- **Entonces** la fórmula de `HU-42` se aplica sobre la **carga real** (peso corporal vigente × porcentaje + carga externa), no solo sobre la carga externa
- **Y** las series con **Peso Corporal** o **Barra Fija**, que hoy no producen 1RM, pasan a producirlo cuando su carga real es mayor que 0
- **Y** con **Peso Añadido**, el 1RM se calcula sobre el cuerpo **más** el lastre, y no sobre el lastre únicamente como hoy
- **Y** el 1RM sigue siendo el máximo histórico de las series que califican, por par ejercicio–implemento
- **Y** el récord registra la **fecha** de la sesión en que se alcanzó, porque la fuerza relativa la necesita (CA-49.03)

#### CA-49.03 — Fuerza relativa junto al 1RM

- **Dado** que un par ejercicio–implemento tiene un 1RM estimado
- **Cuando** El Ejecutante consulta la pantalla de 1RM estimado
- **Entonces** junto al valor en Kg y Lb se muestra la **fuerza relativa**: 1RM ÷ peso corporal vigente en la fecha en que se alcanzó ese 1RM, expresada como múltiplo con dos decimales (por ejemplo, **"1.25× tu peso corporal"**)
- **Y** se indica con qué **peso corporal** y de qué **fecha** se calculó
- **Y** la fuerza relativa no tiene unidad: es la misma con Kg o Lb

#### CA-49.04 — Porcentaje de peso corporal por ejercicio, editable en el Diccionario

- **Dado** que El Ejecutante consulta el detalle de un ejercicio en el Diccionario
- **Cuando** visualiza sus atributos
- **Entonces** el ejercicio expone el atributo **"Peso corporal movido"**, en porcentaje
- **Y** El Ejecutante puede editarlo desde el detalle del ejercicio, igual que la dificultad de progresión (`HU-32`)
- **Y** en una instalación fresca, el catálogo precargado trae estos valores de referencia:

| Ejercicio | Peso corporal movido |
|---|:---:|
| Dominadas | 100 % |
| Sentadilla Búlgara | 85 % |
| Zancadas (Lunges) | 85 % |
| Crunch Abdominal | 35 % |
| Resto del catálogo | 0 % |

- **Y** los ejercicios que El Ejecutante crea reciben **0 %** por defecto, editable después desde su detalle

### Escenario 2: Validaciones

#### CA-49.05 — Validación del porcentaje de peso corporal

- **Dado** que El Ejecutante edita el porcentaje de peso corporal de un ejercicio
- **Cuando** introduce un valor
- **Entonces** el sistema solo acepta **números enteros entre 0 y 100**, ambos incluidos
- **Y** un valor fuera de rango, decimal o vacío muestra un error en línea y no se guarda
- **Y** ningún ejercicio queda sin porcentaje asignado

#### CA-49.06 — El peso corporal vigente es el de la fecha, no el actual

- **Dado** que El Ejecutante tiene varias entradas en su historial de peso
- **Cuando** el sistema necesita el peso corporal de una fecha concreta, ya sea la fecha de una sesión para la carga real (CA-49.01) o la del 1RM para la fuerza relativa (CA-49.03)
- **Entonces** usa la entrada **más reciente con fecha igual o anterior** a esa fecha
- **Y** si hay varias entradas el mismo día, usa la última de ese día
- **Y** registrar un peso nuevo **no altera** la carga real de sesiones anteriores ni la fuerza relativa de 1RM ya alcanzados: solo afecta a lo que ocurra desde su fecha

### Escenario 3: Casos Extremos

#### CA-49.07 — Series que no suman peso corporal

- **Dado** que una serie se registra en alguno de estos casos:
  - el ejercicio tiene **0 %** de peso corporal;
  - el ejercicio es **isométrico**, porque se mide en segundos y no en repeticiones;
  - la serie se hace con un implemento que **no suma** peso corporal (Regla de Negocio 3), como Máquina, Polea, Banda Elástica o TRX, incluidas las **Dominadas asistidas en Máquina**
- **Cuando** el sistema calcula tonelaje y 1RM
- **Entonces** la carga es **solo la carga externa registrada**, exactamente como hoy
- **Y** en las Dominadas asistidas la serie sigue aportando 0 al tonelaje y sin producir 1RM

#### CA-49.08 — Cambiar el porcentaje recalcula el historial

- **Dado** que El Ejecutante cambia el porcentaje de peso corporal de un ejercicio con historial
- **Cuando** guarda el nuevo valor
- **Entonces** el tonelaje de **todas** las series de ese ejercicio, pasadas y futuras, se calcula con el nuevo porcentaje, para que las comparaciones en el tiempo sigan siendo coherentes
- **Y** el 1RM estimado de los pares de ese ejercicio se **recalcula** desde sus series de referencia con el nuevo porcentaje, y puede subir o bajar
- **Y** la fuerza relativa se actualiza en consecuencia

> **Decisión del PO (2026-10-22):** el porcentaje es una propiedad física del ejercicio, no del momento. Por eso un cambio corrige todo el historial y no solo lo que ocurra desde ese día. Se descartó la alternativa de aplicarlo solo hacia adelante, porque el gráfico mostraría un salto que no es real.

#### CA-49.09 — El motor de decisión no cambia

- **Dado** que la carga real y el peso corporal están disponibles
- **Cuando** el sistema evalúa la progresión de un ejercicio, la señal de subir o bajar peso, la meseta, la regresión, la fatiga o la descarga
- **Entonces** el resultado es **idéntico** al de hoy: ninguna de esas reglas lee el peso corporal, el porcentaje, la carga real ni la fuerza relativa
- **Y** los ejercicios con el propio cuerpo siguen progresando por repeticiones (RF-31)
- **Y** las cargas prescritas y precargadas en el registro de serie siguen siendo la carga externa, no la carga real

#### CA-49.10 — El respaldo conserva el porcentaje y la fecha del 1RM

- **Dado** que la aplicación se instala desde cero con esta historia
- **Cuando** El Ejecutante exporta un respaldo y lo importa en otro dispositivo
- **Entonces** el porcentaje de peso corporal de cada ejercicio y la fecha de cada 1RM se restauran íntegros
- **Y** el tonelaje, el 1RM y la fuerza relativa restaurados son idénticos a los del dispositivo de origen

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante, actor único del sistema. Aplicación de un solo usuario y sin roles.
- **Permisos requeridos:** ninguno. Operación 100 % local en el dispositivo.
- **Valor de negocio:** el tonelaje y el 1RM dejan de ignorar el cuerpo del ejecutante en dominadas, búlgaras o zancadas. Pasar del peso corporal a mancuernas ya no hunde el tonelaje de forma artificial, y la fuerza relativa da una lectura de fuerza comparable aunque el peso corporal cambie.

### Reglas de Negocio

1. **Peso corporal vigente en una fecha:** la entrada del historial de peso más reciente con fecha igual o anterior. Si hay varias el mismo día, la última. Registrar un peso nuevo no reescribe el pasado.
2. **Porcentaje de peso corporal:** atributo de cada ejercicio, entero de 0 a 100. Valores precargados: Dominadas 100, Sentadilla Búlgara 85, Zancadas 85, Crunch Abdominal 35, resto 0. Ejercicios creados por El Ejecutante: 0. Editable desde el detalle del ejercicio.
3. **Implementos que suman peso corporal** (peso libre o propio cuerpo): Peso Corporal, Barra Fija, Peso Añadido, Mancuerna, Barra, Barra EZ, Máquina Smith, Pesa Rusa, Balón Medicinal, Paralelas/Dip Station y Rodillo de Abdomen. **No suman:** Máquina, Polea, Banda Elástica y TRX/Suspensión, porque la máquina guía o soporta el cuerpo, la resistencia no es un peso, o el cuerpo está asistido en un ángulo variable.
4. **Carga real de una serie** = peso corporal vigente en la fecha de la sesión × porcentaje + carga externa registrada. Solo aplica si el porcentaje es mayor que 0, el ejercicio no es isométrico y el implemento suma peso corporal (regla 3). En cualquier otro caso, carga real = carga externa.
5. **La carga real alimenta el tonelaje en todas sus vistas** (resumen post-sesión, Métricas, Volumen, Tendencia, evolución del tonelaje y alerta de caída de tonelaje) **y el 1RM estimado**.
6. **1RM:** misma fórmula y misma serie de referencia que `HU-42` (10 repeticiones, RIR 1), aplicadas a la carga real. Las series con Peso Corporal y Barra Fija califican si la carga real es mayor que 0. El récord guarda la fecha de la sesión en que se alcanzó.
7. **Fuerza relativa** = 1RM ÷ peso corporal vigente en la fecha del 1RM, con dos decimales y sin unidad.
8. **Cambio de porcentaje:** aplica a todo el historial del ejercicio. El tonelaje y el 1RM se recalculan (CA-49.08).
9. **El motor de decisión no cambia:** la progresión, las señales de subir o bajar, la meseta, la regresión, la fatiga, la descarga y las cargas prescritas no leen nada de esta historia.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| 1RM estimado | `O1` | Fuerza relativa junto al valor en Kg y Lb de cada par, con el peso corporal y la fecha usados |
| Detalle de ejercicio | `D2` | Nuevo atributo editable **"Peso corporal movido (%)"**. No se muestra en ejercicios isométricos |
| Resumen post-sesión, Métricas, Volumen, Tendencia, Detalle de alerta | `E5`, `G1`, `G2`, `G3`, `H2` | Sin cambios de diseño. Sus valores de tonelaje pasan a incluir la carga real |

#### Detalle de Interfaz de Usuario

- **Diseño general:** no hay pantallas nuevas. Se añaden un dato a cada tarjeta de 1RM (O1) y un atributo al detalle de ejercicio (D2).
- **Campos y controles:**
  - **O1:** en cada tarjeta de par, bajo el 1RM en Kg y Lb, la línea **"N.NN× tu peso corporal"** y, en texto secundario, "con X Kg el DD/MM/AAAA".
  - **D2:** campo **"Peso corporal movido"** con sufijo %, numérico entero de 0 a 100, editable con guardado explícito, como la dificultad de progresión.
- **Flujo de navegación visual:** `Métricas → 1RM estimado → tarjeta del par → fuerza relativa`. `Diccionario → Detalle de ejercicio → Peso corporal movido → Guardar`.
- **Mensajes y feedback:**
  - error en línea de porcentaje fuera de 0–100;
  - confirmación breve al guardar el porcentaje.

### Sistemas Externos

Ninguno. El sistema es local y sin integraciones.

### Disparadores

- El Ejecutante abre la pantalla de 1RM estimado.
- El Ejecutante guarda el porcentaje de peso corporal de un ejercicio en el Diccionario. Se puede repetir; cada cambio recalcula el tonelaje y el 1RM del ejercicio.
- El Ejecutante registra una serie en la sesión activa. La carga real interviene en el 1RM de la serie de referencia.
- El Ejecutante corrige la última serie registrada (`HU-47`). La carga real interviene en el 1RM recalculado.
- El Ejecutante cierra una sesión. La carga real interviene en el tonelaje del resumen y en la alerta de caída de tonelaje.
- El Ejecutante consulta Métricas, Volumen o Tendencia. Se puede repetir las veces que quiera.
- El Ejecutante exporta o importa un respaldo.

### Entidades del Dominio

- Ejercicio (gana el porcentaje de peso corporal movido)
- Tipo de Equipamiento (implemento: decide si la serie suma peso corporal)
- Serie (la carga externa registrada no cambia; su carga real se calcula)
- Registro de Peso Corporal (fuente del peso vigente por fecha)
- Récord de 1RM Estimado (se calcula sobre la carga real y gana la fecha en que se alcanzó)
- Respaldo (gana el porcentaje y la fecha del 1RM)

### Alcance y Continuidad

- **Esta funcionalidad modifica procesos que ya existen; no crea uno nuevo desde cero.** Cambia el cálculo del tonelaje (`HU-15`) y la alerta de caída de tonelaje (`HU-18`) para usar la carga real. Cambia el 1RM estimado (`HU-42`) para calcularse sobre la carga real y guardar su fecha. Usa como fuente el historial de peso corporal que ya existe (`HU-02`).
- **El porcentaje de peso corporal es un atributo nuevo del Ejercicio existente**, no una entidad distinta, igual que la dificultad de progresión de `HU-32`.
- **La fecha en que se alcanzó el 1RM es un atributo nuevo del récord de 1RM existente**, no una entidad distinta.
- **Frontera:** todo ocurre dentro de la aplicación, sin sistemas externos ni conexión. No se registra ni calcula composición corporal.

### Preview de Interfaz

**Preview:** [`49.preview.txt`](./49.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/domain/definition/requirements_specification.md`: RF-31 (progresión por repeticiones en peso corporal: se mantiene), RF-57 (alerta de caída de tonelaje: pasa a leer la carga real).
- `docs/domain/definition/system_definition_document.md`: §2.1, exclusión de la composición corporal (se respeta: el peso corporal solo completa la carga movida).
- `docs/architecture/domain_and_state_model.md`: `exercise` (nuevo porcentaje), `exercise_one_rm` (nueva fecha), `weight_record`, `exercise_set`.
- `docs/architecture/interfaces_contract.md`: triggers `D2-T1`, `E2-T1`, `E2-T2`, `E4-T1`, `E5-T1`, `G1-T1`, `G2-T1`, `G3-T1`, `H2-T1`, `O1-T1`, `J2-T1`, `J3-T1`.
- `docs/architecture/architecture_blueprint.md`: ADR-019 (esquema sin migración durante la beta).

**Historias relacionadas:**
- `HU-48` (hermana): perfil sin nivel de experiencia y peso corporal en Métricas. Independiente: su registro rápido de peso es una fuente más del historial que esta historia lee.
- `HU-02`: historial de peso corporal. Fuente del peso vigente por fecha.
- `HU-15`, `HU-18`: tonelaje y alerta de caída de tonelaje. Pasan a leer la carga real.
- `HU-32`: dificultad de progresión editable en el Diccionario. Precedente directo del porcentaje como atributo editable del ejercicio.
- `HU-39`, `HU-40`: implemento por serie y progresión por par. El implemento decide si la serie suma peso corporal.
- `HU-42`: 1RM estimado. Pasa a calcularse sobre la carga real y a guardar la fecha.
- `HU-47`: corrección de la última serie. La corrección recalcula también la carga real y el 1RM derivados.

**Lecciones aprendidas:**
- `HU-37` y `HU-42` fijaron que las representaciones derivadas (árbol, 1RM) no alimentan el motor de decisión. Esta historia sigue esa disciplina: el peso corporal, la carga real y la fuerza relativa informan; no deciden.
- `HU-42` advierte que el 1RM es un dato persistido que, si se escribe mal, queda mal. Esta historia lo vuelve a escribir al cambiar el porcentaje (CA-49.08): el recálculo debe partir siempre de las series de referencia.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente (CA-49.05)
- [ ] Mensajes implementados
- [ ] Verificado el cálculo de la carga real con los tres ejemplos de CA-49.01 y con las exclusiones de CA-49.07
- [ ] Verificado que el peso vigente es el de la fecha y que un peso nuevo no reescribe el pasado (CA-49.06)
- [ ] Verificado el 1RM sobre la carga real, incluidas las series con Peso Corporal y Barra Fija, y la fecha del récord (CA-49.02)
- [ ] Verificado que cambiar el porcentaje recalcula el tonelaje y el 1RM del ejercicio (CA-49.08)
- [ ] Verificado que el motor de decisión produce resultados idénticos a los actuales (CA-49.09)
- [ ] Documentación actualizada: `domain_and_state_model.md`, `interfaces_contract.md`, RF-57
- [ ] `story_mapping_index.md` actualizado con la historia y sus relaciones
