package me.jack.compose.chart.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jack.compose.chart.context.ChartScrollState
import me.jack.compose.chart.context.isElementAvailable
import me.jack.compose.chart.context.requireChartScrollState
import me.jack.compose.chart.scope.ChartAnchor
import me.jack.compose.chart.scope.ChartScope
import me.jack.compose.chart.scope.MutableChartDataset
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.chartChildDivider
import me.jack.compose.chart.scope.chartGroupDivider
import me.jack.compose.chart.scope.chartGroupOffsets
import me.jack.compose.chart.scope.isHorizontal
import me.jack.compose.chart.scope.rememberMaxValue
import me.jack.compose.chart.theme.LocalChartTheme

interface ChartComponentSpec

/**
 * Specification for ChartBorderComponent
 */
open class BorderSpec(
    val strokeWidth: Dp = 2.dp,
    val color: Color = Color.Gray
) : ChartComponentSpec

class DarkBorderSpec(
    strokeWidth: Dp = 2.dp,
    color: Color = Color.LightGray
) : BorderSpec(strokeWidth, color)

@Composable
fun ChartScope.ChartBorderComponent(
    spec: BorderSpec = LocalChartTheme.current.borderSpec
) {
    Canvas(
        modifier = Modifier
            .fillMaxHeight()
            .anchor(anchor = ChartAnchor.Center)
    ) {
        drawLine(
            color = spec.color,
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            strokeWidth = spec.strokeWidth.toPx()
        )
        drawLine(
            color = spec.color,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = spec.strokeWidth.toPx()
        )
    }
}

open class GridDividerSpec(
    val rows: Int = 4,
    val columns: Int = 5,
    val strokeWidth: Dp = 1.dp,
    val color: Color = Color.Gray
) : ChartComponentSpec

class DarkGridDividerSpec(
    rows: Int = 4,
    columns: Int = 5,
    strokeWidth: Dp = 1.dp,
    color: Color = Color.LightGray
) : GridDividerSpec(rows, columns, strokeWidth, color)

@Composable
fun ChartScope.ChartGridDividerComponent(
    spec: GridDividerSpec = LocalChartTheme.current.gridDividerSpec
) {
    if (chartContext.isElementAvailable(ChartScrollState)) {
        ChartScrollableGridDividerComponent(spec = spec)
    } else {
        ChartFixedGridDividerComponent(spec = spec)
    }
}

@Composable
private fun ChartScope.ChartFixedGridDividerComponent(
    spec: GridDividerSpec = LocalChartTheme.current.gridDividerSpec
) {
    Canvas(
        modifier = Modifier
            .fillMaxHeight()
            .anchor(anchor = ChartAnchor.Center)
            .clipToBounds()
    ) {
        var crossAxisOffset = 0f
        val crossAxisItemSize = size.height / spec.rows
        for (row in 0..spec.rows) {
            drawLine(
                color = spec.color,
                start = Offset(0f, crossAxisOffset),
                end = Offset(size.width, crossAxisOffset),
                strokeWidth = spec.strokeWidth.toPx()
            )
            crossAxisOffset += crossAxisItemSize
        }
        var mainAxisOffset = 0f
        val mainAxisItemSize = size.width / spec.columns
        for (column in 0..spec.columns) {
            drawLine(
                color = spec.color,
                start = Offset(mainAxisOffset, 0f),
                end = Offset(mainAxisOffset, size.height),
                strokeWidth = spec.strokeWidth.toPx()
            )
            mainAxisOffset += mainAxisItemSize
        }
    }
}

@Composable
private fun ChartScope.ChartScrollableGridDividerComponent(
    spec: GridDividerSpec = LocalChartTheme.current.gridDividerSpec
) {
    Canvas(
        modifier = Modifier
            .clipToBounds()
            .fillMaxHeight()
            .anchor(anchor = ChartAnchor.Center)
    ) {
        if (isHorizontal) {
            horizontalScrollableGridDividerComponent(
                chartScope = this@ChartScrollableGridDividerComponent,
                fixedDividerCount = spec.columns,
                dividerColor = spec.color,
                strokeWidth = spec.strokeWidth
            )
        } else {
            verticalScrollableGridDividerComponent(
                chartScope = this@ChartScrollableGridDividerComponent,
                fixedDividerCount = spec.rows,
                dividerColor = spec.color,
                strokeWidth = spec.strokeWidth
            )
        }
    }
}

private fun DrawScope.horizontalScrollableGridDividerComponent(
    chartScope: ChartScope,
    fixedDividerCount: Int = 4,
    dividerColor: Color = Color.LightGray,
    strokeWidth: Dp
) {
    var crossAxisOffset = 0f
    val crossAxisItemSize = size.height / (fixedDividerCount + 1)
    for (row in 0..fixedDividerCount) {
        drawLine(
            color = dividerColor,
            start = Offset(0f, crossAxisOffset),
            end = Offset(size.width, crossAxisOffset),
            strokeWidth = strokeWidth.toPx()
        )
        crossAxisOffset += crossAxisItemSize
    }
    with(chartScope) {
        val chartScrollState = chartContext.requireChartScrollState
        var itemOffset =
            -chartScrollState.firstVisibleItemOffset - chartGroupDivider / 2 - chartChildDivider / 2
        chartScrollState.currentVisibleRange.forEach { _ ->
            drawLine(
                color = dividerColor,
                start = Offset(itemOffset, 0f),
                end = Offset(itemOffset, size.height),
                strokeWidth = strokeWidth.toPx()
            )
            itemOffset += chartGroupOffsets
        }
    }
}

private fun DrawScope.verticalScrollableGridDividerComponent(
    chartScope: ChartScope,
    fixedDividerCount: Int = 4,
    dividerColor: Color = Color.LightGray,
    strokeWidth: Dp
) {
    with(chartScope) {
        val chartScrollState = chartContext.requireChartScrollState
        var itemOffset =
            -chartScrollState.firstVisibleItemOffset - chartGroupDivider / 2 - chartChildDivider / 2
        chartScrollState.currentVisibleRange.forEach { _ ->
            drawLine(
                color = dividerColor,
                start = Offset(0f, itemOffset),
                end = Offset(size.width, itemOffset),
                strokeWidth = strokeWidth.toPx()
            )
            itemOffset += chartGroupOffsets
        }
    }
    val mainAxisItemSize = size.width / fixedDividerCount
    var mainAxisOffset = 0f
    for (i in 0..fixedDividerCount) {
        drawLine(
            color = dividerColor,
            start = Offset(mainAxisOffset, 0f),
            end = Offset(mainAxisOffset, size.height),
            strokeWidth = strokeWidth.toPx()
        )
        mainAxisOffset += mainAxisItemSize
    }
}

/**
 * Specification for ChartBorderComponent
 */
open class IndicationSpec(
    val backgroundColor: Color = Color.Black.copy(alpha = 0.1f),
    val textColor: Color = Color.DarkGray,
    val textSize: TextUnit = 8.sp,
    val size: Dp = 32.dp
) : ChartComponentSpec

/**
 * Specification for ChartBorderComponent
 */
class DarkIndicationSpec(
    backgroundColor: Color = Color.LightGray,
    textColor: Color = Color.White,
    textSize: TextUnit = 8.sp,
    size: Dp = 32.dp
) : IndicationSpec(backgroundColor, textColor, textSize, size)

@Composable
fun ChartScope.ChartIndicatorComponent(
    modifier: Modifier = Modifier,
    spec: IndicationSpec = LocalChartTheme.current.indicationSpec
) {
    val scrollState = chartContext.requireChartScrollState
    val groupMainAxis = chartGroupOffsets
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .clipToBounds()
            .background(color = spec.backgroundColor)
            .chartMainAxisIndicator(this, spec.size)
    ) {
        var textOffset = -scrollState.firstVisibleItemOffset
        for (index in scrollState.firstVisibleItemIndex until scrollState.lastVisibleItemIndex) {
            val textLayoutResult = textMeasurer.measure(
                text = (index + 1).toString(),
                style = TextStyle(color = spec.textColor, fontSize = spec.textSize)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = if (isHorizontal) Offset(
                    textOffset + (groupMainAxis - textLayoutResult.size.width) / 2f,
                    (size.height - textLayoutResult.size.height) / 2f
                )
                else Offset(
                    (size.width - textLayoutResult.size.width) / 2f,
                    textOffset + (groupMainAxis - textLayoutResult.size.height) / 2f,
                )
            )
            textOffset += groupMainAxis
        }
    }
}

fun Modifier.chartMainAxisIndicator(
    chartScope: ChartScope,
    size: Dp,
): Modifier = with(chartScope) {
    return if (isHorizontal) {
        anchor(ChartAnchor.Bottom)
            .fillMaxWidth()
            .height(size)
    } else {
        anchor(ChartAnchor.Start)
            .fillMaxHeight()
            .width(size)
    }
}

fun Modifier.chartCrossAxisSize(
    chartScope: ChartScope,
    size: Dp,
): Modifier = with(chartScope) {
    return if (isHorizontal) {
        anchor(ChartAnchor.Start)
            .fillMaxHeight()
            .width(size)
    } else {
        anchor(ChartAnchor.Bottom)
            .fillMaxWidth()
            .height(size)
    }
}

open class AverageAcrossRanksSpec(
    val backgroundColor: Color = Color.LightGray,
    val level: Int = 10,
    val size: Dp = 32.dp,
    val textColor: Color = Color.Black,
    val textSize: TextUnit = 12.sp
) : ChartComponentSpec

class DarkAverageAcrossRanksSpec(
    backgroundColor: Color = Color.DarkGray,
    level: Int = 10,
    size: Dp = 32.dp,
    textColor: Color = Color.White,
    textSize: TextUnit = 12.sp
) : AverageAcrossRanksSpec(backgroundColor, level, size, textColor, textSize)

@Composable
fun ChartScope.ChartAverageAcrossRanksComponent(
    spec: AverageAcrossRanksSpec = LocalChartTheme.current.averageAcrossRanksSpec,
    maxValue: Float
) {
    ChartAverageAcrossRanksComponent(
        modifier = Modifier.clipToBounds()
            .background(color = spec.backgroundColor)
            .chartCrossAxisSize(this, spec.size),
        spec = spec,
        isHorizontal = isHorizontal,
        maxValue = maxValue
    )
}

@Composable
fun <T> SingleChartScope<T>.ChartAverageAcrossRanksComponent(
    spec: AverageAcrossRanksSpec = LocalChartTheme.current.averageAcrossRanksSpec,
    maxValueEvaluator: (T) -> Float
) {
    val maxValue = chartDataset.rememberMaxValue(maxValueEvaluator)
    if (0 >= maxValue) return
    ChartAverageAcrossRanksComponent(
        modifier = Modifier.clipToBounds()
            .background(color = spec.backgroundColor)
            .chartCrossAxisSize(this, spec.size),
        spec = spec,
        isHorizontal = isHorizontal,
        maxValue = maxValue
    )
}

@Composable
fun ChartAverageAcrossRanksComponent(
    modifier: Modifier = Modifier,
    spec: AverageAcrossRanksSpec = LocalChartTheme.current.averageAcrossRanksSpec,
    isHorizontal: Boolean = true,
    maxValue: Float
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
    ) {
        val textItemSize = this.size.maxDimension / spec.level
        for (i in 0 until spec.level) {
            val levelSize = (maxValue / spec.level * (spec.level - i))
            val textLayoutResult = textMeasurer.measure(
                text = levelSize.toInt().toString(),
                style = TextStyle(color = spec.textColor, fontSize = spec.textSize)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = if (isHorizontal) Offset(
                    (this.size.width - textLayoutResult.size.width) / 2f,
                    i * textItemSize + (textItemSize - textLayoutResult.size.height) / 2
                ) else Offset(
                    (spec.level - i - 1) * textItemSize + (textItemSize - textLayoutResult.size.width) / 2,
                    (this.size.height - textLayoutResult.size.height) / 2f,
                )
            )
        }
    }
}

open class EditDatasetSpec(
    val padding: PaddingValues = PaddingValues(12.dp),
    val indicationSize: Dp = 24.dp,
    val indicationColor: Color = Color.Blue,
    val textColor: Color = Color.Black,
    val textSize: TextUnit = 12.sp
) : ChartComponentSpec

@Composable
fun <T> SingleChartScope<T>.ChartEditDatasetComponent(
    spec: EditDatasetSpec = LocalChartTheme.current.editDatasetSpec
) {
    val mutableChartDataset = chartDataset as MutableChartDataset
    val chartGroups = remember(Unit) {
        mutableChartDataset.chartGroups.toList()
    }
    val originChartGroupDataset = remember(Unit) {
        chartGroups.map { it to mutableChartDataset.dataset[it] }.toMutableStateMap()
    }
    Box(modifier = Modifier.fillMaxSize().padding(spec.padding)) {
        Column(modifier = Modifier.align(Alignment.TopEnd)) {
            val chartGroupSelectStateMap = remember {
                chartGroups.map { it to true }.toMutableStateMap()
            }
            chartGroups.forEach { chartGroup ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                        onClick = {
                            chartGroupSelectStateMap[chartGroup] =
                                !chartGroupSelectStateMap.getOrDefault(chartGroup, false)
                            if (true == chartGroupSelectStateMap[chartGroup]) {
                                val chartData = originChartGroupDataset[chartGroup]
                                if (null != chartData) {
                                    mutableChartDataset.add(chartGroup, chartData)
                                }
                            } else {
                                mutableChartDataset.remove(chartGroup)
                            }
                        }
                    )
                ) {
                    Spacer(
                        modifier = Modifier.size(spec.indicationSize)
                            .background(
                                color = if (chartGroupSelectStateMap.getOrDefault(
                                        chartGroup,
                                        false
                                    )
                                ) spec.indicationColor else spec.indicationColor.copy(0.4f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = chartGroup, color = spec.textColor, fontSize = spec.textSize)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}