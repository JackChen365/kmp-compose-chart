package me.jack.compose.chart.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import me.jack.compose.chart.component.BorderSpec
import me.jack.compose.chart.component.BubbleSpec
import me.jack.compose.chart.component.CandleStickBarSpec
import me.jack.compose.chart.component.CandleStickLeftSideSpec
import me.jack.compose.chart.component.CandleStickSpec
import me.jack.compose.chart.component.CurveLineSpec
import me.jack.compose.chart.component.DarkBorderSpec
import me.jack.compose.chart.component.DarkCandleStickBarSpec
import me.jack.compose.chart.component.DarkCandleStickLeftSideSpec
import me.jack.compose.chart.component.DarkGridDividerSpec
import me.jack.compose.chart.component.DarkIndicationSpec
import me.jack.compose.chart.component.DarkMarkerSpec
import me.jack.compose.chart.component.DonutSpec
import me.jack.compose.chart.component.GridDividerSpec
import me.jack.compose.chart.component.IndicationSpec
import me.jack.compose.chart.component.LineSpec
import me.jack.compose.chart.component.MarkerSpec
import me.jack.compose.chart.component.PieSpec

@Composable
fun ChartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val chartTheme = if (darkTheme) {
        ChartTheme()
    } else {
        DarkChartTheme()
    }
    CompositionLocalProvider(
        LocalChartTheme provides chartTheme
    ) {
        content()
    }
}

val LocalChartTheme = staticCompositionLocalOf {
    ChartTheme()
}

open class ChartTheme(
    val borderSpec: BorderSpec = BorderSpec(),
    val bubbleSpec: BubbleSpec = BubbleSpec(),
    val candleStickLeftSideSpec: CandleStickLeftSideSpec = CandleStickLeftSideSpec(),
    val candleStickBarSpec: CandleStickBarSpec = CandleStickBarSpec(),
    val candleStickSpec: CandleStickSpec = CandleStickSpec(),
    val curveLineSpec: CurveLineSpec = CurveLineSpec(),
    val donutSpec: DonutSpec = DonutSpec(),
    val gridDividerSpec: GridDividerSpec = GridDividerSpec(),
    val indicationSpec: IndicationSpec = IndicationSpec(),
    val lineSpec: LineSpec = LineSpec(),
    val markerSpec: MarkerSpec = MarkerSpec(),
    val pieSpec: PieSpec = PieSpec()
)

class DarkChartTheme(
    borderSpec: BorderSpec = DarkBorderSpec(),
    candleStickLeftSideLabelSpec: CandleStickLeftSideSpec = DarkCandleStickLeftSideSpec(),
    candleStickBarSpec: CandleStickBarSpec = DarkCandleStickBarSpec(),
    gridDividerSpec: GridDividerSpec = DarkGridDividerSpec(),
    indicationSpec: IndicationSpec = DarkIndicationSpec(),
    markerSpec: MarkerSpec = DarkMarkerSpec(),
    bubbleSpec: BubbleSpec = BubbleSpec(),
    candleStickSpec: CandleStickSpec = CandleStickSpec(),
    curveLineSpec: CurveLineSpec = CurveLineSpec(),
    donutSpec: DonutSpec = DonutSpec(),
    lineSpec: LineSpec = LineSpec(),
    pieSpec: PieSpec = PieSpec()
) : ChartTheme(
    borderSpec = borderSpec,
    candleStickLeftSideSpec = candleStickLeftSideLabelSpec,
    candleStickBarSpec = candleStickBarSpec,
    gridDividerSpec = gridDividerSpec,
    indicationSpec = indicationSpec,
    markerSpec = markerSpec,
    bubbleSpec = bubbleSpec,
    candleStickSpec = candleStickSpec,
    curveLineSpec = curveLineSpec,
    donutSpec = donutSpec,
    lineSpec = lineSpec,
    pieSpec = pieSpec
)
