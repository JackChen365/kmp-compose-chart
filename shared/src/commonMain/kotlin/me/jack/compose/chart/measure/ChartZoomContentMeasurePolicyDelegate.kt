package me.jack.compose.chart.measure

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import me.jack.compose.chart.context.isHorizontal
import me.jack.compose.chart.scope.ChartScope

fun ChartContentMeasurePolicy.withZoomableMeasurePolicy(
    onZoomValueChanged: () -> Float = { 1f }
): ChartContentMeasurePolicy {
    return ChartZoomContentMeasurePolicyDelegate(this, onZoomValueChanged)
}

class ChartZoomContentMeasurePolicyDelegate(
    private val measurePolicy: ChartContentMeasurePolicy,
    private val onZoomValueChanged: () -> Float
) : ChartContentMeasurePolicy by measurePolicy {
    override val DrawScope.childItemSize: Size
        get() {
            val childSize = with(measurePolicy) { childItemSize }
            return childSize.times(onZoomValueChanged())
        }

    override val ChartScope.groupSize: Float
        get() = with(measurePolicy) { groupSize * onZoomValueChanged() }

    override val childDividerSize: Float
        get() = measurePolicy.childDividerSize * onZoomValueChanged()
    override val groupDividerSize: Float
        get() = measurePolicy.groupDividerSize * onZoomValueChanged()

    override fun childLeftTop(
        groupCount: Int,
        groupIndex: Int,
        index: Int
    ): Offset {
        val childLeftTop = measurePolicy.childLeftTop(groupCount, groupIndex, index)
        return if (orientation.isHorizontal) {
            childLeftTop.copy(x = childLeftTop.x * onZoomValueChanged())
        } else {
            childLeftTop.copy(y = childLeftTop.y * onZoomValueChanged())
        }
    }

    override fun ChartScope.measureContent(size: IntSize): Size {
        val contentSize = with(measurePolicy) {
            measureContent(size)
        }
        return if (orientation.isHorizontal) {
            contentSize.copy(width = contentSize.width * onZoomValueChanged())
        } else {
            contentSize.copy(height = contentSize.height * onZoomValueChanged())
        }
    }
}