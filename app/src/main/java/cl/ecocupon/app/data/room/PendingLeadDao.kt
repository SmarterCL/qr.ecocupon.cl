package cl.ecocupon.app.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingLeadDao {

    @Query("SELECT * FROM pending_leads ORDER BY createdAt ASC")
    fun getAllPendingLeads(): Flow<List<PendingLeadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: PendingLeadEntity): Long

    @Delete
    suspend fun deleteLead(lead: PendingLeadEntity)

    @Query("SELECT COUNT(*) FROM pending_leads")
    fun getPendingCount(): Flow<Int>
}
