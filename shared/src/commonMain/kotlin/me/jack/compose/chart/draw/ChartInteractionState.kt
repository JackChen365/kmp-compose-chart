package me.jack.compose.chart.draw

import me.jack.compose.chart.context.chartScrollState
import me.jack.compose.chart.context.pressState
import me.jack.compose.chart.draw.interaction.hoverState
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