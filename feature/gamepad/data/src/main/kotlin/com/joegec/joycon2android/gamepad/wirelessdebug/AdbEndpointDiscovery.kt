package com.joegec.joycon2android.gamepad.wirelessdebug

import android.content.Context
import io.github.muntashirakon.adb.android.AdbMdns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watches mDNS for one of adbd's two advertised services and reports where it lives.
 *
 * Always connect to the address mDNS advertises rather than loopback: the daemon binds the
 * Wi-Fi interface, and Google has discussed refusing localhost connections outright.
 */
class AdbEndpointDiscovery(private val context: Context, private val serviceType: String) {

    private val _endpoint = MutableStateFlow<AdbEndpoint?>(null)
    val endpoint: StateFlow<AdbEndpoint?> = _endpoint.asStateFlow()

    private var mdns: AdbMdns? = null

    fun start() {
        if (mdns != null) return
        mdns = AdbMdns(context, serviceType) { host, port ->
            val address = host?.hostAddress
            _endpoint.value = if (address != null && port > 0) AdbEndpoint(address, port) else null
        }.apply { start() }
    }

    fun stop() {
        mdns?.stop()
        mdns = null
        _endpoint.value = null
    }
}
