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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

/**
 * Fire-and-forget animation function for [Int]. This function is overloaded for
 * different parameter types such as [Float], [Color][androidx.compose.ui.graphics.Color], [Offset],
 * etc. When the provided [ChartAnimatableState.value] is changed, the animation will run automatically. If there
 * is already an animation in-flight when [ChartAnimatableState.value] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 * You are not supposed to use this [ChartIntAnimatableState] directly, we use it [ChartCanvas]
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
fun intAnimatableState(
    scope: CoroutineScope,
    initialValue: Int = 0,
    durationMillis: Int = DefaultDurationMillis
): ChartIntAnimatableState {
    return ChartIntAnimatableState(scope, initialValue, durationMillis)
}

fun floatAnimatableState(
    scope: CoroutineScope,
    initialValue: Float = 0f,
    durationMillis: Int = DefaultDurationMillis
): ChartFloatAnimatableState {
    return ChartFloatAnimatableState(scope, initialValue, durationMillis)
}

fun colorAnimatableState(
    scope: CoroutineScope,
    initialValue: Color = Color.Transparent,
    durationMillis: Int = DefaultDurationMillis
): ChartColorAnimatableState {
    return ChartColorAnimatableState(scope, initialValue, durationMillis)
}

fun sizeAnimatableState(
    scope: CoroutineScope,
    initialValue: Size = Size.Zero,
    durationMillis: Int = DefaultDurationMillis
): ChartSizeAnimatableState {
    return ChartSizeAnimatableState(scope, initialValue, durationMillis)
}

fun offsetAnimatableState(
    scope: CoroutineScope,
    initialValue: Offset = Offset.Zero,
    durationMillis: Int = DefaultDurationMillis
): ChartOffsetAnimatableState {
    return ChartOffsetAnimatableState(scope, initialValue, durationMillis)
}

/**
 * [ChartAnimatableState] is a wrapper for Animatable
 * We can use it in non-composable function.
 * Besides, whenever we reset the [ChartAnimatableState] by invoking the [ChartAnimatableState.reset]
 * It will be safe to use to reuse.
 *
 * We use [isResetting] is because the [Animatable.snapTo] can not respond in time due to coroutine.
 * This means we will discard some of the target value while resetting.
 */
open class ChartAnimatableState<T, V : AnimationVector>(
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

class ChartFloatAnimatableState(
    scope: CoroutineScope,
    initialValue: Float = 0F,
    durationMillis: Int = DefaultDurationMillis,
) : ChartAnimatableState<Float, AnimationVector1D>(scope, durationMillis, initialValue, Float.VectorConverter) {
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

class ChartColorAnimatableState(
    scope: CoroutineScope,
    initialValue: Color = Color.Transparent,
    durationMillis: Int = DefaultDurationMillis,
) : ChartAnimatableState<Color, AnimationVector4D>(
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

class ChartIntAnimatableState(
    scope: CoroutineScope,
    initialValue: Int = 0,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimatableState<Int, AnimationVector1D>(scope, durationMillis, initialValue, Int.VectorConverter) {

    operator fun ChartIntAnimatableState.setValue(
        thisRef: Any?, property: KProperty<*>, value: Int
    ) {
        this.value = value
    }

    operator fun ChartIntAnimatableState.getValue(
        thisRef: Any?, property: KProperty<*>
    ): Int {
        return value
    }
}

class ChartSizeAnimatableState(
    scope: CoroutineScope,
    initialValue: Size = Size.Zero,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimatableState<Size, AnimationVector2D>(scope, durationMillis, initialValue, Size.VectorConverter) {

    operator fun ChartSizeAnimatableState.setValue(
        thisRef: Any?, property: KProperty<*>, value: Size
    ) {
        this.value = value
    }

    operator fun ChartSizeAnimatableState.getValue(
        thisRef: Any?, property: KProperty<*>
    ): Size {
        return value
    }
}

class ChartOffsetAnimatableState(
    scope: CoroutineScope,
    initialValue: Offset = Offset.Zero,
    durationMillis: Int = DefaultDurationMillis
) : ChartAnimatableState<Offset, AnimationVector2D>(scope, durationMillis, initialValue, Offset.VectorConverter) {

    operator fun ChartOffsetAnimatableState.setValue(
        thisRef: Any?, property: KProperty<*>, value: Offset
    ) {
        this.value = value
    }

    operator fun ChartOffsetAnimatableState.getValue(
        thisRef: Any?, property: KProperty<*>
    ): Offset {
        return value
    }
}