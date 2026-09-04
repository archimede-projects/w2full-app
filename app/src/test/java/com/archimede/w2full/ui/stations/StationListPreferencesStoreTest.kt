package com.archimede.w2full.ui.stations

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StationListPreferencesStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("station_list_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("station_list_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun preferencesPersistRadiusAndSortMode() {
        val store = SharedPreferencesStationListPreferencesStore(context)
        store.save(
            StationListPreferences(
                radiusEnabled = true,
                radiusKm = 12,
                sortMode = StationSortMode.SELF_PRICE,
            ),
        )

        assertEquals(
            StationListPreferences(
                radiusEnabled = true,
                radiusKm = 12,
                sortMode = StationSortMode.SELF_PRICE,
            ),
            SharedPreferencesStationListPreferencesStore(context).load(),
        )
    }
}
