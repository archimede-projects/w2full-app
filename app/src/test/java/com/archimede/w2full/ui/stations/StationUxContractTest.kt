package com.archimede.w2full.ui.stations

import org.junit.Assert.assertTrue
import org.junit.Test

class StationUxContractTest {
    @Test
    fun `favorite star touch target is at least 48 dp`() {
        assertTrue(FAVORITE_TOUCH_TARGET_DP >= 48)
    }
}
