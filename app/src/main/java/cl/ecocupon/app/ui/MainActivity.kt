package cl.ecocupon.app.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.ecocupon.app.data.model.OBD2Reading
import cl.ecocupon.app.ui.viewmodel.Obd2ViewModel
import cl.ecocupon.app.ui.viewmodel.UiState

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    secondary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                    tertiary = androidx.compose.ui.graphics.Color(0xFF81C784)
                )
            ) {
                EcocuponApp()
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EcocuponApp(viewModel: Obd2ViewModel = viewModel()) {
    val context = LocalContext.current

    // Permission launcher
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            viewModel.loadPairedDevices(context)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ecocupon OBD2") },
                actions = {
                    if (viewModel.pendingCount > 0) {
                        IconButton(onClick = { viewModel.sendPendingLeads() }) {
                            Badge {
                                Text(viewModel.pendingCount.toString())
                            }
                            Icon(Icons.Default.Sync, "Send pending leads")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (viewModel.uiState) {
                is UiState.Idle -> ConnectionScreen(viewModel)
                is UiState.Connecting -> LoadingScreen("Conectando...")
                is UiState.Connected -> DataEntryScreen(viewModel)
                is UiState.Scanning -> LoadingScreen("Leyendo códigos DTC...")
                is UiState.Scanned -> ReviewScreen(viewModel)
                is UiState.Error -> ErrorScreen(viewModel)
                is UiState.Sending -> LoadingScreen("Enviando lead...")
                is UiState.Sent -> SuccessScreen(viewModel)
            }
        }
    }
}

@Composable
fun ConnectionScreen(viewModel: Obd2ViewModel) {
    val context = LocalContext.current
    var showDevices by remember { mutableStateOf(false) }
    var skipBluetooth by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadPairedDevices(context)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.BluetoothSearching,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ecocupon OBD2",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Conecta tu adaptador ELM327 o ingresa datos manualmente",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Manual mode button
        Button(
            onClick = { viewModel.uiState = UiState.Connected },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(Icons.Default.Edit, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ingreso manual")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bluetooth scan button
        OutlinedButton(
            onClick = { showDevices = true },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(Icons.Default.Bluetooth, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Escanear Bluetooth")
        }

        if (showDevices && viewModel.pairedDevices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Dispositivos vinculados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    viewModel.pairedDevices.forEach { device ->
                        OutlinedButton(
                            onClick = {
                                viewModel.connectToDevice(device)
                                showDevices = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(device.name ?: "Desconocido")
                        }
                    }
                }
            }
        }

        if (viewModel.pairedDevices.isEmpty() && showDevices) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No hay dispositivos vinculados. Usá 'Ingreso manual' para probar.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DataEntryScreen(viewModel: Obd2ViewModel) {
    var dtcInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Datos del cliente",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.clientName,
            onValueChange = { viewModel.clientName = it },
            label = { Text("Nombre *") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.clientPhone,
            onValueChange = { viewModel.clientPhone = it },
            label = { Text("Teléfono *") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.clientEmail,
            onValueChange = { viewModel.clientEmail = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Datos del vehículo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = viewModel.vehicleInfo.brand,
                onValueChange = { viewModel.vehicleInfo = viewModel.vehicleInfo.copy(brand = it) },
                label = { Text("Marca") },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = viewModel.vehicleInfo.model,
                onValueChange = { viewModel.vehicleInfo = viewModel.vehicleInfo.copy(model = it) },
                label = { Text("Modelo") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = viewModel.vehicleInfo.year,
                onValueChange = { viewModel.vehicleInfo = viewModel.vehicleInfo.copy(year = it) },
                label = { Text("Año") },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = viewModel.vehicleInfo.plate,
                onValueChange = { viewModel.vehicleInfo = viewModel.vehicleInfo.copy(plate = it) },
                label = { Text("Patente") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Códigos DTC (separados por coma)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dtcInput,
            onValueChange = { dtcInput = it },
            label = { Text("Ej: P0171, P0300, P0420") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val codes = dtcInput.split(",")
                    .map { it.trim() }
                    .filter { it.matches(Regex("[PCBU]\\d{4}")) }
                viewModel.scannedCodes = codes.map { OBD2Reading.fromCode(it) }
                viewModel.uiState = if (codes.isEmpty()) {
                    UiState.Scanned(emptyList())
                } else {
                    UiState.Scanned(viewModel.scannedCodes)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continuar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.uiState = UiState.Idle },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}

@Composable
fun ReviewScreen(viewModel: Obd2ViewModel) {
    val state = viewModel.uiState as? UiState.Scanned

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Códigos detectados: ${viewModel.scannedCodes.size}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.scannedCodes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "No se detectaron códigos de error",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.scannedCodes) { reading ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                reading.code,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                reading.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.sendLead() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Send, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enviar lead")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.scanDTCs() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reescanear")
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
fun ErrorScreen(viewModel: Obd2ViewModel) {
    val state = viewModel.uiState as? UiState.Error

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state?.message ?: "Error desconocido",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            if (viewModel.uiState is UiState.Error) {
                // Check if it's a connection error
                if (viewModel.uiState.toString().contains("conectar") || 
                    viewModel.uiState.toString().contains("Bluetooth")) {
                    viewModel.uiState = UiState.Idle
                } else {
                    viewModel.uiState = UiState.Connected
                }
            }
        }) {
            Text("Reintentar")
        }
    }
}

@Composable
fun SuccessScreen(viewModel: Obd2ViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "¡Lead enviado!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Los datos fueron enviados correctamente",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.uiState = UiState.Connected
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Nuevo escaneo")
        }
    }
}
