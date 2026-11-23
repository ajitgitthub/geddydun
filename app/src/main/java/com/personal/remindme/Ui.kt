package com.personal.remindme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- colours ----------

val Bg = Color(0xFF0A0A0B)
val CardBg = Color(0xFF1A1A1D)
val SheetBg = Color(0xFF151518)
val DockBg = Color(0xFF1C1C1F)
val Line = Color(0xFF1E1E22)
val Muted = Color(0xFF8E8E94)
val Faint = Color(0xFF6E6E74)
val TextC = Color(0xFFEDEDF0)
// The accent is light red, like the bells in the icon. (Named Blue from when it was blue.)
val Blue = Color(0xFFF09595)
val BlueBg = Color(0xFF3A1F22)
val BlueText = Color(0xFFF7C1C1)
/** Text and icons on the accent colour. */
val OnAccent = Color(0xFF4A1B1B)
/** Errors and overdue: a stronger red, so it doesn't look like the accent. */
val Red = Color(0xFFE5534B)
val Amber = Color(0xFFEF9F27)
val Green = Color(0xFF1D9E75)
val GreenDark = Color(0xFF04342C)
val Mono = FontFamily.Monospace
val FieldShape = RoundedCornerShape(12.dp)

/** Every digit the same width, so times and counts line up in columns. */
private val TabularType = Typography().run {
    fun TextStyle.t() = copy(fontFeatureSettings = "tnum")
    copy(
        displayLarge = displayLarge.t(), displayMedium = displayMedium.t(), displaySmall = displaySmall.t(),
        headlineLarge = headlineLarge.t(), headlineMedium = headlineMedium.t(), headlineSmall = headlineSmall.t(),
        titleLarge = titleLarge.t(), titleMedium = titleMedium.t(), titleSmall = titleSmall.t(),
        bodyLarge = bodyLarge.t(), bodyMedium = bodyMedium.t(), bodySmall = bodySmall.t(),
        labelLarge = labelLarge.t(), labelMedium = labelMedium.t(), labelSmall = labelSmall.t(),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Blue,
            onPrimary = OnAccent,
            background = Bg,
            surface = SheetBg,
            onBackground = TextC,
            onSurface = TextC,
            surfaceVariant = CardBg,
            onSurfaceVariant = Muted,
            surfaceContainerHigh = SheetBg,
            surfaceContainerLow = SheetBg,
        ),
        typography = TabularType,
        content = content,
    )
}

/** Black page with a red glow from the top. */
fun Modifier.glow(strong: Boolean = false): Modifier = drawBehind {
    drawRect(Bg)
    val center = Offset(if (strong) size.width / 2f else size.width * 0.2f, 0f)
    val radius = if (strong) size.height * 0.62f else size.height * 0.42f
    drawRect(
        Brush.radialGradient(
            0f to Color(0xFF7A1414),
            0.45f to Color(0xFF2A0809),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
    )
}

// ---------- icons not in the core set ----------

private fun pathIcon(name: String, d: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(pathData = addPathNodes(d), fill = SolidColor(Color.White))
        .build()

val EyeIcon: ImageVector by lazy {
    pathIcon(
        "Eye",
        "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5z" +
            "M12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z" +
            "M12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z",
    )
}

val ChecklistIcon: ImageVector by lazy {
    pathIcon(
        "Checklist",
        "M22,7h-9v2h9V7zM22,15h-9v2h9V15zM5.54,11L2,7.46l1.41,-1.41l2.12,2.12l4.24,-4.24l1.41,1.41L5.54,11z" +
            "M5.54,19L2,15.46l1.41,-1.41l2.12,2.12l4.24,-4.24l1.41,1.41L5.54,19z",
    )
}

val PinIcon: ImageVector by lazy {
    pathIcon(
        "Pin",
        "M16,9V4h1c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3s0.45,1 1,1h1v5c0,1.66 -1.34,3 -3,3v2h5.97v7l1,1 1,-1v-7H19v-2c-1.66,0 -3,-1.34 -3,-3z",
    )
}

val BellIcon: ImageVector by lazy {
    pathIcon(
        "Bell",
        "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z",
    )
}

val BellOffIcon: ImageVector by lazy {
    pathIcon(
        "BellOff",
        "M20,18.69L7.84,6.14 5.27,3.49 4,4.76l2.8,2.8v0.01c-0.52,0.99 -0.8,2.16 -0.8,3.42v5l-2,2v1h13.73l2,2L21,19.72l-1,-1.03zM12,22c1.11,0 2,-0.89 2,-2h-4c0,1.11 0.89,2 2,2zM18,14.68V11c0,-3.08 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68c-0.15,0.03 -0.29,0.08 -0.42,0.12 -0.1,0.03 -0.2,0.07 -0.3,0.11h-0.01c-0.01,0 -0.01,0 -0.02,0.01 -0.23,0.09 -0.46,0.2 -0.68,0.31 0,0 -0.01,0 -0.01,0.01L18,14.68z",
    )
}

val PeopleIcon: ImageVector by lazy {
    pathIcon(
        "People",
        "M16,11c1.66,0 2.99,-1.34 2.99,-3S17.66,5 16,5c-1.66,0 -3,1.34 -3,3s1.34,3 3,3zM8,11c1.66,0 2.99,-1.34 2.99,-3S9.66,5 8,5C6.34,5 5,6.34 5,8s1.34,3 3,3zM8,13c-2.33,0 -7,1.17 -7,3.5V19h14v-2.5c0,-2.33 -4.67,-3.5 -7,-3.5zM16,13c-0.29,0 -0.62,0.02 -0.97,0.05 1.16,0.84 1.97,1.97 1.97,3.45V19h6v-2.5c0,-2.33 -4.67,-3.5 -7,-3.5z",
    )
}

/** Speech bubble with three dots. */
val FeedbackIcon: ImageVector by lazy {
    pathIcon(
        "Feedback",
        "M20,2H4c-1.1,0 -2,0.9 -2,2v18l4,-4h14c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM9,11H7V9h2v2zM13,11h-2V9h2v2zM17,11h-2V9h2v2z",
    )
}

val BillIcon: ImageVector by lazy {
    pathIcon(
        "Bill",
        "M18,17H6v-2h12v2zM18,13H6v-2h12v2zM18,9H6V7h12v2zM3,22l1.5,-1.5L6,22l1.5,-1.5L9,22l1.5,-1.5L12,22l1.5,-1.5L15,22l1.5,-1.5L18,22l1.5,-1.5L21,22V2l-1.5,1.5L18,2l-1.5,1.5L15,2l-1.5,1.5L12,2l-1.5,1.5L9,2L7.5,3.5L6,2L4.5,3.5L3,2v20z",
    )
}

val ErrandIcon: ImageVector by lazy {
    pathIcon(
        "Errand",
        "M18,6h-2c0,-2.21 -1.79,-4 -4,-4S8,3.79 8,6H6c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM12,4c1.1,0 2,0.9 2,2h-4c0,-1.1 0.9,-2 2,-2zM18,20H6V8h2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V8h4v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V8h2v12z",
    )
}

val CheckLaterIcon: ImageVector by lazy {
    pathIcon(
        "Check later",
        "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
    )
}

val NotesIcon: ImageVector by lazy {
    pathIcon(
        "Notes",
        "M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6zM16,18H8v-2h8v2zM16,14H8v-2h8v2zM13,9V3.5L18.5,9H13z",
    )
}

val TimelineIcon: ImageVector by lazy {
    pathIcon("Timeline", "M3,5h18v2H3V5zM3,11h12v2H3v-2zM3,17h15v2H3v-2z")
}

// ---------- shared pieces ----------

/** Big title with the blue full stop. */
@Composable
fun BigTitle(text: String, size: Int = 30, modifier: Modifier = Modifier, center: Boolean = false) {
    val dot = text.lastOrNull()?.isLetterOrDigit() == true
    Text(
        buildAnnotatedString {
            append(text)
            if (dot) withStyle(SpanStyle(color = Blue)) { append(".") }
        },
        modifier = modifier,
        fontSize = size.sp,
        lineHeight = (size + 6).sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.5).sp,
        color = TextC,
        textAlign = if (center) TextAlign.Center else null,
    )
}

/** Small spaced-out section label. */
@Composable
fun Caps(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), modifier.padding(top = 20.dp, bottom = 6.dp),
        fontSize = 11.sp, letterSpacing = 0.8.sp, color = Faint, fontFamily = Mono,
    )
}

@Composable
fun HLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
fun SoftButton(
    text: String, container: Color, content: Color,
    modifier: Modifier = Modifier, height: Int = 56, busy: Boolean = false, radius: Int = 18, onClick: () -> Unit,
) {
    Box(
        modifier.height(height.dp).clip(RoundedCornerShape(radius.dp)).background(container)
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Working on it: a small spinner, and taps are ignored until it's done.
            if (busy) CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            Text(text, color = content, fontSize = 16.sp)
        }
    }
}

@Composable
fun BigButton(text: String, container: Color, content: Color, busy: Boolean = false, onClick: () -> Unit) {
    SoftButton(text, container, content, Modifier.fillMaxWidth(), busy = busy, onClick = onClick)
}

@Composable
fun Pill(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val c = if (selected) BlueText else TextC
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) BlueBg else CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Icons take the label's colour.
        if (icon != null) Icon(icon, null, tint = c, modifier = Modifier.size(16.dp))
        Text(label, color = c, fontSize = 14.sp)
    }
}

/** 30m / 1h / 2h / 4h. */
@Composable
fun SnoozeRow(onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(30 to "30m", 60 to "1h", 120 to "2h", 240 to "4h").forEach { (m, l) ->
            SoftButton(l, CardBg, TextC, Modifier.weight(1f), height = 54) { onPick(m) }
        }
    }
}

@Composable
fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Blue,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = CardBg,
    unfocusedContainerColor = CardBg,
    cursorColor = Blue,
    focusedTextColor = TextC,
    unfocusedTextColor = TextC,
    focusedPlaceholderColor = Faint,
    unfocusedPlaceholderColor = Faint,
    focusedLabelColor = BlueText,
    unfocusedLabelColor = Muted,
)
