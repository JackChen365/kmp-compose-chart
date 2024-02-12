package me.jack.compose.chart.interaction

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.ui.geometry.Offset

interface ChartHoverInteraction : Interaction {

    class Enter(
        val localLocation: Offset,
    ) : ChartElementInteraction

    class Move(val localLocation: Offset) : ChartHoverInteraction

    class Exit(val localLocation: Offset) : ChartHoverInteraction
}

