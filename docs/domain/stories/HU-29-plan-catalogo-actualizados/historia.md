# Historia de Usuario

**Como** El Sistema en el momento de inicialización (instalación fresca),
**Quiero** prepoblar la base de datos con el plan de entrenamiento vigente de El Ejecutante y con un catálogo de ejercicios completo y correctamente clasificado,
**Para** que El Ejecutante inicie su primera sesión con el programa que realmente entrena, sin configurar nada manualmente y sin arrastrar ejercicios mal catalogados.

## Descripción

HU-27 dejó un plan seed de 6 rutinas. Esta historia aplica un **delta** sobre ese resultado para alinearlo con el plan definitivo de El Ejecutante, e incorpora las correcciones de catalogación muscular detectadas.

Los cambios son de tres tipos:

**1. Composición del plan.** Cambian el orden de ejercicios, salen tres asignaciones (*Remo al Mentón* del lunes, *Zancadas* y *Extensión de Cuádriceps* del sábado), entran ejercicios nuevos y se añaden slots duales.

**2. Ampliación del catálogo.** Cuatro ejercicios no existen en el catálogo y deben registrarse: *Press Militar*, *Dominadas*, *Remo Unilateral en Polea Baja* y *Remo Unilateral en Polea Alta*. Un quinto ajuste es de nomenclatura: *Tirón de Dorsales* se renombra a **Jalón al Pecho**, que es el nombre de uso común del mismo movimiento — conserva su identidad, su imagen, su zona muscular y su historial.

**3. Corrección de catalogación.** *Remo al Mentón* figura hoy como ejercicio de Hombro y Trapecio, cuando es un movimiento de Espalda Alta. Se recataloga y se audita la totalidad del catálogo bajo criterio biomecánico.

**Ningún ejercicio se elimina.** Los que salen del plan por defecto permanecen en el Diccionario como alternativas elegibles.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-29.01 — Composición del plan por defecto

- **Dado** que la aplicación se instala por primera vez
- **Cuando** se ejecuta el seeder del plan
- **Entonces** el plan por defecto refleja exactamente esta composición:

| Rutina | Ejercicios (orden · series) |
|---|---|
| **Lunes** — Push, Foco Deltoides Lateral y Medio | Elevación Lateral (4) · Press de Banca Inclinado **o** Press Militar (3) · Press de Banca Plano (3) · Aperturas (3) |
| **Martes** — Pull, Foco Dorsal Ancho | Jalón al Pecho **o** Dominadas (4) · Curl Martillo (3) · Remo Unilateral en Polea Baja (3) · Curl Bayesian en Banco Inclinado (3) · Pull-Over (3) · Crunch Abdominal (3) |
| **Miércoles** — Lower, Foco Cuádriceps | Extensión de Cuádriceps (4) · Sentadilla Hack **o** Prensa Inclinada (3) · Sentadilla Búlgara (3) · Aductores (3) · Elevación de Pantorrilla en Máquina de Pie (3) |
| **Jueves** — Push, Foco Tríceps | Extensión de Tríceps por encima de la Cabeza (4) · Press de Banca Plano (3) · Aperturas (3) · Extensión de Tríceps en Polea Pushdown (3) · Rompecráneos (3) |
| **Viernes** — Pull, Foco Trapecios y Espalda Media | Remo T Inclinado (4) · Face Pull **o** Vuelos Posteriores (3) · Remo Horizontal (3) · Remo Unilateral en Polea Alta (3) · Curl de Predicador (3) · Crunch Abdominal (3) |
| **Sábado** — Lower, Foco Isquiotibiales y Glúteo | Curl de Isquiotibiales Sentado (4) · Peso Muerto Rumano (3) · Hip Thrust (3) · Aductores (3) · Elevación de Pantorrilla en Máquina de Pie (3) |

- **Y** todos los ejercicios usan el rango de repeticiones 8-12
- **Y** siguen existiendo exactamente 6 rutinas con una versión cada una
- **Y** el nombre de cada rutina refleja el enfoque indicado en la tabla

#### CA-29.02 — Ejercicios nuevos registrados

- **Dado** que el plan referencia ejercicios que no existen en el catálogo
- **Cuando** se ejecuta el seeder del catálogo
- **Entonces** quedan registrados con su equipamiento y su zona muscular:

| Ejercicio | Equipamiento | Zona muscular | Peso corporal |
|---|---|---|:---:|
| Press Militar | Mancuernas | Hombro | No |
| Dominadas | Barra Fija | Dorsal Ancho | **Sí** |
| Remo Unilateral en Polea Baja | Polea | Espalda Media | No |
| Remo Unilateral en Polea Alta | Polea | Espalda Alta | No |

- **Y** el conteo total de ejercicios seed pasa de 33 a **37**

#### CA-29.03 — Renombrado de Tirón de Dorsales

- **Dado** que *Tirón de Dorsales* y *Jalón al Pecho* designan el mismo movimiento
- **Cuando** se ejecuta el seeder
- **Entonces** ese ejercicio queda registrado con el nombre **Jalón al Pecho**
- **Y** conserva su identificador, su imagen, su equipamiento y su zona muscular (Dorsal Ancho)
- **Y** no se crea un ejercicio duplicado

### Escenario 2: Validaciones

#### CA-29.04 — Slots duales del nuevo plan

- **Dado** que el plan define slots con dos ejercicios intercambiables
- **Cuando** se ejecuta el seeder
- **Entonces** existen exactamente **4 slots duales**, cada par compartiendo slot dentro de su versión de rutina con idénticas series y repeticiones:
  - Lunes — Press de Banca Inclinado / Press Militar
  - Martes — Jalón al Pecho / Dominadas
  - Miércoles — Sentadilla Hack / Prensa Inclinada
  - Viernes — Face Pull / Vuelos Posteriores
- **Y** el primer ejercicio de cada par es el primario y el segundo la alternativa
- **Y** *Curl Bayesian en Banco Inclinado* es un **único ejercicio**, no un slot dual

#### CA-29.05 — Variantes de equipamiento no generan slots duales

- **Dado** que el plan menciona ejercicios con alternativa de equipamiento (ej. "Barra o Mancuernas", "Mancuernas o Polea")
- **Cuando** se registra ese ejercicio en el plan
- **Entonces** se inserta una única asignación para ese slot
- **Y** el nombre del ejercicio no incorpora la mención del equipamiento

#### CA-29.06 — Recursos visuales de los ejercicios nuevos

- **Dado** que cada ejercicio del catálogo debe tener una imagen asociada
- **Cuando** se registran los 4 ejercicios nuevos
- **Entonces** cada uno referencia por nombre, sin ruta ni extensión, su archivo PNG en `app/src/main/assets/exercises/`:

| Ejercicio | `media_resource` |
|---|---|
| Press Militar | `press_militar_mancuernas` |
| Dominadas | `dominadas_barra_fija` |
| Remo Unilateral en Polea Baja | `remo_unilateral_en_polea_baja_polea` |
| Remo Unilateral en Polea Alta | `remo_unilateral_en_polea_alta_polea` |

- **Y** los 4 archivos son PNG reales, conforme a la resolución de assets vigente que compone la ruta como `exercises/{media_resource}.png`
- **Y** el nombre de cada recurso sigue la convención `<nombre_snake>_<equipamiento_snake>`
- **Y** la carpeta contiene exactamente 37 archivos PNG, uno por ejercicio del catálogo
- **Y** El Ejecutante visualiza la imagen correcta en el detalle del ejercicio y en la sesión activa

#### CA-29.07 — Preservación del diccionario

- **Dado** que existen ejercicios que salen del plan por defecto
- **Cuando** se ejecuta el seeder
- **Entonces** *Remo al Mentón*, *Zancadas* y todos los demás ejercicios previos siguen presentes en el Diccionario
- **Y** siguen siendo elegibles como alternativa de slot o para asignación manual al plan
- **Y** no se ejecuta ningún borrado sobre el catálogo de ejercicios

### Escenario 3: Casos Extremos

#### CA-29.08 — Auditoría de catalogación muscular

- **Dado** que existen ejercicios asignados a zonas musculares incorrectas
- **Cuando** se revisa el catálogo completo
- **Entonces** *Remo al Mentón* queda catalogado en **Espalda Alta** y deja de figurar como ejercicio de Hombro y Trapecio
- **Y** se audita la totalidad de las asignaciones ejercicio-zona bajo criterio biomecánico
- **Y** toda corrección adicional detectada queda registrada en el archivo de cambios de esta historia

#### CA-29.09 — Funcionalidad de gestión del plan no regresionada

- **Dado** que El Ejecutante tiene la aplicación con el nuevo plan por defecto
- **Cuando** crea una versión de rutina, asigna o remueve ejercicios, o agrega alternativas a un slot
- **Entonces** todas esas operaciones siguen funcionando sin cambios
- **Y** el plan por defecto es únicamente el punto de partida, no una restricción de ejecución

#### CA-29.10 — Documentación actualizada

- **Dado** que cambian el catálogo seed y el plan por defecto
- **Cuando** se actualiza la documentación
- **Entonces** `docs/architecture/domain_and_state_model.md` refleja el nuevo conteo de ejercicios (37), el conteo de relaciones ejercicio-zona y la composición del plan por defecto en su sección de Datos Semilla
- **Y** `docs/architecture/architecture_blueprint.md` refleja el conteo actualizado del catálogo

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Sistema, como agente de inicialización. El Ejecutante es el beneficiario.
- **Permisos requeridos:** Ninguno — operación interna de la inicialización de la base de datos.
- **Valor de negocio:** El Ejecutante arranca con el programa que realmente entrena, sin reconstruirlo a mano y sin ejercicios clasificados en el grupo muscular equivocado. Una catalogación errónea contamina además las métricas por grupo muscular y las alertas de tonelaje.

### Reglas de Negocio

1. **El diccionario no se depura.** Ningún ejercicio se elimina jamás del catálogo. Los que salen del plan quedan disponibles como alternativa.
2. **Variante de equipamiento ≠ ejercicio distinto.** "Barra o Mancuernas" describe el implemento de un mismo ejercicio y se modela como un único registro. "Face Pull o Vuelos Posteriores" son ejercicios distintos y se modelan como slot dual.
3. **Renombrar no es duplicar.** *Jalón al Pecho* es el mismo ejercicio que *Tirón de Dorsales*: se cambia el nombre conservando identidad, imagen, equipamiento, zona muscular e historial.
4. **Catalogación biomecánica.** La zona muscular de un ejercicio se asigna por el músculo que ejecuta el movimiento, no por la máquina ni por la ubicación aparente.
5. **Alcance exclusivo a instalación fresca.** Los cambios aplican al seed. No se requiere migración.

### Interfaz

No hay cambios en la interfaz de usuario. Las pantallas de Plan de Entrenamiento, Detalle de Versión de Rutina, Diccionario de Ejercicios y Sesión Activa ya soportan todo lo que esta historia produce: rutinas, versiones, slots simples y slots duales.

Los cambios son exclusivamente de datos semilla y documentación.

### Sistemas Externos

Ninguno. La operación es 100% local, ejecutada durante la inicialización de la base de datos en instalación fresca.

### Preview de Interfaz

**Preview:** [`29.preview.txt`](./29.preview.txt) | **Formato:** ASCII (wireframe de texto)

Sin cambios de interfaz. Aplican los mockups existentes de Plan de Entrenamiento, Detalle de Versión de Rutina, Diccionario de Ejercicios y Sesión Activa.

---

## Contexto y Referencias

**Precondición: ✅ cumplida (2026-08-30).** Los 4 recursos visuales ya están en `app/src/main/assets/exercises/`. El Ejecutante los aportó como JPEG; se convirtieron a PNG real y se renombraron a la convención del catálogo, dejando la carpeta con 37 archivos — uno por ejercicio. No queda ninguna dependencia externa para iniciar el desarrollo.

**Arquitectura:** `docs/architecture/domain_and_state_model.md` (entidades `exercise`, `exercise_muscle_zone`, `routine`, `routine_version`, `plan_assignment`; sección 6.1 Datos Semilla), `docs/architecture/architecture_blueprint.md`

**Historias relacionadas:**

- **HU-27** — Plan predeterminado de 6 rutinas. Esta historia aplica un delta sobre su resultado.
- **HU-26** — Alternativas por slot. Esta historia instancia 4 slots duales bajo su modelo.
- **HU-24** — Actualización del Diccionario de Ejercicios. Estableció el catálogo de 33 ejercicios.
- **HU-03** — Diccionario de Ejercicios con media visual.
- **HU-23** — Rutinas y versiones definidas por el usuario. Base de `routine_version` y `plan_assignment`.

**Restricciones transversales aplicables:**

- RNF31 — Seed data en recursos versionados, no hardcodeado
- RNF14 — Base de datos local SQLite
- **Beta sin migración:** la base de datos se reinicia; los cambios se validan sobre instalación fresca. Excepción documentada a RNF19, limitada a esta historia.

**Lecciones aprendidas:**

- HU-27 estableció la distinción entre variante de equipamiento (un ejercicio) y ejercicio distinto con misma zona muscular (slot dual). Es crítico aplicarla al mapear el plan.
- Una catalogación muscular errónea no es un problema cosmético: contamina las métricas por grupo muscular, las alertas de caída de tonelaje y cualquier agregación por zona.

**Gap conocido:** el inventario completo de ejercicios mal catalogados no está cerrado. Solo se confirmó *Remo al Mentón*. CA-29.08 deja abierta la auditoría; el inventario definitivo se establecerá durante el análisis arquitectónico.

---

## Definición de Terminado (Inicial)

- [ ] Plan por defecto seed con la composición exacta de CA-29.01
- [ ] 4 ejercicios nuevos registrados con equipamiento, zona muscular y flags correctos — 37 ejercicios en total
- [ ] *Tirón de Dorsales* renombrado a *Jalón al Pecho* conservando identidad e historial
- [ ] 4 slots duales configurados; *Curl Bayesian en Banco Inclinado* como ejercicio único
- [ ] Variantes de equipamiento registradas como un solo ejercicio
- [x] Los 4 recursos PNG presentes en assets/exercises con nombres según convención (37 archivos en total) — completado 2026-08-30
- [ ] media_resource de los 4 ejercicios nuevos apuntando a esos archivos
- [ ] Ningún ejercicio eliminado del Diccionario
- [ ] *Remo al Mentón* recatalogado en Espalda Alta; auditoría del catálogo completada y registrada en `cambios.md`
- [ ] `domain_and_state_model.md` y `architecture_blueprint.md` actualizados
- [ ] Gestión del plan (crear versión, asignar, remover, alternativas) sin regresión
- [ ] Instalación fresca validada: el plan por defecto inicia con las 6 rutinas correctas
