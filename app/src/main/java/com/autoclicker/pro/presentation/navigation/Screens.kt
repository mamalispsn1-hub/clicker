package com.autoclicker.pro.presentation.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Editor : Screen("editor")
    data object Profiles : Screen("profiles")
    data object Settings : Screen("settings")
    data object AppBindings : Screen("app_bindings")
    data object Debug : Screen("debug")
}
