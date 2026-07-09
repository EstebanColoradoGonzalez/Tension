# Historia de Usuario

**Como** ejecutante,
**Quiero** registrar mi perfil personal con peso corporal, altura y nivel de experiencia, y poder actualizarlo en cualquier momento,
**Para** que el sistema tenga el contexto necesario sobre mi físico y nivel de entrenamiento al calcular métricas y prescripciones.

## Descripción

Esta historia define el registro y edición del perfil del ejecutante. Al abrir la aplicación por primera vez, el ejecutante configura su perfil con peso corporal, altura y nivel de experiencia. El sistema persiste estos datos de forma atómica en la base de datos local (creando simultáneamente el primer registro de peso y el estado de rotación inicial) y los utiliza para calcular métricas y prescripciones personalizadas. El ejecutante puede actualizar su perfil en cualquier momento sin restricciones de frecuencia.

---

## Criterios de Aceptación

### CA-01.01 — Registro inicial del perfil

**Dado que** el ejecutante abre la aplicación por primera vez y no tiene perfil registrado,
**cuando** accede al formulario de registro de perfil,
**entonces** el sistema presenta campos para: peso corporal (Kg), altura (m) y nivel de experiencia, siendo todos los campos obligatorios para completar el registro.

### CA-01.02 — Opciones de nivel de experiencia

**Dado que** el ejecutante está completando el formulario de perfil,
**cuando** interactúa con el campo de nivel de experiencia,
**entonces** el sistema presenta exactamente tres opciones seleccionables: "Principiante", "Intermedio" y "Avanzado", sin permitir texto libre ni valores fuera de estas opciones.

### CA-01.03 — Teclado numérico para campos numéricos

**Dado que** el ejecutante está completando o editando el formulario de perfil,
**cuando** activa el campo de peso corporal o el campo de altura,
**entonces** el sistema despliega un teclado numérico optimizado, no el teclado alfanumérico completo.

### CA-01.04 — Validación de peso corporal

**Dado que** el ejecutante ingresa un valor de peso corporal que es negativo o igual a cero,
**cuando** intenta guardar el perfil,
**entonces** el sistema rechaza la operación y muestra un mensaje de error claro indicando que el peso corporal debe ser un valor positivo en kilogramos.

### CA-01.05 — Validación de altura

**Dado que** el ejecutante ingresa un valor de altura que es negativo o igual a cero,
**cuando** intenta guardar el perfil,
**entonces** el sistema rechaza la operación y muestra un mensaje de error claro indicando que la altura debe ser un valor positivo en metros.

### CA-01.06 — Persistencia del perfil

**Dado que** el ejecutante ha ingresado datos válidos en todos los campos del formulario de perfil,
**cuando** confirma el registro,
**entonces** el sistema persiste los datos en la base de datos local y los mantiene disponibles para consulta y uso en cálculos posteriores.

### CA-01.07 — Actualización del perfil en cualquier momento

**Dado que** el ejecutante ya tiene un perfil registrado,
**cuando** accede a la opción de editar perfil,
**entonces** el sistema muestra el formulario con los datos actuales precargados, permitiendo modificar cualquier campo (peso corporal, altura, nivel de experiencia) sin restricciones de momento o frecuencia.

### CA-01.08 — Validación en la actualización

**Dado que** el ejecutante está actualizando su perfil,
**cuando** ingresa valores fuera de los rangos permitidos en cualquier campo numérico,
**entonces** el sistema aplica las mismas validaciones que en el registro inicial, rechazando valores inválidos con mensajes de error claros antes de persistir.

### CA-01.09 — Persistencia de la actualización

**Dado que** el ejecutante ha modificado datos válidos en su perfil,
**cuando** confirma la actualización,
**entonces** el sistema reemplaza los datos anteriores con los nuevos valores, persistiéndolos en la base de datos local de forma inmediata.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante (usuario principal de la app).
- **Permisos requeridos:** Ninguno — operación local sobre la base de datos del dispositivo.
- **Valor de negocio:** Permite al sistema conocer el contexto físico y de experiencia del ejecutante para calcular métricas y prescripciones personalizadas.

### Reglas de Negocio

1. **Transacción atómica en creación:** Al crear el perfil, se insertan atómicamente `profile`, el primer `weight_record` e `rotation_state` con defaults. Si falla cualquier insert, se revierte todo. Transición: sin perfil → perfil activo.
2. **Peso no almacenado en perfil:** El peso actual se obtiene del registro más reciente en `weight_record`. Cada actualización de peso crea un nuevo `WeightRecordEntity` sin eliminar los anteriores (historial conservado — CA-02.05).
3. **Nivel de experiencia restringido:** Solo acepta `BEGINNER`, `INTERMEDIATE`, `ADVANCED` — sin texto libre ni valores fuera de estas opciones.
4. **Validaciones centralizadas en Use Cases:** Las validaciones (peso > 0, altura > 0) residen en los Use Cases, no en ViewModels ni en la capa UI.
5. **`weekly_frequency` DEFAULT = 6:** Valor definido en Modelo de Datos §3.8 como fuente autoritativa. Rango permitido: 4-6 (configurable desde J1 en HU-21 CA-21.05).
6. **`rotation_state` se inicializa al crear perfil** (Modelo de Datos §3.14): requerido por HU-05 para determinar qué módulo/versión corresponde al ejecutar una sesión. Sin esto, HU-05 fallaría al consultar qué módulo/versión toca.

### Interfaz

- **Formulario A1 — `RegisterProfileScreen`:** Pantalla de onboarding. Logo "Tension" centrado + subtítulo "Configura tu perfil". `OutlinedTextField` numérico para peso (sufijo "Kg", `keyboardType = Number`), `OutlinedTextField` numérico para altura (sufijo "m"), `RadioButton` group con 3 opciones (Principiante/Intermedio/Avanzado). Botón "Registrar" (Filled Button, habilitado solo si todos los campos son válidos). Sin Bottom Navigation. Colores según Especificación Visual §8 A1.
- **Formulario C1 — `ProfileScreen`:** Pantalla de edición con datos precargados. `CenterAlignedTopAppBar` con `←` retorno + título "Mi Perfil". Mismo estilo de campos que A1. Botón "Guardar" deshabilitado si no hay cambios (dirty state). Enlace "Ver historial de peso →" que navega a C2. Bottom Navigation visible con Configuración activo. Colores según Especificación Visual §8 C1.
- **Payload requerido:** `weightKg: Double (>0)`, `heightM: Double (>0)`, `experienceLevel: ExperienceLevel (BEGINNER|INTERMEDIATE|ADVANCED)`.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo del ejecutante.

### Preview de Interfaz

Ver `Especificación Visual §8 A1` para RegisterProfileScreen y `§8 C1` para ProfileScreen. Los mockups definen colores, tipografía y layout de cada formulario. Adicionalmente, los siguientes stubs mínimos son parte del alcance de esta historia para garantizar los flujos de navegación desde el primer momento:

- **B1 — `HomeScreen`:** Placeholder con TopAppBar "Tension", texto de bienvenida y Bottom Navigation (Inicio activo).
- **J1 — `SettingsScreen`:** Stub con TopAppBar "Configuración" y `ListItem` "Editar perfil" → navega a C1.
- **C2 — `WeightHistoryScreen`:** Stub con TopAppBar ← + "Historial de Peso" y lista vacía placeholder.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` | `docs/architecture/domain_and_state_model.md`

**Entidades afectadas:** `ProfileEntity`, `WeightRecordEntity`, `RotationStateEntity` (ver `docs/architecture/domain_and_state_model.md`)

**Interfaces de referencia:** `Formulario A1 — RegisterProfileScreen` | `Formulario C1 — ProfileScreen` (ver `docs/architecture/interfaces_contract.md`)

**Requisitos cubiertos:** RF01, RF02 | RNF03, RNF12

**Épica / Módulo:** `EPIC-01: Perfil y Catálogos`

**Prioridad:** Alta

**Historias relacionadas:**
- HU-02 (Historial de peso — C2 se crea como stub en esta historia; lógica completa en HU-02)
- HU-05 (Iniciar sesión — requiere `rotation_state` inicializado en la transacción atómica de esta historia)
- HU-18 (funcionalidad completa de B1 — se crea como placeholder aquí)
- HU-21 (Configuraciones — modifica `weekly_frequency` definido con DEFAULT 6 aquí; J1 se crea como stub)
- HU-31/32 (funcionalidad completa de J1)

**Nota arquitectónica:** HU-01 es la primera historia del proyecto. Además de los requisitos funcionales del perfil, construye la infraestructura base que todas las historias posteriores reutilizarán: configuración Gradle (Room, Hilt, KSP, Navigation Compose), estructura de 4 capas MVVM, sistema de diseño visual Tension (seed `#8B1A1A`), colores semánticos extendidos via `CompositionLocal`, y cáscaras de navegación. Es el cimiento de las 31 historias restantes.

---

## Definición de Terminado (Inicial)

- [x] Perfil registrado con peso, altura y nivel de experiencia (todos los campos obligatorios)
- [x] Teclado numérico activo al activar campos de peso y altura
- [x] Validaciones rechazan peso ≤ 0 y altura ≤ 0 con mensajes de error claros
- [x] Nivel de experiencia con exactamente 3 opciones (`RadioButton`): Principiante, Intermedio, Avanzado
- [x] Perfil editable en cualquier momento con datos actuales precargados en C1
- [x] Mismas validaciones aplicadas en actualización (CA-01.08)
- [x] Transacción atómica garantizada: profile + weight_record + rotation_state al crear perfil
- [x] 5 Use Cases con tests unitarios (18 tests pasando, cobertura 100% Use Cases)
- [x] Stubs de navegación operativos: B1 placeholder, J1 stub, C2 stub
- [x] Navegación funcional: A1 → B1 (post-registro, back stack limpio), J1 → C1, C1 → C2
- [x] Infraestructura base del proyecto operativa (Hilt, Room, Navigation Compose, Tema Tension)
- [x] `TensionDatabase` con 3 entidades, versión 1, `fallbackToDestructiveMigration()`
- [x] Colores semánticos extendidos definidos en `Color.kt` y expuestos via `CompositionLocal` en `Theme.kt`
