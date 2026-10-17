package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.game.model.WeaponType
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    // Audio synthesis for realistic, unique weapon sound effects
    private val sampleRate = 22050
    private val weaponTracks = mutableMapOf<WeaponType, AudioTrack>()
    private var explosionTrack: AudioTrack? = null
    private var reloadTrack: AudioTrack? = null
    private var coinTrack: AudioTrack? = null
    private var hitTrack: AudioTrack? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        } catch (_: Exception) {
            toneGenerator = null
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            vibrator = null
        }

        initProceduralAudio()
    }

    private fun initProceduralAudio() {
        try {
            // Synthesize unique procedural sounds for each weapon
            for (type in WeaponType.values()) {
                val samples = generateWeaponPcm(type)
                val track = createStaticTrack(samples)
                if (track != null) {
                    weaponTracks[type] = track
                }
            }

            // Synthesize auxiliary sounds
            val explosionSamples = generateExplosionPcm()
            explosionTrack = createStaticTrack(explosionSamples)

            val reloadSamples = generateReloadPcm()
            reloadTrack = createStaticTrack(reloadSamples)

            val coinSamples = generateCoinPcm()
            coinTrack = createStaticTrack(coinSamples)

            val hitSamples = generateHitPcm()
            hitTrack = createStaticTrack(hitSamples)
        } catch (_: Exception) {}
    }

    private fun createStaticTrack(pcmData: ShortArray): AudioTrack? {
        return try {
            val bufferSize = pcmData.size * 2
            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STATIC
                )
            }
            track.write(pcmData, 0, pcmData.size)
            track
        } catch (_: Exception) {
            null
        }
    }

    private fun generateWeaponPcm(type: WeaponType): ShortArray {
        val random = Random()
        return when (type) {
            WeaponType.REVOLVER -> {
                // Sharp revolver crack (90ms)
                val count = (sampleRate * 0.08).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 55.0)
                    val noise = (random.nextDouble() * 2.0 - 1.0)
                    val tone = sin(2.0 * PI * 220.0 * t)
                    val sample = (0.75 * noise + 0.25 * tone) * env
                    buffer[i] = (sample * 28000).toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            WeaponType.SHOTGUN -> {
                // Heavy 12-gauge concussive blast with deep bass roar (220ms)
                val count = (sampleRate * 0.22).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 22.0)
                    val bass = sin(2.0 * PI * 65.0 * t) * exp(-t * 15.0)
                    val noise = (random.nextDouble() * 2.0 - 1.0)
                    val sample = (0.65 * noise + 0.35 * bass) * env
                    buffer[i] = (sample * 31000).toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            WeaponType.GRENADE -> {
                // Pin pull and underhand throw: short whoosh (160ms)
                val count = (sampleRate * 0.16).toInt()
                val buffer = ShortArray(count)
                var last = 0.0
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = sin(PI * (i.toDouble() / count))
                    last = 0.8 * last + 0.2 * (random.nextDouble() * 2.0 - 1.0)
                    val click = if (i < 120) (random.nextDouble() * 2.0 - 1.0) * 0.8 else 0.0
                    buffer[i] = ((last * env * 0.6 + click) * 20000).toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

        }
    }

    private fun generateExplosionPcm(): ShortArray {
        val count = (sampleRate * 0.28).toInt()
        val buffer = ShortArray(count)
        val random = Random()
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 16.0)
            val bass = sin(2.0 * PI * 50.0 * t) * exp(-t * 8.0)
            val noise = (random.nextDouble() * 2.0 - 1.0)
            val sample = (0.7 * noise + 0.3 * bass) * env
            buffer[i] = (sample * 31000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateReloadPcm(): ShortArray {
        val count = (sampleRate * 0.12).toInt()
        val buffer = ShortArray(count)
        val random = Random()
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            // Double metallic click
            val click1 = if (t in 0.01..0.03) (random.nextDouble() * 2.0 - 1.0) else 0.0
            val click2 = if (t in 0.07..0.09) (random.nextDouble() * 2.0 - 1.0) else 0.0
            buffer[i] = ((click1 + click2) * 22000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateCoinPcm(): ShortArray {
        val count = (sampleRate * 0.10).toInt()
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val freq = if (t < 0.05) 987.0 else 1318.0
            val env = exp(-t * 28.0)
            val sample = sin(2.0 * PI * freq * t) * env
            buffer[i] = (sample * 24000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateHitPcm(): ShortArray {
        val count = (sampleRate * 0.04).toInt()
        val buffer = ShortArray(count)
        val random = Random()
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 70.0)
            val noise = (random.nextDouble() * 2.0 - 1.0)
            buffer[i] = (noise * env * 22000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    fun playShotSound(weaponType: WeaponType) {
        try {
            val track = weaponTracks[weaponType]
            if (track != null) {
                try {
                    track.stop()
                    track.reloadStaticData()
                    track.play()
                } catch (_: Exception) {}
            } else {
                // Fallback to ToneGenerator
                when (weaponType) {
                    WeaponType.REVOLVER -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
                    WeaponType.SHOTGUN -> toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 80)
                    WeaponType.GRENADE -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 40)
                }
            }

            // Tailored haptic feedback for each weapon's punch
            when (weaponType) {
                WeaponType.REVOLVER -> vibrate(18)
                WeaponType.SHOTGUN -> vibrate(55)
                WeaponType.GRENADE -> vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun playExplosion() {
        try {
            val track = explosionTrack
            if (track != null) {
                track.stop()
                track.reloadStaticData()
                track.play()
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150)
            }
            vibrate(80)
        } catch (_: Exception) {}
    }

    fun playReload() {
        try {
            val track = reloadTrack
            if (track != null) {
                track.stop()
                track.reloadStaticData()
                track.play()
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 60)
            }
            vibrate(18)
        } catch (_: Exception) {}
    }

    fun playZombieHit() {
        try {
            val track = hitTrack
            if (track != null) {
                track.stop()
                track.reloadStaticData()
                track.play()
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 20)
            }
        } catch (_: Exception) {}
    }

    fun playZombieDeath() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 40)
        } catch (_: Exception) {}
    }

    fun playCoinPickup() {
        try {
            val track = coinTrack
            if (track != null) {
                track.stop()
                track.reloadStaticData()
                track.play()
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 45)
            }
        } catch (_: Exception) {}
    }

    fun playHouseDamage() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 90)
            vibrate(60)
        } catch (_: Exception) {}
    }

    fun playWaveCompleted() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 200)
            vibrate(50)
        } catch (_: Exception) {}
    }

    fun playGameOver() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_LOW_L, 400)
            vibrate(150)
        } catch (_: Exception) {}
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            weaponTracks.values.forEach {
                try {
                    it.stop()
                    it.release()
                } catch (_: Exception) {}
            }
            weaponTracks.clear()

            explosionTrack?.release()
            explosionTrack = null
            reloadTrack?.release()
            reloadTrack = null
            coinTrack?.release()
            coinTrack = null
            hitTrack?.release()
            hitTrack = null

            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}

