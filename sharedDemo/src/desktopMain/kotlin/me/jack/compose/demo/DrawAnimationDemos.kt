package me.jack.compose.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import me.jack.compose.chart.draw.AnimateCanvas

class DrawAnimationDemos {
    @Composable
    fun DrawElementDemo() {
        val image = useResource("swipe_down.png") { loadImageBitmap(it) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            val textMeasurer = rememberTextMeasurer()
            AnimateCanvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCircle(
                    color = Color.Red whenPressedAnimateTo Color.Red.copy(0.4f),
                    center = Offset(x = 60.dp.toPx(), y = 60.dp.toPx()),
                    radius = 40.dp.toPx() whenPressedAnimateTo 48.dp.toPx()
                )
                drawCircle(
                    color = Color.Blue whenPressedAnimateTo Color.Yellow.copy(0.4f),
                    center = Offset(x = 80.dp.toPx(), y = 80.dp.toPx()),
                    radius = 40.dp.toPx() whenPressedAnimateTo 42.dp.toPx()
                )
                drawRect(
                    color = Color.Red whenPressedAnimateTo Color.Red.copy(0.4f),
                    topLeft = Offset(
                        x = 120.dp.toPx(),
                        y = 40.dp.toPx() whenPressedAnimateTo 60.dp.toPx()
                    ),
                    size = DpSize(40.dp, 40.dp).toSize()
                )
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(
                        x = 180.dp.toPx(),
                        y = 20.dp.toPx()
                    ),
                    size = DpSize(80.dp, 80.dp).toSize(),
                    style = Stroke(1f)
                )
                drawArc(
                    color = Color.Red whenPressedAnimateTo Color.Red.copy(0.4f),
                    startAngle = 0f,
                    sweepAngle = 60f,
                    topLeft = Offset(
                        x = 180.dp.toPx(),
                        y = 20.dp.toPx()
                    ),
                    useCenter = true,
                    size = DpSize(80.dp, 80.dp).toSize() whenPressedAnimateTo DpSize(100.dp, 100.dp).toSize()
                )
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(
                        x = 260.dp.toPx(),
                        y = 20.dp.toPx()
                    ),
                    size = DpSize(80.dp, 80.dp).toSize(),
                    style = Stroke(1f)
                )
                drawArc(
                    color = Color.Red whenPressedAnimateTo Color.Red.copy(0.4f),
                    startAngle = 0f,
                    sweepAngle = 60f,
                    topLeft = Offset(
                        x = 260.dp.toPx(),
                        y = 20.dp.toPx()
                    ),
                    useCenter = false,
                    size = DpSize(80.dp, 80.dp).toSize() whenPressedAnimateTo DpSize(100.dp, 100.dp).toSize(),
                    style = Stroke(32.dp.toPx())
                )
                drawLine(
                    color = Color.Blue whenPressedAnimateTo Color.Green,
                    start = Offset(x = 360.dp.toPx(), y = 60.dp.toPx()),
                    end = Offset(x = 520.dp.toPx(), y = 60.dp.toPx()) whenPressedAnimateTo
                            Offset(x = 540.dp.toPx(), y = 80.dp.toPx()),
                    strokeWidth = 20.dp.toPx()
                )
                drawLine(
                    color = Color.Blue whenPressedAnimateTo Color.Red,
                    start = Offset(x = 540.dp.toPx(), y = 20.dp.toPx()),
                    end = Offset(x = 680.dp.toPx(), y = 180.dp.toPx()),
                    strokeWidth = 20.dp.toPx() whenPressedAnimateTo 32.dp.toPx()
                )
                val textLayoutResult = textMeasurer.measure(
                    text = "Hello world",
                    style = TextStyle(
                        color = Color.Blue whenPressedAnimateTo Color.Yellow,
                        fontSize = 20.sp whenPressedAnimateTo 32.sp
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(x = 480.dp.toPx(), y = 120.dp.toPx()) whenPressedAnimateTo
                            Offset(x = 520.dp.toPx(), y = 120.dp.toPx())
                )
                drawRect(
                    color = Color.Red,
                    Offset(x = 480.dp.toPx(), y = 120.dp.toPx()),
                    textLayoutResult.size.toSize(),
                    style = Stroke(1f)
                )

                drawRoundRect(
                    color = Color.Blue whenPressedAnimateTo Color.Yellow,
                    Offset(
                        x = 120.dp.toPx(),
                        y = 120.dp.toPx()
                    ),
                    size = DpSize(120.dp, 120.dp).toSize(),
                    cornerRadius = CornerRadius(36.dp.toPx(), 24.dp.toPx())
                )

                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(
                        x = 120.dp.toPx(),
                        y = 120.dp.toPx()
                    ),
                    size = DpSize(120.dp, 120.dp).toSize(),
                    style = Stroke(1f)
                )

                drawOval(
                    color = Color.Red whenPressedAnimateTo Color.Yellow,
                    topLeft = Offset(
                        x = 260.dp.toPx(),
                        y = 120.dp.toPx()
                    ),
                    size = DpSize(80.dp, 120.dp).toSize(),
                )

                drawOval(
                    color = Color.Red,
                    topLeft = Offset(
                        x = 260.dp.toPx(),
                        y = 120.dp.toPx()
                    ),
                    size = DpSize(80.dp, 120.dp).toSize(),
                    style = Stroke(1f)
                )

                drawImage(
                    image = image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstOffset = Offset(
                        x = 340.dp.toPx(),
                        y = 120.dp.toPx()
                    ).round() whenPressedAnimateTo Offset(
                        x = 360.dp.toPx(),
                        y = 120.dp.toPx()
                    ).round(),
                    dstSize = IntSize(80.dp.roundToPx(), 80.dp.roundToPx()),
                    alpha = 1f whenPressedAnimateTo 0.2f,
                    colorFilter = ColorFilter.tint(color = Color.Red)
                )

                drawRect(
                    color = Color.Red,
                    topLeft = Offset(20.dp.toPx(), 260.dp.toPx()),
                    size = Size(200.dp.toPx(), 200.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
                clickableGroup(
                    topLeft = Offset(20.dp.toPx(), 260.dp.toPx()),
                    size = Size(200.dp.toPx(), 200.dp.toPx())
                ) {
                    drawCircle(
                        color = Color.Red whenPressedAnimateTo Color.Yellow,
                        center = Offset(80.dp.toPx(), 320.dp.toPx()),
                        radius = 40.dp.toPx()
                    )
                    drawCircle(
                        color = Color.Red whenPressedAnimateTo Color.Yellow,
                        center = Offset(160.dp.toPx(), 320.dp.toPx()),
                        radius = 40.dp.toPx()
                    )
                }
            }
        }
    }
}