package io.github.jumergel.quizhaven

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.jumergel.quizhaven.ui.theme.Cedar
import io.github.jumergel.quizhaven.ui.theme.Typography

enum class TopTab {
    Learn,
    Test,
    Stats
}

@Composable
fun LearnTestNavBar(
    selectedTab: TopTab,
    onLearnClick: () -> Unit,
    onTestClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // teaching and quiz screens
        Row {
            TextButton(onClick = onLearnClick) {
                Text(
                    text = "Learn",
                    color = if (selectedTab == TopTab.Learn) Cedar else Color.Gray,
                    style = Typography.titleMedium
                )
            }

            TextButton(onClick = onTestClick) {
                Text(
                    text = "Test",
                    color = if (selectedTab == TopTab.Test) Cedar else Color.Gray,
                    style = Typography.titleMedium
                )
            }
        }

        // statss screen
        TextButton(onClick = onStatsClick) {
            Text(
                text = "Stats",
                color = if (selectedTab == TopTab.Stats) Cedar else Color.Gray,
                style = Typography.titleMedium
            )
        }
    }

    HorizontalDivider()
}
