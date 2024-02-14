package me.jack.compose.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jack.compose.chart.component.BarChart
import me.jack.compose.chart.component.BarChartContent
import me.jack.compose.chart.component.ChartEditDatasetComponent
import me.jack.compose.chart.component.rememberOnTap
import me.jack.compose.chart.component.toPx
import me.jack.compose.chart.measure.rememberFixedContentMeasurePolicy
import me.jack.compose.chart.model.BarData
import me.jack.compose.chart.model.SimpleBarData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.rememberChartDataGroup
import kotlin.random.Random

class BarDemos {
    @Composable
    fun BarChartDemo() {
        val barDataset = buildChartDataset()
        Box {
            var snackBarVisible by remember { mutableStateOf(false) }
            var currentItem by remember { mutableStateOf<BarData?>(null) }
            Column(modifier = Modifier) {
                BarChart(
                    modifier = Modifier.height(240.dp),
                    contentMeasurePolicy = rememberFixedContentMeasurePolicy(32.dp.toPx(), 8.dp.toPx(), 16.dp.toPx()),
                    chartDataset = barDataset,
                    tapGestures = rememberOnTap {
                        currentItem = it
                        snackBarVisible = true
                    }
                ){
                    BarChartContent()
                    ChartEditDatasetComponent()
                }
//                Spacer(modifier = Modifier.height(8.dp))
//                BarChart(
//                    modifier = Modifier.height(240.dp),
//                    contentMeasurePolicy = rememberFixedVerticalContentMeasurePolicy(
//                        32.dp.toPx(),
//                        8.dp.toPx(),
//                        16.dp.toPx()
//                    ),
//                    chartDataset = barDataset,
//                    tapGestures = rememberOnTap {
//                        currentItem = it
//                        snackBarVisible = true
//                    }
//                )
            }
            if (snackBarVisible) {
                LaunchedEffect(Unit) {
                    launch {
                        delay(3000L)
                        snackBarVisible = false
                    }
                }
                Snackbar(
                    action = {
                        Button(onClick = { snackBarVisible = false }) {
                            Text("Dismiss")
                        }
                    },
                    modifier = Modifier.padding(12.dp).align(Alignment.BottomEnd)
                ) { Text(text = "Clicked item value:${currentItem?.value}") }
            }
        }
    }

//    @Composable
//    fun StackBarChartDemo() {
//        val barDataset = buildChartDataset()
//        Box {
//            var snackBarVisible by remember { mutableStateOf(false) }
//            var currentItem by remember { mutableStateOf<BarData?>(null) }
//            Column(modifier = Modifier) {
//                BarChart(
//                    modifier = Modifier.height(240.dp),
//                    barStyle = BarStyle.Stack,
//                    contentMeasurePolicy = rememberFixedOverlayContentMeasurePolicy(
//                        32.dp.toPx(),
//                        8.dp.toPx(),
//                        Orientation.Horizontal
//                    ),
//                    chartDataset = barDataset,
//                    tapGestures = rememberOnTap {
//                        currentItem = it
//                        snackBarVisible = true
//                    }
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//                BarChart(
//                    modifier = Modifier.height(240.dp),
//                    barStyle = BarStyle.Stack,
//                    contentMeasurePolicy = rememberFixedOverlayContentMeasurePolicy(
//                        32.dp.toPx(),
//                        8.dp.toPx(),
//                        Orientation.Vertical
//                    ),
//                    chartDataset = barDataset,
//                    tapGestures = rememberOnTap {
//                        currentItem = it
//                        snackBarVisible = true
//                    }
//                )
//            }
//            if (snackBarVisible) {
//                LaunchedEffect(Unit) {
//                    launch {
//                        delay(3000L)
//                        snackBarVisible = false
//                    }
//                }
//                Snackbar(
//                    action = {
//                        Button(onClick = { snackBarVisible = false }) {
//                            Text("Dismiss")
//                        }
//                    },
//                    modifier = Modifier.padding(12.dp).align(Alignment.BottomEnd)
//                ) { Text(text = "Clicked item value:${currentItem?.value}") }
//            }
//        }
//    }
//
//    @Composable
//    fun BarMarkChartDemo() {
//        val barDataset: ChartDataset<BarData> = rememberChartDataGroup {
//            repeat(3) { chartIndex ->
//                dataset("Group:$chartIndex") {
//                    items(5) {
//                        SimpleBarData(
//                            value = 10 + Random.nextInt(10, 50).toFloat(),
//                            color = Color(
//                                Random.nextInt(0, 255),
//                                Random.nextInt(0, 255),
//                                Random.nextInt(0, 255),
//                                0xFF
//                            )
//                        )
//                    }
//                }
//            }
//        }
//        val markedChartDataset1 = rememberMarkedChartDataset()
//        val markedChartDataset2 = rememberMarkedChartDataset()
//        Box {
//            var snackBarVisible by remember { mutableStateOf(false) }
//            var currentItem by remember { mutableStateOf<BarData?>(null) }
//            Column(modifier = Modifier) {
//                val maxValue = barDataset.rememberMaxValue { it.value }
//                BarChart(
//                    modifier = Modifier.height(240.dp),
//                    contentMeasurePolicy = rememberFixedContentMeasurePolicy(
//                        32.dp.toPx(),
//                        8.dp.toPx()
//                    ),
//                    chartDataset = barDataset,
//                    tapGestures = rememberOnTap {
//                        currentItem = it
//                        snackBarVisible = true
//                    }
//                ) {
//                    BarChartContent()
//                    BarStickMarkComponent(
//                        markedChartDataset = markedChartDataset1,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                    BarStickMarkComponent(
//                        markedChartDataset = markedChartDataset2,
//                        color = MaterialTheme.colorScheme.secondary
//                    )
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//
//                val markedIndices = remember { mutableMapOf<Int, MutableList<Int>>() }
//                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
//                    Button(
//                        onClick = {
//                            val chartGroupIndex = Random.nextInt(barDataset.groupSize)
//                            val index = Random.nextInt(barDataset.size)
//                            val indices = markedIndices.getOrPut(chartGroupIndex) { mutableListOf() }
//                            indices.add(index)
//                            markedChartDataset1.addMarkedData(
//                                chartGroupIndex = chartGroupIndex,
//                                index = index,
//                                value = Random.nextInt(maxValue.toInt()).toFloat()
//                            )
//                            markedChartDataset2.addMarkedData(
//                                chartGroupIndex = chartGroupIndex,
//                                index = index,
//                                value = Random.nextInt(maxValue.toInt()).toFloat()
//                            )
//                        }
//                    ) {
//                        Text(text = "Random Add")
//                    }
//                    Button(
//                        onClick = {
//                            if (markedIndices.keys.isNotEmpty()) {
//                                val chartGroupIndex = markedIndices.keys.last()
//                                val index = markedIndices[chartGroupIndex]?.lastOrNull()
//                                if (null != index) {
//                                    markedChartDataset1.removeMarkedData(
//                                        chartGroupIndex = chartGroupIndex,
//                                        index = index
//                                    )
//                                    markedChartDataset2.removeMarkedData(
//                                        chartGroupIndex = chartGroupIndex,
//                                        index = index
//                                    )
//                                }
//                            }
//                        }
//                    ) {
//                        Text(text = "Random Remove")
//                    }
//                    Button(
//                        onClick = {
//                            markedChartDataset1.clearMarkedData()
//                            markedChartDataset2.clearMarkedData()
//                        }
//                    ) {
//                        Text(text = "Clear")
//                    }
//                }
//            }
//            if (snackBarVisible) {
//                LaunchedEffect(Unit) {
//                    launch {
//                        delay(3000L)
//                        snackBarVisible = false
//                    }
//                }
//                Snackbar(
//                    action = {
//                        Button(onClick = { snackBarVisible = false }) {
//                            Text("Dismiss")
//                        }
//                    },
//                    modifier = Modifier.padding(12.dp).align(Alignment.BottomEnd)
//                ) { Text(text = "Clicked item value:${currentItem?.value}") }
//            }
//        }
//    }
//
//    @Composable
//    fun AnimatableBarChartDemo() {
//        val scope = rememberCoroutineScope()
//        val barDataset:ChartDataset<BarData> = rememberChartDataGroup {
//            repeat(3) { chartIndex ->
//                animatableDataset(scope, "Group:$chartIndex") {
//                    items(5000) {
//                        SimpleBarData(
//                            value = 10 + Random.nextInt(10, 50).toFloat(),
//                            color = Color(
//                                Random.nextInt(0, 255),
//                                Random.nextInt(0, 255),
//                                Random.nextInt(0, 255),
//                                0xFF
//                            )
//                        )
//                    }
//                }
//            }
//        }
//        Box(modifier = Modifier.fillMaxSize()) {
//            BarChart(
//                modifier = Modifier.height(360.dp),
//                contentMeasurePolicy = rememberFixedContentMeasurePolicy(
//                    fixedRowSize = 32.dp.toPx(),
//                    divideSize = 8.dp.toPx(),
//                    groupDividerSize = 16.dp.toPx()
//                ),
//                chartDataset = barDataset
//            ){
//                BarChartContent()
//                scope.launch {
//                    while (true) {
//                        delay(1000L)
//                        fastForEach {
//                            it.value = 10 + Random.nextInt(10, 50).toFloat()
//                        }
//                    }
//                }
//            }
//        }
//    }
//
    @Composable
    private fun buildChartDataset(): ChartDataset<BarData> {
        return rememberChartDataGroup {
            repeat(3) { chartIndex ->
                dataset("Group:$chartIndex") {
                    items(50) {
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
}