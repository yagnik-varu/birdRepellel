package com.yagnik.birdrepeller.ui.roi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yagnik.birdrepeller.data.settings.RoiRepository

class RoiViewModelFactory(private val repository: RoiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
