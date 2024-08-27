package com.mstf.dragtoreveal

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import kotlin.math.pow


@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun DragToReveal(
    modifier: Modifier = Modifier,
    instructionBackgroundColor: Color = Color.Gray,
    hiddenContentBackgroundColor: Color = Color.LightGray,
    instructionTextColor: Color = Color.Black,
    instructionSwipingText: String,
    instructionReleaseText: String,
    maxRevealedLayoutHeight: Dp = 350.dp,
    revealingElasticityPower: Float = 3f,
    contentToReveal: @Composable () -> Unit,
    content: @Composable (LazyListState, ScrollState) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // a toggle for changing reveal state
    // shows that the user dragged enough to reveal the hidden content
    var revealStateToggle by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(revealStateToggle) {
        vibrate(revealStateToggle, vibrator)
    }

    var isContentRevealed by remember { mutableStateOf(false) }
    // the variable we control the scroll and resize logic with
    // after the hidden layout is revealed
    var isAfterRevealed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isTouchingScrollable by remember { mutableStateOf(false) }

    // height of the hidden layout while being dragged
    var revealingLayoutHeight by remember { mutableStateOf(0.dp) }

    val minHeightToReveal = remember { 75.dp }

    var contentToRevealHeight by remember { mutableStateOf(0.dp) }
    // an indicator that tells either the final height of the hidden layout is measured or not
    var isContentToRevealHeightMeasured by remember { mutableStateOf(false) }
    val contentToRevealAnimatedHeight by animateDpAsState(
        targetValue = revealingLayoutHeight,
        label = "hidden_content_height",
        finishedListener = {
            // set isAfterRevealed to true after the revealing animation is done,
            // so we can have the opening animation
            if (revealingLayoutHeight >= maxRevealedLayoutHeight) {
                isAfterRevealed = true
            }
        }
    )

    // Draw the content that needs to be revealed only once to measure the height (this can't be seen)
    if (!isContentToRevealHeightMeasured) {
        Box(modifier = Modifier
            // the maximum height considered for the revealed hidden layout
            .heightIn(min = 0.dp, max = maxRevealedLayoutHeight)
            .onSizeChanged {
                contentToRevealHeight = with(density) { it.height.toDp() }
                isContentToRevealHeightMeasured = true
            }) { contentToReveal() }
    }

    val animatedBackgroundColor by animateColorAsState(
        if (isContentRevealed) hiddenContentBackgroundColor else instructionBackgroundColor,
        label = "background_color",
    )

    // scrollable object states that are needed for checking if the scrollable objects can get scrolled or not
    val lazyListState = rememberLazyListState()
    val scrollState = rememberScrollState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dragAmount = available.y

                // whenever this scope is called it means user is touching over the scrollable view
                // first we check if isDragging is true so fling event won't be considered as touching
                if (isDragging) isTouchingScrollable = true

                // the amount of height that needs to be consumed because of the revealing
                val calculatedRevealingLayoutHeight: Dp

                if (isAfterRevealed) {
                    // when it is on after revealed state, we'll consume all the drag amount
                    // when it's totally revealed or totally hidden the we won't consume any amount
                    if (revealingLayoutHeight <= 0.dp) return Offset.Zero
                    if (revealingLayoutHeight >= maxRevealedLayoutHeight) return Offset.Zero

                    // calculating the amount we need to consume
                    // to prevent the scrollable layout from being scrolled
                    calculatedRevealingLayoutHeight = revealingLayoutHeight +
                            // after the hidden layout got revealed, no need elasticity on dragging
                            with(density) { dragAmount.toDp() }
                } else {
                    // if the hidden content is already revealed, don't need to start revealing process
                    if (isContentRevealed) return Offset.Zero
                    // to prevent revealing process with fling event
                    if (dragAmount >= 200) return Offset.Zero
                    // if the touching event is not dragging (for example it's a fling event),
                    // don't have to start revealing
                    if (!isDragging) return Offset.Zero
                    // if the scrollable is not at the starting position, don't have to start revealing process
                    if (lazyListState.canScrollBackward || scrollState.canScrollBackward) return Offset.Zero

                    // calculating the amount we need to consume
                    // to prevent the scrollable layout from being scrolled
                    calculatedRevealingLayoutHeight = revealingLayoutHeight +
                            with(density) {
                                dragAmount
                                    .div(revealingElasticityPower)
                                    .toDp()
                            }
                }

                return Offset(
                    x = 0f,
                    y =
                    if (calculatedRevealingLayoutHeight <= 0.dp) 0f
                    else dragAmount,
                )
            }
        }
    }

    val dragDetectionModifier = Modifier
        .nestedScroll(nestedScrollConnection)
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {

                        PointerEventType.Press -> {
                            isDragging = true
                        }

                        PointerEventType.Release -> {
                            isDragging = false
                            isTouchingScrollable = false

                            if (!isAfterRevealed) {
                                if (revealingLayoutHeight >= minHeightToReveal) {
                                    isContentRevealed = true
                                    revealingLayoutHeight = contentToRevealHeight
                                } else {
                                    isContentRevealed = false
                                    revealingLayoutHeight = 0.dp
                                }
                            }
                        }

                        PointerEventType.Move -> {
                            val dragAmount =
                                event.changes[0].let { it.position.y - it.previousPosition.y }

                            if (
                            // if the hidden content is already revealed,
                            // don't need to start revealing process
                                !isContentRevealed &&
                                // to prevent revealing process with fling event
                                dragAmount < 200 &&
                                // the drag event must come from a non-scrollable
                                (!isTouchingScrollable || (!lazyListState.canScrollBackward && !scrollState.canScrollBackward))
                            ) {
                                revealingLayoutHeight = (revealingLayoutHeight +
                                        with(density) {
                                            dragAmount
                                                .div(revealingElasticityPower)
                                                .toDp()
                                        }).coerceIn(0.dp, maxRevealedLayoutHeight)

                                if (
                                    dragAmount > 0 &&
                                    revealingLayoutHeight >= minHeightToReveal
                                ) {
                                    revealStateToggle = true
                                }

                                if (
                                    dragAmount < 0 &&
                                    revealingLayoutHeight < minHeightToReveal &&
                                    revealStateToggle != null
                                ) {
                                    revealStateToggle = false
                                }

                            } else if (isAfterRevealed) {
                                revealingLayoutHeight = (revealingLayoutHeight +
                                        // after the hidden layout got revealed, no need elasticity on dragging
                                        with(density) { dragAmount.toDp() })
                                    .coerceIn(0.dp, maxRevealedLayoutHeight)

                                // when the revealed layout is totally closed and hidden again
                                // we need to reset everything
                                if (revealingLayoutHeight <= 0.dp) {
                                    isAfterRevealed = false
                                    isContentRevealed = false
                                    revealStateToggle = null
                                }
                            }
                        }
                    }
                }
            }
        }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Content that needs to be revealed on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    if (isDragging || isAfterRevealed) revealingLayoutHeight
                    else contentToRevealAnimatedHeight
                )
                .background(animatedBackgroundColor),
            contentAlignment = if (isContentRevealed) Alignment.TopStart else Alignment.BottomCenter,
        ) {
            this@Column.AnimatedVisibility(
                visible = isContentRevealed,
                label = "hidden_content",
                enter = fadeIn(tween(400)),
                exit = fadeOut(),
            ) {
                contentToReveal()
            }

            if (!isContentRevealed) {
                // instruction text composable
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
                        color = instructionTextColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // The rest of the content that is always revealed
        Box(
            modifier = dragDetectionModifier
                .fillMaxWidth()
                .weight(1f),
        ) { content(lazyListState, scrollState) }
    }
}

private fun vibrate(revealStateToggle: Boolean?, vibrator: Vibrator) {
    if (revealStateToggle == null) return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    vibrator.vibrate(
        VibrationEffect.createOneShot(
            10,
            VibrationEffect.DEFAULT_AMPLITUDE,
        )
    )
}

fun powerCurveInterpolate(start: Float, end: Float, t: Float, power: Float): Float {
    return (start + (end - start) * t.pow(power))
}