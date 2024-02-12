package me.jack.compose.chart.draw.interaction

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.Flow
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.ChartInteractionState
import me.jack.compose.chart.context.chartInteractionHandler
import me.jack.compose.chart.context.getInteractionState
import me.jack.compose.chart.interaction.ChartHoverInteraction

val ChartContext.hoverState: State<Boolean>
    get() = chartInteractionHandler.getInteractionState<ChartHoverInteractionState>().state

val ChartContext.hoverLocation: Offset
    get() = chartInteractionHandler.getInteractionState<ChartHoverInteractionState>().location

class ChartHoverInteractionState : ChartInteractionState<Boolean> {
    private val hoverState = mutableStateOf(false)
    private val hoverLocation = mutableStateOf(Offset.Zero)
    override val state: State<Boolean>
        get() = hoverState
    override val location: Offset
        get() = hoverLocation.value

    override suspend fun handleInteraction(interactions: Flow<Interaction>) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartHoverInteraction.Enter -> {
                    hoverState.value = true
                    hoverLocation.value = interaction.localLocation
                }

                is ChartHoverInteraction.Move -> {
                    hoverLocation.value = interaction.localLocation
                }

                is ChartHoverInteraction.Exit -> {
                    hoverState.value = false
                    hoverLocation.value = interaction.localLocation
                }
            }
        }
    }

}