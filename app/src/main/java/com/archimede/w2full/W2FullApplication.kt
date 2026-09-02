package com.archimede.w2full

import android.app.Application
import android.util.Log
import com.archimede.w2full.data.local.W2FullDatabase
import com.archimede.w2full.data.mimit.EniStationDistanceService
import com.archimede.w2full.data.mimit.LogcatMimitLogger
import com.archimede.w2full.data.mimit.MimitCsvClient
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.data.mimit.RoomNearbyStationsRepository
import com.archimede.w2full.data.repository.RefuelingRepository
import com.archimede.w2full.location.FusedUserLocationProvider
import com.archimede.w2full.sync.MimitSyncScheduler

class W2FullApplication : Application() {
    private val database by lazy { W2FullDatabase.getInstance(this) }

    val refuelingRepository: RefuelingRepository by lazy {
        RefuelingRepository(
            vehicleDao = database.vehicleDao(),
            rifornimentoDao = database.rifornimentoDao(),
        )
    }

    val nearbyStationsRepository: NearbyStationsRepository by lazy {
        RoomNearbyStationsRepository(
            database = database,
            cacheDao = database.mimitCacheDao(),
            vehicleDao = database.vehicleDao(),
            dataSource = MimitCsvClient(),
            distanceService = EniStationDistanceService(
                userLocationProvider = FusedUserLocationProvider(this),
            ),
            logger = LogcatMimitLogger(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        try {
            MimitSyncScheduler.schedule(this)
        } catch (exception: RuntimeException) {
            Log.e("W2Full-MIMIT", "Unable to schedule periodic MIMIT sync", exception)
        }
    }
}
