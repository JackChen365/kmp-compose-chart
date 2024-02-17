package me.jack.compose.chart.draw

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import me.jack.compose.chart.util.isPointInArc
import me.jack.compose.chart.util.isPointInArcWithStrokeWidth
import me.jack.compose.chart.util.isPointInCircle
import me.jack.compose.chart.util.isPointInLine
import me.jack.compose.chart.util.isPointInOval
import me.jack.compose.chart.util.isPointInRect
import me.jack.compose.chart.util.isPointInRoundRect

sealed class DrawElement {
    open var focusPoint: Offset = Offset.Unspecified

    /**
     * Active means there are drawing animate state cooperate with this element.
     */
    open var isActivated: Boolean = false

    var isPressed: Boolean = false

    var isHovered: Boolean = false

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
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero
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
            return location.isPointInRect(topLeft, size)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Rect) {
                topLeft = other.topLeft
                size = other.size
                strokeWidth = other.strokeWidth
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
            if (topLeft != other.topLeft) return false
            if (strokeWidth != other.strokeWidth) return false
            return size == other.size
        }

        override fun hashCode(): Int {
            var result = topLeft.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            return result
        }
    }

    class Circle : DrawElement() {
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
            return location.isPointInCircle(center, radius)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Circle) {
                radius = other.radius
                center = other.center
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

            if (radius != other.radius) return false
            return center == other.center
        }

        override fun hashCode(): Int {
            var result = radius.hashCode()
            result = 31 * result + center.hashCode()
            return result
        }
    }

    @Stable
    class Oval : DrawElement() {
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
            return location.isPointInOval(topLeft + size.center, size / 2f)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Oval) {
                topLeft = other.topLeft
                size = other.size
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

            if (topLeft != other.topLeft) return false
            return size == other.size
        }

        override fun hashCode(): Int {
            var result = topLeft.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    @Stable
    class Arc : DrawElement() {
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
                location.isPointInArcWithStrokeWidth(
                    leftTop = topLeft,
                    size = size,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    strokeWidth = strokeWidth
                )
            } else {
                location.isPointInArc(
                    leftTop = topLeft,
                    size = size,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle
                )
            }
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Arc) {
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
            return "Arc(topLeft=$topLeft, size=$size, startAngle=$startAngle, sweepAngle=$sweepAngle, strokeWidth=$strokeWidth)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arc

            if (topLeft != other.topLeft) return false
            if (size != other.size) return false
            if (startAngle != other.startAngle) return false
            if (sweepAngle != other.sweepAngle) return false
            return strokeWidth == other.strokeWidth
        }

        override fun hashCode(): Int {
            var result = topLeft.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + startAngle.hashCode()
            result = 31 * result + sweepAngle.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            return result
        }
    }


    @Stable
    class Line : DrawElement() {
        var start: Offset = Offset.Zero
        var end: Offset = Offset.Zero
        var strokeWidth: Float = 0f
        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    Offset(
                        x = (end.x + start.x) / 2,
                        y = (end.y + start.y) / 2
                    )
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return location.isPointInLine(start, end, strokeWidth)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Line) {
                start = other.start
                end = other.end
                strokeWidth = other.strokeWidth
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Line(start=$start, end=$end, strokeWidth=$strokeWidth)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Line
            if (start != other.start) return false
            if (end != other.end) return false
            return strokeWidth == other.strokeWidth
        }

        override fun hashCode(): Int {
            var result = start.hashCode()
            result = 31 * result + end.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            return result
        }
    }

    @Stable
    class Points : DrawElement() {
        var points: List<Offset> = emptyList()
        var strokeWidth: Float = 0f
        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified && points.isNotEmpty()) {
                    points.first()
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return points.any {
                it == location
            }
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Points) {
                points = other.points
                strokeWidth = other.strokeWidth
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Points

            if (points != other.points) return false
            return strokeWidth == other.strokeWidth
        }

        override fun hashCode(): Int {
            var result = points.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            return result
        }

        override fun toString(): String {
            return "Points(points=$points, strokeWidth=$strokeWidth)"
        }
    }

    @Stable
    class RoundRect : DrawElement() {
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero
        var cornerRadius: CornerRadius = CornerRadius.Zero

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
            return location.isPointInRoundRect(topLeft, size, cornerRadius)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is RoundRect) {
                topLeft = other.topLeft
                size = other.size
                cornerRadius = other.cornerRadius
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RoundRect

            if (topLeft != other.topLeft) return false
            if (size != other.size) return false
            return cornerRadius == other.cornerRadius
        }

        override fun hashCode(): Int {
            var result = topLeft.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + cornerRadius.hashCode()
            return result
        }

        override fun toString(): String {
            return "RoundRect(topLeft=$topLeft, size=$size, cornerRadius=$cornerRadius)"
        }
    }

    @Stable
    class Path : DrawElement() {
        var bounds: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero
        var strokeWidth: Float = 0f
        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    bounds.center
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return location.isPointInRect(bounds.topLeft, bounds.size)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Path) {
                bounds = other.bounds
            }
            return this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Path

            if (bounds != other.bounds) return false
            return strokeWidth == other.strokeWidth
        }

        override fun hashCode(): Int {
            var result = bounds.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            return result
        }

        override fun toString(): String {
            return "Path(bounds=$bounds, strokeWidth=$strokeWidth)"
        }
    }

    @Stable
    class DrawElementGroup : DrawElement() {
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero
        var children: ResizableList<DrawElement> = ResizableList()

        override operator fun contains(location: Offset): Boolean {
            return location.isPointInRect(topLeft, size)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is DrawElementGroup) {
                topLeft = other.topLeft
                size = other.size
                children = other.children
            }
            return this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DrawElementGroup

            if (topLeft != other.topLeft) return false
            if (size != other.size) return false
            return children == other.children
        }

        override fun hashCode(): Int {
            var result = topLeft.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + children.hashCode()
            return result
        }

        override fun toString(): String {
            return "DrawElementGroup(topLeft=$topLeft, size=$size, children=$children)"
        }
    }
}

class ResizableList<T> : Iterable<T> {

    private val list: MutableList<T> = mutableListOf()
    private var pointer = 0

    fun add(element: T) {
        list.add(element)
    }

    fun remove(element: T): Boolean {
        return list.remove(element)
    }

    operator fun get(index: Int): T {
        return list[index]
    }

    fun set(index: Int, element: T) {
        list[index] = element
    }

    fun size(): Int {
        return list.size
    }

    fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    fun resetPointer() {
        pointer = 0
    }

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {

            override fun hasNext(): Boolean {
                return pointer < list.size
            }

            override fun next(): T {
                return list[pointer++]
            }
        }
    }
}

fun DrawElement.isTap(interactionStates: ChartInteractionStates): Boolean {
    return interactionStates.isTap && interactionStates.tapState.location in this
}

fun DrawElement.isLongPressed(interactionStates: ChartInteractionStates): Boolean {
    return interactionStates.isLongPress && interactionStates.longPressState.location in this
}

fun DrawElement.isDoubleTap(interactionStates: ChartInteractionStates): Boolean {
    return interactionStates.isDoubleTap && interactionStates.doubleTapState.location in this
}