# Historia de Usuario

**Como** ejecutante,
**Quiero** que el sistema determine automáticamente qué módulo y versión me corresponde entrenar según la rotación cíclica, y que al iniciar sesión me muestre los ejercicios prescritos con la carga objetivo derivada de mi historial,
**Para** saber exactamente qué toca entrenar y con cuánto peso sin tener que recordar ni calcular nada manualmente.

## Descripción

Esta historia implementa la capacidad central de determinar e iniciar sesiones de entrenamiento basadas en la rotación cíclica del sistema. El ejecutante, al iniciar sesión, ve automáticamente qué módulo (A, B o C) y versión (V1, V2 o V3) le corresponde, junto con la lista completa de ejercicios prescritos y la carga objetivo para cada uno.

La rotación cíclica es independiente del calendario: se basa exclusivamente en la secuencia de sesiones completadas (Completadas o Incompletas), no en el día de la semana ni la fecha. La secuencia de módulos es A→B→C→A→B→C, y la de versiones es V1→V2→V3→V1 para cada módulo.

Esta es la historia más compleja hasta ahora: involucra tres capacidades distintas que se orquestan en un flujo único: (1) Determinación del módulo-versión por rotación cíclica, (2) Creación de la sesión en transacción atómica, (3) Presentación de la prescripción con carga objetivo. También implementa crash recovery (RNF10) y transforma B1 de stub a Home funcional.

**Lo que HU-05 NO incluye:** Registrar series (HU-06), sustituir ejercicios (HU-07), cerrar sesión y avanzar rotación (HU-09), clasificación de progresión (HU-10), cálculo de Doble Umbral (HU-11).

---

## Criterios de Aceptación

### CA-05.01 — Determinación automática del módulo por rotación cíclica

```
DADO que el ejecutante desea iniciar una nueva sesión de entrenamiento
CUANDO accede a la funcionalidad de iniciar sesión
ENTONCES el sistema determina automáticamente el módulo que corresponde según la posición actual en la rotación cíclica (A→B→C→A→B→C), basándose exclusivamente en la secuencia de sesiones completadas (Completadas o Incompletas), sin considerar el día de la semana ni la fecha del calendario
```

### CA-05.02 — Determinación automática de la versión

```
DADO que el sistema ha determinado el módulo que corresponde
CUANDO calcula la versión a ejecutar
ENTONCES asigna la versión según la secuencia de versiones del módulo: V1→V2→V3→V1 para todos los módulos (A, B y C), basándose en la última versión ejecutada del mismo módulo
```

### CA-05.03 — Primera sesión del sistema

```
DADO que el ejecutante no tiene sesiones previas en el sistema
CUANDO inicia su primera sesión
ENTONCES el sistema asigna el Módulo A, Versión 1, como punto de partida de la rotación cíclica
```

### CA-05.04 — Persistencia indefinida de la posición en la rotación

```
DADO que el ejecutante ha completado sesiones previas y se ausenta durante un período de tiempo (días, semanas o meses)
CUANDO regresa y desea iniciar una nueva sesión
ENTONCES el sistema retoma exactamente la posición en la rotación de módulos y la secuencia de versiones donde se quedó, sin reiniciar ni alterar la posición por la ausencia
```

### CA-05.05 — Durabilidad del estado de rotación

```
DADO que el ejecutante tiene una posición activa en la rotación cíclica y la secuencia de versiones
CUANDO la aplicación se cierra, el dispositivo se reinicia o la app se actualiza a una nueva versión
ENTONCES el estado de rotación (módulo actual y versión por módulo) persiste de forma durable y se recupera correctamente al reabrir la aplicación
```

### CA-05.06 — Presentación de la prescripción al iniciar sesión

```
DADO que el sistema ha determinado el módulo y la versión que corresponde
CUANDO presenta la sesión al ejecutante
ENTONCES muestra: el identificador del módulo y versión asignados, la lista completa de ejercicios prescritos para esa combinación módulo-versión, y la carga objetivo para cada ejercicio derivada de su último registro histórico
```

### CA-05.07 — Carga objetivo para ejercicios sin historial

```
DADO que el ejecutante inicia una sesión que contiene un ejercicio que nunca ha sido ejecutado previamente
CUANDO el sistema presenta la prescripción
ENTONCES el ejercicio se muestra sin carga objetivo precargada, indicando al ejecutante que debe establecer su carga inicial
```

### CA-05.08 — Carga objetivo derivada del historial

```
DADO que el ejecutante ha registrado previamente datos para un ejercicio prescrito en la sesión
CUANDO el sistema presenta la prescripción
ENTONCES la carga objetivo para ese ejercicio se deriva del último registro histórico del mismo ejercicio, reflejando incrementos ya calculados por la Regla de Doble Umbral si corresponde, o mantenimiento de carga si no se cumplieron las condiciones de progresión
```

### CA-05.09 — Independencia total del calendario

```
DADO que el sistema determina el módulo y la versión de una sesión
CUANDO realiza el cálculo
ENTONCES en ningún momento consulta ni utiliza el día de la semana, la fecha del calendario ni ninguna referencia temporal para la determinación del módulo o versión; la decisión se basa exclusivamente en la secuencia de sesiones completadas
```
