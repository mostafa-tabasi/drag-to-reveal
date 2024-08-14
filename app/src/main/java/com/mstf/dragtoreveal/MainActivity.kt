package com.mstf.dragtoreveal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mstf.dragtoreveal.ui.theme.DragToRevealTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DragToRevealTheme {
                DragToReveal(
                    contentToReveal = {
                        Text(text = "Hello World!", Modifier.padding(8.dp))
                    },
                    content = {},
                )
            }
        }
    }
}