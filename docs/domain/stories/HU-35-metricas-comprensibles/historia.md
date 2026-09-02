# Historia de Usuario

**Como** El Ejecutante,
**Quiero** entender qué significa cada número que la aplicación me muestra en sus métricas, en qué unidad está y sobre qué período se calculó,
**Para** poder usar mi analítica para tomar decisiones de entrenamiento en lugar de ignorarla porque no sé qué me está diciendo.

## Descripción

La aplicación calcula una batería completa de indicadores de entrenamiento (HU-15): tonelaje, volumen por grupo muscular, tendencias, tasa de progresión, adherencia. El cálculo es correcto, pero la presentación no comunica: los números aparecen sin explicar qué representan, sobre qué ventana se calcularon ni en qué unidad están.

El resultado es que una capacidad valiosa del producto queda infrautilizada, porque interpretar cada pantalla exige recordar cómo se define cada indicador.

Esta historia no cambia ningún cálculo. Cambia **cómo se presentan**: cada indicador se explica a sí mismo, los indicadores se agrupan y jerarquizan visualmente, y cuando no hay datos suficientes el sistema lo dice en lugar de mostrar un cero engañoso.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-35.01 — Cada métrica se explica a sí misma

- **Dado** que El Ejecutante consulta las pantallas de Métricas, Volumen o Tendencia
- **Cuando** visualiza un indicador numérico
- **Entonces** el indicador muestra su **etiqueta**, su **valor**, su **unidad** y una **descripción breve** de qué representa
- **Y** El Ejecutante puede interpretarlo sin consultar documentación externa

#### CA-35.02 — Jerarquía visual

- **Dado** que una pantalla de métricas presenta múltiples indicadores
- **Cuando** El Ejecutante la abre
- **Entonces** cada indicador se presenta como una **tarjeta** que compone, en este orden: etiqueta, valor con su unidad, descripción breve y período
- **Y** el valor es el elemento tipográficamente dominante de la tarjeta
- **Y** las tarjetas se agrupan en secciones con encabezado temático

#### CA-35.03 — Unidad y período visibles

- **Dado** que un indicador depende de un período de evaluación o de una unidad de medida
- **Cuando** El Ejecutante lo visualiza
- **Entonces** el período y la unidad se muestran junto al valor
- **Y** todas las cargas y tonelajes se expresan en kilogramos

### Escenario 2: Validaciones

#### CA-35.04 — Datos insuficientes

- **Dado** que no existe historial suficiente para calcular un indicador
- **Cuando** El Ejecutante abre la pantalla que lo contiene
- **Entonces** el sistema muestra un mensaje explícito indicando qué falta para poder calcularlo
- **Y** no muestra cero, guiones ni un valor engañoso

#### CA-35.05 — Los cálculos no cambian

- **Dado** que esta historia interviene sobre la presentación
- **Cuando** se comparan los valores mostrados antes y después del cambio
- **Entonces** los resultados de todos los indicadores son idénticos
- **Y** ninguna regla de cálculo de HU-15 sufre modificación

### Escenario 3: Casos Extremos

#### CA-35.06 — Gráficas legibles

- **Dado** que una pantalla presenta una gráfica de tendencia o de tonelaje
- **Cuando** El Ejecutante la visualiza
- **Entonces** la gráfica muestra su unidad, el período representado y el significado de sus ejes
- **Y** es legible en pantallas de 5 pulgadas y en tema claro y oscuro

#### CA-35.07 — Indicador con valor cero legítimo

- **Dado** que un indicador tiene datos suficientes y su valor calculado es efectivamente cero
- **Cuando** El Ejecutante lo visualiza
- **Entonces** se muestra el cero como valor válido
- **Y** se distingue visualmente del estado de datos insuficientes

#### CA-35.08 — Documentación actualizada

- **Dado** que cambia la presentación de las pantallas de analítica
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/interfaces_contract.md` refleja la estructura de presentación de los indicadores, incluyendo etiqueta, unidad, descripción, período y estado de datos insuficientes

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** La analítica es una de las capacidades más costosas del sistema y una de las menos usadas, precisamente porque no se entiende. Hacerla legible no añade funcionalidad nueva: activa la que ya existe.

### Reglas de Negocio

1. **Un indicador que necesita explicación externa no está terminado.** Etiqueta, valor, unidad y descripción son parte del indicador, no adorno.
2. **Sin dato no hay número.** Cuando falta historial, el sistema lo declara. Nunca rellena con cero ni con guiones.
3. **El cero calculado y el dato ausente son estados distintos** y deben distinguirse visualmente.
4. **Kilogramo como unidad de presentación.** Todas las cargas y tonelajes se expresan en kilogramos, con independencia de la unidad de captura.
5. **Esta historia no toca cálculos.** Cualquier discrepancia numérica respecto al comportamiento previo es un defecto.

### Interfaz

No se crea ninguna pantalla nueva. Se rediseña la presentación de las tres pantallas de analítica.

- **Métricas:** agrupación temática de indicadores, jerarquía visual, etiqueta + valor + unidad + descripción en cada uno.
- **Volumen:** mismo tratamiento; período y unidad visibles en el volumen por grupo muscular.
- **Tendencia:** mismo tratamiento; ejes de la gráfica etiquetados con su significado y unidad.
- **Todas:** estado explícito de datos insuficientes.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Se conserva la navegación y el sistema visual. Cambia la composición interna de las pantallas de analítica: cada indicador se presenta como **tarjeta**, y las tarjetas se agrupan en secciones con encabezado temático.

  ```
  TONELAJE
  ────────────────────────────────
  ┌──────────────┐ ┌──────────────┐
  │ Total semanal│ │ Por sesión   │   ← etiqueta
  │              │ │              │
  │   12 480 kg  │ │   2 080 kg   │   ← valor dominante + unidad
  │              │ │              │
  │ Peso movido  │ │ Promedio de  │   ← descripción breve
  │ en 7 días    │ │ tus sesiones │
  │ ············ │ │ ············ │
  │ últimos 7 d  │ │ últimos 30 d │   ← período (pie)
  └──────────────┘ └──────────────┘
  ```

- **Campos y controles:** Ningún control nuevo. Cada tarjeta compone cuatro elementos en este orden: **etiqueta** (arriba), **valor dominante con su unidad** (centro, tipografía de mayor tamaño de la tarjeta), **descripción breve** (debajo) y **período** (pie, tipografía secundaria).
- **Flujo de navegación visual:** Sin rutas nuevas.
- **Mensajes y feedback:** Mensaje explícito de datos insuficientes por indicador, indicando qué falta (ej. número de sesiones o semanas necesarias).

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14).

### Preview de Interfaz

**Preview:** [`35.preview.txt`](./35.preview.txt) | **Formato:** ASCII (wireframe de texto)

Aplican los mockups existentes de Métricas, Volumen y Tendencia, con la composición y el etiquetado descritos.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/interfaces_contract.md`, `docs/architecture/coding-standards.md`

**Historias relacionadas:**

- **HU-15** — Analítica y KPIs del Entrenamiento. Esta historia rediseña su presentación sin alterar sus cálculos.
- **HU-13** — Resumen post-sesión con señales de acción. Comparte criterio de legibilidad.
- **HU-30** — Captura de carga en kg/lb. Establece el kilogramo como unidad de presentación de todo agregado.
- **HU-28** — Legibilidad de nombres y títulos largos. Comparte el criterio de tratamiento de texto.

**Restricciones transversales aplicables:**

- RNF01 — Operaciones de cálculo no bloquean la interfaz
- RNF05 — Señales del sistema visibles y comprensibles
- RNF08 — Interfaz en español
- RNF21 — Pantallas de 5" a 7", layout responsivo
- RNF23 — Tema claro/oscuro automático
- **Beta sin migración:** la base de datos se reinicia; no se requiere migración de datos.

**Gap conocido:** no se identificó un indicador concreto que resulte especialmente confuso hoy. Los criterios de esta historia son transversales y aplican a todos por igual. Si durante el refinamiento aparece un indicador puntualmente problemático, se incorporará como criterio de aceptación adicional.

**Lecciones aprendidas:** Un indicador correcto pero incomprensible tiene el mismo valor de uso que uno inexistente, con todo su coste de cómputo y mantenimiento.

---

## Definición de Terminado (Inicial)

- [ ] Todos los indicadores de Métricas, Volumen y Tendencia con etiqueta, valor, unidad y descripción
- [ ] Cada indicador presentado como tarjeta (etiqueta / valor dominante + unidad / descripción / período), agrupadas en secciones con encabezado temático
- [ ] Período y unidad visibles en todo indicador que dependa de ellos
- [ ] Cargas y tonelajes expresados en kilogramos
- [ ] Estado explícito de datos insuficientes, distinguible del cero calculado
- [ ] Gráficas con ejes etiquetados, unidad y período; legibles en 5" y en tema claro y oscuro
- [ ] Verificado que ningún valor calculado cambia respecto al comportamiento previo
- [ ] `interfaces_contract.md` actualizado
