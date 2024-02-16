import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.jack.compose.demo.AppMainView
import me.jack.compose.demo.BarDemos
import me.jack.compose.demo.BubbleDemos
import me.jack.compose.demo.CandleStickDemos
import me.jack.compose.demo.ChartThemeDemos
import me.jack.compose.demo.CombinedChartDemos
import me.jack.compose.demo.DonutDemos
import me.jack.compose.demo.DrawAnimationDemos
import me.jack.compose.demo.LineDemos
import me.jack.compose.demo.PieDemos

fun main() = application {
    Window(
        title = "ComposeChart",
        icon = painterResource("android_icon.png"),
        onCloseRequest = ::exitApplication
    ) {
        AppMainView(
            DrawAnimationDemos::class.java,
            BarDemos::class.java,
            LineDemos::class.java,
            BubbleDemos::class.java,
            DonutDemos::class.java,
            PieDemos::class.java,
            CandleStickDemos::class.java,
            CombinedChartDemos::class.java,
            ChartThemeDemos::class.java
        )
    }
}