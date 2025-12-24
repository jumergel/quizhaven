package io.github.jumergel.quizhaven

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.jumergel.quizhaven.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import android.widget.Toast



// ----------------------------
// DATA MODEL
// ----------------------------
data class Question(
    val text: String,
    val options: List<String>
)

// ----------------------------
// QUESTION CARD
// ----------------------------
@Composable
fun QuestionCard(
    question: Question,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    correctAnswer: String?,
    isSubmitted: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(question.text, style = Typography.titleMedium, color = Cedar)

            question.options.forEach { option ->
                val isCorrect = isSubmitted && correctAnswer == option

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        enabled = !isSubmitted,
                        onClick = {
                            if (!isSubmitted) onOptionSelected(option)
                        }
                    )
                    Text(
                        text = option,
                        color = if (isCorrect) Color(0xFF2E7D32) else Color.Unspecified
                    )
                }
            }
        }
    }
}

// ----------------------------
// QUIZ SCREEN
// ----------------------------
@Composable
fun QuizScreen(navController: NavController, inputId: String) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val functions = remember { Firebase.functions }
    val uid = auth.currentUser?.uid

    val scope = rememberCoroutineScope()

    // Questions + answers
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var correctAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    val selections = remember { mutableStateListOf<String?>() }

    // 0 = before submit, 1 = review, 2 = generate new quiz
    var quizState by remember { mutableStateOf(0) }

    // ----------------------------
    // LOAD QUIZ helper (suspend)
    // ----------------------------
    suspend fun loadQuiz() {
        if (uid == null) return

        try {
            val data = hashMapOf("inputId" to inputId)

            val result = functions
                .getHttpsCallable("generateQuestionSet")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val resultData = result.data as Map<String, Any>

            val qTexts = resultData["questions"] as List<String>
            val choices = resultData["choices"] as List<List<String>>
            val correct = resultData["correctLabels"] as List<String>

            correctAnswers = correct

            questions = qTexts.mapIndexed { idx, stem ->
                Question(stem, choices.getOrNull(idx) ?: emptyList())
            }

            selections.clear()
            repeat(questions.size) { selections.add(null) }

            quizState = 0   // reset to "Save Quiz" state
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load quiz: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Initial load
    LaunchedEffect(inputId, uid) {
        loadQuiz()
    }

    // ----------------------------
    // UI LAYOUT
    // ----------------------------
    Box(Modifier.fillMaxSize()) {
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
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(vertical = sidePad),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    LearnTestNavBar(
                        selectedTab = TopTab.Test,
                        onLearnClick = { navController.navigate("teaching/$inputId") },
                        onTestClick = {}
                    )

                    // SCROLLABLE QUESTION LIST
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(questions) { index, q ->
                            QuestionCard(
                                question = q,
                                selectedOption = selections.getOrNull(index),
                                onOptionSelected = { chosen ->
                                    if (quizState == 0) selections[index] = chosen
                                },
                                correctAnswer = correctAnswers.getOrNull(index),
                                isSubmitted = (quizState >= 1)
                            )
                        }
                    }

                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // MAIN BUTTON
                    Button(
                        onClick = {
                            if (uid == null) {
                                Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                                return@Button
                            }

                            val inputRef = db.collection("users")
                                .document(uid)
                                .collection("inputs")
                                .document(inputId)

                            when (quizState) {

                                // 0 → SAVE QUIZ
                                0 -> {
                                    val saveAnswers = selections.map { it }

                                    inputRef.update("quizUserAnswers", saveAnswers)
                                        .addOnSuccessListener {
                                            quizState = 1
                                            Toast.makeText(
                                                context,
                                                "Quiz submitted!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Error: ${e.localizedMessage}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }

                                // 1 → REVIEW MATERIAL (save incorrectQuestions)
                                1 -> {
                                    val incorrect = questions.mapIndexedNotNull { idx, q ->
                                        val chosen = selections.getOrNull(idx)
                                        val correct = correctAnswers.getOrNull(idx)
                                        if (chosen == null || chosen != correct) q.text else null
                                    }

                                    inputRef.update("incorrectQuestions", incorrect)
                                        .addOnSuccessListener {
                                            quizState = 2
                                            Toast.makeText(
                                                context,
                                                "Saved incorrect questions.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // You also navigate back if you want:
                                            navController.navigate("teaching/$inputId")
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Error saving incorrect questions: ${e.localizedMessage}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }

                                // 2 → GENERATE NEW QUIZ (reload)
                                2 -> {
                                    scope.launch {
                                        loadQuiz()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Crossfade(targetState = quizState) { state ->
                            when (state) {
                                0 -> Text("Save Quiz")
                                1 -> Text("Review Material")
                                2 -> Text("Generate New Quiz")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
