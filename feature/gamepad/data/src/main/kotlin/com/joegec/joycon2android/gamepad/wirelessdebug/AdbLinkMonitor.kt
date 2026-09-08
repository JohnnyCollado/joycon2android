package com.joegec.joycon2android.gamepad.wirelessdebug

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Catches the daemon revoking us while the app sits idle. Nothing is in flight to fail at that
 * point, and the local socket stays open, so the link has to be polled.
 */
class AdbLinkMonitor(private val scope: CoroutineScope, private val isLinkUp: () -> Boolean) {

    private var job: Job? = null

    fun watch(onLost: () -> Unit) {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (!isLinkUp()) {
                    onLost()
                    break
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
    }
}
