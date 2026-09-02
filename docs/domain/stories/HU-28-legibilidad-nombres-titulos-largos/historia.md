# Historia de Usuario

**Como** El Ejecutante,
**Quiero** leer completo el nombre del ejercicio, el título de la sesión y el contador de series en cualquier pantalla de la aplicación,
**Para** saber en todo momento qué estoy haciendo y en qué punto voy, sin tener que adivinar a partir de un texto cortado.

## Descripción

Cuando el nombre de un ejercicio o de una sesión es largo, se muestra cortado e incompleto. Ocurre en la sesión activa, en la pestaña Plan, en el Diccionario de Ejercicios y en todos los apartados donde aparece el título de la sesión. El indicador "Serie 1 de X" también se corta en ocasiones, precisamente cuando compite por espacio con un nombre de ejercicio largo.

Es un problema de presentación puro: no cambia ningún dato, ninguna regla de negocio ni ninguna entidad. Se resuelve estableciendo una **regla única de tratamiento de texto largo** y aplicándola de forma consistente en todas las ubicaciones afectadas.

La regla acordada es: el texto ocupa **hasta 2 líneas** antes de aplicar elipsis, y el contador de serie **nunca cede espacio** — si algo debe recortarse, es el nombre del ejercicio.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-28.01 — Nombre de ejercicio completo

- **Dado** que un ejercicio tiene un nombre largo (ej. "Extensión de Tríceps por encima de la Cabeza")
- **Cuando** El Ejecutante lo visualiza en cualquier pantalla de la aplicación
- **Entonces** el nombre se muestra en hasta 2 líneas antes de aplicar elipsis
- **Y** no se corta a mitad de palabra en la primera línea

#### CA-28.02 — Título de sesión legible en todos los apartados

- **Dado** que una rutina tiene un nombre largo (ej. "Martes: Espalda, Bíceps y Abdomen (Pull - Foco Dorsal Ancho)")
- **Cuando** El Ejecutante lo visualiza en la pantalla de inicio, la sesión activa, el preview de sesión, el resumen post-sesión, el historial de sesiones o el detalle de sesión
- **Entonces** el título es legible en su totalidad o en hasta 2 líneas con elipsis
- **Y** el comportamiento es idéntico en todas esas pantallas

### Escenario 2: Validaciones

#### CA-28.03 — Contador de serie nunca truncado

- **Dado** que la pantalla de sesión activa muestra el indicador "Serie X de Y"
- **Cuando** el nombre del ejercicio compite por el espacio horizontal disponible
- **Entonces** el contador de serie se muestra **completo y legible en todos los casos**
- **Y** es el nombre del ejercicio el que cede espacio, nunca el contador

#### CA-28.04 — Legibilidad en catálogo y plan

- **Dado** que El Ejecutante navega el Diccionario de Ejercicios o la pestaña Plan
- **Cuando** un nombre de ejercicio, de rutina o de versión excede el ancho disponible
- **Entonces** se aplica la misma regla de 2 líneas con elipsis
- **Y** ningún elemento de la lista queda con texto ilegible o cortado a mitad de palabra sin indicador

### Escenario 3: Casos Extremos

#### CA-28.05 — Nombre que excede dos líneas

- **Dado** que un nombre es tan largo que no cabe ni en 2 líneas
- **Cuando** El Ejecutante lo visualiza
- **Entonces** se muestra elipsis al final de la segunda línea
- **Y** el nombre completo permanece accesible en la pantalla de detalle del elemento

#### CA-28.06 — Pantalla estrecha

- **Dado** que El Ejecutante usa un dispositivo de 5 pulgadas (mínimo soportado por RNF21)
- **Cuando** visualiza una sesión activa con un nombre de ejercicio largo
- **Entonces** el contador de serie sigue siendo legible por completo
- **Y** el nombre del ejercicio conserva la regla de 2 líneas sin romper el diseño

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante — único usuario del sistema. La aplicación es single-user y no maneja roles.
- **Permisos requeridos:** Ninguno.
- **Valor de negocio:** Durante una sesión de entrenamiento el ejecutante consulta la pantalla entre series, con poco tiempo y poca atención disponible. Un texto cortado lo obliga a inferir qué ejercicio o qué serie le toca. Corregirlo elimina fricción en el momento de mayor demanda cognitiva.

### Reglas de Negocio

1. **Regla única de texto largo:** hasta 2 líneas, elipsis solo si excede. Se aplica de forma idéntica en todas las ubicaciones.
2. **Prioridad de espacio:** el contador "Serie X de Y" tiene prioridad absoluta sobre el nombre del ejercicio. Nunca se trunca.
3. **Consistencia:** ninguna pantalla implementa un tratamiento propio distinto al de las demás.
4. **Sin pérdida de información:** cuando se aplica elipsis, el texto completo permanece accesible en la vista de detalle del elemento.

### Interfaz

No se crea ninguna pantalla nueva. Se ajusta el tratamiento de texto en las ubicaciones afectadas:

- Sesión activa — nombre del ejercicio y contador "Serie X de Y"
- Pantalla de inicio — título de la sesión propuesta
- Preview de sesión — título de sesión y nombres de ejercicio
- Resumen post-sesión — título de sesión y nombres de ejercicio
- Historial de sesiones y detalle de sesión — título de sesión y nombres de ejercicio
- Diccionario de Ejercicios — nombres de ejercicio en lista y detalle
- Pestaña Plan — nombres de rutina, de versión y de ejercicio asignado

#### Detalle de Interfaz de Usuario

- **Diseño general:** Sin cambios estructurales. Se conserva el sistema visual y la navegación vigentes.
- **Campos y controles:** Ninguno nuevo. Solo se modifica el comportamiento de presentación de los componentes de texto existentes.
- **Flujo de navegación visual:** Sin cambios.
- **Mensajes y feedback:** Sin mensajes nuevos.

### Sistemas Externos

Ninguno. La aplicación opera 100% offline sobre almacenamiento local (RNF09, RNF14).

### Preview de Interfaz

**Preview:** [`28.preview.txt`](./28.preview.txt) | **Formato:** ASCII (wireframe de texto)

Aplican los mockups existentes de sesión activa, inicio, preview de sesión, resumen post-sesión, historial, Diccionario de Ejercicios y pestaña Plan, con el tratamiento de texto ajustado.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/interfaces_contract.md`, `docs/architecture/coding-standards.md`

**Historias relacionadas:**

- **HU-06** — Registrar series en sesión activa. Contiene el contador "Serie X de Y".
- **HU-22** — Preview de sesión y cronómetro. Una de las pantallas afectadas.
- **HU-03 y HU-24** — Diccionario de Ejercicios. Otra de las pantallas afectadas.
- **HU-17** — Historial de ejercicios y sesiones. Otra de las pantallas afectadas.

**Restricciones transversales aplicables:**

- RNF06 — Elementos interactivos mínimo 48×48 dp
- RNF07 — Solo modo vertical
- RNF21 — Pantallas de 5" a 7", layout responsivo
- RNF22 — Resoluciones desde 720p hasta 1440p
- RNF23 — Tema claro/oscuro automático
- **Beta sin migración:** la base de datos se reinicia; no se requiere migración de datos.

**Lecciones aprendidas:** Un tratamiento de texto resuelto pantalla por pantalla produce inconsistencias que reaparecen con cada pantalla nueva. La regla debe quedar centralizada y reutilizable.

---

## Definición de Terminado (Inicial)

- [ ] Regla de texto largo (2 líneas + elipsis) definida de forma centralizada y reutilizable
- [ ] Aplicada en las 7 ubicaciones identificadas
- [ ] Contador "Serie X de Y" legible por completo en todos los casos, incluida pantalla de 5"
- [ ] Nombre completo accesible en la vista de detalle cuando se aplica elipsis
- [ ] Verificado en tema claro y oscuro
- [ ] Sin regresión visual en el resto de la aplicación
