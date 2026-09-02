package com.archimede.w2full

import android.app.Application
import com.archimede.w2full.data.local.W2FullDatabase
import com.archimede.w2full.data.mimit.EniStationDistanceService
import com.archimede.w2full.data.mimit.MimitCsvClient
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.data.mimit.SessionNearbyStationsRepository
import com.archimede.w2full.data.repository.RefuelingRepository
import com.archimede.w2full.location.FusedUserLocationProvider

class W2FullApplication : Application() {
    private val database by lazy { W2FullDatabase.getInstance(this) }

    val refuelingRepository: RefuelingRepository by lazy {
        RefuelingRepository(
            vehicleDao = database.vehicleDao(),
            rifornimentoDao = database.rifornimentoDao(),
        )
    }

    val nearbyStationsRepository: NearbyStationsRepository by lazy {
        SessionNearbyStationsRepository(
            stationSource = MimitCsvClient(),
            distanceService = EniStationDistanceService(
                userLocationProvider = FusedUserLocationProvider(this),
            ),
        )
    }
}
