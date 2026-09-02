package com.estebancoloradogonzalez.tension.ui.tree

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import com.estebancoloradogonzalez.tension.ui.components.TreeIcon
import com.estebancoloradogonzalez.tension.ui.components.treeStageLabelRes

/** Lado del área del árbol. Fijo a propósito: ver [TreeVisual]. */
private val TREE_AREA_SIZE = 180.dp

private const val HEALTH_HIGH_MIN = 67
private const val HEALTH_MEDIUM_MIN = 34
private const val HEALTH_LOW_MIN = 1

/** Duración del fundido con el que el modelo 3D sustituye al ícono nativo. */
private const val RENDER_FADE_MS = 220

private const val DAYS_TODAY = 0
private const val DAYS_YESTERDAY = 1

/**
 * Pantalla dedicada del árbol.
 *
 * La única acción de navegación es el retroceso nativo de la barra superior: aquí no se decide
 * nada, solo se mira.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeScreen(
    onNavigateBack: () -> Unit,
    viewModel: TreeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tree_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            TreeVisual(
                stage = uiState.stage,
                healthScore = uiState.healthScore,
                hasHistory = uiState.hasHistory,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(treeStageLabelRes(uiState.stage)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = uiState.healthScore.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.tree_health_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sin historial no hay línea de días: no existe una referencia contra la cual
            // contar, y un "hace 0 días" sugeriría una inactividad que no ha ocurrido.
            uiState.daysSinceLastSession?.let { days ->
                Text(
                    text = lastSessionText(days),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = contextualMessage(
                    healthScore = uiState.healthScore,
                    hasHistory = uiState.hasHistory,
                    days = uiState.daysSinceLastSession,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * El área del árbol, de tamaño fijo y con una única responsabilidad: elegir la representación.
 *
 * La representación por defecto es el **modelo tridimensional** de [Tree3DView]. La nativa de
 * HU-37 no desaparece: es el fallback, y se conserva de forma permanente porque garantiza que
 * ningún ejecutante se quede sin árbol por culpa de su dispositivo (CA-38.05).
 *
 * El ícono no espera a que el 3D falle para aparecer: **está desde el primer instante** y el
 * WebView se compone encima con opacidad cero, apareciendo con un fundido cuando reporta su
 * primer fotograma. De ahí que el área nunca quede vacía y que un fallo, temprano o tardío, se
 * vea como un árbol que simplemente no cambió de forma. No hay mensaje de error porque no hay
 * nada que el ejecutante deba hacer.
 *
 * Ningún otro elemento del layout depende de lo que haya aquí dentro, solo de que ocupe
 * [TREE_AREA_SIZE]: la pantalla no se reorganiza (CA-38.01).
 */
@Composable
private fun TreeVisual(
    stage: TreeGrowthStage,
    healthScore: Int,
    hasHistory: Boolean,
) {
    // El soporte se resuelve una sola vez: no cambia mientras la pantalla vive, y consultarlo
    // en cada recomposición sería preguntarle al PackageManager lo mismo una y otra vez.
    val supports3D = remember { isTree3DSupported() }

    // Se reutiliza la descripción de accesibilidad de HU-37: esta historia no añade ninguna
    // cadena nueva, y lo que el área presenta sigue siendo el mismo árbol.
    val treeDescription = stringResource(R.string.tree_icon_description)

    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val showNativeIcon = !supports3D || failed || !ready
    val renderAlpha by animateFloatAsState(
        targetValue = if (ready && !failed) 1f else 0f,
        animationSpec = tween(durationMillis = RENDER_FADE_MS),
        label = "treeRenderAlpha",
    )

    Box(
        modifier = Modifier
            .size(TREE_AREA_SIZE)
            .semantics { contentDescription = treeDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (showNativeIcon) {
            TreeIcon(
                stage = stage,
                healthScore = healthScore,
                hasHistory = hasHistory,
                size = TREE_AREA_SIZE,
            )
        }

        if (supports3D && !failed) {
            Tree3DView(
                healthScore = healthScore,
                stage = stage,
                isDarkTheme = isSystemInDarkTheme(),
                onReady = { ready = true },
                onFailure = {
                    failed = true
                    ready = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = renderAlpha },
            )
        }
    }
}

@Composable
private fun lastSessionText(days: Int): String = when (days) {
    DAYS_TODAY -> stringResource(R.string.tree_last_session_today)
    DAYS_YESTERDAY -> stringResource(R.string.tree_last_session_yesterday)
    else -> stringResource(R.string.tree_last_session_days, days)
}

@Composable
private fun contextualMessage(healthScore: Int, hasHistory: Boolean, days: Int?): String {
    if (!hasHistory || days == null) {
        return stringResource(R.string.tree_message_seed)
    }
    return when {
        healthScore >= HEALTH_HIGH_MIN -> stringResource(R.string.tree_message_high)
        healthScore >= HEALTH_MEDIUM_MIN -> stringResource(R.string.tree_message_medium, days)
        healthScore >= HEALTH_LOW_MIN -> stringResource(R.string.tree_message_low, days)
        else -> stringResource(R.string.tree_message_withered, days)
    }
}
