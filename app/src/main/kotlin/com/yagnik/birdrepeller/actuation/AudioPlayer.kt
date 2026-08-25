package com.yagnik.birdrepeller.actuation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.yagnik.birdrepeller.data.settings.AudioRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean

class AudioPlayer(
    private val context: Context,
    private val repository: AudioRepository
) {
    private var mediaPlayer: MediaPlayer? = null
    private val isPlaying = AtomicBoolean(false)

    /**
     * Plays the next deterrent sound from the playlist.
     * Strategy: Random-without-immediate-repeat.
     */
    suspend fun playNextDeterrent() {
        if (isPlaying.get()) {
            Log.d("AudioPlayer", "Already playing, skipping request")
            return
        }

        val allUris = repository.audioUrisFlow.first().toList()
        if (allUris.isEmpty()) {
            Log.w("AudioPlayer", "No audio files in playlist")
            return
        }

        val lastPlayed = repository.lastPlayedUriFlow.first()
        val eligibleUris = if (allUris.size > 1) {
            allUris.filter { it != lastPlayed }
        } else {
            allUris
        }

        val nextUri = eligibleUris.random()
        playUri(nextUri)
    }

    private suspend fun playUri(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            
            // 1. Force Max Volume
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

            // 2. Setup MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                setOnCompletionListener {
                    Log.d("AudioPlayer", "Playback completed: $uriString")
                    cleanup()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer error: $what, $extra for $uriString")
                    cleanup()
                    true
                }
                prepare()
                start()
            }
            
            isPlaying.set(true)
            repository.setLastPlayedUri(uriString)
            Log.d("AudioPlayer", "Started playing: $uriString at max volume ($maxVolume)")

        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play audio: ${e.message}", e)
            cleanup()
        }
    }

    private fun cleanup() {
        isPlaying.set(false)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
