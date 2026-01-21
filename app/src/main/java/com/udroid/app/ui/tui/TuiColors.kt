package com.udroid.app.ui.tui

import androidx.compose.ui.graphics.Color

/**
 * TUI Color scheme matching PK-Puzld.ai terminal aesthetic.
 * Derived from Puzld.ai Banner.tsx colors.
 */
object TuiColors {
    // Primary brand colors
    val Red = Color(0xFFFC3855)          // PK-Puzld accent color (#fc3855)
    val White = Color(0xFFFFFFFF)        // Primary text
    val Gray = Color(0xFF808080)         // Secondary text, borders
    val DimGray = Color(0xFF666666)      // Dimmed/muted text

    // Terminal background colors
    val Background = Color(0xFF1E1E1E)   // Terminal dark background
    val Surface = Color(0xFF252526)      // Slightly lighter surface
    val SurfaceVariant = Color(0xFF2D2D30) // Card/panel backgrounds

    // Text colors
    val Foreground = Color(0xFFD4D4D4)   // Primary terminal text
    val ForegroundDim = Color(0xFF808080) // Dimmed text
    val ForegroundMuted = Color(0xFF5A5A5A) // Very muted text

    // Semantic colors
    val Success = Color(0xFF4EC9B0)      // Green for success/ready
    val Warning = Color(0xFFDCDCAA)      // Yellow for warnings
    val Error = Color(0xFFF44747)        // Red for errors
    val Info = Color(0xFF569CD6)         // Blue for info

    // Agent status colors
    val AgentReady = Color(0xFF4EC9B0)   // Green circle for ready
    val AgentOff = Color(0xFF5A5A5A)     // Gray circle for off

    // Message role colors
    val UserMessage = Color(0xFF569CD6)  // Blue for user input
    val AssistantMessage = Color(0xFFD4D4D4) // Default for assistant
    val SystemMessage = Color(0xFFDCDCAA) // Yellow for system
    val ErrorMessage = Color(0xFFF44747) // Red for errors

    // Border colors
    val Border = Color(0xFF3C3C3C)       // Standard border
    val BorderFocused = Color(0xFF569CD6) // Focused input border
    val BorderAccent = Color(0xFFFC3855) // Accent border (red)
}

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
