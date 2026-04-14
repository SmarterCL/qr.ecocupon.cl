package cl.ecocupon.app.data.model

import com.google.gson.annotations.SerializedName

data class LeadRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("email") val email: String?,
    @SerializedName("vehiculo_marca") val vehiculoMarca: String,
    @SerializedName("vehiculo_modelo") val vehiculoModelo: String,
    @SerializedName("vehiculo_anio") val vehiculoAnio: String?,
    @SerializedName("vehiculo_patente") val vehiculoPatente: String?,
    @SerializedName("dtc_codes") val dtcCodes: List<String>,
    @SerializedName("dtc_raw") val dtcRaw: String?,
    @SerializedName("source") val source: String = "app_obd2"
)

data class LeadResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("timestamp") val timestamp: String?
)
