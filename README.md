# याद दिलाओ (Yaad)

A talking reminder app for Mom. At the set time it beeps, then **speaks the reminder out loud in Hindi** at full alarm volume — even if the phone is locked or on silent — and shows a big screen with one giant **"हो गया"** button.

> "मम्मी, सुनिए! सुबह के 8 बज गए हैं। दवाई लेनी है।"

If she doesn't tap anything, it repeats every 3 minutes (5 times). "10 मिनट बाद" snoozes.

**Voice:** a natural-sounding neural Hindi voice ([Piper](https://github.com/rhasspy/piper) "priyamvada", run by [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)) is bundled inside the app and runs fully offline. The phone's built-in voice is only used as a fallback if it can't start. Times are spoken naturally ("साढ़े आठ", "पौने नौ", "ढाई").

## Get the APK

**Option A — GitHub (no setup):**
1. Create a **private** repo and push this folder to `main`.
2. Go to **Actions → Build APK** (runs on push, ~5 min).
3. Download the `yaad-apk` artifact, unzip, send `app-release.apk` to Mom's phone and install (allow "install unknown apps").

**Option B — Android Studio:** run `bash scripts/fetch-voice.sh` once (downloads the ~80 MB voice and native libs, which aren't committed), then open the folder and press Run.

The repo includes a fixed signing key (`keystore/yaad.jks`), so every new build installs as an update and her reminders are kept. Keep the repo private.

## One-time setup on Mom's phone

1. Open the app. A yellow card lists anything that needs switching on — tap **चालू करें** on each until the card disappears.
2. Open the app once after installing — the first launch unpacks the voice (a few seconds).
3. **Xiaomi / Redmi / Realme / Oppo / Vivo / OnePlus:** these kill background apps. In phone Settings → Apps → याद दिलाओ:
   - turn on **Autostart**
   - set Battery to **No restrictions**
   - (Xiaomi) also enable **Show on lock screen** and **Display pop-up windows while running in background**
4. ⚙️ (top right) → set the name it calls out (default "मम्मी") → **10 सेकंड में टेस्ट करें**, lock the phone and wait. If it speaks, you're done.
5. Keep the phone's **alarm volume** on — the app pushes it to max during an announcement and restores it after.

## Adding reminders

**नया रिमाइंडर** → 1. what (type it or tap a suggestion) → 2. time → 3. days → **सुनकर देखें** to hear it → **सेव करें**. Tap any reminder to edit or delete; the switch turns it off without deleting.

## How it works (for you)

| Piece | File |
|---|---|
| Exact alarms via `AlarmManager.setAlarmClock` (survives Doze) | `Scheduler.kt` |
| Fires → reschedules next day → starts announcer | `Receivers.kt` |
| Foreground service: max alarm volume, beep, speak on `USAGE_ALARM`, repeat rounds | `AnnounceService.kt` |
| Offline neural Hindi voice (sherpa-onnx + Piper) → `AudioTrack` | `HindiVoice.kt` |
| Full-screen lock-screen UI | `AnnounceActivity.kt` |
| Reboot / time-change rescheduling | `BootReceiver` in `Receivers.kt` |
| Permission checklist | `Setup.kt` |
| Hindi time phrasing ("दोपहर के 2 बजकर 15 मिनट") | `Text.kt` |

Tweak repeat count / gap / snooze in the `companion object` of `AnnounceService`.
