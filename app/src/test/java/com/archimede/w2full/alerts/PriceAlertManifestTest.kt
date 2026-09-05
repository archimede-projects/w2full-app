package com.archimede.w2full.alerts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PriceAlertManifestTest {
    @Test
    fun notificationPermissionIsDeclaredWithoutBackgroundLocationPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions.orEmpty().toSet()
        assertTrue(Manifest.permission.POST_NOTIFICATIONS in permissions)
        assertFalse(Manifest.permission.ACCESS_BACKGROUND_LOCATION in permissions)
    }
}
