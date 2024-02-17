package me.jack.compose.demo

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jack.compose.demo.builder.ComposableDemo
import me.jack.compose.demo.builder.ComposableDemoBuilder
import me.jack.compose.demo.builder.Demo
import me.jack.compose.demo.builder.DemoCategory

fun ComponentActivity.appMainView(
    vararg classArray: Class<*>
) {
    val builder = ComposableDemoBuilder()
    val demoList = builder.buildDemoList(classArray.toList())
    val rootCategory = DemoCategory("#", demoList.toMutableList())
    setContent {
        AndroidAppDemo(rootCategory)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DemoTopBar(title: String) {
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
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun AndroidAppDemo(
    rootCategory: DemoCategory
) {
    Scaffold(
        topBar = {}
    ) { paddings ->
        Column(modifier = Modifier.padding(paddings)) {
            var currentDemo by remember {
                val first = rootCategory.demoList.firstOrNull()
                mutableStateOf(first)
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                val demoList = remember(currentDemo) {
                    val demoList = mutableListOf<ComposableDemo>()
                    if (null != currentDemo) {
                        findDemoFromCategory(currentDemo!!, demoList)
                    }
                    demoList
                }
                Column(
                    modifier = Modifier
                ) {
                    if (demoList.isNotEmpty()) {
                        DemoTopBar(demoList.first().title)
                    }
                    val verticalScrollState = rememberScrollState()
                    Column (modifier = Modifier.verticalScroll(verticalScrollState)){
                        demoList.forEach { demo ->
                            Text(
                                text = demo.title,
                                modifier = Modifier
                                    .background(color = Color.LightGray.copy(0.4f))
                                    .fillMaxWidth().padding(8.dp)
                            )
                            Box(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)) {
                                demo.demo()
                            }
                        }
                    }
                }
            }
            NavigationBar(
                modifier = Modifier.horizontalScroll(state = rememberScrollState())
            ) {
                val chartIcons = remember {
                    mutableListOf(
                        R.drawable.bar_chart,
                        R.drawable.line_chart,
                        R.drawable.bubble_chart,
                        R.drawable.donut_chart,
                        R.drawable.pie_chart2,
                        R.drawable.stock_chart,
                        R.drawable.table_chart_view,
                        R.drawable.finance,
                        R.drawable.palette,
                        R.drawable.stacked_bar_chart,
                        R.drawable.show_chart
                    )
                }
                rootCategory.demoList.forEachIndexed { index, demo ->
                    NavigationRailItem(
                        modifier = Modifier.width(60.dp),
                        selected = currentDemo == demo,
                        icon = {
                            Icon(
                                painter = painterResource(chartIcons[index]),
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

@OptIn(ExperimentalMaterial3Api::class)
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
        modifier = Modifier.shadow(4.dp)
    )
}