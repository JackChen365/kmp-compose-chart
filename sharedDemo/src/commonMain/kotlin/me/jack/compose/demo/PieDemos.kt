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
import me.jack.compose.chart.component.PieChart
import me.jack.compose.chart.component.rememberCombinedTapGestures
import me.jack.compose.chart.model.PieData
import me.jack.compose.chart.model.SimplePieData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.SINGLE_GROUP_NAME
import me.jack.compose.chart.scope.rememberChartDataGroup
import kotlin.random.Random

class PieDemos {

    @Composable
    fun PieChartDemo() {
        var snackBarVisible by remember { mutableStateOf(false) }
        var currentHintText by remember { mutableStateOf("") }
        Box {
            Column {
                PieChart(
                    modifier = Modifier.height(240.dp),
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
                Spacer(modifier = Modifier.height(8.dp))
                PieChart(
                    modifier = Modifier
                        .height(240.dp)
                        .padding(12.dp),
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

    @Composable
    private fun buildChartDataset(): ChartDataset<PieData> {
        return rememberChartDataGroup {
            repeat(3) {
                val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                dataset("Group:$it") {
                    items(3) { index ->
                        SimplePieData(
                            label = "Label:$index",
                            value = Random.nextInt(30, 1000).toFloat(),
                            color = groupColor
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun buildChartSingleDataset(): ChartDataset<PieData> {
        return rememberChartDataGroup {
            dataset(SINGLE_GROUP_NAME) {
                items(5) { index ->
                    SimplePieData(
                        label = "Label:$index",
                        value = Random.nextInt(30, 1000).toFloat(),
                        color = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                    )
                }
            }
        }
    }
}