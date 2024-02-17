package me.jack.compose.chart.draw

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.CoroutineScope
import me.jack.compose.chart.component.TapGestures
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.draw.cache.DrawingKeyframeCache
import me.jack.compose.chart.interaction.ChartTapInteraction
import me.jack.compose.chart.interaction.DrawElementInteraction
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.ChartDatasetAccessScope
import me.jack.compose.chart.scope.ChartDatasetAccessScopeInstance
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.drawElementInteraction
import me.jack.compose.chart.scope.fastForEach
import me.jack.compose.chart.scope.fastForEachWithNext
import me.jack.compose.chart.scope.getChartGroupData

@Composable
fun <T> SingleChartScope<T>.ChartCanvas(
    modifier: Modifier = Modifier,
    onDraw: ChartDrawScope<T>.() -> Unit
) {
    val drawBlock by rememberUpdatedState(onDraw)
    var chartDrawScope: ChartDrawScope<T>? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    Spacer(
        modifier = modifier.drawBehind {
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
    modifier: Modifier = Modifier,
    onDraw: ChartDrawScope<T>.(current: T) -> Unit
) {
    var chartDrawScope: ChartDrawScope<T>? by remember {
        mutableStateOf(null)
    }
    val drawBlock by rememberUpdatedState {
        if (null != chartDrawScope) {
            fastForEach { currentItem ->
                onDraw(chartDrawScope!!, currentItem)
            }
        }
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
            drawBlock.invoke()
            currentChartDrawScope.isPreLayout = false
            currentChartDrawScope.updateCurrentActivatedDrawElement()
            currentChartDrawScope.moveToNextDrawElement()
            drawBlock.invoke()
        }
    )
}

@Composable
fun <T> SingleChartScope<T>.LazyChartCanvas(
    modifier: Modifier = Modifier,
    onDraw: ChartDrawScope<T>.(current: T, next: T) -> Unit
) {
    var chartDrawScope: ChartDrawScope<T>? by remember {
        mutableStateOf(null)
    }
    val drawBlock by rememberUpdatedState {
        if (null != chartDrawScope) {
            fastForEachWithNext { current, next ->
                onDraw(chartDrawScope!!, current, next)
            }
        }
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
            drawBlock.invoke()
            currentChartDrawScope.isPreLayout = false
            currentChartDrawScope.updateCurrentActivatedDrawElement()
            currentChartDrawScope.moveToNextDrawElement()
            drawBlock.invoke()
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
        DrawElement.Line::class.java -> DrawElement.Line()
        DrawElement.Path::class.java -> DrawElement.Path()
        DrawElement.RoundRect::class.java -> DrawElement.RoundRect()
        DrawElement.DrawElementGroup::class.java -> DrawElement.DrawElementGroup()
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
    private val interactionSource: MutableInteractionSource = singleChartScope.interactionSource
    private val drawElementInteraction: DrawElementInteraction
        get() = singleChartScope.drawElementInteraction

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

    inline fun clickableGroup(
        topLeft: Offset = currentLeftTopOffset,
        size: Size = childSize,
        onDraw: () -> Unit
    ) {
        if (isPreLayout()) {
            try {
                val drawElementGroup: DrawElement.DrawElementGroup = obtainDrawElement()
                drawElementGroup.topLeft = topLeft
                drawElementGroup.size = size
                drawElementGroup.isActivated = true
                drawElementGroup.children.resetPointer()
                addChildDrawElement(drawElementGroup)
                setCurrentDrawElementGroup(drawElementGroup)
                onDraw()
            } finally {
                setCurrentDrawElementGroup(null)
            }
        } else {
            onDraw()
            moveToNextDrawElement()
        }
    }

    fun interactionArc(
        topLeft: Offset,
        size: Size,
        startAngle: Float,
        sweepAngle: Float,
        strokeWidth: Float = 0f,
        focusPoint: Offset = Offset.Unspecified
    ) {
        val drawElement: DrawElement.Arc = obtainDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        drawElement.startAngle = startAngle
        drawElement.sweepAngle = sweepAngle
        drawElement.strokeWidth = strokeWidth
        drawElement.focusPoint = focusPoint
        detectClickableInteraction(drawElement, currentItem())
        detectChartInteraction(drawElement)
    }

    fun interactionCircle(
        center: Offset,
        radius: Float,
        focusPoint: Offset = Offset.Unspecified
    ) {
        val drawElement: DrawElement.Circle = obtainDrawElement()
        drawElement.center = center
        drawElement.radius = radius
        drawElement.focusPoint = focusPoint
        detectClickableInteraction(drawElement, currentItem())
        detectChartInteraction(drawElement)
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
        detectClickableInteraction(drawElement, currentItem())
        detectChartInteraction(drawElement)
    }

    fun interactionLine(
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        focusPoint: Offset = Offset.Unspecified
    ) {
        val drawElement: DrawElement.Line = obtainDrawElement()
        drawElement.start = start
        drawElement.end = end
        drawElement.strokeWidth = strokeWidth
        drawElement.focusPoint = focusPoint
        detectClickableInteraction(drawElement, currentItem())
        detectChartInteraction(drawElement)
    }

    fun interactionPath(
        bounds: Rect,
        strokeWidth: Float = 0f,
        focusPoint: Offset = Offset.Unspecified
    ) {
        val drawElement: DrawElement.Path = obtainDrawElement()
        drawElement.bounds = bounds
        drawElement.strokeWidth = strokeWidth
        drawElement.focusPoint = focusPoint
        detectClickableInteraction(drawElement, currentItem())
        detectChartInteraction(drawElement)
    }

    fun interactionOval(
        topLeft: Offset,
        size: Size,
        focusPoint: Offset = Offset.Unspecified
    ) {
        val drawElement: DrawElement.Oval = obtainDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        drawElement.focusPoint = focusPoint
        detectClickableInteraction(drawElement, currentItem())
        detectChartInteraction(drawElement)
    }

    fun interactionRoundRect(
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius = CornerRadius.Zero,
        focusPoint: Offset = Offset.Unspecified
    ) {
        val drawElement: DrawElement.RoundRect = obtainDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        drawElement.cornerRadius = cornerRadius
        drawElement.focusPoint = focusPoint
        detectClickableInteraction(drawElement, currentItem())
        detectChartInteraction(drawElement)
    }

    private fun <T : DrawElement> T.copyInteractionDrawElement(): T {
        val newDrawElement: T = interactionDrawingElementCache.getCachedDrawElement(this::class.java)
        @Suppress("UNCHECKED_CAST")
        return newDrawElement.copy(this) as T
    }

    private fun detectChartInteraction(drawElement: DrawElement) {
        if (!drawElement.isHoveredOrPressed()) return
        val elementInteraction = drawElementInteraction
        val chartGroupData = chartDataset.getChartGroupData(index)
        var oldElement: DrawElement = DrawElement.None
        if (elementInteraction is DrawElementInteraction.Element<*, *>) {
            oldElement = elementInteraction.drawElement
        }
        if (oldElement != drawElement) {
            val newDrawElement = drawElement.copyInteractionDrawElement()
            interactionSource.tryEmit(
                DrawElementInteraction.Element(
                    drawElement = newDrawElement,
                    currentItem = currentItem(),
                    currentGroupItems = chartGroupData
                )
            )
        }
    }

    private fun detectClickableInteraction(
        drawElement: DrawElement,
        currentItem: T
    ) {
        if (drawElement.isTap(interactionStates)) {
            val currentElement = currentDrawElement
            if (currentElement is DrawElement.DrawElementGroup) {
                val chartGroupData = chartDataset.getChartGroupData(index)
                tapGestures.performGroupTap(chartGroupData)
            } else {
                tapGestures.performTap(currentItem)
            }
            interactionSource.tryEmit(
                ChartTapInteraction.ExitTap
            )
        }
        if (drawElement.isDoubleTap(interactionStates)) {
            val currentElement = currentDrawElement
            if (currentElement is DrawElement.DrawElementGroup) {
                val chartGroupData = chartDataset.getChartGroupData(index)
                tapGestures.performGroupDoubleTap(chartGroupData)
            } else {
                tapGestures.performDoubleTap(currentItem)
            }
            interactionSource.tryEmit(
                ChartTapInteraction.ExitDoubleTap
            )
        }
        if (drawElement.isLongPressed(interactionStates)) {
            val currentElement = currentDrawElement
            if (currentElement is DrawElement.DrawElementGroup) {
                val chartGroupData = chartDataset.getChartGroupData(index)
                tapGestures.performGroupLongPress(chartGroupData)
            } else {
                tapGestures.performLongPress(currentItem)
            }
            interactionSource.tryEmit(
                ChartTapInteraction.ExitLongPress
            )
        }
    }
}