package com.bakadev.yaad

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar

/**
 * Speaks the reminder out loud at full alarm volume, then repeats every few minutes
 * until Mom taps "हो गया" (done) or "बाद में" (snooze).
 */
class AnnounceService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "Yaad"
        const val CHANNEL_ID = "announce_v1"
        const val NOTIF_ID = 4242
        const val ACTION_DONE = "com.bakadev.yaad.DONE"
        const val ACTION_SNOOZE = "com.bakadev.yaad.SNOOZE"
        const val ACTION_REPEAT = "com.bakadev.yaad.REPEAT"

        /** How many times to announce if nobody responds */
        const val ROUNDS = 5
        /** Gap between announcements */
        const val ROUND_GAP_MS = 3 * 60_000L
        const val SNOOZE_MINUTES = 10
        private const val GIVE_UP_MS = 2 * 60 * 60_000L

        /** The reminder currently being announced (null = nothing ringing). */
        val active = MutableStateFlow<Reminder?>(null)

        fun start(ctx: Context, id: Int) {
            val i = Intent(ctx, AnnounceService::class.java).putExtra(Scheduler.EXTRA_ID, id)
            try {
                ContextCompat.startForegroundService(ctx, i)
            } catch (e: Exception) {
                Log.e(TAG, "could not start announcer", e)
            }
        }

        fun send(ctx: Context, action: String) {
            try {
                ctx.startService(Intent(ctx, AnnounceService::class.java).setAction(action))
            } catch (e: Exception) {
                Log.e(TAG, "could not send $action", e)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var tone: ToneGenerator? = null
    private var wake: PowerManager.WakeLock? = null
    private var savedVolume = -1
    private var rounds = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DONE -> {
                finishAll(); return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                active.value?.let { Scheduler.snooze(this, it.id, SNOOZE_MINUTES) }
                finishAll(); return START_NOT_STICKY
            }
            ACTION_REPEAT -> {
                active.value?.let { r ->
                    boostVolume()
                    speak(r)
                    if (rounds >= ROUNDS) handler.postDelayed({ restoreVolume() }, 30_000)
                }
                return START_NOT_STICKY
            }
        }

        val id = intent?.getIntExtra(Scheduler.EXTRA_ID, -1) ?: -1
        val r = if (id == Scheduler.TEST_ID) testReminder() else ReminderStore.get(this, id)

        // Must go foreground immediately, before anything else.
        active.value = r
        createChannel()
        val n = buildNotification(r ?: Reminder(-1, 0, 0, ""))
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            getSystemService(NotificationManager::class.java).notify(NOTIF_ID, n)
        }

        if (r == null) {
            finishAll(); return START_NOT_STICKY
        }

        handler.removeCallbacksAndMessages(null)
        rounds = 0
        acquireWake()
        boostVolume()
        handler.postDelayed({ finishAll() }, GIVE_UP_MS)

        when {
            tts == null -> {
                tts = TextToSpeech(this, this)
                // Safety net if the speech engine never answers: still beep.
                handler.postDelayed({ if (rounds == 0) runRound() }, 6_000)
            }
            ttsReady -> runRound()
            // else: onInit is still pending and will start the first round
        }
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        val t = tts
        if (status == TextToSpeech.SUCCESS && t != null) {
            t.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM) // plays even on silent/vibrate
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            ttsReady = t.setLanguage(Speech.HINDI) >= TextToSpeech.LANG_AVAILABLE
            t.setSpeechRate(0.85f) // a little slower = clearer
        }
        if (rounds == 0 && active.value != null) runRound()
    }

    private fun runRound() {
        val r = active.value ?: return
        rounds++
        beep()
        handler.postDelayed({ speak(r) }, 1_200)
        if (rounds < ROUNDS) {
            handler.postDelayed({ runRound() }, ROUND_GAP_MS)
        } else {
            // Stop making noise, but leave the notification until she taps it.
            handler.postDelayed({ restoreVolume(); releaseWake() }, 60_000)
        }
    }

    private fun speak(r: Reminder) {
        val t = tts
        if (!ttsReady || t == null) {
            beep(); return
        }
        val msg = Speech.message(this, r.text)
        t.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "m1")
        t.playSilentUtterance(1_500, TextToSpeech.QUEUE_ADD, "gap")
        t.speak(msg, TextToSpeech.QUEUE_ADD, null, "m2")
    }

    private fun beep() {
        try {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
            tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
        } catch (e: Exception) {
            Log.w(TAG, "beep failed", e)
        }
    }

    private fun testReminder(): Reminder {
        val c = Calendar.getInstance()
        return Reminder(
            Scheduler.TEST_ID,
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            "यह एक टेस्ट है। रिमाइंडर ठीक से काम कर रहा है",
        )
    }

    // ---- volume & wake lock -------------------------------------------------

    private fun boostVolume() {
        val am = getSystemService(AudioManager::class.java)
        try {
            if (savedVolume < 0) savedVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        } catch (e: Exception) {
            Log.w(TAG, "volume boost failed", e)
        }
    }

    private fun restoreVolume() {
        if (savedVolume < 0) return
        try {
            getSystemService(AudioManager::class.java)
                .setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "volume restore failed", e)
        }
        savedVolume = -1
    }

    private fun acquireWake() {
        if (wake?.isHeld == true) return
        wake = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "yaad:announce")
            .apply { acquire(20 * 60_000L) }
    }

    private fun releaseWake() {
        try {
            if (wake?.isHeld == true) wake?.release()
        } catch (_: Exception) {
        }
        wake = null
    }

    // ---- notification -------------------------------------------------------

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "रिमाइंडर", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "रिमाइंडर बोलकर बताना"
            setSound(null, null) // the app speaks; no extra ringtone
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 400, 700)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(r: Reminder): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = Intent(this, AnnounceActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openPi = PendingIntent.getActivity(this, 1, open, flags)
        val donePi = PendingIntent.getService(
            this, 2, Intent(this, AnnounceService::class.java).setAction(ACTION_DONE), flags
        )
        val snoozePi = PendingIntent.getService(
            this, 3, Intent(this, AnnounceService::class.java).setAction(ACTION_SNOOZE), flags
        )
        val now = Calendar.getInstance()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.text.ifBlank { "रिमाइंडर" })
            .setContentText(
                "${HindiTime.display(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))} • काम होने पर 'हो गया' दबाएँ"
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPi)
            .setFullScreenIntent(openPi, true)
            .addAction(0, "✅ हो गया", donePi)
            .addAction(0, "⏰ $SNOOZE_MINUTES मिनट बाद", snoozePi)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    // ---- teardown -----------------------------------------------------------

    private fun finishAll() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        restoreVolume()
        releaseWake()
        active.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.shutdown()
        tts = null
        tone?.release()
        tone = null
        restoreVolume()
        releaseWake()
        active.value = null
        super.onDestroy()
    }
}
