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

    /**
     * Deja constancia del presupuesto realmente usado (CA-38.06).
     *
     * Los mensajes de consola del WebView llegan a logcat bajo la etiqueta `chromium`, así que
     * esto es observable con `adb logcat -s chromium` sin depurador ni cliente extra. Existe
     * porque la degradación por medida era invisible: `tree.js` podía bajar un escalón a los
     * pocos segundos y no había forma de saber con qué calidad se estaba dibujando, ni de
     * comprobar que el presupuesto se cumple en un dispositivo concreto.
     */
    function reportBudget(evento) {
        var segmentos = branchNodes ? branchNodes.length : 0;
        var hojas = foliageSpecs ? foliageSpecs.length : 0;
        console.log(
            '[tree] ' + evento +
            ' etapa=' + stageCode +
            ' calidad=' + quality.name +
            ' segmentos=' + segmentos +
            ' hojas=' + hojas +
            ' sombras=' + quality.shadows
        );
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
     *   1. sin sombras  2. menos hojas por punta  3. menos segmentos en el tronco
     *   4. menos polígonos por primitiva
     * Cada escalón lo materializa una columna de esta tabla, y la ramificación añade el suyo.
     *
     * Los valores son **topes, no la forma**. Cuántos niveles de ramificación tiene el árbol
     * lo decide su etapa: la calidad solo recorta ese número cuando el dispositivo no da para
     * tanto. Un árbol maduro en calidad baja sigue siendo un árbol maduro, con menos detalle.
     *
     * `maxBranchDepth` es el tope que más pesa —cada nivel multiplica los segmentos—, así que
     * es lo primero que se recorta. `junctions` son las esferas que tapan la unión entre una
     * rama y su padre: en calidad baja se prescinde de ellas y se acepta la costura, porque es
     * el único detalle cuya ausencia no deja un hueco por el que se vea el interior.
     */
    var QUALITY_PRESETS = {
        high: {
            name: 'high',
            shadows: true,
            maxBranchDepth: 4,
            maxFoliagePerTip: 4,
            junctions: true,
            trunkRadialSegments: 10,
            foliageDetail: 1,
            maxPixelRatio: 2.0,
            antialias: true
        },
        medium: {
            name: 'medium',
            shadows: false,
            maxBranchDepth: 3,
            maxFoliagePerTip: 3,
            junctions: true,
            trunkRadialSegments: 8,
            foliageDetail: 1,
            maxPixelRatio: 1.5,
            antialias: true
        },
        low: {
            name: 'low',
            shadows: false,
            maxBranchDepth: 2,
            maxFoliagePerTip: 2,
            junctions: false,
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

    var MOUND_RADIUS = 0.62;

    /*
     * Base del árbol (D12).
     *
     * El tronco **se hunde** en el montículo en lugar de apoyarse encima. Antes no se tocaban:
     * la cúpula terminaba en y = -0.06 y el tronco arrancaba en y = 0, así que entre ambos
     * quedaba un hueco por el que se veía el fondo desde cualquier ángulo bajo. Enterrarlo es
     * lo que hace que la unión no pueda tener costura, porque deja de haber unión que ver.
     *
     * El ensanchamiento de la base imita el pie de un árbol real y, de paso, cubre el anillo
     * donde el tronco atraviesa la tierra. Ambos son proporcionales al grosor del tronco, que
     * depende de la etapa: un tallo de plántula con el pie de un roble sería una seta.
     */
    var TRUNK_BURY_MIN = 0.16;
    var TRUNK_BURY_FACTOR = 1.8;
    var ROOT_FLARE_RADIUS = 1.55;
    var ROOT_FLARE_HEIGHT_FACTOR = 1.7;

    /** Centro del montículo. Su cúspide queda por encima del arranque visible del tronco. */
    var MOUND_CENTER_Y = -0.20;
    var MOUND_FLATTEN = 0.58;

    // ── Ramificación recursiva (D12) ────────────────────────────────────────────────────────

    /**
     * Semilla fija del generador.
     *
     * La forma del árbol **no puede cambiar entre reconstrucciones**: `rebuildTree()` se vuelve
     * a llamar cuando la sonda de rendimiento degrada la calidad, unos segundos después de
     * abrir la pantalla. Con azar sin semilla el ejecutante vería su árbol convertirse en otro
     * árbol distinto delante de él. La irregularidad es deliberada; la aleatoriedad, no.
     */
    var BRANCH_SEED = 20260902;

    /** Hijos por nudo a partir del tronco. El primer reparto lo fija la etapa. */
    var BRANCH_SPLIT = 2;


    /**
     * Las ramas adelgazan despacio.
     *
     * Un decaimiento agresivo deja las puntas con grosor de alambre, y en el árbol marchito
     * —donde la ramificación es todo lo que se ve— eso lo convierte en una maraña de hilos en
     * lugar de un árbol sin hojas.
     */
    var BRANCH_RADIUS_DECAY = 0.74;

    /** Margen de variación de la apertura, cuyo valor base fija la etapa. */
    var BRANCH_SPREAD_JITTER = degToRad(13);
    var BRANCH_ROLL_JITTER = degToRad(26);
    var BRANCH_LENGTH_JITTER = 0.22;

    /**
     * Los hijos nacen algo antes de la punta del padre.
     *
     * Nacer justo en la punta deja los cilindros tocándose por una arista y se ve el hueco;
     * solapándolos, la esfera de unión tiene material a ambos lados que cubrir.
     */
    var BRANCH_ATTACH = 0.88;

    /** En el tronco el reparto sube casi hasta la punta, para que el fuste se lea largo. */
    var TRUNK_ATTACH = 0.94;

    /** Radio de la esfera que tapa cada bifurcación, en múltiplos del radio de la rama. */
    var JUNCTION_SCALE = 1.22;

    /** Dispersión de las hojas alrededor de la punta que las sostiene. */
    var FOLIAGE_TIP_SPREAD = 0.34;

    /*
     * Forma por etapa (D8, D13).
     *
     * **La etapa es una forma, no un tamaño.** Un brote no es un árbol maduro visto de lejos:
     * es una plántula con un tallo fino, apenas una bifurcación y unas pocas hojas grandes en
     * proporción. Un joven tiene tronco esbelto, ramas más verticales y copa estrecha. Un
     * maduro tiene el tronco grueso, cuatro niveles de ramificación y la copa ancha. Escalar un
     * único modelo producía cuatro veces la misma silueta, y el crecimiento no se leía.
     *
     * De ahí se sigue algo que antes no ocurría: **marchitarse respeta la edad**. Al quitarle
     * las hojas a un brote queda un tallito pelado, y a un maduro un árbol desnudo y ramificado.
     * El marchitado no tiene que saber nada de la etapa; le basta con actuar sobre la forma que
     * haya.
     *
     * `frameFill` es la fracción del cuadro que ocupa el árbol en reposo. Sigue creciendo con la
     * etapa para que el tamaño acompañe a la forma, pero ya no es lo único que las distingue.
     * La distancia de cámara se deriva de la geometría real (ver `collectFitSamples`), así que
     * cambiar cualquier parámetro de forma no obliga a reajustar ninguna cámara.
     */
    var STAGE_PRESETS = {
        SEED: {
            seed: true,
            frameFill: 0.34,
            moundScale: 0.52
        },
        SPROUT: {
            seed: false,
            frameFill: 0.46,
            moundScale: 0.60,
            // Una plántula: tallo fino que se abre una sola vez, con dos hojas y poco más.
            branchDepth: 1,
            trunkSplit: 2,
            trunkHeight: 0.78,
            trunkRadius: 0.032,
            spread: degToRad(44),
            firstSplitDecay: 0.46,
            lengthDecay: 0.70,
            foliagePerTip: 1,
            foliageScale: 0.78,
            // Muy aplanadas: a este tamaño una esfera es un caramelo, no una hoja.
            foliageFlatten: 0.34,
            // Sin madera que la sostenga, una plántula se vence entera.
            droopMax: degToRad(56)
        },
        YOUNG: {
            seed: false,
            frameFill: 0.68,
            moundScale: 0.80,
            // Árbol joven: esbelto y vertical, copa estrecha que todavía no se ha abierto.
            branchDepth: 3,
            trunkSplit: 3,
            trunkHeight: 1.35,
            trunkRadius: 0.095,
            spread: degToRad(26),
            firstSplitDecay: 0.48,
            lengthDecay: 0.72,
            foliagePerTip: 3,
            foliageScale: 0.92,
            foliageFlatten: 0.78,
            droopMax: degToRad(40)
        },
        MATURE: {
            seed: false,
            frameFill: 0.90,
            moundScale: 1.00,
            // Maduro: tronco grueso y corto en proporción, ramificación densa y copa ancha.
            branchDepth: 4,
            trunkSplit: 3,
            trunkHeight: 1.60,
            trunkRadius: 0.20,
            spread: degToRad(44),
            firstSplitDecay: 0.56,
            lengthDecay: 0.70,
            foliagePerTip: 4,
            foliageScale: 0.95,
            foliageFlatten: 0.92,
            // La madera vieja aguanta: un maduro marchito se despeina, no se derrumba.
            droopMax: degToRad(30)
        }
    };

    /** Un código de etapa desconocido cae en SEED, igual que TreeGrowthStage.fromCode. */
    var DEFAULT_STAGE = 'SEED';

    var FOV = 34;

    // Órbita horizontal libre; elevación acotada entre -10° y +55° (CA-38.03, D9).
    var PHI_MIN = degToRad(35);
    var PHI_MAX = degToRad(100);
    var PHI_INITIAL = degToRad(78);
    var THETA_INITIAL = degToRad(35);

    /*
     * Zoom acotado (CA-38.03).
     *
     * El mínimo **no es un factor**: es la distancia a la que el árbol cabe exacto en el cuadro,
     * calculada de su geometría. Así el acercamiento máximo deja el árbol tocando los bordes y
     * ni lo recorta ni permite atravesarlo, que es literalmente lo que pide el criterio.
     * Un factor fijo no podía garantizarlo, porque no sabe cuánto mide el árbol.
     */
    var ZOOM_MAX_FACTOR = 1.60;

    /**
     * Rejilla de ángulos que se muestrea al calcular el encuadre.
     *
     * El encuadre tiene que valer para **cualquier** ángulo al que el ejecutante pueda girar la
     * cámara, no solo para el inicial: si se calculara con la vista de partida, inclinar el
     * árbol lo sacaría del cuadro. Se toma la distancia más exigente de la rejilla.
     *
     * El giro se muestrea fino aunque el modelo sea casi simétrico en torno a su eje, porque lo
     * que se mide no es el modelo sino su **volumen envolvente**, y las esquinas de una caja
     * sobresalen justo en las diagonales: con la huella cuadrada de este árbol, el peor caso
     * está a 45° y un muestreo grueso lo saltaría por completo. La elevación varía poco —menos
     * de un 10% entre los extremos—, así que siete muestras la cubren de sobra.
     */
    var FRAME_PHI_SAMPLES = 7;
    var FRAME_THETA_SAMPLES = 16;

    var ROTATE_SPEED = 0.010;


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

    /*
     * El esqueleto y las mallas instanciadas que lo dibujan.
     *
     * El esqueleto es una jerarquía de `Object3D` **sin geometría**: solo nudos con su posición
     * y su rotación. Three.js compone sus matrices gratis, así que la caída de las ramas sigue
     * siendo una rotación por nudo y no un recálculo a mano de la forma.
     *
     * El dibujo va aparte, en tres `InstancedMesh` que leen la matriz de cada nudo. Es lo que
     * permite subir de 12 a más de un centenar de piezas **bajando** las llamadas de dibujo de
     * 12 a 5: sin instanciar, cada rama y cada hoja sería una llamada propia, y ahí sí se
     * notaría en un dispositivo de gama baja.
     */
    /** Forma resuelta de la etapa en curso, o `null` en Semilla. La fija `buildTree`. */
    var form = null;

    var branchNodes = [];     // nudos con segmento: {node, length, radius, level}
    var foliageSpecs = [];    // hojas: {node, offset, radius}
    var branchMesh = null;
    var junctionMesh = null;
    var foliageMesh = null;
    var trunkMaterial = null;
    var foliageMaterial = null;

    /** Objetos reutilizados al componer matrices, para no asignar memoria por fotograma. */
    var tmpMatrix = null;
    var tmpScale = null;
    var tmpOffset = null;
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

    /** Distancia a la que el árbol de la etapa actual cabe exacto. Es el tope de acercamiento. */
    var stageFitRadius = 0;

    /** Distancia en reposo: [stageFitRadius] repartida por el `frameFill` de la etapa. */
    var stageBaseRadius = 0;

    /** Altura del centro del árbol. Es a donde mira la cámara, y sale del volumen envolvente. */
    var stageTargetY = 0;

    var radius = 0;

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
    /** Estrechamiento de cada segmento respecto a su base. */
    var SEGMENT_TAPER = 0.68;

    /**
     * Generador pseudoaleatorio con semilla (congruencial lineal).
     *
     * `Math.random` no sirve aquí: `rebuildTree()` se vuelve a llamar cuando la sonda de
     * rendimiento degrada la calidad, unos segundos después de abrir la pantalla, y el
     * ejecutante vería su árbol convertirse en otro árbol distinto delante de él. La
     * irregularidad de la ramificación es deliberada; la aleatoriedad entre reconstrucciones,
     * no. Con semilla fija el árbol es siempre el mismo y sigue sin parecer geométrico.
     */
    function seededRandom(seed) {
        var state = seed >>> 0;
        return function () {
            state = (state * 1664525 + 1013904223) >>> 0;
            return state / 4294967296;
        };
    }

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

    // ── Esqueleto ───────────────────────────────────────────────────────────────────────────

    /**
     * Hace crecer un nudo y, recursivamente, sus hijos.
     *
     * Un nudo es un `Object3D` **sin geometría** que define dónde empieza un segmento y hacia
     * dónde apunta; el segmento se extiende por su +Y local. Separar el esqueleto del dibujo es
     * lo que permite que la caída por salud siga siendo una rotación por nudo —Three.js compone
     * las matrices— mientras el dibujo va en mallas instanciadas.
     *
     * El orden de inserción en [branchNodes] es **padre antes que hijo**, y de eso depende que
     * las matrices relativas se puedan calcular en una sola pasada.
     */
    function growBranch(node, parentEntry, level, length, radius, rnd) {
        var entry = {
            node: node,
            parentEntry: parentEntry,
            length: length,
            radius: radius,
            level: level,
            rel: new THREE.Matrix4()
        };
        branchNodes.push(entry);

        if (level >= form.branchDepth) {
            // Las hojas cuelgan de la punta real de la rama, no de posiciones fijas alrededor
            // del tronco. Es lo que cierra los huecos de la copa: la masa de follaje sigue a la
            // ramificación en lugar de flotar sobre ella.
            for (var h = 0; h < form.foliagePerTip; h++) {
                foliageSpecs.push({
                    entry: entry,
                    offset: new THREE.Vector3(
                        (rnd() - 0.5) * length * FOLIAGE_TIP_SPREAD * 2,
                        length * (0.45 + rnd() * 0.75),
                        (rnd() - 0.5) * length * FOLIAGE_TIP_SPREAD * 2
                    ),
                    radius: length * form.foliageScale * (0.78 + rnd() * 0.42)
                });
            }
            return;
        }

        var hijos = level === 0 ? form.trunkSplit : BRANCH_SPLIT;
        var rollBase = rnd() * Math.PI * 2;

        for (var i = 0; i < hijos; i++) {
            var child = new THREE.Object3D();
            // Los hijos nacen algo antes de la punta del padre: naciendo justo en el extremo,
            // los dos cilindros se tocarían por una arista y se vería el hueco entre ambos.
            child.position.y = length * (level === 0 ? TRUNK_ATTACH : BRANCH_ATTACH);
            child.rotation.order = 'YZX';
            child.rotation.y = rollBase +
                (i / hijos) * Math.PI * 2 +
                (rnd() - 0.5) * BRANCH_ROLL_JITTER;

            var spread = form.spread + (rnd() - 0.5) * BRANCH_SPREAD_JITTER * 2;
            child.rotation.z = spread;
            child.name = 'branchPivot';
            child.userData.baseRotZ = spread;
            // La caída es progresiva hacia las puntas: una rama gruesa junto al tronco apenas
            // cede, y las finas del final cuelgan del todo.
            child.userData.droopFactor = (level + 1) / Math.max(1, form.branchDepth);
            node.add(child);

            var decay = level === 0 ? form.firstSplitDecay : form.lengthDecay;
            var childLength = length * decay *
                (1 - BRANCH_LENGTH_JITTER / 2 + rnd() * BRANCH_LENGTH_JITTER);
            growBranch(child, entry, level + 1, childLength, radius * BRANCH_RADIUS_DECAY, rnd);
        }
    }

    /**
     * Tronco enterrado y ramificación colgando de él.
     *
     * El nudo raíz arranca **bajo** la cúspide del montículo, no sobre ella. Esa es la
     * corrección del hueco de la base: no hay costura que tapar porque no hay unión que ver.
     */
    function buildSkeleton() {
        branchNodes = [];
        foliageSpecs = [];

        var raiz = new THREE.Object3D();
        if (!form) {
            return raiz;
        }

        raiz.position.y = -trunkBury();

        growBranch(
            raiz,
            null,
            0,
            form.trunkHeight + trunkBury(),
            form.trunkRadius,
            seededRandom(BRANCH_SEED)
        );

        return raiz;
    }

    /**
     * Cuánto se hunde el tronco en el montículo.
     *
     * Proporcional al grosor, con un mínimo: un tallo de plántula no necesita —ni admite— el
     * mismo enterramiento que un tronco maduro, pero cualquiera de los dos tiene que entrar lo
     * suficiente para que ningún ángulo de cámara cuele la vista entre la madera y la tierra.
     */
    function trunkBury() {
        return Math.max(TRUNK_BURY_MIN, form.trunkRadius * TRUNK_BURY_FACTOR);
    }

    /**
     * Resuelve la forma de la etapa actual acotada por lo que el dispositivo aguanta.
     *
     * La etapa manda sobre la silueta; la calidad solo puede recortar. Devuelve `null` en la
     * etapa Semilla, que no usa esqueleto sino su propio grupo.
     */
    function resolveForm() {
        var preset = stagePreset();
        if (preset.seed) {
            return null;
        }
        return {
            branchDepth: Math.min(preset.branchDepth, quality.maxBranchDepth),
            foliagePerTip: Math.min(preset.foliagePerTip, quality.maxFoliagePerTip),
            trunkSplit: preset.trunkSplit,
            trunkHeight: preset.trunkHeight,
            trunkRadius: preset.trunkRadius,
            spread: preset.spread,
            firstSplitDecay: preset.firstSplitDecay,
            lengthDecay: preset.lengthDecay,
            foliageScale: preset.foliageScale,
            foliageFlatten: preset.foliageFlatten,
            droopMax: preset.droopMax
        };
    }

    // ── Dibujo instanciado ──────────────────────────────────────────────────────────────────

    /**
     * Una malla instanciada por familia de piezas: segmentos, uniones y hojas.
     *
     * `frustumCulled` se apaga porque el volumen envolvente de una malla instanciada describe
     * la geometría unitaria, no dónde acaban sus instancias: dejarlo activo hace desaparecer el
     * árbol entero en cuanto la cámara gira.
     */
    function buildInstancedMeshes() {
        var segmentos = quality.trunkRadialSegments;

        var branchGeometry = new THREE.CylinderGeometry(SEGMENT_TAPER, 1, 1, segmentos, 1, false);
        branchMesh = new THREE.InstancedMesh(
            branchGeometry,
            trunkMaterial,
            Math.max(1, branchNodes.length)
        );
        branchMesh.castShadow = quality.shadows;
        branchMesh.frustumCulled = false;
        trunkGroup.add(branchMesh);

        if (quality.junctions) {
            var junctionGeometry = new THREE.IcosahedronGeometry(1, quality.foliageDetail);
            junctionMesh = new THREE.InstancedMesh(
                junctionGeometry,
                trunkMaterial,
                Math.max(1, branchNodes.length)
            );
            junctionMesh.castShadow = quality.shadows;
            junctionMesh.frustumCulled = false;
            trunkGroup.add(junctionMesh);
        } else {
            junctionMesh = null;
        }

        var foliageGeometry = new THREE.IcosahedronGeometry(1, quality.foliageDetail);
        foliageMesh = new THREE.InstancedMesh(
            foliageGeometry,
            foliageMaterial,
            Math.max(1, foliageSpecs.length)
        );
        foliageMesh.castShadow = quality.shadows;
        foliageMesh.frustumCulled = false;
        foliageGroup.add(foliageMesh);
    }

    /**
     * Aplica la caída a todos los pivotes del esqueleto.
     *
     * @param amount 0 = ramas erguidas, 1 = caída máxima.
     */
    function applyDroop(amount) {
        if (!form) {
            return;
        }
        trunkGroup.traverse(function (child) {
            if (child.name === 'branchPivot') {
                child.rotation.z = child.userData.baseRotZ +
                    form.droopMax * amount * child.userData.droopFactor;
            }
        });
    }

    /**
     * Vuelca el esqueleto en las matrices de instancia.
     *
     * Las matrices se calculan **relativas a la raíz del modelo**, componiéndolas a mano en una
     * sola pasada. Usar `matrixWorld` ataría el resultado a la escala de etapa que lleva `root`
     * y habría que deshacerla después, porque Three.js ya multiplica la matriz de la malla por
     * la de cada instancia.
     *
     * @param foliageT tamaño del follaje, 0..1. Se pasa aparte de la salud para poder medir el
     *   encuadre con la copa completa sin tocar el estado visible.
     */
    function updateSkeletonMatrices(foliageT) {
        var i;
        var entry;

        for (i = 0; i < branchNodes.length; i++) {
            entry = branchNodes[i];
            entry.node.updateMatrix();
            if (entry.parentEntry) {
                entry.rel.multiplyMatrices(entry.parentEntry.rel, entry.node.matrix);
            } else {
                entry.rel.copy(entry.node.matrix);
            }

            // El cilindro unitario está centrado en el origen, así que hay que subirlo media
            // longitud para que arranque en el nudo y termine en la punta.
            tmpOffset.set(0, entry.length / 2, 0);
            tmpScale.set(entry.radius, entry.length, entry.radius);
            tmpMatrix.identity().makeTranslation(tmpOffset.x, tmpOffset.y, tmpOffset.z);
            tmpMatrix.scale(tmpScale);
            tmpMatrix.premultiply(entry.rel);
            branchMesh.setMatrixAt(i, tmpMatrix);

            if (junctionMesh) {
                var junctionRadius = entry.radius * JUNCTION_SCALE;
                tmpMatrix.identity();
                tmpMatrix.makeScale(junctionRadius, junctionRadius, junctionRadius);
                tmpMatrix.premultiply(entry.rel);
                junctionMesh.setMatrixAt(i, tmpMatrix);
            }
        }
        // La cuenta se fija aquí y no al crear la malla: en Semilla no hay esqueleto y una
        // malla instanciada con capacidad reservada pero sin instancias válidas dibujaría
        // basura en el origen.
        branchMesh.count = branchNodes.length;
        branchMesh.instanceMatrix.needsUpdate = true;
        if (junctionMesh) {
            junctionMesh.count = branchNodes.length;
            junctionMesh.instanceMatrix.needsUpdate = true;
        }

        // Al perder salud la copa no solo encoge: se contrae hacia las ramas. Encogiendo cada
        // hoja en su sitio se abrirían huecos entre ellas justo a media salud, que es
        // exactamente lo que la copa no debe tener.
        var offsetScale = 0.62 + 0.38 * foliageT;
        for (i = 0; i < foliageSpecs.length; i++) {
            var spec = foliageSpecs[i];
            var blobRadius = spec.radius * foliageT;
            tmpMatrix.identity().makeTranslation(
                spec.offset.x * offsetScale,
                spec.offset.y * offsetScale,
                spec.offset.z * offsetScale
            );
            // Aplanar la hoja es lo que la distingue de una bola. En una plántula la
            // diferencia entre dos cotiledones y un caramelo verde es exactamente esto.
            tmpMatrix.scale(tmpScale.set(
                blobRadius,
                blobRadius * form.foliageFlatten,
                blobRadius
            ));
            tmpMatrix.premultiply(spec.entry.rel);
            foliageMesh.setMatrixAt(i, tmpMatrix);
        }
        foliageMesh.count = foliageSpecs.length;
        foliageMesh.instanceMatrix.needsUpdate = true;
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

    /**
     * El montículo de tierra. Da referencia de tamaño constante entre las cuatro etapas.
     *
     * Su cúspide queda **por encima** del arranque visible del tronco, que a su vez nace
     * enterrado. Antes la cúpula terminaba en y = -0.06 y el tronco empezaba en y = 0: entre
     * ambos quedaba un hueco de aire por el que se veía el fondo en cuanto la cámara bajaba.
     */
    function buildMound() {
        var geometry = new THREE.IcosahedronGeometry(moundRadius(), quality.foliageDetail + 1);
        var color = new THREE.Color(trunkColorHex).multiplyScalar(0.62);
        var mesh = new THREE.Mesh(geometry, new THREE.MeshLambertMaterial({ color: color }));
        mesh.position.y = moundCenterY();
        mesh.scale.set(1.0, MOUND_FLATTEN, 1.0);
        mesh.castShadow = quality.shadows;
        return mesh;
    }

    /*
     * El montículo crece con la etapa, pero **menos que el árbol**.
     *
     * Constante aplastaba a la plántula: el árbol dejaba de ser el asunto del cuadro y el
     * encuadre se calculaba casi sobre la tierra. Creciendo a la par no diría nada, porque la
     * proporción entre ambos no cambiaría. Creciendo por detrás, la tierra hace de referencia y
     * el árbol se ve ganarle terreno conforme madura.
     */
    function moundRadius() {
        return MOUND_RADIUS * stagePreset().moundScale;
    }

    function moundCenterY() {
        return MOUND_CENTER_Y * stagePreset().moundScale;
    }

    /**
     * Ensanchamiento del pie del tronco.
     *
     * Imita el arranque de raíces de un árbol real y, de paso, engorda el volumen justo donde
     * el tronco atraviesa la tierra, de modo que ningún ángulo de cámara pueda colar la vista
     * entre uno y otra.
     */
    function buildRootFlare() {
        var altura = form.trunkRadius * ROOT_FLARE_HEIGHT_FACTOR;
        var geometry = new THREE.CylinderGeometry(
            form.trunkRadius,
            form.trunkRadius * ROOT_FLARE_RADIUS,
            altura,
            quality.trunkRadialSegments
        );
        var mesh = new THREE.Mesh(geometry, trunkMaterial);
        mesh.position.y = -trunkBury() + altura / 2;
        mesh.castShadow = quality.shadows;
        return mesh;
    }

    function buildTree() {
        root = new THREE.Group();

        trunkMaterial = new THREE.MeshLambertMaterial({ color: trunkColorHex });
        foliageMaterial = new THREE.MeshLambertMaterial({ color: foliageColor(1) });

        form = resolveForm();

        moundMesh = buildMound();
        root.add(moundMesh);

        trunkGroup = new THREE.Group();
        trunkGroup.add(buildSkeleton());
        if (form) {
            trunkGroup.add(buildRootFlare());
        }
        root.add(trunkGroup);

        foliageGroup = new THREE.Group();
        foliageGroup.name = 'foliage';
        root.add(foliageGroup);

        // El esqueleto ya existe, así que las mallas conocen cuántas instancias reservar.
        buildInstancedMeshes();

        seedGroup = buildSeed();
        root.add(seedGroup);

        scene.add(root);
        reportBudget('construido');
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

        frameStage(true);
    }

    // ── Encuadre derivado de la geometría ───────────────────────────────────────────────────

    /**
     * Describe el árbol como un puñado de esferas: una por hoja y dos por rama.
     *
     * Es lo que se usa para encuadrar, en lugar del volumen envolvente. Una caja alrededor de
     * un árbol es en su mayor parte aire: sus esquinas en diagonal quedan a `√2` del eje
     * mientras la copa no pasa del radio, y encuadrar contra ellas alejaba la cámara un 40% de
     * más y dejaba el árbol pequeño en un cuadro medio vacío. Las esferas describen la silueta
     * real, así que la distancia que sale es la que de verdad hace falta.
     *
     * Se llama con el árbol en su extensión máxima —follaje completo, ramas erguidas—, porque
     * el encuadre depende de la etapa y no de la salud (CA-38.02).
     *
     * `root` no lleva transformación propia, así que `matrixWorld` y las matrices del esqueleto
     * ya están en el sistema del modelo y no hay nada que deshacer.
     */
    function collectFitSamples() {
        var samples = [];
        var i;

        // El montículo entra siempre: es la referencia de tamaño constante entre etapas, y en
        // Semilla es prácticamente lo único que hay que encuadrar.
        samples.push({ x: 0, y: moundCenterY(), z: 0, r: moundRadius() });

        if (stagePreset().seed) {
            for (i = 0; i < seedGroup.children.length; i++) {
                var mesh = seedGroup.children[i];
                if (!mesh.geometry) {
                    continue;
                }
                if (!mesh.geometry.boundingSphere) {
                    mesh.geometry.computeBoundingSphere();
                }
                var sphere = mesh.geometry.boundingSphere;
                tmpOffset.copy(sphere.center).applyMatrix4(mesh.matrixWorld);
                var escala = Math.max(mesh.scale.x, Math.max(mesh.scale.y, mesh.scale.z));
                samples.push({
                    x: tmpOffset.x,
                    y: tmpOffset.y,
                    z: tmpOffset.z,
                    r: sphere.radius * escala
                });
            }
            return samples;
        }

        for (i = 0; i < branchNodes.length; i++) {
            var entry = branchNodes[i];
            // Arranque de la rama, con el radio de su esfera de unión.
            tmpOffset.setFromMatrixPosition(entry.rel);
            samples.push({
                x: tmpOffset.x,
                y: tmpOffset.y,
                z: tmpOffset.z,
                r: entry.radius * JUNCTION_SCALE
            });
            // Punta.
            tmpOffset.set(0, entry.length, 0).applyMatrix4(entry.rel);
            samples.push({ x: tmpOffset.x, y: tmpOffset.y, z: tmpOffset.z, r: entry.radius });
        }

        for (i = 0; i < foliageSpecs.length; i++) {
            var spec = foliageSpecs[i];
            tmpOffset.copy(spec.offset).applyMatrix4(spec.entry.rel);
            samples.push({ x: tmpOffset.x, y: tmpOffset.y, z: tmpOffset.z, r: spec.radius });
        }

        return samples;
    }

    /**
     * Distancia mínima a la que todas las esferas caben en el cuadro.
     *
     * Para una orientación de cámara dada, una esfera entra en el tronco de visión si su
     * desplazamiento lateral más su radio no superan la mitad del cuadro a su profundidad.
     * Despejando la distancia queda `d >= (|lateral| + r) / tan(fov/2) - profundidad`, y basta
     * tomar el máximo sobre todas las esferas. Se repite sobre la rejilla de ángulos
     * alcanzables y se conserva el peor.
     */
    function fitRadiusForSamples(samples, centerY) {
        var tanV = Math.tan(degToRad(FOV) / 2);
        var tanH = tanV * (camera.aspect || 1);
        var required = 0;

        for (var p = 0; p < FRAME_PHI_SAMPLES; p++) {
            var phiSample = PHI_MIN + (PHI_MAX - PHI_MIN) * (p / (FRAME_PHI_SAMPLES - 1));
            var sinPhi = Math.sin(phiSample);
            var cosPhi = Math.cos(phiSample);

            for (var t = 0; t < FRAME_THETA_SAMPLES; t++) {
                var thetaSample = (Math.PI * 2) * (t / FRAME_THETA_SAMPLES);
                // Dirección del objetivo hacia la cámara, y base de la cámara a su alrededor.
                var dx = sinPhi * Math.sin(thetaSample);
                var dy = cosPhi;
                var dz = sinPhi * Math.cos(thetaSample);
                // Derecha = normalizar(arriba x dirección); con arriba = (0,1,0) sale directa.
                var rightLength = Math.sqrt(dz * dz + dx * dx) || 1;
                var rx = dz / rightLength;
                var rz = -dx / rightLength;
                // Arriba de la cámara = dirección x derecha.
                var ux = dy * rz;
                var uy = dz * rx - dx * rz;
                var uz = -dy * rx;

                for (var s = 0; s < samples.length; s++) {
                    var sample = samples[s];
                    var qx = sample.x;
                    var qy = sample.y - centerY;
                    var qz = sample.z;

                    var lateralV = Math.abs(qx * ux + qy * uy + qz * uz) + sample.r;
                    var lateralH = Math.abs(qx * rx + qz * rz) + sample.r;
                    // Profundidad medida desde el objetivo hacia adelante = -dot(q, dirección).
                    var depth = -(qx * dx + qy * dy + qz * dz);

                    required = Math.max(
                        required,
                        lateralV / tanV - depth,
                        lateralH / tanH - depth
                    );
                }
            }
        }
        return required;
    }

    /**
     * Recalcula el encuadre de la etapa actual y reencaja el zoom dentro de sus nuevos topes.
     *
     * Se llama al cambiar de etapa y al cambiar el tamaño del contenedor, que son los dos
     * únicos momentos en que el encuadre puede quedar obsoleto. Nunca al cambiar la salud.
     *
     * @param reponerDistancia `true` al cambiar de etapa, `false` al redimensionar.
     */
    function frameStage(reponerDistancia) {
        if (!camera || !root) {
            return;
        }

        var foliageVisible = foliageGroup.visible;

        // Extensión máxima: ramas erguidas y copa completa, sea cual sea la salud actual.
        applyDroop(0);
        updateSkeletonMatrices(1);
        foliageGroup.visible = !stagePreset().seed;
        root.updateMatrixWorld(true);

        var samples = collectFitSamples();

        // Devolver el modelo al estado que corresponde a la salud real.
        foliageGroup.visible = foliageVisible;
        applyHealth(health);
        root.updateMatrixWorld(true);

        if (!samples.length) {
            return;
        }

        var minY = Infinity;
        var maxY = -Infinity;
        for (var i = 0; i < samples.length; i++) {
            minY = Math.min(minY, samples[i].y - samples[i].r);
            maxY = Math.max(maxY, samples[i].y + samples[i].r);
        }
        stageTargetY = (minY + maxY) / 2;

        stageFitRadius = fitRadiusForSamples(samples, stageTargetY);
        stageBaseRadius = stageFitRadius / stagePreset().frameFill;

        // Al cambiar de etapa se repone la distancia de reposo; al cambiar el tamaño del
        // contenedor se conserva el zoom del ejecutante y solo se reajusta a los topes nuevos.
        //
        // Acotar también al cambiar de etapa era un error sutil y caro: la distancia de la
        // etapa anterior quedaba pegada al tope de acercamiento de la nueva, con lo que todas
        // las etapas terminaban ocupando el cuadro entero y el tamaño dejaba de expresar la
        // etapa, que es justo lo que CA-38.02 pide que se lea.
        radius = (reponerDistancia || radius <= 0)
            ? stageBaseRadius
            : clamp(radius, stageFitRadius, stageBaseRadius * ZOOM_MAX_FACTOR);
    }

    /**
     * La salud gobierna el color, el tamaño del follaje y la caída de las ramas. Nada del
     * tamaño del árbol (D8). Un maduro marchito es un tronco grande y pelado.
     */
    function applyHealth(value) {
        var t = clamp(value, 0, 1);
        var color = foliageColor(t);

        // Un solo material para toda la copa: las hojas comparten color, así que no hace falta
        // color por instancia y basta con cambiarlo una vez.
        foliageMaterial.color.setHex(color);
        foliageGroup.visible = !stagePreset().seed && t > FOLIAGE_MIN_VISIBLE;

        seedGroup.children.forEach(function (part) {
            part.material.color.setHex(color);
        });

        applyDroop(1 - t);
        updateSkeletonMatrices(t);
    }

    function updateCamera() {
        var sinPhi = Math.sin(phi);
        camera.position.set(
            radius * sinPhi * Math.sin(theta),
            radius * Math.cos(phi) + stageTargetY,
            radius * sinPhi * Math.cos(theta)
        );
        camera.lookAt(0, stageTargetY, 0);
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
        reportBudget('degradado');
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
            var ratio = touchDistance(event.touches) / pinchStartDistance;
            // El tope de acercamiento es la distancia a la que el árbol cabe exacto, así que ni
            // el pellizco más agresivo puede recortarlo ni meter la cámara dentro (CA-38.03).
            radius = clamp(
                pinchStartRadius / ratio,
                stageFitRadius,
                stageBaseRadius * ZOOM_MAX_FACTOR
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
                // Reconstruir y no solo reencuadrar: la etapa define la silueta —niveles de
                // ramificación, grosor del tronco, apertura de las ramas—, así que cambiarla
                // es cambiar el modelo, no la distancia desde la que se mira.
                rebuildTree();
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

        // El encuadre depende de la proporción del contenedor: la distancia que hace caber el
        // árbol en un cuadro ancho no lo hace caber en uno estrecho. Recalcularlo aquí es lo
        // que hace que el árbol quepa entero sea cual sea el área que le reserve la pantalla.
        frameStage(false);

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

        // Se crean aquí y no al declararlos porque THREE puede no existir todavía cuando el
        // módulo se evalúa. Se reutilizan en cada fotograma para no asignar memoria al componer
        // las matrices de instancia.
        tmpMatrix = new THREE.Matrix4();
        tmpScale = new THREE.Vector3();
        tmpOffset = new THREE.Vector3();

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
        // Justo bajo la base del montículo. Con el montículo agrandado, la altura
        // anterior caía dentro de él y la sombra se recortaba contra su propia tierra.
        shadowPlane.position.y = MOUND_CENTER_Y - MOUND_RADIUS * MOUND_FLATTEN;
        shadowPlane.name = 'shadowPlane';
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
