package me.jack.compose.demo

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jack.compose.demo.builder.ComposableDemo
import me.jack.compose.demo.builder.Demo
import me.jack.compose.demo.builder.DemoCategory
import me.jack.compose.demo.builder.DemoNavigator

@Composable
fun AppMainView(
    vararg classArray: Class<*>
) {
    val demoList = buildComposableDemoList(*classArray)
    val rootCategory = DemoCategory("#", demoList.toMutableList())
    DesktopAppDemo(rootCategory)
}

@Composable
fun DesktopAppDemo(
    rootCategory: DemoCategory
) {
    Scaffold(
        topBar = {}
    ) {
        Row {
            var currentDemo by remember {
                val first = rootCategory.demoList.firstOrNull()
                mutableStateOf(first)
            }
            NavigationRail(
                modifier = Modifier.fillMaxHeight().verticalScroll(state = rememberScrollState())
            ) {
                val density = LocalDensity.current
                val chartIcons = remember {
                    mutableListOf(
                        "bar_chart.svg",
                        "line_chart.svg",
                        "bubble_chart.svg",
                        "donut_chart.svg",
                        "pie_chart2.svg",
                        "stock_chart.svg",
                        "table_chart_view.svg",
                        "palette.svg",
                        "finance.svg",
                        "stacked_bar_chart.svg",
                        "show_chart.svg"
                    )
                }
                rootCategory.demoList.forEachIndexed { index, demo ->
                    NavigationRailItem(
                        selected = currentDemo == demo,
                        icon = {
                            Icon(
                                painter = remember {
                                    useResource(chartIcons[index]) { loadSvgPainter(it, density) }
                                },
                                contentDescription = "Sample",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = demo.title,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                            )
                        },
                        alwaysShowLabel = false,
                        onClick = {
                            currentDemo = demo
                        }
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val demoList = remember(currentDemo) {
                    val demoList = mutableListOf<ComposableDemo>()
                    if (null != currentDemo) {
                        findDemoFromCategory(currentDemo!!, demoList)
                    }
                    demoList
                }
                val verticalScrollState = rememberScrollState()
                Column(
                    modifier = Modifier.verticalScroll(verticalScrollState)
                ) {
                    demoList.forEach { demo ->
                        Text(
                            text = demo.title,
                            modifier = Modifier
                                .background(color = Color.LightGray.copy(0.4f))
                                .fillMaxWidth().padding(8.dp)
                        )
                        Box(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, end = 36.dp)) {
                            demo.demo()
                        }
                    }

                }
                VerticalScrollbar(
                    modifier = Modifier.width(24.dp).align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(verticalScrollState)
                )
            }
        }
    }
}

private fun findDemoFromCategory(demo: Demo, demoList: MutableList<ComposableDemo>) {
    if (demo is DemoCategory) {
        demo.demoList.forEach { subDemo ->
            findDemoFromCategory(subDemo, demoList)
        }
    } else if (demo is ComposableDemo) {
        demoList.add(demo)
    }
}

@Composable
private fun RootTopBar(title: String) {
    TopAppBar(
        title = {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        },
        modifier = Modifier.shadow(4.dp),
        backgroundColor = MaterialTheme.colors.primary
    )
}

fun interface OnDemoItemClickListener {
    fun onClick(navigator: DemoNavigator, demo: Demo)
}