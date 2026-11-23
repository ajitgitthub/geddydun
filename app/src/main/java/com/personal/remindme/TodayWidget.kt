package com.personal.remindme

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Home-screen widget (4 × 3): what's left for today, same list as the Today screen. */
class TodayWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        Store.init(ctx)
        render(ctx, mgr, ids)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_TICK) {
            Store.init(ctx)
            refresh(ctx)
        }
    }

    override fun onDisabled(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(tickPi(ctx))
    }

    companion object {
        private const val ACTION_TICK = "com.personal.remindme.WIDGET_TICK"
        private const val BLUE = 0xFFF09595.toInt()
        private const val FAINT = 0xFF6E6E74.toInt()
        private const val MUTED = 0xFF8E8E94.toInt()
        private const val RED = 0xFFE5534B.toInt()
        private const val BLUE_TEXT = 0xFFF7C1C1.toInt()

        /** Redraw every placed widget. Cheap; called whenever reminders change. */
        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, TodayWidget::class.java))
            if (ids.isEmpty()) return
            try {
                render(ctx, mgr, ids)
            } catch (_: Exception) {
            }
        }

        private fun openPi(ctx: Context, code: Int, target: String, mutable: Boolean = false): PendingIntent =
            PendingIntent.getActivity(
                ctx, code,
                Intent(ctx, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("open", target),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE,
            )

        private fun tickPi(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
            ctx, 41, Intent(ctx, TodayWidget::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            val all = Store.reminders.value
            val slots = Timeline.slots(all, today, now).filter { it.paid == null && !it.moved && !it.r.assignedByMe }

            val v = RemoteViews(ctx.packageName, R.layout.widget_today)

            val title = SpannableStringBuilder()
            title.append(today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
            title.append(".", ForegroundColorSpan(BLUE), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            title.append("  " + "%02d".format(today.dayOfMonth), ForegroundColorSpan(FAINT), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            v.setTextViewText(R.id.w_title, title)
            v.setTextViewText(R.id.w_count, if (slots.isEmpty()) "ALL DONE" else "${slots.size} LEFT")

            val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
            slots.forEachIndexed { i, s -> items.addItem(s.r.id * 31 + i, row(ctx, s)) }
            v.setRemoteAdapter(R.id.w_list, items.build())
            v.setEmptyView(R.id.w_list, R.id.w_empty)
            v.setPendingIntentTemplate(R.id.w_list, openPi(ctx, 42, "today", mutable = true))
            v.setTextViewText(R.id.w_empty, emptyText(all, today))

            val pinned = all.filter { it.pinned && it.type == Type.ERRAND && !it.isTest }.minByOrNull { it.id }
            if (pinned == null) {
                v.setViewVisibility(R.id.w_pinned, View.INVISIBLE)
            } else {
                val left = pinned.items.count { !it.done }
                val text = if (pinned.items.isEmpty()) pinned.title else "${pinned.title} · " + if (left == 0) "All got" else "$left left"
                v.setViewVisibility(R.id.w_pinned, View.VISIBLE)
                v.setTextViewText(R.id.w_pinned, text)
                v.setOnClickPendingIntent(R.id.w_pinned, openPi(ctx, 43, "view:${pinned.id}"))
            }
            v.setOnClickPendingIntent(R.id.w_header, openPi(ctx, 44, "today"))
            v.setOnClickPendingIntent(R.id.w_add, openPi(ctx, 45, "new"))

            mgr.updateAppWidget(ids, v)
            scheduleNext(ctx, slots, now)
        }

        private fun row(ctx: Context, s: Slot): RemoteViews {
            val r = s.r
            val late = s.overdue || taskOverdue(r)
            val pending = r.items.filter { !it.done }
            val base = when {
                r.type == Type.CHECK -> "Check later"
                r.type == Type.ERRAND && r.items.isNotEmpty() ->
                    if (pending.isEmpty()) "Everything got" else "${pending.size} left"
                else -> repeatShort(r)
            }
            val sub = buildString {
                if (s.overdue) append("Overdue · ")
                if (r.assignedToMe) append("From ${r.taskPerson} · due ${dueLabel(r.dueBy)}")
                else if (r.isBoth) append("${r.bothNames} · $base") else append(base)
            }
            val rv = RemoteViews(ctx.packageName, R.layout.widget_row)
            rv.setTextViewText(R.id.r_time, fmtTime(s.at))
            rv.setTextColor(R.id.r_time, if (late) RED else MUTED)
            rv.setTextViewText(R.id.r_title, r.title)
            rv.setTextViewText(R.id.r_sub, sub)
            rv.setTextColor(R.id.r_sub, if (late) RED else if (r.assignedToMe || r.isBoth) BLUE_TEXT else MUTED)
            rv.setOnClickFillInIntent(R.id.r_root, Intent().putExtra("open", "view:${r.id}"))
            return rv
        }

        private fun emptyText(all: List<Reminder>, today: LocalDate): String {
            val next = all.filter {
                !it.completed && !it.isTest && it.dueAt > 0 && !it.assignedByMe && it.type != Type.CHECK &&
                    toLdt(it.nextTrigger).toLocalDate().isAfter(today)
            }.minByOrNull { it.nextTrigger } ?: return "Nothing left for today"
            val d = toLdt(next.nextTrigger).toLocalDate()
            val day = if (d == today.plusDays(1)) "tomorrow" else d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            return "Nothing left. Next: $day ${fmtTime(next.nextTrigger)}"
        }

        /** Redraw when the next item turns overdue, and at midnight for the new day. */
        private fun scheduleNext(ctx: Context, slots: List<Slot>, now: Long) {
            val midnight = fromLdt(LocalDate.now().plusDays(1).atStartOfDay()) + 1_000
            val at = (slots.map { it.at + 1_000 }.filter { it > now } + midnight).min()
            ctx.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(AlarmManager.RTC, at, tickPi(ctx))
        }
    }
}
