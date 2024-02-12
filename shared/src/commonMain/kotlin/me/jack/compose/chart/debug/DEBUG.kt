package me.jack.compose.chart.debug

private const val DEBUG = false
internal inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("compose-chart: ${generateMsg()}")
    }
}