package me.jack.compose.chart.interaction

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.draw.InteractionState

interface DrawElementInteractionState : InteractionState<DrawElementInteraction>

class MutableDrawElementInteractionState : DrawElementInteractionState {
    override var state: DrawElementInteraction by mutableStateOf(DrawElementInteraction.Idle)
    override var location: Offset by mutableStateOf(Offset.Zero)
}

@Composable
fun InteractionSource.collectDrawElementAsState(): DrawElementInteractionState {
    val elementInteractionState by remember { mutableStateOf(MutableDrawElementInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is DrawElementInteraction.Element<*, *> -> {
                    elementInteractionState.state = interaction
                }

                is ChartHoverInteraction.Exit -> {
                    elementInteractionState.state = DrawElementInteraction.Idle
                }
            }
        }
    }
    return elementInteractionState
}

interface DrawElementInteraction : Interaction {
    object Idle : DrawElementInteraction

    data class Element<T, E : DrawElement>(
        val drawElement: E,
        val currentItem: T,
        val currentGroupItems: List<T>
    ) : DrawElementInteraction

    class ExitElement : DrawElementInteraction
}
