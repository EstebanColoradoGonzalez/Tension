package com.estebancoloradogonzalez.tension.domain.rules

object VolumeDistributionRule {

    fun calculate(setsByZone: Map<String, Int>, totalSets: Int): Map<String, Double> {
        if (totalSets == 0) return setsByZone.mapValues { 0.0 }
        return setsByZone.mapValues { (_, count) ->
            (count.toDouble() / totalSets) * 100.0
        }
    }
}
