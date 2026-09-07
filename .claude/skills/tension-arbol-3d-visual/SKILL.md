---
name: tension-arbol-3d-visual
description: "Flujo de trabajo para ajustar el árbol 3D de entrenamiento (HU-37 / HU-38) y verificarlo visualmente. Úsala SIEMPRE que la tarea toque `assets/tree/tree.js`, la pantalla del árbol, la tarjeta del árbol en Inicio, la geometría o el encuadre del modelo, las etapas de crecimiento (Semilla, Brote, Joven, Maduro), el marchitado por salud, el fallback nativo del WebView o la calidad de render del árbol. También cuando se pida 'ver cómo queda el árbol', 'probar el árbol', 'capturas del árbol' o comparar etapas. Define el reparto de trabajo: el agente edita el modelo y decide qué escenarios mirar; la PERSONA ejecuta el emulador y comparte la ruta de las capturas. El agente NO ejecuta el emulador ni toma capturas."
---

# Ajuste visual del árbol 3D

## Regla que gobierna todo lo demás

**El agente no ejecuta el emulador, no siembra escenarios y no toma capturas.** Sembrar,
compilar, esperar arranques en frío y capturar pantallas son minutos de espera por iteración.
Que el agente los vigile consume contexto en sondeos que no aportan nada al resultado.

| Paso | Quién |
| --- | --- |
| 1. Leer el modelo y decidir el cambio | Agente |
| 2. Editar `tree.js` (u otro código) | Agente |
| 3. Decir **qué comando ejecutar y qué escenarios mirar** | Agente |
| 4. **Ejecutar el comando y compartir la ruta de la tira** | **Persona** |
| 5. Leer la tira, evaluar y decidir la siguiente iteración | Agente |

En el paso 3 el agente **para y pide**. No lanza el emulador «para adelantar», no comprueba si
ya está corriendo, no hace `adb` de nada. Pide y espera.

Cuando la persona comparta la ruta, el agente lee **la tira** (`tira-<etiqueta>.png`), no las
capturas sueltas: es una sola imagen con todas las etapas rotuladas una al lado de otra, y por
eso cuesta una lectura en vez de cinco.

### Cómo pedirlo

Un pedido bien formado lleva el comando exacto, los escenarios y qué se va a mirar:

> Listo el cambio en `tree.js`. Ejecuta esto y compárteme la ruta que imprime al final:
>
> ```powershell
> .\tools\capturar-arbol.ps1 -Instalar -Escenarios 01,03,07,12,14 -Etiqueta forma-v2
> ```
>
> Voy a comprobar que la copa no tenga huecos y que el maduro se lea más grande que el brote.

`-Instalar` es **obligatorio si se tocó `tree.js`**: los assets del WebView viajan dentro del
APK y sin reinstalar se sigue viendo el modelo anterior. Omitirlo es la causa más probable de
«no cambió nada».

---

## Los dos comandos de siempre

```powershell
# Crecimiento: la etapa se lee por forma, no por tamaño
.\tools\capturar-arbol.ps1 -Instalar -Escenarios 01,03,07,12 -Etiqueta crecimiento

# Marchitado por edad: misma salud (0) sobre tres edades
.\tools\capturar-arbol.ps1 -Escenarios 20,21,14 -Etiqueta marchito
```

El script siembra cada escenario, captura, compone la tira, mide el árbol en píxeles e imprime
el presupuesto de render que reportó `tree.js`.

Con una `-Etiqueta` distinta por iteración se comparan versiones sin sobrescribir nada.

---

## Qué mirar en la tira

| Comprobación | Criterio | Señal de fallo |
| --- | --- | --- |
| Nada recortado | CA-38.03 | Copa o tronco cortados en plano contra el borde del recuadro |
| La etapa se lee | CA-38.02 | Un brote igual de grande —o mayor— que un maduro |
| Copa sin huecos | — | Se ve el esqueleto entre floretes de follaje separados |
| Base sellada | — | Se ve el fondo entre el tronco y la tierra |
| Marchito ramificado | CA-38.02 | El maduro sin hojas parece un poste o una estaca |
| Color por salud | CA-38.02 | Verde vivo en 100, amarillento en 50, marrón en 0 |

La **tabla de medidas** que imprime el script es el juez de las dos que el ojo estima mal:

- **`MargenSup` y `MargenInf` mayores que cero** en todos los escenarios ⇒ no hay recorte.
- **`AltoPx` creciente** de `01` a `12` ⇒ la etapa se lee por tamaño además de por forma.

⚠️ **Antes de comparar medidas entre iteraciones, mirar la línea `calidad=`.** Un cambio de
calidad cambia el tamaño aparente —menos hojas dan una copa más pequeña, el encuadre se acerca
y el árbol se ve mayor—, y es fácil atribuir a la geometría lo que hizo la sonda de rendimiento.

---

## Lo que sí ejecuta el agente

Rápido y sin emulador:

```powershell
# Sintaxis del JS: ningún paso del build la valida
node --check Tension/app/src/main/assets/tree/tree.js

# Pruebas unitarias
cd Tension; .\gradlew.bat --quiet :app:testDebugUnitTest
```

Cambiar `tree.js` **no** puede romper las pruebas de Kotlin ni el estado en base de datos: el
árbol se deriva de `session` y el JS solo dibuja. Si el barrido de escenarios pasaba antes,
sigue pasando; no hace falta repetirlo por un cambio de geometría.

---

## Detalle bajo demanda

Leer solo lo que la tarea necesite:

| Archivo | Cuándo leerlo |
| --- | --- |
| `references/modelo.md` | **Antes de tocar `tree.js`.** Cómo funcionan la forma por etapa, el encuadre derivado, la ramificación y el presupuesto de render |
| `references/escenarios.md` | Para elegir qué escenarios pedir, o para añadir uno nuevo |
| `references/mantenimiento.md` | Si la siembra falla, si algo no se ve como se espera, o para evolucionar esta skill |

---

## Archivos

| Ruta | Qué es |
| --- | --- |
| `Tension/app/src/main/assets/tree/tree.js` | El modelo entero. **Lo que se edita** |
| `Tension/app/src/main/java/.../ui/tree/Tree3DView.kt` | WebView, fallback nativo, calidad |
| `Tension/app/src/main/java/.../ui/tree/TreeScreen.kt` | Pantalla dedicada |
| `Tension/app/src/main/java/.../data/repository/TreeRepositoryImpl.kt` | De dónde sale el estado |
| `tools/capturar-arbol.ps1` | **Lo que ejecuta la persona** |
| `tools/seed-arbol.ps1` | Siembra un escenario. `-Todos` verifica los 21 |
| `tools/README-pruebas-arbol.md` | Matriz completa y guion del eje de entorno |
| `.../androidTest/.../testdata/TreeTestScenarios.kt` | Catálogo de escenarios |
| `docs/domain/stories/HU-38-arbol-3d-interactivo/historia.md` | Criterios CA-38.xx |

> `tools/` y `androidTest/.../testdata/` están **excluidos del control de versiones en local**
> vía `.git/info/exclude`. Existen en la máquina pero no se publican. Si en una sesión no
> aparecen, es que se está en otra copia del repositorio: pregunta antes de darlos por perdidos.
> Esta skill sí está versionada.
