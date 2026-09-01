package com.archimede.w2full

import org.junit.Assert.assertEquals
import org.junit.Test

class W2FullAppInfoTest {
    @Test
    fun appIdentityIsStable() {
        assertEquals("W2Full", W2FullAppInfo.name)
        assertEquals("com.archimede.w2full", W2FullAppInfo.applicationId)
    }
}
