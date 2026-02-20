package com.estebancoloradogonzalez.tension.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAlertDetail: (Long) -> Unit,
    viewModel: AlertCenterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.alert_center_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (uiState is AlertCenterUiState.Loaded) {
                            val totalCount =
                                (uiState as AlertCenterUiState.Loaded).totalCount
                            Text(
                                text = stringResource(
                                    R.string.alert_center_subtitle,
                                    totalCount,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is AlertCenterUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is AlertCenterUiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF2E7D32),
                    )
                    Text(
                        text = stringResource(R.string.alert_center_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            is AlertCenterUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.crisisAlerts.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.alert_center_section_crisis),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFBA1A1A),
                            )
                            HorizontalDivider(
                                color = Color(0xFFBA1A1A),
                                thickness = 1.dp,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                        items(
                            items = state.crisisAlerts,
                            key = { it.alertId },
                        ) { alert ->
                            AlertCard(
                                alert = alert,
                                onClick = { onNavigateToAlertDetail(alert.alertId) },
                            )
                        }
                    }
                    if (state.regularAlerts.isNotEmpty()) {
                        item {
                            Column(
                                modifier = if (state.crisisAlerts.isNotEmpty()) {
                                    Modifier.padding(top = 16.dp)
                                } else {
                                    Modifier
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.alert_center_section_alerts),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                        items(
                            items = state.regularAlerts,
                            key = { it.alertId },
                        ) { alert ->
                            AlertCard(
                                alert = alert,
                                onClick = { onNavigateToAlertDetail(alert.alertId) },
                            )
                        }
                    }
                }
            }
        }
    }
}
