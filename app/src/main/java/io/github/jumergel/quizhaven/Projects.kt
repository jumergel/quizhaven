package io.github.jumergel.quizhaven

import android.widget.Toast
import io.github.jumergel.quizhaven.ui.Layout

import com.google.firebase.firestore.Query
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.google.firebase.firestore.ListenerRegistration

data class ProjectItem(
    val id: String,
    val title: String
)

@Composable
fun ProjectsScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // UI state
    var projects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    // listen to user's projects
    DisposableEffect(Unit) {
        var reg: ListenerRegistration? = null
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
            navController.navigate("login")
        } else {
            reg = db.collection("users")
                .document(uid)
                .collection("projects")
                .orderBy("createdAt")
                .addSnapshotListener { snap, err ->
                    if (err != null) return@addSnapshotListener
                    val list = snap?.documents?.map { doc ->
                        ProjectItem(
                            id = doc.id,
                            title = (doc.getString("title") ?: "Untitled")
                        )
                    } ?: emptyList()
                    projects = list
                }
        }

        onDispose { reg?.remove() }
    }


    Box(Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(R.drawable.plant_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "My Study Sets",
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Card(
                modifier =  Layout.standardWidth().weight(1f),


                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "No study sets yet.\nTap + to create one!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF2B2B2B)
                        )
                    }
                } else {
                    LazyColumn( //lazy column to show projects that fit on screen
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(projects, key = { it.id }) { p ->
                            ProjectRow(
                                title = p.title,
                                onClick = {
                                    val uid = auth.currentUser?.uid
                                    if (uid == null) {
                                        Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
                                        navController.navigate("login")
                                        return@ProjectRow
                                    }

                                    //  most recent input in this project
                                    //will navigate to screens depending on whether user
                                    //was in progress for creating the project or is
                                    //starting entirely from scratch
                                    db.collection("users")
                                        .document(uid)
                                        .collection("projects")
                                        .document(p.id)
                                        .collection("inputs")
                                        .orderBy("createdAt", Query.Direction.DESCENDING)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener { snap ->
                                            // If no input docs were created -> go to TextInput
                                            if (snap.isEmpty) {
                                                navController.navigate("textInput/${p.id}")
                                                return@addOnSuccessListener
                                            }

                                            val doc = snap.documents.first()
                                            val inputId = doc.id

                                            val initialInput = doc.getString("initialInput")?.trim().orEmpty()
                                            val surveyMap = doc.get("survey") as? Map<*, *>

                                            val hasText = initialInput.isNotBlank()
                                            val hasSurvey = surveyMap != null && surveyMap.isNotEmpty()

                                            when {
                                                !hasText -> { //text not inputted
                                                    navController.navigate("textInput/${p.id}")
                                                }
                                                !hasSurvey -> { //survey not done
                                                    navController.navigate("survey/${p.id}/$inputId")
                                                }
                                                else -> { //both done already
                                                    navController.navigate("teaching/${p.id}/$inputId")
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Open failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                }

                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }

        // + button in the bottom right for creating new project
        FloatingActionButton(
            onClick = {
                newTitle = ""
                showCreateDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            containerColor = Color.White,
            contentColor = Color(0xFF2B2B2B)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create project")
        }

        // Create project dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isCreating) showCreateDialog = false
                },
                title = { Text("New Project") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enter a project title:")
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            singleLine = true,
                            placeholder = { Text("e.g., CV Exam Prep") },
                            enabled = !isCreating
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isCreating && newTitle.isNotBlank(),
                        onClick = {
                            val uid = auth.currentUser?.uid
                            if (uid == null) {
                                Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                                return@TextButton
                            }

                            isCreating = true
                            val data = mapOf(
                                "title" to newTitle.trim(),
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            db.collection("users")
                                .document(uid)
                                .collection("projects")
                                .add(data)
                                .addOnSuccessListener { ref ->
                                    isCreating = false
                                    showCreateDialog = false
                                    Toast.makeText(context, "Project created!", Toast.LENGTH_SHORT).show()
                                    // Go straight to TextInput under this project
                                    navController.navigate("textInput/${ref.id}")
                                }
                                .addOnFailureListener { e ->
                                    isCreating = false
                                    Toast.makeText(context, "Create failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        }
                    ) {
                        Text(if (isCreating) "Creating..." else "Create")
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isCreating,
                        onClick = { showCreateDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProjectRow(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF2B2B2B)
        )
    }
}
