package com.joegec.joycon2android.gamepad.presentation

import com.joegec.joycon2android.gamepad.wirelessdebug.AdbFailure
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbState

data class AdbSetupState(
    val supported: Boolean = false,
    val state: AdbState = AdbState.DISCONNECTED,
    val failure: AdbFailure? = null,
    val notificationsGranted: Boolean = true,
)
