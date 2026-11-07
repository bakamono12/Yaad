package com.bakadev.yaad

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class SetupItem(val title: String, val desc: String, val fix: () -> Unit)

/** Everything that must be switched on for reminders to ring reliably. */
object Setup {
    fun items(act: MainActivity, hindiOk: Boolean?): List<SetupItem> {
        val list = mutableListOf<SetupItem>()
        val pkg = act.packageName
        val pkgUri = Uri.parse("package:$pkg")

        val notifGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(act, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!notifGranted) {
            list += SetupItem("नोटिफ़िकेशन की इजाज़त दें", "इसके बिना रिमाइंडर स्क्रीन पर नहीं आएगा") {
                act.requestNotifications()
            }
        } else if (!NotificationManagerCompat.from(act).areNotificationsEnabled()) {
            list += SetupItem("नोटिफ़िकेशन चालू करें", "अभी इस ऐप के नोटिफ़िकेशन बंद हैं") {
                act.open(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                )
            }
        }

        if (!Scheduler.canExact(act)) {
            list += SetupItem("ठीक समय पर बजने दें", "\"अलार्म और रिमाइंडर\" की इजाज़त चाहिए") {
                act.open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri))
            }
        }

        if (Build.VERSION.SDK_INT >= 34 &&
            !act.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) {
            list += SetupItem("लॉक स्क्रीन पर दिखाएँ", "फ़ोन लॉक हो तब भी बड़ी स्क्रीन खुले") {
                act.open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri))
            }
        }

        if (!act.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(pkg)) {
            list += SetupItem("बैटरी सेविंग से छूट दें", "ताकि फ़ोन इस ऐप को बंद न करे") {
                act.open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri))
            }
        }

        if (hindiOk == false) {
            list += SetupItem("हिंदी आवाज़ डाउनलोड करें", "फ़ोन में हिंदी बोलने वाली आवाज़ नहीं मिली") {
                act.open(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            }
        }
        return list
    }
}
