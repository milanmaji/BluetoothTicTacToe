package com.example.tictactoe.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.example.tictactoe.data.mapper.toByteArray
import com.example.tictactoe.domain.BluetoothServerController
import com.example.tictactoe.domain.model.BluetoothDeviceDomain
import com.example.tictactoe.domain.model.BluetoothMessage
import com.example.tictactoe.domain.model.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothServerController(private val context: Context) : BluetoothServerController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var dataTransferService: BluetoothDataTransferService? = null

    override fun getDeviceName(): String {
       return bluetoothAdapter?.name ?: "Unknown name"
    }
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val bluetoothStateReceiver = BluetoothStateReceiver()

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null


    init {

        bluetoothStateReceiver.onStateChanged = { isConnected, bluetoothDevice ->
            if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
                Log.d("Connection","isConnected: $isConnected")
                _isConnected.update { isConnected }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    _errors.emit("Can't connect to a non-paired device.")
                }
            }
        }

        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )


    }



    override fun startBluetoothServer(): Flow<ConnectionResult> {

        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }
            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                SERVICE_NAME,
                UUID.fromString(SERVICE_UUID)
            )

//            var shouldLoop = true
//
//            while (shouldLoop) {
//                currentClientSocket = try {
//                    currentServerSocket?.accept()
//                } catch (_: IOException) {
//                    shouldLoop = false
//                    null
//                }
//
//                currentClientSocket?.let {
//                    emit(ConnectionResult.ConnectionEstablished)
//                    currentServerSocket?.close()
//                    val service = BluetoothDataTransferService(it)
//                    dataTransferService = service
//
//                    emitAll(
//                        service
//                            .listenForIncomingMessages()
//                            .map {
//                                ConnectionResult.TransferSucceeded(it)
//                            }
//                    )
//                }
//
//            }
            val socket = currentServerSocket?.accept() // blocking call
            currentClientSocket = socket
            currentServerSocket?.close() // Only allow one connection
            currentServerSocket = null

            socket?.let {
                val service = BluetoothDataTransferService(it)
                dataTransferService = service

                emit(ConnectionResult.ConnectionEstablished) // Moved after setting up the service

                emitAll(
                    service.listenForIncomingMessages()
                        .map { message -> ConnectionResult.TransferSucceeded(message) }
                )
            }


        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {

        return  flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }
            currentClientSocket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()

                    BluetoothDataTransferService(socket).also {
                        dataTransferService = it
                        emit(ConnectionResult.ConnectionEstablished)
                        emitAll(
                            it.listenForIncomingMessages()
                                .map { ConnectionResult.TransferSucceeded(it) }
                        )
                    }
                } catch (_: IOException) {
                    socket.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Connection was interrupted"))
                }
            }

        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)

    }

    override suspend fun trySendMessage(bluetoothMessage: BluetoothMessage): BluetoothMessage? {
        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return null
        }

        if(dataTransferService == null) {
            return null
        }

//        val bluetoothMessage = BluetoothMessage(
//            index = index,
//            senderName = bluetoothAdapter?.name ?: "Unknown name",
//            isFromLocalUser = true
//        )

        dataTransferService?.sendMessage(bluetoothMessage.toByteArray())

        return bluetoothMessage
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
    }

    override fun release() {
        try {
            bluetoothStateReceiver.onStateChanged = null
            context.unregisterReceiver(bluetoothStateReceiver)
        }catch (_: Exception){}
        closeConnection()
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val SERVICE_NAME = "TicTacToeBluetooth"
    }
}