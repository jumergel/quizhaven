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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContent { QuizHavenTheme { HomeScreen() } }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
 //button navigation
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.plant_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text (
                text = "<logo>",
                fontSize = 25.sp,
                color = Color.White
            )
            Spacer(Modifier.height(150.dp))
            Text(
                text = "Welcome to \nQuiz Haven",
                color = Cedar,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = Typography.displayLarge
            )
            Spacer(Modifier.height(50.dp))
            val buttonMod = Modifier
                .fillMaxWidth(0.6f)
                .height(65.dp)



            EnterButton(
                text = "Login",
                onClick = {navController.navigate("login")},
                modifier = buttonMod
            )
            Spacer(Modifier.height(40.dp))
            EnterButton(
                text = "Sign Up",
                onClick = {navController.navigate("signup") },
                modifier = buttonMod
            )
            Spacer(Modifier.height(230.dp))
            Text (
                text = "Enjoy stress-free studying",
                fontSize = 20.sp,
                color = Ivory
            )
        }
    }
}

@Composable
fun EnterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Tan,
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
//fun PreviewHome() {
//    QuizHavenTheme { HomeScreen() }
//}
