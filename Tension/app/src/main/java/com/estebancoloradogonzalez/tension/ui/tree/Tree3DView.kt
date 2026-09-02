package com.estebancoloradogonzalez.tension.ui.tree

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import kotlinx.coroutines.delay

/** Asset local. Es la única URL que este WebView carga en toda su vida (RNF09). */
private const val TREE_ASSET_URL = "file:///android_asset/tree/tree.html"

private const val BLANK_URL = "about:blank"

/**
 * Margen para el primer fotograma. Es la última red del fallback: cubre los fallos que ni el
 * cliente del WebView ni el propio JavaScript alcanzaron a reportar. Generoso respecto al
 * presupuesto de 1 segundo a propósito — un arranque en frío del WebView puede pasar de ese
 * presupuesto sin estar roto, y equivocarse aquí significa negar el 3D a quien podía verlo.
 */
private const val READY_TIMEOUT_MS = 2_500L

/**
 * El árbol como modelo tridimensional, renderizado en un WebView sobre fondo transparente.
 *
 * Es el primer y único WebView de la aplicación, y vive exclusivamente dentro de la pantalla
 * del árbol: la tarjeta de Inicio es nativa de forma permanente (RNF01).
 *
 * El composable **no decide** si se ve: siempre que se compone intenta renderizar, y avisa por
 * [onReady] cuando el primer fotograma está pintado o por [onFailure] cuando no va a haberlo.
 * Quien elige entre este render y el ícono nativo es la pantalla.
 *
 * @param healthScore 0–100. Junto con [stage], lo único que cruza el puente hacia el código web.
 * @param isDarkTheme selecciona la paleta clara u oscura del render. Viaja en la URL y no por
 *   el puente: es una propiedad del sistema, no del estado del árbol.
 * @param onFailure puede llegar **después** de [onReady] si el proceso de render muere.
 */
@Composable
fun Tree3DView(
    healthScore: Int,
    stage: TreeGrowthStage,
    isDarkTheme: Boolean,
    onReady: () -> Unit,
    onFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnFailure by rememberUpdatedState(onFailure)

    var reportedReady by remember { mutableStateOf(false) }
    var reportedFailure by remember { mutableStateOf(false) }
    var pageLoaded by remember { mutableStateOf(false) }

    val notifyReady = {
        if (!reportedReady && !reportedFailure) {
            reportedReady = true
            currentOnReady()
        }
    }
    val notifyFailure = {
        if (!reportedFailure) {
            reportedFailure = true
            currentOnFailure()
        }
    }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bridge = remember { TreeBridge(post = { block -> mainHandler.post(block) }) }

    // Construir un WebView puede fallar en un dispositivo cuyo paquete de WebView está roto,
    // aunque el sistema lo declare presente. Un null aquí es un fallback, no una excepción.
    val webView = remember {
        runCatching { createTreeWebView(context, bridge) }.getOrNull()
    }

    if (webView == null) {
        LaunchedEffect(Unit) { notifyFailure() }
        return
    }

    DisposableEffect(bridge) {
        bridge.listener = object : TreeBridge.Listener {
            override fun onReady() = notifyReady()
            override fun onFailure(reason: String) = notifyFailure()
        }
        onDispose { bridge.release() }
    }

    DisposableEffect(webView, isDarkTheme) {
        webView.webViewClient = object : WebViewClient() {

            /**
             * Bloqueo total de navegación: este WebView carga su asset local y nada más. No
             * hay contenido remoto que alcanzar —la aplicación no declara el permiso
             * `INTERNET`— y aun así la negativa se hace explícita aquí (CA-38.04).
             */
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean = true

            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null && url.startsWith(TREE_ASSET_URL)) {
                    pageLoaded = true
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                // Solo el frame principal decide: un sub-recurso que falle ya lo reporta el JS.
                if (request?.isForMainFrame == true) {
                    notifyFailure()
                }
            }

            /**
             * El proceso de render murió. Devolver `true` es lo que evita que se lleve la
             * aplicación por delante: el árbol se degrada al ícono y la pantalla sigue viva.
             */
            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?,
            ): Boolean {
                notifyFailure()
                return true
            }
        }

        val quality = resolveRenderQuality(context)
        pageLoaded = false
        webView.loadUrl("$TREE_ASSET_URL?quality=${quality.code}&dark=$isDarkTheme")

        onDispose { }
    }

    // El estado se entrega en cuanto la página está cargada, y de nuevo cada vez que cambia.
    // El primer envío es el que desbloquea el render: `tree.js` no pinta nada antes de tenerlo,
    // para que el fotograma que dispara `onReady` salga ya con la salud y la etapa correctas.
    LaunchedEffect(pageLoaded, healthScore, stage) {
        if (pageLoaded) {
            webView.evaluateJavascript(
                "window.tensionTree.setState($healthScore, '${stage.code}')",
                null,
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(READY_TIMEOUT_MS)
        if (!reportedReady) {
            notifyFailure()
        }
    }

    // Segundo plano: sin esto el bucle de render seguiría consumiendo GPU con la app invisible.
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier,
        onRelease = { view -> releaseTreeWebView(view, bridge) },
    )
}

/**
 * Si el dispositivo puede intentar el render 3D.
 *
 * Se comprueba antes de componer [Tree3DView] para que un dispositivo sin WebView vea el ícono
 * nativo de inmediato, sin pasar por el timeout.
 */
fun isTree3DSupported(): Boolean = runCatching {
    val webViewPackage = WebView.getCurrentWebViewPackage() ?: return@runCatching false
    TreeWebViewSupport.isSupportedVersion(webViewPackage.versionName)
}.getOrDefault(false)

/**
 * Calidad de partida del render, a partir de las señales del dispositivo. La sonda de
 * `tree.js` puede bajarla después si la medida real no cumple el presupuesto.
 *
 * Un `ActivityManager` ausente se trata como el peor caso: más vale un árbol simple de más que
 * una pantalla trabada.
 */
private fun resolveRenderQuality(context: Context): TreeRenderQuality {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return TreeRenderQuality.resolve(
        memoryClassMb = activityManager?.memoryClass ?: 0,
        isLowRamDevice = activityManager?.isLowRamDevice ?: true,
        processorCount = Runtime.getRuntime().availableProcessors(),
    )
}

// El WebView no tiene semantica de clic: el gesto lo interpreta el JavaScript como orbita de
// camara. El listener existe solo para que ningun contenedor futuro le robe el arrastre.
@SuppressLint("ClickableViewAccessibility")
private fun createTreeWebView(context: Context, bridge: TreeBridge): WebView =
    WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        // Fondo transparente en la vista y en el render: el fondo nativo de la aplicación se ve
        // a través del WebView en modo claro y oscuro (CA-38.04, RNF23).
        setBackgroundColor(Color.TRANSPARENT)

        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER

        settings.apply {
            javaScriptEnabled = true
            // Ninguna de estas capacidades hace falta para dibujar un árbol, y todas amplían la
            // superficie de un WebView que solo debe ejecutar su propio asset. El acceso de
            // JavaScript a otras URL `file://` no se toca porque desde API 16 ya viene cerrado
            // por defecto, y sus setters están deprecados: apagar algo ya apagado solo añadiría
            // dos avisos de deprecación.
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = false
            blockNetworkLoads = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = true
        }

        addJavascriptInterface(bridge, TreeBridge.NAME)

        // El gesto de órbita es del WebView y de nadie más. Hoy la pantalla del árbol no tiene
        // scroll nativo que pudiera robarlo; si alguien se lo añade, esto lo sigue protegiendo.
        setOnTouchListener { view, _ ->
            view.parent?.requestDisallowInterceptTouchEvent(true)
            false
        }
    }

/**
 * Desmonta el WebView por completo (CA-38.04).
 *
 * El orden no es decorativo: primero se desregistra el puente y se anula su listener —destruir
 * con el puente vivo dejaría una referencia retenida a la pantalla—, después se detiene la
 * carga y se vacía el contenido, y solo al final se desancla y se destruye.
 */
@SuppressLint("ClickableViewAccessibility")
private fun releaseTreeWebView(webView: WebView, bridge: TreeBridge) {
    runCatching {
        webView.removeJavascriptInterface(TreeBridge.NAME)
        bridge.release()
        webView.setOnTouchListener(null)
        webView.stopLoading()
        webView.webViewClient = WebViewClient()
        webView.loadUrl(BLANK_URL)
        webView.clearHistory()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }
}
