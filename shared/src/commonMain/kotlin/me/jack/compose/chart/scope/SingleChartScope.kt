package me.jack.compose.chart.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import me.jack.compose.chart.component.DonutData
import me.jack.compose.chart.component.TapGestures
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.chartScrollState
import me.jack.compose.chart.context.isHorizontal
import me.jack.compose.chart.measure.ChartContentMeasurePolicy
import me.jack.compose.chart.model.BarData
import me.jack.compose.chart.model.BubbleData
import me.jack.compose.chart.model.CandleData
import me.jack.compose.chart.model.LineData
import me.jack.compose.chart.model.PieData
import java.util.Objects
import kotlin.math.max

typealias LineChartScope = SingleChartScope<LineData>
typealias BarChartScope = SingleChartScope<BarData>
typealias DonutChartScope = SingleChartScope<DonutData>
typealias BubbleChartScope = SingleChartScope<BubbleData>
typealias PieChartScope = SingleChartScope<PieData>
typealias CandleStickChartScope = SingleChartScope<CandleData>

enum class ChartAnchor {
    Start, Top, End, Bottom, Center
}

internal val IntrinsicMeasurable.chartParentData: ChartParentData?
    get() = parentData as? ChartParentData

internal val ChartParentData?.anchor: ChartAnchor
    get() = this?.anchor ?: ChartAnchor.Center

internal val Placeable?.anchor: ChartAnchor
    get() = (this?.parentData as? ChartParentData)?.anchor ?: ChartAnchor.Center

internal val Placeable?.alignContent: Boolean
    get() = (this?.parentData as? ChartParentData)?.alignContent ?: true

/**
 * Parent data associated with children.
 */
internal data class ChartParentData(
    var anchor: ChartAnchor = ChartAnchor.Center, var alignContent: Boolean = true
)

interface MutableScrollableScope {
    var contentSize: Size
    var contentRange: Size
}

class SingleChartScopeInstance<T>(
    override val chartDataset: ChartDataset<T>,
    override val chartContext: ChartContext = ChartContext,
    override val tapGestures: TapGestures<T> = TapGestures(),
    override val contentMeasurePolicy: ChartContentMeasurePolicy,
) : SingleChartScope<T>, MutableScrollableScope {
    override var contentSize: Size = Size.Zero
    override var contentRange: Size = Size.Zero

    internal var chartContent: @Composable (SingleChartScope<T>.() -> Unit)? = null

    @Composable
    override fun ChartContent() {
        chartContent?.invoke(this)
    }

    override fun Modifier.anchor(anchor: ChartAnchor, alignContent: Boolean): Modifier {
        return this.then(ChartAnchorElement(anchor))
    }
}

class ChartCombinedScope(
    override val chartContext: ChartContext,
    override val contentMeasurePolicy: ChartContentMeasurePolicy,
    override val childItemCount: Int,
    override val groupCount: Int,
    val chartScopes: List<SingleChartScopeInstance<*>>
) : ChartScope, MutableScrollableScope {
    override var contentSize: Size = Size.Zero
    override var contentRange: Size = Size.Zero

    override fun Modifier.anchor(anchor: ChartAnchor, alignContent: Boolean): Modifier {
        return this.then(ChartAnchorElement(anchor))
    }

    inline fun <reified T> withSingleScope(block: SingleChartScope<T>.() -> Unit) {
        val singleChartScope = chartScopes.find {
            CandleData::class.java.isAssignableFrom(it.chartDataset.datasetType)
        }
        if (null != singleChartScope) {
            @Suppress("UNCHECKED_CAST")
            block.invoke(singleChartScope as SingleChartScope<T>)
        }
    }

    inline fun withAnyScope(singleChartScope: SingleChartScope<*>.() -> Unit) {
        check(chartScopes.isNotEmpty())
        singleChartScope.invoke(chartScopes.first())
    }
}

val ChartScope.chartChildDivider: Float
    get() = contentMeasurePolicy.childDividerSize

val ChartScope.chartGroupDivider: Float
    get() = contentMeasurePolicy.groupDividerSize

val ChartScope.chartGroupOffsets: Float
    get() = with(contentMeasurePolicy) { groupSize }

val ChartScope.isHorizontal: Boolean
    get() = contentMeasurePolicy.orientation.isHorizontal

val <T> SingleChartScope<T>.currentRange: IntRange
    get() {
        var start = 0
        var end = chartDataset.size
        val scrollState = chartContext.chartScrollState
        if (null != scrollState) {
            start = scrollState.firstVisibleItemIndex
            end = scrollState.lastVisibleItemIndex
        }
        return start until end
    }

@Immutable
// @LayoutScopeMarker remove it due to access chartData in any scope
interface ChartScope {
    val chartContext: ChartContext

    val contentMeasurePolicy: ChartContentMeasurePolicy

    val contentSize: Size

    val contentRange: Size

    val childItemCount: Int

    val groupCount: Int
        get() = 1

    val Size.mainAxis: Float
        get() = if (isHorizontal) width else height

    val Size.crossAxis: Float
        get() = if (isHorizontal) height else width

    val IntSize.mainAxis: Int
        get() = if (isHorizontal) width else height

    val IntSize.crossAxis: Int
        get() = if (isHorizontal) height else width

    val Offset.mainAxis: Float
        get() = if (isHorizontal) x else y

    val Offset.crossAxis: Float
        get() = if (isHorizontal) y else x

    @Stable
    fun Modifier.anchor(
        anchor: ChartAnchor, alignContent: Boolean = true
    ): Modifier
}

interface SingleChartScope<T> : ChartScope {
    val chartDataset: ChartDataset<T>
    val tapGestures: TapGestures<T>

    override val childItemCount: Int
        get() = chartDataset.size

    override val groupCount: Int
        get() = chartDataset.groupSize

    @Composable
    fun ChartContent()
}

internal class ChartAnchorNode(
    var anchor: ChartAnchor, var alignContent: Boolean = true
) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?): ChartParentData {
        return ((parentData as? ChartParentData) ?: ChartParentData()).also {
            it.anchor = anchor
            it.alignContent = alignContent
        }
    }
}

internal class ChartAnchorElement(
    val anchor: ChartAnchor,
    var alignContent: Boolean = true
) : ModifierNodeElement<ChartAnchorNode>() {
    override fun create(): ChartAnchorNode {
        return ChartAnchorNode(anchor, alignContent)
    }

    override fun update(node: ChartAnchorNode) {
        node.anchor = anchor
        node.alignContent = alignContent
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "ChartAnchorElement"
        value = anchor
    }

    override fun hashCode(): Int = Objects.hash(anchor, alignContent)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? ChartAnchorNode ?: return false
        return anchor == otherModifier.anchor && alignContent == otherModifier.alignContent
    }
}

interface ChartDatasetAccessScope {
    val chartGroup: String
    val chartGroupCount: Int
    val chartItemCount: Int
    val firstVisibleItem: Int
    val lastVisibleItem: Int
    val groupIndex: Int
    val index: Int
    fun <T> currentItem(): T
}

fun ChartDatasetAccessScope.isLastGroupIndex(): Boolean {
    return groupIndex == chartGroupCount - 1
}

fun ChartDatasetAccessScope.isFirstIndex(): Boolean {
    return index == firstVisibleItem
}

fun ChartDatasetAccessScope.isLastIndex(): Boolean {
    return index == lastVisibleItem
}

object ChartDatasetAccessScopeInstance : ChartDatasetAccessScope {
    override val chartGroup: String
        get() = internalChartGroup ?: error("The current chart group is null.")
    override val chartGroupCount: Int
        get() = internalGroupCount
    override val chartItemCount: Int
        get() = internalItemCount
    override val firstVisibleItem: Int
        get() = internalFirstVisibleItem
    override val lastVisibleItem: Int
        get() = internalLastVisibleItem
    override var groupIndex: Int = -1
    override var index: Int = -1
    var internalGroupCount: Int = -1
    var internalFirstVisibleItem: Int = -1
    var internalLastVisibleItem: Int = -1
    var internalItemCount: Int = -1
    var internalChartGroup: String? = null
    var internalCurrentItem: Any? = null
    override fun <T> currentItem(): T {
        @Suppress("UNCHECKED_CAST")
        return checkNotNull(internalCurrentItem) as T
    }
}

inline fun <T> SingleChartScope<T>.fastForEach(
    action: ChartDatasetAccessScope.(current: T) -> Unit
) {
    val range = currentRange
    ChartDatasetAccessScopeInstance.internalFirstVisibleItem = range.first
    ChartDatasetAccessScopeInstance.internalLastVisibleItem = range.last + 1
    chartDataset.forEachGroup { chartGroup ->
        chartDataset.forEach(
            chartGroup = chartGroup,
            start = range.first,
            end = range.last + 1
        ) { currentItem ->
            action(currentItem)
        }
    }
}

inline fun <T> SingleChartScope<T>.fastForEachByGroupIndex(
    action: ChartDatasetAccessScope.(T) -> Unit
) {
    val range = currentRange
    ChartDatasetAccessScopeInstance.internalFirstVisibleItem = range.first
    ChartDatasetAccessScopeInstance.internalLastVisibleItem = range.last
    for (index in currentRange) {
        chartDataset.forEachGroupIndexed { groupIndex, chartGroup ->
            val item = chartDataset[chartGroup][index]
            ChartDatasetAccessScopeInstance.internalChartGroup = chartGroup
            ChartDatasetAccessScopeInstance.internalGroupCount = groupCount
            ChartDatasetAccessScopeInstance.internalItemCount = childItemCount
            ChartDatasetAccessScopeInstance.internalCurrentItem = item
            ChartDatasetAccessScopeInstance.groupIndex = groupIndex
            ChartDatasetAccessScopeInstance.index = index
            action(ChartDatasetAccessScopeInstance, item)
        }
    }
}

inline fun <T> SingleChartScope<T>.fastForEachWithNext(
    start: Int = max(0, currentRange.first - 1),
    end: Int = currentRange.last + 1,
    action: ChartDatasetAccessScope.(current: T, next: T) -> Unit
) {
    chartDataset.forEachGroup { chartGroup ->
        chartDataset.forEachWithNext(
            chartGroup = chartGroup,
            start = start,
            end = end
        ) { current, next ->
            action(ChartDatasetAccessScopeInstance, current, next)
        }
    }
}
