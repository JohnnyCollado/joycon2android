package com.joegec.joycon2android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.joegec.joycon2android.buttonmapping.Console
import com.joegec.joycon2android.buttonmapping.JoyconSide
import com.joegec.joycon2android.buttonmapping.presentation.ControllerMappingScreen
import com.joegec.joycon2android.buttonmapping.presentation.ControllerMappingViewModel
import com.joegec.joycon2android.dsu.presentation.DsuViewModel
import com.joegec.joycon2android.gamepad.emulator.EdenGamepadConfig
import com.joegec.joycon2android.gamepad.presentation.AdbSetupState
import com.joegec.joycon2android.gamepad.presentation.GamepadViewModel
import com.joegec.joycon2android.gamepad.presentation.gamepadFailureMessage
import com.joegec.joycon2android.gamepad.wirelessdebug.AdbState
import com.joegec.joycon2android.ui.Joycon2ViewModel
import com.joegec.joycon2android.ui.JoyconScreen
import com.joegec.joycon2android.dsu.presentation.DsuCardState
import com.joegec.joycon2android.ui.theme.Background
import com.joegec.joycon2android.ui.theme.Joycon2AndroidTheme

class MainActivity : ComponentActivity() {

    private val viewModel: Joycon2ViewModel by viewModels()
    private val dsuViewModel: DsuViewModel by viewModels {
        viewModelFactory {
            initializer {
                val c = (application as JoyconApplication).container
                DsuViewModel(
                    c.observeDsuStatus,
                    c.enableDsu,
                    c.disableDsu,
                    dolphinInstalled = c.emulatorSetup.dolphinInstalled,
                    configureDolphin = c.emulatorSetup::configureDolphinDsu,
                )
            }
        }
    }
    private val gamepadViewModel: GamepadViewModel by viewModels {
        viewModelFactory {
            initializer {
                val c = (application as JoyconApplication).container
                GamepadViewModel(
                    c.observeGamepadStatus,
                    c.observeWirelessDebugStatus,
                    c.startPairing,
                    c.enableGamepad,
                    c.disableGamepad,
                    gamepadEmulators = c.emulatorSetup.gamepadEmulators(),
                    configureGamepad = c.emulatorSetup::configureGamepad,
                )
            }
        }
    }
    private val controllerMappingViewModel: ControllerMappingViewModel by viewModels {
        viewModelFactory {
            initializer {
                val c = (application as JoyconApplication).container
                ControllerMappingViewModel(c.observeControllerMapping, c.setControllerMapping, c.resetControllerMapping)
            }
        }
    }

    private val notificationsGranted = mutableStateOf(true)
    private var notificationAsked = false

    override fun onResume() {
        super.onResume()
        viewModel.recheckPermissions()
        notificationsGranted.value = hasNotificationPermission()
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        // The UI is always dark, so force light bar icons rather than letting them follow the
        // device's light/dark mode (which would render dark-on-dark on a light-mode device).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val permissionHandler = viewModel.permissionHandler

        val permLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            if (grants.values.all { it }) {
                viewModel.onPermissionsGranted()
                viewModel.startScan()
            } else {
                viewModel.onPermissionsDenied()
            }
        }

        // Pairing prompts for its code via a notification, so that permission gates pairing
        val notificationPermLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> notificationsGranted.value = granted }

        notificationsGranted.value = hasNotificationPermission()

        // Only asked once the user opts into pairing, never on launch. A second refusal is
        // permanent, so send them to the app's notification settings instead of a dead prompt.
        val enableNotifications = {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (notificationAsked && !shouldShowRequestPermissionRationale(permission)) {
                    startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
                    )
                } else {
                    notificationAsked = true
                    notificationPermLauncher.launch(permission)
                }
            }
        }

        setContent {
            Joycon2AndroidTheme {
                Surface(Modifier.fillMaxSize(), color = Background) {
                    var mappingConsole by remember { mutableStateOf<Console?>(null) }
                    val console = mappingConsole

                    if (console != null) {
                        val leftMapping by controllerMappingViewModel.mapping(console, JoyconSide.LEFT).collectAsState()
                        val rightMapping by controllerMappingViewModel.mapping(console, JoyconSide.RIGHT).collectAsState()
                        val dualMapping by controllerMappingViewModel.mapping(console, JoyconSide.DUAL).collectAsState()

                        ControllerMappingScreen(
                            console = console,
                            leftMapping = leftMapping,
                            rightMapping = rightMapping,
                            dualMapping = dualMapping,
                            onSetMapping = { side, targetKey, sourceId ->
                                controllerMappingViewModel.setMapping(console, side, targetKey, sourceId)
                            },
                            onResetMapping = { side -> controllerMappingViewModel.resetMapping(console, side) },
                            onBack = { mappingConsole = null },
                        )
                        return@Surface
                    }

                    val state by viewModel.uiState.collectAsState()
                    val gamepadStatus by gamepadViewModel.status.collectAsState()
                    val wirelessDebug by gamepadViewModel.wirelessDebug.collectAsState()
                    val dsuStatus by dsuViewModel.status.collectAsState()
                    val dolphinPhase by dsuViewModel.dolphinPhase.collectAsState()
                    val gamepadSetupPhase by gamepadViewModel.setupPhase.collectAsState()
                    val selectedEmulator by gamepadViewModel.selectedEmulator.collectAsState()
                    val permissionDenied by viewModel.permissionDenied.collectAsState()
                    val viewMode by viewModel.viewMode.collectAsState()
                    val privilegedAccess = wirelessDebug.state == AdbState.CONNECTED

                    // A written emulator config is keyed to the current assignment; once it changes,
                    // the Done/Failed state is stale, so reset both setup buttons.
                    val assignmentKey = state.players.map {
                        Triple(it.player.index, it.left?.address, it.right?.address)
                    }
                    LaunchedEffect(assignmentKey) {
                        dsuViewModel.resetDolphinPhase()
                        gamepadViewModel.resetSetupPhase()
                    }

                    JoyconScreen(
                        state = state,
                        gamepadEnabled = gamepadStatus.enabled,
                        gamepadError = gamepadFailureMessage(gamepadStatus.failure),
                        dsuState = DsuCardState(
                            enabled = dsuStatus.enabled,
                            error = dsuStatus.error,
                            clientCount = dsuStatus.clientCount,
                            address = dsuStatus.address,
                            showSlotLimitNote = state.activePlayers.any { it.player.index > 4 },
                            dolphinInstalled = dsuViewModel.dolphinInstalled,
                            dolphinAutoConfigAvailable = privilegedAccess,
                            dolphinPhase = dolphinPhase,
                        ),
                        adbSetup = AdbSetupState(
                            supported = wirelessDebug.supported,
                            state = wirelessDebug.state,
                            failure = wirelessDebug.failure,
                            notificationsGranted = notificationsGranted.value,
                        ),
                        permissionDenied = permissionDenied,
                        onScan = { permLauncher.launch(permissionHandler.requiredPermissions) },
                        onDisconnectAll = viewModel::disconnectAll,
                        onAssign = viewModel::assignToPlayer,
                        onUnassign = viewModel::unassign,
                        onDisconnect = viewModel::disconnect,
                        onGamepadToggle = { enabled ->
                            gamepadViewModel.toggle(enabled, state.activePlayers)
                        },
                        gamepadEmulators = gamepadViewModel.gamepadEmulators,
                        selectedGamepadEmulator = selectedEmulator,
                        onSelectGamepadEmulator = gamepadViewModel::selectEmulator,
                        gamepadSetupAvailable = privilegedAccess,
                        gamepadSetupPhase = gamepadSetupPhase,
                        onConfigureGamepad = { gamepadViewModel.configureGamepad(state.activePlayers) },
                        onOpenGamepadMapping = {
                            mappingConsole = if (selectedEmulator in EdenGamepadConfig.PACKAGES) {
                                Console.SWITCH_PRO
                            } else {
                                Console.GAMECUBE
                            }
                        },
                        onDsuToggle = dsuViewModel::toggle,
                        onConfigureDolphin = { dsuViewModel.configureDolphinDsu(state.activePlayers) },
                        onOpenDsuMapping = { mappingConsole = Console.WIIMOTE_NUNCHUK },
                        onOpenSettings = { startActivity(permissionHandler.buildSettingsIntent()) },
                        onEnableNotifications = enableNotifications,
                        onStartAdbPairing = gamepadViewModel::pairDevice,
                        privilegedAccess = privilegedAccess,
                        viewMode = viewMode,
                        onViewModeChange = viewModel::setViewMode,
                    )
                }
            }
        }
    }
}
