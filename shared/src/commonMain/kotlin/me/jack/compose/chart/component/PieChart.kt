package me.jack.compose.chart.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.chartInteraction
import me.jack.compose.chart.draw.LazyChartCanvas
import me.jack.compose.chart.measure.rememberBoxChartContentMeasurePolicy
import me.jack.compose.chart.model.PieData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.PieChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.rememberSumValue
import me.jack.compose.chart.theme.LocalChartTheme

class PieSpec(
    val pressedScale: Float = 1.10f,
    val pressedAlpha: Float = 0.8f,
    val padding: Dp = 12.dp
) : ChartComponentSpec

@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<PieData>,
    tapGestures: TapGestures<PieData> = rememberCombinedTapGestures(),
    spec: PieSpec = LocalChartTheme.current.pieSpec,
    content: @Composable SingleChartScope<PieData>.() -> Unit = simpleChartContent
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
        PieComponent(spec = spec)
    }
}

@Composable
private fun PieChartScope.PieComponent(
    modifier: Modifier = Modifier,
    spec: PieSpec = LocalChartTheme.current.pieSpec,
) {
    val maxValue = chartDataset.rememberSumValue { it.value }
    val degreesValue = maxValue / 360f
    var angleOffset = 0f
    LazyChartCanvas(
        modifier = modifier
            .fillMaxSize()
            .padding(spec.padding)
    ) { pieData ->
        val arcSize = Size(size.minDimension, size.minDimension)
        val sweepAngle = pieData.value / degreesValue
        clickable {
            drawArc(
                color = pieData.color whenPressedAnimateTo pieData.color.copy(alpha = spec.pressedAlpha),
                startAngle = angleOffset,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(
                    x = (size.width - size.minDimension) / 2,
                    y = (size.height - size.minDimension) / 2
                ) whenPressedAnimateTo Offset(
                    x = (size.width - size.minDimension * spec.pressedScale) / 2,
                    y = (size.height - size.minDimension * spec.pressedScale) / 2
                ),
                size = arcSize whenPressedAnimateTo arcSize.times(spec.pressedScale),
                style = Fill
            )
        }
        angleOffset += sweepAngle
    }
}