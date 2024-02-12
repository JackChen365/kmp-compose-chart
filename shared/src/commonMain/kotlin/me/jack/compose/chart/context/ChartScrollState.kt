package me.jack.compose.chart.context

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.coroutineScope
import me.jack.compose.chart.debug.debugLog

val ChartContext.requireChartScrollState: ChartScrollState
    get() = chartScrollState ?: error("Can not found the chart scroll state.")

val ChartContext.chartScrollState: ChartScrollState?
    get() = get(ChartScrollState)

fun ChartContext.scrollable(
    orientation: Orientation = Orientation.Horizontal
): ChartContext {
    return this + MutableChartScrollState(orientation)
}

internal class MutableChartScrollState(
    override val orientation: Orientation
) : ChartScrollState {
    private var firstVisibleItemState = mutableIntStateOf(0)
    private var firstVisibleItemOffsetState = mutableFloatStateOf(0f)
    private var lastVisibleItemState = mutableIntStateOf(0)
    override var offset: Float by mutableFloatStateOf(0f)
    override var density: Density = Density(1f, 1f)
    override var isScrollInProgress: Boolean = false
    override var firstVisibleItemIndex: Int
        set(value) {
            firstVisibleItemState.intValue = value
        }
        get() = firstVisibleItemState.intValue


    override var firstVisibleItemOffset: Float
        set(value) {
            firstVisibleItemOffsetState.floatValue = value
        }
        get() = firstVisibleItemOffsetState.floatValue

    override var lastVisibleItemIndex: Int
        set(value) {
            lastVisibleItemState.intValue = value
        }
        get() = lastVisibleItemState.intValue

    override var itemCount: Int = 0
}

val Orientation.isHorizontal: Boolean
    get() = this == Orientation.Horizontal

interface ChartScrollState : ChartContext.Element {

    companion object Key : ChartContext.Key<ChartScrollState>

    override val key: ChartContext.Key<*> get() = Key

    val orientation: Orientation

    var offset: Float
    var density: Density
    var isScrollInProgress: Boolean
    val firstVisibleItemIndex: Int
    val firstVisibleItemOffset: Float
    val lastVisibleItemIndex: Int
    val itemCount: Int

    val currentVisibleRange: IntRange
        get() = firstVisibleItemIndex..lastVisibleItemIndex
}

@Composable
fun rememberScrollableState(): ChartScrollableState {
    val flingBehavior = flingBehavior()
    return remember {
        ChartScrollableState(flingBehavior = flingBehavior)
    }
}

@Composable
fun rememberScrollDelegate(
    chartScrollState: ChartScrollState,
    targetItemOffset: (groupIndex: Int, index: Int) -> Float,
    onDelta: (Float) -> Float
): ChartScrollDelegate {
    val lambdaState = rememberUpdatedState(onDelta)
    val lambdaTargetItemOffset = rememberUpdatedState(targetItemOffset)
    return remember {
        DefaultChartScrollDelegate(
            chartScrollState = chartScrollState,
            targetItemOffset = lambdaTargetItemOffset.value,
            onDelta = lambdaState.value
        )
    }
}

class ChartScrollableState(
    val flingBehavior: FlingBehavior
) : ScrollableState {
    var chartScrollDelegate: ChartScrollDelegate? = null

    private val scrollMutex = MutatorMutex()

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value

    private val isScrollingState = mutableStateOf(false)

    private val scrollScope: ScrollScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float {
            if (pixels.isNaN()) return 0f
            return chartScrollDelegate?.onDelta?.invoke(pixels) ?: pixels
        }
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) = coroutineScope {
        scrollMutex.mutateWith(scrollScope, scrollPriority) {
            isScrollingState.value = true
            try {
                block()
            } finally {
                isScrollingState.value = false
            }
        }
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return chartScrollDelegate?.onDelta?.invoke(delta) ?: delta
    }

    suspend fun scrollToItem(
        groupIndex: Int,
        index: Int,
        scrollOffset: Float = 0f
    ) {
        val animationScrollDelegate = chartScrollDelegate ?: return
        scroll {
            with(animationScrollDelegate) {
                scrollToItem(groupIndex, index, scrollOffset)
            }
        }
    }

    suspend fun animateScrollToItem(
        groupIndex: Int,
        index: Int,
        scrollOffset: Float = 0f
    ) {
        val animationScrollDelegate = chartScrollDelegate ?: return
        scroll {
            with(animationScrollDelegate) {
                animateScrollToItem(groupIndex, index, scrollOffset)
            }
        }
    }

    suspend fun performFling(velocity: Float) {
        scroll {
            with(flingBehavior) {
                performFling(velocity)
            }
        }
    }
}

interface ChartScrollDelegate {
    val chartScrollState: ChartScrollState
    val targetItemOffset: (groupIndex: Int, index: Int) -> Float
    val onDelta: (Float) -> Float

    fun getTargetItemOffset(groupIndex: Int, index: Int, offset: Float): Float

    suspend fun ScrollScope.scrollToItem(
        groupIndex: Int,
        index: Int,
        scrollOffset: Float = 0f
    )

    suspend fun ScrollScope.animateScrollToItem(
        groupIndex: Int,
        index: Int,
        itemOffset: Float = 0f
    )
}

class DefaultChartScrollDelegate(
    override val chartScrollState: ChartScrollState,
    override val targetItemOffset: (groupIndex: Int, index: Int) -> Float,
    override val onDelta: (Float) -> Float
) : ChartScrollDelegate {
    override fun getTargetItemOffset(groupIndex: Int, index: Int, offset: Float): Float =
        targetItemOffset(groupIndex, index) + offset

    override suspend fun ScrollScope.scrollToItem(groupIndex: Int, index: Int, scrollOffset: Float) {
        val targetItemOffset = targetItemOffset(groupIndex, index)
        scrollBy(-(targetItemOffset + scrollOffset))
    }

    override suspend fun ScrollScope.animateScrollToItem(
        groupIndex: Int,
        index: Int,
        itemOffset: Float
    ) {
        require(index >= 0f) { "Index should be non-negative ($index)" }
        val scrollOffset = chartScrollState.offset
        val anim = AnimationState(scrollOffset)
        var prevValue = scrollOffset
        // the target item offset with current scroll offset
        val target = scrollOffset - getTargetItemOffset(groupIndex, index, itemOffset)
        anim.animateTo(
            targetValue = target,
            sequentialAnimation = (anim.velocity != 0f)
        ) {
            val delta = value - prevValue
            debugLog { "Seeking by $delta (coercedValue = $value)" }
            scrollBy(delta)
            if (value == target) {
                debugLog { "Seeking by $delta consumed:$delta coercedValue:$value target:$target" }
                cancelAnimation()
            }
            prevValue += delta
        }
    }
}