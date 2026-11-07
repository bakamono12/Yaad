package com.bakadev.yaad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(Scheduler.EXTRA_ID, -1)
        val isSnooze = intent.getBooleanExtra(Scheduler.EXTRA_SNOOZE, false)

        if (id == Scheduler.TEST_ID) {
            AnnounceService.start(ctx, id)
            return
        }
        val r = ReminderStore.get(ctx, id) ?: return
        // Line up tomorrow's (or the next chosen day's) alarm right away.
        if (!isSnooze) Scheduler.schedule(ctx, r)
        if (r.enabled) AnnounceService.start(ctx, id)
    }
}

/** Phone restarted, app updated or clock changed → set all alarms again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.rescheduleAll(ctx)
    }
}
