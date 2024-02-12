package me.jack.compose.chart.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import me.jack.compose.chart.draw.ChartCanvas
import me.jack.compose.chart.draw.interaction.longPressTapState
import me.jack.compose.chart.scope.ChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.isHorizontal
import me.jack.compose.chart.theme.LocalChartTheme

open class MarkerSpec(
    val tooltipSize: DpSize = DpSize(width = 80.dp, height = 40.dp),
    val tooltipCornerRadius: CornerSize = CornerSize(8.dp),
    val tooltipTickSize: Dp = 12.dp,
    val tooltipTextSize: TextUnit = 12.sp,
    val borderColor: Color = Color.LightGray,
    val borderSize: Dp = 2.dp,
    val borderElevation: Dp = 8.dp,
) : ChartComponentSpec

class DarkMarkerSpec(
    tooltipSize: DpSize = DpSize(width = 80.dp, height = 40.dp),
    tooltipCornerRadius: CornerSize = CornerSize(8.dp),
    tooltipTickSize: Dp = 12.dp,
    tooltipTextSize: TextUnit = 12.sp,
    borderColor: Color = Color.Gray,
    borderSize: Dp = 2.dp,
    borderElevation: Dp = 8.dp,
) : MarkerSpec(
    tooltipSize,
    tooltipCornerRadius,
    tooltipTickSize,
    tooltipTextSize,
    borderColor,
    borderSize,
    borderElevation
)

@Composable
fun ChartScope.MarkerComponent(
    spec: MarkerSpec = LocalChartTheme.current.markerSpec,
    leftTop: Offset,
    size: Size,
    displayInfo: String,
    focusPoint: Offset = Offset.Unspecified
) {
    if (!chartContext.longPressTapState.value) return
    val tooltipWidth = spec.tooltipSize.width.toPx()
    val tooltipHeight = spec.tooltipSize.height.toPx()
    val tooltipContentSize by remember {
        mutableStateOf(
            Size(
                width = tooltipWidth,
                height = tooltipHeight
            )
        )
    }
    var alignment = TooltipAlignment.Bottom
    val offset: IntOffset
    if (isHorizontal) {
        offset = if (Offset.Unspecified == focusPoint)
            IntOffset(
                x = (leftTop.x - tooltipContentSize.width / 2 + size.width / 2)
                    .coerceIn(0f, contentSize.width - tooltipContentSize.width)
                    .toInt(),
                y = (leftTop.y - tooltipContentSize.height - spec.tooltipTickSize.toPx())
                    .coerceAtLeast(0f)
                    .toInt()
            )
        else IntOffset(
            x = (focusPoint.x - tooltipContentSize.width / 2)
                .coerceIn(0f, contentSize.width - tooltipContentSize.width)
                .toInt(),
            y = (focusPoint.y - tooltipContentSize.height - spec.tooltipTickSize.toPx())
                .coerceAtLeast(0f)
                .toInt()
        )
    } else {
        offset = if (Offset.Unspecified == focusPoint)
            IntOffset(
                x = (size.width + spec.tooltipTickSize.toPx())
                    .coerceAtMost(contentSize.width - tooltipContentSize.width)
                    .toInt(),
                y = (leftTop.y - tooltipContentSize.height / 2 + size.height / 2)
                    .coerceIn(0f, contentSize.height - tooltipContentSize.height)
                    .toInt()
            )
        else IntOffset(
            x = (focusPoint.x + spec.tooltipTickSize.toPx())
                .coerceIn(0f, contentSize.width - tooltipContentSize.width)
                .toInt(),
            y = (leftTop.y - tooltipContentSize.height / 2 + size.height / 2)
                .coerceIn(0f, contentSize.height - tooltipContentSize.height)
                .toInt()
        )
        alignment = TooltipAlignment.Start
    }
    val shape = remember {
        TooltipShape(
            cornerRadius = spec.tooltipCornerRadius,
            alignment = alignment,
            tickSize = spec.tooltipTickSize
        )
    }
    Card(
        modifier = Modifier
            .width(spec.tooltipSize.width)
            .height(spec.tooltipSize.height)
            .offset { offset }
            .shadow(elevation = spec.borderElevation, shape = shape),
        shape = shape,
        border = BorderStroke(
            width = spec.borderSize,
            color = spec.borderColor
        )
    ) {
        Text(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(align = Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text = displayInfo,
            fontSize = spec.tooltipTextSize
        )
    }
}

enum class TooltipAlignment {
    Top, Start, End, Bottom
}

class TooltipShape(
    private val cornerRadius: CornerSize = CornerSize(4.dp),
    private val alignment: TooltipAlignment = TooltipAlignment.Bottom,
    private val offsetFraction: Float = 0.5f,
    private val tickSize: Dp = 12.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadius = cornerRadius.toPx(size, density)
        val diameter = cornerRadius * 2
        val tickSize = with(density) { tickSize.toPx() }
        return Outline.Generic(
            Path().apply {
                reset()
                //Top left arc
                arcTo(
                    rect = Rect(
                        left = 0f,
                        top = 0f,
                        right = diameter,
                        bottom = diameter
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                if (alignment == TooltipAlignment.Top) {
                    // width minus both left and right round circle diameter and the tick size/2
                    val start = diameter + tickSize / 2 + (size.width - diameter * 2 - tickSize) * offsetFraction
                    lineTo(x = start - tickSize / 2, y = 0f)
                    lineTo(x = start, y = -tickSize)
                    lineTo(x = start + tickSize / 2, y = 0f)
                }
                lineTo(x = size.width - diameter, y = 0f)
                //Top right arc
                arcTo(
                    rect = Rect(
                        left = size.width - diameter,
                        top = 0f,
                        right = size.width,
                        bottom = diameter
                    ),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                if (alignment == TooltipAlignment.End && layoutDirection == LayoutDirection.Ltr
                    || alignment == TooltipAlignment.Start && layoutDirection == LayoutDirection.Rtl
                ) {
                    val start = diameter + tickSize / 2 + (size.height - diameter * 2 - tickSize) * offsetFraction
                    lineTo(x = size.width, y = (start - tickSize * 0.5f))
                    lineTo(x = size.width + tickSize, y = start)
                    lineTo(x = size.width, y = (start + tickSize * 0.5f))
                }
                lineTo(x = size.width, y = size.height - diameter)
                // Bottom right arc
                arcTo(
                    rect = Rect(
                        left = size.width - 2 * cornerRadius,
                        top = size.height - 2 * cornerRadius,
                        right = size.width,
                        bottom = size.height
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90.0f,
                    forceMoveTo = false
                )
                if (alignment == TooltipAlignment.Bottom) {
                    val start = diameter + tickSize / 2 + (size.width - diameter * 2 - tickSize) * offsetFraction
                    lineTo(x = start + tickSize * 0.5f, y = size.height)
                    lineTo(x = start, y = size.height + tickSize)
                    lineTo(x = start - tickSize * 0.5f, y = size.height)
                }
                lineTo(x = 2 * cornerRadius, y = size.height)
                // Bottom left arc
                arcTo(
                    rect = Rect(
                        left = 0f,
                        top = size.height - 2 * cornerRadius,
                        right = 2 * cornerRadius,
                        bottom = size.height
                    ),
                    startAngleDegrees = 90.0f,
                    sweepAngleDegrees = 90.0f,
                    forceMoveTo = false
                )
                if (alignment == TooltipAlignment.Start && layoutDirection == LayoutDirection.Ltr
                    || alignment == TooltipAlignment.End && layoutDirection == LayoutDirection.Rtl
                ) {
                    val start = diameter + tickSize / 2 + (size.height - diameter * 2 - tickSize) * offsetFraction
                    lineTo(x = 0f, y = (start + tickSize * 0.5f))
                    lineTo(x = -tickSize, y = start)
                    lineTo(x = 0f, y = (start - tickSize * 0.5f))
                }
                lineTo(x = 0f, y = cornerRadius)
            }
        )
    }
}

@Composable
fun SingleChartScope<*>.MarkerDashLineComponent(
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    topLeft: Offset,
    contentSize: Size,
    focusPoint: Offset
) {
    val pathEffect = remember {
        PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    ChartCanvas(
        Modifier.fillMaxSize()
    ) {
        if (isHorizontal) {
            clickableRect(topLeft = topLeft, size = contentSize)
            drawLine(
                color = Color.Transparent whenPressedAnimateTo color,
                start = Offset(
                    x = topLeft.x + contentSize.width / 2,
                    y = topLeft.y
                ),
                end = Offset(
                    x = topLeft.x + contentSize.width / 2,
                    y = topLeft.y + contentSize.height
                ),
                strokeWidth = strokeWidth.toPx(),
                pathEffect = pathEffect
            )
            drawLine(
                color = Color.Transparent whenPressedAnimateTo color,
                start = Offset(
                    x = topLeft.x,
                    y = if (focusPoint == Offset.Unspecified)
                        topLeft.y + contentSize.height / 2
                    else focusPoint.y
                ),
                end = Offset(
                    x = topLeft.x + contentSize.width,
                    y = if (focusPoint == Offset.Unspecified)
                        topLeft.y + contentSize.height / 2
                    else focusPoint.y
                ),
                strokeWidth = strokeWidth.toPx(),
                pathEffect = pathEffect
            )
        } else {
            drawLine(
                color = color,
                start = Offset(
                    x = topLeft.x,
                    y = topLeft.y + contentSize.height / 2
                ),
                end = Offset(
                    x = topLeft.x + contentSize.width,
                    y = topLeft.y + contentSize.height / 2
                ),
                strokeWidth = strokeWidth.toPx(),
                pathEffect = pathEffect
            )
            drawLine(
                color = color,
                start = Offset(
                    x = if (focusPoint == Offset.Unspecified)
                        topLeft.x + contentSize.width / 2
                    else focusPoint.x,
                    y = topLeft.y
                ),
                end = Offset(
                    x = if (focusPoint == Offset.Unspecified)
                        topLeft.x + contentSize.width / 2
                    else focusPoint.x,
                    y = topLeft.y + contentSize.height
                ),
                strokeWidth = strokeWidth.toPx(),
                pathEffect = pathEffect
            )
        }
    }
}

@Composable
fun SingleChartScope<*>.HoverMarkerComponent(
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
    topLeft: Offset,
    contentSize: Size
) {
    ChartCanvas(
        Modifier.fillMaxSize()
    ) {
        clickable {
            drawRect(
                color = Color.Transparent whenPressedAnimateTo color,
                topLeft = topLeft,
                size = contentSize
            )
        }
    }
}