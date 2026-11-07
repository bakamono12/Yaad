package com.bakadev.yaad

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Offline neural Hindi voice: Piper "priyamvada" running on sherpa-onnx.
 * Everything runs on the phone — no internet, no Google voice pack needed.
 */
object HindiVoice {
    private const val TAG = "YaadVoice"
    private const val MODEL_DIR = "vits-piper-hi_IN-priyamvada-medium"
    private const val MODEL = "hi_IN-priyamvada-medium.onnx"
    /** Bump when the bundled espeak data changes, so it's re-copied. */
    private const val DATA_VERSION = 1
    /** < 1 = slower. 0.9 is calm and clear without sounding sleepy. */
    private const val SPEED = 0.9f
    private const val GAP_SECONDS = 1.5f

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger(0)

    @Volatile private var engine: OfflineTts? = null
    @Volatile private var failed = false
    @Volatile private var current: AudioTrack? = null

    /** True if the voice files are bundled and the engine hasn't failed. */
    fun available(ctx: Context): Boolean = !failed && bundled(ctx)

    private fun bundled(ctx: Context): Boolean = try {
        ctx.assets.list(MODEL_DIR)?.contains(MODEL) == true
    } catch (e: Exception) {
        false
    }

    /** Load the model in the background so the first announcement is instant. */
    fun warmUp(ctx: Context) {
        val app = ctx.applicationContext
        worker.execute { ensure(app) }
    }

    /**
     * Speak [text] [times] times (with a short pause between).
     * [alarm] = play on the alarm stream (loud, ignores silent mode).
     * [onDone] runs on the main thread: true if it played, false if the caller
     * should fall back to the phone's built-in voice.
     */
    fun speak(
        ctx: Context,
        text: String,
        alarm: Boolean,
        times: Int = 1,
        onDone: (Boolean) -> Unit = {},
    ) {
        val app = ctx.applicationContext
        val gen = generation.incrementAndGet()
        stopTrack()
        worker.execute {
            if (gen != generation.get()) return@execute // superseded or stopped
            val tts = ensure(app)
            if (tts == null) {
                main.post { onDone(false) }
                return@execute
            }
            val ok = try {
                val audio = tts.generate(text, sid = 0, speed = SPEED)
                if (gen == generation.get() && audio.samples.isNotEmpty()) {
                    play(withRepeats(audio.samples, audio.sampleRate, times), audio.sampleRate, alarm, gen)
                }
                true
            } catch (t: Throwable) {
                Log.e(TAG, "generate/play failed", t)
                false
            }
            main.post { onDone(ok) }
        }
    }

    fun stop() {
        generation.incrementAndGet()
        stopTrack()
    }

    // ---- internals ----------------------------------------------------------

    private fun ensure(ctx: Context): OfflineTts? {
        engine?.let { return it }
        if (failed) return null
        return try {
            if (!bundled(ctx)) {
                failed = true
                return null
            }
            val dataDir = copyEspeakData(ctx)
            val config = getOfflineTtsConfig(
                modelDir = MODEL_DIR,
                modelName = MODEL,
                acousticModelName = "",
                vocoder = "",
                voices = "",
                lexicon = "",
                dataDir = dataDir,
                dictDir = "",
                ruleFsts = "",
                ruleFars = "",
                numThreads = 2,
            )
            OfflineTts(assetManager = ctx.assets, config = config).also {
                engine = it
                Log.i(TAG, "Hindi voice ready (${it.sampleRate()} Hz)")
            }
        } catch (t: Throwable) {
            // e.g. unsupported CPU — we'll quietly fall back to the system voice.
            Log.e(TAG, "could not start Hindi voice", t)
            failed = true
            null
        }
    }

    /** espeak-ng needs real files, not assets, so copy them once to app storage. */
    private fun copyEspeakData(ctx: Context): String {
        val target = File(ctx.filesDir, "espeak-ng-data")
        val marker = File(ctx.filesDir, "espeak-ng-data.v$DATA_VERSION")
        if (marker.exists() && target.isDirectory) return target.absolutePath
        target.deleteRecursively()
        copyAssetTree(ctx, "$MODEL_DIR/espeak-ng-data", target)
        marker.createNewFile()
        return target.absolutePath
    }

    private fun copyAssetTree(ctx: Context, assetPath: String, dest: File) {
        val children = ctx.assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            dest.parentFile?.mkdirs()
            ctx.assets.open(assetPath).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            dest.mkdirs()
            children.forEach { copyAssetTree(ctx, "$assetPath/$it", File(dest, it)) }
        }
    }

    private fun withRepeats(samples: FloatArray, sampleRate: Int, times: Int): FloatArray {
        val n = times.coerceAtLeast(1)
        val gap = (sampleRate * GAP_SECONDS).toInt()
        val tail = sampleRate / 3 // a little silence so the last word isn't clipped
        val out = FloatArray(samples.size * n + gap * (n - 1) + tail)
        var pos = 0
        repeat(n) { i ->
            samples.copyInto(out, pos)
            pos += samples.size
            if (i < n - 1) pos += gap
        }
        return out
    }

    private fun play(samples: FloatArray, sampleRate: Int, alarm: Boolean, gen: Int) {
        val attrs = AudioAttributes.Builder()
            .setUsage(if (alarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minBuf, sampleRate * 4 / 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        current = track
        try {
            track.play()
            var offset = 0
            while (offset < samples.size && gen == generation.get()) {
                val n = track.write(
                    samples, offset, minOf(4096, samples.size - offset), AudioTrack.WRITE_BLOCKING
                )
                if (n <= 0) break
                offset += n
            }
            // Wait for the buffered audio to finish playing.
            val deadline = SystemClock.uptimeMillis() + 3_000
            while (gen == generation.get() &&
                track.playbackHeadPosition < offset &&
                SystemClock.uptimeMillis() < deadline
            ) {
                Thread.sleep(50)
            }
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            track.release()
            if (current === track) current = null
        }
    }

    private fun stopTrack() {
        current?.let {
            try {
                it.pause()
                it.flush()
            } catch (_: Exception) {
            }
        }
    }
}
