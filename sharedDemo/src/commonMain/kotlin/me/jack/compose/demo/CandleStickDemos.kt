package me.jack.compose.demo

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jack.compose.chart.component.CancelStickChartContent
import me.jack.compose.chart.component.CandleStickBarComponent
import me.jack.compose.chart.component.CandleStickChart
import me.jack.compose.chart.component.CandleStickLeftSideLabel
import me.jack.compose.chart.component.CandleStickScrollableBarComponent
import me.jack.compose.chart.component.ChartBorderComponent
import me.jack.compose.chart.component.ChartIndicatorComponent
import me.jack.compose.chart.component.IndicationSpec
import me.jack.compose.chart.component.TapGestures
import me.jack.compose.chart.context.ChartScrollableState
import me.jack.compose.chart.context.rememberScrollableState
import me.jack.compose.chart.model.CandleData
import me.jack.compose.chart.model.SimpleCandleData
import me.jack.compose.chart.scope.ChartAnchor
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.MutableChartDataset
import me.jack.compose.chart.scope.SINGLE_GROUP_NAME
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.currentRange
import me.jack.compose.chart.scope.rememberChartMutableDataGroup
import kotlin.random.Random

class CandleStickDemos {
    @Composable
    fun PerformanceTestDemo() {
        val chartDataset: MutableChartDataset<CandleData> = rememberChartMutableDataGroup {
            dataset(SINGLE_GROUP_NAME) {
                items(5) {
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
        val scrollableState = rememberScrollableState()
        var isMarketOpened by remember {
            mutableStateOf(true)
        }
        if (isMarketOpened) {
            LaunchStockJob(
                chartDataset = chartDataset,
                scrollableState = scrollableState,
                internal = 10L,
                animateScrollToItem = false
            )
        }
        Column {
            CandleStickChart(modifier = Modifier.requiredHeight(360.dp),
                candleStickSize = 20.dp,
                chartDataset = chartDataset,
                scrollableState = scrollableState,
                tapGestures = TapGestures<CandleData>().onTap {
                }
            ) {
                CandleStickLeftSideLabel()
                ChartBorderComponent()
                ChartIndicatorComponent(
                    modifier = Modifier
                        .anchor(ChartAnchor.Top)
                        .height(20.dp),
                    spec = IndicationSpec(textSize = 8.sp)
                )
                CandleStickBarComponent()
                ChartContent()
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .align(Alignment.End)
            ) {
                Button(onClick = { isMarketOpened = true }) {
                    Text(text = "Open market")
                }
                Button(onClick = { isMarketOpened = false }) {
                    Text(text = "Close market")
                }
            }
        }
    }

    @Composable
    fun CandleStickChartDemo() {
        val chartDataset: ChartDataset<CandleData> = rememberChartMutableDataGroup {
            dataset("Group") {
                items(50) {
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
        Column {
            CandleStickChart(
                modifier = Modifier.requiredHeight(320.dp),
                candleStickSize = 24.dp,
                chartDataset = chartDataset,
                tapGestures = TapGestures<CandleData>().onTap {
                }
            ) {
                CandleStickLeftSideLabel()
                ChartBorderComponent()
                ChartIndicatorComponent(
                    modifier = Modifier
                        .anchor(ChartAnchor.Top)
                        .height(20.dp),
                    spec = IndicationSpec(textSize = 8.sp)
                )
                CandleStickScrollableBarComponent()
                ChartContent()
            }
        }
    }

    @Composable
    fun CandleStickChartMutableDataDemo() {
        val chartDataset: MutableChartDataset<CandleData> = rememberChartMutableDataGroup {
            dataset(SINGLE_GROUP_NAME) {
                items(50) {
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
        val coroutineScope = rememberCoroutineScope()
        val scrollableState = rememberScrollableState()
        Column {
            var tapCount by remember {
                mutableStateOf(0)
            }
            CandleStickChart(modifier = Modifier.requiredHeight(240.dp),
                candleStickSize = 24.dp,
                chartDataset = chartDataset,
                scrollableState = scrollableState,
                tapGestures = TapGestures<CandleData>().onTap {
                    tapCount++
                }
            )
            var positionText by remember {
                mutableStateOf("")
            }
            Text(
                text = positionText,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .align(Alignment.End)
            ) {
                Button(onClick = {
                    val low = Random.nextInt(50)
                    val high = Random.nextInt(low + 10, 100)
                    val start = Random.nextInt(low, low + (high - low) / 2)
                    val end = Random.nextInt(low + (high - low) / 2, high)
                    val win = Random.nextBoolean()
                    chartDataset.add(
                        chartGroup = SINGLE_GROUP_NAME, chartData = SimpleCandleData(
                            high = high.toFloat(),
                            low = low.toFloat(),
                            open = if (win) end.toFloat() else start.toFloat(),
                            close = if (!win) end.toFloat() else start.toFloat()
                        )
                    )
                }) {
                    Text(text = "Add item")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    if (0 < chartDataset.size) {
                        chartDataset.remove(SINGLE_GROUP_NAME, chartDataset.size - 1)
                    }
                }) {
                    Text(text = "Remove item")
                }
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .align(Alignment.End)
            ) {
                var currentItem by remember {
                    mutableIntStateOf(20)
                }
                Button(onClick = {
                    coroutineScope.launch {
                        var newTarget = Random.nextInt(0, chartDataset.size - 15)
                        while (currentItem == newTarget) {
                            newTarget = Random.nextInt(0, chartDataset.size - 15)
                        }
                        positionText += "Position:${newTarget + 1}\n"
                        currentItem = newTarget
                        scrollableState.animateScrollToItem(0, currentItem)
                    }
                }) {
                    Text(text = "Animate to next item")
                }
                Button(onClick = {
                    coroutineScope.launch {
                        var newTarget = Random.nextInt(0, chartDataset.size - 15)
                        while (currentItem == newTarget) {
                            newTarget = Random.nextInt(0, chartDataset.size - 15)
                        }
                        positionText += "Position:${newTarget + 1}\n"
                        currentItem = newTarget
                        scrollableState.scrollToItem(0, currentItem)
                    }
                }) {
                    Text(text = "Next item")
                }
            }
        }
    }

    @Composable
    fun CandleStickChartAutoScrollDemo() {
        val chartDataset: MutableChartDataset<CandleData> = rememberChartMutableDataGroup {
            dataset(SINGLE_GROUP_NAME) {
                items(50) {
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
        val scrollableState = rememberScrollableState()
        var isAutoScroll by remember {
            mutableStateOf(true)
        }
        Column {
            CandleStickChart(modifier = Modifier.requiredHeight(240.dp),
                candleStickSize = 24.dp,
                chartDataset = chartDataset,
                scrollableState = scrollableState,
                tapGestures = TapGestures<CandleData>().onTap {
                }
            ) {
                if (isAutoScroll) {
                    LaunchAutoScrollJob(chartDataset, scrollableState)
                }
                CancelStickChartContent()
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .align(Alignment.End)
            ) {
                Button(onClick = { isAutoScroll = true }) {
                    Text(text = "Auto scroll")
                }
                Button(onClick = { isAutoScroll = false }) {
                    Text(text = "Stop")
                }
            }
        }
    }

    @Composable
    private fun SingleChartScope<CandleData>.LaunchAutoScrollJob(
        chartDataset: MutableChartDataset<CandleData>,
        scrollableState: ChartScrollableState
    ) {
        LaunchedEffect(Unit) {
            launch {
                while (true) {
                    delay(1000L)
                    val visibleSize = (currentRange.last - currentRange.first)
                    val nextIndex = Random.nextInt(chartDataset.size - visibleSize)
                    scrollableState.animateScrollToItem(0, nextIndex)
                }
            }
        }
    }

    @Composable
    fun StockTradeDemo() {
        val chartDataset: MutableChartDataset<CandleData> = rememberChartMutableDataGroup {
            dataset(SINGLE_GROUP_NAME) {
                items(5) {
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
        val scrollableState = rememberScrollableState()
        var isMarketOpened by remember {
            mutableStateOf(true)
        }
        if (isMarketOpened) {
            LaunchStockJob(
                chartDataset = chartDataset,
                scrollableState = scrollableState,
                internal = 10L,
                animateScrollToItem = true
            )
        }
        Column {
            CandleStickChart(modifier = Modifier.requiredHeight(360.dp),
                candleStickSize = 20.dp,
                chartDataset = chartDataset,
                scrollableState = scrollableState,
                tapGestures = TapGestures<CandleData>().onTap {
                }
            ) {
                CandleStickLeftSideLabel()
                ChartBorderComponent()
                ChartIndicatorComponent(
                    modifier = Modifier
                        .anchor(ChartAnchor.Top)
                        .height(20.dp),
                    spec = IndicationSpec(textSize = 8.sp)
                )
                CandleStickBarComponent()
                ChartContent()
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .align(Alignment.End)
            ) {
                Button(onClick = { isMarketOpened = true }) {
                    Text(text = "Open market")
                }
                Button(onClick = { isMarketOpened = false }) {
                    Text(text = "Close market")
                }
            }
        }
    }

    @Composable
    private fun LaunchStockJob(
        chartDataset: MutableChartDataset<CandleData>,
        scrollableState: ChartScrollableState,
        internal: Long = 100L,
        animateScrollToItem: Boolean
    ) {
        LaunchedEffect(Unit) {
            launch {
                while (true) {
                    val low = Random.nextInt(50)
                    val high = Random.nextInt(low + 10, 100)
                    val start = Random.nextInt(low, low + (high - low) / 2)
                    val end = Random.nextInt(low + (high - low) / 2, high)
                    val win = Random.nextBoolean()
                    chartDataset.add(
                        chartGroup = SINGLE_GROUP_NAME,
                        chartData = SimpleCandleData(
                            high = high.toFloat(),
                            low = low.toFloat(),
                            open = if (win) end.toFloat() else start.toFloat(),
                            close = if (!win) end.toFloat() else start.toFloat()
                        )
                    )
                    delay(internal)
                    if (animateScrollToItem) {
                        scrollableState.animateScrollToItem(0, chartDataset.size)
                    } else {
                        scrollableState.scrollToItem(0, chartDataset.size)
                    }
                }
            }
        }
    }
}