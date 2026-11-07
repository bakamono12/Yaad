package com.bakadev.yaad

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object Scheduler {
    const val EXTRA_ID = "id"
    const val EXTRA_SNOOZE = "snooze"
    /** Special id used by the "test" button; never stored. */
    const val TEST_ID = 0
    private const val SNOOZE_OFFSET = 100_000

    /** The next moment (after now) this reminder should go off. */
    fun nextTrigger(r: Reminder, now: Long = System.currentTimeMillis()): Long {
        val days = r.days.ifEmpty { Reminder.ALL_DAYS }
        val cal = Calendar.getInstance()
        for (d in 0..7) {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, d)
            cal.set(Calendar.HOUR_OF_DAY, r.hour)
            cal.set(Calendar.MINUTE, r.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis > now && cal.get(Calendar.DAY_OF_WEEK) in days) {
                return cal.timeInMillis
            }
        }
        return now + 24 * 60 * 60_000L
    }

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun alarmIntent(ctx: Context, id: Int, snooze: Boolean): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_SNOOZE, snooze)
        val code = if (snooze) id + SNOOZE_OFFSET else id
        return PendingIntent.getBroadcast(
            ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAlarm(ctx: Context, at: Long, op: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        try {
            if (canExact(ctx)) {
                // setAlarmClock = treated like a real alarm clock: exact, wakes the phone,
                // and survives Doze. This is what makes it reliable.
                val show = PendingIntent.getActivity(
                    ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), op)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
            }
        } catch (e: SecurityException) {
            Log.e("Yaad", "exact alarm refused, falling back", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }

    fun schedule(ctx: Context, r: Reminder) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val op = alarmIntent(ctx, r.id, false)
        am.cancel(op)
        if (r.enabled) setAlarm(ctx, nextTrigger(r), op)
    }

    fun snooze(ctx: Context, id: Int, minutes: Int) {
        setAlarm(ctx, System.currentTimeMillis() + minutes * 60_000L, alarmIntent(ctx, id, true))
    }

    fun testIn(ctx: Context, seconds: Int) {
        setAlarm(ctx, System.currentTimeMillis() + seconds * 1000L, alarmIntent(ctx, TEST_ID, true))
    }

    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(alarmIntent(ctx, id, false))
        am.cancel(alarmIntent(ctx, id, true))
    }

    fun rescheduleAll(ctx: Context) {
        ReminderStore.load(ctx).forEach { schedule(ctx, it) }
    }
}
