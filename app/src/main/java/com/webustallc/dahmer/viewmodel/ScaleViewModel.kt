package com.webustallc.dahmer.viewmodel

import android.app.Application
import android.view.MotionEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webustallc.dahmer.data.PreferencesManager
import com.webustallc.dahmer.sensor.TouchPressureManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ScaleViewModel(application: Application) : AndroidViewModel(application) {

    private val touchPressureManager = TouchPressureManager(application)
    private val preferencesManager = PreferencesManager(application)

    val weight = touchPressureManager.weight
    val touchPressure = touchPressureManager.touchPressure
    val isSensorAvailable = touchPressureManager.isSensorAvailable
    val isScaleActive = touchPressureManager.isActive

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _calibrationStep = MutableStateFlow(CalibrationStep.NONE)
    val calibrationStep: StateFlow<CalibrationStep> = _calibrationStep.asStateFlow()

    private val _unitIsGrams = MutableStateFlow(true)
    val unitIsGrams: StateFlow<Boolean> = _unitIsGrams.asStateFlow()

    private val _statusMessage = MutableStateFlow("Place object on the weighing area")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _measurementAccuracy = MutableStateFlow(MeasurementAccuracy.UNCALIBRATED)
    val measurementAccuracy: StateFlow<MeasurementAccuracy> = _measurementAccuracy.asStateFlow()

    enum class CalibrationStep {
        NONE,           // Not calibrating
        ZERO_PENDING,   // Zero calibration requested but not applied
        ZERO_COMPLETE,  // Zero calibration done, waiting for weight calibration
        WEIGHT_PENDING, // Weight calibration in progress
        COMPLETE        // All calibrations done
    }

    enum class MeasurementAccuracy {
        UNCALIBRATED,   // No calibration performed
        ZERO_ONLY,      // Only zero calibration done
        FULLY_CALIBRATED, // Both zero and weight calibration done
        HIGH_PRECISION  // Multiple calibrations with validation
    }

    init {
        // Load saved preferences
        viewModelScope.launch {
            preferencesManager.unitIsGrams.collect { isGrams ->
                _unitIsGrams.value = isGrams
            }
        }

        // Load and apply saved calibration - TEMPORARILY DISABLED FOR DEBUGGING
        viewModelScope.launch {
            combine(
                preferencesManager.baselinePressure,
                preferencesManager.calibrationFactor,
                preferencesManager.isCalibrated
            ) { zeroOffset, weightFactor, isCalibrated ->
                if (isCalibrated) {
                    android.util.Log.d("ScaleViewModel", "SKIPPING calibration load: zeroOffset=$zeroOffset (would override fresh start)")
                    // touchPressureManager.applyCalibration(zeroOffset, 9.81f, weightFactor)
                    // updateAccuracyStatus()
                }
            }.collect { }
        }

        // Enhanced status monitoring with professional feedback
        viewModelScope.launch {
            combine(weight, touchPressure, isScaleActive) { currentWeight, pressure, isActive ->
                updateStatusMessage(currentWeight, pressure, isActive)
            }.collect { }
        }

        // Monitor calibration status
        viewModelScope.launch {
            // Check calibration status periodically
            updateAccuracyStatus()
        }
    }

    private fun updateStatusMessage(weight: Float, pressure: Float, isActive: Boolean) {
        val newMessage = when {
            !isActive -> "Scale is stopped - Press Start to begin"
            weight > 250f -> "⚠️ DANGER: Remove weight immediately! Risk of screen damage!"
            weight > 200f -> "⚠️ WARNING: Weight too high - Remove object to prevent damage"
            !touchPressureManager.isCalibrated() -> {
                when {
                    weight < 0.5f -> "Ready - Touch screen to measure weight"  
                    weight < 2f -> "Detecting: ${getDisplayWeight()}"
                    else -> "Measuring: ${getDisplayWeight()} (Uncalibrated)"
                }
            }
            weight < 0.5f -> "Ready - Place object and apply pressure"
            weight < 1f -> "Detecting: ${getDisplayWeight()}"
            weight < 5f -> "Light weight: ${getDisplayWeight()}"
            weight < 20f -> "Measuring: ${getDisplayWeight()}"
            weight < 100f -> "Weight: ${getDisplayWeight()}"
            weight < 200f -> "Heavy weight: ${getDisplayWeight()}"
            else -> "⚠️ Maximum safe capacity: ${getDisplayWeight()}"
        }
        _statusMessage.value = newMessage
        android.util.Log.d("ScaleViewModel", "Status updated: $newMessage (weight=$weight, pressure=$pressure)")
    }

    private fun updateAccuracyStatus() {
        _measurementAccuracy.value = when {
            !touchPressureManager.isCalibrated() -> MeasurementAccuracy.UNCALIBRATED
            touchPressureManager.isCalibrated() -> MeasurementAccuracy.FULLY_CALIBRATED
            else -> MeasurementAccuracy.ZERO_ONLY
        }
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        return touchPressureManager.handleTouchEvent(event)
    }

    fun startScale() {
        touchPressureManager.startListening()
        _statusMessage.value = "Scale started - Place object on weighing area"
    }

    fun stopScale() {
        touchPressureManager.stopListening()
        _statusMessage.value = "Scale stopped"
    }

    fun startCalibration() {
        if (isScaleActive.value) {
            _isCalibrating.value = true
            _calibrationStep.value = CalibrationStep.ZERO_PENDING
            _statusMessage.value = "Calibration started - Remove all objects first"
        } else {
            _statusMessage.value = "Please start the scale before calibration"
        }
    }

    fun performZeroCalibration() {
        if (_calibrationStep.value == CalibrationStep.ZERO_PENDING) {
            touchPressureManager.calibrateZero()
            _calibrationStep.value = CalibrationStep.ZERO_COMPLETE
            _statusMessage.value = "Zero calibration completed. Place a known weight object."
            updateAccuracyStatus()
        }
    }

    fun performWeightCalibration(knownWeight: Float) {
        if (_calibrationStep.value == CalibrationStep.ZERO_COMPLETE && knownWeight > 0) {
            _calibrationStep.value = CalibrationStep.WEIGHT_PENDING
            
            // Validate that there's actual pressure reading
            val currentPressure = touchPressure.value
            if (currentPressure > 0.01f) {
                touchPressureManager.calibrateWeight(knownWeight)
                _calibrationStep.value = CalibrationStep.COMPLETE
                _isCalibrating.value = false
                
                // Save calibration data
                viewModelScope.launch {
                    val (zeroOffset, _, weightFactor) = touchPressureManager.getCalibrationData()
                    preferencesManager.saveCalibration(zeroOffset, weightFactor)
                }
                
                _statusMessage.value = "Weight calibration completed! Scale is now calibrated."
                updateAccuracyStatus()
            } else {
                _statusMessage.value = "No pressure detected. Please ensure object is placed properly."
                _calibrationStep.value = CalibrationStep.ZERO_COMPLETE
            }
        }
    }

    fun cancelCalibration() {
        _isCalibrating.value = false
        _calibrationStep.value = CalibrationStep.NONE
        _statusMessage.value = "Calibration cancelled"
    }

    fun resetCalibration() {
        touchPressureManager.resetCalibration()
        viewModelScope.launch {
            preferencesManager.resetCalibration()
        }
        _calibrationStep.value = CalibrationStep.NONE
        _isCalibrating.value = false
        _statusMessage.value = "All calibration data cleared"
        updateAccuracyStatus()
    }

    fun toggleUnit() {
        val newUnit = !_unitIsGrams.value
        _unitIsGrams.value = newUnit
        viewModelScope.launch {
            preferencesManager.setUnit(newUnit)
        }
    }

    fun getDisplayWeight(): String {
        val weightValue = weight.value
        return if (_unitIsGrams.value) {
            when {
                weightValue < 1f -> "${String.format("%.2f", weightValue)} g"
                weightValue < 100f -> "${String.format("%.1f", weightValue)} g"
                else -> "${String.format("%.0f", weightValue)} g"
            }
        } else {
            val weightInOz = weightValue * 0.035274f
            when {
                weightInOz < 0.1f -> "${String.format("%.3f", weightInOz)} oz"
                weightInOz < 10f -> "${String.format("%.2f", weightInOz)} oz"
                else -> "${String.format("%.1f", weightInOz)} oz"
            }
        }
    }

    fun getDisplayPressure(): String {
        val pressure = touchPressure.value
        return when {
            pressure < 0.01f -> "0.00 hPa"
            pressure < 0.1f -> "${String.format("%.3f", pressure)} hPa"
            pressure < 1f -> "${String.format("%.2f", pressure)} hPa"
            else -> "${String.format("%.1f", pressure)} hPa"
        }
    }

    fun getAccuracyIndicator(): String {
        return when (_measurementAccuracy.value) {
            MeasurementAccuracy.UNCALIBRATED -> "⚠️ Uncalibrated"
            MeasurementAccuracy.ZERO_ONLY -> "📏 Basic Calibration"
            MeasurementAccuracy.FULLY_CALIBRATED -> "✅ Calibrated"
            MeasurementAccuracy.HIGH_PRECISION -> "🎯 High Precision"
        }
    }

    fun isReadyForMeasurement(): Boolean {
        return isScaleActive.value && isSensorAvailable.value
    }

    fun getCalibrationProgress(): Float {
        return when (_calibrationStep.value) {
            CalibrationStep.NONE -> 0f
            CalibrationStep.ZERO_PENDING -> 0.25f
            CalibrationStep.ZERO_COMPLETE -> 0.5f
            CalibrationStep.WEIGHT_PENDING -> 0.75f
            CalibrationStep.COMPLETE -> 1f
        }
    }

    override fun onCleared() {
        super.onCleared()
        touchPressureManager.stopListening()
    }
}
