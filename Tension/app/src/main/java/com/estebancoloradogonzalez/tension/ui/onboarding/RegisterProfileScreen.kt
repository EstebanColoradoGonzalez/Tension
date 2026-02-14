package com.estebancoloradogonzalez.tension.ui.onboarding

import androidx.compose.foundation.Image
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

@Composable
fun RegisterProfileScreen(
    viewModel: RegisterProfileViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RegisterProfileEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    RegisterProfileContent(
        uiState = uiState,
        onWeightChanged = viewModel::onWeightChanged,
        onHeightChanged = viewModel::onHeightChanged,
        onExperienceLevelSelected = viewModel::onExperienceLevelSelected,
        onRegisterClick = viewModel::onRegisterClick,
    )
}

@Composable
private fun RegisterProfileContent(
    uiState: RegisterProfileUiState,
    onWeightChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onExperienceLevelSelected: (ExperienceLevel) -> Unit,
    onRegisterClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.logo_content_description),
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(48.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRegisterClick,
            enabled = uiState.isFormValid && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.register_button))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
