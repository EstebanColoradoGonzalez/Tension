package com.estebancoloradogonzalez.tension.domain.usecase.tree

import com.estebancoloradogonzalez.tension.domain.model.TreeState
import com.estebancoloradogonzalez.tension.domain.repository.TreeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** El estado actual del árbol, para la tarjeta de Inicio y la pantalla dedicada. */
class GetTreeStateUseCase @Inject constructor(
    private val treeRepository: TreeRepository,
) {
    operator fun invoke(): Flow<TreeState> = treeRepository.getTreeState()
}
