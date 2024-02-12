package me.jack.compose.chart.draw.interaction

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
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

val ChartContext.elementInteractionLocation: Offset
    get() = chartInteractionHandler.getInteractionState<ChartElementInteractionState>().location

class ChartElementInteractionState : ChartInteractionState<ChartElementInteraction> {
    private val interactionState = mutableStateOf<ChartElementInteraction>(ChartElementInteraction.Idle)
    private val pressInteractionLocation = mutableStateOf(Offset.Zero)
    override val state: State<ChartElementInteraction>
        get() = interactionState
    override val location: Offset
        get() = pressInteractionLocation.value

    override suspend fun handleInteraction(interactions: Flow<Interaction>) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartElementInteraction.Element<*> -> {
                    if (pressInteractionLocation.value != interaction.location) {
                        interactionState.value = interaction
                        pressInteractionLocation.value = interaction.location
                    }
                }
                is PressInteraction.Release -> {
                    val chartPressInteraction = interactionState.value
                    if (chartPressInteraction is ChartElementInteraction.Element<*>) {
                        interactionState.value =
                            ChartElementInteraction.ExitElement()
                    }
                }

                is PressInteraction.Cancel -> {
                    val chartPressInteraction = interactionState.value
                    if (chartPressInteraction is ChartElementInteraction.Element<*>) {
                        interactionState.value =
                            ChartElementInteraction.ExitElement()
                    }
                }

                is ChartHoverInteraction.Exit -> {
                    val chartPressInteraction = interactionState.value
                    if (chartPressInteraction is ChartElementInteraction.Element<*>) {
                        interactionState.value =
                            ChartElementInteraction.ExitElement()
                    }
                }
            }
        }
    }

}