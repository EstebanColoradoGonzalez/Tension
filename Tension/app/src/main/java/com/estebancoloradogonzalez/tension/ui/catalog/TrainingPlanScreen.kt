package com.estebancoloradogonzalez.tension.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanScreen(
    onNavigateToExerciseDictionary: () -> Unit,
    onNavigateToPlanVersionDetail: (Long) -> Unit,
    viewModel: TrainingPlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.dictionary_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                )
                TabRow(
                    selectedTabIndex = 1,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[1]),
                            width = tabPositions[1].contentWidth,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    Tab(
                        selected = false,
                        onClick = onNavigateToExerciseDictionary,
                        text = {
                            Text(
                                text = stringResource(R.string.tab_exercises),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    Tab(
                        selected = true,
                        onClick = { },
                        text = {
                            Text(
                                text = stringResource(R.string.tab_plan),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    uiState.modules.forEachIndexed { moduleIndex, module ->
                        item(key = "header_${module.moduleCode}") {
                            Column(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = if (moduleIndex == 0) 16.dp else 0.dp,
                                    bottom = 4.dp,
                                ),
                            ) {
                                Text(
                                    text = module.moduleName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = module.groupDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        itemsIndexed(
                            items = module.versions,
                            key = { _, version -> version.moduleVersionId },
                        ) { _, version ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = stringResource(
                                            R.string.version_label_format,
                                            version.versionNumber,
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                trailingContent = {
                                    Text(
                                        text = stringResource(
                                            R.string.exercise_count_format_short,
                                            version.exerciseCount,
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier
                                    .clickable {
                                        onNavigateToPlanVersionDetail(version.moduleVersionId)
                                    }
                                    .height(56.dp),
                            )
                        }
                        if (moduleIndex < uiState.modules.lastIndex) {
                            item(key = "divider_${module.moduleCode}") {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
