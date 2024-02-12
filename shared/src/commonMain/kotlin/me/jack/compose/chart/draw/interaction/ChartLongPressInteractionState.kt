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

val ChartContext.longPressTapState: State<Boolean>
    get() = chartInteractionHandler.getInteractionState<ChartLongPressInteractionState>().state

val ChartContext.longPressLocation: Offset
    get() = chartInteractionHandler.getInteractionState<ChartLongPressInteractionState>().location

class ChartLongPressInteractionState : ChartInteractionState<Boolean> {
    private val longPressState = mutableStateOf(false)
    private val longPressLocation = mutableStateOf(Offset.Zero)
    override val state: State<Boolean>
        get() = longPressState
    override val location: Offset
        get() = longPressLocation.value

    override suspend fun handleInteraction(interactions: Flow<Interaction>) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.LongPress -> {
                    longPressState.value = true
                    longPressLocation.value = interaction.location
                }

                is ChartTapInteraction.ExitLongPress -> {
                    longPressState.value = false
                }
            }
        }
    }

}