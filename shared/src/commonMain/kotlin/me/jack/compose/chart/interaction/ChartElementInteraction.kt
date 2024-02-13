package me.jack.compose.chart.interaction

import androidx.compose.foundation.interaction.Interaction
import me.jack.compose.chart.draw.DrawElement

interface ChartElementInteraction : Interaction {
    object Idle : ChartElementInteraction

    class Element<T>(
        val drawElement: DrawElement,
        val currentItem: T,
        val currentGroupItems: List<T>
    ) : ChartElementInteraction

    class ExitElement : ChartElementInteraction
}
