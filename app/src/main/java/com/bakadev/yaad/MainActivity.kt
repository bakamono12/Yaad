package com.bakadev.yaad

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bakadev.yaad.ui.HomeScreen
import com.bakadev.yaad.ui.YaadTheme

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var hindiOk by mutableStateOf<Boolean?>(null)
    private var reminders by mutableStateOf(emptyList<Reminder>())
    private var callName by mutableStateOf("")
    private var refresh by mutableIntStateOf(0)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reload()
        callName = Speech.callName(this)
        Scheduler.rescheduleAll(this)
        tts = TextToSpeech(this, this)

        setContent {
            YaadTheme {
                val tick = refresh
                val ok = hindiOk
                val setup = remember(tick, ok) { Setup.items(this@MainActivity, ok) }
                HomeScreen(
                    reminders = reminders,
                    setup = setup,
                    callName = callName,
                    newId = { ReminderStore.newId(this@MainActivity) },
                    onSave = { save(it) },
                    onToggle = { r, on -> save(r.copy(enabled = on)) },
                    onDelete = { delete(it) },
                    onPreview = { preview(it) },
                    onCallNameChange = {
                        Speech.setCallName(this@MainActivity, it)
                        callName = Speech.callName(this@MainActivity)
                    },
                    onTest = { Scheduler.testIn(this@MainActivity, 10) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
        reload()
    }

    override fun onInit(status: Int) {
        val t = tts
        hindiOk = if (status == TextToSpeech.SUCCESS && t != null) {
            val ok = t.setLanguage(Speech.HINDI) >= TextToSpeech.LANG_AVAILABLE
            t.setSpeechRate(0.85f)
            ok
        } else {
            false
        }
    }

    private fun reload() {
        reminders = ReminderStore.load(this)
    }

    private fun save(r: Reminder) {
        ReminderStore.upsert(this, r)
        Scheduler.schedule(this, r)
        reload()
    }

    private fun delete(id: Int) {
        Scheduler.cancel(this, id)
        ReminderStore.delete(this, id)
        reload()
    }

    private fun preview(text: String) {
        tts?.speak(Speech.message(this, text), TextToSpeech.QUEUE_FLUSH, null, "preview")
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
