package com.estebancoloradogonzalez.tension.domain.rules

import kotlin.math.roundToInt

object AvgRirRule {

    fun calculate(rirValues: List<Int>): Double {
        if (rirValues.isEmpty()) return 0.0
        return (rirValues.sum().toDouble() / rirValues.size)
            .let { (it * 10).roundToInt() / 10.0 }
    }
}
