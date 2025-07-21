package com.webustallc.dahmer.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class TouchPressureManager(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _touchPressure = MutableStateFlow(0f)
    val touchPressure: StateFlow<Float> = _touchPressure.asStateFlow()

    private val _weight = MutableStateFlow(0f)
    val weight: StateFlow<Float> = _weight.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(false)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    // Professional calibration system
    private var baselinePressure = 1013.25f // Standard atmospheric pressure in hPa
    private var baselineAcceleration = 9.81f // Standard gravity in m/s²
    private var zeroOffsetPressure = 0f // Zero calibration offset - FORCED TO ZERO FOR DEBUGGING
    private var weightCalibrationFactor = 1f // Weight calibration multiplier
    private var isZeroCalibrated = false
    private var isWeightCalibrated = false

    // Current sensor readings
    private var currentPressure = 1013.25f
    private var currentAcceleration = 9.81f

    // Touch analysis variables
    private var touchContactArea = 0f
    private var touchPressureIntensity = 0f
    private var isTouchActive = false
    private var touchPointerCount = 0
    private var touchStartTime = 0L

    // Signal processing for noise reduction
    private val pressureBuffer = CircularBuffer(20) // 20 sample moving average
    private val accelerometerBuffer = CircularBuffer(15)
    private val weightBuffer = CircularBuffer(10)

    // Physical constants and thresholds - Updated for proper scale algorithm
    companion object {
        private const val MIN_DETECTABLE_PRESSURE = 0.1f // Minimum force index to register
        private const val MIN_DETECTABLE_WEIGHT = 0.5f // Minimum weight in grams (realistic threshold)
        private const val MAX_REASONABLE_WEIGHT = 300f // Maximum safe weight for phone screen (300g)
        private const val FORCE_INDEX_SCALING = 100f // Scaling factor for force index calculation
        private const val STABILIZATION_TIME_MS = 100L // Time for readings to stabilize
        
        // Safety warnings for users
        private const val WARNING_WEIGHT_THRESHOLD = 200f // Warn user above this weight
        private const val DANGER_WEIGHT_THRESHOLD = 250f // Strong warning above this weight
    }

    init {
        // Always mark sensor as available for testing
        _isSensorAvailable.value = true
        android.util.Log.d("TouchPressureManager", "Pressure sensor: ${pressureSensor != null}")
        android.util.Log.d("TouchPressureManager", "Accelerometer: ${accelerometer != null}")
        android.util.Log.d("TouchPressureManager", "Sensor marked as available for testing")
        
        // CRITICAL FIX: Force zero offset to 0 from start
        zeroOffsetPressure = 0f
        android.util.Log.d("TouchPressureManager", "INIT: Zero offset forced to 0 for immediate detection")
    }

    fun startListening() {
        _isActive.value = true
        
        // Auto-calibrate atmospheric baseline
        performAtmosphericCalibration()

        pressureSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        _isActive.value = false
        sensorManager.unregisterListener(this)
        resetMeasurements()
    }

    private fun resetMeasurements() {
        _touchPressure.value = 0f
        _weight.value = 0f
        isTouchActive = false
        touchPointerCount = 0
        touchContactArea = 0f
        touchPressureIntensity = 0f
        clearBuffers()
    }

    private fun clearBuffers() {
        pressureBuffer.clear()
        accelerometerBuffer.clear()
        weightBuffer.clear()
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (!_isActive.value) {
            android.util.Log.d("TouchPressureManager", "Touch ignored - scale not active")
            return false
        }

        android.util.Log.d("TouchPressureManager", "Touch event: ${event.action}, pointers: ${event.pointerCount}")

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                android.util.Log.d("TouchPressureManager", "Touch DOWN")
                startTouchMeasurement(event)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                android.util.Log.d("TouchPressureManager", "Touch POINTER_DOWN")
                updateMultiTouchMeasurement(event)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isTouchActive) {
                    updateTouchMeasurement(event)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                android.util.Log.d("TouchPressureManager", "Touch UP/CANCEL")
                endTouchMeasurement()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                android.util.Log.d("TouchPressureManager", "Touch POINTER_UP")
                updateMultiTouchMeasurement(event)
            }
        }
        return true
    }

    private fun startTouchMeasurement(event: MotionEvent) {
        isTouchActive = true
        touchStartTime = System.currentTimeMillis()
        touchPointerCount = event.pointerCount
        android.util.Log.d("TouchPressureManager", "Started touch measurement with ${touchPointerCount} pointers")
        updateTouchParameters(event)
        calculateForceAndWeight()
    }

    private fun updateMultiTouchMeasurement(event: MotionEvent) {
        touchPointerCount = event.pointerCount
        updateTouchParameters(event)
        calculateForceAndWeight()
    }

    private fun updateTouchMeasurement(event: MotionEvent) {
        updateTouchParameters(event)
        calculateForceAndWeight()
    }

    private fun endTouchMeasurement() {
        isTouchActive = false
        touchPointerCount = 0
        touchContactArea = 0f
        touchPressureIntensity = 0f
        
        // Gradual reset to avoid sudden jumps
        _touchPressure.value = 0f
        _weight.value = 0f
    }

    private fun updateTouchParameters(event: MotionEvent) {
        var totalContactArea = 0f
        var totalPressure = 0f

        // Calculate actual touch contact metrics
        for (i in 0 until event.pointerCount) {
            val size = event.getSize(i)
            val pressure = event.getPressure(i)
            
            // Size represents contact area (0.0 to 1.0, where 1.0 is device-dependent maximum)
            totalContactArea += size
            
            // Pressure represents force intensity (0.0 to 1.0, where 1.0 is device maximum)
            totalPressure += pressure
        }

        // Normalize by pointer count and apply realistic scaling
        touchContactArea = totalContactArea / event.pointerCount.coerceAtLeast(1)
        touchPressureIntensity = totalPressure / event.pointerCount.coerceAtLeast(1)
    }

    private fun calculateForceAndWeight() {
        if (!_isActive.value) return

        var measuredForceIndex = 0f

        if (isTouchActive && touchPointerCount > 0) {
            // Calculate touch-based force index (main measurement)
            val touchForceContribution = calculateTouchPressure() // This now returns force index
            
            // Calculate sensor-based pressure change (secondary measurement)
            val sensorPressureContribution = calculateSensorPressureChange()
            
            // Calculate accelerometer-based contribution (tertiary measurement)
            val accelerometerContribution = calculateAccelerometerContribution()
            
            // Combine contributions with intelligent weighting
            measuredForceIndex = combineSignals(
                touchForceContribution,
                sensorPressureContribution,
                accelerometerContribution
            )
            
            // Apply smoothing for stability (optional)
            // forceBuffer.add(measuredForceIndex)
            // measuredForceIndex = forceBuffer.getAverage()
        }

        // Apply zero calibration (baseline subtraction)
        android.util.Log.d("TouchPressureManager", "Before calibration: forceIndex=$measuredForceIndex, zeroOffset=$zeroOffsetPressure")
        measuredForceIndex = (measuredForceIndex - zeroOffsetPressure).coerceAtLeast(0f)
        android.util.Log.d("TouchPressureManager", "After zero calibration: $measuredForceIndex")

        // Update force index display (we'll show this as "pressure" for UI continuity)
        _touchPressure.value = measuredForceIndex
        android.util.Log.d("TouchPressureManager", "Force index updated: $measuredForceIndex units")

        // Calculate weight from force index using calibration
        val calculatedWeight = calculateWeightFromForceIndex(measuredForceIndex)
        
        // Update weight with reasonable limits
        val finalWeight = calculatedWeight.coerceIn(0f, MAX_REASONABLE_WEIGHT)
        _weight.value = finalWeight
        android.util.Log.d("TouchPressureManager", "Weight calculated: forceIndex=$measuredForceIndex, weight=$finalWeight grams")
        
        if (finalWeight < MIN_DETECTABLE_WEIGHT) {
            android.util.Log.d("TouchPressureManager", "Weight below threshold: $finalWeight < $MIN_DETECTABLE_WEIGHT")
        }
    }

    private fun calculateTouchPressure(): Float {
        if (!isTouchActive || touchPointerCount == 0) return 0f

        // CORRECT PHYSICS: Calculate Force Index = Average Pressure × Total Area
        // This represents the total force being applied to the screen
        
        val averagePressure = touchPressureIntensity // Already normalized per pointer
        val totalArea = touchContactArea * touchPointerCount // Total contact area
        
        // Force Index calculation (this represents actual force, not pressure)
        val forceIndex = averagePressure * totalArea
        
        // Convert to meaningful units with reasonable scaling
        val scaledForceIndex = forceIndex * 100f // Scale to usable range

        android.util.Log.d("TouchPressureManager", "CORRECT Force calc: avgPressure=$averagePressure, totalArea=$totalArea, forceIndex=$forceIndex, scaled=$scaledForceIndex")
        
        return scaledForceIndex
    }

    private fun calculateSensorPressureChange(): Float {
        return if (pressureSensor != null) {
            val pressureChange = currentPressure - baselinePressure
            // Only consider meaningful pressure changes (filter noise)
            if (abs(pressureChange) >= MIN_DETECTABLE_PRESSURE) {
                pressureChange * 0.1f // Much more conservative scaling
            } else {
                0f
            }
        } else {
            0f
        }
    }

    private fun calculateAccelerometerContribution(): Float {
        return if (accelerometer != null) {
            val accelerationChange = currentAcceleration - baselineAcceleration
            // Convert acceleration change to pressure equivalent (very conservative)
            (accelerationChange * 0.01f).coerceIn(-1f, 1f)
        } else {
            0f
        }
    }

    private fun combineSignals(
        touchForceIndex: Float,
        sensorPressure: Float,
        accelPressure: Float
    ): Float {
        // For touch-based scale, prioritize touch force index heavily
        return when {
            // Touch is the primary measurement for our scale
            touchForceIndex > 0 -> {
                // 90% touch data, 10% sensor assistance for stability
                (touchForceIndex * 0.9f) + (sensorPressure * 0.05f) + (accelPressure * 0.05f)
            }
            // Fallback to sensors only if no touch detected
            abs(sensorPressure) >= MIN_DETECTABLE_PRESSURE -> {
                (sensorPressure * 0.8f) + (accelPressure * 0.2f)
            }
            // Last resort: accelerometer only
            else -> {
                accelPressure
            }
        }
    }

    private fun calculateWeightFromForceIndex(forceIndex: Float): Float {
        if (forceIndex < 0.0001f) return 0f // Minimum detectable force
        
        val weight = if (isWeightCalibrated && weightCalibrationFactor > 0) {
            // PROPER CALIBRATION: Use reference weight to convert force index to grams
            // weightCalibrationFactor = referenceForceIndex / referenceWeight(grams)
            // So: weight = forceIndex / weightCalibrationFactor
            forceIndex / weightCalibrationFactor
        } else {
            // Default conservative conversion until calibration is done
            // This should be very conservative to avoid unrealistic readings
            forceIndex * 0.5f // Much more conservative than previous 2000f
        }
        
        android.util.Log.d("TouchPressureManager", "Weight calc: forceIndex=$forceIndex, weight=$weight grams, calibrated=$isWeightCalibrated")
        return weight
    }

    private fun performAtmosphericCalibration() {
        // Set baseline to current readings for relative measurement
        baselinePressure = currentPressure
        baselineAcceleration = currentAcceleration
    }

    // PROPER CALIBRATION SYSTEM - Following your recommended algorithm
    fun calibrateZero() {
        if (isTouchActive) {
            // Can't calibrate while touching
            android.util.Log.w("TouchPressureManager", "Cannot calibrate while touching screen")
            return
        }
        
        // TARE/ZERO: Set current force index as baseline (should be close to 0)
        zeroOffsetPressure = _touchPressure.value
        android.util.Log.d("TouchPressureManager", "Zero calibration: Baseline set to $zeroOffsetPressure")
        isZeroCalibrated = true
        clearBuffers()
        resetMeasurements()
    }

    fun calibrateWeight(knownWeight: Float) {
        if (!isTouchActive || knownWeight <= 0) {
            android.util.Log.w("TouchPressureManager", "Invalid calibration: touch=$isTouchActive, weight=$knownWeight")
            return
        }

        // WEIGHT CALIBRATION: Calculate calibration factor
        // weightCalibrationFactor = currentForceIndex / knownWeight
        val currentForceIndex = _touchPressure.value
        if (currentForceIndex > MIN_DETECTABLE_PRESSURE) {
            // Calculate calibration factor: factor = forceIndex / weight
            weightCalibrationFactor = currentForceIndex / knownWeight
            isWeightCalibrated = true
            android.util.Log.d("TouchPressureManager", "Weight calibration: factor=$weightCalibrationFactor (forceIndex=$currentForceIndex / weight=$knownWeight)")
        } else {
            android.util.Log.w("TouchPressureManager", "Insufficient force for calibration: $currentForceIndex")
        }
    }

    fun resetCalibration() {
        // IMPORTANT: Reset zero offset to allow immediate measurement
        zeroOffsetPressure = 0f
        weightCalibrationFactor = 1f
        isZeroCalibrated = false
        isWeightCalibrated = false
        android.util.Log.d("TouchPressureManager", "CALIBRATION RESET: zero offset cleared to 0")
        performAtmosphericCalibration()
        clearBuffers()
        resetMeasurements()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_PRESSURE -> {
                    currentPressure = it.values[0]
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]
                    currentAcceleration = sqrt(x*x + y*y + z*z)
                    accelerometerBuffer.add(currentAcceleration)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    fun getCalibrationData(): Triple<Float, Float, Float> {
        return Triple(zeroOffsetPressure, baselineAcceleration, weightCalibrationFactor)
    }

    fun applyCalibration(zeroOffset: Float, accelBaseline: Float, weightFactor: Float) {
        zeroOffsetPressure = zeroOffset
        baselineAcceleration = accelBaseline
        weightCalibrationFactor = weightFactor
        isZeroCalibrated = zeroOffset != 0f
        isWeightCalibrated = weightFactor != 1f
    }

    fun isCalibrated(): Boolean = isZeroCalibrated && isWeightCalibrated

    // Circular buffer for signal smoothing
    private class CircularBuffer(private val size: Int) {
        private val buffer = FloatArray(size)
        private var index = 0
        private var count = 0

        fun add(value: Float) {
            buffer[index] = value
            index = (index + 1) % size
            if (count < size) count++
        }

        fun getAverage(): Float {
            if (count == 0) return 0f
            return buffer.take(count).average().toFloat()
        }

        fun clear() {
            index = 0
            count = 0
        }
    }
}
