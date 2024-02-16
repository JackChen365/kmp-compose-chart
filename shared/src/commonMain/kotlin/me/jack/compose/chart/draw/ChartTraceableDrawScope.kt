package me.jack.compose.chart.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import me.jack.compose.chart.draw.cache.DrawingKeyframeCache

class ChartTraceableDrawScope<T>(
    private val drawScope: ChartDrawScope<T>,
    private val drawingElementCache: DrawingKeyframeCache
) : DrawScope by drawScope {
    var currentDrawElement: DrawElement = DrawElement.None
    private var isCurrentDrawElementUpdated = false
    private var currentItem: T? = null
    private var currentIndex: Int = 0

    val currentLeftTopOffset: Offset
        get() = with(drawScope) { currentLeftTopOffset }

    val nextLeftTopOffset: Offset
        get() = with(drawScope) { nextLeftTopOffset }

    val childCenterOffset: Offset
        get() = with(drawScope) { childCenterOffset }

    val childSize: Size
        get() = with(drawScope) { childSize }

    infix fun Color.whenPressed(targetValue: Color): Color {
        return with(drawScope) { whenPressed(targetValue) }
    }

    infix fun Int.whenPressedAnimateTo(targetValue: Int): Int {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Float.whenPressedAnimateTo(targetValue: Float): Float {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Color.whenPressedAnimateTo(targetValue: Color): Color {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Offset.whenPressedAnimateTo(targetValue: Offset): Offset {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Size.whenPressedAnimateTo(targetValue: Size): Size {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Dp.whenPressedAnimateTo(targetValue: Dp): Dp {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun DpOffset.whenPressedAnimateTo(targetValue: DpOffset): DpOffset {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    override fun drawRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val rectDrawElement: DrawElement.Rect = obtainDrawElement()
                rectDrawElement.topLeft = topLeft
                rectDrawElement.size = size
                currentDrawElement = rectDrawElement
            },
            onDraw = {
                drawScope.drawRect(brush, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val rectDrawElement: DrawElement.Rect = obtainDrawElement()
                rectDrawElement.topLeft = topLeft
                rectDrawElement.size = size
                currentDrawElement = rectDrawElement
            },
            onDraw = {
                drawScope.drawRect(color, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val circleDrawElement: DrawElement.Circle = obtainDrawElement()
                circleDrawElement.radius = radius
                circleDrawElement.center = center
                currentDrawElement = circleDrawElement
            },
            onDraw = {
                drawScope.drawCircle(brush, radius, center, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val circleDrawElement: DrawElement.Circle = obtainDrawElement()
                circleDrawElement.radius = radius
                circleDrawElement.center = center
                currentDrawElement = circleDrawElement
            },
            onDraw = {
                drawScope.drawCircle(color, radius, center, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawOval(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val ovalDrawElement: DrawElement.Oval = obtainDrawElement()
                ovalDrawElement.topLeft = topLeft
                ovalDrawElement.size = size
                currentDrawElement = ovalDrawElement
            },
            onDraw = {
                drawScope.drawOval(brush, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawOval(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val drawElement: DrawElement.Oval = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = size
                currentDrawElement = drawElement
            },
            onDraw = {
                drawScope.drawOval(color, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val drawElement: DrawElement.Arc = obtainDrawElement()
                drawElement.startAngle = startAngle
                drawElement.startAngle = startAngle
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement.sweepAngle = sweepAngle
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                } else {
                    drawElement.strokeWidth = 0f
                }
                currentDrawElement = drawElement
            },
            onDraw = {
                drawScope.drawArc(color, startAngle, sweepAngle, useCenter, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val drawElement: DrawElement.Arc = obtainDrawElement()
                drawElement.startAngle = startAngle
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement.sweepAngle = sweepAngle
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                } else {
                    drawElement.strokeWidth = 0f
                }
                currentDrawElement = drawElement
            },
            onDraw = {
                drawScope.drawArc(brush, startAngle, sweepAngle, useCenter, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    private inline fun drawElement(
        onUpdateDrawElement: () -> Unit,
        onDraw: () -> Unit
    ) {
        try {
            if (!isCurrentDrawElementUpdated) {
                onUpdateDrawElement()
            } else {
                onDraw()
            }
        } finally {
            isCurrentDrawElementUpdated = !isCurrentDrawElementUpdated
        }
    }

    fun trackChartData(currentItem: T, index: Int) {
        this.currentItem = currentItem
        this.currentIndex = index
    }

    private inline fun <reified T : DrawElement> obtainDrawElement(): T {
        return drawingElementCache.getCachedDrawElement(key = T::class.java)
    }
}