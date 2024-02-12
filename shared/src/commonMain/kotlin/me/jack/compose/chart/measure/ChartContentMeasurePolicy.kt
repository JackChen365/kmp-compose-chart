package me.jack.compose.chart.measure

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import me.jack.compose.chart.scope.ChartScope

@Composable
fun rememberFixedContentMeasurePolicy(
    fixedRowSize: Float,
    divideSize: Float = 0f,
    groupDividerSize: Float = 0f
): ChartContentMeasurePolicy {
    return remember {
        FixedContentMeasurePolicy(fixedRowSize, divideSize, groupDividerSize)
    }
}

@Composable
fun rememberFixedVerticalContentMeasurePolicy(
    fixedRowSize: Float,
    divideSize: Float = 0f,
    groupDividerSize: Float = 0f
): ChartContentMeasurePolicy {
    return remember {
        FixedContentMeasurePolicy(fixedRowSize, divideSize, groupDividerSize, Orientation.Vertical)
    }
}

@Composable
fun rememberFixedOverlayContentMeasurePolicy(
    fixedRowSize: Float,
    divideSize: Float = 0f,
    orientation: Orientation = Orientation.Horizontal
) = remember {
    FixedOverlayContentMeasurePolicy(fixedRowSize, divideSize, orientation)
}

@Composable
fun rememberBoxChartContentMeasurePolicy() = remember {
    BoxChartContentMeasurePolicy()
}

interface ChartContentMeasurePolicy {
    var contentSize: IntSize

    val ChartScope.groupSize: Float

    val childDividerSize: Float
        get() = 0f

    val groupDividerSize: Float
        get() = 0f

    val DrawScope.childItemSize: Size

    val orientation: Orientation
        get() = Orientation.Horizontal

    fun childLeftTop(
        groupCount: Int,
        groupIndex: Int,
        index: Int
    ): Offset

    fun ChartScope.measureContent(
        size: IntSize
    ): Size
}
