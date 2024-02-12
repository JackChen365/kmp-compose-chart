package me.jack.compose.demo

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import me.jack.compose.demo.builder.ComposableDemo
import me.jack.compose.demo.builder.Demo
import me.jack.compose.demo.builder.DemoCategory
import me.jack.compose.demo.builder.DemoNavigator
import me.jack.compose.demo.builder.HideTopBar

@Composable
fun AppDemo(
    navigator: DemoNavigator,
    demo: Demo,
    onItemClick: OnDemoItemClickListener
) {
    when (demo) {
        is DemoCategory -> {
            CategoryDemoPage(
                navigator = navigator,
                category = demo,
                onItemClick = onItemClick
            )
        }

        is ComposableDemo -> {
            DemoPage(navigator = navigator, demoItem = demo)
        }

        is FragmentComposableDemo<*> -> {
            FragmentDemoPage(navigator = navigator, demoItem = demo)
        }

        else -> Unit
    }
}

@Composable
private fun CategoryDemoPage(
    navigator: DemoNavigator,
    category: DemoCategory,
    onItemClick: OnDemoItemClickListener
) {
    AppPage(
        topBar = {
            if (navigator.isRoot()) {
                RootTopBar(category.title)
            } else {
                CategoryTopBar(navigator, category.title)
            }
        },
        navigator = navigator
    ) {
        Crossfade(category, label = "") { category ->
            LazyColumn(Modifier.padding(12.dp)) {
                items(category.demoList) { item ->
                    CategoryListItem(navigator, item, onItemClick)
                    Divider(color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
private fun CategoryListItem(
    navigator: DemoNavigator,
    demo: Demo,
    onItemClick: OnDemoItemClickListener
) {
    Row(modifier = Modifier
        .semantics { testTag = demo.title }
        .clickable(
            interactionSource = MutableInteractionSource(),
            indication = rememberRipple(color = Color.LightGray),
            onClick = {
                onItemClick.onClick(navigator, demo)
            }
        )
    ) {
        Text(
            text = demo.title,
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        )
        if (demo is DemoCategory) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(32.dp),
                tint = Color.LightGray,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun DemoPage(
    navigator: DemoNavigator,
    demoItem: ComposableDemo
) {
    AppPage(
        topBar = {
            if (navigator.isRoot()) {
                RootTopBar(demoItem.title)
            } else {
                CategoryTopBar(navigator, demoItem.title)
            }
        },
        navigator = navigator
    ) {
        demoItem.demo.invoke()
    }
}

@Composable
private fun FragmentDemoPage(
    navigator: DemoNavigator,
    demoItem: FragmentComposableDemo<*>
) {
    var showTopBar by remember {
        mutableStateOf(demoItem.fragmentClass.java.annotations.none { it is HideTopBar })
    }
    AppPage(
        topBar = {
            if (showTopBar) {
                if (navigator.isRoot()) {
                    RootTopBar(demoItem.title)
                } else {
                    CategoryTopBar(navigator, demoItem.title)
                }
            }
        },
        navigator = navigator
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).also {
                    it.id = android.R.id.home
                }
            }
        )
        val context = LocalContext.current
        DisposableEffect(demoItem) {
            val fragmentActivity = context.getFragmentActivity()
            val fm = fragmentActivity.supportFragmentManager
            val args = Bundle().apply {
                putString("title", demoItem.title)
            }
            fm.addFragmentOnAttachListener { _, fragment ->
                fragment.childFragmentManager.addOnBackStackChangedListener {
                    showTopBar = (0 == fragment.childFragmentManager.backStackEntryCount)
                }
            }
            fm.beginTransaction()
                .add(android.R.id.home, demoItem.fragmentClass.java, args, null)
                .commit()
            onDispose {
                fm.beginTransaction().remove(fm.findFragmentById(android.R.id.home)!!)
                    .commitAllowingStateLoss()
            }
        }
    }
}

@Composable
private fun AppPage(
    navigator: DemoNavigator,
    topBar: @Composable () -> Unit = {},
    content: @Composable (Demo) -> Unit
) {
    Scaffold(
        topBar = topBar
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content(navigator.currentDemo)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategoryTopBar(navigator: DemoNavigator, title: String) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = { navigator.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        },
        title = {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        },
        modifier = Modifier.shadow(8.dp),
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
    )
}

fun Context.getFragmentActivity(): FragmentActivity {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    error("Can not find the activity.")
}

fun interface OnDemoItemClickListener {
    fun onClick(navigator: DemoNavigator, demo: Demo)
}