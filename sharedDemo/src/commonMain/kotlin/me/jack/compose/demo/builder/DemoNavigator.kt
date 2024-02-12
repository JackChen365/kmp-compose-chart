package me.jack.compose.demo.builder

interface DemoNavigator {
    val currentDemo: Demo

    fun isRoot(): Boolean

    fun navigateTo(demo: Demo)

    fun backTo(demo: Demo)

    fun popBackStack()
}