# Escenarios

Catálogo en `Tension/app/src/androidTest/.../testdata/TreeTestScenarios.kt`. **21 escenarios**
con fechas relativas al día de ejecución, así que ninguno caduca.

Cada uno se define con dos números —sesiones cerradas y días desde la última— porque son las
dos únicas lecturas de las que sale el estado del árbol (ver `modelo.md` §1).

---

## Los que importan para lo visual

| Id | Vista | Para qué |
| --- | --- | --- |
| `01` | Semilla | Tallito con dos hojas planas sobre la tierra |
| `03` | Brote sano | Plántula: tallo fino, una bifurcación, dos hojas |
| `07` | Joven sano | Tronco esbelto y vertical, copa estrecha |
| `08` | Joven salud 50 | Follaje reducido, verde amarillento (CA-38.02) |
| `12` | **Maduro salud 100** | Referencia de fidelidad máxima: copa ancha y densa |
| `14` | **Maduro salud 0** | Árbol desnudo pero ramificado |
| `20` | Brote salud 0 | Tallito pelado |
| `21` | Joven salud 0 | Arbolito desnudo |

## Las ternas que hay que pedir en pareja

| Terna | Qué demuestra |
| --- | --- |
| `01,03,07,12` | La secuencia de crecimiento. **La etapa se lee por forma, no por tamaño** |
| `20,21,14` | Misma salud (0) sobre tres edades. **El marchitado respeta la forma de cada una** |
| `12,14` | La ortogonalidad: misma etapa, salud opuesta. El corazón de las dos historias |
| `08,12,14` | Los tres estados de follaje que describe CA-38.02, en orden |

## Matriz completa

| # | Escenario | Ses. | Última | Etapa | Salud | Criterios |
| --- | --- | --- | --- | --- | --- | --- |
| 01 | Instalación virgen | 0 | — | SEED | 100 | CA-37.10 |
| 02 | Solo sesión en curso | 0 | — | SEED | 100 | CA-37.07 |
| 03 | Primer entrenamiento, hoy | 1 | hoy | SPROUT | 100 | CA-37.04 |
| 04 | Borde del margen de 48 h | 5 | −2 | SPROUT | 100 | CA-37.03 |
| 05 | Primer día de descenso | 5 | −3 | SPROUT | 92 | CA-37.03 |
| 06 | Último Brote antes de Joven | 9 | −1 | SPROUT | 100 | CA-37.04 |
| 07 | Primer día de Joven | 10 | hoy | YOUNG | 100 | CA-37.04 |
| 08 | Salud media exacta | 15 | −8 | YOUNG | 50 | CA-37.03 · CA-38.02 |
| 09 | Salud baja | 15 | −11 | YOUNG | 25 | CA-37.03 |
| 10 | Último Joven antes de Maduro | 29 | −5 | YOUNG | 75 | CA-37.04 |
| 11 | Primer día de Maduro | 30 | hoy | MATURE | 100 | CA-37.04 |
| 12 | Maduro pleno | 45 | −1 | MATURE | 100 | CA-38.02 |
| 13 | Último día con salud | 45 | −13 | MATURE | 8 | CA-37.03 |
| 14 | Marchito, corte exacto | 45 | −14 | MATURE | 0 | CA-37.03 · CA-37.11 |
| 15 | Ausencia prolongada | 45 | −30 | MATURE | 0 | CA-37.11 |
| 16 | «Hoy no entreno» no protege | 15 | −10 | YOUNG | 33 | CA-37.07 |
| 17 | Historial mixto | 20 | −4 | YOUNG | 83 | CA-37.07 |
| 18 | Sesión reasignada cuenta igual | 20 | hoy | YOUNG | 100 | CA-37.07 |
| 19 | Ausencia extrema | 60 | −200 | MATURE | 0 | robustez |
| 20 | Brote marchito | 5 | −14 | SPROUT | 0 | CA-37.04 · CA-38.02 |
| 21 | Joven marchito | 15 | −14 | YOUNG | 0 | CA-37.04 · CA-38.02 |

Los escenarios `02`, `04`–`06`, `09`–`11`, `13`, `15`–`19` existen sobre todo para la
comprobación automática de los cálculos (`seed-arbol.ps1 -Todos`), no para lo visual.

---

## Añadir un escenario

Requiere tocar **tres** sitios. Olvidar el segundo deja la tira con un rótulo pobre; olvidar el
tercero deja el catálogo sin documentar.

1. **`TreeTestScenarios.kt`** — añadir un `TreeScenario` con su id, sesiones, días y los valores
   esperados. Los valores esperados se **transcriben a mano** de las tablas de CA-37.03 y
   CA-37.04: derivarlos de `TreeHealthRule` o `TreeGrowthStageRule` volvería la comprobación
   circular y un error en las reglas pasaría inadvertido.
2. **`tools/capturar-arbol.ps1`**, tabla `$rotulos` — el rótulo que llevará en la tira. Sin
   entrada se rotula solo con el id.
3. **Este archivo** — la matriz y, si el escenario forma pareja con otro, la tabla de ternas.

`seed-arbol.ps1 -Todos` **no** necesita cambios: lee la lista del propio inyector, así que
incorpora el escenario nuevo al barrido por su cuenta.

### Restricciones del inyector al inventar un escenario

- La sesión `IN_PROGRESS` solo puede ser **una y fechada hoy**. De un día anterior la descarta
  el barrido de `ResolveStaleSessionUseCase` antes de que se pueda ver, y varias simultáneas
  rompen la invariante de sesión activa única que asume el resto de la aplicación.
- `day_skip` y `daily_routine_override` solo se honran cuando su fecha es **hoy**.
- Un escenario con `omitirHoy` no debe tener sesión de hoy: se estorbarían.
