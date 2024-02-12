package me.jack.compose.demo

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
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

fun ComponentActivity.initialAndDisplayDemoList(appName: String, block: DemoDslBuilder.DemoScope.() -> Unit) {
    val demoList = buildAppDemo(block)
    val rootCategory = DemoCategory(appName, demoList.toMutableList())
    buildActivityDemoList(rootCategory)
}

fun ComponentActivity.initialAndDisplayComposeClassDemoList(
    appName: String,
    vararg classArray: Class<*>
) {
    val demoList = buildComposableDemoList(*classArray)
    val rootCategory = DemoCategory(appName, demoList.toMutableList())
    buildActivityDemoList(rootCategory)
}

private fun ComponentActivity.buildActivityDemoList(rootCategory: DemoCategory) {
    val androidDemoNavigator = AndroidDemoNavigator(
        backDispatcher = onBackPressedDispatcher,
        rootDemo = rootCategory
    ) { navigator, demo ->
        //Going backward.
        navigator.backTo(demo)
    }
    setContent {
        val context = LocalContext.current
        AppDemo(androidDemoNavigator, androidDemoNavigator.currentDemo) { navigator, demo ->
            if (demo is ActivityComposableDemo<*>) {
                context.startActivity(Intent(context, demo.activityClass.java).also {
                    it.putExtra(
                        "title",
                        demo.title
                    )
                })
            } else {
                // Going forward
                navigator.navigateTo(demo)
            }
        }
    }
}

class AndroidDemoNavigator(
    private val rootDemo: Demo,
    private val backDispatcher: OnBackPressedDispatcher,
    private val backStack: ArrayDeque<Demo> = ArrayDeque(),
    private val onBackPressed: (AndroidDemoNavigator, Demo) -> Unit,
) : DemoNavigator {
    private var _currentDemo by mutableStateOf(rootDemo)
    override var currentDemo: Demo
        get() = _currentDemo
        private set(value) {
            _currentDemo = value
            onBackPressedCallback.isEnabled = !isRoot()
        }

    init {
        backStack.push(rootDemo)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            popBackStack()
        }
    }.apply {
        isEnabled = !isRoot()
        backDispatcher.addCallback(this)
    }

    override fun isRoot() = backStack.peek() == rootDemo

    override fun navigateTo(demo: Demo) {
        currentDemo = demo
        backStack.push(demo)
        onBackPressedCallback.isEnabled = !isRoot()
    }

    override fun backTo(demo: Demo) {
        currentDemo = demo
        onBackPressedCallback.isEnabled = !isRoot()
    }

    override fun popBackStack() {
        if (!backStack.isEmpty()) {
            backStack.pop()
            val demo = backStack.peek()
            onBackPressed(this, demo)
        }
        onBackPressedCallback.isEnabled = !isRoot()
    }
}

fun DemoDslBuilder.DemoScope.fragmentDemo(title: String, fragmentClass: KClass<Fragment>) {
    addDemo(FragmentComposableDemo(title, fragmentClass))
}

fun<T : Activity> DemoDslBuilder.DemoScope.activityDemo(title: String, activityClass: KClass<T>) {
    addDemo(ActivityComposableDemo(title, activityClass))
}

class FragmentComposableDemo<T : Fragment>(title: String, val fragmentClass: KClass<T>) : Demo(title)

class ActivityComposableDemo<T : Activity>(title: String, val activityClass: KClass<T>) : Demo(title)

fun<T : Fragment> DemoCategory.fragmentDemo(title: String, fragmentClass: KClass<T>) {
    val demoItem = FragmentComposableDemo(title, fragmentClass)
    addDemo(demoItem)
}

fun<T : Activity> DemoCategory.activityDemo(title: String, activityClass: KClass<T>) {
    val demoItem = ActivityComposableDemo(title, activityClass)
    addDemo(demoItem)
}
