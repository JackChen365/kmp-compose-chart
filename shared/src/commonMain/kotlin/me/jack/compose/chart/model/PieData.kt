package me.jack.compose.chart.model

import androidx.compose.ui.graphics.Color

interface PieData {
    val label: String
    val value: Float
    val color: Color
}

class SimplePieData(
    override val label: String, override val value: Float, override val color: Color
) : PieData


