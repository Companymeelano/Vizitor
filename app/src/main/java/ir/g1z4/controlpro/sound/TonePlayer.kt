package ir.g1z4.controlpro.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.g1z4.controlpro.data.settings.SettingsStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

@Singleton
class TonePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsStore
) {
    enum class Kind { SENT, SUCCESS, ERROR, ALERT, ARM }

    suspend fun play(kind: Kind) {
        val prefs = settings.settings.first()
        if (!prefs.sounds) return
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio?.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        val freq = when (kind) {
            Kind.SENT -> 494.0
            Kind.SUCCESS -> 660.0
            Kind.ERROR -> 196.0
            Kind.ALERT -> 880.0
            Kind.ARM -> 523.0
        }
        val rate = 22050
        val n = (rate * 0.16).toInt()
        val pcm = ShortArray(n) { i ->
            val env = (1.0 - i.toDouble() / n).coerceIn(0.0, 1.0)
            (sin(2.0 * PI * freq * i / rate) * env * 12000).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
    }
}
