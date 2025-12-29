package io.github.jumergel.quizhaven.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.jumergel.quizhaven.R


val Newsreader = FontFamily(
    //regular
    Font(R.font.newsreader_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.newsreader_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.newsreader_bold, FontWeight.Bold, FontStyle.Normal),

    //italic
    Font(R.font.newsreader_italic_variable, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.newsreader_italic_variable, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.newsreader_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

//global text style
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 50.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.SemiBold,
        fontSize = 35.sp,
        lineHeight = 50.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
    )
)