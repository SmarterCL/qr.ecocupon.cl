package cl.ecocupon.app.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

private const val TAG = "Obd2Connection"
private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

class Obd2Connection {

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    var isConnected = false
        private set

    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))
            bluetoothSocket = socket

            // Cancel discovery to speed up connection
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()

            socket.connect()
            inputStream = socket.inputStream
            outputStream = socket.outputStream
            isConnected = true

            Log.d(TAG, "Connected to OBD2 device")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            isConnected = false
            false
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        } finally {
            isConnected = false
            inputStream = null
            outputStream = null
            bluetoothSocket = null
        }
    }

    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext ""

        try {
            // Send command
            outputStream?.write((command + "\r").toByteArray())
            outputStream?.flush()

            // Read response
            val buffer = ByteArray(256)
            val bytesRead = inputStream?.read(buffer) ?: 0

            if (bytesRead > 0) {
                String(buffer, 0, bytesRead).trim()
            } else {
                ""
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error sending command: ${e.message}")
            ""
        }
    }

    suspend fun readDTCs(): List<String> = withContext(Dispatchers.IO) {
        val codes = mutableListOf<String>()

        // Send 03 command to get stored DTCs
        val response = sendCommand("03")

        if (response.isNotEmpty()) {
            codes.addAll(parseDTCResponse(response))
        }

        // Also check pending DTCs with 07
        val pendingResponse = sendCommand("07")
        if (pendingResponse.isNotEmpty()) {
            codes.addAll(parseDTCResponse(pendingResponse))
        }

        codes.distinct()
    }

    private fun parseDTCResponse(response: String): List<String> {
        val codes = mutableListOf<String>()

        // Remove echoes and whitespace
        val cleanResponse = response
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")
            .replace("03", "")
            .replace(" ", "")
            .trim()

        // Each DTC is 4 bytes (8 hex chars) starting after the header
        if (cleanResponse.length >= 8) {
            var i = 0
            while (i + 8 <= cleanResponse.length) {
                val hexCode = cleanResponse.substring(i, i + 8)

                // Skip if it's just a search result echo or empty
                if (hexCode != "00000000" && hexCode.isNotBlank()) {
                    val dtc = convertHexToDTC(hexCode)
                    if (dtc.isNotBlank()) {
                        codes.add(dtc)
                    }
                }
                i += 8
            }
        }

        return codes
    }

    private fun convertHexToDTC(hex: String): String {
        if (hex.length < 4) return ""

        // First two hex chars determine the prefix
        val firstByte = hex.substring(0, 2).toInt(16)
        val prefix = when ((firstByte shr 2) and 0x03) {
            0 -> "P" // Powertrain
            1 -> "C" // Chassis
            2 -> "B" // Body
            3 -> "U" // Network
            else -> "P"
        }

        // Second nibble
        val secondDigit = (firstByte and 0x03).toString()

        // Remaining 3 digits
        val remaining = hex.substring(2, 5).toInt(16).toString().padStart(3, '0')

        return "$prefix$secondDigit$remaining"
    }

    suspend fun getVIN(): String = withContext(Dispatchers.IO) {
        sendCommand("09 02")
    }

    suspend fun getRPM(): String = withContext(Dispatchers.IO) {
        val response = sendCommand("01 0C")
        parseSimpleResponse(response)
    }

    private fun parseSimpleResponse(response: String): String {
        val cleanResponse = response
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")
            .replace(" ", "")
            .trim()

        // Remove the request echo (first 4 chars)
        if (cleanResponse.length > 4) {
            return cleanResponse.substring(4)
        }
        return ""
    }
}
