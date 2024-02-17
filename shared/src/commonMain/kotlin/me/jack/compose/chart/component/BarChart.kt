package me.jack.compose.chart.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.ChartScrollableState
import me.jack.compose.chart.context.scrollable
import me.jack.compose.chart.context.zoom
import me.jack.compose.chart.draw.ChartCanvas
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.draw.LazyChartCanvas
import me.jack.compose.chart.measure.ChartContentMeasurePolicy
import me.jack.compose.chart.model.BarData
import me.jack.compose.chart.scope.BarChartScope
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.MarkedChartDataset
import me.jack.compose.chart.scope.computeGroupTotalValues
import me.jack.compose.chart.scope.drawElementInteraction
import me.jack.compose.chart.scope.fastForEachByGroupIndex
import me.jack.compose.chart.scope.isHorizontal
import me.jack.compose.chart.scope.isLastGroupIndex
import me.jack.compose.chart.scope.rememberMaxValue

enum class BarStyle {
    Normal, Stack
}

val BarChartContent: @Composable BarChartScope.() -> Unit = {
    ChartBorderComponent()
    ChartGridDividerComponent()
    ChartAverageAcrossRanksComponent { it.value }
    ChartIndicatorComponent()
    ChartContent()
}

@Composable
fun SimpleBarChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<BarData>,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    barStyle: BarStyle = BarStyle.Normal,
    tapGestures: TapGestures<BarData> = rememberCombinedTapGestures(),
    content: @Composable BarChartScope.() -> Unit = simpleChartContent
) {
    BarChart(
        modifier = modifier,
        barStyle = barStyle,
        contentMeasurePolicy = contentMeasurePolicy,
        chartDataset = chartDataset,
        tapGestures = tapGestures,
        content = content
    )
}

@Composable
fun BarChart(
    modifier: Modifier = Modifier,
    barStyle: BarStyle = BarStyle.Normal,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    chartDataset: ChartDataset<BarData>,
    tapGestures: TapGestures<BarData> = rememberCombinedTapGestures(),
    scrollableState: ChartScrollableState? = null,
    content: @Composable (BarChartScope.() -> Unit) = BarChartContent
) {
    val chartContext = remember {
        ChartContext
            .scrollable(
                orientation = contentMeasurePolicy.orientation
            ).zoom()
    }
    SingleChartLayout(
        modifier = modifier,
        chartContext = chartContext,
        tapGestures = tapGestures,
        contentMeasurePolicy = contentMeasurePolicy,
        scrollableState = scrollableState,
        chartDataset = chartDataset,
        content = content
    ) {
        when (barStyle) {
            BarStyle.Normal -> {
                BarComponent()
                BarMarkerComponent()
            }

            BarStyle.Stack -> {
                BarStackComponent()
                BarMarkerComponent()
            }
        }
    }
}

/**
 * The standard bar component.
 * The component in [BarChartScope] and it helps generate the bar by [BarData]
 */
@Composable
fun BarChartScope.BarComponent() {
    val maxValue = chartDataset.rememberMaxValue { it.value }
    if (0 >= maxValue) return
    LazyChartCanvas(
        modifier = Modifier.fillMaxSize()
    ) { current ->
        val barItemSize = size.crossAxis / maxValue
        val (topLeft, size) = if (isHorizontal) {
            Offset(currentLeftTopOffset.x, size.height - barItemSize * current.value) to
                    Size(childSize.mainAxis, barItemSize * current.value)
        } else {
            Offset(0f, currentLeftTopOffset.y) to
                    Size(barItemSize * current.value, childSize.mainAxis)
        }
        interactionRect(topLeft, size)
        drawRect(
            color = current.color whenPressedAnimateTo current.color.copy(alpha = 0.4f),
            topLeft = topLeft,
            size = size
        )
    }
}

@Composable
fun BarChartScope.BarStackComponent() {
    val sumValueSet = remember(chartDataset) {
        chartDataset.computeGroupTotalValues { it.value }
    }
    val maxValue = remember(sumValueSet) {
        sumValueSet.maxOf { it }
    }
    var offset = 0f
    ChartCanvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val barItemSize = size.crossAxis / maxValue
        fastForEachByGroupIndex { current ->
            val (topLeft, rectSize) = if (isHorizontal) {
                Offset(
                    currentLeftTopOffset.x, size.height - offset - barItemSize * current.value
                ) to Size(childSize.mainAxis, barItemSize * current.value)
            } else {
                Offset(offset, currentLeftTopOffset.y) to Size(barItemSize * current.value, childSize.mainAxis)
            }
            interactionRect(topLeft, rectSize)
            drawRect(
                color = current.color whenPressedAnimateTo current.color.copy(alpha = 0.4f),
                topLeft = topLeft,
                size = rectSize
            )
            offset = if (isLastGroupIndex()) 0f else offset + barItemSize * current.value
        }
    }
}

@Composable
fun BarChartScope.BarMarkerComponent() {
    val (drawElement, current) = drawElementInteraction<BarData, DrawElement.Rect>() ?: return
    MarkerDashLineComponent(
        drawElement = drawElement,
        topLeft = drawElement.topLeft,
        contentSize = drawElement.size
    )
    RectMarkerComponent(
        topLeft = drawElement.topLeft,
        size = drawElement.size,
        focusPoint = drawElement.focusPoint,
        displayInfo = "(" + current.value.toString() + ")"
    )
}

@Composable
fun BarChartScope.BarStickMarkComponent(
    markedChartDataset: MarkedChartDataset = MarkedChartDataset(),
    color: Color = MaterialTheme.colorScheme.primary,
    radius: Dp = 8.dp
) {
    val maxValue = chartDataset.rememberMaxValue { it.value }
    if (0 >= maxValue) return
    LazyChartCanvas(
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        val barItemSize = size.crossAxis / maxValue
        if (markedChartDataset.contains(groupIndex, index)) {
            val markedData = markedChartDataset.getMarkedData(groupIndex, index)
            if (isHorizontal) {
                drawCircle(
                    color = color,
                    center = Offset(childCenterOffset.x, size.height - barItemSize * markedData),
                    radius = radius.toPx()
                )
            } else {
                drawCircle(
                    color = color,
                    center = Offset(barItemSize * markedData, childCenterOffset.y),
                    radius = radius.toPx()
                )
            }
        }
    }
}