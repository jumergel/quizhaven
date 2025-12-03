package io.github.jumergel.quizhaven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jumergel.quizhaven.ui.theme.*
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext



import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember

@Composable
fun TextInput(navController: NavController) {
    var userText by rememberSaveable { mutableStateOf("") } //saved text
    val contentWidth = 0.8f  // screen content width
    val sidePad = 24.dp //extra padding on the side

    //firebase
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db   = remember { FirebaseFirestore.getInstance() }
//    val context = LocalContext.current
//    val auth = rememberSaveable { FirebaseAuth.getInstance() }
//    val db = rememberSaveable { FirebaseFirestore.getInstance() }

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
            Spacer(modifier = Modifier.height(150.dp))

            //logo
            Text(
                text = "<logo>",
                fontSize = 25.sp,
                color = Color.White
            )
            Spacer(Modifier.height(150.dp))

            //text
            Text(
                text = "Input Text",
                color = Cedar,
                textAlign = TextAlign.Center,
                style = Typography.displayLarge
            )
            Spacer(Modifier.height(30.dp))

            //text input
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
                        .fillMaxHeight(0.5f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TextField(
                        value = userText,
                        onValueChange = { userText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(16.dp),
                        placeholder = { Text("Type or paste your text here…") },
                        singleLine = false,
                        minLines = 6,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        )
                    )
                }
            }

            //buttons
            val buttonMod = Modifier //TODO better way??
                .height(65.dp)
                .weight(1f)

            Spacer(Modifier.height(120.dp))
            Row (
                modifier = Modifier
                    .fillMaxWidth(contentWidth)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = sidePad),
                horizontalArrangement = Arrangement.SpaceBetween)
            {
                EnterButton(
                    text = "Clear",
                    onClick = { userText = "" },
                    modifier = buttonMod
                )
                Spacer(Modifier.width(25.dp))
                EnterButton(
                    text = "Save", //save and proceed
                    onClick = {
                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
                            navController.navigate("login")
                            return@EnterButton
                        }
                        if (userText.isBlank()) {
                            Toast.makeText(context, "Text is empty.", Toast.LENGTH_SHORT).show()
                            return@EnterButton
                        }
                        val data = mapOf(
                            "initialInput" to userText,
                            "messages" to emptyList<String>() ,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "keywords" to emptyList<String>()
                        )
                        db.collection("users")
                            .document(uid)
                            .collection("inputs")
                            .add(data)
                            .addOnSuccessListener { ref ->
                                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                                navController.navigate("teaching/${ref.id}")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    },
                    modifier = buttonMod
                )
            }
        }
    }
}