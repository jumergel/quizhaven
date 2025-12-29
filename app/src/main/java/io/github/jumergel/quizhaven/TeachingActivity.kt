package io.github.jumergel.quizhaven

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.functions
import io.github.jumergel.quizhaven.ui.Layout
import io.github.jumergel.quizhaven.ui.theme.*
import kotlinx.coroutines.delay

data class ChatMessage(
    val text: String,
    val isUser: Boolean = true,
    val animate: Boolean = false // only animate newly-arrived AI messages
)

// markdown subset: **bold**
private fun markdownBoldToAnnotated(text: String): AnnotatedString {
    val regex = Regex("\\*\\*(.+?)\\*\\*")
    return buildAnnotatedString {
        var lastIndex = 0
        for (m in regex.findAll(text)) {
            val start = m.range.first
            val end = m.range.last + 1
            if (start > lastIndex) append(text.substring(lastIndex, start))
            val boldText = m.groupValues[1]
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(boldText)
            pop()
            lastIndex = end
        }
        if (lastIndex < text.length) append(text.substring(lastIndex))
    }
}

private fun callAI(
    projectId: String,
    inputId: String,
    lastUserMsg: String,
    context: android.content.Context,
    onFail: (() -> Unit)? = null
) {
    Firebase.functions
        .getHttpsCallable("replyToInput")
        .call(
            mapOf(
                "projectId" to projectId,
                "inputId" to inputId,
                "lastUserMsg" to lastUserMsg
            )
        )
        .addOnFailureListener { e ->
            Toast.makeText(context, "AI failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            onFail?.invoke()
        }
}

private fun looksLikeYes(s: String): Boolean {
    val t = s.trim().lowercase()
    if (t.isEmpty()) return false
    val yesWords = listOf(
        "yes", "y", "yeah", "yep", "ready", "next", "continue", "go on",
        "move on", "ok", "okay", "sure"
    )
    return yesWords.any { t == it || t.contains(it) }
}

/**
 * Detects "I don't know what to review / what should I study" style prompts
 */
private fun looksLikeWhatToReview(s: String): Boolean {
    val t = s.trim().lowercase()
    if (t.isEmpty()) return false

    val triggers = listOf(
        "what should i review",
        "what should i study",
        "what do i review",
        "what do i study",
        "i don't know what to review",
        "i dont know what to review",
        "i don't know what to study",
        "i dont know what to study",
        "i'm not sure what to review",
        "im not sure what to review",
        "help me study",
        "help me review",
        "suggest a topic",
        "suggest something",
        "pick something for me",
        "what next"
    )
    return triggers.any { t.contains(it) }
}

private fun isExitCommand(s: String): Boolean {
    val t = s.trim().lowercase()
    return t == "skip" || t == "exit"
}

@Composable
fun ChatBubble(message: ChatMessage) {
    var shownText by remember(message.text, message.animate) {
        mutableStateOf(if (message.animate) "" else message.text)
    }

    LaunchedEffect(message.text, message.animate) {
        if (!message.animate) {
            shownText = message.text
            return@LaunchedEffect
        }

        // chunk typing: bigger chunks, make text appear faster
        val chunkSize = 60
        val delayMs = 8L

        shownText = ""
        var i = 0
        while (i < message.text.length) {
            val next = (i + chunkSize).coerceAtMost(message.text.length)
            shownText = message.text.substring(0, next)
            i = next
            delay(delayMs)
        }
    }

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
                text = markdownBoldToAnnotated(shownText),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = Cedar,
                style = Typography.bodyMedium
            )
        }
    }
}

@Composable
fun TeachingScreen(navController: NavController, projectId: String, inputId: String) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val uid = auth.currentUser?.uid
    if (uid == null) {
        Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
        navController.navigate("login")
        return
    }

    // input doc
    val inputRef = db.collection("users").document(uid)
        .collection("projects").document(projectId)
        .collection("inputs").document(inputId)

    // project doc
    val projectRef = db.collection("users").document(uid)
        .collection("projects").document(projectId)

    var userText by rememberSaveable { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var problemSpots by remember { mutableStateOf<List<String>>(emptyList()) }

    // track when ai messages arrive
    var didInitialLoad by remember { mutableStateOf(false) }
    var lastMessageCount by remember { mutableIntStateOf(0) }

    // firestore review fields
    var reviewActive by remember { mutableStateOf(false) }
    var reviewStarted by remember { mutableStateOf(false) }
    var reviewIndex by remember { mutableIntStateOf(0) }
    var incorrectQuestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var reviewWaitingForYes by remember { mutableStateOf(false) }

    // local busy guard: only one AI call at a time for review
    var reviewBusy by remember { mutableStateOf(false) }

    fun endReviewSession() {
        inputRef.update(
            mapOf(
                "reviewActive" to false,
                "reviewStarted" to false,
                "reviewIndex" to 0,
                "reviewWaitingForYes" to false
            )
        )
    }

    //listen for problemSpots
    LaunchedEffect(projectId) {
        projectRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
            problemSpots = (snap.get("problemSpots") as? List<*>)?.map { it.toString() } ?: emptyList()
        }
    }

    // listen for input doc messages + review fields
    LaunchedEffect(inputId) {
        inputRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener

            // review fields
            reviewActive = snap.getBoolean("reviewActive") == true
            reviewStarted = snap.getBoolean("reviewStarted") == true
            reviewIndex = (snap.getLong("reviewIndex") ?: 0L).toInt()
            reviewWaitingForYes = snap.getBoolean("reviewWaitingForYes") == true
            incorrectQuestions =
                (snap.get("incorrectQuestions") as? List<*>)?.map { it.toString() } ?: emptyList()

            // messages
            val arr = snap.get("messages") as? List<*> ?: emptyList<Any>()
            val rawMessages = arr.mapNotNull { it as? String }
            val newCount = rawMessages.size

            val grew = didInitialLoad && newCount > lastMessageCount

            val rebuilt = rawMessages.map { raw ->
                val isAi = raw.startsWith("AI:")
                ChatMessage(
                    text = if (isAi) raw.removePrefix("AI:").trim() else raw,
                    isUser = !isAi,
                    animate = false // do NOT re-animate old stuff
                )
            }.toMutableList()

            // animate only the newest AI message
            if (grew && rebuilt.isNotEmpty()) {
                val last = rebuilt.last()
                if (!last.isUser) rebuilt[rebuilt.lastIndex] = last.copy(animate = true)
            }

            messages.clear()
            messages.addAll(rebuilt)

            didInitialLoad = true
            lastMessageCount = newCount

            // if we were waiting on AI and it arrived, release the guard
            if (grew && rebuilt.isNotEmpty() && !rebuilt.last().isUser) {
                reviewBusy = false
            }
        }
    }

    // Kick off review once when reviewActive becomes true (set by QuizScreen after "Review Material")
    LaunchedEffect(reviewActive, reviewStarted, incorrectQuestions) {
        if (!reviewActive) return@LaunchedEffect
        if (reviewStarted) return@LaunchedEffect
        if (incorrectQuestions.isEmpty()) {
            inputRef.update(mapOf("reviewActive" to false))
            return@LaunchedEffect
        }
        if (reviewBusy) return@LaunchedEffect

        reviewBusy = true

        val firstQ = incorrectQuestions[0]
        val prompt = """
Let's review the missed questions!

Missed Question 1:
$firstQ

Explain the correct idea clearly.
Then ask ONE short follow-up question to test the student.
End by asking: "Ready to move on to the next missed question? (yes/no) Or type skip to exit review."
""".trim()

        // mark started so we don't re-trigger
        inputRef.update(
            mapOf(
                "reviewStarted" to true,
                "reviewIndex" to 0,
                "reviewWaitingForYes" to true
            )
        )

        callAI(projectId, inputId, prompt, context) { reviewBusy = false }
    }

    fun sendReviewNextQuestion(nextIndex: Int) {
        if (nextIndex >= incorrectQuestions.size) {
            reviewBusy = true
            callAI(
                projectId,
                inputId,
                "Great work — that was the last missed question. Give a brief encouraging wrap-up in 2-3 sentences.",
                context
            ) { reviewBusy = false }

            endReviewSession()
            return
        }

        reviewBusy = true

        val qText = incorrectQuestions[nextIndex]
        val prompt = """
Next missed question (${nextIndex + 1}/${incorrectQuestions.size}):

$qText

Explain the correct idea clearly.
Then ask ONE short follow-up question to test the student.
End by asking: "Ready to move on to the next missed question? (yes/no)"
""".trim()

        inputRef.update(
            mapOf(
                "reviewIndex" to nextIndex,
                "reviewWaitingForYes" to true
            )
        )

        callAI(projectId, inputId, prompt, context) { reviewBusy = false }
    }

    fun sendReviewClarification(studentMsg: String) {
        val idx = reviewIndex.coerceIn(0, (incorrectQuestions.size - 1).coerceAtLeast(0))
        val qText = incorrectQuestions.getOrNull(idx) ?: return

        reviewBusy = true

        val prompt = """
We are reviewing missed question ${idx + 1}/${incorrectQuestions.size}:

$qText

Student response:
$studentMsg

Help the student understand (briefly). Re-explain the key idea based on their response.
Then ask ONE follow-up question again.
End by asking: "Ready to move on to the next missed question? (yes/no)"
""".trim()

        inputRef.update("reviewWaitingForYes", true)

        callAI(projectId, inputId, prompt, context) { reviewBusy = false }
    }

    fun sendMessage() {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return
        userText = ""

        inputRef.update("messages", FieldValue.arrayUnion(trimmed))
            .addOnSuccessListener {

                // LEAVE REVIEW MODE if user types skip or exit
                if (reviewActive && isExitCommand(trimmed)) {
                    endReviewSession()
                    reviewBusy = false

                    // one AI message acknowledging exit
                    val exitPrompt = """
The student typed "$trimmed" to exit review mode.
Reply with ONE short message acknowledging that review mode is off,
and ask what they want to work on next.
""".trim()
                    callAI(projectId, inputId, exitPrompt, context)
                    return@addOnSuccessListener
                }

                // 1) If in review mode, interpret user as "yes/no" gate
                if (reviewActive && reviewWaitingForYes) {
                    if (looksLikeYes(trimmed)) {
                        inputRef.update("reviewWaitingForYes", false)
                        sendReviewNextQuestion(reviewIndex + 1)
                    } else {
                        sendReviewClarification(trimmed)
                    }
                    return@addOnSuccessListener
                }

                // 2) If user doesn't know what to review, suggest a problemSpot (if exists)
                if (looksLikeWhatToReview(trimmed) && problemSpots.isNotEmpty()) {
                    val suggested = problemSpots.first()
                    val prompt = """
The student says they don't know what to review.

Their tracked problem spots are:
${problemSpots.joinToString(", ")}

Pick ONE suggestion (preferably: "$suggested") and explain why briefly.
Then give a 3-step micro-plan (very short).
End with ONE question: "Want to review this now? (yes/no)"
""".trim()
                    callAI(projectId, inputId, prompt, context)
                    return@addOnSuccessListener
                }

                // normal chat
                callAI(projectId = projectId, inputId = inputId, lastUserMsg = trimmed, context = context)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Send failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
                        .fillMaxWidth(Layout.contentWidth)
                        .fillMaxHeight(Layout.contentHeight)
                        .padding(vertical = Layout.sidePad),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LearnTestNavBar(
                            selectedTab = TopTab.Learn,
                            onLearnClick = { /* already here */ },
                            onTestClick = { navController.navigate("quiz/$projectId/$inputId") },
                            onStatsClick = { navController.navigate("stats/$projectId/$inputId") }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Start at bottom without forcing scroll: reverse layout
                        LazyColumn(
                            reverseLayout = true,
                            modifier = Modifier.weight(1f)
                        ) {
                            items(messages.asReversed()) { msg ->
                                ChatBubble(message = msg)
                            }
                        }

                        HorizontalDivider()

                        Row(modifier = Modifier.padding(top = 6.dp)) {
                            OutlinedTextField(
                                value = userText,
                                onValueChange = { userText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type your questions here") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { sendMessage() })
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
