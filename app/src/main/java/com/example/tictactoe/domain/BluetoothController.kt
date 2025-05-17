package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.BluetoothDeviceDomain
import com.example.tictactoe.domain.model.BluetoothMessage
import com.example.tictactoe.domain.model.ConnectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isScanStarted: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>

    fun pairDevice(device: BluetoothDeviceDomain)
    fun startDiscovery()
    fun stopDiscovery()
    fun release()
}