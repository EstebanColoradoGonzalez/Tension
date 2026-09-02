package com.estebancoloradogonzalez.tension.domain.model

/**
 * Día de la semana. Dominio cerrado de 7 valores (HU-36).
 *
 * [isoNumber] es el número ISO-8601 del día y coincide con `java.time.DayOfWeek.getValue()`:
 * es la clave primaria de la tabla `week_day`, de modo que resolver "hoy" a su fila es una
 * lectura directa sin mapa intermedio.
 *
 * El enum no lleva etiquetas: "Lunes" y "LUN" son presentación y viven en `strings.xml`,
 * accesibles desde `ui/components/WeekDayLabel.kt`.
 */
enum class WeekDay(val isoNumber: Int) {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7),
    ;

    val code: String get() = name

    companion object {
        fun fromIso(isoNumber: Int): WeekDay =
            entries.firstOrNull { it.isoNumber == isoNumber }
                ?: throw IllegalArgumentException("Invalid ISO-8601 day of week: $isoNumber")

        fun fromCode(code: String?): WeekDay? = entries.firstOrNull { it.name == code }
    }
}
