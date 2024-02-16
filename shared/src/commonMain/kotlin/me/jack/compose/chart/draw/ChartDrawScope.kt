package me.jack.compose.chart.draw

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.CoroutineScope
import me.jack.compose.chart.component.TapGestures
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.chartInteractionHandler
import me.jack.compose.chart.context.chartScrollState
import me.jack.compose.chart.context.tryEmit
import me.jack.compose.chart.draw.cache.DrawingKeyframeCache
import me.jack.compose.chart.draw.interaction.elementInteraction
import me.jack.compose.chart.interaction.ChartElementInteraction
import me.jack.compose.chart.interaction.ChartTapInteraction
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.ChartDatasetAccessScope
import me.jack.compose.chart.scope.ChartDatasetAccessScopeInstance
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.fastForEach
import me.jack.compose.chart.scope.fastForEachWithNext
import me.jack.compose.chart.scope.getChartGroupData

@Composable
fun <T> SingleChartScope<T>.ChartCanvas(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    onDraw: ChartDrawScope<T>.() -> Unit
) {
    val interactionStates = rememberInteractionStates(interactionSource)
    val drawBlock by rememberUpdatedState(onDraw)
    var chartDrawScope: ChartDrawScope<T>? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    Spacer(
        modifier = modifier
            .drawBehind {
                if (null == chartDrawScope) {
                    chartDrawScope = ChartDrawScope(
                        singleChartScope = this@ChartCanvas,
                        drawScope = this,
                        scope = scope,
                        tapGestures = tapGestures,
                        interactionStates = interactionStates
                    )
                }
                val currentChartDrawScope = checkNotNull(chartDrawScope)
                currentChartDrawScope.reset()
                currentChartDrawScope.isPreLayout = true
                drawBlock.invoke(currentChartDrawScope)
                currentChartDrawScope.isPreLayout = false
                currentChartDrawScope.updateCurrentActivatedDrawElement()
                currentChartDrawScope.moveToNextDrawElement()
                drawBlock.invoke(currentChartDrawScope)
            }
    )
}

@Composable
fun <T> SingleChartScope<T>.LazyChartCanvas(
    modifier: Modifier = Modifier.padding(),
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    onDraw: ChartDrawScope<T>.(current: T) -> Unit
) {
    val interactionStates = rememberInteractionStates(interactionSource)
    val drawBlock by rememberUpdatedState(onDraw)
    var chartDrawScope: ChartDrawScope<T>? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    Spacer(
        modifier = modifier.drawBehind {
            if (null == chartDrawScope) {
                chartDrawScope = ChartDrawScope(
                    singleChartScope = this@LazyChartCanvas,
                    drawScope = this,
                    scope = scope,
                    tapGestures = tapGestures,
                    interactionStates = interactionStates
                )
            }
            val currentChartDrawScope = checkNotNull(chartDrawScope)
            currentChartDrawScope.reset()
            currentChartDrawScope.isPreLayout = true
            fastForEach { currentItem ->
                drawBlock(currentChartDrawScope, currentItem)
            }
            currentChartDrawScope.isPreLayout = false
            currentChartDrawScope.updateCurrentActivatedDrawElement()
            currentChartDrawScope.moveToNextDrawElement()
            fastForEach { currentItem ->
                drawBlock(currentChartDrawScope, currentItem)
            }
        }
    )
}

@Composable
fun <T> SingleChartScope<T>.LazyChartCanvas(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    onDraw: ChartDrawScope<T>.(current: T, next: T) -> Unit
) {
    val interactionStates = rememberInteractionStates(interactionSource)
    val drawBlock by rememberUpdatedState(onDraw)
    var chartDrawScope: ChartDrawScope<T>? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    Spacer(
        modifier = modifier.drawBehind {
            if (null == chartDrawScope) {
                chartDrawScope = ChartDrawScope(
                    singleChartScope = this@LazyChartCanvas,
                    drawScope = this,
                    scope = scope,
                    tapGestures = tapGestures,
                    interactionStates = interactionStates
                )
            }
            val currentChartDrawScope = checkNotNull(chartDrawScope)
            currentChartDrawScope.reset()
            currentChartDrawScope.isPreLayout = true
            fastForEachWithNext { current, next ->
                drawBlock(currentChartDrawScope, current, next)
            }
            currentChartDrawScope.isPreLayout = false
            currentChartDrawScope.updateCurrentActivatedDrawElement()
            currentChartDrawScope.moveToNextDrawElement()
            fastForEachWithNext { current, next ->
                drawBlock(currentChartDrawScope, current, next)
            }
        }
    )
}

internal fun <T : DrawElement> DrawingKeyframeCache.getCachedDrawElement(
    key: Class<T>,
    onHitCache: (T) -> Unit = { },
    defaultValue: (key: Class<T>) -> T = ::defaultDrawingElementFactory
): T {
    return getCachedValueOrPut(
        key = key,
        onHitCache = onHitCache,
        defaultValue = defaultValue
    )
}

internal fun <T : DrawElement> defaultDrawingElementFactory(key: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    return when (key) {
        DrawElement.Rect::class.java -> DrawElement.Rect()
        DrawElement.Circle::class.java -> DrawElement.Circle()
        DrawElement.Oval::class.java -> DrawElement.Oval()
        DrawElement.Arc::class.java -> DrawElement.Arc()
        else -> error("Does not support this class type:$key")
    } as T
}

class ChartDrawScope<T>(
    drawScope: DrawScope,
    interactionStates: ChartInteractionStates,
    scope: CoroutineScope,
    val singleChartScope: SingleChartScope<T>,
    private val tapGestures: TapGestures<T>
) : ChartAnimateDrawScope(drawScope, scope, interactionStates),
    ChartDatasetAccessScope by ChartDatasetAccessScopeInstance {
    val chartContext: ChartContext = singleChartScope.chartContext
    val chartDataset: ChartDataset<T> = singleChartScope.chartDataset

    /**
     * Interaction drawing element cache pool, we can not mix this pool with screen drawing element.
     * The [drawingElementCache] will be reused per frame but only for interaction, we are supposed to keep it until user change his behavior
     */
    private val interactionDrawingElementCache = DrawingKeyframeCache()

    val currentLeftTopOffset: Offset
        get() {
            return with(singleChartScope) {
                with(singleChartScope.contentMeasurePolicy) {
                    childLeftTop(groupCount, groupIndex, index)
                }
            }
        }

    val nextLeftTopOffset: Offset
        get() {
            return with(singleChartScope) {
                with(singleChartScope.contentMeasurePolicy) {
                    childLeftTop(groupCount, groupIndex, index + 1)
                }
            }
        }

    val childCenterOffset: Offset
        get() {
            return Offset(
                x = currentLeftTopOffset.x + childSize.width / 2,
                y = currentLeftTopOffset.y + childSize.height / 2
            )
        }

    val nextChildCenterOffset: Offset
        get() {
            return Offset(
                x = nextLeftTopOffset.x + childSize.width / 2,
                y = nextLeftTopOffset.y + childSize.height / 2
            )
        }

    val childSize: Size
        get() = with(singleChartScope.contentMeasurePolicy) { childItemSize }

    val childOffsets: Offset
        get() = with(singleChartScope.contentMeasurePolicy) {
            Offset(
                x = childSize.width + childDividerSize,
                y = childSize.height + childDividerSize
            )
        }

    fun interactionRect(
        topLeft: Offset,
        size: Size
    ) {
        val drawElement: DrawElement.Rect = obtainDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        currentDrawElement = drawElement
        detectClickableInteraction(currentDrawElement, currentItem())
    }

    fun interactionArc(
        topLeft: Offset,
        size: Size,
        startAngle: Float,
        sweepAngle: Float,
        strokeWidth: Float = 0f
    ) {
        val drawElement: DrawElement.Arc = obtainDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        drawElement.startAngle = startAngle
        drawElement.sweepAngle = sweepAngle
        drawElement.strokeWidth = strokeWidth
        currentDrawElement = drawElement
        detectClickableInteraction(currentDrawElement, currentItem())
    }

    fun interactionCircle(
        center: Offset,
        radius: Float
    ) {
        val drawElement: DrawElement.Circle = obtainDrawElement()
        drawElement.center = center
        drawElement.radius = radius
        currentDrawElement = drawElement
        detectClickableInteraction(currentDrawElement, currentItem())
    }

    fun interactionRect(
        topLeft: Offset,
        size: Size,
        focusPoint: Offset = Offset.Unspecified
    ) {
        val drawElement: DrawElement.Rect = obtainDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        drawElement.focusPoint = focusPoint
        currentDrawElement = drawElement
        detectClickableInteraction(currentDrawElement, currentItem())
        detectChartInteraction()
    }

    private fun <T : DrawElement> T.copyInteractionDrawElement(): T {
        val newDrawElement: T = interactionDrawingElementCache.getCachedDrawElement(this::class.java)
        @Suppress("UNCHECKED_CAST")
        return newDrawElement.copy(this) as T
    }

    private fun detectChartInteraction() {
        if (!isHoveredOrPressed()) return
        val elementInteraction = chartContext.elementInteraction
        val chartGroupData = chartDataset.getChartGroupData(index)
        var oldElement: DrawElement = DrawElement.None
        if (elementInteraction is ChartElementInteraction.Element<*>) {
            oldElement = elementInteraction.drawElement
        }
        if (oldElement != currentDrawElement) {
            val newDrawElement = currentDrawElement.copyInteractionDrawElement()
            chartContext.chartInteractionHandler.tryEmit(
                ChartElementInteraction.Element(
                    drawElement = newDrawElement,
                    currentItem = currentItem(),
                    currentGroupItems = chartGroupData
                )
            )
        }
    }

    private fun isScrollInProgress(): Boolean {
        return chartContext.chartScrollState?.isScrollInProgress ?: false
    }

    private fun detectClickableInteraction(
        drawElement: DrawElement,
        currentItem: T
    ) {
        if (drawElement.isTap(chartContext)) {
            tapGestures.performTap(currentItem)
            chartContext.chartInteractionHandler.tryEmit(
                ChartTapInteraction.ExitTap
            )
        }
        if (drawElement.isDoubleTap(chartContext)) {
            tapGestures.performDoubleTap(currentItem)
            chartContext.chartInteractionHandler.tryEmit(
                ChartTapInteraction.ExitDoubleTap
            )
        }
        if (drawElement.isLongPressed(chartContext)) {
            tapGestures.performLongPress(currentItem)
            chartContext.chartInteractionHandler.tryEmit(
                ChartTapInteraction.ExitLongPress
            )
        }
    }
}