package me.jack.compose.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import me.jack.compose.chart.component.DonutChart
import me.jack.compose.chart.component.DonutData
import me.jack.compose.chart.component.SimpleDonutData
import me.jack.compose.chart.component.rememberCombinedTapGestures
import me.jack.compose.chart.debug.DebugDonutComponent
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.SINGLE_GROUP_NAME
import me.jack.compose.chart.scope.rememberChartDataGroup
import kotlin.random.Random

class DonutDemos {
    @Composable
    private fun buildChartDataset(): ChartDataset<DonutData> {
        return rememberChartDataGroup {
            repeat(3) {
                dataset("Group:$it") {
                    items(5) { index ->
                        SimpleDonutData(
                            label = "Label:$index",
                            value = Random.nextInt(30, 1000).toFloat(),
                            color = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun buildChartSingleDataset(): ChartDataset<DonutData> {
        return rememberChartDataGroup {
            dataset(SINGLE_GROUP_NAME) {
                items(5) { index ->
                    SimpleDonutData(
                        label = "Label:$index",
                        value = Random.nextInt(30, 1000).toFloat(),
                        color = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                    )
                }
            }
        }
    }

    @Composable
    fun DonutChartDemo() {
        var snackBarVisible by remember { mutableStateOf(false) }
        var currentHintText by remember { mutableStateOf("") }
        Box {
            Column {
                DonutChart(
                    modifier = Modifier.height(240.dp),
                    chartDataset = buildChartDataset(),
                    tapGestures = rememberCombinedTapGestures(
                        onTap = {
                            currentHintText = "Tap item value:${it.value}"
                            snackBarVisible = true
                        },
                        onDoubleTap = {
                            currentHintText = "Double tap item value:${it.value}"
                            snackBarVisible = true
                        },
                        onLongPress = {
                            currentHintText = "Long Press item value:${it.value}"
                            snackBarVisible = true
                        }
                    )
                ){
                    ChartContent()
                    DebugDonutComponent()
                }
                Spacer(modifier = Modifier.height(8.dp))
                DonutChart(
                    modifier = Modifier
                        .height(240.dp),
                    chartDataset = buildChartSingleDataset(),
                    tapGestures = rememberCombinedTapGestures(
                        onTap = {
                            currentHintText = "Tap item value:${it.value}"
                            snackBarVisible = true
                        },
                        onDoubleTap = {
                            currentHintText = "Double tap item value:${it.value}"
                            snackBarVisible = true
                        },
                        onLongPress = {
                            currentHintText = "Long Press item value:${it.value}"
                            snackBarVisible = true
                        }
                    )
                )
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
                ) { Text(text = currentHintText) }
            }
        }
    }
}