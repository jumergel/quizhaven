package io.github.jumergel.quizhaven

import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.github.jumergel.quizhaven.ui.Layout
import io.github.jumergel.quizhaven.ui.theme.Cedar
import io.github.jumergel.quizhaven.ui.theme.SoftGreen
import io.github.jumergel.quizhaven.ui.theme.Typography
import kotlin.math.ceil
import kotlin.math.max

data class ScorePoint(
    val attempt: Int,
    val percent: Int,
)

@Composable
fun StatsScreen(
    navController: NavController,
    projectId: String,
    inputIdForBackNav: String
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val uid = auth.currentUser?.uid
    if (uid == null) {
        Toast.makeText(context, "Sign in error", Toast.LENGTH_SHORT).show()
        navController.navigate("login")
        return
    }

    var isLoading by remember { mutableStateOf(true) }
    var points by remember { mutableStateOf<List<ScorePoint>>(emptyList()) }
    var problemSpots by remember { mutableStateOf<List<String>>(emptyList()) }

    // -------------------------
    // Scores listener
    // -------------------------
    DisposableEffect(uid, projectId) {
        val ref = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("outputs").document("scores")
            .collection("items")

        val reg: ListenerRegistration = ref
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    isLoading = false
                    Toast.makeText(context, "Stats load failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                val docs = snap?.documents ?: emptyList()
                val parsed = docs.mapIndexedNotNull { idx, d ->
                    val percent = (d.getLong("percent") ?: return@mapIndexedNotNull null).toInt()
                    ScorePoint(attempt = idx + 1, percent = percent)
                }

                points = parsed
                isLoading = false
            }

        onDispose { reg.remove() }
    }

    // -------------------------
    // problem spots listener
    // -------------------------
    DisposableEffect(uid, projectId) {
        val ref = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("outputs").document("problemSpots")

        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                // just ignore
                return@addSnapshotListener
            }
            val topics = snap?.get("problemSpots") as? List<*> ?: emptyList<Any>()
            val parsed = topics
                .mapNotNull { it?.toString()?.trim() }
                .filter { it.isNotEmpty() }
                .take(10)

            problemSpots = parsed
        }

        onDispose { reg.remove() }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.plant_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // full-screen white cover
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White.copy(alpha = 0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = Layout.sidePad),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(Layout.contentWidth)
                        .fillMaxHeight(Layout.contentHeight),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LearnTestNavBar(
                        selectedTab = TopTab.Stats,
                        onLearnClick = { navController.navigate("teaching/$projectId/$inputIdForBackNav") },
                        onTestClick = { navController.navigate("quiz/$projectId/$inputIdForBackNav") },
                        onStatsClick = { /* already here */ }
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "My Progress",
                        style = Typography.displayMedium,
                        color = Cedar,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    when {
                        isLoading -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }

                        points.isEmpty() -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("No quiz attempts yet.", color = Cedar) }
                        }

                        else -> {
                            ProgressLineChart(
                                points = points,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "X = attempt number, Y = score (%)",
                        style = Typography.bodyMedium,
                        color = Cedar.copy(alpha = 0.75f)
                    )

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(14.dp))

//DISPLAY USERS PROBLEM SPOTS
                    Text(
                        text = "Problem Spots",
                        style = MaterialTheme.typography.displayLarge,
                        color = Cedar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Left
                    )

                    Spacer(Modifier.height(10.dp))

                    if (problemSpots.isEmpty()) {
                        Text(
                            text = "No problem spots yet — take a quiz first!",
                            style = Typography.bodyMedium,
                            color = Cedar.copy(alpha = 0.75f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp)
                        )
                    } else {
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            FlowRowChips(items = problemSpots)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowRowChips(items: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        var row = mutableListOf<String>()
        var countInRow = 0

        @Composable
        fun flushRow() {
            if (row.isEmpty()) return
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { s ->
                    AssistChip(
                        onClick = { /* optional */ },
                        label = { Text(s, style = Typography.bodyMedium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SoftGreen.copy(alpha = 0.25f),
                            labelColor = Cedar
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            row = mutableListOf()
            countInRow = 0
        }

        for (s in items) {
            row.add(s)
            countInRow++
            if (countInRow >= 3) flushRow() // 3 chips per row
        }
        flushRow()
    }
}

@Composable //graph to display progress
private fun ProgressLineChart(
    points: List<ScorePoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padLeft = 90f
        val padRight = 22f
        val padTop = 22f
        val padBottom = 78f

        val plotW = (w - padLeft - padRight).coerceAtLeast(1f)
        val plotH = (h - padTop - padBottom).coerceAtLeast(1f)

        val n = points.size
        val maxY = max(100, points.maxOf { it.percent })
        val minY = 0

        fun xPos(attempt: Int): Float {
            if (n == 1) return padLeft + plotW / 2f
            val t = (attempt - 1).toFloat() / (n - 1).toFloat()
            return padLeft + t * plotW
        }

        fun yPos(percent: Int): Float {
            val t = (percent - minY).toFloat() / (maxY - minY).toFloat()
            return padTop + (1f - t) * plotH
        }

        // axes
        val axisColor = Color(0xFF9E9E9E)
        drawLine(axisColor, Offset(padLeft, padTop), Offset(padLeft, padTop + plotH), 3f)
        drawLine(axisColor, Offset(padLeft, padTop + plotH), Offset(padLeft + plotW, padTop + plotH), 3f)

        // y ticks
        val tickVals = buildList {
            add(0); add(50); add(100)
            if (maxY > 100) add(maxY)
        }.distinct().sorted()

        val yPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 22f
            isAntiAlias = true
        }

        tickVals.forEach { v ->
            val y = yPos(v)
            drawLine(
                color = Color(0xFFBDBDBD),
                start = Offset(padLeft, y),
                end = Offset(padLeft + plotW, y),
                strokeWidth = 1.2f
            )
            drawContext.canvas.nativeCanvas.drawText(
                v.toString(),
                12f,
                y + 8f,
                yPaint
            )
        }

        // data points
        val offsets = points.map { p -> Offset(xPos(p.attempt), yPos(p.percent)) }

        // line
        for (i in 0 until offsets.size - 1) {
            drawLine(
                color = Cedar,
                start = offsets[i],
                end = offsets[i + 1],
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        // markers
        offsets.forEach { o ->
            drawCircle(color = Cedar, radius = 7f, center = o)
            drawCircle(color = Color.White, radius = 4f, center = o)
        }

        // x labels
        val maxLabels = 6
        val step = max(1, ceil(n / maxLabels.toFloat()).toInt())

        val xPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun drawXLabel(attempt: Int) {
            val x = xPos(attempt).coerceIn(padLeft + 6f, padLeft + plotW - 6f)
            val y = padTop + plotH + 32f
            drawContext.canvas.nativeCanvas.drawText(attempt.toString(), x, y, xPaint)
        }

        drawXLabel(1)
        for (a in 1..n) {
            if (a != 1 && a != n && a % step == 0) drawXLabel(a)
        }
        if (n > 1) drawXLabel(n)

        // Axis titles
        val titlePaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }

        drawContext.canvas.nativeCanvas.drawText(
            "Attempt",
            padLeft + plotW / 2f,
            padTop + plotH + 62f,
            titlePaint.apply { textAlign = Paint.Align.CENTER }
        )

        drawContext.canvas.nativeCanvas.drawText(
            "Score",
            padLeft + 12f,
            padTop + 28f,
            titlePaint.apply { textAlign = Paint.Align.LEFT }
        )
    }
}
