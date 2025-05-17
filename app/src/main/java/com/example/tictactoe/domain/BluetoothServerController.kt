package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.BluetoothDeviceDomain
import com.example.tictactoe.domain.model.BluetoothMessage
import com.example.tictactoe.domain.model.ConnectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothServerController {
    val isConnected: StateFlow<Boolean>
    val errors: SharedFlow<String>

    fun getDeviceName(): String
    fun startBluetoothServer(): Flow<ConnectionResult>
    fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult>

    suspend fun trySendMessage(index: BluetoothMessage): BluetoothMessage?

    fun closeConnection()
    fun release()
}