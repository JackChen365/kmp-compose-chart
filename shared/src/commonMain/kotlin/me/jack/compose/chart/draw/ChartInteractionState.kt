package me.jack.compose.chart.draw

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import me.jack.compose.chart.interaction.ChartHoverInteraction
import me.jack.compose.chart.interaction.ChartTapInteraction

interface InteractionState<T> {
    val state: T
    val location: Offset
}

interface BooleanInteractionState: InteractionState<Boolean>

class MutableBooleanInteractionState : BooleanInteractionState {
    override var state: Boolean by mutableStateOf(false)
    override var location: Offset by mutableStateOf(Offset.Zero)
}

private class EmptyBooleanInteractionState(
    override val state: Boolean = false,
    override val location: Offset = Offset.Unspecified
) : BooleanInteractionState

interface ChartInteractionStates {
    val pressState: BooleanInteractionState
    val hoverState: BooleanInteractionState
    val tapState: BooleanInteractionState
    val doubleTapState: BooleanInteractionState
    val longPressState: BooleanInteractionState
    val interactionStates: List<InteractionState<*>>

    val isPressed: Boolean
        get() = pressState.state

    val isHovered: Boolean
        get() = hoverState.state

    val isHoveredOrPressed: Boolean
        get() = hoverState.state || pressState.state

    val isTap: Boolean
        get() = tapState.state

    val isDoubleTap: Boolean
        get() = pressState.state

    val isLongPress: Boolean
        get() = longPressState.state
}

@Composable
fun rememberInteractionStates(
    interactionSource: InteractionSource,
    vararg interactionStates: InteractionState<*>
): ChartInteractionStates {
    val pressState = interactionSource.collectIsPressedAsState()
    val hoverState = interactionSource.collectIsHoveredAsState()
    val tapState = interactionSource.collectTapAsState()
    val doubleTapState = interactionSource.collectDoubleTapAsState()
    val longPressState = interactionSource.collectLongPressAsState()
    return remember {
        ChartMutableInteractionStates(
            pressState = pressState,
            hoverState = hoverState,
            tapState = tapState,
            doubleTapState = doubleTapState,
            longPressState = longPressState,
            interactionStates = interactionStates.toList()
        )
    }
}

class ChartDummyInteractionStates(
    override val pressState: BooleanInteractionState = EmptyBooleanInteractionState(),
    override val hoverState: BooleanInteractionState = EmptyBooleanInteractionState(),
    override val tapState: BooleanInteractionState = EmptyBooleanInteractionState(),
    override val doubleTapState: BooleanInteractionState = EmptyBooleanInteractionState(),
    override val longPressState: BooleanInteractionState = EmptyBooleanInteractionState(),
    override val interactionStates: List<InteractionState<*>> = emptyList()
) : ChartInteractionStates

open class ChartMutableInteractionStates(
    override val pressState: BooleanInteractionState,
    override val hoverState: BooleanInteractionState,
    override val tapState: BooleanInteractionState,
    override val doubleTapState: BooleanInteractionState,
    override val longPressState: BooleanInteractionState,
    override val interactionStates: List<InteractionState<*>>
) : ChartInteractionStates

@Composable
private fun InteractionSource.collectIsPressedAsState(): BooleanInteractionState {
    val pressState by remember { mutableStateOf(MutableBooleanInteractionState()) }
    LaunchedEffect(this) {
        val pressInteractions = mutableListOf<PressInteraction.Press>()
        interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressInteractions.add(interaction)
                    pressState.location = interaction.pressPosition
                }

                is PressInteraction.Release -> {
                    pressInteractions.remove(interaction.press)
                    pressState.location = Offset.Unspecified
                }

                is PressInteraction.Cancel -> {
                    pressInteractions.remove(interaction.press)
                    pressState.location = Offset.Unspecified
                }
            }
            pressState.state = pressInteractions.isNotEmpty()
        }
    }
    return pressState
}

@Composable
private fun InteractionSource.collectIsHoveredAsState(): BooleanInteractionState {
    val hoverState by remember { mutableStateOf(MutableBooleanInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartHoverInteraction.Enter -> {
                    hoverState.state = true
                    hoverState.location = interaction.localLocation
                }

                is ChartHoverInteraction.Move -> {
                    hoverState.location = interaction.localLocation
                }

                is ChartHoverInteraction.Exit -> {
                    hoverState.state = false
                    hoverState.location = interaction.localLocation
                }
            }
        }
    }
    return hoverState
}

@Composable
private fun InteractionSource.collectTapAsState(): BooleanInteractionState {
    val tapState by remember { mutableStateOf(MutableBooleanInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.Tap -> {
                    tapState.state = true
                    tapState.location = interaction.location
                }

                is ChartTapInteraction.ExitTap -> {
                    tapState.state = false
                }
            }
        }
    }
    return tapState
}

@Composable
private fun InteractionSource.collectDoubleTapAsState(): BooleanInteractionState {
    val doubleTapState by remember { mutableStateOf(MutableBooleanInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.DoubleTap -> {
                    doubleTapState.state = true
                    doubleTapState.location = interaction.location
                }

                is ChartTapInteraction.ExitDoubleTap -> {
                    doubleTapState.state = false
                }
            }
        }
    }
    return doubleTapState
}

@Composable
private fun InteractionSource.collectLongPressAsState(): BooleanInteractionState {
    val longPressState by remember { mutableStateOf(MutableBooleanInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.LongPress -> {
                    longPressState.state = true
                    longPressState.location = interaction.location
                }

                is ChartTapInteraction.ExitLongPress -> {
                    longPressState.state = false
                }
            }
        }
    }
    return longPressState
}

