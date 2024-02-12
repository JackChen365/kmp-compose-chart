package me.jack.compose.chart.measure

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import me.jack.compose.chart.scope.ChartScope

class BoxChartContentMeasurePolicy : ChartContentMeasurePolicy {

    override fun ChartScope.measureContent(size: IntSize): Size {
        return size.toSize()
    }

    override var contentSize: IntSize = IntSize.Zero

    override val ChartScope.groupSize: Float
        get() = 0f

    override fun childLeftTop(
        groupCount: Int,
        groupIndex: Int,
        index: Int
    ): Offset {
        return Offset.Zero
    }

    override val DrawScope.childItemSize: Size
        get() = size

}