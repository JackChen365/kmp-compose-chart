package me.jack.compose.chart.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.jack.compose.chart.animation.ChartAnimateState
import me.jack.compose.chart.animation.ChartColorAnimateState
import me.jack.compose.chart.animation.ChartFloatAnimateState
import me.jack.compose.chart.animation.ChartIntAnimateState
import me.jack.compose.chart.animation.colorAnimateState
import me.jack.compose.chart.animation.floatAnimateState
import me.jack.compose.chart.animation.intAnimateState
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.math.max
import kotlin.math.min

const val SINGLE_GROUP_NAME = "#"
fun <T> List<T>.asChartDataset(): ChartDataset<T> =
    MutableChartDataset<T>().also { it.add(SINGLE_GROUP_NAME, this) }

@Composable
fun <T> rememberSimpleChartDataset(): MutableChartDataset<T> {
    return remember {
        MutableChartDataset()
    }
}

@Composable
fun <T> rememberChartDataGroup(block: ChartDataGroupBuilder<T>.() -> Unit): ChartDataset<T> {
    return remember {
        val datasetBuilder = ChartDataGroupBuilder<T>()
        block.invoke(datasetBuilder)
        datasetBuilder.getChartDataset()
    }
}

@Composable
fun <T> rememberChartMutableDataGroup(block: ChartDataGroupBuilder<T>.() -> Unit): MutableChartDataset<T> {
    return remember {
        val datasetBuilder = ChartDataGroupBuilder<T>()
        block.invoke(datasetBuilder)
        val chartDataset = datasetBuilder.getChartDataset()
        chartDataset as MutableChartDataset<T>
    }
}

@Composable
inline fun <T> ChartDataset<T>.rememberMaxValue(
    crossinline maxValueEvaluator: (T) -> Float
): Float {
    var maxValue by remember {
        // avoid divide zero issue
        mutableFloatStateOf(1f)
    }
    LaunchedEffect(groupSize, size) {
        launch {
            maxValue = maxOf(block = maxValueEvaluator)
        }
    }
    return maxValue
}

@Composable
inline fun <T> ChartDataset<T>.rememberMinValue(
    crossinline minValueEvaluator: (T) -> Float
): Float {
    var minValue by remember {
        // avoid divide zero issue
        mutableFloatStateOf(1f)
    }
    LaunchedEffect(groupSize, size) {
        launch {
            minValue = minOf(block = minValueEvaluator)
        }
    }
    return minValue
}

@Composable
inline fun <T> ChartDataset<T>.rememberSumValue(
    crossinline sumValueEvaluator: (T) -> Float
): Float {
    var sumValue by remember {
        // avoid divide zero issue
        mutableFloatStateOf(1f)
    }
    LaunchedEffect(groupSize, size) {
        launch {
            sumValue = sumOf(block = sumValueEvaluator)
        }
    }
    return sumValue
}

@DslMarker
annotation class ChartDataGroupBuilderMarker

@ChartDataGroupBuilderMarker
class ChartDataGroupBuilder<T> {
    private val chartDataset: MutableChartDataset<T> = MutableChartDataset()
    fun dataset(chartGroup: String, block: DatasetBuilder<T>.() -> Unit) {
        val datasetBuilder = DatasetBuilder<T>()
        block.invoke(datasetBuilder)
        chartDataset.add(chartGroup = chartGroup, chartData = datasetBuilder.getDataset())
    }

    fun animatableDataset(
        scope: CoroutineScope, chartGroup: String, block: DatasetBuilder<T>.() -> Unit
    ) {
        val datasetBuilder = DatasetBuilder<T>()
        block.invoke(datasetBuilder)
        val dataset = datasetBuilder.getDataset()
        val animatableItems = dataset.map { getAnimatableDelegateItem(scope, it) }
        chartDataset.add(chartGroup = chartGroup, chartData = animatableItems)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getAnimatableDelegateItem(scope: CoroutineScope, item: T): T {
        val itemClass = item!!::class.java
        val itemAnimatableFields = mutableMapOf<String, ChartAnimateState<*, *>>()
        return Proxy.newProxyInstance(itemClass.classLoader, itemClass.interfaces, object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
                val propertyName = resolvePropertyName(method.name)
                if (method.name.startsWith("set")) {
                    val firstArgument = args?.firstOrNull()
                    checkNotNull(firstArgument)
                    val animatableState = getPropertyAnimatableField(item, propertyName, firstArgument::class.java)
                    if (null != animatableState) {
                        setAnimatableFieldValue(animatableState, firstArgument)
                    }
                } else if (method.name.startsWith("get")) {
                    val animatableState = getPropertyAnimatableField(item, propertyName, method.returnType)
                    if (null != animatableState) {
                        return animatableState.value
                    }
                }
                return if (null != args) {
                    method.invoke(item, *args)
                } else {
                    method.invoke(item)
                }
            }

            private fun setAnimatableFieldValue(animatableState: ChartAnimateState<*, *>, value: Any) {
                when (animatableState) {
                    is ChartColorAnimateState -> {
                        animatableState.value = value as Color
                    }

                    is ChartIntAnimateState -> {
                        animatableState.value = value as Int
                    }

                    is ChartFloatAnimateState -> {
                        animatableState.value = value as Float
                    }
                }
            }

            private fun getPropertyAnimatableField(
                item: T, propertyName: String, propertyType: Class<*>
            ): ChartAnimateState<*, *>? {
                val key = propertyName + System.identityHashCode(item)
                var animatableState = itemAnimatableFields[key]
                if (null == animatableState) {
                    animatableState = when (propertyType) {
                        Color::class.java -> {
                            colorAnimateState(scope, readInstanceProperty(item as Any, propertyName))
                        }

                        Float::class.java -> {
                            floatAnimateState(scope, readInstanceProperty(item as Any, propertyName))
                        }

                        Int::class.java -> {
                            intAnimateState(scope, readInstanceProperty(item as Any, propertyName))
                        }

                        else -> null
                    }
                    if (null != animatableState) {
                        itemAnimatableFields[key] = animatableState
                    }
                }
                return animatableState
            }

            private fun resolvePropertyName(name: String): String {
                return if (name.startsWith("set")) {
                    name.substringAfterLast("set")
                } else if (name.startsWith("get")) {
                    name.substringAfterLast("get")
                } else name
            }

            fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
                val getPropertyValueMethod = instance::class.java.getMethod("get$propertyName")
                return getPropertyValueMethod.invoke(instance) as R
            }
        }) as T
    }

    class DatasetBuilder<T> {
        private val internalList = mutableListOf<T>()
        fun items(itemCount: Int, block: (index: Int) -> T) {
            repeat(itemCount) { index ->
                internalList.add(block(index))
            }
        }

        fun <E> items(list: List<E>, block: (index: Int, item: E) -> T) {
            list.forEachIndexed { index, item ->
                internalList.add(block(index, item))
            }
        }

        fun getDataset() = internalList
    }

    fun getChartDataset(): ChartDataset<T> {
        return chartDataset
    }
}

inline fun ChartDataset<*>.forEachGroup(action: (chartGroup: String) -> Unit) {
    for (element in chartGroups) action(element)
}

inline fun ChartDataset<*>.forEachGroupIndexed(action: (index: Int, chartGroup: String) -> Unit) {
    var index = 0
    for (element in chartGroups) action(index++, element)
}

inline fun <T> ChartDataset<T>.forEach(
    action: ChartDatasetAccessScope.(chartGroup: String, T) -> Unit
) {
    forEachGroupIndexed { groupIndex, chartGroup ->
        ChartDatasetAccessScopeInstance.groupIndex = groupIndex
        ChartDatasetAccessScopeInstance.internalChartGroup = chartGroup
        this[chartGroup].forEachIndexed { index, currentItem ->
            ChartDatasetAccessScopeInstance.internalCurrentItem = currentItem
            ChartDatasetAccessScopeInstance.index = index
            ChartDatasetAccessScopeInstance.action(chartGroup, currentItem)
        }
    }
}

inline fun <T> ChartDataset<T>.forEach(
    chartGroup: String = SINGLE_GROUP_NAME, action: ChartDatasetAccessScope.(T) -> Unit
) {
    ChartDatasetAccessScopeInstance.internalChartGroup = chartGroup
    ChartDatasetAccessScopeInstance.groupIndex = chartGroups.indexOf(chartGroup)
    this[chartGroup].forEachIndexed { index, currentItem ->
        ChartDatasetAccessScopeInstance.internalCurrentItem = currentItem
        ChartDatasetAccessScopeInstance.index = index
        ChartDatasetAccessScopeInstance.action(currentItem)
    }
}

inline fun <T> ChartDataset<T>.forEach(
    chartGroup: String = SINGLE_GROUP_NAME,
    start: Int = 0,
    end: Int = Int.MAX_VALUE,
    action: ChartDatasetAccessScope.(T) -> Unit
) {
    val dataset = this[chartGroup]
    var index = max(0, start)
    val dataSize = min(size, end)
    ChartDatasetAccessScopeInstance.internalFirstVisibleItem = start
    ChartDatasetAccessScopeInstance.internalLastVisibleItem = dataSize
    ChartDatasetAccessScopeInstance.internalChartGroup = chartGroup
    ChartDatasetAccessScopeInstance.groupIndex = chartGroups.indexOf(chartGroup)
    while (index < dataSize) {
        val currentItem = dataset[index]
        ChartDatasetAccessScopeInstance.internalCurrentItem = currentItem
        ChartDatasetAccessScopeInstance.index = index
        ChartDatasetAccessScopeInstance.action(currentItem)
        index++
    }
}

inline fun <T> ChartDataset<T>.forEachWithNext(
    chartGroup: String = SINGLE_GROUP_NAME,
    start: Int = 0,
    end: Int = Int.MAX_VALUE,
    action: ChartDatasetAccessScope.(current: T, next: T) -> Unit
) {
    val dataset = this[chartGroup]
    var index = start
    val dataSize = min(size, end)
    ChartDatasetAccessScopeInstance.internalFirstVisibleItem = start
    ChartDatasetAccessScopeInstance.internalLastVisibleItem = dataSize
    ChartDatasetAccessScopeInstance.internalChartGroup = chartGroup
    ChartDatasetAccessScopeInstance.groupIndex = chartGroups.indexOf(chartGroup)
    while (index + 1 < dataSize) {
        val currentItem = dataset[index]
        ChartDatasetAccessScopeInstance.internalCurrentItem = currentItem
        ChartDatasetAccessScopeInstance.index = index
        ChartDatasetAccessScopeInstance.action(currentItem, dataset[index + 1])
        index++
    }
}

inline fun <T> ChartDataset<T>.sumOf(
    start: Int = 0, end: Int = Int.MAX_VALUE, block: (T) -> Float
): Float {
    var sumValue = 0f
    forEachGroup { chartGroup ->
        forEach(chartGroup, start, end) { chartData ->
            sumValue += block(chartData)
        }
    }
    return sumValue
}

/**
 * Calculate the max value of the given ChartDataset
 */
inline fun <T> ChartDataset<T>.maxOf(
    start: Int = 0, end: Int = Int.MAX_VALUE, block: (T) -> Float
): Float {
    var maxValue = Float.MIN_VALUE
    forEachGroup { chartGroup ->
        forEach(chartGroup, start, end) { chartData ->
            maxValue = maxValue.coerceAtLeast(block(chartData))
        }
    }
    return maxValue
}

inline fun <T> ChartDataset<T>.minOf(
    start: Int = 0, end: Int = Int.MAX_VALUE, block: (T) -> Float
): Float {
    var minValue = Float.MAX_VALUE
    chartGroups.forEach { group ->
        forEach(group, start, end) { chartData ->
            minValue = minValue.coerceAtMost(block(chartData))
        }
    }
    return minValue
}

inline fun <T> ChartDataset<T>.maxGroupValueOf(action: (T) -> Float): Float {
    var maxValue = Float.MIN_VALUE
    for (index in indices) {
        var sumValue = 0f
        forEachGroup { chartGroup ->
            val item = get(chartGroup)[index]
            sumValue += action(item)
        }
        maxValue = max(maxValue, sumValue)
    }
    return maxValue
}

inline fun <T> ChartDataset<T>.minGroupValueOf(action: (T) -> Float): Float {
    var minValue = Float.MAX_VALUE
    for (index in indices) {
        var sumValue = 0f
        forEachGroup { chartGroup ->
            val item = get(chartGroup)[index]
            sumValue += action(item)
        }
        minValue = min(minValue, sumValue)
    }
    return minValue
}

inline fun <T> ChartDataset<T>.computeGroupTotalValues(
    valueProvider: (T) -> Float
): List<Float> {
    val sumValueList = mutableListOf<Float>()
    for (index in indices) {
        var sumValue = 0f
        forEachGroup { groupName ->
            val chartData = get(groupName)[index]
            sumValue += valueProvider(chartData)
        }
        sumValueList.add(sumValue)
    }
    return sumValueList
}

fun <T> ChartDataset<T>.getChartGroupData(currentIndex: Int): List<T> {
    val chartGroupData = mutableListOf<T>()
    forEachGroup { chartGroup ->
        val item = dataset[chartGroup]?.get(currentIndex)
        if (null != item) {
            chartGroupData.add(item)
        }
    }
    return chartGroupData
}

interface ChartDataset<T> {
    val dataset: Map<String, List<T>>
    val size: Int
    val groupSize: Int
    val chartGroups: Set<String>
        get() = dataset.keys

    val indices: IntRange
        get() = 0 until size

    val datasetType: Class<out T>

    operator fun get(key: String): List<T> {
        return dataset[key] ?: emptyList()
    }
}

interface MutableDataset<T> {
    fun add(chartGroup: String, chartData: List<T>)

    fun remove(chartGroup: String)

    fun add(chartGroup: String, chartData: T)

    fun remove(chartGroup: String, chartData: T)

    fun remove(chartGroup: String, index: Int)
}

open class MutableChartDataset<T> : ChartDataset<T>, MutableDataset<T> {
    private val internalMutableChartDataset = SnapshotStateMap<String, SnapshotStateList<T>>()
    private var internalDatasetType: Class<out T>? = null
    override val dataset: Map<String, SnapshotStateList<T>>
        get() = internalMutableChartDataset

    private var chartDatasetSize by mutableIntStateOf(0)
    override val size: Int
        get() = chartDatasetSize

    override val groupSize: Int
        get() = dataset.keys.size

    override val datasetType: Class<out T>
        get() = internalDatasetType ?: error("An empty dataset")

    override fun add(chartGroup: String, chartData: List<T>) {
        if (chartData is SnapshotStateList) {
            internalMutableChartDataset[chartGroup] = chartData
        } else {
            internalMutableChartDataset[chartGroup] = chartData.toMutableStateList()
        }
        chartDatasetSize = if (internalMutableChartDataset.isEmpty()) {
            chartData.size
        } else {
            max(chartData.size, internalMutableChartDataset.values.maxOf { it.size })
        }
        internalDatasetType = chartData.firstNotNullOfOrNull { it!!::class.java }
    }

    override fun remove(chartGroup: String) {
        internalMutableChartDataset.remove(chartGroup)
        chartDatasetSize = if (internalMutableChartDataset.isEmpty()) 0 else
            internalMutableChartDataset.values.maxOf { it.size }
    }

    override fun add(chartGroup: String, chartData: T) {
        internalMutableChartDataset[chartGroup]?.add(chartData)
        chartDatasetSize = if (internalMutableChartDataset.isEmpty()) 0 else
            internalMutableChartDataset.values.maxOf { it.size }
    }

    override fun remove(chartGroup: String, chartData: T) {
        internalMutableChartDataset[chartGroup]?.remove(chartData)
        chartDatasetSize = if (internalMutableChartDataset.isEmpty()) 0 else
            internalMutableChartDataset.values.maxOf { it.size }
    }

    override fun remove(chartGroup: String, index: Int) {
        internalMutableChartDataset[chartGroup]?.removeAt(index)
        chartDatasetSize = if (internalMutableChartDataset.isEmpty()) 0 else
            internalMutableChartDataset.values.maxOf { it.size }
    }
}