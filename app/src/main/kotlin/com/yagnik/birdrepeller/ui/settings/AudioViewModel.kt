package com.yagnik.birdrepeller.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yagnik.birdrepeller.data.settings.AudioRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AudioFile(
    val uri: Uri,
    val name: String,
    val isValid: Boolean = true
)

class AudioViewModel(
    private val context: Context,
    private val repository: AudioRepository
) : ViewModel() {

    var audioFiles by mutableStateOf<List<AudioFile>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            repository.audioUrisFlow.collectLatest { uris ->
                audioFiles = uris.map { uriString ->
                    val uri = Uri.parse(uriString)
                    val name = getFileName(uri)
                    val isValid = checkUriValid(uri)
                    AudioFile(uri, name, isValid)
                }
            }
        }
    }

    fun addAudioUris(uris: List<Uri>) {
        val strings = uris.map { it.toString() }.toSet()
        viewModelScope.launch {
            repository.addAudioUris(strings)
        }
    }

    fun removeAudioFile(audioFile: AudioFile) {
        viewModelScope.launch {
            repository.removeAudioUri(audioFile.uri.toString())
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "Unknown"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            name = uri.lastPathSegment ?: "Unknown"
        }
        return name
    }

    private fun checkUriValid(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
