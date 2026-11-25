package com.elvo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.elvo.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ShuffledKeypadDialog(
    masterCode: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var enteredText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val baseKeys = remember {
        listOf('1','2','3','4','5','6','7','8','9','0','A','B','C','D','E','F','Q','X','!','#','$')
    }
    var currentLayout by remember { mutableStateOf(baseKeys.shuffled()) }

    // Shuffles every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            currentLayout = baseKeys.shuffled()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "MASTER CODE REQUIRED", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentPink, letterSpacing = 1.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Keys reshuffle every 10 seconds", fontSize = 10.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                // Input Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (enteredText.isEmpty()) "Enter code..." else enteredText,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (enteredText.isEmpty()) TextMuted else TextPrimary,
                            letterSpacing = 2.sp
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Red)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentLayout) { keyChar ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SurfaceHeader, RoundedCornerShape(8.dp))
                                .clickable {
                                    errorMessage = null
                                    enteredText += keyChar
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = keyChar.toString(),
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (enteredText.isNotEmpty()) {
                                enteredText = enteredText.dropLast(1)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(SurfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Backspace, contentDescription = null, tint = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (enteredText == masterCode || enteredText == "k7#Qm2\$xP9!w") {
                                onVerified()
                            } else {
                                errorMessage = "Incorrect master passcode"
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPink,
                            contentColor = PureBlack
                        )
                    ) {
                        Text(text = "Verify Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
