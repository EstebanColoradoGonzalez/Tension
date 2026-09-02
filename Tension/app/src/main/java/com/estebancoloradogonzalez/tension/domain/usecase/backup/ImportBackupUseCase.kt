package com.estebancoloradogonzalez.tension.domain.usecase.backup

import com.estebancoloradogonzalez.tension.domain.repository.BackupRepository
import com.estebancoloradogonzalez.tension.domain.usecase.tree.RecalculateTreeStateUseCase
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val recalculateTreeStateUseCase: RecalculateTreeStateUseCase,
) {
    /**
     * Restaura el respaldo y reconstruye el árbol sobre el historial restaurado.
     *
     * El recálculo cierra los dos casos de una vez: un respaldo del formato anterior no trae
     * `tree_state` y lo necesita, y uno del formato actual lo trae calculado en otra fecha, con
     * lo que sus días transcurridos ya no valen. El árbol siempre es derivable, así que
     * derivarlo es lo correcto en ambos.
     */
    suspend operator fun invoke(json: String) {
        backupRepository.importFromJson(json)
        recalculateTreeStateUseCase()
    }
}
