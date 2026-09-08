package com.joegec.joycon2android.gamepad

import com.joegec.joycon2android.gamepad.privileged.PrivilegedShell
import com.joegec.joycon2android.model.PlayerNumber
import com.joegec.joycon2android.model.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GamepadOutput(
    private val scope: CoroutineScope,
    private val gamepadManager: GamepadManager,
    private val acquireShell: () -> PrivilegedShell?,
) : GamepadRepository {

    private val _enabled = MutableStateFlow(false)
    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _failure = MutableStateFlow<GamepadFailure?>(null)
    override val failure: StateFlow<GamepadFailure?> = _failure.asStateFlow()

    private var shell: PrivilegedShell? = null

    private val playerStateFlows = PlayerNumber.entries.associateWith {
        MutableStateFlow(PlayerState(it))
    }

    override fun enable(players: List<PlayerState>) {
        if (_enabled.value) return
        val granted = acquireShell()
        if (granted == null) {
            _failure.value = GamepadFailure.NO_PRIVILEGED_ACCESS
            return
        }
        shell = granted
        scope.launch { startOutput(granted, players) }
    }

    override fun disable() {
        _enabled.value = false
        shell = null
        gamepadManager.destroyAll()
    }

    override fun clearFailure() {
        _failure.value = null
    }

    fun destroyAll() = gamepadManager.destroyAll()

    override fun push(players: List<PlayerState>) {
        if (!_enabled.value) return
        players.forEach { playerStateFlows[it.player]?.value = it }
    }

    override fun onPlayerAssigned(player: PlayerNumber) {
        if (!_enabled.value) return
        val active = shell ?: return
        scope.launch {
            if (gamepadManager.createGamepad(player, active)) {
                gamepadManager.startReporting(player, playerStateFlows.getValue(player))
            }
        }
    }

    override fun onPlayerUnassigned(player: PlayerNumber) {
        if (!_enabled.value) return
        gamepadManager.destroyGamepad(player)
        if (gamepadManager.activeCount == 0) disable()
    }

    private suspend fun startOutput(shell: PrivilegedShell, players: List<PlayerState>) {
        val active = players
        if (active.isEmpty()) {
            _failure.value = GamepadFailure.NO_CONTROLLERS_ASSIGNED
            return
        }

        var anyCreated = false
        for (playerState in active) {
            if (gamepadManager.createGamepad(playerState.player, shell)) {
                val flow = playerStateFlows.getValue(playerState.player)
                flow.value = playerState
                gamepadManager.startReporting(playerState.player, flow)
                anyCreated = true
            }
        }

        if (anyCreated) {
            _enabled.value = true
            _failure.value = null
        } else {
            _failure.value = GamepadFailure.CREATE_FAILED
        }
    }
}
