package com.joegec.joycon2android.gamepad

/** Why the virtual gamepad isn't running, for presentation to phrase. */
enum class GamepadFailure {
    NO_PRIVILEGED_ACCESS,
    NO_CONTROLLERS_ASSIGNED,
    CREATE_FAILED,
}
