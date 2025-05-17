package com.example.tictactoe.prasentation.ui.gameBoard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.tictactoe.domain.BluetoothServerController
import com.example.tictactoe.domain.model.BluetoothDeviceDomain
import com.example.tictactoe.domain.model.BluetoothMessage
import com.example.tictactoe.domain.model.ConnectionResult
import com.example.tictactoe.domain.usecase.MakeMoveUseCase
import com.example.tictactoe.prasentation.MainDest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val makeMoveUseCase: MakeMoveUseCase,
    private val bluetoothServerController: BluetoothServerController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val gameBoard: MainDest.GameBoard = savedStateHandle.toRoute()

    private val _gameUiState = MutableStateFlow<GameUiState>(GameUiState.Connecting(false))
    val gameUiState: StateFlow<GameUiState> = _gameUiState

    private var localPlayerInfo: PlayerInfo = PlayerInfo("", Player.X, true)
    private var opponentPlayerInfo: PlayerInfo = PlayerInfo("", Player.O, false)
    private var isHostDevice = true

    private var deviceConnectionJob: Job? = null


    init {

        if (gameBoard.deviceAddress != null) {
            isHostDevice = false
            setPlayerRole(
                name = gameBoard.name?.takeIf { it.isNotBlank() }
                    ?: bluetoothServerController.getDeviceName(),
                player = Player.X,
                isFromLocal = true,
            )
            connectToDevice(BluetoothDeviceDomain(gameBoard.deviceName, gameBoard.deviceAddress))
        } else {
            isHostDevice = true
            setPlayerRole(
                name = gameBoard.name?.takeIf { it.isNotBlank() }
                    ?: bluetoothServerController.getDeviceName(),
                player = Player.from(gameBoard.selectedPlayer ?: "") ?: Player.X,
                isFromLocal = true,
            )

            waitForIncomingConnections()
        }

        bluetoothServerController.isConnected.onEach { isConnected ->
            Log.d("Connection", "isConnected observer: $isConnected")
//            if (isConnected) {
//                _gameUiState.update {
//                    GameUiState.Connected(
//                        localPlayerInfo = localPlayerInfo,
//                        opponentPlayerInfo = opponentPlayerInfo,
//                        isServer = isHostDevice
//                    )
//                }
//            }
        }.launchIn(viewModelScope)

        bluetoothServerController.errors.onEach { error ->
            _gameUiState.update {
                GameUiState.Error(
                    errorMessage = error
                )
            }
        }.launchIn(viewModelScope)
    }


    fun connectToDevice(device: BluetoothDeviceDomain) {
        _gameUiState.update { GameUiState.Connecting(isServer = false) }
        deviceConnectionJob = bluetoothServerController
            .connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothServerController.closeConnection()
        _gameUiState.update { GameUiState.Disconnected }
    }

    fun waitForIncomingConnections() {
        _gameUiState.update { GameUiState.Connecting(isServer = true) }
        deviceConnectionJob = bluetoothServerController
            .startBluetoothServer()
            .listen()
    }

    fun setPlayerRole(name: String, player: Player?, isFromLocal: Boolean) {

        if (isFromLocal) {
            localPlayerInfo = localPlayerInfo.copy(
                name = name,
                player = player ?: localPlayerInfo.player,
                isHost = isHostDevice
            )
            opponentPlayerInfo = opponentPlayerInfo.copy(
                player = player?.opponent() ?: opponentPlayerInfo.player,
                isHost = isHostDevice
            )
        } else {
            localPlayerInfo = localPlayerInfo.copy(
                player = player?.opponent() ?: localPlayerInfo.player,
                isHost = isHostDevice
            )
            opponentPlayerInfo = opponentPlayerInfo.copy(
                name = name,
                player = player ?: opponentPlayerInfo.player,
                isHost = isHostDevice
            )
        }

        val state = _gameUiState.value as? GameUiState.Connected ?: return
        _gameUiState.update {
            state.copy(
                localPlayerInfo = localPlayerInfo,
                opponentPlayerInfo = opponentPlayerInfo
            )
        }
    }

    private fun startGame() {
        _gameUiState.update {
            GameUiState.GameStarted.create(
                localPlayerInfo = localPlayerInfo,
                opponentPlayerInfo = opponentPlayerInfo
            )
        }
    }

    fun startButtonClick() {
        if (!localPlayerInfo.isHost) return
        startGame()
        sendMessage(BluetoothMessage.Start(localPlayerInfo.player))
    }

    fun restartButtonClick() {
        if (!localPlayerInfo.isHost) return
        startGame()
        sendMessage(BluetoothMessage.Restart)
    }

    fun onCellClicked(index: Int) {
        val state = _gameUiState.value as? GameUiState.GameStarted ?: return
        if (state.currentPlayer != localPlayerInfo.player) return
        if (state.result != GameResult.InProgress) return
        if (state.board[index].isNotEmpty()) return

        _gameUiState.update { makeMoveUseCase.invoke(state, index) }
        sendMessage(BluetoothMessage.Turn(index = index, player = localPlayerInfo.player))
    }

    fun sendMessage(bluetoothMessage: BluetoothMessage) {
        viewModelScope.launch {
            val bluetoothMessage = bluetoothServerController.trySendMessage(bluetoothMessage)
            if (bluetoothMessage != null) {
                Log.d("message", "message Sent: $bluetoothMessage")
            }
        }
    }


    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                ConnectionResult.ConnectionEstablished -> {
                    // _gameUiState.update { GameUiState.Connected(playerName = null) }
                    Log.d("Connection", "ConnectionEstablished: ${localPlayerInfo.name}")
                    _gameUiState.update {
                        GameUiState.Connected(
                            localPlayerInfo = localPlayerInfo,
                            opponentPlayerInfo = opponentPlayerInfo,
                            isServer = isHostDevice
                        )
                    }
                    sendMessage(
                        BluetoothMessage.Name(
                            name = localPlayerInfo.name,
                            player = if (isHostDevice) localPlayerInfo.player else null,
                            isLocal = true
                        )
                    )

                }

                is ConnectionResult.TransferSucceeded -> {
                    handleMessage(result.message)
                }

                is ConnectionResult.Error -> {
                    _gameUiState.update {
                        GameUiState.Error(result.message)
                    }
                }
            }
        }.catch { throwable ->
            bluetoothServerController.closeConnection()
            _gameUiState.update { GameUiState.Disconnected }
        }.launchIn(viewModelScope)
    }

    private fun handleMessage(msg: BluetoothMessage) {
        Log.d("message", "message received: $msg")
        when (msg) {
            is BluetoothMessage.Start -> {
                startGame()
            }

            is BluetoothMessage.Turn -> {
                val state = _gameUiState.value as? GameUiState.GameStarted ?: return
                if (state.currentPlayer == msg.player && state.board[msg.index].isEmpty()) {
                    _gameUiState.update { makeMoveUseCase.invoke(state, msg.index) }
                }
            }

            is BluetoothMessage.Restart -> {
                startGame()
            }

            is BluetoothMessage.Name -> {
                _gameUiState.value as? GameUiState.Connected ?: return
                setPlayerRole(name = msg.name, player = msg.player, isFromLocal = msg.isLocal)
            }

            BluetoothMessage.Unknown -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothServerController.release()
    }
}