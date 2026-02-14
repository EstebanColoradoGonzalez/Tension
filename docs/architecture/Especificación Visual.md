# Especificación Visual (Alta Fidelidad) — Tension

---

## 1. Propósito

Este documento define el sistema de diseño visual completo de Tension: paleta de colores, tipografía, dimensiones, espaciado, estilos de componentes e iconografía. Complementa los Wireframes de Baja Fidelidad con las decisiones de presentación visual necesarias para implementar la capa View en Jetpack Compose con Material 3.

No es un mockup gráfico. Es una especificación textual-tabular que un desarrollador puede traducir directamente a código Compose (Theme.kt, Color.kt, Type.kt).

---

## 2. Restricciones técnicas aplicables

| Restricción | Fuente | Impacto en diseño |
|-------------|--------|--------------------|
| Jetpack Compose + Material 3 | RNF34 | Usar Material 3 Design Tokens, componentes M3 y esquema de color dinámico |
| Tema claro/oscuro del sistema | RNF23 | Definir esquemas para ambos modos; respetar la configuración del dispositivo |
| Tamaño mínimo interactivo 48×48 dp | RNF06 | Todos los botones, campos y elementos tocables ≥ 48 dp |
| Solo modo vertical (portrait) | RNF07 | Layout diseñado exclusivamente para orientación vertical |
| Idioma español exclusivo | RNF08 | Sin consideraciones de longitud variable por traducción |
| Pantallas 5"–7", 720p–1440p | RNF21, RNF22 | Diseño adaptable a densidades mdpi–xxxhdpi |
| Señales distinguibles por color + ícono | RNF05 | Progresión/mantenimiento/regresión nunca solo por color |

---

## 3. Identidad Visual

### 3.1 Concepto

Tension evoca disciplina, fuerza y estructura. La identidad se inspira en el rojo del Imperio Romano — un tono profundo y autoritario — combinado con superficies cálidas y neutras que transmiten calma y legibilidad. El resultado es una app que se siente seria y funcional sin ser agresiva.

### 3.2 Color Seed

El sistema de color Material 3 se genera a partir de un **color seed** único. Todas las variantes (primary, secondary, tertiary, surface, etc.) se derivan algorítmicamente de este seed.

**Color seed:** `#8B1A1A` — Rojo Imperio Romano (Dark Red)

> **Nota sobre color dinámico:** El esquema de color se genera a partir del seed `#8B1A1A`. Material You dynamic color (derivado del wallpaper del dispositivo) **NO se utiliza** — el esquema propio de Tension siempre prevalece. La preferencia del sistema entre modo claro y modo oscuro **SÍ se respeta** para seleccionar entre §4.1 y §4.2.

### 3.3 Logotipo

Tension cuenta con un logotipo oficial en formato PNG (`logo.png`) que refuerza la identidad visual de la aplicación.

- **Archivo fuente:** `res/drawable/logo.png`
- **Formato:** PNG con transparencia
- **Uso como ícono de la app:** El logotipo se utiliza como `foreground` del Adaptive Icon de Android, sobre un fondo sólido `#8B1A1A` (Primary). Esto garantiza coherencia entre la identidad de la app en el launcher y dentro de la aplicación.
- **Accesibilidad:** Todas las instancias del logo incluyen `contentDescription` descriptivo ("Logo de Tension").

#### Uso del logotipo en pantallas

| Pantalla | Tamaño | Posición | Propósito |
|----------|--------|----------|-----------|
| Splash / Loading | 120 dp | Centro de la pantalla, sobre el nombre "Tension" | Identidad de marca durante la carga inicial |
| A1 — Registro de Perfil (Onboarding) | 120 dp | Centrado, sobre el título "Tension" y el subtítulo | Primer contacto visual con la marca, establece confianza |
| B1 — Home | 80 dp | Centrado en el body, sobre el texto de bienvenida | Refuerzo de identidad en el punto de entrada recurrente |
| C1 — Perfil del Ejecutante | 120 dp | Centrado, sobre el formulario de edición | Coherencia visual con el onboarding |

> **Nota:** El logotipo siempre se presenta centrado horizontalmente. No se distorsiona ni se recorta. El tamaño varía según la jerarquía de la pantalla: más grande en el splash (impacto visual), mediano en onboarding (prominencia), y compacto en home (presencia sutil).

---

## 4. Paleta de Colores

### 4.1 Esquema Claro (Light Theme)

| Rol M3 | Hex | Uso en Tension |
|---------|-----|----------------|
| **Primary** | `#8B1A1A` | Botones primarios, FAB, elementos de acción principal, Bottom Nav activo |
| **On Primary** | `#FFFFFF` | Texto e íconos sobre elementos Primary |
| **Primary Container** | `#F5DDDD` | Fondo de cards destacadas (Próxima Sesión, Estado de Descarga), FAB crear ejercicio |
| **On Primary Container** | `#5C0E0E` | Texto sobre Primary Container |
| **Secondary** | `#6B4F4F` | Botones secundarios (Cerrar Sesión, Compartir), elementos de soporte |
| **On Secondary** | `#FFFFFF` | Texto sobre elementos Secondary |
| **Secondary Container** | `#F0E0E0` | Fondo de cards de información secundaria, secciones de métricas |
| **On Secondary Container** | `#4A3333` | Texto sobre Secondary Container |
| **Tertiary** | `#5C6B4F` | Acentos complementarios, enlaces de navegación, tendencia ascendente en gráficos |
| **On Tertiary** | `#FFFFFF` | Texto sobre elementos Tertiary |
| **Tertiary Container** | `#E0EEDD` | Fondo de cards de éxito, badges de estado positivo |
| **On Tertiary Container** | `#334A29` | Texto sobre Tertiary Container |
| **Background** | `#FFF8F0` | Fondo general de la app (crema cálido) |
| **On Background** | `#1C1B1B` | Texto principal sobre fondo |
| **Surface** | `#FFF8F0` | Superficies de cards, diálogos, sheets |
| **On Surface** | `#1C1B1B` | Texto principal sobre superficies |
| **Surface Variant** | `#F5E6D8` | Fondo de filas de lista, separadores, elementos secundarios |
| **On Surface Variant** | `#524340` | Texto secundario, labels, subtítulos |
| **Outline** | `#857370` | Bordes de campos de texto, divisores, contornos de componentes |
| **Outline Variant** | `#D8C2BD` | Divisores sutiles, bordes de cards |
| **Error** | `#BA1A1A` | Estados de error, validación fallida, alertas de crisis |
| **On Error** | `#FFFFFF` | Texto sobre Error |
| **Error Container** | `#FFDAD6` | Fondo de mensajes de error inline, cards de crisis |
| **On Error Container** | `#410002` | Texto sobre Error Container |
| **Surface Container Lowest** | `#FFFFFF` | Bottom Navigation, Top Bar |
| **Surface Container Low** | `#FEF1E6` | Cards elevadas |
| **Surface Container** | `#F8EBE0` | Diálogos, modales |
| **Surface Container High** | `#F2E5DA` | Cards con mayor énfasis |
| **Surface Container Highest** | `#EDE0D5` | Inputs, campos de formulario |
| **Inverse Surface** | `#322F2E` | Snackbars, tooltips |
| **Inverse On Surface** | `#FBF0EB` | Texto en Inverse Surface |

### 4.2 Esquema Oscuro (Dark Theme)

| Rol M3 | Hex | Uso en Tension |
|---------|-----|----------------|
| **Primary** | `#FFB4AB` | Botones primarios, elementos de acción principal |
| **On Primary** | `#690005` | Texto sobre Primary |
| **Primary Container** | `#6E1010` | Fondo de cards destacadas |
| **On Primary Container** | `#FFDAD6` | Texto sobre Primary Container |
| **Secondary** | `#E0C0C0` | Botones secundarios, elementos de soporte |
| **On Secondary** | `#3E2828` | Texto sobre Secondary |
| **Secondary Container** | `#523A3A` | Cards secundarias |
| **On Secondary Container** | `#F0E0E0` | Texto sobre Secondary Container |
| **Tertiary** | `#C0D4B0` | Acentos, enlaces, tendencia ascendente |
| **On Tertiary** | `#2A3A20` | Texto sobre Tertiary |
| **Tertiary Container** | `#425238` | Cards de éxito |
| **On Tertiary Container** | `#E0EEDD` | Texto sobre Tertiary Container |
| **Background** | `#1C1B1B` | Fondo general |
| **On Background** | `#E8E0DC` | Texto principal |
| **Surface** | `#1C1B1B` | Superficies |
| **On Surface** | `#E8E0DC` | Texto principal |
| **Surface Variant** | `#524340` | Filas, separadores |
| **On Surface Variant** | `#D8C2BD` | Texto secundario |
| **Outline** | `#A08C88` | Bordes, contornos |
| **Outline Variant** | `#524340` | Bordes sutiles |
| **Error** | `#FFB4AB` | Error, crisis |
| **On Error** | `#690005` | Texto sobre Error |
| **Error Container** | `#930009` | Cards de error |
| **On Error Container** | `#FFDAD6` | Texto sobre Error Container |
| **Surface Container Lowest** | `#141313` | Bottom Nav, Top Bar |
| **Surface Container Low** | `#1C1B1B` | Cards |
| **Surface Container** | `#252322` | Diálogos |
| **Surface Container High** | `#302E2D` | Cards énfasis |
| **Surface Container Highest** | `#3B3937` | Inputs |
| **Inverse Surface** | `#E8E0DC` | Snackbars |
| **Inverse On Surface** | `#322F2E` | Texto en Inverse Surface |

### 4.3 Colores Semánticos del Dominio

Estos colores se usan para señales de progresión, estados de ejercicio, alertas y tendencias. Se definen como colores extendidos fuera del esquema M3 estándar.

#### Señales de Progresión (RNF05)

| Señal | Ícono | Color claro | Color oscuro | Uso |
|-------|-------|-------------|--------------|-----|
| Progresión positiva | `↑` | `#2E7D32` (verde bosque) | `#81C784` | E5, F2, F3: ejercicio mejoró |
| Mantenimiento | `=` | `#8D6E00` (ámbar oscuro) | `#FFD54F` | E5, F2, F3: ejercicio estable |
| Regresión | `↓` | `#C62828` (rojo profundo) | `#EF9A9A` | E5, F2, F3: ejercicio empeoró |

#### Estados de Ejercicio en Sesión Activa (E1)

| Estado | Ícono | Color claro | Color oscuro | Fondo fila claro | Fondo fila oscuro |
|--------|-------|-------------|--------------|-------------------|-------------------|
| No Iniciado | `⚪` | `#857370` (outline) | `#A08C88` | Transparente | Transparente |
| En Ejecución | `🔵` | `#1565C0` (azul) | `#64B5F6` | `#E3F2FD` | `#1A2733` |
| Completado | `✅` | `#2E7D32` (verde) | `#81C784` | `#E8F5E9` | `#1A2E1A` |

#### Estados de Progresión de Ejercicio (F3)

| Estado | Indicador | Color claro | Color oscuro |
|--------|-----------|-------------|--------------|
| En Progresión | `🟢` | `#2E7D32` | `#81C784` |
| En Meseta | `🟡` | `#8D6E00` | `#FFD54F` |
| En Descarga | `🔵` | `#1565C0` | `#64B5F6` |
| Sin Historial | `⚪` | `#857370` | `#A08C88` |

#### Niveles de Alerta (H1, H2)

| Nivel | Indicador | Color claro | Color oscuro | Fondo card claro | Fondo card oscuro |
|-------|-----------|-------------|--------------|-------------------|-------------------|
| Crisis | `🔴` | `#C62828` | `#EF9A9A` | `#FFDAD6` | `#930009` |
| Alerta alta | `🟠` | `#E65100` | `#FFB74D` | `#FFF3E0` | `#4E2600` |
| Alerta media | `🟡` | `#8D6E00` | `#FFD54F` | `#FFF8E1` | `#4A3800` |
| Sin alertas | `✅` | `#2E7D32` | `#81C784` | — | — |

#### Tendencias de Grupo Muscular (G3)

| Tendencia | Ícono | Color claro | Color oscuro |
|-----------|-------|-------------|--------------|
| Ascendente | `📈` | `#2E7D32` | `#81C784` |
| Estable | `📊` | `#8D6E00` | `#FFD54F` |
| En declive | `📉` | `#C62828` | `#EF9A9A` |

#### Estado de Sesión Cerrada (E5, F1, F2)

| Estado | Ícono | Color claro | Color oscuro |
|--------|-------|-------------|--------------|
| Completada | `✅` | `#2E7D32` | `#81C784` |
| Incompleta | `⚠️` | `#E65100` | `#FFB74D` |

#### Indicador de Descarga (B1, E1, I1)

| Estado | Color claro | Color oscuro |
|--------|-------------|--------------|
| Descarga activa / requerida | `#1565C0` | `#64B5F6` |

---

## 5. Tipografía

Material 3 define una escala tipográfica con 15 estilos. Tension usa la fuente del sistema (Roboto en Android) sin fuentes personalizadas.

### 5.1 Escala Tipográfica

| Estilo M3 | Tamaño | Peso | Line Height | Uso en Tension |
|-----------|--------|------|-------------|----------------|
| Display Large | 57 sp | 400 | 64 sp | No usado |
| Display Medium | 45 sp | 400 | 52 sp | No usado |
| Display Small | 36 sp | 400 | 44 sp | No usado |
| Headline Large | 32 sp | 400 | 40 sp | Tonelaje total en E5 (número prominente) |
| Headline Medium | 28 sp | 400 | 36 sp | Conteo de microciclos en B1, porcentaje de adherencia en G1 |
| Headline Small | 24 sp | 400 | 32 sp | Títulos de sección en G1, G2 |
| Title Large | 22 sp | 400 | 28 sp | Top Bar: títulos de pantalla (CenterAlignedTopAppBar) |
| Title Medium | 16 sp | 500 | 24 sp | Nombre de ejercicio en listas (E1, D1, D4, F2), encabezados de card |
| Title Small | 14 sp | 500 | 20 sp | Subtítulos de Top Bar, "Serie N de 4" en E2 |
| Body Large | 16 sp | 400 | 24 sp | Contenido principal de cards, información de ejercicio en D2, texto de análisis en H2 |
| Body Medium | 14 sp | 400 | 20 sp | Texto secundario en filas de lista, datos de series (peso × reps · RIR), fechas |
| Body Small | 12 sp | 400 | 16 sp | Etiqueta "Registro inicial" en C2, texto de referencia (30-45 seg), contadores |
| Label Large | 14 sp | 500 | 20 sp | Texto dentro de botones (Registrar, Confirmar, Iniciar Sesión) |
| Label Medium | 12 sp | 500 | 16 sp | Labels de Bottom Navigation, labels de dropdowns de filtro, selector RIR, etiquetas de campo |
| Label Small | 11 sp | 500 | 16 sp | Badge de alertas (número), conteo de ejercicios (11 ej.) |

### 5.2 Color de Texto

| Contexto | Token M3 |
|----------|----------|
| Texto principal (títulos, nombres, valores) | On Surface |
| Texto secundario (subtítulos, labels, metadata) | On Surface Variant |
| Texto sobre botón primario | On Primary |
| Texto de enlace / link de navegación | Primary |
| Texto de error | Error |
| Texto de señal de progresión | Color semántico correspondiente (§4.3) |

---

## 6. Dimensiones y Espaciado

### 6.1 Grid y Márgenes

| Dimensión | Valor | Aplicación |
|-----------|-------|------------|
| Margen horizontal de pantalla | 16 dp | Padding lateral del Body en todas las vistas |
| Margen vertical Top Bar ↔ Body | 8 dp | Separación entre Top Bar y primer elemento del Body |
| Separación vertical entre secciones | 24 dp | Entre cards o secciones dentro del Body (separadores `---`) |
| Separación vertical entre elementos | 16 dp | Entre campos de formulario, entre filas de lista agrupadas |
| Separación interna entre sub-elementos | 8 dp | Entre label y campo, entre ícono y texto, entre líneas de una fila |
| Padding interno de cards | 16 dp | Espacio interior de todas las cards |

### 6.2 Componentes: Dimensiones

| Componente | Altura | Ancho | Otros |
|------------|--------|-------|-------|
| Top Bar (Small/Center) | 64 dp | Full width | Elevation 0, Surface Container Lowest |
| Bottom Navigation | 80 dp | Full width | 5 ítems, Surface Container Lowest |
| Botón primario | 48 dp (mín.) | Full width o wrap content | Corner radius 24 dp (Filled Button M3) |
| Botón secundario | 48 dp (mín.) | Wrap content | Corner radius 24 dp (Outlined Button M3) |
| Botón destructivo | 48 dp (mín.) | Full width | Filled Button con containerColor = Error |
| Campo de texto (OutlinedTextField) | 56 dp | Full width | Corner radius 4 dp |
| Dropdown de filtro (D1) | 56 dp | weight(1f) en Row de 3 | OutlinedTextField M3 read-only con ExposedDropdownMenuBox |
| Chip selector RIR (E2) | 48 × 48 dp | 48 dp fijo | Corner radius 24 dp (circular), selección single |
| Card | Wrap content | Full width - 32 dp (margen) | Corner radius 12 dp, Elevation Tonal (Filled Card M3) |
| Fila de lista | 72 dp (mín.) | Full width | Divider como separador, 1 dp, Outline Variant |
| Diálogo (AlertDialog) | Wrap content | 280–560 dp | Corner radius 28 dp (M3 Dialog) |
| Barra de progreso | 8 dp altura | Full width | Corner radius 4 dp (Linear Progress M3) |
| Media visual en D2 | 240 dp altura | Full width | Corner radius 12 dp top, contenido AspectRatio 16:9 |
| Badge de alertas (B1) | 24 dp | 24 dp (mín.) | Circular, sobre ícono campana |
| Gráfico de tendencia (F3, G2) | 200 dp altura | Full width | Corner radius 12 dp, fondo Surface Container |

### 6.3 Elevación

| Nivel | Valor | Uso |
|-------|-------|-----|
| Level 0 | 0 dp | Top Bar, Bottom Navigation, fondo general |
| Level 1 | 1 dp | Filled Cards (tonal elevation via color, sin sombra) |
| Level 2 | 3 dp | Elevated Cards — Card "Reanudar Sesión" en B1, Card de crisis en H1 |
| Level 3 | 6 dp | Diálogos (E4, E3 confirmación) |
| Level 4 | 8 dp | — No usado |
| Level 5 | 12 dp | — No usado |

---

## 7. Componentes Reutilizables

### 7.1 Top App Bar

| Variante | Vistas | Estilo |
|----------|--------|--------|
| Center Aligned (sin retorno) | B1, D1/D3, F1, G1, J1 | Título centrado. Sin ícono de retorno. Color: Surface Container Lowest |
| Center Aligned (con retorno) | C1, C2, D2, D4, F2, F3, G2, G3, H1, H2, I1, J2, J3 | Título centrado + ícono `←` a la izquierda. Color: Surface Container Lowest |
| Center Aligned (con cierre) | E2, E3 | Título centrado + ícono `✕` a la izquierda. Sin Bottom Nav |
| Sin estilo estándar | A1 | Logo "Tension" centrado + subtítulo. Sin ícono. Sin Bottom Nav |
| Sesión Activa | E1 | Título izquierda (Módulo — Versión) + subtítulo "Sesión activa" + badge descarga condicional. Sin Bottom Nav |
| Post-Sesión | E5 | Título centrado + subtítulo módulo/versión. Sin ícono retorno. Sin Bottom Nav |

### 7.2 Bottom Navigation Bar

| Propiedad | Valor |
|-----------|-------|
| Ítems | 5: Inicio, Diccionario, Historial, Métricas, Configuración |
| Íconos | Material Symbols: `Home`, `MenuBook`, `History`, `BarChart`, `Settings` |
| Ícono activo | Filled, color Primary |
| Ícono inactivo | Outlined, color On Surface Variant |
| Label activo | Label Medium, color Primary |
| Label inactivo | Label Medium, color On Surface Variant |
| Indicador activo | Primary Container (pill shape detrás del ícono activo) |
| Excluida de | A1, E1, E2, E3, E4, E5 |
| Condicional en D2 | Visible si origen ∈ {D1, D4, F3} · Oculta si origen = E1 (sesión activa) |
| Condicional en F3 | Visible si origen ∈ {F2, G1, H2, D2} · Oculta si origen = E5 (post-sesión) |

### 7.3 Cards

| Variante | Vistas | Estilo M3 | Color fondo | Corner radius |
|----------|--------|-----------|-------------|---------------|
| Card primaria destacada | B1 (Próxima Sesión) | Filled Card | Primary Container | 12 dp |
| Card de advertencia/reanudar | B1 (Reanudar Sesión) | Elevated Card | Error Container | 12 dp |
| Card informativa | B1 (Progreso, Descarga), E5 (Estado), I1 | Filled Card | Secondary Container | 12 dp |
| Card de datos | F2 (Resumen), H2 (Datos alerta) | Filled Card | Surface Container High | 12 dp |
| Card de éxito | J2 (Exportación OK), J3 (Import OK) | Filled Card | Tertiary Container | 12 dp |
| Card de error | J3 (Import fallo) | Filled Card | Error Container | 12 dp |
| Card de advertencia | J2 (Advertencia cifrado), J3 (Advertencia reemplazo) | Outlined Card | Surface + borde Outline | 12 dp |
| Card de alerta crisis | H1 (sección Crisis) | Elevated Card | Error Container | 12 dp |
| Card de alerta alta | H1 (sección Alertas 🟠) | Filled Card | `#FFF3E0` / `#4E2600` | 12 dp |
| Card de alerta media | H1 (sección Alertas 🟡) | Filled Card | `#FFF8E1` / `#4A3800` | 12 dp |

### 7.4 Botones

| Variante M3 | Uso en Tension | Ejemplo |
|-------------|----------------|---------|
| Filled Button | Acción principal de la vista | "Iniciar Sesión", "Registrar", "Confirmar", "Ir al Inicio", "Activar Descarga", "Exportar datos" |
| Outlined Button | Acción secundaria o cancelar | "Cancelar" en diálogos, "Cerrar Sesión" en E1 |
| Text Button | Links de navegación | "Ver historial de peso →", "Ver técnica de ejecución →", "Cancelar" como enlace en E2 |
| Filled Tonal Button | Acción de soporte | "Compartir" en J2, "Reanudar Sesión" en B1 |
| Filled Button (Error) | Acción destructiva | "Restaurar datos" en J3 (containerColor = Error, contentColor = On Error) |
| Icon Button | Retorno, cierre, media visual | `←` retorno, `✕` cierre, `📷` media visual en E1 |

### 7.5 Campos de Formulario

| Variante | Uso | Estilo |
|----------|-----|--------|
| OutlinedTextField (numérico) | Peso (Kg), Altura (m), Repeticiones, Segundos | keyboardType = Number, sufijo de unidad como trailingIcon, color borde outline → primary al focus |
| OutlinedTextField (no editable) | Peso en ejercicio peso corporal/isométrico | Mismo estilo pero enabled = false, fondo Surface Container Highest con opacidad reducida |
| Error inline | Validación fallida | supportingText con color Error, borde cambia a Error |

### 7.6 Selector RIR (E2)

| Propiedad | Valor |
|-----------|-------|
| Tipo | Fila horizontal de 6 chips circulares (0, 1, 2, 3, 4, 5) |
| Tamaño por chip | 48 × 48 dp (cumple RNF06) |
| Espaciado entre chips | 8 dp |
| Chip no seleccionado | Fondo: Surface Container, borde: Outline, texto: On Surface |
| Chip seleccionado | Fondo: Primary, borde: none, texto: On Primary |
| Selección | Single select — al tocar uno, se deselecciona el anterior |

### 7.7 Listas

| Variante | Vistas | Estructura de fila |
|----------|--------|--------------------|
| Lista de ejercicios (catálogo) | D1, D4, E3 | 3 líneas: Title Medium (nombre), Body Medium (metadata separada por `·`), Body Medium (series · rango). Divider 1 dp entre filas |
| Lista de ejercicios (sesión) | E1 | Leading: ícono de estado (24 dp), 3 líneas: Title Medium (nombre), Body Medium (estado + series), Body Medium (carga). Trailing: botones de acción. Fondo de fila según estado (§4.3) |
| Lista de progresión | E5, F2 | 3 líneas: Title Medium (nombre), Body Medium (señal + color + carga), Body Small (señal de acción). Leading: ícono de progresión. Color de texto del ícono según señal |
| Lista de sesiones | F1 | 3 líneas: Body Medium (fecha), Title Medium (módulo-versión + estado con ícono), Body Medium (tonelaje). Divider entre filas |
| Lista de historial ejercicio | F3 | 3 líneas: Body Medium (fecha + módulo-versión), Body Medium (peso + reps + RIR), Body Small (clasificación progresión con color). Divider entre filas |
| Lista de alertas | H1 | Leading: indicador circular de nivel (12 dp, color del nivel). 3 líneas: Title Medium (tipo), Body Medium (entidad), Body Small (dato clave). Agrupadas por sección (Crisis / Alertas) |
| Lista de KPIs | G1 | 2 líneas: Title Medium (nombre ejercicio/módulo), Body Medium (valor + indicador). Trailing: ícono de tendencia con color. Divider entre filas |
| Lista de tendencias | G3 | 2 líneas: Title Medium (grupo muscular), Body Medium (clasificación). Trailing: ícono de tendencia. Divider entre filas |
| Lista de peso corporal | C2 | 2 líneas: Body Medium (fecha izquierda, peso derecha). Etiqueta "Registro inicial" como Body Small si aplica |
| Lista de versiones | D3 | Dentro de secciones agrupadas. 1 línea: Title Medium ("Versión N") + trailing Body Medium ("(X ej.)") |

### 7.8 Barras de Progreso

| Uso | Tipo M3 | Color track | Color indicator |
|-----|---------|-------------|-----------------|
| Progreso de sesión (E1) | LinearProgressIndicator (determinate) | Surface Variant | Primary |
| Progreso de descarga (I1) | LinearProgressIndicator (determinate) | Surface Variant | `#1565C0` / `#64B5F6` (azul descarga) |
| Exportación/importación (J2, J3) | LinearProgressIndicator (indeterminate) | Surface Variant | Primary |

### 7.9 Filtros (D1)

| Propiedad | Valor |
|-----------|-------|
| Tipo | 3 × ExposedDropdownMenuBox M3 con OutlinedTextField read-only |
| Opciones por filtro | Módulo: "Todos", "A", "B", "C". Equipo: "Todos" + [9 tipos]. Zona: "Todos" + [15 zonas] |
| Labels | "Módulo", "Equipo", "Zona" — cada dropdown con su label flotante |
| Valor por defecto | "Todos" en los 3 filtros (equivale a sin filtro activo) |
| Disposición | Los 3 dropdowns en una única fila horizontal (Row), cada uno con `weight(1f)`, separados por 8 dp. Padding horizontal 16 dp, vertical 8 dp |
| Touch target | Altura visual ~56 dp (OutlinedTextField M3), cumple RNF06 |
| Texto del valor | 13 sp, singleLine, overflow ellipsis |
| Borde no enfocado | Outline Variant |
| Trailing icon | ExposedDropdownMenuDefaults.TrailingIcon (flecha desplegable) |
| Menú desplegable | ExposedDropdownMenu M3. Primer ítem: "Todos" (limpia filtro). Demás ítems: opciones del filtro |

### 7.10 Tabs (D1/D3)

| Propiedad | Valor |
|-----------|-------|
| Tipo | Primary Tabs M3 (Tab Row) |
| Tabs | "Ejercicios" (→ D1), "Plan" (→ D3) |
| Tab activo | Texto: Primary, indicador inferior: Primary (3 dp) |
| Tab inactivo | Texto: On Surface Variant |
| Posición | Bajo el título "Diccionario" en Top Bar, fijo (no scrollable) |

### 7.11 Diálogos

| Diálogo | Vista | Estilo |
|---------|-------|--------|
| Confirmación de cierre (completada) | E4 | AlertDialog M3. Título: "Cerrar sesión". Botón confirmar: Filled Button (Primary). Botón cancelar: Text Button |
| Confirmación de cierre (incompleta) | E4 | AlertDialog M3. Título: "Cerrar sesión". Ícono `⚠️`. Botón confirmar: Filled Button (Error). Botón cancelar: Text Button |
| Confirmación de sustitución | E3 | AlertDialog M3. Título: "¿Sustituir [A] por [B]?". Botón confirmar: Filled Button. Botón cancelar: Text Button |

### 7.12 Gráficos

| Gráfico | Vista | Estilo |
|---------|-------|--------|
| Tendencia de carga | F3 | Gráfico lineal. Línea: Primary (2 dp). Puntos: Primary (6 dp círculo). Fondo: Surface Container. Grid: Outline Variant, 1 dp dashed. Labels ejes: Label Small, On Surface Variant |
| Tonelaje por grupo (barras) | G2 | Barras horizontales. Barra: Primary. Fondo barra: Surface Variant. Label: Body Medium. Valor: Body Medium, alineado derecha |
| Distribución volumen (barras %) | G2 | Barras horizontales. Barra: Secondary. Porcentaje: Body Medium |
| Evolución temporal (multilínea) | G2 | Gráfico multilínea. Líneas: colores diferenciados por grupo (Primary, Secondary, Tertiary, y variantes). Leyenda: Label Small |

### 7.13 Estados Vacíos

| Vista | Mensaje | Estilo |
|-------|---------|--------|
| F1 sin sesiones | "No hay sesiones pasadas disponibles." | Body Large, centrado, On Surface Variant. Ícono ilustrativo opcional (48 dp) |
| D1 sin resultados de filtro | "No hay ejercicios que coincidan con los filtros seleccionados." | Body Large, centrado, On Surface Variant |
| F3 sin historial | "No hay registros disponibles para este ejercicio." | Body Large, centrado, On Surface Variant |
| H1 sin alertas | "No hay alertas activas. ✅ Todo en orden." | Body Large, centrado, On Surface Variant. Ícono ✅ en color verde (Progresión) |
| G2 datos insuficientes | "Se necesitan al menos 2 microciclos para mostrar evolución comparativa." | Body Large, centrado, On Surface Variant |
| G3 datos insuficientes | "Se necesitan al menos 4 microciclos completados. Faltan N microciclos." | Body Large, centrado, On Surface Variant |

---

## 8. Especificación Visual por Vista

> Cada vista documenta: referencia a HUs y RFs, estructura de color y componentes por zona, todos los estados visuales con sus colores explícitos, y las variantes condicionales. Los colores se expresan como tokens M3 definidos en §4 o colores semánticos de §4.3.

---

### Flujo A — Onboarding

#### A1 — Registro de Perfil

**Trazabilidad:** HU-01 (CA-01.01 a CA-01.06) · RF01 · RNF03, RNF06, RNF12

**Contexto visual:** Única pantalla sin Bottom Navigation ni Top App Bar estándar. El ejecutante llega aquí al abrir la app sin perfil registrado. La pantalla debe transmitir la identidad de la marca con un encabezado limpio y un formulario claro.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Encabezado | Logo "Tension" | Text | Headline Medium · Color: Primary (`#8B1A1A`) · Centrado horizontalmente · Padding top: 48 dp desde borde superior |
| Encabezado | Subtítulo "Configura tu perfil" | Text | Title Small · Color: On Surface Variant (`#524340`) · Centrado · Spacing: 8 dp bajo logo |
| Body | Campo "Peso corporal (Kg)" | OutlinedTextField | Label: "Peso corporal" · trailingIcon: Text "Kg" en Body Small, On Surface Variant · keyboardType: Number · Borde reposo: Outline (`#857370`) · Borde focus: Primary · Full width · Margin top: 32 dp |
| Body | Campo "Altura (m)" | OutlinedTextField | Label: "Altura" · trailingIcon: Text "m" · Mismo estilo que Peso · Spacing: 16 dp sobre campo anterior |
| Body | Selector "Nivel de experiencia" | Column + RadioButton M3 | 3 opciones verticales con spacing 12 dp: "Principiante", "Intermedio", "Avanzado" · Texto: Body Large, On Surface · Radio no seleccionado: borde Outline · Radio seleccionado: relleno Primary · Margin top: 24 dp · Label de sección: Label Large, On Surface Variant, margin bottom: 8 dp |
| Body | Botón "Registrar" | Filled Button, full width | Height: 48 dp · Corner radius: 24 dp · Texto: Label Large · Margin top: 32 dp, bottom: 24 dp |
| — | Sin Bottom Navigation | — | — |

**Estados visuales de A1:**

| Estado | Botón "Registrar" | Campos | Notas |
|--------|--------------------|--------|-------|
| Inicial (vacío) | containerColor: On Surface 12% opacity · contentColor: On Surface 38% opacity · enabled = false | Bordes: Outline · Campos vacíos, placeholder text en On Surface Variant 60% opacity | Los 3 campos están vacíos. Radio sin selección |
| Parcialmente completo | Igual que Inicial (disabled) | Campos con datos muestran texto On Surface. Campos vacíos mantienen placeholder | Al menos un campo incompleto o selección radio faltante |
| Listo (todos válidos) | containerColor: Primary (`#8B1A1A`) · contentColor: On Primary (`#FFFFFF`) · enabled = true | Todos los bordes: Primary si en focus, Outline si no · Textos: On Surface | Los 3 campos con valor válido + radio seleccionado |
| Error de validación | Se mantiene disabled | Campo inválido: borde Error (`#BA1A1A`), supportingText en Error ("El peso debe ser un valor positivo") · Campos válidos: sin cambio | Se muestra inline bajo el campo afectado (CA-01.04, CA-01.05) |

---

### Flujo B — Inicio

#### B1 — Pantalla Principal (Home)

**Trazabilidad:** HU-05 (CA-05.01, CA-05.02, CA-05.06, CA-05.07) · HU-18 (CA-18.05) · RF09, RF10, RF12, RF41 · RNF10
**HU indirectas visibles:** HU-14, HU-16, HU-17, HU-26–HU-30 (badge de alertas + Card Estado de Descarga)

**Contexto visual:** Punto de entrada recurrente. Fondo: Background (`#FFF8F0`). Combinación de cards condicionales que comunican el estado actual del entrenamiento. El orden visual varía según estado.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | "Tension" | Text, Title Large | Color: Primary (`#8B1A1A`) · Alineado izquierda · Padding horizontal: 16 dp · Top Bar: Surface Container Lowest (`#FFFFFF`), elevation 0 |
| Top Bar | Badge alertas `🔔` | IconButton + Badge M3 | Ícono: `Notifications` Material Symbol, tint On Surface Variant · Badge circular sobre ícono, containerColor: Error (`#BA1A1A`), content: On Error (`#FFFFFF`), texto: Label Small (número de alertas) · Sin alertas: badge sin número, containerColor: Outline (`#857370`), tamaño reducido (6 dp punto) |
| Body | Card "Reanudar Sesión" | Elevated Card | **Condicional:** solo visible si hay sesión activa no cerrada (crash recovery, CA-05 + RNF10) · Fondo: Error Container (`#FFDAD6`) · Elevation: Level 2 (3 dp sombra) · Corner: 12 dp · Leading ícono: ⚠️, 24 dp, color Error · Título "Tienes una sesión activa sin cerrar": Title Medium, On Error Container (`#410002`) · Subtítulo "Módulo X — Versión N · N de M completados": Body Medium, On Error Container · Botón "Reanudar Sesión": Filled Button, containerColor: Primary, contentColor: On Primary · **Cuando visible, se muestra ANTES de Card Próxima Sesión y la oculta** |
| Body | Card "Próxima Sesión" | Filled Card | **Oculta si hay sesión pendiente de reanudar** · Fondo: Primary Container (`#F5DDDD`) · Corner: 12 dp · Elevation: tonal (Level 1) · "Módulo X — Versión N": Title Medium, On Primary Container (`#5C0E0E`) · "Tu próxima sesión": Body Medium, On Primary Container · Botón "Iniciar Sesión": Filled Button, full width dentro de card, containerColor: Primary, contentColor: On Primary · Padding card interno: 16 dp |
| Body | Sección "Progreso" | Column | Separación top: 24 dp (separador visual `---` = Divider M3, Outline Variant) · Número: Headline Medium, color Primary · Label "microciclos": Body Small, On Surface Variant · Centrado horizontalmente · Sin card, texto directo |
| Body | Card "Estado de Descarga" | Filled Card | **Condicional:** visible si descarga activa o módulo que la requiere (HU-16, HU-17) · Fondo: Secondary Container (`#F0E0E0`) · Corner: 12 dp · Ícono 🔄: 24 dp, color azul descarga (`#1565C0`) · "Descarga activa" o "Módulo X requiere descarga": Title Medium, On Secondary Container · Progreso o indicación: Body Medium · Enlace "Ver gestión de descarga →": Text Button, color Primary |
| Bottom | Bottom Navigation | NavigationBar M3 | 5 ítems · Inicio activo: ícono filled Primary, label Primary, pill indicator Primary Container · Innactivos: ícono outlined On Surface Variant, label On Surface Variant · Fondo: Surface Container Lowest |

**Estados de B1:**

| Estado | Card Reanudar | Card Próxima | Card Descarga | Microciclos | Badge |
|--------|---------------|--------------|---------------|-------------|-------|
| Primera sesión (sin historial) | Oculta | Visible: "Módulo A — Versión 1" | Oculta | "0" | "0" (punto solo) |
| Uso normal | Oculta | Visible: módulo/versión calculado | Oculta | Número ≥ 1 | Número real de alertas |
| Sesión pendiente (crash) | **Visible**, prominente | **Oculta** | Según estado descarga | Sin cambio | Sin cambio |
| Descarga activa | Según sesión pendiente | Según sesión pendiente | **Visible**: progreso N/6 | Sin cambio | Sin cambio |
| Descarga requerida (no activa) | Según sesión pendiente | Según sesión pendiente | **Visible**: indicación con enlace a I1 | Sin cambio | Sin cambio |

---

### Flujo C — Perfil

#### C1 — Perfil del Ejecutante

**Trazabilidad:** HU-01 (CA-01.07 a CA-01.09) · HU-02 (CA-02.01) · RF02 · RNF03, RNF06, RNF12

**Contexto visual:** Formulario de edición. Fondo: Background. Los campos vienen precargados con los valores actuales. El registro automático en historial de peso (HU-02, CA-02.01) ocurre sin feedback visual adicional — es transparente al ejecutante.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Mi Perfil" | CenterAlignedTopAppBar | navigationIcon: ArrowBack, tint On Surface (`#1C1B1B`) · Título: Title Large, On Surface · Fondo: Surface Container Lowest |
| Body | Campo "Peso corporal (Kg)" | OutlinedTextField | Mismos estilos que A1 · Valor precargado: On Surface · Si no ha cambiado: borde Outline · Si cambiado: borde Primary |
| Body | Campo "Altura (m)" | OutlinedTextField | Mismos estilos que A1 · Precargado |
| Body | Selector "Nivel de experiencia" | RadioButton group | Mismos estilos que A1 · Opción actual preseleccionada con radio filled Primary |
| Body | Botón "Guardar" | Filled Button, full width | Margin top: 32 dp · **Deshabilitado si no hay cambios** (sin dirty state): misma apariencia disabled que A1 · Habilitado si hay cambios válidos: Primary |
| Body | "Ver historial de peso →" | Text Button | Color: Primary · Alineado izquierda · Margin top: 16 dp · Sin padding adicional: alineado con margen de 16 dp |
| Bottom | Bottom Navigation | NavigationBar M3 | Configuración activo |

**Estados visuales de C1:**

| Estado | Botón "Guardar" | Campos | Enlace historial |
|--------|--------------------|--------|------------------|
| Sin cambios | Disabled (opacity 12%/38%) | Todos precargados, borde Outline | Siempre visible |
| Con cambios válidos | Enabled: Primary | Campos modificados con borde Primary (focus) | Siempre visible |
| Error de validación | Disabled | Campo afectado: borde Error + supportingText Error | Siempre visible |

---

#### C2 — Historial de Peso Corporal

**Trazabilidad:** HU-02 (CA-02.02 a CA-02.05) · RF03

**Contexto visual:** Lista cronológica de solo lectura. Cada entrada muestra fecha y peso. La entrada más antigua del perfil se distingue con etiqueta "Registro inicial".

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Historial de Peso" | CenterAlignedTopAppBar | navigationIcon: ArrowBack · Título: Title Large |
| Body | Fila de peso | ListItem M3, 56 dp | headlineContent: Body Medium, On Surface (fecha "dd MMM yyyy", alineado izquierda) · trailingContent: Body Medium, On Surface, fontWeight Medium (peso "XX.X Kg", alineado derecha) · Padding horizontal: 16 dp |
| Body | Etiqueta "Registro inicial" | Text | Body Small · On Surface Variant · Solo en la última fila (la más antigua). Padding top: 2 dp bajo el peso |
| Body | Divider entre filas | Divider M3 | Thickness: 1 dp · Color: Outline Variant (`#D8C2BD`) · Sin inset (full width) |
| Bottom | Bottom Navigation | NavigationBar M3 | Configuración activo |

**Estados:**

| Estado | Contenido | Estilo |
|--------|-----------|--------|
| Con historial (múltiples entradas) | Lista scrollable, cronológico descendente | Filas con divider. Primera fila = más reciente |
| Solo registro inicial (1 entrada) | Una única fila con etiqueta "Registro inicial" | Misma estructura, etiqueta en Body Small On Surface Variant |

---

### Flujo D — Catálogo

#### D1 — Diccionario de Ejercicios

**Trazabilidad:** HU-03 (CA-03.01 a CA-03.06, CA-03.10, CA-03.11) · RF04, RF07, RF62 · RNF06

**Contexto visual:** Lista filtrable de ejercicios (43 precargados + creados por el ejecutante) con tabs compartidos con D3. Los filtros se aplican en tiempo real. Comparte sección "Diccionario" del Bottom Navigation.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | "Diccionario" | CenterAlignedTopAppBar | Título: Title Large, On Surface · Fondo: Surface Container Lowest |
| Top Bar | Tabs "Ejercicios" / "Plan" | TabRow M3 (Primary Tabs) | Tab activo "Ejercicios": texto Primary (`#8B1A1A`), indicador inferior Primary (3 dp height) · Tab inactivo "Plan": texto On Surface Variant (`#524340`) · Fondo TabRow: Surface Container Lowest |
| Body | Fila de filtros (Row) | 3 × ExposedDropdownMenuBox M3 | Los 3 dropdowns en una fila horizontal: (1) "Módulo" — opciones: Todos, A, B, C; (2) "Equipo" — opciones: Todos + 9 tipos; (3) "Zona" — opciones: Todos + 15 zonas. Cada dropdown: OutlinedTextField read-only, label flotante, trailing icon flecha, 13 sp texto valor, singleLine. Borde: Outline Variant. Cada uno con `weight(1f)`. Spacing: 8 dp entre dropdowns. Padding: horizontal 16 dp, vertical 8 dp. Valor por defecto: "Todos" |
| Body | Contador resultados | Text | Body Small · On Surface Variant · "Mostrando N de T ejercicios" (T = total en diccionario, incluye precargados + creados) · Padding top: 12 dp, bottom: 8 dp · Alineado izquierda |
| Body | Fila de ejercicio | ListItem M3, 72 dp | headlineContent: Title Medium, On Surface (nombre prominente) · supportingContent: Body Medium, On Surface Variant (módulo · equipo · zona, separados por " · ") · Para ejercicios custom (`is_custom = 1`), mostrar badge "Personalizado" en On Tertiary Container · clickable: ripple default · Divider: 1 dp, Outline Variant entre filas |
| Body | Estado sin resultados | Column centrada | Text: Body Large, On Surface Variant, centrado. "No hay ejercicios que coincidan con los filtros seleccionados." · Padding vertical: 48 dp |
| FAB | Botón crear ejercicio | FloatingActionButton M3 | Ícono: Add (24 dp) · Container: Primary Container · Content: On Primary Container · Posición: bottom-end, margin 16 dp · Al tocar → navega a formulario de creación de ejercicio (CA-03.10) |
| Bottom | Bottom Navigation | NavigationBar M3 | Diccionario activo |

**Estados de D1:**

| Estado | Filtros | Lista | Contador |
|--------|---------|-------|----------|
| Sin filtros (default) | Los 3 dropdowns muestran "Todos" | Todos los ejercicios (precargados + creados) | "Mostrando T de T ejercicios" |
| Con filtros activos | Dropdown(s) muestran la opción seleccionada | Solo ejercicios que cumplen TODOS los filtros | "Mostrando N de T ejercicios" |
| Sin resultados | Filtros activos | Estado vacío centrado (ver §7.13) | "Mostrando 0 de T ejercicios" |

---

#### D2 — Detalle de Ejercicio

**Trazabilidad:** HU-03 (CA-03.07, CA-03.08) · RF61 · RNF06

**Contexto visual:** Ficha de ejercicio con media visual prominente. Vista reutilizable (desde D1, D4, E1, F3). La Bottom Navigation es condicional según el origen.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + nombre del ejercicio | CenterAlignedTopAppBar | navigationIcon: ArrowBack (retorna al origen: D1, D4, E1 o F3) · Título: Title Large, On Surface (nombre dinámico) |
| Body | Media visual | Image (PNG) en Box, clickable | Height: 240 dp · Width: full width · ContentScale: Crop · Corner radius top: 12 dp, bottom: 0 · **Placeholder** (cuando `mediaResource == null`): logo de la app centrado (64 dp) + ícono AddAPhoto (24 dp, On Surface Variant) + texto "Toca para agregar imagen" (Body Small, On Surface Variant) · **Imagen cargada**: si `mediaResource` es ruta absoluta (almacenamiento interno, ejercicio custom), se carga desde filesystem; si no, se carga desde `assets/exercises/module-{code}/{mediaResource}.png` via AssetManager · **Clickable**: al tocar abre selector de galería del dispositivo (aplica tanto a ejercicios precargados como custom). La imagen seleccionada se copia a `filesDir/exercise_images/` y se actualiza `media_resource` en BD · **Overlay**: ícono AddAPhoto 24 dp en esquina inferior derecha (alpha 0.7, On Primary Container, fondo semi-transparente) para indicar que la imagen es editable · Nota: en futuras iteraciones se podrá migrar a AnimatedImage (GIF) / VideoPlayer sin cambios en el layout |
| Body | Campo "Nombre" | Column | overlineContent: Label Medium, On Surface Variant ("Nombre") · headlineContent: Body Large, On Surface (valor) · Padding vertical: 8 dp |
| Body | Campo "Módulo" | Column | overlineContent: Label Medium, On Surface Variant ("Módulo") · headlineContent: Body Large, On Surface ("A — Superior" / "B — Superior" / "C — Inferior") |
| Body | Campo "Tipo de equipo" | Column | overlineContent: Label Medium, On Surface Variant ("Tipo de equipo") · headlineContent: Body Large, On Surface (valor) |
| Body | Campo "Zona muscular" | Column | overlineContent: Label Medium, On Surface Variant ("Zona muscular") · headlineContent: Body Large, On Surface (valor) |
| Body | "Ver historial de este ejercicio →" | Text Button | Color: Primary · Margin top: 24 dp · Navega a F3 |
| Bottom | Bottom Navigation | Condicional | **Visible** si origen = D1, D4, F3 (Diccionario activo o según origen) · **Oculto** si origen = E1 (sesión activa — sin nav global) |

---

#### D5 — Crear Ejercicio

**Trazabilidad:** HU-03 (CA-03.10, CA-03.11) · RF62

**Contexto visual:** Formulario de creación de ejercicio personalizado con imagen opcional. Accesible desde el FAB de D1. Scroll vertical para acomodar todos los campos.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Crear ejercicio" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → D1 · Título: Title Large, On Surface |
| Body | Zona de imagen | Box clickable + Column | Height: 240 dp · Corner radius: 12 dp · **Placeholder** (sin imagen): `ExerciseImagePlaceholder` compartido (logo de la app 64 dp + ícono AddAPhoto 24 dp, On Surface Variant + texto "Toca para agregar imagen" Body Small, On Surface Variant) · **Con imagen**: Image con ContentScale.Crop, full size · Al tocar → abre selector de galería del dispositivo (`image/*`) |
| Body | Hint de imagen | Text | Body Small · On Surface Variant · "La imagen es opcional. Puedes agregarla después." |
| Body | Campo "Nombre" | OutlinedTextField | Obligatorio · singleLine · Label: "Nombre" · supportingText: error en Color Error si vacío |
| Body | Dropdown "Módulo" | ExposedDropdownMenuBox | Obligatorio · OutlinedTextField read-only · Label: "Módulo" · Opciones: A — Superior, B — Superior, C — Inferior · supportingText: error si no seleccionado |
| Body | Dropdown "Tipo de equipo" | ExposedDropdownMenuBox | Obligatorio · OutlinedTextField read-only · Label: "Tipo de equipo" · Opciones: 9 tipos · supportingText: error si no seleccionado |
| Body | Label "Zona muscular" | Text | Label Large · On Surface · Obligatorio (≥1 seleccionada) |
| Body | Zonas musculares | FlowRow de FilterChip | Multi-select · 15 zonas · `FilterChipDefaults.filterChipColors`: selectedContainerColor Primary Container, selectedLabelColor On Primary Container · Spacing: horizontal 8 dp, vertical 4 dp |
| Body | Label "Condiciones especiales" | Text | Label Large · On Surface |
| Body | Checkbox "Peso corporal" | Row (Checkbox + Text) | Body Medium · Checkbox + texto clickable |
| Body | Checkbox "Isométrico" | Row (Checkbox + Text) | Body Medium · Checkbox + texto clickable |
| Body | Checkbox "Al fallo técnico" | Row (Checkbox + Text) | Body Medium · Checkbox + texto clickable |
| Body | Botón "Crear" | Button (filled) full-width | Texto: "Crear" · enabled solo cuando `canSave` es true · **Guardando**: muestra CircularProgressIndicator (20 dp, strokeWidth 2 dp, On Primary) en lugar del texto |
| Snackbar | Error de guardado | SnackbarHost | Muestra errores de validación de unicidad u otros errores de runtime |
| Bottom | Bottom Navigation | NavigationBar M3 | Diccionario activo |

**Estados de D5:**

| Estado | Contenido | Acción |
|--------|-----------|--------|
| Cargando opciones | CircularProgressIndicator centrado | Esperando módulos, equipos y zonas |
| Formulario vacío | Placeholder imagen, campos vacíos, botón disabled | El ejecutante completa campos |
| Formulario parcial | Algunos campos completos, botón disabled | Completar campos faltantes |
| Imagen seleccionada | Imagen de galería reemplaza placeholder | Opcional — no bloquea el botón |
| Campos con error | supportingText en rojo bajo campos vacíos | El ejecutante corrige |
| Guardando | Botón muestra CircularProgressIndicator | Esperando persistencia |
| Éxito | Auto-navega a D1 | El ejercicio aparece en el diccionario (con badge "Personalizado") |
| Error unicidad | Snackbar con mensaje de error | El ejecutante modifica nombre o equipo |

---

#### D3 — Plan de Entrenamiento

**Trazabilidad:** HU-04 (CA-04.01, CA-04.05) · RF05, RF08

**Contexto visual:** Navegación por módulos y versiones. Comparte tabs con D1. Los módulos se presentan como secciones agrupadas con encabezado descriptivo.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | "Diccionario" + Tabs | CenterAlignedTopAppBar + TabRow | Tab activo = "Plan": texto Primary, indicador Primary · Tab inactivo = "Ejercicios": texto On Surface Variant |
| Body | Sección Módulo A | Column | Encabezado: Title Medium, On Surface ("Módulo A — Superior") · Subtítulo: Body Small, On Surface Variant ("Pecho, Espalda, Abdomen") · Margin bottom tras subtítulo: 8 dp |
| Body | Fila Versión N (dentro de módulo) | ListItem M3, 56 dp | headlineContent: Title Medium, On Surface ("Versión N") · trailingContent: Body Medium, On Surface Variant ("(11 ej.)") · clickable → D4 · Ripple default |
| Body | Sección Módulo B | Column | Encabezado: Title Medium ("Módulo B — Superior") · Subtítulo: Body Small ("Hombro, Tríceps, Bíceps") · 3 versiones |
| Body | Sección Módulo C | Column | Encabezado: Title Medium ("Módulo C — Inferior") · Subtítulo: Body Small ("Pierna") · 3 versiones |
| Body | Separación entre módulos | Spacer + Divider | Spacer: 12 dp · Divider: Outline Variant · Spacer: 12 dp |
| Bottom | Bottom Navigation | NavigationBar M3 | Diccionario activo |

---

#### D4 — Detalle de Versión del Plan

**Trazabilidad:** HU-04 (CA-04.02 a CA-04.04, CA-04.06, CA-04.07, CA-04.08) · RF05, RF06, RF08, RF63, RF64

**Contexto visual:** Lista de ejercicios de una combinación módulo-versión. Nota explícita de que el listado no implica orden de ejecución (RF06). Las condiciones especiales (peso corporal, isométricos) se presentan con estilo diferenciado. El ejecutante puede asignar nuevos ejercicios (FAB) y desasignar existentes.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Módulo X — Versión N" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → D3 · Título dinámico: Title Large |
| Body | Subtítulo informativo | Text | Body Small · On Surface Variant · "N ejercicios · Sin orden obligatorio" · Padding bottom: 16 dp |
| Body | Fila ejercicio estándar | ListItem M3, 80 dp | headlineContent: Title Medium, On Surface (nombre) · supportingContent, línea 1: Body Medium, On Surface Variant (zona · equipo) · supportingContent, línea 2: Body Medium, On Surface Variant ("4 series · 8-12 reps") · trailingContent: IconButton Delete (tint Error, 48 dp touch target) para desasignar (CA-04.08) · clickable → D2 · Divider 1 dp entre filas |
| Body | Fila ejercicio peso corporal (Flexiones) | ListItem M3, 80 dp | Misma estructura · Línea 2: Body Medium, On Surface Variant, **fontStyle: italic** ("4 series · Al fallo técnico") |
| Body | Fila ejercicio isométrico (Plancha, Plancha Lateral) | ListItem M3, 80 dp | Misma estructura · Línea 2: Body Medium, On Surface Variant, **fontStyle: italic** ("4 series · 30-45 seg") |
| Body | Fila ejercicio peso corporal (otros: Abdominales, etc.) | ListItem M3, 80 dp | Misma estructura · Línea 2: Body Medium, On Surface Variant ("4 series · 8-12 reps") — sin distinción visual especial |
| FAB | Botón asignar ejercicio | FloatingActionButton M3 | Ícono: Add (24 dp) · Container: Primary Container · Content: On Primary Container · Posición: bottom-end, margin 16 dp · Al tocar → presenta lista de ejercicios del mismo módulo aún no asignados a esta versión (CA-04.07) |
| Bottom | Bottom Navigation | NavigationBar M3 | Diccionario activo |

---

### Flujo E — Sesión Activa

#### E1 — Sesión Activa

**Trazabilidad:** HU-05 (CA-05.06 a CA-05.09) · HU-06 (CA-06.10, CA-06.12) · HU-07 (CA-07.01, CA-07.05, CA-07.06) · HU-08 (CA-08.01, CA-08.04) · RF12, RF13, RF15, RF16, RF22 · RNF04, RNF05, RNF06, RNF10
**HU indirectas visibles:** HU-11 (carga objetivo), HU-17 (indicador de descarga)

**Contexto visual:** Centro operativo de la sesión. Sin Bottom Navigation (restricción de sesión activa). El fondo de cada fila de ejercicio cambia según su estado (No Iniciado / En Ejecución / Completado). Los botones de acción visibles dependen del estado.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | Módulo — Versión | Text, Title Large | On Surface · Alineado izquierda · "Módulo X — Versión N" |
| Top Bar | "Sesión activa" | Text, Title Small | On Surface Variant · Bajo el módulo |
| Top Bar | Badge descarga | AssistChip M3 | **Condicional:** solo si descarga activa (HU-17) · Ícono: 🔄 en azul descarga (`#1565C0`) · Label: "Descarga · Sesión N/6" en Label Medium · containerColor: `#E3F2FD` (claro) / `#1A2733` (oscuro) · Margin top: 4 dp bajo "Sesión activa" |
| Body | Barra progreso | Column | Texto: Body Medium, On Surface ("N de M ejercicios completados") · LinearProgressIndicator determinate: track Surface Variant, indicator Primary, height 8 dp, corner 4 dp · Porcentaje: Label Small, On Surface Variant, alineado derecha · Padding bottom: 16 dp |
| Body | Fila ejercicio **No Iniciado** | Outlined Card, 88 dp | Borde: Outline Variant · Fondo: transparente (Background) · Leading: ícono ⚪ gris Outline (`#857370`), 24 dp · Línea 1: Title Medium, On Surface (nombre) · Línea 2: Body Medium, On Surface Variant ("No Iniciado · 0/4 series") · Línea 3: Body Medium, On Surface Variant (carga objetivo — ver variantes de carga abajo) · Trailing: Row con spacing 8 dp: botón "Registrar" (Filled Tonal Button, compact 36 dp visual height, **48 dp touch target** con padding vertical incluido, containerColor Primary Container, contentColor On Primary Container) + "Sustituir" (Outlined Button, compact 36 dp visual height, **48 dp touch target**, borderColor Outline) + IconButton 📷 (tint On Surface Variant, 48×48 dp touch, 24 dp icon) |
| Body | Fila ejercicio **En Ejecución** | Filled Card, 88 dp | Fondo: azul fila claro `#E3F2FD` / oscuro `#1A2733` · Leading: ícono 🔵 azul (`#1565C0` / `#64B5F6`), 24 dp · Línea 2: "En Ejecución · N/4 series" · Trailing: "Registrar" (Filled Button, compact 36 dp visual height, **48 dp touch target**, containerColor Primary) + IconButton 📷 · **Sin botón "Sustituir"** (CA-07.06: solo si 0 series) |
| Body | Fila ejercicio **Completado** | Filled Card, 72 dp | Fondo: verde fila claro `#E8F5E9` / oscuro `#1A2E1A` · Leading: ícono ✅ verde (`#2E7D32` / `#81C784`), 24 dp · Línea 2: "Completado · 4/4 series" · Texto: On Surface con opacity 0.7 (menor énfasis — completado) · Trailing: solo IconButton 📷 · **Sin botones de acción** |
| Body | Botón "Cerrar Sesión" | Outlined Button, full width | borderColor: Secondary (`#6B4F4F`) · contentColor: Secondary · Margin top: 24 dp · Navega a E4 |
| — | Sin Bottom Navigation | — | — |

**Variantes de carga mostrada en E1:**

| Tipo de ejercicio | Texto de carga | Estilo |
|-------------------|----------------|--------|
| Estándar con historial | "60 Kg" (carga objetivo derivada del historial, CA-05.08) | Body Medium, On Surface |
| Estándar sin historial | "Sin historial — establecer carga" (CA-05.07) | Body Medium, On Surface Variant, fontStyle italic |
| Estándar en descarga activa | "36 Kg" (60% de la carga habitual, HU-17) | Body Medium, azul descarga (`#1565C0`), con ícono 🔄 inline |
| Peso corporal | "Peso corporal" | Body Medium, On Surface Variant |
| Isométrico | "Isométrico (30-45s)" | Body Medium, On Surface Variant |
| Peso corporal en descarga activa | "Peso corporal (8 reps objetivo)" | Body Medium, azul descarga (`#1565C0`), con ícono 🔄 inline |
| Isométrico en descarga activa | "Isométrico (30s)" | Body Medium, azul descarga (`#1565C0`), con ícono 🔄 inline |

---

#### E2 — Registro de Serie

**Trazabilidad:** HU-06 (CA-06.01 a CA-06.09) · HU-08 (CA-08.01, CA-08.04, CA-08.05, CA-08.08) · RF13, RF14 · RNF02, RNF03, RNF04, RNF06, RNF12

**Contexto visual:** Formulario de captura rápida — máximo 3 toques (RNF02). Sin Bottom Navigation. El layout cambia según tipo de ejercicio (estándar, peso corporal, isométrico).

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `✕` + nombre ejercicio | CenterAlignedTopAppBar | navigationIcon: Close (`✕`), tint On Surface · Título: Title Large (nombre del ejercicio) |
| Top Bar | "Serie N de 4" | Text | Title Small · On Surface Variant · Número de serie asignado automáticamente (CA-06.09) |
| Body | Campo "Peso (Kg)" — **estándar** | OutlinedTextField | Label: "Peso (Kg)" · trailingIcon: "Kg" Body Small, On Surface Variant · keyboardType: Number · **Precargado** con último peso utilizado (CA-06.04, RNF04): texto On Surface · Borde: Outline → Primary al focus · Validación: ≥ 0 (CA-06.05) |
| Body | Campo "Peso (Kg)" — **peso corporal / isométrico** | OutlinedTextField | Mismo layout pero **enabled = false** · Valor fijo: "0" · Fondo: Surface Container Highest (`#EDE0D5`) con opacity 0.5 · Texto: On Surface Variant · Label incluye nota "(Peso corporal)" o "(Isométrico)" |
| Body | Campo "Repeticiones" — **estándar y peso corporal** | OutlinedTextField | Label: "Repeticiones" · trailingIcon: "reps" · keyboardType: Number · Sin precarga (vacío) · Validación: ≥ 1 (CA-06.06) |
| Body | Campo "Segundos sostenidos" — **isométrico** | OutlinedTextField | Label: "Segundos sostenidos" · trailingIcon: "seg" · keyboardType: Number · Sin precarga · supportingText: "(Referencia: 30-45 seg)" en Body Small, On Surface Variant (CA-08.05) · Validación: ≥ 1 |
| Body | Selector RIR | Row de 6 chips circulares | Label superior: "RIR" en Label Medium, On Surface Variant · 6 chips: "0", "1", "2", "3", "4", "5" · Tamaño: 48×48 dp cada uno (RNF06) · Spacing: 8 dp · No seleccionado: fondo Surface Container (`#F8EBE0`), borde Outline, texto On Surface · Seleccionado: fondo Primary (`#8B1A1A`), sin borde, texto On Primary (`#FFFFFF`) · Single select · Sin precarga |
| Body | Botón "Confirmar" | Filled Button, full width | containerColor: Primary · Height: 48 dp · Margin top: 24 dp · Al confirmar con datos válidos: persiste serie (CA-06.08) → retorna a E1 |
| Body | "Cancelar" | Text Button, centrado | Color: Primary · Descarta → retorna a E1 sin cambios |
| — | Sin Bottom Navigation | — | — |

**Estados de validación en E2:**

| Campo | Error | Mensaje (supportingText) | Estilo error |
|-------|-------|--------------------------|--------------|
| Peso | < 0 | "El peso debe ser ≥ 0 Kg" | Borde Error (`#BA1A1A`), supportingText Error |
| Repeticiones | < 1 | "Las repeticiones deben ser ≥ 1" | Borde Error, supportingText Error |
| Segundos | < 1 | "La duración debe ser ≥ 1 segundo" | Borde Error, supportingText Error |
| RIR | Ningún chip seleccionado | — (no se puede seleccionar fuera de rango al ser chips fijos 0-5) | Botón Confirmar disabled hasta selección |

**Resumen de variantes de E2:**

| Variante | Campo Peso | Campo segundo | RIR |
|----------|-----------|---------------|-----|
| Estándar (con peso) | Editable, precargado | "Repeticiones" ≥ 1 | Igual |
| Peso corporal | Fijo 0, no editable | "Repeticiones" ≥ 1 | Igual |
| Isométrico | Fijo 0, no editable | "Segundos sostenidos" ≥ 1 + referencia 30-45s | Igual |

---

#### E3 — Selección de Ejercicio Sustituto

**Trazabilidad:** HU-07 (CA-07.01 a CA-07.06) · RF16 · RNF06

**Contexto visual:** Lista filtrada de ejercicios elegibles del mismo módulo. Solo accesible si el ejercicio está en "No Iniciado" (0 series). La sustitución es puntual: no modifica el Plan.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `✕` + "Sustituir ejercicio" | CenterAlignedTopAppBar | navigationIcon: Close · Título: Title Large ("Sustituir ejercicio") |
| Top Bar | "Reemplazar: [nombre]" | Text | Title Small · On Surface Variant · Nombre del ejercicio a reemplazar |
| Body | Texto informativo | Column de Text | Body Medium, On Surface Variant · Línea 1: "Selecciona un ejercicio del mismo módulo como reemplazo." · Línea 2: "La sustitución es puntual y no modifica el Plan." · Margin bottom: 16 dp |
| Body | Fila ejercicio elegible | ListItem M3, 64 dp | headlineContent: Title Medium, On Surface (nombre) · supportingContent: Body Medium, On Surface Variant (zona · equipo) · Solo ejercicios del mismo módulo, excluidos los ya prescritos en la sesión (CA-07.01) · clickable → diálogo de confirmación |
| Body | Divider entre filas | Divider M3 | 1 dp, Outline Variant |
| Body | Diálogo de confirmación | AlertDialog M3 | title: Title Medium, "¿Sustituir [original] por [sustituto]?" · text: Body Medium, "Esta sustitución es solo para esta sesión." · confirmButton: Filled Button "Confirmar" (Primary) · dismissButton: Text Button "Cancelar" · Corner: 28 dp · Elevation: Level 3 |
| Body | "Cancelar" (enlace) | Text Button | Color: Primary · Retorna a E1 sin cambios. Margin top: 16 dp |
| — | Sin Bottom Navigation | — | — |

---

#### E4 — Confirmación de Cierre de Sesión

**Trazabilidad:** HU-09 (CA-09.01 a CA-09.03) · RF18, RF19

**Contexto visual:** Diálogo modal sobre E1 (no es navegación — E1 difuminado detrás). Dos variantes según estado de la sesión.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Overlay | Scrim (fondo) | Scrim M3 | Color: Scrim sobre E1 · Opacity: 32% negro · E1 no interactiva |
| Diálogo | **Caso A: Sesión completada** | AlertDialog M3 | title: Title Medium, On Surface ("Cerrar sesión") · text: Body Medium, On Surface ("Todos los ejercicios están completados. La sesión se cerrará como Completada.") · confirmButton: Filled Button, containerColor Primary, label "Cerrar ✓" · dismissButton: Text Button "Cancelar" · Corner: 28 dp |
| Diálogo | **Caso B: Sesión parcial** | AlertDialog M3 | title: Title Medium, On Surface ("Cerrar sesión") · icon: ⚠️, 24 dp, tint Error (`#BA1A1A`) · text: Body Medium, On Surface ("Hay N ejercicios sin completar. La sesión se cerrará como Incompleta. Los datos parciales se conservarán.") · confirmButton: Filled Button, containerColor **Error** (`#BA1A1A`), contentColor On Error, label "Cerrar ⚠️" · dismissButton: Text Button "Cancelar" · Corner: 28 dp |

**Diferenciación visual entre casos:**

| Aspecto | Caso A (completa) | Caso B (parcial) |
|---------|-------------------|-------------------|
| Ícono en diálogo | Ninguno | ⚠️ en Error |
| Tono del mensaje | Neutro, confirmatorio | Advertencia explícita |
| Botón confirmar color | Primary (rojo impero) | Error (rojo alerta) |
| Texto botón confirmar | "Cerrar ✓" | "Cerrar ⚠️" |

---

#### E5 — Resumen Post-Sesión

**Trazabilidad:** HU-13 (CA-13.01 a CA-13.07) · HU-08 (CA-08.07 — badge isométrico dominado) · HU-10 (clasificación de progresión) · HU-11 (señal de subir carga) · HU-12 (señal de descarga) · RF20, RF23, RF24, RF25, RF26, RF27, RF33, RF59 · RNF05

**Contexto visual:** Presentado automáticamente tras confirmación en E4. No hay botón de retorno (sesión cerrada = inmutable, CA-09.07). Sin Bottom Navigation. El tonelaje se muestra de forma prominente. Cada ejercicio tiene clasificación de progresión con código de color + ícono + texto (RNF05).

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | "Resumen de Sesión" | CenterAlignedTopAppBar | Sin navigationIcon (no hay retorno) · Título: Title Large |
| Top Bar | "Módulo X — VN" | Text | Title Small · On Surface Variant |
| Body | Card estado + tonelaje (Completada) | Filled Card | Fondo: Primary Container (`#F5DDDD`) · Estado: Title Medium + ícono ✅ verde `#2E7D32`, texto "Completada" · Tonelaje: Headline Large (`32 sp`), color Primary (`#8B1A1A`), separador de miles (" 12,450 Kg") · Ejercicios: Body Medium, On Primary Container ("11/11 ejercicios") · Corner: 12 dp · Padding: 16 dp |
| Body | Card estado + tonelaje (Incompleta) | Filled Card | Fondo: Error Container (`#FFDAD6`) · Estado: Title Medium + ícono ⚠️ naranja `#E65100`, texto "Incompleta" · Tonelaje parcial: Headline Large, On Error Container · Ejercicios: "N/M ejercicios" · Solo lista ejercicios CON registros (CA-13.07) |
| Body | Separador | Divider M3 | Outline Variant · Margin vertical 16 dp |
| Body | Fila progresión — **Progresión positiva** | ListItem M3, 80 dp | Leading: ícono `↑` 24 dp, color verde `#2E7D32` / `#81C784` · headlineContent: Title Medium, On Surface (nombre) · supportingContent línea 1: Body Medium, color verde ("↑ Progresión · 60 Kg") · supportingContent línea 2: Body Small, On Surface Variant (señal: "Subir carga → 62.5 Kg" para estándar, o "48 reps totales (+3)" para peso corporal, o "4×42s — Progresando" para isométrico) · clickable → F3 |
| Body | Fila progresión — **Mantenimiento** | ListItem M3, 80 dp | Leading: ícono `=` 24 dp, color ámbar `#8D6E00` / `#FFD54F` · supportingContent línea 1: Body Medium, color ámbar `#8D6E00` / `#FFD54F` ("= Mantenimiento · 22.5 Kg") · línea 2: Body Small ("Mantener carga") · clickable → F3 |
| Body | Fila progresión — **Regresión** | ListItem M3, 80 dp | Leading: ícono `↓` 24 dp, color rojo `#C62828` / `#EF9A9A` · supportingContent línea 1: Body Medium, color rojo ("↓ Regresión · 40 Kg") · línea 2: Body Small ("Considerar descarga") · clickable → F3 |
| Body | Badge "🏆 Dominado" | AssistChip M3 | **Condicional:** solo isométricos con 4 series ≥ 45s (CA-08.07) · containerColor: Tertiary Container (`#E0EEDD`) · label: "Dominado", Label Medium, On Tertiary Container · leadingIcon: 🏆 emoji · Se muestra junto al nombre del ejercicio |
| Body | Botón "Ir al Inicio" | Filled Button, full width | containerColor: Primary · Margin top: 24 dp · Navega a B1 |
| — | Sin Bottom Navigation | — | — |

**Señales de acción por tipo en E5:**

| Tipo ejercicio | Si progresión | Si mantenimiento | Si regresión |
|----------------|---------------|-------------------|--------------|
| Estándar — Doble Umbral cumplido (≥12 reps en 3/4 series + RIR prom ≥ 2) | "Subir carga → X Kg" (RF26: +2.5 Kg módulos A/B, +5 Kg módulo C) | N/A (si DU se cumple, la clasificación es siempre Progresión) | N/A |
| Estándar — Doble Umbral no cumplido | "Progresar en reps (misma carga)" (RF27) | "Mantener carga — progresar en reps" | "Considerar descarga" |
| Peso corporal | "N reps totales (+diferencia)" | "N reps totales (=)" | "N reps totales (−diferencia)" |
| Isométrico | "N×Xs — Progresando" | "N×Xs — Manteniendo" | "N×Xs — Regresando" |
| Isométrico dominado | "4×45s — 🏆 Dominado" | — | — |

---

### Flujo F — Historial

#### F1 — Historial de Sesiones

**Trazabilidad:** HU-24 (CA-24.01, CA-24.03, CA-24.06) · RF60

**Contexto visual:** Lista cronológica descendente de sesiones cerradas. Cada fila tiene indicador visual del estado (Completada / Incompleta) con color diferenciado.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | "Historial" | CenterAlignedTopAppBar | Sin navigationIcon · Título: Title Large |
| Body | Fila de sesión | ListItem M3, 80 dp | overlineContent: Body Medium, On Surface Variant (fecha "dd MMM yyyy") · headlineContent: Row — Title Medium On Surface ("Módulo X — VN") + Spacer + Badge estado (ver abajo) · supportingContent: Body Medium, On Surface Variant ("Tonelaje: 12,450 Kg") · clickable → F2 · Divider 1 dp Outline Variant entre filas |
| Body | Badge "Completada" | Text + ícono inline | ícono ✅ 16 dp, color verde `#2E7D32` · texto "Completada", Body Small, color verde |
| Body | Badge "Incompleta" | Text + ícono inline | ícono ⚠️ 16 dp, color naranja `#E65100` · texto "Incompleta", Body Small, color naranja |
| Body | Estado vacío | Column centrada | Body Large, On Surface Variant, centrado · "No hay sesiones pasadas disponibles." · Padding vertical: 48 dp |
| Bottom | Bottom Navigation | NavigationBar M3 | Historial activo |

---

#### F2 — Detalle de Sesión Pasada

**Trazabilidad:** HU-24 (CA-24.02, CA-24.04, CA-24.05) · HU-10 (clasificación de progresión) · RF60

**Contexto visual:** Solo lectura (CA-24.05). Resumen seguido de lista expandida de ejercicios con sus 4 series. Sustituciones se indican con nota.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Módulo X — VN" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → F1 · Título: Title Large |
| Top Bar | "dd MMM yyyy · Completada/Incompleta" | Text | Title Small · On Surface Variant |
| Body | Card resumen | Filled Card | Fondo: Surface Container High (`#F2E5DA`) · Corner: 12 dp · "Tonelaje: X Kg": Title Medium, On Surface · "Ejercicios: N/M": Body Medium, On Surface Variant · "Estado: Completada ✅" o "Incompleta ⚠️": Body Medium con ícono + color estado (verde/naranja) |
| Body | Separador | Divider M3 | Outline Variant · Margin vertical 16 dp |
| Body | Fila ejercicio | Column dentro de Card | **Encabezado fila:** Title Medium, On Surface (nombre) · Badge progresión: ícono + texto + color semántico (↑ verde / = ámbar / ↓ rojo) en Body Small · **Nota sustitución** (si aplica, CA-24.04): Body Small, On Surface Variant, fontStyle italic ("Sustituyó a: [original]") · **Series:** 4 filas de Body Medium, On Surface Variant ("Serie 1: 60 Kg × 12 · RIR 2", etc.) con padding izquierdo 16 dp · Divider 1 dp entre ejercicios · clickable → F3 |
| Bottom | Bottom Navigation | NavigationBar M3 | Historial activo |

---

#### F3 — Historial de Ejercicio

**Trazabilidad:** HU-23 (CA-23.01 a CA-23.06) · HU-10 (clasificación) · RF43, RF50, RF51

**Contexto visual:** Historial longitudinal de un ejercicio: estado de progresión actual, gráfico de tendencia, lista de sesiones. Incluye registros de cualquier módulo-versión (CA-23.03).

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + nombre ejercicio | CenterAlignedTopAppBar | navigationIcon: ArrowBack (retorna a origen: F2, E5, G1, H2 o D2) · Título: Title Large |
| Top Bar | "Historial completo" | Text | Title Small · On Surface Variant |
| Body | Badge estado progresión | AssistChip M3, read-only | En Progresión: containerColor `#E8F5E9`/`#1A2E1A`, label verde, ícono 🟢 · En Meseta: containerColor `#FFF8E1`/`#4A3800`, label ámbar, ícono 🟡 · En Descarga: containerColor `#E3F2FD`/`#1A2733`, label azul, ícono 🔵 · Sin Historial: containerColor Surface Variant, label On Surface Variant, ícono ⚪ |
| Body | Gráfico tendencia de carga | Canvas/Chart en Filled Card | Fondo card: Surface Container (`#F8EBE0`) · Corner: 12 dp · Padding: 16 dp · Línea: Primary, strokeWidth 2 dp · Puntos: Primary, radio 6 dp (relleno), 3 dp borde On Primary · Grid horizontal: Outline Variant, 1 dp dashed · Labels eje Y: Label Small, On Surface Variant (Kg o "reps" para peso corporal) · Labels eje X: Label Small, On Surface Variant ("S1", "S2"... sesiones) · Height total: 200 dp · Para peso corporal: eje Y = reps totales |
| Body | Separador | Divider M3 | Outline Variant · Margin vertical 16 dp |
| Body | Fila sesión del ejercicio | ListItem M3, 72 dp | overlineContent: Body Medium, On Surface Variant (fecha + módulo-versión: "10 feb 2026 · A-V1") · headlineContent: Body Medium, On Surface (peso + reps totales + RIR: "60 Kg · 45 reps · RIR 2.3") · supportingContent: Body Small con ícono + color semántico (clasificación: "↑ Progresión" verde, "= Mantenimiento" ámbar, "↓ Regresión" rojo) · Divider entre filas |
| Body | "Ver técnica de ejecución →" | Text Button | Color: Primary · Navega a D2 · Margin top: 16 dp |
| Body | Estado vacío | Column centrada | Body Large, On Surface Variant · "No hay registros disponibles para este ejercicio." · Padding vertical: 48 dp |
| Bottom | Bottom Navigation | Condicional | **Visible** si origen = F2, G1, H2, D2 · **Oculto** si origen = E5 |

---

### Flujo G — Métricas y KPIs

#### G1 — Panel de Métricas

**Trazabilidad:** HU-19 (CA-19.01 a CA-19.06) · HU-21 (CA-21.01 a CA-21.07) · RF44, RF46, RF47, RF48

**Contexto visual:** Dashboard vertical con 4 secciones de KPIs + 2 accesos rápidos. Los selectores de período permiten configurar el rango de evaluación.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | "Métricas" | CenterAlignedTopAppBar | Sin navigationIcon · Título: Title Large |
| Body | **Sección 1: Índice de Adherencia** | Filled Card | Fondo: Secondary Container (`#F0E0E0`) · Corner: 12 dp · "Adherencia semanal": Title Medium, On Secondary Container · Porcentaje: Headline Medium, Primary (`#8B1A1A`) · Desglose: Body Medium, On Secondary Container ("5 de 6 sesiones esta semana") · Referencia: objetivo desde J1 (CA-21.05) |
| Body | **Sección 2: RIR Promedio por Módulo** | Filled Card, Surface Container High | Corner: 12 dp · Selector período: ExposedDropdownMenuBox M3, opciones "2 últimas sesiones" (defecto), "4 últimas sesiones", "6 últimas sesiones" (CA-21.02) · OutlinedTextField readOnly con trailingIcon ▼ · Dropdown: Surface Container, Elevation Level 2 · **Fila por módulo (A, B, C):** Row — Title Medium (módulo) + Body Medium fontWeight Medium (valor numérico RIR) + Badge interpretación · Badge Óptimo (2-3): containerColor `#E8F5E9`, label `#1B5E20` (verde oscuro) 🟢 · Badge Riesgo (< 1.5): containerColor `#FFDAD6`, label On Error Container (`#410002`) 🔴 · Badge Insuficiente (> 3.5): containerColor `#FFF8E1`, label `#5D4200` (ámbar oscuro) 🟡 · Referencia: Body Small, On Surface Variant ("2-3 = óptimo, < 1.5 = riesgo, > 3.5 = insuficiente") |
| Body | **Sección 3: Tasa de Progresión** | Column | Selector período: ExposedDropdownMenuBox, opciones "4 semanas" (defecto), "8 semanas", "12 semanas" (CA-19.02) · **Fila por ejercicio:** ListItem M3, 56 dp — headlineContent: Title Medium (nombre) + trailing: Body Medium fontWeight Medium (porcentaje) + ícono tendencia (↑ verde si ≥ 60%, = ámbar si 40-59%, ↓ rojo si < 40%) · clickable → F3 · Divider entre filas |
| Body | **Sección 4: Velocidad de Carga** | Column | **Fila por ejercicio:** ListItem M3, 56 dp — headlineContent: Title Medium (nombre) + trailing: Body Medium ("+X.X Kg/sesión" o "0 Kg/sesión") · Peso corporal: trailing "N/A" en On Surface Variant, fontStyle italic · clickable → F3 · Divider entre filas |
| Body | Separador | Divider M3 | Outline Variant · Margin vertical 16 dp |
| Body | Acceso rápido "Volumen por Grupo Muscular →" | Text Button | Color: Primary · Full width, alineado izquierda · Navega a G2 |
| Body | Acceso rápido "Tendencia de Progresión →" | Text Button | Color: Primary · Navega a G3 |
| Bottom | Bottom Navigation | NavigationBar M3 | Métricas activo |

---

#### G2 — Volumen por Grupo Muscular

**Trazabilidad:** HU-20 (CA-20.01 a CA-20.06) · HU-25 (CA-25.01 a CA-25.04) · RF45, RF49, RF52

**Contexto visual:** Detalle de volumen con selector de microciclo, barras de tonelaje, distribución porcentual y gráfico de evolución temporal.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Volumen por Grupo Muscular" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → G1 |
| Body | Selector microciclo | Row centrada | IconButton ◀: ArrowBack icon, tint On Surface, 48 dp touch · Text: Title Medium, On Surface ("Microciclo 11") · IconButton ▶: ArrowForward icon · Deshabilitado si en el extremo (primer/último microciclo): tint On Surface 38% opacity |
| Body | **Tonelaje por grupo muscular** | Filled Card | Fondo: Surface Container · Corner: 12 dp · 12 filas (una por grupo muscular) · Cada fila: Row — Body Medium On Surface (nombre grupo, width fijo 120 dp) + Barra horizontal (fondo Surface Variant, fill Primary, height 16 dp, corner 4 dp, width proporcional al máximo) + Body Medium On Surface Variant (valor "X,XXX Kg" alineado derecha) · Spacing entre filas: 8 dp |
| Body | **Distribución de volumen (%)** | Filled Card | Fondo: Surface Container · Corner: 12 dp · Misma estructura de filas · Barra: fill Secondary (`#6B4F4F`) · Porcentaje: Body Medium ("22%") |
| Body | **Evolución temporal** (multilínea) | Filled Card | Fondo: Surface Container · Corner: 12 dp · Height: 200 dp · Líneas diferenciadas por grupo (Primary, Secondary, Tertiary y variantes: `#8B1A1A`, `#6B4F4F`, `#5C6B4F`, más tonos) · Leyenda inferior: Label Small con punto de color + nombre grupo · Grid: Outline Variant dashed · Labels ejes: Label Small, On Surface Variant |
| Body | Estado vacío (< 2 microciclos) | Column centrada | Body Large, On Surface Variant · "Se necesitan al menos 2 microciclos para mostrar evolución comparativa." (CA-25.04) |
| Bottom | Bottom Navigation | NavigationBar M3 | Métricas activo |

---

#### G3 — Tendencia de Progresión por Grupo Muscular

**Trazabilidad:** HU-22 (CA-22.01 a CA-22.05) · RF42

**Contexto visual:** Evaluación a largo plazo. Lista simple con clasificación visual por grupo muscular.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Tendencia de Progresión" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → G1 |
| Body | Período evaluado | Text | Body Medium · On Surface Variant · "Evaluación: últimos 4-6 microciclos" · Padding bottom: 16 dp |
| Body | Fila grupo muscular (Ascendente) | ListItem M3, 56 dp | headlineContent: Title Medium, On Surface (nombre grupo) · trailingContent: Row — Body Medium verde `#2E7D32` ("Ascendente") + ícono 📈 16 dp |
| Body | Fila grupo muscular (Estable) | ListItem M3, 56 dp | headlineContent: Title Medium, On Surface · trailingContent: Row — Body Medium ámbar `#8D6E00` ("Estable") + ícono 📊 16 dp |
| Body | Fila grupo muscular (En declive) | ListItem M3, 56 dp | headlineContent: Title Medium, On Surface · trailingContent: Row — Body Medium rojo `#C62828` ("En declive") + ícono 📉 16 dp |
| Body | Divider entre filas | Divider M3 | 1 dp, Outline Variant |
| Body | Estado datos insuficientes (< 4 microciclos) | Column centrada | Body Large, On Surface Variant · "Se necesitan al menos 4 microciclos completados. Faltan N microciclos." (CA-22.04) |
| Bottom | Bottom Navigation | NavigationBar M3 | Métricas activo |

---

### Flujo H — Alertas

#### H1 — Centro de Alertas

**Trazabilidad:** HU-14 · HU-16 · HU-26 a HU-30 · RF34, RF35, RF37, RF53 a RF58 · RNF05
**HU indirectas visibles:** HU-12 (fatiga acumulada → descarga), HU-15 (recomendaciones en H2)

**Contexto visual:** Alertas agrupadas en 2 secciones: Crisis (prioridad máxima, rojo) y Alertas (naranja/amarillo). Las alertas son no bloqueantes (informativas). Diferenciación visual por niveles obligatoria (RNF05).

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Alertas" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → B1 |
| Top Bar | "N alertas activas" | Text | Title Small · On Surface Variant |
| Body | Encabezado "Crisis" | Row | Text: Title Small, Error (`#BA1A1A`) · Solo visible si hay alertas crisis · Divider inferior: Error, 1 dp · Margin bottom: 12 dp |
| Body | Card alerta **crisis (🔴)** | Elevated Card | Fondo: Error Container (`#FFDAD6` / `#930009`) · Elevation Level 2 · Corner: 12 dp · Leading: indicador circular relleno, 12 dp, color `#C62828` / `#EF9A9A` · Título tipo alerta: Title Medium, On Error Container · Entidad afectada: Body Medium, On Error Container · Dato clave: Body Small, On Error Container · Spacing entre cards: 8 dp · clickable → H2 |
| Body | Encabezado "Alertas" | Row | Text: Title Small, On Surface Variant · Solo visible si hay alertas no-crisis · Divider inferior: Outline Variant · Margin top: 16 dp si hay crisis arriba |
| Body | Card alerta **alta (🟠)** | Filled Card | Fondo: `#FFF3E0` / `#4E2600` · Corner: 12 dp · Leading: indicador circular 12 dp, color `#E65100` / `#FFB74D` · Texto: On Surface (`#1C1B1B`) / On Surface dark (`#E8E0DC`) · Tipos: meseta detectada, módulo requiere descarga · clickable → H2 |
| Body | Card alerta **media (🟡)** | Filled Card | Fondo: `#FFF8E1` / `#4A3800` · Corner: 12 dp · Leading: indicador circular 12 dp, color `#8D6E00` / `#FFD54F` · Texto: On Surface (`#1C1B1B`) / On Surface dark (`#E8E0DC`) · Tipos: RIR fuera de rango, adherencia baja, caída tonelaje > 10%, inactividad > 10 días, tasa progresión < 40% · clickable → H2 |
| Body | Estado vacío | Column centrada | ícono ✅ 48 dp, color verde `#2E7D32` · Text: Body Large, On Surface Variant, "No hay alertas activas. Todo en orden." · Padding vertical: 48 dp |
| Bottom | Bottom Navigation | NavigationBar M3 | Inicio activo (se llega desde B1) |

**Tipos de alerta y su nivel visual:**

| Tipo | Entidad | Nivel 🟡 | Nivel 🟠 | Nivel 🔴 |
|------|---------|----------|----------|----------|
| Tasa de progresión baja | Ejercicio | < 40% | — | < 20% |
| Meseta detectada | Ejercicio | — | 3 sesiones sin progresión | — |
| RIR fuera de rango | Módulo | < 1.5 o > 3.5 sostenido 2+ sesiones | — | — |
| Adherencia baja | Semanal | < 60% (1 semana) | — | < 60% (2+ semanas) |
| Caída de tonelaje | Grupo muscular | > 10% | — | > 20% |
| Inactividad por módulo | Módulo | > 10 días | — | > 14 días |
| Módulo requiere descarga | Módulo | — | ≥ 50% en meseta/regresión | — |

---

#### H2 — Detalle de Alerta

**Trazabilidad:** HU-14 (CA-14.04 a CA-14.06) · HU-15 (CA-15.01 a CA-15.05) · HU-16 · HU-26 a HU-30 · RF35, RF36, RF53 a RF58 · RNF05
**HU indirecta visible:** HU-12 (contextualiza caídas de tonelaje: descarga planificada vs. regresión)

**Contexto visual:** Desglose completo de una alerta con datos, análisis causal y recomendaciones escalonadas. Las recomendaciones son informativas (no bloqueantes, CA-15.04).

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Detalle de Alerta" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → H1 |
| Body | Card tipo y nivel | Filled Card | Fondo: color según nivel de alerta (ver H1 §4.3) · Corner: 12 dp · Leading: indicador de nivel (circular, 16 dp) · Título tipo: Title Medium, On [nivel] Container (ej. "MESETA DETECTADA") · Entidad: Body Medium (nombre ejercicio/módulo) · Dato resumen: Body Small ("3 sesiones sin progresión") |
| Body | Datos que dispararon la alerta | Filled Card | Fondo: Surface Container High (`#F2E5DA`) · Corner: 12 dp · Contenido: Column de Body Medium, On Surface (sesión por sesión: "Sesión 1: 22.5 Kg · 38 reps", etc.) · Período: Body Small, On Surface Variant ("Período: 27 ene — 10 feb") |
| Body | Análisis causal | Column | Text: Body Large, On Surface · Sin card (texto directo sobre Background) · Padding horizontal: 16 dp · **Contenido varía por tipo de alerta:** Meseta + RIR bajo (0-1): "entrenando cerca del fallo técnico — posible límite de carga" (CA-14.04) · Meseta + RIR alto (3+): "carga conservadora — margen para incrementar" (CA-14.05) · Estancamiento grupal: "fatiga sistémica del grupo muscular" (CA-14.06) · Caída tonelaje + descarga activa: "Caída esperada por descarga — controlada" (HU-12, HU-29 CA-29.03, CA-29.04) en color azul descarga, no crisis |
| Body | Recomendaciones escalonadas | Column | Título: Title Small, On Surface ("Recomendaciones") · Cada recomendación: Row — Leading `▸` Text Primary + Body Medium On Surface · Sesión 4: "Intentar microincremento (+2.5 Kg) o extensión de reps" (CA-15.01) · Sesión 6+: "Considerar rotar versión del módulo" (CA-15.02, CA-15.03) · Padding entre recomendaciones: 8 dp |
| Body | "Ver historial del ejercicio →" | Text Button | **Condicional:** visible en alertas de meseta y tasa progresión baja · Color: Primary · Navega a F3 |
| Body | "Gestionar descarga →" | Text Button | **Condicional:** visible en alertas de descarga/fatiga (módulo requiere descarga, caída tonelaje por fatiga) · Color: Primary · Navega a I1 |
| Bottom | Bottom Navigation | NavigationBar M3 | Inicio activo |

---

### Flujo I — Descarga

#### I1 — Gestión de Descarga

**Trazabilidad:** HU-16 (CA-16.01, CA-16.04) · HU-17 (CA-17.01 a CA-17.09) · RF37 a RF40
**HU indirecta visible:** HU-12 (detección de fatiga acumulada → módulo requiere descarga)

**Contexto visual:** Pantalla con 3 estados mutuamente excluyentes. El protocolo de descarga incluye excepciones para peso corporal e isométricos (CA-17.09). La activación es decisión del ejecutante (informativa, CA-16.04).

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Gestión de Descarga" | CenterAlignedTopAppBar | navigationIcon: ArrowBack (retorna a origen: B1 o H2) |
| | | | |
| **Estado A: Descarga requerida, no activa** | | | |
| Body | Card estado actual | Outlined Card | Borde: Outline · Corner: 12 dp · "No hay descarga activa": Body Large, On Surface · Ícono ⚠️ + "Módulo X requiere descarga": Body Medium, Error · "≥ 50% de los ejercicios están en meseta o regresión": Body Small, On Surface Variant |
| Body | Protocolo de descarga | Filled Card | Fondo: Secondary Container (`#F0E0E0`) · Corner: 12 dp · Título: Title Small, On Secondary Container ("Al activar la descarga:") · Parámetros, cada uno como Body Medium, On Secondary Container, con leading "·": Carga al 60%, 4 series, 8 repeticiones, RIR 4-5, Duración 1 microciclo (6 sesiones), Versiones congeladas · **Excepciones (CA-17.09):** "· Peso corporal: 8 reps, RIR 4-5 (sin ajuste de carga, Peso = 0)" e "· Isométricos: 30 seg, RIR 4-5" · Nota final: "· Al finalizar: reinicio al 90% de carga pre-descarga" |
| Body | Botón "Activar Descarga" | Filled Button, full width | containerColor: Primary · Margin top: 24 dp |
| | | | |
| **Estado B: Descarga activa** | | | |
| Body | Card progreso | Filled Card | Fondo: Secondary Container · Corner: 12 dp · Ícono 🔄 24 dp color azul descarga (`#1565C0`) + "Descarga activa": Title Medium, color azul · "Progreso: N/6 sesiones": Body Medium, On Secondary Container · LinearProgressIndicator determinate: track Surface Variant, indicator azul descarga, height 8 dp · "Sesiones restantes: N": Body Medium, On Secondary Container |
| Body | Parámetros vigentes | Filled Card | Fondo: Surface Container High · Corner: 12 dp · Lista de parámetros: Body Medium, On Surface ("Carga: 60%", "Series: 4", "Reps: 8", "RIR objetivo: 4-5") · Versiones congeladas: Body Medium, On Surface Variant ("Versión congelada: A-V2, B-V1, C-V3") |
| | | | |
| **Estado C: Post-descarga** | | | |
| Body | Card reinicio | Filled Card | Fondo: Tertiary Container (`#E0EEDD`) · Corner: 12 dp · Ícono ✅ 24 dp verde · "Descarga completada": Title Medium, On Tertiary Container (`#334A29`) · "Cargas de reinicio (90% pre-descarga):": Body Medium, On Tertiary Container · Lista ejercicios: Body Medium, On Tertiary Container ("Press de banca: 54 Kg", etc.) · Nota: Body Small, On Tertiary Container italic ("Las versiones retoman su avance normal.") |
| Bottom | Bottom Navigation | NavigationBar M3 | Inicio activo |

---

### Flujo J — Configuración y Respaldo

#### J1 — Configuración

**Trazabilidad:** HU-21 (CA-21.05) · HU-31 · HU-32 · RNF15, RNF16

**Contexto visual:** Menú de opciones agrupado en 3 secciones. El selector de frecuencia persiste inmediatamente al cambio. Los enlaces de respaldo navegan a J2/J3.

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | "Configuración" | CenterAlignedTopAppBar | Sin navigationIcon · Título: Title Large |
| Body | Encabezado "Perfil" | Text | Label Large · On Surface Variant · Padding top: 8 dp |
| Body | "Editar perfil" | ListItem M3, 56 dp | headlineContent: Body Large, On Surface ("Editar perfil") · trailingContent: ícono ChevronRight, On Surface Variant, 24 dp · clickable → C1 |
| Body | Divider | Divider M3 | 1 dp, Outline Variant · Margin vertical: 8 dp |
| Body | Encabezado "Entrenamiento" | Text | Label Large · On Surface Variant |
| Body | "Objetivo de frecuencia semanal" | Column | Label: Body Medium, On Surface ("Objetivo de frecuencia semanal") · Selector: Row de 3 chips (4, 5, 6), mismos estilos que selector RIR (§7.6): 48×48 dp, single select, Primary cuando seleccionado · Sublabel: Body Small, On Surface Variant ("sesiones/semana") · Rango: 4 a 6 (CA-21.05) · Persiste inmediatamente al seleccionar |
| Body | Divider | Divider M3 | 1 dp, Outline Variant · Margin vertical: 8 dp |
| Body | Encabezado "Datos" | Text | Label Large · On Surface Variant |
| Body | "Exportar respaldo" | ListItem M3, 56 dp | headlineContent: Body Large, On Surface · trailingContent: ChevronRight · clickable → J2 |
| Body | "Importar respaldo" | ListItem M3, 56 dp | headlineContent: Body Large, On Surface · trailingContent: ChevronRight · clickable → J3 |
| Bottom | Bottom Navigation | NavigationBar M3 | Configuración activo |

---

#### J2 — Exportar Respaldo

**Trazabilidad:** HU-31 (CA-31.01 a CA-31.07) · RNF15, RNF17, RNF18, RNF26

**Contexto visual:** Flujo lineal de 3 pasos (pre-export → durante → post-export). La advertencia de no-cifrado es obligatoria (RNF26). El proceso < 10 segundos (RNF18).

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Exportar Respaldo" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → J1 |
| Body (pre) | Card advertencia | Outlined Card | Borde: Outline · Corner: 12 dp · Leading: ícono ⚠️ 24 dp, tint `#E65100` · Text: Body Medium, On Surface ("El archivo de respaldo contiene todos tus datos de entrenamiento y no está cifrado.") · Padding: 16 dp |
| Body (pre) | Botón "Exportar datos" | Filled Button, full width | containerColor: Primary · Margin top: 24 dp |
| Body (durante) | Card progreso | Filled Card | Fondo: Surface Container · Corner: 12 dp · "Exportando datos...": Body Medium, On Surface · LinearProgressIndicator indeterminate: indicator Primary |
| Body (post) | Card confirmación | Filled Card | Fondo: Tertiary Container (`#E0EEDD`) · Corner: 12 dp · Leading: ícono ✅ 24 dp, verde · "Respaldo exportado": Title Medium, On Tertiary Container · "Archivo: tension_backup_YYYYMMDD.json": Body Medium · "Ubicación: Descargas/": Body Small, On Tertiary Container |
| Body (post) | Botón "Compartir" | Filled Tonal Button | containerColor: Secondary Container · contentColor: On Secondary Container · Abre share sheet nativo (CA-31.03) |
| Bottom | Bottom Navigation | NavigationBar M3 | Configuración activo |

---

#### J3 — Importar Respaldo

**Trazabilidad:** HU-32 (CA-32.01 a CA-32.08) · RNF16, RNF17, RNF18

**Contexto visual:** Flujo de 4 pasos con confirmación explícita antes de la operación destructiva. El botón de restauración usa estilo Error (acción destructiva). Rollback automático en fallo (CA-32.08).

| Zona | Elemento | Componente M3 | Estilo detallado |
|------|----------|----------------|------------------|
| Top Bar | `←` + "Importar Respaldo" | CenterAlignedTopAppBar | navigationIcon: ArrowBack → J1 (solo si no se ha iniciado restauración) |
| Body (paso 1) | Botón "Seleccionar archivo" | Outlined Button, full width | borderColor: Outline · leadingIcon: FileOpen Material Symbol, tint On Surface · Abre file picker del sistema (CA-32.01) |
| Body (paso 2, válido) | Card validación OK | Filled Card | Fondo: Tertiary Container · Corner: 12 dp · ícono ✅ · "Archivo válido": Title Medium · "Versión: X.X": Body Medium · "Fecha del respaldo: dd MMM yyyy": Body Medium · "Sesiones incluidas: N": Body Medium |
| Body (paso 2, inválido) | Card validación error | Filled Card | Fondo: Error Container (`#FFDAD6`) · Corner: 12 dp · ícono ❌ 24 dp, Error · "Archivo no válido": Title Medium, On Error Container · "El archivo seleccionado no es un respaldo válido o está corrupto.": Body Medium (CA-32.02) |
| Body (paso 3) | Card advertencia | Filled Card | Fondo: Error Container · Corner: 12 dp · ícono ⚠️ 24 dp, Error · "ATENCIÓN": Title Medium, On Error Container, fontWeight Bold · "Todos los datos actuales serán reemplazados por los datos del respaldo. Esta operación no es reversible.": Body Medium, On Error Container (CA-32.03) |
| Body (paso 3) | Botón "Restaurar datos" | Filled Button Error, full width | containerColor: Error (`#BA1A1A`) · contentColor: On Error (`#FFFFFF`) · Height: 48 dp |
| Body (paso 3) | "Cancelar" | Text Button, centrado | Color: Primary · Retorna a J1 |
| Body (paso 4) | Indicador progreso | Column | "Restaurando datos...": Body Medium, On Surface · LinearProgressIndicator indeterminate: indicator Primary · Padding: 24 dp |
| Body (resultado OK) | Texto éxito | Column centrada | ícono ✅ 48 dp, verde · "Datos restaurados exitosamente.": Body Large, On Surface, centrado · Navegación automática a B1 tras 2 segundos (CA-32.04) |
| Body (resultado error) | Card error rollback | Filled Card | Fondo: Error Container · ícono ❌ · "La importación falló. Tus datos originales han sido preservados.": Body Medium, On Error Container (CA-32.08) · Botón "Volver a Configuración": Outlined Button, borderColor Outline → J1 |
| Bottom | Bottom Navigation | NavigationBar M3 | Configuración activo |

---

## 9. Animaciones y Transiciones

| Transición | Tipo | Duración | Curva |
|------------|------|----------|-------|
| Navegación de avance (→) | Slide in from right | 300 ms | EaseOut |
| Retorno (←) | Slide in from left | 250 ms | EaseOut |
| Diálogo aparece (E4, E3) | Fade + Scale from 0.8 | 200 ms | EaseOut |
| Diálogo desaparece | Fade out | 150 ms | EaseIn |
| Bottom Nav switch | Crossfade | 200 ms | Linear |
| Tabs D1↔D3 | Horizontal pager slide | 300 ms | EaseInOut |
| Progress bar update (E1, I1) | Animate float | 300 ms | EaseInOut |
| Card condicional aparece (B1 reanudar/descarga) | Expand vertically | 250 ms | EaseOut |
| Chip selección (filtros, RIR) | Container color animate | 150 ms | EaseInOut |
| Estado de ejercicio cambia (E1) | Background color animate | 300 ms | EaseInOut |

---

## 10. Accesibilidad

| Aspecto | Especificación |
|---------|----------------|
| Contraste mínimo | Todos los pares texto/fondo cumplen WCAG AA (ratio ≥ 4.5:1 para texto normal, ≥ 3:1 para texto grande). Los colores semánticos (§4.3) fueron validados contra sus fondos respectivos; el ámbar claro usa `#8D6E00` (ratio ~7:1 sobre Background) en lugar de `#F9A825` para cumplir AA. Las señales de progresión/estado usan siempre ícono + color + texto (RNF05), proporcionando redundancia adicional. |
| Touch target | ≥ 48 × 48 dp para todo elemento interactivo (RNF06) |
| Content descriptions | Todos los íconos de estado (⚪🔵✅↑=↓🔴🟠🟡📈📊📉🏆) llevan contentDescription en español |
| Semántica M3 | Componentes M3 aportan semántica accesible por defecto (roles, estados) |
| No depender solo de color | Toda señal visual usa color + ícono + texto (RNF05). Ejemplo: progresión = verde + ↑ + "Progresión" |
| Focus order | Orden natural top-to-bottom. Formularios (A1, C1, E2): campos → botón confirmar |
| State layers | Los componentes interactivos usan state layers M3 por defecto: pressed = 12% On Surface overlay, focused = 12%, hovered = 8%. No se definen state overrides personalizados. |
