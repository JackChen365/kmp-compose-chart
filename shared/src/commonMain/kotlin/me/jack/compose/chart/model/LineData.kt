package me.jack.compose.chart.model

import androidx.compose.ui.graphics.Color

interface LineData {
    var value: Float
    var color: Color
}

class SimpleLineData(
    override var value: Float,
    override var color: Color
) : LineData

fun List<Float>.asLineDataList(color: Color): List<LineData> {
    return map { SimpleLineData(color = color, value = it) }
}

fun List<Float>.asLineDataList(color: (Int) -> Color): List<LineData> {
    return mapIndexed { index, value -> SimpleLineData(color = color(index), value = value.toFloat()) }
}