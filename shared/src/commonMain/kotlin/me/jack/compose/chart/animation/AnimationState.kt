package me.jack.compose.chart.animation

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

/**
 * Fire-and-forget animation function for [Int]. This function is overloaded for
 * different parameter types such as [Float], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [ChartAnimateState.value] is changed, the animation will run automatically. If there
 * is already an animation in-flight when [ChartAnimateState.value] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 * You are not supposed to use this [ChartIntAnimateState] directly, we use it [ChartCanvas]
 *
 * ```
 * ChartCanvas(modifier = Modifier.fillMaxSize()){
 *      drawRect(
 *          color = Color.Red whenPressedAnimateTo Color.Blue,
 *          topLeft = Offset(x = 10f,y = 10f) whenPressedAnimateTo Offset(x = 100f,y = 10f),
 *          size = Size(100f,100f)
 *      )
 * }
 * ```
 */
fun intAnimateState(
    scope: CoroutineScope,
    initialValue: Int = 0,
    durationMillis: Int = DefaultDurationMillis
): ChartIntAnimateState {
    return ChartIntAnimateState(scope, initialValue, durationMillis)
}

fun floatAnimateState(
    scope: CoroutineScope,
    initialValue: Float = 0f,
    durationMillis: Int = DefaultDurationMillis
): ChartFloatAnimateState {
    return ChartFloatAnimateState(scope, initialValue, durationMillis)
}

fun colorAnimateState(
    scope: CoroutineScope,
    initialValue: Color = Color.Transparent,
    durationMillis: Int = DefaultDurationMillis
): ChartColorAnimateState {
    return ChartColorAnimateState(scope, initialValue, durationMillis)
}

fun sizeAnimateState(
    scope: CoroutineScope,
    initialValue: Size = Size.Zero,
    durationMillis: Int = DefaultDurationMillis
): ChartSizeAnimateState {
    return ChartSizeAnimateState(scope, initialValue, durationMillis)
}

fun offsetAnimateState(
    scope: CoroutineScope,
    initialValue: Offset = Offset.Zero,
    durationMillis: Int = DefaultDurationMillis
): ChartOffsetAnimateState {
    return ChartOffsetAnimateState(scope, initialValue, durationMillis)
}

fun intOffsetAnimateState(
    scope: CoroutineScope,
    initialValue: IntOffset = IntOffset.Zero,
    durationMillis: Int = DefaultDurationMillis
): ChartIntOffsetAnimateState {
    return ChartIntOffsetAnimateState(scope, initialValue, durationMillis)
}

fun dpAnimateState(
    scope: CoroutineScope,
    initialValue: Dp = 0.dp,
    durationMillis: Int = DefaultDurationMillis
): ChartDpAnimateState {
    return ChartDpAnimateState(scope, initialValue, durationMillis)
}

fun dpOffsetAnimateState(
    scope: CoroutineScope,
    initialValue: DpOffset = DpOffset.Zero,
    durationMillis: Int = DefaultDurationMillis
): ChartDpOffsetAnimateState {
    return ChartDpOffsetAnimateState(scope, initialValue, durationMillis)
}

fun spAnimateState(
    scope: CoroutineScope,
    initialValue: TextUnit = 0.sp,
    durationMillis: Int = DefaultDurationMillis
): ChartSpAnimateState {
    return ChartSpAnimateState(scope, initialValue, durationMillis)
}

/**
 * [ChartAnimateState] is a wrapper for Animatable
 * We can use it in non-composable function.
 * Besides, whenever we reset the [ChartAnimateState] by invoking the [ChartAnimateState.reset]
 * It will be safe to use to reuse.
 *
 * We use [isResetting] is because the [Animatable.snapTo] can not respond in time due to coroutine.
 * This means we will discard some of the target value while resetting.
 */
open class ChartAnimateState<T, V : AnimationVector>(
    private val scope: CoroutineScope,
    private val durationMillis: Int = DefaultDurationMillis,
    private var initialValue: T,
    typeConverter: TwoWayConverter<T, V>
) {
    private val animatable = Animatable(initialValue, typeConverter)
    private var isResetting = false
    val targetValue: T
        get() = animatable.targetValue

    val isRunning: Boolean
        get() = animatable.isRunning

    open var value: T
        set(value) {
            if (!isResetting && value != animatable.targetValue) {
                scope.launch {
                    animatable.animateTo(
                        targetValue = value,
                        animationSpec = tween(durationMillis = durationMillis),
                    )
                }
            }
        }
        get() {
            return if (isResetting) initialValue else animatable.value
        }

    fun reset(newInitialValue: T) {
        if (initialValue == newInitialValue) return
        this.isResetting = true
        this.initialValue = newInitialValue
        scope.launch {
            try {
                animatable.snapTo(newInitialValue)
            } finally {
                isResetting = false
            }
        }
    }

    fun snapTo(targetValue: T) {
        scope.launch {
            animatable.snapTo(targetValue)
        }
    }
}

class ChartFloatAnimateState(
    scope: CoroutineScope,
    initialValue: Float = 0F,
    durationMillis: Int = DefaultDurationMillis,
) : ChartAnimateState<Float, AnimationVector1D>(scope, durationMillis, initialValue, Float.VectorConverter) {
    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: Float
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): Float {
        return value
    }
}

class ChartColorAnimateState(
    scope: CoroutineScope,
    initialValue: Color = Color.Transparent,
    durationMillis: Int = DefaultDurationMillis,
) : ChartAnimateState<Color, AnimationVector4D>(
    scope,
    durationMillis,
    initialValue,
    (Color.VectorConverter)(initialValue.colorSpace)
) {
    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: Color
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): Color {
        return value
    }

}

class ChartIntAnimateState(
    scope: CoroutineScope,
    initialValue: Int = 0,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimateState<Int, AnimationVector1D>(scope, durationMillis, initialValue, Int.VectorConverter) {

    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: Int
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): Int {
        return value
    }
}

class ChartSizeAnimateState(
    scope: CoroutineScope,
    initialValue: Size = Size.Zero,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimateState<Size, AnimationVector2D>(scope, durationMillis, initialValue, Size.VectorConverter) {

    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: Size
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): Size {
        return value
    }
}

class ChartOffsetAnimateState(
    scope: CoroutineScope,
    initialValue: Offset = Offset.Zero,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimateState<Offset, AnimationVector2D>(scope, durationMillis, initialValue, Offset.VectorConverter) {

    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: Offset
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): Offset {
        return value
    }
}

class ChartIntOffsetAnimateState(
    scope: CoroutineScope,
    initialValue: IntOffset = IntOffset.Zero,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimateState<IntOffset, AnimationVector2D>(scope, durationMillis, initialValue, IntOffset.VectorConverter) {

    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: IntOffset
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): IntOffset {
        return value
    }
}

class ChartDpAnimateState(
    scope: CoroutineScope,
    initialValue: Dp = 0.dp,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimateState<Dp, AnimationVector1D>(scope, durationMillis, initialValue, Dp.VectorConverter) {

    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: Dp
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): Dp {
        return value
    }
}

class ChartDpOffsetAnimateState(
    scope: CoroutineScope,
    initialValue: DpOffset = DpOffset.Zero,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimateState<DpOffset, AnimationVector2D>(scope, durationMillis, initialValue, DpOffset.VectorConverter) {

    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: DpOffset
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): DpOffset {
        return value
    }
}

/**
 * A type converter that converts a [TextUnit] to a [AnimationVector1D], and vice versa.
 */
private val SpToVector: TwoWayConverter<TextUnit, AnimationVector1D> = TwoWayConverter(
    convertToVector = { AnimationVector1D(it.value) },
    convertFromVector = { TextUnit(it.value, TextUnitType.Sp) }
)

class ChartSpAnimateState(
    scope: CoroutineScope,
    initialValue: TextUnit = 0.sp,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimateState<TextUnit, AnimationVector1D>(scope, durationMillis, initialValue, SpToVector) {

    operator fun setValue(
        thisRef: Any?, property: KProperty<*>, value: TextUnit
    ) {
        this.value = value
    }

    operator fun getValue(
        thisRef: Any?, property: KProperty<*>
    ): TextUnit {
        return value
    }
}