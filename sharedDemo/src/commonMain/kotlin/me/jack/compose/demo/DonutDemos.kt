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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jack.compose.chart.component.DonutChart
import me.jack.compose.chart.component.rememberCombinedTapGestures
import me.jack.compose.chart.debug.DebugDonutComponent
import me.jack.compose.demo.data.buildDonutChartDataset

class DonutDemos {

    @Composable
    fun DonutChartDemo() {
        var snackBarVisible by remember { mutableStateOf(false) }
        var currentHintText by remember { mutableStateOf("") }
        Box {
            Column {
                DonutChart(
                    modifier = Modifier.height(240.dp),
                    chartDataset = buildDonutChartDataset(
                        groupCount = 1,
                        itemCount = 5
                    ),
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
                    chartDataset = buildDonutChartDataset(
                        groupCount = 3,
                        itemCount = 5
                    ),
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