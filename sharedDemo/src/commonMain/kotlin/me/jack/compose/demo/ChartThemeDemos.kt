package me.jack.compose.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.component.CurveLineChart
import me.jack.compose.chart.component.LineChart
import me.jack.compose.chart.component.toPx
import me.jack.compose.chart.measure.rememberFixedOverlayContentMeasurePolicy
import me.jack.compose.chart.model.LineData
import me.jack.compose.chart.model.SimpleLineData
import me.jack.compose.chart.scope.rememberChartDataGroup
import me.jack.compose.chart.theme.DarkChartTheme
import me.jack.compose.chart.theme.LightChartTheme
import me.jack.compose.chart.theme.LocalChartTheme
import kotlin.random.Random

class ChartThemeDemos {
    @Composable
    fun LineChartThemeDemo() {
        val dataset = rememberChartDataGroup<LineData> {
            repeat(3) {
                val groupColor = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                dataset("Group:$it") {
                    items(50) {
                        SimpleLineData(
                            value = Random.nextInt(30, 100).toFloat(), color = groupColor
                        )
                    }
                }
            }
        }
        Box {
            var isDarkTheme by remember {
                mutableStateOf(false)
            }
            CompositionLocalProvider(
                LocalChartTheme provides if (isDarkTheme) DarkChartTheme() else LightChartTheme()
            ) {
                Column {
                    LineChart(
                        modifier = Modifier.height(240.dp),
                        contentMeasurePolicy = rememberFixedOverlayContentMeasurePolicy(32.dp.toPx()),
                        chartDataset = dataset
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CurveLineChart(
                        modifier = Modifier.height(240.dp),
                        contentMeasurePolicy = rememberFixedOverlayContentMeasurePolicy(32.dp.toPx()),
                        chartDataset = dataset
                    )
                }
                Button(
                    onClick = {
                        isDarkTheme = !isDarkTheme
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) {
                    Text("Toggle theme")
                }
            }
        }
    }
}