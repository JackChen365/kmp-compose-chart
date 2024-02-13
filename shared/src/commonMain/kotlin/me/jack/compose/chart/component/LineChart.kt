package me.jack.compose.chart.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.ChartScrollableState
import me.jack.compose.chart.context.chartInteraction
import me.jack.compose.chart.context.scrollable
import me.jack.compose.chart.context.zoom
import me.jack.compose.chart.draw.ChartCanvas
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.draw.LazyChartCanvas
import me.jack.compose.chart.measure.ChartContentMeasurePolicy
import me.jack.compose.chart.measure.rememberFixedContentMeasurePolicy
import me.jack.compose.chart.model.LineData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.LineChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.currentRange
import me.jack.compose.chart.scope.fastForEachWithNext
import me.jack.compose.chart.scope.isFirstIndex
import me.jack.compose.chart.scope.isHorizontal
import me.jack.compose.chart.scope.rememberMaxValue
import me.jack.compose.chart.scope.withChartElementInteraction
import me.jack.compose.chart.theme.LocalChartTheme
import kotlin.math.max

private val DEFAULT_CROSS_AXIS_SIZE = 32.dp

open class LineSpec(
    val backgroundColor: Color = Color.White,
    val strokeWidth: Dp = 4.dp,
    val circleRadius: Dp = 8.dp,
    val pressAlpha: Float = 0.4f
) : ChartComponentSpec

class DarkLineSpec(
    backgroundColor: Color = Color.DarkGray,
    strokeWidth: Dp = 4.dp,
    circleRadius: Dp = 8.dp,
    pressAlpha: Float = 0.4f
) : LineSpec(backgroundColor, strokeWidth, circleRadius, pressAlpha)

class CurveLineSpec(
    val strokeWidth: Dp = 4.dp,
    val circleRadius: Dp = 8.dp,
    val pressAlpha: Float = 0.4f,
    val style: DrawStyle = Fill
) : ChartComponentSpec

val LineChartContent: @Composable SingleChartScope<LineData>.() -> Unit = {
    ChartBorderComponent()
    ChartGridDividerComponent()
    ChartAverageAcrossRanksComponent { it.value }
    ChartIndicatorComponent()
    ChartContent()
}

val CurveLineChartContent: @Composable LineChartScope.() -> Unit = {
    ChartBorderComponent()
    ChartGridDividerComponent()
    ChartAverageAcrossRanksComponent { it.value }
    ChartIndicatorComponent()
    ChartContent()
}

@Composable
fun SimpleLineChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<LineData>,
    lineSpec: LineSpec = LocalChartTheme.current.lineSpec,
    contentMeasurePolicy: ChartContentMeasurePolicy = rememberFixedContentMeasurePolicy(DEFAULT_CROSS_AXIS_SIZE.toPx()),
    tapGestures: TapGestures<LineData> = rememberCombinedTapGestures(),
    content: @Composable SingleChartScope<LineData>.() -> Unit = simpleChartContent
) {
    LineChart(
        modifier = modifier,
        chartDataset = chartDataset,
        lineSpec = lineSpec,
        contentMeasurePolicy = contentMeasurePolicy,
        tapGestures = tapGestures,
        content = content
    )
}

@Composable
fun LineChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<LineData>,
    lineSpec: LineSpec = LocalChartTheme.current.lineSpec,
    contentMeasurePolicy: ChartContentMeasurePolicy = rememberFixedContentMeasurePolicy(DEFAULT_CROSS_AXIS_SIZE.toPx()),
    tapGestures: TapGestures<LineData> = rememberCombinedTapGestures(),
    scrollableState: ChartScrollableState? = null,
    content: @Composable SingleChartScope<LineData>.() -> Unit = LineChartContent
) {
    SingleChartLayout(
        modifier = modifier.background(lineSpec.backgroundColor),
        chartContext = ChartContext.chartInteraction(remember { MutableInteractionSource() })
            .scrollable(
                orientation = contentMeasurePolicy.orientation
            )
            .zoom(),
        tapGestures = tapGestures,
        contentMeasurePolicy = contentMeasurePolicy,
        chartDataset = chartDataset,
        scrollableState = scrollableState,
        content = content
    ) {
        ChartLineComponent(lineSpec = lineSpec)
        LineMarkerComponent()
    }
}

@Composable
fun StockLineChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<LineData>,
    contentMeasurePolicy: ChartContentMeasurePolicy = rememberFixedContentMeasurePolicy(DEFAULT_CROSS_AXIS_SIZE.toPx()),
    tapGestures: TapGestures<LineData> = rememberCombinedTapGestures(),
    scrollableState: ChartScrollableState? = null,
    content: @Composable SingleChartScope<LineData>.() -> Unit = LineChartContent
) {
    SingleChartLayout(
        modifier = modifier,
        chartContext = ChartContext.chartInteraction(remember { MutableInteractionSource() })
            .scrollable(
                orientation = contentMeasurePolicy.orientation
            )
            .zoom(),
        tapGestures = tapGestures,
        contentMeasurePolicy = contentMeasurePolicy,
        chartDataset = chartDataset,
        scrollableState = scrollableState,
        content = content
    ) {
        ChartStockLineComponent()
        LineMarkerComponent()
    }
}

@Composable
fun LineChartScope.LineMarkerComponent() {
    withChartElementInteraction<LineData, DrawElement.Rect> { drawElement, _, currentGroupItems ->
        MarkerDashLineComponent(
            drawElement = drawElement,
            topLeft = drawElement.topLeft,
            contentSize = drawElement.size
        )
        MarkerComponent(
            topLeft = drawElement.topLeft,
            contentSize = drawElement.size,
            focusPoint = drawElement.focusPoint,
            displayInfo = "(" + currentGroupItems.joinToString { String.format("%.2f", it.value) } + ")"
        )
    }
}

@Composable
fun SingleChartScope<LineData>.ChartLineComponent(
    lineSpec: LineSpec = LocalChartTheme.current.lineSpec
) {
    val maxValue = chartDataset.rememberMaxValue { it.value }
    val circleRadiusPx = lineSpec.circleRadius.toPx()
    LazyChartCanvas(
        modifier = Modifier.fillMaxSize()
    ) { current, next ->
        val lineItemSize = size.crossAxis / maxValue
        drawLine(
            color = current.color,
            start = if (isHorizontal) Offset(
                x = childCenterOffset.x,
                y = size.height - current.value * lineItemSize
            ) else Offset(
                x = size.width - current.value * lineItemSize,
                y = childCenterOffset.y
            ),
            end = if (isHorizontal) Offset(
                x = nextChildCenterOffset.x,
                y = size.height - next.value * lineItemSize
            ) else Offset(
                x = size.width - next.value * lineItemSize,
                y = 0f
            ),
            strokeWidth = lineSpec.strokeWidth.toPx()
        )
        if (isFirstIndex()) {
            clickableRectWithInteraction(
                topLeft = currentLeftTopOffset,
                size = childSize,
                focusPoint = Offset(
                    x = childCenterOffset.x,
                    y = size.height - current.value * lineItemSize,
                )
            )
            drawCircle(
                color = current.color whenPressedAnimateTo current.color.copy(alpha = lineSpec.pressAlpha),
                radius = circleRadiusPx whenPressedAnimateTo circleRadiusPx * 1.4f,
                center = Offset(
                    x = childCenterOffset.x,
                    y = size.height - current.value * lineItemSize,
                )
            )
        }
        clickableRectWithInteraction(
            topLeft = nextLeftTopOffset,
            size = childSize,
            focusPoint = Offset(
                x = nextChildCenterOffset.x,
                y = size.height - next.value * lineItemSize,
            )
        )
        drawCircle(
            color = current.color whenPressedAnimateTo next.color.copy(alpha = lineSpec.pressAlpha),
            radius = circleRadiusPx whenPressedAnimateTo circleRadiusPx * 1.4f,
            center = Offset(
                x = nextChildCenterOffset.x,
                y = size.height - next.value * lineItemSize,
            )
        )
    }
}

@Composable
fun SimpleCurveLineChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<LineData>,
    lineSpec: CurveLineSpec = LocalChartTheme.current.curveLineSpec,
    contentMeasurePolicy: ChartContentMeasurePolicy = rememberFixedContentMeasurePolicy(DEFAULT_CROSS_AXIS_SIZE.toPx()),
    tapGestures: TapGestures<LineData> = rememberCombinedTapGestures(),
    content: @Composable SingleChartScope<LineData>.() -> Unit = simpleChartContent
) {
    SingleChartLayout(
        modifier = modifier,
        chartContext = ChartContext.chartInteraction(remember { MutableInteractionSource() })
            .scrollable(
                orientation = contentMeasurePolicy.orientation
            )
            .zoom(),
        tapGestures = tapGestures,
        contentMeasurePolicy = contentMeasurePolicy,
        chartDataset = chartDataset,
        content = { content() }
    ) {
        CurveLineComponent(lineSpec)
        LineMarkerComponent()
    }
}

@Composable
fun CurveLineChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<LineData>,
    lineSpec: CurveLineSpec = LocalChartTheme.current.curveLineSpec,
    contentMeasurePolicy: ChartContentMeasurePolicy = rememberFixedContentMeasurePolicy(DEFAULT_CROSS_AXIS_SIZE.toPx()),
    tapGestures: TapGestures<LineData> = rememberCombinedTapGestures(),
    scrollableState: ChartScrollableState? = null,
    content: @Composable LineChartScope.() -> Unit = CurveLineChartContent
) {
    SingleChartLayout(
        modifier = modifier,
        chartContext = ChartContext.chartInteraction(remember { MutableInteractionSource() })
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
        CurveLineComponent(lineSpec)
        LineMarkerComponent()
    }
}

@Composable
fun SingleChartScope<LineData>.CurveLineComponent(
    lineSpec: CurveLineSpec = LocalChartTheme.current.curveLineSpec
) {
    HorizontalCurveLine(lineSpec)
}

@Composable
private fun SingleChartScope<LineData>.HorizontalCurveLine(
    spec: CurveLineSpec = LocalChartTheme.current.curveLineSpec,
) {
    val path = remember { Path() }
    val maxValue = chartDataset.rememberMaxValue { it.value }
    ChartCanvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val range = currentRange
        val strokeWidthPx = spec.strokeWidth.toPx()
        val lineItemSize = size.crossAxis / maxValue
        val start = max(0, range.first - 1)
        val end = range.last + 1
        fastForEachWithNext(start, end) { current, next ->
            val currentOffset = childCenterOffset.copy(y = size.height - current.value * lineItemSize)
            val nextOffset = nextChildCenterOffset.copy(y = size.height - next.value * lineItemSize)
            val firstControlPoint = Offset(
                x = currentOffset.x + (nextOffset.x - currentOffset.x) / 2F,
                y = currentOffset.y,
            )
            val secondControlPoint = Offset(
                x = currentOffset.x + (nextOffset.x - currentOffset.x) / 2F,
                y = nextOffset.y,
            )
            if (0 == index) {
                path.reset()
                path.moveTo(currentOffset.x - childOffsets.mainAxis, currentOffset.y)
                path.lineTo(currentOffset.x, currentOffset.y)
            } else if (index == start) {
                path.reset()
                path.moveTo(currentOffset.x, size.height)
                path.lineTo(currentOffset.x, currentOffset.y)
            }
            path.cubicTo(
                x1 = firstControlPoint.x,
                y1 = firstControlPoint.y,
                x2 = secondControlPoint.x,
                y2 = secondControlPoint.y,
                x3 = nextOffset.x,
                y3 = nextOffset.y,
            )
            // add clickable rect
            clickableRectWithInteraction(
                topLeft = currentLeftTopOffset,
                size = Size(width = childSize.width, size.height),
                focusPoint = currentOffset
            )
            drawCircle(
                color = current.color whenPressedAnimateTo current.color.copy(alpha = spec.pressAlpha),
                radius = 0f whenPressedAnimateTo spec.circleRadius.toPx(),
                center = currentOffset
            )
            if (index + 1 == range.last) {
                clickableRectWithInteraction(
                    topLeft = nextLeftTopOffset,
                    size = Size(width = childSize.width, size.height),
                    focusPoint = nextChildCenterOffset.copy(y = nextOffset.y)
                )
                drawCircle(
                    color = next.color whenPressedAnimateTo next.color.copy(alpha = spec.pressAlpha),
                    radius = 0f whenPressedAnimateTo spec.circleRadius.toPx(),
                    center = nextChildCenterOffset.copy(y = nextOffset.y)
                )
                val currentItem: LineData = currentItem()
                path.lineTo(nextOffset.x + childSize.mainAxis / 2 + strokeWidthPx, nextOffset.y)
                drawPath(
                    path = path,
                    color = currentItem.color,
                    style = Stroke(strokeWidthPx),
                )
                path.lineTo(nextOffset.x + childSize.mainAxis / 2 + strokeWidthPx, size.height + strokeWidthPx)
                path.lineTo(nextOffset.x + childSize.mainAxis / 2, size.height)
                path.lineTo(0f, size.height)
                drawPath(
                    path = path,
                    color = currentItem.color.copy(alpha = spec.pressAlpha),
                    style = spec.style
                )
            }
        }
    }
}

@Composable
fun SingleChartScope<LineData>.ChartStockLineComponent(
    strokeWidth: Dp = 1.dp,
) {
    val maxValue = chartDataset.rememberMaxValue { it.value }
    LazyChartCanvas(
        modifier = Modifier.fillMaxSize()
    ) { current, next ->
        val lineItemSize = size.crossAxis / maxValue
        drawLine(
            color = current.color,
            start = if (isHorizontal) Offset(
                x = childCenterOffset.x,
                y = current.value * lineItemSize
            ) else Offset(
                x = current.value * lineItemSize,
                y = childCenterOffset.y
            ),
            end = if (isHorizontal) Offset(
                x = nextChildCenterOffset.x,
                y = next.value * lineItemSize
            ) else Offset(
                x = size.width - next.value * lineItemSize,
                y = 0f
            ),
            strokeWidth = strokeWidth.toPx()
        )
    }
}
