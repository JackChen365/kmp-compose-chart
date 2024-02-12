package me.jack.compose.chart.measure

import androidx.compose.ui.geometry.Offset
import me.jack.compose.chart.context.isHorizontal

fun ChartContentMeasurePolicy.withScrollMeasurePolicy(
    onScrollValueChanged: () -> Float = { 0f }
): ChartContentMeasurePolicy {
    return ChartScrollContentMeasurePolicyDelegate(this, onScrollValueChanged)
}

class ChartScrollContentMeasurePolicyDelegate(
    private val measurePolicy: ChartContentMeasurePolicy,
    private val onScrollValueChanged: () -> Float
) : ChartContentMeasurePolicy by measurePolicy {

    override fun childLeftTop(
        groupCount: Int,
        groupIndex: Int,
        index: Int
    ): Offset {
        val scrollValue = onScrollValueChanged()
        val childLeftTop = measurePolicy.childLeftTop(groupCount, groupIndex, index)

        val offset = if (orientation.isHorizontal) {
            childLeftTop.copy(x = childLeftTop.x + scrollValue)
        } else {
            childLeftTop.copy(y = childLeftTop.y + scrollValue)
        }
        return offset
    }
}