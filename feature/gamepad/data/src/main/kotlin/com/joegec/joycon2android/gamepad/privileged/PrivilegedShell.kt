package com.joegec.joycon2android.gamepad.privileged

import java.io.InputStream
import java.io.OutputStream

/**
 * A source of shell-uid processes — the one privilege the UHID relay needs (to reach
 * `/dev/uhid`). Backed by an in-app ADB connection to the device's own wireless-debugging
 * daemon, so the relay layer is unaware of how the privilege was granted.
 */
interface PrivilegedShell {
    val isReady: Boolean
    fun newProcess(argv: Array<String>): ShellProcess?

    /** Runs [script] through the device shell, hiding the per-backend argv differences. */
    fun shell(script: String): ShellProcess?
}

interface ShellProcess {
    val outputStream: OutputStream
    val inputStream: InputStream
    fun waitFor()
    fun destroy()
}
