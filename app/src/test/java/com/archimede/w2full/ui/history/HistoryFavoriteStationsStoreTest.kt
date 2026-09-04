package com.archimede.w2full.ui.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryFavoriteStationsStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearPreferences()
    }

    @After
    fun tearDown() {
        clearPreferences()
    }

    @Test
    fun emptyStoreLoadsAsEmptySet() {
        assertTrue(SharedPreferencesHistoryFavoriteStationsStore(context).load().isEmpty())
    }

    @Test
    fun favoritesPersistAcrossStoreInstancesAndCanBeRemoved() {
        val store = SharedPreferencesHistoryFavoriteStationsStore(context)
        store.save(setOf(56865L, 62161L))

        val reloaded = SharedPreferencesHistoryFavoriteStationsStore(context)
        assertEquals(setOf(56865L, 62161L), reloaded.load())

        reloaded.save(toggledHistoryFavoriteStationIds(reloaded.load(), 56865L))
        assertEquals(
            setOf(62161L),
            SharedPreferencesHistoryFavoriteStationsStore(context).load(),
        )
    }

    private fun clearPreferences() {
        context.getSharedPreferences(HISTORY_FAVORITES_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
