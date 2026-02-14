package com.estebancoloradogonzalez.tension.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
import com.estebancoloradogonzalez.tension.ui.components.TensionTopAppBar

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToWeightHistory: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveSuccessMsg = stringResource(R.string.save_success)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.SaveSuccess -> {
                    snackbarHostState.showSnackbar(saveSuccessMsg)
                }
                is ProfileEvent.SaveError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TensionTopAppBar(
                title = stringResource(R.string.profile_title),
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        } else {
            ProfileContent(
                uiState = uiState,
                onWeightChanged = viewModel::onWeightChanged,
                onHeightChanged = viewModel::onHeightChanged,
                onExperienceLevelSelected = viewModel::onExperienceLevelSelected,
                onSaveClick = viewModel::onSaveClick,
                onNavigateToWeightHistory = onNavigateToWeightHistory,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onWeightChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onExperienceLevelSelected: (ExperienceLevel) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateToWeightHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.logo_content_description),
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.weightKg,
            onValueChange = onWeightChanged,
            label = { Text(stringResource(R.string.weight_label)) },
            suffix = { Text("Kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = uiState.showWeightError,
            supportingText = if (uiState.showWeightError) {
                { Text(stringResource(R.string.error_weight_positive)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.heightM,
            onValueChange = onHeightChanged,
            label = { Text(stringResource(R.string.height_label)) },
            suffix = { Text("m") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = uiState.showHeightError,
            supportingText = if (uiState.showHeightError) {
                { Text(stringResource(R.string.error_height_positive)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExperienceLevelDropdown(
            selectedLevel = uiState.experienceLevel,
            onLevelSelected = onExperienceLevelSelected,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSaveClick,
            enabled = uiState.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.save_button))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.weight_history_link),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onNavigateToWeightHistory() }
                .padding(vertical = 8.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperienceLevelDropdown(
    selectedLevel: ExperienceLevel?,
    onLevelSelected: (ExperienceLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val levels = listOf(
        ExperienceLevel.BEGINNER to stringResource(R.string.experience_beginner),
        ExperienceLevel.INTERMEDIATE to stringResource(R.string.experience_intermediate),
        ExperienceLevel.ADVANCED to stringResource(R.string.experience_advanced),
    )

    val selectedText = levels.firstOrNull { it.first == selectedLevel }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.experience_level_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            levels.forEach { (level, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onLevelSelected(level)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}