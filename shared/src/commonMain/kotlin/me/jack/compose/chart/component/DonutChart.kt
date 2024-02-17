package me.jack.compose.chart.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.draw.LazyChartCanvas
import me.jack.compose.chart.measure.rememberBoxChartContentMeasurePolicy
import me.jack.compose.chart.model.PieData
import me.jack.compose.chart.model.SimplePieData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.DonutChartScope
import me.jack.compose.chart.scope.PieChartScope
import me.jack.compose.chart.scope.drawElementInteraction
import me.jack.compose.chart.scope.isFirstGroupAndFirstIndex
import me.jack.compose.chart.scope.rememberSumValue
import me.jack.compose.chart.theme.LocalChartTheme
import me.jack.compose.chart.util.convertAngleToCoordinates

typealias DonutData = PieData
typealias SimpleDonutData = SimplePieData

class DonutSpec(
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
    content: @Composable DonutChartScope.() -> Unit = simpleChartContent
) {
    SingleChartLayout(
        modifier = modifier,
        chartContext = ChartContext,
        tapGestures = tapGestures,
        contentMeasurePolicy = rememberBoxChartContentMeasurePolicy(),
        chartDataset = chartDataset,
        content = content
    ) {
        DonutComponent(spec = spec)
        DonutTextComponent(strokeWidth = spec.strokeWidth)
        ArcMarkerComponent()
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
    ) { current ->
        if (isFirstGroupAndFirstIndex()) {
            angleOffset = 0f
        }
        val strokeWidthPx = spec.strokeWidth.toPx()
        val arcSize = Size(
            width = size.minDimension - strokeWidthPx,
            height = size.minDimension - strokeWidthPx
        )
        val sweepAngle = current.value / degreesValue
        interactionArc(
            startAngle = angleOffset,
            sweepAngle = sweepAngle,
            topLeft = Offset(
                x = (size.width - arcSize.width) / 2,
                y = (size.height - arcSize.height) / 2
            ),
            size = arcSize whenPressedAnimateTo arcSize.times(spec.pressedScale),
            strokeWidth = strokeWidthPx
        )
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
        angleOffset += sweepAngle
    }
}

@Composable
fun DonutChartScope.ArcMarkerComponent() {
    val (drawElement, current) = drawElementInteraction<DonutData, DrawElement.Arc>() ?: return
    ArcMarkerComponent(
        drawElement = drawElement,
        displayInfo = "(" + current.value.toString() + ")"
    )
}

class DonutTextSpec(
    val textColor: Color = Color.White,
    val textPressedColor: Color = Color.Red,
    val fontSize: TextUnit = 8.sp,
    val fontPressedSize: TextUnit = 12.sp,
) : ChartComponentSpec

@Composable
fun PieChartScope.DonutTextComponent(
    modifier: Modifier = Modifier,
    spec: DonutTextSpec = LocalChartTheme.current.donutTextSpec,
    strokeWidth: Dp
) {
    val maxValue = chartDataset.rememberSumValue { it.value }
    val degreesValue = maxValue / 360f
    var startAngle = 0f
    val textMeasure = rememberTextMeasurer()
    val strokeWidthPx = strokeWidth.toPx()
    LazyChartCanvas(
        modifier = modifier.fillMaxSize()
    ) { current ->
        if (isFirstGroupAndFirstIndex()) {
            startAngle = 0f
        }
        val sweepAngle = current.value / degreesValue
        val angle = startAngle + sweepAngle / 2
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
            (size.minDimension - strokeWidthPx + textLayoutResult.size.height) / 2
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

    fun drawText(
        textLayoutResult: TextLayoutResult,
        color: Color = Color.Unspecified,
        topLeft: Offset = Offset.Zero,
        alpha: Float = Float.NaN,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {

    }
}