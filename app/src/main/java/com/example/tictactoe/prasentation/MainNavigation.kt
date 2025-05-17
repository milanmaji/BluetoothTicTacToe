package com.example.tictactoe.prasentation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.tictactoe.domain.model.BluetoothDeviceDomain
import com.example.tictactoe.prasentation.component.CustomNavType
import com.example.tictactoe.prasentation.ui.deviceScan.DeviceScanScreen
import com.example.tictactoe.prasentation.ui.gameBoard.GameScreen
import com.example.tictactoe.prasentation.ui.home.HomeScreen
import kotlin.reflect.typeOf

@Composable
fun MainNavigation(
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()

    Box(modifier = modifier) {
        NavHost(navController = navController, startDestination = MainDest.Home) {

            composable<MainDest.Home> {
                HomeScreen(
                    homeViewModel = hiltViewModel(),
                    createGame = { name, player ->
                        navController.navigate(
                            MainDest.GameBoard(
                                name = name,
                                deviceName = null,
                                deviceAddress = null,
                                selectedPlayer = player.name
                            )
                        )
                    },
                    joinGame = { name ->
                        navController.navigate(MainDest.DeviceScan(name))
                    }
                )
            }

            composable<MainDest.DeviceScan> { backStackEntry ->
                val deviceScan: MainDest.DeviceScan = backStackEntry.toRoute()
                DeviceScanScreen(deviceScanViewModel = hiltViewModel()) { device ->
                    navController.navigate(
                        MainDest.GameBoard(
                            name = deviceScan.name,
                            deviceName = device.name,
                            deviceAddress = device.address,
                            selectedPlayer = null
                        )
                    ) {
                        popUpTo(MainDest.Home)
                    }
                }
            }

            composable<MainDest.GameBoard> {
                GameScreen(gameViewModel = hiltViewModel()) {
                    navController.navigate(MainDest.Home) {
                        popUpTo(MainDest.Home) { inclusive = true }
                    }
                }
            }
        }

    }

}