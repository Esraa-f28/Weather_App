package com.example.myapplication.ui.alert.view

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.DialogAlertSettingsBinding
import com.example.myapplication.databinding.FragmentAlertBinding
import com.example.myapplication.model.local.LocalDataSource
import com.example.myapplication.model.repo.Repository
import com.example.myapplication.ui.alert.AlarmBroadcastReceiver
import com.example.myapplication.ui.alert.viewmodel.AlertFactory
import com.example.myapplication.ui.alert.viewmodel.AlertViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.model.local.AlarmType
import com.example.myapplication.model.local.Alert
import com.example.myapplication.model.remote.RemoteDataSource

class AlertFragment : Fragment() {
    private var _binding: FragmentAlertBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding not initialized")
    private var _dialogBinding: DialogAlertSettingsBinding? = null
    private lateinit var viewModel: AlertViewModel
    private var fromTimeMillis: Long? = null
    private var toTimeMillis: Long? = null
    private val alarmReceiver = AlarmBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AlertFragment", "onCreate called")
        try {
            val filter = IntentFilter().apply {
                addAction(AlarmBroadcastReceiver.ACTION_START_ALARM)
                addAction(AlarmBroadcastReceiver.ACTION_STOP_ALARM)
            }
            requireContext().registerReceiver(alarmReceiver, filter)
            Log.d("AlertFragment", "Registered AlarmBroadcastReceiver in onCreate")
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error in onCreate: ${e.message}", e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("AlertFragment", "onCreateView called")
        try {
            _binding = FragmentAlertBinding.inflate(inflater, container, false)
            val factory = AlertFactory(
                Repository.getInstance(
                    RemoteDataSource(),
                    LocalDataSource.getInstance(requireContext())),
                requireContext()
            )
            viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]
            return binding.root
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error in onCreateView: ${e.message}", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AlertFragment", "onViewCreated called")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("AlertFragment", "Requesting POST_NOTIFICATIONS permission")
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
                }
            }
            // Delay setup to avoid UI thread overload
            lifecycleScope.launch(Dispatchers.Main) {
                delay(100)
                setupRecyclerView()
                setupAddButton()
                setupObservers()
            }
            // Check battery optimization status
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                    Log.w("AlertFragment", "Battery optimization enabled, alerts may not work reliably")
                    Toast.makeText(
                        requireContext(),
                        "Battery optimization is enabled. Please disable it in Settings for reliable alerts.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error in onViewCreated: ${e.message}", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            if (requestCode == 100) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("AlertFragment", "POST_NOTIFICATIONS permission granted")
                } else {
                    Log.w("AlertFragment", "POST_NOTIFICATIONS permission denied")
                    Toast.makeText(
                        requireContext(),
                        "Notification permission denied, alerts may not work",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error in onRequestPermissionsResult: ${e.message}", e)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("AlertFragment", "onStart called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("AlertFragment", "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlertFragment", "onDestroy called")
        try {
            requireContext().unregisterReceiver(alarmReceiver)
            Log.d("AlertFragment", "Unregistered AlarmBroadcastReceiver in onDestroy")
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error unregistering receiver: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AlertFragment", "onDestroyView called")
        _binding = null
        _dialogBinding = null
    }

    private fun setupRecyclerView() {
        try {
            binding.alertRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            Log.d("AlertFragment", "Setting up RecyclerView")
            binding.alertRecyclerView.adapter = AlertAdapter(
                onStopClick = { alertId ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            viewModel.stopAlert(alertId)
                            Log.d("AlertFragment", "Stopped alert: $alertId")
                        } catch (e: Exception) {
                            Log.e("AlertFragment", "Error stopping alert: ${e.message}", e)
                            Toast.makeText(context, "Failed to stop alert", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDeleteClick = { alertId ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            viewModel.deleteAlert(alertId)
                            Log.d("AlertFragment", "Deleted alert: $alertId")
                        } catch (e: Exception) {
                            Log.e("AlertFragment", "Error deleting alert: ${e.message}", e)
                            Toast.makeText(context, "Failed to delete alert", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error setting up RecyclerView: ${e.message}", e)
        }
    }

    private fun setupAddButton() {
        try {
            Log.d("AlertFragment", "Setting up add button")
            binding.addAlertButton.setOnClickListener {
                Log.d("AlertFragment", "Add alert button clicked")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w("AlertFragment", "Cannot show dialog: POST_NOTIFICATIONS permission not granted")
                    Toast.makeText(
                        requireContext(),
                        "Notification permission required for alerts",
                        Toast.LENGTH_SHORT
                    ).show()
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
                } else {
                    showAlertDialog()
                }
            }
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error setting up add button: ${e.message}", e)
        }
    }

    private fun setupObservers() {
        try {
            Log.d("AlertFragment", "Setting up observers")
            viewModel.alerts.observe(viewLifecycleOwner) { alerts: List<Alert> ->
                Log.d("AlertFragment", "Alerts updated: ${alerts.size} items")
                (binding.alertRecyclerView.adapter as? AlertAdapter)?.submitList(alerts)
            }
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error setting up observers: ${e.message}", e)
        }
    }

    private fun showAlertDialog() {
        try {
            Log.d("AlertFragment", "Showing alert dialog")
            val dialogBinding = DialogAlertSettingsBinding.inflate(layoutInflater)
            _dialogBinding = dialogBinding
            fromTimeMillis = null
            toTimeMillis = null

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .create()

            dialogBinding.fromContainer.setOnClickListener {
                Log.d("AlertFragment", "From time picker clicked")
                showDateTimePicker(true, dialogBinding)
            }

            dialogBinding.toContainer.setOnClickListener {
                Log.d("AlertFragment", "To time picker clicked")
                showDateTimePicker(false, dialogBinding)
            }

            dialogBinding.saveButton.setOnClickListener {
                Log.d("AlertFragment", "Save button clicked")
                if (fromTimeMillis == null || toTimeMillis == null) {
                    Toast.makeText(requireContext(), "Please select both From and To times", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (toTimeMillis!! <= fromTimeMillis!!) {
                    Toast.makeText(requireContext(), "To time must be after From time", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (toTimeMillis!! < System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), "End time cannot be in the past", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val alarmType = when (dialogBinding.alertTypeRadioGroup.checkedRadioButtonId) {
                    dialogBinding.notificationRadioButton.id -> AlarmType.NOTIFICATION
                    dialogBinding.defaultAlarmRadioButton.id -> AlarmType.DEFAULT_ALARM
                    else -> AlarmType.NOTIFICATION
                }
                Log.d("AlertFragment", "Saving alert with alarmType=$alarmType")
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val durationHours = TimeUnit.MILLISECONDS.toHours(toTimeMillis!! - fromTimeMillis!!).toInt()
                        viewModel.addAlert(durationHours, alarmType, fromTimeMillis!!, toTimeMillis!!)
                        Log.d("AlertFragment", "Alert saved successfully")
                    } catch (e: Exception) {
                        Log.e("AlertFragment", "Error saving alert: ${e.message}", e)
                        Toast.makeText(context, "Failed to save alert", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }

            dialogBinding.cancelButton.setOnClickListener {
                Log.d("AlertFragment", "Cancel button clicked")
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error showing alert dialog: ${e.message}", e)
        }
    }

    private fun showDateTimePicker(isFrom: Boolean, dialogBinding: DialogAlertSettingsBinding) {
        try {
            Log.d("AlertFragment", "Showing date-time picker, isFrom=$isFrom")
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    TimePickerDialog(
                        requireContext(),
                        { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                            if (isFrom) {
                                dialogBinding.fromDate.text = dateFormat.format(calendar.time)
                                dialogBinding.fromTime.text = timeFormat.format(calendar.time)
                                fromTimeMillis = calendar.timeInMillis
                            } else {
                                dialogBinding.toDate.text = dateFormat.format(calendar.time)
                                dialogBinding.toTime.text = timeFormat.format(calendar.time)
                                toTimeMillis = calendar.timeInMillis
                            }
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        } catch (e: Exception) {
            Log.e("AlertFragment", "Error showing date-time picker: ${e.message}", e)
        }
    }
}