package me.jack.compose.chart.context

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset

val ChartContext.requireChartZoomState: ChartZoomState
    get() = get(ChartZoomState) ?: error("Can not found the chart zoom state.")

val ChartContext.chartZoomState: ChartZoomState?
    get() = get(ChartZoomState)

fun ChartContext.zoom(
    minimumZoom: Float = 1f,
    maximumZoom: Float = 3f
): ChartContext {
    return this + MutableChartZoomState(minimumZoom, maximumZoom)
}

internal class MutableChartZoomState(
    override val minimumZoom: Float,
    override val maximumZoom: Float
) : ChartZoomState {
    private var zoomState = mutableFloatStateOf(1f)
    private var zoomOffsetState = mutableStateOf(Offset.Zero)
    override var offset: Offset
        set(value) {
            zoomOffsetState.value = value
        }
        get() = zoomOffsetState.value
    override var zoom: Float
        set(value) {
            zoomState.floatValue = value.coerceIn(minimumZoom, maximumZoom)
        }
        get() = zoomState.floatValue

}

interface ChartZoomState : ChartContext.Element {
    companion object Key : ChartContext.Key<ChartZoomState>

    override val key: ChartContext.Key<*> get() = Key

    val maximumZoom: Float
    val minimumZoom: Float

    var offset: Offset
    var zoom: Float

    val zoomRange: ClosedFloatingPointRange<Float>
        get() = minimumZoom..maximumZoom
}