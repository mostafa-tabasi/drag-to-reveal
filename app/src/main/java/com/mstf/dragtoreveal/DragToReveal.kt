package com.mstf.dragtoreveal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp


@Composable
fun DragToReveal(
    modifier: Modifier = Modifier,
    contentToReveal: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var isContentRevealed by remember { mutableStateOf(false) }
    var revealingLayoutHeight by remember { mutableStateOf(0.dp) }
    var revealedLayoutHeight by remember { mutableStateOf(0.dp) }
    val minHeightToReveal = remember { 50.dp }

    Box(modifier = modifier
        .fillMaxSize()
        .pointerInput(true) {
            detectVerticalDragGestures(
                onDragStart = { offset: Offset ->
                    println("===onDragStart, offset: $offset")
                },
                onDragEnd = {
                    println("onDragEnd===")
                    if (revealingLayoutHeight >= minHeightToReveal) {
                        isContentRevealed = true
                    } else {
                        isContentRevealed = false
                        revealingLayoutHeight = 0.dp
                    }
                },
                onVerticalDrag = { _, dragAmount: Float ->
                    println("onDrag, dragAmount: $dragAmount")
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
                    revealedLayoutHeight = with(density) { it.height.toDp() }
                },
        ) { contentToReveal() }

        // The rest of the content that is always revealed
        Box(
            modifier = modifier
                .offset(y = revealedLayoutHeight),
        ) { content() }
    }
}