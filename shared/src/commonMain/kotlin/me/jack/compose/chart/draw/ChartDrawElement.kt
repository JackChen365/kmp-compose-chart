package me.jack.compose.chart.draw

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.pressLocation
import me.jack.compose.chart.context.pressState
import me.jack.compose.chart.draw.interaction.doubleTapLocation
import me.jack.compose.chart.draw.interaction.doubleTapState
import me.jack.compose.chart.draw.interaction.hoverLocation
import me.jack.compose.chart.draw.interaction.hoverState
import me.jack.compose.chart.draw.interaction.longPressLocation
import me.jack.compose.chart.draw.interaction.longPressTapState
import me.jack.compose.chart.draw.interaction.tapLocation
import me.jack.compose.chart.draw.interaction.tapState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

sealed class DrawElement {
    open var focusPoint: Offset = Offset.Unspecified

    open operator fun contains(location: Offset): Boolean = false

    open fun copy(other: DrawElement): DrawElement {
        return this
    }

    fun isNone(): Boolean {
        return this == None
    }

    /**
     * Since we have defined the method: copy, we should not make it a data object.
     */
    @Suppress("ConvertObjectToDataObject")
    object None : DrawElement()

    class Rect : DrawElement() {
        var color: Color = Color.Unspecified
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero
        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    Offset(
                        x = topLeft.x + size.width / 2,
                        y = topLeft.y + size.height / 2
                    )
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return location.intersect(topLeft, size)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Rect) {
                topLeft = other.topLeft
                size = other.size
                color = other.color
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Rect(topLeft=$topLeft, size=$size, focus:${focusPoint})"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Rect

            if (color != other.color) return false
            if (topLeft != other.topLeft) return false
            return size == other.size
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + topLeft.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    class Circle : DrawElement() {
        var color: Color = Color.Unspecified
        var radius: Float = 0f
        var center: Offset = Offset.Zero

        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    center
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return location.intersectCircle(center, radius)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Circle) {
                radius = other.radius
                center = other.center
                color = other.color
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Circle(radius=$radius, center=$center)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Circle

            if (color != other.color) return false
            if (radius != other.radius) return false
            return center == other.center
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + radius.hashCode()
            result = 31 * result + center.hashCode()
            return result
        }
    }

    @Stable
    class Oval : DrawElement() {
        var color: Color = Color.Unspecified
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero

        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    Offset(
                        x = topLeft.x + size.width / 2,
                        y = topLeft.y + size.height / 2
                    )
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return location.intersectOval(topLeft + size.center, size)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Oval) {
                topLeft = other.topLeft
                size = other.size
                color = other.color
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Oval(topLeft=$topLeft, size=$size)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Oval

            if (color != other.color) return false
            if (topLeft != other.topLeft) return false
            return size == other.size
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + topLeft.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    @Stable
    class Arc : DrawElement() {
        var color: Color = Color.Unspecified
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero
        var startAngle: Float = 0f
        var sweepAngle: Float = 0f
        var strokeWidth: Float = 0f

        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    Offset(
                        x = topLeft.x + size.width / 2,
                        y = topLeft.y + size.height / 2
                    )
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return if (0 < strokeWidth) {
                location.intersectArcWithStrokeWidth(
                    leftTop = topLeft,
                    size = size,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    strokeWidth = strokeWidth
                )
            } else {
                location.intersectArc(
                    leftTop = topLeft,
                    size = size,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle
                )
            }
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Arc) {
                color = other.color
                topLeft = other.topLeft
                size = other.size
                startAngle = other.startAngle
                sweepAngle = other.sweepAngle
                strokeWidth = other.strokeWidth
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Arc(color=$color, topLeft=$topLeft, size=$size, startAngle=$startAngle, sweepAngle=$sweepAngle, strokeWidth=$strokeWidth)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arc

            if (color != other.color) return false
            if (topLeft != other.topLeft) return false
            if (size != other.size) return false
            if (startAngle != other.startAngle) return false
            if (sweepAngle != other.sweepAngle) return false
            return strokeWidth == other.strokeWidth
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + topLeft.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + startAngle.hashCode()
            result = 31 * result + sweepAngle.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            return result
        }
    }
}

fun DrawElement.isHoveredOrPressed(chartContext: ChartContext): Boolean {
    return chartContext.hoverState.value && chartContext.hoverLocation in this ||
            chartContext.pressState.value && chartContext.pressLocation in this
}

fun DrawElement.isTap(chartContext: ChartContext): Boolean {
    return chartContext.tapState.value && chartContext.tapLocation in this
}

fun DrawElement.isLongPressed(chartContext: ChartContext): Boolean {
    return chartContext.longPressTapState.value && chartContext.longPressLocation in this
}

fun DrawElement.isDoubleTap(chartContext: ChartContext): Boolean {
    return chartContext.doubleTapState.value && chartContext.doubleTapLocation in this
}

fun Offset.intersect(topLeft: Offset, size: Size): Boolean {
    return x in topLeft.x..<topLeft.x + size.width && y in topLeft.y..<topLeft.y + size.height
}

fun Offset.intersectCircle(center: Offset, radius: Float): Boolean {
    val distanceSquared = (x - center.x) * (x - center.x) + (y - center.y) * (y - center.y)
    return distanceSquared <= radius * radius
}

fun Offset.intersectOval(center: Offset, size: Size): Boolean {
    val term1 = ((x - center.x) * (x - center.x)) / (size.width * size.width)
    val term2 = ((y - center.y) * (y - center.y)) / (size.height * size.height)
    return (term1 + term2) < 1
}

fun Offset.intersectArc(
    leftTop: Offset, size: Size, startAngle: Float, sweepAngle: Float
): Boolean {
    val centerX = leftTop.x + size.width / 2
    val centerY = leftTop.y + size.height / 2

    val dx = x - centerX
    val dy = y - centerY

    val distance = sqrt(dx * dx + dy * dy)
    var angle = atan2(dy, dx) * (180 / PI)
    if (angle < 0) angle += 360.0

    val start = startAngle % 360
    val end = (start + sweepAngle) % 360

    val isWithinDistance = distance <= size.width / 2
    val isWithinAngles = if (start < end) {
        angle in start..end
    } else {
        angle in 0f..end || angle in start..360f
    }
    return isWithinDistance && isWithinAngles
}

fun Offset.intersectArcWithStrokeWidth(
    leftTop: Offset, size: Size, startAngle: Float, sweepAngle: Float, strokeWidth: Float
): Boolean {
    val centerX = leftTop.x + size.width / 2
    val centerY = leftTop.y + size.height / 2

    val dx = x - centerX
    val dy = y - centerY

    val distance = sqrt(dx * dx + dy * dy)
    var angle = atan2(dy, dx) * (180 / PI)
    if (angle < 0) angle += 360.0

    val start = startAngle % 360
    val end = (startAngle + sweepAngle) % 360
    val isWithinDistance =
        distance < (size.minDimension / 2 + strokeWidth / 2) && distance > (size.minDimension / 2 - strokeWidth/2)
    val isWithinAngles = if (start < end) {
        angle in start..end
    } else {
        angle in 0f..end || angle in start..360f
    }
    return isWithinDistance && isWithinAngles
}
