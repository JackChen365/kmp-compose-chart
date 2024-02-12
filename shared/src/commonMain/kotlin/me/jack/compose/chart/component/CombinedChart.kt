package me.jack.compose.chart.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.measure.ChartContentMeasurePolicy
import me.jack.compose.chart.model.BarData
import me.jack.compose.chart.model.BubbleData
import me.jack.compose.chart.model.CandleData
import me.jack.compose.chart.model.LineData
import me.jack.compose.chart.scope.BarChartScope
import me.jack.compose.chart.scope.BubbleChartScope
import me.jack.compose.chart.scope.CandleStickChartScope
import me.jack.compose.chart.scope.ChartCombinedScope
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.LineChartScope
import me.jack.compose.chart.scope.SingleChartScope

class ChartComponent<T>(
    val chartDataset: ChartDataset<T>,
    val tapGestures: TapGestures<T>,
    val content: @Composable (SingleChartScope<T>.() -> Unit) = { }
)

class ChartComponentScope {
    internal val chartComponents = mutableListOf<ChartComponent<*>>()

    @Composable
    fun barChart(
        chartDataset: ChartDataset<BarData>,
        tapGestures: TapGestures<BarData> = rememberCombinedTapGestures(),
        content: @Composable BarChartScope.() -> Unit = {
            BarComponent()
        }
    ) {
        chartComponents.add(
            ChartComponent(
                chartDataset = chartDataset,
                tapGestures = tapGestures,
                content = content
            )
        )
    }

    @Composable
    fun lineChart(
        chartDataset: ChartDataset<LineData>,
        tapGestures: TapGestures<LineData> = rememberCombinedTapGestures(),
        content: @Composable LineChartScope.() -> Unit = {
            ChartLineComponent()
        }
    ) {
        chartComponents.add(
            ChartComponent(
                chartDataset = chartDataset,
                tapGestures = tapGestures,
                content = content
            )
        )
    }

    @Composable
    fun curveLineChart(
        chartDataset: ChartDataset<LineData>,
        tapGestures: TapGestures<LineData> = rememberCombinedTapGestures(),
        content: @Composable LineChartScope.() -> Unit = {
            CurveLineComponent()
        }
    ) {
        chartComponents.add(
            ChartComponent(
                chartDataset = chartDataset,
                tapGestures = tapGestures,
                content = content
            )
        )
    }

    @Composable
    fun bubbleChart(
        chartDataset: ChartDataset<BubbleData>,
        tapGestures: TapGestures<BubbleData> = rememberCombinedTapGestures(),
        content: @Composable BubbleChartScope.() -> Unit = {
            BubbleComponent()
        }
    ) {
        chartComponents.add(
            ChartComponent(
                chartDataset = chartDataset,
                tapGestures = tapGestures,
                content = content
            )
        )
    }

    @Composable
    fun stockLineChart(
        chartDataset: ChartDataset<LineData>,
        tapGestures: TapGestures<LineData> = rememberCombinedTapGestures(),
        content: @Composable LineChartScope.() -> Unit = {
            ChartStockLineComponent()
        }
    ) {
        chartComponents.add(
            ChartComponent(
                chartDataset = chartDataset,
                tapGestures = tapGestures,
                content = content
            )
        )
    }

    @Composable
    fun candleStickChart(
        chartDataset: ChartDataset<CandleData>,
        tapGestures: TapGestures<CandleData> = rememberCombinedTapGestures(),
        content: @Composable CandleStickChartScope.() -> Unit = {
            ChartCandleStickComponent()
            ChartCandleDataMarkerComponent()
        }
    ) {
        chartComponents.add(
            ChartComponent(
                chartDataset = chartDataset,
                tapGestures = tapGestures,
                content = content
            )
        )
    }
}

@Composable
fun CombinedChart(
    modifier: Modifier = Modifier,
    chartContext: ChartContext = ChartContext,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    componentContent: @Composable ChartComponentScope.() -> Unit = { },
    content: @Composable ChartCombinedScope.() -> Unit = { }
) {
    val componentScope = ChartComponentScope()
    componentContent.invoke(componentScope)
    @Suppress("UNCHECKED_CAST")
    (CombinedChartLayout(
        modifier = modifier,
        chartContext = chartContext,
        contentMeasurePolicy = contentMeasurePolicy,
        chartComponents = componentScope.chartComponents as List<ChartComponent<Any>>,
        content = content
    ))
}