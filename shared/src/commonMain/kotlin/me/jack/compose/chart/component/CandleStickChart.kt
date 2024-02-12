package me.jack.compose.chart.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.ChartInteractionHandler
import me.jack.compose.chart.context.ChartScrollableState
import me.jack.compose.chart.context.chartInteraction
import me.jack.compose.chart.context.scrollable
import me.jack.compose.chart.context.zoom
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.draw.LazyChartCanvas
import me.jack.compose.chart.draw.interaction.elementInteraction
import me.jack.compose.chart.interaction.asElementInteraction
import me.jack.compose.chart.measure.rememberFixedContentMeasurePolicy
import me.jack.compose.chart.model.CandleData
import me.jack.compose.chart.scope.CandleStickChartScope
import me.jack.compose.chart.scope.ChartAnchor
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.rememberMaxValue
import me.jack.compose.chart.scope.rememberMinValue
import me.jack.compose.chart.theme.LocalChartTheme
import kotlin.math.absoluteValue
import kotlin.math.min

class CandleStickSpec(
    val candleStickSize: Dp = 8.dp,
    val stackLineSize: Dp = 2.dp,
)

open class CandleStickLeftSideSpec(
    val backgroundColor: Color = Color.Transparent,
    val textSize: TextUnit = 12.sp,
    val textColor: Color = Color.DarkGray,
    val size: Dp = 42.dp,
    val padding: PaddingValues = PaddingValues(4.dp),
)

/**
 * Specification for CandleStickLeftSideLabel
 */
class DarkCandleStickLeftSideSpec(
    backgroundColor: Color = Color.LightGray,
    textColor: Color = Color.White,
    textSize: TextUnit = 16.sp,
    size: Dp = 32.dp,
    padding: PaddingValues = PaddingValues(4.dp)
) : CandleStickLeftSideSpec(backgroundColor, textSize, textColor, size, padding)


val CancelStickChartContent: @Composable SingleChartScope<CandleData>.() -> Unit =
    {
        CandleStickLeftSideLabel()
        ChartBorderComponent()
        ChartIndicatorComponent()
        ChartGridDividerComponent()
        ChartContent()
    }

@Composable
fun SimpleCandleStickChart(
    modifier: Modifier = Modifier,
    candleStickSize: Dp = 32.dp,
    chartDataset: ChartDataset<CandleData>,
    spec: CandleStickSpec = LocalChartTheme.current.candleStickSpec,
    tapGestures: TapGestures<CandleData> = rememberCombinedTapGestures(),
    content: @Composable SingleChartScope<CandleData>.() -> Unit = simpleChartContent
) {
    CandleStickChart(
        modifier = modifier,
        candleStickSize = candleStickSize,
        chartDataset = chartDataset,
        spec = spec,
        tapGestures = tapGestures,
        content = content
    )
}

@Composable
fun CandleStickChart(
    modifier: Modifier = Modifier,
    candleStickSize: Dp = 32.dp,
    chartDataset: ChartDataset<CandleData>,
    spec: CandleStickSpec = LocalChartTheme.current.candleStickSpec,
    tapGestures: TapGestures<CandleData> = rememberCombinedTapGestures(),
    scrollableState: ChartScrollableState? = null,
    content: @Composable (SingleChartScope<CandleData>.() -> Unit) = CancelStickChartContent
) {
    val contentMeasurePolicy = rememberFixedContentMeasurePolicy(candleStickSize.toPx())
    SingleChartLayout(
        modifier = modifier,
        chartContext = ChartContext
            .chartInteraction(
                remember { MutableInteractionSource() }
            ).scrollable(
                orientation = contentMeasurePolicy.orientation
            )
            .zoom(),
        tapGestures = tapGestures,
        contentMeasurePolicy = contentMeasurePolicy,
        scrollableState = scrollableState,
        chartDataset = chartDataset,
        content = content
    ) {
        ChartCandleStickComponent(spec)
        ChartCandleDataMarkerComponent()
    }
}

@Composable
fun CandleStickChartScope.ChartCandleStickComponent(
    spec: CandleStickSpec = LocalChartTheme.current.candleStickSpec
) {
    val candleStickWidth = spec.candleStickSize.toPx()
    val stickLineWidth = spec.stackLineSize.toPx()
    val highestValue = chartDataset.rememberMaxValue { it.high }
    LazyChartCanvas(
        modifier = Modifier.fillMaxSize()
    ) { candleData ->
        val candleBlockSize = size.height / highestValue
        // we calculate the lastVisibleItemIndex due to other places need it.
        clickableRect(currentLeftTopOffset, childSize)
        drawRect(
            color = Color.Blue whenPressedAnimateTo Color.Blue.copy(alpha = 0.4f),
            topLeft = Offset(
                x = currentLeftTopOffset.x + (childSize.mainAxis - stickLineWidth) / 2,
                y = candleBlockSize * candleData.low
            ),
            size = Size(
                width = stickLineWidth,
                height = candleBlockSize * (candleData.high - candleData.low)
            )
        )
        val color = if (candleData.open > candleData.close) Color.Red else Color.Green
        drawRect(
            color = color whenPressedAnimateTo color.copy(alpha = 0.4f),
            topLeft = Offset(
                x = currentLeftTopOffset.x + (childSize.mainAxis - candleStickWidth) / 2,
                y = candleBlockSize * min(candleData.open, candleData.close)
            ),
            size = Size(
                width = candleStickWidth,
                height = candleBlockSize * (candleData.open - candleData.close).absoluteValue
            )
        )
    }
}

@Composable
fun CandleStickChartScope.ChartCandleDataMarkerComponent() {
    val elementInteraction = chartContext.elementInteraction.asElementInteraction<CandleData>()
    val currentItem = elementInteraction?.currentItem
    val drawElement = elementInteraction?.drawElement
    if (null != currentItem && null != drawElement && drawElement is DrawElement.Rect) {
        HoverMarkerComponent(
            topLeft = drawElement.topLeft,
            contentSize = drawElement.size,
        )
        MarkerDashLineComponent(
            topLeft = drawElement.topLeft,
            contentSize = drawElement.size,
            focusPoint = drawElement.focusPoint
        )
        MarkerComponent(
            leftTop = drawElement.topLeft,
            size = drawElement.size,
            focusPoint = drawElement.focusPoint,
            displayInfo = "(" + currentItem.high + "-" + currentItem.low + ")" +
                    "(" + currentItem.open + "-" + currentItem.close + ")"
        )
    }
}

@Composable
fun CandleStickChartScope.CandleStickLeftSideLabel(
    spec: CandleStickLeftSideSpec = LocalChartTheme.current.candleStickLeftSideSpec
) {
    val textMeasurer = rememberTextMeasurer()
    val lowest = chartDataset.rememberMinValue { it.low }
    val highest = chartDataset.rememberMaxValue { it.high }
    Canvas(
        modifier = Modifier
            .anchor(ChartAnchor.Start)
            .widthIn(min = spec.size)
            .padding(spec.padding)
            .background(color = spec.backgroundColor)
            .fillMaxHeight()
    ) {
        var textLayoutResult = textMeasurer.measure(
            text = if (0 == chartDataset.size) "#" else highest.toString(),
            style = TextStyle(color = spec.textColor, fontSize = spec.textSize)
        )
        // at the top of the rect.
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset((size.width - textLayoutResult.size.width) / 2f, 0f)
        )
        // at the bottom of the rect.
        textLayoutResult = textMeasurer.measure(
            text = if (0 == chartDataset.size) "#" else lowest.toString(),
            style = TextStyle(color = spec.textColor, fontSize = spec.textSize)
        )
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                (size.width - textLayoutResult.size.width) / 2f,
                size.height - textLayoutResult.size.height,
            )
        )
    }
}

open class CandleStickBarSpec(
    val color: Color = Color.LightGray,
    val pressedColor: Color = Color.Transparent,
) : ChartComponentSpec

class DarkCandleStickBarSpec(
    color: Color = Color.LightGray.copy(0.2f),
    pressedColor: Color = Color.Transparent,
) : CandleStickBarSpec(color, pressedColor)


@Composable
fun CandleStickChartScope.CandleStickBarComponent(
    spec: CandleStickBarSpec = LocalChartTheme.current.candleStickBarSpec
) {
    val maxValue = chartDataset.rememberMaxValue { it.high }
    LazyChartCanvas(
        modifier = Modifier
            .anchor(ChartAnchor.Bottom)
            .height(120.dp)
            .clipToBounds()
    ) { current ->
        val cancelStickSize = size.crossAxis / maxValue
        drawRect(
            color = spec.color whenPressedAnimateTo spec.pressedColor,
            topLeft = Offset(currentLeftTopOffset.x, size.height - cancelStickSize * current.open),
            size = Size(childSize.mainAxis, cancelStickSize * current.open)
        )
    }
}

@Composable
fun CandleStickChartScope.CandleStickScrollableBarComponent(
    context: ChartContext = chartContext,
    spec: CandleStickBarSpec = LocalChartTheme.current.candleStickBarSpec
) {
    val maxValue = chartDataset.rememberMaxValue { it.high }
    ChartBox(
        modifier = Modifier
            .anchor(ChartAnchor.Bottom)
            .height(120.dp)
            .clipToBounds(),
        chartContext = context.minusKey(ChartInteractionHandler)
    ) {
        LazyChartCanvas(
            modifier = Modifier.fillMaxSize()
        ) { current ->
            val cancelStickSize = size.crossAxis / maxValue
            drawRect(
                color = spec.color whenPressedAnimateTo spec.pressedColor,
                topLeft = Offset(currentLeftTopOffset.x, size.height - cancelStickSize * current.open),
                size = Size(childSize.mainAxis, cancelStickSize * current.open)
            )
        }
    }
}