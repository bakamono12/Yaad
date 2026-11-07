package com.bakadev.yaad

import android.content.Context
import java.util.Calendar
import java.util.Locale

object HindiTime {
    fun period(h: Int): String = when (h) {
        in 4..11 -> "सुबह"
        in 12..15 -> "दोपहर"
        in 16..18 -> "शाम"
        else -> "रात"
    }

    fun h12(h: Int): Int = (h % 12).let { if (it == 0) 12 else it }

    /** "8:05" */
    fun short(h: Int, m: Int): String = "${h12(h)}:${m.toString().padStart(2, '0')}"

    /** "सुबह 8:05" */
    fun display(h: Int, m: Int): String = "${period(h)} ${short(h, m)}"

    /** What the phone says out loud: "सुबह के 8 बजकर 5 मिनट हो गए हैं" */
    fun spoken(h: Int, m: Int): String {
        val hh = h12(h)
        return when {
            m == 0 && hh == 1 -> "${period(h)} का 1 बज गया है"
            m == 0 -> "${period(h)} के $hh बज गए हैं"
            else -> "${period(h)} के $hh बजकर $m मिनट हो गए हैं"
        }
    }
}

object Days {
    /** Monday-first, the way most of us read a week */
    val ORDER = listOf(2, 3, 4, 5, 6, 7, 1)

    val short = mapOf(
        1 to "रवि", 2 to "सोम", 3 to "मंगल", 4 to "बुध",
        5 to "गुरु", 6 to "शुक्र", 7 to "शनि",
    )

    val full = mapOf(
        1 to "रविवार", 2 to "सोमवार", 3 to "मंगलवार", 4 to "बुधवार",
        5 to "गुरुवार", 6 to "शुक्रवार", 7 to "शनिवार",
    )

    fun summary(days: Set<Int>): String = when {
        days.size == 7 || days.isEmpty() -> "रोज़"
        days == setOf(2, 3, 4, 5, 6) -> "सोम से शुक्र"
        else -> ORDER.filter { it in days }.joinToString(", ") { short.getValue(it) }
    }
}

object Speech {
    val HINDI: Locale = Locale("hi", "IN")
    private const val PREFS = "yaad"
    private const val KEY_NAME = "call_name"

    fun callName(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NAME, "मम्मी") ?: ""

    fun setCallName(ctx: Context, name: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_NAME, name.trim()).apply()
    }

    /** "मम्मी, सुनिए! सुबह के 8 बज गए हैं। दवाई लेनी है।" */
    fun message(ctx: Context, text: String, now: Calendar = Calendar.getInstance()): String {
        val name = callName(ctx).trim()
        val hello = if (name.isEmpty()) "सुनिए!" else "$name, सुनिए!"
        val time = HindiTime.spoken(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        return "$hello $time। ${text.trim()}।"
    }
}
