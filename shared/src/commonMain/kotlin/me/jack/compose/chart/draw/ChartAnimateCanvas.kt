package me.jack.compose.chart.draw

import androidx.compose.animation.core.AnimationState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import me.jack.compose.chart.animation.ChartAnimateState
import me.jack.compose.chart.animation.ChartColorAnimateState
import me.jack.compose.chart.animation.ChartDpAnimateState
import me.jack.compose.chart.animation.ChartDpOffsetAnimateState
import me.jack.compose.chart.animation.ChartFloatAnimateState
import me.jack.compose.chart.animation.ChartIntAnimateState
import me.jack.compose.chart.animation.ChartIntOffsetAnimateState
import me.jack.compose.chart.animation.ChartOffsetAnimateState
import me.jack.compose.chart.animation.ChartSizeAnimateState
import me.jack.compose.chart.animation.ChartSpAnimateState
import me.jack.compose.chart.animation.colorAnimateState
import me.jack.compose.chart.animation.dpAnimateState
import me.jack.compose.chart.animation.dpOffsetAnimateState
import me.jack.compose.chart.animation.floatAnimateState
import me.jack.compose.chart.animation.intAnimateState
import me.jack.compose.chart.animation.intOffsetAnimateState
import me.jack.compose.chart.animation.offsetAnimateState
import me.jack.compose.chart.animation.sizeAnimateState
import me.jack.compose.chart.animation.spAnimateState
import me.jack.compose.chart.component.onHoverPointerEvent
import me.jack.compose.chart.draw.cache.DrawingKeyframeCache
import me.jack.compose.chart.draw.cache.getCachedAnimateState
import me.jack.compose.chart.draw.cache.getCachedDrawElement
import me.jack.compose.chart.interaction.ChartHoverInteraction
import me.jack.compose.chart.interaction.ChartTapInteraction

/**
 * AnimateCanvas is a canvas that could draw any thing with animation.
 * ```
 * AnimateCanvas(modifier = Modifier.fillMaxSize(){
 *     drawCircle(
 *         color = Color.Red whenPressedAnimateTo Color.Red.copy(0.4f),
 *         center = Offset(x = 60.dp.toPx(), y = 60.dp.toPx()),
 *         radius = 40.dp.toPx() whenPressedAnimateTo 60.dp.toPx()
 *     )
 * }
 * ```
 * AnimateCanvas should be an immutable canvas, means invoke multiple times with same parameter will have the same result.
 * We will invoke the onDraw twice, first we evaluate all the drawing elements, and [ChartAnimateDrawScope.isPreLayout] will be true,
 * Then, we use the drawing elements to check if we should change the drawing states, and [ChartAnimateDrawScope.isPreLayout] will be false
 *
 * A drawing Element means any operations inside [DrawScope], such as [DrawScope.drawRect] will be an [DrawElement.Rect]
 * If a [DrawElement] with any of `40.dp.toPx() whenPressedAnimateTo 60.dp.toPx()` we will make it active.
 * Means it will be an activated [DrawElement] and will change its value when state changed
 * The states means: pressed, hovered or other states.
 */
@Composable
fun AnimateCanvas(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    chartInteractionStates: ChartInteractionStates? = null,
    onDraw: ChartAnimateDrawScope.() -> Unit
) {
    val interactionStates = chartInteractionStates ?: rememberInteractionStates(interactionSource)
    val drawBlock by rememberUpdatedState(onDraw)
    var drawScope: ChartAnimateDrawScope? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    Spacer(
        modifier = modifier.animateInteraction(interactionSource).drawBehind {
            if (null == drawScope) {
                drawScope = ChartAnimateDrawScope(
                    drawScope = this,
                    scope = scope,
                    interactionStates = interactionStates
                )
            }
            val currentChartDrawScope = checkNotNull(drawScope)
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

fun Modifier.animateInteraction(
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

open class ChartAnimateDrawScope(
    private val drawScope: DrawScope,
    private val scope: CoroutineScope,
    val interactionStates: ChartInteractionStates
) : DrawScope by drawScope {
    internal var isPreLayout = true

    /**
     * Animate table state drawing cache pool.
     */
    private val drawingStateKeyframeCache = DrawingKeyframeCache()

    /**
     * Current drawing element cache pool.
     */
    val drawingElementCache = DrawingKeyframeCache()
    protected var currentDrawElement: DrawElement = DrawElement.None
    private var currentDrawElementGroup: DrawElement.DrawElementGroup? = null
    private val animateStateTable: MutableMap<Any, out AnimationState<*, *>> = mutableMapOf()
    private var animateStateUsedCount: Int = 0
    private var children: ArrayDeque<DrawElement> = ArrayDeque()

    open fun reset() {
        children.clear()
        animateStateTable.clear()
        animateStateUsedCount = 0
        drawingStateKeyframeCache.resetChildIds()
        drawingElementCache.resetChildIds()
    }

    // Factory for AnimateState
    private fun <V, T : ChartAnimateState<V, *>> defaultAnimateStateFactory(
        key: Class<T>
    ): T {
        @Suppress("UNCHECKED_CAST")
        return when (key) {
            ChartColorAnimateState::class.java -> colorAnimateState(scope, Color.Transparent)
            ChartFloatAnimateState::class.java -> floatAnimateState(scope, 0f)
            ChartIntAnimateState::class.java -> intAnimateState(scope, 0)
            ChartSizeAnimateState::class.java -> sizeAnimateState(scope, Size.Zero)
            ChartIntOffsetAnimateState::class.java -> intOffsetAnimateState(scope, IntOffset.Zero)
            ChartOffsetAnimateState::class.java -> offsetAnimateState(scope, Offset.Zero)
            ChartDpAnimateState::class.java -> dpAnimateState(scope, 0.dp)
            ChartDpOffsetAnimateState::class.java -> dpOffsetAnimateState(scope, DpOffset.Zero)
            ChartSpAnimateState::class.java -> spAnimateState(scope, 0.sp)
            else -> error("Doesn't support this type:$key.")
        } as T
    }

    fun isPreLayout(): Boolean = isPreLayout

    inline fun <reified T : DrawElement> obtainDrawElement(): T {
        val drawElement = drawingElementCache.getCachedDrawElement<T>()
        drawElement.isActivated = false
        drawElement.isPressed = false
        drawElement.isHovered = false
        return drawElement
    }

    infix fun Color.whenPressed(targetValue: Color): Color {
        return valueIf(targetValue = targetValue, condition = ::isCurrentElementHoveredOrPressed)
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
        return animateToIf(
            key = ChartIntAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    infix fun Float.whenPressedAnimateTo(targetValue: Float): Float {
        return animateToIf(
            key = ChartFloatAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    infix fun IntOffset.whenPressedAnimateTo(targetValue: IntOffset): IntOffset {
        return animateToIf(
            key = ChartIntOffsetAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    infix fun Color.whenPressedAnimateTo(targetValue: Color): Color {
        return animateToIf(
            key = ChartColorAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    infix fun Offset.whenPressedAnimateTo(targetValue: Offset): Offset {
        return animateToIf(
            key = ChartOffsetAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    infix fun Size.whenPressedAnimateTo(targetValue: Size): Size {
        return animateToIf(
            key = ChartSizeAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    infix fun Dp.whenPressedAnimateTo(targetValue: Dp): Dp {
        return animateToIf(
            key = ChartDpAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    infix fun DpOffset.whenPressedAnimateTo(targetValue: DpOffset): DpOffset {
        return animateToIf(
            key = ChartDpOffsetAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    /**
     * The text unit support em/sp, but for this method, we only support sp.
     */
    infix fun TextUnit.whenPressedAnimateTo(targetValue: TextUnit): TextUnit {
        return animateToIf(
            key = ChartSpAnimateState::class.java,
            targetValue = targetValue,
            condition = ::isCurrentElementHoveredOrPressed
        )
    }

    private fun DrawElement.isPressed(): Boolean {
        return interactionStates.isPressed &&
                (interactionStates.pressState.location in this || currentDrawElement.isPressed)
    }

    private fun DrawElement.isHovered(): Boolean {
        return interactionStates.isHovered &&
                (interactionStates.hoverState.location in this || currentDrawElement.isHovered)
    }

    internal fun updateCurrentActivatedDrawElement() {
        if (interactionStates.isHovered || interactionStates.isPressed) {
            children.forEach { drawElement ->
                if (drawElement.isActivated && (interactionStates.hoverState.location in drawElement)) {
                    if (drawElement is DrawElement.DrawElementGroup) {
                        drawElement.children.forEach { child ->
                            child.isHovered = interactionStates.isHovered
                            child.isPressed = interactionStates.isPressed
                        }
                    } else {
                        drawElement.isHovered = interactionStates.isHovered
                        drawElement.isPressed = interactionStates.isPressed
                    }
                }
            }
        }
    }

    inline fun clickableGroup(
        topLeft: Offset,
        size: Size,
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

    protected fun isCurrentElementHoveredOrPressed(): Boolean {
        return currentDrawElement.isHovered() || currentDrawElement.isPressed()
    }

    protected fun DrawElement.isHoveredOrPressed(): Boolean {
        return isHovered() || isPressed()
    }

    private fun <V, T : ChartAnimateState<V, *>> V.animateToIf(
        key: Class<T>,
        targetValue: V,
        condition: () -> Boolean
    ): V {
        animateStateUsedCount++
        val animateState = animationState(this, key).also {
            it.value = if (condition()) targetValue else this
        }
        return animateState.value
    }

    private fun <V, T : ChartAnimateState<V, *>> animationState(
        initialValue: V,
        key: Class<T>
    ): T {
        return drawingStateKeyframeCache.getCachedAnimateState(
            key = key,
            onHitCache = {
                val animateState = it as ChartAnimateState<V, *>
                animateState.reset(initialValue)
            },
            defaultValue = ::defaultAnimateStateFactory
        ).also {
            val animateState = it as ChartAnimateState<V, *>
            animateState.reset(initialValue)
        }
    }

    fun moveToNextDrawElement() {
        if (isPreLayout()) return
        currentDrawElement = children.removeFirstOrNull() ?: DrawElement.None
    }

    fun setCurrentDrawElementGroup(drawElementGroup: DrawElement.DrawElementGroup?) {
        this.currentDrawElementGroup = drawElementGroup
    }

    fun addChildDrawElement(drawElement: DrawElement) {
        children.add(drawElement)
    }

    open fun onDrawElement(drawElement: DrawElement) = Unit

    open fun onCreateDrawElement(drawElement: DrawElement) = Unit

    private fun addDrawElementIfNecessary(drawElement: DrawElement) {
        val drawElementGroup = currentDrawElementGroup ?: return
        drawElementGroup.children.add(drawElement)
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
            createDrawElement = {
                val drawElement: DrawElement.Rect = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement
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
            createDrawElement = {
                val drawElement: DrawElement.Rect = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = size
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                }
                drawElement
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
            createDrawElement = {
                val drawElement: DrawElement.Circle = obtainDrawElement()
                drawElement.radius = radius
                drawElement.center = center
                drawElement
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
            createDrawElement = {
                val drawElement: DrawElement.Circle = obtainDrawElement()
                drawElement.radius = radius
                drawElement.center = center
                drawElement
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
            createDrawElement = {
                val drawElement: DrawElement.Oval = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement
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
            createDrawElement = {
                val drawElement: DrawElement.Oval = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement
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
            createDrawElement = {
                val drawElement: DrawElement.Arc = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement.startAngle = startAngle
                drawElement.sweepAngle = sweepAngle
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                } else {
                    drawElement.strokeWidth = 0f
                }
                drawElement
            },
            onDraw = {
                drawScope.drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = useCenter,
                    topLeft = topLeft,
                    size = size,
                    alpha = alpha,
                    style = style,
                    colorFilter = colorFilter,
                    blendMode = blendMode
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
            createDrawElement = {
                val drawElement: DrawElement.Arc = obtainDrawElement()
                drawElement.startAngle = startAngle
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement.sweepAngle = sweepAngle
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                } else {
                    drawElement.strokeWidth = 0f
                }
                drawElement
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

    override fun drawImage(
        image: ImageBitmap,
        topLeft: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val rectDrawElement: DrawElement.Rect = obtainDrawElement()
                rectDrawElement.topLeft = topLeft
                rectDrawElement.size = Size(image.width.toFloat(), image.height.toFloat())
                rectDrawElement
            },
            onDraw = {
                drawScope.drawImage(image, topLeft, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Line = obtainDrawElement()
                drawElement.start = start
                drawElement.end = end
                drawElement.strokeWidth = strokeWidth
                drawElement.start = start
                drawElement
            },
            onDraw = {
                drawScope.drawLine(brush, start, end, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
            }
        )
    }

    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Line = obtainDrawElement()
                drawElement.start = start
                drawElement.end = end
                drawElement.strokeWidth = strokeWidth
                drawElement
            },
            onDraw = {
                drawScope.drawLine(color, start, end, strokeWidth, cap, pathEffect, alpha, colorFilter, blendMode)
            }
        )
    }

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Points = obtainDrawElement()
                drawElement.points = points
                drawElement.strokeWidth = strokeWidth
                drawElement
            },
            onDraw = {
                drawScope.drawPoints(
                    points,
                    pointMode,
                    brush,
                    strokeWidth,
                    cap,
                    pathEffect,
                    alpha,
                    colorFilter,
                    blendMode
                )
            }
        )
    }

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Points = obtainDrawElement()
                drawElement.points = points
                drawElement.strokeWidth = strokeWidth
                drawElement
            },
            onDraw = {
                drawScope.drawPoints(
                    points,
                    pointMode,
                    color,
                    strokeWidth,
                    cap,
                    pathEffect,
                    alpha,
                    colorFilter,
                    blendMode
                )
            }
        )
    }

    override fun drawRoundRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        style: DrawStyle,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.RoundRect = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement.cornerRadius = cornerRadius
                drawElement
            },
            onDraw = {
                drawScope.drawRoundRect(color, topLeft, size, cornerRadius, style, alpha, colorFilter, blendMode)
            }
        )
    }

    override fun drawRoundRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.RoundRect = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = size
                drawElement.cornerRadius = cornerRadius
                drawElement
            },
            onDraw = {
                drawScope.drawRoundRect(brush, topLeft, size, cornerRadius, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
        filterQuality: FilterQuality
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Rect = obtainDrawElement()
                drawElement.topLeft = dstOffset.toOffset()
                drawElement.size = dstSize.toSize()
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                }
                drawElement
            },
            onDraw = {
                drawScope.drawImage(
                    image,
                    srcOffset,
                    srcSize,
                    dstOffset,
                    dstSize,
                    alpha,
                    style,
                    colorFilter,
                    blendMode,
                    filterQuality
                )
            }
        )
    }

    @Deprecated(
        "Prefer usage of drawImage that consumes an optional FilterQuality parameter",
        replaceWith = ReplaceWith(
            "drawImage(image, srcOffset, srcSize, dstOffset, dstSize, alpha, style, colorFilter, blendMode, FilterQuality.Low)",
            "androidx.compose.ui.graphics.drawscope",
            "androidx.compose.ui.graphics.FilterQuality"
        ),
        level = DeprecationLevel.HIDDEN
    )
    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Rect = obtainDrawElement()
                drawElement.topLeft = dstOffset.toOffset()
                drawElement.size = dstSize.toSize()
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                }
                drawElement
            },
            onDraw = {
                drawScope.drawImage(image, srcOffset, srcSize, dstOffset, dstSize, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawPath(
        path: Path,
        brush: Brush,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Path = obtainDrawElement()
                drawElement.bounds = path.getBounds()
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                }
                drawElement
            },
            onDraw = {
                drawScope.drawPath(path, brush, alpha, style, colorFilter, blendMode)
            }
        )
    }

    override fun drawPath(
        path: Path,
        color: Color,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Path = obtainDrawElement()
                drawElement.bounds = path.getBounds()
                if (style is Stroke) {
                    drawElement.strokeWidth = style.width
                }
                drawElement
            },
            onDraw = {
                drawScope.drawPath(path, color, alpha, style, colorFilter, blendMode)
            }
        )
    }

    fun drawText(
        textMeasurer: TextMeasurer,
        text: AnnotatedString,
        topLeft: Offset = Offset.Zero,
        style: TextStyle = TextStyle.Default,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList(),
        size: Size = Size.Unspecified,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Rect = obtainDrawElement()
                drawElement.topLeft = topLeft
                if (size == Size.Unspecified) {
                    val textLayoutResult = textMeasurer.measure(text, style, overflow, softWrap, maxLines, placeholders)
                    drawElement.size = textLayoutResult.size.toSize()
                } else {
                    drawElement.size = size
                }
                drawElement
            },
            onDraw = {
                drawScope.drawText(
                    textMeasurer,
                    text,
                    topLeft,
                    style,
                    overflow,
                    softWrap,
                    maxLines,
                    placeholders,
                    size,
                    blendMode
                )
            }
        )
    }

    fun drawText(
        textMeasurer: TextMeasurer,
        text: String,
        topLeft: Offset = Offset.Zero,
        style: TextStyle = TextStyle.Default,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        size: Size = Size.Unspecified,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Rect = obtainDrawElement()
                drawElement.topLeft = topLeft
                if (size == Size.Unspecified) {
                    val textLayoutResult = textMeasurer.measure(text, style, overflow, softWrap, maxLines)
                    drawElement.size = textLayoutResult.size.toSize()
                } else {
                    drawElement.size = size
                }
                drawElement
            },
            onDraw = {
                drawScope.drawText(textMeasurer, text, topLeft, style, overflow, softWrap, maxLines, size, blendMode)
            }
        )
    }

    fun drawText(
        textLayoutResult: TextLayoutResult,
        color: Color = Color.Unspecified,
        topLeft: Offset = Offset.Zero,
        alpha: Float = Float.NaN,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Rect = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = textLayoutResult.size.toSize()
                drawElement
            },
            onDraw = {
                drawScope.drawText(
                    textLayoutResult,
                    color,
                    topLeft,
                    alpha,
                    shadow,
                    textDecoration,
                    drawStyle,
                    blendMode
                )
            }
        )
    }

    fun drawText(
        textLayoutResult: TextLayoutResult,
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        alpha: Float = Float.NaN,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null,
        drawStyle: DrawStyle? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) {
        drawElement(
            createDrawElement = {
                val drawElement: DrawElement.Rect = obtainDrawElement()
                drawElement.topLeft = topLeft
                drawElement.size = textLayoutResult.size.toSize()
                drawElement
            },
            onDraw = {
                drawScope.drawText(
                    textLayoutResult,
                    brush,
                    topLeft,
                    alpha,
                    shadow,
                    textDecoration,
                    drawStyle,
                    blendMode
                )
            }
        )
    }

    private inline fun drawElement(
        createDrawElement: () -> DrawElement,
        onDraw: () -> Unit
    ) {
        if (isPreLayout()) {
            val drawElement = createDrawElement()
            onCreateDrawElement(drawElement)
            addDrawElementIfNecessary(drawElement)
            drawElement.isActivated = 0 < animateStateUsedCount
            animateStateUsedCount = 0
            addChildDrawElement(drawElement)
        } else {
            onDrawElement(currentDrawElement)
            onDraw()
            moveToNextDrawElement()
        }
    }
}