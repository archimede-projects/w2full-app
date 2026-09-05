package com.archimede.w2full.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.archimede.w2full.MainActivity
import com.archimede.w2full.R
import com.archimede.w2full.data.repository.PriceAlertRule
import java.util.Locale

interface PriceAlertNotifier {
    fun notificationsAllowed(): Boolean
    fun notify(rule: PriceAlertRule, candidates: List<PriceAlertCandidate>): Boolean
}

class AndroidPriceAlertNotifier(context: Context) : PriceAlertNotifier {
    private val applicationContext = context.applicationContext

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Avvisi W2Full quando i prezzi Eni scendono sotto la soglia scelta"
            },
        )
    }

    override fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun notify(rule: PriceAlertRule, candidates: List<PriceAlertCandidate>): Boolean {
        if (candidates.isEmpty() || !notificationsAllowed()) return false
        createChannel()
        val best = candidates.first()
        val service = if (rule.isSelf) "Self" else "Servito"
        val price = formatPrice(best.priceMilliEuroPerUnit)
        val content = if (candidates.size == 1) {
            buildString {
                append(best.stationName)
                append(" · ${rule.fuelDescription} $service · $price")
                best.distanceKm?.let { append(String.format(Locale.ITALY, " · %.1f km", it)) }
            }
        } else {
            "${candidates.size} stazioni sotto soglia · ${best.stationName} da $price"
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_price)
            .setContentTitle("Prezzo carburante sotto soglia")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun formatPrice(milliEuro: Long): String =
        String.format(Locale.ITALY, "%.3f €/L", milliEuro / 1000.0)

    companion object {
        const val CHANNEL_ID = "w2full_price_alerts"
        const val CHANNEL_NAME = "Avvisi prezzo"
        const val NOTIFICATION_ID = 6001
    }
}
