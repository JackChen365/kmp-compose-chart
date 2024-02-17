package me.jack.compose.chart.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.ChartScrollState
import me.jack.compose.chart.context.ChartScrollableState
import me.jack.compose.chart.context.ChartZoomState
import me.jack.compose.chart.context.MutableChartScrollState
import me.jack.compose.chart.context.chartScrollState
import me.jack.compose.chart.context.chartZoomState
import me.jack.compose.chart.context.isElementAvailable
import me.jack.compose.chart.context.rememberScrollDelegate
import me.jack.compose.chart.context.requireChartScrollState
import me.jack.compose.chart.context.requireChartZoomState
import me.jack.compose.chart.draw.rememberInteractionStates
import me.jack.compose.chart.interaction.ChartHoverInteraction
import me.jack.compose.chart.interaction.ChartTapInteraction
import me.jack.compose.chart.interaction.collectDrawElementAsState
import me.jack.compose.chart.measure.ChartContentMeasurePolicy
import me.jack.compose.chart.measure.withScrollMeasurePolicy
import me.jack.compose.chart.measure.withZoomableMeasurePolicy
import me.jack.compose.chart.scope.ChartAnchor
import me.jack.compose.chart.scope.ChartCombinedScope
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.ChartScope
import me.jack.compose.chart.scope.MutableChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.SingleChartScopeInstance
import me.jack.compose.chart.scope.alignContent
import me.jack.compose.chart.scope.anchor
import me.jack.compose.chart.scope.chartGroupOffsets
import me.jack.compose.chart.scope.chartParentData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val simpleChartContent: @Composable SingleChartScope<*>.() -> Unit = { ChartContent() }

@Composable
fun <T : Any> SingleChartLayout(
    modifier: Modifier,
    chartContext: ChartContext = ChartContext,
    tapGestures: TapGestures<T> = rememberCombinedTapGestures(),
    contentMeasurePolicy: ChartContentMeasurePolicy,
    chartDataset: ChartDataset<T>,
    scrollableState: ChartScrollableState? = null,
    content: @Composable (SingleChartScope<T>.() -> Unit) = simpleChartContent,
    chartContent: @Composable (SingleChartScope<T>.() -> Unit)
) {
    val contentBlock by rememberUpdatedState(content)
    val chartContentBlock by rememberUpdatedState(chartContent)
    @Suppress("DEPRECATION")
    MultiMeasureLayout(
        modifier = modifier,
        content = {
            SingleChartContent(
                modifier = Modifier,
                chartContext = chartContext,
                tapGestures = tapGestures,
                contentMeasurePolicy = contentMeasurePolicy,
                chartDataset = chartDataset,
                scrollableState = scrollableState,
                content = contentBlock,
                chartContent = chartContentBlock
            )
        },
        measurePolicy = { measurables, constraints ->
            measureContent(
                measurables = measurables,
                constraints = constraints
            )
        }
    )
}

@Composable
fun CombinedChartLayout(
    modifier: Modifier,
    chartContext: ChartContext = ChartContext,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    chartComponents: List<ChartComponent<Any>>,
    content: @Composable ChartCombinedScope.() -> Unit
) {
    @Suppress("DEPRECATION")
    MultiMeasureLayout(
        modifier = modifier,
        content = {
            CombinedChartContent(
                modifier = Modifier,
                contentMeasurePolicy = contentMeasurePolicy,
                chartContext = chartContext,
                chartComponents = chartComponents,
                content = content
            )
        },
        measurePolicy = { measurables, constraints ->
            measureContent(
                measurables = measurables,
                constraints = constraints
            )
        }
    )
}

@Composable
private fun <T> SingleChartContent(
    modifier: Modifier,
    chartContext: ChartContext,
    tapGestures: TapGestures<T>,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    chartDataset: ChartDataset<T>,
    scrollableState: ChartScrollableState? = null,
    content: @Composable (SingleChartScope<T>.() -> Unit)? = null,
    chartContent: @Composable (SingleChartScope<T>.() -> Unit)
) {
    val rememberContentMeasurePolicy = remember {
        var wrappedContentMeasurePolicy = contentMeasurePolicy
        if (chartContext.isElementAvailable(ChartZoomState)) {
            wrappedContentMeasurePolicy = wrappedContentMeasurePolicy.withZoomableMeasurePolicy {
                chartContext.requireChartZoomState.zoom
            }
        }
        if (chartContext.isElementAvailable(ChartScrollState)) {
            wrappedContentMeasurePolicy = wrappedContentMeasurePolicy.withScrollMeasurePolicy {
                chartContext.requireChartScrollState.offset
            }
        }
        wrappedContentMeasurePolicy
    }
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val interactionStates =
        rememberInteractionStates(interactionSource, interactionSource.collectDrawElementAsState())
    val chartScopeInstance = remember(chartDataset) {
        SingleChartScopeInstance(
            chartDataset = chartDataset,
            chartContext = chartContext,
            tapGestures = tapGestures,
            contentMeasurePolicy = rememberContentMeasurePolicy,
            interactionSource = interactionSource,
            interactionStates = interactionStates
        )
    }
    chartScopeInstance.chartContent = {
        ChartBox(
            modifier = modifier,
            scrollableState = scrollableState
        ) { chartContent() }
    }
    content?.invoke(chartScopeInstance)
}

@Composable
fun <T> SingleChartScope<T>.ChartBox(
    modifier: Modifier = Modifier,
    chartContext: ChartContext = this.chartContext,
    scrollableState: ChartScrollableState? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .chartInteraction(interactionSource)
            .chartZoom(chartContext) { _, zoom ->
                val currentChartScrollState = chartScrollState
                if (null != currentChartScrollState) {
                    val newOffset = currentChartScrollState.offset * zoom
                    val minOffset = contentSize.times(zoom).mainAxis -
                            contentRange.times(zoom).mainAxis
                    currentChartScrollState.offset = newOffset.coerceIn(minOffset, 0f)
                }
            }
            .chartScrollable(
                chartScope = this,
                contentMeasurePolicy = contentMeasurePolicy,
                scrollableState = scrollableState,
                datasetSize = chartDataset.size
            ),
        content = content
    )
}

fun Modifier.chartInteraction(
    interactionSource: MutableInteractionSource
) = pointerInput(Unit) {
    detectTapGestures(
        onPress = { offset ->
            val press = PressInteraction.Press(offset)
            interactionSource.emit(press)
            if (tryAwaitRelease()) {
                interactionSource.emit(PressInteraction.Release(press))
            } else {
                interactionSource.emit(PressInteraction.Cancel(press))
            }
        },
        onDoubleTap = { offset ->
            interactionSource.tryEmit(ChartTapInteraction.DoubleTap(offset))
        },
        onTap = { offset ->
            interactionSource.tryEmit(ChartTapInteraction.Tap(offset))
        },
        onLongPress = { offset ->
            interactionSource.tryEmit(ChartTapInteraction.LongPress(offset))
        }
    )
}.onHoverPointerEvent { event ->
    val last = event.changes.lastOrNull()
    if (null != last) {
        when (event.type) {
            PointerEventType.Enter ->
                interactionSource.tryEmit(ChartHoverInteraction.Enter(last.position))

            PointerEventType.Exit ->
                interactionSource.tryEmit(ChartHoverInteraction.Exit(last.position))

            PointerEventType.Move -> {
                interactionSource.tryEmit(ChartHoverInteraction.Move(last.position))
            }
        }
    }
}

@Composable
private fun CombinedChartContent(
    modifier: Modifier,
    chartContext: ChartContext,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    chartComponents: List<ChartComponent<Any>>,
    content: @Composable (ChartCombinedScope.() -> Unit)? = null
) {
    var childItemCount = 0
    var groupCount = 1
    chartComponents.forEach { component ->
        childItemCount = childItemCount.coerceAtLeast(component.chartDataset.size)
        groupCount = groupCount.coerceAtLeast(component.chartDataset.groupSize)
    }
    val rememberContentMeasurePolicy = remember {
        var wrappedContentMeasurePolicy = contentMeasurePolicy
        if (chartContext.isElementAvailable(ChartZoomState)) {
            wrappedContentMeasurePolicy = wrappedContentMeasurePolicy.withZoomableMeasurePolicy {
                chartContext.requireChartZoomState.zoom
            }
        }
        if (chartContext.isElementAvailable(ChartScrollState)) {
            wrappedContentMeasurePolicy = wrappedContentMeasurePolicy.withScrollMeasurePolicy {
                chartContext.requireChartScrollState.offset
            }
        }
        wrappedContentMeasurePolicy
    }
    val interactionSource = remember { MutableInteractionSource() }
    val interactionStates = rememberInteractionStates(
        interactionSource, interactionSource.collectDrawElementAsState()
    )
    val chartComponentScopes = remember(chartComponents) {
        val componentScopes = mutableMapOf<ChartComponent<Any>, SingleChartScopeInstance<Any>>()
        chartComponents.forEach { chartComponent ->
            val singleChartScopeInstance = SingleChartScopeInstance(
                chartDataset = chartComponent.chartDataset,
                chartContext = chartContext,
                tapGestures = chartComponent.tapGestures,
                contentMeasurePolicy = rememberContentMeasurePolicy,
                interactionSource = interactionSource,
                interactionStates = interactionStates
            )
            componentScopes[chartComponent] = singleChartScopeInstance
        }
        componentScopes
    }
    val chartCombinedScope = remember(chartComponents) {
        ChartCombinedScope(
            childItemCount = childItemCount,
            chartContext = chartContext,
            groupCount = groupCount,
            chartScopes = chartComponentScopes.values.toList(),
            contentMeasurePolicy = rememberContentMeasurePolicy,
            interactionSource = interactionSource,
            interactionStates = interactionStates
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .chartInteraction(interactionSource)
            .chartZoom(chartContext) { _, zoom ->
                with(chartCombinedScope) {
                    val currentChartScrollState = chartScrollState
                    if (null != currentChartScrollState) {
                        val newOffset = currentChartScrollState.offset * zoom
                        val minOffset = contentSize.times(zoom).mainAxis - contentRange.times(zoom).mainAxis
                        currentChartScrollState.offset = newOffset.coerceIn(minOffset, 0f)
                    }
                }
            }
            .chartScrollable(
                chartScope = chartCombinedScope,
                contentMeasurePolicy = contentMeasurePolicy,
                datasetSize = childItemCount
            )
    ) {
        chartComponentScopes.forEach { (chartComponent, singleChartScope) ->
            singleChartScope.contentSize = chartCombinedScope.contentSize
            singleChartScope.contentRange = chartCombinedScope.contentRange
            chartComponent.content.invoke(singleChartScope)
        }
    }
    content?.invoke(chartCombinedScope)
}

/**
 * https://developer.android.com/jetpack/compose/custom-modifiers
 */
@Composable
private fun Modifier.chartScrollable(
    chartScope: ChartScope,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    scrollableState: ChartScrollableState? = null,
    datasetSize: Int
): Modifier {
    val chartScrollState = chartScope.chartContext.chartScrollState as? MutableChartScrollState
        ?: return this.onSizeChanged { size ->
            if (chartScope is MutableChartScope) {
                chartScope.contentSize = size.toSize()
                chartScope.contentRange = size.toSize()
            }
        }
    chartScrollState.density = LocalDensity.current
    val maxOffset = 0f
    var minOffset by remember {
        mutableFloatStateOf(-1f)
    }
    var contentSize by remember {
        mutableStateOf(IntSize.Zero)
    }
    val rememberedScrollableState = scrollableState ?: me.jack.compose.chart.context.rememberScrollableState()
    rememberedScrollableState.chartScrollDelegate = rememberScrollDelegate(
        chartScrollState = chartScrollState,
        targetItemOffset = { groupIndex, index ->
            with(chartScope) {
                contentMeasurePolicy.childLeftTop(groupCount, groupIndex, index).mainAxis
            }
        }
    ) { delta ->
        val resultingOffset = chartScrollState.offset + delta
        val consume = if (resultingOffset > maxOffset) {
            maxOffset - chartScrollState.offset
        } else if (resultingOffset < minOffset) {
            minOffset - chartScrollState.offset
        } else {
            delta
        }
        chartScrollState.offset += consume
        consume
    }
    with(chartScope) {
        if (IntSize.Zero != contentSize) {
            contentMeasurePolicy.contentSize = contentSize
            val contentRange = with(contentMeasurePolicy) { measureContent(size = contentSize) }
            if (chartScope is MutableChartScope) {
                chartScope.contentSize = contentSize.toSize()
                chartScope.contentRange = contentRange
            }
            minOffset = (contentSize.mainAxis - contentRange.mainAxis).coerceAtMost(0f)
            // when remove the last visible item and the current offset + content size less than scroll range.
            // we are supposed to adjust the current offset.
            if (contentSize.mainAxis - chartScrollState.offset > contentRange.mainAxis) {
                chartScrollState.offset = if (contentSize.mainAxis < contentRange.mainAxis) {
                    contentSize.mainAxis - contentRange.mainAxis
                } else {
                    0f
                }
            }
            // Use this chartScrollState.offset(MutableState) here to associate the current scope with this state
            val position = (-chartScrollState.offset / chartGroupOffsets).toInt()
            val positionOffset = -chartScrollState.offset % chartGroupOffsets
            updateScrollState(
                chartScrollState = chartScrollState,
                contentSize = contentSize,
                datasetSize = datasetSize,
                position = position,
                positionOffset = positionOffset,
                isScrollInProgress = rememberedScrollableState.isScrollInProgress
            )
        }
    }
    val coroutineScope = rememberCoroutineScope()
    // Used to calculate a settling position of a fling animation.
    return onSizeChanged { size ->
        contentSize = size
    }.scrollable(
        orientation = chartScrollState.orientation,
        state = rememberedScrollableState
    ).draggable(
        // support finger drag and mouse drag.
        orientation = chartScrollState.orientation,
        onDragStopped = { velocity ->
            coroutineScope.launch {
                rememberedScrollableState.performFling(velocity)
            }
        },
        state = rememberDraggableState { delta ->
            coroutineScope.launch {
                rememberedScrollableState.scrollBy(delta)
            }
        }
    ).mouseScrollFilter { scrollDelta, _ ->
        coroutineScope.launch {
            // the move distance is too slow, times 10x seems the normal speed.
            rememberedScrollableState.scrollBy(-scrollDelta.y * 10f)
        }
        true
    }
}

fun Modifier.mouseScrollFilter(
    onMouseScroll: (scrollDelta: Offset, size: IntSize) -> Boolean
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Scroll) {
                val scrollDelta = event.changes.first().scrollDelta
                val bounds = this.size
                if (onMouseScroll(scrollDelta, bounds)) {
                    event.changes.first().consume()
                }
            }
        }
    }
}

private fun ChartScope.updateScrollState(
    chartScrollState: MutableChartScrollState,
    contentSize: IntSize,
    datasetSize: Int,
    position: Int,
    positionOffset: Float,
    isScrollInProgress: Boolean
) {
    chartScrollState.itemCount = datasetSize
    chartScrollState.firstVisibleItemIndex = position
    chartScrollState.firstVisibleItemOffset = positionOffset
    chartScrollState.isScrollInProgress = isScrollInProgress
    chartScrollState.lastVisibleItemIndex = calculateLastVisibleItem(
        itemCount = datasetSize,
        childMainAxis = chartGroupOffsets,
        firstVisibleItem = position,
        firstVisibleItemOffset = positionOffset,
        size = contentSize
    )
}

private fun Modifier.chartZoom(
    context: ChartContext,
    onZoom: ChartContext.(centroid: Offset, zoom: Float) -> Unit
): Modifier {
    val chartZoomState = context.chartZoomState ?: return this
    return pointerInput(Unit) {
        detectZoomGestures { centroid, zoom ->
            val oldZoomValue = chartZoomState.zoom
            chartZoomState.zoom *= zoom
            // Update the offset to implement panning when zoomed.
            if (oldZoomValue != chartZoomState.zoom) {
                with(context) {
                    onZoom(centroid, chartZoomState.zoom / oldZoomValue)
                }
            }
        }
    }
}

internal suspend fun PointerInputScope.detectZoomGestures(
    onZoom: (centroid: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    if (zoomMotion > touchSlop) pastTouchSlop = true
                }
                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f) onZoom(centroid, zoomChange)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}


@Composable
fun <T> rememberCombinedTapGestures(
    onTap: (currentItem: T) -> Unit = {},
    onDoubleTap: (currentItem: T) -> Unit = { },
    onLongPress: (currentItem: T) -> Unit = { },
    onGroupTap: (currentItem: List<T>) -> Unit = { },
    onGroupDoubleTap: (currentItem: List<T>) -> Unit = { },
    onGroupLongPress: (currentItem: List<T>) -> Unit = { },
): TapGestures<T> {
    val onTapBlock by rememberUpdatedState(onTap)
    val onDoubleTapBlock by rememberUpdatedState(onDoubleTap)
    val onLongPressBlock by rememberUpdatedState(onLongPress)
    val onGroupTapBlock by rememberUpdatedState(onGroupTap)
    val onGroupDoubleTapBlock by rememberUpdatedState(onGroupDoubleTap)
    val onGroupLongPressBlock by rememberUpdatedState(onGroupLongPress)
    return remember {
        TapGestures<T>()
            .onTap(onTapBlock)
            .onDoubleTap(onDoubleTapBlock)
            .onLongPress(onLongPressBlock)
            .onGroupTap(onGroupTapBlock)
            .onGroupDoubleTap(onGroupDoubleTapBlock)
            .onGroupLongPress(onGroupLongPressBlock)
    }
}

@Composable
fun <T> rememberOnTap(
    onTap: (currentItem: T) -> Unit = {},
): TapGestures<T> {
    val onTapBlock by rememberUpdatedState(onTap)
    return remember {
        TapGestures<T>().onTap(onTapBlock)
    }
}

@Composable
fun <T> rememberOnDoubleTap(
    onDoubleTap: (currentItem: T) -> Unit = {},
): TapGestures<T> {
    val onDoubleTapBlock by rememberUpdatedState(onDoubleTap)
    return remember {
        TapGestures<T>().onDoubleTap(onDoubleTapBlock)
    }
}

@Composable
fun <T> rememberOnLongPress(
    onLongPress: (currentItem: T) -> Unit = { }
): TapGestures<T> {
    val onLongPressBlock by rememberUpdatedState(onLongPress)
    return remember {
        TapGestures<T>().onLongPress(onLongPressBlock)
    }
}

@Composable
fun <T> rememberOnGroupTap(
    onGroupTap: (currentItem: List<T>) -> Unit = { }
): TapGestures<T> {
    val onGroupTapBlock by rememberUpdatedState(onGroupTap)
    return remember {
        TapGestures<T>().onGroupTap(onGroupTapBlock)
    }
}

@Composable
fun <T> rememberOnGroupDoubleTap(
    onGroupLongPress: (currentItem: List<T>) -> Unit = { }
): TapGestures<T> {
    val onGroupLongPressBlock by rememberUpdatedState(onGroupLongPress)
    return remember {
        TapGestures<T>().onGroupDoubleTap(onGroupLongPressBlock)
    }
}

@Composable
fun <T> rememberOnGroupLongPress(
    onGroupLongPress: (currentItem: List<T>) -> Unit = { }
): TapGestures<T> {
    val onGroupLongPressBlock by rememberUpdatedState(onGroupLongPress)
    return remember {
        TapGestures<T>().onGroupLongPress(onGroupLongPressBlock)
    }
}

@Suppress("unused")
class TapGestures<T> {
    private var onTap: (currentItem: T) -> Unit = { }
    private var onGroupTap: (currentItems: List<T>) -> Unit = { }
    private var onDoubleTap: (currentItem: T) -> Unit = { }
    private var onGroupDoubleTap: (currentItems: List<T>) -> Unit = { }
    private var onLongPress: (currentItem: T) -> Unit = { }
    private var onGroupLongPress: (currentItems: List<T>) -> Unit = { }

    fun onTap(onTap: (currentItem: T) -> Unit): TapGestures<T> {
        this.onTap = onTap
        return this
    }

    fun onDoubleTap(onDoubleTap: (currentItem: T) -> Unit): TapGestures<T> {
        this.onDoubleTap = onDoubleTap
        return this
    }

    fun onLongPress(onLongPress: (currentItem: T) -> Unit): TapGestures<T> {
        this.onLongPress = onLongPress
        return this
    }

    fun onGroupTap(onGroupTap: (currentItem: List<T>) -> Unit): TapGestures<T> {
        this.onGroupTap = onGroupTap
        return this
    }

    fun onGroupDoubleTap(onGroupDoubleTap: (currentItem: List<T>) -> Unit): TapGestures<T> {
        this.onGroupDoubleTap = onGroupDoubleTap
        return this
    }

    fun onGroupLongPress(onGroupLongPress: (currentItem: List<T>) -> Unit): TapGestures<T> {
        this.onGroupLongPress = onGroupLongPress
        return this
    }

    fun performTap(currentItem: T) {
        this.onTap.invoke(currentItem)
    }

    fun performGroupTap(currentItems: List<T>) {
        this.onGroupTap.invoke(currentItems)
    }

    fun performDoubleTap(currentItem: T) {
        this.onDoubleTap.invoke(currentItem)
    }

    fun performGroupDoubleTap(currentItems: List<T>) {
        this.onGroupDoubleTap.invoke(currentItems)
    }

    fun performLongPress(currentItem: T) {
        this.onLongPress.invoke(currentItem)
    }

    fun performGroupLongPress(currentItems: List<T>) {
        this.onGroupLongPress.invoke(currentItems)
    }
}

fun Modifier.onHoverPointerEvent(
    pass: PointerEventPass = PointerEventPass.Main,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
): Modifier = composed {
    val currentOnEvent by rememberUpdatedState(onEvent)
    pointerInput(pass) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass)
                if (event.type == PointerEventType.Enter ||
                    event.type == PointerEventType.Exit ||
                    event.type == PointerEventType.Scroll ||
                    event.type == PointerEventType.Move
                ) {
                    currentOnEvent(event)
                }
            }
        }
    }
}

fun ChartScope.calculateLastVisibleItem(
    itemCount: Int,
    childMainAxis: Float,
    firstVisibleItem: Int,
    firstVisibleItemOffset: Float,
    size: IntSize
): Int {
    var index = firstVisibleItem
    var offset = -firstVisibleItemOffset
    val mainAxis = size.mainAxis
    while (index < itemCount && offset < mainAxis) {
        offset += childMainAxis
        index++
    }
    return min(itemCount, index + 1)
}

private fun MeasureScope.measureContent(
    measurables: List<Measurable>,
    constraints: Constraints
): MeasureResult {
    val contentRect = measureContentRectWithChartAnchor(
        constraints = constraints,
        measurables = measurables
    )
    val measureResult = measureContentWithChartAnchor(
        constraints = constraints,
        contentRect = contentRect,
        measurables = measurables
    )
    return layout(constraints.maxWidth, constraints.maxHeight) {
        val centerRect = measureResult.centerRect
        measureResult.placeables.forEach { placeable ->
            when (placeable.anchor) {
                ChartAnchor.Start -> placeable.placeRelative(0, if (placeable.alignContent) centerRect.top else 0)

                ChartAnchor.Top -> placeable.placeRelative(if (placeable.alignContent) centerRect.left else 0, 0)

                ChartAnchor.End -> placeable.placeRelative(
                    centerRect.right, if (placeable.alignContent) centerRect.top else 0
                )

                ChartAnchor.Bottom -> placeable.placeRelative(
                    if (placeable.alignContent) centerRect.left else 0, centerRect.bottom
                )

                ChartAnchor.Center -> placeable.placeRelative(centerRect.left, centerRect.top)
            }
        }
    }
}

/**
 * Measure the content rect without measure the chart content.
 * This is to determine the content rect and than measure and layout all the nodes with a correct position.
 */
fun measureContentRectWithChartAnchor(
    constraints: Constraints,
    measurables: List<Measurable>
): IntRect {
    var maxTopMainAxis = 0
    var maxBottomMainAxis = 0
    var maxStartCrossAxis = 0
    var maxEndCrossAxis = 0
    measurables.forEach { measurable ->
        when (measurable.chartParentData.anchor) {
            ChartAnchor.Top -> {
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0
                    )
                )
                maxTopMainAxis = max(maxTopMainAxis, placeable.height)
            }

            ChartAnchor.Bottom -> {
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0
                    )
                )
                maxBottomMainAxis = max(maxBottomMainAxis, placeable.height)
            }

            ChartAnchor.Start -> {
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxHeight = constraints.maxHeight - maxTopMainAxis - maxBottomMainAxis
                    )
                )
                maxStartCrossAxis = max(maxStartCrossAxis, placeable.width)
            }

            ChartAnchor.End -> {
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxHeight = constraints.maxHeight - maxTopMainAxis - maxBottomMainAxis
                    )
                )
                maxEndCrossAxis = max(maxEndCrossAxis, placeable.width)
            }

            else -> Unit
        }
    }
    return IntRect(
        maxStartCrossAxis,
        maxTopMainAxis,
        constraints.maxWidth - maxEndCrossAxis,
        constraints.maxHeight - maxBottomMainAxis
    )
}


/**
 * Measure the layout like Java swing BorderLayout.
 * <img src="https://media.geeksforgeeks.org/wp-content/uploads/Border-1.png" />
 */
fun measureContentWithChartAnchor(
    constraints: Constraints,
    contentRect: IntRect,
    measurables: List<Measurable>
): ChartMeasureResult {
    val placeables = measurables.map { measurable ->
        val alignContent = measurable.chartParentData?.alignContent ?: false
        val flexibleWidth = if (alignContent) contentRect.width else constraints.maxWidth
        val flexibleHeight = if (alignContent) contentRect.height else constraints.maxHeight
        when (measurable.chartParentData.anchor) {
            ChartAnchor.Top -> {
                measurable.measure(
                    constraints.copy(
                        minWidth = flexibleWidth,
                        maxWidth = flexibleWidth,
                        minHeight = 0
                    )
                )
            }

            ChartAnchor.Start -> {
                measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxHeight = flexibleHeight
                    )
                )
            }

            ChartAnchor.Bottom -> {
                measurable.measure(
                    constraints.copy(
                        minWidth = flexibleWidth,
                        maxWidth = flexibleWidth,
                        minHeight = 0
                    )
                )
            }

            ChartAnchor.End -> {
                measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxHeight = flexibleHeight
                    )
                )
            }

            else -> {
                measurable.measure(
                    constraints.copy(
                        minWidth = contentRect.width,
                        maxWidth = contentRect.width,
                        minHeight = contentRect.height,
                        maxHeight = contentRect.height
                    )
                )
            }
        }
    }
    return ChartMeasureResult(
        centerRect = contentRect,
        placeables = placeables
    )
}

class ChartMeasureResult(
    val centerRect: IntRect,
    val placeables: List<Placeable>
)

@Composable
fun Dp.toPx() = with(LocalDensity.current) { this@toPx.toPx() }

@Composable
fun Float.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

@Composable
fun Int.toDp() = with(LocalDensity.current) { this@toDp.toDp() }