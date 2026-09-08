package com.joegec.joycon2android.gamepad.wirelessdebug

/** Why the wireless-debugging link isn't up, for presentation to phrase. */
enum class AdbFailure {
    PAIRING_WINDOW_CLOSED,
    PAIRING_REJECTED,
    CONNECT_FAILED,
    NOT_PAIRED,
}
