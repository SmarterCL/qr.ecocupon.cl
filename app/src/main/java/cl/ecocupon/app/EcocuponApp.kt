package cl.ecocupon.app

import android.app.Application
import cl.ecocupon.app.data.room.AppDatabase

class EcocuponApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

}
