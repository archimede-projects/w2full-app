package com.archimede.w2full

import android.app.Application
import android.util.Log
import com.archimede.w2full.alerts.AndroidPriceAlertNotifier
import com.archimede.w2full.alerts.PriceAlertEvaluator
import com.archimede.w2full.data.local.W2FullDatabase
import com.archimede.w2full.data.mimit.EniStationDistanceService
import com.archimede.w2full.data.mimit.LogcatMimitLogger
import com.archimede.w2full.data.mimit.MimitCsvClient
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.data.mimit.RoomNearbyStationsRepository
import com.archimede.w2full.data.repository.PriceAlertRepository
import com.archimede.w2full.data.repository.PriceHistoryRepository
import com.archimede.w2full.data.repository.RefuelingRepository
import com.archimede.w2full.data.repository.RoomPriceHistoryRepository
import com.archimede.w2full.data.repository.RoomVehicleSettingsRepository
import com.archimede.w2full.data.repository.VehicleSettingsRepository
import com.archimede.w2full.location.FusedUserLocationProvider
import com.archimede.w2full.location.LastForegroundLocationStore
import com.archimede.w2full.location.SharedPreferencesLastForegroundLocationStore
import com.archimede.w2full.sync.MimitSyncScheduler
import com.archimede.w2full.ui.history.HistoryFavoriteStationsStore
import com.archimede.w2full.ui.history.HistoryPreferencesStore
import com.archimede.w2full.ui.history.SharedPreferencesHistoryFavoriteStationsStore
import com.archimede.w2full.ui.history.SharedPreferencesHistoryPreferencesStore
import com.archimede.w2full.ui.stations.SharedPreferencesStationListPreferencesStore
import com.archimede.w2full.ui.stations.StationListPreferencesStore

class W2FullApplication : Application() {
    private val database by lazy { W2FullDatabase.getInstance(this) }

    val refuelingRepository: RefuelingRepository by lazy {
        RefuelingRepository(
            vehicleDao = database.vehicleDao(),
            rifornimentoDao = database.rifornimentoDao(),
        )
    }

    val lastForegroundLocationStore: LastForegroundLocationStore by lazy {
        SharedPreferencesLastForegroundLocationStore(this)
    }

    val priceAlertRepository: PriceAlertRepository by lazy {
        PriceAlertRepository(
            alertDao = database.priceAlertDao(),
            vehicleDao = database.vehicleDao(),
            cacheDao = database.mimitCacheDao(),
        )
    }

    private val priceAlertNotifier: AndroidPriceAlertNotifier by lazy {
        AndroidPriceAlertNotifier(this)
    }

    val priceAlertEvaluator: PriceAlertEvaluator by lazy {
        PriceAlertEvaluator(
            repository = priceAlertRepository,
            cacheDao = database.mimitCacheDao(),
            locationStore = lastForegroundLocationStore,
            notifier = priceAlertNotifier,
        )
    }

    val nearbyStationsRepository: NearbyStationsRepository by lazy {
        RoomNearbyStationsRepository(
            database = database,
            cacheDao = database.mimitCacheDao(),
            vehicleDao = database.vehicleDao(),
            dataSource = MimitCsvClient(),
            distanceService = EniStationDistanceService(
                userLocationProvider = FusedUserLocationProvider(
                    context = this,
                    onLocationAvailable = { point ->
                        lastForegroundLocationStore.save(point, System.currentTimeMillis())
                    },
                ),
            ),
            logger = LogcatMimitLogger(),
            onRefreshSuccess = { priceAlertEvaluator.evaluate() },
        )
    }

    val stationListPreferencesStore: StationListPreferencesStore by lazy {
        SharedPreferencesStationListPreferencesStore(this)
    }

    val historyFavoriteStationsStore: HistoryFavoriteStationsStore by lazy {
        SharedPreferencesHistoryFavoriteStationsStore(this)
    }

    val historyPreferencesStore: HistoryPreferencesStore by lazy {
        SharedPreferencesHistoryPreferencesStore(this)
    }

    val priceHistoryRepository: PriceHistoryRepository by lazy {
        RoomPriceHistoryRepository(
            cacheDao = database.mimitCacheDao(),
            vehicleDao = database.vehicleDao(),
        )
    }

    val vehicleSettingsRepository: VehicleSettingsRepository by lazy {
        RoomVehicleSettingsRepository(
            vehicleDao = database.vehicleDao(),
            cacheDao = database.mimitCacheDao(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        priceAlertNotifier.createChannel()
        try {
            MimitSyncScheduler.schedule(this)
        } catch (exception: RuntimeException) {
            Log.e("W2Full-MIMIT", "Unable to schedule periodic MIMIT sync", exception)
        }
    }
}
