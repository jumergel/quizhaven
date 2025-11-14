package io.github.jumergel.quizhaven

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.jumergel.quizhaven.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Spacer(modifier = Modifier.height(230.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.92f) // white box
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(35.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.displayLarge,
                    color = Cedar,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // EMAIL
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = {
                        Text(
                            if (emailError.isEmpty()) "Email" else emailError,
                            color = if (emailError.isEmpty()) Color.Unspecified else Color.Red
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.AccountCircle, null) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    // Transparent container so the card's white shows through
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // PASSWORD
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            if (passwordError.isEmpty()) "Password" else passwordError,
                            color = if (passwordError.isEmpty()) Color.Unspecified else Color.Red
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) R.drawable.visibility
                        else R.drawable.visibilityoff
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Button(
                    onClick = {
                        emailError = if (email.isBlank()) "Email is required" else ""
                        passwordError = when {
                            password.isBlank() -> "Password is required"
                            password.length < 6 -> "At least 6 characters"
                            else -> ""
                        }
                        if (emailError.isNotEmpty() || passwordError.isNotEmpty()) return@Button

                        isBusy = true
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                isBusy = false
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                                    // Option A: go straight to home
                                    // navController.navigate("home") { popUpTo("signup") { inclusive = true } }

                                    // Option B: go to login (common pattern)
                                    navController.navigate("login") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                } else {
                                    val msg = task.exception?.localizedMessage ?: "Sign up failed"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGreen,
                        contentColor = Ivory
                    )
                ) {
                    Text(if (isBusy) "Creating..." else "Sign Up")
//                    Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }


                Spacer(modifier = Modifier.height(50.dp))

                Row {
                    Text(text = "Already a member? ")

                    Text(
                        text = "Sign in here!",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            navController.navigate("login")
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}


//@Preview(showBackground = true)
//@Composable
//fun PreviewSignUpScreen() {
//    QuizHavenTheme { SignUpScreen() }
//}
