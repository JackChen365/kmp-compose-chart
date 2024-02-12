package me.jack.compose.chart.interaction

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.ui.geometry.Offset
import me.jack.compose.chart.draw.DrawElement

@Suppress("UNCHECKED_CAST")
inline fun <reified T> ChartElementInteraction.asElementInteraction(): ChartElementInteraction.Element<T>? {
    val chartElementInteraction = this as? ChartElementInteraction.Element<Any>
    if(null != chartElementInteraction && chartElementInteraction.currentItem is T){
        return chartElementInteraction as ChartElementInteraction.Element<T>
    }
    return null
}

interface ChartElementInteraction : Interaction {
    object Idle : ChartElementInteraction

    class Element<T>(
        val location: Offset,
        val drawElement: DrawElement,
        val currentItem: T,
        val currentGroupItems: List<T>
    ) : ChartElementInteraction

    class ExitElement : ChartElementInteraction
}
