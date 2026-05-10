# Wireframes de Baja Fidelidad — Tension

---

## 1. Propósito

Este documento define los wireframes de baja fidelidad de las 27 vistas de la aplicación Tension. Para cada pantalla se especifica:

- **Estructura visual:** Disposición de los elementos en la pantalla (header, body, footer).
- **Contenido:** Qué información y controles contiene cada zona.
- **Interacciones:** Qué acciones puede realizar el ejecutante sobre los elementos.
- **Estados:** Variaciones visuales según el estado del dato o del sistema.

No se definen estilos, colores ni tipografías (eso corresponde a mockups de alta fidelidad). El foco está en **qué aparece** y **dónde aparece**.

---

## 2. Convenciones

- Los wireframes se describen textualmente como **layout vertical** (de arriba hacia abajo), simulando la pantalla de un dispositivo móvil.
- **[ Elemento ]** denota un componente interactivo (botón, campo, selector).
- **{ Zona }** denota una región o contenedor visual.
- **(Texto)** denota contenido textual informativo.
- **``---``** denota un separador visual horizontal.
- Las posiciones se indican con las zonas: **Top Bar**, **Body** (scrollable), **Bottom Bar** (fija).
- La **Bottom Navigation** se incluye en todas las pantallas donde aplica (excluida en A1 y E1-E5).

---

## 3. Wireframes por Vista

---

#### A1 — Registro de Perfil

**Contexto:** Primera pantalla que ve el ejecutante al abrir la app sin perfil registrado. No tiene Bottom Navigation ni botón de retorno. Es un paso obligatorio.

**HU de referencia:** HU-01 (CA-01.01 a CA-01.06)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│          [ Logo 96×96 dp ]           │
│  (Nombre: "Tension")                 │
│  (Subtítulo: "Configura tu perfil")  │
│                                      │
├──────────────────────────────────────┤
│              BODY                    │
│                                      │
│  { Formulario de Perfil }            │
│                                      │
│  (Label: "Peso corporal (Kg)")       │
│  [ Campo numérico: _____ Kg ]        │
│    → Teclado numérico al activar     │
│    → Validación: > 0                 │
│    → Error inline si valor inválido  │
│                                      │
│  (Label: "Altura (m)")              │
│  [ Campo numérico: _____ m ]         │
│    → Teclado numérico al activar     │
│    → Validación: > 0                 │
│    → Error inline si valor inválido  │
│                                      │
│  (Label: "Nivel de experiencia")     │
│  [ Selector desplegable ]            │
│    ▼ Seleccionar nivel               │
│      · Principiante                  │
│      · Intermedio                    │
│      · Avanzado                      │
│    → Obligatorio, selección única    │
│                                      │
│  ---                                 │
│                                      │
│  [ Botón primario: "Registrar" ]     │
│    → Habilitado solo si todos los    │
│      campos son válidos              │
│    → Al tocar: persiste perfil y     │
│      navega a B1 (Home)              │
│                                      │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Logo | Imagen (120 dp) | Top Bar, centrado | Logotipo oficial de Tension (`logo.png`) |
| 2 | Nombre "Tension" | Texto estático | Top Bar, bajo logo, centrado | Identidad de la app, estilo headlineLarge, color Primary |
| 3 | Subtítulo "Configura tu perfil" | Texto estático | Top Bar, bajo nombre | Contexto de la pantalla |
| 4 | Campo "Peso corporal" | Input numérico | Body, primer campo | Teclado numérico. Validación > 0. Sufijo "Kg". Mensaje de error inline bajo el campo si inválido |
| 5 | Campo "Altura" | Input numérico | Body, segundo campo | Teclado numérico. Validación > 0. Sufijo "m". Mensaje de error inline bajo el campo si inválido |
| 6 | Selector "Nivel de experiencia" | Dropdown desplegable (3 opciones) | Body, tercer campo | ExposedDropdownMenuBox M3. Opciones mutuamente excluyentes: Principiante, Intermedio, Avanzado. Obligatorio |
| 7 | Botón "Registrar" | Botón primario | Body, final del formulario | Deshabilitado visualmente hasta que los 3 campos sean válidos. Al confirmar: persiste datos → navega a B1 |

**Estados:**

- **Inicial:** Campos vacíos, botón deshabilitado.
- **Parcialmente completo:** Campos con datos pero botón deshabilitado si falta alguno o hay error de validación.
- **Listo:** Los 3 campos válidos, botón habilitado.
- **Error:** Mensaje inline rojo bajo el campo que tiene valor inválido.

---

#### B1 — Pantalla Principal (Home)

**Contexto:** Punto de entrada recurrente del ejecutante tras el onboarding. Contiene accesos rápidos a las funcionalidades principales y muestra el estado actual del entrenamiento. Incluye Bottom Navigation.

**HU de referencia:** HU-05 (CA-05.01, CA-05.02, CA-05.06, CA-05.07), HU-18 (CA-18.05)
**HU indirectas visibles:** HU-14, HU-16 (detección necesidad descarga), HU-17 (gestión descarga), HU-26–HU-30 (badge de alertas + Card Estado de Descarga)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  (Nombre: "Tension")     [ 🔔 3 ]   │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Card: Próxima Sesión }            │
│  ┌────────────────────────────────┐  │
│  │ (Rutina 2 — Versión 1)        │  │
│  │ ("Tu próxima sesión")         │  │
│  │                                │  │
│  │ [ Botón primario:             │  │
│  │   "Iniciar Sesión" ]          │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Card: Reanudar Sesión }           │
│  ┌────────────────────────────────┐  │
│  │ ⚠️ ("Tienes una sesión        │  │
│  │  activa sin cerrar")          │  │
│  │ (Rutina 1 — Versión 1)        │  │
│  │ (3 de 11 ejercicios           │  │
│  │  completados)                 │  │
│  │                                │  │
│  │ [ Botón destacado:            │  │
│  │   "Reanudar Sesión" ]         │  │
│  └────────────────────────────────┘  │
│  → Solo visible si hay sesión        │
│    activa pendiente (crash recovery) │
│                                      │
│  ---                                 │
│                                      │
│  { Sección: Progreso }               │
│  ┌────────────────────────────────┐  │
│  │ (Microciclos completados)     │  │
│  │ (     "12"     )              │  │
│  │ ("microciclos")               │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Card: Estado de Descarga }        │
│  ┌────────────────────────────────┐  │
│  │ ("🔄 Descarga activa")        │  │
│  │ ("Rutina 3 — Sesión 3 de 3")  │  │
│  │                                │  │
│  │ [ Enlace: "Ver gestión        │  │
│  │   de descarga →" ]            │  │
│  └────────────────────────────────┘  │
│  → Solo visible si hay descarga      │
│    activa o rutina que la requiere   │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│                                      │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│  (●)                                │
│                                      │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Nombre "Tension" | Texto estático | Top Bar, izquierda | Identidad de la app |
| 2 | Badge de alertas "🔔 N" | Botón con badge | Top Bar, derecha | Muestra conteo de alertas activas (siempre visible, "0" si no hay). Al tocar → navega a H1 |
| 3 | Card "Próxima Sesión" | Card interactiva | Body, primera sección | Muestra rutina y versión determinados por la rotación cíclica definida por el usuario. Contiene botón "Iniciar Sesión" que al tocar → navega a E1 |
| 4 | Card "Reanudar Sesión" | Card condicional | Body, sobre la card de próxima sesión | Solo visible si hay sesión activa no cerrada (crash recovery). Muestra rutina/versión y progreso parcial. Botón "Reanudar Sesión" → navega a E1 con sesión existente. Cuando es visible, se muestra antes de la Card de Próxima Sesión con estilo prominente |
| 5 | Contador de microciclos | Indicador numérico | Body, sección "Progreso" | Muestra número total de microciclos completados (HU-18). Solo lectura |
| 6 | Card "Estado de Descarga" | Card condicional | Body, después de Progreso | Solo visible si descarga activa o rutina que requiere descarga. Muestra estado y progreso. Enlace "Ver gestión de descarga" → navega a I1 |
| 7 | Bottom Navigation | Barra de navegación fija | Bottom Bar | 5 secciones: Inicio(●), Diccionario, Historial, Métricas, Ajustes. Inicio marcado como activo |

**Estados de la pantalla:**

| Estado | Variación visual |
|--------|------------------|
| Primera sesión (sin historial) | Card "Próxima Sesión" muestra la primera rutina y versión del plan. Sin card de reanudar. Sin card de descarga. Microciclos = 0. Badge alertas = 0 |
| Uso normal | Card "Próxima Sesión" con rutina/versión calculado según rotación del usuario. Microciclos ≥ 1. Badge alertas con conteo real |
| Sesión activa pendiente (crash) | Card "Reanudar Sesión" visible y prominente encima de la Card de Próxima Sesión. La Card de Próxima Sesión se oculta mientras haya sesión pendiente |
| Descarga activa | Card "Estado de Descarga" visible con progreso del microciclo de descarga |
| Descarga requerida (no activa) | Card "Estado de Descarga" visible con indicación de que una rutina requiere descarga y enlace a I1 |

---

#### C1 — Perfil del Ejecutante

**Contexto:** Vista de consulta y edición del perfil personal. Accesible únicamente desde J1 (Ajustes → "Editar perfil"). Incluye Bottom Navigation.

**HU de referencia:** HU-01 (CA-01.07 a CA-01.09)
**HU indirecta:** HU-02 (CA-02.01 — registro automático en historial al actualizar peso)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Mi Perfil")       │
│                                      │
├──────────────────────────────────────┤
│              BODY                    │
│                                      │
│          [ Logo 120×120 dp ]          │
│                                      │
│  { Formulario de Perfil }            │
│                                      │
│  (Label: "Peso corporal (Kg)")       │
│  [ Campo numérico: 78.5 Kg ]        │
│    → Precargado con valor actual     │
│    → Teclado numérico al activar     │
│    → Validación: > 0                 │
│                                      │
│  (Label: "Altura (m)")              │
│  [ Campo numérico: 1.75 m ]         │
│    → Precargado con valor actual     │
│    → Teclado numérico al activar     │
│    → Validación: > 0                 │
│                                      │
│  (Label: "Nivel de experiencia")     │
│  [ Selector desplegable ]            │
│    ▼ Intermedio ← seleccionado        │
│      · Principiante                  │
│      · Intermedio                    │
│      · Avanzado                      │
│                                      │
│  ---                                 │
│                                      │
│  [ Botón primario: "Guardar" ]       │
│    → Habilitado solo si hay cambios  │
│      y todos los campos son válidos  │
│    → Al tocar: persiste cambios,     │
│      si peso cambió → registra       │
│      entrada en historial de peso    │
│                                      │
│  ---                                 │
│                                      │
│  [ Enlace: "Ver historial de         │
│    peso →" ]                         │
│    → Navega a C2                     │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                                (●)   │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a J1 (Ajustes) |
| 2 | Título "Mi Perfil" | Texto estático | Top Bar, centrado | Identifica la pantalla |
| 3 | Logo | Imagen (120 dp) | Body, centrado, sobre el formulario | Logotipo oficial de Tension (`logo.png`). Coherencia visual con onboarding |
| 4 | Campo "Peso corporal" | Input numérico | Body, primer campo | Precargado con valor actual. Teclado numérico. Validación > 0. Sufijo "Kg" |
| 5 | Campo "Altura" | Input numérico | Body, segundo campo | Precargado con valor actual. Teclado numérico. Validación > 0. Sufijo "m" |
| 6 | Selector "Nivel de experiencia" | Dropdown desplegable (3 opciones) | Body, tercer campo | ExposedDropdownMenuBox M3. Precargado con valor actual. Principiante / Intermedio / Avanzado |
| 7 | Botón "Guardar" | Botón primario | Body, bajo el formulario | Deshabilitado si no hay cambios o hay errores de validación. Al confirmar: persiste datos. Si el peso cambió, el sistema registra automáticamente una entrada en el historial de peso (HU-02 CA-02.01) |
| 8 | Enlace "Ver historial de peso" | Link de navegación | Body, bajo el botón Guardar | Navega a C2. Siempre visible independientemente de si se editaron datos |
| 9 | Bottom Navigation | Barra fija | Bottom Bar | Ajustes marcado como activo (se llega desde J1) |

**Estados:**

- **Sin cambios:** Campos precargados con datos actuales. Botón "Guardar" deshabilitado.
- **Con cambios válidos:** Al menos un campo modificado con valor válido. Botón "Guardar" habilitado.
- **Error de validación:** Mensaje inline rojo bajo el campo inválido. Botón "Guardar" deshabilitado.

---

#### C2 — Historial de Peso Corporal

**Contexto:** Lista cronológica de todas las entradas de peso registradas. Accesible desde C1 (enlace "Ver historial de peso"). Incluye Bottom Navigation.

**HU de referencia:** HU-02 (CA-02.02 a CA-02.05)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Historial         │
│           de Peso")                  │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Lista de entradas }               │
│                                      │
│  ┌────────────────────────────────┐  │
│  │  (10 feb 2026)       78.5 Kg  │  │
│  ├────────────────────────────────┤  │
│  │  (28 ene 2026)       79.0 Kg  │  │
│  ├────────────────────────────────┤  │
│  │  (15 ene 2026)       80.2 Kg  │  │
│  ├────────────────────────────────┤  │
│  │  (01 ene 2026)       81.0 Kg  │  │
│  │  (Registro inicial)           │  │
│  └────────────────────────────────┘  │
│                                      │
│  → Ordenado de más reciente a        │
│    más antiguo                       │
│  → Cada fila: fecha + peso en Kg     │
│  → La primera entrada del perfil     │
│    se marca como "Registro inicial"  │
│                                      │
│  { Estado vacío }                    │
│  → Si solo hay el registro inicial:  │
│    se muestra la única entrada con   │
│    la etiqueta "Registro inicial"    │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                                (●)   │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a C1 (Perfil) |
| 2 | Título "Historial de Peso" | Texto estático | Top Bar, centrado | Identifica la pantalla |
| 3 | Lista de entradas de peso | Lista de solo lectura | Body, scrollable | Una fila por entrada. Cada fila: fecha (dd mmm yyyy) a la izquierda, peso (Kg) a la derecha. Ordenada cronológicamente de más reciente a más antigua. La entrada del registro inicial se etiqueta como "Registro inicial" |
| 4 | Bottom Navigation | Barra fija | Bottom Bar | Ajustes marcado como activo |

**Estados:**

- **Con historial:** Múltiples entradas listadas cronológicamente. Se puede hacer scroll si la lista es larga.
- **Solo registro inicial:** Una única entrada con fecha de creación del perfil y la etiqueta "Registro inicial" (CA-02.04).

---

#### D1 — Diccionario de Ejercicios

**Contexto:** Listado filtrable de los 43 ejercicios precargados. Comparte sección "Diccionario" del Bottom Navigation con D3 (Plan de Entrenamiento) mediante tabs superiores. Incluye Bottom Navigation.

**HU de referencia:** HU-03 (CA-03.01 a CA-03.06)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  (Título: "Diccionario")             │
│                                      │
│  { Tabs }                            │
│  [ Ejercicios (●) ] [ Plan ]        │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Barra de filtros — 1 fila }       │
│  ┌─────────────────────────────┐  │
│  │ Equipo ▼  │ Zona ▼            │  │
│  │ (Todos)   │ (Todos)           │  │
│  └─────────────────────────────┘  │
│  → 2 dropdowns compactos en 1 fila   │
│  → Filtros combinables               │
│  → Al seleccionar: se filtra la      │
│    lista en tiempo real              │
│  → Opción "Todos" para limpiar       │
│    cada filtro                       │
│                                      │
│  (Mostrando N de 43 ejercicios)      │
│                                      │
│  { Lista de ejercicios }             │
│  ┌────────────────────────────────┐  │
│  │ (Press de banca)              │  │
│  │ Máquina · Pecho Medio           │  │
│  ├────────────────────────────────┤  │
│  │ (Press de mancuerna)          │  │
│  │ Mancuernas · Pecho Medio       │  │
│  ├────────────────────────────────┤  │
│  │ (Flexiones)                   │  │
│  │ Cuerpo · Pecho Inferior        │  │
│  ├────────────────────────────────┤  │
│  │       ...más ejercicios...    │  │
│  └────────────────────────────────┘  │
│  → Cada fila al tocar → navega a D2  │
│                                      │
│  { Estado: sin resultados }          │
│  → Si la combinación de filtros no   │
│    arroja resultados: "No hay        │
│    ejercicios que coincidan con      │
│    los filtros seleccionados"        │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│           (●)                        │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Título "Diccionario" | Texto estático | Top Bar | Identifica la sección |
| 2 | Tabs "Ejercicios" / "Plan" | Tab bar | Top Bar, bajo título | Dos tabs: "Ejercicios" (activo = D1) y "Plan" (al tocar → D3). Navegación lateral dentro de la sección |
| 3 | Filtro "Equipo" | ExposedDropdownMenuBox (OutlinedTextField read-only) | Body, barra de filtros, columna 1 de 2 | Label: "Equipo". Opciones: Todos, Máquina, Mancuerna, Mancuernas, Barra de Pesas, Cuerpo, Polea, Pesa, Mancuerna o Pesa Rusa, Máquina Multiestación. Valor por defecto: "Todos" |
| 4 | Filtro "Zona muscular" | ExposedDropdownMenuBox (OutlinedTextField read-only) | Body, barra de filtros, columna 2 de 2 | Label: "Zona". Opciones: Todos + todas las zonas musculares únicas del diccionario. Valor por defecto: "Todos" |
| 5 | Contador de resultados | Texto dinámico | Body, bajo filtros | "Mostrando N de T ejercicios" (T = total en diccionario, se actualiza dinámicamente). Se actualiza al aplicar filtros |
| 6 | Lista de ejercicios | Lista interactiva | Body, scrollable | Cada fila: nombre (prominente), línea secundaria con tipo de equipo + zona muscular separados por "·". Ejercicios creados por el ejecutante muestran badge "Personalizado". Al tocar una fila → navega a D2 (Detalle de Ejercicio) |
| 7 | Botón crear ejercicio (FAB) | FloatingActionButton | Bottom-end, sobre Bottom Nav | Ícono "+" (Add). Al tocar → navega a formulario de creación de ejercicio (CA-03.10). Visible siempre |
| 8 | Bottom Navigation | Barra fija | Bottom Bar | Diccionario marcado como activo |

**Estados:**

- **Sin filtros:** Muestra todos los ejercicios (precargados + creados por el ejecutante).
- **Con filtros activos:** Muestra solo los que cumplen todos los filtros. Contador actualizado.
- **Sin resultados:** Mensaje "No hay ejercicios que coincidan con los filtros seleccionados".

---

#### D2 — Detalle de Ejercicio

**Contexto:** Ficha completa de un ejercicio. Vista reutilizable accesible desde D1, D4, E1 y F3. Preserva el contexto de origen para el retorno. Incluye Bottom Navigation (excepto si se accede desde E1, donde no hay Bottom Nav).

**HU de referencia:** HU-03 (CA-03.07, CA-03.08)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: nombre del         │
│           ejercicio)                 │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Media visual }                    │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  │   [ Imagen PNG 3D ]            │  │
│  │   (Ilustración de la          │  │
│  │    ejecución correcta          │  │
│  │    del movimiento)             │  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Información del ejercicio }       │
│  ┌────────────────────────────────┐  │
│  │ (Nombre)                      │  │
│  │  "Press de banca"             │  │
│  │                                │  │
│  │ (Tipo de equipo)              │  │
│  │  "Máquina"                    │  │
│  │                                │  │
│  │ (Zona muscular)               │  │
│  │  "Pecho Medio"                │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  [ Enlace: "Ver historial de         │
│    este ejercicio →" ]               │
│    → Navega a F3 (Historial          │
│      de Ejercicio)                   │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  (Visible según origen: sí desde     │
│   D1/D4/F3, no desde E1)            │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a la vista de origen: D1 (Diccionario), D4 (Plan), E1 (Sesión Activa) o F3 (Historial de Ejercicio) |
| 2 | Título (nombre del ejercicio) | Texto dinámico | Top Bar, centrado | Muestra el nombre del ejercicio actual |
| 3 | Media visual | Imagen PNG (3D minimalista, fondo blanco) o logo placeholder | Body, parte superior (prominente) | Ilustra la ejecución correcta del movimiento. Si el ejercicio no tiene imagen (`media_resource = NULL`), muestra el logo de la app como placeholder con ícono de cámara y texto "Toca para agregar imagen". **La imagen es clickable**: al tocar, abre el selector de galería del dispositivo para agregar o cambiar la imagen (aplica a todos los ejercicios, tanto precargados como custom). Al seleccionar, la imagen se copia al almacenamiento interno y se actualiza en BD |
| 4 | Información textual | Campos de solo lectura | Body, bajo la media visual | 3 campos: Nombre, Tipo de equipo, Zona muscular |
| 5 | Enlace "Ver historial de este ejercicio" | Link de navegación | Body, parte inferior | Navega a F3 con el contexto del ejercicio actual. Permite al ejecutante explorar su progresión histórica directamente desde la ficha |
| 6 | Bottom Navigation | Barra fija (condicional) | Bottom Bar | Visible si se accedió desde D1, D4 o F3. Oculto si se accedió desde E1 (sesión activa — restricción de navegación global) |

---

#### D5 — Crear Ejercicio

**Contexto:** Formulario de creación de ejercicio personalizado. Accesible desde el FAB de D1. Incluye soporte para imagen opcional seleccionable desde la galería del dispositivo. Bottom Navigation visible (Diccionario activo).

**HU de referencia:** HU-03 (CA-03.10, CA-03.11)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  "Crear ejercicio"           │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Imagen (opcional) }               │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  │   [Logo de la app]            │  │
│  │   [📷 ícono de cámara]         │  │
│  │   "Toca para agregar imagen"   │  │
│  │                                │  │
│  └────────────────────────────────┘  │
│  → Al tocar → abre galería           │
│  → Imagen seleccionada se muestra    │
│    en lugar del placeholder          │
│  "La imagen es opcional. Puedes      │
│   agregarla después."               │
│                                      │
│  { Nombre (obligatorio) }            │
│  ┌────────────────────────────────┐  │
│  │ Nombre: [                    ] │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Tipo de equipo (obligatorio) }    │
│  ┌────────────────────────────────┐  │
│  │ Tipo de equipo: [      ▼    ] │  │
│  │ Opciones: 9 tipos de equipo   │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Zonas musculares (≥1 oblig.) }    │
│  "Zona muscular"                     │
│  ┌────────────────────────────────┐  │
│  │ [Pecho Medio] [Pecho Alto]    │  │
│  │ [Espalda Alta] [Dorsal]       │  │
│  │ [Abdomen] [Oblicuos]          │  │
│  │ [Hombro] [Tríceps] [Bíceps]   │  │
│  │ [Cuádriceps] [Aductor]        │  │
│  │ [Abductor] [Glúteos] [Gemelo] │  │
│  └────────────────────────────────┘  │
│  → FilterChips multi-select          │
│                                      │
│  { Condiciones especiales }          │
│  "Condiciones especiales"            │
│  ☐ Peso corporal                    │
│  ☐ Isométrico                       │
│  ☐ Al fallo técnico                 │
│                                      │
│  ┌────────────────────────────────┐  │
│  │         [ Crear ]             │  │
│  └────────────────────────────────┘  │
│  → Disabled hasta completar todos    │
│    los campos obligatorios           │
│  → Al confirmar: valida unicidad     │
│    (nombre, equipo), persiste con    │
│    is_custom = 1, navega de vuelta   │
│    a D1                              │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│           (●)                        │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a D1 (Diccionario) sin crear |
| 2 | Título "Crear ejercicio" | Texto estático | Top Bar, centrado | Identifica la acción |
| 3 | Zona de imagen | Box clickable | Body, parte superior | Placeholder: logo de la app + ícono cámara + texto "Toca para agregar imagen". Al tocar → abre galería (`image/*`). Si se selecciona imagen, la muestra con ContentScale.Crop. Imagen opcional — si no se selecciona, el ejercicio se crea con `media_resource = NULL` |
| 4 | Texto hint imagen | Texto estático | Body, bajo zona de imagen | "La imagen es opcional. Puedes agregarla después." |
| 4 | Campo Nombre | OutlinedTextField | Body | Obligatorio. Validación: no vacío. Error: "El nombre es obligatorio" |
| 5 | Dropdown Tipo de equipo | ExposedDropdownMenuBox | Body | Obligatorio. Opciones: 9 tipos. Error: "Selecciona un tipo de equipo" |
| 6 | Zonas musculares | FlowRow de FilterChip | Body | Obligatorio (≥1). 15 zonas, multi-select. Error: "Selecciona al menos una zona muscular" |
| 7 | Condiciones especiales | 3 × Checkbox | Body | Opcionales: Peso corporal, Isométrico, Al fallo técnico |
| 8 | Botón Crear | Button full-width | Body, parte inferior | Disabled hasta completar todos obligatorios. Al tocar → valida unicidad, crea ejercicio, navega a D1 |
| 9 | Bottom Navigation | Barra fija | Bottom Bar | Diccionario marcado como activo |

**Estados:**

- **Formulario vacío:** Placeholder de imagen (logo), campos vacíos, botón Crear disabled.
- **Formulario parcial:** Algunos campos completos, botón Crear disabled hasta completar todos.
- **Imagen seleccionada:** La imagen seleccionada reemplaza el placeholder.
- **Validación fallida:** Campos con error muestran mensaje de supportingText en rojo.
- **Guardando:** Botón Crear muestra CircularProgressIndicator.
- **Error de unicidad:** Snackbar "An exercise with this name and equipment type already exists".

---

#### D6 — Crear/Editar Rutina

**Contexto:** Formulario para crear una nueva rutina o editar una existente. Accesible desde D3 (FAB para crear, tocar nombre de rutina para editar). Incluye Bottom Navigation.

**HU de referencia:** HU-22 (CA-01 a CA-06, CA-07, CA-08)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Crear rutina"      │
│          o "Editar rutina")           │
│                                      │
├──────────────────────────────────────┤
│              BODY                    │
│                                      │
│  { Nombre de la rutina }             │
│  (Label: "Nombre")                   │
│  [ Campo texto: ______________ ]     │
│    → Máximo 50 caracteres             │
│    → Obligatorio                      │
│                                      │
│  ---                                 │
│                                      │
│  { Versiones }                        │
│  ("Versiones de esta rutina")         │
│  ┌────────────────────────────────┐  │
│  │ Versión 1  (8 ej.)   [ 🗑️ ]    │  │
│  │ Versión 2  (6 ej.)   [ 🗑️ ]    │  │
│  └────────────────────────────────┘  │
│  → 🗑️ solo visible si hay >1 versión │
│  → Eliminar versión con confirmación  │
│                                      │
│  [ + Agregar versión ]               │
│                                      │
│  ---                                 │
│                                      │
│  [ Botón primario: "Guardar" ]       │
│    → Habilitado si nombre válido      │
│                                      │
│  --- Solo en modo edición ---        │
│                                      │
│  [ Botón destructivo:                │
│    "Eliminar rutina" ]               │
│    → Solo si no es la única rutina    │
│    → Solo si no hay sesión activa     │
│    → Con confirmación explícita       │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│           (●)                        │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a D3 (Plan de Entrenamiento) |
| 2 | Título dinámico | Texto dinámico | Top Bar | "Crear rutina" o "Editar rutina" según contexto |
| 3 | Campo Nombre | OutlinedTextField | Body | Obligatorio. Máximo 50 caracteres. Error: "El nombre es obligatorio" |
| 4 | Lista de versiones | Lista interactiva | Body | Muestra versiones existentes con conteo de ejercicios. Botón 🗑️ para eliminar (solo si >1 versión, con confirmación). En modo creación, se crea automáticamente la Versión 1 |
| 5 | Botón "+ Agregar versión" | Botón de acción | Body, bajo versiones | Crea una nueva versión vacía en la rutina |
| 6 | Botón "Guardar" | Botón primario | Body | Persiste la rutina y retorna a D3 |
| 7 | Botón "Eliminar rutina" | Botón destructivo | Body (solo edición) | Solo visible en edición, si no es la única rutina y no hay sesión activa. Con diálogo de confirmación |
| 8 | Bottom Navigation | Barra fija | Bottom Bar | Diccionario marcado como activo |

**Estados:**

- **Crear:** Nombre vacío, Versión 1 creada automáticamente, botón Guardar disabled hasta nombre válido.
- **Editar:** Nombre precargado, versiones existentes listadas, botón "Eliminar rutina" visible (si aplica).

---

#### D3 — Plan de Entrenamiento

**Contexto:** Navegación por las rutinas creadas por el usuario y sus versiones. Comparte sección "Diccionario" del Bottom Navigation con D1 mediante tabs superiores. Incluye Bottom Navigation.

**HU de referencia:** HU-04 (CA-04.01, CA-04.05), HU-22

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  (Título: "Diccionario")             │
│                                      │
│  { Tabs }                            │
│  [ Ejercicios ] [ Plan (●) ]        │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Rutina 1: "Superior (Pull)" }     │
│  ┌────────────────────────────────┐  │
│  │ [ Versión 1 ]  (11 ej.)      │  │
│  │ [ Versión 2 ]  (10 ej.)      │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Rutina 2: "Superior (Push)" }     │
│  ┌────────────────────────────────┐  │
│  │ [ Versión 1 ]  (8 ej.)       │  │
│  │ [ Versión 2 ]  (8 ej.)       │  │
│  │ [ Versión 3 ]  (8 ej.)       │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Rutina 3: "Inferior" }            │
│  ┌────────────────────────────────┐  │
│  │ [ Versión 1 ]  (8 ej.)       │  │
│  └────────────────────────────────┘  │
│                                      │
│  → Rutinas y versiones definidas      │
│    por el usuario                     │
│  → Al tocar una versión → D4         │
│  → Al tocar nombre de rutina → D6    │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│           (●)                        │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Título "Diccionario" | Texto estático | Top Bar | Mismo título que D1 (comparten sección) |
| 2 | Tabs "Ejercicios" / "Plan" | Tab bar | Top Bar, bajo título | Dos tabs: "Ejercicios" (al tocar → D1) y "Plan" (activo = D3) |
| 3 | Secciones de rutina | Agrupaciones visuales | Body | Cada rutina muestra su nombre como encabezado y las versiones disponibles como filas. El usuario puede crear cualquier número de rutinas |
| 4 | Fila de versión | Elemento interactivo | Dentro de cada sección de rutina | Muestra: "Versión N" + "(X ej.)" indicando la cantidad de ejercicios. Al tocar → navega a D4 con la combinación rutina-versión seleccionada |
| 5 | Nombre de rutina | Elemento interactivo | Encabezado de cada sección | Al tocar → navega a D6 (Crear/Editar Rutina) para editar la rutina |
| 6 | Botón crear rutina (FAB) | FloatingActionButton | Bottom-end, sobre Bottom Nav | Ícono "+" (Add). Al tocar → navega a D6 (Crear/Editar Rutina) para crear una nueva rutina |
| 7 | Bottom Navigation | Barra fija | Bottom Bar | Diccionario marcado como activo |

---

#### D4 — Detalle de Versión del Plan

**Contexto:** Lista de ejercicios asignados a una combinación rutina-versión específica. Accesible desde D3. Incluye Bottom Navigation.

**HU de referencia:** HU-04 (CA-04.02 a CA-04.04, CA-04.06, CA-04.07, CA-04.08), HU-22, HU-25

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Rutina 1 —        │
│           Versión 1")                │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  (N ejercicios · Sin orden           │
│   obligatorio)                       │
│                                      │
│  { Lista de ejercicios (1 fila =     │
│    1 slot) }                         │
│  ┌────────────────────────────────┐  │
│  │ (Press de banca)         [+][✏️][🗑️]│
│  │ Pecho Medio · Máquina         │  │
│  │ 4 series · 8-12 reps          │  │
│  ├────────────────────────────────┤  │
│  │ (Hip Thrust ó Sentadilla      │  │
│  │  Búlgara ó Sentadilla Zumo)   │  │
│  │         [+][✏️][🗑️]              │  │
│  │ Glúteos · Máquina/Mancuernas  │  │
│  │ 4 series · 8-12 reps          │  │
│  ├────────────────────────────────┤  │
│  │ (Plancha)                [+][✏️][🗑️]│
│  │ Abdomen · Cuerpo              │  │
│  │ 4 series · 30-45 seg          │  │
│  ├────────────────────────────────┤  │
│  │       ...más ejercicios...    │  │
│  └────────────────────────────────┘  │
│  → Cada fila al tocar → navega a D2  │
│    (detalle del ejercicio primario)  │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│           (●)                        │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a D3 (Plan de Entrenamiento) |
| 2 | Título dinámico | Texto dinámico | Top Bar, centrado | Muestra "[Nombre de Rutina] — Versión N" según la selección |
| 3 | Subtítulo informativo | Texto estático | Body, encabezado | "N ejercicios · Sin orden obligatorio" — N refleja la cantidad de slots (no de plan_assignments individuales) |
| 4 | Lista de ejercicios | Lista interactiva | Body, scrollable | Cada fila representa un **slot** del plan. Si el slot tiene un solo ejercicio, muestra su nombre normalmente. Si tiene alternativas, el encabezado concatena los nombres con "ó" (e.g., "Hip Thrust ó Sentadilla Búlgara ó Sentadilla de Zumo"). Cada fila muestra: (1) Nombre(s) del ejercicio (prominente), (2) Zona muscular + tipo de equipo del primario, (3) Series + Rango de repeticiones. Al tocar → navega a D2 del ejercicio primario |
| 5 | Botón "+" (agregar alternativa) | IconButton | Trailing de cada fila | Abre un bottom sheet para agregar un ejercicio alternativo al slot (HU-25 CA-01/CA-02). La alternativa hereda series y reps del slot |
| 6 | Botón "✏️" (editar prescripción) | IconButton | Trailing de cada fila | Permite editar series y rango de repeticiones. Los cambios se propagan a todas las alternativas del slot (HU-25 CA-04) |
| 7 | Botón "🗑️" (eliminar slot) | IconButton | Trailing de cada fila | Elimina **todo el slot** (principal + alternativas) con confirmación previa. No disponible si hay sesión activa de la versión (HU-25 CA-05) |
| 8 | Botón asignar ejercicio (FAB) | FloatingActionButton | Bottom-end, sobre Bottom Nav | Ícono "+" (Add). Al tocar → presenta lista de ejercicios del Diccionario aún no asignados a esta versión. El ejecutante selecciona uno, confirma series y rango de repeticiones. Crea un nuevo slot |
| 9 | Bottom Navigation | Barra fija | Bottom Bar | Diccionario marcado como activo |

**Diferenciación de condiciones especiales en la lista:**

| Tipo de ejercicio | Tercera línea de la fila |
|-------------------|--------------------------|
| Estándar (con peso / máquina) | "{N} series · 8-12 reps" |
| Peso corporal — Flexiones | "{N} series · Al fallo técnico" |
| Isométrico (Plancha, Plancha Lateral) | "{N} series · 30-45 seg" |
| Peso corporal — otros (Abdominales, Escalador, Giro Ruso, Sentadilla/Cuerpo) | "{N} series · 8-12 reps" |

---

#### E1 — Sesión Activa

**Contexto:** Vista principal de entrenamiento. Es el centro operativo durante toda la sesión. No tiene Bottom Navigation (restricción de sesión activa). Accesible desde B1 ("Iniciar Sesión" o "Reanudar Sesión").

**HU de referencia:** HU-05 (CA-05.06 a CA-05.09), HU-06 (CA-06.10, CA-06.12), HU-07 (CA-07.01, CA-07.05, CA-07.06), HU-08 (CA-08.01, CA-08.04), HU-25 (CA-10 a CA-13)
**HU indirectas visibles:** HU-11 (carga objetivo), HU-17 (indicador de descarga)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  (Rutina 1 — Versión 1)              │
│  (Estado: "Sesión activa")           │
│                                      │
│  { Indicador de descarga }           │
│  → Solo visible si descarga activa   │
│  ("🔄 Descarga · Sesión 2/6")       │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Progreso de la sesión }           │
│  (4 de 11 ejercicios completados)    │
│  [ ████████░░░░░░░░░░░ ] 36%        │
│                                      │
│  ---                                 │
│                                      │
│  { Lista de ejercicios }             │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ ✅ (Press de banca)           │  │
│  │   Completado · {N}/{N} series     │  │
│  │   Carga: 60 Kg                │  │
│  │                    [ 📷 ]     │  │
│  ├────────────────────────────────┤  │
│  │ 🔵 (Press de mancuerna)       │  │
│  │   En Ejecución · 2/{N} series   │  │
│  │   Carga: 22.5 Kg              │  │
│  │         [ Registrar ] [ 📷 ]  │  │
│  ├────────────────────────────────┤  │
│  │ ⚪ (Flexiones)                │  │
│  │   No Iniciado · 0/{N} series    │  │
│  │   Carga: Peso corporal        │  │
│  │  [ Registrar ] [ Sustituir ]  │  │
│  │                       [ 📷 ]  │  │
│  ├────────────────────────────────┤  │
│  │ ⚪ (Hip Thrust)       [↔]    │  │
│  │   No Iniciado · 0/{N} series    │  │
│  │   Carga: 80 Kg                │  │
│  │  [ Registrar ] [ Sustituir ]  │  │
│  │                       [ 📷 ]  │  │
│  │  → [↔] = SwapHoriz, solo si   │  │
│  │    slot tiene alternativas     │  │
│  ├────────────────────────────────┤  │
│  │ ⚪ (Plancha)                  │  │
│  │   No Iniciado · 0/{N} series    │  │
│  │   Carga: Isométrico (30-45s)  │  │
│  │  [ Registrar ] [ Sustituir ]  │  │
│  │                       [ 📷 ]  │  │
│  ├────────────────────────────────┤  │
│  │       ...más ejercicios...    │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  [ Botón: "Cerrar Sesión" ]          │
│    → Navega a E4 (Confirmación)      │
│                                      │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Rutina y Versión | Texto estático | Top Bar, izquierda | Muestra la rutina y versión de la sesión en curso |
| 2 | Estado "Sesión activa" | Texto estático | Top Bar, bajo rutina | Confirma que la sesión está en curso |
| 3 | Indicador de descarga | Badge condicional | Top Bar, bajo estado | Solo visible si descarga activa. Muestra "🔄 Descarga · Sesión N/T" (T = total de rutinas del plan). Las cargas de los ejercicios reflejan el ajuste al 60% |
| 4 | Barra de progreso | Indicador visual | Body, encabezado | "N de M ejercicios completados" + barra de progreso porcentual. Se actualiza en tiempo real al completar ejercicios |
| 5 | Lista de ejercicios | Lista interactiva | Body, scrollable | Cada fila muestra: icono de estado, nombre del ejercicio, estado textual + conteo de series (N/4), carga objetivo. Los botones disponibles dependen del estado del ejercicio (ver tabla de estados abajo) |
| 6 | Icono 📷 (media visual) | Botón icónico | Cada fila, derecha | Al tocar → navega a D2 (Detalle de Ejercicio, media visual). Siempre visible en todos los estados |
| 7 | Botón "Registrar" | Botón de acción | Cada fila (si estado ≠ Completado) | Al tocar → navega a E2 (Registro de Serie) con el contexto del ejercicio y el número de serie siguiente |
| 8 | Botón "Sustituir" | Botón de acción | Cada fila (solo si No Iniciado) | Al tocar → navega a E3 (Selección de Sustituto). Solo visible si 0 series registradas (CA-07.06) |
| 9 | Botón "↔ Intercambiar" (SwapHoriz) | Botón icónico | Cada fila (solo si No Iniciado con alternativas) | Solo visible si el slot tiene alternativas (`alternativesInSlot > 1`) y el ejercicio está NOT_STARTED con 0 series. Al tocar → abre bottom sheet con las alternativas del slot para intercambiar (HU-25 CA-10/CA-11) |
| 10 | Botón "Cerrar Sesión" | Botón secundario | Body, final de la lista | Al tocar → navega a E4 (Confirmación de Cierre) |

**Estados por ejercicio:**

| Estado | Icono | Series | Botones visibles | Carga mostrada |
|--------|-------|--------|------------------|----------------|
| No Iniciado | ⚪ | 0/4 | Registrar, Sustituir, ↔ Intercambiar (condicional: alternativas en slot), 📷 | Carga objetivo del historial (o "Sin historial — establecer carga" si es primera vez) |
| En Ejecución | 🔵 | 1-3/4 | Registrar, 📷 | Última carga registrada en esta sesión |
| Completado | ✅ | 4/4 | 📷 | Carga final registrada |

**Variaciones de carga mostrada:**

| Tipo de ejercicio | Carga mostrada |
|-------------------|----------------|
| Estándar (con peso) | "60 Kg" (carga objetivo derivada del historial, o precarga si descarga activa al 60%) |
| Sin historial | "Sin historial — establecer carga" |
| Peso corporal | "Peso corporal" |
| Isométrico | "Isométrico (30-45s)" |

---

#### E2 — Registro de Serie

**Contexto:** Formulario modal o pantalla de captura rápida para registrar una serie individual. Diseñado para máximo 3 toques (CA-06.02). Sin Bottom Navigation.

**HU de referencia:** HU-06 (CA-06.01 a CA-06.09), HU-08 (CA-08.01, CA-08.04, CA-08.05, CA-08.08)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ✕ ]  (Título: "Press de banca")  │
│         (Subtítulo: "Serie 3 de 4")  │
│                                      │
├──────────────────────────────────────┤
│              BODY                    │
│                                      │
│  { Formulario de serie }             │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ (Label: "Peso (Kg)")          │  │
│  │ [ Campo numérico: 60 Kg ]     │  │
│  │   → Precargado con último     │  │
│  │     peso utilizado             │  │
│  │   → Teclado numérico          │  │
│  │   → Validación: ≥ 0           │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ (Label: "Repeticiones")       │  │
│  │ [ Campo numérico: ___ ]       │  │
│  │   → Teclado numérico          │  │
│  │   → Validación: ≥ 1           │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ (Label: "RIR")                │  │
│  │ [ Selector: 0 1 2 ]           │  │
│  │   → Selector rápido de un     │  │
│  │     solo toque (chips o       │  │
│  │     stepper)                   │  │
│  │   → Rango: 0 a 2              │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  [ Botón primario: "Confirmar" ]     │
│    → Valida campos → persiste →      │
│      retorna a E1 con serie          │
│      registrada                      │
│                                      │
│  [ Enlace: "Cancelar" ]             │
│    → Descarta → retorna a E1        │
│    sin cambios                       │
│                                      │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ✕ (cerrar) | Botón de cierre | Top Bar, izquierda | Descarta el formulario y retorna a E1 sin registrar (equivalente a "Cancelar") |
| 2 | Nombre del ejercicio | Texto dinámico | Top Bar, centrado | Muestra el nombre del ejercicio al que se le registra la serie |
| 3 | Indicador de serie | Texto dinámico | Top Bar, bajo el nombre | "Serie N de 4" — indica qué número de serie se está registrando (asignado automáticamente por el sistema, CA-06.09) |
| 4 | Campo "Peso (Kg)" | Input numérico | Body, primer campo | Precargado con último peso utilizado para este ejercicio (CA-06.04). Si es peso corporal: fijado en 0 y no editable (CA-08.01). Teclado numérico. Validación ≥ 0 |
| 5 | Campo "Repeticiones" | Input numérico | Body, segundo campo | Sin precarga (el ejecutante lo ingresa). Teclado numérico. Validación ≥ 1. Para ejercicios estándar y peso corporal |
| 6 | Campo "RIR" | Selector rápido (chips) | Body, tercer campo | 3 opciones: 0, 1, 2. Selección de un solo toque (chip o botón numérico). Sin precarga |
| 7 | Botón "Confirmar" | Botón primario | Body, final | Valida los 3 campos. Si válidos: persiste la serie, asocia metadatos automáticos (fecha, rutina, versión, ejercicio, número de serie) y retorna a E1. Si inválidos: muestra error inline |
| 8 | Enlace "Cancelar" | Link de acción | Body, bajo el botón | Descarta y retorna a E1 sin cambios |

**Variante: Ejercicio isométrico**

Cuando el ejercicio es isométrico (Plancha, Plancha Lateral), el formulario cambia:

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ✕ ]  (Título: "Plancha")         │
│         (Subtítulo: "Serie 2 de 4")  │
│                                      │
├──────────────────────────────────────┤
│              BODY                    │
│                                      │
│  { Formulario de serie isométrica }  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ (Label: "Peso (Kg)")          │  │
│  │ [ Campo: 0 Kg ] (no editable) │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ (Label: "Segundos sostenidos") │  │
│  │ [ Campo numérico: ___ seg ]   │  │
│  │   → Teclado numérico          │  │
│  │   → Validación: ≥ 1           │  │
│  │   → (Referencia: 30-45 seg)   │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ (Label: "RIR")                │  │
│  │ [ Selector: 0 1 2 3 4 5 ]    │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  [ Botón primario: "Confirmar" ]     │
│  [ Enlace: "Cancelar" ]             │
│                                      │
└──────────────────────────────────────┘
```

**Diferencias en variante isométrica:**

| Campo | Ejercicio estándar | Ejercicio isométrico |
|-------|-------------------|---------------------|
| Peso | Precargado, editable | Fijado en 0, no editable |
| Segundo campo | "Repeticiones" (validación ≥ 1) | "Segundos sostenidos" (validación ≥ 1, referencia visual 30-45s) |
| RIR | Igual | Igual |

**Variante: Ejercicio de peso corporal (no isométrico)**

| Campo | Comportamiento |
|-------|----------------|
| Peso | Fijado en 0 Kg, no editable (CA-08.01) |
| Repeticiones | Igual que estándar (editable, ≥ 1) |
| RIR | Igual |

---

#### E3 — Selección de Ejercicio Sustituto

**Contexto:** Lista filtrada de ejercicios elegibles como sustitutos. Solo accesible desde E1 para ejercicios en estado "No Iniciado". Sin Bottom Navigation.

**HU de referencia:** HU-07 (CA-07.01 a CA-07.06)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ✕ ]  (Título: "Sustituir         │
│           ejercicio")                │
│  (Subtítulo: "Reemplazar:            │
│   Flexiones")                        │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  (Selecciona un ejercicio de la      │
│   misma zona muscular como           │
│   reemplazo.)                        │
│  (La sustitución es puntual y no     │
│   modifica el Plan.)                 │
│                                      │
│  ---                                 │
│                                      │
│  { Lista de ejercicios elegibles }   │
│  ┌────────────────────────────────┐  │
│  │ (Cruce en polea alta)         │  │
│  │ Pecho Inferior · Máquina      │  │
│  ├────────────────────────────────┤  │
│  │ (Apertura de pecho sentado)   │  │
│  │ Pecho Medio · Máquina         │  │
│  ├────────────────────────────────┤  │
│  │ (Remo con Inclinación)        │  │
│  │ Espalda Media · Barra de      │  │
│  │ Pesas                         │  │
│  ├────────────────────────────────┤  │
│  │       ...más ejercicios...    │  │
│  └────────────────────────────────┘  │
│  → Solo ejercicios de la misma zona  │
│    muscular del Diccionario          │
│  → Excluye los ya prescritos en      │
│    la sesión activa                  │
│  → Al tocar un ejercicio: diálogo    │
│    de confirmación                   │
│                                      │
│  { Diálogo de confirmación }         │
│  ┌────────────────────────────────┐  │
│  │ "¿Sustituir Flexiones por     │  │
│  │  Cruce en polea alta?"        │  │
│  │                                │  │
│  │ [ Cancelar ] [ Confirmar ]    │  │
│  └────────────────────────────────┘  │
│                                      │
│  [ Enlace: "Cancelar" ]             │
│    → Retorna a E1 sin cambios        │
│                                      │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ✕ (cerrar) | Botón de cierre | Top Bar, izquierda | Retorna a E1 sin realizar sustitución |
| 2 | Título "Sustituir ejercicio" | Texto estático | Top Bar, centrado | Identifica la acción |
| 3 | Subtítulo "Reemplazar: [nombre]" | Texto dinámico | Top Bar, bajo título | Indica qué ejercicio se va a reemplazar |
| 4 | Texto informativo | Texto estático | Body, encabezado | Aclara: (1) solo se muestran ejercicios de la misma zona muscular, (2) la sustitución es puntual y no modifica el Plan |
| 5 | Lista de ejercicios elegibles | Lista interactiva | Body, scrollable | Muestra todos los ejercicios del Diccionario de la misma zona muscular, excluyendo los ya prescritos en la sesión. Cada fila: nombre + zona muscular + tipo de equipo |
| 6 | Diálogo de confirmación | Modal/diálogo | Sobre el body | Al tocar un ejercicio de la lista → muestra diálogo: "¿Sustituir [original] por [sustituto]?" con botones Cancelar y Confirmar. Si confirma: reemplaza en la sesión → retorna a E1 |
| 7 | Enlace "Cancelar" | Link de acción | Body, final | Retorna a E1 sin cambios |

---

#### E4 — Confirmación de Cierre de Sesión

**Contexto:** Diálogo modal superpuesto sobre E1 (no es una pantalla de navegación completa). Solicita confirmación antes de cerrar la sesión. Sin Bottom Navigation.

**HU de referencia:** HU-09 (CA-09.01 a CA-09.03)

```
┌──────────────────────────────────────┐
│                                      │
│         { E1 difuminado detrás }     │
│                                      │
│  ┌────────────────────────────────┐  │
│  │      DIÁLOGO DE CIERRE        │  │
│  │                                │  │
│  │  --- Caso A: Sesión completa  │  │
│  │                                │  │
│  │  ("Cerrar sesión")            │  │
│  │  ("Todos los ejercicios       │  │
│  │   están completados.")        │  │
│  │  ("La sesión se cerrará       │  │
│  │   como Completada.")          │  │
│  │                                │  │
│  │  [ Cancelar ] [ Cerrar ✓ ]   │  │
│  │                                │  │
│  │  --- Caso B: Sesión parcial   │  │
│  │                                │  │
│  │  ("Cerrar sesión")            │  │
│  │  ⚠️ ("Hay 5 ejercicios sin    │  │
│  │   completar.")                │  │
│  │  ("La sesión se cerrará       │  │
│  │   como Incompleta. Los datos  │  │
│  │   parciales se conservarán.") │  │
│  │                                │  │
│  │  [ Cancelar ] [ Cerrar ⚠️ ]  │  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Fondo difuminado (E1) | Overlay | Fondo completo | E1 permanece visible pero no interactiva debajo del diálogo |
| 2 | Título "Cerrar sesión" | Texto estático | Diálogo, encabezado | Identifica la acción |
| 3 | Mensaje descriptivo | Texto dinámico | Diálogo, cuerpo | Caso A (todos completados): "Todos los ejercicios están completados. La sesión se cerrará como Completada." Caso B (parcial): "Hay N ejercicios sin completar. La sesión se cerrará como Incompleta. Los datos parciales se conservarán." |
| 4 | Botón "Cancelar" | Botón secundario | Diálogo, pie izquierdo | Cierra el diálogo → retorna a E1 (la sesión sigue activa) |
| 5 | Botón "Cerrar" | Botón primario | Diálogo, pie derecho | Caso A: "Cerrar ✓" (estilo normal). Caso B: "Cerrar ⚠️" (estilo advertencia). Al confirmar: cierre de sesión → motor de reglas (tonelaje, progresión, doble umbral, rotación) → navega a E5 |

**Estados del diálogo:**

| Estado | Mensaje | Botón de cierre |
|--------|---------|-----------------|
| Todos los ejercicios finalizados | "Todos los ejercicios están completados. La sesión se cerrará como Completada." | "Cerrar ✓" (estilo confirmación) |
| Al menos un ejercicio incompleto | "Hay N ejercicios sin completar. La sesión se cerrará como Incompleta. Los datos parciales se conservarán." | "Cerrar ⚠️" (estilo advertencia) |

---

#### E5 — Resumen Post-Sesión

**Contexto:** Vista presentada automáticamente tras confirmar cierre en E4. Muestra el resumen completo de la sesión. No tiene botón de retorno a E1 (sesión ya cerrada e inmutable). Sin Bottom Navigation.

**HU de referencia:** HU-13 (CA-13.01 a CA-13.07)
**HU indirectas visibles:** HU-08 (CA-08.07 — badge isométrico dominado), HU-10 (clasificación de progresión), HU-11 (señal de subir carga), HU-12 (señal de considerar descarga)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  (Título: "Resumen de Sesión")       │
│  (Subtítulo: "Rutina 1 — V1")       │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Card: Estado y Tonelaje }         │
│  ┌────────────────────────────────┐  │
│  │ (Estado: "Completada" ✅ )    │  │
│  │       — o —                   │  │
│  │ (Estado: "Incompleta" ⚠️ )    │  │
│  │                                │  │
│  │ (Tonelaje total)              │  │
│  │ ("12,450 Kg")                 │  │
│  │                                │  │
│  │ (Ejercicios completados)      │  │
│  │ ("9/11 ejercicios")           │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Lista: Progresión por ejercicio } │
│  ┌────────────────────────────────┐  │
│  │ (Press de banca)              │  │
│  │ ↑ Progresión · 60 Kg         │  │
│  │ ▸ "Subir carga → 62.5 Kg"    │  │
│  ├────────────────────────────────┤  │
│  │ (Press de mancuerna)          │  │
│  │ = Mantenimiento · 22.5 Kg    │  │
│  │ ▸ "Mantener carga"            │  │
│  ├────────────────────────────────┤  │
│  │ (Remo con Inclinación)        │  │
│  │ ↓ Regresión · 40 Kg          │  │
│  │ ▸ "Considerar descarga"       │  │
│  ├────────────────────────────────┤  │
│  │ (Flexiones)                   │  │
│  │ ↑ Progresión · Peso corporal  │  │
│  │ ▸ "48 reps totales (+3)"     │  │
│  ├────────────────────────────────┤  │
│  │ (Plancha)  🏆 Dominado        │  │
│  │ ↑ Progresión · Isométrico    │  │
│  │ ▸ "4×45s — Dominado"          │  │
│  ├────────────────────────────────┤  │
│  │       ...más ejercicios...    │  │
│  └────────────────────────────────┘  │
│  → Cada fila al tocar → F3          │
│    (Historial de Ejercicio)          │
│                                      │
│  ---                                 │
│                                      │
│  [ Botón primario:                   │
│    "Ir al Inicio" ]                  │
│    → Navega a B1                     │
│                                      │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Título "Resumen de Sesión" | Texto estático | Top Bar | Identifica la pantalla |
| 2 | Subtítulo rutina/versión | Texto dinámico | Top Bar, bajo título | "[Nombre de Rutina] — VN" de la sesión recién cerrada |
| 3 | Estado de la sesión | Badge visual | Body, card superior | "Completada ✅" o "Incompleta ⚠️" con diferenciación visual clara |
| 4 | Tonelaje total | Texto numérico prominente | Body, card superior | Σ Peso × Reps de todas las series, expresado en Kg con separador de miles |
| 5 | Ejercicios completados | Texto informativo | Body, card superior | "N/M ejercicios" (completados vs. total de la sesión) |
| 6 | Lista de progresión por ejercicio | Lista interactiva | Body, sección central | Cada fila muestra: nombre del ejercicio, clasificación de progresión con icono y color, carga utilizada, señal de acción para la próxima sesión. Al tocar → navega a F3 (Historial de Ejercicio) |
| 7 | Botón "Ir al Inicio" | Botón primario | Body, final | Navega a B1. Única salida principal de esta pantalla |

**Nota sobre sesión incompleta (CA-13.07):** En sesiones cerradas como "Incompleta", solo se muestran en la lista de progresión los ejercicios que tienen al menos 1 serie registrada. Los ejercicios sin ningún registro no aparecen en la lista (no hay datos para calcular progresión). El tonelaje mostrado es parcial, calculado únicamente sobre las series efectivamente registradas.

**Clasificación de progresión (señales visuales):**

| Clasificación | Icono | Color | Significado |
|---------------|-------|-------|-------------|
| Progresión | ↑ | Verde | El ejercicio mejoró respecto a la sesión anterior |
| Mantenimiento | = | Amarillo | El ejercicio se mantuvo igual |
| Regresión | ↓ | Rojo | El ejercicio empeoró respecto a la sesión anterior |

**Señales de acción por tipo de ejercicio:**

| Tipo | Señal si progresión | Señal si mantenimiento | Señal si regresión |
|------|--------------------|-----------------------|-------------------|
| Estándar (con peso) | "Subir carga → X Kg" (Doble Umbral cumplido) | "Mantener carga" | "Considerar descarga" |
| Peso corporal | "N reps totales (+diferencia)" | "N reps totales (=)" | "N reps totales (−diferencia)" |
| Isométrico | "N×Xs — Progresando" | "N×Xs — Manteniendo" | "N×Xs — Regresando" |
| Isométrico dominado | "4×45s — 🏆 Dominado" (CA-08.07) | — | — |

---

#### F1 — Historial de Sesiones

**Contexto:** Listado cronológico de todas las sesiones cerradas. Accesible desde la navegación global (sección "Historial"). Incluye Bottom Navigation.

**HU de referencia:** HU-24 (CA-24.01, CA-24.03, CA-24.06)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  (Título: "Historial")              │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Lista de sesiones }               │
│  ┌────────────────────────────────┐  │
│  │ (10 feb 2026)                 │  │
│  │ Rutina 1 — V1 · Completada ✅ │  │
│  │ Tonelaje: 12,450 Kg           │  │
│  ├────────────────────────────────┤  │
│  │ (08 feb 2026)                 │  │
│  │ Rutina 3 — V3 · Incompleta ⚠️│  │
│  │ Tonelaje: 8,230 Kg            │  │
│  ├────────────────────────────────┤  │
│  │ (06 feb 2026)                 │  │
│  │ Rutina 2 — V2 · Completada ✅ │  │
│  │ Tonelaje: 10,800 Kg           │  │
│  ├────────────────────────────────┤  │
│  │       ...más sesiones...      │  │
│  └────────────────────────────────┘  │
│  → Ordenadas de más reciente a       │
│    más antigua                       │
│  → Al tocar una sesión → F2          │
│                                      │
│  { Estado vacío }                    │
│  ("No hay sesiones pasadas           │
│   disponibles.")                     │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                      (●)             │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Título "Historial" | Texto estático | Top Bar | Identifica la sección |
| 2 | Lista de sesiones | Lista interactiva | Body, scrollable | Cada fila: fecha, rutina + versión, estado (Completada ✅ / Incompleta ⚠️), tonelaje total en Kg. Orden cronológico descendente. Al tocar → F2 |
| 3 | Estado vacío | Texto informativo | Body (si no hay sesiones) | "No hay sesiones pasadas disponibles." (CA-24.06) |
| 4 | Bottom Navigation | Barra fija | Bottom Bar | Historial marcado como activo |

---

#### F2 — Detalle de Sesión Pasada

**Contexto:** Vista de solo lectura de una sesión cerrada. Accesible desde F1. No permite edición (CA-24.05). Incluye Bottom Navigation.

**HU de referencia:** HU-24 (CA-24.02, CA-24.04, CA-24.05)
**HU indirecta visible:** HU-10 (clasificación de progresión)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Rutina 1 — V1")  │
│  (Subtítulo: "10 feb 2026 ·         │
│   Completada")                       │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Resumen }                         │
│  ┌────────────────────────────────┐  │
│  │ Tonelaje: 12,450 Kg           │  │
│  │ Ejercicios: 11/11             │  │
│  │ Estado: Completada ✅          │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Lista de ejercicios ejecutados }  │
│  ┌────────────────────────────────┐  │
│  │ (Press de banca)              │  │
│  │ ↑ Progresión                  │  │
│  │ Serie 1: 60 Kg × 12 · RIR 2  │  │
│  │ Serie 2: 60 Kg × 11 · RIR 2  │  │
│  │ Serie 3: 60 Kg × 12 · RIR 2  │  │
│  │ Serie 4: 60 Kg × 10 · RIR 2  │  │
│  ├────────────────────────────────┤  │
│  │ (Cruce en polea alta)         │  │
│  │ = Mantenimiento               │  │
│  │ (Sustituyó a: Flexiones)     │  │
│  │ Serie 1: 25 Kg × 10 · RIR 2  │  │
│  │ Serie 2: 25 Kg × 10 · RIR 2  │  │
│  │ Serie 3: 25 Kg × 9 · RIR 2   │  │
│  │ Serie 4: 25 Kg × 9 · RIR 2   │  │
│  ├────────────────────────────────┤  │
│  │       ...más ejercicios...    │  │
│  └────────────────────────────────┘  │
│  → Solo lectura, sin edición         │
│  → Al tocar un ejercicio → F3       │
│  → Si hubo sustitución: se muestra   │
│    el ejercicio ejecutado con nota   │
│    "Sustituyó a: [original]"         │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                      (●)             │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a F1 |
| 2 | Título dinámico | Texto dinámico | Top Bar | "[Nombre de Rutina] — VN" |
| 3 | Subtítulo | Texto dinámico | Top Bar, bajo título | Fecha + estado de la sesión |
| 4 | Resumen | Card de solo lectura | Body, encabezado | Tonelaje total, ejercicios completados/total, estado |
| 5 | Lista de ejercicios | Lista interactiva (solo lectura) | Body, scrollable | Cada ejercicio: nombre, clasificación de progresión (↑/=/↓), series detalladas (peso × reps · RIR por serie). Si hubo sustitución: nota "Sustituyó a: [original]" (CA-24.04). Al tocar → F3 |
| 6 | Bottom Navigation | Barra fija | Bottom Bar | Historial marcado como activo |

---

#### F3 — Historial de Ejercicio

**Contexto:** Historial completo de un ejercicio específico a lo largo de todas las sesiones. Accesible desde F2, E5, G1, H2 y D2. Incluye Bottom Navigation (excepto si se accedió desde E5).

**HU de referencia:** HU-23 (CA-23.01 a CA-23.06)
**HU indirecta visible:** HU-10 (clasificación de progresión por sesión)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Press de banca") │
│  (Subtítulo: "Historial completo")   │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Estado de progresión actual }     │
│  ┌────────────────────────────────┐  │
│  │ Estado: "En Progresión" 🟢    │  │
│  │  — o —                        │  │
│  │ Estado: "En Meseta" 🟡        │  │
│  │  — o —                        │  │
│  │ Estado: "En Descarga" 🔵      │  │
│  │  — o —                        │  │
│  │ Estado: "Sin Historial" ⚪    │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Tendencia de carga }              │
│  ┌────────────────────────────────┐  │
│  │  Kg                            │  │
│  │  65│          ╱───             │  │
│  │  60│     ╱───╱                 │  │
│  │  55│╱───╱                      │  │
│  │  50│                           │  │
│  │    └───┬───┬───┬───┬──→       │  │
│  │       S1  S2  S3  S4  Sesión  │  │
│  └────────────────────────────────┘  │
│  → Para peso corporal: tendencia     │
│    por repeticiones totales          │
│  → Visualización que permite          │
│    identificar tendencia              │
│    (ascendente/estable/descendente)  │
│                                      │
│  ---                                 │
│                                      │
│  { Lista de sesiones del ejercicio } │
│  ┌────────────────────────────────┐  │
│  │ (10 feb 2026) · R1-V1          │  │
│  │ 60 Kg · 45 reps · RIR 2.3    │  │
│  │ ↑ Progresión                  │  │
│  ├────────────────────────────────┤  │
│  │ (03 feb 2026) · R1-V1          │  │
│  │ 57.5 Kg · 42 reps · RIR 2.0  │  │
│  │ = Mantenimiento               │  │
│  ├────────────────────────────────┤  │
│  │ (27 ene 2026) · R1-V1          │  │
│  │ 57.5 Kg · 40 reps · RIR 1.8  │  │
│  │ ↑ Progresión                  │  │
│  ├────────────────────────────────┤  │
│  │       ...más sesiones...      │  │
│  └────────────────────────────────┘  │
│  → Cronológico descendente           │
│  → Incluye registros de cualquier    │
│    rutina-versión (CA-23.03)         │
│                                      │
│  ---                                 │
│                                      │
│  [ Enlace: "Ver técnica de           │
│    ejecución →" ]                    │
│    → Navega a D2 (media visual)      │
│                                      │
│  { Estado vacío }                    │
│  ("No hay registros disponibles      │
│   para este ejercicio.")             │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  (Visible según origen: sí desde     │
│   F2/G1/H2/D2, no desde E5)         │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a la vista de origen (F2, E5, G1, H2 o D2) |
| 2 | Título (nombre del ejercicio) | Texto dinámico | Top Bar | Nombre del ejercicio consultado |
| 3 | Estado de progresión actual | Badge visual | Body, encabezado | En Progresión 🟢, En Meseta 🟡, En Descarga 🔵, Sin Historial ⚪ |
| 4 | Tendencia de carga | Gráfico lineal simplificado | Body, zona central | Eje Y = Kg (o reps totales para peso corporal). Eje X = sesiones. Permite identificar tendencia ascendente/estable/descendente (CA-23.04, CA-23.06) |
| 5 | Lista de sesiones | Lista de solo lectura | Body, scrollable | Cada fila: fecha + rutina-versión, peso + reps totales + RIR promedio, clasificación de progresión. Orden cronológico descendente. Independiente de rutina-versión (CA-23.03) |
| 6 | Enlace "Ver técnica de ejecución" | Link de navegación | Body, final | Navega a D2 (Detalle de Ejercicio con media visual) |
| 7 | Estado vacío | Texto informativo | Body | "No hay registros disponibles para este ejercicio." (CA-23.05) |
| 8 | Bottom Navigation | Barra fija (condicional) | Bottom Bar | Visible si se accedió desde F2, G1, H2 o D2. Oculto si se accedió desde E5 (post-sesión, sin nav global) |

---

#### G1 — Panel de Métricas

**Contexto:** Vista central de KPIs del sistema. Accesible desde la navegación global (sección "Métricas"). Incluye Bottom Navigation.

**HU de referencia:** HU-19 (CA-19.01 a CA-19.06), HU-21 (CA-21.01 a CA-21.07)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  (Título: "Métricas")               │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Sección: Índice de Adherencia }   │
│  ┌────────────────────────────────┐  │
│  │ ("Adherencia semanal")        │  │
│  │ ("83%")                       │  │
│  │ (5 de 6 sesiones esta semana) │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Sección: RIR Promedio por Rutina } │
│  [ Período ▼: 2 últimas sesiones ]   │
│  → Configurable: 2, 4, 6 sesiones   │
│  ┌────────────────────────────────┐  │
│  │ Rutina 1:  2.4  (Óptimo 🟢)  │  │
│  │ Rutina 2:  1.3  (Riesgo 🔴)  │  │
│  │ Rutina 3:  3.8  (Insuf. 🟡)  │  │
│  │                                │  │
│  │ (Referencia: 1 = óptimo,       │  │
│  │  < 0.5 = riesgo de fatiga,    │  │
│  │  > 1.8 = estímulo insuficiente)│  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Sección: Tasa de Progresión }     │
│  [ Período ▼: 4 semanas ]            │
│  → Configurable: 4, 8, 12 semanas   │
│  ┌────────────────────────────────┐  │
│  │ Press de banca       75% ↑    │  │
│  │ Press de mancuerna   50% =    │  │
│  │ Remo con Inclinación 25% ↓    │  │
│  │ Flexiones            60% ↑    │  │
│  │       ...más...               │  │
│  └────────────────────────────────┘  │
│  → Al tocar un ejercicio → F3       │
│                                      │
│  ---                                 │
│                                      │
│  { Sección: Velocidad de Carga }     │
│  ┌────────────────────────────────┐  │
│  │ Press de banca     +1.2 Kg/s  │  │
│  │ Press de mancuerna +0.5 Kg/s  │  │
│  │ Remo con Inclinación 0 Kg/s   │  │
│  │ (Flexiones: N/A)              │  │
│  │       ...más...               │  │
│  └────────────────────────────────┘  │
│  → Al tocar un ejercicio → F3       │
│  → Peso corporal excluido (N/A)      │
│                                      │
│  ---                                 │
│                                      │
│  { Accesos rápidos }                 │
│  [ Volumen por Grupo Muscular → ]    │
│    → Navega a G2                     │
│  [ Tendencia de Progresión → ]       │
│    → Navega a G3                     │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                              (●)     │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Título "Métricas" | Texto estático | Top Bar | Identifica la sección |
| 2 | Índice de Adherencia | Indicador numérico | Body, primera sección | Porcentaje de sesiones completadas vs. planificadas en la semana + desglose (N de M sesiones). Objetivo de frecuencia del perfil como denominador (CA-21.04) |
| 3 | RIR Promedio por Rutina | Indicadores con interpretación | Body, segunda sección | Un valor por rutina con referencia de interpretación: 1 óptimo 🟢, < 0.5 riesgo 🔴, > 1.8 insuficiente 🟡 (CA-21.03). Período configurable (2, 4, 6 últimas sesiones de la rutina) con defecto de 2 (CA-21.02) |
| 4 | Tasa de Progresión | Lista interactiva | Body, tercera sección | Ejercicios con su % de sesiones con progresión positiva. Período configurable (4, 8, 12 semanas) con defecto de 4 semanas (CA-19.02). Al tocar → F3 (CA-19.01, CA-19.02) |
| 5 | Velocidad de Carga | Lista interactiva | Body, cuarta sección | Ejercicios con Kg/sesión. Peso corporal excluido (muestra "N/A"). Al tocar → F3 (CA-19.03, CA-19.05) |
| 6 | Enlace "Volumen por Grupo Muscular" | Link de navegación | Body, accesos rápidos | Navega a G2 |
| 7 | Enlace "Tendencia de Progresión" | Link de navegación | Body, accesos rápidos | Navega a G3 |
| 8 | Bottom Navigation | Barra fija | Bottom Bar | Métricas marcado como activo |

---

#### G2 — Volumen por Grupo Muscular

**Contexto:** Detalle de tonelaje acumulado y distribución de volumen por zona muscular por microciclo. Accesible desde G1. Incluye Bottom Navigation.

**HU de referencia:** HU-20 (CA-20.01 a CA-20.06), HU-15 (CA-25.01 a CA-25.04, pre-renumeración)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Volumen por       │
│           Grupo Muscular")           │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Selector de microciclo }          │
│  [ ◀ Microciclo 11 ▶ ]             │
│                                      │
│  ---                                 │
│                                      │
│  { Tonelaje por grupo muscular }     │
│  ┌────────────────────────────────┐  │
│  │ Pecho          4,200 Kg  ████ │  │
│  │ Espalda        3,800 Kg  ███  │  │
│  │ Abdomen        1,200 Kg  █    │  │
│  │ Hombro         2,500 Kg  ██   │  │
│  │ Tríceps        1,800 Kg  █    │  │
│  │ Bíceps         1,600 Kg  █    │  │
│  │ Cuádriceps     5,100 Kg  █████│  │
│  │ Isquiotibiales 2,200 Kg  ██   │  │
│  │ Glúteos        3,000 Kg  ███  │  │
│  │ Aductores      1,400 Kg  █    │  │
│  │ Abductores     1,100 Kg  █    │  │
│  │ Gemelos          800 Kg  █    │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Distribución de volumen (%) }     │
│  ┌────────────────────────────────┐  │
│  │ (Distribución de series por    │  │
│  │  zona muscular vs. total de la │  │
│  │  rutina)                      │  │
│  │                                │  │
│  │ Pecho       22%  ██████       │  │
│  │ Espalda     18%  █████        │  │
│  │ Abdomen     15%  ████         │  │
│  │ ...etc por rutina...           │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Evolución temporal }              │
│  ┌────────────────────────────────┐  │
│  │  Kg                            │  │
│  │  5K│  ╱╲   ╱───              │  │
│  │  4K│ ╱  ╲╱╱                   │  │
│  │  3K│╱                          │  │
│  │    └──┬──┬──┬──┬──→           │  │
│  │      M8 M9 M10 M11 Micro     │  │
│  │  — Pecho  — Espalda  — ...    │  │
│  └────────────────────────────────┘  │
│  → Tendencias: ascendente, estable   │
│    o en caída por grupo              │
│                                      │
│  { Estado vacío }                    │
│  ("Se necesitan al menos 2           │
│   microciclos para mostrar           │
│   evolución comparativa.")           │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                              (●)     │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a G1 |
| 2 | Título | Texto estático | Top Bar | "Volumen por Grupo Muscular" |
| 3 | Selector de microciclo | Stepper / paginator | Body, encabezado | Flechas ◀▶ para navegar entre microciclos completados |
| 4 | Tonelaje por grupo muscular | Barras horizontales con valor | Body | Desglose del Σ Peso × Reps por cada zona muscular del microciclo seleccionado (CA-20.01). Ejercicios multizona contabilizan en cada zona (CA-20.03) |
| 5 | Distribución de volumen | Barras porcentuales | Body | % de series por zona vs. total de la rutina (CA-20.04) |
| 6 | Evolución temporal | Gráfico multilínea | Body | Tonelaje por grupo a lo largo de microciclos. Permite identificar tendencias (HU-15, CA-25.01, CA-25.02 pre-renumeración) |
| 7 | Estado vacío | Texto informativo | Body | Si < 2 microciclos: "Se necesitan al menos 2 microciclos para mostrar evolución comparativa." (HU-15, CA-25.04 pre-renumeración) |
| 8 | Bottom Navigation | Barra fija | Bottom Bar | Métricas marcado como activo |

---

#### G3 — Tendencia de Progresión por Grupo Muscular

**Contexto:** Evaluación a largo plazo de la trayectoria de cada grupo muscular. Accesible desde G1. Incluye Bottom Navigation.

**HU de referencia:** HU-22 (CA-22.01 a CA-22.05)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Tendencia de      │
│           Progresión")               │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  (Evaluación: últimos 4-6            │
│   microciclos)                       │
│                                      │
│  { Lista de grupos musculares }      │
│  ┌────────────────────────────────┐  │
│  │ Pecho         Ascendente  📈  │  │
│  ├────────────────────────────────┤  │
│  │ Espalda       Estable     📊  │  │
│  ├────────────────────────────────┤  │
│  │ Abdomen       En declive  📉  │  │
│  ├────────────────────────────────┤  │
│  │ Hombro        Ascendente  📈  │  │
│  ├────────────────────────────────┤  │
│  │ Tríceps       Estable     📊  │  │
│  ├────────────────────────────────┤  │
│  │ Bíceps        Ascendente  📈  │  │
│  ├────────────────────────────────┤  │
│  │ Cuádriceps    Estable     📊  │  │
│  ├────────────────────────────────┤  │
│  │ Isquiotibiales En declive 📉  │  │
│  ├────────────────────────────────┤  │
│  │ Glúteos       Ascendente  📈  │  │
│  ├────────────────────────────────┤  │
│  │ Aductores     Estable     📊  │  │
│  ├────────────────────────────────┤  │
│  │ Abductores    Estable     📊  │  │
│  ├────────────────────────────────┤  │
│  │ Gemelos       En declive  📉  │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Estado: datos insuficientes }     │
│  ("Se necesitan al menos 4           │
│   microciclos completados.           │
│   Faltan N microciclos.")            │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                              (●)     │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a G1 |
| 2 | Título | Texto estático | Top Bar | "Tendencia de Progresión" |
| 3 | Período evaluado | Texto informativo | Body, encabezado | "Evaluación: últimos 4-6 microciclos" |
| 4 | Lista de grupos musculares | Lista de solo lectura | Body, scrollable | Cada fila: nombre del grupo + clasificación (Ascendente 📈 / Estable 📊 / En declive 📉). Desglose por los 12 grupos musculares del sistema (CA-22.02, CA-22.05) |
| 5 | Estado datos insuficientes | Texto informativo | Body | Si < 4 microciclos: indica cuántos faltan (CA-22.04) |
| 6 | Bottom Navigation | Barra fija | Bottom Bar | Métricas marcado como activo |

---

#### H1 — Centro de Alertas

**Contexto:** Listado de todas las alertas activas. Accesible incondicionalmente desde B1 (badge de alertas, siempre visible). Incluye Bottom Navigation.

**HU de referencia:** HU-14, HU-16, HU-26 a HU-30
**HU indirectas visibles:** HU-12 (detección de fatiga acumulada → alerta de descarga), HU-15 (recomendaciones correctivas visibles en H2)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Alertas")        │
│  (Subtítulo: "N alertas activas")    │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Sección: Crisis }                 │
│  ┌────────────────────────────────┐  │
│  │ 🔴 Tasa de progresión < 20%   │  │
│  │ Remo con Inclinación          │  │
│  │ (Progresión: 15% — Crítico)   │  │
│  ├────────────────────────────────┤  │
│  │ 🔴 Inactividad > 14 días      │  │
│  │ Rutina 3                      │  │
│  │ (18 días sin sesión)          │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Sección: Alertas }                │
│  ┌────────────────────────────────┐  │
│  │ 🟠 Meseta detectada           │  │
│  │ Press de mancuerna            │  │
│  │ (3 sesiones sin progresión)   │  │
│  ├────────────────────────────────┤  │
│  │ 🟠 Rutina requiere descarga    │  │
│  │ Rutina 1                      │  │
│  │ (≥ 50% ej. en meseta/regr.)  │  │
│  ├────────────────────────────────┤  │
│  │ 🟡 RIR fuera de rango         │  │
│  │ Rutina 2 — RIR 1.2            │  │
│  │ (< 0.5 en 2+ sesiones)       │  │
│  ├────────────────────────────────┤  │
│  │ 🟡 Adherencia baja            │  │
│  │ Semana actual: 50%            │  │
│  │ (< 60% del objetivo)         │  │
│  ├────────────────────────────────┤  │
│  │ 🟡 Caída de tonelaje > 10%    │  │
│  │ Grupo: Cuádriceps             │  │
│  │ (Tonelaje −12% vs. anterior)  │  │
│  └────────────────────────────────┘  │
│  → Ordenadas: crisis primero,        │
│    alertas después                   │
│  → Al tocar una alerta → H2         │
│                                      │
│  { Estado vacío }                    │
│  ("No hay alertas activas. ✅        │
│   Todo en orden.")                   │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│  (●)                                │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a B1 |
| 2 | Título "Alertas" | Texto estático | Top Bar | Identifica la pantalla |
| 3 | Contador de alertas | Texto dinámico | Top Bar, subtítulo | "N alertas activas" |
| 4 | Sección "Crisis" | Agrupación visual | Body, primera | Alertas de nivel crisis (🔴): progresión < 20%, inactividad > 14 días, caída tonelaje > 20%. Prioridad visual máxima |
| 5 | Sección "Alertas" | Agrupación visual | Body, segunda | Alertas de nivel alerta (🟠🟡): meseta, descarga requerida, RIR fuera de rango, adherencia baja, progresión < 40%, caída tonelaje > 10%, inactividad > 10 días |
| 6 | Fila de alerta | Elemento interactivo | Dentro de cada sección | Icono de nivel + tipo de alerta + entidad afectada (ejercicio/rutina/grupo) + dato clave. Al tocar → H2 |
| 7 | Estado vacío | Texto informativo | Body | "No hay alertas activas. ✅ Todo en orden." (si el ejecutante accede sin alertas) |
| 8 | Bottom Navigation | Barra fija | Bottom Bar | Inicio marcado como activo (se llega desde B1) |

**Tipos de alerta soportados:**

| Tipo | Entidad | Niveles |
|------|---------|---------|
| Meseta | Ejercicio | 🟠 Alerta (3 sesiones sin progresión) |
| Tasa de progresión baja | Ejercicio | 🟡 Alerta (< 40%), 🔴 Crisis (< 20%) |
| RIR fuera de rango | Rutina | 🟡 Alerta (< 0.5 o > 1.8 sostenido 2+ sesiones) |
| Adherencia baja | Semanal | 🟡 Alerta (< 60%, 1 semana), 🔴 Crisis (< 60%, 2+ semanas consecutivas) |
| Caída de tonelaje | Grupo muscular | 🟡 Alerta (> 10%), 🔴 Crisis (> 20%) |
| Inactividad por rutina | Rutina | 🟡 Alerta (> 10 días), 🔴 Crisis (> 14 días) |
| Rutina requiere descarga | Rutina | 🟠 Alerta (≥ 50% ejercicios en meseta/regresión) |

---

#### H2 — Detalle de Alerta

**Contexto:** Información completa de una alerta incluyendo análisis causal y recomendaciones. Accesible desde H1. Incluye Bottom Navigation.

**HU de referencia:** HU-14 (CA-14.04 a CA-14.06), HU-15, HU-16, HU-26 a HU-30
**HU indirecta visible:** HU-12 (detección de fatiga/regresión — contextualiza alertas de caída de tonelaje vs. descarga planificada)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Detalle de        │
│           Alerta")                   │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  { Card: Tipo y nivel }              │
│  ┌────────────────────────────────┐  │
│  │ 🟠 MESETA DETECTADA           │  │
│  │ (Press de mancuerna)          │  │
│  │ (3 sesiones sin progresión)   │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Datos que dispararon la alerta }  │
│  ┌────────────────────────────────┐  │
│  │ Sesión 1: 22.5 Kg · 38 reps  │  │
│  │ Sesión 2: 22.5 Kg · 37 reps  │  │
│  │ Sesión 3: 22.5 Kg · 36 reps  │  │
│  │ (Período: 27 ene — 10 feb)   │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Análisis causal }                 │
│  ┌────────────────────────────────┐  │
│  │ ("RIR promedio = 1.2 en las   │  │
│  │  últimas 3 sesiones")         │  │
│  │                                │  │
│  │ ("El ejecutante está          │  │
│  │  entrenando cerca del fallo   │  │
│  │  técnico — posible límite de  │  │
│  │  carga con este peso. El      │  │
│  │  cuerpo no tiene reserva      │  │
│  │  suficiente para progresar.") │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Recomendaciones }                 │
│  ┌────────────────────────────────┐  │
│  │ ▸ Sesión 4: intentar          │  │
│  │   microincremento (+2.5 Kg)   │  │
│  │   o extensión de reps         │  │
│  │ ▸ Sesión 6+: considerar       │  │
│  │   rotar versión de la rutina   │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Acciones }                        │
│  [ Ver historial del ejercicio → ]   │
│    → Navega a F3                     │
│  [ Gestionar descarga → ]            │
│    → Solo si alerta de descarga;     │
│      navega a I1                     │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│  (●)                                │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a H1 |
| 2 | Título "Detalle de Alerta" | Texto estático | Top Bar | Identifica la pantalla |
| 3 | Card tipo y nivel | Card informativa | Body, encabezado | Icono de nivel + nombre del tipo de alerta + entidad afectada + dato resumen |
| 4 | Datos que dispararon la alerta | Sección de solo lectura | Body | Datos concretos: sesiones, valores, período evaluado |
| 5 | Análisis causal | Sección de texto | Body | Para mesetas: análisis basado en RIR (CA-14.04: RIR bajo → límite de carga, CA-14.05: RIR alto → carga conservadora, CA-14.06: estancamiento grupal → fatiga sistémica). Para caída de tonelaje: verificación de si es descarga planificada o regresión |
| 6 | Recomendaciones | Lista de acciones sugeridas | Body | Escalonadas por sesión. Informativas, no bloqueantes |
| 7 | Enlace "Ver historial del ejercicio" | Link de navegación | Body, acciones | Navega a F3. Visible en alertas de meseta y progresión baja |
| 8 | Enlace "Gestionar descarga" | Link de navegación condicional | Body, acciones | Navega a I1. Solo visible en alertas de descarga/fatiga |
| 9 | Bottom Navigation | Barra fija | Bottom Bar | Inicio marcado como activo |

---

#### I1 — Gestión de Descarga

**Contexto:** Estado y control del modo de descarga. Accesible desde B1 (indicador de descarga) y H2 (enlace "Gestionar descarga"). Incluye Bottom Navigation.

**HU de referencia:** HU-16 (CA-16.01, CA-16.04), HU-17 (CA-17.01 a CA-17.09)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Gestión de        │
│           Descarga")                 │
│                                      │
├──────────────────────────────────────┤
│              BODY (scrollable)       │
│                                      │
│  --- Estado A: Sin descarga activa,  │
│      rutina requiere descarga ---    │
│                                      │
│  { Card: Estado actual }             │
│  ┌────────────────────────────────┐  │
│  │ ("No hay descarga activa")    │  │
│  │                                │  │
│  │ ⚠️ ("Rutina 1 requiere         │  │
│  │  descarga")                   │  │
│  │ ("≥ 50% de los ejercicios     │  │
│  │  están en meseta o regresión")│  │
│  └────────────────────────────────┘  │
│                                      │
│  { Información del protocolo }       │
│  ┌────────────────────────────────┐  │
│  │ Al activar la descarga:       │  │
│  │ · Carga ajustada al 60%       │  │
│  │ · Series prescritas            │  │
│  │ · 8 repeticiones              │  │
│  │ · RIR objetivo: 2             │  │
│  │ · Duración: 1 microciclo      │  │
│  │   (todas las rutinas)        │  │
│  │ · Versiones congeladas        │  │
│  │ · Peso corporal: 8 reps,      │  │
│  │   RIR 2 (sin ajuste de        │  │
│  │   carga, Peso = 0)            │  │
│  │ · Isométricos: 30 seg,        │  │
│  │   RIR 2                      │  │
│  │ · Al finalizar: reinicio al   │  │
│  │   90% de carga pre-descarga   │  │
│  └────────────────────────────────┘  │
│                                      │
│  [ Botón primario:                   │
│    "Activar Descarga" ]              │
│    → Activa modo descarga para       │
│      todas las rutinas               │
│                                      │
│  --- Estado B: Descarga activa ---   │
│                                      │
│  { Card: Descarga en progreso }      │
│  ┌────────────────────────────────┐  │
│  │ 🔄 ("Descarga activa")        │  │
│  │                                │  │
│  │ Progreso: 3/6 sesiones        │  │
│  │ [ ████████░░░░░░░░ ] 50%     │  │
│  │                                │  │
│  │ Sesiones restantes: 3         │  │
│  └────────────────────────────────┘  │
│                                      │
│  { Parámetros vigentes }             │
│  ┌────────────────────────────────┐  │
│  │ Carga: 60% de la habitual     │  │
│  │ Series: 4                     │  │
│  │ Reps: 8                       │  │
│  │ RIR objetivo: 2               │  │
│  │ Versión congelada: R1-V2,     │  │
│  │  R2-V1, R3-V3                │  │
│  └────────────────────────────────┘  │
│                                      │
│  --- Estado C: Post-descarga ---     │
│                                      │
│  { Card: Cargas de reinicio }        │
│  ┌────────────────────────────────┐  │
│  │ ✅ ("Descarga completada")    │  │
│  │                                │  │
│  │ ("Cargas de reinicio          │  │
│  │  (90% pre-descarga):")        │  │
│  │                                │  │
│  │ Press de banca:     54 Kg     │  │
│  │ Press de mancuerna: 20 Kg     │  │
│  │ Remo con Inclinación: 36 Kg  │  │
│  │       ...más ejercicios...    │  │
│  │                                │  │
│  │ ("Las versiones retoman su    │  │
│  │  avance normal.")             │  │
│  └────────────────────────────────┘  │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│  (●)                                │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a la vista de origen (B1 o H2) |
| 2 | Título | Texto estático | Top Bar | "Gestión de Descarga" |
| 3 | Card estado actual | Card dinámica | Body | Varía según el estado (ver estados abajo) |
| 4 | Información del protocolo | Sección informativa | Body (Estado A) | Detalle de los parámetros de la descarga antes de activar, incluyendo excepciones para peso corporal (8 reps, RIR 2, Peso=0) e isométricos (30 seg, RIR 2) según CA-17.09 |
| 5 | Botón "Activar Descarga" | Botón primario | Body (Estado A) | Activa modo descarga. La señal es informativa (CA-16.04): el ejecutante decide activarla |
| 6 | Progreso de descarga | Barra de progreso | Body (Estado B) | "N/T sesiones" (T = total de rutinas del plan) + barra porcentual + sesiones restantes |
| 7 | Parámetros vigentes | Sección informativa | Body (Estado B) | Carga 60%, series prescritas, 8 reps, RIR 2, versiones congeladas por rutina |
| 8 | Cargas de reinicio | Lista informativa | Body (Estado C) | Post-descarga: 90% de la carga pre-descarga por ejercicio (CA-17.05, CA-17.06) |
| 9 | Bottom Navigation | Barra fija | Bottom Bar | Inicio marcado como activo |

**Estados de la pantalla:**

| Estado | Condición | Contenido principal |
|--------|-----------|---------------------|
| A — Descarga requerida | Rutina señalada por motor de reglas, descarga no activa | Indicación de rutina que requiere descarga + protocolo + botón "Activar Descarga" |
| B — Descarga activa | Modo descarga activo, sesiones en progreso | Progreso N/T (T = total de rutinas), parámetros vigentes, versiones congeladas |
| C — Post-descarga | Descarga recién completada (primera sesión post-descarga) | Cargas de reinicio al 90% por ejercicio, aviso de versiones retomando avance |

---

#### J1 — Ajustes

**Contexto:** Menú de opciones del sistema. Accesible desde la navegación global (sección "Ajustes"). Incluye Bottom Navigation.

**HU de referencia:** HU-21 (CA-21.05 — objetivo de frecuencia), HU-31, HU-32

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  (Título: "Ajustes")          │
│                                      │
├──────────────────────────────────────┤
│              BODY                    │
│                                      │
│  { Sección: Perfil }                 │
│  ┌────────────────────────────────┐  │
│  │ [ Editar perfil → ]           │  │
│  │   → Navega a C1               │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Sección: Entrenamiento }          │
│  ┌────────────────────────────────┐  │
│  │ (Objetivo de frecuencia       │  │
│  │  semanal)                     │  │
│  │ [ Selector: 4 5 ●6 ]          │  │
│  │   sesiones/semana             │  │
│  │   → Rango: 4 a 6             │  │
│  │   → Usado para cálculo de     │  │
│  │     Adherencia (HU-21)        │  │
│  └────────────────────────────────┘  │
│                                      │
│  ---                                 │
│                                      │
│  { Sección: Datos }                  │
│  ┌────────────────────────────────┐  │
│  │ [ Exportar respaldo → ]       │  │
│  │   → Navega a J2               │  │
│  │                                │  │
│  │ [ Importar respaldo → ]       │  │
│  │   → Navega a J3               │  │
│  └────────────────────────────────┘  │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                                (●)   │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Título "Ajustes" | Texto estático | Top Bar | Identifica la sección |
| 2 | Enlace "Editar perfil" | Link de navegación | Body, sección Perfil | Navega a C1 (Perfil del Ejecutante) |
| 3 | Selector de frecuencia semanal | Selector numérico (chips) | Body, sección Entrenamiento | Opciones: 4, 5, 6 sesiones/semana. Selección de un toque. Persiste inmediatamente. Usado como denominador del Índice de Adherencia (CA-21.05) |
| 4 | Enlace "Exportar respaldo" | Link de navegación | Body, sección Datos | Navega a J2 |
| 5 | Enlace "Importar respaldo" | Link de navegación | Body, sección Datos | Navega a J3 |
| 6 | Bottom Navigation | Barra fija | Bottom Bar | Ajustes marcado como activo |

---

#### J2 — Exportar Respaldo

**Contexto:** Pantalla para ejecutar la exportación de backup. Accesible desde J1. Incluye Bottom Navigation.

**HU de referencia:** HU-31 (CA-31.01 a CA-31.07)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Exportar          │
│           Respaldo")                 │
│                                      │
├──────────────────────────────────────┤
│              BODY                    │
│                                      │
│  --- Antes de exportar ---           │
│                                      │
│  { Advertencia }                     │
│  ┌────────────────────────────────┐  │
│  │ ⚠️ ("El archivo de respaldo    │  │
│  │  contiene todos tus datos de   │  │
│  │  entrenamiento y no está       │  │
│  │  cifrado.")                    │  │
│  └────────────────────────────────┘  │
│                                      │
│  [ Botón primario:                   │
│    "Exportar datos" ]                │
│                                      │
│  --- Durante exportación ---         │
│                                      │
│  { Indicador de progreso }           │
│  ┌────────────────────────────────┐  │
│  │ ("Exportando datos...")       │  │
│  │ [ ████████████░░░░ ] 75%     │  │
│  └────────────────────────────────┘  │
│                                      │
│  --- Exportación completada ---      │
│                                      │
│  { Confirmación }                    │
│  ┌────────────────────────────────┐  │
│  │ ✅ ("Respaldo exportado")     │  │
│  │ ("Archivo: tension_backup_    │  │
│  │  20260210.json")              │  │
│  │ ("Ubicación: Descargas/")     │  │
│  └────────────────────────────────┘  │
│                                      │
│  [ Botón: "Compartir" ]             │
│    → Abre menú de compartir del      │
│      sistema (Drive, correo, etc.)   │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                                (●)   │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a J1 (antes o después de exportar) |
| 2 | Título | Texto estático | Top Bar | "Exportar Respaldo" |
| 3 | Advertencia | Card informativa | Body (pre-export) | Avisa que el archivo no está cifrado (CA-31.05) |
| 4 | Botón "Exportar datos" | Botón primario | Body (pre-export) | Inicia el proceso de exportación |
| 5 | Indicador de progreso | Barra de progreso | Body (durante) | Muestra porcentaje de avance (CA-31.07). Proceso < 10s (CA-31.04) |
| 6 | Confirmación | Card informativa | Body (post-export) | Nombre del archivo + ubicación (CA-31.07) |
| 7 | Botón "Compartir" | Botón secundario | Body (post-export) | Abre share sheet del sistema (CA-31.03) |
| 8 | Bottom Navigation | Barra fija | Bottom Bar | Ajustes marcado como activo |

---

#### J3 — Importar Respaldo

**Contexto:** Pantalla para la restauración de datos desde backup. Accesible desde J1. Incluye Bottom Navigation.

**HU de referencia:** HU-32 (CA-32.01 a CA-32.08)

```
┌──────────────────────────────────────┐
│              TOP BAR                 │
│                                      │
│  [ ← ]  (Título: "Importar          │
│           Respaldo")                 │
│                                      │
├──────────────────────────────────────┤
│              BODY                    │
│                                      │
│  --- Paso 1: Selección ---           │
│                                      │
│  [ Botón: "Seleccionar archivo" ]    │
│    → Abre selector de archivos       │
│      del sistema                     │
│                                      │
│  --- Paso 2: Validación ---          │
│                                      │
│  { Resultado de validación }         │
│  ┌────────────────────────────────┐  │
│  │ ✅ ("Archivo válido")         │  │
│  │ (Versión: 1.2)               │  │
│  │ (Fecha del respaldo:          │  │
│  │  08 feb 2026)                 │  │
│  │ (Sesiones incluidas: 142)    │  │
│  └────────────────────────────────┘  │
│  — o —                               │
│  ┌────────────────────────────────┐  │
│  │ ❌ ("Archivo no válido")      │  │
│  │ ("El archivo seleccionado     │  │
│  │  no es un respaldo válido     │  │
│  │  o está corrupto.")           │  │
│  └────────────────────────────────┘  │
│                                      │
│  --- Paso 3: Confirmación ---        │
│                                      │
│  { Advertencia de reemplazo }        │
│  ┌────────────────────────────────┐  │
│  │ ⚠️ ("ATENCIÓN: Todos los       │  │
│  │  datos actuales serán          │  │
│  │  reemplazados por los datos    │  │
│  │  del respaldo. Esta operación  │  │
│  │  no es reversible.")           │  │
│  └────────────────────────────────┘  │
│                                      │
│  [ Botón destructivo:               │
│    "Restaurar datos" ]               │
│                                      │
│  [ Enlace: "Cancelar" ]             │
│    → Retorna a J1 sin cambios        │
│                                      │
│  --- Paso 4: Importación ---         │
│                                      │
│  { Indicador de progreso }           │
│  ┌────────────────────────────────┐  │
│  │ ("Restaurando datos...")      │  │
│  │ [ ████████████░░░░ ] 75%     │  │
│  └────────────────────────────────┘  │
│                                      │
│  --- Resultado ---                   │
│                                      │
│  ✅ ("Datos restaurados             │
│   exitosamente.")                    │
│  → Navega automáticamente a B1       │
│    con los datos restaurados         │
│                                      │
│  — o si falla —                      │
│                                      │
│  ❌ ("La importación falló. Tus     │
│   datos originales han sido          │
│   preservados.")                     │
│  [ Botón: "Volver a                  │
│    Ajustes" ]                  │
│    → Retorna a J1                    │
│                                      │
├──────────────────────────────────────┤
│          BOTTOM NAVIGATION           │
│  🏠        📖       📋       📊    ⚙️ │
│ Inicio  Diccionario Historial Métr. Config│
│                                (●)   │
└──────────────────────────────────────┘
```

**Elementos y comportamiento:**

| # | Elemento | Tipo | Posición | Comportamiento |
|---|----------|------|----------|----------------|
| 1 | Botón ← (retorno) | Botón de navegación | Top Bar, izquierda | Retorna a J1 (si aún no se confirmó la importación) |
| 2 | Título | Texto estático | Top Bar | "Importar Respaldo" |
| 3 | Botón "Seleccionar archivo" | Botón de acción | Body, paso 1 | Abre el file picker del sistema para seleccionar archive de backup (CA-32.01) |
| 4 | Resultado de validación | Card informativa | Body, paso 2 | Si válido: versión, fecha, contenido resumen. Si inválido: mensaje de error claro (CA-32.02) |
| 5 | Advertencia de reemplazo | Card de advertencia | Body, paso 3 | Texto explícito: todos los datos serán reemplazados, operación no reversible (CA-32.03) |
| 6 | Botón "Restaurar datos" | Botón destructivo | Body, paso 3 | Estilo de advertencia (rojo/destructivo). Inicia la importación |
| 7 | Enlace "Cancelar" | Link de acción | Body, paso 3 | Cancela y retorna a J1 sin cambios |
| 8 | Indicador de progreso | Barra de progreso | Body, paso 4 | Muestra avance. Proceso < 10s (CA-32.06, CA-32.07) |
| 9 | Éxito | Texto + navegación automática | Body, resultado | "Datos restaurados exitosamente." → navega a B1 (CA-32.04) |
| 10 | Error / Rollback | Texto + botón | Body, resultado | "La importación falló. Tus datos originales han sido preservados." (CA-32.08). Botón para retornar a J1 |
| 11 | Bottom Navigation | Barra fija | Bottom Bar | Ajustes marcado como activo |
