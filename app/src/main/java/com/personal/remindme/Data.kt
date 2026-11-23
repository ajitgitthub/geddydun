package com.personal.remindme

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class Type { BILL, ERRAND, CHECK }
enum class Repeat {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY, EVERY_N_DAYS,
    // custom
    NTH_WEEKDAY, WEEKDAYS, EVERY_N_WEEKS, LAST_DAY;

    val isCustom: Boolean get() = this in setOf(EVERY_N_DAYS, NTH_WEEKDAY, WEEKDAYS, EVERY_N_WEEKS, LAST_DAY)
}

/** What a repeating list looks like after you're done with it. */
enum class ListMode { UNTICK_ALL, KEEP_PENDING, EMPTY }

fun applyListMode(items: List<Item>, mode: ListMode): List<Item> = when (mode) {
    ListMode.UNTICK_ALL -> items.map { it.copy(done = false, doneBy = "") }
    ListMode.KEEP_PENDING -> items.filter { !it.done }
    ListMode.EMPTY -> emptyList()
}

data class Item(
    val name: String,
    val done: Boolean = false,
    val id: String = newItemId(),
    val ts: Long = System.currentTimeMillis(),
    val by: String = "",      // name of who added it (shared lists)
    val byUid: String = "",
    val doneBy: String = "",  // name of who ticked it
)

fun newItemId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(16)

/** Tick or untick, remembering who did it. */
fun Item.toggled(): Item = if (done) copy(done = false, doneBy = "") else copy(done = true, doneBy = Store.userName)
data class Paid(val dueAt: Long, val paidAt: Long)

/** One line in History: something done or changed. */
data class HistoryEvent(
    val ts: Long, val title: String, val text: String, val done: Boolean, val bill: Boolean = false,
    /** Alarm rang, snoozed or moved: a log to check alarms are working. */
    val alarm: Boolean = false,
)

data class Reminder(
    val id: Long,
    val type: Type,
    val title: String,
    val notes: String = "",
    val dueAt: Long = 0L,
    val repeat: Repeat = Repeat.NONE,
    val everyDays: Int = 1,
    val anchorDay: Int = 1,
    val snoozeUntil: Long? = null,
    val items: List<Item> = emptyList(),
    val history: List<Paid> = emptyList(),
    val completed: Boolean = false,
    val rings: Int = 0,
    val isTest: Boolean = false,
    val maxNotifs: Int = 0,   // check-later: 0 = until ticked off
    val notifCount: Int = 0,  // check-later: how many digests included it
    val nth: Int = 1,         // NTH_WEEKDAY: 1..4, 5 = last
    val weekday: Int = 6,     // NTH_WEEKDAY / EVERY_N_WEEKS: 1 = Monday .. 7 = Sunday
    val weekMask: Int = 0,    // WEEKDAYS: bit (day-1) set for each chosen day
    val fromDone: Boolean = false, // Daily / Weekly / Every N days: next one counts from the day it's marked done
    val pinned: Boolean = false,
    val sharedId: String? = null,       // cloud list id when shared
    val sharedOwner: Boolean = false,   // true on the phone that shared it
    val sharedBy: String = "",          // owner's name (on the other person's phone)
    val sharedWith: List<String> = emptyList(), // other people's names
    val taskId: String? = null,        // cloud id when this errand is assigned
    val taskRole: String = "",         // "from" = I asked someone, "to" = someone asked me
    val taskPerson: String = "",       // the other person's name
    val taskPersonUid: String = "",
    val taskStatus: String = "",       // pending, accepted, declined, done, cancelled
    val dueBy: Long = 0L,              // assigned errands: due date as epoch day, 0 = none
    val taskBoth: Boolean = false,     // a shared alarm that rings on both phones
    val taskBaseAt: Long = 0L,         // shared alarm: the agreed time, before my own snoozes
) {
    val isSharedMember: Boolean get() = sharedId != null && !sharedOwner
    val assignedByMe: Boolean get() = taskId != null && taskRole == "from" && !taskBoth
    val assignedToMe: Boolean get() = taskId != null && taskRole == "to" && !taskBoth
    /** Rings for both of us; each phone snoozes and finishes it on its own. */
    val isBoth: Boolean get() = taskId != null && taskBoth
    /** "Alex + Sam", my name first. */
    val bothNames: String get() = Store.userName.ifBlank { "Me" } + " + " + taskPerson
    val dueByDate: LocalDate? get() = if (dueBy > 0) LocalDate.ofEpochDay(dueBy) else null
    val nextTrigger: Long get() = snoozeUntil ?: dueAt
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Backup.start(this)
        Notes.init(this)
        restoreVoiceMutes(this)
        Notifs.createChannels(this)
        Cloud.start(this)
        Store.onSaved = { TodayWidget.refresh(this) }
        ApprovalCheck.start(this)
    }
}

object Store {
    private lateinit var file: File
    private lateinit var prefs: SharedPreferences
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders

    @Synchronized
    fun init(ctx: Context) {
        if (::file.isInitialized) return
        file = File(ctx.filesDir, "reminders.json")
        prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _reminders.value = load()
    }

    fun get(id: Long): Reminder? = _reminders.value.find { it.id == id }

    /** Called after a local change, so shared lists can sync. */
    var onLocalChange: ((old: Reminder?, new: Reminder) -> Unit)? = null
    var onLocalDelete: ((Reminder) -> Unit)? = null

    fun upsert(r: Reminder) {
        val old = get(r.id)
        upsertQuiet(r)
        logItemChanges(old, r)
        if (r.sharedId != null || old?.sharedId != null || r.taskId != null) onLocalChange?.invoke(old, r)
    }

    /** Save without syncing (used for changes that came from the cloud). */
    @Synchronized
    fun upsertQuiet(r: Reminder) {
        val list = _reminders.value.toMutableList()
        val i = list.indexOfFirst { it.id == r.id }
        if (i >= 0) list[i] = r else list.add(r)
        save(list)
    }

    fun delete(id: Long) {
        val old = get(id)
        deleteQuiet(id)
        if (old?.sharedId != null || old?.taskId != null) onLocalDelete?.invoke(old)
    }

    @Synchronized
    fun deleteQuiet(id: Long) = save(_reminders.value.filter { it.id != id })

    fun newId(): Long = System.currentTimeMillis()

    var digestHour: Int
        get() = prefs.getInt("digestHour", 20)
        set(v) = prefs.edit().putInt("digestHour", v).apply()
    var digestMinute: Int
        get() = prefs.getInt("digestMinute", 0)
        set(v) = prefs.edit().putInt("digestMinute", v).apply()
    var digestSnoozeUntil: Long
        get() = prefs.getLong("digestSnooze", 0L)
        set(v) = prefs.edit().putLong("digestSnooze", v).apply()
    /** Say the alarm title out loud and listen for done / later / tomorrow. */
    var talkToAlarms: Boolean
        get() = prefs.getBoolean("talkToAlarms", true)
        set(v) = prefs.edit().putBoolean("talkToAlarms", v).apply()

    /** Spoken alarm titles in a male voice (false = female). */
    var voiceMale: Boolean
        get() = prefs.getBoolean("voiceMale", false)
        set(v) = prefs.edit().putBoolean("voiceMale", v).apply()

    /** Play the Sparkle sound when something's marked done. */
    var celebrateSound: Boolean
        get() = prefs.getBoolean("celebrateSound", true)
        set(v) = prefs.edit().putBoolean("celebrateSound", v).apply()
    /** Asked once on opening the app for the microphone (talking alarms). */
    var micAsked: Boolean
        get() = prefs.getBoolean("micAsked", false)
        set(v) = prefs.edit().putBoolean("micAsked", v).apply()
    var setupShown: Boolean
        get() = prefs.getBoolean("setupShown", false)
        set(v) = prefs.edit().putBoolean("setupShown", v).apply()
    var userName: String
        get() = prefs.getString("userName", "") ?: ""
        set(v) = prefs.edit().putString("userName", v).apply()
    /** Day (yyyy-MM-dd) whose alarms are silenced, and when that was set. */
    var mutedDay: String
        get() = prefs.getString("mutedDay", "") ?: ""
        set(v) = prefs.edit().putString("mutedDay", v).apply()
    var mutedAt: Long
        get() = prefs.getLong("mutedAt", 0L)
        set(v) = prefs.edit().putLong("mutedAt", v).apply()
    val mutedToday: Boolean get() = mutedDay == LocalDate.now().toString()

    var myCode: String
        get() = prefs.getString("myCode", "") ?: ""
        set(v) = prefs.edit().putString("myCode", v).apply()
    var notifAsked: Boolean
        get() = prefs.getBoolean("notifAsked", false)
        set(v) = prefs.edit().putBoolean("notifAsked", v).apply()

    // ---------- history (things done on this phone that aren't shared) ----------

    private val _events = MutableStateFlow<List<HistoryEvent>>(emptyList())
    val events: StateFlow<List<HistoryEvent>> get() {
        if (_events.value.isEmpty()) _events.value = loadEvents()
        return _events
    }

    private fun loadEvents(): List<HistoryEvent> = try {
        val a = JSONArray(prefs.getString("events", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            HistoryEvent(
                o.getLong("ts"), o.optString("title"), o.optString("text"),
                o.optBoolean("done"), o.optBoolean("bill"), o.optBoolean("alarm"),
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    @Synchronized
    fun logEvent(e: HistoryEvent) {
        val list = (listOf(e) + events.value).take(600)
        _events.value = list
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("ts", it.ts).put("title", it.title).put("text", it.text).put("done", it.done).put("bill", it.bill).put("alarm", it.alarm))
        }
        prefs.edit().putString("events", a.toString()).apply()
    }

    /** A reminder, errand or check-later item was finished. Shared ones are logged in the cloud instead. */
    fun logDone(r: Reminder) {
        if (r.isTest || r.type == Type.BILL || r.sharedId != null || r.taskId != null) return
        logEvent(HistoryEvent(System.currentTimeMillis(), r.title, if (r.type == Type.CHECK) "Check later · done" else "Done · you", done = true))
    }

    /** Alarm log: rang, snoozed, moved. Kept for every reminder, test alarms included. */
    fun logAlarm(r: Reminder, text: String) =
        logEvent(HistoryEvent(System.currentTimeMillis(), r.title, text, done = false, alarm = true))

    private fun logItemChanges(old: Reminder?, new: Reminder) {
        if (old == null || new.isTest || new.sharedId != null || new.taskId != null) return
        val before = old.items.associateBy { it.id }
        val now = System.currentTimeMillis()
        new.items.forEach { n ->
            val o = before[n.id]
            if (o != null && !o.done && n.done) logEvent(HistoryEvent(now, n.name, "${new.title} · ticked by you", done = true))
        }
        val added = new.items.filter { it.id !in before }.map { it.name }
        if (added.isNotEmpty()) {
            val names = if (added.size > 4) "${added.size} items" else added.joinToString(", ")
            logEvent(HistoryEvent(now, new.title, "You added $names", done = false))
        }
    }

    /** Every item name ever added to a shopping list, most recent first. */
    private fun itemHistory(): List<String> = try {
        val a = JSONArray(prefs.getString("itemHistory", "[]"))
        (0 until a.length()).map { a.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }

    fun rememberItem(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        val list = listOf(n) + itemHistory().filter { !it.equals(n, ignoreCase = true) }
        val a = JSONArray()
        list.take(500).forEach { a.put(it) }
        prefs.edit().putString("itemHistory", a.toString()).apply()
    }

    /** Past item names matching what's typed: starts-with matches first, then contains. */
    fun itemSuggestions(typed: String, exclude: Collection<String>): List<String> {
        val q = typed.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val skip = exclude.map { it.lowercase() }.toSet() + q
        val all = (itemHistory() + _reminders.value.flatMap { r -> r.items.map { it.name } })
            .distinctBy { it.lowercase() }
            .filter { it.lowercase() !in skip }
        val starts = all.filter { it.lowercase().startsWith(q) }
        val words = all.filter { it !in starts && it.lowercase().split(' ').any { w -> w.startsWith(q) } }
        val contains = if (q.length >= 2) all.filter { it !in starts && it !in words && q in it.lowercase() } else emptyList()
        return (starts + words + contains).take(6)
    }

    /** Called after every save (the home-screen widget redraws). */
    var onSaved: (() -> Unit)? = null

    private fun save(list: List<Reminder>) {
        _reminders.value = list
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        val tmp = File(file.parentFile, "reminders.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
        onSaved?.invoke()
        Backup.changed()
    }

    // ---------- backup ----------

    /** This person's own backup code: long and random, so only someone who has it can find the backup. */
    var recoveryCode: String
        get() {
            val c = prefs.getString("recoveryCode", null)
            if (c != null && c != Backup.LEGACY_CODE) return c
            return Backup.newCode().also { recoveryCode = it }
        }
        set(v) = prefs.edit().putString("recoveryCode", v).apply()

    /** Settings that belong to this phone only, never backed up or restored. */
    private val phoneOnly = setOf("micAsked", "notifAsked", "setupShown")

    fun remindersJson(): String = JSONArray().apply { _reminders.value.forEach { put(toJson(it)) } }.toString()

    fun prefsJson(): String {
        val o = JSONObject()
        prefs.all.forEach { (k, v) ->
            if (k in phoneOnly) return@forEach
            val t = when (v) {
                is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; is String -> "s"; else -> return@forEach
            }
            o.put(k, JSONObject().put("t", t).put("v", v))
        }
        return o.toString()
    }

    fun onPrefsChanged(l: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(l)

    /**
     * Replaces everything on this phone with a backup. Errands and live shared lists belonged to the
     * old install, so they come back as plain reminders on this phone.
     */
    @Synchronized
    fun restoreFrom(remindersJson: String, prefsJson: String) {
        val p = JSONObject(prefsJson)
        val e = prefs.edit()
        p.keys().forEach { k ->
            if (k in phoneOnly) return@forEach
            val o = p.getJSONObject(k)
            when (o.getString("t")) {
                "b" -> e.putBoolean(k, o.getBoolean("v"))
                "i" -> e.putInt(k, o.getInt("v"))
                "l" -> e.putLong(k, o.getLong("v"))
                "f" -> e.putFloat(k, o.getDouble("v").toFloat())
                "s" -> e.putString(k, o.getString("v"))
            }
        }
        e.commit()
        _events.value = emptyList()
        val arr = JSONArray(remindersJson)
        val list = (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }.map {
            if (it.taskId == null && it.sharedId == null) it
            else it.copy(
                sharedId = null, sharedOwner = false, sharedBy = "", sharedWith = emptyList(),
                taskId = null, taskRole = "", taskPerson = "", taskPersonUid = "", taskStatus = "",
                taskBoth = false, taskBaseAt = 0L,
            )
        }
        save(list)
    }

    private fun load(): List<Reminder> = try {
        if (!file.exists()) emptyList() else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun toJson(r: Reminder) = JSONObject().apply {
        put("id", r.id)
        put("type", r.type.name)
        put("title", r.title)
        put("notes", r.notes)
        put("dueAt", r.dueAt)
        put("repeat", r.repeat.name)
        put("everyDays", r.everyDays)
        put("anchorDay", r.anchorDay)
        put("snooze", r.snoozeUntil ?: JSONObject.NULL)
        put("completed", r.completed)
        put("rings", r.rings)
        put("isTest", r.isTest)
        put("maxNotifs", r.maxNotifs)
        put("notifCount", r.notifCount)
        put("nth", r.nth)
        put("weekday", r.weekday)
        put("weekMask", r.weekMask)
        put("fromDone", r.fromDone)
        put("pinned", r.pinned)
        val items = JSONArray()
        r.items.forEach {
            items.put(
                JSONObject().put("n", it.name).put("d", it.done).put("id", it.id).put("ts", it.ts)
                    .put("by", it.by).put("bu", it.byUid).put("db", it.doneBy),
            )
        }
        put("sharedId", r.sharedId ?: JSONObject.NULL)
        put("sharedOwner", r.sharedOwner)
        put("sharedBy", r.sharedBy)
        put("sharedWith", JSONArray(r.sharedWith))
        put("taskId", r.taskId ?: JSONObject.NULL)
        put("taskRole", r.taskRole)
        put("taskPerson", r.taskPerson)
        put("taskPersonUid", r.taskPersonUid)
        put("taskStatus", r.taskStatus)
        put("dueBy", r.dueBy)
        put("taskBoth", r.taskBoth)
        put("taskBaseAt", r.taskBaseAt)
        put("items", items)
        val hist = JSONArray()
        r.history.forEach { hist.put(JSONObject().put("due", it.dueAt).put("paid", it.paidAt)) }
        put("history", hist)
    }

    private fun fromJson(o: JSONObject): Reminder {
        val items = o.optJSONArray("items") ?: JSONArray()
        val hist = o.optJSONArray("history") ?: JSONArray()
        return Reminder(
            id = o.getLong("id"),
            type = Type.valueOf(o.getString("type")),
            title = o.optString("title"),
            notes = o.optString("notes"),
            dueAt = o.optLong("dueAt"),
            repeat = runCatching { Repeat.valueOf(o.optString("repeat", "NONE")) }.getOrDefault(Repeat.NONE),
            everyDays = o.optInt("everyDays", 1),
            anchorDay = o.optInt("anchorDay", 1),
            snoozeUntil = if (o.isNull("snooze")) null else o.getLong("snooze"),
            completed = o.optBoolean("completed"),
            rings = o.optInt("rings"),
            isTest = o.optBoolean("isTest"),
            maxNotifs = o.optInt("maxNotifs"),
            notifCount = o.optInt("notifCount"),
            nth = o.optInt("nth", 1),
            weekday = o.optInt("weekday", 6),
            weekMask = o.optInt("weekMask"),
            fromDone = o.optBoolean("fromDone"),
            pinned = o.optBoolean("pinned"),
            sharedId = if (o.isNull("sharedId") || !o.has("sharedId")) null else o.getString("sharedId"),
            sharedOwner = o.optBoolean("sharedOwner"),
            sharedBy = o.optString("sharedBy"),
            sharedWith = o.optJSONArray("sharedWith")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            taskId = if (o.isNull("taskId") || !o.has("taskId")) null else o.getString("taskId"),
            taskRole = o.optString("taskRole"),
            taskPerson = o.optString("taskPerson"),
            taskPersonUid = o.optString("taskPersonUid"),
            taskStatus = o.optString("taskStatus"),
            dueBy = o.optLong("dueBy"),
            taskBoth = o.optBoolean("taskBoth"),
            taskBaseAt = o.optLong("taskBaseAt"),
            items = (0 until items.length()).map {
                val j = items.getJSONObject(it)
                Item(
                    name = j.getString("n"), done = j.optBoolean("d"),
                    id = j.optString("id").ifBlank { newItemId() },
                    ts = j.optLong("ts", 0L).takeIf { t -> t > 0 } ?: (1_000_000L - it),
                    by = j.optString("by"), byUid = j.optString("bu"), doneBy = j.optString("db"),
                )
            },
            history = (0 until hist.length()).map {
                val j = hist.getJSONObject(it); Paid(j.getLong("due"), j.getLong("paid"))
            },
        )
    }
}

// ---------- time helpers ----------

fun toLdt(ms: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
fun fromLdt(d: LocalDateTime): Long = d.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val longDate = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

fun fmtTime(t: LocalTime): String = t.format(timeFmt).uppercase(Locale.ENGLISH)
fun fmtTime(ms: Long): String = fmtTime(toLdt(ms).toLocalTime())
fun fmtDate(d: LocalDate): String = d.format(longDate)
fun fmtShortDate(ms: Long): String = toLdt(ms).format(shortDate)

fun fmtWhen(ms: Long): String {
    val d = toLdt(ms).toLocalDate()
    val today = LocalDate.now()
    val day = when (d) {
        today -> "today"
        today.plusDays(1) -> "tomorrow"
        else -> fmtShortDate(ms)
    }
    return "$day ${fmtTime(ms)}"
}

fun ordinal(n: Int): String = n.toString() + when {
    n in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

fun repeatText(r: Reminder): String =
    (if (r.repeat == Repeat.NONE) "One time" else repeatShort(r)) + ", " + fmtTime(r.dueAt)

fun repeatShort(r: Reminder): String =
    repeatBase(r) + if (r.trailing) " after done" else ""

/** Daily, Weekly and Every N days can count the next one from when it's done. Bills keep their date. */
val Repeat.canTrail: Boolean get() = this == Repeat.DAILY || this == Repeat.WEEKLY || this == Repeat.EVERY_N_DAYS

/** This reminder's next one counts from the day it's marked done. */
val Reminder.trailing: Boolean get() = fromDone && repeat.canTrail && type != Type.BILL

private fun repeatBase(r: Reminder): String {
    val d = toLdt(r.dueAt)
    return when (r.repeat) {
        Repeat.NONE -> "Once"
        Repeat.DAILY -> "Daily"
        Repeat.WEEKLY -> "Weekly, " + d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        Repeat.MONTHLY -> "Monthly, ${ordinal(r.anchorDay)}"
        Repeat.YEARLY -> "Yearly, " + d.format(shortDate)
        Repeat.EVERY_N_DAYS -> "Every ${r.everyDays} days"
        Repeat.NTH_WEEKDAY -> "Monthly, ${Rules.nthLabel(r.nth)} ${Rules.dayShort(r.weekday)}"
        Repeat.WEEKDAYS -> Rules.maskLabel(r.weekMask)
        Repeat.EVERY_N_WEEKS -> "Every ${r.everyDays} weeks, ${Rules.dayShort(r.weekday)}"
        Repeat.LAST_DAY -> "Monthly, last day"
    }
}

/** "3m", "2h", "4d" */
fun shortSpan(ms: Long): String {
    val m = ms / 60_000
    return when {
        m < 60 -> "${max(1L, m)}m"
        m < 1440 -> "${m / 60}h"
        else -> "${m / 1440}d"
    }
}

fun statusText(r: Reminder, now: Long): String {
    if (r.completed) return "Done"
    if (r.dueAt == 0L) return if (r.sharedBy.isNotBlank()) "Shared by ${r.sharedBy}" else "No date"
    val s = r.snoozeUntil
    if (s != null && s > now) return "Snoozed till ${fmtWhen(s)}"
    if (r.dueAt <= now) {
        val days = ChronoUnit.DAYS.between(toLdt(r.dueAt).toLocalDate(), LocalDate.now())
        return if (days >= 1) "Overdue $days day${if (days > 1) "s" else ""}" else "Due now"
    }
    val days = ChronoUnit.DAYS.between(LocalDate.now(), toLdt(r.dueAt).toLocalDate())
    return when {
        days == 0L -> "Today ${fmtTime(r.dueAt)}"
        days == 1L -> "Tomorrow ${fmtTime(r.dueAt)}"
        days < 7 -> "In $days days"
        else -> fmtShortDate(r.dueAt)
    }
}

fun daysLate(p: Paid): Long =
    ChronoUnit.DAYS.between(toLdt(p.dueAt).toLocalDate(), toLdt(p.paidAt).toLocalDate())

/** Rules for check-later items in the daily digest. */
object Digest {
    private fun freq(r: Reminder) = if (r.repeat == Repeat.WEEKLY || r.repeat == Repeat.MONTHLY) r.repeat else Repeat.DAILY
    private fun dueDate(r: Reminder): LocalDate? = if (r.dueAt > 0) toLdt(r.dueAt).toLocalDate() else null
    private fun startDate(r: Reminder): LocalDate = toLdt(r.id).toLocalDate()

    fun isFinished(r: Reminder, today: LocalDate = LocalDate.now()): Boolean {
        if (r.maxNotifs > 0 && r.notifCount >= r.maxNotifs) return true
        val due = dueDate(r)
        return due != null && today.isAfter(due)
    }

    /** Should this item be in today's digest? */
    fun dueToday(r: Reminder, today: LocalDate = LocalDate.now()): Boolean {
        if (r.type != Type.CHECK || isFinished(r, today)) return false
        if (dueDate(r) == today) return true
        val start = startDate(r)
        return when (freq(r)) {
            Repeat.WEEKLY -> today.dayOfWeek == start.dayOfWeek
            Repeat.MONTHLY -> today.dayOfMonth == minOf(start.dayOfMonth, today.lengthOfMonth())
            else -> true
        }
    }

    fun describe(r: Reminder): String {
        val parts = mutableListOf<String>()
        parts += when (freq(r)) {
            Repeat.WEEKLY -> "Weekly, " + startDate(r).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            Repeat.MONTHLY -> "Monthly, ${ordinal(startDate(r).dayOfMonth)}"
            else -> "Daily"
        }
        parts += if (r.maxNotifs > 0) "${r.notifCount} of ${r.maxNotifs} reminders" else "until ticked off"
        dueDate(r)?.let { parts += "due " + it.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) }
        if (isFinished(r)) parts += "finished"
        return parts.joinToString(" · ")
    }
}

/** One card on the day timeline. */
data class Slot(
    val r: Reminder, val at: Long, val overdue: Boolean = false, val paid: Paid? = null,
    /** Snoozed to a later day: a faded card where it used to be. */
    val moved: Boolean = false,
)

object Timeline {
    fun slots(all: List<Reminder>, day: LocalDate, now: Long): List<Slot> {
        val today = LocalDate.now()
        val out = mutableListOf<Slot>()
        for (r in all) {
            if (r.isTest) continue
            if (r.type == Type.CHECK) {
                if (!day.isBefore(today) && Digest.dueToday(r, day)) {
                    out += Slot(r, fromLdt(day.atTime(Store.digestHour, Store.digestMinute)))
                }
                continue
            }
            if (!day.isAfter(today)) {
                r.history.filter { toLdt(it.paidAt).toLocalDate() == day }.forEach { out += Slot(r, it.paidAt, paid = it) }
            }
            if (r.completed || r.dueAt == 0L) continue
            val next = r.nextTrigger
            val nextDay = toLdt(next).toLocalDate()
            val snooze = r.snoozeUntil
            val dueDay = toLdt(r.dueAt).toLocalDate()
            if (day == today && !nextDay.isAfter(today)) {
                // Snoozed for a while: keep its usual spot; the card says when it rings.
                val at = if (snooze != null && dueDay == today && snooze > r.dueAt) r.dueAt else next
                out += Slot(r, at, overdue = next <= now)
            } else if (day == today && snooze != null && dueDay == today) {
                out += Slot(r, r.dueAt, moved = true)
            } else if (day.isAfter(today)) {
                if (nextDay == day) {
                    out += Slot(r, next)
                } else if (r.repeat != Repeat.NONE) {
                    var d = Rules.step(r, toLdt(r.dueAt))
                    var guard = 0
                    while (!d.toLocalDate().isAfter(day) && guard++ < 2000) {
                        if (d.toLocalDate() == day && d.toLocalDate() != nextDay) {
                            out += Slot(r, fromLdt(d)); break
                        }
                        d = Rules.step(r, d)
                    }
                }
            }
        }
        return out.sortedBy { it.at }
    }
}

/** Next-occurrence maths for every repeat type. */
object Rules {
    private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val fullDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun dayShort(d: Int) = days[(d - 1).coerceIn(0, 6)]
    fun dayFull(d: Int) = fullDays[(d - 1).coerceIn(0, 6)]
    fun nthLabel(n: Int) = if (n >= 5) "last" else ordinal(n)

    fun maskLabel(mask: Int): String {
        val picked = (1..7).filter { mask and (1 shl (it - 1)) != 0 }
        return when {
            picked.isEmpty() -> "No days picked"
            picked.size == 7 -> "Every day"
            picked == listOf(1, 2, 3, 4, 5) -> "Weekdays"
            picked == listOf(6, 7) -> "Weekends"
            else -> picked.joinToString(", ") { dayShort(it) }
        }
    }

    /** The nth (5 = last) given weekday of a month. */
    fun nthWeekdayOf(year: Int, month: Int, nth: Int, weekday: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
        if (nth >= 5) {
            var d = first.withDayOfMonth(first.lengthOfMonth())
            while (d.dayOfWeek.value != weekday) d = d.minusDays(1)
            return d
        }
        var d = first
        while (d.dayOfWeek.value != weekday) d = d.plusDays(1)
        return d.plusWeeks((nth - 1).toLong())
    }

    /** Does this date fit the rule (ignores intervals)? */
    fun matches(r: Reminder, date: LocalDate): Boolean = when (r.repeat) {
        Repeat.NTH_WEEKDAY -> date == nthWeekdayOf(date.year, date.monthValue, r.nth, r.weekday)
        Repeat.WEEKDAYS -> r.weekMask and (1 shl (date.dayOfWeek.value - 1)) != 0
        Repeat.EVERY_N_WEEKS -> date.dayOfWeek.value == r.weekday
        Repeat.LAST_DAY -> date.dayOfMonth == date.lengthOfMonth()
        else -> true
    }

    /** First occurrence at or after [start], keeping its time of day. */
    fun firstOnOrAfter(r: Reminder, start: LocalDateTime): LocalDateTime =
        if (!r.repeat.isCustom || r.repeat == Repeat.EVERY_N_DAYS || matches(r, start.toLocalDate())) start
        else nextMatchAfter(r, start)

    private fun nextMatchAfter(r: Reminder, d: LocalDateTime): LocalDateTime {
        if (r.repeat == Repeat.WEEKDAYS && r.weekMask == 0) return d.plusYears(1000)
        var x = d.plusDays(1)
        var guard = 0
        while (!matches(r, x.toLocalDate()) && guard++ < 400) x = x.plusDays(1)
        return x
    }

    /** The occurrence after [d]. */
    fun step(r: Reminder, d: LocalDateTime): LocalDateTime = when (r.repeat) {
        Repeat.DAILY -> d.plusDays(1)
        Repeat.WEEKLY -> d.plusWeeks(1)
        Repeat.MONTHLY -> {
            val m = d.plusMonths(1)
            m.withDayOfMonth(min(r.anchorDay, m.toLocalDate().lengthOfMonth()))
        }
        Repeat.YEARLY -> d.plusYears(1)
        Repeat.EVERY_N_DAYS -> d.plusDays(max(1, r.everyDays).toLong())
        Repeat.EVERY_N_WEEKS -> d.plusWeeks(max(1, r.everyDays).toLong())
        Repeat.NTH_WEEKDAY, Repeat.WEEKDAYS, Repeat.LAST_DAY -> nextMatchAfter(r, d)
        Repeat.NONE -> d.plusYears(1000)
    }
}
