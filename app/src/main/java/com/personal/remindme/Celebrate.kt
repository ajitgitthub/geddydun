package com.personal.remindme

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Cheer(val title: String, val line: String)

/** "Great job" lines after the alarm's done button. */
object Cheers {
    fun pick(r: Reminder): Cheer {
        val t = r.title
        if (r.assignedToMe && r.taskPerson.isNotBlank()) {
            val who = r.taskPerson
            return Cheer(
                listOf("$who's going to love this", "Point scored", "Teamwork", "Look at you go", "Delivered").random(),
                listOf("$who asked, you delivered.", "$who will see it's done.", "$t, done.", "Gold star from $who, probably.").random(),
            )
        }
        return if (r.type == Type.BILL) Cheer(
            listOf("Nailed it", "Done and dusted", "Boss move", "Adulting level: expert", "Paid and proud", "That's handled").random(),
            listOf(
                "$t paid. Future you says thanks.", "That bill never stood a chance.", "One less thing on your mind.",
                "Paid before it could nag you twice.", "$t, sorted.",
            ).random(),
        ) else Cheer(
            listOf("Look at you go", "Hero of the household", "Geddydun", "Crushed it", "Off the list", "That's how it's done").random(),
            listOf(
                "$t, handled.", "Future you says thanks.", "Feels good, doesn't it?", "One less thing.",
                "The house runs better because of you.",
            ).random(),
        )
    }
}

private class Bit(val angle: Float, val speed: Float, val spin: Float, val color: Color, val w: Float, val h: Float, val life: Float)

private val bitColors = listOf(Blue, Red, Green, Amber, TextC, BlueText)

/** Green tick, confetti and a cheer. Closes itself after a few seconds, or on tap. */
@Composable
fun Celebration(cheer: Cheer, onDone: () -> Unit) {
    val view = LocalView.current
    val ring = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val words = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val bits = remember {
        List(80) {
            Bit(
                angle = Random.nextFloat() * 6.283f, speed = 0.45f + Random.nextFloat() * 0.75f,
                spin = (Random.nextFloat() - 0.5f) * 1440f, color = bitColors[it % bitColors.size],
                w = 7f + Random.nextFloat() * 5f, h = 11f + Random.nextFloat() * 6f, life = 0.75f + Random.nextFloat() * 0.25f,
            )
        }
    }
    LaunchedEffect(Unit) {
        launch { burst.animateTo(1f, tween(2400, easing = LinearEasing)) }
        launch { ring.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
        launch {
            delay(330)
            tick.animateTo(1f, tween(320))
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
        launch {
            delay(430)
            words.animateTo(1f, tween(420))
        }
        delay(3200)
        onDone()
    }
    Box(
        Modifier.fillMaxSize().background(Bg.copy(alpha = 0.96f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.42f)
            val reach = size.minDimension * 0.55f
            val p = burst.value
            bits.forEach { b ->
                val t = (p / b.life).coerceAtMost(1f)
                if (t >= 1f) return@forEach
                // Fly out fast, slow down, then fall.
                val out = 1f - (1f - t) * (1f - t) * (1f - t)
                val x = c.x + cos(b.angle) * b.speed * reach * out
                val y = c.y + sin(b.angle) * b.speed * reach * out - reach * 0.35f * out + size.height * 0.55f * t * t
                val alpha = if (t > 0.7f) (1f - t) / 0.3f else 1f
                val w = b.w * 0.7f * density
                val h = b.h * 0.7f * density
                rotate(b.spin * t, Offset(x, y)) {
                    drawRect(b.color.copy(alpha = alpha), Offset(x - w / 2, y - h / 2), Size(w, h))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 28.dp)) {
            Box(
                Modifier.size(116.dp).scale(ring.value).clip(CircleShape).background(Green),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(64.dp)) {
                    val s = size.width / 24f
                    val path = Path().apply {
                        moveTo(5f * s, 12.5f * s); lineTo(9.5f * s, 17f * s); lineTo(19f * s, 7.5f * s)
                    }
                    val m = PathMeasure().apply { setPath(path, false) }
                    val part = Path()
                    m.getSegment(0f, m.length * tick.value, part, true)
                    drawPath(part, GreenDark, style = Stroke(width = 3.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Box(Modifier.padding(top = 28.dp).graphicsLayer { alpha = words.value; translationY = (1f - words.value) * 40f }) {
                BigTitle(cheer.title, 30, center = true)
            }
            Text(
                cheer.line, color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp).graphicsLayer {
                    alpha = (words.value * 1.4f - 0.4f).coerceIn(0f, 1f); translationY = (1f - words.value) * 40f
                },
            )
        }
        Text(
            "TAP ANYWHERE", fontFamily = Mono, fontSize = 11.sp, letterSpacing = 0.8.sp, color = Faint,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )
    }
}
