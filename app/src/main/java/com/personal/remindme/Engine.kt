package com.personal.remindme

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

enum class Act { DONE, SNOOZE, TOMORROW }

object Scheduler {
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private fun alarmPi(ctx: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        ctx, id.hashCode(),
        Intent(ctx, AlarmReceiver::class.java).setAction("ALARM_$id").putExtra("id", id),
        FLAGS,
    )

    fun cancel(ctx: Context, id: Long) {
        ctx.getSystemService(AlarmManager::class.java).cancel(alarmPi(ctx, id))
    }

    fun schedule(ctx: Context, r: Reminder) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmPi(ctx, r.id)
        am.cancel(pi)
        if (r.type == Type.CHECK || r.completed || r.dueAt == 0L || r.assignedByMe) return
        val now = System.currentTimeMillis()
        // Already ringing for an old trigger: don't fire a duplicate.
        if (r.nextTrigger <= now && r.id in AlarmService.queue.value) return
        val t = max(r.nextTrigger, now + 1000)
        if (am.canScheduleExactAlarms()) {
            val show = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java), FLAGS,
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(t, show), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        }
    }

    fun scheduleDigest(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 7, Intent(ctx, DigestReceiver::class.java).setAction(DigestReceiver.SHOW), FLAGS,
        )
        val now = System.currentTimeMillis()
        val snooze = Store.digestSnoozeUntil
        val t = if (snooze > now) snooze else {
            var d = LocalDate.now().atTime(Store.digestHour, Store.digestMinute)
            if (!d.isAfter(LocalDateTime.now())) d = d.plusDays(1)
            fromLdt(d)
        }
        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
    }

    fun rescheduleAll(ctx: Context) {
        Store.reminders.value.forEach { schedule(ctx, it) }
        scheduleDigest(ctx)
    }

    /** Moves a reminder to its next cycle (or marks a one-time one complete). */
    fun advance(r: Reminder): Reminder {
        if (r.repeat == Repeat.NONE) return r.copy(completed = true, snoozeUntil = null, rings = 0)
        if (r.trailing) {
            // Counts from today (the day it's done), at its usual time.
            val d = Rules.step(r, LocalDate.now().atTime(toLdt(r.dueAt).toLocalTime()))
            return r.copy(dueAt = fromLdt(d), snoozeUntil = null, rings = 0)
        }
        var d = toLdt(r.dueAt)
        val now = LocalDateTime.now()
        do {
            d = Rules.step(r, d)
        } while (!d.isAfter(now))
        return r.copy(dueAt = fromLdt(d), snoozeUntil = null, rings = 0)
    }

    fun tomorrowSameTime(r: Reminder): Long =
        fromLdt(LocalDate.now().plusDays(1).atTime(toLdt(r.dueAt).toLocalTime()))
}

private fun snoozeLabel(m: Int) = if (m < 60) "${m}m" else "${m / 60}h"

object Actions {
    fun handle(ctx: Context, id: Long, act: Act, minutes: Int = 0, listMode: ListMode? = null) {
        val r = Store.get(id)
        if (r == null) {
            AlarmService.resolve(ctx, id); return
        }
        val now = System.currentTimeMillis()
        if (act == Act.DONE && r.isTest) {
            Celebrate.play(ctx) // test alarms celebrate too
            Store.delete(id); Scheduler.cancel(ctx, id); AlarmService.resolve(ctx, id); return
        }
        val updated = when (act) {
            Act.DONE -> {
                val withHistory = if (r.type == Type.BILL)
                    r.copy(history = listOf(Paid(r.dueAt, now)) + r.history) else r
                val next = Scheduler.advance(withHistory)
                if (listMode != null) next.copy(items = applyListMode(next.items, listMode)) else next
            }
            Act.SNOOZE -> r.copy(snoozeUntil = now + minutes * 60_000L, rings = 0)
            Act.TOMORROW -> r.copy(snoozeUntil = Scheduler.tomorrowSameTime(r), rings = 0)
        }
        Store.upsert(updated)
        if (act == Act.DONE) {
            Store.logDone(r)
            Celebrate.play(ctx)
        }
        when (act) {
            Act.SNOOZE -> Store.logAlarm(r, "Snoozed ${snoozeLabel(minutes)} · rings ${fmtTime(updated.nextTrigger)}")
            Act.TOMORROW -> Store.logAlarm(r, "Moved to ${fmtWhen(updated.nextTrigger)}")
            Act.DONE -> Store.logAlarm(r, if (r.type == Type.BILL) "Marked paid" else "Marked done")
        }
        AlarmService.resolve(ctx, id)
        Scheduler.schedule(ctx, updated)
        if (act == Act.DONE && r.assignedToMe) Cloud.taskDone(updated)
    }
}

object Perms {
    fun notifications(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    fun exact(ctx: Context) = ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun overlay(ctx: Context) = Settings.canDrawOverlays(ctx)
    fun fullScreen(ctx: Context) = if (Build.VERSION.SDK_INT >= 34)
        ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent() else true
    fun battery(ctx: Context) =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    fun missingCount(ctx: Context) = listOf(
        notifications(ctx), exact(ctx), overlay(ctx), fullScreen(ctx), battery(ctx),
    ).count { !it }

    fun allOk(ctx: Context) =
        notifications(ctx) && exact(ctx) && overlay(ctx) && fullScreen(ctx) && battery(ctx)
}

object Notifs {
    const val ALARM = "alarm"
    const val DIGEST = "digest"
    const val DIGEST_ID = 77
    const val QUIET = "quiet"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(QUIET, "Quiet reminders and invites", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(DIGEST, "Check later digest", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    @SuppressLint("MissingPermission")
    fun showDigest(ctx: Context, countIt: Boolean) {
        val items = Store.reminders.value.filter { Digest.dueToday(it) }
        if (items.isEmpty() || !Perms.notifications(ctx)) return
        if (countIt) items.forEach { Store.upsert(it.copy(notifCount = it.notifCount + 1)) }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            ctx, 2,
            Intent(ctx, MainActivity::class.java).putExtra("digest", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags,
        )
        val tomorrow = PendingIntent.getBroadcast(
            ctx, 3, Intent(ctx, DigestReceiver::class.java).setAction(DigestReceiver.TOMORROW), flags,
        )
        val style = NotificationCompat.InboxStyle()
        items.take(6).forEach { style.addLine(it.title) }
        if (items.size > 6) style.setSummaryText("+${items.size - 6} more")
        val n = NotificationCompat.Builder(ctx, DIGEST)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("${items.size} thing${if (items.size > 1) "s" else ""} to check today")
            .setContentText(items.joinToString(", ") { it.title })
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setSilent(true)
            .addAction(0, "Remind in…", open)
            .addAction(0, "Tomorrow", tomorrow)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(DIGEST_ID, n)
        } catch (_: SecurityException) {
        }
    }

    fun cancelDigest(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(DIGEST_ID)

    /** A plain, quiet notification (assigned errands, updates from the other person). */
    @SuppressLint("MissingPermission")
    fun note(ctx: Context, key: String, title: String, text: String, target: String = "today") {
        if (!Perms.notifications(ctx)) return
        // target: "review:<taskId>", "task:<taskId>" or "today". Tapping opens that screen.
        val open = PendingIntent.getActivity(
            ctx, key.hashCode(),
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("open", target),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, QUIET)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(key.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }

    private const val NAG_EVERY = 4 * 60 * 60 * 1000L

    /**
     * Something waiting for my OK. Shown once; if I swipe it away without answering,
     * it comes back every 4 hours. [seen] names this exact request, so new ones show at once.
     */
    fun nag(ctx: Context, seen: String, key: String, title: String, text: String, target: String) {
        val p = ctx.getSharedPreferences("nags", Context.MODE_PRIVATE)
        val last = p.getLong(seen, 0L)
        val now = System.currentTimeMillis()
        if (last > 0) {
            val showing = runCatching {
                ctx.getSystemService(android.app.NotificationManager::class.java)
                    .activeNotifications.any { it.id == key.hashCode() }
            }.getOrDefault(false)
            if (showing || now - last < NAG_EVERY) return
        }
        note(ctx, key, title, text, target)
        p.edit().putLong(seen, now).apply()
    }

    /** Forget requests that are no longer waiting. */
    fun keepNags(ctx: Context, waiting: Set<String>) {
        val p = ctx.getSharedPreferences("nags", Context.MODE_PRIVATE)
        val gone = p.all.keys.filter { it !in waiting }
        if (gone.isNotEmpty()) p.edit().apply { gone.forEach { remove(it) } }.apply()
    }

    /** Alarms are off today: a silent notification instead of the full-screen alarm. */
    @SuppressLint("MissingPermission")
    fun showQuiet(ctx: Context, r: Reminder, next: String) {
        if (!Perms.notifications(ctx)) return
        val open = PendingIntent.getActivity(
            ctx, 6, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, QUIET)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("${r.title} · ${fmtTime(r.nextTrigger)}")
            .setContentText("Alarms are off today. Next: $next")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(r.id.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }
}

object Quiet {
    /** True if this reminder should stay silent right now. */
    fun applies(r: Reminder): Boolean = Store.mutedToday && r.id < Store.mutedAt && !r.isTest

    /** Bills and one-off reminders move to tomorrow; repeating errands go to their next date. */
    fun defer(ctx: Context, r: Reminder) {
        val moved = if (r.type == Type.BILL || r.repeat == Repeat.NONE) {
            r.copy(snoozeUntil = Scheduler.tomorrowSameTime(r), rings = 0)
        } else {
            Scheduler.advance(r)
        }
        Store.upsert(moved)
        Scheduler.schedule(ctx, moved)
        Notifs.showQuiet(ctx, r, fmtWhen(moved.nextTrigger))
        Store.logAlarm(r, "Stayed silent (alarms off today) · next ${fmtWhen(moved.nextTrigger)}")
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1)
        val r = Store.get(id) ?: return
        if (r.completed || r.type == Type.CHECK || r.assignedByMe) return
        if (Quiet.applies(r)) return Quiet.defer(ctx, r)
        AlarmService.fire(ctx, id)
    }
}

class DigestReceiver : BroadcastReceiver() {
    companion object {
        const val SHOW = "com.personal.remindme.DIGEST_SHOW"
        const val TOMORROW = "com.personal.remindme.DIGEST_TOMORROW"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            SHOW -> {
                val wasSnoozed = Store.digestSnoozeUntil > 0
                Store.digestSnoozeUntil = 0
                Notifs.showDigest(ctx, countIt = !wasSnoozed)
                Scheduler.scheduleDigest(ctx)
            }
            TOMORROW -> {
                Store.digestSnoozeUntil = 0
                Notifs.cancelDigest(ctx)
                Scheduler.scheduleDigest(ctx)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.rescheduleAll(ctx)
        TodayWidget.refresh(ctx)
    }
}
