package io.github.jumergel.quizhaven.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object Layout {
    val sidePad = 24.dp
    val CardCornerRadius = 16.dp
    val CardElevation = 6.dp
    val contentHeight = 1f
    val contentWidth = 1f

    fun standardWidth(modifier: Modifier = Modifier): Modifier =
        modifier
            .fillMaxWidth(contentWidth)
            .padding(horizontal = sidePad)
}
