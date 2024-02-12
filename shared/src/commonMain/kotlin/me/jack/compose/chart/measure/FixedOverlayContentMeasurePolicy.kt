package me.jack.compose.chart.measure

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import me.jack.compose.chart.context.isHorizontal
import me.jack.compose.chart.scope.ChartScope

class FixedOverlayContentMeasurePolicy(
    private val fixedChildSize: Float,
    override val childDividerSize: Float = 0f,
    override val orientation: Orientation = Orientation.Horizontal
) : ChartContentMeasurePolicy {

    override fun ChartScope.measureContent(size: IntSize): Size {
        return if (orientation.isHorizontal) {
            Size(
                width = (groupSize * childItemCount) - childDividerSize,
                height = size.height.toFloat()
            )
        } else {
            Size(
                width = size.width.toFloat(),
                height = (groupSize * childItemCount) - childDividerSize
            )
        }
    }

    override var contentSize: IntSize = IntSize.Zero

    override val ChartScope.groupSize: Float
        get() = (fixedChildSize + childDividerSize)

    override fun childLeftTop(
        groupCount: Int,
        groupIndex: Int,
        index: Int
    ): Offset {
        return if (orientation.isHorizontal) {
            Offset(
                x = (fixedChildSize + childDividerSize) * index,
                y = 0f
            )
        } else {
            Offset(
                x = 0f,
                y = (fixedChildSize + childDividerSize) * index
            )
        }
    }

    override val DrawScope.childItemSize: Size
        get() = if (orientation.isHorizontal) {
            Size(
                width = fixedChildSize,
                height = size.height
            )
        } else {
            Size(
                width = size.width,
                height = fixedChildSize
            )
        }
}