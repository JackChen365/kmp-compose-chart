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

val ChartContext.doubleTapState: State<Boolean>
    get() = chartInteractionHandler.getInteractionState<ChartDoubleTapInteractionState>().state

val ChartContext.doubleTapLocation: Offset
    get() = chartInteractionHandler.getInteractionState<ChartDoubleTapInteractionState>().location

class ChartDoubleTapInteractionState : ChartInteractionState<Boolean> {
    private val doubleTapState = mutableStateOf(false)
    private val doubleTapLocation = mutableStateOf(Offset.Zero)
    override val state: State<Boolean>
        get() = doubleTapState
    override val location: Offset
        get() = doubleTapLocation.value

    override suspend fun handleInteraction(interactions: Flow<Interaction>) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.DoubleTap -> {
                    doubleTapState.value = true
                    doubleTapLocation.value = interaction.location
                }

                is ChartTapInteraction.ExitDoubleTap -> {
                    doubleTapState.value = false
                }
            }
        }
    }

}