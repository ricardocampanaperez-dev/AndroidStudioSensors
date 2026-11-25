package com.example.androidstudio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.androidstudio.databinding.FragmentStudyModeBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class StudyModeFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentStudyModeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null

    private var isFaceDown = false
    private var isCovered = false
    private var isFocusActive = false

    private var startTimeMillis: Long = 0L
    private var activationJob: Job? = null
    private var timerJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            lightSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        activationJob?.cancel()
        if (isFocusActive) stopFocusMode(saveSession = true)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val z = event.values[2]
                isFaceDown = z < -8.5f
            }
            Sensor.TYPE_PROXIMITY -> {
                isCovered = event.values[0] < event.sensor.maximumRange
            }
            Sensor.TYPE_LIGHT -> {
                isCovered = event.values[0] < 10f
            }
        }
        updateStatusText()
        evaluateFocusState()
    }

    private fun updateStatusText() {
        if (!isFocusActive) {
            binding.tvSensorStatus.text =
                "Boca abajo: ${if (isFaceDown) "SÍ" else "NO"} | Pantalla tapada: ${if (isCovered) "SÍ" else "NO"}"
        }
    }

    private fun evaluateFocusState() {
        val shouldBeActive = isFaceDown && isCovered

        if (shouldBeActive && !isFocusActive && activationJob == null) {
            activationJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(1500L)
                startFocusMode()
            }
        } else if (!shouldBeActive) {
            activationJob?.cancel()
            activationJob = null
            if (isFocusActive) {
                stopFocusMode(saveSession = true)
            }
        }
    }

    private fun startFocusMode() {
        isFocusActive = true
        startTimeMillis = SystemClock.elapsedRealtime()

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.blackOverlayView.visibility = View.VISIBLE
        hideSystemBars()

        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val elapsedSec = (SystemClock.elapsedRealtime() - startTimeMillis) / 1000
                val hours = elapsedSec / 3600
                val minutes = (elapsedSec % 3600) / 60
                val seconds = elapsedSec % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                delay(1000L)
            }
        }
    }

    private fun stopFocusMode(saveSession: Boolean) {
        isFocusActive = false
        timerJob?.cancel()
        val durationSec = (SystemClock.elapsedRealtime() - startTimeMillis) / 1000

        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.blackOverlayView.visibility = View.GONE
        showSystemBars()

        if (saveSession && durationSec >= 5L) {
            Toast.makeText(requireContext(), "Sesión guardada: $durationSec seg", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(requireActivity().window, requireView()).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemBars() {
        WindowCompat.getInsetsController(requireActivity().window, requireView())
            .show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}