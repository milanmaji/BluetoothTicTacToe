package com.example.tictactoe.prasentation.ui.deviceScan

import com.example.tictactoe.domain.model.BluetoothDeviceDomain

data class DeviceScanUiState(
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val pairedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val isScanning: Boolean = false
)
