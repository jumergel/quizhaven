package io.github.jumergel.quizhaven

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import io.github.jumergel.quizhaven.ui.Layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.functions
import io.github.jumergel.quizhaven.ui.theme.Cedar
import io.github.jumergel.quizhaven.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSubmitted) {
                            if (!isSubmitted) onOptionSelected(option)
                        }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        enabled = !isSubmitted,
                        onClick = { if (!isSubmitted) onOptionSelected(option) }
                    )
                    Spacer(Modifier.width(8.dp))
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
// QUIZ SCREEN (RESTORE + GENERATE + SAVE + SCORE SAVE)
// ----------------------------
@Composable
fun QuizScreen(navController: NavController, projectId: String, inputId: String) {
    val context = LocalContext.current

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val functions = remember { Firebase.functions }

    val uid = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    // Questions + answers (UI state)
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var correctAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    val selections = remember { mutableStateListOf<String?>() }

    // 0 = before submit, 1 = review button stage, 2 = generate new quiz stage
    var quizState by remember { mutableStateOf(0) }

    var isGenerating by remember { mutableStateOf(false) }

    // Firestore input doc (project-scoped)
    val inputRef = remember(uid, projectId, inputId) {
        if (uid == null) null else db.collection("users")
            .document(uid)
            .collection("projects").document(projectId)
            .collection("inputs").document(inputId)
    }

    val listState = rememberLazyListState()

    // ----------------------------
    // Derived score + message
    // ----------------------------
    val score by remember(quizState, selections, correctAnswers) {
        derivedStateOf {
            if (quizState < 1) 0
            else selections.indices.count { idx ->
                selections.getOrNull(idx) != null &&
                        selections.getOrNull(idx) == correctAnswers.getOrNull(idx)
            }
        }
    }

    val scoreMessage by remember(score) {
        derivedStateOf {
            when {
                score < 5 -> "Keep going, you'll get there.."
                score in 5..8 -> "Good job! You got this"
                else -> "Wow! You've mastered these."
            }
        }
    }

    // Scroll to score footer once quiz is submitted
    LaunchedEffect(quizState, questions.size) {
        if (quizState >= 1 && questions.isNotEmpty()) {
            delay(80)
            // footer is after last question => index = questions.size
            listState.animateScrollToItem(index = questions.size)
        }
    }

    // auto scroll to top of new quiz
    LaunchedEffect(quizState, questions.size) {
        if (quizState == 0 && questions.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    // Save score to outputs/scores/items
    fun saveScoreToFirestore(correctCount: Int) {
        val u = uid ?: return
        val total = questions.size
        val percent = if (total == 0) 0 else ((correctCount * 100f) / total).toInt()

        val scoreData = hashMapOf(
            "inputId" to inputId,
            "correct" to correctCount,
            "total" to total,
            "percent" to percent,
            "createdAt" to FieldValue.serverTimestamp()
        )

        // users/{uid}/projects/{projectId}/outputs/scores/items/{autoId}
        db.collection("users").document(u)
            .collection("projects").document(projectId)
            .collection("outputs").document("scores")
            .collection("items")
            .add(scoreData)
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save score: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    // ----------------------------
    // Helper: restore from Firestore
    // ----------------------------
    fun restoreFromSnapshot(snap: com.google.firebase.firestore.DocumentSnapshot): Boolean {
        val savedQuestions = snap.get("quizQuestions") as? List<*> ?: emptyList<Any>()
        val savedCorrect = snap.get("quizCorrectAnswers") as? List<*> ?: emptyList<Any>()

        @Suppress("UNCHECKED_CAST")
        val savedChoicesMap = snap.get("quizChoices") as? Map<String, Any> ?: emptyMap()

        val qTexts = savedQuestions.mapNotNull { it as? String }
        val correct = savedCorrect.mapNotNull { it as? String }

        val hasSavedQuiz =
            qTexts.isNotEmpty() && correct.isNotEmpty() && savedChoicesMap.isNotEmpty()

        if (!hasSavedQuiz) return false

        // Map -> List<List<String>> in index order
        val choices: List<List<String>> =
            (0 until qTexts.size).map { idx ->
                val arr = savedChoicesMap[idx.toString()] as? List<*> ?: emptyList<Any>()
                arr.map { it.toString() }
            }

        // user answers can be list or map
        val savedUserAnswersList =
            (snap.get("quizUserAnswers") as? List<*>)?.map { it as? String }

        val savedUserAnswersMap =
            (snap.get("quizUserAnswers") as? Map<String, *>)?.mapValues { it.value as? String }

        val savedUserAnswers: List<String?> =
            savedUserAnswersList ?: (0 until qTexts.size).map { idx ->
                savedUserAnswersMap?.get(idx.toString())
            }

        val savedState = (snap.getLong("quizState") ?: 0L).toInt()

        correctAnswers = correct
        questions = qTexts.mapIndexed { idx, stem ->
            Question(stem, choices.getOrNull(idx) ?: emptyList())
        }

        selections.clear()
        repeat(questions.size) { idx ->
            selections.add(savedUserAnswers.getOrNull(idx))
        }

        quizState = savedState
        return true
    }

    // ----------------------------
    // LOAD QUIZ helper (call function + save to Firestore)
    // ----------------------------
    suspend fun loadQuizAndSave(forceNew: Boolean = false) {
        val ref = inputRef ?: return

        try {
            if (forceNew) {
                isGenerating = true
                questions = emptyList()
                correctAnswers = emptyList()
                selections.clear()
                quizState = 0
            }

            val data = hashMapOf(
                "projectId" to projectId,
                "inputId" to inputId,
                "forceNew" to forceNew
            )

            val result = functions
                .getHttpsCallable("generateQuestionSet")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val resultData = result.data as Map<String, Any>

            val qTexts = (resultData["questions"] as? List<*>)?.map { it.toString() } ?: emptyList()
            val correct = (resultData["correctLabels"] as? List<*>)?.map { it.toString() } ?: emptyList()

            val choicesNested = resultData["choices"] as? List<*>
            val choices: List<List<String>> =
                choicesNested?.map { row ->
                    (row as? List<*>)?.map { it.toString() } ?: emptyList()
                } ?: emptyList()

            if (qTexts.isEmpty() || correct.isEmpty() || choices.isEmpty()) {
                Toast.makeText(context, "Failed to load quiz: empty data", Toast.LENGTH_LONG).show()
                return
            }

            // UI
            correctAnswers = correct
            questions = qTexts.mapIndexed { idx, stem ->
                Question(stem, choices.getOrNull(idx) ?: emptyList())
            }
            selections.clear()
            repeat(questions.size) { selections.add(null) }
            quizState = 0

            // Firestore storage format (NO nested arrays): Map<String, List<String>>
            val quizChoicesMap: Map<String, List<String>> =
                choices.mapIndexed { idx, arr -> idx.toString() to arr }.toMap()

            // user answers as map so each answer is easy to update later
            val quizUserAnswersMap: Map<String, String?> =
                (0 until qTexts.size).associate { it.toString() to null }

            // Save to Firestore so next time we can restore without regenerating
            ref.update(
                mapOf(
                    "quizQuestions" to qTexts,
                    "quizChoices" to quizChoicesMap,
                    "quizCorrectAnswers" to correct,
                    "quizUserAnswers" to quizUserAnswersMap,
                    "quizState" to 0,
                    "incorrectQuestions" to emptyList<String>(),
                    "quizUpdatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load quiz: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            isGenerating = false
        }
    }

    // ----------------------------
    // Initial load: RESTORE if possible, else GENERATE+SAVE
    // ----------------------------
    LaunchedEffect(uid, projectId, inputId) {
        val ref = inputRef ?: return@LaunchedEffect

        try {
            val snap = ref.get().await()
            if (snap.exists()) {
                val restored = restoreFromSnapshot(snap)
                if (restored) return@LaunchedEffect
            }
            loadQuizAndSave(forceNew = false)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to restore quiz: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            loadQuizAndSave(forceNew = false)
        }
    }

    // ----------------------------
    // UI
    // ----------------------------
    Box(Modifier.fillMaxSize()) {

        if (!isGenerating) {
            Image(
                painter = painterResource(R.drawable.plant_home),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

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
                        .padding(vertical = Layout.sidePad),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    LearnTestNavBar(
                        selectedTab = TopTab.Test,
                        onLearnClick = { navController.navigate("teaching/$projectId/$inputId") },
                        onTestClick = { /* already here */ },
                        onStatsClick = { navController.navigate("stats/$projectId/$inputId") }
                    )

                    LazyColumn(
                        state = listState,
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
                                    if (quizState == 0 && index < selections.size) {
                                        selections[index] = chosen
                                    }
                                },
                                correctAnswer = correctAnswers.getOrNull(index),
                                isSubmitted = (quizState >= 1)
                            )
                        }

                        // SCORE footer AFTER submit
                        if (quizState >= 1 && questions.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .padding(start = 44.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Score: $score / ${questions.size}",
                                            style = Typography.titleLarge,
                                            color = Cedar
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = scoreMessage,
                                            style = Typography.titleMedium,
                                            color = Cedar
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val u = uid
                            val ref = inputRef
                            if (u == null || ref == null) {
                                Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                                return@Button
                            }

                            when (quizState) {
                                // 0 -> submit/save answers + save incorrectQuestions + save score
                                0 -> {
                                    val saveAnswersMap: Map<String, String?> =
                                        selections.mapIndexed { idx, v -> idx.toString() to v }.toMap()

                                    val incorrect = questions.mapIndexedNotNull { idx, q ->
                                        val chosen = selections.getOrNull(idx)
                                        val correct = correctAnswers.getOrNull(idx)
                                        if (chosen == null || chosen != correct) q.text else null
                                    }

                                    ref.update(
                                        mapOf(
                                            "quizUserAnswers" to saveAnswersMap,
                                            "incorrectQuestions" to incorrect,
                                            "quizState" to 1,
                                            "quizUpdatedAt" to FieldValue.serverTimestamp(),
                                            // review flags reset until user taps Review Material
                                            "reviewActive" to false,
                                            "reviewIndex" to 0,
                                            "reviewStarted" to false,
                                            "reviewWaitingForYes" to false
                                        )
                                    )
                                        .addOnSuccessListener {
                                            quizState = 1
                                            Toast.makeText(context, "Quiz submitted!", Toast.LENGTH_SHORT).show()

                                            val correctCount = selections.indices.count { idx ->
                                                selections.getOrNull(idx) != null &&
                                                        selections.getOrNull(idx) == correctAnswers.getOrNull(idx)
                                            }
                                            saveScoreToFirestore(correctCount)
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                }

                                // 1 -> review material: set reviewActive then go to teaching
                                1 -> {
                                    ref.update(
                                        mapOf(
                                            "reviewActive" to true,
                                            "reviewIndex" to 0,
                                            "reviewStarted" to false,
                                            "reviewWaitingForYes" to true,
                                            "quizState" to 2,
                                            "quizUpdatedAt" to FieldValue.serverTimestamp()
                                        )
                                    )
                                        .addOnSuccessListener {
                                            quizState = 2
                                            Toast.makeText(context, "Reviewing missed questions…", Toast.LENGTH_SHORT).show()
                                            navController.navigate("teaching/$projectId/$inputId")
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                }

                                // 2 -> generate new quiz (forceNew = true)
                                2 -> {
                                    scope.launch { loadQuizAndSave(forceNew = true) }
                                }
                            }
                        },
                        enabled = !isGenerating,
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

        if (isGenerating) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingGif()
            }
        }
    }
}
