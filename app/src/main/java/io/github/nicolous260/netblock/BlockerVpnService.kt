package io.github.nicolous260.netblock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.ServiceCompat
import java.io.FileInputStream
import java.io.IOException

/**
 * Local "black hole" VPN.
 * Apps we want to BLOCK are routed into the tunnel and their packets are dropped.
 * Other apps are not routed at all, so they use the normal network.
 */
class BlockerVpnService : VpnService() {

    private var tunnel: ParcelFileDescriptor? = null
    private var drainThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        running = true
    }

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

    /** (Re)builds the tunnel from the current block list. Safe to call while already running. */
    private fun startBlocking() {
        val builder = Builder()
            .setSession("NetBlock")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)        // all IPv4 of the blocked apps -> tunnel
            .addAddress("fd00::2", 128)
            .addRoute("::", 0)             // all IPv6 of the blocked apps -> tunnel
            .setBlocking(true)

        var routed = 0
        for (pkg in Prefs.blocked(this)) {
            try {
                builder.addAllowedApplication(pkg) // route only these apps into the tunnel
                routed++
            } catch (_: PackageManager.NameNotFoundException) {
                Prefs.setBlocked(this, pkg, false) // uninstalled: forget it
            }
        }

        // With no allowed apps Android routes EVERY app into the tunnel, which would cut the
        // whole phone off. If there is nothing valid to block, there is nothing to establish.
        if (routed == 0) {
            stopBlocking()
            return
        }

        // Establishing a new interface replaces the old one without a gap, so blocked apps
        // never get a moment of open internet while the list changes.
        val fresh = builder.establish()
        if (fresh == null) { // VPN permission was revoked
            stopBlocking()
            return
        }
        val oldTunnel = tunnel
        val oldThread = drainThread
        tunnel = fresh
        drainThread = startDrain(fresh)
        closeTunnel(oldTunnel, oldThread)
        Prefs.setBlocking(this, true)
    }

    /** Read and discard everything so the tunnel never fills up. No forwarding = no internet. */
    private fun startDrain(pfd: ParcelFileDescriptor): Thread {
        val input = FileInputStream(pfd.fileDescriptor)
        return Thread({
            val buf = ByteArray(32767)
            try {
                while (input.read(buf) >= 0) { /* drop */ } // blocks, so no busy loop
            } catch (_: IOException) {
                // tunnel closed
            }
        }, "netblock-drain").also {
            it.isDaemon = true
            it.start()
        }
    }

    private fun closeTunnel(pfd: ParcelFileDescriptor?, thread: Thread?) {
        try { pfd?.close() } catch (_: IOException) {}
        thread?.interrupt()
    }

    private fun stopBlocking() {
        closeTunnel(tunnel, drainThread)
        tunnel = null
        drainThread = null
        Prefs.setBlocking(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() { // user turned the VPN off from system settings
        stopBlocking()
        super.onRevoke()
    }

    override fun onDestroy() {
        closeTunnel(tunnel, drainThread)
        tunnel = null
        drainThread = null
        running = false
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
        const val ACTION_STOP = "io.github.nicolous260.netblock.STOP"
        private const val CHANNEL_ID = "netblock_status"
        private const val NOTIF_ID = 1

        /** True while the service exists in this process. Resets on force-stop or process death. */
        @Volatile
        var running = false
            private set
    }
}
