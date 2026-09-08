package com.joegec.joycon2android.gamepad.wirelessdebug

import kotlinx.coroutines.flow.StateFlow

/**
 * The app's privileged backend: an ADB connection to the device's own wireless-debugging
 * daemon. Surfaces pairing and connection state; the framework layer shows the pairing-code
 * prompt in response to [pairingServiceAvailable].
 */
interface WirelessDebugRepository {
    /** False below Android 11, where there is no wireless debugging to connect to. */
    val supported: Boolean

    val adbState: StateFlow<AdbState>
    val failure: StateFlow<AdbFailure?>
    val pairingServiceAvailable: StateFlow<Boolean>

    fun startDiscovery()
    fun stopDiscovery()
    fun startPairing()
    fun submitPairingCode(code: String)
}
