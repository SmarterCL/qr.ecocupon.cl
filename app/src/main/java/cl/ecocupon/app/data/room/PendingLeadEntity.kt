package cl.ecocupon.app.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_leads")
data class PendingLeadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val telefono: String,
    val email: String?,
    val vehiculoMarca: String,
    val vehiculoModelo: String,
    val vehiculoAnio: String?,
    val vehiculoPatente: String?,
    val dtcCodes: String, // JSON string
    val dtcRaw: String?,
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)
