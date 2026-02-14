package com.estebancoloradogonzalez.tension.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.ui.components.TensionTopAppBar

@Composable
fun ExerciseHistoryScreen(
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TensionTopAppBar(
                title = stringResource(R.string.exercise_history_title),
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.coming_soon),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
