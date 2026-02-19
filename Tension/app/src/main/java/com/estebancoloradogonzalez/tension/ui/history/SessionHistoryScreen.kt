package com.estebancoloradogonzalez.tension.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.SessionHistoryItem
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    onNavigateToSessionDetail: (Long) -> Unit,
    viewModel: SessionHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.session_history_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is SessionHistoryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is SessionHistoryUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.session_history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is SessionHistoryUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    items(state.sessions) { session ->
                        SessionHistoryRow(
                            session = session,
                            onClick = { onNavigateToSessionDetail(session.sessionId) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryRow(
    session: SessionHistoryItem,
    onClick: () -> Unit,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))
    val formattedDate = try {
        LocalDate.parse(session.date).format(dateFormatter)
    } catch (_: Exception) {
        session.date
    }

    val numberFormat = NumberFormat.getNumberInstance(Locale("es")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }

    val isCompleted = session.status == "COMPLETED"
    val statusText = if (isCompleted) {
        stringResource(R.string.session_history_completed)
    } else {
        stringResource(R.string.session_history_incomplete)
    }
    val statusColor = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100)

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.session_module_version_format,
                        session.moduleCode,
                        session.versionNumber,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isCompleted) "\u2705 $statusText" else "\u26A0\uFE0F $statusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
        },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.session_history_tonnage_format,
                    numberFormat.format(session.totalTonnageKg),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
