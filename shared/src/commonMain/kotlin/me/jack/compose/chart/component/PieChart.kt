package me.jack.compose.chart.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.draw.LazyChartCanvas
import me.jack.compose.chart.measure.rememberBoxChartContentMeasurePolicy
import me.jack.compose.chart.model.PieData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.PieChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.drawElementInteraction
import me.jack.compose.chart.scope.isFirstGroupAndFirstIndex
import me.jack.compose.chart.scope.rememberSumValue
import me.jack.compose.chart.theme.LocalChartTheme
import me.jack.compose.chart.util.convertAngleToCoordinates

class PieSpec(
    val pressedScale: Float = 1.10f,
    val pressedAlpha: Float = 0.8f
) : ChartComponentSpec

@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    chartDataset: ChartDataset<PieData>,
    tapGestures: TapGestures<PieData> = rememberCombinedTapGestures(),
    spec: PieSpec = LocalChartTheme.current.pieSpec,
    content: @Composable SingleChartScope<PieData>.() -> Unit = simpleChartContent
) {
    SingleChartLayout(
        modifier = modifier,
        chartContext = ChartContext,
        tapGestures = tapGestures,
        contentMeasurePolicy = rememberBoxChartContentMeasurePolicy(),
        chartDataset = chartDataset,
        content = content
    ) {
        PieComponent(spec = spec)
        PieMarkerComponent()
        PieTextComponent()
    }
}

@Composable
private fun PieChartScope.PieComponent(
    modifier: Modifier = Modifier,
    spec: PieSpec = LocalChartTheme.current.pieSpec
) {
    val maxValue = chartDataset.rememberSumValue { it.value }
    val degreesValue = maxValue / 360f
    var angleOffset = 0f
    LazyChartCanvas(
        modifier = modifier.fillMaxSize()
    ) { pieData ->
        if (isFirstGroupAndFirstIndex()) {
            angleOffset = 0f
        }
        val arcSize = Size(size.minDimension, size.minDimension)
        val sweepAngle = pieData.value / degreesValue
        interactionArc(
            startAngle = angleOffset,
            sweepAngle = sweepAngle,
            topLeft = Offset(
                x = (size.width - size.minDimension) / 2,
                y = (size.height - size.minDimension) / 2
            ),
            size = arcSize
        )
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
        angleOffset += sweepAngle
    }
}

@Composable
fun PieChartScope.PieMarkerComponent() {
    val (drawElement, current) = drawElementInteraction<PieData, DrawElement.Arc>() ?: return
    ArcMarkerComponent(
        drawElement = drawElement,
        displayInfo = "(" + current.value.toString() + ")"
    )
}

class PieTextSpec(
    val textColor: Color = Color.White,
    val textPressedColor: Color = Color.Red,
    val fontSize: TextUnit = 8.sp,
    val fontPressedSize: TextUnit = 16.sp,
) : ChartComponentSpec

@Composable
fun PieChartScope.PieTextComponent(
    modifier: Modifier = Modifier,
    spec: PieTextSpec = LocalChartTheme.current.pieTextSpec
) {
    val maxValue = chartDataset.rememberSumValue { it.value }
    val degreesValue = maxValue / 360f
    var startAngle = 0f
    val textMeasure = rememberTextMeasurer()
    LazyChartCanvas(
        modifier = modifier.fillMaxSize()
    ) { current ->
        if (isFirstGroupAndFirstIndex()) {
            startAngle = 0f
        }
        val sweepAngle = current.value / degreesValue
        val angle = startAngle + sweepAngle / 2
        val radius = size.minDimension / 2
        val textLayoutResult = textMeasure.measure(
            text = current.value.toString(),
            style = TextStyle(
                color = spec.textColor whenPressedAnimateTo spec.textPressedColor,
                fontSize = spec.fontSize whenPressedAnimateTo spec.fontPressedSize
            )
        )
        val offset = convertAngleToCoordinates(
            center = center,
            angle = angle,
            radius = radius - textLayoutResult.size.height
        )
        val textTopLeft = Offset(
            x = offset.x - textLayoutResult.size.width / 2,
            y = offset.y
        )
        rotate(
            degrees = angle + 90,
            pivot = offset
        ) {
            drawText(textLayoutResult, topLeft = textTopLeft)
        }
        startAngle += sweepAngle
    }
}