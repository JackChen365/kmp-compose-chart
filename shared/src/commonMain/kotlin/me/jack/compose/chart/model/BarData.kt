package me.jack.compose.chart.model

import androidx.compose.ui.graphics.Color

interface BarData {
    var value: Float
    var color: Color
}

class SimpleBarData(
    override var value: Float,
    override var color: Color
) : BarData

fun List<Float>.asBarDataList(color: Color): List<BarData> {
    return map { SimpleBarData(color = color, value = it) }
}

fun List<Float>.asBarDataList(color: (Int) -> Color): List<BarData> {
    return mapIndexed { index, value -> SimpleBarData(color = color(index), value = value.toFloat()) }
}