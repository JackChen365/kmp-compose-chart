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
import me.jack.compose.chart.interaction.ChartElementInteraction
import me.jack.compose.chart.interaction.ChartHoverInteraction

val ChartContext.elementInteraction: ChartElementInteraction
    get() = chartInteractionHandler.getInteractionState<ChartElementInteractionState>().state.value

class ChartElementInteractionState : ChartInteractionState<ChartElementInteraction> {
    private val interactionState = mutableStateOf<ChartElementInteraction>(ChartElementInteraction.Idle)
    override val state: State<ChartElementInteraction>
        get() = interactionState
    override val location: Offset
        get() = Offset.Zero

    override suspend fun handleInteraction(interactions: Flow<Interaction>) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartElementInteraction.Element<*> -> {
                    interactionState.value = interaction
                }
                is ChartHoverInteraction.Exit->{

                }
            }
        }
    }

}