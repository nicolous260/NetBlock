package com.example.netblock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Restores blocking after a reboot if it was switched on before. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.isBlocking(context)) return

        if (VpnService.prepare(context) != null) {
            // VPN permission is gone (another VPN app took over), so we can't restart silently.
            Prefs.setBlocking(context, false)
            return
        }
        context.startForegroundService(Intent(context, BlockerVpnService::class.java))
    }
}
