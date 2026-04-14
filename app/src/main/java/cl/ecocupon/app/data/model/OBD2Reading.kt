package cl.ecocupon.app.data.model

data class OBD2Reading(
    val code: String,
    val description: String
) {
    companion object {
        fun fromCode(code: String): OBD2Reading {
            val description = DTC_DESCRIPTIONS[code] ?: "Código desconocido"
            return OBD2Reading(code, description)
        }
    }
}

val DTC_DESCRIPTIONS = mapOf(
    "P0100" to "Mass or Volume Air Flow Circuit Malfunction",
    "P0101" to "Mass or Volume Air Flow Circuit Range/Performance Problem",
    "P0102" to "Mass or Volume Air Flow Circuit Low Input",
    "P0103" to "Mass or Volume Air Flow Circuit High Input",
    "P0105" to "Manifold Absolute Pressure Circuit Malfunction",
    "P0106" to "Manifold Absolute Pressure Circuit Range/Performance Problem",
    "P0107" to "Manifold Absolute Pressure Circuit Low Input",
    "P0108" to "Manifold Absolute Pressure Circuit High Input",
    "P0110" to "Intake Air Temperature Circuit Malfunction",
    "P0115" to "Engine Coolant Temperature Circuit Malfunction",
    "P0116" to "Engine Coolant Temperature Circuit Range/Performance Problem",
    "P0117" to "Engine Coolant Temperature Circuit Low Input",
    "P0118" to "Engine Coolant Temperature Circuit High Input",
    "P0120" to "Throttle Pedal Position Sensor/Switch A Circuit Malfunction",
    "P0121" to "Throttle/Pedal Position Sensor/Switch A Circuit Range/Performance Problem",
    "P0122" to "Throttle/Pedal Position Sensor/Switch A Circuit Low Input",
    "P0123" to "Throttle/Pedal Position Sensor/Switch A Circuit High Input",
    "P0130" to "O2 Sensor Circuit Malfunction (Bank 1, Sensor 1)",
    "P0131" to "O2 Sensor Circuit Low Voltage (Bank 1, Sensor 1)",
    "P0132" to "O2 Sensor Circuit High Voltage (Bank 1, Sensor 1)",
    "P0133" to "O2 Sensor Circuit Slow Response (Bank 1, Sensor 1)",
    "P0135" to "O2 Sensor Heater Circuit Malfunction (Bank 1, Sensor 1)",
    "P0141" to "O2 Sensor Heater Circuit Malfunction (Bank 1, Sensor 2)",
    "P0171" to "System Too Lean (Bank 1)",
    "P0172" to "System Too Rich (Bank 1)",
    "P0174" to "System Too Lean (Bank 2)",
    "P0175" to "System Too Rich (Bank 2)",
    "P0300" to "Random/Multiple Cylinder Misfire Detected",
    "P0301" to "Cylinder 1 Misfire Detected",
    "P0302" to "Cylinder 2 Misfire Detected",
    "P0303" to "Cylinder 3 Misfire Detected",
    "P0304" to "Cylinder 4 Misfire Detected",
    "P0325" to "Knock Sensor Circuit Malfunction (Bank 1 or Single Sensor)",
    "P0335" to "Crankshaft Position Sensor A Circuit Malfunction",
    "P0340" to "Camshaft Position Sensor A Circuit Malfunction",
    "P0400" to "Exhaust Gas Recirculation Flow Malfunction",
    "P0401" to "Exhaust Gas Recirculation Flow Insufficient Detected",
    "P0420" to "Catalyst System Efficiency Below Threshold (Bank 1)",
    "P0430" to "Catalyst System Efficiency Below Threshold (Bank 2)",
    "P0440" to "Evaporative Emission Control System Malfunction",
    "P0442" to "Evaporative Emission Control System Leak Detected (small leak)",
    "P0446" to "Evaporative Emission Control System Vent Control Circuit Malfunction",
    "P0455" to "Evaporative Emission Control System Leak Detected (gross leak)",
    "P0500" to "Vehicle Speed Sensor A Malfunction",
    "P0505" to "Idle Control System Malfunction",
    "P0600" to "Serial Communication Link Malfunction",
    "P0603" to "Internal Control Module Keep Alive Memory (KAM) Error",
    "P0606" to "Internal Control Module Keep Alive Memory (KAM) Error",
    "P0700" to "Transmission Control System Malfunction",
    "P0705" to "Transmission Range Sensor Circuit Malfunction (PRNDL Input)",
    "P0715" to "Input/Turbine Speed Sensor Circuit Malfunction",
    "P0720" to "Output Speed Sensor Circuit Malfunction",
    "P0750" to "Shift Solenoid A Malfunction",
    "P0755" to "Shift Solenoid B Malfunction",
    "P1300" to "Ignition Circuit Malfunction",
    "P1400" to "DPFE Sensor Circuit Low Input",
    "P1500" to "Vehicle Speed Sensor Intermittent",
)
