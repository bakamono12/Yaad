package com.bakadev.yaad

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakadev.yaad.ui.Cream
import com.bakadev.yaad.ui.DoneGreen
import com.bakadev.yaad.ui.Ink
import com.bakadev.yaad.ui.Muted
import com.bakadev.yaad.ui.Saffron
import com.bakadev.yaad.ui.YaadTheme
import java.util.Calendar

class AnnounceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over the lock screen and switch the screen on.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            YaadTheme {
                val active by AnnounceService.active.collectAsState()
                LaunchedEffect(active) { if (active == null) finish() }
                active?.let { r ->
                    AnnounceScreen(
                        text = r.text,
                        onDone = { AnnounceService.send(this, AnnounceService.ACTION_DONE); finish() },
                        onSnooze = { AnnounceService.send(this, AnnounceService.ACTION_SNOOZE); finish() },
                        onRepeat = { AnnounceService.send(this, AnnounceService.ACTION_REPEAT) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnounceScreen(
    text: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onRepeat: () -> Unit,
) {
    val now = Calendar.getInstance()
    val h = now.get(Calendar.HOUR_OF_DAY)
    val m = now.get(Calendar.MINUTE)

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Filled.Notifications,
            contentDescription = null,
            tint = Saffron,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(HindiTime.period(h), fontSize = 24.sp, color = Muted)
        Text(
            HindiTime.short(h, m),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )

        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text,
                fontSize = 40.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Saffron,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRepeat) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(6.dp))
                Text("फिर से सुनें", fontSize = 20.sp)
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DoneGreen, contentColor = Color.White),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Text("हो गया", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text("${AnnounceService.SNOOZE_MINUTES} मिनट बाद याद दिलाना", fontSize = 22.sp, color = Ink)
        }
        Spacer(Modifier.height(16.dp))
    }
}
