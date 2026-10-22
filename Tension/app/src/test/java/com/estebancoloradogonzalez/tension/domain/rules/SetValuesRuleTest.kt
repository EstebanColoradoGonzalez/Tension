package com.estebancoloradogonzalez.tension.domain.rules

import org.junit.Test

/**
 * The validations shared by registering a set and correcting the last one (CA-47.06). One
 * rule for both is what keeps them identical; these cases pin down what "identical" means.
 */
class SetValuesRuleTest {

    @Test
    fun `given valid values, when validating, then nothing is thrown`() {
        SetValuesRule.requireValid(weightKg = 82.5, reps = 10, rir = 1, equipmentTypeName = "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given a negative weight, when validating, then it is rejected`() {
        SetValuesRule.requireValid(weightKg = -1.0, reps = 10, rir = 1, equipmentTypeName = "Barra")
    }

    @Test
    fun `given zero weight without added weight, when validating, then it is accepted as in the registration`() {
        // Same as RegisterSetUseCaseTest: zero is only rejected for «Peso Añadido».
        SetValuesRule.requireValid(weightKg = 0.0, reps = 10, rir = 1, equipmentTypeName = "Barra")
    }

    @Test
    fun `given the minimum increment, when validating, then it is accepted`() {
        SetValuesRule.requireValid(weightKg = 0.5, reps = 10, rir = 1, equipmentTypeName = "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given a weight above 500 kg, when validating, then it is rejected`() {
        SetValuesRule.requireValid(weightKg = 500.01, reps = 10, rir = 1, equipmentTypeName = "Barra")
    }

    @Test
    fun `given exactly 500 kg, when validating, then it is accepted`() {
        SetValuesRule.requireValid(weightKg = 500.0, reps = 10, rir = 1, equipmentTypeName = "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given zero reps, when validating, then it is rejected`() {
        SetValuesRule.requireValid(weightKg = 60.0, reps = 0, rir = 1, equipmentTypeName = "Barra")
    }

    @Test
    fun `given one rep, when validating, then it is accepted`() {
        SetValuesRule.requireValid(weightKg = 60.0, reps = 1, rir = 1, equipmentTypeName = "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given an RIR of 3, when validating, then it is rejected`() {
        SetValuesRule.requireValid(weightKg = 60.0, reps = 10, rir = 3, equipmentTypeName = "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given a negative RIR, when validating, then it is rejected`() {
        SetValuesRule.requireValid(weightKg = 60.0, reps = 10, rir = -1, equipmentTypeName = "Barra")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given added weight of zero, when validating, then it is rejected`() {
        SetValuesRule.requireValid(
            weightKg = 0.0,
            reps = 10,
            rir = 1,
            equipmentTypeName = ExternalLoadRule.PESO_ANADIDO,
        )
    }

    @Test
    fun `given zero weight on bodyweight, when validating, then it is accepted`() {
        SetValuesRule.requireValid(
            weightKg = 0.0,
            reps = 45,
            rir = 2,
            equipmentTypeName = ExternalLoadRule.PESO_CORPORAL,
        )
    }
}
