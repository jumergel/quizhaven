package io.github.jumergel.quizhaven

import android.widget.Toast
import androidx.compose.foundation.clickable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun Survey(navController: NavController, inputId: String) {
    // firebase
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // Single-choice state
    var selectedAge by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGoal by rememberSaveable { mutableStateOf<String?>(null) }

    // Multi-choice state
    var qTerminology by rememberSaveable { mutableStateOf(false) }
    var qMultipleChoice by rememberSaveable { mutableStateOf(false) }
    var qFreeResponse by rememberSaveable { mutableStateOf(false) }
    var qMath by rememberSaveable { mutableStateOf(false) }
    var qEssay by rememberSaveable { mutableStateOf(false) }

    // Derived validation + payload
    val selectedTypes = remember(qTerminology, qMultipleChoice, qFreeResponse, qMath, qEssay) {
        buildList {
            if (qTerminology) add("terminology")
            if (qMultipleChoice) add("multiple choice")
            if (qFreeResponse) add("free response")
            if (qMath) add("math")
            if (qEssay) add("essay")
        }
    }
    val isSurveyComplete = selectedAge != null && selectedGoal != null && selectedTypes.isNotEmpty()

    val scrollState = rememberScrollState()

    // Layout sizing (wider survey + button, consistent widths)
    val contentWidth = 0.92f
    val sidePad = 16.dp

    Box(Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(R.drawable.plant_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Goals Survey",
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Survey card
            Card(
                modifier = Modifier
                    .fillMaxWidth(contentWidth)
                    .padding(horizontal = sidePad)
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // AGE
                    SectionTitle("Your age?")
                    RadioRow("Child", selectedAge == "child") { selectedAge = "child" }
                    RadioRow("High school", selectedAge == "high school") { selectedAge = "high school" }
                    RadioRow("College", selectedAge == "college") { selectedAge = "college" }
                    RadioRow("Adult", selectedAge == "adult") { selectedAge = "adult" }

                    Divider()

                    // GOALS
                    SectionTitle("Your goals")
                    RadioRow("Introduce a topic", selectedGoal == "introduce a topic") { selectedGoal = "introduce a topic" }
                    RadioRow("Study", selectedGoal == "study") { selectedGoal = "study" }
                    RadioRow("Learn terminology", selectedGoal == "learn terminology") { selectedGoal = "learn terminology" }
                    RadioRow("Test me", selectedGoal == "test me") { selectedGoal = "test me" }

                    Divider()

                    // QUESTION TYPES
                    SectionTitle("Types of questions")
                    CheckboxRow("Terminology", qTerminology) { qTerminology = it }
                    CheckboxRow("Multiple choice", qMultipleChoice) { qMultipleChoice = it }
                    CheckboxRow("Free response", qFreeResponse) { qFreeResponse = it }
                    CheckboxRow("Math", qMath) { qMath = it }
                    CheckboxRow("Essay", qEssay) { qEssay = it }

                    Spacer(Modifier.height(6.dp))
                }
            }

            // spacing between card + button
            Spacer(Modifier.height(16.dp))

            // Save button (same width as card)
            val buttonMod = Modifier
                .fillMaxWidth(contentWidth)
                .padding(horizontal = sidePad)
                .height(56.dp)

            EnterButton(
                text = "Save",
                modifier = buttonMod,
                enabled = isSurveyComplete,
                onClick = {
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
                        navController.navigate("login")
                        return@EnterButton
                    }

                    if (!isSurveyComplete) {
                        Toast.makeText(context, "Please complete the survey.", Toast.LENGTH_SHORT).show()
                        return@EnterButton
                    }

                    val surveyData = mapOf(
                        "age" to selectedAge,
                        "goal" to selectedGoal,
                        "questionTypes" to selectedTypes
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("inputs")
                        .document(inputId)
                        .update(
                            mapOf(
                                "survey" to surveyData,
                                "surveyCompletedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(context, "Survey saved!", Toast.LENGTH_SHORT).show()
                            // Navigate to teaching with SAME inputId
                            navController.navigate("teaching/$inputId")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFF2B2B2B)
    )
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF2B2B2B))
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) } // toggle when tapping text/row
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF2B2B2B))
    }
}


@Composable
fun EnterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text)
    }
}
