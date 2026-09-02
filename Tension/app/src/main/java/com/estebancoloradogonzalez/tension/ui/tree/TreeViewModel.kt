package com.estebancoloradogonzalez.tension.ui.tree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.tension.domain.usecase.tree.GetTreeStateUseCase
import com.estebancoloradogonzalez.tension.domain.usecase.tree.RecalculateTreeStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pantalla dedicada del árbol.
 *
 * Recalcula **antes** de observar: es el tercero de los tres momentos de recálculo y garantiza
 * que lo que se muestra no sea un valor rancio, aunque la app llevara horas abierta sin cruzar
 * la medianoche.
 */
@HiltViewModel
class TreeViewModel @Inject constructor(
    private val getTreeStateUseCase: GetTreeStateUseCase,
    private val recalculateTreeStateUseCase: RecalculateTreeStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TreeUiState())
    val uiState: StateFlow<TreeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                recalculateTreeStateUseCase()
            } catch (_: Exception) {
                // Si el recálculo falla se muestra lo último persistido, que sigue siendo una
                // lectura válida del historial. El árbol nunca deja la pantalla vacía.
            }

            getTreeStateUseCase().collect { treeState ->
                _uiState.update { it.copy(isLoading = false, treeState = treeState) }
            }
        }
    }
}
