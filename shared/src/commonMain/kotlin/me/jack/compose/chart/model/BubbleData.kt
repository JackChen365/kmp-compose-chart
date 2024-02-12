package me.jack.compose.chart.model

import androidx.compose.ui.graphics.Color

interface BubbleData {
    val label: String
    val value: Float
    val volume: Float
    val color: Color
}

class SimpleBubbleData(
    override val label: String,
    override val value: Float,
    override val volume: Float,
    override val color: Color
) : BubbleData

