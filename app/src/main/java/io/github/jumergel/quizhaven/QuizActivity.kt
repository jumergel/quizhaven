package io.github.jumergel.quizhaven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import io.github.jumergel.quizhaven.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue


//class QuizActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            QuizHavenTheme {
//                Surface(modifier = Modifier.fillMaxSize()) {
//                    QuizScreen(navController, inputId)
//                }
//            }
//        }
//    }
//}
data class Question(
    val text: String,
    val options: List<String>
)

@Composable
fun QuestionCard(
    question: Question,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.92f)
        ),
        modifier = Modifier
            .fillMaxWidth()          // width = card width
            .padding(vertical = 4.dp) // tiny gap between cards
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(question.text, style = Typography.titleMedium, color = Cedar)

            question.options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        onClick = { onOptionSelected(option) }
                    )
                    Text(text = option)
                }
            }
        }
    }
}


@Composable
fun QuizScreen(navController: NavController, inputId: String) {
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        // background image like teaching screen
        Image(
            painter = painterResource(R.drawable.plant_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(vertical = sidePad),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TOP NAV BAR, now Test is selected
                    LearnTestNavBar(
                        selectedTab = TopTab.Test,
                        onLearnClick = {
                            navController.navigate("teaching/$inputId")
                        },
                        onTestClick = { /* already here */ }
                    )

                    // TODO: your actual quiz content goes here
                    //show prev quiz results + take quiz button

                    val questions = listOf(
                        Question("What is 2 + 2?", listOf("1", "2", "3", "4")),
                        Question("What is 3 + 5?", listOf("6", "7", "8", "9")),
                        Question("Capital of France?", listOf("London", "Berlin", "Paris", "Rome")),
                        Question("2 * 3 = ?", listOf("4", "5", "6", "7")),
                        Question("Which is even?", listOf("1", "3", "5", "8")),
                        Question("What color is the sky?", listOf("Green", "Blue", "Red", "Yellow")),
                        Question("Which is a fruit?", listOf("Carrot", "Apple", "Potato", "Apple")),
                        Question("5 - 2 = ?", listOf("1", "2", "3", "4")),
                        Question("Largest planet?", listOf("Earth", "Mars", "Jupiter", "Venus")),
                        Question("Binary of 2?", listOf("00", "01", "10", "11"))
                    )
                    val selections = remember {
                        mutableStateListOf<String?>().apply {
                            repeat(questions.size) { add(null) }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)                 // take remaining vertical space inside the big white card
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(questions) { index, q ->
                            QuestionCard(
                                question = q,
                                selectedOption = selections[index],
                                onOptionSelected = { chosen ->
                                    selections[index] = chosen
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                }
            }
        }
    }
}
