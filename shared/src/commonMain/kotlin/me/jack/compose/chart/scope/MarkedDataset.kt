package me.jack.compose.chart.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap

@Composable
fun rememberMarkedChartDataset(): MarkedChartDataset {
    return remember {
        MarkedChartDataset()
    }
}

open class MarkedChartDataset {
    private val internalMutableMarkedDataset = SnapshotStateMap<Int, SnapshotStateMap<Int, Float>>()
    val dataset: Map<Int, Map<Int, Float>>
        get() = internalMutableMarkedDataset

    fun addMarkedData(chartGroupIndex: Int, vararg values: Pair<Int, Float>) {
        val snapshotStateMap = internalMutableMarkedDataset.getOrPut(chartGroupIndex) {
            mutableStateMapOf()
        }
        values.forEach { (key, value) ->
            snapshotStateMap[key] = value
        }
    }

    fun addMarkedData(chartGroupIndex: Int, index: Int, value: Float) {
        val snapshotStateMap = internalMutableMarkedDataset.getOrPut(chartGroupIndex) {
            mutableStateMapOf()
        }
        snapshotStateMap[index] = value
    }

    fun removeMarkedData(chartGroupIndex: Int) {
        internalMutableMarkedDataset.remove(chartGroupIndex)
    }

    fun removeMarkedData(chartGroupIndex: Int, index: Int) {
        val markedDataset = internalMutableMarkedDataset[chartGroupIndex]
        markedDataset?.remove(index)
        if (markedDataset.isNullOrEmpty()) {
            internalMutableMarkedDataset.remove(chartGroupIndex)
        }
    }

    fun getMarkedData(chartGroupIndex: Int, index: Int): Float {
        return internalMutableMarkedDataset[chartGroupIndex]?.get(index) ?: 0f
    }

    fun clearMarkedData() {
        internalMutableMarkedDataset.clear()
    }

    fun contains(chartGroupIndex: Int, index: Int): Boolean {
        return 0f != getMarkedData(chartGroupIndex, index)
    }
}