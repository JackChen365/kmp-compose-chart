package me.jack.compose.chart.context

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.jack.compose.chart.draw.interaction.ChartDoubleTapInteractionState
import me.jack.compose.chart.draw.interaction.ChartElementInteractionState
import me.jack.compose.chart.draw.interaction.ChartHoverInteractionState
import me.jack.compose.chart.draw.interaction.ChartLongPressInteractionState
import me.jack.compose.chart.draw.interaction.ChartTapInteractionState

val ChartContext.chartInteractionHandler: ChartInteractionHandler
    get() = get(ChartInteractionHandler) ?: error("Can not found the chart interaction handler.")

suspend fun ChartInteractionHandler.emit(interaction: Interaction) {
    val mutableInteractionSource = interactionSource as MutableInteractionSource
    mutableInteractionSource.emit(interaction = interaction)
}

fun ChartInteractionHandler.tryEmit(interaction: Interaction) {
    val mutableInteractionSource = interactionSource as MutableInteractionSource
    mutableInteractionSource.tryEmit(interaction = interaction)
}

fun ChartContext.chartInteraction(
    interactionSource: MutableInteractionSource = MutableInteractionSource()
): ChartContext {
    return this + SimpleChartInteractionHandler(interactionSource).also {
        it.addChartInteractionState(ChartTapInteractionState())
        it.addChartInteractionState(ChartDoubleTapInteractionState())
        it.addChartInteractionState(ChartLongPressInteractionState())
        it.addChartInteractionState(ChartPressInteractionState())
        it.addChartInteractionState(ChartElementInteractionState())
        it.addChartInteractionState(ChartHoverInteractionState())
    }
}

val ChartContext.pressState: State<Boolean>
    get() {
        val pressInteractionState = chartInteractionHandler.chartInteractionStates.find {
            it is ChartPressInteractionState
        }
        @Suppress("UNCHECKED_CAST")
        return pressInteractionState?.state as State<Boolean>
    }

val ChartContext.pressLocation: Offset
    get() {
        val pressInteractionState = chartInteractionHandler.chartInteractionStates.find {
            it is ChartPressInteractionState
        }
        checkNotNull(pressInteractionState)
        return pressInteractionState.location
    }

interface ChartInteractionState<T> {
    val state: State<T>
    val location: Offset

    suspend fun handleInteraction(interactions: Flow<Interaction>)
}

class ChartPressInteractionState : ChartInteractionState<Boolean> {
    private val pressState = mutableStateOf(false)
    private val pressLocation = mutableStateOf(Offset.Zero)
    override val state: State<Boolean>
        get() = pressState
    override val location: Offset
        get() = pressLocation.value

    override suspend fun handleInteraction(interactions: Flow<Interaction>) {
        val pressInteractions = mutableListOf<PressInteraction.Press>()
        interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressInteractions.add(interaction)
                    pressLocation.value = interaction.pressPosition
                }

                is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
            }
            pressState.value = pressInteractions.isNotEmpty()
        }
    }

}

class SimpleChartInteractionHandler(
    override val interactionSource: InteractionSource
) : ChartInteractionHandler {
    private val mutableChartInteractionStates = mutableListOf<ChartInteractionState<*>>()

    override val chartInteractionStates: List<ChartInteractionState<*>>
        get() = mutableChartInteractionStates

    override fun addChartInteractionState(interactionState: ChartInteractionState<*>) {
        mutableChartInteractionStates.add(interactionState)
    }

    override fun CoroutineScope.handleInteractionSource() {
        chartInteractionStates.forEach { interactionState ->
            launch {
                interactionState.handleInteraction(interactionSource.interactions)
            }
        }
    }
}

inline fun <reified T : ChartInteractionState<*>> ChartInteractionHandler.getInteractionState(): T {
    val interactionState = chartInteractionStates.find { it is T }
    checkNotNull(interactionState)
    return interactionState as T
}

interface ChartInteractionHandler : ChartContext.Element {
    companion object Key : ChartContext.Key<ChartInteractionHandler>

    override val key: ChartContext.Key<*> get() = Key

    val interactionSource: InteractionSource

    val chartInteractionStates: List<ChartInteractionState<*>>

    fun addChartInteractionState(interactionState: ChartInteractionState<*>)

    fun CoroutineScope.handleInteractionSource()
}