package me.jack.compose.demo.builder

interface DemoBuilder<T> {
    fun buildDemoList(demoList: T): List<Demo>
}