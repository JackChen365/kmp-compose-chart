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
import me.jack.compose.chart.context.chartScrollState
import me.jack.compose.chart.context.pressState
import me.jack.compose.chart.draw.interaction.hoverState
import me.jack.compose.chart.interaction.ChartHoverInteraction
import me.jack.compose.chart.interaction.ChartTapInteraction
import me.jack.compose.chart.scope.ChartScope

private fun ChartScope.isScrollInProgress(): Boolean {
    return chartContext.chartScrollState?.isScrollInProgress ?: false
}

fun ChartScope.isPressed(): Boolean {
    return !isScrollInProgress() && isPressedState()
}

private fun ChartScope.isPressedState(): Boolean {
    return chartContext.pressState.value
}

fun ChartScope.isHovered(): Boolean {
    return !isScrollInProgress() && isHoveredState()
}

private fun ChartScope.isHoveredState(): Boolean {
    return chartContext.hoverState.value
}

fun ChartScope.isHoveredOrPressed(): Boolean {
    return isHovered() || isPressed()
}

interface InteractionState {
    val isActive: Boolean
    val location: Offset
}

class MutableInteractionState : InteractionState {
    override var isActive: Boolean by mutableStateOf(false)
    override var location: Offset by mutableStateOf(Offset.Zero)
}

interface ChartInteractionStates {
    val pressState: InteractionState
    val hoverState: InteractionState
    val tapState: InteractionState
    val doubleTapState: InteractionState
    val longPressState: InteractionState

    val isPressed: Boolean
        get() = pressState.isActive

    val isHovered: Boolean
        get() = hoverState.isActive

    val isTap: Boolean
        get() = tapState.isActive

    val isDoubleTap: Boolean
        get() = pressState.isActive

    val isLongPress: Boolean
        get() = longPressState.isActive
}

@Composable
fun rememberInteractionStates(interactionSource: InteractionSource): ChartInteractionStates {
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
            longPressState = longPressState
        )
    }
}

class ChartMutableInteractionStates(
    override val pressState: InteractionState,
    override val hoverState: InteractionState,
    override val tapState: InteractionState,
    override val doubleTapState: InteractionState,
    override val longPressState: InteractionState
) : ChartInteractionStates

@Composable
private fun InteractionSource.collectIsPressedAsState(): InteractionState {
    val pressState by remember { mutableStateOf(MutableInteractionState()) }
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
            pressState.isActive = pressInteractions.isNotEmpty()
        }
    }
    return pressState
}

@Composable
private fun InteractionSource.collectIsHoveredAsState(): InteractionState {
    val hoverState by remember { mutableStateOf(MutableInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartHoverInteraction.Enter -> {
                    hoverState.isActive = true
                    hoverState.location = interaction.localLocation
                }

                is ChartHoverInteraction.Move -> {
                    hoverState.location = interaction.localLocation
                }

                is ChartHoverInteraction.Exit -> {
                    hoverState.isActive = false
                    hoverState.location = interaction.localLocation
                }
            }
        }
    }
    return hoverState
}

@Composable
private fun InteractionSource.collectTapAsState(): InteractionState {
    val tapState by remember { mutableStateOf(MutableInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.Tap -> {
                    tapState.isActive = true
                    tapState.location = interaction.location
                }

                is ChartTapInteraction.ExitTap -> {
                    tapState.isActive = false
                }
            }
        }
    }
    return tapState
}

@Composable
private fun InteractionSource.collectDoubleTapAsState(): InteractionState {
    val doubleTapState by remember { mutableStateOf(MutableInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.DoubleTap -> {
                    doubleTapState.isActive = true
                    doubleTapState.location = interaction.location
                }

                is ChartTapInteraction.ExitDoubleTap -> {
                    doubleTapState.isActive = false
                }
            }
        }
    }
    return doubleTapState
}

@Composable
private fun InteractionSource.collectLongPressAsState(): InteractionState {
    val longPressState by remember { mutableStateOf(MutableInteractionState()) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is ChartTapInteraction.LongPress -> {
                    longPressState.isActive = true
                    longPressState.location = interaction.location
                }

                is ChartTapInteraction.ExitLongPress -> {
                    longPressState.isActive = false
                }
            }
        }
    }
    return longPressState
}

