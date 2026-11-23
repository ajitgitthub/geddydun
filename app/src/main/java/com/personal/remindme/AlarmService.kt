package com.personal.remindme

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Rings for 5 minutes, then goes quiet (alarm screen closes so the phone can sleep) and re-rings 30 minutes later.
 * Stays alive (with an undismissable notification) until every queued reminder is answered.
 */
class AlarmService : Service() {
    companion object {
        const val RING_MS = 5 * 60_000L
        const val RERING_MS = 30 * 60_000L
        private const val ACTION_FIRE = "fire"
        private const val ACTION_UPDATE = "update"
        private const val NOTIF_ID = 42

        /** Reminders waiting for an answer; the first one is on screen. */
        val queue = MutableStateFlow<List<Long>>(emptyList())

        @Volatile
        var activityVisible = false

        private var current: AlarmService? = null

        /** True while the alarm sound is on (false after the 5-minute timeout). */
        @Volatile
        var ringingNow = false

        /** True after the 5-minute timeout until the next ring: the alarm screen closes and lets the phone sleep. */
        val quiet = MutableStateFlow(false)

        /** Quiets the ring (and buzz) while the alarm speaks and listens; false brings it back. */
        fun duck(on: Boolean) {
            val s = current ?: return
            s.handler.post { s.setDucked(on) }
        }

        fun fire(ctx: Context, id: Long) {
            ContextCompat.startForegroundService(
                ctx, Intent(ctx, AlarmService::class.java).setAction(ACTION_FIRE).putExtra("id", id),
            )
        }

        fun resolve(ctx: Context, id: Long) {
            if (id !in queue.value) return
            queue.value = queue.value - id
            try {
                ctx.startService(Intent(ctx, AlarmService::class.java).setAction(ACTION_UPDATE))
            } catch (_: Exception) {
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringing = false
    private var watchdogOn = false

    private var ducked = false

    override fun onCreate() {
        super.onCreate()
        current = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setDucked(on: Boolean) {
        if (ducked == on) return
        ducked = on
        try {
            // Softly, not silent, so the alarm is still heard while it listens.
            // Very soft while listening: the ring is the main thing that drowns out a far-away voice.
            player?.setVolume(if (on) 0.04f else 1f, if (on) 0.04f else 1f)
        } catch (_: Exception) {
        }
        if (on) vibrator?.cancel() else if (ringing) vibrate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FIRE -> {
                val id = intent.getLongExtra("id", -1)
                val r = Store.get(id)
                if (r != null && !r.completed) {
                    Store.upsert(r.copy(rings = r.rings + 1))
                    Store.logAlarm(r, if (r.rings == 0) "Alarm rang" else "Alarm rang again · ${r.rings + 1} times")
                    if (id !in queue.value) queue.value = queue.value + id
                }
                goForeground()
                if (queue.value.isEmpty()) {
                    stopAll(); return START_NOT_STICKY
                }
                startRinging()
                launchActivity()
            }
            else -> if (queue.value.isEmpty()) stopAll() else goForeground()
        }
        return START_NOT_STICKY
    }

    private fun goForeground() {
        val r = queue.value.firstOrNull()?.let { Store.get(it) }
        val pi = PendingIntent.getActivity(
            this, 1,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (ringing) "Tap to respond" else "Rings again in 30 min. Tap to respond."
        val n = NotificationCompat.Builder(this, Notifs.ALARM)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(r?.title ?: "Reminder")
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
        ServiceCompat.startForeground(
            this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    private fun launchActivity() {
        try {
            startActivity(
                Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
        }
    }

    private fun startRinging() {
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, RING_MS)
        if (!watchdogOn) {
            watchdogOn = true
            handler.post(watchdog)
        }
        if (ringing) return
        ringing = true
        ringingNow = true
        quiet.value = false
        goForeground()

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "remindme:ring")
            .apply { acquire(RING_MS + 30_000L) }

        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            null
        }

        vibrator = getSystemService(VibratorManager::class.java).defaultVibrator
        ducked = false
        vibrate()
    }

    private fun vibrate() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator?.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
    }

    private fun stopRinging() {
        ringing = false
        ringingNow = false
        handler.removeCallbacks(timeout)
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release()
        player = null
        vibrator?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    /** 5 minutes with no answer: go quiet, close the alarm screen so the phone can sleep, ring again in 30 minutes. */
    private val timeout = Runnable {
        stopRinging()
        handler.removeCallbacks(watchdog)
        watchdogOn = false
        quiet.value = true
        val now = System.currentTimeMillis()
        queue.value.forEach { id ->
            Store.get(id)?.let {
                val u = it.copy(snoozeUntil = now + RERING_MS)
                Store.upsert(u)
                Scheduler.schedule(this, u)
            }
        }
        goForeground()
    }

    /** Brings the alarm screen back if you leave it (only while the screen is on). */
    private val watchdog = object : Runnable {
        override fun run() {
            if (queue.value.isEmpty()) {
                watchdogOn = false; return
            }
            if (!activityVisible && getSystemService(PowerManager::class.java).isInteractive) {
                launchActivity()
            }
            handler.postDelayed(this, 2000)
        }
    }

    private fun stopAll() {
        stopRinging()
        handler.removeCallbacksAndMessages(null)
        watchdogOn = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (current === this) current = null
        stopRinging()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
