package me.jack.compose.chart.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.ChartScrollableState
import me.jack.compose.chart.context.chartInteraction
import me.jack.compose.chart.context.scrollable
import me.jack.compose.chart.context.zoom
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.draw.LazyChartCanvas
import me.jack.compose.chart.draw.interaction.elementInteraction
import me.jack.compose.chart.interaction.asElementInteraction
import me.jack.compose.chart.measure.ChartContentMeasurePolicy
import me.jack.compose.chart.model.BubbleData
import me.jack.compose.chart.scope.BubbleChartScope
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.isHorizontal
import me.jack.compose.chart.scope.rememberMaxValue
import me.jack.compose.chart.theme.LocalChartTheme

class BubbleSpec(
    val maxRadius: Dp = 40.dp,
) : ChartComponentSpec

val BubbleChartContent: @Composable SingleChartScope<BubbleData>.() -> Unit = {
    ChartBorderComponent()
    ChartGridDividerComponent()
    ChartIndicatorComponent()
    ChartContent()
}

@Composable
fun SimpleBubbleChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<BubbleData>,
    bubbleSpec: BubbleSpec = LocalChartTheme.current.bubbleSpec,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    tapGestures: TapGestures<BubbleData> = rememberCombinedTapGestures(),
    content: @Composable SingleChartScope<BubbleData>.() -> Unit = simpleChartContent
) {
    BubbleChart(
        modifier = modifier,
        chartDataset = chartDataset,
        bubbleSpec = bubbleSpec,
        contentMeasurePolicy = contentMeasurePolicy,
        tapGestures = tapGestures,
        content = content
    )
}

@Composable
fun BubbleChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<BubbleData>,
    bubbleSpec: BubbleSpec = LocalChartTheme.current.bubbleSpec,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    tapGestures: TapGestures<BubbleData> = rememberCombinedTapGestures(),
    scrollableState: ChartScrollableState? = null,
    content: @Composable SingleChartScope<BubbleData>.() -> Unit = BubbleChartContent
) {
    SingleChartLayout(
        modifier = modifier,
        chartContext = ChartContext
            .chartInteraction(remember { MutableInteractionSource() })
            .scrollable(
                orientation = contentMeasurePolicy.orientation
            )
            .zoom(),
        tapGestures = tapGestures,
        contentMeasurePolicy = contentMeasurePolicy,
        scrollableState = scrollableState,
        chartDataset = chartDataset,
        content = content
    ) {
        BubbleComponent(bubbleSpec)
        BubbleMarkerComponent()
    }
}

@Composable
fun SingleChartScope<BubbleData>.BubbleMarkerComponent() {
    val pressInteraction = chartContext.elementInteraction.asElementInteraction<BubbleData>() ?: return
    val currentItem = pressInteraction.currentItem
    val drawElement = pressInteraction.drawElement
    if (drawElement is DrawElement.Circle) {
        MarkerDashLineComponent(
            topLeft = Offset(
                x = drawElement.center.x - drawElement.radius,
                y = drawElement.center.y - drawElement.radius
            ),
            contentSize = Size(
                width = 2 * drawElement.radius,
                height = 2 * drawElement.radius
            ),
            focusPoint = drawElement.center
        )
        MarkerComponent(
            leftTop = Offset(
                x = drawElement.center.x - drawElement.radius,
                y = drawElement.center.y - drawElement.radius
            ),
            size = Size(
                width = 2 * drawElement.radius,
                height = 2 * drawElement.radius
            ),
            focusPoint = drawElement.center,
            displayInfo = "(" + currentItem.value.toString() + ")"
        )
    }
}

@Composable
fun BubbleChartScope.BubbleComponent(
    bubbleSpec: BubbleSpec = LocalChartTheme.current.bubbleSpec
) {
    val maxValue = chartDataset.rememberMaxValue { it.value }
    val maxVolume = chartDataset.rememberMaxValue { it.volume }
    val volumeSize = bubbleSpec.maxRadius.toPx() / maxVolume
    LazyChartCanvas(
        modifier = Modifier.fillMaxSize()
    ) { current ->
        val bubbleItemSize = size.height / maxValue
        clickable {
            drawCircle(
                color = current.color whenPressedAnimateTo current.color.copy(alpha = 0.8f),
                radius = (current.volume * volumeSize) whenPressedAnimateTo (current.volume * volumeSize * 1.2f),
                center = if (isHorizontal) Offset(
                    x = childCenterOffset.x,
                    y = size.crossAxis - current.value * bubbleItemSize
                ) else Offset(
                    x = size.crossAxis - current.value * bubbleItemSize,
                    y = childCenterOffset.y
                )
            )
        }
    }
}