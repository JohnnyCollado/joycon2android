package com.joegec.joycon2android.gamepad.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.joegec.joycon2android.gamepad.GamepadFailure

@Composable
fun gamepadFailureMessage(failure: GamepadFailure?): String? = failure?.let {
    stringResource(
        when (it) {
            GamepadFailure.NO_PRIVILEGED_ACCESS -> R.string.gamepad_failure_no_privileged_access
            GamepadFailure.NO_CONTROLLERS_ASSIGNED -> R.string.gamepad_failure_no_controllers_assigned
            GamepadFailure.CREATE_FAILED -> R.string.gamepad_failure_create_failed
        },
    )
}
