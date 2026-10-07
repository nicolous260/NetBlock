package com.example.netblock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.ServiceCompat
import java.io.FileInputStream

/**
 * Local "black hole" VPN.
 * Apps we want to BLOCK are routed into the tunnel and their packets are dropped.
 * Other apps are not routed at all, so they use the normal network.
 */
class BlockerVpnService : VpnService() {

    private var tunnel: ParcelFileDescriptor? = null
    private var drainThread: Thread? = null
    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopBlocking()
            return START_NOT_STICKY
        }
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
        startBlocking()
        return START_STICKY
    }

    private fun startBlocking() {
        stopTunnelOnly()

        val toBlock = Prefs.packagesToBlock(this)
        if (toBlock.isEmpty()) {
            stopBlocking()
            return
        }

        val builder = Builder()
            .setSession("NetBlock")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)        // all IPv4 of the blocked apps -> tunnel
            .addAddress("fd00::2", 128)
            .addRoute("::", 0)             // all IPv6 of the blocked apps -> tunnel
            .setBlocking(true)

        for (pkg in toBlock) {
            try {
                builder.addAllowedApplication(pkg) // route only these apps into VPN tunnel
            } catch (_: Exception) {
                // package uninstalled; ignore
            }
        }

        tunnel = builder.establish() ?: run { stopBlocking(); return }
        running = true
        Prefs.setBlocking(this, on = true)

        // Read and discard everything so the tunnel never fills up. No forwarding = no internet.
        val input = FileInputStream(tunnel!!.fileDescriptor)
        drainThread = Thread {
            val buf = ByteArray(32767)
            try {
                while (running) {
                    if (input.read(buf) < 0) break // blocks, so no busy loop and no battery drain
                }
            } catch (_: Exception) {
            }
        }.also { it.start() }
    }

    private fun stopTunnelOnly() {
        running = false
        try { tunnel?.close() } catch (_: Exception) {}
        tunnel = null
        drainThread?.interrupt()
        drainThread = null
    }

    private fun stopBlocking() {
        stopTunnelOnly()
        Prefs.setBlocking(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() { // user turned the VPN off from system settings
        stopBlocking()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnelOnly()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Blocking status",
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NetBlock is active")
            .setContentText("Selected apps have no internet")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.example.netblock.STOP"
        private const val CHANNEL_ID = "netblock_status"
        private const val NOTIF_ID = 1
    }
}
