# Historia de Usuario

**Como** ejecutante,
**Quiero** consultar el historial de cambios de mi peso corporal con la fecha de cada actualización,
**Para** poder visualizar mi evolución de peso en el tiempo y correlacionarla con mi progresión de entrenamiento.

## Descripción

Esta historia implementa la pantalla C2 — WeightHistoryScreen, reemplazando el stub creado en HU-01 con la funcionalidad completa. El ejecutante puede ver todas las entradas de su historial de peso ordenadas de la más reciente a la más antigua. La primera entrada (registro inicial del perfil) se etiqueta visualmente como "Registro inicial". El historial es de solo lectura — el registro de nuevos pesos ya está cubierto por HU-01 (`UpdateWeightUseCase`).

---

## Criterios de Aceptación

### CA-02.01 — Registro automático en el historial al actualizar peso

**Dado que** el ejecutante actualiza su peso corporal en el perfil,
**cuando** la actualización se persiste exitosamente,
**entonces** el sistema almacena automáticamente una entrada en el historial de peso con el nuevo valor en kilogramos y la fecha de la actualización, sin requerir acción adicional del ejecutante.

### CA-02.02 — Consulta del historial de peso

**Dado que** el ejecutante tiene al menos una entrada en el historial de peso corporal,
**cuando** accede a la consulta de historial de peso,
**entonces** el sistema muestra todas las entradas registradas, cada una con la fecha de actualización y el peso en Kg, ordenadas cronológicamente de la más reciente a la más antigua.

### CA-02.03 — Visualización de la evolución temporal

**Dado que** el ejecutante tiene múltiples entradas en el historial de peso,
**cuando** consulta el historial,
**entonces** el sistema presenta la información de manera que permita identificar la tendencia del peso a lo largo del tiempo (ascendente, descendente o estable).

### CA-02.04 — Historial vacío

**Dado que** el ejecutante no ha actualizado su peso corporal desde el registro inicial del perfil,
**cuando** accede a la consulta de historial de peso,
**entonces** el sistema muestra el peso del registro inicial como única entrada, con la fecha en que se creó el perfil.

### CA-02.05 — Conservación de todas las entradas

**Dado que** el ejecutante ha actualizado su peso múltiples veces,
**cuando** consulta el historial,
**entonces** el sistema muestra todas las entradas históricas sin excepción; ninguna entrada se elimina ni se sobrescribe al registrar una nueva actualización.

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante.
- **Permisos requeridos:** Ninguno — operación de solo lectura sobre la base de datos local.
- **Valor de negocio:** Permite al ejecutante visualizar su evolución de peso y correlacionarla con su progresión de entrenamiento.

### Reglas de Negocio

1. **CA-02.01 ya está cubierto por HU-01.** El `ProfileViewModel` en C1 ya invoca `UpdateWeightUseCase` cuando el peso cambia, que inserta un nuevo `WeightRecordEntity` sin borrar anteriores. No se requiere trabajo adicional para este CA en HU-02.
2. **Historial append-only:** `weight_record` es de solo inserción (`INSERT`, nunca `UPDATE` ni `DELETE`). La conservación de todas las entradas (CA-02.05) está garantizada por diseño en `ProfileRepositoryImpl`.
3. **Registro inicial siempre existe:** La tabla `weight_record` siempre tiene al menos 1 entrada (el registro creado junto al perfil en HU-01). No hay estado vacío real.
4. **"Registro inicial" identificado por `MIN(date)`** (Modelo de Datos §3.9): en la lista ordenada DESC, es el último elemento. Se calcula en el ViewModel comparando contra el último ítem de la lista; no requiere flag en base de datos.
5. **Formato de fecha en español:** Usar `DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))` para garantizar formato consistente ("10 feb 2026") independientemente del locale del dispositivo (RNF08: UI en español).
6. **CA-02.03 (tendencia temporal)** se cumple mediante la lista cronológica ordenada. La Especificación Visual §8 C2 no define un componente gráfico de tendencia; la presentación descendente con pesos visibles permite identificarla visualmente.

### Interfaz

- **Navegación:** `C1 — ProfileScreen` → enlace "Ver historial de peso →" → `C2 — WeightHistoryScreen`. Ruta: `weight-history`.
- **Componente Visual:** `C2 — WeightHistoryScreen` reemplaza el stub creado en HU-01.
- **Top Bar:** `TensionTopAppBar` con `←` retorno + título "Historial de Peso".
- **Body:** `LazyColumn` scrollable. Cada fila es un `ListItem` M3 de 56 dp mínimo:
  - `headlineContent`: fecha formato "dd MMM yyyy" (ej: "10 feb 2026"), `Body Medium`, `On Surface`, alineada izquierda.
  - `trailingContent`: peso con sufijo "Kg" (ej: "78.5 Kg"), `Body Medium`, `On Surface`, `fontWeight = Medium`.
  - Si `isInitialRecord == true`: `supportingContent` con `Text("Registro inicial")` en `Body Small`, `On Surface Variant`, `paddingTop = 2.dp` (Especificación Visual §8 C2 y §7.7).
  - `HorizontalDivider` M3 entre filas: 1 dp, `Outline Variant`, full width.
- **Bottom Navigation:** `BottomNavigationBar` con Configuración activo. Requiere ajuste: la condición de selección del ítem Configuración debe incluir rutas hijas del flujo perfil: `currentRoute in setOf("settings", "profile", "weight-history")`.
- **Payload:** Ninguno — lectura reactiva via `Flow<List<WeightRecord>>`.

### Sistemas Externos

Ninguno. Operación 100% local sobre SQLite (Room) en el dispositivo.

### Preview de Interfaz

Ver `Especificación Visual §8 C2` y Wireframes C2 para el layout de la lista. La etiqueta "Registro inicial" se define en §7.7. No hay mockup de estado vacío ya que siempre existe al menos 1 entrada.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` | `docs/architecture/domain_and_state_model.md`

**Entidades afectadas:** `WeightRecordEntity` / `WeightRecord` (solo lectura)

**Interfaces de referencia:** `Navegación C1 → C2` — enlace "Ver historial de peso →" en `ProfileScreen` (ver `docs/architecture/interfaces_contract.md`)

**Requisitos cubiertos:** RF03

**Épica / Módulo:** `EPIC-01: Perfil y Catálogos`

**Prioridad:** Alta

**Historias relacionadas:**
- HU-01 (Infraestructura base, stub C2, `UpdateWeightUseCase`, `WeightRecordDao.getAllDescByDate()` — todo ya existe)
- HU-19 (Backup y Restauración — verifica que el historial de peso es dato incluido en backup, confirmando el diseño append-only)

---

## Definición de Terminado (Inicial)

- [x] `GetWeightHistoryUseCase` implementado con test unitario (3 tests: múltiples registros, solo registro inicial, delegación al repositorio)
- [x] `WeightHistoryViewModel` con flag `isInitialRecord` calculado correctamente (último ítem de lista DESC)
- [x] `WeightHistoryScreen` funcional: lista `LazyColumn` con fecha, peso y etiqueta "Registro inicial"
- [x] Formato de fecha "dd MMM yyyy" con `Locale("es")` explícito
- [x] `BottomNavigationBar` marca Configuración como activo en rutas `settings`, `profile` y `weight-history`
- [x] Strings `weight_history_initial_record` y `weight_history_kg_suffix` agregados a `strings.xml`
- [x] 21 tests unitarios pasando (18 HU-01 + 3 HU-02)
- [x] Build exitoso
