package com.joegec.joycon2android.gamepad.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbFailure

@Composable
fun adbFailureMessage(failure: AdbFailure?): String? = failure?.let {
    stringResource(
        when (it) {
            AdbFailure.PAIRING_WINDOW_CLOSED -> R.string.adb_failure_pairing_window_closed
            AdbFailure.PAIRING_REJECTED -> R.string.adb_failure_pairing_rejected
            AdbFailure.CONNECT_FAILED -> R.string.adb_failure_connect_failed
            AdbFailure.NOT_PAIRED -> R.string.adb_failure_not_paired
        },
    )
}
