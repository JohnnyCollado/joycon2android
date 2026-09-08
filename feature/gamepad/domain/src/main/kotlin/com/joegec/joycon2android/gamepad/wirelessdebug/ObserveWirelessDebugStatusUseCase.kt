package com.joegec.joycon2android.gamepad.wirelessdebug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveWirelessDebugStatusUseCase(private val repository: WirelessDebugRepository) {
    operator fun invoke(): Flow<WirelessDebugStatus> = combine(
        repository.adbState,
        repository.failure,
        repository.pairingServiceAvailable,
    ) { state, failure, pairingAvailable ->
        WirelessDebugStatus(repository.supported, state, failure, pairingAvailable)
    }
}
