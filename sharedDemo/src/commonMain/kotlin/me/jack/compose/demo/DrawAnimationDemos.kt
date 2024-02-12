package me.jack.compose.demo

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.component.SingleChartLayout
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.chartInteraction
import me.jack.compose.chart.draw.ChartCanvas
import me.jack.compose.chart.measure.rememberBoxChartContentMeasurePolicy
import me.jack.compose.chart.model.LineData
import me.jack.compose.chart.model.SimpleLineData
import me.jack.compose.chart.scope.SINGLE_GROUP_NAME
import me.jack.compose.chart.scope.fastForEach
import me.jack.compose.chart.scope.rememberChartDataGroup
import kotlin.random.Random

class DrawAnimationDemos {

    @Composable
    fun DrawElementDemo() {
        val dataset = rememberChartDataGroup<LineData> {
            dataset(SINGLE_GROUP_NAME) {
                items(1) {
                    SimpleLineData(
                        value = Random.nextInt(100).toFloat(),
                        color = Color(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255), 0xFF)
                    )
                }
            }
        }
        SingleChartLayout(
            modifier = Modifier.fillMaxSize(),
            chartContext = ChartContext.chartInteraction(MutableInteractionSource()),
            contentMeasurePolicy = rememberBoxChartContentMeasurePolicy(),
            chartDataset = dataset,
        ) {
            ChartCanvas(Modifier.fillMaxSize()) {
                fastForEach {
                    drawCircle(
                        color = Color.Red whenPressedAnimateTo Color.Red.copy(alpha = 0.4f),
                        center = size.center,
                        radius = 100.dp.toPx() whenPressedAnimateTo 120.dp.toPx()
                    )
                    drawRect(
                        color = Color.Red whenPressedAnimateTo Color.Red.copy(alpha = 0.4f),
                        topLeft = Offset(
                            x = 100.dp.toPx() whenPressedAnimateTo 200.dp.toPx(),
                            y = 100.dp.toPx()
                        ),
                        size = DpSize(100.dp, 100.dp).toSize()
                    )
                }
            }
        }
    }
}