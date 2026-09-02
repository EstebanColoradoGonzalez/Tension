# Registro de Cambios — Historia #38

| Fecha | Fase | Descripción | Autor |
|-------|------|-------------|-------|
| 2026-09-02 | Creación | Historia creada como **hija 2 de 2** de la partición de *Árbol de progreso del entrenamiento* (HU-37 original, 16 CAs). Score INVEST 4/6, falla `Small`; complejidad Alta en integraciones externas. Aísla el riesgo del primer WebView del proyecto y de Three.js. 7 CAs. | Esteban Colorado González (PO) |
| 2026-09-02 | Creación | Dependencia dura declarada hacia `HU-37`: hereda entidad, cálculo, ruta, pantalla dedicada, bloque de estado y respaldo. Esta historia **solo** sustituye el área del árbol. | Esteban Colorado González (PO) |
| 2026-09-02 | Creación | Decisión del PO: **el presupuesto de rendimiento (<1s) manda sobre la fidelidad visual**. Ante conflicto se degradan los gráficos por código —sin sombras, menos esferas, menos polígonos— antes que entregar una experiencia trabada. | Esteban Colorado González (PO) |
| 2026-09-02 | Creación | La representación nativa de `HU-37` pasa a **fallback permanente**, no se elimina. Se añade requisito explícito de gestión del ciclo de vida del WebView para evitar fugas de memoria. | Esteban Colorado González (PO) |
| 2026-09-02 | Creación | Verificado sobre el repositorio: **no existe ningún WebView** en el código actual. Esta historia introduce el primero, y la primera dependencia JavaScript del proyecto. | Esteban Colorado González (PO) |
| 2026-09-02 | Creación | Preview de interfaz generado en formato ASCII: `38.preview.txt` | Esteban Colorado González (PO) |
| 2026-09-02 | Creación | Mapeada en `story_mapping_index.md` §2 bajo **EPIC-09** y **Release 1.5**. | Esteban Colorado González (PO) |
| 2026-09-02 | Creación | **Fase Creación HU cerrada** (2026-09-02 00:33). Estado: Borrador (PO) — lista para análisis arquitectónico, después de HU-37. | Esteban Colorado González (PO) |
