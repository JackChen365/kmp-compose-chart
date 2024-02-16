package me.jack.compose.chart.util

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun convertAngleToCoordinates(center: Offset, angle: Float, radius: Float): Offset {
    val angleRadians = (angle * Math.PI / 180).toFloat()
    return Offset(
        x = radius * cos(angleRadians) + center.x,
        y = radius * sin(angleRadians) + center.y
    )
}

fun calculateAngle(center: Offset, point: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val angle = atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI
    return angle.toFloat()
}


fun Offset.isPointInRect(topLeft: Offset, size: Size): Boolean {
    if (this == Offset.Unspecified) return false
    return x in topLeft.x..<topLeft.x + size.width && y in topLeft.y..<topLeft.y + size.height
}

fun Offset.isPointInCircle(center: Offset, radius: Float): Boolean {
    if (this == Offset.Unspecified) return false
    val distanceSquared = (x - center.x) * (x - center.x) + (y - center.y) * (y - center.y)
    return distanceSquared <= radius * radius
}

fun Offset.isPointInOval(center: Offset, size: Size): Boolean {
    if (this == Offset.Unspecified) return false
    val term1 = ((x - center.x) * (x - center.x)) / (size.width * size.width)
    val term2 = ((y - center.y) * (y - center.y)) / (size.height * size.height)
    return (term1 + term2) < 1
}

fun Offset.isPointInArc(
    leftTop: Offset, size: Size, startAngle: Float, sweepAngle: Float
): Boolean {
    if (this == Offset.Unspecified) return false
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

fun Offset.isPointInArcWithStrokeWidth(
    leftTop: Offset, size: Size, startAngle: Float, sweepAngle: Float, strokeWidth: Float
): Boolean {
    if (this == Offset.Unspecified) return false
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
        distance < (size.minDimension / 2 + strokeWidth / 2) && distance > (size.minDimension / 2 - strokeWidth / 2)
    val isWithinAngles = if (start < end) {
        angle in start..end
    } else {
        angle in 0f..end || angle in start..360f
    }
    return isWithinDistance && isWithinAngles
}

fun Offset.isPointInLine(
    start: Offset,
    end: Offset,
    strokeWidth: Float
): Boolean {
    val (x1, y1) = start
    val (x2, y2) = end
    val lineLength = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    val distanceToStart = sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1))
    val distanceToEnd = sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2))
    if (distanceToStart > lineLength || distanceToEnd > lineLength) {
        return false
    }
    val distanceToLine = abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1) / lineLength
    return distanceToLine <= strokeWidth / 2
}
fun Offset.isPointInRoundRect(
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius
): Boolean {
    val left = topLeft.x
    val top = topLeft.y
    val right = topLeft.x + size.width
    val bottom = topLeft.y + size.height
    val radiusX = cornerRadius.x
    val radiusY = cornerRadius.y
    // Check if the point is inside the rectangle.
    if (x < left || x > right || y < top || y > bottom) {
        return false
    }
    // left top
    if (x < left + radiusX && y < top + radiusY) {
        return isPointInOval(
            center = Offset(
                x = left + radiusX,
                y = top + radiusY
            ),
            size = Size(width = radiusX, height = radiusY)
        )
    }

    // right top
    if (x > right - radiusX && y < top + radiusY) {
        return isPointInOval(
            center = Offset(
                x = right - radiusX,
                y = top + radiusY
            ),
            size = Size(width = radiusX, height = radiusY)
        )
    }

    // top bottom
    if (x < left + radiusX && y > bottom - radiusY) {
        return isPointInOval(
            center = Offset(
                x = left + radiusX,
                y = bottom - radiusY
            ),
            size = Size(width = radiusX, height = radiusY)
        )
    }

    // right bottom
    if (x > right - radiusX && y > bottom - radiusY) {
        return isPointInOval(
            center = Offset(
                x = right - radiusX,
                y = bottom - radiusY
            ),
            size = Size(width = radiusX, height = radiusY)
        )
    }
    return true
}