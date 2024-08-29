package com.mstf.dragtoreveal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import com.mstf.dragtoreveal.ui.theme.DragToRevealTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DragToRevealTheme {
                Scaffold { innerPadding ->
                    MainScreen(innerPadding)
                }
            }
        }
    }
}