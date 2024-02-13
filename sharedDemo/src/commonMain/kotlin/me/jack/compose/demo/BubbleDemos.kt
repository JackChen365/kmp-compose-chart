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
import me.jack.compose.chart.component.BubbleChart
import me.jack.compose.chart.component.rememberOnTap
import me.jack.compose.chart.component.toPx
import me.jack.compose.chart.measure.rememberFixedContentMeasurePolicy
import me.jack.compose.chart.measure.rememberFixedVerticalContentMeasurePolicy
import me.jack.compose.chart.model.BubbleData
import me.jack.compose.chart.model.SimpleBubbleData
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.rememberChartDataGroup
import kotlin.random.Random

class BubbleDemos {

    @Composable
    private fun buildChartDataset(): ChartDataset<BubbleData> {
        return rememberChartDataGroup {
            repeat(3) { groupIndex ->
                val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                dataset("Group:$groupIndex") {
                    items(50) {
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
    fun BubbleChartDemo() {
        Box {
            var snackBarVisible by remember { mutableStateOf(false) }
            var currentItem by remember { mutableStateOf<BubbleData?>(null) }
            Column {
                BubbleChart(
                    modifier = Modifier.height(240.dp),
                    contentMeasurePolicy = rememberFixedContentMeasurePolicy(32.dp.toPx()),
                    chartDataset = buildChartDataset(),
                    tapGestures = rememberOnTap {
                        currentItem = it
                        snackBarVisible = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BubbleChart(
                    modifier = Modifier.height(240.dp),
                    contentMeasurePolicy = rememberFixedVerticalContentMeasurePolicy(32.dp.toPx()),
                    chartDataset = buildChartDataset(),
                    tapGestures = rememberOnTap {
                        currentItem = it
                        snackBarVisible = true
                    }
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
                ) { Text(text = "Clicked item value:${currentItem?.value}") }
            }
        }
    }
}