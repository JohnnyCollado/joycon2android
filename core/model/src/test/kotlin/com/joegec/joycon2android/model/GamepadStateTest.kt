package com.joegec.joycon2android.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GamepadStateTest {

    @Test
    fun `pro controller keeps its left stick unrotated`() {
        val gamepad = GamepadState.from(proController(stickX = 3000, stickY = 1000))

        assertEquals(3000, gamepad.leftStickX)
        assertEquals(1000, gamepad.leftStickY)
    }

    @Test
    fun `pro controller reports its own right stick`() {
        val gamepad = GamepadState.from(proController(rightStickX = 500, rightStickY = 3500))

        assertEquals(500, gamepad.rightStickX)
        assertEquals(3500, gamepad.rightStickY)
    }

    @Test
    fun `pro controller buttons are not remapped for sideways use`() {
        val pressed = setOf(JoyconButton.Up.id, JoyconButton.SlLeft.id)

        val gamepad = GamepadState.from(proController(pressed = pressed))

        assertEquals(pressed, gamepad.pressed)
    }

    @Test
    fun `single left joycon still rotates onto the gamepad orientation`() {
        val state = PlayerState(PlayerNumber.P1, left = joycon(Side.LEFT, JoyconInput(stickX = 3000, stickY = 1000)))

        val gamepad = GamepadState.from(state)

        assertEquals(SidewaysMapper.rotateStickLeft(3000, 1000).first, gamepad.leftStickX)
        assertEquals(SidewaysMapper.rotateStickLeft(3000, 1000).second, gamepad.leftStickY)
    }

    private fun proController(
        stickX: Int = 2048,
        stickY: Int = 2048,
        rightStickX: Int = 2048,
        rightStickY: Int = 2048,
        pressed: Set<String> = emptySet(),
    ) = PlayerState(
        player = PlayerNumber.P1,
        left = joycon(
            Side.PRO,
            JoyconInput(
                pressed = pressed,
                stickX = stickX,
                stickY = stickY,
                rightStickX = rightStickX,
                rightStickY = rightStickY,
            ),
        ),
    )

    private fun joycon(side: Side, input: JoyconInput) = ConnectedJoycon(
        address = "00:11:22:33:44:55",
        side = side,
        deviceName = "test",
        input = input,
        ready = true,
    )
}
