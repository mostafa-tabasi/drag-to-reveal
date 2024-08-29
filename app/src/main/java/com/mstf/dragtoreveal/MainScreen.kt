package com.mstf.dragtoreveal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainScreen(innerPadding: PaddingValues, viewModel: MainViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .background(color = Color.LightGray.copy(alpha = 0.2f))
            .padding(
                top = innerPadding.calculateTopPadding(),
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val state by viewModel.uiState.collectAsState()

        AdjustmentComposable(
            description = "Min drag amount to reveal: ${state.minDragAmountToReveal.value.toInt()}dp",
            sliderValue = state.minDragAmountToReveal.value,
            onValueChange = { viewModel.setMinDragAmount(it) },
            steps = 149,
            valueRange = 50f..200f,
            enabled = !state.isContentRevealed,
        )

        AdjustmentComposable(
            description = "Drag elasticity level: ${state.dragElasticityLevel.toInt()}",
            sliderValue = state.dragElasticityLevel,
            onValueChange = { viewModel.setElasticityLevel(it) },
            steps = 8,
            valueRange = 1f..10f,
            enabled = !state.isContentRevealed,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            DragToReveal(
                instructionTextColor = Color.White,
                instructionBackgroundColor = Color.Gray,
                revealedContentBackgroundColor = Color.LightGray,
                dragElasticityLevel = state.dragElasticityLevel,
                minDragHeightToReveal = state.minDragAmountToReveal,
                contentToReveal = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "A placeholder compose used to feel a space",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            fontSize = 15.sp,
                        )
                        LazyRow(
                            state = rememberLazyListState(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(count = 10,
                                itemContent = {
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(150.dp)
                                            .padding(horizontal = 8.dp)
                                            .clip(shape = RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.5f)),
                                    )
                                })
                        }
                    }
                },
                content = { lazyListState, scrollState ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(),
                        state = lazyListState,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)
                    ) {
                        items(
                            count = 20,
                            itemContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(75.dp)
                                        .padding(8.dp)
                                        .background(Color.White, shape = RoundedCornerShape(8.dp)),
                                )
                            }
                        )
                    }
                },
                onRevealStateChange = { viewModel.toggleRevealState(it) }
            )

            if (!state.isContentRevealed) DashedLineComposable(state.minDragAmountToReveal)
        }

    }
}

@Composable
private fun DashedLineComposable(minDragAmountToReveal: Dp) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(
            color = Color.Red,
            start = Offset(
                0f,
                minDragAmountToReveal.toPx()
            ),
            end = Offset(
                size.width,
                minDragAmountToReveal.toPx()
            ),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

@Composable
private fun AdjustmentComposable(
    description: String,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1.4f),
            text = description,
            fontSize = 13.sp,
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = onValueChange,
            steps = steps,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.DarkGray,
                activeTrackColor = Color.Gray,
                inactiveTrackColor = Color.LightGray.copy(alpha = 0.5f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledActiveTrackColor = Color.Gray.copy(alpha = 0.5f),
                disabledInactiveTrackColor = Color.LightGray.copy(alpha = 0.3f),
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent,
            ),
        )
    }
}