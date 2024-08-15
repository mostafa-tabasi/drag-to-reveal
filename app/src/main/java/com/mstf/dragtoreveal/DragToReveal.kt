package com.mstf.dragtoreveal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.pow


@Composable
fun DragToReveal(
    instructionSwipingText: String,
    instructionReleaseText: String,
    modifier: Modifier = Modifier,
    contentToReveal: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    var isContentRevealed by remember { mutableStateOf(false) }

    var revealingLayoutHeight by remember { mutableStateOf(0.dp) }

    var revealedLayoutWidth by remember { mutableStateOf(0.dp) }
    var revealedLayoutHeight by remember { mutableStateOf(0.dp) }

    val minHeightToReveal = remember { 75.dp }

    Box(modifier = modifier
        .fillMaxSize()
        .pointerInput(true) {
            detectVerticalDragGestures(
                onDragEnd = {
                    if (revealingLayoutHeight >= minHeightToReveal) {
                        isContentRevealed = true
                    } else {
                        isContentRevealed = false
                        revealingLayoutHeight = 0.dp
                    }
                },
                onVerticalDrag = { _, dragAmount: Float ->
                    revealingLayoutHeight += dragAmount
                        .div(5)
                        .toDp()
                }
            )
        }) {

        // Content that needs to be revealed on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isContentRevealed) Modifier.wrapContentHeight()
                    else Modifier.height(revealingLayoutHeight)
                )
                .background(Color.Gray)
                .onSizeChanged {
                    revealedLayoutWidth = with(density) { it.width.toDp() }
                    revealedLayoutHeight = with(density) { it.height.toDp() }
                },
            contentAlignment = if (isContentRevealed) Alignment.TopStart else Alignment.BottomCenter,
        ) {
            if (isContentRevealed) contentToReveal()
            else {
                AnimatedContent(
                    targetState = if (revealingLayoutHeight < minHeightToReveal)
                        instructionSwipingText else instructionReleaseText,
                    transitionSpec = {
                        if (targetState == instructionReleaseText) {
                            // If the target text is the instruction release text, it slides up and fades in
                            // while the initial text (instruction swiping text) slides up and fades out.
                            slideInVertically { height -> height } + fadeIn(tween(500)) togetherWith
                                    slideOutVertically { height -> -height } + fadeOut(tween(200))
                        } else {
                            // If the target text is the instruction swiping text, it slides down and fades in
                            // while the initial text (instruction release text) slides down and fades out.
                            slideInVertically { height -> -height } + fadeIn(tween(500)) togetherWith
                                    slideOutVertically { height -> height } + fadeOut(tween(100))
                        }.using(
                            // Disable clipping since the faded slide-in/out should be displayed out of bounds.
                            SizeTransform(clip = false)
                        )
                    },
                    label = "instruction_text",
                ) { target ->
                    Text(
                        text = target,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = powerCurveInterpolate(
                                    0f,
                                    1f,
                                    (revealingLayoutHeight / minHeightToReveal.div(1.2f))
                                        .coerceIn(0f, 1f),
                                    4f,
                                )
                            },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // The rest of the content that is always revealed
        Box(
            modifier = modifier
                .offset(y = revealedLayoutHeight),
        ) { content() }
    }
}

fun powerCurveInterpolate(start: Float, end: Float, t: Float, power: Float): Float {
    return (start + (end - start) * t.pow(power))
}