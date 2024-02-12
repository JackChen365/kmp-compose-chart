package me.jack.compose.chart

import android.os.Bundle
import androidx.activity.ComponentActivity
import me.jack.compose.demo.BarDemos
import me.jack.compose.demo.BubbleDemos
import me.jack.compose.demo.CandleStickDemos
import me.jack.compose.demo.CombinedChartDemos
import me.jack.compose.demo.DonutDemos
import me.jack.compose.demo.LineDemos
import me.jack.compose.demo.PieDemos
import me.jack.compose.demo.initialAndDisplayComposeClassDemoList

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialAndDisplayComposeClassDemoList(
            getString(R.string.app_name),
            BarDemos::class.java,
            LineDemos::class.java,
            BubbleDemos::class.java,
            DonutDemos::class.java,
            PieDemos::class.java,
            CandleStickDemos::class.java,
            CombinedChartDemos::class.java
        )
    }
}