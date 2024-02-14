package me.jack.compose.chart.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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