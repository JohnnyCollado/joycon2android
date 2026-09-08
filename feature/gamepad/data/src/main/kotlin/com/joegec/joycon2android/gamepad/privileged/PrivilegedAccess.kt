package com.joegec.joycon2android.gamepad.privileged

import android.content.Context
import android.os.Build
import android.util.Log
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbEndpointDiscovery
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbFailure
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbLinkMonitor
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbShell
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbState
import com.joegec.joycon2android.gamepad.wirelessdebug.WirelessDebugRepository
import io.github.muntashirakon.adb.android.AdbMdns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reaches `/dev/uhid` and other apps' config files through an ADB connection to the device's
 * own wireless-debugging daemon.
 *
 * Pairing discovery only runs while the user asks for it, because it drives the code prompt and
 * the system pairing dialog withdraws its service the moment the app comes to the foreground.
 * Connect discovery runs silently so the link comes back on its own after a reboot.
 */
class PrivilegedAccess(
    private val context: Context,
    private val scope: CoroutineScope,
) : WirelessDebugRepository {

    // Building the ADB identity generates a 2048-bit RSA keypair the first time, which is far
    // too slow for the main thread. Deferring it puts that cost on the first pairing attempt,
    // which already runs on Dispatchers.IO.
    private val adb by lazy { AdbShell(context) }
    private val pairing = AdbEndpointDiscovery(context, AdbMdns.SERVICE_TYPE_TLS_PAIRING)
    private val connect = AdbEndpointDiscovery(context, AdbMdns.SERVICE_TYPE_TLS_CONNECT)
    private val linkMonitor = AdbLinkMonitor(scope) { adb.isReady }

    override val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private val _adbState = MutableStateFlow(AdbState.DISCONNECTED)
    override val adbState: StateFlow<AdbState> = _adbState.asStateFlow()

    private val _failure = MutableStateFlow<AdbFailure?>(null)
    override val failure: StateFlow<AdbFailure?> = _failure.asStateFlow()

    override val pairingServiceAvailable: StateFlow<Boolean> = pairing.endpoint
        .map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private var autoConnect: Job? = null

    /** The privileged shell, or null while the link is down. */
    fun acquire(): PrivilegedShell? = adb.takeIf { it.isReady }

    override fun startDiscovery() {
        if (!supported || autoConnect != null) return
        connect.start()
        autoConnect = scope.launch {
            connect.endpoint.filterNotNull().collect {
                if (_adbState.value == AdbState.DISCONNECTED) {
                    attempt(surfaceFailure = false) { if (connectNow()) null else AdbFailure.CONNECT_FAILED }
                }
            }
        }
    }

    override fun stopDiscovery() {
        autoConnect?.cancel()
        autoConnect = null
        connect.stop()
        linkMonitor.stop()
        pairing.stop()
    }

    override fun startPairing() {
        if (!supported) return
        startDiscovery()
        pairing.start()
    }

    override fun submitPairingCode(code: String) {
        val endpoint = pairing.endpoint.value
        pairing.stop()
        if (endpoint == null) {
            _failure.value = AdbFailure.PAIRING_WINDOW_CLOSED
            return
        }
        attempt(surfaceFailure = true) {
            when {
                !adb.pair(endpoint.host, endpoint.port, code) -> AdbFailure.PAIRING_REJECTED
                connectNow() -> null
                else -> AdbFailure.CONNECT_FAILED
            }
        }
    }

    private fun connectNow(): Boolean =
        connect.endpoint.value?.let { adb.connect(it.host, it.port) } == true

    private fun attempt(surfaceFailure: Boolean, block: () -> AdbFailure?) {
        if (_adbState.value == AdbState.WORKING) return
        _adbState.value = AdbState.WORKING
        if (surfaceFailure) _failure.value = null
        scope.launch {
            val outcome = runCatching { withContext(Dispatchers.IO) { block() } }.getOrElse {
                // Auto-connect retries on every mDNS tick and throws until the daemon trusts our
                // key, so only the user's own pairing submission should surface that as an error.
                Log.d(TAG, "adb attempt failed: ${it.message}")
                AdbFailure.NOT_PAIRED
            }
            val connected = outcome == null && adb.isReady
            _adbState.value = if (connected) AdbState.CONNECTED else AdbState.DISCONNECTED
            if (connected) {
                pairing.stop()
                linkMonitor.watch { _adbState.value = AdbState.DISCONNECTED }
            } else if (surfaceFailure) {
                _failure.value = outcome
            }
        }
    }

    private companion object {
        const val TAG = "PrivilegedAccess"
    }
}
