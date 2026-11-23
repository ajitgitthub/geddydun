package com.personal.remindme

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow

data class Person(val uid: String, val name: String)
data class Invite(val id: String, val listId: String, val fromUid: String, val fromName: String, val title: String, val count: Int)
data class LogEntry(val who: String, val uid: String, val text: String, val ts: Long)

/** An errand someone assigned to you, waiting for you to accept. */
data class TaskRequest(
    val id: String, val fromUid: String, val fromName: String, val title: String, val notes: String,
    val dueBy: Long, val remindAt: Long, val repeat: Repeat, val everyDays: Int, val anchorDay: Int,
    val nth: Int, val weekday: Int, val weekMask: Int, val items: List<Item>,
    /** A shared alarm: rings for both of us once accepted. */
    val both: Boolean = false,
    val fromDone: Boolean = false,
) {
    /** The errand as it would sit on your phone. */
    fun toReminder(id: Long = Store.newId()) = Reminder(
        id = id, type = Type.ERRAND, title = title, notes = notes, dueAt = remindAt,
        repeat = repeat, everyDays = everyDays, anchorDay = anchorDay, nth = nth, weekday = weekday,
        weekMask = weekMask, fromDone = fromDone, items = items, taskId = this.id, taskRole = "to", taskPerson = fromName,
        taskPersonUid = fromUid, taskStatus = "pending", dueBy = dueBy,
        taskBoth = both, taskBaseAt = if (both) remindAt else 0L,
    )
}

/** Changes one person suggested to an assigned errand, waiting for the other's OK. */
data class Proposal(
    val taskId: String, val byUid: String, val byName: String, val ts: Long,
    val title: String, val notes: String, val dueBy: Long,
    val repeat: Repeat, val everyDays: Int, val anchorDay: Int, val nth: Int, val weekday: Int, val weekMask: Int,
    /** New reminder time. 0 = unchanged. */
    val remindAt: Long = 0L,
    /** New list (names and ids only; ticks stay live). null = unchanged. */
    val items: List<Item>? = null,
) {
    /** The errand with these changes applied. */
    fun applyTo(r: Reminder) = r.copy(
        title = title, dueBy = dueBy, repeat = repeat, everyDays = everyDays,
        anchorDay = anchorDay, nth = nth, weekday = weekday, weekMask = weekMask,
        dueAt = if (remindAt > 0) remindAt else r.dueAt,
        items = items?.map { pi -> r.items.find { it.id == pi.id }?.copy(name = pi.name) ?: pi } ?: r.items,
    )

    companion object {
        fun of(taskId: String, byUid: String, byName: String, r: Reminder, remindAt: Long = 0L, items: List<Item>? = null) = Proposal(
            taskId, byUid, byName, System.currentTimeMillis(), r.title, r.notes, r.dueBy,
            r.repeat, r.everyDays, r.anchorDay, r.nth, r.weekday, r.weekMask, remindAt, items,
        )
    }
}

/** Same items with the same names (ticks ignored). */
fun sameItems(a: List<Item>, b: List<Item>): Boolean =
    a.associate { it.id to it.name } == b.associate { it.id to it.name }

/** What a proposal changes: label, old value, new value. */
fun proposalDiff(r: Reminder, p: Proposal): List<Triple<String, String, String>> {
    val out = mutableListOf<Triple<String, String, String>>()
    fun day(d: Long) = if (d <= 0) "No due date" else fmtDate(java.time.LocalDate.ofEpochDay(d))
    if (p.title != r.title) out += Triple("Title", r.title, p.title)
    if (p.dueBy != r.dueBy) out += Triple("Due by", day(r.dueBy), day(p.dueBy))
    if (p.remindAt > 0 && p.remindAt != r.dueAt) out += Triple("Reminder", fmtWhen(r.dueAt), fmtWhen(p.remindAt))
    p.items?.let { list ->
        val before = r.items.associateBy { it.id }
        val added = list.filter { it.id !in before }.map { it.name }
        val removed = r.items.filter { o -> list.none { it.id == o.id } }.map { it.name }
        val renamed = list.mapNotNull { n -> before[n.id]?.takeIf { it.name != n.name }?.let { it.name to n.name } }
        if (added.isNotEmpty()) out += Triple("Added", "", added.joinToString(", "))
        if (removed.isNotEmpty()) out += Triple("Removed", removed.joinToString(", "), "")
        renamed.forEach { (o, n) -> out += Triple("Renamed", o, n) }
    }
    val newRule = p.applyTo(r)
    if (repeatShort(newRule) != repeatShort(r)) out += Triple("Repeat", repeatShort(r), repeatShort(newRule))
    return out
}

/**
 * Shared lists, live.
 *
 * Firestore layout:
 *  users/{uid}                 name, code
 *  users/{uid}/people/{uid2}   name            (both sides written when you connect)
 *  codes/{code}                uid, name
 *  lists/{listId}              ownerUid, ownerName, title, members[], memberNames{uid:name}
 *  lists/{listId}/items/{id}   name, done, ts, by, byUid, doneBy
 *  lists/{listId}/log/{auto}   who, uid, text, ts
 *  invites/{listId_toUid}      listId, fromUid, fromName, toUid, title, count, status
 *  tasks/{id}                  fromUid, fromName, toUid, toName, title, notes, dueBy, remindAt,
 *                              repeat fields, items[], status, proposal{...}
 *  tasks/{id}/log/{auto}       who, uid, text, ts
 *
 * Only the list items are shared. Alarms, times and repeats stay on each phone.
 */
object Cloud {
    val ready = MutableStateFlow(false)
    val people = MutableStateFlow<List<Person>>(emptyList())
    val invites = MutableStateFlow<List<Invite>>(emptyList())
    val logs = MutableStateFlow<Map<String, List<LogEntry>>>(emptyMap())
    val error = MutableStateFlow<String?>(null)
    val requests = MutableStateFlow<List<TaskRequest>>(emptyList())
    /** taskId → changes waiting for an OK (from either person). */
    val proposals = MutableStateFlow<Map<String, Proposal>>(emptyMap())

    private lateinit var app: Context
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val listListeners = mutableMapOf<String, List<ListenerRegistration>>()
    private var peopleReg: ListenerRegistration? = null
    private var invitesReg: ListenerRegistration? = null
    private var tasksInReg: ListenerRegistration? = null
    private var tasksOutReg: ListenerRegistration? = null
    private val taskLogRegs = mutableMapOf<String, ListenerRegistration>()

    val uid: String get() = auth.currentUser?.uid ?: ""
    private val myName: String get() = Store.userName.ifBlank { "Someone" }

    fun start(ctx: Context) {
        app = ctx.applicationContext
        Store.onLocalChange = { old, new -> onLocalChange(old, new) }
        Store.onLocalDelete = { r -> onLocalDelete(r) }
        try {
            if (auth.currentUser != null) onSignedIn()
            else auth.signInAnonymously()
                .addOnSuccessListener { onSignedIn() }
                .addOnFailureListener { error.value = "Couldn't connect: ${it.message}" }
        } catch (e: Exception) {
            error.value = "Couldn't connect: ${e.message}"
        }
    }

    private fun onSignedIn() {
        ensureProfile()
        listenPeople()
        listenInvites()
        listenTasks()
        FeedbackBox.listen()
        Notes.listen()
        Store.reminders.value.filter { it.sharedId != null }.forEach { listenList(it.sharedId!!) }
        ready.value = true
    }

    // ---------- profile and code ----------

    private fun newCode(): String {
        val abc = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val r = java.security.SecureRandom()
        val s = (1..6).map { abc[r.nextInt(abc.length)] }.joinToString("")
        return s.take(2) + "-" + s.drop(2)
    }

    private fun ensureProfile() {
        if (Store.myCode.isBlank()) Store.myCode = newCode()
        val code = Store.myCode
        db.collection("users").document(uid).set(mapOf("name" to myName, "code" to code), SetOptions.merge())
        db.collection("codes").document(code).set(mapOf("uid" to uid, "name" to myName))
    }

    fun updateName() {
        if (uid.isBlank()) return
        ensureProfile()
        Store.reminders.value.filter { it.sharedId != null }.forEach { r ->
            db.collection("lists").document(r.sharedId!!).update(
                if (r.sharedOwner) mapOf("ownerName" to myName, "memberNames.$uid" to myName)
                else mapOf("memberNames.$uid" to myName),
            )
        }
    }

    // ---------- people ----------

    private fun listenPeople() {
        peopleReg?.remove()
        peopleReg = db.collection("users").document(uid).collection("people")
            .addSnapshotListener { snap, _ ->
                val docs = snap?.documents
                // Someone restored Geddydun on a new install: drop the link to their old one.
                val replaced = docs?.mapNotNull { it.getString("replaces") }?.toSet().orEmpty()
                replaced.forEach { old ->
                    if (docs?.any { it.id == old } == true) {
                        db.collection("users").document(uid).collection("people").document(old).delete()
                    }
                }
                people.value = docs?.filter { it.id !in replaced }?.map { Person(it.id, it.getString("name") ?: "Someone") }
                    ?.sortedBy { it.name.lowercase() } ?: people.value
            }
    }

    /** Connect with someone using their code. */
    fun connect(code: String, done: (String?) -> Unit) {
        val c = code.trim().uppercase().replace(" ", "").let { if (it.length == 6) it.take(2) + "-" + it.drop(2) else it }
        if (uid.isBlank()) return done("Not connected yet. Check your internet and try again.")
        if (c == Store.myCode) return done("That's your own code.")
        db.collection("codes").document(c).get()
            .addOnSuccessListener { d ->
                val other = d.getString("uid")
                val name = d.getString("name") ?: "Someone"
                if (!d.exists() || other == null) return@addOnSuccessListener done("No one has that code. Check it and try again.")
                if (other == uid) return@addOnSuccessListener done("That's your own code.")
                val batch = db.batch()
                batch.set(db.collection("users").document(uid).collection("people").document(other), mapOf("name" to name))
                batch.set(db.collection("users").document(other).collection("people").document(uid), mapOf("name" to myName))
                batch.commit().addOnSuccessListener { done(null) }.addOnFailureListener { done("Couldn't connect: ${it.message}") }
            }
            .addOnFailureListener { done("Couldn't look up that code: ${it.message}") }
    }

    fun removePerson(p: Person) {
        db.collection("users").document(uid).collection("people").document(p.uid).delete()
        db.collection("users").document(p.uid).collection("people").document(uid).delete()
        // Take them off lists I own.
        Store.reminders.value.filter { it.sharedOwner && it.sharedId != null }.forEach { r ->
            removeMember(r, p.uid)
        }
    }

    // ---------- sharing a list ----------

    /** Upload a local errand list (if needed) and invite people to it. */
    fun share(r: Reminder, to: List<Person>, done: (String?) -> Unit) {
        if (uid.isBlank()) return done("Not connected yet. Check your internet and try again.")
        val listId = r.sharedId ?: db.collection("lists").document().id
        val list = db.collection("lists").document(listId)
        fun sendInvites() {
            val batch = db.batch()
            to.forEach { p ->
                batch.set(
                    db.collection("invites").document("${listId}_${p.uid}"),
                    mapOf(
                        "listId" to listId, "fromUid" to uid, "fromName" to myName, "toUid" to p.uid,
                        "title" to r.title, "count" to r.items.count { !it.done }, "status" to "pending",
                        "ts" to System.currentTimeMillis(),
                    ),
                )
            }
            batch.commit().addOnSuccessListener { done(null) }.addOnFailureListener { done("Couldn't send: ${it.message}") }
        }
        if (r.sharedId != null) return sendInvites()

        // First time: create the list and copy items up.
        val batch = db.batch()
        batch.set(
            list,
            mapOf(
                "ownerUid" to uid, "ownerName" to myName, "title" to r.title,
                "members" to listOf(uid), "memberNames" to mapOf(uid to myName),
                "updatedAt" to System.currentTimeMillis(),
            ),
        )
        val items = r.items.map { if (it.byUid.isBlank()) it.copy(by = myName, byUid = uid) else it }
        items.forEach { batch.set(list.collection("items").document(it.id), itemMap(it)) }
        batch.set(list.collection("log").document(), logMap("started sharing this list"))
        batch.commit()
            .addOnSuccessListener {
                Store.upsertQuiet(r.copy(sharedId = listId, sharedOwner = true, items = items))
                listenList(listId)
                sendInvites()
            }
            .addOnFailureListener { done("Couldn't share: ${it.message}") }
    }

    fun accept(inv: Invite, done: (String?) -> Unit) {
        val list = db.collection("lists").document(inv.listId)
        list.update(
            mapOf(
                "members" to FieldValue.arrayUnion(uid),
                "memberNames.$uid" to myName,
                "updatedAt" to System.currentTimeMillis(),
            ),
        ).addOnSuccessListener {
            db.collection("invites").document(inv.id).update("status", "accepted")
            list.collection("log").document().set(logMap("joined the list"))
            if (Store.reminders.value.none { it.sharedId == inv.listId }) {
                Store.upsertQuiet(
                    Reminder(
                        id = Store.newId(), type = Type.ERRAND, title = inv.title, dueAt = 0L,
                        sharedId = inv.listId, sharedOwner = false, sharedBy = inv.fromName,
                    ),
                )
            }
            listenList(inv.listId)
            done(null)
        }.addOnFailureListener { done("Couldn't join: ${it.message}") }
    }

    fun decline(inv: Invite) {
        db.collection("invites").document(inv.id).update("status", "declined")
    }

    /** Owner: take one person off a list. */
    fun removeMember(r: Reminder, memberUid: String) {
        val id = r.sharedId ?: return
        db.collection("lists").document(id).update(
            mapOf(
                "members" to FieldValue.arrayRemove(memberUid),
                "memberNames.$memberUid" to FieldValue.delete(),
                "updatedAt" to System.currentTimeMillis(),
            ),
        )
        db.collection("invites").document("${id}_$memberUid").delete()
    }

    /** Owner: stop sharing with everyone. Keeps the list on this phone. */
    fun stopSharing(r: Reminder) {
        val id = r.sharedId ?: return
        stopListening(id)
        db.collection("lists").document(id).delete()
        Store.upsertQuiet(r.copy(sharedId = null, sharedOwner = false, sharedWith = emptyList()))
    }

    /** Other person: leave a list shared with you. */
    fun leave(r: Reminder) {
        val id = r.sharedId ?: return
        stopListening(id)
        db.collection("lists").document(id).collection("log").document().set(logMap("left the list"))
        db.collection("lists").document(id).update(
            mapOf(
                "members" to FieldValue.arrayRemove(uid),
                "memberNames.$uid" to FieldValue.delete(),
                "updatedAt" to System.currentTimeMillis(),
            ),
        )
        Store.deleteQuiet(r.id)
    }

    // ---------- live sync ----------

    private fun itemMap(i: Item) = mapOf(
        "name" to i.name, "done" to i.done, "ts" to i.ts,
        "by" to i.by, "byUid" to i.byUid, "doneBy" to i.doneBy,
    )

    private fun logMap(text: String) = mapOf(
        "who" to myName, "uid" to uid, "text" to text, "ts" to System.currentTimeMillis(),
    )

    private fun stopListening(listId: String) {
        listListeners.remove(listId)?.forEach { it.remove() }
    }

    private fun localFor(listId: String) = Store.reminders.value.find { it.sharedId == listId }

    private fun listenList(listId: String) {
        if (listId in listListeners) return
        val list = db.collection("lists").document(listId)

        val head = list.addSnapshotListener { snap, err ->
            val r = localFor(listId) ?: return@addSnapshotListener
            if (err != null) {
                if (err.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && !r.sharedOwner) removedFromList(r)
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            if (!snap.exists()) {
                if (r.sharedOwner) Store.upsertQuiet(r.copy(sharedId = null, sharedOwner = false, sharedWith = emptyList()))
                else removedFromList(r)
                return@addSnapshotListener
            }
            @Suppress("UNCHECKED_CAST")
            val members = (snap.get("members") as? List<String>) ?: emptyList()
            if (!r.sharedOwner && uid !in members) return@addSnapshotListener removedFromList(r)
            @Suppress("UNCHECKED_CAST")
            val names = (snap.get("memberNames") as? Map<String, String>) ?: emptyMap()
            val others = names.filterKeys { it != uid }.values.sorted()
            val title = snap.getString("title") ?: r.title
            val owner = snap.getString("ownerName") ?: r.sharedBy
            val updated = if (r.sharedOwner) r.copy(sharedWith = others)
            else r.copy(title = title, sharedBy = owner, sharedWith = others)
            if (updated != r) Store.upsertQuiet(updated)
        }

        val items = list.collection("items").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            val r = localFor(listId) ?: return@addSnapshotListener
            val remote = snap.documents.map { d ->
                Item(
                    name = d.getString("name") ?: "",
                    done = d.getBoolean("done") ?: false,
                    id = d.id,
                    ts = d.getLong("ts") ?: 0L,
                    by = d.getString("by") ?: "",
                    byUid = d.getString("byUid") ?: "",
                    doneBy = d.getString("doneBy") ?: "",
                )
            }.sortedByDescending { it.ts }
            if (remote != r.items) Store.upsertQuiet(r.copy(items = remote))
        }

        val log = list.collection("log").orderBy("ts", Query.Direction.DESCENDING).limit(60)
            .addSnapshotListener { snap, _ ->
                val entries = snap?.documents?.map {
                    LogEntry(it.getString("who") ?: "", it.getString("uid") ?: "", it.getString("text") ?: "", it.getLong("ts") ?: 0L)
                } ?: return@addSnapshotListener
                logs.value = logs.value + (listId to entries)
            }

        listListeners[listId] = listOf(head, items, log)
    }

    private fun removedFromList(r: Reminder) {
        r.sharedId?.let { stopListening(it) }
        Scheduler.cancel(app, r.id)
        Store.deleteQuiet(r.id)
    }

    /** Push local list edits up, with a log line for each. */
    private fun onLocalChange(old: Reminder?, new: Reminder) {
        if (new.taskId != null) return pushTask(old, new)
        val listId = new.sharedId ?: return
        if (uid.isBlank()) return
        val list = db.collection("lists").document(listId)
        val batch = db.batch()
        var writes = 0
        if (new.sharedOwner && old != null && old.title != new.title) {
            batch.update(list, mapOf("title" to new.title, "updatedAt" to System.currentTimeMillis()))
            writes++
        }
        val before = old?.items?.associateBy { it.id } ?: emptyMap()
        val after = new.items.associateBy { it.id }
        val lines = mutableListOf<String>()
        for (n in new.items) {
            val o = before[n.id]
            val item = if (n.byUid.isBlank() && o == null) n.copy(by = myName, byUid = uid) else n
            if (o == null) {
                batch.set(list.collection("items").document(n.id), itemMap(item)); writes++
                lines += "added ${n.name}"
            } else if (o != n) {
                batch.set(list.collection("items").document(n.id), itemMap(item)); writes++
                when {
                    !o.done && n.done -> lines += "got ${n.name}"
                    o.done && !n.done -> lines += "unticked ${n.name}"
                    o.name != n.name -> lines += "renamed ${o.name} to ${n.name}"
                }
            }
        }
        for (o in old?.items ?: emptyList()) {
            if (o.id !in after) {
                batch.delete(list.collection("items").document(o.id)); writes++
                lines += "removed ${o.name}"
            }
        }
        val logLines = if (lines.size > 3) listOf("updated ${lines.size} items") else lines
        logLines.forEach { batch.set(list.collection("log").document(), logMap(it)); writes++ }
        if (writes > 0) batch.commit()
    }

    private fun onLocalDelete(r: Reminder) {
        if (r.taskId != null) return deleteTask(r)
        if (r.sharedOwner) {
            r.sharedId?.let { stopListening(it); db.collection("lists").document(it).delete() }
        } else {
            leave(r)
        }
    }

    // ---------- invites ----------

    private fun listenInvites() {
        invitesReg?.remove()
        invitesReg = db.collection("invites").whereEqualTo("toUid", uid).whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    Invite(
                        id = it.id,
                        listId = it.getString("listId") ?: "",
                        fromUid = it.getString("fromUid") ?: "",
                        fromName = it.getString("fromName") ?: "Someone",
                        title = it.getString("title") ?: "List",
                        count = (it.getLong("count") ?: 0L).toInt(),
                    )
                }?.filter { inv -> Store.reminders.value.none { it.sharedId == inv.listId } } ?: return@addSnapshotListener
                invites.value = list
                list.forEach { notifyInvite(app, it) }
            }
    }

    private fun notifyInvite(ctx: Context, inv: Invite) {
        Notifs.nag(
            ctx, "inv_" + inv.id, "inv_" + inv.id, "${inv.fromName} wants to share a list",
            "${inv.title} · open Geddydun to accept", "today",
        )
    }

    // ---------- assigned errands ----------

    private fun tasks() = db.collection("tasks")

    private fun repeatMap(r: Reminder) = mapOf(
        "remindAt" to r.dueAt, "repeat" to r.repeat.name, "everyDays" to r.everyDays,
        "anchorDay" to r.anchorDay, "nth" to r.nth, "weekday" to r.weekday, "weekMask" to r.weekMask,
        "fromDone" to r.fromDone,
    )

    private fun itemsList(items: List<Item>) = items.map {
        mapOf("id" to it.id, "name" to it.name, "done" to it.done, "ts" to it.ts, "by" to it.by, "doneBy" to it.doneBy)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readItems(v: Any?): List<Item> = (v as? List<Map<String, Any?>>)?.map {
        Item(
            name = it["name"] as? String ?: "", done = it["done"] as? Boolean ?: false,
            id = it["id"] as? String ?: newItemId(), ts = (it["ts"] as? Number)?.toLong() ?: 0L,
            by = it["by"] as? String ?: "", doneBy = it["doneBy"] as? String ?: "",
        )
    } ?: emptyList()

    private fun readRequest(d: com.google.firebase.firestore.DocumentSnapshot) = TaskRequest(
        id = d.id,
        fromUid = d.getString("fromUid") ?: "",
        fromName = d.getString("fromName") ?: "Someone",
        title = d.getString("title") ?: "Errand",
        notes = d.getString("notes") ?: "",
        dueBy = d.getLong("dueBy") ?: 0L,
        remindAt = d.getLong("remindAt") ?: 0L,
        repeat = runCatching { Repeat.valueOf(d.getString("repeat") ?: "NONE") }.getOrDefault(Repeat.NONE),
        everyDays = (d.getLong("everyDays") ?: 1L).toInt(),
        anchorDay = (d.getLong("anchorDay") ?: 1L).toInt(),
        nth = (d.getLong("nth") ?: 1L).toInt(),
        weekday = (d.getLong("weekday") ?: 6L).toInt(),
        weekMask = (d.getLong("weekMask") ?: 0L).toInt(),
        items = readItems(d.get("items")),
        both = d.getBoolean("both") == true,
        fromDone = d.getBoolean("fromDone") == true,
    )

    private fun taskLog(taskId: String, text: String) {
        tasks().document(taskId).collection("log").document().set(logMap(text))
    }

    private fun dayLabel(epochDay: Long): String =
        if (epochDay <= 0) "no date" else fmtDate(java.time.LocalDate.ofEpochDay(epochDay))

    /** Ask someone to do an errand. Saves it on this phone too (without an alarm). */
    fun assign(r: Reminder, to: Person, done: (String?) -> Unit) {
        if (uid.isBlank()) return done("Not connected yet. Check your internet and try again.")
        val ref = tasks().document()
        val local = r.copy(
            taskId = ref.id, taskRole = "from", taskPerson = to.name, taskPersonUid = to.uid,
            taskStatus = "pending", sharedId = null,
        )
        val data = mutableMapOf<String, Any>(
            "fromUid" to uid, "fromName" to myName, "toUid" to to.uid, "toName" to to.name,
            "title" to r.title, "notes" to r.notes, "dueBy" to r.dueBy, "items" to itemsList(r.items),
            "status" to "pending", "updatedAt" to System.currentTimeMillis(),
        )
        data.putAll(repeatMap(r))
        ref.set(data)
            .addOnSuccessListener {
                taskLog(ref.id, "asked ${to.name}")
                done(null)
            }
            .addOnFailureListener { done("Couldn't send: ${it.message}") }
        Store.upsertQuiet(local)
        Scheduler.cancel(app, local.id)
        listenTaskLog(ref.id)
    }

    /** Accept a request, with your own reminder time and repeat. */
    fun acceptTask(req: TaskRequest, mine: Reminder, done: (String?) -> Unit) {
        val upd = mutableMapOf<String, Any>(
            "status" to "accepted", "toName" to myName, "updatedAt" to System.currentTimeMillis(),
        )
        upd.putAll(repeatMap(mine))
        tasks().document(req.id).update(upd)
            .addOnSuccessListener {
                taskLog(req.id, "accepted" + if (mine.dueAt != req.remindAt || mine.repeat != req.repeat)
                    ", reminder ${fmtWhen(mine.dueAt)} · ${repeatShort(mine)}" else "")
                val r = mine.copy(taskStatus = "accepted")
                Store.upsertQuiet(r)
                Scheduler.schedule(app, r)
                requests.value = requests.value.filter { it.id != req.id }
                listenTaskLog(req.id)
                done(null)
            }
            .addOnFailureListener { done("Couldn't accept: ${it.message}") }
    }

    fun declineTask(req: TaskRequest) {
        tasks().document(req.id).update(mapOf("status" to "declined", "updatedAt" to System.currentTimeMillis()))
        taskLog(req.id, "declined")
        requests.value = requests.value.filter { it.id != req.id }
    }

    /** Assignee changed their own reminder time or repeat. */
    fun updateMyReminder(r: Reminder, log: Boolean = true) {
        val id = r.taskId ?: return
        val upd = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis())
        upd.putAll(repeatMap(r))
        tasks().document(id).update(upd)
        if (log) taskLog(id, "set their reminder to ${fmtWhen(r.dueAt)} · ${repeatShort(r)}")
    }

    /** Assignee finished it (or this cycle of it). */
    fun taskDone(r: Reminder) {
        val id = r.taskId ?: return
        val upd = mutableMapOf<String, Any>(
            "lastDoneAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis(),
        )
        if (r.completed) upd["status"] = "done"
        upd.putAll(repeatMap(r))
        upd["items"] = itemsList(r.items)
        tasks().document(id).update(upd)
        taskLog(id, if (r.completed) "did it" else "did it · next ${fmtWhen(r.dueAt)}")
        if (r.completed) Store.upsertQuiet(r.copy(taskStatus = "done"))
    }

    /** Assigner: stop asking and do it yourself. It becomes a normal errand with an alarm. */
    fun takeBack(r: Reminder) {
        val id = r.taskId ?: return
        tasks().document(id).update(mapOf("status" to "cancelled", "updatedAt" to System.currentTimeMillis()))
        taskLog(id, "will do it themselves")
        stopTaskLog(id)
        var mine = r.copy(taskId = null, taskRole = "", taskPerson = "", taskPersonUid = "", taskStatus = "", completed = false)
        if (mine.dueAt <= System.currentTimeMillis()) mine = mine.copy(dueAt = Scheduler.tomorrowSameTime(mine))
        Store.upsertQuiet(mine)
        Scheduler.schedule(app, mine)
    }

    // ---------- waiting for my OK, even with the app closed ----------

    private fun nagRequest(ctx: Context, it: TaskRequest) {
        if (it.both) Notifs.nag(
            ctx, "req_" + it.id, "task_" + it.id, "${it.fromName} set a shared alarm: ${it.title}",
            "Rings ${fmtWhen(it.remindAt)} for both of you · tap to accept", "review:" + it.id,
        )
        else Notifs.nag(
            ctx, "req_" + it.id, "task_" + it.id, "${it.fromName} asked you: ${it.title}",
            "Due ${dayLabel(it.dueBy)} · tap to review", "review:" + it.id,
        )
    }

    private fun nagProposal(ctx: Context, taskId: String, p: Proposal, status: String, title: String) {
        val head = if (status == "pending") "${p.byName} will do it, with changes" else "${p.byName} suggested changes"
        Notifs.nag(ctx, "prop_${taskId}_${p.ts}", "task_$taskId", head, "$title · tap to review", "changes:$taskId")
    }

    /**
     * Runs in the background every 15 minutes or so: looks for anything waiting for my OK
     * and shows (or re-shows) its notification. Blocking; call off the main thread.
     */
    fun checkApprovals(ctx: Context) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val col = FirebaseFirestore.getInstance().collection("tasks")
        val wait = { t: com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> ->
            com.google.android.gms.tasks.Tasks.await(t, 30, java.util.concurrent.TimeUnit.SECONDS)
        }
        val incoming = wait(col.whereEqualTo("toUid", me).get()).documents
        val outgoing = wait(col.whereEqualTo("fromUid", me).get()).documents
        val local = Store.reminders.value.mapNotNull { it.taskId }.toSet()
        val waiting = mutableSetOf<String>()
        for (d in incoming) {
            val status = d.getString("status") ?: "pending"
            val p = readProposal(d)
            if (status == "pending" && d.id !in local && (p == null || p.byUid != me)) {
                val req = readRequest(d)
                waiting += "req_" + d.id
                nagRequest(ctx, req)
            }
        }
        for (d in incoming + outgoing) {
            val status = d.getString("status") ?: "pending"
            if (status != "pending" && status != "accepted") continue
            val p = readProposal(d) ?: continue
            if (p.byUid == me) continue
            waiting += "prop_${d.id}_${p.ts}"
            nagProposal(ctx, d.id, p, status, d.getString("title") ?: "Errand")
        }
        val invs = wait(
            FirebaseFirestore.getInstance().collection("invites").whereEqualTo("toUid", me).whereEqualTo("status", "pending").get(),
        ).documents
        val lists = Store.reminders.value.mapNotNull { it.sharedId }.toSet()
        for (d in invs) {
            val listId = d.getString("listId") ?: ""
            if (listId in lists) continue
            waiting += "inv_" + d.id
            notifyInvite(ctx, Invite(d.id, listId, d.getString("fromUid") ?: "", d.getString("fromName") ?: "Someone", d.getString("title") ?: "List", 0))
        }
        Notifs.keepNags(ctx, waiting)
    }

    // ---------- shared alarms (both of us) ----------

    /** Set an alarm for both of us. It rings here straight away; they accept it once. */
    fun assignBoth(r: Reminder, to: Person, done: (String?) -> Unit) {
        if (uid.isBlank()) return done("Not connected yet. Check your internet and try again.")
        val ref = tasks().document()
        val local = r.copy(
            taskId = ref.id, taskRole = "from", taskPerson = to.name, taskPersonUid = to.uid,
            taskStatus = "pending", taskBoth = true, taskBaseAt = r.dueAt, sharedId = null, dueBy = 0L,
        )
        val data = mutableMapOf<String, Any>(
            "fromUid" to uid, "fromName" to myName, "toUid" to to.uid, "toName" to to.name, "both" to true,
            "title" to r.title, "notes" to r.notes, "dueBy" to 0L, "items" to itemsList(r.items),
            "status" to "pending", "updatedAt" to System.currentTimeMillis(),
        )
        data.putAll(repeatMap(local))
        ref.set(data)
            .addOnSuccessListener {
                taskLog(ref.id, "set a shared alarm with ${to.name}")
                done(null)
            }
            .addOnFailureListener { done("Couldn't send: ${it.message}") }
        Store.upsertQuiet(local)
        Scheduler.schedule(app, local)
        listenTaskLog(ref.id)
    }

    /** The one OK a shared alarm needs. After this, either of us can change it freely. */
    fun acceptBoth(req: TaskRequest, done: (String?) -> Unit) {
        var mine = req.toReminder().copy(taskStatus = "accepted")
        if (mine.dueAt <= System.currentTimeMillis()) {
            mine = if (mine.repeat != Repeat.NONE) mine.copy(dueAt = fromLdt(Rules.firstOnOrAfter(mine, java.time.LocalDateTime.now().plusMinutes(1))))
            else mine.copy(completed = true)
        }
        tasks().document(req.id).update(
            mapOf("status" to "accepted", "toName" to myName, "updatedAt" to System.currentTimeMillis()),
        ).addOnSuccessListener {
            taskLog(req.id, "accepted the shared alarm")
            Store.upsertQuiet(mine)
            Scheduler.schedule(app, mine)
            requests.value = requests.value.filter { it.id != req.id }
            listenTaskLog(req.id)
            done(null)
        }.addOnFailureListener { done("Couldn't accept: ${it.message}") }
    }

    /** Take the other person's edits to a shared alarm. My own snoozes stay unless the time itself changed. */
    private fun syncBoth(local: Reminder, req: TaskRequest, status: String, person: String) {
        var r = local.copy(
            title = req.title, notes = req.notes, items = req.items, taskStatus = status, taskPerson = person,
            repeat = req.repeat, everyDays = req.everyDays, anchorDay = req.anchorDay,
            nth = req.nth, weekday = req.weekday, weekMask = req.weekMask, fromDone = req.fromDone,
        )
        val timeChanged = req.remindAt > 0 && req.remindAt != local.taskBaseAt
        if (timeChanged) {
            r = r.copy(dueAt = req.remindAt, taskBaseAt = req.remindAt, snoozeUntil = null, rings = 0, completed = false)
        }
        if ((timeChanged || r.repeat != local.repeat) && !r.completed && r.dueAt <= System.currentTimeMillis()) {
            r = if (r.repeat != Repeat.NONE) r.copy(dueAt = fromLdt(Rules.firstOnOrAfter(r, java.time.LocalDateTime.now().plusMinutes(1))))
            else r.copy(completed = true)
        }
        if (r != local) {
            Store.upsertQuiet(r)
            Scheduler.schedule(app, r)
            if (timeChanged && local.taskStatus == "accepted") Notifs.note(
                app, "task_" + req.id, "$person moved ${r.title}",
                "Shared alarm now rings ${fmtWhen(r.dueAt)}", "task:" + req.id,
            )
        }
    }

    // ---------- suggested changes ----------

    private fun proposalMap(p: Proposal) = mapOf(
        "byUid" to p.byUid, "byName" to p.byName, "ts" to p.ts, "title" to p.title, "notes" to p.notes,
        "dueBy" to p.dueBy, "repeat" to p.repeat.name, "everyDays" to p.everyDays, "anchorDay" to p.anchorDay,
        "nth" to p.nth, "weekday" to p.weekday, "weekMask" to p.weekMask, "remindAt" to p.remindAt,
        "items" to p.items?.map { mapOf("id" to it.id, "name" to it.name) },
    )

    @Suppress("UNCHECKED_CAST")
    private fun readProposal(d: com.google.firebase.firestore.DocumentSnapshot): Proposal? {
        val m = d.get("proposal") as? Map<String, Any?> ?: return null
        fun l(k: String, def: Long) = (m[k] as? Number)?.toLong() ?: def
        return Proposal(
            taskId = d.id, byUid = m["byUid"] as? String ?: return null, byName = m["byName"] as? String ?: "Someone",
            ts = l("ts", 0), title = m["title"] as? String ?: "", notes = m["notes"] as? String ?: "",
            dueBy = l("dueBy", 0),
            repeat = runCatching { Repeat.valueOf(m["repeat"] as? String ?: "NONE") }.getOrDefault(Repeat.NONE),
            everyDays = l("everyDays", 1).toInt(), anchorDay = l("anchorDay", 1).toInt(), nth = l("nth", 1).toInt(),
            weekday = l("weekday", 6).toInt(), weekMask = l("weekMask", 0).toInt(), remindAt = l("remindAt", 0),
            items = (m["items"] as? List<Map<String, Any?>>)?.map {
                Item(name = it["name"] as? String ?: "", id = it["id"] as? String ?: newItemId())
            },
        )
    }

    private fun diffText(r: Reminder, p: Proposal): String =
        proposalDiff(r, p).joinToString(", ") { (label, old, new) ->
            when (label) {
                "Notes" -> "notes"
                "Added" -> "added $new"
                "Removed" -> "removed $old"
                "Renamed" -> "renamed $old to $new"
                else -> "${label.lowercase()} $old → $new"
            }
        }

    /** Send changes to the other person for their OK. Replaces any earlier suggestion of yours. */
    fun propose(current: Reminder, edited: Reminder, done: (String?) -> Unit) {
        val id = current.taskId ?: return done("This errand isn't shared")
        if (uid.isBlank()) return done("Not connected yet. Check your internet and try again.")
        val p = Proposal.of(
            id, uid, myName, edited,
            remindAt = if (edited.dueAt != current.dueAt) edited.dueAt else 0L,
            items = if (sameItems(current.items, edited.items)) null else edited.items,
        )
        val sentBack = proposals.value[id]?.let { it.byUid != uid } == true
        tasks().document(id).update(mapOf("proposal" to proposalMap(p), "updatedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                taskLog(id, (if (sentBack) "sent back changes: " else "suggested: ") + diffText(current, p))
                done(null)
            }
            .addOnFailureListener { done("Couldn't send: ${it.message}") }
        proposals.value = proposals.value + (id to p)
    }

    /** Notes and list items change straight away on an assigned errand; only these need no OK. */
    fun setNotes(r: Reminder, notes: String) {
        val id = r.taskId ?: return
        Store.upsertQuiet(r.copy(notes = notes))
        tasks().document(id).update(mapOf("notes" to notes, "updatedAt" to System.currentTimeMillis()))
        taskLog(id, "changed the notes")
    }

    /** Reviewing a request: I'll do it, but with a different reminder or repeat. Goes back for an OK. */
    fun counterOffer(req: TaskRequest, mine: Reminder, done: (String?) -> Unit) {
        if (uid.isBlank()) return done("Not connected yet. Check your internet and try again.")
        val p = Proposal(
            req.id, uid, myName, System.currentTimeMillis(), req.title, req.notes, req.dueBy,
            mine.repeat, mine.everyDays, mine.anchorDay, mine.nth, mine.weekday, mine.weekMask,
            remindAt = if (mine.dueAt != req.remindAt) mine.dueAt else 0L,
        )
        tasks().document(req.id).update(
            mapOf("proposal" to proposalMap(p), "toName" to myName, "updatedAt" to System.currentTimeMillis()),
        ).addOnSuccessListener {
            taskLog(req.id, "will do it, with changes: " + diffText(req.toReminder(0), p))
            done(null)
        }.addOnFailureListener { done("Couldn't send: ${it.message}") }
        proposals.value = proposals.value + (req.id to p)
    }

    fun withdraw(taskId: String) {
        tasks().document(taskId).update(mapOf("proposal" to FieldValue.delete(), "updatedAt" to System.currentTimeMillis()))
        taskLog(taskId, "withdrew their changes")
        proposals.value = proposals.value - taskId
    }

    fun acceptProposal(taskId: String, done: (String?) -> Unit) {
        val p = proposals.value[taskId] ?: return done("These changes were withdrawn")
        val upd = mutableMapOf<String, Any>(
            "title" to p.title, "dueBy" to p.dueBy, "repeat" to p.repeat.name,
            "everyDays" to p.everyDays, "anchorDay" to p.anchorDay, "nth" to p.nth, "weekday" to p.weekday,
            "weekMask" to p.weekMask, "proposal" to FieldValue.delete(), "updatedAt" to System.currentTimeMillis(),
        )
        if (p.remindAt > 0) upd["remindAt"] = p.remindAt
        val local = localTask(taskId)
        if (p.items != null && local != null) upd["items"] = itemsList(p.applyTo(local).items)
        // Their changes came while accepting my request: accepting them means it's on.
        if (local?.taskStatus == "pending") upd["status"] = "accepted"
        if (local != null && local.assignedToMe) {
            // My alarm: apply it here now, so nothing is shifted twice when the update comes back.
            var mine = p.applyTo(local)
            if (p.remindAt > 0) mine = mine.copy(snoozeUntil = null, rings = 0, completed = false)
            if (mine.repeat != Repeat.NONE && !mine.completed) mine = mine.copy(dueAt = fromLdt(Rules.firstOnOrAfter(mine, toLdt(mine.dueAt))))
            Store.upsertQuiet(mine)
            Scheduler.schedule(app, mine)
            if (mine.dueAt != local.dueAt) upd["remindAt"] = mine.dueAt
        }
        tasks().document(taskId).update(upd).addOnSuccessListener {
            taskLog(taskId, "accepted ${p.byName}'s changes")
            done(null)
        }.addOnFailureListener { done("Couldn't accept: ${it.message}") }
        proposals.value = proposals.value - taskId
    }

    fun declineProposal(taskId: String) {
        val p = proposals.value[taskId]
        tasks().document(taskId).update(mapOf("proposal" to FieldValue.delete(), "updatedAt" to System.currentTimeMillis()))
        taskLog(taskId, if (p != null) "declined ${p.byName}'s changes" else "declined the changes")
        proposals.value = proposals.value - taskId
    }

    /** Keep the proposals map in step with a task document, and tell me about new ones. */
    /** Keep the proposals map in step with a task document. Returns my suggestion if they just took it. */
    private fun trackProposal(d: com.google.firebase.firestore.DocumentSnapshot, local: Reminder?, req: TaskRequest): Proposal? {
        val p = readProposal(d)
        val before = proposals.value[d.id]
        proposals.value = if (p == null) proposals.value - d.id else proposals.value + (d.id to p)
        val title = local?.title ?: req.title
        val status = d.getString("status") ?: "pending"
        if (p != null && p.byUid != uid) nagProposal(app, d.id, p, status, title)
        if (p == null && before != null && before.byUid == uid) {
            // My suggestion is gone: did they take it?
            val took = req.title == before.title && req.dueBy == before.dueBy &&
                req.repeat == before.repeat && (before.remindAt <= 0 || req.remindAt == before.remindAt) &&
                status != "pending"
            val who = local?.taskPerson?.ifBlank { null } ?: req.fromName
            when {
                took -> Notifs.note(app, "task_" + d.id, "$who accepted your changes", req.title, "task:" + d.id)
                status == "pending" && local == null ->
                    Notifs.note(app, "task_" + d.id, "$who sent it back to you", "${req.title} · tap to review", "review:" + d.id)
                else -> Notifs.note(app, "task_" + d.id, "$who didn't take your changes", req.title, "task:" + d.id)
            }
            return if (took) before else null
        }
        return null
    }

    private fun pushTask(old: Reminder?, new: Reminder) {
        val id = new.taskId ?: return
        if (uid.isBlank() || old == null || old.taskId != id) return
        val upd = mutableMapOf<String, Any>()
        if (old.items != new.items) upd["items"] = itemsList(new.items)
        if (new.isBoth) {
            // Either of us can change it; no OK needed after the first one. Snoozes and done stay on this phone.
            if (old.title != new.title) upd["title"] = new.title
            if (old.notes != new.notes) upd["notes"] = new.notes
            val rep = listOf("repeat", "everyDays", "anchorDay", "nth", "weekday", "weekMask", "fromDone")
            val a = repeatMap(old); val b = repeatMap(new)
            rep.filter { a[it] != b[it] }.forEach { upd[it] = b[it]!! }
            if (old.taskBaseAt != new.taskBaseAt && new.taskBaseAt > 0) {
                upd["remindAt"] = new.taskBaseAt
                taskLog(id, "changed the time to ${fmtWhen(new.taskBaseAt)} · ${repeatShort(new)}")
            } else if (rep.any { a[it] != b[it] }) {
                taskLog(id, "changed the repeat to ${repeatShort(new)}")
            }
            if (old.title != new.title) taskLog(id, "renamed it to ${new.title}")
        } else if (new.assignedByMe) {
            if (old.title != new.title) upd["title"] = new.title
            if (old.notes != new.notes) upd["notes"] = new.notes
            if (old.dueBy != new.dueBy) {
                upd["dueBy"] = new.dueBy
                taskLog(id, "changed the due date to ${dayLabel(new.dueBy)}")
            }
            // Before it's accepted, the reminder time is still the suggestion.
            if (new.taskStatus == "pending" && repeatMap(old) != repeatMap(new)) upd.putAll(repeatMap(new))
            // They asked for changes before accepting; I changed it myself instead, so it goes back to them.
            val theirs = proposals.value[id]
            if (new.taskStatus == "pending" && theirs != null && theirs.byUid != uid && upd.isNotEmpty()) {
                upd["proposal"] = FieldValue.delete()
                proposals.value = proposals.value - id
                taskLog(id, "changed it and sent it back")
            }
        } else if (new.assignedToMe) {
            if (repeatMap(old) != repeatMap(new)) upd.putAll(repeatMap(new))
        }
        if (old.items != new.items) {
            val before = old.items.associateBy { it.id }
            val lines = new.items.mapNotNull { n ->
                val o = before[n.id]
                when {
                    o == null -> "added ${n.name}"
                    !o.done && n.done -> "got ${n.name}"
                    else -> null
                }
            }
            (if (lines.size > 3) listOf("updated ${lines.size} items") else lines).forEach { taskLog(id, it) }
        }
        if (upd.isNotEmpty()) {
            upd["updatedAt"] = System.currentTimeMillis()
            tasks().document(id).update(upd)
        }
    }

    private fun deleteTask(r: Reminder) {
        val id = r.taskId ?: return
        stopTaskLog(id)
        if (r.isBoth) {
            if (r.taskRole == "from") {
                if (r.taskStatus != "cancelled") {
                    tasks().document(id).update(mapOf("status" to "cancelled", "updatedAt" to System.currentTimeMillis()))
                    taskLog(id, "deleted the shared alarm")
                }
            } else if (r.taskStatus == "accepted") {
                tasks().document(id).update(mapOf("status" to "left", "updatedAt" to System.currentTimeMillis()))
                taskLog(id, "removed it from their phone")
            }
            return
        }
        if (r.assignedByMe) {
            if (r.taskStatus != "cancelled" && r.taskStatus != "done") {
                tasks().document(id).update(mapOf("status" to "cancelled", "updatedAt" to System.currentTimeMillis()))
                taskLog(id, "cancelled it")
            }
        } else if (r.taskStatus == "accepted" || r.taskStatus == "pending") {
            tasks().document(id).update(mapOf("status" to "declined", "updatedAt" to System.currentTimeMillis()))
            taskLog(id, "removed it from their phone")
        }
    }

    private fun localTask(id: String) = Store.reminders.value.find { it.taskId == id }

    private fun listenTasks() {
        tasksInReg?.remove()
        tasksOutReg?.remove()
        tasksInReg = tasks().whereEqualTo("toUid", uid).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val pending = mutableListOf<TaskRequest>()
            for (d in snap.documents) {
                val status = d.getString("status") ?: "pending"
                val req = readRequest(d)
                var local = localTask(d.id)
                if (req.both) {
                    when {
                        local == null && status == "pending" -> pending += req
                        local == null -> {}
                        status == "cancelled" -> {
                            Scheduler.cancel(app, local.id)
                            AlarmService.resolve(app, local.id)
                            Store.deleteQuiet(local.id)
                            stopTaskLog(d.id)
                            Notifs.note(app, "task_" + d.id, "${req.fromName} deleted ${req.title}", "The shared alarm is off your phone too.")
                        }
                        status == "accepted" -> {
                            syncBoth(local, req, status, req.fromName)
                            listenTaskLog(d.id)
                        }
                    }
                    continue
                }
                if (local == null) {
                    if (status == "pending") {
                        trackProposal(d, null, req)
                        pending += req
                        continue
                    }
                    if (status != "accepted") continue
                    // They accepted the changes I asked for while reviewing: it's on, with my reminder.
                    trackProposal(d, null, req)
                    val mine = req.toReminder().copy(taskStatus = "accepted")
                    Store.upsertQuiet(mine)
                    Scheduler.schedule(app, mine)
                    listenTaskLog(d.id)
                    continue
                }
                if (status == "cancelled") {
                    Scheduler.cancel(app, local.id)
                    AlarmService.resolve(app, local.id)
                    Store.deleteQuiet(local.id)
                    stopTaskLog(d.id)
                    Notifs.note(app, "task_" + d.id, "${req.fromName} cancelled ${req.title}", "It's been taken off your list.")
                    continue
                }
                val taken = trackProposal(d, local, req)
                if (taken != null && taken.remindAt > 0 && taken.remindAt != local.dueAt) {
                    // They OK'd my new reminder time.
                    local = local.copy(dueAt = taken.remindAt, snoozeUntil = null, rings = 0, completed = false)
                }
                var r = local.copy(title = req.title, notes = req.notes, items = req.items, taskPerson = req.fromName)
                if (status == "accepted" && (req.repeat != local.repeat || req.everyDays != local.everyDays ||
                        req.nth != local.nth || req.weekday != local.weekday || req.weekMask != local.weekMask ||
                        req.anchorDay != local.anchorDay || req.fromDone != local.fromDone)
                ) {
                    // Repeat changed (they accepted my suggestion, or I accepted theirs): keep my time, new pattern.
                    r = r.copy(
                        repeat = req.repeat, everyDays = req.everyDays, anchorDay = req.anchorDay,
                        nth = req.nth, weekday = req.weekday, weekMask = req.weekMask, fromDone = req.fromDone,
                    )
                    if (r.repeat != Repeat.NONE && !r.completed) {
                        r = r.copy(dueAt = fromLdt(Rules.firstOnOrAfter(r, toLdt(r.dueAt))))
                    }
                }
                if (req.dueBy != local.dueBy && local.dueBy > 0 && req.dueBy > 0 && taken?.remindAt?.let { it > 0 } != true) {
                    // Due date moved: move my reminder by the same number of days.
                    val shift = req.dueBy - local.dueBy
                    val moved = fromLdt(toLdt(local.dueAt).plusDays(shift))
                    r = r.copy(dueBy = req.dueBy, dueAt = moved, snoozeUntil = null)
                    if (taken == null) Notifs.note(
                        app, "task_" + d.id, "${req.fromName} changed the due date",
                        "${req.title} is now due ${dayLabel(req.dueBy)}. Your reminder moved to ${fmtWhen(moved)}.",
                        "task:" + d.id,
                    )
                    updateMyReminder(r, log = false)
                } else if (req.dueBy != local.dueBy) {
                    r = r.copy(dueBy = req.dueBy)
                }
                if (r != localTask(d.id)) {
                    Store.upsertQuiet(r)
                    Scheduler.schedule(app, r)
                    if (taken != null && taken.remindAt > 0) updateMyReminder(r, log = false)
                }
                listenTaskLog(d.id)
            }
            requests.value = pending.sortedBy { it.dueBy }
            pending.forEach { nagRequest(app, it) }
        }
        tasksOutReg = tasks().whereEqualTo("fromUid", uid).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            for (d in snap.documents) {
                val local = localTask(d.id) ?: continue
                val status = d.getString("status") ?: "pending"
                val req = readRequest(d)
                val toName = d.getString("toName") ?: local.taskPerson
                if (req.both) {
                    if (status != local.taskStatus && local.taskStatus != "cancelled") {
                        val msg = when (status) {
                            "accepted" -> "$toName accepted ${local.title}"
                            "declined" -> "$toName declined ${local.title}"
                            "left" -> "$toName removed ${local.title}"
                            else -> null
                        }
                        if (msg != null) Notifs.note(
                            app, "task_" + d.id, msg,
                            if (status == "accepted") "The shared alarm rings for both of you" else "It still rings on your phone",
                            "task:" + d.id,
                        )
                    }
                    syncBoth(local, req, status, toName)
                    listenTaskLog(d.id)
                    continue
                }
                if (status != local.taskStatus && local.taskStatus != "cancelled") {
                    val msg = when (status) {
                        "accepted" -> "$toName accepted ${local.title}"
                        "declined" -> "$toName declined ${local.title}"
                        "done" -> "$toName did ${local.title}"
                        else -> null
                    }
                    if (msg != null) Notifs.note(app, "task_" + d.id, msg, "Due ${dayLabel(local.dueBy)}", "task:" + d.id)
                }
                trackProposal(d, local, req)
                val doneAt = d.getLong("lastDoneAt") ?: 0L
                val r = local.copy(
                    taskStatus = status, taskPerson = toName, items = req.items,
                    title = req.title, notes = req.notes, dueBy = req.dueBy,
                    dueAt = if (req.remindAt > 0) req.remindAt else local.dueAt,
                    repeat = req.repeat, everyDays = req.everyDays, anchorDay = req.anchorDay,
                    nth = req.nth, weekday = req.weekday, weekMask = req.weekMask, fromDone = req.fromDone,
                    completed = status == "done",
                    history = if (doneAt > 0 && local.history.none { it.paidAt == doneAt })
                        listOf(Paid(local.dueAt, doneAt)) + local.history else local.history,
                )
                if (r != local) Store.upsertQuiet(r)
                listenTaskLog(d.id)
            }
        }
    }

    private fun listenTaskLog(id: String) {
        if (id in taskLogRegs) return
        taskLogRegs[id] = tasks().document(id).collection("log").orderBy("ts", Query.Direction.DESCENDING).limit(60)
            .addSnapshotListener { snap, _ ->
                val entries = snap?.documents?.map {
                    LogEntry(it.getString("who") ?: "", it.getString("uid") ?: "", it.getString("text") ?: "", it.getLong("ts") ?: 0L)
                } ?: return@addSnapshotListener
                logs.value = logs.value + (id to entries)
            }
    }

    private fun stopTaskLog(id: String) {
        taskLogRegs.remove(id)?.remove()
    }
}
