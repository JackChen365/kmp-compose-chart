package me.jack.compose.demo

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jack.compose.chart.component.BorderSpec
import me.jack.compose.chart.component.CandleStickBarComponent
import me.jack.compose.chart.component.CandleStickLeftSideLabel
import me.jack.compose.chart.component.ChartAverageAcrossRanksComponent
import me.jack.compose.chart.component.ChartBorderComponent
import me.jack.compose.chart.component.ChartGridDividerComponent
import me.jack.compose.chart.component.ChartIndicatorComponent
import me.jack.compose.chart.component.CombinedChart
import me.jack.compose.chart.component.GridDividerSpec
import me.jack.compose.chart.component.IndicationSpec
import me.jack.compose.chart.component.toPx
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.chartInteraction
import me.jack.compose.chart.context.scrollable
import me.jack.compose.chart.measure.rememberFixedOverlayContentMeasurePolicy
import me.jack.compose.chart.model.BarData
import me.jack.compose.chart.model.BubbleData
import me.jack.compose.chart.model.CandleData
import me.jack.compose.chart.model.LineData
import me.jack.compose.chart.model.SimpleBarData
import me.jack.compose.chart.model.SimpleBubbleData
import me.jack.compose.chart.model.SimpleCandleData
import me.jack.compose.chart.model.SimpleLineData
import me.jack.compose.chart.scope.ChartAnchor
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.forEach
import me.jack.compose.chart.scope.forEachGroup
import me.jack.compose.chart.scope.rememberChartDataGroup
import me.jack.compose.chart.scope.rememberChartMutableDataGroup
import me.jack.compose.chart.scope.rememberSimpleChartDataset
import kotlin.random.Random

class CombinedChartDemos {

    @Composable
    private fun buildLineChartDatasetFromBarData(
        barDataset: ChartDataset<BarData>
    ): ChartDataset<LineData> {
        val dataset = rememberSimpleChartDataset<LineData>()
        barDataset.forEachGroup { chartGroup ->
            val newDataset = mutableListOf<LineData>()
            barDataset.forEach(chartGroup) { data ->
                newDataset.add(SimpleLineData(value = data.value, color = data.color))
            }
            dataset.add(chartGroup, newDataset)
        }
        return dataset
    }

    @Composable
    private fun buildLineChartDatasetFromCandleData(
        dataset: ChartDataset<CandleData>
    ): ChartDataset<LineData> {
        val newLineDataset = rememberSimpleChartDataset<LineData>()
        dataset.forEachGroup { chartGroup ->
            var newDataset = mutableListOf<LineData>()
            val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
            dataset.forEach(chartGroup) { data ->
                newDataset.add(SimpleLineData(value = data.high, color = groupColor))
            }
            newLineDataset.add("$chartGroup-1", newDataset)
            newDataset = mutableListOf()
            val groupColor1 = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
            dataset.forEach(chartGroup) { data ->
                newDataset.add(SimpleLineData(value = data.open, color = groupColor1))
            }
            newLineDataset.add("$chartGroup-2", newDataset)
        }
        return newLineDataset
    }

    @Composable
    private fun buildCancelStickDataset(): ChartDataset<CandleData> {
        return rememberChartMutableDataGroup {
            dataset("Group") {
                items(500) {
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

    @Composable
    private fun buildBarChartDataset(): ChartDataset<BarData> {
        val barDataset = rememberSimpleChartDataset<BarData>()
        repeat(3) {
            val barDataList = mutableListOf<BarData>()
            val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
            repeat(500) {
                barDataList.add(
                    SimpleBarData(
                        value = 10 + Random.nextInt(10, 50).toFloat(),
                        color = groupColor
                    )
                )
            }
            barDataset.add("Group:$it", barDataList)
        }
        return barDataset
    }

    @Composable
    private fun buildBubbleDataset(): ChartDataset<BubbleData> {
        return rememberChartDataGroup {
            repeat(3) { groupIndex ->
                val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                dataset("Group:$groupIndex") {
                    items(500) {
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
    fun CombinedChartPreview() {
        val cancelStickDataset = buildCancelStickDataset()
        CombinedChart(
            modifier = Modifier.height(320.dp),
            chartContext = ChartContext
                .scrollable()
                .chartInteraction(MutableInteractionSource()),
            contentMeasurePolicy = rememberFixedOverlayContentMeasurePolicy(fixedRowSize = 16.dp.toPx()),
            componentContent = {
                candleStickChart(cancelStickDataset)
                stockLineChart(buildLineChartDatasetFromCandleData(cancelStickDataset))
            }
        ) {
            ChartGridDividerComponent(
                GridDividerSpec(
                    color = Color.LightGray.copy(0.2f),
                )
            )
            ChartBorderComponent(
                BorderSpec(
                    color = Color.LightGray.copy(0.2f),
                )
            )
            withSingleScope<CandleData> {
                ChartIndicatorComponent(
                    modifier = Modifier
                        .anchor(ChartAnchor.Top)
                        .height(20.dp),
                    spec = IndicationSpec(textSize = 8.sp)
                )
                CandleStickBarComponent()
                ChartAverageAcrossRanksComponent(textColor = Color.White) { it.high }
                CandleStickLeftSideLabel()
            }
        }
    }
}