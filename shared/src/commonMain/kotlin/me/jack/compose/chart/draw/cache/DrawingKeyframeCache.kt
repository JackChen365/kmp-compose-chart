package me.jack.compose.chart.draw.cache

import me.jack.compose.chart.animation.ChartAnimateState

internal inline fun <reified T : ChartAnimateState<*, *>> DrawingKeyframeCache.getCachedValueOrPut(
    noinline onHitCache: (T) -> Unit,
    noinline defaultValue: (key: Class<*>) -> T
): T {
    return getCachedValueOrPut(
        key = T::class.java,
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