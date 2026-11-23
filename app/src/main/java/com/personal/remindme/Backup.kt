package com.personal.remindme

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** One person's backup under a recovery code. */
data class BackupSlot(
    val uid: String, val name: String, val updatedAt: Long, val count: Int,
    val reminders: String, val prefs: String, val people: List<Person>, val notes: String = "",
)

/**
 * Everything on this phone, copied to Firebase a few seconds after every change.
 *  backups/{recoveryCode}/people/{uid}  name, uid, updatedAt, count, reminders (json), prefs (json),
 *                                       people [{uid, name}], githubAt (set by the GitHub copy)
 * A GitHub Action copies these into the private remind-me repo.
 */
object Backup {
    /** When this phone's backup last reached Firebase (0 = not yet). */
    val lastAt = MutableStateFlow(0L)
    /** When the GitHub copy last picked it up (0 = not set up yet). */
    val githubAt = MutableStateFlow(0L)
    val error = MutableStateFlow<String?>(null)
    /** True while restoring, so the half-restored phone isn't backed up over the backup. */
    @Volatile var restoring = false

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val h = Handler(Looper.getMainLooper())
    private val run = Runnable { now() }
    private lateinit var local: SharedPreferences
    // Kept here: SharedPreferences only holds listeners weakly.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> changed() }

    fun start(ctx: Context) {
        local = ctx.getSharedPreferences("backup", Context.MODE_PRIVATE)
        lastAt.value = local.getLong("lastAt", 0L)
        Store.onPrefsChanged(prefsListener)
    }

    /** The old shared code every install used to start with. The rules now refuse it. */
    const val LEGACY_CODE = "16092026"

    /** 12 random characters, no look-alikes (0/O, 1/I/L). */
    fun newCode(): String {
        val abc = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val r = java.security.SecureRandom()
        return (1..12).map { abc[r.nextInt(abc.length)] }.joinToString("")
    }

    private fun slot(code: String = Store.recoveryCode, uid: String = Cloud.uid) =
        db.collection("backups").document(code).collection("people").document(uid)

    /** Something changed: back up a few seconds later (many quick changes = one backup). */
    fun changed() {
        h.removeCallbacks(run)
        h.postDelayed(run, 3000)
    }

    fun now() {
        h.removeCallbacks(run)
        val me = Cloud.uid
        if (restoring || me.isBlank() || Store.userName.isBlank()) return
        // Take this phone's backup out of the old shared code, once.
        if (!local.getBoolean("legacyGone", false)) {
            slot(LEGACY_CODE, me).delete().addOnSuccessListener { local.edit().putBoolean("legacyGone", true).apply() }
        }
        val reminders = Store.remindersJson()
        var prefs = Store.prefsJson()
        // Firestore documents top out at 1 MB; drop the history log first if it gets that big.
        if (reminders.length + prefs.length > 900_000) {
            prefs = JSONObject(prefs).apply { remove("events") }.toString()
        }
        val at = System.currentTimeMillis()
        slot(uid = me).set(
            mapOf(
                "uid" to me, "name" to Store.userName, "updatedAt" to at,
                "count" to Store.reminders.value.count { !it.isTest },
                "reminders" to reminders, "prefs" to prefs, "notes" to Notes.backupJson(),
                "people" to Cloud.people.value.map { mapOf("uid" to it.uid, "name" to it.name) },
            ),
            SetOptions.merge(),
        ).addOnSuccessListener {
            lastAt.value = at
            local.edit().putLong("lastAt", at).apply()
            error.value = null
        }.addOnFailureListener { error.value = it.message }
    }

    /** Reads when the GitHub copy last ran, for Settings. */
    fun refreshGithub() {
        if (Cloud.uid.isBlank()) return
        slot().get().addOnSuccessListener { d -> githubAt.value = d.getLong("githubAt") ?: 0L }
    }

    /** New code: move this phone's backup there. */
    fun changeCode(newCode: String) {
        val old = Store.recoveryCode
        if (newCode == old) return
        if (Cloud.uid.isNotBlank()) slot(old).delete()
        Store.recoveryCode = newCode
        now()
    }

    /** All backups under a code (one per person). */
    fun find(code: String, done: (List<BackupSlot>?, String?) -> Unit) {
        if (Cloud.uid.isBlank()) return done(null, "Not connected yet. Check your internet and try again.")
        db.collection("backups").document(code).collection("people").get()
            .addOnSuccessListener { q ->
                done(
                    q.documents.map { d ->
                        @Suppress("UNCHECKED_CAST")
                        val ppl = (d.get("people") as? List<Map<String, Any?>>).orEmpty()
                            .mapNotNull { m -> (m["uid"] as? String)?.let { Person(it, m["name"] as? String ?: "Someone") } }
                        BackupSlot(
                            uid = d.id, name = d.getString("name") ?: "Someone", updatedAt = d.getLong("updatedAt") ?: 0L,
                            count = (d.getLong("count") ?: 0L).toInt(), reminders = d.getString("reminders") ?: "[]",
                            prefs = d.getString("prefs") ?: "{}", people = ppl, notes = d.getString("notes") ?: "",
                        )
                    }.sortedByDescending { it.updatedAt },
                    null,
                )
            }
            .addOnFailureListener { done(null, "Couldn't look that up: ${it.message}") }
    }

    /** Bring a backup onto this phone, reconnect with the same people, and retire the old backup. */
    fun restore(ctx: Context, code: String, s: BackupSlot): String? {
        val me = Cloud.uid
        if (me.isBlank()) return "Not connected yet. Check your internet and try again."
        restoring = true
        try {
            Store.restoreFrom(s.reminders, s.prefs)
            Notes.restoreFrom(s.notes)
        } catch (e: Exception) {
            restoring = false
            return "That backup couldn't be read."
        }
        Store.recoveryCode = code
        Cloud.updateName() // same name and connect code, now on this install
        val myName = Store.userName.ifBlank { "Someone" }
        val users = db.collection("users")
        val batch = db.batch()
        s.people.filter { it.uid != me }.forEach { p ->
            batch.set(users.document(me).collection("people").document(p.uid), mapOf("name" to p.name))
            // "replaces" tells their phone to drop the link to the old install.
            batch.set(users.document(p.uid).collection("people").document(me), mapOf("name" to myName, "replaces" to s.uid))
        }
        batch.commit()
        if (s.uid != me) slot(code, s.uid).delete()
        Scheduler.rescheduleAll(ctx)
        restoring = false
        now()
        return null
    }
}

fun agoText(ts: Long): String {
    if (ts <= 0) return "never"
    val m = (System.currentTimeMillis() - ts) / 60_000
    return when {
        m < 1 -> "just now"
        m < 60 -> "$m min ago"
        m < 24 * 60 -> "${m / 60} h ago"
        else -> fmtShortDate(ts)
    }
}

/** "K7QM4XHP92TA" → "K7QM 4XHP 92TA". */
private fun spaced(code: String) = code.chunked(4).joinToString(" ")

/** Settings → Backup. */
@Composable
fun BackupSection() {
    val ctx = LocalContext.current
    val last by Backup.lastAt.collectAsState()
    val github by Backup.githubAt.collectAsState()
    val err by Backup.error.collectAsState()
    val all by Store.reminders.collectAsState()
    var backingUp by remember { mutableStateOf(false) }
    // Finished (or failed): stop the spinner. Gives up after 15 s if there's no answer.
    LaunchedEffect(last, err) { backingUp = false }
    LaunchedEffect(backingUp) { if (backingUp) { delay(15_000); backingUp = false } }
    val code = Store.recoveryCode
    LaunchedEffect(Unit) { Backup.refreshGithub() }

    Caps("Backup")
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp).clip(RoundedCornerShape(18.dp)).background(CardBg).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Recovery code", color = Muted, fontSize = 12.sp)
        // Never shown, only copied (feedback: mask the code).
        Text(spaced("•".repeat(code.length)), fontFamily = Mono, fontSize = 26.sp, letterSpacing = 3.sp, color = TextC, modifier = Modifier.padding(top = 4.dp))
        Text("Copy it somewhere safe. You'll need it on a new phone.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        SoftButton("Copy code", BlueBg, BlueText, Modifier.fillMaxWidth().padding(top = 10.dp), height = 40, radius = 8) {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // Marked sensitive so Android doesn't show the code in the "copied" preview.
            val clip = ClipData.newPlainText("Geddydun recovery code", code)
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
            cm.setPrimaryClip(clip)
            Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
        }
    }
    SetRow(
        "Backed up", sub = "Every change, straight away",
        value = if (err != null) "failed" else agoText(last),
        valueColor = if (err != null) Red else if (last > 0) Green else Muted,
    )
    SetRow("Copy on GitHub", sub = "Checked every 30 min", value = if (github > 0) agoText(github) else "not set up yet")
    val reminders = all.count { !it.isTest && it.type != Type.BILL && it.items.isEmpty() }
    val lists = all.count { !it.isTest && it.items.isNotEmpty() }
    val bills = all.count { !it.isTest && it.type == Type.BILL }
    SetRow("What's backed up", sub = "$reminders reminders · $lists lists · $bills bills · settings")
    SetRow(
        if (backingUp) "Backing up…" else "Back up now", titleColor = Blue,
        trailing = if (backingUp) ({
            CircularProgressIndicator(color = Blue, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        }) else null,
        onClick = {
            if (!backingUp) {
                backingUp = true
                Backup.now()
            }
        },
    )
}

/** First screen → Restore from backup. */
@Composable
fun RestoreScreen(onClose: () -> Unit, onRestored: () -> Unit) {
    val ctx = LocalContext.current
    var code by remember { mutableStateOf("") }
    var slots by remember { mutableStateOf<List<BackupSlot>?>(null) }
    var picked by remember { mutableStateOf<BackupSlot?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun find() {
        val c = code.filter { it.isLetterOrDigit() }.uppercase()
        if (c.length < 4) {
            error = "Type your recovery code"; return
        }
        busy = true; error = null
        Backup.find(c) { found, err ->
            busy = false
            when {
                err != null -> error = err
                found.isNullOrEmpty() -> { slots = null; error = "No backup with that code. Check it and try again." }
                else -> { slots = found; picked = found.first() }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose, feedback = false)
        BigTitle("Restore", 30, Modifier.padding(top = 4.dp))
        Caps("Recovery code")
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter { c -> c.isLetterOrDigit() || c == ' ' }.take(24); slots = null; error = null },
            placeholder = { Text("XXXX XXXX XXXX") }, singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Mono, fontSize = 20.sp, letterSpacing = 3.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            shape = FieldShape, colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
        val found = slots
        if (found == null) {
            Column(Modifier.padding(top = 16.dp)) {
                BigButton(if (busy) "Finding your backup…" else "Find my backup", Blue, OnAccent, busy = busy) { find() }
            }
        } else {
            Caps("Whose backup?")
            found.forEach { s ->
                val sel = picked?.uid == s.uid
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) BlueBg else CardBg)
                        .then(if (sel) Modifier.border(1.dp, Blue, RoundedCornerShape(18.dp)) else Modifier)
                        .clickable { picked = s }.padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(s.name, color = if (sel) BlueText else TextC, fontSize = 15.sp)
                    Text("Backed up ${agoText(s.updatedAt)} · ${s.count} reminders", color = Muted, fontSize = 12.sp)
                }
            }
            val p = picked
            Column(Modifier.padding(top = 10.dp)) {
                BigButton(
                    if (p == null) "Pick one" else if (busy) "Restoring…" else "Restore ${p.name}'s backup",
                    Blue, OnAccent, busy = busy,
                ) {
                    if (p == null || busy) return@BigButton
                    busy = true
                    scope.launch {
                        delay(80) // let the spinner show before the work starts
                        val err = Backup.restore(ctx, code.filter { it.isLetterOrDigit() }.uppercase(), p)
                        busy = false
                        if (err != null) {
                            error = err
                        } else {
                            Toast.makeText(ctx, "Welcome back, ${Store.userName}", Toast.LENGTH_LONG).show()
                            onRestored()
                        }
                    }
                }
            }
            val others = p?.people?.joinToString(" and ") { it.name }.orEmpty()
            Text(
                (if (others.isNotBlank()) "Reconnects you with $others. " else "") +
                    "Errands in progress, live shared lists and shared notes come back as your own (private) ones.",
                color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 24.dp),
            )
        }
    }
}
