# Historia de Usuario

**Como** ejecutante,
**Quiero** consultar el Diccionario de Ejercicios con filtros por módulo, tipo de equipo y zona muscular, ver una imagen de la ejecución correcta de cada ejercicio, y poder crear nuevos ejercicios que se integren al catálogo,
**Para** conocer los movimientos disponibles en mi plan, verificar la técnica adecuada, personalizar mi repertorio y tomar decisiones informadas sobre sustituciones.

## Descripción

Esta historia introduce el catálogo de datos estáticos del dominio: módulos, zonas musculares, tipos de equipo, ejercicios y su relación N:M con zonas musculares. El sistema precarga 220 filas de seed data en la primera instalación usando `RoomDatabase.Callback.onCreate()` con el patrón Facade (ADR-11). Construye las vistas D1 (Diccionario con filtros y FAB), D2 (Detalle de Ejercicio con media visual), D5 (Crear Ejercicio) y los stubs D3 y F3. Es la historia con mayor volumen de entidades nuevas (7 tablas) y seed data del proyecto.

---

## Criterios de Aceptación

### CA-03.01 — Diccionario precargado y completo

**Dado que** el ejecutante abre la aplicación,
**cuando** accede al Diccionario de Ejercicios,
**entonces** el sistema muestra los 43 ejercicios precargados (y los ejercicios creados por el ejecutante, si los hubiera), cada uno clasificado por su módulo (A, B o C), tipo de equipo y zona muscular, sin requerir carga de datos externa ni conexión a internet.

### CA-03.02 — Información visible por ejercicio

**Dado que** el ejecutante consulta el Diccionario de Ejercicios,
**cuando** visualiza un ejercicio en el listado,
**entonces** el sistema muestra para cada ejercicio como mínimo: nombre del ejercicio, módulo al que pertenece (A, B o C), tipo de equipo (Máquina, Mancuerna, Barra de Pesas, Cuerpo, Polea, Pesa, Mancuernas, Mancuerna o Pesa Rusa, Máquina Multiestación) y zona muscular objetivo.

### CA-03.03 — Filtro por módulo

**Dado que** el ejecutante está consultando el Diccionario de Ejercicios,
**cuando** aplica un filtro por módulo (A, B o C),
**entonces** el sistema muestra únicamente los ejercicios que pertenecen al módulo seleccionado, excluyendo los demás.

### CA-03.04 — Filtro por tipo de equipo

**Dado que** el ejecutante está consultando el Diccionario de Ejercicios,
**cuando** aplica un filtro por tipo de equipo,
**entonces** el sistema muestra únicamente los ejercicios que utilizan el tipo de equipo seleccionado.

### CA-03.05 — Filtro por zona muscular

**Dado que** el ejecutante está consultando el Diccionario de Ejercicios,
**cuando** aplica un filtro por zona muscular,
**entonces** el sistema muestra únicamente los ejercicios que trabajan la zona muscular seleccionada.

### CA-03.06 — Combinación de filtros

**Dado que** el ejecutante está consultando el Diccionario de Ejercicios,
**cuando** aplica múltiples filtros simultáneamente (módulo, tipo de equipo y/o zona muscular),
**entonces** el sistema muestra únicamente los ejercicios que cumplen todos los filtros aplicados simultáneamente.

### CA-03.07 — Media visual por ejercicio en el Diccionario

**Dado que** el ejecutante consulta el detalle de un ejercicio en el Diccionario,
**cuando** visualiza el ejercicio,
**entonces** el sistema muestra una imagen estática (PNG 3D minimalista con fondo blanco) que ilustra la ejecución correcta del movimiento, permitiendo al ejecutante verificar la técnica adecuada. En futuras iteraciones, la imagen podrá ser reemplazada por video o animación (GIF u otro formato) sin cambios en la arquitectura.

### CA-03.08 — Media visual accesible durante sesión activa

**Dado que** el ejecutante está registrando series de un ejercicio durante una sesión activa,
**cuando** consulta la media visual del ejercicio en ejecución,
**entonces** el sistema muestra la imagen de ejecución correcta del ejercicio sin interrumpir ni cerrar la sesión activa.

### CA-03.09 — Assets multimedia optimizados

**Dado que** la aplicación incluye assets multimedia (imágenes PNG) para los 43 ejercicios precargados del Diccionario,
**cuando** se genera el APK de la aplicación,
**entonces** los assets deben estar optimizados para dispositivos móviles y el tamaño total del APK no debe exceder los 150 MB.

### CA-03.10 — Crear nuevo ejercicio

**Dado que** el ejecutante quiere agregar un ejercicio que no existe en el Diccionario,
**cuando** accede a la función de crear ejercicio desde el Diccionario (D1) tocando el FAB,
**entonces** el sistema navega a la pantalla de creación de ejercicio (D5) que presenta un formulario con: imagen del ejercicio (opcional, seleccionable desde la galería del dispositivo), nombre del ejercicio (obligatorio), módulo al que pertenece (A, B o C, obligatorio), tipo de equipo (seleccionable de los 9 tipos existentes, obligatorio), zona(s) muscular(es) objetivo (seleccionable de las 15 zonas existentes, al menos una obligatoria), condiciones especiales (peso corporal, isométrico, al fallo técnico — checkboxes opcionales). Si el ejecutante selecciona una imagen, esta se persiste en el almacenamiento interno de la app (`filesDir/exercise_images/`) y se almacena la ruta absoluta en `media_resource`. Si no selecciona imagen, `media_resource = NULL`. Al confirmar, el ejercicio se persiste con `is_custom = 1`, queda disponible en el Diccionario, en las sustituciones de sesión (HU-07) y para asignación a versiones del plan (HU-04). Se valida que la combinación (nombre, tipo de equipo) no exista previamente.

### CA-03.11 — Placeholder visual e imagen editable para ejercicios

**Dado que** el ejecutante visualiza un ejercicio que no tiene imagen asociada (ya sea un ejercicio custom recién creado o cualquier ejercicio sin `media_resource`),
**cuando** visualiza el detalle del ejercicio (D2),
**entonces** el sistema muestra el logo de la aplicación como placeholder junto con un ícono de cámara y texto "Toca para agregar imagen", indicando que puede agregar una imagen. Al tocar la zona de imagen, se abre el selector de galería del dispositivo. La imagen seleccionada se persiste en almacenamiento interno y se actualiza el campo `media_resource` del ejercicio. Esta funcionalidad de cambio de imagen está disponible para TODOS los ejercicios del diccionario (precargados y custom).

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante.
- **Permisos requeridos:** Acceso a galería del dispositivo (para CA-03.10 y CA-03.11, opcional).
- **Valor de negocio:** Permite al ejecutante conocer los movimientos disponibles, verificar la técnica mediante imágenes, personalizar su repertorio con ejercicios custom, y tomar decisiones informadas sobre sustituciones durante sesión activa.

### Reglas de Negocio

1. **Seed data atómico (ADR-11):** Los 220 registros se insertan en una única transacción `onCreate()`. Si falla cualquier insert, se revierte todo.
2. **Filtrado en memoria:** Los 43 ejercicios se cargan completos y el ViewModel filtra reactivamente. El filtro de zona muscular verifica si ALGUNA zona del ejercicio coincide (ejercicios multi-zona tienen 2 zonas). Los 3 filtros son AND.
3. **`RotationSeeder` excluido del `PrepopulateFacade`:** El estado de rotación se inicializa en `ProfileRepositoryImpl.createProfile()` (HU-01), porque es estado de usuario que solo existe cuando hay perfil (Modelo de Datos §3.14).
4. **Unicidad de ejercicio:** La combinación (nombre, `equipment_type_id`) debe ser única en la tabla `exercise`. `CreateExerciseUseCase` valida esto antes de insertar.
5. **`media_resource` dual:** Para ejercicios seed almacena el nombre normalizado (ej: `press_de_banca_maquina`). Para ejercicios custom almacena la ruta absoluta en almacenamiento interno o `null`. El composable D2 usa doble estrategia de carga: primero intenta ruta absoluta, si falla construye ruta de asset.
6. **Imagen editable para todos:** La funcionalidad de cambio de imagen (CA-03.11) aplica a todos los ejercicios (seed y custom), no solo a los custom.
7. **`ImageStorageHelper` elimina imagen anterior:** Al cambiar la imagen de un ejercicio, `deleteImageIfInternal()` limpia el archivo previo del almacenamiento interno para prevenir archivos huérfanos.
8. **Tipos de equipo son 9:** Máquina, Mancuerna, Barra de Pesas, Cuerpo, Polea, Pesa, Mancuernas, Mancuerna o Pesa Rusa, Máquina Multiestación.

### Interfaz

- **Tab Diccionario — Bottom Navigation → D1:** Pantalla D1 `ExerciseDictionaryScreen` con `TabRow` (tab "Ejercicios" activo / tab "Plan" → D3), 3 dropdowns de filtro, contador, `LazyColumn` de ejercicios, FAB Add → D5.
- **D1 ListItem click → D2:** Navega a `ExerciseDetailScreen` con argumento `exerciseId: Long`.
- **D1 FAB → D5:** Navega a `CreateExerciseScreen`.
- **D2 enlace → F3:** Navega a `ExerciseHistoryScreen` con argumento `exerciseId: Long`.
- **Payload:** `exerciseId: Long` como argumento de navegación en rutas `exercise-detail/{exerciseId}` y `exercise-history/{exerciseId}`.

### Sistemas Externos

- **Galería del dispositivo:** Accedida via `rememberLauncherForActivityResult(GetContent)` para seleccionar imagen. Solo se accede cuando el ejecutante toca activamente la zona de imagen en D2 o D5.

### Preview de Interfaz

Ver `Especificación Visual §8 D1` (lista con filtros, FAB, badge "Personalizado"), `§8 D2` (media 240dp, 4 campos informativos, enlace F3), `§7.2` (Bottom Navigation con "Diccionario"). Wireframes D1 y D2 definen layout detallado. Stubs D3 y F3 son placeholders mínimos para garantizar navegación de tabs.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` (sección `data.local.seed`) | `docs/architecture/domain_and_state_model.md` (entidades §3.1-§3.7)

**Entidades afectadas:** `ModuleEntity`, `MuscleZoneEntity`, `EquipmentTypeEntity`, `ExerciseEntity`, `ExerciseMuscleZoneEntity`, `ModuleVersionEntity`, `PlanAssignmentEntity` — 7 entidades nuevas

**Interfaces de referencia:** Tab Diccionario → D1 | D1 ListItem → D2 | D1 FAB → D5 (ver `docs/architecture/interfaces_contract.md`)

**Assets:** 43 PNGs en `app/src/main/assets/exercises/` (subdirectorios `module-a/`, `module-b/`, `module-c/`)

**Requisitos cubiertos:** RF04, RF07, RF61, RF62 | RNF24

**Épica / Módulo:** `EPIC-01: Perfil y Catálogos`

**Prioridad:** Alta

**Historias relacionadas:**
- HU-01/HU-02: Infraestructura base ya disponible
- HU-04: Consultar Plan de Entrenamiento → consume `module_version`, `plan_assignment`, D3/D4
- HU-05: Iniciar sesión → consume `module_version`, `exercise`, `plan_assignment`
- HU-06: Registrar series → consume `exercise`
- HU-07: Sustitución de ejercicios → consume `exercise` filtrado por `module_code`
- HU-08: Ejercicios especiales → consume flags `is_bodyweight`, `is_isometric`, `is_to_technical_failure`
- HU-20: Tonelaje por grupo muscular → consume `exercise_muscle_zone` + `muscle_zone.muscle_group`
- HU-23: Historial de ejercicio → consume `exercise` para F3
- HU-19: Backup/Restore → incluye todas las tablas del catálogo

---

## Definición de Terminado (Inicial)

- [x] 220 filas seed insertadas en transacción atómica (`PrepopulateCallback.onCreate()`): 3 módulos, 15 zonas musculares, 9 tipos de equipo, 43 ejercicios, 48 relaciones ejercicio-zona, 9 versiones de módulo, 93 asignaciones de plan
- [x] D1 `ExerciseDictionaryScreen`: lista completa de ejercicios con 3 filtros combinables (módulo, equipo, zona), contador, badge "Personalizado"
- [x] D2 `ExerciseDetailScreen`: media visual PNG 240dp, carga doble (ruta absoluta → asset → placeholder), imagen clickable para cambiar
- [x] D5 `CreateExerciseScreen`: formulario completo con imagen opcional, validación de unicidad (nombre, equipo)
- [x] `ImageStorageHelper` operativo: copia imagen a `filesDir/exercise_images/`, limpia imagen anterior
- [x] 3 Use Cases con tests unitarios: `GetExercisesUseCase`, `GetExerciseDetailUseCase`, `GetFilterOptionsUseCase`
- [x] Stubs D3 y F3 con navegación funcional
- [x] `BottomNavigationBar` marca Diccionario como activo en D1, D2, D3, D5, F3 (rutas con argumentos via prefijo)
- [x] `nav_dictionary` corregido de "Ejercicios" a "Diccionario" en `strings.xml`
- [x] APK ≤ 150 MB (CA-03.09, RNF24)
- [x] Build exitoso con `TensionDatabase` versión 2 y `fallbackToDestructiveMigration()`
