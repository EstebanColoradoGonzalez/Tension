# Tension

Sistema de decisiones de entrenamiento de fuerza para Android. Transforma el registro de carga, repeticiones e intensidad percibida (RIR) en un ciclo cerrado de retroalimentación que garantiza la sobrecarga progresiva continua.

100% offline. Sin cuentas. Sin servidores. Los datos viven en el dispositivo.

## Qué hace

- **Prescribe** — Determina automáticamente módulo, versión, ejercicios y carga objetivo antes de cada sesión
- **Detecta** — Identifica mesetas, regresiones, fatiga acumulada y emite alertas con diagnóstico causal
- **Evidencia** — Construye historial analítico: tonelaje por grupo muscular, tasa de progresión, tendencias de carga

## Stack tecnológico

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM con capa Domain explícita |
| Persistencia | Room (SQLite local) |
| DI | Hilt |
| Navegación | Navigation Compose (Single Activity) |
| Testing | JUnit 4 + MockK |
| Min SDK | 26 (Android 8.0) |

## Requisitos previos

- **Android Studio** Ladybug (2024.2) o superior
- **JDK 11+**
- **Gradle 8.11.1** (incluido vía wrapper)

## Compilar y ejecutar

```bash
cd Tension
./gradlew assembleDebug
```

El APK se genera en `Tension/app/build/outputs/apk/debug/`.

Para instalar directamente en un dispositivo conectado:

```bash
./gradlew installDebug
```

## Ejecutar tests

```bash
cd Tension
./gradlew test
```

## Estructura del proyecto

```
Tension/                  ← Proyecto Android (Gradle root)
  app/src/main/java/.../tension/
    ├── ui/               ← Screens y ViewModels por flujo
    ├── domain/           ← Use Cases, modelos y motor de reglas (Kotlin puro)
    ├── data/             ← Room (entities, DAOs, repositories, seed data)
    └── di/               ← Módulos Hilt

docs/                     ← Documentación del producto
  ├── architecture/       ← ADRs, modelo de datos, wireframes, especificación visual
  ├── business_definition/← Visión, requerimientos, diccionario de ejercicios
  └── stories/            ← 32 historias de usuario con refinamiento técnico
```

## Documentación

| Documento | Descripción |
|-----------|-------------|
| [Visión del Producto](docs/business_definition/Visión%20del%20Producto.md) | Problema, solución, principios rectores y alcance |
| [Arquitectura Técnica](docs/architecture/Arquitectura%20Técnica.md) | Stack, capas MVVM, paquetes y navegación |
| [Modelo de Datos](docs/architecture/Modelo%20de%20Datos.md) | 16 entidades Room con relaciones y seed data |
| [ADR](docs/architecture/ADR.md) | 18 decisiones arquitectónicas documentadas |
| [Especificación Visual](docs/architecture/Especificación%20Visual.md) | Sistema de diseño M3, paleta y componentes |
| [Requerimientos](docs/business_definition/Requerimientos.md) | Requisitos funcionales y no funcionales |

## Licencia

Proyecto personal. Todos los derechos reservados.
