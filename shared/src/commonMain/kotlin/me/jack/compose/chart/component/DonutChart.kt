package me.jack.compose.chart.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.chartInteraction
import me.jack.compose.chart.draw.LazyChartCanvas
import me.jack.compose.chart.measure.rememberBoxChartContentMeasurePolicy
import me.jack.compose.chart.model.PieData
import me.jack.compose.chart.model.SimplePieData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.DonutChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.rememberSumValue
import me.jack.compose.chart.theme.LocalChartTheme

typealias DonutData = PieData
typealias SimpleDonutData = SimplePieData

class DonutSpec(
    val padding: PaddingValues = PaddingValues(8.dp),
    val strokeWidth: Dp = 36.dp,
    val pressedScale: Float = 1.1f,
    val pressedAlpha: Float = 0.8f
) : ChartComponentSpec

@Composable
fun DonutChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<DonutData>,
    spec: DonutSpec = LocalChartTheme.current.donutSpec,
    tapGestures: TapGestures<DonutData> = rememberCombinedTapGestures(),
    content: @Composable SingleChartScope<DonutData>.() -> Unit = simpleChartContent
) {
    val chartContext = remember {
        ChartContext.chartInteraction(MutableInteractionSource())
    }
    SingleChartLayout(
        modifier = modifier,
        chartContext = chartContext,
        tapGestures = tapGestures,
        contentMeasurePolicy = rememberBoxChartContentMeasurePolicy(),
        chartDataset = chartDataset,
        content = content
    ) {
        DonutComponent(spec = spec)
    }
}

@Composable
fun DonutChartScope.DonutComponent(
    modifier: Modifier = Modifier,
    spec: DonutSpec = LocalChartTheme.current.donutSpec
) {
    val maxValue = chartDataset.rememberSumValue { it.value }
    val degreesValue = maxValue / 360f
    var angleOffset = 0f
    LazyChartCanvas(
        modifier = modifier
            .fillMaxSize()
            .padding(spec.padding)
    ) { current ->
        val strokeWidthPx = spec.strokeWidth.toPx()
        val arcSize = Size(
            width = size.minDimension - strokeWidthPx,
            height = size.minDimension - strokeWidthPx
        )
        val sweepAngle = current.value / degreesValue
        clickable {
            drawArc(
                color = current.color whenPressedAnimateTo current.color.copy(alpha = spec.pressedAlpha),
                startAngle = angleOffset,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    x = (size.width - arcSize.width) / 2,
                    y = (size.height - arcSize.height) / 2
                ) whenPressedAnimateTo Offset(
                    x = (size.width - arcSize.width * spec.pressedScale) / 2,
                    y = (size.height - arcSize.height * spec.pressedScale) / 2
                ),
                size = arcSize whenPressedAnimateTo arcSize.times(spec.pressedScale),
                style = Stroke(strokeWidthPx)
            )
        }
        angleOffset += sweepAngle
    }
}