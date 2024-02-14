package me.jack.compose.chart.model

interface CandleData {
    val high: Float
    val low: Float
    val open: Float
    val close: Float
}

class SimpleCandleData(
    override val high: Float,
    override val low: Float,
    override val open: Float,
    override val close: Float
) : CandleData