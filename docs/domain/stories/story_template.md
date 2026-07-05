# Historia de Usuario: `[US-ID]` - `[Título Breve y Descriptivo]`

> Este documento define una unidad atómica de valor. El código desarrollado debe cumplir **estrictamente** con los Criterios de Aceptación listados aquí. No se deben añadir funcionalidades no descritas en este documento (evitar *scope creep*).
>

## 1. Metadatos

*Información de seguimiento para el enrutador de historias (Story Mapping Index).*

- **ID:** `[Ej. US-001]`
- **Épica / Módulo:** `[Ej. EPIC-01: Autenticación y Onboarding]`
- **Estado:** `[Todo | In Progress | Done]`
- **Prioridad:** `[Alta | Media | Baja]`
- **Agente Asignado:** `[PO | Architect | Developer | Peer-Reviewer]`

## 2. Narrativa de Negocio

*El propósito de la historia explicado en lenguaje natural desde la perspectiva del actor.*

- **Como** `[Actor / Rol del Usuario o Sistema, ej. Jugador o Administrador]`
- **Quiero** `[Acción o capacidad que se desea alcanzar, ej. iniciar sesión con mi correo y contraseña]`
- **Para** `[Valor o beneficio aportado al negocio, ej. poder acceder a mi inventario guardado en la nube]`

## 3. Criterios de Aceptación (BDD / Gherkin)

*Las reglas de validación exactas. El agente Peer-Reviewer utilizará estos bloques para generar las pruebas unitarias y de integración (TDD/BDD).*

### Escenario 1: `[Nombre del escenario de éxito. Ej. Inicio de sesión exitoso]`

Gherkin

```
Dado [El contexto inicial o estado del sistema. Ej. que existe un usuario registrado con el correo "test@app.com" y contraseña "12345"]
Y [Condición adicional. Ej. el usuario se encuentra en la pantalla de login]
Cuando [La acción que ejecuta el trigger. Ej. el usuario envía el formulario con credenciales válidas]
Entonces [El resultado esperado y verificable. Ej. el sistema debe devolver un token JWT válido (HTTP 200)]
Y [Efecto secundario esperado. Ej. el estado del usuario cambia a "Autenticado"]
```

### Escenario 2: `[Nombre del escenario alternativo o de fallo. Ej. Contraseña incorrecta]`

Gherkin

```
Dado [Contexto inicial. Ej. que existe el usuario "test@app.com"]
Cuando [Acción. Ej. envía el formulario con la contraseña "incorrecta"]
Entonces [Resultado. Ej. el sistema debe devolver el error ERR_UNAUTHORIZED (HTTP 401)]
Y [Efecto secundario. Ej. registrar un intento fallido en el log de seguridad]
```

*(Añadir tantos escenarios como variaciones tenga la regla de negocio)*

## 4. Dependencias Técnicas e Integración

*Mapeo explícito realizado por el Arquitecto para indicarle al Desarrollador dónde interactúa esta historia con la arquitectura del sistema.*

### 4.1. Modelo de Dominio y Estado

- **Entidades Afectadas:** `[Ej. Entidad "User", Entidad "SessionLog"]` (Referencia a `docs_definitive\architecture\domain_and_state_model.md`).
- **Mutaciones de Estado:** `[Ej. Transición de Estado: INVITADO -> AUTENTICADO]`.

### 4.2. Contrato de Interfaces (Triggers / API)

- **Trigger / Endpoint consumido:** `[Ej. POST /api/v1/login | Evento: 'User_Submit_Login']` (Referencia a `docs_definitive\architecture\interfaces_contract.md`).
- **Payload requerido:** `[Referenciar los campos clave que envía esta historia]`.

### 4.3. UI / Assets (Opcional)

- **Mockups / Wireframes:** `[Ruta a la imagen en la carpeta /assets, ej. ./assets/ui_mockups/login_screen.png]`
- **Componente Visual:** `[Ej. Módulo de Formulario de Autenticación en frontend]`
