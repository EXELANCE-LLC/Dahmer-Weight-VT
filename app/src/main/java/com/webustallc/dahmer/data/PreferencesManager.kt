package com.webustallc.dahmer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scale_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val BASELINE_PRESSURE = floatPreferencesKey("baseline_pressure")
        private val CALIBRATION_FACTOR = floatPreferencesKey("calibration_factor")
        private val IS_CALIBRATED = booleanPreferencesKey("is_calibrated")
        private val UNIT_IS_GRAMS = booleanPreferencesKey("unit_is_grams")
    }

    val baselinePressure: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[BASELINE_PRESSURE] ?: 0f }

    val calibrationFactor: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[CALIBRATION_FACTOR] ?: 1.0f }

    val isCalibrated: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_CALIBRATED] ?: false }

    val unitIsGrams: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[UNIT_IS_GRAMS] ?: true }

    suspend fun saveCalibration(baseline: Float, factor: Float) {
        context.dataStore.edit { preferences ->
            preferences[BASELINE_PRESSURE] = baseline
            preferences[CALIBRATION_FACTOR] = factor
            preferences[IS_CALIBRATED] = true
        }
    }

    suspend fun resetCalibration() {
        context.dataStore.edit { preferences ->
            preferences[BASELINE_PRESSURE] = 0f
            preferences[CALIBRATION_FACTOR] = 1.0f
            preferences[IS_CALIBRATED] = false
        }
    }

    suspend fun setUnit(isGrams: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[UNIT_IS_GRAMS] = isGrams
        }
    }
}
