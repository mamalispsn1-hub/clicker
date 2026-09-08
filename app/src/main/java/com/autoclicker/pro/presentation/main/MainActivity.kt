package com.autoclicker.pro.presentation.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autoclicker.pro.overlay.OverlayService
import com.autoclicker.pro.presentation.appbindings.AppBindingsScreen
import com.autoclicker.pro.presentation.debug.DebugScreen
import com.autoclicker.pro.presentation.editor.ClickPointEditorScreen
import com.autoclicker.pro.presentation.navigation.Screen
import com.autoclicker.pro.presentation.profiles.ProfilesScreen
import com.autoclicker.pro.presentation.settings.SettingsScreen
import com.autoclicker.pro.presentation.theme.AutoClickerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoClickerTheme {
                val context = LocalContext.current

                LaunchedEffect(Unit) { viewModel.refreshPermissions() }

                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Screen.Main.route) {
                    composable(Screen.Main.route) {
                        MainScreen(
                            viewModel = viewModel,
                            onEdit = { navController.navigate(Screen.Editor.route) },
                            onProfiles = { navController.navigate(Screen.Profiles.route) },
                            onSettings = { navController.navigate(Screen.Settings.route) },
                            onDebug = { navController.navigate(Screen.Debug.route) },
                            onRequestOverlayPermission = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                )
                            },
                            onRequestAccessibilityPermission = {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onShowOverlay = { OverlayService.show(context) },
                            onHideOverlay = { OverlayService.hide(context) }
                        )
                    }
                    composable(Screen.Editor.route) {
                        ClickPointEditorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Profiles.route) {
                        ProfilesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onManageAppBindings = { navController.navigate(Screen.AppBindings.route) }
                        )
                    }
                    composable(Screen.AppBindings.route) {
                        AppBindingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Debug.route) {
                        DebugScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Overlay / Accessibility permissions are granted in system Settings
        // screens that we navigate away to, so re-check every time we regain focus.
        viewModel.refreshPermissions()
    }
}
