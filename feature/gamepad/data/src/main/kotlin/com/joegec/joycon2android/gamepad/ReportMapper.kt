package com.joegec.joycon2android.gamepad

import com.joegec.joycon2android.model.JoyconButton
import com.joegec.joycon2android.model.PlayerState

object ReportMapper {

    private const val REPORT_SIZE = 13

    // Bit N of the report is HID Button N+1, and for a Game Pad collection the kernel maps
    // Button N to BTN_SOUTH + (N-1) -- a Sega-style layout that spends slots 3 and 6 on BTN_C and
    // BTN_Z, between the face buttons and the shoulders. Assigning our buttons in their own order
    // would therefore land X on BTN_C (KEYCODE_BUTTON_C, which games ignore) and shift everything
    // after it, so each button is placed on the bit whose BTN_ code it should actually produce.
    //
    // Android's Generic.kl maps only 0x130-0x13e, so bit 15 (0x13f) is delivered to nothing. GR
    // takes that slot: it exists only on the Pro Controller, whereas Camera is on every Joy-Con 2.
    private val BUTTON_MAP: Map<String, Int> = mapOf(
        JoyconButton.A.id to 0,       // BTN_SOUTH  -> BUTTON_A
        JoyconButton.B.id to 1,       // BTN_EAST   -> BUTTON_B
        JoyconButton.Camera.id to 2,  // BTN_C      -> BUTTON_C
        JoyconButton.X.id to 3,       // BTN_NORTH  -> BUTTON_X
        JoyconButton.Y.id to 4,       // BTN_WEST   -> BUTTON_Y
        JoyconButton.GL.id to 5,      // BTN_Z      -> BUTTON_Z
        JoyconButton.L.id to 6,       // BTN_TL     -> BUTTON_L1
        JoyconButton.R.id to 7,       // BTN_TR     -> BUTTON_R1
        JoyconButton.ZL.id to 8,      // BTN_TL2    -> BUTTON_L2
        JoyconButton.ZR.id to 9,      // BTN_TR2    -> BUTTON_R2
        JoyconButton.Minus.id to 10,  // BTN_SELECT -> BUTTON_SELECT
        JoyconButton.Plus.id to 11,   // BTN_START  -> BUTTON_START
        JoyconButton.Home.id to 12,   // BTN_MODE   -> BUTTON_MODE
        JoyconButton.LS.id to 13,     // BTN_THUMBL -> BUTTON_THUMBL
        JoyconButton.RS.id to 14,     // BTN_THUMBR -> BUTTON_THUMBR
        JoyconButton.GR.id to 15,     // 0x13f      -> unmapped by Generic.kl
    )

    private const val HAT_CENTER = 0x0F

    fun buildReport(state: PlayerState): ByteArray {
        val gamepad = state.gamepad
        val report = ByteArray(REPORT_SIZE)
        val pressed = gamepad.pressed

        // Bytes 0-1: 16 button bits (little-endian)
        var buttons = 0
        for (name in pressed) {
            BUTTON_MAP[name]?.let { bit -> buttons = buttons or (1 shl bit) }
        }
        report[0] = (buttons and 0xFF).toByte()
        report[1] = ((buttons shr 8) and 0xFF).toByte()

        // Byte 2: hat switch in lower nibble, upper nibble is padding (zeros)
        report[2] = hatFromPressed(pressed).toByte()

        // Bytes 3-4: left stick X (16-bit signed LE)
        putInt16LE(report, 3, mapStick(gamepad.leftStickX))
        // Bytes 5-6: left stick Y (16-bit signed LE, inverted for HID convention)
        putInt16LE(report, 5, mapStick(4096 - gamepad.leftStickY))
        // Bytes 7-8: right stick X
        putInt16LE(report, 7, mapStick(gamepad.rightStickX))
        // Bytes 9-10: right stick Y (inverted)
        putInt16LE(report, 9, mapStick(4096 - gamepad.rightStickY))

        // Byte 11: left trigger (digital: 0 or 255)
        report[11] = if (JoyconButton.ZL.id in pressed) 0xFF.toByte() else 0x00
        // Byte 12: right trigger (digital: 0 or 255)
        report[12] = if (JoyconButton.ZR.id in pressed) 0xFF.toByte() else 0x00

        return report
    }

    // Map 0-4095 (center 2048) → -32767..32767
    private fun mapStick(value: Int): Int {
        return ((value - 2048).toLong() * 32767 / 2048).toInt().coerceIn(-32767, 32767)
    }

    private fun putInt16LE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    // Hat switch: 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW, 0x0F=center
    private fun hatFromPressed(pressed: Set<String>): Int {
        val up = JoyconButton.Up.id in pressed
        val down = JoyconButton.Down.id in pressed
        val left = JoyconButton.Left.id in pressed
        val right = JoyconButton.Right.id in pressed
        return when {
            up && right -> 1
            right && down -> 3
            down && left -> 5
            left && up -> 7
            up -> 0
            right -> 2
            down -> 4
            left -> 6
            else -> HAT_CENTER
        }
    }
}
