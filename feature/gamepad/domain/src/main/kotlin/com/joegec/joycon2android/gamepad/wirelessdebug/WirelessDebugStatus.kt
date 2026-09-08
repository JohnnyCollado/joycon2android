package com.joegec.joycon2android.gamepad.wirelessdebug

data class WirelessDebugStatus(
    val supported: Boolean = false,
    val state: AdbState = AdbState.DISCONNECTED,
    val failure: AdbFailure? = null,
    val pairingServiceAvailable: Boolean = false,
)
