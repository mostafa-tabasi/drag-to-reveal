package com.mstf.dragtoreveal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mstf.dragtoreveal.ui.theme.DragToRevealTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DragToRevealTheme {
                DragToReveal(
                    instructionSwipingText = "Swipe down to reveal",
                    instructionReleaseText = "Release to reveal",
                    contentToReveal = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            (1..30).map {
                                Text(
                                    text = "Hello World! #$it",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            LazyRow {
                                items(count = 5,
                                    itemContent = {
                                        Text(
                                            text = "Hello World! #$it",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    })
                            }
                        }
                    },
                    content = { lazyListState, scrollState ->
                        /*
                        Column(
                            modifier = Modifier.verticalScroll(scrollState),
                        ) {
                            (1..30).map {
                                Text(
                                    text = "Hello World! #$it",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        */

                        LazyColumn(
                            state = lazyListState,
                        ) {
                            items(
                                count = 30,
                                itemContent = {
                                    Text(
                                        text = "Hello World! #$it",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            )
                        }
                    },
                )
            }
        }
    }
}