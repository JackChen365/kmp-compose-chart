package me.jack.compose.chart.component

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import me.jack.compose.chart.draw.ChartCanvas
import me.jack.compose.chart.draw.DrawElement
import me.jack.compose.chart.draw.isHoveredOrPressed
import me.jack.compose.chart.scope.ChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.isHorizontal
import me.jack.compose.chart.theme.LocalChartTheme
import me.jack.compose.chart.util.convertAngleToCoordinates
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
fun ChartScope.RectMarkerComponent(
    spec: MarkerSpec = LocalChartTheme.current.markerSpec,
    topLeft: Offset,
    size: Size,
    displayInfo: String,
    focusPoint: Offset = Offset.Unspecified
) {
    if (!isHoveredOrPressed()) return
    val contentSizeState by animateSizeAsState(size)
    val topLeftState by animateOffsetAsState(topLeft)
    val focusPointState by animateOffsetAsState(focusPoint)
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
        offset = IntOffset(
            x = (focusPointState.x - tooltipContentSize.width / 2)
                .coerceIn(0f, this.contentSize.width - tooltipContentSize.width)
                .toInt(),
            y = (focusPointState.y - tooltipContentSize.height - spec.tooltipTickSize.toPx())
                .coerceAtLeast(0f)
                .toInt()
        )
    } else {
        offset = IntOffset(
            x = focusPointState.x
                .coerceIn(0f, this.contentSize.width - tooltipContentSize.width)
                .toInt(),
            y = (topLeftState.y - tooltipContentSize.height / 2 + contentSizeState.height / 2)
                .coerceIn(0f, this.contentSize.height - tooltipContentSize.height)
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

open class MarkerDashLineSpec(
    val color: Color = Color.Gray,
    val strokeWidth: Dp = 1.dp,
    val angle: Float = 15f,
    val step: Dp = 8.dp,
) : ChartComponentSpec

class DarkMarkerDashLineSpec(
    color: Color = Color.Gray,
    strokeWidth: Dp = 1.dp,
    angle: Float = 15f,
    step: Dp = 8.dp,
) : MarkerDashLineSpec(color, strokeWidth, angle, step)

@Composable
fun SingleChartScope<*>.MarkerDashLineComponent(
    spec: MarkerDashLineSpec = MarkerDashLineSpec(),
    drawElement: DrawElement,
    topLeft: Offset,
    contentSize: Size
) {
    val pathEffect = remember {
        PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    val contentSizeState by animateSizeAsState(contentSize)
    val topLeftState by animateOffsetAsState(topLeft)
    ChartCanvas(
        Modifier
            .size(contentSizeState.width.toDp(), contentSizeState.height.toDp())
            .offset { topLeftState.round() }
            .graphicsLayer {
                clip = true
                shape = if (drawElement is DrawElement.Circle)
                    CircleShape
                else DrawElementShape(drawElement)
            }
            .clipToBounds()
    ) {
        val stepPx = spec.step.toPx()
        val strokeWidthPx = spec.strokeWidth.toPx()
        val hypotenuse = sqrt(size.height * size.height + size.width * size.width)
        val stepsCount = (hypotenuse / stepPx).roundToInt()
        animatableRect(topLeftState, contentSizeState)
        val lineColor = Color.Transparent whenPressedAnimateTo spec.color
        rotate(spec.angle, pivot = Offset.Zero) {
            for (i in 0..stepsCount) {
                drawLine(
                    color = lineColor,
                    start = Offset(x = i * stepPx + strokeWidthPx, y = -size.width),
                    end = Offset(x = i * stepPx, y = hypotenuse),
                    strokeWidth = strokeWidthPx,
                    pathEffect = pathEffect
                )
            }
        }
    }
}

internal class DrawElementShape(
    private val drawElement: DrawElement
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            when (drawElement) {
                is DrawElement.Oval -> {
                    addOval(Rect(0f, 0f, size.width, size.height))
                }

                is DrawElement.Arc -> {
                    addArc(
                        oval = Rect(
                            left = drawElement.topLeft.x,
                            top = drawElement.topLeft.y,
                            right = drawElement.topLeft.x + drawElement.size.width,
                            bottom = drawElement.topLeft.y + drawElement.size.height,
                        ),
                        startAngleDegrees = drawElement.startAngle,
                        sweepAngleDegrees = drawElement.sweepAngle
                    )
                }

                else -> {
                    addRect(Rect(0f, 0f, size.width, size.height))
                }
            }
        }
        return Outline.Generic(path)
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
        animatable {
            drawRect(
                color = Color.Transparent whenPressedAnimateTo color,
                topLeft = topLeft,
                size = contentSize
            )
        }
    }
}

@Composable
fun SingleChartScope<*>.ArcMarkerComponent(
    spec: MarkerSpec = LocalChartTheme.current.markerSpec,
    drawElement: DrawElement.Arc,
    displayInfo: String
) {
    if (!isHoveredOrPressed()) return
    val shape = remember {
        TooltipShape(
            cornerRadius = spec.tooltipCornerRadius,
            alignment = TooltipAlignment.Bottom,
            tickSize = spec.tooltipTickSize
        )
    }
    val center = Offset(
        x = drawElement.topLeft.x + drawElement.size.width / 2,
        y = drawElement.topLeft.y + drawElement.size.height / 2
    )
    val angle = drawElement.startAngle + drawElement.sweepAngle / 2 + 90
    val offset = convertAngleToCoordinates(
        center = center,
        angle = drawElement.startAngle + drawElement.sweepAngle / 2,
        radius = drawElement.size.minDimension / 2 + drawElement.strokeWidth / 2
    )
    val arrowOffset = Offset(
        x = offset.x - spec.tooltipSize.width.toPx() / 2,
        y = offset.y - spec.tooltipSize.height.toPx() - spec.tooltipTickSize.toPx()
    )
    val arrowOffsetState by animateOffsetAsState(arrowOffset)
    Card(
        modifier = Modifier
            .width(spec.tooltipSize.width)
            .height(spec.tooltipSize.height + spec.tooltipTickSize)
            .offset { arrowOffsetState.round() }
            .graphicsLayer(
                transformOrigin = TransformOrigin(
                    pivotFractionX = 0.5f,
                    pivotFractionY = 1f,
                ),
                rotationZ = angle,
            )
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