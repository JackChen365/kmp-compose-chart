package me.jack.compose.demo.builder

import androidx.compose.runtime.Composable

class DemoDslBuilder : DemoBuilder<DemoDslBuilder.DemoScope.() -> Unit> {
    override fun buildDemoList(block: DemoScope.() -> Unit): List<Demo> {
        val demoScope = DemoScope()
        demoScope.apply(block)
        return demoScope.getCategoryList()
    }

    class DemoScope {
        private val internalCategoryList: MutableList<Demo> = mutableListOf()

        fun getCategoryList(): List<Demo> = internalCategoryList

        fun demo(title: String, block: @Composable () -> Unit) {
            val demoItem = ComposableDemo(title, block)
            internalCategoryList.add(demoItem)
        }

        fun category(title: String, block: DemoCategory.() -> Unit) {
            val categoryItem = DemoCategory(title)
            categoryItem.apply(block)
            internalCategoryList.add(categoryItem)
        }

        fun addDemo(demo: Demo){
            internalCategoryList.add(demo)
        }
    }
}