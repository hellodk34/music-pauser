package com.musicpauser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {
            if (Prefs.isOverlayEnabled(context)) {
                try {
                    OverlayService.start(context, autoPlay = true)
                } catch (_: Exception) {
                }
            }
        }
    }
}
