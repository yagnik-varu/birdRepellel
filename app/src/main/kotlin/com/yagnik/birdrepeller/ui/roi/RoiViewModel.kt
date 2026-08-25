package com.yagnik.birdrepeller.ui.roi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yagnik.birdrepeller.data.settings.NormalizedRoi
import com.yagnik.birdrepeller.data.settings.RoiRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RoiViewModel(private val repository: RoiRepository) : ViewModel() {

    var roi by mutableStateOf(NormalizedRoi())
        private set

    var isEditMode by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            repository.roiFlow.collectLatest { savedRoi ->
                roi = savedRoi
            }
        }
    }

    fun updateRoi(newRoi: NormalizedRoi) {
        roi = newRoi
    }

    fun toggleEditMode() {
        isEditMode = !isEditMode
    }

    fun saveRoi() {
        viewModelScope.launch {
            repository.saveRoi(roi)
            isEditMode = false
        }
    }

    fun resetRoi() {
        viewModelScope.launch {
            repository.resetRoi()
        }
    }
}
