package io.github.jumergel.quizhaven

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jumergel.quizhaven.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

data class ChatMessage(
    val text: String,
    val isUser: Boolean = true // later you can add AI messages with false
)

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.isUser) Cedar.copy(alpha = 0.15f) else Color(0xFFE0E0E0),
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = Cedar,
                style = Typography.bodyMedium
            )
        }
    }
}


@Composable
fun TeachingScreen(navController: NavController) {
    var userText by rememberSaveable { mutableStateOf("") } //saved text
    val contentWidth = 1f  // screen content width
    val contentHeight = 1f  // screen content width
    val sidePad = 24.dp //extra padding on the side

    // List of chat messages (cells)
    val messages = remember { mutableStateListOf<ChatMessage>() }

    fun sendMessage() {
        val trimmed = userText.trim()
        if (trimmed.isNotEmpty()) {
            messages.add(ChatMessage(text = trimmed, isUser = true))
            userText = ""
            // later you can trigger AI response here and add another ChatMessage with isUser = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.plant_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            //card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.92f) // white box
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(contentWidth)
                        .fillMaxHeight(contentHeight)
                        .padding(vertical = sidePad),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(){
                        Column(){
                            Spacer(Modifier.height(16.dp))
                            Text( //Title
                                text = "Teaching Screen",
                                color = Cedar,
                                textAlign = TextAlign.Center,
                                style = Typography.displayLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()

                            //CHAT List
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f) // take all remaining space
                            ) {
                                items(messages) { msg ->
                                    ChatBubble(message = msg)
                                }
                            }

                            HorizontalDivider()

                            Row() {
                                OutlinedTextField(
                                    value = userText,
                                    onValueChange = { userText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Type your questions here") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        imeAction = ImeAction.Send
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = { sendMessage() }
                                    )
                                )

                                Spacer(Modifier.width(8.dp))

                                IconButton(onClick = { sendMessage() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Cedar
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}