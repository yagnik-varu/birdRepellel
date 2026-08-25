package com.yagnik.birdrepeller.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.compose.ui.geometry.Offset

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "roi_settings")

class RoiRepository(private val context: Context) {

    private val KEY_TL_X = floatPreferencesKey("roi_tl_x")
    private val KEY_TL_Y = floatPreferencesKey("roi_tl_y")
    private val KEY_TR_X = floatPreferencesKey("roi_tr_x")
    private val KEY_TR_Y = floatPreferencesKey("roi_tr_y")
    private val KEY_BL_X = floatPreferencesKey("roi_bl_x")
    private val KEY_BL_Y = floatPreferencesKey("roi_bl_y")
    private val KEY_BR_X = floatPreferencesKey("roi_br_x")
    private val KEY_BR_Y = floatPreferencesKey("roi_br_y")

    val roiFlow: Flow<NormalizedRoi> = context.dataStore.data.map { preferences ->
        NormalizedRoi(
            topLeft = Offset(preferences[KEY_TL_X] ?: 0.2f, preferences[KEY_TL_Y] ?: 0.2f),
            topRight = Offset(preferences[KEY_TR_X] ?: 0.8f, preferences[KEY_TR_Y] ?: 0.2f),
            bottomLeft = Offset(preferences[KEY_BL_X] ?: 0.2f, preferences[KEY_BL_Y] ?: 0.8f),
            bottomRight = Offset(preferences[KEY_BR_X] ?: 0.8f, preferences[KEY_BR_Y] ?: 0.8f)
        )
    }

    suspend fun saveRoi(roi: NormalizedRoi) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TL_X] = roi.topLeft.x
            preferences[KEY_TL_Y] = roi.topLeft.y
            preferences[KEY_TR_X] = roi.topRight.x
            preferences[KEY_TR_Y] = roi.topRight.y
            preferences[KEY_BL_X] = roi.bottomLeft.x
            preferences[KEY_BL_Y] = roi.bottomLeft.y
            preferences[KEY_BR_X] = roi.bottomRight.x
            preferences[KEY_BR_Y] = roi.bottomRight.y
        }
    }

    suspend fun resetRoi() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
