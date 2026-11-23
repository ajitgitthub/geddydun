package com.personal.remindme

import android.content.Context
import android.media.MediaPlayer
import android.app.NotificationManager
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.util.Locale

/** What the alarm says out loud. */
fun spokenTitle(r: Reminder): String = if (r.type == Type.BILL) "${r.title} is due" else r.title

/**
 * While an alarm rings: says the title out loud, then again every 30 seconds.
 * The ring dips just while it speaks. Stops when the alarm is answered or goes quiet,
 * and starts again if it re-rings 30 minutes later.
 */
class AlarmVoice(private val ctx: Context, private val text: String) {
    private val h = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var stopped = false
    private var says = 0

    private companion object {
        const val EVERY_MS = 30_000L
    }

    fun start() {
        tts = TextToSpeech(ctx) { status ->
            if (status != TextToSpeech.SUCCESS || stopped) return@TextToSpeech
            val t = tts ?: return@TextToSpeech
            val india = Locale("en", "IN")
            t.language = if (t.isLanguageAvailable(india) >= TextToSpeech.LANG_AVAILABLE) india else Locale.ENGLISH
            pickVoice(t, Store.voiceMale)
            t.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            )
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { h.post { spoken() } }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) { h.post { spoken() } }
            })
            h.postDelayed({ say() }, 2000)
        }
    }

    private fun say() {
        if (stopped) return
        if (!AlarmService.ringingNow) return waitForRing()
        says++
        AlarmService.duck(true)
        // A short pause so the ring has faded before speaking.
        h.postDelayed({ if (!stopped) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "title$says") }, 400)
    }

    /** Done speaking: ring at full volume again, and say it again in a bit. */
    private fun spoken() {
        if (stopped) return
        AlarmService.duck(false)
        h.postDelayed({ say() }, EVERY_MS)
    }

    /** The ring timed out: say nothing until it re-rings. */
    private fun waitForRing() {
        h.postDelayed(object : Runnable {
            override fun run() {
                if (stopped) return
                if (AlarmService.ringingNow) say() else h.postDelayed(this, 5000)
            }
        }, 5000)
    }

    fun stop() {
        stopped = true
        h.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        AlarmService.duck(false)
    }
}

/**
 * Picks a female or male English voice the phone has, Indian English first.
 * Google's voices don't say their gender, so this goes by their names (and "male"/"female" when an engine says).
 * Returns false when the phone has no voice of that kind and the default is used.
 */
fun pickVoice(t: TextToSpeech, male: Boolean): Boolean {
    val all = try {
        t.voices.orEmpty().filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
    } catch (_: Exception) {
        return false
    }
    // Google's voice codes (the part after "-x-"), e.g. en-in-x-end-local.
    val maleCodes = setOf("end", "ene", "cxx", "iol", "iom", "tpd", "gbb", "gbd", "rjs")
    val femaleCodes = setOf("ena", "enc", "ahp", "iob", "iog", "tpc", "tpf", "sfg", "gba", "gbc", "gbg")
    fun code(n: String) = n.substringAfter("-x-", "").take(3)
    val wanted = all.filter { v ->
        val n = v.name.lowercase()
        if (male) code(n) in maleCodes || (n.contains("male") && !n.contains("female"))
        else code(n) in femaleCodes || n.contains("female")
    }
    val best = wanted.sortedWith(compareBy({ if (it.locale.country == "IN") 0 else 1 }, { it.quality.unaryMinus() })).firstOrNull()
        ?: return false
    return try {
        t.voice = best; true
    } catch (_: Exception) {
        false
    }
}

/** Settings row: female or male voice for the alarm title, with a sample. */
@Composable
fun VoiceRow() {
    val ctx = LocalContext.current
    var male by remember { mutableStateOf(Store.voiceMale) }
    var missing by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) { onDispose { tts?.shutdown() } }
    fun sample(m: Boolean) {
        tts?.shutdown()
        var t: TextToSpeech? = null
        t = TextToSpeech(ctx) { status ->
            val e = t ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            val india = Locale("en", "IN")
            e.language = if (e.isLanguageAvailable(india) >= TextToSpeech.LANG_AVAILABLE) india else Locale.ENGLISH
            missing = !pickVoice(e, m)
            e.speak("Wash clothes", TextToSpeech.QUEUE_FLUSH, null, "sample")
        }
        tts = t
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("Voice", color = TextC, fontSize = 15.sp)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("Female", !male) { male = false; Store.voiceMale = false; sample(false) }
            Pill("Male", male) { male = true; Store.voiceMale = true; sample(true) }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable { sample(male) }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.PlayArrow, null, tint = BlueText, modifier = Modifier.size(18.dp).offset(x = 1.dp))
                Text("Play sample", color = BlueText, fontSize = 13.sp)
            }
        }
        if (missing) {
            Text(
                "This phone has no ${if (male) "male" else "female"} voice installed, so it uses its usual one.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Settings row: the Sparkle sound on done, on or off. Tap the row to hear it and see what happened. */
@Composable
fun CelebrateRow() {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(Store.celebrateSound) }
    val last by Celebrate.last.collectAsState()
    fun test() {
        val r = Celebrate.play(ctx, force = true)
        Toast.makeText(ctx, r, Toast.LENGTH_LONG).show()
    }
    SetRow(
        "Celebration sound",
        sub = when {
            !on -> "Off"
            last.isNotBlank() -> "Sparkle, when something's marked done · last: ${last.lowercase()}"
            else -> "Sparkle, when something's marked done · tap to hear it"
        },
        onClick = { test() },
        trailing = {
            Switch(
                checked = on,
                onCheckedChange = { on = it; Store.celebrateSound = it; if (it) test() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnAccent, checkedTrackColor = Blue,
                    uncheckedThumbColor = Muted, uncheckedTrackColor = CardBg, uncheckedBorderColor = Line,
                ),
            )
        },
    )
}

/**
 * The Sparkle sound. Quiet when the phone is on silent or vibrate.
 * Plays on notification volume, or media volume when notifications are muted or Do Not Disturb is on.
 */
object Celebrate {
    private var player: MediaPlayer? = null

    /** What happened last time, for Settings: how it played, or why it didn't. */
    val last = MutableStateFlow("")

    fun play(ctx: Context, force: Boolean = false): String {
        if (!Store.celebrateSound && !force) return ""
        val app = ctx.applicationContext
        val am = app.getSystemService(AudioManager::class.java) ?: return note("Couldn't reach the phone's audio")
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return note("Didn't play: the phone is on silent or vibrate")
        val filter = app.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
        val dnd = filter != null && filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        fun up(stream: Int) = am.getStreamVolume(stream) > 0 && !am.isStreamMute(stream)
        val (usage, where) = when {
            !dnd && up(AudioManager.STREAM_NOTIFICATION) -> AudioAttributes.USAGE_NOTIFICATION_EVENT to "notification volume"
            up(AudioManager.STREAM_MUSIC) -> AudioAttributes.USAGE_MEDIA to "media volume"
            dnd -> return note("Didn't play: Do Not Disturb is on and media volume is off")
            else -> return note("Didn't play: notification and media volume are both off")
        }
        return try {
            player?.release()
            player = null
            val attrs = AudioAttributes.Builder().setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            val p = MediaPlayer.create(app, R.raw.sparkle, attrs, am.generateAudioSessionId())
                ?: return note("Didn't play: couldn't load the sound")
            p.setOnCompletionListener { it.release(); if (player === it) player = null }
            p.start()
            player = p
            note("Played on $where")
        } catch (e: Exception) {
            note("Didn't play: ${e.javaClass.simpleName}${e.message?.let { " ($it)" } ?: ""}")
        }
    }

    private fun note(s: String): String {
        last.value = s
        return s
    }
}

/** On app start: un-mute anything a talking alarm muted and never put back (e.g. the app was killed). */
fun restoreVoiceMutes(ctx: Context) {
    val p = ctx.getSharedPreferences("voice", Context.MODE_PRIVATE)
    val left = p.getString("muted", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }
    if (left.isEmpty() || AlarmService.ringingNow) return
    val am = ctx.getSystemService(AudioManager::class.java) ?: return
    left.forEach { s ->
        try {
            am.adjustStreamVolume(s, AudioManager.ADJUST_UNMUTE, 0)
        } catch (_: Exception) {
        }
    }
    p.edit().putString("muted", "").apply()
}

/** Settings row: say the alarm title out loud, on or off. */
@Composable
fun TalkToAlarmsRow() {
    var on by remember { mutableStateOf(Store.talkToAlarms) }
    SetRow(
        "Say the alarm title",
        sub = if (on) "Says it out loud when the alarm rings, then every 30 seconds" else "Off",
        trailing = {
            Switch(
                checked = on,
                onCheckedChange = { on = it; Store.talkToAlarms = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnAccent, checkedTrackColor = Blue,
                    uncheckedThumbColor = Muted, uncheckedTrackColor = CardBg, uncheckedBorderColor = Line,
                ),
            )
        },
    )
}
