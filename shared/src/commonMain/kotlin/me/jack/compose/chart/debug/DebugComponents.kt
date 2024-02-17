package me.jack.compose.chart.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.component.DonutData
import me.jack.compose.chart.component.toPx
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.scope.PieChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.drawElementInteraction
import me.jack.compose.chart.util.calculateAngle
import me.jack.compose.chart.util.convertAngleToCoordinates

@Composable
fun PieChartScope.DebugDonutComponent(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(8.dp)
) {
    val (drawElement, current) = drawElementInteraction<DonutData, DrawElement.Arc>() ?: return
    DebugArcDrawElementComponent(
        drawElement = drawElement,
        padding = padding,
        displayInfo = "(" + current.value.toString() + ")"
    )
}

@Composable
fun SingleChartScope<*>.DebugArcDrawElementComponent(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(8.dp),
    drawElement: DrawElement.Arc,
    displayInfo: String
) {
    val startPadding = padding.calculateStartPadding(LayoutDirection.Ltr).toPx()
    val topPadding = padding.calculateTopPadding().toPx()
    val center = Offset(
        x = startPadding + drawElement.topLeft.x + drawElement.size.width / 2,
        y = topPadding + drawElement.topLeft.y + drawElement.size.height / 2
    )
    val angle = drawElement.startAngle + drawElement.sweepAngle / 2
    val offset = convertAngleToCoordinates(
        center = center,
        angle = angle,
        radius = drawElement.size.minDimension / 2
    )
    val currentPosition = interactionStates.hoverState.location
    var currentAngle by remember {
        mutableStateOf(0f)
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            currentAngle = calculateAngle(
                center = center,
                point = currentPosition
            )
            drawCircle(color = Color.Red, radius = 8.dp.toPx())
            drawRect(color = Color.Red, style = Stroke(1f))
            drawRect(
                color = Color.Red,
                style = Stroke(1f),
                topLeft = Offset(
                    x = padding.calculateStartPadding(LayoutDirection.Ltr).toPx() + (size.width - size.minDimension
                            ) / 2,
                    y = padding.calculateTopPadding().toPx() + (size.height - size.minDimension) / 2
                ),
                size = Size(
                    width = size.minDimension - padding.calculateStartPadding(LayoutDirection.Ltr)
                        .toPx() - padding.calculateEndPadding(
                        LayoutDirection.Ltr
                    ).toPx(),
                    height = size.minDimension - padding.calculateTopPadding().toPx() - padding.calculateBottomPadding()
                        .toPx()
                )
            )
        }
        Text(
            text = "Position:$currentPosition \n" +
                    "CurrentAngle:$currentAngle \n" +
                    "StartAngle:${drawElement.startAngle} \n" +
                    "EndAngle:${drawElement.startAngle + drawElement.sweepAngle} \n" +
                    "Center:$center \n" +
                    "Offset:$offset",
            modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
        )
    }
}