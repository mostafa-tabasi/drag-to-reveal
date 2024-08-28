package com.mstf.dragtoreveal

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private var _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    fun setElasticityLevel(level: Float) {
        _uiState.value = uiState.value.copy(dragElasticityLevel = level)
    }

    fun setMinDragAmount(amount: Float) {
        _uiState.value = uiState.value.copy(minDragAmountToReveal = amount.dp)
    }

    fun toggleRevealState(isHiddenContentRevealed: Boolean) {
        _uiState.value = uiState.value.copy(isContentRevealed = isHiddenContentRevealed)
    }
}

data class MainState(
    val isContentRevealed: Boolean = false,
    val dragElasticityLevel: Float = 1f,
    val minDragAmountToReveal: Dp = 1.dp,
)