// LBoard.kt — Fixed version
// A minimal custom keyboard with basic functions and long-press delete.
// -------------------------------------------------------------

package com.example.lboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Main Input Method Service
// -------------------------------------------------------------
class LBoardService : InputMethodService() {

    override fun onCreateInputView(): View {
        // Use a ComposeView as keyboard root
        return ComposeView(this).apply {
            setContent {
                KeyboardScreen { key ->
                    handleKeyPress(key)
                }
            }
        }
    }

    // ---------------------------------------------------------
    // Handles key events (letters, space, delete, enter)
    // ---------------------------------------------------------
    private fun handleKeyPress(key: String) {
        val ic: InputConnection = currentInputConnection ?: return
        when (key) {
            "DEL" -> ic.deleteSurroundingText(1, 0)
            "SPACE" -> ic.commitText(" ", 1)
            "ENTER" -> ic.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
            else -> ic.commitText(key, 1)
        }
    }
}

// -------------------------------------------------------------
// Basic data for rows of keys
// -------------------------------------------------------------
private val row1 = listOf("Q","W","E","R","T","Y","U","I","O","P")
private val row2 = listOf("A","S","D","F","G","H","J","K","L")
private val row3 = listOf("Z","X","C","V","B","N","M")
private val row4 = listOf("SPACE","DEL","ENTER")

// -------------------------------------------------------------
// KeyboardScreen() — composable root of keyboard UI
// -------------------------------------------------------------
@Composable
fun KeyboardScreen(onKeyPress: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF202124))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        KeyRow(keys = row1, onKeyPress = onKeyPress)
        KeyRow(keys = row2, onKeyPress = onKeyPress)
        KeyRow(keys = row3, onKeyPress = onKeyPress)
        KeyRow(keys = row4, onKeyPress = onKeyPress)
    }
}

// -------------------------------------------------------------
// KeyRow() — builds one horizontal row of keys
// -------------------------------------------------------------
@Composable
fun KeyRow(keys: List<String>, onKeyPress: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (key in keys) {
            KeyButton(
                label = key,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                onKeyPress = onKeyPress
            )
        }
    }
}

// -------------------------------------------------------------
// KeyButton() — composable for individual keys with press tint and popup feedback
// -------------------------------------------------------------
@Composable
fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onKeyPress: (String) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var deleteJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val keyColor = if (pressed) Color(0xFF3C4043) else Color(0xFF2C2F33)
    val textColor = Color.White
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .background(keyColor, shape)
            .border(1.dp, Color(0xFF555555), shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    pressed = true
                    showPopup = true
                    onKeyPress(label)
                },
                onLongClick = {
                    if (label == "DEL") {
                        pressed = true
                        showPopup = true
                        // Start continuous deletion
                        deleteJob = scope.launch {
                            while (true) {
                                onKeyPress("DEL")
                                delay(80)
                            }
                        }
                    }
                },
                onLongClickLabel = "Hold"
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Key label
        Text(
            text = when (label) {
                "SPACE" -> "␣"
                "DEL" -> "⌫"
                "ENTER" -> "⏎"
                else -> label
            },
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        // Popup feedback above key (shown briefly)
        if (showPopup) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-60).dp)
                    .background(Color(0xFF3C4043), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Auto-hide feedback popup & reset tint
            LaunchedEffect(showPopup) {
                if (showPopup) {
                    delay(120)
                    showPopup = false
                    pressed = false
                    deleteJob?.cancel()
                    deleteJob = null
                }
            }
        }
    }

    // Cancel deletion job when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            deleteJob?.cancel()
        }
    }
}
