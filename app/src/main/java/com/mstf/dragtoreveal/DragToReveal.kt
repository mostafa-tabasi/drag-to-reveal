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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import kotlin.math.pow


/**
 * A Composable function that provides a swipe-down gesture to reveal hidden content.
 *
 * @param modifier A [Modifier] to be applied to the DragToReveal composable.
 * @param instructionBackgroundColor The background color of the instruction layout shown while
 * swiping down, before the hidden content is revealed. Default is [Color.LightGray].
 * @param revealedContentBackgroundColor The background color of the content to reveal after
 * sufficient swiping down. Default is [Color.LightGray] with 50% opacity.
 * @param instructionTextColor The color of the text in the instruction layout. Default is [Color.Black].
 * @param instructionSwipingText The instruction text shown while swiping down but before the hidden
 * content is revealed. Default is "Swipe down to reveal".
 * @param instructionReleaseText The instruction text shown when the user has swiped down far enough
 * to reveal the hidden content. Default is "Release to reveal".
 * @param minDragHeightToReveal The minimum drag height in dp required to reveal the hidden content.
 * This value must be between 50.dp and 600.dp. Default is 75.dp.
 * @param maxRevealedLayoutHeight The maximum height in dp for the revealed layout after the swipe
 * is complete. This value must be between 50.dp and 600.dp. Default is 350.dp.
 * @param dragElasticityLevel The elasticity level of the swipe gesture. Higher values make swiping
 * down harder. This value must be more than or equal 1f. Default is 4f.
 * @param contentToReveal A composable that is hidden by default and revealed after swiping
 * down sufficiently.
 * @param content A composable that is always visible. It must have a [Column] or [LazyColumn] as
 * the parent composable. The function provides a [LazyListState] and [ScrollState] to be used
 * depending on the parent composable.
 * @param onRevealStateChange A callback function that triggers when the revealing state changes
 * (true if content is revealed, false otherwise). Default is an empty function.
 */
@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun DragToReveal(
    modifier: Modifier = Modifier,
    instructionBackgroundColor: Color = Color.LightGray,
    revealedContentBackgroundColor: Color = Color.LightGray.copy(alpha = 0.5f),
    instructionTextColor: Color = Color.Black,
    instructionSwipingText: String = "Swipe down to reveal",
    instructionReleaseText: String = "Release to reveal",
    minDragHeightToReveal: Dp = 75.dp,
    maxRevealedLayoutHeight: Dp = 350.dp,
    dragElasticityLevel: Float = 4f,
    contentToReveal: @Composable () -> Unit,
    content: @Composable (LazyListState, ScrollState) -> Unit,
    onRevealStateChange: (Boolean) -> Unit = {},
) {
    require(minDragHeightToReveal >= 50.dp && minDragHeightToReveal <= 600.dp) {
        "The amount of drag to reveal the content must be between 50dp and 600dp."
    }
    require(maxRevealedLayoutHeight >= 50.dp && maxRevealedLayoutHeight <= 600.dp) {
        "The amount of height for revealed content must be between 50dp and 600dp."
    }
    require(dragElasticityLevel >= 1f) {
        "Minimum amount of drag elasticity must be 1."
    }

    val context = LocalContext.current
    val density = LocalDensity.current

    val vibrator = remember { vibratorService(context) }

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

    var contentToRevealHeight by remember { mutableStateOf(0.dp) }
    // an indicator that tells either the final height of the hidden layout is measured or not
    var isContentToRevealHeightMeasured by remember { mutableStateOf(false) }
    val contentToRevealAnimatedHeight by animateDpAsState(
        targetValue = revealingLayoutHeight,
        label = "hidden_content_height",
        finishedListener = {
            // set isAfterRevealed to true after the revealing animation is done,
            // so we can have the opening animation
            isAfterRevealed = isContentRevealed
        }
    )

    val animatedBackgroundColor by animateColorAsState(
        if (isContentRevealed) revealedContentBackgroundColor else instructionBackgroundColor,
        label = "background_color",
    )

    LaunchedEffect(isContentRevealed) {
        onRevealStateChange(isContentRevealed)
    }

    // scrollable object states that are needed for checking if the scrollable objects can get scrolled or not
    val lazyListState = rememberLazyListState()
    val scrollState = rememberScrollState()

    // Draw the content that needs to be revealed only once to measure the height (this can't be seen)
    if (!isContentToRevealHeightMeasured) {
        HiddenLayoutForMeasuring(
            maxRevealedLayoutHeight,
            contentToReveal,
            onSizeChanged = {
                contentToRevealHeight = with(density) { it.height.toDp() }
                isContentToRevealHeightMeasured = true
            }
        )
    }

    val nestedScrollConnection = remember(key1 = dragElasticityLevel) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dragAmount = available.y

                // whenever this scope is called it means user is touching over the scrollable view
                // first we check if isDragging is true so fling event won't be considered as touching
                if (isDragging) isTouchingScrollable = true

                // the amount of height that needs to be consumed because of the revealing
                val calculatedRevealingLayoutHeight: Dp

                if (isAfterRevealed) {
                    // when it is on after revealed state, we'll consume all the drag amount.
                    // when it's totally revealed or totally hidden, then we won't consume any amount
                    if (revealingLayoutHeight <= 0.dp) return Offset.Zero
                    if (revealingLayoutHeight >= contentToRevealHeight) return Offset.Zero

                    // calculating the amount we need to consume
                    // to prevent the scrollable layout from being scrolled
                    calculatedRevealingLayoutHeight = revealingLayoutHeight +
                            // after the hidden layout got revealed, no need elasticity on dragging
                            with(density) { dragAmount.toDp() }
                } else {
                    // if the hidden content is already revealed, don't need to start revealing process
                    if (isContentRevealed) return Offset.Zero
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
                                    .div(dragElasticityLevel)
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

    var skipDragEventCounter by remember { mutableIntStateOf(0) }
    val dragDetectionModifier = remember(
        keys = arrayOf(
            dragElasticityLevel,
            minDragHeightToReveal,
        ),
    ) {
        Modifier
            .nestedScroll(nestedScrollConnection)
            .pointerInput(
                keys = arrayOf(
                    dragElasticityLevel,
                    minDragHeightToReveal,
                )
            ) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {

                            PointerEventType.Press -> {
                                isDragging = true
                            }

                            PointerEventType.Release -> {
                                skipDragEventCounter = 0
                                isDragging = false
                                isTouchingScrollable = false

                                if (!isAfterRevealed) {
                                    if (revealingLayoutHeight >= minDragHeightToReveal) {
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
                                    // the drag event must come from a non-scrollable
                                    (!isTouchingScrollable || (!lazyListState.canScrollBackward && !scrollState.canScrollBackward))
                                ) {
                                    // we skip the first 10 drag event,
                                    // to prevent revealing process with fling event
                                    if (skipDragEventCounter < 10) {
                                        skipDragEventCounter++
                                    } else {
                                        //start the revealing process
                                        revealingLayoutHeight = (revealingLayoutHeight +
                                                with(density) {
                                                    dragAmount
                                                        .div(dragElasticityLevel)
                                                        .toDp()
                                                }).coerceIn(0.dp, minDragHeightToReveal + 50.dp)

                                        if (
                                            dragAmount > 0 &&
                                            revealingLayoutHeight >= minDragHeightToReveal
                                        ) {
                                            revealStateToggle = true
                                        }

                                        if (
                                            dragAmount < 0 &&
                                            revealingLayoutHeight < minDragHeightToReveal &&
                                            revealStateToggle != null
                                        ) {
                                            revealStateToggle = false
                                        }
                                    }

                                } else if (isAfterRevealed) {
                                    revealingLayoutHeight = (revealingLayoutHeight +
                                            // after the hidden layout got revealed, no need elasticity on dragging
                                            with(density) { dragAmount.toDp() })
                                        .coerceIn(0.dp, contentToRevealHeight)

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
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                contentToReveal()
            }

            if (!isContentRevealed) {
                InstructionTextComposable(
                    swipingText = instructionSwipingText,
                    releaseText = instructionReleaseText,
                    textColor = instructionTextColor,
                    textVisibilityAlpha = powerCurveInterpolate(
                        0f,
                        1f,
                        (revealingLayoutHeight / minDragHeightToReveal.div(1.2f))
                            .coerceIn(0f, 1f),
                        4f,
                    ),
                    hasDraggedEnough = revealStateToggle == true,
                )
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

private fun vibratorService(context: Context) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

@Composable
private fun HiddenLayoutForMeasuring(
    maxRevealedLayoutHeight: Dp,
    contentToReveal: @Composable () -> Unit,
    onSizeChanged: (IntSize) -> Unit,
) {
    Box(modifier = Modifier
        // the maximum height considered for the revealed hidden layout
        .heightIn(min = 0.dp, max = maxRevealedLayoutHeight)
        .onSizeChanged { onSizeChanged(it) }
    ) { contentToReveal() }
}

@Composable
private fun InstructionTextComposable(
    swipingText: String,
    releaseText: String,
    textColor: Color,
    textVisibilityAlpha: Float,
    hasDraggedEnough: Boolean,
) {
    AnimatedContent(
        targetState = if (hasDraggedEnough) releaseText else swipingText,
        transitionSpec = {
            if (targetState == releaseText) {
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
                    alpha = textVisibilityAlpha
                },
            textAlign = TextAlign.Center,
            color = textColor,
            fontWeight = FontWeight.Bold,
        )
    }
}