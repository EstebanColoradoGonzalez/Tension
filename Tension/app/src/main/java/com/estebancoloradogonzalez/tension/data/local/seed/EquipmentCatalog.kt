package com.estebancoloradogonzalez.tension.data.local.seed

import com.estebancoloradogonzalez.tension.data.local.seed.model.SeedEquipmentType
import com.estebancoloradogonzalez.tension.domain.rules.ExternalLoadRule

/**
 * Catálogo de equipamiento precargado en instalación fresca: **15 tipos atómicos**.
 *
 * Un tipo, un implemento. Los valores compuestos que el catálogo llegó a tener
 * —`Mancuerna o Polea`, `Barra o Mancuernas`, `Mancuerna o Polea o Barra`— describían una
 * disyunción y no un implemento: el ejercicio decía «se hace con una cosa o con otra» y la
 * serie no registraba cuál. Desde HU-39 la disyunción vive en `exercise_equipment` y la
 * elección en `exercise_set.equipment_type_id`.
 *
 * El identificador **encoda el orden declarado**, que es el orden en que la lista de
 * casillas del formulario de ejercicio presenta las opciones. La tabla es cerrada y
 * sembrada —no existe interfaz para crear tipos de equipamiento— así que ordenar por `id`
 * es ordenar por el criterio del catálogo y no por un accidente alfabético.
 *
 * Los agarres no son equipamiento: cuerda y barra en V son `Polea`. `Máquina de Remo` y
 * `Máquina Contractor` no son tipos propios: ambos son `Máquina`.
 */
object EquipmentCatalog {

    const val MAQUINA = 1L
    const val MAQUINA_SMITH = 2L
    const val POLEA = 3L
    const val BARRA = 4L
    const val BARRA_FIJA = 5L
    const val MANCUERNA = 6L
    const val PESA_RUSA = 7L
    const val BANDA_ELASTICA = 8L
    const val PESO_CORPORAL = 9L
    const val PESO_ANADIDO = 10L
    const val BARRA_EZ = 11L
    const val TRX_SUSPENSION = 12L
    const val BALON_MEDICINAL = 13L
    const val RODILLO_DE_ABDOMEN = 14L
    const val PARALELAS_DIP_STATION = 15L

    val ALL: List<SeedEquipmentType> = listOf(
        SeedEquipmentType(MAQUINA, "Máquina"),
        SeedEquipmentType(MAQUINA_SMITH, "Máquina Smith"),
        SeedEquipmentType(POLEA, "Polea"),
        SeedEquipmentType(BARRA, "Barra"),
        SeedEquipmentType(BARRA_FIJA, "Barra Fija"),
        SeedEquipmentType(MANCUERNA, "Mancuerna"),
        SeedEquipmentType(PESA_RUSA, "Pesa Rusa"),
        SeedEquipmentType(BANDA_ELASTICA, "Banda Elástica"),
        SeedEquipmentType(PESO_CORPORAL, ExternalLoadRule.PESO_CORPORAL),
        SeedEquipmentType(PESO_ANADIDO, ExternalLoadRule.PESO_ANADIDO),
        // Sin ejercicio semilla: disponibles para los ejercicios que cree el ejecutante.
        SeedEquipmentType(BARRA_EZ, "Barra EZ"),
        SeedEquipmentType(TRX_SUSPENSION, "TRX/Suspensión"),
        SeedEquipmentType(BALON_MEDICINAL, "Balón Medicinal"),
        SeedEquipmentType(RODILLO_DE_ABDOMEN, "Rodillo de Abdomen"),
        SeedEquipmentType(PARALELAS_DIP_STATION, "Paralelas/Dip Station"),
    )

    fun byId(id: Long): SeedEquipmentType? = ALL.firstOrNull { it.id == id }
}
