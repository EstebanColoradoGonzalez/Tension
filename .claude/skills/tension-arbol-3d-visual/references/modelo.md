# El modelo del árbol — cómo está construido

Leer **antes de tocar `tree.js`**. Son los hechos que ahorran horas, descubiertos a base de
romper cosas.

---

## 1. El estado del árbol es derivado; la única palanca es `session`

`TreeRepositoryImpl.recalculate()` lee exactamente dos cosas:

```kotlin
val sessionCount    = sessionDao.countClosedSessions()       // → etapa
val lastSessionDate = sessionDao.getLastClosedSessionDate()  // → salud
```

Y recalcula en tres momentos (CA-37.06), uno de ellos **al abrir la pantalla del árbol**.

Consecuencia: escribir `tree_state` a mano —por SQL, por respaldo, por lo que sea— no sirve de
nada, el primer recálculo lo sobrescribe. Por eso los escenarios se describen con dos números:
cuántas sesiones cerradas hay y hace cuántos días fue la última.

## 2. La etapa es una **forma**, no un tamaño

`STAGE_PRESETS` da a cada etapa su propia profundidad de ramificación, grosor de tronco,
apertura de ramas, tamaño y achatamiento de hoja, y caída al marchitarse:

| Etapa | Niveles | Silueta |
| --- | --- | --- |
| `SEED` | — | Tallito con dos hojas planas sobre la tierra (grupo aparte, sin esqueleto) |
| `SPROUT` | 1 | Plántula: tallo fino que se abre una vez, dos hojas |
| `YOUNG` | 3 | Tronco esbelto y vertical, copa estrecha aún sin abrir |
| `MATURE` | 4 | Tronco grueso, copa ancha y densa |

Cambiar de etapa **reconstruye el modelo** (`rebuildTree()`), no mueve la cámara.

**No existe un parámetro de escala, y no hay que reintroducirlo.** El encuadre se deriva del
modelo, así que agrandarlo alejaría la cámara en la misma proporción y el tamaño aparente no
cambiaría. Un mando que no mueve nada es peor que no tenerlo: el tamaño lo gobierna `frameFill`
y solo `frameFill`.

Corolario que salió gratis: **marchitarse respeta la edad**. Quitarle las hojas a un brote deja
un tallito pelado y a un maduro un árbol desnudo pero ramificado. El marchitado no sabe nada de
la etapa; actúa sobre la forma que haya.

## 3. El encuadre se deriva de la geometría real

`collectFitSamples()` describe el árbol como esferas —una por hoja, dos por rama—, y
`fitRadiusForSamples()` calcula a qué distancia caben todas, muestreando toda la órbita
alcanzable. Dos implicaciones prácticas:

- **Cambiar la forma no obliga a reajustar ninguna cámara.** Se recoloca sola.
- **No volver a medir con un volumen envolvente.** Las diagonales de una caja alrededor de un
  árbol son aire, y encuadrar contra ellas aleja la cámara un 40 % de más: el árbol queda
  pequeño en un cuadro medio vacío.

`frameStage(reponerDistancia)` distingue dos casos, y confundirlos ya costó una iteración:

- **`true`** al cambiar de etapa ⇒ repone la distancia de reposo de la etapa nueva.
- **`false`** al redimensionar ⇒ conserva el zoom de la persona y solo lo reajusta a los topes.

Acotar también al cambiar de etapa dejaba la distancia anterior pegada al tope de acercamiento
de la nueva, con lo que **todas las etapas terminaban ocupando el cuadro entero** y el tamaño
dejaba de expresar la etapa.

El tope de acercamiento del zoom es `stageFitRadius`, la distancia de encaje exacto: así el
pellizco más agresivo deja el árbol tocando los bordes sin recortarlo ni atravesarlo, que es
literalmente lo que pide CA-38.03.

## 4. El azar necesita semilla

`BRANCH_SEED` es fija a propósito. `rebuildTree()` se vuelve a llamar cuando la sonda de
rendimiento degrada la calidad, unos segundos después de abrir la pantalla: con `Math.random()`
el árbol se convertiría en otro árbol delante de la persona. La irregularidad es deliberada; la
aleatoriedad entre reconstrucciones, no.

## 5. Los huecos aparecen en tres sitios, no en uno

| Hueco | Cómo se cierra |
| --- | --- |
| En la copa | Las hojas cuelgan de las puntas reales, con **diámetro mayor que la separación entre puntas vecinas** (`FOLIAGE_TIP_SCALE`) |
| En las bifurcaciones | Una esfera de unión por nudo, y los hijos nacen **antes** de la punta del padre (`BRANCH_ATTACH`) |
| **En la base** | El tronco **nace enterrado** en el montículo, con el pie ensanchado (`TRUNK_BURY_*`, `ROOT_FLARE_*`) |

El de la base es el que se olvida. Antes la cúpula terminaba en `y = −0.06` y el tronco
arrancaba en `y = 0`: no se tocaban, y desde cualquier ángulo bajo se veía el fondo entre ambos.

El umbral del follaje es el que más se afina y el que más se equivoca: por debajo, la copa se
lee como floretes sueltos y se ve el esqueleto entre ellos; por encima, la copa se come el
tronco y desaparece la ramificación.

## 6. El esqueleto y el dibujo están separados

- **Esqueleto**: jerarquía de `Object3D` **sin geometría**, solo nudos con posición y rotación.
  Three.js compone sus matrices gratis, así que la caída por salud sigue siendo una rotación por
  nudo y no un recálculo a mano de la forma.
- **Dibujo**: tres `InstancedMesh` —segmentos, uniones, hojas— que leen la matriz de cada nudo.

`updateSkeletonMatrices()` calcula las matrices **relativas a la raíz del modelo**, a mano y en
una sola pasada. Usar `matrixWorld` ataría el resultado a las transformaciones de los ancestros
y habría que deshacerlas, porque Three.js ya multiplica la matriz de la malla por la de cada
instancia.

`frustumCulled` debe seguir en **`false`**: el volumen envolvente de un `InstancedMesh` describe
la geometría unitaria, no dónde acaban sus instancias, y dejarlo activo hace desaparecer el
árbol entero al girar la cámara.

---

## Presupuesto de render

`tree.js` informa por la consola del WebView, que llega a logcat. El script de captura lo
imprime al final:

```
[tree] construido etapa=MATURE calidad=medium segmentos=22 hojas=36 sombras=false
```

- **`calidad`** es lo que `TreeRenderQuality.resolve` decidió por las señales del dispositivo.
  Los topes de calidad **acotan** la forma, no la definen: un maduro en calidad baja sigue
  siendo un maduro, con menos detalle.
- **Una línea `degradado`** significa que la sonda de los primeros fotogramas no cumplió el
  presupuesto y bajó un escalón. En el emulador `Medium_Phone_API_35` **aparece de forma
  intermitente** con el modelo actual: a veces se queda en `medium` y a veces baja a `low`. Por
  sí sola no es señal de problema; lo es que aparezca **siempre**, y en un dispositivo que antes
  aguantaba.

Cuentas de referencia de la etapa Maduro, para comparar tras un cambio:

| Calidad | Niveles | Segmentos | Hojas | Triángulos | Llamadas de dibujo |
| --- | --- | --- | --- | --- | --- |
| `high` | 4 | 46 | 96 | ≈ 13 600 | 5 |
| `medium` | 3 | 22 | 36 | ≈ 5 700 | 5 |
| `low` | 2 | 10 | 12 | ≈ 600 | 4 |
| *(versión anterior, copa fija)* | — | 4 | 7 | ≈ 760 | 12 |

**Añadir detalle es más barato que añadir mallas.** En gráficos móviles la llamada de dibujo
cuesta más que el triángulo: el modelo actual tiene 18× los triángulos del anterior con menos
de la mitad de llamadas. Si hace falta más geometría, que entre en las instancias existentes y
no como mallas nuevas.

Peso en el APK, sin cambios por la ramificación —es solo código, no assets nuevos—:
**220 KiB** comprimidos, de los cuales 199 son Three.js.

## Estado de verificación por calidad

| Calidad | Verificada en dispositivo |
| --- | --- |
| `medium` | Sí, es lo que resuelve `Medium_Phone_API_35` |
| `low` | Sí, cuando la sonda degrada: renderiza sin esferas de unión, base igual de sellada |
| `high` | **No.** Requiere un AVD de 8 núcleos, como describe el eje B3 del README |
