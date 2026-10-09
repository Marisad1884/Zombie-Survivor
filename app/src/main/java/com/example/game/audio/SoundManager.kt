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
            WeaponType.PISTOL -> {
                // Sharp 9mm handgun pop with quick crack (80ms)
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

            WeaponType.CROSSBOW -> {
                // Mechanical bowstring release "twang/thwip" (100ms) - wooden pitch bend
                val count = (sampleRate * 0.10).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 30.0)
                    val freq = 380.0 - (t * 1800.0).coerceAtMost(220.0)
                    val stringTone = sin(2.0 * PI * freq * t)
                    val rustle = (random.nextDouble() * 2.0 - 1.0) * exp(-t * 90.0)
                    val sample = (0.7 * stringTone + 0.3 * rustle) * env
                    buffer[i] = (sample * 26000).toInt().coerceIn(-32767, 32767).toShort()
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

            WeaponType.RIFLE -> {
                // Snappy military 5.56 rifle crack (65ms) - sharp supersonic impulse
                val count = (sampleRate * 0.065).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 60.0)
                    val crack = sin(2.0 * PI * 850.0 * t) * exp(-t * 90.0)
                    val noise = (random.nextDouble() * 2.0 - 1.0)
                    val sample = (0.6 * noise + 0.4 * crack) * env
                    buffer[i] = (sample * 29000).toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            WeaponType.FLAMETHROWER -> {
                // Pressurized napalm flame jet whoosh and hiss (140ms)
                val count = (sampleRate * 0.14).toInt()
                val buffer = ShortArray(count)
                var lastNoise = 0.0
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = sin(PI * (i.toDouble() / count)) // smooth bell curve
                    val rawNoise = (random.nextDouble() * 2.0 - 1.0)
                    // Low-pass filter for deep whoosh
                    lastNoise = 0.7 * lastNoise + 0.3 * rawNoise
                    val rumble = sin(2.0 * PI * 110.0 * t) * 0.3
                    val sample = (lastNoise + rumble) * env
                    buffer[i] = (sample * 27000).toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            WeaponType.LAUNCHER -> {
                // Heavy pneumatic launch tube "THUMP" with propellant pressure (180ms)
                val count = (sampleRate * 0.18).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 26.0)
                    val thump = sin(2.0 * PI * 85.0 * t) * exp(-t * 18.0)
                    val rush = (random.nextDouble() * 2.0 - 1.0) * exp(-t * 35.0)
                    val sample = (0.6 * thump + 0.4 * rush) * env
                    buffer[i] = (sample * 31000).toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            WeaponType.MINIGUN -> {
                // Rapid rotary Gatling motor whine and sharp cyclic bullet punch (40ms)
                val count = (sampleRate * 0.04).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 85.0)
                    val motor = sin(2.0 * PI * 1200.0 * t) * 0.3
                    val metallic = (random.nextDouble() * 2.0 - 1.0) * 0.7
                    val sample = (motor + metallic) * env
                    buffer[i] = (sample * 29000).toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            WeaponType.SNIPER -> {
                // High-caliber .50 BMG rifle thunderclap: supersonic whip + deep bass echo (300ms)
                val count = (sampleRate * 0.30).toInt()
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 14.0)
                    val crack = (random.nextDouble() * 2.0 - 1.0) * exp(-t * 80.0)
                    val thunder = sin(2.0 * PI * 48.0 * t) * exp(-t * 10.0)
                    val sub = sin(2.0 * PI * 95.0 * t) * exp(-t * 16.0)
                    val sample = (0.45 * crack + 0.35 * thunder + 0.20 * sub) * env
                    buffer[i] = (sample * 32000).toInt().coerceIn(-32767, 32767).toShort()
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
                    WeaponType.PISTOL -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
                    WeaponType.CROSSBOW -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 30)
                    WeaponType.SHOTGUN -> toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 80)
                    WeaponType.RIFLE -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 25)
                    WeaponType.FLAMETHROWER -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 20)
                    WeaponType.LAUNCHER -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 120)
                    WeaponType.MINIGUN -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 20)
                    WeaponType.SNIPER -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 140)
                }
            }

            // Tailored haptic feedback for each weapon's punch
            when (weaponType) {
                WeaponType.PISTOL -> vibrate(16)
                WeaponType.CROSSBOW -> vibrate(12)
                WeaponType.SHOTGUN -> vibrate(50)
                WeaponType.RIFLE -> vibrate(14)
                WeaponType.FLAMETHROWER -> vibrate(8)
                WeaponType.LAUNCHER -> vibrate(65)
                WeaponType.MINIGUN -> vibrate(9)
                WeaponType.SNIPER -> vibrate(75)
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

