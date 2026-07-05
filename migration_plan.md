---
Plan de Migración Documental: docs_legacy/ → docs/

▎ Restricción crítica: Zero-Data-Loss. Cada campo, tabla y sección de los archivos origen debe ser trazado explícitamente a una sección destino antes de considerarse migrado.
▎
▎ Convenciones del plan: [ORIGEN] = ruta relativa en docs_legacy/. [DESTINO] = ruta relativa en docs/. ✔ VALIDAR: = criterio de aceptación del paso.

---
Fase 0 — Análisis de Inventario y Trazabilidad

Propósito: Establecer el mapa de trazabilidad explícita antes de escribir una sola línea en el destino. Es la red de seguridad del Zero-Data-Loss.

Paso 0.1 — Construir Matriz de Trazabilidad

┌───────────────────────────────────────────┬───────────────────────┬─────────────────────────────────────────────────┬────────────────────────────────┐
│             Artefacto Origen              │    Sección Origen     │                Artefacto Destino                │        Sección Destino         │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ business_definition/Visión del            │ Declaración de        │                                                 │ §1.1 Contexto y Entropía, §1.2 │
│ Producto.md                               │ visión, problema,     │ domain/definition/system_definition_document.md │  Propósito Central             │
│                                           │ diferenciación        │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│                                           │ Misión sistémica,     │                                                 │                                │
│ business_definition/Manifiesto de Dominio │ ecosistema,           │ domain/definition/system_definition_document.md │ §1.2, §1.3, §2.1, §4.1, §5.1   │
│  Sistémico.md                             │ fronteras,            │                                                 │                                │
│                                           │ propiedades           │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ business_definition/Requerimientos.md     │ RF01–RF65             │ domain/definition/requirements_specification.md │ §1 Capacidades Funcionales     │
│                                           │                       │                                                 │ Base                           │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ business_definition/Requerimientos.md     │ RNF01–RNF37           │ domain/definition/requirements_specification.md │ §2 Atributos de Calidad        │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ business_definition/Diccionario de        │ Tabla de ejercicios   │ domain/definition/requirements_specification.md │ §3 Glosario del Dominio        │
│ Ejercicios.md                             │ (nombre, tipo, zona)  │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ business_definition/Diccionario de        │ Tabla de ejercicios   │ architecture/domain_and_state_model.md          │ §6.1 Condiciones de            │
│ Ejercicios.md                             │ (seed data)           │                                                 │ Inicialización                 │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ business_definition/Plan de               │ Estructura de         │                                                 │ §6.1 Condiciones de            │
│ Entrenamiento.md                          │ microciclos,          │ architecture/domain_and_state_model.md          │ Inicialización                 │
│                                           │ rotación, versiones   │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│                                           │ Entidades, atributos, │                                                 │ §1 Convenciones, §2 Esquema,   │
│ architecture/Modelo de Datos.md           │  tipos, restricciones │ architecture/domain_and_state_model.md          │ §3 Relaciones, §4 Enums, §5    │
│                                           │  (v1–v13)             │                                                 │ Máquina de Estados             │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│                                           │ Stack tecnológico,    │                                                 │ §2.1 Inventario de             │
│ architecture/Arquitectura Técnica.md      │ versiones,            │ architecture/architecture_blueprint.md          │ Contenedores                   │
│                                           │ dependencias          │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ architecture/Arquitectura Técnica.md      │ Componentes MVVM,     │ architecture/architecture_blueprint.md          │ §3 Topología Lógica            │
│                                           │ ciclo de vida, estado │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ architecture/ADR.md                       │ ADR-01–ADR-11         │ architecture/architecture_blueprint.md          │ §5 Registros ADR               │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ architecture/Arquitectura Técnica.md      │ RNFs vinculados a     │ architecture/architecture_blueprint.md          │ §4 Trazabilidad Funcional      │
│                                           │ stack                 │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ architecture/Mapa de Navegación.md        │ 27 vistas, flujos,    │ architecture/interfaces_contract.md             │ §2 Catálogo de Triggers        │
│                                           │ IDs                   │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│                                           │ Layout,               │                                                 │                                │
│ architecture/Wireframes.md                │ interacciones,        │ architecture/interfaces_contract.md             │ §2 Catálogo de Triggers        │
│                                           │ estados de 27         │                                                 │ (Payload/Output)               │
│                                           │ pantallas             │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│                                           │ Paleta, tipografía,   │                                                 │ §1 Protocolos/Canales, §4      │
│ architecture/Especificación Visual.md     │ espaciado,            │ architecture/interfaces_contract.md             │ Restricciones                  │
│                                           │ restricciones Compose │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│                                           │ Distribución de       │                                                 │ §1 Módulos y Épicas, §2 Story  │
│ stories/Mapa de Historias de Usuario.md   │ epicas, inventario    │ domain/stories/story_mapping_index.md           │ Router                         │
│                                           │ RF/RNF                │                                                 │                                │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ stories/HU-01.md … HU-25.md (26 archivos, │ Narrativa, BDD,       │ domain/stories/HU-{n}.md (26 archivos,          │ Plantilla completa             │
│ incluyendo HU-15.5)                       │ requisitos vinculados │ HU-01 a HU-26, sin IDs decimales)               │ story_template.md              │
├───────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────┼────────────────────────────────┤
│ architecture/ADR.md +                     │ Convenciones de       │                                                 │ §1 Propósito, §2 Nomenclatura, │
│ architecture/Arquitectura Técnica.md      │ nomenclatura,         │ architecture/coding-standards.md                │  §3 Formato                    │
│                                           │ patrones adoptados    │                                                 │                                │
└───────────────────────────────────────────┴───────────────────────┴─────────────────────────────────────────────────┴────────────────────────────────┘

✔ VALIDAR: Toda sección de todo archivo legacy tiene al menos una fila en esta matriz. No debe existir ningún archivo origen sin un destino asignado.

---
Fase 1 — Documento de Definición del Sistema (SDD)

Destino: docs/domain/definition/system_definition_document.md

Paso 1.1 — Migrar §1: Visión y Propósito

- Acción: Extraer de business_definition/Visión del Producto.md la declaración "Para/Que necesita/Tension es", el bloque del problema y la diferenciación. Mapear al §1.1 (Contexto y Entropía) y §1.2 (Misión). Extraer de business_definition/Manifiesto de Dominio Sistémico.md las Propiedades del Sistema (claridad, detección de señales, disciplina estructural) como §1.3 Estados de Cierre.
- Origen: business_definition/Visión del Producto.md + business_definition/Manifiesto de Dominio Sistémico.md
- ✔ VALIDAR: §1.1 menciona "brecha dato-decisión" o equivalent. §1.2 contiene la misión sistémica en una sola oración. §1.3 tiene al menos 2 métricas derivadas de las propiedades del Manifiesto.

Paso 1.2 — Migrar §2: Entorno y Fronteras

- Acción: Extraer del Manifiesto las secciones "Ecosistema y fronteras" y "Exclusiones". Poblar §2.1 (In-Scope / Out-of-Scope). Extraer roles del Ejecutante y del sistema como §2.2 Agentes.
- Origen: business_definition/Manifiesto de Dominio Sistémico.md
- ✔ VALIDAR: §2.1 lista al menos un ítem In-Scope y uno Out-of-Scope. §2.2 define al menos al "Ejecutante" como agente primario.

Paso 1.3 — Migrar §3 y §4: Ontología y Dinámica

- Acción: Extraer del Manifiesto las entidades conceptuales (Sesión, Rutina, Ejercicio, Progresión) para §3.1. Mapear el ciclo cerrado de retroalimentación (dato→decisión→acción) como §4.1 Bucles de Retroalimentación.
- Origen: business_definition/Manifiesto de Dominio Sistémico.md
- ✔ VALIDAR: §3.1 define ≥4 entidades conceptuales sin términos técnicos (sin Room, sin SQLite). §4.1 describe el bucle de refuerzo del sistema.

Paso 1.4 — Migrar §5: Leyes y Restricciones

- Acción: Extraer invariantes de negocio del Manifiesto y del business_definition/Plan de Entrenamiento.md (ej. una sesión completada no se puede retrotraer). Mapear como Reglas Absolutas en §5.1. Extraer restricciones de capacidad del Plan de Entrenamiento para §5.2.
- Origen: business_definition/Manifiesto de Dominio Sistémico.md + business_definition/Plan de Entrenamiento.md
- ✔ VALIDAR: §5.1 contiene ≥2 invariantes expresadas como leyes absolutas. §5.2 contiene ≥1 restricción de capacidad medible (ej. límite de series, rangos de repeticiones).

---
Fase 2 — Especificación de Requerimientos

Destino: docs/domain/definition/requirements_specification.md

Paso 2.1 — Migrar RF01–RF65 al §1

- Acción: Copiar íntegramente los 65 Requerimientos Funcionales de la tabla origen. Adaptar el formato de | ID | Descripción | Prioridad | al formato de lista - **[RF-01] [Nombre corto]:** [descripción atómica]. Preservar el ID original.
- Origen: business_definition/Requerimientos.md (tabla RF)
- ✔ VALIDAR: Contar filas en el origen (65) y ítems en el destino (65). Verificar que el ID más alto en destino sea RF-65.

Paso 2.2 — Migrar RNF01–RNF37 al §2

- Acción: Copiar íntegramente los 37 Requerimientos No Funcionales. Incluir la categoría implícita como prefijo en el nombre corto (ej. [RNF-03] [Portabilidad - Offline]:).
- Origen: business_definition/Requerimientos.md (tabla RNF)
- ✔ VALIDAR: Contar filas en origen (37) y ítems en destino (37). Ningún RNF debe quedar sin categoría asignada.

Paso 2.3 — Construir §3: Glosario del Dominio

- Acción: Extraer cada ejercicio del business_definition/Diccionario de Ejercicios.md y convertirlo en entrada de glosario con su Tipo de equipo y Zona muscular como contexto definitorio. Añadir términos de dominio clave extraídos del Manifiesto (Ejecutante, Sesión, Rutina, Progresión, Deload, Alternativa, etc.).
- Origen: business_definition/Diccionario de Ejercicios.md + business_definition/Manifiesto de Dominio Sistémico.md
- ✔ VALIDAR: El glosario contiene ≥26 ejercicios (todos los del diccionario legacy) + ≥6 términos de dominio conceptuales. Ningún término se define en un archivo legacy pero queda ausente del glosario.

---
Fase 3 — Modelo de Dominio y Estado

Destino: docs/architecture/domain_and_state_model.md

Paso 3.1 — Migrar §1: Convenciones Base

- Acción: Extraer las convenciones de manejo de datos del architecture/Modelo de Datos.md (fechas ISO 8601, booleanos 0/1, tipos SQLite, convenciones SQL). Traducir al vocabulario de §1 (Tiempos/Fechas, Estados Lógicos, Valores de Alta Precisión).
- Origen: architecture/Modelo de Datos.md (sección de convenciones)
- ✔ VALIDAR: §1 tiene exactamente 3 convenciones base pobladas (no placeholders). Cada convención referencia el estándar técnico específico del proyecto (SQLite, Room, ISO 8601).

Paso 3.2 — Migrar §2: Esquema de Estructuras (por entidad)

- Acción: Por cada una de las 13 entidades/versiones identificadas en el Modelo de Datos legacy, crear un bloque model en la sintaxis declarativa. Preservar todos los atributos, tipos y restricciones. Usar los comentarios inline del legacy como texto de diccionario de datos.
- Origen: architecture/Modelo de Datos.md (definiciones de entidades v1–v13)
- Regla de atomicidad: Migrar una entidad por vez, verificar antes de continuar con la siguiente.
- ✔ VALIDAR: Contar entidades en origen y bloques model en destino. Cada campo del legacy tiene su equivalente en el bloque destino con comentario inline. Ningún campo se omite.

Paso 3.3 — Construir §3: Matriz de Relaciones

- Acción: Extraer todas las claves foráneas y relaciones del Modelo de Datos legacy. Construir la tabla de Cardinalidad con columnas: Entidad Origen | Cardinalidad | Entidad Destino | Verbo | Regla de Integridad.
- Origen: architecture/Modelo de Datos.md (FK, relaciones, constraints)
- ✔ VALIDAR: Cada FK identificada en el legacy tiene una fila en la tabla. La regla de integridad (Cascade/Restrict) está explícitamente poblada.

Paso 3.4 — Migrar §4: Dominios Cerrados (Enums)

- Acción: Extraer todos los valores de tipo Enum del Modelo de Datos (estados de sesión, tipos de equipo, niveles de experiencia, clasificaciones de progresión). Crear bloques enum por cada dominio cerrado.
- Origen: architecture/Modelo de Datos.md + business_definition/Diccionario de Ejercicios.md (Tipo de equipo)
- ✔ VALIDAR: Cada valor de enumeración del legacy tiene su equivalente en un bloque enum destino con su significado lógico comentado.

Paso 3.5 — Construir §5: Máquinas de Estado

- Acción: Identificar las entidades con ciclo de vida (Sesión, Rutina, PlanEntrenamiento). Para cada una, construir la tabla de transiciones con: Estado Origen | Evento/Trigger | Estado Destino | Condiciones. Extraer los estados implícitos del Modelo de Datos y del Manifiesto.
- Origen: architecture/Modelo de Datos.md + business_definition/Manifiesto de Dominio Sistémico.md
- ✔ VALIDAR: Al menos 3 entidades tienen su Máquina de Estados definida. No existe ningún estado huérfano (estado final sin transición de entrada).

Paso 3.6 — Migrar §6: Datos Semilla

- Acción: Poblar §6.1 con los datos de inicialización extraídos del business_definition/Diccionario de Ejercicios.md (26+ ejercicios precargados) y del business_definition/Plan de Entrenamiento.md (rutinas de referencia, microciclos). Poblar §6.2 con las políticas de depuración implícitas (historial de sesiones, límites de almacenamiento local).
- Origen: business_definition/Diccionario de Ejercicios.md + business_definition/Plan de Entrenamiento.md
- ✔ VALIDAR: §6.1 menciona explícitamente los 26 ejercicios seed. §6.2 tiene al menos 1 política de depuración derivada del contexto offline del sistema.

---
Fase 4 — Contrato de Interfaces

Destino: docs/architecture/interfaces_contract.md

Paso 4.1 — Poblar §1: Protocolos y Canales

- Acción: Extraer de architecture/Especificación Visual.md el canal principal (sistema de input Android + Material 3), el formato de intercambio (UI Events de Jetpack Compose) y las restricciones de autenticación (ninguna, app local).
- Origen: architecture/Especificación Visual.md + architecture/Arquitectura Técnica.md
- ✔ VALIDAR: §1 tiene los 3 campos poblados: Canal Principal, Formato de Intercambio Base, Autenticación/Autorización.

Paso 4.2 — Migrar §2: Catálogo de Triggers (por módulo, una vista a la vez)

- Acción: Por cada una de las 27 vistas del architecture/Mapa de Navegación.md, crear una subsección en §2 con el ID y nombre de la vista. Cruzar con architecture/Wireframes.md para poblar: Tipo de Trigger (interacción UI), Descripción, Payload (inputs del usuario), Respuesta esperada (output de la vista). Procesar un módulo de navegación por vez (flujo A, B, C...).
- Origen: architecture/Mapa de Navegación.md + architecture/Wireframes.md
- Regla de atomicidad: Un flujo de navegación por paso.
- ✔ VALIDAR: Contar vistas en Mapa de Navegación (27) y subsecciones en §2 (27). Cada sección tiene al menos Tipo de Trigger y Descripción poblados. Sin placeholders [...] vacíos.

Paso 4.3 — Construir §3: Manejo de Errores

- Acción: Derivar los códigos de error del contexto (Room exceptions, errores de validación de input, estados de Sesión inválidos). Construir la tabla §3.2 con al menos 4 tipos de error (Validación, Not Found, Estado Inválido, IO Error).
- Origen: architecture/Arquitectura Técnica.md + architecture/Modelo de Datos.md (constraints)
- ✔ VALIDAR: §3.1 tiene el esquema estándar de error poblado. §3.2 tiene ≥4 filas en la tabla de códigos.

Paso 4.4 — Poblar §4: Restricciones de Interfaz

- Acción: Extraer de architecture/Especificación Visual.md las restricciones técnicas (orientación vertical fija, tamaños mínimos de target interactivo, restricciones de Material 3) y mapearlas a §4 (tasa máxima de interacciones, timeouts).
- Origen: architecture/Especificación Visual.md
- ✔ VALIDAR: §4 tiene los 3 campos poblados: Tasa Máxima, Penalización, Timeout.

---
Fase 5 — Blueprint de Arquitectura

Destino: docs/architecture/architecture_blueprint.md

Paso 5.1 — Migrar §1: Contexto y Fronteras

- Acción: Mapear los actores del Manifiesto y la Visión del Producto a §1.1 (Ejecutante como actor primario). Extraer dependencias del architecture/Arquitectura Técnica.md (Android OS, Room, Compose) para §1.2 Sistemas Externos. Derivar exclusiones explícitas (sin backend, sin autenticación remota) para §1.3.
- Origen: business_definition/Manifiesto de Dominio Sistémico.md + architecture/Arquitectura Técnica.md
- ✔ VALIDAR: §1.1 define ≥1 actor. §1.2 lista ≥3 dependencias externas. §1.3 tiene ≥1 exclusión explícita con justificación.

Paso 5.2 — Migrar §2: Stack Tecnológico

- Acción: Extraer la tabla de stack del architecture/Arquitectura Técnica.md (Kotlin 2.0.21, Android API 26-35, Compose, Material 3, Room, Hilt, etc.) y crear un bloque Contenedor por cada capa lógica (Presentación, Dominio, Datos, Persistencia). Incluir versiones exactas.
- Origen: architecture/Arquitectura Técnica.md
- ✔ VALIDAR: Cada tecnología listada en el legacy tiene su contenedor o mención en §2.1. Ninguna versión se omite o generaliza.

Paso 5.3 — Migrar §3: Topología Lógica (MVVM)

- Acción: Extraer la estructura MVVM del architecture/Arquitectura Técnica.md (ViewModel, Repository, DAO, UseCase). Por cada capa, crear un bloque de Módulo/Componente con Responsabilidad, Interfaces Expuestas y Dependencias Internas.
- Origen: architecture/Arquitectura Técnica.md
- ✔ VALIDAR: §3 tiene ≥4 bloques de componentes (ViewModel, Repository, DAO, Composable). Cada bloque tiene los 4 campos requeridos (Responsabilidad, Interfaces, Dependencias, Estructuras Atómicas).

Paso 5.4 — Construir §4: Trazabilidad Funcional

- Acción: Cruzar los RF del legacy con la arquitectura MVVM. Crear ≥5 entradas de trazabilidad que mapeen [ID RF] → [Componente responsable].
- Origen: business_definition/Requerimientos.md + architecture/Arquitectura Técnica.md
- ✔ VALIDAR: §4 tiene al menos 5 ítems de trazabilidad. Ningún RF crítico (alta prioridad) queda sin componente asignado.

Paso 5.5 — Migrar §5: ADRs (uno por uno)

- Acción: Por cada uno de los 11 ADRs del architecture/ADR.md, crear un bloque ADR-00N en §5 con: Contexto (motivación), Decisión (elección técnica), Consecuencias (trade-offs). Preservar el ID y título originales.
- Origen: architecture/ADR.md
- Regla de atomicidad: Un ADR por paso.
- ✔ VALIDAR: Contar ADRs en origen (11) y bloques en §5 (11). Cada ADR tiene los 3 campos completos. Los ADRs con estado "Sustituida" incluyen nota de sustitución.

---
Fase 6 — Mapa de Historias

Destino: docs/domain/stories/story_mapping_index.md

Paso 6.0 — Tabla Canónica de Migración de IDs

- Acción: Antes de construir cualquier índice, establecer la tabla de migración de IDs como referencia vinculante para todas las fases posteriores. Todo artefacto que referencie un ID legacy debe actualizarlo al ID destino usando esta tabla.
- Origen: `stories/HU-01.md … HU-25.md` (listado de archivos)

| ID Legacy | ID Destino | Acción |
|-----------|------------|--------|
| HU-01 | HU-01 | Preservar |
| HU-02 | HU-02 | Preservar |
| HU-03 | HU-03 | Preservar |
| HU-04 | HU-04 | Preservar |
| HU-05 | HU-05 | Preservar |
| HU-06 | HU-06 | Preservar |
| HU-07 | HU-07 | Preservar |
| HU-08 | HU-08 | Preservar |
| HU-09 | HU-09 | Preservar |
| HU-10 | HU-10 | Preservar |
| HU-11 | HU-11 | Preservar |
| HU-12 | HU-12 | Preservar |
| HU-13 | HU-13 | Preservar |
| HU-14 | HU-14 | Preservar |
| HU-15 | HU-15 | Preservar |
| HU-15.5 | HU-16 | **Renombrar** — historia intermedia promovida a posición canónica |
| HU-16 | HU-17 | **Renombrar** — desplazada por absorción de HU-15.5 |
| HU-17 | HU-18 | **Renombrar** |
| HU-18 | HU-19 | **Renombrar** |
| HU-19 | HU-20 | **Renombrar** |
| HU-20 | HU-21 | **Renombrar** |
| HU-21 | HU-22 | **Renombrar** |
| HU-22 | HU-23 | **Renombrar** |
| HU-23 | HU-24 | **Renombrar** |
| HU-24 | HU-25 | **Renombrar** |
| HU-25 | HU-26 | **Renombrar** |

- ✔ VALIDAR: La tabla tiene exactamente 26 filas de entrada (26 archivos legacy) y 26 IDs destino únicos y secuenciales (HU-01 a HU-26). No existen IDs con notación decimal en la columna destino.

Paso 6.1 — Migrar §1: Módulos y Épicas

- Acción: Extraer la estructura de épicas del `stories/Mapa de Historias de Usuario.md`. Organizar las 26 historias por módulo funcional (Perfil, Diccionario, Plan, Sesión, Progresión, Análisis). Crear la jerarquía §1: Épica → Lista de IDs de HU. **Aplicar la tabla de migración del Paso 6.0:** todos los IDs en el índice deben usar los IDs destino (ej. donde el legacy dice HU-15.5 escribir HU-16; donde dice HU-16 escribir HU-17, etc.).
- Origen: `stories/Mapa de Historias de Usuario.md`
- ✔ VALIDAR: §1 cubre las 26 historias distribuidas en épicas usando IDs destino. Ningún ID decimal (X.Y) aparece en el documento. Ninguna HU queda sin épica asignada.

Paso 6.2 — Construir §2: Story Router

- Acción: Crear la tabla de índice plano con: ID Destino | Título | ID Legacy (referencia) | Épica | Estado | Prioridad. La columna "ID Legacy" es una referencia interna para trazabilidad durante la migración, no para uso permanente. Extraer metadatos del encabezado de cada archivo HU individual y aplicar la tabla de migración del Paso 6.0 para asignar el ID destino correcto.
- Origen: `stories/HU-01.md … HU-25.md` (encabezados de cada archivo)
- ✔ VALIDAR: La tabla tiene exactamente 26 filas con IDs HU-01 a HU-26, sin saltos ni IDs decimales. El Estado y Prioridad están poblados para cada fila.

---
Fase 7 — Historias de Usuario Individuales

Destino: docs/domain/stories/HU-{destino}.md para cada historia

Regla de atomicidad: Una historia por vez, en orden numérico del ID legacy.
Regla de renombramiento: El nombre del archivo destino y el campo ID del §1 Metadatos siempre deben usar el ID destino definido en la tabla del Paso 6.0, nunca el ID legacy.

Paso 7.N — Migrar HU-{legacy} → HU-{destino}

(Ejecutar en orden estricto usando la siguiente tabla de secuencia):

| Paso  | Leer origen (legacy)    | Crear destino                                         |
|-------|-------------------------|-------------------------------------------------------|
| 7.01  | stories/HU-01.md        | docs/domain/stories/HU-01.md                          |
| 7.02  | stories/HU-02.md        | docs/domain/stories/HU-02.md                          |
| 7.03  | stories/HU-03.md        | docs/domain/stories/HU-03.md                          |
| 7.04  | stories/HU-04.md        | docs/domain/stories/HU-04.md                          |
| 7.05  | stories/HU-05.md        | docs/domain/stories/HU-05.md                          |
| 7.06  | stories/HU-06.md        | docs/domain/stories/HU-06.md                          |
| 7.07  | stories/HU-07.md        | docs/domain/stories/HU-07.md                          |
| 7.08  | stories/HU-08.md        | docs/domain/stories/HU-08.md                          |
| 7.09  | stories/HU-09.md        | docs/domain/stories/HU-09.md                          |
| 7.10  | stories/HU-10.md        | docs/domain/stories/HU-10.md                          |
| 7.11  | stories/HU-11.md        | docs/domain/stories/HU-11.md                          |
| 7.12  | stories/HU-12.md        | docs/domain/stories/HU-12.md                          |
| 7.13  | stories/HU-13.md        | docs/domain/stories/HU-13.md                          |
| 7.14  | stories/HU-14.md        | docs/domain/stories/HU-14.md                          |
| 7.15  | stories/HU-15.md        | docs/domain/stories/HU-15.md                          |
| 7.16  | stories/HU-15.5.md      | docs/domain/stories/HU-16.md ← Renombrar        |
| 7.17  | stories/HU-16.md        | docs/domain/stories/HU-17.md ← Renombrar        |
| 7.18  | stories/HU-17.md        | docs/domain/stories/HU-18.md ← Renombrar        |
| 7.19  | stories/HU-18.md        | docs/domain/stories/HU-19.md ← Renombrar        |
| 7.20  | stories/HU-19.md        | docs/domain/stories/HU-20.md ← Renombrar        |
| 7.21  | stories/HU-20.md        | docs/domain/stories/HU-21.md ← Renombrar        |
| 7.22  | stories/HU-21.md        | docs/domain/stories/HU-22.md ← Renombrar        |
| 7.23  | stories/HU-22.md        | docs/domain/stories/HU-23.md ← Renombrar        |
| 7.24  | stories/HU-23.md        | docs/domain/stories/HU-24.md ← Renombrar        |
| 7.25  | stories/HU-24.md        | docs/domain/stories/HU-25.md ← Renombrar        |
| 7.26  | stories/HU-25.md        | docs/domain/stories/HU-26.md ← Renombrar        |

Para cada paso 7.N aplicar la siguiente acción:

- Acción: Crear el archivo destino indicado en la tabla aplicando story_template.md. Mapear:
  - §1 Metadatos: ID: usar el ID destino de la tabla (no el ID del archivo legacy). Épica: derivar del story_mapping_index.md (Paso 6). Estado: preservar del legacy. Prioridad: preservar del legacy. Agente Asignado: Developer.
  - §2 Narrativa: Extraer la narrativa "Como/Quiero/Para" del archivo legacy. Si no existe explícitamente, derivar de la descripción funcional del legacy.
  - §3 Criterios BDD: Preservar íntegramente cada bloque Dado/Cuando/Entonces. No suprimir ni resumir ningún criterio de aceptación.
  - §4 Dependencias Técnicas: Cruzar los IDs de RF vinculados en el legacy con el Modelo de Dominio (§4.1) y el Contrato de Interfaces (§4.2). Si alguna referencia interna apunta a otra HU, actualizar ese ID referenciado usando la tabla del Paso 6.0.
- Origen: archivo legacy indicado en la columna "Leer origen" de la tabla
- ✔ VALIDAR por historia:
  a. El nombre del archivo destino coincide con el ID destino de la tabla (ej. HU-16.md para el contenido de HU-15.5.md).
  b. El campo ID en §1 Metadatos del documento destino usa el ID destino, no el legacy.
  c. El conteo de criterios de aceptación en origen = conteo de escenarios BDD en destino.
  d. Todos los IDs de RF referenciados en el legacy aparecen en §4.
  e. Cualquier referencia cruzada a otras HU dentro del documento usa el ID destino actualizado.
  f. No existe ningún placeholder [...] sin poblar.

---
Fase 8 — Estándares de Código

Destino: docs/architecture/coding-standards.md

Paso 8.1 — Extraer Convenciones de Nomenclatura

- Acción: Extraer del architecture/Arquitectura Técnica.md y del architecture/ADR.md las convenciones explícitas de nomenclatura (estructura de paquetes, prefijos de clases Compose, convenciones SQL snake_case, nomenclatura de entidades Room). Poblar §2 del destino.
- Origen: architecture/Arquitectura Técnica.md + architecture/ADR.md
- ✔ VALIDAR: §2 tiene ≥3 reglas de nomenclatura derivadas directamente del ADR o del stack adoptado. Ninguna regla es genérica o de placeholder.

Paso 8.2 — Extraer Estándares de Formato y Sintaxis

- Acción: Extraer del architecture/ADR.md las decisiones que impactan formato de código (ADRs de estructura de paquetes, uso de StateFlow vs LiveData, Hilt sobre Koin). Traducir cada ADR en una regla de estándar de código con ejemplo positivo y negativo.
- Origen: architecture/ADR.md (ADR-04, ADR-06, ADR-07, ADR-08, ADR-09 relevantes)
- ✔ VALIDAR: Cada ADR de tipo "convención de código" tiene su equivalente en §3 como regla accionable. Los ADRs con estado "Sustituida" generan una regla de "prohibición explícita" del patrón abandonado.

Paso 8.3 — Integrar Convenciones Visuales/Compose

- Acción: Extraer de architecture/Especificación Visual.md las restricciones de componentes (tamaños mínimos, tema claro/oscuro, iconografía Material 3) y convertirlas en estándares de código Compose obligatorios.
- Origen: architecture/Especificación Visual.md
- ✔ VALIDAR: §3 o §4 del destino contiene ≥2 estándares específicos de Jetpack Compose derivados de la especificación visual legacy.

---
Resumen de Conteo y Trazabilidad Final

┌──────┬───────────────────────────────────┬──────────────────────────────────────────────────────────┬────────┐
│ Fase │         Artefacto Destino         │                     Archivos Origen                      │ Estado │
├──────┼───────────────────────────────────┼──────────────────────────────────────────────────────────┼────────┤
│ 1    │ system_definition_document.md     │ Visión del Producto + Manifiesto                         │ ☐      │
├──────┼───────────────────────────────────┼──────────────────────────────────────────────────────────┼────────┤
│ 2    │ requirements_specification.md     │ Requerimientos + Diccionario de Ejercicios + Manifiesto  │ ☐      │
├──────┼───────────────────────────────────┼──────────────────────────────────────────────────────────┼────────┤
│ 3    │ domain_and_state_model.md         │ Modelo de Datos + Diccionario + Plan de Entrenamiento    │ ☐      │
├──────┼───────────────────────────────────┼──────────────────────────────────────────────────────────┼────────┤
│ 4    │ interfaces_contract.md            │ Mapa de Navegación + Wireframes + Especificación Visual  │ ☐      │
├──────┼───────────────────────────────────┼──────────────────────────────────────────────────────────┼────────┤
│ 5    │ architecture_blueprint.md         │ Arquitectura Técnica + ADR + Manifiesto + Requerimientos │ ☐      │
├──────┼───────────────────────────────────┼──────────────────────────────────────────────────────────┼────────┤
│ 6    │ story_mapping_index.md            │ Mapa de Historias + HU-01…HU-25 (26 archivos legacy)     │ ☐      │
├──────┼───────────────────────────────────┼──────────────────────────────────────────────────────────┼────────┤
│ 7    │ HU-01.md … HU-26.md (26 archivos) │ HU-01…HU-25 + HU-15.5 individuales (renombrados)         │ ☐      │
├──────┼───────────────────────────────────┼──────────────────────────────────────────────────────────┼────────┤
│ 8    │ coding-standards.md               │ Arquitectura Técnica + ADR + Especificación Visual       │ ☐      │
└──────┴───────────────────────────────────┴──────────────────────────────────────────────────────────┴────────┘

▎ Criterio de cierre del plan: El plan se considera ejecutado cuando la suma de checklist ☐ → ✔ es 8/8, y ningún archivo en docs_legacy/ contiene información que no tenga trazabilidad explícita a una sección de docs/.