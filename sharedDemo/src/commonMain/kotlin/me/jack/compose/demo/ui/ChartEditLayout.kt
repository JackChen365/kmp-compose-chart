package me.jack.compose.demo.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.jack.compose.chart.component.ChartComponent
import me.jack.compose.chart.context.ChartContext
import me.jack.compose.chart.context.ChartScrollState
import me.jack.compose.chart.context.ChartZoomState
import me.jack.compose.chart.context.isElementAvailable
import me.jack.compose.chart.context.requireChartScrollState
import me.jack.compose.chart.context.requireChartZoomState
import me.jack.compose.chart.measure.ChartContentMeasurePolicy
import me.jack.compose.chart.measure.withScrollMeasurePolicy
import me.jack.compose.chart.measure.withZoomableMeasurePolicy
import me.jack.compose.chart.scope.ChartAnchor
import me.jack.compose.chart.scope.ChartCombinedScope
import me.jack.compose.chart.scope.ChartScope
import me.jack.compose.chart.scope.SingleChartScope
import me.jack.compose.chart.scope.SingleChartScopeInstance
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ChartEditLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    gridCells: ChartGridCells = ChartGridCells.FixedSize(120.dp),
    chartContext: ChartContext = ChartContext,
    contentMeasurePolicy: ChartContentMeasurePolicy,
    chartComponents: List<ChartComponent<Any>>,
    components: @Composable ChartEditLayoutScope.() -> Unit,
    content: @Composable () -> Unit
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
    var childItemCount = 0
    var groupCount = 1
    chartComponents.forEach { component ->
        childItemCount = childItemCount.coerceAtLeast(component.chartDataset.size)
        groupCount = groupCount.coerceAtLeast(component.chartDataset.groupSize)
    }
    val chartComponentScopes = remember(chartComponents) {
        val componentScopes = mutableMapOf<ChartComponent<Any>, SingleChartScopeInstance<Any>>()
        chartComponents.forEach { chartComponent ->
            val singleChartScopeInstance = SingleChartScopeInstance(
                chartDataset = chartComponent.chartDataset,
                chartContext = chartContext,
                tapGestures = chartComponent.tapGestures,
                contentMeasurePolicy = rememberContentMeasurePolicy
            )
            componentScopes[chartComponent] = singleChartScopeInstance
        }
        componentScopes
    }
    val chartCombinedScope = remember {
        ChartCombinedScope(
            childItemCount = childItemCount,
            chartContext = chartContext,
            contentMeasurePolicy = rememberContentMeasurePolicy,
            groupCount = groupCount,
            chartScopes = chartComponentScopes.values.toList()
        )
    }
    val chartEditLayoutScope = remember {
        InternalChartEditLayoutScope(chartCombinedScope)
    }
    val density = LocalDensity.current
    Layout(
        modifier = modifier,
        content = {
            // Invoke content, here means chart.
            content()
            // Build component
            components(chartEditLayoutScope)
            // Use component, here means all chart component.
            chartEditLayoutScope.components.forEach { component ->
                component()
            }
        },
        measurePolicy = { measurables, constraints ->
            measureContent(
                density = density,
                measurables = measurables,
                constraints = constraints,
                contentPadding = contentPadding,
                gridCells = gridCells
            )
        }
    )
}

private fun MeasureScope.measureContent(
    density: Density,
    measurables: List<Measurable>,
    constraints: Constraints,
    contentPadding: PaddingValues,
    gridCells: ChartGridCells,
): MeasureResult {
    var maxHeight = 0f
    val placeables = arrayOfNulls<Placeable>(measurables.size)
    measurables.forEachIndexed { index, measurable: Measurable ->
        val chartEditLayoutParentData = measurable.chartEditLayoutParentData
        val placeable = if (null == chartEditLayoutParentData) {
            // content
            measurable.measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity
                )
            ).also {
                maxHeight += it.height
            }
        } else {
            // component
            var previewCellConstraint = constraints.copy(minWidth = 0, minHeight = 0)
            val horizontalPadding = contentPadding.calculateStartPadding(LayoutDirection.Ltr) +
                    contentPadding.calculateEndPadding(LayoutDirection.Ltr)
            val horizontalPaddingPx = with(density) { horizontalPadding.roundToPx() }
            if (gridCells is ChartGridCells.Fixed) {
                val gridCellWidth = (constraints.maxWidth - horizontalPaddingPx) / gridCells.count
                previewCellConstraint = constraints.copy(minWidth = 0, minHeight = 0, maxWidth = gridCellWidth)
            } else if (gridCells is ChartGridCells.FixedSize) {
                val gridCellWidth = with(density) { gridCells.size.toPx() }
                previewCellConstraint =
                    constraints.copy(minWidth = 0, minHeight = 0, maxWidth = gridCellWidth.roundToInt())
            }
            measurable.measure(previewCellConstraint)
        }
        placeables[index] = placeable
    }
    // preview
    val startPadding = contentPadding.calculateStartPadding(LayoutDirection.Ltr).toPx()
    val topPadding = contentPadding.calculateTopPadding().toPx()
    var startOffset = startPadding.roundToInt()
    var maxItemHeight = 0
    var topOffset = topPadding.roundToInt()
    val layoutWidth = (constraints.maxWidth - startPadding).roundToInt()
    placeables
        .filterNotNull()
        .slice(1 until placeables.size).forEachIndexed { index, placeable ->
            if (gridCells is ChartGridCells.Fixed) {
                startOffset += placeable.width
                maxItemHeight = max(maxItemHeight, placeable.height)
                if (gridCells.count == index + 1) {
                    startOffset = startPadding.roundToInt()
                    maxHeight += maxItemHeight
                }
            } else if (gridCells is ChartGridCells.FixedSize) {
                startOffset += placeable.width
                maxItemHeight = max(maxItemHeight, placeable.height)
                if (startOffset >= layoutWidth) {
                    startOffset = startPadding.roundToInt()
                    maxHeight += maxItemHeight
                }
            }
        }
    maxHeight += maxItemHeight
    return layout(constraints.maxWidth, maxHeight.roundToInt()) {
        startOffset = startPadding.roundToInt()
        topOffset = topPadding.roundToInt()
        placeables.filterNotNull().forEachIndexed { index, placeable ->
            if (0 == index) {
                // content
                placeable.place(
                    x = startPadding.roundToInt(),
                    y = topPadding.roundToInt()
                )
            } else {
                // preview
                val previewIndex = index - 1
                if (gridCells is ChartGridCells.Fixed) {
                    placeable.place(
                        x = startOffset,
                        y = topOffset
                    )
                    startOffset += placeable.width
                    if (gridCells.count == previewIndex + 1) {
                        startOffset = startPadding.roundToInt()
                        topOffset += placeable.height
                    }
                } else if (gridCells is ChartGridCells.FixedSize) {
                    placeable.place(
                        x = startOffset,
                        y = topOffset
                    )
                    startOffset += placeable.width
                    if (startOffset >= layoutWidth) {
                        startOffset = startPadding.roundToInt()
                        topOffset += placeable.height
                    }
                }
            }
        }
    }
}

inline fun <reified T> ChartEditLayoutScope.singleChartScopeComponent(
    modifier: Modifier = Modifier,
    crossinline content: @Composable SingleChartScope<T>.() -> Unit
) {
    val singleChartScope = chartScope.chartScopes.find {
        T::class.java.isAssignableFrom(it.chartDataset.datasetType)
    }
    if (null != singleChartScope) {
        @Suppress("UNCHECKED_CAST")
        addChartComponent {
            Box(modifier = modifier) {
                content.invoke(singleChartScope as SingleChartScope<T>)
            }
        }
    }
}

interface ChartEditLayoutScope {
    val chartScope: ChartCombinedScope
    val components: List<@Composable () -> Unit>

    fun chartScopeComponent(
        modifier: Modifier = Modifier,
        content: @Composable ChartScope.() -> Unit
    )

    fun addChartComponent(content: @Composable () -> Unit)

    fun Modifier.chartConstraintInfo(
        name: String,
        vararg chartConstraintAnchors: ChartAnchor
    ): Modifier {
        return this.then(ChartEditLayoutElement(name, chartConstraintAnchors))
    }
}

class InternalChartEditLayoutScope(
    override val chartScope: ChartCombinedScope
) : ChartEditLayoutScope {
    private val mutableComponents = mutableListOf<@Composable () -> Unit>()
    override val components: List<@Composable () -> Unit>
        get() = mutableComponents

    override fun chartScopeComponent(
        modifier: Modifier,
        content: @Composable ChartScope.() -> Unit
    ) {
        mutableComponents.add {
            var offsetX by remember {
                mutableStateOf(0f)
            }
            var offsetY by remember {
                mutableStateOf(0f)
            }
            var startDrag by remember {
                mutableStateOf(false)
            }
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .alpha(if (startDrag) 0.2f else 1f)
                ) {
                    content.invoke(chartScope)
                }
                Box(
                    modifier = Modifier.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }.alpha(if (startDrag) 1f else 0f)
                ) {
                    content.invoke(chartScope)
                }
            }
        }
    }

    override fun addChartComponent(content: @Composable () -> Unit) {
        mutableComponents.add(content)
    }
}

internal val IntrinsicMeasurable.chartEditLayoutParentData: ChartEditLayoutParentData?
    get() = parentData as? ChartEditLayoutParentData

/**
 * Parent data associated with children.
 */
internal data class ChartEditLayoutParentData(
    var name: String? = null,
    var chartConstraintAnchors: Array<out ChartAnchor> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartEditLayoutParentData

        if (name != other.name) return false
        return chartConstraintAnchors.contentEquals(other.chartConstraintAnchors)
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + chartConstraintAnchors.contentHashCode()
        return result
    }
}

internal class ChartEditLayoutElement(
    val name: String,
    val chartConstraintAnchors: Array<out ChartAnchor>
) : ModifierNodeElement<ChartEditLayoutParentDataModifierNode>() {
    override fun create(): ChartEditLayoutParentDataModifierNode {
        return ChartEditLayoutParentDataModifierNode(name, chartConstraintAnchors)
    }

    override fun update(node: ChartEditLayoutParentDataModifierNode) {
        node.name = name
        node.chartConstraintAnchors = chartConstraintAnchors
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "ChartEditLayoutElement"
        value = chartConstraintAnchors
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartEditLayoutElement

        if (name != other.name) return false
        return chartConstraintAnchors.contentEquals(other.chartConstraintAnchors)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + chartConstraintAnchors.contentHashCode()
        return result
    }


}

internal class ChartEditLayoutParentDataModifierNode(
    var name: String,
    var chartConstraintAnchors: Array<out ChartAnchor>
) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?): ChartEditLayoutParentData {
        return ((parentData as? ChartEditLayoutParentData) ?: ChartEditLayoutParentData()).also {
            it.name = name
            it.chartConstraintAnchors = chartConstraintAnchors
        }
    }
}


@Stable
interface ChartGridCells {

    class Fixed(val count: Int) : ChartGridCells {
        init {
            require(count > 0) { "Provided count $count should be larger than zero" }
        }

        override fun hashCode(): Int {
            return -count // Different sign from Adaptive.
        }

        override fun equals(other: Any?): Boolean {
            return other is Fixed && count == other.count
        }
    }


    class FixedSize(val size: Dp) : ChartGridCells {
        init {
            require(size > 0.dp) { "Provided size $size should be larger than zero." }
        }

        override fun hashCode(): Int {
            return size.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is FixedSize && size == other.size
        }
    }
}