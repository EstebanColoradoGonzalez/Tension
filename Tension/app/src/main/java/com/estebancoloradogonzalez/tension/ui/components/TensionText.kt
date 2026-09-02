package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Kinds of text whose long-value treatment is governed by a single application-wide rule.
 */
enum class TensionTextKind {
    /** Names and titles of domain entities: exercises, routines, versions, sessions. */
    ENTITY_NAME,

    /** Progress counters such as "Serie X de Y". Must always stay fully readable. */
    COUNTER,
}

/**
 * Single source of truth for how long text is rendered across every screen.
 *
 * Entity names wrap up to two lines and only then get an ellipsis. Counters never
 * wrap and are never truncated: when they compete for horizontal space with an
 * entity name, the name is the one that yields.
 */
object TensionTextRules {

    /** Maximum lines an entity name may occupy before the ellipsis is applied. */
    const val ENTITY_NAME_MAX_LINES = 2

    /** Counters always render on a single line. */
    const val COUNTER_MAX_LINES = 1

    /** Returns the maximum number of lines allowed for the given kind of text. */
    fun maxLinesFor(kind: TensionTextKind): Int = when (kind) {
        TensionTextKind.ENTITY_NAME -> ENTITY_NAME_MAX_LINES
        TensionTextKind.COUNTER -> COUNTER_MAX_LINES
    }

    /** Returns whether the given kind of text may be shortened with an ellipsis. */
    fun isTruncatable(kind: TensionTextKind): Boolean = when (kind) {
        TensionTextKind.ENTITY_NAME -> true
        TensionTextKind.COUNTER -> false
    }
}

/**
 * Renders an entity name or title applying [TensionTextKind.ENTITY_NAME]: up to two
 * lines, ellipsis beyond that. The full value stays reachable on the detail screen
 * of the element.
 */
@Composable
fun EntityNameText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = TensionTextRules.maxLinesFor(TensionTextKind.ENTITY_NAME),
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Renders a progress counter applying [TensionTextKind.COUNTER]: a single line that
 * is never wrapped nor ellipsized.
 *
 * Place it inside a [androidx.compose.foundation.layout.Row] **without** a weight so
 * it is measured before any weighted sibling and therefore always keeps the space it
 * needs.
 */
@Composable
fun CounterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        softWrap = false,
        maxLines = TensionTextRules.maxLinesFor(TensionTextKind.COUNTER),
        overflow = TextOverflow.Clip,
    )
}
