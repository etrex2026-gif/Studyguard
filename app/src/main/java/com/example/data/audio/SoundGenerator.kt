package com.example.data.audio

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundGenerator {
    private const val TAG = "SoundGenerator"
    private const val SAMPLE_RATE = 44100

    fun ensureBuiltInSounds(context: Context) {
        val soundsDir = File(context.filesDir, "sounds")
        if (!soundsDir.exists()) {
            soundsDir.mkdirs()
        }

        val radarFile = File(soundsDir, "radar_alarm.wav")
        if (!radarFile.exists() || radarFile.length() < 1000) {
            generateRadarWav(radarFile)
        }

        val chimeFile = File(soundsDir, "chime_alarm.wav")
        if (!chimeFile.exists() || chimeFile.length() < 1000) {
            generateChimeWav(chimeFile)
        }

        val sirenFile = File(soundsDir, "siren_alarm.wav")
        if (!sirenFile.exists() || sirenFile.length() < 1000) {
            generateSirenWav(sirenFile)
        }
    }

    fun getSoundFile(context: Context, fileName: String): File {
        val soundsDir = File(context.filesDir, "sounds")
        if (!soundsDir.exists()) soundsDir.mkdirs()
        val file = File(soundsDir, fileName)
        if (!file.exists()) {
            ensureBuiltInSounds(context)
        }
        return file
    }

    private fun generateRadarWav(file: File) {
        try {
            val durationSeconds = 2.0
            val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
            val samples = ShortArray(numSamples)

            // Two rhythmic high-impact beeps
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val sampleValue = when {
                    t in 0.0..0.25 -> {
                        // High punchy 950Hz with sharp envelope
                        val envelope = if (t < 0.02) t / 0.02 else exp(-(t - 0.02) * 8.0)
                        (sin(2.0 * PI * 950.0 * t) * envelope * 28000).toInt()
                    }
                    t in 0.35..0.60 -> {
                        val tRel = t - 0.35
                        val envelope = if (tRel < 0.02) tRel / 0.02 else exp(-(tRel - 0.02) * 8.0)
                        (sin(2.0 * PI * 1350.0 * tRel) * envelope * 30000).toInt()
                    }
                    t in 0.9..1.15 -> {
                        val tRel = t - 0.9
                        val envelope = if (tRel < 0.02) tRel / 0.02 else exp(-(tRel - 0.02) * 8.0)
                        (sin(2.0 * PI * 950.0 * tRel) * envelope * 28000).toInt()
                    }
                    t in 1.25..1.50 -> {
                        val tRel = t - 1.25
                        val envelope = if (tRel < 0.02) tRel / 0.02 else exp(-(tRel - 0.02) * 8.0)
                        (sin(2.0 * PI * 1350.0 * tRel) * envelope * 30000).toInt()
                    }
                    else -> 0
                }
                samples[i] = sampleValue.coerceIn(-32767, 32767).toShort()
            }
            writeWavFile(file, samples, SAMPLE_RATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating radar WAV", e)
        }
    }

    private fun generateChimeWav(file: File) {
        try {
            val durationSeconds = 2.5
            val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
            val samples = ShortArray(numSamples)

            // Melodic ascending bell chimes (E5: 659.25Hz, G#5: 830.6Hz, B5: 987.77Hz, E6: 1318.5Hz)
            val notes = listOf(
                Pair(0.0, 659.25),
                Pair(0.35, 830.6),
                Pair(0.70, 987.77),
                Pair(1.05, 1318.5)
            )

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                var sum = 0.0
                for ((noteStart, freq) in notes) {
                    if (t >= noteStart) {
                        val noteT = t - noteStart
                        val decay = exp(-noteT * 3.5)
                        val fundamental = sin(2.0 * PI * freq * noteT)
                        val harmonic = 0.35 * sin(2.0 * PI * freq * 2.0 * noteT)
                        sum += (fundamental + harmonic) * decay * 14000
                    }
                }
                samples[i] = sum.toInt().coerceIn(-32767, 32767).toShort()
            }
            writeWavFile(file, samples, SAMPLE_RATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating chime WAV", e)
        }
    }

    private fun generateSirenWav(file: File) {
        try {
            val durationSeconds = 2.0
            val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
            val samples = ShortArray(numSamples)

            // Dynamic oscillating wake siren (sweeps smoothly between 700Hz and 1400Hz)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // 3 oscillations per second
                val instantFreq = 1050.0 + 350.0 * sin(2.0 * PI * 3.0 * t)
                phase += 2.0 * PI * instantFreq / SAMPLE_RATE
                val sampleValue = (sin(phase) * 31000).toInt()
                samples[i] = sampleValue.coerceIn(-32767, 32767).toShort()
            }
            writeWavFile(file, samples, SAMPLE_RATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating siren WAV", e)
        }
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val dataSize = samples.size * (bitsPerSample / 8)
        val totalSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                // RIFF chunk descriptor
                put("RIFF".toByteArray())
                putInt(totalSize)
                put("WAVE".toByteArray())
                // fmt sub-chunk
                put("fmt ".toByteArray())
                putInt(16) // Subchunk1Size for PCM
                putShort(1) // AudioFormat (1 = PCM)
                putShort(numChannels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort((numChannels * (bitsPerSample / 8)).toShort()) // BlockAlign
                putShort(bitsPerSample.toShort())
                // data sub-chunk
                put("data".toByteArray())
                putInt(dataSize)
            }
            fos.write(header.array())

            // Write PCM audio data
            val audioBuffer = ByteBuffer.allocate(samples.size * 2).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                for (s in samples) {
                    putShort(s)
                }
            }
            fos.write(audioBuffer.array())
        }
    }
}
