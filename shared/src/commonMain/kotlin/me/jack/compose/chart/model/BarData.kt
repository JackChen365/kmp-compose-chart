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