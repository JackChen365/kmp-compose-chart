package me.jack.compose.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.component.BarComponent
import me.jack.compose.chart.component.ChartBorderComponent
import me.jack.compose.chart.component.ChartComponent
import me.jack.compose.chart.component.ChartGridDividerComponent
import me.jack.compose.chart.component.ChartIndicatorComponent
import me.jack.compose.chart.component.toPx
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.scrollable
import me.jack.compose.chart.measure.rememberFixedContentMeasurePolicy
import me.jack.compose.chart.model.BarData
import me.jack.compose.chart.scope.ChartAnchor
import me.jack.compose.demo.ui.ChartEditLayout
import me.jack.compose.demo.ui.ChartEditLayoutScope
import me.jack.compose.demo.ui.singleChartScopeComponent

class PlaygroundDemos {
    @Composable
    fun PlaygroundDemo() {
        val chartComponents = remember {
            mutableStateListOf<ChartComponent<Any>>()
        }
        ChartEditLayout(
            chartContext = ChartContext
                .scrollable(),
            contentMeasurePolicy = rememberFixedContentMeasurePolicy(32.dp.toPx()),
            chartComponents = chartComponents,
            components = {
                buildChartComponent()
            }
        ) {
            Box {
                // left
                Canvas(
                    modifier = Modifier.width(40.dp).fillMaxHeight().align(Alignment.TopStart)
                ) {
                    drawRect(color = Color.Green, style = Stroke(1f))
                }
                // top
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.TopStart)
                ) {
                    drawRect(color = Color.Green, style = Stroke(1f))
                }
                // right
                Canvas(
                    modifier = Modifier.width(40.dp).fillMaxHeight().align(Alignment.TopEnd)
                ) {
                    drawRect(color = Color.Green, style = Stroke(1f))
                }
                // bottom
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomStart)
                ) {
                    drawRect(color = Color.Green, style = Stroke(1f))
                }
                // Center
                Canvas(
                    modifier = Modifier.fillMaxSize().padding(40.dp).align(Alignment.Center)
                ) {
                    drawRect(color = Color.Green, style = Stroke(1f))
                }
            }
        }
    }

    private fun ChartEditLayoutScope.buildChartComponent() {
        chartScopeComponent(
            modifier = Modifier.chartConstraintInfo(
                name = "BorderComponent",
                ChartAnchor.Center
            )
        ) {
            ChartBorderComponent()
        }
        chartScopeComponent(
            modifier = Modifier.chartConstraintInfo(
                name = "GridDividerComponent",
                ChartAnchor.Center
            )
        ) {
            ChartGridDividerComponent()
        }
        chartScopeComponent(
            modifier = Modifier.chartConstraintInfo(
                name = "ChartIndicatorComponent",
                ChartAnchor.Top, ChartAnchor.Bottom
            )
        ) {
            ChartIndicatorComponent()
        }
        singleChartScopeComponent<BarData>(
            modifier = Modifier.chartConstraintInfo(
                name = "BarComponent",
                ChartAnchor.Center
            )
        ) {
            BarComponent()
        }
    }
}