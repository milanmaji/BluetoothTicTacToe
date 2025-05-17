package com.example.tictactoe.domain.usecase

import com.example.tictactoe.domain.BluetoothController
import javax.inject.Inject

class StartBluetoothDiscoveryUseCase @Inject constructor(private val bluetoothController: BluetoothController
) {

    operator fun invoke() {
        bluetoothController.startDiscovery()
    }
}