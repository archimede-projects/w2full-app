package com.archimede.w2full

import android.app.Application
import com.archimede.w2full.data.local.W2FullDatabase
import com.archimede.w2full.data.repository.RefuelingRepository

class W2FullApplication : Application() {
    private val database by lazy { W2FullDatabase.getInstance(this) }

    val refuelingRepository: RefuelingRepository by lazy {
        RefuelingRepository(
            vehicleDao = database.vehicleDao(),
            rifornimentoDao = database.rifornimentoDao(),
        )
    }
}
