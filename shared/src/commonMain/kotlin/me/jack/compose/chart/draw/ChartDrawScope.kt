package me.jack.compose.chart.draw

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.CoroutineScope
import me.jack.compose.chart.animation.ChartColorAnimatableState
import me.jack.compose.chart.animation.ChartFloatAnimatableState
import me.jack.compose.chart.animation.ChartIntAnimatableState
import me.jack.compose.chart.animation.ChartOffsetAnimatableState
import me.jack.compose.chart.animation.ChartSizeAnimatableState
import me.jack.compose.chart.animation.colorAnimatableState
import me.jack.compose.chart.animation.floatAnimatableState
import me.jack.compose.chart.animation.intAnimatableState
import me.jack.compose.chart.animation.offsetAnimatableState
import me.jack.compose.chart.animation.sizeAnimatableState
import me.jack.compose.chart.component.TapGestures
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.chartInteractionHandler
import me.jack.compose.chart.context.chartScrollState
import me.jack.compose.chart.context.pressLocation
import me.jack.compose.chart.context.pressState
import me.jack.compose.chart.context.tryEmit
import me.jack.compose.chart.draw.cache.DrawingKeyframeCache
import me.jack.compose.chart.draw.cache.getCachedValueOrPut
import me.jack.compose.chart.draw.interaction.doubleTapLocation
import me.jack.compose.chart.draw.interaction.doubleTapState
import me.jack.compose.chart.draw.interaction.elementInteraction
import me.jack.compose.chart.draw.interaction.hoverLocation
import me.jack.compose.chart.draw.interaction.hoverState
import me.jack.compose.chart.draw.interaction.longPressLocation
import me.jack.compose.chart.draw.interaction.longPressTapState
import me.jack.compose.chart.draw.interaction.tapLocation
import me.jack.compose.chart.draw.interaction.tapState
import me.jack.compose.chart.interaction.ChartElementInteraction
import me.jack.compose.chart.interaction.ChartTapInteraction
import me.jack.compose.chart.scope.ChartDataset
import me.jack.compose.chart.scope.ChartDatasetAccessScope
import me.jack.compose.chart.scope.ChartDatasetAccessScopeInstance
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.fastForEach
import me.jack.compose.chart.scope.fastForEachWithNext
import me.jack.compose.chart.scope.getChartGroupData
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

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
    modifier: Modifier = Modifier,
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
    // Factory for AnimatableState
    private val defaultIntAnimatableStateFactory: (Class<*>) -> ChartIntAnimatableState =
        { intAnimatableState(scope, 0) }
    private val defaultFloatAnimatableStateFactory: (Class<*>) -> ChartFloatAnimatableState =
        { floatAnimatableState(scope, 0f) }
    private val defaultColorAnimatableStateFactory: (Class<*>) -> ChartColorAnimatableState =
        { colorAnimatableState(scope, Color.Transparent) }
    private val defaultSizeAnimatableStateFactory: (Class<*>) -> ChartSizeAnimatableState =
        { sizeAnimatableState(scope, Size.Zero) }
    private val defaultOffsetAnimatableStateFactory: (Class<*>) -> ChartOffsetAnimatableState =
        { offsetAnimatableState(scope, Offset.Zero) }
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
    private val updateDrawingElementDrawScope = UpdateDrawingElementDrawScope(this, drawingElementCache)

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

    fun clickableRect(
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

    fun clickableRectWithInteraction(
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

    fun clickable(
        block: UpdateDrawingElementDrawScope<T>.() -> Unit
    ) {
        updateDrawingElementDrawScope.trackChartData(currentItem(), index)
        // invoke twice, first we update the draw element, and then the second time we draw the element.
        block.invoke(updateDrawingElementDrawScope)
        currentDrawElement = updateDrawingElementDrawScope.currentDrawElement
        updateActiveDrawElementIfNeeded(currentDrawElement)
        detectClickableInteraction(currentDrawElement, currentItem())
        block.invoke(updateDrawingElementDrawScope)
    }

    fun <T> ChartDrawScope<T>.clickableWithInteraction(
        block: UpdateDrawingElementDrawScope<T>.() -> Unit
    ) {
        clickable(block)
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
            println("newDrawElement:$newDrawElement")
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

    private fun isScrollInProgress(): Boolean {
        return chartContext.chartScrollState?.isScrollInProgress ?: false
    }

    fun isPressed(): Boolean {
        return activatedDrawElement == currentDrawElement && !isScrollInProgress() && isPressedState()
    }

    private fun isPressedState(): Boolean {
        return chartContext.pressState.value &&
                chartContext.pressLocation in currentDrawElement
    }

    fun isHovered(): Boolean {
        return activatedDrawElement == currentDrawElement && !isScrollInProgress() && isHoveredState()
    }

    private fun isHoveredState(): Boolean {
        return chartContext.hoverState.value && chartContext.hoverLocation in currentDrawElement
    }

    fun isHoveredOrPressed(): Boolean {
        return isHovered() || isPressed()
    }

    fun Int.animateToIf(
        targetValue: Int, condition: () -> Boolean
    ): Int {
        val intAnimatableState = intAnimationState(this)
        intAnimatableState.value = if (condition()) targetValue else this
        return intAnimatableState.value
    }

    fun Float.animateToIf(
        targetValue: Float, condition: () -> Boolean
    ): Float {
        val floatAnimatableState = floatAnimationState(this)
        floatAnimatableState.value = if (condition()) targetValue else this
        return floatAnimatableState.value
    }

    fun Color.animateToIf(
        targetValue: Color, condition: () -> Boolean
    ): Color {
        val colorAnimatableState = colorAnimationState(this)
        colorAnimatableState.value = if (condition()) targetValue else this
        return colorAnimatableState.value
    }

    fun Offset.animateToIf(
        targetValue: Offset = Offset.Zero, condition: () -> Boolean
    ): Offset {
        val offsetAnimatableState = offsetAnimationState(this)
        offsetAnimatableState.value = if (condition()) targetValue else this
        return offsetAnimatableState.value
    }

    fun Size.animateToIf(targetValue: Size = Size.Zero, condition: () -> Boolean): Size {
        val sizeAnimatableState = sizeAnimationState(this)
        sizeAnimatableState.value = if (condition()) targetValue else this
        return sizeAnimatableState.value
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

    fun intAnimationState(initialValue: Int = 0): ChartIntAnimatableState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultIntAnimatableStateFactory
        )
    }

    fun floatAnimationState(initialValue: Float = 0f): ChartFloatAnimatableState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultFloatAnimatableStateFactory
        )
    }

    fun colorAnimationState(initialValue: Color = Color.Transparent): ChartColorAnimatableState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultColorAnimatableStateFactory
        )
    }

    fun sizeAnimationState(initialValue: Size = Size.Zero): ChartSizeAnimatableState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultSizeAnimatableStateFactory
        )
    }

    fun offsetAnimationState(initialValue: Offset = Offset.Zero): ChartOffsetAnimatableState {
        return drawingStateKeyframeCache.getCachedValueOrPut(
            onHitCache = { it.reset(initialValue) },
            defaultValue = defaultOffsetAnimatableStateFactory
        )
    }
}

fun DrawElement.isHoveredOrPressed(chartContext: ChartContext): Boolean {
    return chartContext.hoverState.value && chartContext.hoverLocation in this ||
            chartContext.pressState.value && chartContext.pressLocation in this
}

fun DrawElement.isTap(chartContext: ChartContext): Boolean {
    return chartContext.tapState.value && chartContext.tapLocation in this
}

fun DrawElement.isLongPressed(chartContext: ChartContext): Boolean {
    return chartContext.longPressTapState.value && chartContext.longPressLocation in this
}

fun DrawElement.isDoubleTap(chartContext: ChartContext): Boolean {
    return chartContext.doubleTapState.value && chartContext.doubleTapLocation in this
}

sealed class DrawElement {
    open var focusPoint: Offset = Offset.Unspecified

    open operator fun contains(location: Offset): Boolean = false

    open fun copy(other: DrawElement): DrawElement {
        return this
    }

    fun isNone(): Boolean {
        return this == None
    }

    /**
     * Since we have defined the method: copy, we should not make it a data object.
     */
    @Suppress("ConvertObjectToDataObject")
    object None : DrawElement()

    class Rect : DrawElement() {
        var color: Color = Color.Unspecified
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero
        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    Offset(
                        x = topLeft.x + size.width / 2,
                        y = topLeft.y + size.height / 2
                    )
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return location.intersect(topLeft, size)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Rect) {
                topLeft = other.topLeft
                size = other.size
                color = other.color
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Rect(topLeft=$topLeft, size=$size, focus:${focusPoint})"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Rect

            if (color != other.color) return false
            if (topLeft != other.topLeft) return false
            return size == other.size
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + topLeft.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    class Circle : DrawElement() {
        var color: Color = Color.Unspecified
        var radius: Float = 0f
        var center: Offset = Offset.Zero

        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    center
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return location.intersectCircle(center, radius)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Circle) {
                radius = other.radius
                center = other.center
                color = other.color
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Circle(radius=$radius, center=$center)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Circle

            if (color != other.color) return false
            if (radius != other.radius) return false
            return center == other.center
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + radius.hashCode()
            result = 31 * result + center.hashCode()
            return result
        }
    }

    @Stable
    class Oval : DrawElement() {
        var color: Color = Color.Unspecified
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero

        override var focusPoint: Offset = Offset.Unspecified
            get() {
                return if (field == Offset.Unspecified) {
                    Offset(
                        x = topLeft.x + size.width / 2,
                        y = topLeft.y + size.height / 2
                    )
                } else {
                    field
                }
            }

        override operator fun contains(location: Offset): Boolean {
            return location.intersectOval(topLeft + size.center, size)
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Oval) {
                topLeft = other.topLeft
                size = other.size
                color = other.color
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Oval(topLeft=$topLeft, size=$size)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Oval

            if (color != other.color) return false
            if (topLeft != other.topLeft) return false
            return size == other.size
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + topLeft.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    @Stable
    class Arc : DrawElement() {
        var color: Color = Color.Unspecified
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero
        var startAngle: Float = 0f
        var sweepAngle: Float = 0f
        var strokeWidth: Float = 0f
        override operator fun contains(location: Offset): Boolean {
            return if (0 < strokeWidth) {
                location.intersectArcStrokeWidth(
                    leftTop = topLeft,
                    size = size,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    strokeWidth = strokeWidth
                )
            } else {
                location.intersectArc(
                    leftTop = topLeft,
                    size = size,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle
                )
            }
        }

        override fun copy(other: DrawElement): DrawElement {
            if (other is Arc) {
                color = other.color
                topLeft = other.topLeft
                size = other.size
                startAngle = other.startAngle
                sweepAngle = other.sweepAngle
                strokeWidth = other.strokeWidth
                focusPoint = other.focusPoint
            }
            return this
        }

        override fun toString(): String {
            return "Arc(color=$color, topLeft=$topLeft, size=$size, startAngle=$startAngle, sweepAngle=$sweepAngle, strokeWidth=$strokeWidth)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arc

            if (color != other.color) return false
            if (topLeft != other.topLeft) return false
            if (size != other.size) return false
            if (startAngle != other.startAngle) return false
            if (sweepAngle != other.sweepAngle) return false
            return strokeWidth == other.strokeWidth
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + topLeft.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + startAngle.hashCode()
            result = 31 * result + sweepAngle.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            return result
        }
    }
}

fun Offset.intersect(topLeft: Offset, size: Size): Boolean {
    return x in topLeft.x..<topLeft.x + size.width && y in topLeft.y..<topLeft.y + size.height
}

fun Offset.intersectCircle(center: Offset, radius: Float): Boolean {
    val distanceSquared = (x - center.x) * (x - center.x) + (y - center.y) * (y - center.y)
    return distanceSquared <= radius * radius
}

fun Offset.intersectOval(center: Offset, size: Size): Boolean {
    val term1 = ((x - center.x) * (x - center.x)) / (size.width * size.width)
    val term2 = ((y - center.y) * (y - center.y)) / (size.height * size.height)
    return (term1 + term2) < 1
}

fun Offset.intersectArc(
    leftTop: Offset, size: Size, startAngle: Float, sweepAngle: Float
): Boolean {
    val centerX = leftTop.x + size.width / 2
    val centerY = leftTop.y + size.height / 2

    val dx = x - centerX
    val dy = y - centerY

    val distance = sqrt(dx * dx + dy * dy)
    var angle = atan2(dy, dx) * (180 / PI)
    if (angle < 0) angle += 360.0

    val start = startAngle % 360
    val end = (start + sweepAngle) % 360

    val isWithinDistance = distance <= size.width / 2
    val isWithinAngles = if (start < end) {
        angle in start..end
    } else {
        angle in 0f..end || angle in start..360f
    }
    return isWithinDistance && isWithinAngles
}

fun Offset.intersectArcStrokeWidth(
    leftTop: Offset, size: Size, startAngle: Float, sweepAngle: Float, strokeWidth: Float
): Boolean {
    val centerX = leftTop.x + size.width / 2
    val centerY = leftTop.y + size.height / 2

    val dx = x - centerX
    val dy = y - centerY

    val distance = sqrt(dx * dx + dy * dy)
    var angle = atan2(dy, dx) * (180 / PI)
    if (angle < 0) angle += 360.0

    val start = startAngle % 360
    val end = (start + sweepAngle) % 360

    val isWithinDistance =
        distance < (size.width / 2 + strokeWidth / 2) && distance > (size.width / 2 - strokeWidth)
    val isWithinAngles = if (start < end) {
        angle in start..end
    } else {
        angle in 0f..end || angle in start..360f
    }
    return isWithinDistance && isWithinAngles
}
