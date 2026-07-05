# Historia de Usuario

**Como** El Sistema en el momento de inicialización (instalación fresca),
**Quiero** prepoblar la base de datos con el nuevo plan de entrenamiento predeterminado definido en `docs/new_plan.md`,
**Para** que El Ejecutante inicie su primera sesión con un programa actualizado de 6 rutinas semanales que refleja su plan de entrenamiento vigente, sin necesidad de configurar el plan manualmente desde cero.

## Descripción

El plan de entrenamiento predeterminado actual (4 rutinas con múltiples versiones: Pierna V1, Pierna V2, Push V1, Pull V1) debe ser reemplazado por el nuevo programa de 6 días definido en `docs/new_plan.md`. El reemplazo afecta exclusivamente al seed de instalaciones frescas; no se requiere migración de datos para instalaciones existentes (la app está en fase de pruebas).

El nuevo plan tiene 6 rutinas (una por día de semana), cada una con una sola versión predeterminada. Algunos ejercicios del nuevo plan no existen aún en el catálogo seed de 43 ejercicios — solo cuentan con su asset PNG en `app/src/main/assets/exercises/`. Estos deben registrarse en `ExerciseSeeder` con sus zonas musculares y su referencia de imagen antes de ser asignados al plan.

Tres slots del nuevo plan corresponden a ejercicios genuinamente distintos que trabajan las mismas zonas musculares y se ofrecen como alternativas intercambiables en sesión (sistema dual de HU-26):
- **Miércoles**: Sentadilla Hack / Prensa Inclinada
- **Viernes**: Face Pull / Vuelos Posteriores
- **Martes**: Curl Inclinado / Bayesian Curl

Las menciones de maquinaria alternativa en el plan (e.g., "Barra o Mancuernas") indican variantes de equipamiento de un mismo ejercicio — indiferentes para el sistema — y se registran como un único ejercicio.

Los ejercicios del plan anterior que no aparecen en el nuevo plan **no se eliminan**. Permanecen en el diccionario global de ejercicios como alternativas elegibles para personalización del ejecutante.

---

## Criterios de Aceptación

### Bloque A — Catálogo de ejercicios actualizado

**CA-27.01 — Nuevos ejercicios registrados en el seeder**
```
DADO que la app se instala por primera vez
CUANDO se ejecuta el callback RoomDatabase.Callback.onCreate()
ENTONCES todos los ejercicios referenciados en docs/new_plan.md que no existían en el catálogo previo
  quedan insertados en la tabla exercise con:
  - name, equipment_type_id, is_bodyweight, is_isometric, is_to_technical_failure, is_custom = 0
  - media_resource apuntando al asset PNG correspondiente en exercises/
  Y quedan registradas sus relaciones en exercise_muscle_zone con las zonas musculares correctas
```

**CA-27.02 — Preservación del diccionario global**
```
DADO que existen ejercicios en el catálogo seed previo que no aparecen en el nuevo plan
CUANDO se ejecuta el seeder en una instalación fresca
ENTONCES dichos ejercicios siguen presentes en la tabla exercise
  Y siguen siendo elegibles como sustitutos o alternativas en cualquier rutina (RF-61, RF-62)
  Y NO se ejecuta ningún DELETE sobre la tabla exercise durante el seed
```

**CA-27.03 — Assets correctamente referenciados**
```
DADO que un ejercicio nuevo tiene su asset PNG en app/src/main/assets/exercises/
CUANDO se inserta el ejercicio en ExerciseSeeder
ENTONCES el campo media_resource contiene exactamente el nombre del archivo PNG
  sin ruta absoluta, sin extensión duplicada, coincidiendo con el asset existente en el repositorio
```

### Bloque B — Rutinas y plan predeterminado

**CA-27.04 — Seis rutinas, una versión cada una**
```
DADO que la app se instala por primera vez
CUANDO se ejecuta el seeder
ENTONCES existen exactamente 6 registros en la tabla routine con sort_order 1-6
  Y cada routine tiene exactamente 1 registro asociado en routine_version
  Y el nombre de cada rutina refleja el día y foco del nuevo plan:
    1. Push — Foco Deltoides Lateral (Lunes)
    2. Pull — Foco Dorsal Ancho (Martes)
    3. Lower — Foco Cuádriceps (Miércoles)
    4. Push — Foco Tríceps (Jueves)
    5. Pull — Foco Espalda Alta (Viernes)
    6. Lower — Foco Isquiotibiales (Sábado)
```

**CA-27.05 — Plan anterior reemplazado completamente**
```
DADO que el seeder anterior insertaba 4 rutinas (Pierna V1, Pierna V2, Push V1, Pull V1)
CUANDO se ejecuta el nuevo seeder en una instalación fresca
ENTONCES no existe ningún registro de esas 4 rutinas antiguas en routine ni en routine_version
  Y el plan predeterminado refleja exclusivamente las 6 rutinas del nuevo programa
```

**CA-27.06 — Variantes de maquinaria: ejercicio único**
```
DADO que el plan menciona ejercicios con alternativa de equipamiento
  (ej. "Press de Banca Inclinado — Barra o Mancuernas", "Peso Muerto Rumano — Mancuernas o Barra")
CUANDO se registra ese ejercicio en plan_assignment
ENTONCES se inserta UN SOLO registro por ese slot (sort_order único, sin alternativa dual)
  Y el nombre del ejercicio en exercise no incluye la mención de maquinaria como parte del nombre del ejercicio
```

**CA-27.07 — Slots duales correctamente configurados**
```
DADO que el plan incluye 3 pares de ejercicios genuinamente distintos para el mismo slot:
  - Miércoles: Sentadilla Hack / Prensa Inclinada
  - Viernes: Face Pull / Vuelos Posteriores
  - Martes: Curl Inclinado / Bayesian Curl
CUANDO se ejecuta el seeder
ENTONCES cada par comparte el mismo valor de slot en plan_assignment dentro de su routine_version
  Y ambos ejercicios del par tienen el mismo sets y reps en ese slot
  Y cada ejercicio del par tiene su propio sort_order (1 = primario, 2 = alternativa)
```

### Bloque C — Comportamiento en sesión (heredado de HU-26)

**CA-27.08 — Slot dual inicia sesión con el ejercicio primario**
```
DADO que una rutina del nuevo plan tiene un slot con dos ejercicios (slot dual)
CUANDO El Ejecutante inicia una sesión de esa rutina
ENTONCES el session_exercise se genera con el ejercicio de sort_order = 1 del slot
  Y El Ejecutante puede intercambiar al ejercicio alternativo antes de registrar su primera serie (CA-26.10)
```

**CA-27.09 — Slots simples se comportan igual que antes**
```
DADO que la mayoría de los slots del nuevo plan tienen un único ejercicio
CUANDO El Ejecutante inicia una sesión
ENTONCES esos slots generan un session_exercise sin icono de intercambio (alternativesInSlot = 1)
  Y el comportamiento es idéntico al de cualquier slot simple preexistente
```

### Bloque D — Documentación actualizada

**CA-27.10 — Blueprint arquitectónico refleja el nuevo catálogo**
```
DADO que se agregan nuevos ejercicios al catálogo seed
CUANDO se actualiza la documentación
ENTONCES docs/architecture/architecture_blueprint.md refleja el nuevo conteo total de ejercicios seed
  Y el conteo de relaciones exercise_muscle_zone se actualiza correspondientemente
```

**CA-27.11 — Modelo de dominio actualizado**
```
DADO que el conteo de datos seed cambia (ejercicios, relaciones ejercicio-zona)
CUANDO se actualiza la documentación
ENTONCES docs/architecture/domain_and_state_model.md refleja los nuevos valores de cardinalidad seed
  en los comentarios de las entidades exercise y exercise_muscle_zone
```

### Bloque E — No regresión

**CA-27.12 — Funcionalidad runtime no afectada**
```
DADO que El Ejecutante tiene la app con el nuevo plan predeterminado
CUANDO utiliza las funcionalidades de gestión del plan (D3/D4):
  - Crear nueva versión de rutina
  - Asignar o remover ejercicios de una versión
  - Agregar alternativas a un slot
ENTONCES todas esas operaciones siguen funcionando sin cambios
  Y el nuevo plan predeterminado es únicamente el punto de partida, no una restricción de runtime
```

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Sistema (agente de inicialización). El Ejecutante es el beneficiario.
- **Permisos requeridos:** Ninguno — operación interna del callback `onCreate()`.
- **Valor de negocio:** El Ejecutante inicia la app con su programa de entrenamiento vigente sin configuración manual, reduciendo la fricción del primer uso.

### Reglas de Negocio

1. **Paridad de actualización:** Los cambios deben reflejarse de forma coherente en el código (seeder) y en la documentación (blueprint, modelo de dominio).
2. **Mapeo de assets:** Los assets PNG de los nuevos ejercicios ya existen en `app/src/main/assets/exercises/`. El campo `media_resource` en `exercise` debe referenciarlos por nombre de archivo.
3. **Preservación del diccionario (soft-delete implícito):** Ningún ejercicio del catálogo existente se elimina. Los ejercicios que desaparecen del plan predeterminado permanecen disponibles como alternativas elegibles (RF-61, RF-62).
4. **Sistema dual solo para ejercicios distintos:** La alternativa dual (slot compartido per HU-26) aplica únicamente cuando son ejercicios funcionalmente distintos que trabajan las mismas zonas musculares. Las variantes de equipamiento de un mismo ejercicio se modelan como un único registro.
5. **Alcance exclusivo a instalaciones frescas:** La app está en fase de pruebas y no hay datos de producción que migrar. No se requiere `Migration` de Room.

### Interfaz

No hay cambios en la interfaz de usuario. Las pantallas D3 (Plan de Entrenamiento), D4 (Detalle de Versión de Rutina) y E1 (Sesión Activa) ya soportan la visualización del plan con slots simples y duales per HU-26.

Los cambios son exclusivamente en el seed data y la documentación.

### Sistemas Externos

Ninguno. La operación es 100% local, ejecutada durante `RoomDatabase.Callback.onCreate()` en instalación fresca.

### Preview de Interfaz

Sin cambios de UI. Ver mockups existentes de D3, D4 y E1.

---

## Contexto y Referencias

**Arquitectura:** `docs/architecture/architecture_blueprint.md` (sección 3 — `data.local.seed`, `ExerciseSeeder`), `docs/architecture/domain_and_state_model.md` (entidades `exercise`, `exercise_muscle_zone`, `routine`, `routine_version`, `plan_assignment`)

**Plan de referencia:** `docs/new_plan.md`

**Historias relacionadas:**
- HU-22 (Rutinas dinámicas — base de `routine_version` y `plan_assignment`)
- HU-24 (Actualización del Diccionario de Ejercicios — seed de 43 ejercicios actual)
- HU-26 (Alternativas por slot — sistema dual que esta historia instancia)

**Lecciones aprendidas:** El sistema dual de HU-26 distingue explícitamente entre variantes de equipamiento (un solo ejercicio) y ejercicios distintos con mismas zonas musculares (slot dual). Es crítico aplicar esta distinción durante el mapeo del nuevo plan para evitar slots duales incorrectos.

---

## Definición de Terminado (Inicial)

- [ ] Todos los ejercicios del nuevo plan presentes en `ExerciseSeeder` con zonas musculares y assets
- [ ] Ningún ejercicio del catálogo previo eliminado
- [ ] 6 rutinas seed con 1 versión cada una, reemplazando el plan de 4 rutinas anterior
- [ ] 3 slots duales configurados correctamente (Sentadilla Hack/Prensa Inclinada, Face Pull/Vuelos Posteriores, Curl Inclinado/Bayesian)
- [ ] Variantes de maquinaria registradas como un solo ejercicio (no dual)
- [ ] `docs/architecture/architecture_blueprint.md` actualizado con nuevo conteo de ejercicios y relaciones
- [ ] `docs/architecture/domain_and_state_model.md` actualizado con cardinalidad seed correcta
- [ ] Instalación fresca validada: el plan predeterminado inicia con las 6 rutinas del nuevo programa
- [ ] Funcionalidad runtime de gestión del plan no regresionada
