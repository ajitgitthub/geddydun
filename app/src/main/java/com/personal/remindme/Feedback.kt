@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.personal.remindme

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow

/** Something one of us noticed about the app, to improve it later. */
data class Feedback(
    val id: String, val fromUid: String, val fromName: String, val kind: String,
    val screen: String, val text: String, val ts: Long, val fixed: Boolean,
)

/**
 * Feedback both of us log from the app.
 *  feedback/{auto}  fromUid, fromName, kind, screen, text, ts, fixed, seen[uids who can read it]
 */
object FeedbackBox {
    val all = MutableStateFlow<List<Feedback>>(emptyList())
    /** True while the feedback sheet is open. */
    val sheet = MutableStateFlow(false)

    private val db by lazy { FirebaseFirestore.getInstance() }
    private var reg: ListenerRegistration? = null

    fun listen() {
        val me = Cloud.uid
        if (me.isBlank()) return
        reg?.remove()
        reg = db.collection("feedback").whereArrayContains("seen", me).addSnapshotListener { snap, _ ->
            all.value = snap?.documents?.map { d ->
                Feedback(
                    id = d.id, fromUid = d.getString("fromUid") ?: "", fromName = d.getString("fromName") ?: "Someone",
                    kind = d.getString("kind") ?: "Idea", screen = d.getString("screen") ?: "",
                    text = d.getString("text") ?: "", ts = d.getLong("ts") ?: 0L, fixed = d.getBoolean("fixed") == true,
                )
            }?.sortedByDescending { it.ts } ?: return@addSnapshotListener
        }
    }

    /** Saves straight away; Firestore sends it when there's internet. */
    fun add(kind: String, screen: String, text: String): String? {
        val me = Cloud.uid
        if (me.isBlank()) return "Not connected yet. Check your internet and try again."
        val seen = (listOf(me) + Cloud.people.value.map { it.uid }).distinct()
        db.collection("feedback").add(
            mapOf(
                "fromUid" to me, "fromName" to Store.userName.ifBlank { "Someone" }, "kind" to kind,
                "screen" to screen, "text" to text, "ts" to System.currentTimeMillis(), "fixed" to false, "seen" to seen,
            ),
        )
        return null
    }

    fun setFixed(f: Feedback, fixed: Boolean) {
        db.collection("feedback").document(f.id).update("fixed", fixed)
    }

    /** Open feedback as plain text, to paste somewhere. */
    fun asText(list: List<Feedback>): String = buildString {
        append("Geddydun feedback (${list.size} open)\n")
        list.forEach { append("\n- [${it.kind} · ${it.screen} · ${it.fromName}] ${it.text}") }
    }
}

/** What the feedback sheet calls the screen you were on. */
fun Screen.label(): String = when (this) {
    Screen.Today -> "Today"
    Screen.Lists -> "Lists"
    Screen.Settings -> "Settings"
    is Screen.View -> "Reminder details"
    is Screen.Edit -> if (id == null) "New reminder" else "Edit reminder"
    is Screen.Changes -> "Changes"
    Screen.History -> "History"
    is Screen.Review -> "Review errand"
    Screen.Search -> "Search"
    is Screen.Paid -> "Paid cycles"
    Screen.Digest -> "Check-later digest"
    Screen.Permissions -> "Permissions"
    Screen.AddPerson -> "Add a person"
    Screen.Name -> "Name"
    Screen.Feedback -> "Feedback"
    Screen.Restore -> "Restore"
    Screen.Notes -> "Notes"
    is Screen.NoteView -> "Note"
    is Screen.NoteEdit -> if (id == null) "New note" else "Edit note"
}

/** The small speech bubble at the top right of every screen. big = the Today screen's round buttons. */
@Composable
fun FeedbackButton(big: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier.size(if (big) 44.dp else 30.dp).clip(CircleShape).background(CardBg)
            .clickable { FeedbackBox.sheet.value = true },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            FeedbackIcon, "Feedback",
            tint = if (big) Color(0xFFC9C9CE) else Muted,
            modifier = Modifier.size(if (big) 20.dp else 15.dp),
        )
    }
}

@Composable
fun FeedbackSheet(screen: String, onSeeAll: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val all by FeedbackBox.all.collectAsState()
    var kind by remember { mutableStateOf("Bug") }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).imePadding()) {
            BigTitle("Feedback", 22)
            Text("From the $screen screen", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Bug", "Idea", "Change").forEach { k -> Pill(k, kind == k) { kind = k } }
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it; error = null },
                placeholder = { Text("What's on your mind?") },
                colors = fieldColors(), shape = RoundedCornerShape(12.dp), minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            error?.let { Text(it, color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
            Box(Modifier.padding(top = 14.dp)) {
                BigButton("Send", Blue, OnAccent) {
                    if (text.isBlank()) {
                        error = "Type a line first"; return@BigButton
                    }
                    val err = FeedbackBox.add(kind, screen, text.trim())
                    if (err != null) {
                        error = err
                    } else {
                        Toast.makeText(ctx, "Feedback saved", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            }
            Text(
                "See all feedback" + if (all.isNotEmpty()) " (${all.size})" else "",
                color = BlueText, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { onSeeAll() }.padding(10.dp),
            )
        }
    }
}

private fun ago(ts: Long): String {
    val m = (System.currentTimeMillis() - ts) / 60_000
    return when {
        m < 1 -> "just now"
        m < 60 -> "${m}m ago"
        m < 24 * 60 -> "${m / 60}h ago"
        m < 48 * 60 -> "yesterday"
        else -> fmtShortDate(ts)
    }
}

@Composable
fun FeedbackScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val all by FeedbackBox.all.collectAsState()
    var showFixed by remember { mutableStateOf(false) }
    val open = all.filter { !it.fixed }
    val fixed = all.filter { it.fixed }
    val shown = if (showFixed) fixed else open

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose, feedback = false)
            BigTitle("Feedback", 34, Modifier.padding(top = 4.dp))
            FlowRow(Modifier.padding(top = 10.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Open ${open.size}", !showFixed) { showFixed = false }
                Pill("Fixed ${fixed.size}", showFixed) { showFixed = true }
            }
        }
        if (shown.isEmpty()) item {
            Text(
                if (showFixed) "Nothing marked fixed yet."
                else "Nothing open. Tap the speech bubble on any screen to add something.",
                color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp),
            )
        }
        items(shown, key = { it.id }) { f ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg)
                    .combinedClickable(onClick = {}, onLongClick = {
                        FeedbackBox.setFixed(f, !f.fixed)
                        Toast.makeText(ctx, if (f.fixed) "Moved back to open" else "Marked fixed", Toast.LENGTH_SHORT).show()
                    })
                    .heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = BlueText)) { append(f.fromName) }
                        withStyle(SpanStyle(color = Muted)) { append(" · ${f.kind} · ${f.screen} · ${ago(f.ts)}") }
                    },
                    fontSize = 12.sp,
                )
                Text(f.text, color = if (f.fixed) Muted else TextC, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (!showFixed && open.isNotEmpty()) item {
            SoftButton("Copy open feedback", CardBg, BlueText, Modifier.fillMaxWidth().padding(top = 10.dp)) {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Geddydun feedback", FeedbackBox.asText(open)))
                Toast.makeText(ctx, "Copied ${open.size}", Toast.LENGTH_SHORT).show()
            }
        }
        if (shown.isNotEmpty()) item {
            Text(
                "Long-press one to mark it ${if (showFixed) "open again" else "fixed"}",
                color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}
