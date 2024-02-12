package me.jack.compose.demo.builder

import androidx.compose.runtime.Composable

/**
 * Generic demo with a [title] that will be displayed in the list of demos.
 */
open class Demo(val title: String) {
    override fun toString() = title
}

class ComposableDemo(title: String, var demo: (@Composable () -> Unit)) : Demo(title)

class DemoCategory(
    title: String,
    private val internalDemoList: MutableList<Demo> = mutableListOf()
) : Demo(title) {

    val demoList: List<Demo> = internalDemoList

    fun category(title: String, block: DemoCategory.() -> Unit) {
        val categoryItem = DemoCategory(title)
        categoryItem.apply(block)
        internalDemoList.add(categoryItem)
    }

    fun demo(title: String, block: @Composable () -> Unit) {
        val demoItem = ComposableDemo(title, block)
        internalDemoList.add(demoItem)
    }

    fun addDemo(demo: Demo) {
        internalDemoList.add(demo)
    }
}
