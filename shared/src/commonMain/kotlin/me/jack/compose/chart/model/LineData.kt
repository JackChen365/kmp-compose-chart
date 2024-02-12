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