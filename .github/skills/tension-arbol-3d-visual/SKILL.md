---
name: tension-arbol-3d-visual
description: "Flujo de trabajo para ajustar el árbol 3D de entrenamiento (HU-37 / HU-38 / HU-44) y verificarlo visualmente. Úsala SIEMPRE que la tarea toque `assets/tree/tree.js`, la pantalla del árbol, la tarjeta del árbol en Inicio, la geometría o el encuadre del modelo, las etapas de crecimiento (Semilla, Brote, Joven, Maduro), el marchitado por salud, el fallback nativo del WebView o la calidad de render del árbol. También cuando se pida 'ver cómo queda el árbol', 'probar el árbol', 'capturas del árbol' o comparar etapas. Define el reparto de trabajo: el agente edita el modelo, verifica lo que se puede verificar sin emulador, y entrega una GUÍA PASO A PASO (emulador, pantalla encendida, carpeta, comandos en orden) y SE DETIENE; la PERSONA ejecuta y avisa; el agente lee las tiras y decide la siguiente iteración. El agente NO ejecuta el emulador, NO siembra y NO toma capturas."
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
| 3. Verificar sin emulador: `node --check`, arnés de humo, presupuesto | Agente |
| 4. **Entregar la guía paso a paso y PARAR** | Agente |
| 5. **Ejecutar los comandos y avisar** | **Persona** |
| 6. Leer las tiras, evaluar y decidir la siguiente iteración | Agente |

En el paso 4 el agente **para y espera**. No lanza el emulador «para adelantar», no comprueba
si ya está corriendo, no siembra, no captura. **Entrega la guía y no sigue hasta que la
persona avise de que los comandos terminaron.**

Cuando la persona avise, el agente lee **las tiras** (`tira-<etiqueta>.png`), no las capturas
sueltas: cada tira es una sola imagen con todos los escenarios rotulados uno al lado de otro, y
por eso cuesta una lectura en vez de N.

---

## Paso 4 — la guía que se entrega

**No basta con pegar el comando.** La persona necesita saber qué emulador, desde qué carpeta,
en qué orden y cuándo ha terminado. Una guía incompleta cuesta una ronda entera, y cada ronda
son 10–15 minutos suyos.

La guía **siempre** lleva estas siete piezas, en este orden:

1. **Aviso de que no toma ninguna captura a mano.** El script siembra, navega, captura, compone
   la tira y mide. La persona solo lanza comandos. Decirlo primero evita que lo intente.
2. **Cómo abrir el emulador**, con el nombre del AVD. Android Studio → Device Manager → ▶, o
   por línea de comandos:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_35
   ```
3. **⚠️ Comprobar que la pantalla está ENCENDIDA**, no solo que el emulador arrancó. Ver la
   sección siguiente: es el fallo que más caro ha salido.
4. **Desde qué carpeta**: la raíz del repositorio, con una comprobación que lo confirme.
   ```powershell
   cd D:\Users\Esteban\Repositories\Tension
   ls .\tools\capturar-arbol.ps1      # si imprime el archivo, es la carpeta correcta
   ```
5. **Los comandos numerados y en orden**, diciendo cuál lleva `-Instalar` y por qué, y cuánto
   tarda cada uno.
6. **Dónde queda todo** (`tools/capturas/`) y **qué compartir**: basta con avisar; el agente lee
   las tiras de esa carpeta.
7. **Qué va a mirar el agente en cada tira**, en orden de probabilidad de fallo. Es lo que
   convierte la espera en una revisión dirigida en vez de una impresión general.

Y cierra pidiendo explícitamente: **«avísame cuando terminen y las reviso»**.

`-Instalar` es **obligatorio si se tocó `tree.js`**: los assets del WebView viajan dentro del
APK y sin reinstalar se sigue viendo el modelo anterior. Va **solo en el primer comando** de la
tanda; los siguientes reutilizan el APK instalado. Omitirlo es la causa más probable de «no
cambió nada».

---

## ⚠️ La pantalla apagada: el fallo que no se ve

Un emulador con la pantalla apagada **sigue ejecutando Android**: el volcado de jerarquía
encuentra las vistas, los toques llegan y la app navega hasta la pantalla del árbol. Pero
`screencap` devuelve un fotograma **negro**.

Lo que lo hace caro es que **no se parece a un fallo**. El 2026-09-30 produjo diez capturas
negras y una tabla de medidas de aspecto impecable, porque el medidor clasifica el negro puro
como madera (`max(R,G,B) = 0 < 120` y `R ≥ G ≥ B`) y «midió» el recuadro entero: `473×473`,
`FillAlto 100%`, márgenes `0` en los diez. Costó una ronda completa.

**Desde entonces `capturar-arbol.ps1` lo impide por sí solo:**

- `Enable-Pantalla` corre **antes** del bucle: si `dumpsys power` no dice `mWakefulness=Awake`,
  envía `KEYCODE_WAKEUP` y desbloquea. Después fija `svc power stayon true`, que mantiene la
  pantalla encendida mientras el dispositivo esté «cargando» —un emulador siempre lo está— para
  que una tanda de diez escenarios no cruce el apagado automático a mitad.
- `Test-CapturaConContenido` corre **después de cada captura**: muestrea una rejilla y cuenta
  colores distintos. Con menos de cuatro, borra el PNG y **aborta en el primer escenario**.
  Seguir adelante produciría una tira negra y una tabla creíble, que es peor que no producir
  nada.

Aun así, la guía del paso 4 **sigue pidiendo comprobar la pantalla a ojo**: el guard convierte
un desperdicio de quince minutos en un error inmediato, pero mirar la ventana del emulador lo
evita del todo.

Señales de que ocurrió, si alguna vez el guard no lo atrapa:

| Señal | Qué significa |
| --- | --- |
| Todas las capturas pesan **exactamente lo mismo** | Son el mismo fotograma; un negro uniforme comprime siempre igual |
| Pesan ~15 KB en vez de **76–128 KB** | No hay contenido que comprimir |
| `AltoPx` = `AnchoPx` = alto del recuadro, con márgenes `0`, **en todos** | El medidor midió el fondo, no el árbol |

> **Distinción importante:** si la ventana del emulador se ve bien y **solo el recuadro del
> árbol** sale negro, eso **sí** es un fallo de render —WebGL, el JS o la geometría— y hay que
> investigarlo. Pantalla entera negra = emulador. Recuadro negro = código.

---

## Los comandos

### Los dos de siempre, para una comprobación rápida

```powershell
# Crecimiento: la etapa se lee por forma, no por tamaño
.\tools\capturar-arbol.ps1 -Instalar -Escenarios 01,03,07,12 -Etiqueta crecimiento

# Marchitado por edad: misma salud (0) sobre tres edades
.\tools\capturar-arbol.ps1 -Escenarios 20,21,14 -Etiqueta marchito
```

El script siembra cada escenario, captura, compone la tira, mide el árbol en píxeles e imprime
el presupuesto de render que reportó `tree.js`.

### Para una aprobación formal de silueta

Tres tandas, **una por banda de salud**, cada una en orden de crecimiento. Son las 10 capturas
alcanzables, sin repetir ninguna:

```powershell
.\tools\capturar-arbol.ps1 -Instalar -Escenarios 01,03,07,12 -Etiqueta v1-alta
.\tools\capturar-arbol.ps1 -Escenarios 22,08,23 -Etiqueta v1-media
.\tools\capturar-arbol.ps1 -Escenarios 20,21,14 -Etiqueta v1-marchito
```

Son **diez y no doce**: la etapa Semilla existe si y solo si hay 0 sesiones cerradas, y sin
última sesión la salud es 100 siempre. Sus otras dos bandas no existen en el dominio y falsear
`tree_state` no sirve (ver `references/modelo.md` §1).

### La etiqueta lleva el número de iteración

`v1-alta`, `v2-alta`, `v3-alta`… Nada se sobrescribe y el agente puede comparar contra la ronda
anterior, que es lo que convierte «se ve mejor» en «la copa pasó de 4 lóbulos a masa continua y
el margen superior bajó de 70 a 45 px».

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

### Arnés de humo: lo que se puede verificar sin emulador

`tree.js` no tiene pruebas automatizadas y no las va a tener —ADR-021: el proyecto no tiene ni
tendrá cadena de build JavaScript—. Pero **Node puede cargar el `three.min.js` del propio
repositorio** y ejecutar el pipeline de geometría entero con el renderer y el DOM stubeados. Un
arnés desechable en el scratchpad de la sesión, **fuera del repositorio**, permite comprobar en
segundos:

- que ninguna malla queda **sin normales** y que ninguna las tiene **apuntando hacia dentro**
  (producto escalar de cada normal contra su dirección desde el centroide);
- que no hay posiciones ni matrices **no finitas**;
- el **presupuesto real** de triángulos y llamadas de dibujo por etapa y calidad;
- el **determinismo**: recorrer todas las etapas y volver, y comprobar que el hash de posiciones
  **y matrices de instancia** coincide exacto.

El 2026-09-30 esto atrapó un defecto que ninguna otra cosa habría visto antes del dispositivo:
las tapas del tubo y el grumo entero con las caras invertidas, porque los anillos del tubo
ascienden mientras las filas del grumo descienden y el mismo orden de índices da orientaciones
contrarias. Habría llegado a las capturas tres iteraciones más tarde.

**Correrlo antes de pedir capturas.** Las rondas de emulador son el recurso caro; todo lo que
se pueda descartar sin gastarlas, se descarta.

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
| `tools/seed-arbol.ps1` | Siembra un escenario. `-Todos` verifica los 23 |
| `tools/README-pruebas-arbol.md` | Matriz completa y guion del eje de entorno |
| `.../androidTest/.../testdata/TreeTestScenarios.kt` | Catálogo de escenarios |
| `docs/domain/stories/HU-38-arbol-3d-interactivo/historia.md` | Criterios CA-38.xx |

> `tools/` y `androidTest/.../testdata/` están **excluidos del control de versiones en local**
> vía `.git/info/exclude`. Existen en la máquina pero no se publican. Si en una sesión no
> aparecen, es que se está en otra copia del repositorio: pregunta antes de darlos por perdidos.
> Esta skill sí está versionada.
