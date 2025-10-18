package io.github.jumergel.quizhaven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { QuizHavenTheme {
            Box(Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.plant_home),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Foreground UI (transparent so bg is visible)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) { innerPadding ->
                    LoginScreen(paddingValues = innerPadding)
                }
            }
        } }
    }
}

@Composable
fun LoginScreen(paddingValues: PaddingValues) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Spacer(modifier = Modifier.height(200.dp))
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Login",
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
                        passwordError = if (password.isBlank()) "Password is required" else ""
                        if (emailError.isEmpty() && passwordError.isEmpty()) {
                            // TODO handle login
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGreen,
                        contentColor = Ivory
                    )
                ) {
                    Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

//        Text(
//            text = "Forget Password?",
//            color = MaterialTheme.colorScheme.primary,
//            modifier = Modifier.clickable { //handle forgot password logic
//            }
//        )
//
//        Spacer(modifier = Modifier.height(50.dp))
//
//        Row {
//            Text(text = "Not a member? ")
//
//            Text(
//                text = "Sign in now!",
//                color = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.clickable { //handle sign logic
//                }
//            )
       // }
    }




//    Box(Modifier.fillMaxSize()) {
//        Image(
//            painter = painterResource(R.drawable.plant_home),
//            contentDescription = null,
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.fillMaxSize()
//        )
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(horizontal = 24.dp)
//                .padding(top = 80.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Text (
//                text = "<logo>",
//                fontSize = 25.sp,
//                color = Color.White
//            )
//            Spacer(Modifier.height(150.dp))
//            Text(
//                text = "Login",
//                color = Cedar,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth(),
//                style = Typography.displayLarge
//            )
//            Spacer(Modifier.height(50.dp))
//            val buttonMod = Modifier
//                .fillMaxWidth(0.7f)
//                .height(50.dp)
//
//
//            DoneButton(
//                text = "Login",
//                onClick = { },
//                modifier = buttonMod
//            )
//            Spacer(Modifier.height(230.dp))
//            Text (
//                text = "Enjoy stress-free studying",
//                fontSize = 20.sp,
//                color = Ivory
//            )
//        }
//    }
}

@Composable
fun DoneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SoftGreen,
            contentColor = Ivory
        ),
        border = BorderStroke(1.dp, Ivory),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text, fontSize = 20.sp)
    }
}

//@Preview(showBackground = true)
//@Composable
//fun PreviewLoginScreen() {
//    QuizHavenTheme { LoginScreen() }
//}
