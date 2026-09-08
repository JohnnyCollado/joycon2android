package com.joegec.joycon2android.gamepad.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbState
import com.joegec.joycon2android.ui.components.ErrorBox
import com.joegec.joycon2android.ui.theme.Accent
import com.joegec.joycon2android.ui.theme.CardBg
import com.joegec.joycon2android.ui.theme.Dimens
import com.joegec.joycon2android.ui.theme.TextDim
import com.joegec.joycon2android.ui.theme.TextOnAccent

/**
 * Onboarding for the app's privileged backend: pairing with the device's own wireless-debugging
 * daemon. Once connected only the status line remains — the pairing persists in the system
 * Wireless debugging list, so there is nothing to undo here.
 */
@Composable
fun AdbSetupCard(
    state: AdbSetupState,
    onEnableNotifications: () -> Unit,
    onStartPairing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.buttonCorner))
            .background(CardBg)
            .padding(Dimens.cardPadding),
    ) {
        Header(state)

        when {
            !state.supported -> Body(stringResource(R.string.adb_unsupported))
            state.state == AdbState.CONNECTED -> Unit
            !state.notificationsGranted -> NotificationsGate(onEnableNotifications)
            else -> PairingGate(onStartPairing)
        }

        adbFailureMessage(state.failure)?.let { message ->
            Spacer(Modifier.height(Dimens.elementSpacing))
            ErrorBox(message)
        }
    }
}

@Composable
private fun Header(state: AdbSetupState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.adb_title),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(subtitleFor(state.state)),
                color = if (state.state == AdbState.CONNECTED) Accent else TextDim,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.state == AdbState.WORKING) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.progressIndicatorSmall),
                color = Accent,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun NotificationsGate(onEnableNotifications: () -> Unit) {
    Body(stringResource(R.string.adb_notifications_required))
    AccentButton(stringResource(R.string.adb_enable_notifications), onEnableNotifications)
}

@Composable
private fun PairingGate(onStartPairing: () -> Unit) {
    val context = LocalContext.current
    Body(stringResource(R.string.adb_instructions))
    AccentButton(stringResource(R.string.adb_pair_device)) {
        onStartPairing()
        WirelessDebuggingSettings.open(context)
    }
}

@Composable
private fun Body(text: String) {
    Spacer(Modifier.height(Dimens.elementSpacing))
    Text(text, color = TextDim, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AccentButton(label: String, onClick: () -> Unit) {
    Spacer(Modifier.height(Dimens.elementSpacing))
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
        shape = RoundedCornerShape(Dimens.buttonCorner),
        colors = ButtonDefaults.buttonColors(containerColor = Accent),
    ) {
        Text(label, color = TextOnAccent, style = MaterialTheme.typography.labelLarge)
    }
}

private fun subtitleFor(state: AdbState) = when (state) {
    AdbState.CONNECTED -> R.string.adb_subtitle_connected
    AdbState.WORKING -> R.string.adb_subtitle_working
    AdbState.DISCONNECTED -> R.string.adb_subtitle_disconnected
}
