package cl.ecocupon.app.ui.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.ecocupon.app.EcocuponApp
import cl.ecocupon.app.data.api.LeadApiService
import cl.ecocupon.app.data.bluetooth.Obd2Connection
import cl.ecocupon.app.data.model.LeadRequest
import cl.ecocupon.app.data.model.OBD2Reading
import cl.ecocupon.app.data.model.VehicleInfo
import cl.ecocupon.app.data.room.PendingLeadEntity
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "Obd2ViewModel"

sealed class UiState {
    object Idle : UiState()
    object Connecting : UiState()
    object Connected : UiState()
    object Scanning : UiState()
    data class Scanned(val codes: List<OBD2Reading>) : UiState()
    data class Error(val message: String) : UiState()
    object Sending : UiState()
    object Sent : UiState()
}

class Obd2ViewModel(application: Application) : AndroidViewModel(application) {

    private val api = LeadApiService.create()
    private val database = (application as EcocuponApp).database
    private val obd2 = Obd2Connection()
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    var uiState by mutableStateOf<UiState>(UiState.Idle)
    var vehicleInfo by mutableStateOf(VehicleInfo())
    var clientName by mutableStateOf("")
    var clientPhone by mutableStateOf("")
    var clientEmail by mutableStateOf("")
    var scannedCodes by mutableStateOf<List<OBD2Reading>>(emptyList())
    var pairedDevices by mutableStateOf<List<BluetoothDevice>>(emptyList())
    var pendingCount by mutableStateOf(0)

    init {
        viewModelScope.launch {
            database.pendingLeadDao().getPendingCount().collect { count ->
                pendingCount = count
            }
        }
    }

    fun loadPairedDevices(context: Context) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter

        if (adapter == null) {
            uiState = UiState.Error("Bluetooth no disponible")
            return
        }

        // Check permissions for Android 12+
        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            uiState = UiState.Error("Permiso de Bluetooth requerido")
            return
        }

        val devices = adapter.bondedDevices.toList()
        // Filter for devices likely to be OBD2 adapters
        pairedDevices = devices.filter {
            it.name.contains("OBD", ignoreCase = true) ||
            it.name.contains("ELM", ignoreCase = true) ||
            it.name.contains("VLINK", ignoreCase = true)
        }

        if (pairedDevices.isEmpty()) {
            // Show all paired devices if no OBD2-specific ones found
            pairedDevices = devices
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            uiState = UiState.Connecting

            val success = obd2.connect(device)

            if (success) {
                uiState = UiState.Connected
                // Initialize ELM327
                obd2.sendCommand("AT Z") // Reset
                delay(1000)
                obd2.sendCommand("AT E0") // Echo off
                obd2.sendCommand("AT L0") // Linefeeds off
                obd2.sendCommand("AT S0") // Spaces off
                obd2.sendCommand("AT H0") // Headers off
            } else {
                uiState = UiState.Error("No se pudo conectar al dispositivo OBD2")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            obd2.disconnect()
            uiState = UiState.Idle
            scannedCodes = emptyList()
        }
    }

    fun scanDTCs() {
        viewModelScope.launch {
            uiState = UiState.Scanning

            try {
                val rawCodes = obd2.readDTCs()

                scannedCodes = rawCodes.map { code ->
                    OBD2Reading.fromCode(code)
                }

                if (scannedCodes.isEmpty()) {
                    uiState = UiState.Scanned(emptyList())
                } else {
                    uiState = UiState.Scanned(scannedCodes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning DTCs: ${e.message}")
                uiState = UiState.Error("Error leyendo códigos DTC")
            }
        }
    }

    fun sendLead() {
        if (clientName.isBlank() || clientPhone.isBlank()) {
            uiState = UiState.Error("Nombre y teléfono son requeridos")
            return
        }

        viewModelScope.launch {
            uiState = UiState.Sending

            val lead = LeadRequest(
                nombre = clientName,
                telefono = clientPhone,
                email = clientEmail.takeIf { it.isNotBlank() },
                vehiculoMarca = vehicleInfo.brand,
                vehiculoModelo = vehicleInfo.model,
                vehiculoAnio = vehicleInfo.year.takeIf { it.isNotBlank() },
                vehiculoPatente = vehicleInfo.plate.takeIf { it.isNotBlank() },
                dtcCodes = scannedCodes.map { it.code },
                dtcRaw = scannedCodes.joinToString(", ") { "${it.code}: ${it.description}" },
                source = "app_obd2"
            )

            try {
                val response = api.createLead(lead)

                if (response.isSuccessful && response.body() != null) {
                    uiState = UiState.Sent
                    // Clear form
                    clearForm()
                } else {
                    // Save locally if API fails
                    saveLeadOffline(lead)
                    uiState = UiState.Error("Error del servidor. Guardado offline.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending lead: ${e.message}")
                // Save locally
                saveLeadOffline(lead)
                uiState = UiState.Error("Sin conexión. Lead guardado offline.")
            }
        }
    }

    fun sendPendingLeads() {
        viewModelScope.launch {
            val pendingLeads = database.pendingLeadDao().getAllPendingLeads().first()

            for (entity in pendingLeads) {
                try {
                    val lead = LeadRequest(
                        nombre = entity.nombre,
                        telefono = entity.telefono,
                        email = entity.email,
                        vehiculoMarca = entity.vehiculoMarca,
                        vehiculoModelo = entity.vehiculoModelo,
                        vehiculoAnio = entity.vehiculoAnio,
                        vehiculoPatente = entity.vehiculoPatente,
                        dtcCodes = entity.dtcCodes.split(", "),
                        dtcRaw = entity.dtcRaw,
                        source = entity.source
                    )

                    val response = api.createLead(lead)
                    if (response.isSuccessful && response.body()?.id != null) {
                        database.pendingLeadDao().deleteLead(entity)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending pending lead: ${e.message}")
                }
            }
        }
    }

    private suspend fun saveLeadOffline(lead: LeadRequest) {
        val entity = PendingLeadEntity(
            nombre = lead.nombre,
            telefono = lead.telefono,
            email = lead.email,
            vehiculoMarca = lead.vehiculoMarca,
            vehiculoModelo = lead.vehiculoModelo,
            vehiculoAnio = lead.vehiculoAnio,
            vehiculoPatente = lead.vehiculoPatente,
            dtcCodes = lead.dtcCodes.joinToString(", "),
            dtcRaw = lead.dtcRaw,
            source = lead.source
        )
        database.pendingLeadDao().insertLead(entity)
    }

    private fun clearForm() {
        clientName = ""
        clientPhone = ""
        clientEmail = ""
        vehicleInfo = VehicleInfo()
        scannedCodes = emptyList()
    }
}
