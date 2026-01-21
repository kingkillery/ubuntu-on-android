package com.udroid.app.ui.tui

import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.graphics.Color

/**
 * TUI Color scheme matching PK-Puzld.ai terminal aesthetic.
 * Derived from Puzld.ai Banner.tsx colors.
 */
// TuiColors object removed - Use MaterialTheme.colorScheme instead

/**
 * Box drawing characters for TUI borders (Unicode rounded corners).
 * Matching Puzld.ai Banner.tsx box characters.
 */
object TuiBoxChars {
    // Rounded corners
    const val TOP_LEFT = "╭"
    const val TOP_RIGHT = "╮"
    const val BOTTOM_LEFT = "╰"
    const val BOTTOM_RIGHT = "╯"

    // Straight lines
    const val HORIZONTAL = "─"
    const val VERTICAL = "│"

    // T-junctions
    const val LEFT_T = "├"
    const val RIGHT_T = "┤"
    const val TOP_T = "┬"
    const val BOTTOM_T = "┴"

    // Cross
    const val CROSS = "┼"

    // Status bullets
    const val BULLET_FILLED = "●"
    const val BULLET_EMPTY = "○"

    /**
     * Create a horizontal line of specified width.
     */
    fun horizontalLine(width: Int): String = HORIZONTAL.repeat(width)

    /**
     * Create a top border with optional title.
     */
    fun topBorder(width: Int, title: String? = null): String {
        return if (title != null && title.length < width - 4) {
            val padding = width - title.length - 4
            "$TOP_LEFT$HORIZONTAL$HORIZONTAL $title ${HORIZONTAL.repeat(padding)}$TOP_RIGHT"
        } else {
            "$TOP_LEFT${HORIZONTAL.repeat(width)}$TOP_RIGHT"
        }
    }

    /**
     * Create a bottom border.
     */
    fun bottomBorder(width: Int): String = "$BOTTOM_LEFT${HORIZONTAL.repeat(width)}$BOTTOM_RIGHT"

    /**
     * Create a middle separator.
     */
    fun middleSeparator(width: Int): String = "$LEFT_T${HORIZONTAL.repeat(width)}$RIGHT_T"
}

/**
 * Typography helpers for TUI text styling.
 */
object TuiTypography {
    /**
     * Format a number as compact token display (e.g., 1500 -> "1.5k").
     */
    fun formatTokens(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
            else -> count.toString()
        }
    }

    /**
     * Pad a string to fixed width.
     */
    fun padEnd(text: String, width: Int, padChar: Char = ' '): String {
        return if (text.length >= width) text.take(width)
        else text + padChar.toString().repeat(width - text.length)
    }

    /**
     * Center a string within fixed width.
     */
    fun center(text: String, width: Int, padChar: Char = ' '): String {
        if (text.length >= width) return text.take(width)
        val padding = width - text.length
        val leftPad = padding / 2
        val rightPad = padding - leftPad
        return padChar.toString().repeat(leftPad) + text + padChar.toString().repeat(rightPad)
    }

    /**
     * Truncate text with ellipsis if too long.
     */
    fun truncate(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text
        else text.take(maxLength - 3) + "..."
    }
}
