package me.jack.compose.demo.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import me.jack.compose.chart.component.DonutData
import me.jack.compose.chart.component.SimpleDonutData
import me.jack.compose.chart.model.BarData
import me.jack.compose.chart.model.BubbleData
import me.jack.compose.chart.model.CandleData
import me.jack.compose.chart.model.LineData
import me.jack.compose.chart.model.PieData
import me.jack.compose.chart.model.SimpleBarData
import me.jack.compose.chart.model.SimpleBubbleData
import me.jack.compose.chart.model.SimpleCandleData
import me.jack.compose.chart.model.SimpleLineData
import me.jack.compose.chart.model.SimplePieData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.MutableChartDataset
import me.jack.compose.chart.scope.rememberChartDataGroup
import me.jack.compose.chart.scope.rememberChartMutableDataGroup
import kotlin.random.Random

@Composable
fun buildBarChartDataset(groupCount: Int = 3, itemCount: Int = 50): ChartDataset<BarData> {
    return rememberChartDataGroup {
        repeat(groupCount) { chartIndex ->
            dataset("Group:$chartIndex") {
                items(itemCount) {
                    SimpleBarData(
                        value = 10 + Random.nextInt(10, 50).toFloat(),
                        color = Color(
                            Random.nextInt(0, 255),
                            Random.nextInt(0, 255),
                            Random.nextInt(0, 255),
                            0xFF
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun buildAnimateBarDataset(
    groupCount: Int = 3,
    itemCount: Int = 50
): ChartDataset<BarData> {
    val scope = rememberCoroutineScope()
    return rememberChartDataGroup {
        repeat(groupCount) { chartIndex ->
            animatableDataset(scope, "Group:$chartIndex") {
                items(itemCount) {
                    SimpleBarData(
                        value = 10 + Random.nextInt(10, 50).toFloat(),
                        color = Color(
                            Random.nextInt(0, 255),
                            Random.nextInt(0, 255),
                            Random.nextInt(0, 255),
                            0xFF
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun buildBubbleDataset(
    groupCount: Int = 3,
    itemCount: Int = 50
): ChartDataset<BubbleData> {
    return rememberChartDataGroup {
        repeat(groupCount) { groupIndex ->
            val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
            dataset("Group:$groupIndex") {
                items(itemCount) {
                    SimpleBubbleData(
                        label = "Label$groupIndex-$it",
                        value = Random.nextInt(10, 100).toFloat(),
                        volume = Random.nextInt(2, 12).toFloat(),
                        color = groupColor
                    )
                }
            }
        }
    }
}

@Composable
fun buildCandleStickDataset(
    groupCount: Int = 1,
    itemCount: Int = 50
): MutableChartDataset<CandleData> {
    return rememberChartMutableDataGroup {
        repeat(groupCount) { groupIndex ->
            dataset("Group:$groupIndex") {
                items(itemCount) {
                    val low = Random.nextInt(50)
                    val high = Random.nextInt(low + 10, 100)
                    val start = Random.nextInt(low, low + (high - low) / 2)
                    val end = Random.nextInt(low + (high - low) / 2, high)
                    val win = Random.nextBoolean()
                    SimpleCandleData(
                        high = high.toFloat(),
                        low = low.toFloat(),
                        open = if (win) end.toFloat() else start.toFloat(),
                        close = if (!win) end.toFloat() else start.toFloat()
                    )
                }
            }
        }
    }
}

@Composable
fun buildLineDataset(
    groupCount: Int = 3,
    itemCount: Int = 50
): MutableChartDataset<LineData> {
    return rememberChartMutableDataGroup {
        repeat(groupCount) {
            val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
            dataset("Group:$it") {
                items(itemCount) {
                    SimpleLineData(
                        value = Random.nextInt(30, 100).toFloat(), color = groupColor
                    )
                }
            }
        }
    }
}

@Composable
fun buildAnimateLineDataset(
    scope: CoroutineScope,
    groupCount: Int = 3,
    itemCount: Int = 50
): ChartDataset<LineData> {
    return rememberChartDataGroup {
        repeat(groupCount) {
            val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
            animatableDataset(scope, "Group:$it") {
                items(itemCount) {
                    SimpleLineData(
                        value = Random.nextInt(30, 100).toFloat(), color = groupColor
                    )
                }
            }
        }
    }
}

@Composable
fun buildDonutChartDataset(
    groupCount: Int = 3,
    itemCount: Int = 5
): ChartDataset<DonutData> {
    return rememberChartDataGroup {
        repeat(groupCount) {
            val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
            dataset("Group:$it") {
                items(itemCount) { index ->
                    SimpleDonutData(
                        label = "Label:$index",
                        value = Random.nextInt(30, 1000).toFloat(),
                        color = if (1 < groupCount) groupColor else
                            Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                    )
                }
            }
        }
    }
}

@Composable
fun buildPieChartDataset(
    groupCount: Int = 3,
    itemCount: Int = 5
): ChartDataset<PieData> {
    return rememberChartDataGroup {
        repeat(groupCount) {
            val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
            dataset("Group:$it") {
                items(itemCount) { index ->
                    SimplePieData(
                        label = "Label:$index",
                        value = Random.nextInt(30, 1000).toFloat(),
                        color = if (1 < groupCount) groupColor else
                            Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                    )
                }
            }
        }
    }
}