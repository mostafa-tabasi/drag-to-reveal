package com.mstf.dragtoreveal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mstf.dragtoreveal.ui.theme.DragToRevealTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DragToRevealTheme {
                MainScreen()
            }
        }
    }
}