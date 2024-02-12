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
import me.jack.compose.chart.interaction.ChartTapInteraction

val ChartContext.tapState: State<Boolean>
    get() = chartInteractionHandler.getInteractionState<ChartTapInteractionState>().state

val ChartContext.tapLocation: Offset
    get() = chartInteractionHandler.getInteractionState<ChartTapInteractionState>().location

class ChartTapInteractionState : ChartInteractionState<Boolean> {
    private val tapState = mutableStateOf(false)
    private val tapLocation = mutableStateOf(Offset.Zero)
    override val state: State<Boolean>
        get() = tapState
    override val location: Offset
        get() = tapLocation.value

    override suspend fun handleInteraction(interactions: Flow<Interaction>) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.Tap -> {
                    tapState.value = true
                    tapLocation.value = interaction.location
                }

                is ChartTapInteraction.ExitTap -> {
                    tapState.value = false
                }
            }
        }
    }

}