package com.autoclicker.pro.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.autoclicker.pro.automation.AutoClickEngine
import com.autoclicker.pro.data.model.ExecutionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Owns the mandatory foreground notification (requirement #13) that must be
 * visible any time a sequence is actually running or paused. Fully driven by
 * [AutoClickEngine.status] — this service starts itself into the foreground
 * on RUNNING/PAUSED and stops itself once the engine reports IDLE/STOPPED, so
 * there is exactly one source of truth for "is automation active".
 */
@AndroidEntryPoint
class ClickForegroundService : Service() {

    @Inject lateinit var autoClickEngine: AutoClickEngine

    private var scope: CoroutineScope? = null

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ClickForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ClickForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        autoClickEngine.status.onEach { status ->
            when (status.state) {
                ExecutionState.RUNNING -> startForeground(
                    NotificationHelper.EXECUTION_NOTIF_ID,
                    NotificationHelper.buildExecutionNotification(this, isPaused = false, clickCount = status.clickCount)
                )
                ExecutionState.PAUSED -> startForeground(
                    NotificationHelper.EXECUTION_NOTIF_ID,
                    NotificationHelper.buildExecutionNotification(this, isPaused = true, clickCount = status.clickCount)
                )
                ExecutionState.IDLE, ExecutionState.STOPPED -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }.launchIn(newScope)
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
