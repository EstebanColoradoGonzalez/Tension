# Historia de Usuario

**Como** El Ejecutante,
**Quiero** que la aplicación deje de pedirme un nivel de experiencia que no sirve para nada y que me muestre mi peso corporal en Métricas, con un registro rápido para mantenerlo al día,
**Para** que mi perfil solo contenga datos útiles y pueda leer mi peso en el contexto de mi entrenamiento.

> **Historia resultante de partición.** Es la **primera de dos** historias en que se dividió la historia original *Eliminar el nivel de experiencia y usar el peso corporal en las métricas* (17 CAs). Su hermana [`HU-49`](../HU-49-carga-real-peso-corporal/historia.md) añade la carga real del peso corporal al tonelaje y al 1RM, y la fuerza relativa. **Las dos son independientes y pueden implementarse en cualquier orden.**

## Descripción

Los datos que pide el perfil hoy **no participan en el funcionamiento de la aplicación**.

**1. El nivel de experiencia se elimina definitivamente.** Hoy el registro inicial pide peso, altura y nivel de experiencia (Principiante / Intermedio / Avanzado), y el perfil permite editarlo. Pero ninguna regla, cálculo, métrica ni alerta lo lee: elegir cualquiera de los tres niveles produce exactamente la misma aplicación. Además, el nivel no evoluciona, porque es un dato declarado a mano. Es un campo que pide información, no la usa y confunde. Esta historia lo retira del registro inicial, del perfil y del modelo de datos, sin dejar rastro.

**2. El peso corporal y la altura aparecen en Métricas como complemento.** Hoy el peso solo se registra y se consulta en su historial (`HU-02`), y la altura no se usa en ninguna parte. Esta historia añade una sección **"Peso corporal"** al panel de Métricas con:

- el peso actual y su variación en el período;
- su evolución en el mismo eje temporal que el tonelaje, para leer uno en contexto del otro;
- el IMC como **dato secundario y sin categorías**;
- un **registro rápido de peso**, para que el dato se mantenga al día sin pasar por la edición del perfil.

El peso y la altura se usan **como complemento, nunca como métrica en sí misma ni como juicio sobre el cuerpo**.

### Frontera de alcance

- **El IMC no es una métrica ni un diagnóstico.** Se muestra como número de contexto junto al peso, sin categorías de la OMS (normal, sobrepeso…) ni mensajes de valoración. En hipertrofia, ganar músculo sube el IMC, y etiquetarlo como "sobrepeso" contradiría el propósito del sistema. Se mantiene así la frontera del sistema, que deja fuera el seguimiento de composición corporal.
- **El tonelaje del gráfico es el tonelaje del sistema.** Esta historia no cambia cómo se calcula el tonelaje. Cuando se implemente `HU-49`, el mismo gráfico mostrará la carga real sin cambios adicionales.
- **El peso corporal no alimenta ninguna decisión.** La progresión, las señales de subir o bajar peso, la meseta, la regresión, la fatiga, la descarga y las alertas no leen nada de esta historia.
- **Sin migración.** La aplicación está en beta y se valida sobre instalación fresca (ADR-019). Los respaldos se generan con la estructura resultante de esta historia, así que no se contempla compatibilidad con respaldos anteriores.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

#### CA-48.01 — El registro inicial ya no pide nivel de experiencia

- **Dado** que El Ejecutante abre la aplicación por primera vez
- **Cuando** se muestra el formulario de configuración del perfil
- **Entonces** el formulario pide **únicamente** peso corporal (Kg) y altura (m)
- **Y** no existe ningún campo, selector u opción de nivel de experiencia
- **Y** el botón **"Registrar"** se habilita en cuanto peso y altura son válidos
- **Y** al registrar, el perfil queda creado con peso y altura, y el peso inicial queda como primera entrada del historial de peso, igual que hoy

#### CA-48.02 — El perfil ya no muestra ni edita el nivel de experiencia

- **Dado** que El Ejecutante tiene su perfil registrado
- **Cuando** abre la edición de su perfil
- **Entonces** puede editar peso corporal y altura, como hoy
- **Y** no existe ningún campo de nivel de experiencia
- **Y** el nivel no aparece en ninguna otra pantalla de la aplicación (Ajustes, Métricas, Inicio, respaldo)

#### CA-48.03 — Sección "Peso corporal" en Métricas

- **Dado** que El Ejecutante abre el panel de Métricas
- **Cuando** visualiza la sección **"Peso corporal"**
- **Entonces** la sección muestra:
  - el **peso actual** (último registrado) en Kg y la **fecha** de ese registro;
  - la **variación del peso** en el período seleccionado (4, 8 o 12 semanas, con el mismo selector de período del resto de indicadores), en Kg y con signo;
  - un **gráfico** con la evolución del peso corporal y la del tonelaje en el **mismo eje temporal** y el mismo período, para leer uno en contexto del otro: el peso con **un punto por cada registro** y el tonelaje con **un punto por semana** (semana ISO, de lunes a domingo, como en `HU-15`), que suma el tonelaje de todas las sesiones de esa semana;
  - el **IMC** como dato secundario, junto al peso, con un decimal;
- **Y** cada dato muestra su etiqueta, su unidad y una descripción breve, siguiendo la convención de `HU-35`
- **Y** el IMC se muestra **sin categoría** ni mensaje de valoración, con una descripción que aclara que es un dato de contexto que no distingue músculo de grasa

#### CA-48.04 — Registro rápido de peso desde Métricas

- **Dado** que El Ejecutante está en la sección "Peso corporal" de Métricas
- **Cuando** toca **"Registrar peso"**
- **Entonces** se abre un diálogo con un único campo numérico de peso en **Kg**, precargado con el último peso registrado
- **Y** al confirmar con un valor válido, el peso se guarda como nueva entrada del **historial de peso** (`HU-02`) con la fecha del día, igual que al actualizar el peso desde el perfil
- **Y** el peso actual del perfil pasa a ser ese valor
- **Y** la sección "Peso corporal" se actualiza de inmediato: peso actual, fecha, variación, gráfico e IMC

### Escenario 2: Validaciones

#### CA-48.05 — Validación del registro rápido de peso

- **Dado** que El Ejecutante está en el diálogo de registro rápido de peso
- **Cuando** introduce un valor
- **Entonces** el sistema aplica **exactamente las mismas validaciones** que el peso del perfil: valor numérico mayor que 0, con el mismo mensaje de error en línea
- **Y** el teclado desplegado es numérico
- **Y** el botón de confirmar queda deshabilitado mientras el valor sea inválido
- **Y** si El Ejecutante cancela, no se crea ninguna entrada y el historial de peso no cambia

### Escenario 3: Casos Extremos

#### CA-48.06 — Peso desactualizado

- **Dado** que el último peso registrado tiene **más de 30 días**
- **Cuando** El Ejecutante abre la sección "Peso corporal" de Métricas
- **Entonces** la sección muestra una nota discreta: **"Tu último peso es de hace N días"**, con acceso directo al registro rápido
- **Y** la nota **no es una alerta** del sistema: no aparece en el Centro de Alertas ni genera notificación
- **Y** la nota desaparece en cuanto se registra un peso nuevo

#### CA-48.07 — Datos insuficientes

- **Dado** que el período seleccionado no tiene datos suficientes
- **Cuando** El Ejecutante consulta la sección "Peso corporal"
- **Entonces** con **una sola entrada** de peso en el período, o ninguna anterior que sirva de referencia, la variación se muestra como **"Sin datos suficientes"**, no como 0 Kg
- **Y** si no hay sesiones en el período, el gráfico muestra la línea de peso y comunica que no hay tonelaje en ese período, sin dibujar ceros engañosos (`HU-35`)

#### CA-48.08 — Sin rastro del nivel de experiencia

- **Dado** que la aplicación se instala desde cero con esta historia
- **Cuando** El Ejecutante exporta un respaldo y lo importa en otro dispositivo
- **Entonces** el respaldo no contiene el nivel de experiencia
- **Y** el perfil (peso y altura) y el historial de peso, incluidas las entradas creadas desde Métricas, se restauran íntegros

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Ejecutante, actor único del sistema. Aplicación de un solo usuario y sin roles.
- **Permisos requeridos:** ninguno. Operación 100 % local en el dispositivo.
- **Valor de negocio:** el registro inicial deja de pedir un dato que no sirve, lo que elimina una fuente de confusión. Los datos que sí se piden (peso y altura) empiezan a aportar: el peso se lee junto al tonelaje, y su registro se vuelve tan rápido que el historial se mantiene al día. Ese historial es la base de la carga real y la fuerza relativa de `HU-49`.

### Reglas de Negocio

1. **El nivel de experiencia deja de existir.** No se pide, no se guarda, no se muestra, no se exporta.
2. **El perfil queda en peso corporal y altura**, los dos obligatorios y mayores que 0, como hoy.
3. **Peso actual:** la última entrada del historial de peso.
4. **Variación del período:** peso actual menos el peso vigente al inicio del período seleccionado (la entrada más reciente con fecha igual o anterior al inicio). Sin esa referencia, o con una sola entrada en el período, es "Sin datos suficientes".
5. **IMC** = peso actual ÷ altura². Se muestra con un decimal, como dato secundario, **sin categoría ni valoración**. No alimenta ninguna decisión ni alerta.
6. **Registro rápido de peso:** en Kg, mismo criterio que el peso del perfil (mayor que 0), fecha del día. Crea una entrada del historial de peso y actualiza el peso actual, igual que actualizarlo desde el perfil.
7. **Aviso de peso desactualizado:** nota informativa en la sección "Peso corporal" si el último peso tiene más de 30 días. No es una alerta del sistema.
8. **El gráfico usa el tonelaje del sistema tal como se calcule en cada momento.** Esta historia no cambia su cálculo. **Granularidad:** el peso se dibuja con un punto por cada entrada del historial dentro del período; el tonelaje, con un punto por semana ISO (lunes a domingo) que suma todas las sesiones de esa semana. Una semana sin sesiones no tiene punto de tonelaje (no se dibuja como cero), y la semana en curso suma las sesiones registradas hasta hoy.
9. **El motor de decisión no cambia:** nada de esta historia alimenta progresión, señales, meseta, regresión, fatiga, descarga ni alertas.

### Interfaz

| Punto | Ubicación | Naturaleza del cambio |
|---|---|---|
| Registro inicial | `A1` | Se retira el selector de nivel de experiencia. Quedan peso y altura |
| Perfil | `C1` | Se retira el selector de nivel de experiencia. Quedan peso y altura |
| Panel de Métricas | `G1` | Nueva sección **"Peso corporal"**: peso actual, fecha, variación del período, IMC secundario, gráfico de peso y tonelaje, botón **"Registrar peso"** y nota de peso desactualizado |
| Diálogo de registro rápido | `G1` | Nuevo diálogo con un único campo de peso en Kg |
| Historial de peso | `C2` | Sin cambios de diseño. Muestra también las entradas creadas desde Métricas |

#### Detalle de Interfaz de Usuario

- **Diseño general:** no hay pantallas nuevas. Se retira un campo de dos formularios (A1, C1). Se añaden una sección y un diálogo a Métricas (G1).
- **Campos y controles:**
  - **A1 y C1:** campos de peso (Kg) y altura (m). Sin selector de nivel.
  - **Sección "Peso corporal" (G1):**
    - indicador principal con el peso actual (Kg) y su fecha;
    - indicador de variación del período (± Kg);
    - IMC como texto secundario bajo el peso, con su descripción breve;
    - gráfico de peso (un punto por registro) y tonelaje (un punto por semana) en el mismo eje temporal, con leyenda que distingue las dos series y sus unidades;
    - botón **"Registrar peso"**;
    - nota de peso desactualizado, cuando aplica.
  - **Diálogo "Registrar peso":** campo numérico en Kg precargado con el último peso, botones **"Guardar"** (habilitado solo con valor válido) y **"Cancelar"**.
- **Flujo de navegación visual:** `Métricas → sección Peso corporal → Registrar peso → diálogo → Guardar (se actualiza la sección) | Cancelar`.
- **Mensajes y feedback:**
  - error en línea de peso inválido, el mismo del perfil;
  - confirmación breve al guardar el peso;
  - nota **"Tu último peso es de hace N días"**;
  - **"Sin datos suficientes"** en la variación del período;
  - descripción del IMC que aclara que es un dato de contexto, sin valoración.

### Sistemas Externos

Ninguno. El sistema es local y sin integraciones.

### Disparadores

- El Ejecutante pulsa "Registrar" en la configuración inicial del perfil (ya sin nivel de experiencia).
- El Ejecutante guarda la edición de su perfil (ya sin nivel de experiencia).
- El Ejecutante abre el panel de Métricas o cambia el período seleccionado. Se puede repetir las veces que quiera.
- El Ejecutante pulsa "Registrar peso" en Métricas y guarda el diálogo. Se puede repetir; cada guardado crea una entrada nueva.
- El Ejecutante cancela el diálogo de registro rápido.
- El Ejecutante exporta o importa un respaldo.

### Entidades del Dominio

- Perfil del Ejecutante (pierde el nivel de experiencia; conserva peso actual y altura)
- Registro de Peso Corporal (historial de peso con fecha)
- Respaldo (pierde el nivel de experiencia)

### Alcance y Continuidad

- **Esta funcionalidad modifica procesos que ya existen; no crea uno nuevo desde cero.** Retira el nivel de experiencia del registro y de la edición del perfil (`HU-01`). Lleva el peso corporal, que hoy solo se registra y se consulta (`HU-01`, `HU-02`), al panel de Métricas (`HU-15`, `HU-35`) como complemento del tonelaje. La altura, que hoy no se usa, pasa a alimentar el IMC como dato secundario.
- **El registro rápido de peso no es un registro nuevo:** alimenta el mismo historial de peso de `HU-02`, igual que actualizar el peso desde el perfil.
- **Frontera:** todo ocurre dentro de la aplicación, sin sistemas externos ni conexión. Se respeta la exclusión del seguimiento de composición corporal: el IMC es un dato de contexto sin valoración, y no se registra ni calcula grasa ni masa muscular.

### Preview de Interfaz

**Preview:** [`48.preview.txt`](./48.preview.txt) | **Formato:** ASCII (wireframe de texto)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/domain/definition/requirements_specification.md`: RF-01 (registro de perfil: retira el nivel de experiencia).
- `docs/domain/definition/system_definition_document.md`: §3.1, entidad Ejecutante (retira el nivel de experiencia de sus atributos); §2.1, exclusión de la composición corporal (se respeta).
- `docs/architecture/domain_and_state_model.md`: tabla `profile` y enum `ExperienceLevel` (se retiran), `weight_record`.
- `docs/architecture/interfaces_contract.md`: triggers `A1-T1`, `C1-T1`, `C2-T1`, `G1-T1`, `J2-T1`, `J3-T1` y el error `ERR_VALIDATION_PROFILE` (sin el nivel).
- `docs/architecture/architecture_blueprint.md`: ADR-019 (esquema sin migración durante la beta).

**Historias relacionadas:**
- `HU-49` (hermana): carga real del peso corporal en tonelaje y 1RM, y fuerza relativa. Independiente: si se implementa, el gráfico de esta historia mostrará el tonelaje con carga real sin cambios.
- `HU-01`: registro y actualización del perfil. Esta historia retira el nivel de experiencia de su formulario y de su modelo.
- `HU-02`: historial de peso corporal. El registro rápido alimenta este mismo historial.
- `HU-15`, `HU-35`: panel de Métricas e indicadores autoexplicativos. La nueva sección sigue la convención de etiqueta, valor, unidad, descripción y período, y la de "sin datos suficientes".

**Lecciones aprendidas:**
- El nivel de experiencia (`HU-01`) es el ejemplo de lo que esta historia corrige: un dato pedido "por si acaso", sin ningún consumidor. A partir de aquí, todo dato que pida el perfil debe tener al menos un uso visible.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente (CA-48.05)
- [ ] Mensajes implementados
- [ ] Verificado que el nivel de experiencia no aparece en ninguna pantalla, modelo ni respaldo (CA-48.01, CA-48.02, CA-48.08)
- [ ] Verificado que el registro rápido crea una entrada del mismo historial de peso que el perfil (CA-48.04)
- [ ] Verificado que el IMC se muestra sin categoría ni valoración (CA-48.03)
- [ ] Verificado "Sin datos suficientes" y la nota de peso desactualizado (CA-48.06, CA-48.07)
- [ ] Documentación actualizada: RF-01, SDD §3.1, `domain_and_state_model.md`, `interfaces_contract.md`
- [ ] `story_mapping_index.md` actualizado con la historia y sus relaciones
