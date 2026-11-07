package com.bakadev.yaad

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** days use java.util.Calendar values: SUNDAY = 1 … SATURDAY = 7 */
data class Reminder(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val text: String,
    val days: Set<Int> = ALL_DAYS,
    val enabled: Boolean = true,
) {
    companion object {
        val ALL_DAYS: Set<Int> = (1..7).toSet()
    }
}

object ReminderStore {
    private const val PREFS = "yaad"
    private const val KEY = "reminders"

    fun load(ctx: Context): List<Reminder> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val d = o.getJSONArray("days")
                Reminder(
                    id = o.getInt("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    text = o.getString("text"),
                    days = (0 until d.length()).map { d.getInt(it) }.toSet(),
                    enabled = o.optBoolean("enabled", true),
                )
            }.sortedWith(compareBy({ it.hour }, { it.minute }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(ctx: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("hour", r.hour)
                    .put("minute", r.minute)
                    .put("text", r.text)
                    .put("days", JSONArray(r.days.sorted()))
                    .put("enabled", r.enabled)
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, arr.toString())
            .commit()
    }

    fun get(ctx: Context, id: Int): Reminder? = load(ctx).firstOrNull { it.id == id }

    fun upsert(ctx: Context, r: Reminder) {
        save(ctx, load(ctx).filterNot { it.id == r.id } + r)
    }

    fun delete(ctx: Context, id: Int) {
        save(ctx, load(ctx).filterNot { it.id == id })
    }

    fun newId(ctx: Context): Int = (load(ctx).maxOfOrNull { it.id } ?: 0) + 1
}
