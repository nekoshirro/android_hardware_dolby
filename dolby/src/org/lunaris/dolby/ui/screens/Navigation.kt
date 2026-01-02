/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.lunaris.dolby.ui.viewmodel.DolbyViewModel
import org.lunaris.dolby.ui.viewmodel.EqualizerViewModel

sealed class Screen(val route: String) {
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
}

@Composable
fun DolbyNavHost(
    dolbyViewModel: DolbyViewModel,
    equalizerViewModel: EqualizerViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Settings.route
    ) {
        composable(Screen.Settings.route) {
            ModernDolbySettingsScreen(
                viewModel = dolbyViewModel,
                onNavigateToEqualizer = {
                    navController.navigate(Screen.Equalizer.route)
                }
            )
        }

        composable(Screen.Equalizer.route) {
            ModernEqualizerScreen(
                viewModel = equalizerViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
