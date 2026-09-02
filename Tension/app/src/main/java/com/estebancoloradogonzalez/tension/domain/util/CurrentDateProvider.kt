package com.estebancoloradogonzalez.tension.domain.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La fecha de hoy, reemitida al cruzar la medianoche local.
 *
 * Existe porque el sistema depende del calendario en dos sitios que no pueden leer la fecha una
 * sola vez: la determinación de la sesión del día y el cierre automático de lo que quedó
 * abierto. Leer `LocalDate.now()` al construir un flujo dejaría ambos congelados en el día en
 * que la pantalla se abrió.
 *
 * No es un sondeo: espera exactamente hasta el siguiente cambio de día y se cancela con el
 * alcance de quien recolecta. La primera emisión es inmediata, de modo que recolectarlo al
 * arrancar la app equivale a preguntar «¿qué día es?» y, a partir de ahí, «¿ya cambió?».
 *
 * **Con la app cerrada no emite nada.** Android no ofrece un temporizador fiable en segundo
 * plano para esto, así que lo que ocurra a medianoche se resuelve la próxima vez que la app se
 * abra. El resultado observable es el mismo: el ejecutante solo ve la app cuando la abre.
 */
@Singleton
class CurrentDateProvider @Inject constructor() {

    fun today(): LocalDate = LocalDate.now()

    fun dateFlow(): Flow<LocalDate> = flow {
        while (true) {
            val today = LocalDate.now()
            emit(today)
            val millisToMidnight = Duration.between(
                LocalDateTime.now(),
                today.plusDays(1).atStartOfDay(),
            ).toMillis()
            delay(millisToMidnight.coerceAtLeast(MIN_TICK_MILLIS))
        }
    }

    private companion object {
        /** Cota inferior del tick, por si el cálculo diera cero o negativo. */
        const val MIN_TICK_MILLIS = 1_000L
    }
}
