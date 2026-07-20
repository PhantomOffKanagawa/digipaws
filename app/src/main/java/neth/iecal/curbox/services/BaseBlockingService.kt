package neth.iecal.curbox.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import neth.iecal.curbox.R
import neth.iecal.curbox.utils.DataStoreManager
import neth.iecal.curbox.utils.ServiceProtectionManager
import kotlin.lazy

@SuppressLint("AccessibilityPolicy")
open class BaseBlockingService : AccessibilityService() {

    val dataStoreManager  by lazy {
        DataStoreManager(this)
    }

    private val protectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastHeartbeatMs = 0L

    var lastBackPressTimeStamp: Long =
        SystemClock.uptimeMillis() // prevents repetitive global actions

    override fun onServiceConnected() {
        super.onServiceConnected()
        startForegroundService()
        protectionScope.launch { setupProtectionOnConnect() }
    }

    private suspend fun setupProtectionOnConnect() {
        try {
            val config = dataStoreManager.settings.first().serviceProtectionConfig
            if (config.isEnabled) {
                ServiceWatchdogJob.schedule(this)
                ServiceProtectionManager.reinforceBackgroundExecution(this)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Writes the service's "alive" stamp and keeps the background execution exemptions fresh.
     * Throttled so it runs at most twice a minute.
     */
    private fun maybeHeartbeat() {
        val now = SystemClock.uptimeMillis()
        if (now - lastHeartbeatMs < 30_000L) return
        lastHeartbeatMs = now
        protectionScope.launch { runHeartbeat() }
    }

    private suspend fun runHeartbeat() {
        // Lands settings changes whose delay has run out, even when the UI is never opened
        try {
            dataStoreManager.applyDuePendingChanges()
        } catch (_: Exception) {
        }
        try {
            val config = dataStoreManager.settings.first().serviceProtectionConfig
            if (!config.isEnabled) return

            val nowMs = System.currentTimeMillis()
            dataStoreManager.updateServiceProtectionConfig {
                it.copy(appBlockerLastAliveMs = nowMs)
            }
        } catch (_: Exception) {
        }
    }

    private val serviceNotificationId by lazy { this.javaClass.simpleName.hashCode() }

    private fun startForegroundService() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Normal (status bar) channel.
        notificationManager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.blocking_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.blocking_service_channel_description) }
        )
        // Minimal channel used when the user chooses to hide the notification. IMPORTANCE_MIN
        // keeps it out of the status bar; the notification cannot be removed entirely because
        // Android requires a foreground service to show one.
        notificationManager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID_MIN,
                getString(R.string.blocking_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = getString(R.string.blocking_service_channel_description) }
        )

        // Start on the normal channel immediately so foreground is entered without waiting on
        // an async settings read, then move to the minimal channel if the user asked to hide it.
        val notification = buildServiceNotification(hide = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(serviceNotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(serviceNotificationId, notification)
        }

        protectionScope.launch { observeServiceNotificationVisibility() }
    }

    private fun buildServiceNotification(hide: Boolean): android.app.Notification {
        val className = this::class.simpleName
        return NotificationCompat.Builder(
            this,
            if (hide) SERVICE_CHANNEL_ID_MIN else SERVICE_CHANNEL_ID
        )
            .setContentTitle(getString(R.string.blocking_service_notification_title, className))
            .setContentText(getString(R.string.blocking_service_notification_text))
            .setSmallIcon(R.drawable.icon)
            .setPriority(if (hide) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private suspend fun observeServiceNotificationVisibility() {
        try {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            dataStoreManager.settings
                .map { it.hideServiceNotification }
                .distinctUntilChanged()
                .collect { hide ->
                    notificationManager.notify(serviceNotificationId, buildServiceNotification(hide))
                }
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "blocking_service_channel"
        private const val SERVICE_CHANNEL_ID_MIN = "blocking_service_channel_min"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        maybeHeartbeat()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            protectionScope.cancel()
        } catch (_: Exception) {
        }
    }

    override fun onInterrupt() {
    }


    fun isDelayOver( delay: Int): Boolean {
        val currentTime = SystemClock.uptimeMillis().toFloat()
        return currentTime - lastBackPressTimeStamp > delay
    }

    fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        lastBackPressTimeStamp = SystemClock.uptimeMillis()
    }

    fun pressBack() {
            performGlobalAction(GLOBAL_ACTION_BACK)
            lastBackPressTimeStamp = SystemClock.uptimeMillis()

    }
}
