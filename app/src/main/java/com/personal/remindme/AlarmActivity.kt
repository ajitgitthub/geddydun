package com.personal.remindme

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        // Back does nothing: you have to pick an option.
        onBackPressedDispatcher.addCallback(this) { }

        setContent {
            AppTheme {
                val queue by AlarmService.queue.collectAsState()
                val quiet by AlarmService.quiet.collectAsState()
                var rang by remember { mutableStateOf(false) }
                // Screen stays on only while it rings. When it goes quiet, close so the phone can sleep;
                // the notification brings it back, and it opens itself again on the next ring.
                LaunchedEffect(quiet) {
                    if (!quiet) {
                        rang = true
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        setTurnScreenOn(false)
                        if (rang) finish()
                    }
                }
                val all by Store.reminders.collectAsState()
                var cheer by remember { mutableStateOf<Cheer?>(null) }
                val id = queue.firstOrNull()
                val r = all.find { it.id == id }
                val c = cheer
                if (c != null) {
                    Celebration(c) { cheer = null }
                } else if (id == null) {
                    LaunchedEffect(Unit) { finish() }
                } else if (r == null) {
                    LaunchedEffect(id) { AlarmService.resolve(this@AlarmActivity, id) }
                } else {
                    val onAction: (Act, Int) -> Unit = { act, mins ->
                        if (act == Act.DONE) cheer = Cheers.pick(r)
                        Actions.handle(this@AlarmActivity, r.id, act, mins)
                    }
                    // Say the title out loud while it rings.
                    val voice = remember(r.id) {
                        if (Store.talkToAlarms) AlarmVoice(this@AlarmActivity, spokenTitle(r)) else null
                    }
                    DisposableEffect(voice) {
                        voice?.start()
                        onDispose { voice?.stop() }
                    }
                    AlarmScreen(
                        r = r,
                        onAction = onAction,
                        onToggle = { i ->
                            Store.get(r.id)?.let { cur ->
                                val items = cur.items.toMutableList()
                                items[i] = items[i].toggled()
                                Store.upsert(cur.copy(items = items))
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AlarmService.activityVisible = true
    }

    override fun onStop() {
        AlarmService.activityVisible = false
        super.onStop()
    }
}

private val topFmt = DateTimeFormatter.ofPattern("EEE dd", Locale.ENGLISH)

@Composable
fun AlarmScreen(
    r: Reminder, onAction: (Act, Int) -> Unit, onToggle: (Int) -> Unit,
) {
    val isBill = r.type == Type.BILL
    val due = toLdt(r.dueAt)
    val time = fmtTime(r.dueAt)
    val top = buildString {
        append(LocalDate.now().format(topFmt).uppercase(Locale.ENGLISH))
        if (!isBill && r.assignedToMe && r.dueBy > 0) {
            append(" · DUE ")
            append(fmtShortDate(fromLdt(java.time.LocalDate.ofEpochDay(r.dueBy).atStartOfDay())).uppercase(Locale.ENGLISH))
        }
        if (isBill) {
            val d = due.toLocalDate()
            append(" · ")
            append(if (d == LocalDate.now()) "DUE TODAY" else "DUE " + fmtShortDate(r.dueAt).uppercase(Locale.ENGLISH))
        }
    }
    val pending = r.items.withIndex().filter { !it.value.done }
    val doneCount = r.items.size - pending.size

    Box(Modifier.fillMaxSize().glow(strong = true)) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(if (isBill) 36.dp else 20.dp))
            Text(top, fontFamily = Mono, fontSize = 12.sp, letterSpacing = 1.sp, color = Muted)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                Text(time.substringBefore(" "), fontFamily = Mono, fontSize = 60.sp, letterSpacing = (-1).sp, color = TextC)
                Text(
                    " " + time.substringAfter(" ", ""), fontFamily = Mono, fontSize = 22.sp, color = Muted,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Spacer(Modifier.height(if (isBill) 26.dp else 14.dp))
            BigTitle(if (isBill) "${r.title} is due" else r.title, 28, center = true)
            val sub = when {
                !isBill && r.items.isNotEmpty() ->
                    "${pending.size} item${if (pending.size == 1) "" else "s"} left"
                else -> repeatShort(r)
            }
            if (r.assignedToMe && r.taskPerson.isNotBlank()) {
                Text("From ${r.taskPerson}", color = BlueText, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
            }
            if (r.isBoth && r.taskPerson.isNotBlank()) {
                Text(r.bothNames, color = BlueText, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
            }
            val named = r.assignedToMe || r.isBoth
            Text(sub, color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = if (named) 4.dp else 8.dp))
            if (r.rings > 1) {
                Text("Reminded ${r.rings} times", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (r.notes.isNotBlank()) {
                Text(
                    r.notes, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (isBill && r.history.isNotEmpty()) {
                val p = r.history.first()
                Text(
                    "Last paid ${fmtShortDate(p.paidAt)} · ${paidLabel(p)}",
                    color = Muted, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 26.dp).clip(RoundedCornerShape(12.dp))
                        .background(CardBg).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }

            if (!isBill && r.items.isNotEmpty()) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 18.dp)) {
                    items(pending) { (i, item) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = false, onCheckedChange = { onToggle(i) },
                                colors = CheckboxDefaults.colors(uncheckedColor = Color(0xFF4A4A50)),
                            )
                            Text(item.name, color = TextC, fontSize = 16.sp)
                        }
                        HLine()
                    }
                    if (doneCount > 0) item {
                        Text(
                            "Marked done ($doneCount)", color = Muted, fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 10.dp),
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            BigButton(
                if (isBill) "I'll pay it now" else "I'll do it now",
                container = Green,
                content = GreenDark,
            ) { onAction(Act.DONE, 0) }
            if (r.trailing) {
                // Counts from today, so say where the next one lands.
                val next = toLdt(Scheduler.advance(r).dueAt).toLocalDate()
                val days = next.toEpochDay() - LocalDate.now().toEpochDay()
                val gap = when (days) {
                    1L -> "tomorrow"
                    7L -> "a week from today"
                    else -> "$days days from today"
                }
                Text(
                    "Next: ${next.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))}, $gap",
                    color = BlueText, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                )
            }
            Text(
                "Remind me in", color = Muted, fontSize = 13.sp, letterSpacing = 0.3.sp,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
            SnoozeRow { onAction(Act.SNOOZE, it) }
            Text(
                if (isBill) "Remind me tomorrow, $time" else "Not today, tomorrow $time",
                color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable { onAction(Act.TOMORROW, 0) }.padding(16.dp),
            )
        }
    }
}
