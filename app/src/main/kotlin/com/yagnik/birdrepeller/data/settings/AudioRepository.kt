package com.yagnik.birdrepeller.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AudioRepository(private val context: Context) {

    private val KEY_AUDIO_URIS = stringSetPreferencesKey("audio_uris")
    private val KEY_LAST_PLAYED_URI = stringPreferencesKey("last_played_uri")

    val audioUrisFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUDIO_URIS] ?: emptySet()
    }

    val lastPlayedUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_PLAYED_URI]
    }

    suspend fun addAudioUris(uris: Set<String>) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_AUDIO_URIS] ?: emptySet()
            preferences[KEY_AUDIO_URIS] = current + uris
        }
    }

    suspend fun removeAudioUri(uri: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_AUDIO_URIS] ?: emptySet()
            preferences[KEY_AUDIO_URIS] = current - uri
            if (preferences[KEY_LAST_PLAYED_URI] == uri) {
                preferences.remove(KEY_LAST_PLAYED_URI)
            }
        }
    }

    suspend fun setLastPlayedUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_PLAYED_URI] = uri
        }
    }
}
