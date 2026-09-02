/*
 * Árbol de entrenamiento en 3D — generación procedural (HU-38).
 *
 * El árbol se construye entero por código con primitivas de Three.js. No hay ningún modelo
 * externo: ni .glb ni .gltf, ni texturas (frontera técnica declarada por el PO).
 *
 * Contrato con el lado nativo (CA-38.04):
 *   nativo → web : window.tensionTree.setState(healthScore, stageCode)
 *   web → nativo : TreeBridge.onReady() | TreeBridge.onFailure(reason)
 *
 * La calidad de render y el tema no viajan por setState: son propiedades del dispositivo y del
 * sistema, se fijan una vez y entran como query string de la URL.
 *
 * Comentarios en español, identificadores en inglés (§2.1 de los estándares del proyecto).
 */
(function () {
    'use strict';

    // ── Puente con el lado nativo ───────────────────────────────────────────────────────────

    /**
     * El puente puede no existir: este archivo también se abre en un navegador para depurar la
     * geometría. Su ausencia no es un error, solo significa que nadie está escuchando.
     */
    function reportReady() {
        if (window.TreeBridge && window.TreeBridge.onReady) {
            window.TreeBridge.onReady();
        }
    }

    function reportFailure(reason) {
        if (window.TreeBridge && window.TreeBridge.onFailure) {
            window.TreeBridge.onFailure(String(reason));
        }
    }

    // Cualquier error que escape de los try/catch acaba en el fallback nativo (CA-38.05).
    window.onerror = function (message) {
        reportFailure('window.onerror: ' + message);
        return true;
    };

    // ── Configuración por calidad (CA-38.06, D6) ────────────────────────────────────────────

    /*
     * El orden de degradación es el del preview de la historia:
     *   1. sin sombras  2. menos esferas en la copa  3. menos segmentos en el tronco
     *   4. menos polígonos por primitiva
     * Cada escalón lo materializa una columna de esta tabla.
     */
    var QUALITY_PRESETS = {
        high: {
            name: 'high',
            shadows: true,
            foliageCount: 7,
            trunkRadialSegments: 12,
            foliageDetail: 1,
            maxPixelRatio: 2.0,
            antialias: true
        },
        medium: {
            name: 'medium',
            shadows: false,
            foliageCount: 5,
            trunkRadialSegments: 8,
            foliageDetail: 1,
            maxPixelRatio: 1.5,
            antialias: true
        },
        low: {
            name: 'low',
            shadows: false,
            foliageCount: 3,
            trunkRadialSegments: 6,
            foliageDetail: 0,
            maxPixelRatio: 1.0,
            antialias: false
        }
    };

    /** Escalón inmediatamente inferior, para la degradación por medida. */
    var QUALITY_DOWNGRADE = { high: 'medium', medium: 'low', low: null };

    // ── Paleta (D7) ─────────────────────────────────────────────────────────────────────────

    /*
     * Los cinco colores son exactamente los de ui/theme/Color.kt:153-165. Aquí se usan como
     * paradas de un degradado en vez de bandas: en salud 0, 25, 50 y 100 el resultado es
     * idéntico al del ícono nativo, y entre esos puntos interpola (CA-38.02 exige continuidad).
     *
     * Si esos hexadecimales cambian en Color.kt, tienen que cambiar aquí. La duplicación es el
     * precio de que el puente lleve dos parámetros y no cinco colores.
     */
    var HEALTH_STOPS_LIGHT = [
        { t: 0.00, color: 0x5D4037 },   // TreeWitheredLight
        { t: 0.25, color: 0x8D5524 },   // TreeWitheringLight
        { t: 0.50, color: 0x8D6E00 },   // TreeDryLight
        { t: 1.00, color: 0x2E7D32 }    // TreeHealthyLight
    ];

    var HEALTH_STOPS_DARK = [
        { t: 0.00, color: 0xA1887F },   // TreeWitheredDark
        { t: 0.25, color: 0xD2A679 },   // TreeWitheringDark
        { t: 0.50, color: 0xFFD54F },   // TreeDryDark
        { t: 1.00, color: 0x81C784 }    // TreeHealthyDark
    ];

    /** El tronco es marrón siempre: es madera, no follaje. Es la parada de salud 0. */
    var TRUNK_COLOR_LIGHT = 0x5D4037;
    var TRUNK_COLOR_DARK = 0xA1887F;

    // ── Geometría y cámara ──────────────────────────────────────────────────────────────────

    var TRUNK_HEIGHT = 1.60;
    var TRUNK_RADIUS_BOTTOM = 0.20;
    var TRUNK_RADIUS_TOP = 0.13;
    var CROWN_RADIUS = 0.95;
    var BRANCH_COUNT = 3;
    var MOUND_RADIUS = 0.55;

    /*
     * Escala por etapa y encuadre de la cámara (D8).
     *
     * La escala del modelo es lo que expresa la etapa. La distancia de cámara acompaña para que
     * una semilla no sea una mota invisible en 180 dp, pero acompaña **menos** de lo que crece
     * el árbol: la proporción del modelo dentro del cuadro sube de 0.52 en brote a 0.73 en
     * maduro, así que el crecimiento sigue siendo visible.
     */
    var STAGE_PRESETS = {
        SEED:   { scale: 1.00, cameraRadius: 2.20, targetY: 0.30, seed: true },
        SPROUT: { scale: 0.55, cameraRadius: 3.40, targetY: 0.75, seed: false },
        YOUNG:  { scale: 0.78, cameraRadius: 3.90, targetY: 0.95, seed: false },
        MATURE: { scale: 1.00, cameraRadius: 4.40, targetY: 1.15, seed: false }
    };

    /** Un código de etapa desconocido cae en SEED, igual que TreeGrowthStage.fromCode. */
    var DEFAULT_STAGE = 'SEED';

    var FOV = 34;

    // Órbita horizontal libre; elevación acotada entre -10° y +55° (CA-38.03, D9).
    var PHI_MIN = degToRad(35);
    var PHI_MAX = degToRad(100);
    var PHI_INITIAL = degToRad(78);
    var THETA_INITIAL = degToRad(35);

    // Zoom acotado en proporción al encuadre de la etapa. El mínimo queda muy por encima del
    // radio de la copa, así que la cámara no puede atravesar el árbol.
    var ZOOM_MIN_FACTOR = 0.60;
    var ZOOM_MAX_FACTOR = 1.60;

    var ROTATE_SPEED = 0.010;

    /** Caída máxima de las ramas con salud cero. */
    var BRANCH_DROOP_MAX = degToRad(35);

    /** Por debajo de esta escala el follaje se oculta: con salud cero no hay copa. */
    var FOLIAGE_MIN_VISIBLE = 0.02;

    // ── Presupuesto de rendimiento (CA-38.06) ───────────────────────────────────────────────

    /** Fotogramas de calentamiento que no entran en la medida. */
    var PROBE_WARMUP_FRAMES = 5;

    /** Fotogramas medidos antes de decidir si hay que degradar. */
    var PROBE_FRAMES = 30;

    /** Presupuesto por fotograma. Por encima de esto se baja un escalón, una sola vez. */
    var PROBE_BUDGET_MS = 22;

    /** Duración de la transición entre estados de salud (D11). */
    var TRANSITION_MS = 900;

    // ── Estado del módulo ───────────────────────────────────────────────────────────────────

    var renderer = null;
    var scene = null;
    var camera = null;
    var root = null;          // grupo del árbol completo, escalado por etapa
    var trunkGroup = null;    // tronco + ramas
    var foliageGroup = null;  // copa, escalada por salud
    var seedGroup = null;     // representación de la etapa Semilla
    var moundMesh = null;
    var shadowPlane = null;
    var directionalLight = null;

    var quality = QUALITY_PRESETS.low;
    var healthStops = HEALTH_STOPS_LIGHT;
    var trunkColorHex = TRUNK_COLOR_LIGHT;

    var stageCode = DEFAULT_STAGE;
    var health = 0;           // salud aplicada, 0..1
    var healthFrom = 0;       // origen de la transición en curso
    var healthTo = 0;         // destino de la transición en curso
    var transitionStart = 0;
    var transitioning = false;
    var hasState = false;     // el primer setState se aplica instantáneo (D11)

    var theta = THETA_INITIAL;
    var phi = PHI_INITIAL;
    var radius = STAGE_PRESETS[DEFAULT_STAGE].cameraRadius;

    var frameRequested = false;
    var probeFrame = 0;
    var probeElapsed = 0;
    var probeLastTime = 0;
    var probeDone = false;
    var readyReported = false;

    var dragging = false;
    var lastTouchX = 0;
    var lastTouchY = 0;
    var pinchStartDistance = 0;
    var pinchStartRadius = 0;

    // ── Utilidades ──────────────────────────────────────────────────────────────────────────

    function degToRad(degrees) {
        return degrees * Math.PI / 180;
    }

    function clamp(value, min, max) {
        return value < min ? min : (value > max ? max : value);
    }

    function now() {
        return (window.performance && window.performance.now) ? window.performance.now() : Date.now();
    }

    /** Interpolación lineal por canal entre dos colores hexadecimales. */
    function lerpHex(from, to, t) {
        var fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        var tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        var r = Math.round(fr + (tr - fr) * t);
        var g = Math.round(fg + (tg - fg) * t);
        var b = Math.round(fb + (tb - fb) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Color del follaje para una salud normalizada, interpolado entre las cuatro paradas. */
    function foliageColor(t) {
        var value = clamp(t, 0, 1);
        for (var i = 1; i < healthStops.length; i++) {
            if (value <= healthStops[i].t) {
                var lower = healthStops[i - 1];
                var upper = healthStops[i];
                var span = upper.t - lower.t;
                var local = span === 0 ? 0 : (value - lower.t) / span;
                return lerpHex(lower.color, upper.color, local);
            }
        }
        return healthStops[healthStops.length - 1].color;
    }

    /** Suavizado de la transición: arranca y termina despacio. */
    function easeInOut(t) {
        return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    function readQueryParams() {
        var search = window.location.search || '';
        var params = {};
        var pairs = search.replace(/^\?/, '').split('&');
        for (var i = 0; i < pairs.length; i++) {
            if (!pairs[i]) {
                continue;
            }
            var parts = pairs[i].split('=');
            params[decodeURIComponent(parts[0])] = decodeURIComponent(parts[1] || '');
        }
        return params;
    }

    // ── Construcción del árbol ──────────────────────────────────────────────────────────────

    /**
     * Posiciones de la copa, en fracciones del radio. El orden importa: los tres primeros ya
     * forman una copa reconocible por sí solos, que es lo que se muestra en calidad baja.
     */
    var FOLIAGE_LAYOUT = [
        { x: 0.00, y: 1.00, z: 0.00, r: 1.00 },
        { x: -0.62, y: 0.72, z: 0.18, r: 0.74 },
        { x: 0.60, y: 0.76, z: -0.20, r: 0.76 },
        { x: 0.12, y: 0.60, z: 0.62, r: 0.68 },
        { x: -0.18, y: 0.62, z: -0.60, r: 0.66 },
        { x: -0.42, y: 1.16, z: -0.34, r: 0.56 },
        { x: 0.46, y: 1.18, z: 0.30, r: 0.54 }
    ];

    function disposeGroup(group) {
        if (!group) {
            return;
        }
        group.traverse(function (node) {
            if (node.geometry) {
                node.geometry.dispose();
            }
            if (node.material) {
                node.material.dispose();
            }
        });
        if (group.parent) {
            group.parent.remove(group);
        }
    }

    function buildTrunk() {
        var group = new THREE.Group();
        var trunkMaterial = new THREE.MeshLambertMaterial({ color: trunkColorHex });

        var trunkGeometry = new THREE.CylinderGeometry(
            TRUNK_RADIUS_TOP,
            TRUNK_RADIUS_BOTTOM,
            TRUNK_HEIGHT,
            quality.trunkRadialSegments
        );
        var trunk = new THREE.Mesh(trunkGeometry, trunkMaterial);
        trunk.position.y = TRUNK_HEIGHT / 2;
        trunk.castShadow = quality.shadows;
        group.add(trunk);

        // Las ramas cuelgan de un pivote propio para que la caída por salud sea una rotación
        // del pivote y no una deformación de la geometría.
        for (var i = 0; i < BRANCH_COUNT; i++) {
            var pivot = new THREE.Group();
            var angle = (i / BRANCH_COUNT) * Math.PI * 2;
            pivot.position.y = TRUNK_HEIGHT * 0.72;
            pivot.rotation.y = angle;
            pivot.name = 'branchPivot';

            var branchGeometry = new THREE.CylinderGeometry(
                0.035,
                0.055,
                TRUNK_HEIGHT * 0.55,
                Math.max(4, Math.floor(quality.trunkRadialSegments / 2))
            );
            var branch = new THREE.Mesh(branchGeometry, trunkMaterial);
            branch.position.set(0.30, TRUNK_HEIGHT * 0.22, 0);
            branch.rotation.z = degToRad(-38);
            branch.castShadow = quality.shadows;
            pivot.add(branch);
            group.add(pivot);
        }

        return group;
    }

    function buildFoliage() {
        var group = new THREE.Group();
        var material = new THREE.MeshLambertMaterial({ color: foliageColor(1) });
        var count = Math.min(quality.foliageCount, FOLIAGE_LAYOUT.length);

        for (var i = 0; i < count; i++) {
            var spec = FOLIAGE_LAYOUT[i];
            var geometry = new THREE.IcosahedronGeometry(CROWN_RADIUS * spec.r * 0.62, quality.foliageDetail);
            var blob = new THREE.Mesh(geometry, material);
            blob.position.set(
                CROWN_RADIUS * spec.x,
                TRUNK_HEIGHT + CROWN_RADIUS * (spec.y - 0.35),
                CROWN_RADIUS * spec.z
            );
            blob.castShadow = quality.shadows;
            group.add(blob);
        }

        group.name = 'foliage';
        return group;
    }

    /**
     * Etapa Semilla: un brote mínimo sobre el montículo. No hay tronco ni copa, igual que el
     * drawable ic_tree_seed representa una semilla enterrada y no un árbol pequeño.
     */
    function buildSeed() {
        var group = new THREE.Group();
        var stemMaterial = new THREE.MeshLambertMaterial({ color: foliageColor(1) });

        var stemGeometry = new THREE.CylinderGeometry(0.022, 0.030, 0.34, Math.max(5, quality.trunkRadialSegments - 4));
        var stem = new THREE.Mesh(stemGeometry, stemMaterial);
        stem.position.y = 0.30;
        stem.castShadow = quality.shadows;
        group.add(stem);

        for (var i = 0; i < 2; i++) {
            var leafGeometry = new THREE.IcosahedronGeometry(0.13, quality.foliageDetail);
            var leaf = new THREE.Mesh(leafGeometry, stemMaterial);
            leaf.position.set(i === 0 ? -0.13 : 0.13, 0.46, 0);
            leaf.scale.set(1.5, 0.55, 0.9);
            leaf.castShadow = quality.shadows;
            group.add(leaf);
        }

        group.name = 'seed';
        return group;
    }

    /** El montículo de tierra. Da referencia de tamaño constante entre las cuatro etapas. */
    function buildMound() {
        var geometry = new THREE.IcosahedronGeometry(MOUND_RADIUS, quality.foliageDetail);
        var color = new THREE.Color(trunkColorHex).multiplyScalar(0.62);
        var mesh = new THREE.Mesh(geometry, new THREE.MeshLambertMaterial({ color: color }));
        mesh.position.y = -MOUND_RADIUS * 0.66;
        mesh.scale.set(1.0, 0.55, 1.0);
        return mesh;
    }

    function buildTree() {
        root = new THREE.Group();

        moundMesh = buildMound();
        root.add(moundMesh);

        trunkGroup = buildTrunk();
        root.add(trunkGroup);

        foliageGroup = buildFoliage();
        root.add(foliageGroup);

        seedGroup = buildSeed();
        root.add(seedGroup);

        scene.add(root);
    }

    function rebuildTree() {
        disposeGroup(root);
        buildTree();
        applyStage();
        applyHealth(health);
    }

    // ── Aplicación del estado ───────────────────────────────────────────────────────────────

    function stagePreset() {
        return STAGE_PRESETS[stageCode] || STAGE_PRESETS[DEFAULT_STAGE];
    }

    /** La etapa gobierna el tamaño del modelo y el encuadre. Nada más (D8). */
    function applyStage() {
        var preset = stagePreset();
        var isSeed = preset.seed;

        trunkGroup.visible = !isSeed;
        foliageGroup.visible = !isSeed;
        seedGroup.visible = isSeed;

        root.scale.setScalar(preset.scale);

        radius = clamp(radius, preset.cameraRadius * ZOOM_MIN_FACTOR, preset.cameraRadius * ZOOM_MAX_FACTOR);
    }

    /**
     * La salud gobierna el color, el tamaño del follaje y la caída de las ramas. Nada del
     * tamaño del árbol (D8). Un maduro marchito es un tronco grande y pelado.
     */
    function applyHealth(value) {
        var t = clamp(value, 0, 1);
        var color = foliageColor(t);

        foliageGroup.children.forEach(function (blob) {
            blob.material.color.setHex(color);
        });
        foliageGroup.scale.setScalar(t);
        foliageGroup.visible = !stagePreset().seed && t > FOLIAGE_MIN_VISIBLE;

        seedGroup.children.forEach(function (part) {
            part.material.color.setHex(color);
        });

        var droop = BRANCH_DROOP_MAX * (1 - t);
        trunkGroup.children.forEach(function (child) {
            if (child.name === 'branchPivot') {
                child.rotation.z = droop;
            }
        });
    }

    function updateCamera() {
        var preset = stagePreset();
        var sinPhi = Math.sin(phi);
        camera.position.set(
            radius * sinPhi * Math.sin(theta),
            radius * Math.cos(phi) + preset.targetY,
            radius * sinPhi * Math.cos(theta)
        );
        camera.lookAt(0, preset.targetY, 0);
    }

    // ── Bucle de render ─────────────────────────────────────────────────────────────────────

    /*
     * El render es **a demanda**: un árbol quieto no consume GPU. Se pide un fotograma cuando
     * el gesto mueve la cámara, cuando una transición está en curso o cuando la sonda de
     * rendimiento todavía está midiendo. Es lo que hace que el WebView no degrade el resto de
     * la aplicación mientras la pantalla está abierta (RNF01, CA-38.06).
     */
    function requestRender() {
        if (frameRequested) {
            return;
        }
        frameRequested = true;
        window.requestAnimationFrame(renderFrame);
    }

    function renderFrame() {
        frameRequested = false;
        var frameStart = now();

        if (transitioning) {
            var progress = (frameStart - transitionStart) / TRANSITION_MS;
            if (progress >= 1) {
                progress = 1;
                transitioning = false;
            }
            health = healthFrom + (healthTo - healthFrom) * easeInOut(progress);
            applyHealth(health);
        }

        updateCamera();
        renderer.render(scene, camera);

        // El primer fotograma pintado es el que cierra el presupuesto de carga: se avisa ahí,
        // no al final de la sonda, para que medir no retrase el aviso.
        if (!readyReported) {
            readyReported = true;
            reportReady();
        }

        probe(frameStart);

        if (transitioning || !probeDone) {
            requestRender();
        }
    }

    /** Mide y, si el presupuesto no se cumple, baja un escalón de calidad una sola vez (D6). */
    function probe(frameStart) {
        if (probeDone) {
            return;
        }

        probeFrame++;
        if (probeFrame <= PROBE_WARMUP_FRAMES) {
            probeLastTime = frameStart;
            return;
        }

        probeElapsed += frameStart - probeLastTime;
        probeLastTime = frameStart;

        if (probeFrame < PROBE_WARMUP_FRAMES + PROBE_FRAMES) {
            return;
        }

        probeDone = true;
        var average = probeElapsed / PROBE_FRAMES;
        if (average <= PROBE_BUDGET_MS) {
            return;
        }

        var next = QUALITY_DOWNGRADE[quality.name];
        if (!next) {
            return;
        }

        quality = QUALITY_PRESETS[next];
        renderer.shadowMap.enabled = quality.shadows;
        directionalLight.castShadow = quality.shadows;
        if (shadowPlane) {
            shadowPlane.visible = quality.shadows;
        }
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, quality.maxPixelRatio));
        rebuildTree();
        requestRender();
    }

    // ── Gestos (CA-38.03, D9) ───────────────────────────────────────────────────────────────

    function touchDistance(touches) {
        var dx = touches[0].clientX - touches[1].clientX;
        var dy = touches[0].clientY - touches[1].clientY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    function onTouchStart(event) {
        event.preventDefault();
        if (event.touches.length === 1) {
            dragging = true;
            lastTouchX = event.touches[0].clientX;
            lastTouchY = event.touches[0].clientY;
        } else if (event.touches.length === 2) {
            dragging = false;
            pinchStartDistance = touchDistance(event.touches);
            pinchStartRadius = radius;
        }
    }

    function onTouchMove(event) {
        event.preventDefault();

        if (event.touches.length === 1 && dragging) {
            var x = event.touches[0].clientX;
            var y = event.touches[0].clientY;
            theta -= (x - lastTouchX) * ROTATE_SPEED;
            phi = clamp(phi - (y - lastTouchY) * ROTATE_SPEED, PHI_MIN, PHI_MAX);
            lastTouchX = x;
            lastTouchY = y;
            requestRender();
            return;
        }

        if (event.touches.length === 2 && pinchStartDistance > 0) {
            var preset = stagePreset();
            var ratio = touchDistance(event.touches) / pinchStartDistance;
            radius = clamp(
                pinchStartRadius / ratio,
                preset.cameraRadius * ZOOM_MIN_FACTOR,
                preset.cameraRadius * ZOOM_MAX_FACTOR
            );
            requestRender();
        }
    }

    function onTouchEnd(event) {
        if (event.touches.length === 0) {
            dragging = false;
            pinchStartDistance = 0;
        }
    }

    function bindGestures(canvas) {
        canvas.addEventListener('touchstart', onTouchStart, { passive: false });
        canvas.addEventListener('touchmove', onTouchMove, { passive: false });
        canvas.addEventListener('touchend', onTouchEnd, { passive: false });
        canvas.addEventListener('touchcancel', onTouchEnd, { passive: false });
    }

    // ── API expuesta al lado nativo ─────────────────────────────────────────────────────────

    /**
     * Recibe **salud y etapa**, los dos parámetros del contrato de CA-38.04.
     *
     * El primer estado se aplica instantáneo para no gastar el presupuesto de carga en una
     * animación; los siguientes se interpolan (D11).
     */
    function setState(healthScore, code) {
        try {
            var target = clamp(Number(healthScore) / 100, 0, 1);
            var nextStage = STAGE_PRESETS[code] ? code : DEFAULT_STAGE;

            if (nextStage !== stageCode) {
                stageCode = nextStage;
                applyStage();
            }

            if (!hasState) {
                hasState = true;
                health = target;
                healthFrom = target;
                healthTo = target;
                transitioning = false;
                applyHealth(health);
            } else if (Math.abs(target - healthTo) > 0.001) {
                healthFrom = health;
                healthTo = target;
                transitionStart = now();
                transitioning = true;
            }

            requestRender();
        } catch (error) {
            reportFailure('setState: ' + error);
        }
    }

    // ── Inicialización ──────────────────────────────────────────────────────────────────────

    function resize() {
        var width = window.innerWidth;
        var height = window.innerHeight;
        if (width === 0 || height === 0) {
            return;
        }
        renderer.setSize(width, height, false);
        camera.aspect = width / height;
        camera.updateProjectionMatrix();

        // Nada se pinta antes del primer setState: el fotograma inicial tiene que salir ya con
        // la salud y la etapa correctas, porque es el que dispara onReady y con él la aparición
        // del WebView sobre el ícono nativo. Pintar antes mostraría un árbol marchito durante
        // el fundido (D4, D11).
        if (hasState) {
            requestRender();
        }
    }

    function init() {
        if (typeof THREE === 'undefined') {
            reportFailure('THREE no está definido');
            return;
        }

        var canvas = document.getElementById('tree-canvas');
        if (!canvas) {
            reportFailure('canvas ausente');
            return;
        }

        var params = readQueryParams();
        quality = QUALITY_PRESETS[params.quality] || QUALITY_PRESETS.medium;

        var isDark = params.dark === 'true';
        healthStops = isDark ? HEALTH_STOPS_DARK : HEALTH_STOPS_LIGHT;
        trunkColorHex = isDark ? TRUNK_COLOR_DARK : TRUNK_COLOR_LIGHT;

        // alpha + clearAlpha 0: el fondo nativo se ve a través del WebView (CA-38.04, RNF23).
        renderer = new THREE.WebGLRenderer({
            canvas: canvas,
            alpha: true,
            antialias: quality.antialias
        });
        renderer.setClearColor(0x000000, 0);
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, quality.maxPixelRatio));
        renderer.shadowMap.enabled = quality.shadows;

        scene = new THREE.Scene();
        camera = new THREE.PerspectiveCamera(FOV, 1, 0.1, 100);

        scene.add(new THREE.AmbientLight(0xFFFFFF, isDark ? 0.72 : 0.66));

        directionalLight = new THREE.DirectionalLight(0xFFFFFF, isDark ? 0.62 : 0.78);
        directionalLight.position.set(2.4, 5.0, 3.2);
        directionalLight.castShadow = quality.shadows;
        scene.add(directionalLight);

        // La sombra necesita una superficie que la reciba, y esa superficie no puede pintar
        // fondo: ShadowMaterial dibuja solo la sombra y deja pasar el resto.
        shadowPlane = new THREE.Mesh(
            new THREE.PlaneGeometry(6, 6),
            new THREE.ShadowMaterial({ opacity: isDark ? 0.10 : 0.18 })
        );
        shadowPlane.rotation.x = -Math.PI / 2;
        shadowPlane.position.y = -MOUND_RADIUS * 0.30;
        shadowPlane.receiveShadow = true;
        shadowPlane.visible = quality.shadows;
        scene.add(shadowPlane);

        buildTree();
        applyStage();
        applyHealth(0);

        bindGestures(canvas);
        window.addEventListener('resize', resize);

        resize();
    }

    window.tensionTree = { setState: setState };

    try {
        init();
    } catch (error) {
        reportFailure('init: ' + error);
    }
}());
