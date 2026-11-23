@file:OptIn(ExperimentalLayoutApi::class)

package com.personal.remindme

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A note with no alarm. shared = both people see and edit it, no OK needed. */
data class Note(
    val id: String, val title: String, val text: String, val shared: Boolean,
    val updatedAt: Long, val byName: String = "", val ownerUid: String = "",
)

/**
 * Private notes live in notes.json on this phone. Shared ones live in Firestore:
 *  notes/{id}  title, text, ownerUid, members[uids], updatedAt, updatedBy (name)
 * Pins are per phone.
 */
object Notes {
    val mine = MutableStateFlow<List<Note>>(emptyList())
    val shared = MutableStateFlow<List<Note>>(emptyList())
    val pinned = MutableStateFlow<Set<String>>(emptySet())

    private lateinit var file: File
    private lateinit var prefs: SharedPreferences
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var reg: ListenerRegistration? = null

    fun init(ctx: Context) {
        file = File(ctx.filesDir, "notes.json")
        prefs = ctx.getSharedPreferences("notes", Context.MODE_PRIVATE)
        mine.value = load()
        pinned.value = prefs.getStringSet("pinned", emptySet()).orEmpty().toSet()
    }

    fun listen() {
        val me = Cloud.uid
        if (me.isBlank()) return
        reg?.remove()
        reg = db.collection("notes").whereArrayContains("members", me).addSnapshotListener { snap, _ ->
            shared.value = snap?.documents?.map { d ->
                Note(
                    id = d.id, title = d.getString("title") ?: "", text = d.getString("text") ?: "", shared = true,
                    updatedAt = d.getLong("updatedAt") ?: 0L, byName = d.getString("updatedBy") ?: "",
                    ownerUid = d.getString("ownerUid") ?: "",
                )
            } ?: return@addSnapshotListener
        }
    }

    fun get(id: String): Note? = shared.value.find { it.id == id } ?: mine.value.find { it.id == id }

    fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(20)

    /** Saves a note; moving it between private and shared moves it between the phone and Firestore. */
    fun save(n: Note): String? {
        val now = System.currentTimeMillis()
        val me = Cloud.uid
        val wasShared = shared.value.any { it.id == n.id }
        if (n.shared) {
            if (me.isBlank()) return "Not connected yet. Check your internet and try again."
            val doc = db.collection("notes").document(n.id)
            if (wasShared) {
                doc.update(mapOf("title" to n.title, "text" to n.text, "updatedAt" to now, "updatedBy" to Store.userName))
            } else {
                doc.set(
                    mapOf(
                        "title" to n.title, "text" to n.text, "ownerUid" to me, "updatedAt" to now,
                        "updatedBy" to Store.userName,
                        "members" to (listOf(me) + Cloud.people.value.map { it.uid }).distinct(),
                    ),
                )
                writeMine(mine.value.filter { it.id != n.id })
            }
        } else {
            if (wasShared) db.collection("notes").document(n.id).delete()
            writeMine(mine.value.filter { it.id != n.id } + n.copy(updatedAt = now, byName = Store.userName))
        }
        Backup.changed()
        return null
    }

    fun delete(n: Note) {
        if (n.shared) db.collection("notes").document(n.id).delete()
        else writeMine(mine.value.filter { it.id != n.id })
        setPinned(n.id, false)
        Backup.changed()
    }

    fun setPinned(id: String, on: Boolean) {
        pinned.value = if (on) pinned.value + id else pinned.value - id
        prefs.edit().putStringSet("pinned", pinned.value).apply()
    }

    private fun writeMine(list: List<Note>) {
        mine.value = list
        val a = JSONArray()
        list.forEach { a.put(toJson(it)) }
        val tmp = File(file.parentFile, "notes.json.tmp")
        tmp.writeText(a.toString())
        tmp.renameTo(file)
    }

    private fun toJson(n: Note) = JSONObject().put("id", n.id).put("title", n.title).put("text", n.text)
        .put("shared", n.shared).put("updatedAt", n.updatedAt).put("byName", n.byName)

    private fun fromJson(o: JSONObject) = Note(
        id = o.getString("id"), title = o.optString("title"), text = o.optString("text"),
        shared = o.optBoolean("shared"), updatedAt = o.optLong("updatedAt"), byName = o.optString("byName"),
    )

    private fun load(): List<Note> = try {
        if (!file.exists()) emptyList() else {
            val a = JSONArray(file.readText())
            (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        }
    } catch (_: Exception) {
        emptyList()
    }

    /** All notes (private and shared) plus pins, for the backup. */
    fun backupJson(): String {
        val a = JSONArray()
        (mine.value + shared.value).forEach { a.put(toJson(it)) }
        return JSONObject().put("notes", a).put("pinned", JSONArray(pinned.value.toList())).toString()
    }

    /** Restores notes as private ones on this phone (shared ones belonged to the old install). */
    fun restoreFrom(json: String) {
        if (json.isBlank()) return
        val o = JSONObject(json)
        val a = o.optJSONArray("notes") ?: JSONArray()
        writeMine((0 until a.length()).map { fromJson(a.getJSONObject(it)).copy(shared = false) })
        val p = o.optJSONArray("pinned") ?: JSONArray()
        pinned.value = (0 until p.length()).map { p.getString(it) }.toSet()
        prefs.edit().putStringSet("pinned", pinned.value).apply()
    }
}

// ---------- links ----------

private val linkRx = Regex(
    "(https?://\\S+)|(www\\.\\S+)|(\\b[\\w.-]+\\.(?:com|in|org|net|io|co|app|me|gov|edu)(?:/\\S*)?)|(\\+?\\d[\\d -]{7,}\\d)",
)

/** Text with web links and phone numbers made tappable. */
fun linkified(text: String): AnnotatedString = buildAnnotatedString {
    val style = TextLinkStyles(SpanStyle(color = BlueText, textDecoration = TextDecoration.Underline))
    var at = 0
    linkRx.findAll(text).forEach { m ->
        append(text.substring(at, m.range.first))
        val raw = m.value.trimEnd('.', ',', ')', ';')
        val url = when {
            m.groups[4] != null -> "tel:" + raw.filter { it.isDigit() || it == '+' }
            raw.startsWith("http") -> raw
            else -> "https://$raw"
        }
        withLink(LinkAnnotation.Url(url, style)) { append(raw) }
        append(m.value.substring(raw.length))
        at = m.range.last + 1
    }
    append(text.substring(at))
}

/** Same, but just coloured (for previews, where a tap opens the note). */
private fun tinted(text: String): AnnotatedString = buildAnnotatedString {
    var at = 0
    linkRx.findAll(text).forEach { m ->
        append(text.substring(at, m.range.first))
        withStyle(SpanStyle(color = BlueText)) { append(m.value) }
        at = m.range.last + 1
    }
    append(text.substring(at))
}

/** "Alex + Sam" for shared, "Only you" for private. */
@Composable
private fun WhoTag(shared: Boolean) {
    val people by Cloud.people.collectAsState()
    val (icon, label) = if (shared) PeopleIcon to (listOf(Store.userName) + people.map { it.name }).joinToString(" + ")
    else Icons.Outlined.Lock to "Only you"
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = if (shared) BlueText else Muted, modifier = Modifier.size(13.dp))
        Text(label, color = if (shared) BlueText else Muted, fontSize = 12.sp)
    }
}

@Composable
private fun PinButton(on: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(PinIcon, if (on) "Unpin" else "Pin to top", tint = if (on) Blue else androidx.compose.ui.graphics.Color(0xFFB8B8BE))
    }
}

// ---------- notes tab ----------

@Composable
fun NotesScreen(open: (Screen) -> Unit) {
    val mine by Notes.mine.collectAsState()
    val shared by Notes.shared.collectAsState()
    val pins by Notes.pinned.collectAsState()
    var filter by rememberSaveable { mutableStateOf("all") }
    val all = (shared + mine).sortedByDescending { it.updatedAt }
    val shown = when (filter) {
        "shared" -> all.filter { it.shared }
        "private" -> all.filter { !it.shared }
        else -> all
    }
    val pinned = shown.filter { it.id in pins }
    val rest = shown.filter { it.id !in pins }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BigTitle("Notes", 44, Modifier.weight(1f))
                FeedbackButton()
            }
            Text("Things to keep, with no alarm", color = Muted, fontSize = 14.sp)
            if (all.isNotEmpty()) FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("All ${all.size}", filter == "all") { filter = "all" }
                Pill("Shared ${all.count { it.shared }}", filter == "shared") { filter = "shared" }
                Pill("Private ${all.count { !it.shared }}", filter == "private") { filter = "private" }
            }
        }
        if (all.isEmpty()) item {
            Text(
                "Nothing yet. Tap + to add a note, like the link to your emergency documents folder.",
                color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp),
            )
        }
        if (pinned.isNotEmpty()) {
            item { Caps("Pinned") }
            items(pinned, key = { "p_" + it.id }) { n -> NoteCard(n, true) { open(Screen.NoteView(n.id)) } }
        }
        if (rest.isNotEmpty()) {
            if (pinned.isNotEmpty()) item { Caps("Notes") }
            items(rest, key = { it.id }) { n -> NoteCard(n, false) { open(Screen.NoteView(n.id)) } }
        }
    }
}

@Composable
private fun NoteCard(n: Note, pinned: Boolean, onClick: () -> Unit) {
    TaskCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                n.title.ifBlank { "Untitled" }, fontSize = 16.sp, color = TextC,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (pinned) Icon(PinIcon, "Pinned", tint = Blue, modifier = Modifier.size(15.dp))
        }
        if (n.text.isNotBlank()) Text(
            tinted(n.text.trim()), color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        WhoTag(n.shared)
    }
}

// ---------- view ----------

@Composable
fun NoteViewScreen(id: String, onEdit: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val mine by Notes.mine.collectAsState()
    val shared by Notes.shared.collectAsState()
    val pins by Notes.pinned.collectAsState()
    val n = shared.find { it.id == id } ?: mine.find { it.id == id }
    if (n == null) {
        // Deleted (maybe by the other person).
        androidx.compose.runtime.LaunchedEffect(Unit) { onClose() }
        return
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClose) {
            PinButton(n.id in pins) { Notes.setPinned(n.id, n.id !in pins) }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit note", tint = androidx.compose.ui.graphics.Color(0xFFB8B8BE)) }
        }
        BigTitle(n.title.ifBlank { "Untitled" }, 30, Modifier.padding(top = 4.dp))
        WhoTag(n.shared)
        if (n.shared && n.byName.isNotBlank()) {
            Text("Last edited by ${n.byName} · ${agoText(n.updatedAt)}", color = Faint, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        SelectionContainer {
            Text(
                linkified(n.text), color = TextC, fontSize = 16.sp, lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            )
        }
        if (n.text.isNotBlank()) {
            SoftButton("Copy note", CardBg, BlueText, Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 30.dp), height = 48) {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(n.title, n.text))
                Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// ---------- add / edit ----------

@Composable
fun NoteEditScreen(id: String?, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val ctx = LocalContext.current
    val existing = remember(id) { id?.let { Notes.get(it) } }
    val noteId = remember(id) { id ?: Notes.newId() }
    val people by Cloud.people.collectAsState()
    val pins by Notes.pinned.collectAsState()
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var text by rememberSaveable { mutableStateOf(existing?.text ?: "") }
    // New notes are shared by default when there's someone to share with.
    var shared by rememberSaveable { mutableStateOf(existing?.shared ?: people.isNotEmpty()) }
    var pin by rememberSaveable { mutableStateOf(noteId in pins) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val me = Store.userName.ifBlank { "Me" }

    fun save() {
        if (title.isBlank() && text.isBlank()) {
            error = "Write something first"; return
        }
        val err = Notes.save(
            Note(noteId, title.trim(), text.trim(), shared, System.currentTimeMillis(), ownerUid = existing?.ownerUid ?: Cloud.uid),
        )
        if (err != null) {
            error = err; return
        }
        Notes.setPinned(noteId, pin)
        onSaved(noteId)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        TopRow(Icons.Outlined.Close, "Close", onClose) {
            PinButton(pin) { pin = !pin }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BigTitle(if (existing == null) "New note" else "Edit note", 28, Modifier.weight(1f))
            Text(
                "Save", color = OnAccent, fontSize = 15.sp,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Blue).clickable { save() }
                    .padding(horizontal = 20.dp, vertical = 9.dp),
            )
        }
        Caps("Title")
        OutlinedTextField(
            value = title, onValueChange = { title = it; error = null },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = FieldShape, colors = fieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            placeholder = { Text("Emergency documents") },
        )
        Caps("Note")
        OutlinedTextField(
            value = text, onValueChange = { text = it; error = null },
            modifier = Modifier.fillMaxWidth(), minLines = 6, shape = FieldShape, colors = fieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            placeholder = { Text("Links, numbers, anything to keep") },
        )
        error?.let { Text(it, color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        Caps("Who can see it")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Only $me", !shared, Icons.Outlined.Lock) { shared = false }
            if (people.isNotEmpty() || shared) {
                Pill((listOf(me) + people.map { it.name }).joinToString(" + "), shared, PeopleIcon) { shared = true }
            }
        }
        Text(
            if (shared) "Both of you can edit it. Changes show up straight away, no OK needed."
            else "Stays on this phone only.",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
        )
        if (existing != null) {
            Text(
                "Delete note", color = Red, fontSize = 15.sp,
                modifier = Modifier.padding(top = 26.dp, bottom = 30.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { confirmDelete = true }.padding(vertical = 10.dp, horizontal = 4.dp),
            )
        }
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SheetBg,
            title = { Text("Delete this note?") },
            text = {
                Text(
                    if (existing.shared) "It's removed from both phones." else "It can't be brought back.",
                    color = Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Notes.delete(existing); confirmDelete = false
                    Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show()
                    onClose()
                }) { Text("Delete", color = Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Muted) } },
        )
    }
}

/** Search: notes whose title or text contain the query. */
fun searchNotes(query: String): List<Note> =
    if (query.isBlank()) emptyList()
    else (Notes.shared.value + Notes.mine.value).filter { it.title.contains(query, true) || it.text.contains(query, true) }
