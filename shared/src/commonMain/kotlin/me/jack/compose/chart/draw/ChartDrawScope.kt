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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.CoroutineScope
import me.jack.compose.chart.animation.ChartAnimatableState
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
import me.jack.compose.chart.draw.DrawElement.Companion.getCachedDrawElement
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
import me.jack.compose.chart.interaction.asElementInteraction
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

class ChartDrawScope<T>(
    val singleChartScope: SingleChartScope<T>,
    private val drawScope: DrawScope,
    private val scope: CoroutineScope,
    private val tapGestures: TapGestures<T>
) : DrawScope by drawScope, ChartDatasetAccessScope by ChartDatasetAccessScopeInstance {
    val chartContext: ChartContext = singleChartScope.chartContext
    val chartDataset: ChartDataset<T> = singleChartScope.chartDataset
    val currentDrawElement: DrawElement
        get() = internalCurrentDrawElement
    private var internalCurrentDrawElement: DrawElement = DrawElement.None
    private val traceableDrawScope = TraceableDrawScope(this)
    private val animationStateCaching = mutableMapOf<Class<*>, MutableList<ChartAnimatableState<*, *>>>()
    private var animationChildIds = mutableMapOf<Class<*>, Int>()

    fun reset() {
        animationChildIds.clear()
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
        val drawElement: DrawElement.Rect = getCachedDrawElement()
        drawElement.topLeft = topLeft
        drawElement.size = size
        drawElement.focusPoint = Offset.Unspecified
        internalCurrentDrawElement = drawElement
        detectClickableInteraction(internalCurrentDrawElement, currentItem())
    }

    fun clickable(
        block: TraceableDrawScope<T>.() -> Unit
    ) {
        traceableDrawScope.trackChartData(currentItem(), index)
        // invoke twice, first we update the draw element, and then the second time we draw the element.
        block.invoke(traceableDrawScope)
        internalCurrentDrawElement = traceableDrawScope.currentDrawElement
        detectClickableInteraction(internalCurrentDrawElement, currentItem())
        block.invoke(traceableDrawScope)
    }

    fun <T> ChartDrawScope<T>.clickableWithInteraction(
        block: TraceableDrawScope<T>.() -> Unit
    ) {
        clickable(block)
        detectChartInteraction()
    }

    private fun detectChartInteraction(focusPoint: Offset = Offset.Unspecified) {
        if (!isHoveredOrPressed()) return
        val interactionState = chartContext.elementInteraction.asElementInteraction<Any>()
        if (null == interactionState || currentDrawElement != interactionState.drawElement) {
            val chartGroupData = chartDataset.getChartGroupData(index)
            chartContext.chartInteractionHandler.tryEmit(
                ChartElementInteraction.Element(
                    location = focusPoint,
                    drawElement = currentDrawElement.clone(),
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

    fun isPressed(): Boolean {
        return !isScrollInProgress() && chartContext.pressState.value && chartContext.pressLocation in internalCurrentDrawElement
    }

    private fun isScrollInProgress(): Boolean {
        return chartContext.chartScrollState?.isScrollInProgress ?: false
    }

    fun isHovered(): Boolean {
        return !isScrollInProgress() && chartContext.hoverState.value && chartContext.hoverLocation in internalCurrentDrawElement
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

    private inline fun <reified T : ChartAnimatableState<*, *>> getCachedAnimatableState(): T? {
        val key = T::class.java
        val animatableStates = animationStateCaching.getOrPut(key) { mutableListOf() }
        val animationChildId = animationChildIds.getOrDefault(key = key, 0)
        animationChildIds[key] = animationChildId + 1
        return animatableStates.getOrNull(animationChildId + 1) as? T
    }

    private inline fun <reified T : ChartAnimatableState<*, *>> addCachedAnimatableState(animatableState: T) {
        val key = animatableState::class.java
        val animatableStates = animationStateCaching.getOrPut(key) { mutableListOf() }
        animatableStates.add(animatableState)
    }

    fun intAnimationState(initialValue: Int = 0): ChartIntAnimatableState {
        return getCachedAnimatableState() ?: intAnimatableState(initialValue = initialValue, scope = scope).also {
            addCachedAnimatableState(it)
        }
    }

    fun floatAnimationState(
        initialValue: Float = 0f
    ): ChartFloatAnimatableState {
        return getCachedAnimatableState<ChartFloatAnimatableState>()?.also { state ->
            state.reset(initialValue)
        } ?: floatAnimatableState(
            scope = scope, initialValue = initialValue
        ).also {
            addCachedAnimatableState(it)
        }
    }

    fun colorAnimationState(initialValue: Color = Color.Transparent): ChartColorAnimatableState {
        return getCachedAnimatableState<ChartColorAnimatableState>()?.also { state ->
            state.reset(initialValue)
        } ?: colorAnimatableState(initialValue = initialValue, scope = scope).also {
            addCachedAnimatableState(it)
        }
    }

    fun sizeAnimationState(initialValue: Size = Size.Zero): ChartSizeAnimatableState {
        return getCachedAnimatableState<ChartSizeAnimatableState>()?.also { state ->
            state.reset(initialValue)
        } ?: sizeAnimatableState(initialValue = initialValue, scope = scope).also {
            addCachedAnimatableState(it)
        }
    }

    fun offsetAnimationState(initialValue: Offset = Offset.Zero): ChartOffsetAnimatableState {
        return getCachedAnimatableState<ChartOffsetAnimatableState>()?.also { state ->
            state.reset(initialValue)
        } ?: offsetAnimatableState(initialValue = initialValue, scope = scope).also {
            addCachedAnimatableState(it)
        }
    }
}

class TraceableDrawScope<T>(
    private val drawScope: ChartDrawScope<T>
) : DrawScope by drawScope {
    var currentDrawElement: DrawElement = DrawElement.None
    private var isCurrentDrawElementUpdated = false
    private var currentItem: T? = null
    private var currentIndex: Int = 0

    val currentLeftTopOffset: Offset
        get() = with(drawScope) { currentLeftTopOffset }

    val nextLeftTopOffset: Offset
        get() = with(drawScope) { nextLeftTopOffset }

    val childCenterOffset: Offset
        get() = with(drawScope) { childCenterOffset }

    val childSize: Size
        get() = with(drawScope) { childSize }

    infix fun Color.whenPressed(targetValue: Color): Color {
        return with(drawScope) { whenPressed(targetValue) }
    }

    infix fun Int.whenPressedAnimateTo(targetValue: Int): Int {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Float.whenPressedAnimateTo(targetValue: Float): Float {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Color.whenPressedAnimateTo(targetValue: Color): Color {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Offset.whenPressedAnimateTo(targetValue: Offset): Offset {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    infix fun Size.whenPressedAnimateTo(targetValue: Size): Size {
        return with(drawScope) { whenPressedAnimateTo(targetValue) }
    }

    override fun drawRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val rectDrawElement: DrawElement.Rect = getCachedDrawElement()
                rectDrawElement.topLeft = topLeft
                rectDrawElement.size = size
                rectDrawElement.focusPoint = Offset(
                    x = topLeft.x + size.width / 2,
                    y = topLeft.y + size.height / 2
                )
                currentDrawElement = rectDrawElement
            },
            onDraw = {
                drawScope.drawRect(brush, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val rectDrawElement: DrawElement.Rect = getCachedDrawElement()
                rectDrawElement.color = color
                rectDrawElement.topLeft = topLeft
                rectDrawElement.size = size
                rectDrawElement.focusPoint = Offset(
                    x = topLeft.x + size.width / 2,
                    y = topLeft.y + size.height / 2
                )
                currentDrawElement = rectDrawElement
            },
            onDraw = {
                drawScope.drawRect(color, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val circleDrawElement: DrawElement.Circle = getCachedDrawElement()
                circleDrawElement.radius = radius
                circleDrawElement.center = center
                currentDrawElement = circleDrawElement
            },
            onDraw = {
                drawScope.drawCircle(brush, radius, center, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val circleDrawElement: DrawElement.Circle = getCachedDrawElement()
                circleDrawElement.color = color
                circleDrawElement.radius = radius
                circleDrawElement.center = center
                currentDrawElement = circleDrawElement
            },
            onDraw = {
                drawScope.drawCircle(color, radius, center, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawOval(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val ovalDrawElement: DrawElement.Oval = getCachedDrawElement()
                ovalDrawElement.topLeft = topLeft
                ovalDrawElement.size = size
                currentDrawElement = ovalDrawElement
            },
            onDraw = {
                drawScope.drawOval(brush, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawOval(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val ovalDrawElement: DrawElement.Oval = getCachedDrawElement()
                ovalDrawElement.color = color
                ovalDrawElement.topLeft = topLeft
                ovalDrawElement.size = size
                currentDrawElement = ovalDrawElement
            },
            onDraw = {
                drawScope.drawOval(color, topLeft, size, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val arcDrawElement: DrawElement.Arc = getCachedDrawElement()
                arcDrawElement.color = color
                arcDrawElement.startAngle = startAngle
                arcDrawElement.startAngle = startAngle
                arcDrawElement.leftTop = topLeft
                arcDrawElement.size = size
                arcDrawElement.sweepAngle = sweepAngle
                if (style is Stroke) {
                    arcDrawElement.strokeWidth = style.width
                }
                currentDrawElement = arcDrawElement
            },
            onDraw = {
                drawScope.drawArc(
                    color,
                    startAngle,
                    sweepAngle,
                    useCenter,
                    topLeft,
                    size,
                    alpha,
                    style,
                    colorFilter,
                    blendMode
                )
            }
        )
    }

    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            onUpdateDrawElement = {
                val arcDrawElement: DrawElement.Arc = getCachedDrawElement()
                arcDrawElement.startAngle = startAngle
                arcDrawElement.leftTop = topLeft
                arcDrawElement.size = size
                arcDrawElement.sweepAngle = sweepAngle
                if (style is Stroke) {
                    arcDrawElement.strokeWidth = style.width
                }
                currentDrawElement = arcDrawElement
            },
            onDraw = {
                drawScope.drawArc(
                    brush,
                    startAngle,
                    sweepAngle,
                    useCenter,
                    topLeft,
                    size,
                    alpha,
                    style,
                    colorFilter,
                    blendMode
                )
            }
        )
    }

    private inline fun drawElement(
        onUpdateDrawElement: () -> Unit,
        onDraw: () -> Unit
    ) {
        try {
            if (!isCurrentDrawElementUpdated) {
                onUpdateDrawElement()
            } else {
                onDraw()
            }
        } finally {
            isCurrentDrawElementUpdated = !isCurrentDrawElementUpdated
        }
    }

    fun trackChartData(currentItem: T, index: Int) {
        this.currentItem = currentItem
        this.currentIndex = index
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

sealed class DrawElement : Cloneable {
    companion object {
        internal val drawElementCaching = mutableMapOf<Class<*>, DrawElement>()
        internal inline fun <reified T : DrawElement> getCachedDrawElement(): T {
            val key = T::class.java
            return drawElementCaching.getOrPut(key = key) {
                when (key) {
                    Rect::class.java -> Rect()
                    Circle::class.java -> Circle()
                    Oval::class.java -> Oval()
                    Arc::class.java -> Arc()
                    else -> None
                }
            } as T
        }
    }

    open operator fun contains(location: Offset): Boolean = false

    public override fun clone(): DrawElement {
        return super.clone() as DrawElement
    }

    data object None : DrawElement()

    class Rect : DrawElement() {
        var color: Color = Color.Unspecified
        var topLeft: Offset = Offset.Zero
        var size: Size = Size.Zero
        var focusPoint: Offset = Offset.Unspecified

        override operator fun contains(location: Offset): Boolean {
            return location.intersect(topLeft, size)
        }

        override fun clone(): Rect {
            val rect = super.clone() as Rect
            rect.topLeft = topLeft
            rect.size = size
            rect.color = color
            rect.focusPoint = focusPoint
            return rect
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
            if (size != other.size) return false

            return true
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + topLeft.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + focusPoint.hashCode()
            return result
        }
    }

    class Circle : DrawElement() {
        var color: Color = Color.Unspecified
        var radius: Float = 0f
        var center: Offset = Offset.Zero

        override operator fun contains(location: Offset): Boolean {
            return location.intersectCircle(center, radius)
        }

        override fun clone(): Circle {
            val rect = super.clone() as Circle
            rect.radius = radius
            rect.center = center
            return rect
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
            if (center != other.center) return false

            return true
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
        override operator fun contains(location: Offset): Boolean {
            return location.intersectOval(topLeft + size.center, size)
        }

        override fun clone(): Oval {
            val rect = super.clone() as Oval
            rect.topLeft = topLeft
            rect.size = size
            return rect
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
            if (size != other.size) return false

            return true
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
        var leftTop: Offset = Offset.Zero
        var size: Size = Size.Zero
        var startAngle: Float = 0f
        var sweepAngle: Float = 0f
        var strokeWidth: Float = 0f
        override operator fun contains(location: Offset): Boolean {
            return if (0 < strokeWidth) {
                location.intersectArcStrokeWidth(
                    leftTop = leftTop,
                    size = size,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    strokeWidth = strokeWidth
                )
            } else {
                location.intersectArc(
                    leftTop = leftTop,
                    size = size,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle
                )
            }
        }

        override fun clone(): Arc {
            val arc = super.clone() as Arc
            arc.leftTop = leftTop
            arc.size = size
            arc.startAngle = startAngle
            arc.sweepAngle = sweepAngle
            arc.strokeWidth = strokeWidth
            return arc
        }

        override fun toString(): String {
            return "Arc(color=$color, leftTop=$leftTop, size=$size, startAngle=$startAngle, sweepAngle=$sweepAngle, strokeWidth=$strokeWidth)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Arc

            if (color != other.color) return false
            if (leftTop != other.leftTop) return false
            if (size != other.size) return false
            if (startAngle != other.startAngle) return false
            if (sweepAngle != other.sweepAngle) return false
            if (strokeWidth != other.strokeWidth) return false

            return true
        }

        override fun hashCode(): Int {
            var result = color.hashCode()
            result = 31 * result + leftTop.hashCode()
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
