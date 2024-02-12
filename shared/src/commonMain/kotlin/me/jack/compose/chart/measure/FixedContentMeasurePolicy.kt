package me.jack.compose.chart.measure

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import me.jack.compose.chart.context.isHorizontal
import me.jack.compose.chart.scope.ChartScope

class FixedContentMeasurePolicy(
    private val fixedChildSize: Float,
    override val childDividerSize: Float = 0f,
    override val groupDividerSize: Float = 0f,
    override val orientation: Orientation = Orientation.Horizontal
) : ChartContentMeasurePolicy {

    override var contentSize: IntSize = IntSize.Zero

    override val ChartScope.groupSize: Float
        get() = (fixedChildSize + childDividerSize) * groupCount + groupDividerSize

    override fun ChartScope.measureContent(size: IntSize): Size {
        return if (orientation.isHorizontal) {
            Size(
                width = (((fixedChildSize + childDividerSize) * groupCount + groupDividerSize) * childItemCount) - groupDividerSize - childDividerSize,
                height = size.height.toFloat()
            )
        } else {
            Size(
                width = size.width.toFloat(),
                height = (((fixedChildSize + childDividerSize) * groupCount + groupDividerSize) * childItemCount) - groupDividerSize - childDividerSize,
            )
        }
    }

    override fun childLeftTop(
        groupCount: Int,
        groupIndex: Int,
        index: Int
    ): Offset {
        val leftTopOffset = if (orientation.isHorizontal) {
            Offset(
                x = (fixedChildSize + childDividerSize) * groupCount * index +
                        groupDividerSize * index +
                        (fixedChildSize + childDividerSize) * groupIndex,
                y = 0f
            )
        } else {
            Offset(
                x = 0f,
                y = (fixedChildSize + childDividerSize) * groupCount * index +
                        groupDividerSize * index +
                        (fixedChildSize + childDividerSize) * groupIndex,
            )
        }
        return leftTopOffset
    }

    override val DrawScope.childItemSize: Size
        get() = if (orientation.isHorizontal) {
            Size(width = fixedChildSize, height = size.height)
        } else {
            Size(width = contentSize.width.toFloat(), height = fixedChildSize)
        }
}