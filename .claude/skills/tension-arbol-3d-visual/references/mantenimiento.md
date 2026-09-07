# Mantenimiento y trampas conocidas

---

## La siembra falla: deriva de esquema

El inyector **escribe en las tablas de la aplicación**, así que sigue al esquema. Cuando una
historia nueva añade una columna `NOT NULL` a `session`, `session_exercise` o `exercise_set`, la
siembra revienta:

```
SQLiteConstraintException: NOT NULL constraint failed: exercise_set.equipment_type_id
```

No es un fallo del árbol. Se arregla en `TreeScenarioSeeder.insertarSerie` —o en
`sembrarSesiones`— dando valor a la columna nueva.

**Sacar el valor del catálogo, nunca fijarlo.** Una constante escrita a mano apunta al registro
equivocado en cuanto el catálogo cambia, y si la columna lleva clave foránea el `INSERT` se
rechaza sin más.

Ya ocurrió: HU-39 pasó el tipo de equipo de `exercise` a `exercise_set` y añadió la tabla puente
`exercise_equipment`. La solución fue resolver el equipo por esa tabla, con el primero del
catálogo como respaldo para los ejercicios que no declaran ninguno.

Para saber qué cambió, comparar los **esquemas exportados**, que son la fuente fiable:

```
Tension/app/schemas/com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase/
```

Cada versión tiene su `<n>.json` con las columnas y el DDL exacto. Diferenciar el `<n>` anterior
contra el actual dice exactamente qué se añadió.

---

## Trampas conocidas

- **Olvidar `-Instalar` tras editar `tree.js`.** Se ve el modelo anterior y se saca la
  conclusión contraria. Es el fallo más frecuente y el más caro, porque parece un resultado.
- **Detectar la pantalla del árbol por `content-desc`.** La descripción «Árbol de
  entrenamiento» la comparten la tarjeta de Inicio y el área de render. Detectar por ahí hace
  creer que se está dentro cuando se está capturando Inicio — pasó, y produjo cinco capturas
  inútiles de la pantalla de Inicio. Se detecta por el texto `Salud`, que solo existe en la
  pantalla dedicada. El script ya lo hace bien.
- **`connectedAndroidTest` desinstala los APK al terminar.** Después hay que volver a sembrar
  con `-Instalar`, o el script se queda sin instrumentación que invocar.
- **Medir «todo lo que difiera del fondo».** Eso mide la sombra del plano, no el árbol. El
  script filtra por saturación de color justamente para eso.
- **Medir el encuadre con `Box3.setFromObject`.** Recorre también los nodos invisibles, así que
  en la etapa Semilla mediría el árbol oculto detrás.
- **Nombres de variable en PowerShell.** No distingue mayúsculas: un `$destino` para una ruta de
  archivo pisa un `$Destino` que llevara el destino de `adb`. Costó una corrida con PNG de cero
  bytes.

---

## Cómo evolucionar esta skill

### Dónde va cada cosa

| Contenido | Archivo |
| --- | --- |
| El reparto de trabajo, los comandos, qué mirar | `SKILL.md` |
| Cómo funciona el modelo, el encuadre, el presupuesto | `references/modelo.md` |
| Catálogo de escenarios y cómo añadir uno | `references/escenarios.md` |
| Fallos, trampas y esta guía | `references/mantenimiento.md` |

**`SKILL.md` se carga entero cada vez que la skill se invoca; las referencias solo cuando el
agente decide leerlas.** De ahí la división: lo que se necesita siempre arriba, el detalle
abajo. Al añadir material, la pregunta es «¿hace falta esto en toda tarea del árbol?». Si la
respuesta es no, va a una referencia.

Si una referencia pasa de unas 300 líneas, conviene partirla antes que dejarla crecer: se lee
completa cuando se lee.

### Está versionada, y por qué el nombre lleva prefijo

`.github/` y `.claude/` están ignorados en el repositorio **salvo las skills cuyo nombre empieza
por `tension-`**. La convención existe para que crear una skill nueva del proyecto no obligue a
tocar el `.gitignore`: basta nombrarla así.

Si `git status` sale limpio después de editar esta skill, lo más probable es que un update del
Método Ceiba haya vuelto a añadir `.github/skills/` o `.claude/skills/` al final del
`.gitignore`, anulando las excepciones. Se arregla borrando esas líneas.

### Hay dos copias y deben ser idénticas

`.claude/skills/tension-arbol-3d-visual/` y `.github/skills/tension-arbol-3d-visual/` son la
misma skill para dos herramientas. **Al editar una, copiar la otra**:

```powershell
Copy-Item -Recurse -Force .claude\skills\tension-arbol-3d-visual\* `
    .github\skills\tension-arbol-3d-visual\
```

### Qué merece anotarse aquí

Lo que costó una iteración descubrir. Un dato que se comprueba en diez segundos no hace falta
documentarlo; uno que exigió recompilar, sembrar y mirar una captura, sí. Las trampas de arriba
son todas de esa clase: cada una fue una iteración perdida.

Cuando una corrida de verificación desmienta algo escrito aquí, **corregirlo en el momento**. La
peor versión de esta skill es una que afirme cosas que dejaron de ser verdad: se sigue creyendo
hasta que vuelve a costar una iteración.
