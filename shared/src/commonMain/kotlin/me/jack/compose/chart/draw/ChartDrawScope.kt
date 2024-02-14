package me.jack.compose.chart.draw

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import me.jack.compose.chart.animation.ChartColorAnimateState
import me.jack.compose.chart.animation.ChartDpAnimateState
import me.jack.compose.chart.animation.ChartDpOffsetAnimateState
import me.jack.compose.chart.animation.ChartFloatAnimateState
import me.jack.compose.chart.animation.ChartIntAnimateState
import me.jack.compose.chart.animation.ChartOffsetAnimateState
import me.jack.compose.chart.animation.ChartSizeAnimateState
import me.jack.compose.chart.animation.ChartSpAnimateState
import me.jack.compose.chart.animation.colorAnimateState
import me.jack.compose.chart.animation.dpAnimateState
import me.jack.compose.chart.animation.dpOffsetAnimateState
import me.jack.compose.chart.animation.floatAnimateState
import me.jack.compose.chart.animation.intAnimateState
import me.jack.compose.chart.animation.offsetAnimateState
import me.jack.compose.chart.animation.sizeAnimateState
import me.jack.compose.chart.animation.spAnimateState
import me.jack.compose.chart.component.TapGestures
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.chartInteractionHandler
import me.jack.compose.chart.context.chartScrollState
import me.jack.compose.chart.context.pressLocation
import me.jack.compose.chart.context.pressState
import me.jack.compose.chart.context.tryEmit
import me.jack.compose.chart.draw.cache.DrawingKeyframeCache
import me.jack.compose.chart.draw.cache.getCachedValueOrPut
import me.jack.compose.chart.draw.interaction.elementInteraction
import me.jack.compose.chart.draw.interaction.hoverLocation
import me.jack.compose.chart.draw.interaction.hoverState
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
    onDraw: ChartDrawScope<T>.() -> Unit
) {
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
                        tapGestures = tapGestures
                    )
                }
                val currentChartDrawScope = checkNotNull(chartDrawScope)
                currentChartDrawScope.reset()
                drawBlock.invoke(currentChartDrawScope)
            }
    )
}

@Composable
fun <T> SingleChartScope<T>.LazyChartCanvas(
    modifier: Modifier = Modifier.padding(),
    onDraw: ChartDrawScope<T>.(current: T) -> Unit
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
                    singleChartScope = this@LazyChartCanvas,
                    drawScope = this,
                    scope = scope,
                    tapGestures = tapGestures
                )
            }
            val currentChartDrawScope = checkNotNull(chartDrawScope)
            currentChartDrawScope.reset()
            fastForEach { currentItem ->
                drawBlock(currentChartDrawScope, currentItem)
            }
        }
    )
}

@Composable
fun <T> SingleChartScope<T>.LazyChartCanvas(
    modifier: Modifier = Modifier,
    onDraw: ChartDrawScope<T>.(current: T, next: T) -> Unit
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
                    singleChartScope = this@LazyChartCanvas,
                    drawScope = this,
                    scope = scope,
                    tapGestures = tapGestures
                )
            }
            val currentChartDrawScope = checkNotNull(chartDrawScope)
            currentChartDrawScope.reset()
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

/**
 * This reified type can not handle cases like:
 *
 * ```
 * val currentDrawElement: DrawElement = DrawElement.Rect()
 *
 * private inline fun <reified T : DrawElement> T.copyDrawElement(): T {
 *     val newDrawElement: T = drawingElementCache.getCachedDrawElement()
 *     return newDrawElement.copy(this) as T
 * }
 *
 * currentDrawElement.copyElement() error!! It trying to use `DrawElement` as Key
 * ```
 * It always return DrawElement, so if you got case like this, you are supposed to use [getCachedDrawElement] with specific key.
 */
internal inline fun <reified T : DrawElement> DrawingKeyframeCache.getCachedDrawElement(
    noinline onHitCache: (T) -> Unit = { },
    noinline defaultValue: (key: Class<T>) -> T = ::defaultDrawingElementFactory
): T {
    return getCachedValueOrPut(
        key = T::class.java,
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
    val singleChartScope: SingleChartScope<T>,
    private val drawScope: DrawScope,
    private val scope: CoroutineScope,
    private val tapGestures: TapGestures<T>
) : DrawScope by drawScope, ChartDatasetAccessScope by ChartDatasetAccessScopeInstance {
    // Factory for AnimateState
    private val defaultIntAnimateStateFactory: (Class<*>) -> ChartIntAnimateState =
        { intAnimateState(scope, 0) }
    private val defaultFloatAnimateStateFactory: (Class<*>) -> ChartFloatAnimateState =
        { floatAnimateState(scope, 0f) }
    private val defaultColorAnimateStateFactory: (Class<*>) -> ChartColorAnimateState =
        { colorAnimateState(scope, Color.Transparent) }
    private val defaultSizeAnimateStateFactory: (Class<*>) -> ChartSizeAnimateState =
        { sizeAnimateState(scope, Size.Zero) }
    private val defaultOffsetAnimateStateFactory: (Class<*>) -> ChartOffsetAnimateState =
        { offsetAnimateState(scope, Offset.Zero) }
    private val defaultDpAnimateStateFactory: (Class<*>) -> ChartDpAnimateState =
        { dpAnimateState(scope, 0.dp) }
    private val defaultDpOffsetAnimateStateFactory: (Class<*>) -> ChartDpOffsetAnimateState =
        { dpOffsetAnimateState(scope, DpOffset.Zero) }
    private val defaultSpAnimateStateFactory: (Class<*>) -> ChartSpAnimateState =
        { spAnimateState(scope, 0.sp) }
    val chartContext: ChartContext = singleChartScope.chartContext
    val chartDataset: ChartDataset<T> = singleChartScope.chartDataset

    /**
     * Animatable state drawing cache pool.
     */
    private val drawingStateKeyframeCache = DrawingKeyframeCache()

    /**
     * Current drawing element cache pool.
     */
    private val drawingElementCache = DrawingKeyframeCache()

    /**
     * Interaction drawing element cache pool, we can not mix this pool with screen drawing element.
     * The [drawingElementCache] will be reused per frame but only for interaction, we are supposed to keep it until user change his behavior
     */
    private val interactionDrawingElementCache = DrawingKeyframeCache()
    private var currentDrawElement: DrawElement = DrawElement.None
    private var activatedDrawElement: DrawElement = DrawElement.None
    private val chartTraceableDrawScope = ChartTraceableDrawScope(this, drawingElementCache)

    fun reset() {
        activatedDrawElement = DrawElement.None
        drawingStateKeyframeCache.resetChildIds()
        drawingElementCache.resetChildIds()
    }

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

    fun animatableRect(
        topLeft: Offset,
        size: Size
    ) {
        val drawElement: DrawElement.Rect = obtainDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        currentDrawElement = drawElement
        updateActiveDrawElementIfNeeded(currentDrawElement)
        detectClickableInteraction(currentDrawElement, currentItem())
    }

    fun animatableArc(
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
        updateActiveDrawElementIfNeeded(currentDrawElement)
        detectClickableInteraction(currentDrawElement, currentItem())
    }

    fun animatableCircle(
        center: Offset,
        radius: Float
    ) {
        val drawElement: DrawElement.Circle = obtainDrawElement()
        drawElement.center = center
        drawElement.radius = radius
        currentDrawElement = drawElement
        updateActiveDrawElementIfNeeded(currentDrawElement)
        detectClickableInteraction(currentDrawElement, currentItem())
    }

    fun animatableRectRectWithInteraction(
        topLeft: Offset,
        size: Size,
        focusPoint: Offset = Offset.Unspecified
    ) {
        val drawElement: DrawElement.Rect = obtainDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        drawElement.focusPoint = focusPoint
        currentDrawElement = drawElement
        updateActiveDrawElementIfNeeded(currentDrawElement)
        detectClickableInteraction(currentDrawElement, currentItem())
        detectChartInteraction()
    }

    fun animatable(
        block: ChartTraceableDrawScope<T>.() -> Unit
    ) {
        chartTraceableDrawScope.trackChartData(currentItem(), index)
        // invoke twice, first we update the draw element, and then the second time we draw the element.
        block.invoke(chartTraceableDrawScope)
        currentDrawElement = chartTraceableDrawScope.currentDrawElement
        updateActiveDrawElementIfNeeded(currentDrawElement)
        detectClickableInteraction(currentDrawElement, currentItem())
        block.invoke(chartTraceableDrawScope)
    }

    fun <T> ChartDrawScope<T>.animatableWithInteraction(
        block: ChartTraceableDrawScope<T>.() -> Unit
    ) {
        animatable(block)
        detectChartInteraction()
    }

    private fun updateActiveDrawElementIfNeeded(currentDrawElement: DrawElement) {
        if (activatedDrawElement.isNone() && (isHoveredState() || isPressedState())) {
            activatedDrawElement = currentDrawElement
        }
    }

    private inline fun <reified T : DrawElement> obtainDrawElement(): T {
        return drawingElementCache.getCachedDrawElement()
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

    infix fun Color.whenPressed(targetValue: Color): Color {
        return valueIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    private fun Color.valueIf(
        targetValue: Color, condition: () -> Boolean
    ): Color {
        return if (condition()) targetValue else this
    }

    private fun Int.valueIf(
        targetValue: Int, condition: () -> Boolean
    ): Int {
        return if (condition()) targetValue else this
    }

    private fun Float.valueIf(
        targetValue: Float, condition: () -> Boolean
    ): Float {
        return if (condition()) targetValue else this
    }

    private fun Offset.valueIf(
        targetValue: Offset, condition: () -> Boolean
    ): Offset {
        return if (condition()) targetValue else this
    }

    private fun Size.valueIf(
        targetValue: Size, condition: () -> Boolean
    ): Size {
        return if (condition()) targetValue else this
    }

    infix fun Int.whenPressedAnimateTo(targetValue: Int): Int {
        return animateToIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    infix fun Float.whenPressedAnimateTo(targetValue: Float): Float {
        return animateToIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    infix fun Color.whenPressedAnimateTo(targetValue: Color): Color {
        return animateToIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    infix fun Offset.whenPressedAnimateTo(targetValue: Offset): Offset {
        return animateToIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    infix fun Size.whenPressedAnimateTo(targetValue: Size): Size {
        return animateToIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    infix fun Dp.whenPressedAnimateTo(targetValue: Dp): Dp {
        return animateToIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    infix fun DpOffset.whenPressedAnimateTo(targetValue: DpOffset): DpOffset {
        return animateToIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    /**
     * The text unit support em/sp, but for this method, we only support sp.
     */
    infix fun TextUnit.whenPressedAnimateTo(targetValue: TextUnit): TextUnit {
        return animateToIf(targetValue = targetValue, condition = ::isHoveredOrPressed)
    }

    private fun isScrollInProgress(): Boolean {
        return chartContext.chartScrollState?.isScrollInProgress ?: false
    }

    private fun isPressed(): Boolean {
        return activatedDrawElement == currentDrawElement && !isScrollInProgress() && isPressedState()
    }

    private fun isPressedState(): Boolean {
        return chartContext.pressState.value &&
                chartContext.pressLocation in currentDrawElement
    }

    private fun isHovered(): Boolean {
        return activatedDrawElement == currentDrawElement && !isScrollInProgress() && isHoveredState()
    }

    private fun isHoveredState(): Boolean {
        return chartContext.hoverState.value && chartContext.hoverLocation in currentDrawElement
    }

    private fun isHoveredOrPressed(): Boolean {
        return isHovered() || isPressed()
    }

    private fun Int.animateToIf(
        targetValue: Int, condition: () -> Boolean
    ): Int {
        val animateState = intAnimationState(this)
        animateState.value = if (condition()) targetValue else this
        return animateState.value
    }

    private fun Float.animateToIf(
        targetValue: Float, condition: () -> Boolean
    ): Float {
        return floatAnimationState(this).also {
            it.value = if (condition()) targetValue else this
        }.value
    }

    private fun Color.animateToIf(
        targetValue: Color, condition: () -> Boolean
    ): Color {
        return colorAnimationState(this).also {
            it.value = if (condition()) targetValue else this
        }.value
    }

    private fun Offset.animateToIf(
        targetValue: Offset = Offset.Zero, condition: () -> Boolean
    ): Offset {
        return offsetAnimationState(this).also {
            it.value = if (condition()) targetValue else this
        }.value
    }

    private fun Size.animateToIf(targetValue: Size = Size.Zero, condition: () -> Boolean): Size {
        return sizeAnimationState(this).also {
            it.value = if (condition()) targetValue else this
        }.value
    }

    private fun Dp.animateToIf(targetValue: Dp = 0.dp, condition: () -> Boolean): Dp {
        return dpAnimationState(this).also {
            it.value = if (condition()) targetValue else this
        }.value
    }

    private fun DpOffset.animateToIf(targetValue: DpOffset = DpOffset.Zero, condition: () -> Boolean): DpOffset {
        return dpOffsetAnimationState(this).also {
            it.value = if (condition()) targetValue else this
        }.value
    }

    private fun TextUnit.animateToIf(targetValue: TextUnit = 0.sp, condition: () -> Boolean): TextUnit {
        return spAnimationState(this).also {
            it.value = if (condition()) targetValue else this
        }.value
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

    fun intAnimationState(initialValue: Int = 0): ChartIntAnimateState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultIntAnimateStateFactory
        )
    }

    fun floatAnimationState(initialValue: Float = 0f): ChartFloatAnimateState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultFloatAnimateStateFactory
        )
    }

    fun colorAnimationState(initialValue: Color = Color.Transparent): ChartColorAnimateState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultColorAnimateStateFactory
        )
    }

    fun sizeAnimationState(initialValue: Size = Size.Zero): ChartSizeAnimateState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultSizeAnimateStateFactory
        )
    }

    fun offsetAnimationState(initialValue: Offset = Offset.Zero): ChartOffsetAnimateState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultOffsetAnimateStateFactory
        )
    }

    fun dpAnimationState(initialValue: Dp = 0.dp): ChartDpAnimateState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultDpAnimateStateFactory
        )
    }

    fun dpOffsetAnimationState(initialValue: DpOffset = DpOffset.Zero): ChartDpOffsetAnimateState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultDpOffsetAnimateStateFactory
        )
    }

    fun spAnimationState(initialValue: TextUnit = 0.sp): ChartSpAnimateState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultSpAnimateStateFactory
        )
    }
}