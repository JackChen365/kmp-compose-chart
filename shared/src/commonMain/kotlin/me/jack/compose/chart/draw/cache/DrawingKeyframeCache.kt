package me.jack.compose.chart.draw.cache

import me.jack.compose.chart.animation.ChartAnimateState
import me.jack.compose.chart.draw.DrawElement

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
inline fun <reified T : DrawElement> DrawingKeyframeCache.getCachedDrawElement(
    noinline onHitCache: (T) -> Unit = { },
    noinline defaultValue: (key: Class<T>) -> T = ::defaultDrawingElementFactory
): T {
    return getCachedValueOrPut(
        key = T::class.java,
        onHitCache = onHitCache,
        defaultValue = defaultValue
    )
}

fun <T : DrawElement> defaultDrawingElementFactory(key: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    return when (key) {
        DrawElement.Rect::class.java -> DrawElement.Rect()
        DrawElement.Circle::class.java -> DrawElement.Circle()
        DrawElement.Oval::class.java -> DrawElement.Oval()
        DrawElement.Arc::class.java -> DrawElement.Arc()
        DrawElement.Line::class.java -> DrawElement.Line()
        DrawElement.Points::class.java -> DrawElement.Points()
        DrawElement.RoundRect::class.java -> DrawElement.RoundRect()
        DrawElement.Path::class.java -> DrawElement.Path()
        else -> error("Does not support this class type:$key")
    } as T
}

internal fun <V, T : ChartAnimateState<V, *>> DrawingKeyframeCache.getCachedAnimateState(
    key: Class<T>,
    onHitCache: (T) -> Unit,
    defaultValue: (key: Class<T>) -> T
): T {
    return getCachedValueOrPut(
        key = key,
        onHitCache = onHitCache,
        defaultValue = defaultValue
    )
}

class DrawingKeyframeCache {
    private val caching = mutableMapOf<Class<*>, MutableList<Any>>()
    private var childIds = mutableMapOf<Class<*>, Int>()

    fun <T> getCachedValue(key: Class<T>): T? {
        val animatableStates = caching.getOrPut(key) { mutableListOf() }
        val animationChildId = childIds.getOrDefault(key = key, 0)
        childIds[key] = animationChildId + 1
        @Suppress("UNCHECKED_CAST")
        return animatableStates.getOrNull(animationChildId + 1) as? T
    }

    fun <T> getCachedValueOrPut(
        key: Class<T>,
        onHitCache: (T) -> Unit,
        defaultValue: (key: Class<T>) -> T
    ): T {
        return getCachedValue(key)?.also { onHitCache(it) }
            ?: defaultValue(key).also { addCachedValue(it) }
    }

    fun <T> addCachedValue(value: T) {
        val key = value!!::class.java
        val animatableStates = caching.getOrPut(key) { mutableListOf() }
        animatableStates.add(value)
    }

    fun resetChildIds() {
        childIds.keys.forEach { key ->
            childIds[key] = 0
        }
    }
}