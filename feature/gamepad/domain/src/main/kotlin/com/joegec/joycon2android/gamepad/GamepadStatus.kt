package com.joegec.joycon2android.gamepad

data class GamepadStatus(
    val enabled: Boolean = false,
    val failure: GamepadFailure? = null,
)
