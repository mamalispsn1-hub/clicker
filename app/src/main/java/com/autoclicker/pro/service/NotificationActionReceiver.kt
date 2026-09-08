package com.autoclicker.pro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autoclicker.pro.automation.AutoClickEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Handles the Pause / Resume / Stop actions attached to the foreground
 * notification (requirement #12: emergency stop must also work from the
 * notification, not only the in-app or overlay buttons). */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var autoClickEngine: AutoClickEngine

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.ACTION_PAUSE -> autoClickEngine.pause()
            NotificationHelper.ACTION_RESUME -> autoClickEngine.resume()
            NotificationHelper.ACTION_STOP -> autoClickEngine.stop()
        }
        // Engine state change flows straight into ClickForegroundService's
        // collector, which refreshes/removes the notification accordingly.
        if (intent.action == NotificationHelper.ACTION_STOP) {
            ClickForegroundService.stop(context)
        }
    }
}
