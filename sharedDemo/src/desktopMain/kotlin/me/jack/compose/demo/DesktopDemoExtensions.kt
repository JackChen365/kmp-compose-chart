package me.jack.compose.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.jack.compose.demo.builder.ComposableDemoBuilder
import me.jack.compose.demo.builder.Demo
import me.jack.compose.demo.builder.DemoCategory
import me.jack.compose.demo.builder.DemoDslBuilder
import me.jack.compose.demo.builder.DemoNavigator
import java.util.ArrayDeque
import kotlin.reflect.KClass

/**
 * Build the demo list by DSL
 */
fun buildAppDemo(block: DemoDslBuilder.DemoScope.() -> Unit): List<Demo> {
    val builder = DemoDslBuilder()
    return builder.buildDemoList(block)
}

/**
 * Build the demo list by composable annotation
 */
fun buildComposableDemoList(vararg classArray: Class<*>): List<Demo> {
    val builder = ComposableDemoBuilder()
    return builder.buildDemoList(classArray.toList())
}

